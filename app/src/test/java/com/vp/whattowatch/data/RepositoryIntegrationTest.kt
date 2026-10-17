package com.vp.whattowatch.data

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
import com.vp.whattowatch.domain.model.Category
import com.vp.whattowatch.domain.model.ContentFilter
import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.WatchStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class RepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var contentRepo: ContentRepository
    private lateinit var listRepo: CustomListRepository
    private lateinit var categoryRepo: CategoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contentRepo = ContentRepositoryImpl(db.contentDao())
        listRepo = CustomListRepositoryImpl(db.customListDao())
        categoryRepo = CategoryRepositoryImpl(db.categoryDao())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCategoryGetOrCreateIdempotent() = runBlocking {
        val cat1 = categoryRepo.getOrCreate("Anime")
        val cat2 = categoryRepo.getOrCreate("anime ") // Case and whitespace insensitivity
        assertEquals(cat1.id, cat2.id)
        assertEquals("anime", cat2.name.lowercase())

        val all = categoryRepo.getAll()
        assertEquals(1, all.size)
    }

    @Test
    fun testContentRepositorySaveAndObserve() = runBlocking {
        val cat = categoryRepo.getOrCreate("Sci-Fi")
        val list = listRepo.create("Must Watch")

        val item = ContentItem(
            id = UUID.randomUUID().toString(),
            title = "Arrival",
            contentType = ContentType.MOVIE,
            watchStatus = WatchStatus.NOT_WATCHED,
            categories = listOf(cat),
            listIds = listOf(list.id)
        )

        contentRepo.save(item)

        val retrieved = contentRepo.getById(item.id)
        assertNotNull(retrieved)
        assertEquals("Arrival", retrieved!!.title)
        assertEquals(ContentType.MOVIE, retrieved.contentType)
        assertEquals(WatchStatus.NOT_WATCHED, retrieved.watchStatus)
        assertEquals(1, retrieved.categories.size)
        assertEquals("Sci-Fi", retrieved.categories.first().name)
        assertEquals(listOf(list.id), retrieved.listIds)

        // Update watch status
        contentRepo.updateWatchStatus(item.id, WatchStatus.WATCHED)
        val updated = contentRepo.getById(item.id)
        assertEquals(WatchStatus.WATCHED, updated?.watchStatus)
    }

    @Test
    fun testMultiCriteriaFiltering() = runBlocking {
        val animeCat = categoryRepo.getOrCreate("Anime")
        val sciFiCat = categoryRepo.getOrCreate("Sci-Fi")
        val watchLaterList = listRepo.create("Watch Later")

        // Item 1: Spirited Away (Anime, Movie, Watched, In Watch Later)
        contentRepo.save(
            ContentItem(
                id = UUID.randomUUID().toString(),
                title = "Spirited Away",
                contentType = ContentType.MOVIE,
                watchStatus = WatchStatus.WATCHED,
                categories = listOf(animeCat),
                listIds = listOf(watchLaterList.id)
            )
        )

        // Item 2: Attack on Titan (Anime, Series, Watching, In Watch Later)
        contentRepo.save(
            ContentItem(
                id = UUID.randomUUID().toString(),
                title = "Attack on Titan",
                contentType = ContentType.SERIES,
                watchStatus = WatchStatus.WATCHING,
                categories = listOf(animeCat),
                listIds = listOf(watchLaterList.id)
            )
        )

        // Item 3: Ghost in the Shell (Anime, Movie, Not Watched, In Watch Later)
        contentRepo.save(
            ContentItem(
                id = UUID.randomUUID().toString(),
                title = "Ghost in the Shell",
                contentType = ContentType.MOVIE,
                watchStatus = WatchStatus.NOT_WATCHED,
                categories = listOf(animeCat, sciFiCat),
                listIds = listOf(watchLaterList.id)
            )
        )

        // Item 4: Interstellar (Sci-Fi, Movie, Watched, Not in List)
        contentRepo.save(
            ContentItem(
                id = UUID.randomUUID().toString(),
                title = "Interstellar",
                contentType = ContentType.MOVIE,
                watchStatus = WatchStatus.WATCHED,
                categories = listOf(sciFiCat),
                listIds = emptyList()
            )
        )

        // Test 1: Title query search
        val searchResults = contentRepo.observeAll(ContentFilter(query = "Titan")).first()
        assertEquals(1, searchResults.size)
        assertEquals("Attack on Titan", searchResults.first().title)

        // Test 2: Watch status filter
        val watchingResults = contentRepo.observeAll(ContentFilter(watchStatus = WatchStatus.WATCHING)).first()
        assertEquals(1, watchingResults.size)
        assertEquals("Attack on Titan", watchingResults.first().title)

        // Test 3: Content type filter
        val seriesResults = contentRepo.observeAll(ContentFilter(contentType = ContentType.SERIES)).first()
        assertEquals(1, seriesResults.size)
        assertEquals("Attack on Titan", seriesResults.first().title)

        // Test 4: Category filter
        val animeResults = contentRepo.observeAll(ContentFilter(categoryId = animeCat.id)).first()
        assertEquals(3, animeResults.size)

        // Test 5: List membership filter
        val listResults = contentRepo.observeAll(ContentFilter(listId = watchLaterList.id)).first()
        assertEquals(3, listResults.size)

        // Test 6: Combined filter: Anime category + Movie type + Not Watched status
        val combinedResults = contentRepo.observeAll(
            ContentFilter(
                categoryId = animeCat.id,
                contentType = ContentType.MOVIE,
                watchStatus = WatchStatus.NOT_WATCHED
            )
        ).first()
        assertEquals(1, combinedResults.size)
        assertEquals("Ghost in the Shell", combinedResults.first().title)
    }

    @Test
    fun testCustomListManagementAndCounts() = runBlocking {
        val list = listRepo.create("Comedy Specials")
        assertEquals("Comedy Specials", list.name)

        // Rename list
        listRepo.rename(list.id, "Top Comedy")
        val renamed = listRepo.getById(list.id)
        assertEquals("Top Comedy", renamed?.name)

        // Add 2 items
        val item1 = ContentItem(id = UUID.randomUUID().toString(), title = "Show 1")
        val item2 = ContentItem(id = UUID.randomUUID().toString(), title = "Show 2")
        contentRepo.save(item1)
        contentRepo.save(item2)

        listRepo.addItemToList(list.id, item1.id)
        listRepo.addItemToList(list.id, item2.id)

        // Observe list items
        val itemsInList = listRepo.observeContentsForList(list.id).first()
        assertEquals(2, itemsInList.size)

        // Remove 1 item
        listRepo.removeItemFromList(list.id, item1.id)
        val itemsAfterRemoval = listRepo.observeContentsForList(list.id).first()
        assertEquals(1, itemsAfterRemoval.size)
        assertEquals(item2.id, itemsAfterRemoval.first().id)

        // Delete list
        listRepo.delete(list.id)
        val deletedList = listRepo.getById(list.id)
        assertNull(deletedList)

        // Verify underlying items were not deleted
        assertNotNull(contentRepo.getById(item1.id))
        assertNotNull(contentRepo.getById(item2.id))
    }
}
