package com.vp.whattowatch.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vp.whattowatch.data.local.image.ImageStorageManager
import com.vp.whattowatch.data.repository.CategoryRepository
import com.vp.whattowatch.data.repository.ContentRepository
import com.vp.whattowatch.data.repository.CustomListRepository
import com.vp.whattowatch.domain.model.Category
import com.vp.whattowatch.domain.model.ContentItem
import com.vp.whattowatch.domain.model.ContentType
import com.vp.whattowatch.domain.model.WatchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ContentEditorViewModel(
    private val contentId: String?,
    initialTitle: String?,
    private val contentRepository: ContentRepository,
    private val categoryRepository: CategoryRepository,
    private val customListRepository: CustomListRepository,
    private val imageStorageManager: ImageStorageManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ContentEditorUiState(
            contentId = contentId,
            title = initialTitle ?: "",
            isEditMode = contentId != null,
            isLoading = contentId != null
        )
    )
    val uiState: StateFlow<ContentEditorUiState> = _uiState.asStateFlow()

    private var existingCreatedAt: Long = System.currentTimeMillis()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load categories and custom lists
            val categories = categoryRepository.observeAll().first()
            val lists = customListRepository.observeAll().first()

            _uiState.update {
                it.copy(
                    availableCategories = categories,
                    availableLists = lists
                )
            }

            // If editing, load the item
            if (contentId != null) {
                val item = contentRepository.getById(contentId)
                if (item != null) {
                    existingCreatedAt = item.createdAt
                    _uiState.update {
                        it.copy(
                            title = item.title,
                            contentType = item.contentType,
                            posterPath = item.posterPath,
                            externalUrl = item.externalUrl ?: "",
                            description = item.description ?: "",
                            watchStatus = item.watchStatus,
                            selectedCategoryIds = item.categories.map { cat -> cat.id }.toSet(),
                            selectedListIds = item.listIds.toSet(),
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update {
            it.copy(
                title = newTitle,
                titleErrorRes = if (newTitle.trim().isEmpty()) com.vp.whattowatch.R.string.field_title_required else null
            )
        }
    }

    fun onContentTypeSelected(type: ContentType) {
        _uiState.update { it.copy(contentType = type) }
    }

    fun onPosterSelected(uri: Uri) {
        val storage = imageStorageManager ?: return
        _uiState.update { it.copy(isProcessingPoster = true) }
        viewModelScope.launch {
            val relativeFilename = storage.saveImageFromUri(uri)
            _uiState.update {
                it.copy(
                    posterPath = relativeFilename ?: it.posterPath,
                    isProcessingPoster = false
                )
            }
        }
    }

    fun onRemovePoster() {
        _uiState.update { it.copy(posterPath = null) }
    }

    fun onExternalUrlChanged(newUrl: String) {
        _uiState.update { it.copy(externalUrl = newUrl) }
    }

    fun onDescriptionChanged(newDesc: String) {
        _uiState.update { it.copy(description = newDesc) }
    }

    fun onWatchStatusSelected(status: WatchStatus) {
        _uiState.update { it.copy(watchStatus = status) }
    }

    fun onToggleCategory(categoryId: String) {
        _uiState.update { current ->
            val updated = current.selectedCategoryIds.toMutableSet()
            if (updated.contains(categoryId)) {
                updated.remove(categoryId)
            } else {
                updated.add(categoryId)
            }
            current.copy(selectedCategoryIds = updated)
        }
    }

    fun onToggleList(listId: String) {
        _uiState.update { current ->
            val updated = current.selectedListIds.toMutableSet()
            if (updated.contains(listId)) {
                updated.remove(listId)
            } else {
                updated.add(listId)
            }
            current.copy(selectedListIds = updated)
        }
    }

    fun onOpenNewCategoryDialog() {
        _uiState.update { it.copy(showNewCategoryDialog = true, newCategoryName = "") }
    }

    fun onDismissNewCategoryDialog() {
        _uiState.update { it.copy(showNewCategoryDialog = false, newCategoryName = "") }
    }

    fun onNewCategoryNameChanged(name: String) {
        _uiState.update { it.copy(newCategoryName = name) }
    }

    fun onCreateCategory() {
        val name = _uiState.value.newCategoryName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            val created = categoryRepository.getOrCreate(name)
            val updatedCategories = categoryRepository.getAll()
            _uiState.update { current ->
                current.copy(
                    availableCategories = updatedCategories,
                    selectedCategoryIds = current.selectedCategoryIds + created.id,
                    showNewCategoryDialog = false,
                    newCategoryName = ""
                )
            }
        }
    }

    fun saveContent(onSuccess: () -> Unit) {
        val state = _uiState.value
        val trimmedTitle = state.title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(titleErrorRes = com.vp.whattowatch.R.string.field_title_required) }
            return
        }
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val id = state.contentId ?: UUID.randomUUID().toString()
                val assignedCategories = state.availableCategories.filter {
                    state.selectedCategoryIds.contains(it.id)
                }

                val item = ContentItem(
                    id = id,
                    title = trimmedTitle,
                    contentType = state.contentType,
                    posterPath = state.posterPath,
                    externalUrl = state.externalUrl.trim().ifBlank { null },
                    description = state.description.trim().ifBlank { null },
                    watchStatus = state.watchStatus,
                    categories = assignedCategories,
                    listIds = state.selectedListIds.toList(),
                    createdAt = if (state.isEditMode) existingCreatedAt else now,
                    updatedAt = now
                )

                contentRepository.save(item)
                _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
                onSuccess()
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    companion object {
        fun provideFactory(
            contentId: String?,
            initialTitle: String?,
            contentRepository: ContentRepository,
            categoryRepository: CategoryRepository,
            customListRepository: CustomListRepository,
            imageStorageManager: ImageStorageManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContentEditorViewModel(
                    contentId,
                    initialTitle,
                    contentRepository,
                    categoryRepository,
                    customListRepository,
                    imageStorageManager
                ) as T
            }
        }
    }
}
