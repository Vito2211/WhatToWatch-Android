package com.vp.whattowatch

import android.app.Application
import android.content.Context
import com.vp.whattowatch.data.backup.BackupService
import com.vp.whattowatch.data.local.AppDatabase
import com.vp.whattowatch.data.local.image.ImageStorageManager
import com.vp.whattowatch.data.repository.CategoryRepository
import com.vp.whattowatch.data.repository.CategoryRepositoryImpl
import com.vp.whattowatch.data.repository.ContentRepository
import com.vp.whattowatch.data.repository.ContentRepositoryImpl
import com.vp.whattowatch.data.repository.CustomListRepository
import com.vp.whattowatch.data.repository.CustomListRepositoryImpl

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)
    val imageStorageManager: ImageStorageManager = ImageStorageManager(context)
    val contentRepository: ContentRepository = ContentRepositoryImpl(
        contentDao = database.contentDao(),
        imageStorageManager = imageStorageManager
    )
    val customListRepository: CustomListRepository = CustomListRepositoryImpl(database.customListDao())
    val categoryRepository: CategoryRepository = CategoryRepositoryImpl(database.categoryDao())
    val backupService: BackupService = BackupService(
        context = context,
        database = database,
        contentDao = database.contentDao(),
        categoryDao = database.categoryDao(),
        customListDao = database.customListDao(),
        imageStorageManager = imageStorageManager
    )
    val sharingService: com.vp.whattowatch.data.sharing.SharingService = com.vp.whattowatch.data.sharing.SharingService(
        context = context,
        database = database,
        contentDao = database.contentDao(),
        categoryDao = database.categoryDao(),
        customListDao = database.customListDao()
    )
}

class WhatToWatchApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
