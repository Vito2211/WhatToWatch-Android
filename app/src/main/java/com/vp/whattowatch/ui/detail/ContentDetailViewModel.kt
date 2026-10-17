package com.vp.whattowatch.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vp.whattowatch.data.repository.ContentRepository
import com.vp.whattowatch.data.repository.CustomListRepository
import com.vp.whattowatch.domain.model.WatchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContentDetailViewModel(
    private val contentId: String,
    private val contentRepository: ContentRepository,
    private val customListRepository: CustomListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContentDetailUiState(isLoading = true))
    val uiState: StateFlow<ContentDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            contentRepository.observeById(contentId).collect { item ->
                _uiState.update { it.copy(contentItem = item, isLoading = false) }
            }
        }

        viewModelScope.launch {
            customListRepository.observeAll().collect { lists ->
                _uiState.update { it.copy(allCustomLists = lists) }
            }
        }
    }

    fun onUpdateWatchStatus(newStatus: WatchStatus) {
        viewModelScope.launch {
            contentRepository.updateWatchStatus(contentId, newStatus)
        }
    }

    fun onOpenDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun onDeleteContent(onSuccess: () -> Unit) {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }

        viewModelScope.launch {
            try {
                contentRepository.delete(contentId)
                _uiState.update { it.copy(showDeleteDialog = false) }
                onSuccess()
            } finally {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    fun onOpenManageListsDialog() {
        _uiState.update { it.copy(showManageListsDialog = true) }
    }

    fun onDismissManageListsDialog() {
        _uiState.update { it.copy(showManageListsDialog = false) }
    }

    fun onToggleListMembership(listId: String) {
        val currentItem = _uiState.value.contentItem ?: return
        viewModelScope.launch {
            if (currentItem.listIds.contains(listId)) {
                customListRepository.removeItemFromList(listId, contentId)
            } else {
                customListRepository.addItemToList(listId, contentId)
            }
        }
    }

    companion object {
        fun provideFactory(
            contentId: String,
            contentRepository: ContentRepository,
            customListRepository: CustomListRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContentDetailViewModel(
                    contentId,
                    contentRepository,
                    customListRepository
                ) as T
            }
        }
    }
}
