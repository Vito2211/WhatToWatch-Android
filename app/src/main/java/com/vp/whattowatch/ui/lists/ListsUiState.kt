package com.vp.whattowatch.ui.lists

import com.vp.whattowatch.domain.model.CustomList

data class ListsUiState(
    val lists: List<CustomList> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val newListName: String = "",
    val listToRename: CustomList? = null,
    val renameListName: String = "",
    val listToDelete: CustomList? = null,
    val isProcessing: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && lists.isEmpty()
}
