package com.vp.whattowatch.ui.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vp.whattowatch.data.repository.CustomListRepository
import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.domain.model.CustomList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListDetailUiState(
    val customList: CustomList? = null,
    val items: List<ContentItem> = emptyList(),
    val isLoading: Boolean = true
)

class ListDetailViewModel(
    private val listId: String,
    private val customListRepository: CustomListRepository
) : ViewModel() {

    private val _listInfo = MutableStateFlow<CustomList?>(null)

    init {
        viewModelScope.launch {
            _listInfo.value = customListRepository.getById(listId)
        }
    }

    val uiState: StateFlow<ListDetailUiState> = combine(
        _listInfo,
        customListRepository.observeContentsForList(listId)
    ) { list, items ->
        ListDetailUiState(
            customList = list,
            items = items,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ListDetailUiState(isLoading = true)
    )

    companion object {
        fun provideFactory(
            listId: String,
            customListRepository: CustomListRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ListDetailViewModel(listId, customListRepository) as T
            }
        }
    }
}
