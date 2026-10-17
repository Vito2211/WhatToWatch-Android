package com.vp.whattowatch.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.local.AppDatabase
import com.vp.whattowatch.data.local.dao.ContentDao
import com.vp.whattowatch.data.local.entity.ContentEntity
import com.vp.whattowatch.data.local.image.ImageStorageManager
import kotlinx.coroutines.runBlocking
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ImageStorageManagerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var contentDao: ContentDao
    private lateinit var imageStorageManager: ImageStorageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contentDao = db.contentDao()
        imageStorageManager = ImageStorageManager(context)
        imageStorageManager.postersDir.deleteRecursively()
        imageStorageManager.postersDir.mkdirs()
    }

    @After
    fun tearDown() {
        db.close()
        imageStorageManager.postersDir.deleteRecursively()
    }

    @Test
    fun testSaveImageFromStreamDownscalesAndSaves() = runBlocking {
        val originalBitmap = Bitmap.createBitmap(1440, 2160, Bitmap.Config.ARGB_8888)
        val byteOut = ByteArrayOutputStream()
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteOut)
        val inputStream = ByteArrayInputStream(byteOut.toByteArray())

        val relativeFilename = imageStorageManager.saveImageFromStream(inputStream)
        assertNotNull("Saved poster filename should not be null", relativeFilename)
        assertTrue("Filename should end with .jpg", relativeFilename!!.endsWith(".jpg"))

        val posterFile = imageStorageManager.getPosterFile(relativeFilename)
        assertTrue("Saved poster file must exist on disk", posterFile.exists())
        assertTrue("Saved poster file should have positive byte length", posterFile.length() > 0)

        // Verify downscaled dimensions
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(posterFile.absolutePath, options)
        assertTrue("Decoded width should be <= 720 px, was ${options.outWidth}", options.outWidth <= ImageStorageManager.MAX_POSTER_WIDTH)
        assertTrue("Decoded height should be <= 1080 px, was ${options.outHeight}", options.outHeight <= ImageStorageManager.MAX_POSTER_HEIGHT)

        assertTrue(imageStorageManager.posterExists(relativeFilename))
    }

    @Test
    fun testSaveCorruptedOrEmptyStreamReturnsNull() = runBlocking {
        val emptyStream = ByteArrayInputStream(ByteArray(0))
        val resultEmpty = imageStorageManager.saveImageFromStream(emptyStream)
        assertNull("Empty stream should return null gracefully", resultEmpty)

        val junkStream = ByteArrayInputStream("not an image at all".toByteArray())
        val resultJunk = imageStorageManager.saveImageFromStream(junkStream)
        assertNull("Garbage bytes should return null gracefully", resultJunk)
    }

    @Test
    fun testPosterExists() = runBlocking {
        assertFalse(imageStorageManager.posterExists(null))
        assertFalse(imageStorageManager.posterExists(""))
        assertFalse(imageStorageManager.posterExists("non_existent_poster.jpg"))

        val bitmap = Bitmap.createBitmap(100, 150, Bitmap.Config.ARGB_8888)
        val byteOut = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteOut)
        val filename = imageStorageManager.saveImageFromStream(ByteArrayInputStream(byteOut.toByteArray()))
        assertNotNull(filename)
        assertTrue(imageStorageManager.posterExists(filename))
    }

    @Test
    fun testDeletePosterSafelyReferenceCounting() = runBlocking {
        // Save test image
        val bitmap = Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888)
        val byteOut = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteOut)
        val filename = imageStorageManager.saveImageFromStream(ByteArrayInputStream(byteOut.toByteArray()))
        assertNotNull(filename)
        val file = imageStorageManager.getPosterFile(filename!!)
        assertTrue(file.exists())

        // Insert content referencing this poster
        val contentId = UUID.randomUUID().toString()
        contentDao.insertOrUpdate(
            ContentEntity(
                id = contentId,
                title = "Test Item",
                contentType = "MOVIE",
                watchStatus = "NOT_WATCHED",
                posterPath = filename,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        // Attempt delete when referenced in DB
        val deletedWhileReferenced = imageStorageManager.deletePosterSafely(filename, contentDao)
        assertFalse("Should not delete poster while referenced", deletedWhileReferenced)
        assertTrue("File should still exist", file.exists())

        // Delete content from DB so poster becomes orphaned
        contentDao.deleteById(contentId)
        val deletedWhenOrphaned = imageStorageManager.deletePosterSafely(filename, contentDao)
        assertTrue("Should delete poster when no references exist", deletedWhenOrphaned)
        assertFalse("File should be deleted from disk", file.exists())
    }

    @Test
    fun testGetAllStoredPosterFilenames() = runBlocking {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val byteOut = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteOut)

        val f1 = imageStorageManager.saveImageFromStream(ByteArrayInputStream(byteOut.toByteArray()))
        val f2 = imageStorageManager.saveImageFromStream(ByteArrayInputStream(byteOut.toByteArray()))

        val all = imageStorageManager.getAllStoredPosterFilenames()
        assertEquals(2, all.size)
        assertTrue(all.contains(f1))
        assertTrue(all.contains(f2))
    }
}
