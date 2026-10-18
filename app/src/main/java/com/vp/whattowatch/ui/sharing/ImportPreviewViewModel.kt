package com.vp.whattowatch.ui.sharing

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vp.whattowatch.data.sharing.ImportConflictResolution
import com.vp.whattowatch.data.sharing.ImportResult
import com.vp.whattowatch.data.sharing.SharePayload
import com.vp.whattowatch.data.sharing.SharingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ImportPreviewViewModel(
    application: Application,
    private val sharingService: SharingService,
    private val deepLink: String?,
    private val fileUri: String?,
    private val rawPayload: String?
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ImportPreviewUiState(isLoading = true))
    val uiState: StateFlow<ImportPreviewUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadPayload()
    }

    private fun loadPayload() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val payload: SharePayload = when {
                    !deepLink.isNullOrBlank() -> {
                        sharingService.decodeFromDeepLink(Uri.parse(deepLink))
                    }

                    !fileUri.isNullOrBlank() -> {
                        val stream = app.contentResolver.openInputStream(Uri.parse(fileUri))
                            ?: throw IllegalArgumentException("Cannot open file stream for $fileUri")
                        stream.use { sharingService.decodeFromStream(it) }
                    }

                    !rawPayload.isNullOrBlank() -> {
                        val trimmed = rawPayload.trim()
                        when {
                            trimmed.startsWith("{") -> {
                                json.decodeFromString(SharePayload.serializer(), trimmed)
                            }
                            trimmed.startsWith("whattowatch://") || trimmed.startsWith("https://") || trimmed.startsWith("http://") -> {
                                sharingService.decodeFromDeepLink(Uri.parse(trimmed))
                            }
                            else -> {
                                val urlMatch = Regex("""(https?://[^\s]+|whattowatch://[^\s]+)""").find(trimmed)
                                if (urlMatch != null) {
                                    sharingService.decodeFromDeepLink(Uri.parse(urlMatch.value))
                                } else {
                                    sharingService.decodeAndDecompress(trimmed)
                                }
                            }
                        }
                    }

                    else -> throw IllegalArgumentException("No import source provided.")
                }

                val preview = sharingService.previewImport(payload)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        preview = preview,
                        selectedItemIds = payload.contents.map { item -> item.id }.toSet()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: e.message ?: "Failed to load shared content."
                    )
                }
            }
        }
    }

    fun onToggleItem(contentId: String) {
        _uiState.update { current ->
            val updated = current.selectedItemIds.toMutableSet()
            if (updated.contains(contentId)) {
                updated.remove(contentId)
            } else {
                updated.add(contentId)
            }
            current.copy(selectedItemIds = updated)
        }
    }

    fun onSelectAll() {
        val allIds = _uiState.value.preview?.payload?.contents?.map { it.id }?.toSet() ?: emptySet()
        _uiState.update { it.copy(selectedItemIds = allIds) }
    }

    fun onDeselectAll() {
        _uiState.update { it.copy(selectedItemIds = emptySet()) }
    }

    fun onConflictResolutionChanged(resolution: ImportConflictResolution) {
        _uiState.update { it.copy(conflictResolution = resolution) }
    }

    fun confirmImport(onSuccess: (count: Int) -> Unit) {
        val preview = _uiState.value.preview ?: return
        val selectedIds = _uiState.value.selectedItemIds
        val resolution = _uiState.value.conflictResolution

        if (selectedIds.isEmpty()) return

        _uiState.update { it.copy(isImporting = true) }
        viewModelScope.launch {
            val result = sharingService.executeImport(preview, selectedIds, resolution)
            when (result) {
                is ImportResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importSuccessCount = result.importedCount
                        )
                    }
                    onSuccess(result.importedCount)
                }

                is ImportResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            application: Application,
            sharingService: SharingService,
            deepLink: String?,
            fileUri: String?,
            rawPayload: String?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ImportPreviewViewModel(application, sharingService, deepLink, fileUri, rawPayload) as T
            }
        }
    }
}
