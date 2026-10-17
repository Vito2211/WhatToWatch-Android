package com.vp.whattowatch.data.repository

import com.vp.whattowatch.data.local.dao.CustomListDao
import com.vp.whattowatch.data.local.entity.CategoryEntity
import com.vp.whattowatch.data.local.entity.ContentWithDetails
import com.vp.whattowatch.data.local.entity.CustomListEntity
import com.vp.whattowatch.data.local.entity.CustomListWithCount
import com.vp.whattowatch.data.local.entity.ListItemCrossRef
import com.vp.whattowatch.domain.model.Category
import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.CustomList
import com.vp.whattowatch.domain.model.WatchStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface CustomListRepository {
    fun observeAll(): Flow<List<CustomList>>
    suspend fun getById(id: String): CustomList?
    suspend fun getAll(): List<CustomList>
    suspend fun create(name: String): CustomList
    suspend fun rename(id: String, newName: String)
    suspend fun delete(id: String)
    suspend fun addItemToList(listId: String, contentId: String)
    suspend fun removeItemFromList(listId: String, contentId: String)
    suspend fun getListIdsForContent(contentId: String): List<String>
    fun observeContentsForList(listId: String): Flow<List<ContentItem>>
}

class CustomListRepositoryImpl(
    private val customListDao: CustomListDao
) : CustomListRepository {

    override fun observeAll(): Flow<List<CustomList>> {
        return customListDao.observeAllWithItemCount().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: String): CustomList? {
        return customListDao.getById(id)?.let { entity ->
            val listIds = customListDao.getAll()
            CustomList(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                itemCount = 0 // Will be populated in detailed query if needed
            )
        }
    }

    override suspend fun getAll(): List<CustomList> {
        return customListDao.getAll().map { entity ->
            CustomList(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }

    override suspend fun create(name: String): CustomList {
        val now = System.currentTimeMillis()
        val entity = CustomListEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            createdAt = now,
            updatedAt = now
        )
        customListDao.insert(entity)
        return CustomList(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            itemCount = 0
        )
    }

    override suspend fun rename(id: String, newName: String) {
        val existing = customListDao.getById(id) ?: return
        val updated = existing.copy(
            name = newName.trim(),
            updatedAt = System.currentTimeMillis()
        )
        customListDao.update(updated)
    }

    override suspend fun delete(id: String) {
        customListDao.deleteById(id)
    }

    override suspend fun addItemToList(listId: String, contentId: String) {
        customListDao.addContentToList(
            ListItemCrossRef(listId = listId, contentId = contentId, addedAt = System.currentTimeMillis())
        )
    }

    override suspend fun removeItemFromList(listId: String, contentId: String) {
        customListDao.removeContentFromList(listId = listId, contentId = contentId)
    }

    override suspend fun getListIdsForContent(contentId: String): List<String> {
        return customListDao.getListIdsForContent(contentId)
    }

    override fun observeContentsForList(listId: String): Flow<List<ContentItem>> {
        return customListDao.observeContentsForList(listId).map { list ->
            list.map { it.toDomain() }
        }
    }

    private fun CustomListWithCount.toDomain(): CustomList {
        return CustomList(
            id = list.id,
            name = list.name,
            createdAt = list.createdAt,
            updatedAt = list.updatedAt,
            itemCount = itemCount
        )
    }

    private fun ContentWithDetails.toDomain(): ContentItem {
        return ContentItem(
            id = content.id,
            title = content.title,
            contentType = ContentType.fromString(content.contentType),
            posterPath = content.posterPath,
            externalUrl = content.externalUrl,
            description = content.description,
            watchStatus = WatchStatus.fromString(content.watchStatus),
            categories = categories.map { Category(it.id, it.name, it.createdAt) },
            listIds = lists.map { it.id },
            createdAt = content.createdAt,
            updatedAt = content.updatedAt
        )
    }
}
