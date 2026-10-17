package com.vp.whattowatch.data

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.backup.BackupLibraryPayload
import com.vp.whattowatch.data.backup.BackupManifest
import com.vp.whattowatch.data.backup.BackupResult
import com.vp.whattowatch.data.backup.BackupService
import com.vp.whattowatch.data.backup.ConflictResolutionStrategy
import com.vp.whattowatch.data.backup.RestoreMode
import com.vp.whattowatch.data.backup.RestoreResult
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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class BackupServiceTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var contentDao: ContentDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var customListDao: CustomListDao
    private lateinit var imageStorageManager: ImageStorageManager
    private lateinit var backupService: BackupService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contentDao = db.contentDao()
        categoryDao = db.categoryDao()
        customListDao = db.customListDao()
        imageStorageManager = ImageStorageManager(context)
        imageStorageManager.postersDir.deleteRecursively()
        imageStorageManager.postersDir.mkdirs()

        backupService = BackupService(
            context = context,
            database = db,
            contentDao = contentDao,
            categoryDao = categoryDao,
            customListDao = customListDao,
            imageStorageManager = imageStorageManager
        )
    }

    @After
    fun tearDown() {
        db.close()
        imageStorageManager.postersDir.deleteRecursively()
    }

    @Test
    fun testExportAndPreviewRoundTrip() = runBlocking {
        // Step 1: Populate DB
        val contentId = UUID.randomUUID().toString()
        val catId = UUID.randomUUID().toString()
        val listId = UUID.randomUUID().toString()

        categoryDao.insert(CategoryEntity(id = catId, name = "Sci-Fi", createdAt = 1000L))
        customListDao.insert(CustomListEntity(id = listId, name = "Favorites", createdAt = 1000L, updatedAt = 1000L))

        // Create a real poster
        val bitmap = Bitmap.createBitmap(120, 180, Bitmap.Config.ARGB_8888)
        val byteOut = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteOut)
        val posterFilename = imageStorageManager.saveImageFromStream(ByteArrayInputStream(byteOut.toByteArray()))
        assertNotNull(posterFilename)

        contentDao.insertOrUpdate(
            ContentEntity(
                id = contentId,
                title = "Interstellar",
                contentType = "MOVIE",
                watchStatus = "WATCHED",
                externalUrl = "https://imdb.com/title/tt0816692",
                description = "Christopher Nolan masterpiece",
                posterPath = posterFilename,
                createdAt = 1000L,
                updatedAt = 2000L
            )
        )
        contentDao.insertContentCategories(listOf(ContentCategoryCrossRef(contentId, catId)))
        contentDao.insertListItems(listOf(ListItemCrossRef(listId, contentId, 1000L)))

        // Step 2: Export backup
        val exportStream = ByteArrayOutputStream()
        val exportResult = backupService.exportBackup(exportStream)
        assertTrue("Export must succeed", exportResult is BackupResult.Success)
        val manifest = (exportResult as BackupResult.Success).manifest
        assertEquals(1, manifest.itemCount)
        assertEquals(1, manifest.categoryCount)
        assertEquals(1, manifest.listCount)
        assertEquals(1, manifest.imageCount)

        // Step 3: Verify raw ZIP structure
        val zipBytes = exportStream.toByteArray()
        assertTrue(zipBytes.isNotEmpty())

        val entryNames = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        assertTrue("ZIP must contain manifest.json", entryNames.contains(BackupService.FILE_MANIFEST))
        assertTrue("ZIP must contain library.json", entryNames.contains(BackupService.FILE_LIBRARY))
        assertTrue("ZIP must contain poster entry", entryNames.any { it.startsWith("posters/") })

        // Step 4: Preview restore
        val preview = backupService.previewRestore(ByteArrayInputStream(zipBytes))
        assertTrue("Preview should be valid", preview.isValid)
        assertEquals(1, preview.manifest?.itemCount)
        assertEquals(1, preview.imageFileCount)
        assertEquals(1, preview.conflicts.size) // Interstellar already exists in DB

        // Clean up preview staging
        backupService.cleanUpDir(preview.stagingDir)
    }

    @Test
    fun testExecuteRestoreReplaceMode() = runBlocking {
        // Create backup data
        val oldId = UUID.randomUUID().toString()
        val backupContentId = UUID.randomUUID().toString()

        categoryDao.insert(CategoryEntity("cat-archive", "Documentary", 1000L))
        contentDao.insertOrUpdate(
            ContentEntity(
                id = backupContentId,
                title = "Planet Earth",
                contentType = "SERIES",
                watchStatus = "WATCHED",
                createdAt = 1000L,
                updatedAt = 2000L
            )
        )

        val zipOut = ByteArrayOutputStream()
        backupService.exportBackup(zipOut)
        val zipBytes = zipOut.toByteArray()

        // Mutate current DB state: wipe and insert something else
        contentDao.deleteAllContents()
        categoryDao.deleteAll()
        contentDao.insertOrUpdate(
            ContentEntity(
                id = oldId,
                title = "Temporary Movie",
                contentType = "MOVIE",
                watchStatus = "NOT_WATCHED",
                createdAt = 5000L,
                updatedAt = 5000L
            )
        )

        // Preview & Execute Replace
        val preview = backupService.previewRestore(ByteArrayInputStream(zipBytes))
        val restoreResult = backupService.executeRestore(preview, RestoreMode.REPLACE)
        assertTrue(restoreResult is RestoreResult.Success)
        assertEquals(1, (restoreResult as RestoreResult.Success).restoredItemsCount)

        // Verify "Temporary Movie" was wiped and "Planet Earth" is restored
        assertNull(contentDao.getWithDetailsById(oldId))
        val restoredItem = contentDao.getWithDetailsById(backupContentId)?.content
        assertNotNull(restoredItem)
        assertEquals("Planet Earth", restoredItem?.title)
    }

    @Test
    fun testExecuteRestoreMergeWithConflictStrategies() = runBlocking {
        val existingId = UUID.randomUUID().toString()
        contentDao.insertOrUpdate(
            ContentEntity(
                id = existingId,
                title = "The Matrix",
                contentType = "MOVIE",
                watchStatus = "NOT_WATCHED",
                description = "Original version in DB",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        // Create archive containing "The Matrix" with updated description and a different ID
        val archiveItem = ContentEntity(
            id = UUID.randomUUID().toString(),
            title = "The Matrix",
            contentType = "MOVIE",
            watchStatus = "WATCHED",
            description = "Restored version from backup",
            createdAt = 2000L,
            updatedAt = 2000L
        )

        val manifest = BackupManifest(
            version = 1,
            appName = "What to Watch",
            appVersion = "1.0",
            createdAt = 2000L,
            itemCount = 1,
            listCount = 0,
            categoryCount = 0,
            imageCount = 0
        )
        val payload = BackupLibraryPayload(
            contents = listOf(
                com.vp.whattowatch.data.backup.BackupContentItem(
                    id = archiveItem.id,
                    title = archiveItem.title,
                    contentType = archiveItem.contentType,
                    watchStatus = archiveItem.watchStatus,
                    description = archiveItem.description,
                    createdAt = archiveItem.createdAt,
                    updatedAt = archiveItem.updatedAt
                )
            ),
            categories = emptyList(),
            customLists = emptyList(),
            contentCategories = emptyList(),
            listItems = emptyList()
        )

        val json = Json { prettyPrint = true }
        val zipStream = ByteArrayOutputStream()
        ZipOutputStream(zipStream).use { zOut ->
            zOut.putNextEntry(ZipEntry(BackupService.FILE_MANIFEST))
            zOut.write(json.encodeToString(BackupManifest.serializer(), manifest).toByteArray())
            zOut.closeEntry()

            zOut.putNextEntry(ZipEntry(BackupService.FILE_LIBRARY))
            zOut.write(json.encodeToString(BackupLibraryPayload.serializer(), payload).toByteArray())
            zOut.closeEntry()
        }

        val zipBytes = zipStream.toByteArray()

        // 1. Strategy: SKIP_IMPORT
        val previewSkip = backupService.previewRestore(ByteArrayInputStream(zipBytes))
        val resultSkip = backupService.executeRestore(
            previewSkip,
            RestoreMode.MERGE,
            ConflictResolutionStrategy.SKIP_EXISTING
        )
        assertTrue(resultSkip is RestoreResult.Success)
        val itemsAfterSkip = contentDao.getAllEntities()
        assertEquals(1, itemsAfterSkip.size)
        assertEquals("Original version in DB", itemsAfterSkip.first().description)

        // 2. Strategy: OVERWRITE_EXISTING
        val previewOverwrite = backupService.previewRestore(ByteArrayInputStream(zipBytes))
        val resultOverwrite = backupService.executeRestore(
            previewOverwrite,
            RestoreMode.MERGE,
            ConflictResolutionStrategy.OVERWRITE_EXISTING
        )
        assertTrue(resultOverwrite is RestoreResult.Success)
        val itemsAfterOverwrite = contentDao.getAllEntities()
        assertEquals(1, itemsAfterOverwrite.size)
        assertEquals("Restored version from backup", itemsAfterOverwrite.first().description)

        // 3. Strategy: KEEP_BOTH_IMPORT_AS_COPY
        val previewKeepBoth = backupService.previewRestore(ByteArrayInputStream(zipBytes))
        val resultKeepBoth = backupService.executeRestore(
            previewKeepBoth,
            RestoreMode.MERGE,
            ConflictResolutionStrategy.KEEP_BOTH_IMPORT_AS_COPY
        )
        assertTrue(resultKeepBoth is RestoreResult.Success)
        val itemsAfterKeepBoth = contentDao.getAllEntities()
        assertEquals(2, itemsAfterKeepBoth.size)
        assertTrue(itemsAfterKeepBoth.any { it.title.contains("Copy") })
    }

    @Test
    fun testZipSlipPathTraversalRejection() = runBlocking {
        // Construct a ZIP with a path traversal entry targeting outside the staging directory
        val zipOut = ByteArrayOutputStream()
        ZipOutputStream(zipOut).use { zOut ->
            zOut.putNextEntry(ZipEntry("posters/../../evil_payload.txt"))
            zOut.write("Malicious data".toByteArray())
            zOut.closeEntry()
        }

        val preview = backupService.previewRestore(ByteArrayInputStream(zipOut.toByteArray()))
        assertFalse("Zip-Slip archive must be rejected as invalid", preview.isValid)
        assertTrue("Validation errors must report the failure", preview.validationErrors.isNotEmpty())

        // Ensure no evil file escaped
        val escapedFile = File(context.cacheDir, "evil_payload.txt")
        assertFalse("Escaped file must not exist", escapedFile.exists())
    }
}
