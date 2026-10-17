package com.vp.whattowatch.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.local.AppDatabase
import com.vp.whattowatch.data.local.dao.CategoryDao
import com.vp.whattowatch.data.local.dao.ContentDao
import com.vp.whattowatch.data.local.dao.CustomListDao
import com.vp.whattowatch.data.local.entity.CategoryEntity
import com.vp.whattowatch.data.local.entity.ContentEntity
import com.vp.whattowatch.data.local.entity.CustomListEntity
import com.vp.whattowatch.data.local.entity.ListItemCrossRef
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
class DatabaseRelationshipTest {

    private lateinit var db: AppDatabase
    private lateinit var contentDao: ContentDao
    private lateinit var customListDao: CustomListDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contentDao = db.contentDao()
        customListDao = db.customListDao()
        categoryDao = db.categoryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testContentCreationAndRetrieval() = runBlocking {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val content = ContentEntity(
            id = id,
            title = "Inception",
            contentType = "MOVIE",
            watchStatus = "WATCHED",
            createdAt = now,
            updatedAt = now
        )

        contentDao.insertOrUpdate(content)

        val retrieved = contentDao.getWithDetailsById(id)
        assertNotNull(retrieved)
        assertEquals("Inception", retrieved!!.content.title)
        assertEquals("MOVIE", retrieved.content.contentType)
        assertEquals("WATCHED", retrieved.content.watchStatus)
        assertTrue(retrieved.categories.isEmpty())
        assertTrue(retrieved.lists.isEmpty())
    }

    @Test
    fun testContentWithCategoriesAndLists() = runBlocking {
        val contentId = UUID.randomUUID().toString()
        val catId1 = UUID.randomUUID().toString()
        val catId2 = UUID.randomUUID().toString()
        val listId1 = UUID.randomUUID().toString()
        val listId2 = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // 1. Insert Categories
        categoryDao.insert(CategoryEntity(catId1, "Sci-Fi", now))
        categoryDao.insert(CategoryEntity(catId2, "Mind-Bending", now))

        // 2. Insert Lists
        customListDao.insert(CustomListEntity(listId1, "Favorites", now, now))
        customListDao.insert(CustomListEntity(listId2, "Watch Weekend", now, now))

        // 3. Insert Content with relations
        val content = ContentEntity(
            id = contentId,
            title = "The Matrix",
            contentType = "MOVIE",
            watchStatus = "WATCHING",
            createdAt = now,
            updatedAt = now
        )
        contentDao.upsertContentWithRelations(
            content = content,
            categoryIds = listOf(catId1, catId2),
            listIds = listOf(listId1, listId2)
        )

        // 4. Verify relations populated
        val retrieved = contentDao.getWithDetailsById(contentId)
        assertNotNull(retrieved)
        assertEquals(2, retrieved!!.categories.size)
        assertEquals(2, retrieved.lists.size)

        val categoryNames = retrieved.categories.map { it.name }.toSet()
        assertTrue(categoryNames.contains("Sci-Fi"))
        assertTrue(categoryNames.contains("Mind-Bending"))

        val listNames = retrieved.lists.map { it.name }.toSet()
        assertTrue(listNames.contains("Favorites"))
        assertTrue(listNames.contains("Watch Weekend"))
    }

    @Test
    fun testCascadingDeleteContentRemovesCrossRefsOnly() = runBlocking {
        val contentId = UUID.randomUUID().toString()
        val listId = UUID.randomUUID().toString()
        val catId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        categoryDao.insert(CategoryEntity(catId, "Drama", now))
        customListDao.insert(CustomListEntity(listId, "Must Watch", now, now))

        val content = ContentEntity(
            id = contentId,
            title = "Succession",
            contentType = "SERIES",
            watchStatus = "WATCHED",
            createdAt = now,
            updatedAt = now
        )
        contentDao.upsertContentWithRelations(
            content = content,
            categoryIds = listOf(catId),
            listIds = listOf(listId)
        )

        // Delete content
        contentDao.deleteById(contentId)

        // Content is gone
        assertNull(contentDao.getWithDetailsById(contentId))

        // Category and List are still intact!
        assertNotNull(categoryDao.getById(catId))
        assertNotNull(customListDao.getById(listId))
    }

    @Test
    fun testDeletingListDoesNotDeleteUnderlyingContent() = runBlocking {
        val contentId = UUID.randomUUID().toString()
        val listId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        customListDao.insert(CustomListEntity(listId, "Temp List", now, now))
        val content = ContentEntity(
            id = contentId,
            title = "Blade Runner 2049",
            contentType = "MOVIE",
            watchStatus = "NOT_WATCHED",
            createdAt = now,
            updatedAt = now
        )
        contentDao.insertOrUpdate(content)
        customListDao.addContentToList(ListItemCrossRef(listId, contentId, now))

        // Verify membership
        val listIds = customListDao.getListIdsForContent(contentId)
        assertEquals(listOf(listId), listIds)

        // Delete the list
        customListDao.deleteById(listId)

        // List is gone
        assertNull(customListDao.getById(listId))

        // Content item is still in library!
        val retrievedContent = contentDao.getWithDetailsById(contentId)
        assertNotNull(retrievedContent)
        assertEquals("Blade Runner 2049", retrievedContent!!.content.title)
        assertTrue(retrievedContent.lists.isEmpty())
    }

    @Test
    fun testRemovingFromOneListPreservesOtherLists() = runBlocking {
        val contentId = UUID.randomUUID().toString()
        val listId1 = UUID.randomUUID().toString()
        val listId2 = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        customListDao.insert(CustomListEntity(listId1, "List 1", now, now))
        customListDao.insert(CustomListEntity(listId2, "List 2", now, now))

        val content = ContentEntity(
            id = contentId,
            title = "Dune",
            contentType = "MOVIE",
            watchStatus = "NOT_WATCHED",
            createdAt = now,
            updatedAt = now
        )
        contentDao.upsertContentWithRelations(content, emptyList(), listOf(listId1, listId2))

        // Remove from list 1 only
        customListDao.removeContentFromList(listId1, contentId)

        val retrieved = contentDao.getWithDetailsById(contentId)
        assertNotNull(retrieved)
        assertEquals(1, retrieved!!.lists.size)
        assertEquals(listId2, retrieved.lists.first().id)
    }
}
