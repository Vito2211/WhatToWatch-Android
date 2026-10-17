package com.vp.whattowatch.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.local.AppDatabase
import com.vp.whattowatch.data.repository.ContentRepository
import com.vp.whattowatch.data.repository.ContentRepositoryImpl
import com.vp.whattowatch.data.repository.CustomListRepository
import com.vp.whattowatch.data.repository.CustomListRepositoryImpl
import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.testutil.MainDispatcherRule
import com.vp.whattowatch.ui.lists.ListsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class ListsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var contentRepository: ContentRepository
    private lateinit var customListRepository: CustomListRepository
    private lateinit var viewModel: ListsViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setTransactionExecutor(Dispatchers.Main.asExecutor())
            .setQueryExecutor(Dispatchers.Main.asExecutor())
            .build()
        contentRepository = ContentRepositoryImpl(db.contentDao())
        customListRepository = CustomListRepositoryImpl(db.customListDao())
        viewModel = ListsViewModel(customListRepository)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCreateRenameAndDeleteList() = runBlocking {
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Create list
        viewModel.onOpenCreateDialog()
        assertTrue(viewModel.uiState.value.showCreateDialog)

        viewModel.onNewListNameChanged("Weekend Watch")
        viewModel.onCreateList()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val lists = customListRepository.getAll()
        assertEquals(1, lists.size)
        assertEquals("Weekend Watch", lists.first().name)

        val createdList = lists.first()

        // Rename list
        viewModel.onOpenRenameDialog(createdList)
        viewModel.onRenameListNameChanged("Weekend Movie Marathon")
        viewModel.onRenameList()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val renamed = customListRepository.getById(createdList.id)
        assertEquals("Weekend Movie Marathon", renamed?.name)

        // Assign an item to list
        val item = ContentItem(id = UUID.randomUUID().toString(), title = "The Prestige")
        contentRepository.save(item)
        customListRepository.addItemToList(createdList.id, item.id)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Delete list
        viewModel.onOpenDeleteDialog(createdList)
        viewModel.onDeleteList()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertNull(customListRepository.getById(createdList.id))
        // Verify content item still exists
        assertNotNull(contentRepository.getById(item.id))
    }
}
