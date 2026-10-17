package com.vp.whattowatch.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.vp.whattowatch.data.local.AppDatabase
import com.vp.whattowatch.data.local.dao.CategoryDao
import com.vp.whattowatch.data.local.dao.ContentDao
import com.vp.whattowatch.data.local.dao.CustomListDao
import com.vp.whattowatch.data.local.entity.CategoryEntity
import com.vp.whattowatch.data.local.entity.ContentCategoryCrossRef
import com.vp.whattowatch.data.local.entity.ContentEntity
import com.vp.whattowatch.data.local.entity.CustomListEntity
import com.vp.whattowatch.data.local.entity.ListItemCrossRef
import com.vp.whattowatch.data.local.image.ImageStorageManager
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.WatchStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupService(
    private val context: Context,
    private val database: AppDatabase,
    private val contentDao: ContentDao,
    private val categoryDao: CategoryDao,
    private val customListDao: CustomListDao,
    private val imageStorageManager: ImageStorageManager
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    companion object {
        const val FILE_MANIFEST = "manifest.json"
        const val FILE_LIBRARY = "library.json"
        const val DIR_POSTERS = "posters"

        // Security limits
        const val MAX_ZIP_ENTRIES = 10_000
        const val MAX_ENTRY_BYTES = 25L * 1024L * 1024L // 25 MB max per file
        const val MAX_TOTAL_UNCOMPRESSED_BYTES = 500L * 1024L * 1024L // 500 MB max total
    }

    /**
     * Exports the complete library database and referenced poster images to a streaming ZIP archive.
     */
    suspend fun exportBackup(outputStream: OutputStream): BackupResult = withContext(Dispatchers.IO) {
        var totalBytesWritten = 0L
        try {
            val contents = contentDao.getAllEntities()
            val categories = categoryDao.getAll()
            val customLists = customListDao.getAll()
            val contentCategories = contentDao.getAllContentCategoryCrossRefs()
            val listItems = contentDao.getAllListItemCrossRefs()

            // Collect existing poster files
            val referencedPosters = contents
                .mapNotNull { it.posterPath?.trim()?.ifBlank { null } }
                .distinct()
                .mapNotNull { relativeName ->
                    val file = imageStorageManager.getPosterFile(relativeName)
                    if (file.exists() && file.isFile && file.length() > 0) relativeName to file else null
                }

            val manifest = BackupManifest(
                version = BackupManifest.CURRENT_VERSION,
                appName = "What to Watch",
                appVersion = "1.0",
                createdAt = System.currentTimeMillis(),
                itemCount = contents.size,
                listCount = customLists.size,
                categoryCount = categories.size,
                imageCount = referencedPosters.size
            )

            val payload = BackupLibraryPayload(
                contents = contents.map {
                    BackupContentItem(
                        id = it.id,
                        title = it.title,
                        contentType = it.contentType,
                        posterPath = it.posterPath,
                        externalUrl = it.externalUrl,
                        description = it.description,
                        watchStatus = it.watchStatus,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                },
                categories = categories.map {
                    BackupCategory(
                        id = it.id,
                        name = it.name,
                        createdAt = it.createdAt
                    )
                },
                customLists = customLists.map {
                    BackupCustomList(
                        id = it.id,
                        name = it.name,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                },
                contentCategories = contentCategories.map {
                    BackupContentCategoryRef(
                        contentId = it.contentId,
                        categoryId = it.categoryId
                    )
                },
                listItems = listItems.map {
                    BackupListItemRef(
                        listId = it.listId,
                        contentId = it.contentId
                    )
                }
            )

            val manifestJsonBytes = json.encodeToString(BackupManifest.serializer(), manifest).toByteArray(Charsets.UTF_8)
            val libraryJsonBytes = json.encodeToString(BackupLibraryPayload.serializer(), payload).toByteArray(Charsets.UTF_8)

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // 1. Write manifest.json
                val manifestEntry = ZipEntry(FILE_MANIFEST)
                zipOut.putNextEntry(manifestEntry)
                zipOut.write(manifestJsonBytes)
                zipOut.closeEntry()
                totalBytesWritten += manifestJsonBytes.size

                // 2. Write library.json
                val libraryEntry = ZipEntry(FILE_LIBRARY)
                zipOut.putNextEntry(libraryEntry)
                zipOut.write(libraryJsonBytes)
                zipOut.closeEntry()
                totalBytesWritten += libraryJsonBytes.size

                // 3. Write poster files
                for ((relativeName, posterFile) in referencedPosters) {
                    val entryName = "$DIR_POSTERS/$relativeName"
                    val posterEntry = ZipEntry(entryName)
                    zipOut.putNextEntry(posterEntry)
                    posterFile.inputStream().use { input ->
                        val copied = input.copyTo(zipOut)
                        totalBytesWritten += copied
                    }
                    zipOut.closeEntry()
                }
                zipOut.finish()
                zipOut.flush()
            }

            BackupResult.Success(manifest, totalBytesWritten)
        } catch (e: Exception) {
            BackupResult.Error("Failed to export backup: ${e.localizedMessage ?: e.message}", e)
        }
    }

    /**
     * Inspects and validates a ZIP backup without committing any changes to the database.
     * Extracts files to a temporary staging folder to detect conflicts, counts, and errors.
     */
    suspend fun previewRestore(inputStream: InputStream): RestorePreview = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "restore_staging_${UUID.randomUUID()}").apply { mkdirs() }
        val stagingPostersDir = File(stagingDir, DIR_POSTERS).apply { mkdirs() }
        val validationErrors = mutableListOf<String>()

        var manifest: BackupManifest? = null
        var payload: BackupLibraryPayload? = null
        var entryCount = 0
        var totalExtractedBytes = 0L

        try {
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    entryCount++
                    if (entryCount > MAX_ZIP_ENTRIES) {
                        throw SecurityException("Backup archive exceeds maximum entry limit of $MAX_ZIP_ENTRIES files.")
                    }

                    val sanitizedName = sanitizeZipEntryName(entry.name)

                    if (!entry.isDirectory) {
                        if (sanitizedName == FILE_MANIFEST) {
                            val manifestBytes = readEntryBytesBounded(zipIn, MAX_ENTRY_BYTES)
                            totalExtractedBytes += manifestBytes.size
                            manifest = json.decodeFromString(BackupManifest.serializer(), String(manifestBytes, Charsets.UTF_8))
                        } else if (sanitizedName == FILE_LIBRARY) {
                            val libraryBytes = readEntryBytesBounded(zipIn, MAX_ENTRY_BYTES)
                            totalExtractedBytes += libraryBytes.size
                            payload = json.decodeFromString(BackupLibraryPayload.serializer(), String(libraryBytes, Charsets.UTF_8))
                        } else if (sanitizedName.startsWith("$DIR_POSTERS/")) {
                            val fileName = sanitizedName.removePrefix("$DIR_POSTERS/")
                            val targetPosterFile = File(stagingPostersDir, fileName)
                            // Double-check path traversal for target file
                            if (!targetPosterFile.canonicalPath.startsWith(stagingPostersDir.canonicalPath)) {
                                throw SecurityException("Path traversal attempt in poster entry: ${entry.name}")
                            }

                            FileOutputStream(targetPosterFile).use { out ->
                                val copied = copyBytesBounded(zipIn, out, MAX_ENTRY_BYTES)
                                totalExtractedBytes += copied
                            }
                        }
                    }

                    if (totalExtractedBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                        throw SecurityException("Backup archive exceeds maximum decompression size limit of 500 MB.")
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            // Step 1: Validate manifest presence and version
            if (manifest == null) {
                validationErrors.add("Missing required manifest.json in archive.")
            } else if (manifest.version > BackupManifest.CURRENT_VERSION) {
                validationErrors.add(
                    "Unsupported backup version ${manifest.version}. This version of What to Watch supports version ${BackupManifest.CURRENT_VERSION}."
                )
            }

            // Step 2: Validate library payload
            if (payload == null) {
                validationErrors.add("Missing or corrupted library.json in archive.")
            }

            val validPayload = payload
            if (validPayload != null) {
                // Validate items
                val idSet = mutableSetOf<String>()
                for (item in validPayload.contents) {
                    if (item.id.isBlank()) {
                        validationErrors.add("Content item found with a missing or blank ID.")
                    } else if (!idSet.add(item.id)) {
                        validationErrors.add("Duplicate content ID found in backup: ${item.id}")
                    }
                    if (item.title.isBlank()) {
                        validationErrors.add("Content item '${item.id}' has an empty title.")
                    }
                    if (ContentType.fromString(item.contentType) == ContentType.OTHER && item.contentType != "OTHER") {
                        // Valid, falls back safely to OTHER
                    }
                    if (WatchStatus.fromString(item.watchStatus) == WatchStatus.NOT_WATCHED && item.watchStatus != "NOT_WATCHED") {
                        // Valid, falls back safely to NOT_WATCHED
                    }
                }
            }

            val conflicts = mutableListOf<RestoreConflict>()
            if (validationErrors.isEmpty() && validPayload != null) {
                val existingEntities = contentDao.getAllEntities()

                for (backupItem in validPayload.contents) {
                    val sameIdMatch = existingEntities.find { it.id == backupItem.id }
                    if (sameIdMatch != null) {
                        conflicts.add(
                            RestoreConflict(
                                backupItemId = backupItem.id,
                                backupTitle = backupItem.title,
                                backupContentType = backupItem.contentType,
                                existingItemId = sameIdMatch.id,
                                existingTitle = sameIdMatch.title,
                                conflictReason = ConflictReason.SAME_ID
                            )
                        )
                    } else {
                        val sameTitleTypeMatch = existingEntities.find {
                            it.title.equals(backupItem.title.trim(), ignoreCase = true) &&
                                    it.contentType.equals(backupItem.contentType, ignoreCase = true)
                        }
                        if (sameTitleTypeMatch != null) {
                            conflicts.add(
                                RestoreConflict(
                                    backupItemId = backupItem.id,
                                    backupTitle = backupItem.title,
                                    backupContentType = backupItem.contentType,
                                    existingItemId = sameTitleTypeMatch.id,
                                    existingTitle = sameTitleTypeMatch.title,
                                    conflictReason = ConflictReason.SAME_TITLE_AND_TYPE
                                )
                            )
                        }
                    }
                }
            }

            val extractedImageCount = stagingPostersDir.listFiles { f -> f.isFile && f.length() > 0 }?.size ?: 0

            RestorePreview(
                isValid = validationErrors.isEmpty(),
                manifest = manifest,
                payload = validPayload,
                imageFileCount = extractedImageCount,
                validationErrors = validationErrors,
                conflicts = conflicts,
                stagingDir = stagingDir
            )
        } catch (e: Exception) {
            cleanUpDir(stagingDir)
            RestorePreview(
                isValid = false,
                manifest = manifest,
                payload = payload,
                imageFileCount = 0,
                validationErrors = listOf("Failed to inspect backup archive: ${e.localizedMessage ?: e.message}"),
                stagingDir = null
            )
        }
    }

    /**
     * Executes the restore from an inspected preview.
     * In REPLACE mode, completely replaces local data with the archive contents.
     * In MERGE mode, retains existing records and resolves duplicates based on the strategy.
     */
    suspend fun executeRestore(
        preview: RestorePreview,
        mode: RestoreMode,
        conflictStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.KEEP_BOTH_IMPORT_AS_COPY
    ): RestoreResult = withContext(Dispatchers.IO) {
        val payload = preview.payload
        val stagingDir = preview.stagingDir
        if (!preview.isValid || payload == null || stagingDir == null || !stagingDir.exists()) {
            cleanUpDir(stagingDir)
            return@withContext RestoreResult.Error("Cannot execute restore: backup validation failed or staging directory expired.")
        }

        try {
            var restoredCount = 0
            val stagingPostersDir = File(stagingDir, DIR_POSTERS)

            if (mode == RestoreMode.REPLACE) {
                database.withTransaction {
                    // Wipe all existing data
                    contentDao.deleteAllContents()
                    contentDao.deleteAllContentCategories()
                    contentDao.deleteAllListItems()
                    categoryDao.deleteAll()
                    customListDao.deleteAll()

                    // Insert categories
                    categoryDao.upsertAll(
                        payload.categories.map {
                            CategoryEntity(id = it.id, name = it.name, createdAt = it.createdAt)
                        }
                    )

                    // Insert custom lists
                    customListDao.insertAll(
                        payload.customLists.map {
                            CustomListEntity(id = it.id, name = it.name, createdAt = it.createdAt, updatedAt = it.updatedAt)
                        }
                    )

                    // Insert contents
                    val contentEntities = payload.contents.map {
                        ContentEntity(
                            id = it.id,
                            title = it.title,
                            contentType = it.contentType,
                            posterPath = it.posterPath,
                            externalUrl = it.externalUrl,
                            description = it.description,
                            watchStatus = it.watchStatus,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt
                        )
                    }
                    contentDao.insertAll(contentEntities)
                    restoredCount = contentEntities.size

                    // Insert relations
                    contentDao.insertContentCategories(
                        payload.contentCategories.map {
                            ContentCategoryCrossRef(contentId = it.contentId, categoryId = it.categoryId)
                        }
                    )
                    contentDao.insertListItems(
                        payload.listItems.map {
                            ListItemCrossRef(listId = it.listId, contentId = it.contentId)
                        }
                    )
                }

                // Copy all staging poster files into app-private posters dir
                if (stagingPostersDir.exists()) {
                    stagingPostersDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.length() > 0) {
                            file.copyTo(File(imageStorageManager.postersDir, file.name), overwrite = true)
                        }
                    }
                }

                // Delete orphaned poster files that are not referenced by any content item
                val activePosterPaths = contentDao.getAllPosterPaths().toSet()
                imageStorageManager.getAllStoredPosterFilenames().forEach { filename ->
                    if (!activePosterPaths.contains(filename)) {
                        imageStorageManager.deletePoster(filename)
                    }
                }
            } else {
                // MERGE MODE
                database.withTransaction {
                    // 1. Merge Categories by Name
                    val existingCategories = categoryDao.getAll()
                    val categoryIdMap = mutableMapOf<String, String>()

                    for (backupCat in payload.categories) {
                        val matched = existingCategories.find { it.name.equals(backupCat.name.trim(), ignoreCase = true) }
                        if (matched != null) {
                            categoryIdMap[backupCat.id] = matched.id
                        } else {
                            categoryDao.insert(CategoryEntity(backupCat.id, backupCat.name.trim(), backupCat.createdAt))
                            categoryIdMap[backupCat.id] = backupCat.id
                        }
                    }

                    // 2. Merge Custom Lists by Name
                    val existingLists = customListDao.getAll()
                    val listIdMap = mutableMapOf<String, String>()

                    for (backupList in payload.customLists) {
                        val matched = existingLists.find { it.name.equals(backupList.name.trim(), ignoreCase = true) }
                        if (matched != null) {
                            listIdMap[backupList.id] = matched.id
                        } else {
                            customListDao.insert(CustomListEntity(backupList.id, backupList.name.trim(), backupList.createdAt, backupList.updatedAt))
                            listIdMap[backupList.id] = backupList.id
                        }
                    }

                    // 3. Merge Contents & Conflicts
                    val existingContents = contentDao.getAllEntities()
                    val contentIdMap = mutableMapOf<String, String>()

                    for (backupItem in payload.contents) {
                        val sameIdMatch = existingContents.find { it.id == backupItem.id }
                        val sameTitleTypeMatch = existingContents.find {
                            it.title.equals(backupItem.title.trim(), ignoreCase = true) &&
                                    it.contentType.equals(backupItem.contentType, ignoreCase = true)
                        }

                        val hasConflict = sameIdMatch != null || sameTitleTypeMatch != null

                        if (!hasConflict) {
                            // Clean insert
                            contentDao.insertOrUpdate(
                                ContentEntity(
                                    id = backupItem.id,
                                    title = backupItem.title,
                                    contentType = backupItem.contentType,
                                    posterPath = backupItem.posterPath,
                                    externalUrl = backupItem.externalUrl,
                                    description = backupItem.description,
                                    watchStatus = backupItem.watchStatus,
                                    createdAt = backupItem.createdAt,
                                    updatedAt = backupItem.updatedAt
                                )
                            )
                            contentIdMap[backupItem.id] = backupItem.id
                            restoredCount++
                        } else {
                            when (conflictStrategy) {
                                ConflictResolutionStrategy.SKIP_EXISTING -> {
                                    // Map to existing ID so relations can still link if desired
                                    val matchedId = sameIdMatch?.id ?: sameTitleTypeMatch!!.id
                                    contentIdMap[backupItem.id] = matchedId
                                }

                                ConflictResolutionStrategy.OVERWRITE_EXISTING -> {
                                    val targetId = sameIdMatch?.id ?: sameTitleTypeMatch!!.id
                                    contentDao.insertOrUpdate(
                                        ContentEntity(
                                            id = targetId,
                                            title = backupItem.title,
                                            contentType = backupItem.contentType,
                                            posterPath = backupItem.posterPath,
                                            externalUrl = backupItem.externalUrl,
                                            description = backupItem.description,
                                            watchStatus = backupItem.watchStatus,
                                            createdAt = backupItem.createdAt,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                    contentIdMap[backupItem.id] = targetId
                                    restoredCount++
                                }

                                ConflictResolutionStrategy.KEEP_BOTH_IMPORT_AS_COPY -> {
                                    val newId = UUID.randomUUID().toString()
                                    val copyTitle = if (sameTitleTypeMatch != null) "${backupItem.title} (Copy)" else backupItem.title
                                    contentDao.insertOrUpdate(
                                        ContentEntity(
                                            id = newId,
                                            title = copyTitle,
                                            contentType = backupItem.contentType,
                                            posterPath = backupItem.posterPath,
                                            externalUrl = backupItem.externalUrl,
                                            description = backupItem.description,
                                            watchStatus = backupItem.watchStatus,
                                            createdAt = backupItem.createdAt,
                                            updatedAt = backupItem.updatedAt
                                        )
                                    )
                                    contentIdMap[backupItem.id] = newId
                                    restoredCount++
                                }
                            }
                        }
                    }

                    // 4. Merge Relations
                    for (catRef in payload.contentCategories) {
                        val finalContentId = contentIdMap[catRef.contentId]
                        val finalCategoryId = categoryIdMap[catRef.categoryId]
                        if (finalContentId != null && finalCategoryId != null) {
                            contentDao.insertContentCategories(listOf(ContentCategoryCrossRef(finalContentId, finalCategoryId)))
                        }
                    }

                    for (listRef in payload.listItems) {
                        val finalContentId = contentIdMap[listRef.contentId]
                        val finalListId = listIdMap[listRef.listId]
                        if (finalContentId != null && finalListId != null) {
                            contentDao.insertListItems(listOf(ListItemCrossRef(finalListId, finalContentId)))
                        }
                    }
                }

                // Copy referenced poster images from staging
                if (stagingPostersDir.exists()) {
                    val allPosterPaths = contentDao.getAllPosterPaths().toSet()
                    stagingPostersDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.length() > 0 && allPosterPaths.contains(file.name)) {
                            file.copyTo(File(imageStorageManager.postersDir, file.name), overwrite = true)
                        }
                    }
                }
            }

            cleanUpDir(stagingDir)
            RestoreResult.Success(restoredCount, mode)
        } catch (e: Exception) {
            cleanUpDir(stagingDir)
            RestoreResult.Error("Failed to restore library: ${e.localizedMessage ?: e.message}", e)
        }
    }

    private fun sanitizeZipEntryName(entryName: String): String {
        val normalized = File(entryName).normalize().path.replace("\\", "/")
        if (normalized.startsWith("../") || normalized.contains("/../") || normalized == ".." || normalized.startsWith("/")) {
            throw SecurityException("Malicious ZIP entry with path traversal: $entryName")
        }
        return normalized
    }

    private fun readEntryBytesBounded(zipIn: ZipInputStream, maxBytes: Long): ByteArray {
        val buffer = ByteArray(8192)
        val out = java.io.ByteArrayOutputStream()
        var total = 0L
        var read = zipIn.read(buffer)
        while (read != -1) {
            total += read
            if (total > maxBytes) {
                throw SecurityException("ZIP entry exceeded size limit of $maxBytes bytes.")
            }
            out.write(buffer, 0, read)
            read = zipIn.read(buffer)
        }
        return out.toByteArray()
    }

    private fun copyBytesBounded(input: InputStream, output: OutputStream, maxBytes: Long): Long {
        val buffer = ByteArray(8192)
        var total = 0L
        var read = input.read(buffer)
        while (read != -1) {
            total += read
            if (total > maxBytes) {
                throw SecurityException("ZIP entry exceeded size limit of $maxBytes bytes.")
            }
            output.write(buffer, 0, read)
            read = input.read(buffer)
        }
        output.flush()
        return total
    }

    fun cleanUpDir(dir: File?) {
        if (dir != null && dir.exists()) {
            dir.deleteRecursively()
        }
    }
}
