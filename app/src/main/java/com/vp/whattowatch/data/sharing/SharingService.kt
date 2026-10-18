package com.vp.whattowatch.data.sharing

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Base64
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class SharingService(
    private val context: Context,
    private val database: AppDatabase,
    private val contentDao: ContentDao,
    private val categoryDao: CategoryDao,
    private val customListDao: CustomListDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val fileUriProvider: ((File) -> Uri)? = null
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    companion object {
        const val DEEP_LINK_SCHEME = "whattowatch"
        const val DEEP_LINK_HOST = "share"

        // Maximum safe URL length for Android intents and browsers
        const val MAX_DEEP_LINK_URL_LENGTH = 2000
        const val MAX_COMPRESSED_PAYLOAD_STRING_LENGTH = 20_000
        const val MAX_COMPRESSED_PAYLOAD_BYTES = 100L * 1024L // 100 KB
        const val MAX_DECOMPRESSED_PAYLOAD_BYTES = 2L * 1024L * 1024L // 2 MB
        const val DIR_SHARED = "shared"
        const val FILE_EXTENSION = ".whattowatch"
        const val MIME_TYPE_FILE = "application/json"
    }

    /**
     * Builds a SharePayload for a single content item.
     */
    suspend fun createSingleItemSharePayload(contentId: String): SharePayload? = withContext(ioDispatcher) {
        val details = contentDao.getWithDetailsById(contentId) ?: return@withContext null
        val content = details.content
        val categories = details.categories

        val shareItem = ShareContentItem(
            id = content.id,
            title = content.title,
            contentType = content.contentType,
            watchStatus = content.watchStatus,
            externalUrl = sanitizeUrl(content.externalUrl),
            description = content.description,
            createdAt = content.createdAt,
            updatedAt = content.updatedAt
        )

        val shareCategories = categories.map {
            ShareCategory(id = it.id, name = it.name, createdAt = it.createdAt)
        }

        val categoryRefs = categories.map {
            ShareContentCategoryRef(contentId = content.id, categoryId = it.id)
        }

        SharePayload(
            version = SharePayload.CURRENT_VERSION,
            type = ShareType.SINGLE_ITEM,
            title = content.title,
            contents = listOf(shareItem),
            categories = shareCategories,
            customLists = emptyList(),
            contentCategories = categoryRefs,
            listItems = emptyList()
        )
    }

    /**
     * Builds a SharePayload for an entire custom list and all of its items.
     */
    suspend fun createCustomListSharePayload(listId: String): SharePayload? = withContext(ioDispatcher) {
        val customList = customListDao.getById(listId) ?: return@withContext null
        val itemsWithDetails = customListDao.getContentsForList(listId)

        val shareItems = mutableListOf<ShareContentItem>()
        val categoryMap = mutableMapOf<String, ShareCategory>()
        val categoryRefs = mutableListOf<ShareContentCategoryRef>()
        val listRefs = mutableListOf<ShareListItemRef>()

        for (item in itemsWithDetails) {
            val contentEntity = item.content
            shareItems.add(
                ShareContentItem(
                    id = contentEntity.id,
                    title = contentEntity.title,
                    contentType = contentEntity.contentType,
                    watchStatus = contentEntity.watchStatus,
                    externalUrl = sanitizeUrl(contentEntity.externalUrl),
                    description = contentEntity.description,
                    createdAt = contentEntity.createdAt,
                    updatedAt = contentEntity.updatedAt
                )
            )

            listRefs.add(ShareListItemRef(listId = listId, contentId = contentEntity.id))

            item.categories.forEach { cat ->
                categoryMap[cat.id] = ShareCategory(id = cat.id, name = cat.name, createdAt = cat.createdAt)
                categoryRefs.add(ShareContentCategoryRef(contentId = contentEntity.id, categoryId = cat.id))
            }
        }

        val shareCustomList = ShareCustomList(
            id = customList.id,
            name = customList.name,
            createdAt = customList.createdAt,
            updatedAt = customList.updatedAt
        )

        SharePayload(
            version = SharePayload.CURRENT_VERSION,
            type = ShareType.CUSTOM_LIST,
            title = customList.name,
            contents = shareItems,
            categories = categoryMap.values.toList(),
            customLists = listOf(shareCustomList),
            contentCategories = categoryRefs,
            listItems = listRefs
        )
    }

    /**
     * Compresses and encodes the payload into a URL-safe Base64 GZIP string.
     */
    fun compressAndEncode(payload: SharePayload): String {
        val jsonString = json.encodeToString(SharePayload.serializer(), payload)
        val byteOut = ByteArrayOutputStream()
        GZIPOutputStream(byteOut).use { gzip ->
            gzip.write(jsonString.toByteArray(Charsets.UTF_8))
            gzip.finish()
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(byteOut.toByteArray())
    }

    /**
     * Decodes and decompresses a URL-safe Base64 GZIP string into a SharePayload with bounded limits.
     */
    fun decodeAndDecompress(encodedString: String): SharePayload {
        val clean = encodedString.trim()
        if (clean.length > MAX_COMPRESSED_PAYLOAD_STRING_LENGTH) {
            throw SecurityException("Encoded payload exceeds maximum length limit.")
        }

        val compressedBytes = try {
            Base64.getUrlDecoder().decode(clean)
        } catch (_: IllegalArgumentException) {
            try {
                Base64.getDecoder().decode(clean)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid Base64 payload string: ${e.message}", e)
            }
        }
        if (compressedBytes.size > MAX_COMPRESSED_PAYLOAD_BYTES) {
            throw SecurityException("Compressed payload size exceeds limit of $MAX_COMPRESSED_PAYLOAD_BYTES bytes.")
        }

        val decompressedBytes = decompressGzipBounded(compressedBytes, MAX_DECOMPRESSED_PAYLOAD_BYTES)
        val jsonString = String(decompressedBytes, Charsets.UTF_8)
        val payload = json.decodeFromString(SharePayload.serializer(), jsonString)

        if (payload.version > SharePayload.CURRENT_VERSION) {
            throw IllegalArgumentException("Unsupported sharing version ${payload.version}. Maximum supported is ${SharePayload.CURRENT_VERSION}.")
        }

        return payload
    }

    /**
     * Generates a share result: deep-link URL if <= MAX_DEEP_LINK_URL_LENGTH, otherwise a portable shared file.
     */
    suspend fun generateShareResult(payload: SharePayload): ShareResult = withContext(ioDispatcher) {
        try {
            val encoded = compressAndEncode(payload)
            val deepLinkUrl = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/$encoded"

            if (deepLinkUrl.length <= MAX_DEEP_LINK_URL_LENGTH) {
                ShareResult.DeepLink(url = deepLinkUrl, title = payload.title)
            } else {
                // Fall back to file sharing through FileProvider
                val file = createSharedFile(payload)
                val fileUri = fileUriProvider?.invoke(file) ?: run {
                    val authority = "${context.packageName}.fileprovider"
                    FileProvider.getUriForFile(context, authority, file)
                }
                ShareResult.FileShare(
                    fileUriString = fileUri.toString(),
                    mimeType = MIME_TYPE_FILE,
                    title = payload.title
                )
            }
        } catch (e: Exception) {
            ShareResult.Error("Failed to prepare share payload: ${e.localizedMessage ?: e.message}", e)
        }
    }

    /**
     * Writes payload to a shared file in cacheDir/shared and returns the File.
     */
    fun createSharedFile(payload: SharePayload): File {
        val sharedDir = File(context.cacheDir, DIR_SHARED).apply { mkdirs() }
        cleanupSharedDir(sharedDir)

        val sanitizedTitle = payload.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
        val fileName = "whattowatch_${sanitizedTitle}_${System.currentTimeMillis()}$FILE_EXTENSION"
        val targetFile = File(sharedDir, fileName)

        val jsonString = json.encodeToString(SharePayload.serializer(), payload)
        targetFile.writeText(jsonString, Charsets.UTF_8)
        return targetFile
    }

    /**
     * Decodes a deep-link URI (e.g. whattowatch://share/<encoded> or whattowatch://share?data=<encoded>).
     */
    fun decodeFromDeepLink(deepLinkUri: Uri): SharePayload {
        val scheme = deepLinkUri.scheme
        val host = deepLinkUri.host

        if (scheme != DEEP_LINK_SCHEME || host != DEEP_LINK_HOST) {
            throw IllegalArgumentException("Invalid deep link scheme or host: $deepLinkUri")
        }

        val rawEncoded = deepLinkUri.getQueryParameter("data")
            ?: deepLinkUri.getQueryParameter("share")
            ?: deepLinkUri.path?.removePrefix("/")
            ?: throw IllegalArgumentException("Missing share data in deep link.")

        val clean = Uri.decode(rawEncoded).trim()
        if (clean.isBlank()) {
            throw IllegalArgumentException("Empty payload in deep link.")
        }

        return decodeAndDecompress(clean)
    }

    /**
     * Decodes payload from an input stream (supports both raw JSON and GZIP).
     */
    suspend fun decodeFromStream(inputStream: InputStream): SharePayload = withContext(ioDispatcher) {
        val buffer = ByteArrayOutputStream()
        val temp = ByteArray(4096)
        var totalBytes = 0L
        var read = inputStream.read(temp)
        while (read != -1) {
            totalBytes += read
            if (totalBytes > MAX_DECOMPRESSED_PAYLOAD_BYTES) {
                throw SecurityException("Shared file exceeds maximum size limit of $MAX_DECOMPRESSED_PAYLOAD_BYTES bytes.")
            }
            buffer.write(temp, 0, read)
            read = inputStream.read(temp)
        }

        val rawBytes = buffer.toByteArray()
        if (rawBytes.isEmpty()) {
            throw IllegalArgumentException("Empty shared data stream.")
        }

        // Check if GZIP
        val isGzip = rawBytes.size >= 2 &&
                (rawBytes[0].toInt() and 0xFF == 0x1F) &&
                (rawBytes[1].toInt() and 0xFF == 0x8B)

        val jsonString = if (isGzip) {
            val decompressed = decompressGzipBounded(rawBytes, MAX_DECOMPRESSED_PAYLOAD_BYTES)
            String(decompressed, Charsets.UTF_8)
        } else {
            String(rawBytes, Charsets.UTF_8)
        }

        val payload = json.decodeFromString(SharePayload.serializer(), jsonString)
        if (payload.version > SharePayload.CURRENT_VERSION) {
            throw IllegalArgumentException("Unsupported sharing version ${payload.version}. Maximum supported is ${SharePayload.CURRENT_VERSION}.")
        }

        payload
    }

    /**
     * Inspects current database and generates an ImportPreview with conflict analysis.
     */
    suspend fun previewImport(payload: SharePayload): ImportPreview = withContext(ioDispatcher) {
        val validationErrors = mutableListOf<String>()

        if (payload.contents.isEmpty()) {
            validationErrors.add("Shared payload contains no content items.")
        }

        val existingContents = contentDao.getAllEntities()
        val conflicts = mutableListOf<ImportConflict>()

        for (incoming in payload.contents) {
            if (incoming.title.isBlank()) {
                validationErrors.add("Item found with blank title.")
                continue
            }

            // Check 1: Matching UUID
            val sameId = existingContents.find { it.id == incoming.id }
            if (sameId != null) {
                conflicts.add(
                    ImportConflict(
                        incomingItem = incoming,
                        existingItemId = sameId.id,
                        existingItemTitle = sameId.title,
                        reason = ImportConflictReason.SAME_ID
                    )
                )
                continue
            }

            // Check 2: Matching Title & Type
            val sameTitleType = existingContents.find {
                it.title.trim().equals(incoming.title.trim(), ignoreCase = true) &&
                        it.contentType.equals(incoming.contentType, ignoreCase = true)
            }
            if (sameTitleType != null) {
                conflicts.add(
                    ImportConflict(
                        incomingItem = incoming,
                        existingItemId = sameTitleType.id,
                        existingItemTitle = sameTitleType.title,
                        reason = ImportConflictReason.SAME_TITLE_AND_TYPE
                    )
                )
            }
        }

        ImportPreview(
            isValid = validationErrors.isEmpty(),
            payload = payload,
            conflicts = conflicts,
            validationErrors = validationErrors
        )
    }

    /**
     * Executes the import of selected items with transactional safety.
     */
    suspend fun executeImport(
        preview: ImportPreview,
        selectedItemIds: Set<String>,
        conflictResolution: ImportConflictResolution
    ): ImportResult = withContext(ioDispatcher) {
        val payload = preview.payload
        if (!preview.isValid || payload == null) {
            return@withContext ImportResult.Error("Cannot import: invalid payload.")
        }

        if (selectedItemIds.isEmpty()) {
            return@withContext ImportResult.Success(importedCount = 0, skippedCount = 0)
        }

        try {
            var importedCount = 0
            var skippedCount = 0

            database.withTransaction {
                val existingContents = contentDao.getAllEntities()
                val existingCategories = categoryDao.getAll()
                val existingLists = customListDao.getAll()

                // 1. Remap categories: reuse local category if same name exists
                val categoryIdMap = mutableMapOf<String, String>()
                for (cat in payload.categories) {
                    val matchingLocalCat = existingCategories.find {
                        it.name.trim().equals(cat.name.trim(), ignoreCase = true)
                    }
                    if (matchingLocalCat != null) {
                        categoryIdMap[cat.id] = matchingLocalCat.id
                    } else {
                        // Check if ID collides with an existing category having a different name
                        val idCollides = existingCategories.any { it.id == cat.id }
                        val targetCatId = if (idCollides) UUID.randomUUID().toString() else cat.id
                        categoryDao.insert(CategoryEntity(id = targetCatId, name = cat.name.trim(), createdAt = cat.createdAt))
                        categoryIdMap[cat.id] = targetCatId
                    }
                }

                // 2. Remap custom lists (if sharing a list)
                val listIdMap = mutableMapOf<String, String>()
                for (list in payload.customLists) {
                    val matchingList = existingLists.find { it.name.trim().equals(list.name.trim(), ignoreCase = true) }
                    val targetListId = if (matchingList != null) {
                        // Create imported collection with suffix to keep distinct
                        val newId = UUID.randomUUID().toString()
                        val newName = "${list.name} (Imported)"
                        customListDao.insert(CustomListEntity(id = newId, name = newName, createdAt = list.createdAt, updatedAt = System.currentTimeMillis()))
                        newId
                    } else {
                        val idCollides = existingLists.any { it.id == list.id }
                        val finalId = if (idCollides) UUID.randomUUID().toString() else list.id
                        customListDao.insert(CustomListEntity(id = finalId, name = list.name.trim(), createdAt = list.createdAt, updatedAt = list.updatedAt))
                        finalId
                    }
                    listIdMap[list.id] = targetListId
                }

                // 3. Process Content Items
                val contentIdMap = mutableMapOf<String, String>()
                for (item in payload.contents) {
                    if (!selectedItemIds.contains(item.id)) {
                        continue
                    }

                    val conflict = preview.conflicts.find { it.incomingItem.id == item.id }

                    if (conflict != null) {
                        when (conflictResolution) {
                            ImportConflictResolution.SKIP_EXISTING -> {
                                skippedCount++
                                continue
                            }

                            ImportConflictResolution.OVERWRITE_EXISTING -> {
                                val targetId = conflict.existingItemId
                                contentDao.insertOrUpdate(
                                    ContentEntity(
                                        id = targetId,
                                        title = item.title,
                                        contentType = item.contentType,
                                        watchStatus = item.watchStatus,
                                        posterPath = null, // Shared payloads do not embed local images
                                        externalUrl = sanitizeUrl(item.externalUrl),
                                        description = item.description,
                                        createdAt = item.createdAt,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                                contentIdMap[item.id] = targetId
                                importedCount++
                            }

                            ImportConflictResolution.KEEP_BOTH_AS_COPY -> {
                                val newId = UUID.randomUUID().toString()
                                val copyTitle = if (conflict.reason == ImportConflictReason.SAME_TITLE_AND_TYPE || existingContents.any { it.title.trim().equals(item.title.trim(), ignoreCase = true) }) {
                                    "${item.title} (Copy)"
                                } else {
                                    item.title
                                }
                                contentDao.insertOrUpdate(
                                    ContentEntity(
                                        id = newId,
                                        title = copyTitle,
                                        contentType = item.contentType,
                                        watchStatus = item.watchStatus,
                                        posterPath = null,
                                        externalUrl = sanitizeUrl(item.externalUrl),
                                        description = item.description,
                                        createdAt = item.createdAt,
                                        updatedAt = item.updatedAt
                                    )
                                )
                                contentIdMap[item.id] = newId
                                importedCount++
                            }
                        }
                    } else {
                        // Clean insert: preserve UUID if not colliding
                        val idCollides = existingContents.any { it.id == item.id }
                        val targetId = if (idCollides) UUID.randomUUID().toString() else item.id

                        contentDao.insertOrUpdate(
                            ContentEntity(
                                id = targetId,
                                title = item.title,
                                contentType = item.contentType,
                                watchStatus = item.watchStatus,
                                posterPath = null,
                                externalUrl = sanitizeUrl(item.externalUrl),
                                description = item.description,
                                createdAt = item.createdAt,
                                updatedAt = item.updatedAt
                            )
                        )
                        contentIdMap[item.id] = targetId
                        importedCount++
                    }
                }

                // 4. Insert Content-Category CrossRefs
                val catCrossRefs = payload.contentCategories.mapNotNull { ref ->
                    val remappedContentId = contentIdMap[ref.contentId]
                    val remappedCatId = categoryIdMap[ref.categoryId]
                    if (remappedContentId != null && remappedCatId != null) {
                        ContentCategoryCrossRef(contentId = remappedContentId, categoryId = remappedCatId)
                    } else null
                }
                if (catCrossRefs.isNotEmpty()) {
                    contentDao.insertContentCategories(catCrossRefs)
                }

                // 5. Insert List-Item CrossRefs
                val listItemCrossRefs = payload.listItems.mapNotNull { ref ->
                    val remappedListId = listIdMap[ref.listId]
                    val remappedContentId = contentIdMap[ref.contentId]
                    if (remappedListId != null && remappedContentId != null) {
                        ListItemCrossRef(listId = remappedListId, contentId = remappedContentId, addedAt = System.currentTimeMillis())
                    } else null
                }
                if (listItemCrossRefs.isNotEmpty()) {
                    contentDao.insertListItems(listItemCrossRefs)
                }
            }

            ImportResult.Success(importedCount = importedCount, skippedCount = skippedCount)
        } catch (e: Exception) {
            ImportResult.Error("Import failed: ${e.localizedMessage ?: e.message}", e)
        }
    }

    private fun decompressGzipBounded(compressedBytes: ByteArray, maxBytes: Long): ByteArray {
        val gzipIn = GZIPInputStream(ByteArrayInputStream(compressedBytes))
        val byteOut = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        var totalBytes = 0L
        var read = gzipIn.read(buffer)
        while (read != -1) {
            totalBytes += read
            if (totalBytes > maxBytes) {
                throw SecurityException("Decompressed payload exceeds maximum size limit of $maxBytes bytes.")
            }
            byteOut.write(buffer, 0, read)
            read = gzipIn.read(buffer)
        }
        return byteOut.toByteArray()
    }

    private fun sanitizeUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            null
        }
    }

    private fun cleanupSharedDir(dir: File) {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < oneHourAgo) {
                file.delete()
            }
        }
    }
}
