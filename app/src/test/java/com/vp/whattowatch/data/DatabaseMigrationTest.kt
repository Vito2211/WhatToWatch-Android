package com.vp.whattowatch.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.local.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbFile = File(context.cacheDir, "migration_test.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbFile.absolutePath)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create version 1 schema of contents table
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS contents (
                            id TEXT PRIMARY KEY NOT NULL,
                            title TEXT NOT NULL,
                            contentType TEXT NOT NULL,
                            watchStatus TEXT NOT NULL,
                            externalUrl TEXT,
                            description TEXT,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        helper = FrameworkSQLiteOpenHelperFactory().create(config)
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        if (::db.isInitialized && db.isOpen) {
            db.close()
        }
        if (::helper.isInitialized) {
            helper.close()
        }
        if (::dbFile.isInitialized && dbFile.exists()) {
            dbFile.delete()
        }
    }

    @Test
    fun testMigration1To2AddsPosterPathColumn() {
        // Step 1: Insert row into v1 table
        db.execSQL(
            """
            INSERT INTO contents (id, title, contentType, watchStatus, externalUrl, description, createdAt, updatedAt)
            VALUES ('item-1', 'Dune', 'MOVIE', 'WATCHED', 'https://example.com', 'Sci-Fi epic', 1000, 2000)
            """.trimIndent()
        )

        // Step 2: Run migration 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(db)

        // Step 3: Verify posterPath column exists and existing item has NULL posterPath
        val cursor = db.query("SELECT id, title, posterPath FROM contents WHERE id = 'item-1'")
        assertTrue("Cursor should return row", cursor.moveToFirst())
        assertEquals("item-1", cursor.getString(cursor.getColumnIndexOrThrow("id")))
        assertEquals("Dune", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertNull("posterPath should default to NULL for existing items", cursor.getString(cursor.getColumnIndexOrThrow("posterPath")))
        cursor.close()

        // Step 4: Verify updating and inserting with posterPath works
        db.execSQL("UPDATE contents SET posterPath = 'dune_poster.jpg' WHERE id = 'item-1'")
        val updatedCursor = db.query("SELECT posterPath FROM contents WHERE id = 'item-1'")
        assertTrue(updatedCursor.moveToFirst())
        assertEquals("dune_poster.jpg", updatedCursor.getString(updatedCursor.getColumnIndexOrThrow("posterPath")))
        updatedCursor.close()

        // Step 5: Test idempotency - running migration again should not throw or fail
        AppDatabase.MIGRATION_1_2.migrate(db)
        val idempotentCursor = db.query("SELECT posterPath FROM contents WHERE id = 'item-1'")
        assertTrue(idempotentCursor.moveToFirst())
        assertEquals("dune_poster.jpg", idempotentCursor.getString(idempotentCursor.getColumnIndexOrThrow("posterPath")))
        idempotentCursor.close()
    }
}
