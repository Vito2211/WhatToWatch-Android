package com.vp.whattowatch.ui.library

import com.vp.whattowatch.domain.model.Category
import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.domain.model.CustomList
import com.vp.whattowatch.domain.model.WatchStatus

data class LibraryUiState(
    val items: List<ContentItem> = emptyList(),
    val isLoading: Boolean = true,
    val query: String = "",
    val selectedStatus: WatchStatus? = null,
    val selectedCategoryId: String? = null,
    val selectedListId: String? = null,
    val availableCategories: List<Category> = emptyList(),
    val availableLists: List<CustomList> = emptyList(),
    val showQuickAddSheet: Boolean = false,
    val isSavingQuickAdd: Boolean = false,
    val errorMessage: String? = null
) {
    val isFilterActive: Boolean
        get() = query.isNotBlank() || selectedStatus != null || selectedCategoryId != null || selectedListId != null

    val isLibraryEmpty: Boolean
        get() = !isLoading && items.isEmpty() && !isFilterActive
}
