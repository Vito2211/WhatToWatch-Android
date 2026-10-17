package com.vp.whattowatch.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vp.whattowatch.data.local.dao.CategoryDao
import com.vp.whattowatch.data.local.dao.ContentDao
import com.vp.whattowatch.data.local.dao.CustomListDao
import com.vp.whattowatch.data.local.entity.CategoryEntity
import com.vp.whattowatch.data.local.entity.ContentCategoryCrossRef
import com.vp.whattowatch.data.local.entity.ContentEntity
import com.vp.whattowatch.data.local.entity.CustomListEntity
import com.vp.whattowatch.data.local.entity.ListItemCrossRef

@Database(
    entities = [
        ContentEntity::class,
        CategoryEntity::class,
        ContentCategoryCrossRef::class,
        CustomListEntity::class,
        ListItemCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao
    abstract fun customListDao(): CustomListDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "what_to_watch.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Check if posterPath already exists in contents table
                val cursor = db.query("PRAGMA table_info(contents)")
                var hasPosterPath = false
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1 && cursor.getString(nameIndex) == "posterPath") {
                        hasPosterPath = true
                        break
                    }
                }
                cursor.close()

                if (!hasPosterPath) {
                    db.execSQL("ALTER TABLE contents ADD COLUMN posterPath TEXT DEFAULT NULL")
                }
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
