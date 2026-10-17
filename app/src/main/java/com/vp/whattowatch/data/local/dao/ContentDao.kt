package com.vp.whattowatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vp.whattowatch.data.local.entity.ContentCategoryCrossRef
import com.vp.whattowatch.data.local.entity.ContentEntity
import com.vp.whattowatch.data.local.entity.ContentWithDetails
import com.vp.whattowatch.data.local.entity.ListItemCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {

    @Transaction
    @Query("SELECT * FROM contents ORDER BY updatedAt DESC")
    fun observeAllWithDetails(): Flow<List<ContentWithDetails>>

    @Transaction
    @Query("SELECT * FROM contents WHERE id = :id")
    fun observeWithDetailsById(id: String): Flow<ContentWithDetails?>

    @Transaction
    @Query("SELECT * FROM contents WHERE id = :id")
    suspend fun getWithDetailsById(id: String): ContentWithDetails?

    @Transaction
    @Query("SELECT * FROM contents ORDER BY updatedAt DESC")
    suspend fun getAllWithDetails(): List<ContentWithDetails>

    @Query("SELECT * FROM contents")
    suspend fun getAllEntities(): List<ContentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(content: ContentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contents: List<ContentEntity>)

    @Query("DELETE FROM contents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM contents")
    suspend fun deleteAllContents()

    @Query("UPDATE contents SET watchStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateWatchStatus(id: String, status: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM contents WHERE posterPath = :posterPath")
    suspend fun countByPosterPath(posterPath: String): Int

    @Query("SELECT DISTINCT posterPath FROM contents WHERE posterPath IS NOT NULL AND posterPath != ''")
    suspend fun getAllPosterPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContentCategories(crossRefs: List<ContentCategoryCrossRef>)

    @Query("SELECT * FROM content_category_cross_ref")
    suspend fun getAllContentCategoryCrossRefs(): List<ContentCategoryCrossRef>

    @Query("DELETE FROM content_category_cross_ref WHERE contentId = :contentId")
    suspend fun deleteContentCategories(contentId: String)

    @Query("DELETE FROM content_category_cross_ref")
    suspend fun deleteAllContentCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItems(crossRefs: List<ListItemCrossRef>)

    @Query("SELECT * FROM list_item_cross_ref")
    suspend fun getAllListItemCrossRefs(): List<ListItemCrossRef>

    @Query("DELETE FROM list_item_cross_ref WHERE contentId = :contentId")
    suspend fun deleteContentLists(contentId: String)

    @Query("DELETE FROM list_item_cross_ref")
    suspend fun deleteAllListItems()

    @Transaction
    suspend fun upsertContentWithRelations(
        content: ContentEntity,
        categoryIds: List<String>,
        listIds: List<String>
    ) {
        insertOrUpdate(content)
        deleteContentCategories(content.id)
        if (categoryIds.isNotEmpty()) {
            insertContentCategories(categoryIds.map { ContentCategoryCrossRef(content.id, it) })
        }
        deleteContentLists(content.id)
        if (listIds.isNotEmpty()) {
            insertListItems(listIds.map { ListItemCrossRef(it, content.id) })
        }
    }
}
