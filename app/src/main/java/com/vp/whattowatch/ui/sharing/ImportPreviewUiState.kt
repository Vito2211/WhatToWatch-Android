package com.vp.whattowatch.ui.sharing

import com.vp.whattowatch.data.sharing.ImportConflictResolution
import com.vp.whattowatch.data.sharing.ImportPreview

data class ImportPreviewUiState(
    val isLoading: Boolean = true,
    val preview: ImportPreview? = null,
    val selectedItemIds: Set<String> = emptySet(),
    val conflictResolution: ImportConflictResolution = ImportConflictResolution.SKIP_EXISTING,
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val importSuccessCount: Int? = null
) {
    val canImport: Boolean
        get() = preview != null && preview.isValid && selectedItemIds.isNotEmpty() && !isImporting

    val areAllSelected: Boolean
        get() {
            val total = preview?.payload?.contents?.size ?: 0
            return total > 0 && selectedItemIds.size == total
        }

    val selectedCount: Int
        get() = selectedItemIds.size
}
