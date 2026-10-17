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
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.WatchStatus
import com.vp.whattowatch.testutil.MainDispatcherRule
import com.vp.whattowatch.ui.library.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var contentRepository: ContentRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var customListRepository: CustomListRepository
    private lateinit var viewModel: LibraryViewModel

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
        viewModel = LibraryViewModel(contentRepository, categoryRepository, customListRepository)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInitialLibraryIsEmpty() = runBlocking {
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.items.isEmpty())
        assertFalse(state.isLoading)
        assertTrue(state.isLibraryEmpty)
        assertFalse(state.isFilterActive)
    }

    @Test
    fun testQuickAddSavesItem() = runBlocking {
        viewModel.onOpenQuickAdd()
        assertTrue(viewModel.uiState.value.showQuickAddSheet)

        // Blank title should not save
        viewModel.onSaveQuickAdd("   ")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(contentRepository.getAll().isEmpty())

        // Valid title saves item
        viewModel.onSaveQuickAdd("The Dark Knight")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        val items = contentRepository.getAll()
        assertEquals(1, items.size)
        assertEquals("The Dark Knight", items.first().title)
        assertEquals(WatchStatus.NOT_WATCHED, items.first().watchStatus)
        assertEquals(ContentType.MOVIE, items.first().contentType)
    }

    @Test
    fun testStatusFilterToggling() {
        // Toggle Watching on
        viewModel.onStatusFilterSelected(WatchStatus.WATCHING)
        assertEquals(WatchStatus.WATCHING, viewModel.uiState.value.selectedStatus)

        // Toggle Watching off
        viewModel.onStatusFilterSelected(WatchStatus.WATCHING)
        assertNull(viewModel.uiState.value.selectedStatus)
    }

    @Test
    fun testSearchQueryUpdatesState() {
        viewModel.onSearchQueryChanged("Gladiator")
        assertEquals("Gladiator", viewModel.uiState.value.query)
        assertTrue(viewModel.uiState.value.isFilterActive)

        viewModel.onClearFilters()
        assertEquals("", viewModel.uiState.value.query)
        assertFalse(viewModel.uiState.value.isFilterActive)
    }
}
