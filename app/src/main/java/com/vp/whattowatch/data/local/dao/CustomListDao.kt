package com.vp.whattowatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.vp.whattowatch.data.local.entity.ContentWithDetails
import com.vp.whattowatch.data.local.entity.CustomListEntity
import com.vp.whattowatch.data.local.entity.CustomListWithCount
import com.vp.whattowatch.data.local.entity.ListItemCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomListDao {

    @Query("SELECT * FROM custom_lists ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CustomListEntity>>

    @Query(
        """
        SELECT custom_lists.*, COUNT(list_item_cross_ref.contentId) AS itemCount
        FROM custom_lists
        LEFT JOIN list_item_cross_ref ON custom_lists.id = list_item_cross_ref.listId
        GROUP BY custom_lists.id
        ORDER BY custom_lists.updatedAt DESC
        """
    )
    fun observeAllWithItemCount(): Flow<List<CustomListWithCount>>

    @Query("SELECT * FROM custom_lists WHERE id = :id")
    suspend fun getById(id: String): CustomListEntity?

    @Query("SELECT * FROM custom_lists ORDER BY updatedAt DESC")
    suspend fun getAll(): List<CustomListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: CustomListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lists: List<CustomListEntity>)

    @Update
    suspend fun update(list: CustomListEntity)

    @Query("DELETE FROM custom_lists WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM custom_lists")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addContentToList(crossRef: ListItemCrossRef)

    @Query("DELETE FROM list_item_cross_ref WHERE listId = :listId AND contentId = :contentId")
    suspend fun removeContentFromList(listId: String, contentId: String)

    @Transaction
    @Query(
        """
        SELECT contents.* FROM contents
        INNER JOIN list_item_cross_ref ON contents.id = list_item_cross_ref.contentId
        WHERE list_item_cross_ref.listId = :listId
        ORDER BY list_item_cross_ref.addedAt DESC
        """
    )
    fun observeContentsForList(listId: String): Flow<List<ContentWithDetails>>

    @Transaction
    @Query(
        """
        SELECT contents.* FROM contents
        INNER JOIN list_item_cross_ref ON contents.id = list_item_cross_ref.contentId
        WHERE list_item_cross_ref.listId = :listId
        ORDER BY list_item_cross_ref.addedAt DESC
        """
    )
    suspend fun getContentsForList(listId: String): List<ContentWithDetails>

    @Query("SELECT listId FROM list_item_cross_ref WHERE contentId = :contentId")
    suspend fun getListIdsForContent(contentId: String): List<String>
}
