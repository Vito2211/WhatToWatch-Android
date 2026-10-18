package com.vp.whattowatch.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.local.AppDatabase
import com.vp.whattowatch.data.local.dao.CategoryDao
import com.vp.whattowatch.data.local.dao.ContentDao
import com.vp.whattowatch.data.local.dao.CustomListDao
import com.vp.whattowatch.data.local.entity.CategoryEntity
import com.vp.whattowatch.data.local.entity.ContentCategoryCrossRef
import com.vp.whattowatch.data.local.entity.ContentEntity
import com.vp.whattowatch.data.local.entity.CustomListEntity
import com.vp.whattowatch.data.local.entity.ListItemCrossRef
import com.vp.whattowatch.data.sharing.ImportConflictReason
import com.vp.whattowatch.data.sharing.ImportConflictResolution
import com.vp.whattowatch.data.sharing.ImportResult
import com.vp.whattowatch.data.sharing.ShareCategory
import com.vp.whattowatch.data.sharing.ShareContentCategoryRef
import com.vp.whattowatch.data.sharing.ShareContentItem
import com.vp.whattowatch.data.sharing.ShareCustomList
import com.vp.whattowatch.data.sharing.ShareListItemRef
import com.vp.whattowatch.data.sharing.SharePayload
import com.vp.whattowatch.data.sharing.ShareResult
import com.vp.whattowatch.data.sharing.ShareType
import com.vp.whattowatch.data.sharing.SharingService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class SharingServiceTest {

    @get:Rule
    val globalTimeout: Timeout = Timeout.seconds(30)

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var contentDao: ContentDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var customListDao: CustomListDao
    private lateinit var sharingService: SharingService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        contentDao = db.contentDao()
        categoryDao = db.categoryDao()
        customListDao = db.customListDao()

        sharingService = SharingService(
            context = context,
            database = db,
            contentDao = contentDao,
            categoryDao = categoryDao,
            customListDao = customListDao,
            fileUriProvider = { Uri.fromFile(it) }
        )
    }

    @After
    fun tearDown() {
        db.close()
        val sharedDir = File(context.cacheDir, SharingService.DIR_SHARED)
        sharedDir.deleteRecursively()
    }

    @Test
    fun testCreateSingleItemSharePayload() = runBlocking {
        val catId = UUID.randomUUID().toString()
        val contentId = UUID.randomUUID().toString()

        categoryDao.insert(CategoryEntity(id = catId, name = "Sci-Fi", createdAt = 1000L))
        contentDao.insertOrUpdate(
            ContentEntity(
                id = contentId,
                title = "Inception",
                contentType = "MOVIE",
                watchStatus = "WATCHED",
                externalUrl = "https://imdb.com/title/tt1375666",
                description = "Dream within a dream",
                posterPath = null,
                createdAt = 1000L,
                updatedAt = 2000L
            )
        )
        contentDao.insertContentCategories(listOf(ContentCategoryCrossRef(contentId, catId)))

        val payload = sharingService.createSingleItemSharePayload(contentId)
        assertNotNull(payload)
        assertEquals(SharePayload.CURRENT_VERSION, payload!!.version)
        assertEquals(ShareType.SINGLE_ITEM, payload.type)
        assertEquals("Inception", payload.title)
        assertEquals(1, payload.contents.size)
        assertEquals("Inception", payload.contents[0].title)
        assertEquals(1, payload.categories.size)
        assertEquals("Sci-Fi", payload.categories[0].name)
        assertEquals(1, payload.contentCategories.size)
        assertTrue(payload.customLists.isEmpty())
        assertTrue(payload.listItems.isEmpty())
    }

    @Test
    fun testCreateCustomListSharePayload() = runBlocking {
        val listId = UUID.randomUUID().toString()
        val catId = UUID.randomUUID().toString()
        val movie1Id = UUID.randomUUID().toString()
        val movie2Id = UUID.randomUUID().toString()

        categoryDao.insert(CategoryEntity(id = catId, name = "Action", createdAt = 1000L))
        customListDao.insert(CustomListEntity(id = listId, name = "Weekend Marathon", createdAt = 1000L, updatedAt = 1000L))

        contentDao.insertOrUpdate(
            ContentEntity(id = movie1Id, title = "Die Hard", contentType = "MOVIE", watchStatus = "WATCHED", externalUrl = null, description = null, posterPath = null, createdAt = 1000L, updatedAt = 1000L)
        )
        contentDao.insertOrUpdate(
            ContentEntity(id = movie2Id, title = "Mad Max", contentType = "MOVIE", watchStatus = "NOT_WATCHED", externalUrl = null, description = null, posterPath = null, createdAt = 1100L, updatedAt = 1100L)
        )

        contentDao.insertContentCategories(listOf(
            ContentCategoryCrossRef(movie1Id, catId),
            ContentCategoryCrossRef(movie2Id, catId)
        ))
        contentDao.insertListItems(listOf(
            ListItemCrossRef(listId, movie1Id, 1000L),
            ListItemCrossRef(listId, movie2Id, 1100L)
        ))

        val payload = sharingService.createCustomListSharePayload(listId)
        assertNotNull(payload)
        assertEquals(ShareType.CUSTOM_LIST, payload!!.type)
        assertEquals("Weekend Marathon", payload.title)
        assertEquals(1, payload.customLists.size)
        assertEquals("Weekend Marathon", payload.customLists[0].name)
        assertEquals(2, payload.contents.size)
        assertEquals(1, payload.categories.size)
        assertEquals(2, payload.contentCategories.size)
        assertEquals(2, payload.listItems.size)
    }

    @Test
    fun testCompressAndDecodeRoundTrip() {
        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "The Matrix",
            contents = listOf(
                ShareContentItem(
                    id = "matrix-1",
                    title = "The Matrix",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    externalUrl = "https://example.com/matrix",
                    description = "Take the red pill",
                    createdAt = 1000L,
                    updatedAt = 2000L
                )
            ),
            categories = listOf(ShareCategory(id = "cat-sci-fi", name = "Sci-Fi", createdAt = 1000L)),
            contentCategories = listOf(ShareContentCategoryRef(contentId = "matrix-1", categoryId = "cat-sci-fi"))
        )

        val encoded = sharingService.compressAndEncode(payload)
        assertTrue("Encoded string must be non-empty", encoded.isNotEmpty())

        val decoded = sharingService.decodeAndDecompress(encoded)
        assertEquals(payload.version, decoded.version)
        assertEquals(payload.type, decoded.type)
        assertEquals(payload.title, decoded.title)
        assertEquals(payload.contents.size, decoded.contents.size)
        assertEquals(payload.contents[0].id, decoded.contents[0].id)
        assertEquals(payload.contents[0].title, decoded.contents[0].title)
        assertEquals(payload.categories[0].name, decoded.categories[0].name)
    }

    @Test
    fun testGenerateShareResult_smallPayloadProducesDeepLink() = runBlocking {
        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Short Movie",
            contents = listOf(
                ShareContentItem(
                    id = UUID.randomUUID().toString(),
                    title = "Short Movie",
                    contentType = "MOVIE",
                    watchStatus = "NOT_WATCHED",
                    externalUrl = null,
                    description = "Brief synopsis",
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            )
        )

        val result = sharingService.generateShareResult(payload)
        assertTrue("Expected DeepLink for small payload", result is ShareResult.DeepLink)
        val deepLink = result as ShareResult.DeepLink
        assertTrue(deepLink.url.startsWith("https://vito2211.github.io/WhatoToWatch/#share/"))
        assertTrue(deepLink.url.length <= SharingService.MAX_DEEP_LINK_URL_LENGTH)

        // Verify decodeFromDeepLink
        val decoded = sharingService.decodeFromDeepLink(Uri.parse(deepLink.url))
        assertNotNull(decoded)
        assertEquals("Short Movie", decoded!!.title)
    }

    @Test
    fun testGenerateShareResult_oversizedPayloadFallsBackToFile() = runBlocking {
        // Create an oversized collection that exceeds MAX_DEEP_LINK_URL_LENGTH (2000 chars) after GZIP compression
        val items = (1..60).map { i ->
            ShareContentItem(
                id = UUID.randomUUID().toString(),
                title = "Oversized Movie Title $i with description and metadata ${UUID.randomUUID()}",
                contentType = "MOVIE",
                watchStatus = "WATCHED",
                externalUrl = "https://example.com/movie/$i",
                description = "Extensive plot synopsis for movie $i with diverse keywords ${UUID.randomUUID()}",
                createdAt = 1000L + i,
                updatedAt = 1000L + i
            )
        }
        val payload = SharePayload(
            version = 1,
            type = ShareType.CUSTOM_LIST,
            title = "Huge Payload",
            contents = items
        )

        val result = sharingService.generateShareResult(payload)
        assertTrue("Expected FileShare but got: $result", result is ShareResult.FileShare)
        val fileShare = result as ShareResult.FileShare
        assertTrue(fileShare.fileUriString.isNotEmpty())
        assertEquals(SharingService.MIME_TYPE_FILE, fileShare.mimeType)

        // Read the created file back and verify decodeFromStream
        val sharedDir = File(context.cacheDir, SharingService.DIR_SHARED)
        val files = sharedDir.listFiles()
        assertNotNull(files)
        assertTrue("Shared file should exist in cache/shared", files!!.isNotEmpty())

        val sharedFile = files[0]
        assertTrue(sharedFile.name.endsWith(SharingService.FILE_EXTENSION))
        val decoded = sharedFile.inputStream().use { stream ->
            sharingService.decodeFromStream(stream)
        }
        assertEquals("Huge Payload", decoded.title)
        assertEquals(60, decoded.contents.size)
    }

    @Test(expected = SecurityException::class)
    fun testDecodeAndDecompress_rejectsExcessiveStringLength() {
        val excessiveString = "A".repeat(SharingService.MAX_COMPRESSED_PAYLOAD_STRING_LENGTH + 1)
        sharingService.decodeAndDecompress(excessiveString)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDecodeAndDecompress_rejectsUnsupportedVersion() {
        val futurePayload = SharePayload(
            version = 99,
            type = ShareType.SINGLE_ITEM,
            title = "Future Movie",
            contents = emptyList()
        )
        val encoded = sharingService.compressAndEncode(futurePayload)
        sharingService.decodeAndDecompress(encoded)
    }

    @Test
    fun testPreviewImport_detectsDuplicateUUID() = runBlocking {
        val existingId = UUID.randomUUID().toString()
        contentDao.insertOrUpdate(
            ContentEntity(
                id = existingId,
                title = "Avatar",
                contentType = "MOVIE",
                watchStatus = "NOT_WATCHED",
                externalUrl = null,
                description = "Original",
                posterPath = null,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Avatar 2",
            contents = listOf(
                ShareContentItem(
                    id = existingId,
                    title = "Avatar 2",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    externalUrl = null,
                    description = "Updated",
                    createdAt = 2000L,
                    updatedAt = 2000L
                )
            )
        )

        val preview = sharingService.previewImport(payload)
        assertEquals(1, preview.contentsCount)
        assertEquals(1, preview.conflicts.size)
        assertEquals(existingId, preview.conflicts[0].incomingItem.id)
        assertEquals(ImportConflictReason.SAME_ID, preview.conflicts[0].reason)
        assertEquals("Avatar", preview.conflicts[0].existingItemTitle)
    }

    private val com.vp.whattowatch.data.sharing.ImportPreview.contentsCount: Int
        get() = payload?.contents?.size ?: 0

    @Test
    fun testPreviewImport_detectsDuplicateNormalizedTitleAndType() = runBlocking {
        val localId = UUID.randomUUID().toString()
        val incomingId = UUID.randomUUID().toString()

        contentDao.insertOrUpdate(
            ContentEntity(
                id = localId,
                title = "Gladiator",
                contentType = "MOVIE",
                watchStatus = "NOT_WATCHED",
                externalUrl = null,
                description = null,
                posterPath = null,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "  gladiator  ",
            contents = listOf(
                ShareContentItem(
                    id = incomingId,
                    title = "  gladiator  ",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    externalUrl = null,
                    description = "Are you not entertained?",
                    createdAt = 2000L,
                    updatedAt = 2000L
                )
            )
        )

        val preview = sharingService.previewImport(payload)
        assertEquals(1, preview.contentsCount)
        assertEquals(1, preview.conflicts.size)
        assertEquals(incomingId, preview.conflicts[0].incomingItem.id)
        assertEquals(ImportConflictReason.SAME_TITLE_AND_TYPE, preview.conflicts[0].reason)
        assertEquals("Gladiator", preview.conflicts[0].existingItemTitle)
    }

    @Test
    fun testExecuteImport_skipExisting() = runBlocking {
        val existingId = UUID.randomUUID().toString()
        val newId = UUID.randomUUID().toString()

        contentDao.insertOrUpdate(
            ContentEntity(
                id = existingId,
                title = "Existing Movie",
                contentType = "MOVIE",
                watchStatus = "NOT_WATCHED",
                externalUrl = null,
                description = "Original Note",
                posterPath = null,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val payload = SharePayload(
            version = 1,
            type = ShareType.CUSTOM_LIST,
            title = "Test",
            contents = listOf(
                ShareContentItem(
                    id = existingId,
                    title = "Existing Movie",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    externalUrl = null,
                    description = "Modified Note",
                    createdAt = 2000L,
                    updatedAt = 2000L
                ),
                ShareContentItem(
                    id = newId,
                    title = "Brand New Movie",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    externalUrl = null,
                    description = "Fresh",
                    createdAt = 2000L,
                    updatedAt = 2000L
                )
            )
        )

        val preview = sharingService.previewImport(payload)
        val result = sharingService.executeImport(
            preview = preview,
            selectedItemIds = setOf(existingId, newId),
            conflictResolution = ImportConflictResolution.SKIP_EXISTING
        )

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.importedCount)
        assertEquals(1, success.skippedCount)

        // Existing movie was not modified
        val existingItem = contentDao.getWithDetailsById(existingId)?.content
        assertNotNull(existingItem)
        assertEquals("Original Note", existingItem!!.description)
        assertEquals("NOT_WATCHED", existingItem.watchStatus)

        // New movie was imported
        val newItem = contentDao.getWithDetailsById(newId)?.content
        assertNotNull(newItem)
        assertEquals("Brand New Movie", newItem!!.title)
    }

    @Test
    fun testExecuteImport_keepBothAsCopy() = runBlocking {
        val existingId = UUID.randomUUID().toString()
        contentDao.insertOrUpdate(
            ContentEntity(
                id = existingId,
                title = "Dune",
                contentType = "MOVIE",
                watchStatus = "NOT_WATCHED",
                externalUrl = null,
                description = "Part One",
                posterPath = null,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Dune",
            contents = listOf(
                ShareContentItem(
                    id = existingId,
                    title = "Dune",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    externalUrl = null,
                    description = "Arrakis",
                    createdAt = 2000L,
                    updatedAt = 2000L
                )
            )
        )

        val preview = sharingService.previewImport(payload)
        val result = sharingService.executeImport(
            preview = preview,
            selectedItemIds = setOf(existingId),
            conflictResolution = ImportConflictResolution.KEEP_BOTH_AS_COPY
        )

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.importedCount)
        assertEquals(0, success.skippedCount)

        // Original movie preserved
        val original = contentDao.getWithDetailsById(existingId)?.content
        assertNotNull(original)
        assertEquals("Dune", original!!.title)
        assertEquals("Part One", original.description)

        // Copy created
        val allMovies = contentDao.getAllEntities()
        assertEquals(2, allMovies.size)
        val copy = allMovies.first { it.id != existingId }
        assertEquals("Dune (Copy)", copy.title)
        assertEquals("Arrakis", copy.description)
        assertEquals("WATCHED", copy.watchStatus)
    }

    @Test
    fun testExecuteImport_overwriteExisting() = runBlocking {
        val existingId = UUID.randomUUID().toString()
        contentDao.insertOrUpdate(
            ContentEntity(
                id = existingId,
                title = "Alien",
                contentType = "MOVIE",
                watchStatus = "NOT_WATCHED",
                externalUrl = null,
                description = "Old Note",
                posterPath = null,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Alien",
            contents = listOf(
                ShareContentItem(
                    id = existingId,
                    title = "Alien: Director's Cut",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    externalUrl = "https://imdb.com/title/tt0078748",
                    description = "In space no one can hear you scream",
                    createdAt = 1000L,
                    updatedAt = 2500L
                )
            )
        )

        val preview = sharingService.previewImport(payload)
        val result = sharingService.executeImport(
            preview = preview,
            selectedItemIds = setOf(existingId),
            conflictResolution = ImportConflictResolution.OVERWRITE_EXISTING
        )

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.importedCount)
        assertEquals(0, success.skippedCount)

        val updated = contentDao.getWithDetailsById(existingId)?.content
        assertNotNull(updated)
        assertEquals("Alien: Director's Cut", updated!!.title)
        assertEquals("In space no one can hear you scream", updated.description)
        assertEquals("WATCHED", updated.watchStatus)
        assertEquals("https://imdb.com/title/tt0078748", updated.externalUrl)
    }

    @Test
    fun testExecuteImport_selectiveItems() = runBlocking {
        val item1Id = UUID.randomUUID().toString()
        val item2Id = UUID.randomUUID().toString()

        val payload = SharePayload(
            version = 1,
            type = ShareType.CUSTOM_LIST,
            title = "Sci-Fi Pack",
            contents = listOf(
                ShareContentItem(id = item1Id, title = "Item 1", contentType = "MOVIE", watchStatus = "NOT_WATCHED", externalUrl = null, description = null, createdAt = 1000L, updatedAt = 1000L),
                ShareContentItem(id = item2Id, title = "Item 2", contentType = "MOVIE", watchStatus = "NOT_WATCHED", externalUrl = null, description = null, createdAt = 1000L, updatedAt = 1000L)
            )
        )

        val preview = sharingService.previewImport(payload)
        val result = sharingService.executeImport(
            preview = preview,
            selectedItemIds = setOf(item1Id),
            conflictResolution = ImportConflictResolution.SKIP_EXISTING
        )

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.importedCount)
        assertNotNull(contentDao.getWithDetailsById(item1Id))
        assertNull(contentDao.getWithDetailsById(item2Id))
    }

    @Test
    fun testExecuteImport_customListWithRelations() = runBlocking {
        val listId = UUID.randomUUID().toString()
        val catId = UUID.randomUUID().toString()
        val itemId = UUID.randomUUID().toString()

        val payload = SharePayload(
            version = 1,
            type = ShareType.CUSTOM_LIST,
            title = "Classics",
            contents = listOf(
                ShareContentItem(id = itemId, title = "Casablanca", contentType = "MOVIE", watchStatus = "WATCHED", externalUrl = null, description = null, createdAt = 1000L, updatedAt = 1000L)
            ),
            categories = listOf(
                ShareCategory(id = catId, name = "Drama", createdAt = 1000L)
            ),
            customLists = listOf(ShareCustomList(id = listId, name = "Classics", createdAt = 1000L, updatedAt = 1000L)),
            contentCategories = listOf(ShareContentCategoryRef(contentId = itemId, categoryId = catId)),
            listItems = listOf(ShareListItemRef(listId = listId, contentId = itemId))
        )

        val preview = sharingService.previewImport(payload)
        val result = sharingService.executeImport(
            preview = preview,
            selectedItemIds = setOf(itemId),
            conflictResolution = ImportConflictResolution.SKIP_EXISTING
        )

        assertTrue(result is ImportResult.Success)
        val success = result as ImportResult.Success
        assertEquals(1, success.importedCount)

        // Verify list was created
        val createdList = customListDao.getById(listId)
        assertNotNull(createdList)
        assertEquals("Classics", createdList!!.name)

        // Verify category was created
        val category = categoryDao.getByName("Drama")
        assertNotNull(category)

        // Verify item was created and linked
        val details = contentDao.getWithDetailsById(itemId)
        assertNotNull(details)
        assertEquals(1, details!!.categories.size)
        assertEquals("Drama", details.categories[0].name)

        val listItems = customListDao.getContentsForList(listId)
        assertEquals(1, listItems.size)
        assertEquals(itemId, listItems[0].content.id)
    }

    @Test
    fun testDecodeAndPreviewDoesNotModifyDatabase() = runBlocking {
        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Ghost in the Shell",
            contents = listOf(
                ShareContentItem(id = UUID.randomUUID().toString(), title = "Ghost in the Shell", contentType = "MOVIE", watchStatus = "WATCHED", externalUrl = null, description = null, createdAt = 1000L, updatedAt = 1000L)
            )
        )

        val encoded = sharingService.compressAndEncode(payload)
        val decoded = sharingService.decodeFromDeepLink(Uri.parse("whattowatch://share/$encoded"))
        assertNotNull(decoded)

        val preview = sharingService.previewImport(decoded!!)
        assertNotNull(preview)

        // Check that database remains completely empty before executeImport is called
        assertEquals(0, contentDao.getAllEntities().size)
    }

    @Test
    fun testBuildCustomSchemeUrl_generatesValidWhattowatchLink() {
        val encoded = "test_payload_123"
        val customUrl = sharingService.buildCustomSchemeUrl(encoded)
        assertEquals("whattowatch://share/test_payload_123", customUrl)
    }

    @Test
    fun testDecodeFromDeepLink_supportsCustomSchemeBackwardCompatibility() {
        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Legacy Test",
            contents = listOf(
                ShareContentItem(
                    id = UUID.randomUUID().toString(),
                    title = "Legacy Movie",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            )
        )
        val encoded = sharingService.compressAndEncode(payload)

        // Path format: whattowatch://share/<encoded>
        val decoded1 = sharingService.decodeFromDeepLink(Uri.parse("whattowatch://share/$encoded"))
        assertEquals("Legacy Test", decoded1.title)

        // Query format: whattowatch://share?data=<encoded>
        val decoded2 = sharingService.decodeFromDeepLink(Uri.parse("whattowatch://share?data=$encoded"))
        assertEquals("Legacy Test", decoded2.title)

        // Query format: whattowatch://share?share=<encoded>
        val decoded3 = sharingService.decodeFromDeepLink(Uri.parse("whattowatch://share?share=$encoded"))
        assertEquals("Legacy Test", decoded3.title)
    }

    @Test
    fun testDecodeFromDeepLink_supportsHttpsHashAndQueryVariations() {
        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Web Link Test",
            contents = listOf(
                ShareContentItem(
                    id = UUID.randomUUID().toString(),
                    title = "Web Movie",
                    contentType = "MOVIE",
                    watchStatus = "NOT_WATCHED",
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            )
        )
        val encoded = sharingService.compressAndEncode(payload)

        // Standard hash format: https://vito2211.github.io/WhatoToWatch/#share/<encoded>
        val decodedHash = sharingService.decodeFromDeepLink(Uri.parse("https://vito2211.github.io/WhatoToWatch/#share/$encoded"))
        assertEquals("Web Link Test", decodedHash.title)

        // Slash-share hash format: #/share/<encoded>
        val decodedSlashHash = sharingService.decodeFromDeepLink(Uri.parse("https://vito2211.github.io/WhatoToWatch/#/share/$encoded"))
        assertEquals("Web Link Test", decodedSlashHash.title)

        // Hash data format: #data=<encoded>
        val decodedDataHash = sharingService.decodeFromDeepLink(Uri.parse("https://vito2211.github.io/WhatoToWatch/#data=$encoded"))
        assertEquals("Web Link Test", decodedDataHash.title)

        // Direct hash format: #<encoded>
        val decodedDirectHash = sharingService.decodeFromDeepLink(Uri.parse("https://vito2211.github.io/WhatoToWatch/#$encoded"))
        assertEquals("Web Link Test", decodedDirectHash.title)

        // Query parameter format: ?data=<encoded>
        val decodedQuery = sharingService.decodeFromDeepLink(Uri.parse("https://vito2211.github.io/WhatoToWatch/?data=$encoded"))
        assertEquals("Web Link Test", decodedQuery.title)

        // Query parameter format: ?share=<encoded>
        val decodedShareQuery = sharingService.decodeFromDeepLink(Uri.parse("https://vito2211.github.io/WhatoToWatch/?share=$encoded"))
        assertEquals("Web Link Test", decodedShareQuery.title)

        // Path format: /WhatoToWatch/share/<encoded>
        val decodedPath = sharingService.decodeFromDeepLink(Uri.parse("https://vito2211.github.io/WhatoToWatch/share/$encoded"))
        assertEquals("Web Link Test", decodedPath.title)
    }

    @Test
    fun testDecodeFromDeepLink_rejectsInvalidSchemeOrHost() {
        // Invalid scheme: ftp://
        var errorThrown = false
        try {
            sharingService.decodeFromDeepLink(Uri.parse("ftp://vito2211.github.io/WhatoToWatch/#share/abc"))
        } catch (e: IllegalArgumentException) {
            errorThrown = true
            assertTrue(e.message!!.contains("Invalid deep link scheme"))
        }
        assertTrue("Should reject non-http/https/whattowatch scheme", errorThrown)

        // Invalid host for whattowatch scheme: whattowatch://settings
        errorThrown = false
        try {
            sharingService.decodeFromDeepLink(Uri.parse("whattowatch://settings/abc"))
        } catch (e: IllegalArgumentException) {
            errorThrown = true
            assertTrue(e.message!!.contains("Invalid deep link scheme or host"))
        }
        assertTrue("Should reject non-share host for whattowatch scheme", errorThrown)

        // Missing payload: https://vito2211.github.io/WhatoToWatch/
        errorThrown = false
        try {
            sharingService.decodeFromDeepLink(Uri.parse("https://vito2211.github.io/WhatoToWatch/"))
        } catch (e: IllegalArgumentException) {
            errorThrown = true
            assertTrue(e.message!!.contains("Missing share data"))
        }
        assertTrue("Should reject URL missing share data", errorThrown)
    }

    @Test
    fun testGenerateShareResult_urlLengthEvaluatedWithFullHttpsUrl() = runBlocking {
        // Test with custom service having very small limit to verify evaluation with full HTTPS URL
        val testService = SharingService(
            context = context,
            database = db,
            contentDao = contentDao,
            categoryDao = categoryDao,
            customListDao = customListDao,
            fileUriProvider = { Uri.fromFile(it) }
        )

        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Length Check",
            contents = listOf(
                ShareContentItem(
                    id = "check-1",
                    title = "Title",
                    contentType = "MOVIE",
                    watchStatus = "WATCHED",
                    createdAt = 1000L,
                    updatedAt = 1000L
                )
            )
        )

        val result = testService.generateShareResult(payload)
        assertTrue(result is ShareResult.DeepLink)
        val url = (result as ShareResult.DeepLink).url
        assertTrue(url.startsWith("https://vito2211.github.io/WhatoToWatch/#share/"))
        assertTrue("Full HTTPS URL length must be checked against MAX_DEEP_LINK_URL_LENGTH", url.length <= SharingService.MAX_DEEP_LINK_URL_LENGTH)
    }
}
