package com.vp.whattowatch.data.repository

import com.vp.whattowatch.data.local.dao.ContentDao
import com.vp.whattowatch.data.local.entity.CategoryEntity
import com.vp.whattowatch.data.local.entity.ContentEntity
import com.vp.whattowatch.data.local.entity.ContentWithDetails
import com.vp.whattowatch.data.local.image.ImageStorageManager
import com.vp.whattowatch.domain.model.Category
import com.vp.whattowatch.domain.model.ContentFilter
import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.WatchStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ContentRepository {
    fun observeAll(filter: ContentFilter = ContentFilter()): Flow<List<ContentItem>>
    fun observeById(id: String): Flow<ContentItem?>
    suspend fun getById(id: String): ContentItem?
    suspend fun getAll(): List<ContentItem>
    suspend fun save(item: ContentItem)
    suspend fun updateWatchStatus(id: String, status: WatchStatus)
    suspend fun delete(id: String)
}

class ContentRepositoryImpl(
    private val contentDao: ContentDao,
    private val imageStorageManager: ImageStorageManager? = null
) : ContentRepository {

    override fun observeAll(filter: ContentFilter): Flow<List<ContentItem>> {
        return contentDao.observeAllWithDetails().map { list ->
            list.map { it.toDomain() }.filter { item ->
                matchesFilter(item, filter)
            }
        }
    }

    override fun observeById(id: String): Flow<ContentItem?> {
        return contentDao.observeWithDetailsById(id).map { it?.toDomain() }
    }

    override suspend fun getById(id: String): ContentItem? {
        return contentDao.getWithDetailsById(id)?.toDomain()
    }

    override suspend fun getAll(): List<ContentItem> {
        return contentDao.getAllWithDetails().map { it.toDomain() }
    }

    override suspend fun save(item: ContentItem) {
        val existing = contentDao.getWithDetailsById(item.id)
        val oldPoster = existing?.content?.posterPath
        val newPoster = item.posterPath

        val entity = ContentEntity(
            id = item.id,
            title = item.title.trim(),
            contentType = item.contentType.name,
            posterPath = item.posterPath,
            externalUrl = item.externalUrl?.trim()?.ifBlank { null },
            description = item.description?.trim()?.ifBlank { null },
            watchStatus = item.watchStatus.name,
            createdAt = item.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        contentDao.upsertContentWithRelations(
            content = entity,
            categoryIds = item.categories.map { it.id },
            listIds = item.listIds
        )

        // If poster was replaced, clean up the old file if no other items reference it
        if (!oldPoster.isNullOrBlank() && oldPoster != newPoster && imageStorageManager != null) {
            imageStorageManager.deletePosterSafely(oldPoster, contentDao)
        }
    }

    override suspend fun updateWatchStatus(id: String, status: WatchStatus) {
        contentDao.updateWatchStatus(
            id = id,
            status = status.name,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun delete(id: String) {
        val existing = contentDao.getWithDetailsById(id)
        val oldPoster = existing?.content?.posterPath
        contentDao.deleteById(id)

        // Clean up poster file if no other item references it
        if (!oldPoster.isNullOrBlank() && imageStorageManager != null) {
            imageStorageManager.deletePosterSafely(oldPoster, contentDao)
        }
    }

    private fun matchesFilter(item: ContentItem, filter: ContentFilter): Boolean {
        if (filter.query.isNotBlank() && !item.title.contains(filter.query.trim(), ignoreCase = true)) {
            return false
        }
        if (filter.watchStatus != null && item.watchStatus != filter.watchStatus) {
            return false
        }
        if (filter.contentType != null && item.contentType != filter.contentType) {
            return false
        }
        if (filter.categoryId != null && item.categories.none { it.id == filter.categoryId }) {
            return false
        }
        if (filter.listId != null && !item.listIds.contains(filter.listId)) {
            return false
        }
        return true
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
