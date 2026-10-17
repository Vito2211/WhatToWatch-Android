package com.vp.whattowatch.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vp.whattowatch.R
import com.vp.whattowatch.data.backup.BackupResult
import com.vp.whattowatch.data.backup.BackupService
import com.vp.whattowatch.data.backup.ConflictResolutionStrategy
import com.vp.whattowatch.data.backup.RestoreMode
import com.vp.whattowatch.data.backup.RestoreResult
import com.vp.whattowatch.data.preference.LanguagePreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val backupService: BackupService
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            currentLanguage = LanguagePreferenceManager.getStoredLanguage(application)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onLanguageSelected(tag: String) {
        val app = getApplication<Application>()
        LanguagePreferenceManager.setLanguage(app, tag)
        _uiState.update {
            it.copy(
                currentLanguage = tag,
                showLanguageDialog = false
            )
        }
    }

    fun onOpenLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = true) }
    }

    fun onDismissLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = false) }
    }

    fun exportBackup(uri: Uri) {
        val app = getApplication<Application>()
        _uiState.update { it.copy(isExporting = true, statusMessage = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                val outputStream = app.contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = app.getString(R.string.export_failed, "Cannot write to selected destination.")
                        )
                    }
                    return@launch
                }

                val result = outputStream.use { stream ->
                    backupService.exportBackup(stream)
                }

                when (result) {
                    is BackupResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                statusMessage = app.getString(
                                    R.string.export_success,
                                    result.manifest.itemCount,
                                    result.manifest.listCount,
                                    result.manifest.imageCount
                                )
                            )
                        }
                    }
                    is BackupResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                errorMessage = app.getString(R.string.export_failed, result.message)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = app.getString(R.string.export_failed, e.localizedMessage ?: e.message ?: "Unknown error")
                    )
                }
            }
        }
    }

    fun inspectBackup(uri: Uri) {
        val app = getApplication<Application>()
        _uiState.update { it.copy(isInspecting = true, statusMessage = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                val inputStream = app.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.update {
                        it.copy(
                            isInspecting = false,
                            errorMessage = app.getString(R.string.restore_failed, "Cannot open selected archive.")
                        )
                    }
                    return@launch
                }

                val preview = inputStream.use { stream ->
                    backupService.previewRestore(stream)
                }

                _uiState.update {
                    it.copy(
                        isInspecting = false,
                        restorePreview = preview,
                        selectedRestoreMode = RestoreMode.MERGE,
                        selectedConflictStrategy = ConflictResolutionStrategy.KEEP_BOTH_IMPORT_AS_COPY
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInspecting = false,
                        errorMessage = app.getString(R.string.restore_failed, e.localizedMessage ?: e.message ?: "Unknown error")
                    )
                }
            }
        }
    }

    fun onRestoreModeSelected(mode: RestoreMode) {
        _uiState.update { it.copy(selectedRestoreMode = mode) }
    }

    fun onConflictStrategySelected(strategy: ConflictResolutionStrategy) {
        _uiState.update { it.copy(selectedConflictStrategy = strategy) }
    }

    fun confirmRestore() {
        val app = getApplication<Application>()
        val preview = _uiState.value.restorePreview ?: return
        val mode = _uiState.value.selectedRestoreMode
        val strategy = _uiState.value.selectedConflictStrategy

        _uiState.update { it.copy(isRestoring = true, restorePreview = null) }
        viewModelScope.launch {
            try {
                val result = backupService.executeRestore(preview, mode, strategy)
                when (result) {
                    is RestoreResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isRestoring = false,
                                statusMessage = app.getString(R.string.restore_success, result.restoredItemsCount)
                            )
                        }
                    }
                    is RestoreResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isRestoring = false,
                                errorMessage = app.getString(R.string.restore_failed, result.message)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        errorMessage = app.getString(R.string.restore_failed, e.localizedMessage ?: e.message ?: "Unknown error")
                    )
                }
            }
        }
    }

    fun dismissRestorePreview() {
        val staging = _uiState.value.restorePreview?.stagingDir
        backupService.cleanUpDir(staging)
        _uiState.update { it.copy(restorePreview = null) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        val staging = _uiState.value.restorePreview?.stagingDir
        backupService.cleanUpDir(staging)
    }

    companion object {
        fun provideFactory(
            application: Application,
            backupService: BackupService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(application, backupService) as T
            }
        }
    }
}
