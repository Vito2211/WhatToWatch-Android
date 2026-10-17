package com.vp.whattowatch.ui.detail

import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.domain.model.CustomList

data class ContentDetailUiState(
    val contentItem: ContentItem? = null,
    val allCustomLists: List<CustomList> = emptyList(),
    val isLoading: Boolean = true,
    val showDeleteDialog: Boolean = false,
    val showManageListsDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false
)
