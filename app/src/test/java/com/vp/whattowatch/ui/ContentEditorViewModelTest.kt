package com.vp.whattowatch.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.local.AppDatabase
import com.vp.whattowatch.data.repository.CategoryRepository
import com.vp.whattowatch.data.repository.CategoryRepositoryImpl
import com.vp.whattowatch.data.repository.ContentRepository
import com.vp.whattowatch.data.repository.ContentRepositoryImpl
import com.vp.whattowatch.data.repository.CustomListRepository
import com.vp.whattowatch.data.repository.CustomListRepositoryImpl
import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.WatchStatus
import com.vp.whattowatch.testutil.MainDispatcherRule
import com.vp.whattowatch.ui.editor.ContentEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ContentEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var contentRepository: ContentRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var customListRepository: CustomListRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setTransactionExecutor(Dispatchers.Main.asExecutor())
            .setQueryExecutor(Dispatchers.Main.asExecutor())
            .build()
        contentRepository = ContentRepositoryImpl(db.contentDao())
        categoryRepository = CategoryRepositoryImpl(db.categoryDao())
        customListRepository = CustomListRepositoryImpl(db.customListDao())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCreateContentValidationAndSave() = runBlocking {
        val viewModel = ContentEditorViewModel(
            contentId = null,
            initialTitle = "Pulp Fiction",
            contentRepository = contentRepository,
            categoryRepository = categoryRepository,
            customListRepository = customListRepository
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Pulp Fiction", viewModel.uiState.value.title)
        assertTrue(viewModel.uiState.value.isValid)

        // Set invalid title
        viewModel.onTitleChanged("")
        assertNotNull(viewModel.uiState.value.titleError)

        // Set valid title and attributes
        viewModel.onTitleChanged("Pulp Fiction")
        viewModel.onContentTypeSelected(ContentType.MOVIE)
        viewModel.onWatchStatusSelected(WatchStatus.WATCHED)
        viewModel.onDescriptionChanged("Classic Quentin Tarantino movie")

        var saveSuccess = false
        viewModel.saveContent(onSuccess = { saveSuccess = true })
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saveSuccess)
        val items = contentRepository.getAll()
        assertEquals(1, items.size)
        assertEquals("Pulp Fiction", items.first().title)
        assertEquals(ContentType.MOVIE, items.first().contentType)
        assertEquals(WatchStatus.WATCHED, items.first().watchStatus)
        assertEquals("Classic Quentin Tarantino movie", items.first().description)
    }

    @Test
    fun testEditExistingContentPreservesCreatedAt() = runBlocking {
        val originalCreatedAt = 1000000L
        val originalItem = ContentItem(
            id = UUID.randomUUID().toString(),
            title = "Original Title",
            contentType = ContentType.SERIES,
            watchStatus = WatchStatus.NOT_WATCHED,
            createdAt = originalCreatedAt,
            updatedAt = originalCreatedAt
        )
        contentRepository.save(originalItem)

        val viewModel = ContentEditorViewModel(
            contentId = originalItem.id,
            initialTitle = null,
            contentRepository = contentRepository,
            categoryRepository = categoryRepository,
            customListRepository = customListRepository
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onTitleChanged("Updated Title")
        viewModel.onWatchStatusSelected(WatchStatus.WATCHING)

        var saveSuccess = false
        viewModel.saveContent(onSuccess = { saveSuccess = true })
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(saveSuccess)
        val updated = contentRepository.getById(originalItem.id)
        assertNotNull(updated)
        assertEquals("Updated Title", updated!!.title)
        assertEquals(WatchStatus.WATCHING, updated.watchStatus)
        assertEquals(originalCreatedAt, updated.createdAt)
    }

    @Test
    fun testPosterSelectionAndRemoval() = runBlocking {
        val viewModel = ContentEditorViewModel(
            contentId = null,
            initialTitle = "Blade Runner",
            contentRepository = contentRepository,
            categoryRepository = categoryRepository,
            customListRepository = customListRepository,
            imageStorageManager = null
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Initially posterPath is null
        org.junit.Assert.assertNull(viewModel.uiState.value.posterPath)

        // Remove poster sets it to null
        viewModel.onRemovePoster()
        org.junit.Assert.assertNull(viewModel.uiState.value.posterPath)
    }
}
