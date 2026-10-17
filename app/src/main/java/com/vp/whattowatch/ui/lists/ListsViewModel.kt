package com.vp.whattowatch.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vp.whattowatch.data.repository.CustomListRepository
import com.vp.whattowatch.domain.model.CustomList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListsViewModel(
    private val customListRepository: CustomListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState(isLoading = true))
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            customListRepository.observeAll().collect { lists ->
                _uiState.update { it.copy(lists = lists, isLoading = false) }
            }
        }
    }

    fun onOpenCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, newListName = "") }
    }

    fun onDismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, newListName = "") }
    }

    fun onNewListNameChanged(name: String) {
        _uiState.update { it.copy(newListName = name) }
    }

    fun onCreateList() {
        val name = _uiState.value.newListName.trim()
        if (name.isBlank() || _uiState.value.isProcessing) return

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                customListRepository.create(name)
                _uiState.update { it.copy(showCreateDialog = false, newListName = "") }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun onOpenRenameDialog(list: CustomList) {
        _uiState.update { it.copy(listToRename = list, renameListName = list.name) }
    }

    fun onDismissRenameDialog() {
        _uiState.update { it.copy(listToRename = null, renameListName = "") }
    }

    fun onRenameListNameChanged(name: String) {
        _uiState.update { it.copy(renameListName = name) }
    }

    fun onRenameList() {
        val list = _uiState.value.listToRename ?: return
        val newName = _uiState.value.renameListName.trim()
        if (newName.isBlank() || _uiState.value.isProcessing) return

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                customListRepository.rename(list.id, newName)
                _uiState.update { it.copy(listToRename = null, renameListName = "") }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun onOpenDeleteDialog(list: CustomList) {
        _uiState.update { it.copy(listToDelete = list) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(listToDelete = null) }
    }

    fun onDeleteList() {
        val list = _uiState.value.listToDelete ?: return
        if (_uiState.value.isProcessing) return

        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                customListRepository.delete(list.id)
                _uiState.update { it.copy(listToDelete = null) }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    companion object {
        fun provideFactory(
            customListRepository: CustomListRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ListsViewModel(customListRepository) as T
            }
        }
    }
}
