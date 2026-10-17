package com.vp.whattowatch.ui

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.local.AppDatabase
import com.vp.whattowatch.data.local.dao.CategoryDao
import com.vp.whattowatch.data.local.dao.ContentDao
import com.vp.whattowatch.data.local.dao.CustomListDao
import com.vp.whattowatch.data.sharing.ImportConflictResolution
import com.vp.whattowatch.data.sharing.ShareContentItem
import com.vp.whattowatch.data.sharing.SharePayload
import com.vp.whattowatch.data.sharing.ShareType
import com.vp.whattowatch.data.sharing.SharingService
import com.vp.whattowatch.testutil.MainDispatcherRule
import com.vp.whattowatch.ui.sharing.ImportPreviewViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImportPreviewViewModelTest {

    @get:Rule
    val globalTimeout: Timeout = Timeout.seconds(30)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var contentDao: ContentDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var customListDao: CustomListDao
    private lateinit var sharingService: SharingService

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        contentDao = db.contentDao()
        categoryDao = db.categoryDao()
        customListDao = db.customListDao()

        sharingService = SharingService(
            context = app,
            database = db,
            contentDao = contentDao,
            categoryDao = categoryDao,
            customListDao = customListDao,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testLoadPayloadFromDeepLink_success() = runBlocking {
        val itemId1 = UUID.randomUUID().toString()
        val itemId2 = UUID.randomUUID().toString()
        val payload = SharePayload(
            version = 1,
            type = ShareType.CUSTOM_LIST,
            title = "Top Picks",
            contents = listOf(
                ShareContentItem(id = itemId1, title = "Movie A", contentType = "MOVIE", watchStatus = "WATCHED", externalUrl = null, description = null, createdAt = 1000L, updatedAt = 1000L),
                ShareContentItem(id = itemId2, title = "Movie B", contentType = "MOVIE", watchStatus = "NOT_WATCHED", externalUrl = null, description = null, createdAt = 1000L, updatedAt = 1000L)
            )
        )

        val encoded = sharingService.compressAndEncode(payload)
        val deepLink = "whattowatch://share/$encoded"

        val viewModel = ImportPreviewViewModel(
            application = app,
            sharingService = sharingService,
            deepLink = deepLink,
            fileUri = null,
            rawPayload = null
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertNull("Error message should be null but was: ${state.errorMessage}", state.errorMessage)
        assertFalse("Loading should be finished", state.isLoading)
        assertNotNull("Preview should be loaded", state.preview)
        assertEquals("Top Picks", state.preview?.payload?.title)
        assertEquals(2, state.selectedItemIds.size)
        assertTrue(state.selectedItemIds.contains(itemId1))
        assertTrue(state.selectedItemIds.contains(itemId2))
    }

    @Test
    fun testLoadPayload_invalidDeepLink_setsErrorMessage() = runBlocking {
        val viewModel = ImportPreviewViewModel(
            application = app,
            sharingService = sharingService,
            deepLink = "whattowatch://share/invalid-non-base64-payload!!!",
            fileUri = null,
            rawPayload = null
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertFalse("Loading should be finished", state.isLoading)
        assertNotNull("Error message should be present", state.errorMessage)
        assertNull("Preview should not be loaded", state.preview)
    }

    @Test
    fun testSelectionToggles() = runBlocking {
        val itemId1 = UUID.randomUUID().toString()
        val itemId2 = UUID.randomUUID().toString()
        val payload = SharePayload(
            version = 1,
            type = ShareType.CUSTOM_LIST,
            title = "Selections",
            contents = listOf(
                ShareContentItem(id = itemId1, title = "Movie 1", contentType = "MOVIE", watchStatus = "NOT_WATCHED", externalUrl = null, description = null, createdAt = 1000L, updatedAt = 1000L),
                ShareContentItem(id = itemId2, title = "Movie 2", contentType = "MOVIE", watchStatus = "NOT_WATCHED", externalUrl = null, description = null, createdAt = 1000L, updatedAt = 1000L)
            )
        )

        val encoded = sharingService.compressAndEncode(payload)
        val viewModel = ImportPreviewViewModel(
            application = app,
            sharingService = sharingService,
            deepLink = "whattowatch://share/$encoded",
            fileUri = null,
            rawPayload = null
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(2, state.selectedItemIds.size)

        // Toggle item 1 off
        viewModel.onToggleItem(itemId1)
        assertEquals(1, viewModel.uiState.value.selectedItemIds.size)
        assertFalse(viewModel.uiState.value.selectedItemIds.contains(itemId1))
        assertTrue(viewModel.uiState.value.selectedItemIds.contains(itemId2))

        // Deselect all
        viewModel.onDeselectAll()
        assertTrue(viewModel.uiState.value.selectedItemIds.isEmpty())

        // Select all
        viewModel.onSelectAll()
        assertEquals(2, viewModel.uiState.value.selectedItemIds.size)

        // Toggle item 1 off and then on again
        viewModel.onToggleItem(itemId1)
        assertEquals(1, viewModel.uiState.value.selectedItemIds.size)
        viewModel.onToggleItem(itemId1)
        assertEquals(2, viewModel.uiState.value.selectedItemIds.size)
    }

    @Test
    fun testConflictResolutionChanged() = runBlocking {
        val itemId = UUID.randomUUID().toString()
        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Test",
            contents = listOf(
                ShareContentItem(id = itemId, title = "Movie", contentType = "MOVIE", watchStatus = "NOT_WATCHED", externalUrl = null, description = null, createdAt = 1000L, updatedAt = 1000L)
            )
        )

        val encoded = sharingService.compressAndEncode(payload)
        val viewModel = ImportPreviewViewModel(
            application = app,
            sharingService = sharingService,
            deepLink = "whattowatch://share/$encoded",
            fileUri = null,
            rawPayload = null
        )

        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(ImportConflictResolution.SKIP_EXISTING, state.conflictResolution)

        viewModel.onConflictResolutionChanged(ImportConflictResolution.KEEP_BOTH_AS_COPY)
        assertEquals(ImportConflictResolution.KEEP_BOTH_AS_COPY, viewModel.uiState.value.conflictResolution)

        viewModel.onConflictResolutionChanged(ImportConflictResolution.OVERWRITE_EXISTING)
        assertEquals(ImportConflictResolution.OVERWRITE_EXISTING, viewModel.uiState.value.conflictResolution)
    }

    @Test
    fun testConfirmImport_success() = runBlocking {
        val itemId = UUID.randomUUID().toString()
        val payload = SharePayload(
            version = 1,
            type = ShareType.SINGLE_ITEM,
            title = "Confirm Import Movie",
            contents = listOf(
                ShareContentItem(id = itemId, title = "Confirm Import Movie", contentType = "MOVIE", watchStatus = "WATCHED", externalUrl = null, description = "Great movie", createdAt = 1000L, updatedAt = 1000L)
            )
        )

        val encoded = sharingService.compressAndEncode(payload)
        val viewModel = ImportPreviewViewModel(
            application = app,
            sharingService = sharingService,
            deepLink = "whattowatch://share/$encoded",
            fileUri = null,
            rawPayload = null
        )

        val loadedState = viewModel.uiState.first { !it.isLoading }
        assertNotNull(loadedState.preview)

        var successReportedCount = -1
        viewModel.confirmImport { count ->
            successReportedCount = count
        }

        val completedState = viewModel.uiState.first { !it.isImporting && it.importSuccessCount != null }

        assertEquals(1, successReportedCount)
        assertEquals(1, completedState.importSuccessCount)
        assertFalse(completedState.isImporting)

        // Verify item is now in the database
        val savedItem = contentDao.getWithDetailsById(itemId)?.content
        assertNotNull(savedItem)
        assertEquals("Confirm Import Movie", savedItem!!.title)
    }
}
