package com.vp.whattowatch.ui.editor

import androidx.annotation.StringRes
import com.vp.whattowatch.domain.model.Category
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.CustomList
import com.vp.whattowatch.domain.model.WatchStatus

data class ContentEditorUiState(
    val contentId: String? = null,
    val title: String = "",
    val contentType: ContentType = ContentType.MOVIE,
    val posterPath: String? = null,
    val isProcessingPoster: Boolean = false,
    val externalUrl: String = "",
    val description: String = "",
    val watchStatus: WatchStatus = WatchStatus.NOT_WATCHED,
    val selectedCategoryIds: Set<String> = emptySet(),
    val selectedListIds: Set<String> = emptySet(),
    val availableCategories: List<Category> = emptyList(),
    val availableLists: List<CustomList> = emptyList(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    @param:StringRes val titleErrorRes: Int? = null,
    val showNewCategoryDialog: Boolean = false,
    val newCategoryName: String = "",
    val saveCompleted: Boolean = false
) {
    val isValid: Boolean
        get() = title.trim().isNotBlank()

    val titleError: String?
        get() = if (titleErrorRes != null) "error" else null
}
