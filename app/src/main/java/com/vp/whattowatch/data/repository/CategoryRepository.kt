package com.vp.whattowatch.data.repository

import com.vp.whattowatch.data.local.dao.CategoryDao
import com.vp.whattowatch.data.local.entity.CategoryEntity
import com.vp.whattowatch.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    suspend fun getAll(): List<Category>
    suspend fun getOrCreate(name: String): Category
    suspend fun delete(id: String)
}

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> {
        return categoryDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAll(): List<Category> {
        return categoryDao.getAll().map { it.toDomain() }
    }

    override suspend fun getOrCreate(name: String): Category {
        val trimmed = name.trim()
        val existing = categoryDao.getByName(trimmed)
        if (existing != null) {
            return existing.toDomain()
        }
        val entity = CategoryEntity(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            createdAt = System.currentTimeMillis()
        )
        categoryDao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun delete(id: String) {
        categoryDao.deleteById(id)
    }

    private fun CategoryEntity.toDomain(): Category {
        return Category(
            id = id,
            name = name,
            createdAt = createdAt
        )
    }
}
