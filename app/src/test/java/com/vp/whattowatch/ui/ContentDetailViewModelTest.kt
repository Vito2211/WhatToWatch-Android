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
import com.vp.whattowatch.domain.model.WatchStatus
import com.vp.whattowatch.testutil.MainDispatcherRule
import com.vp.whattowatch.ui.detail.ContentDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class ContentDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var db: AppDatabase
    private lateinit var contentRepository: ContentRepository
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
        customListRepository = CustomListRepositoryImpl(db.customListDao())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testUpdateWatchStatusAndDeletion() = runBlocking {
        val itemId = UUID.randomUUID().toString()
        val item = ContentItem(
            id = itemId,
            title = "Oppenheimer",
            watchStatus = WatchStatus.NOT_WATCHED
        )
        contentRepository.save(item)

        val viewModel = ContentDetailViewModel(
            contentId = itemId,
            contentRepository = contentRepository,
            customListRepository = customListRepository
        )
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Status update
        viewModel.onUpdateWatchStatus(WatchStatus.WATCHED)
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val updated = contentRepository.getById(itemId)
        assertEquals(WatchStatus.WATCHED, updated?.watchStatus)

        // Delete action
        var deleteSuccess = false
        viewModel.onDeleteContent(onSuccess = { deleteSuccess = true })
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(deleteSuccess)
        assertNull(contentRepository.getById(itemId))
    }
}
