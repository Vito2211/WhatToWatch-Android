package com.vp.whattowatch.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vp.whattowatch.data.repository.CategoryRepository
import com.vp.whattowatch.data.repository.ContentRepository
import com.vp.whattowatch.data.repository.CustomListRepository
import com.vp.whattowatch.domain.model.ContentFilter
import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.WatchStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val contentRepository: ContentRepository,
    private val categoryRepository: CategoryRepository,
    private val customListRepository: CustomListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val currentFilter = MutableStateFlow(ContentFilter())

    init {
        viewModelScope.launch {
            currentFilter.flatMapLatest { filter ->
                contentRepository.observeAll(filter)
            }.collect { items ->
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }

        viewModelScope.launch {
            categoryRepository.observeAll().collect { categories ->
                _uiState.update { it.copy(availableCategories = categories) }
            }
        }

        viewModelScope.launch {
            customListRepository.observeAll().collect { lists ->
                _uiState.update { it.copy(availableLists = lists) }
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        updateFilter()
    }

    fun onStatusFilterSelected(status: WatchStatus?) {
        val nextStatus = if (_uiState.value.selectedStatus == status) null else status
        _uiState.update { it.copy(selectedStatus = nextStatus) }
        updateFilter()
    }

    fun onCategoryFilterSelected(categoryId: String?) {
        val nextCat = if (_uiState.value.selectedCategoryId == categoryId) null else categoryId
        _uiState.update { it.copy(selectedCategoryId = nextCat) }
        updateFilter()
    }

    fun onListFilterSelected(listId: String?) {
        val nextList = if (_uiState.value.selectedListId == listId) null else listId
        _uiState.update { it.copy(selectedListId = nextList) }
        updateFilter()
    }

    fun onClearFilters() {
        _uiState.update {
            it.copy(
                query = "",
                selectedStatus = null,
                selectedCategoryId = null,
                selectedListId = null
            )
        }
        updateFilter()
    }

    private fun updateFilter() {
        val state = _uiState.value
        currentFilter.value = ContentFilter(
            query = state.query,
            watchStatus = state.selectedStatus,
            categoryId = state.selectedCategoryId,
            listId = state.selectedListId
        )
    }

    fun onOpenQuickAdd() {
        _uiState.update { it.copy(showQuickAddSheet = true) }
    }

    fun onDismissQuickAdd() {
        if (!_uiState.value.isSavingQuickAdd) {
            _uiState.update { it.copy(showQuickAddSheet = false) }
        }
    }

    fun onSaveQuickAdd(title: String, onSuccess: () -> Unit = {}) {
        val trimmed = title.trim()
        if (trimmed.isBlank() || _uiState.value.isSavingQuickAdd) return

        _uiState.update { it.copy(isSavingQuickAdd = true) }
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val newItem = ContentItem(
                    id = UUID.randomUUID().toString(),
                    title = trimmed,
                    contentType = ContentType.MOVIE,
                    watchStatus = WatchStatus.NOT_WATCHED,
                    createdAt = now,
                    updatedAt = now
                )
                contentRepository.save(newItem)
                _uiState.update { it.copy(showQuickAddSheet = false) }
                onSuccess()
            } finally {
                _uiState.update { it.copy(isSavingQuickAdd = false) }
            }
        }
    }

    companion object {
        fun provideFactory(
            contentRepository: ContentRepository,
            categoryRepository: CategoryRepository,
            customListRepository: CustomListRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(
                    contentRepository,
                    categoryRepository,
                    customListRepository
                ) as T
            }
        }
    }
}
