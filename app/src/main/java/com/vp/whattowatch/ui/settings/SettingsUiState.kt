package com.vp.whattowatch.ui.settings

import com.vp.whattowatch.data.backup.ConflictResolutionStrategy
import com.vp.whattowatch.data.backup.RestoreMode
import com.vp.whattowatch.data.backup.RestorePreview

data class SettingsUiState(
    val currentLanguage: String = "",
    val isExporting: Boolean = false,
    val isInspecting: Boolean = false,
    val isRestoring: Boolean = false,
    val restorePreview: RestorePreview? = null,
    val selectedRestoreMode: RestoreMode = RestoreMode.MERGE,
    val selectedConflictStrategy: ConflictResolutionStrategy = ConflictResolutionStrategy.KEEP_BOTH_IMPORT_AS_COPY,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val showLanguageDialog: Boolean = false
) {
    val isBusy: Boolean
        get() = isExporting || isInspecting || isRestoring
}
