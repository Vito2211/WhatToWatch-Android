package com.vp.whattowatch.ui

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.vp.whattowatch.data.backup.BackupService
import com.vp.whattowatch.data.backup.ConflictResolutionStrategy
import com.vp.whattowatch.data.backup.RestoreMode
import com.vp.whattowatch.data.local.AppDatabase
import com.vp.whattowatch.data.local.image.ImageStorageManager
import com.vp.whattowatch.data.preference.LanguagePreferenceManager
import com.vp.whattowatch.testutil.MainDispatcherRule
import com.vp.whattowatch.ui.settings.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var imageStorageManager: ImageStorageManager
    private lateinit var backupService: BackupService
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        imageStorageManager = ImageStorageManager(app)
        backupService = BackupService(
            context = app,
            database = db,
            contentDao = db.contentDao(),
            categoryDao = db.categoryDao(),
            customListDao = db.customListDao(),
            imageStorageManager = imageStorageManager
        )
        viewModel = SettingsViewModel(app, backupService)
    }

    @After
    fun tearDown() {
        db.close()
        imageStorageManager.postersDir.deleteRecursively()
    }

    @Test
    fun testLanguageSelectionUpdatesStateAndPreferences() {
        assertFalse(viewModel.uiState.value.showLanguageDialog)

        viewModel.onOpenLanguageDialog()
        assertTrue(viewModel.uiState.value.showLanguageDialog)

        viewModel.onLanguageSelected(LanguagePreferenceManager.LANGUAGE_RUSSIAN)
        assertEquals(LanguagePreferenceManager.LANGUAGE_RUSSIAN, viewModel.uiState.value.currentLanguage)
        assertFalse(viewModel.uiState.value.showLanguageDialog)

        // Verify persisted preference
        val stored = LanguagePreferenceManager.getStoredLanguage(app)
        assertEquals(LanguagePreferenceManager.LANGUAGE_RUSSIAN, stored)
    }

    @Test
    fun testRestoreModeAndStrategySelection() {
        assertEquals(RestoreMode.MERGE, viewModel.uiState.value.selectedRestoreMode)
        assertEquals(
            ConflictResolutionStrategy.KEEP_BOTH_IMPORT_AS_COPY,
            viewModel.uiState.value.selectedConflictStrategy
        )

        viewModel.onRestoreModeSelected(RestoreMode.REPLACE)
        assertEquals(RestoreMode.REPLACE, viewModel.uiState.value.selectedRestoreMode)

        viewModel.onConflictStrategySelected(ConflictResolutionStrategy.OVERWRITE_EXISTING)
        assertEquals(
            ConflictResolutionStrategy.OVERWRITE_EXISTING,
            viewModel.uiState.value.selectedConflictStrategy
        )
    }

    @Test
    fun testStatusAndErrorMessageClearing() {
        assertNull(viewModel.uiState.value.statusMessage)
        assertNull(viewModel.uiState.value.errorMessage)

        viewModel.clearStatusMessage()
        viewModel.clearErrorMessage()
        assertNull(viewModel.uiState.value.statusMessage)
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
