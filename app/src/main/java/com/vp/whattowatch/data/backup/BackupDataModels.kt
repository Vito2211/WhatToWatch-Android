package com.vp.whattowatch.data.backup

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class BackupManifest(
    val version: Int = CURRENT_VERSION,
    val appName: String = "What to Watch",
    val appVersion: String = "1.0",
    val createdAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0,
    val listCount: Int = 0,
    val categoryCount: Int = 0,
    val imageCount: Int = 0
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupContentItem(
    val id: String,
    val title: String,
    val contentType: String,
    val posterPath: String? = null,
    val externalUrl: String? = null,
    val description: String? = null,
    val watchStatus: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupCategory(
    val id: String,
    val name: String,
    val createdAt: Long
)

@Serializable
data class BackupCustomList(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BackupContentCategoryRef(
    val contentId: String,
    val categoryId: String
)

@Serializable
data class BackupListItemRef(
    val listId: String,
    val contentId: String
)

@Serializable
data class BackupLibraryPayload(
    val contents: List<BackupContentItem> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val customLists: List<BackupCustomList> = emptyList(),
    val contentCategories: List<BackupContentCategoryRef> = emptyList(),
    val listItems: List<BackupListItemRef> = emptyList()
)

enum class ConflictReason {
    SAME_ID,
    SAME_TITLE_AND_TYPE
}

data class RestoreConflict(
    val backupItemId: String,
    val backupTitle: String,
    val backupContentType: String,
    val existingItemId: String,
    val existingTitle: String,
    val conflictReason: ConflictReason
)

enum class RestoreMode {
    MERGE,
    REPLACE
}

enum class ConflictResolutionStrategy {
    SKIP_EXISTING,
    OVERWRITE_EXISTING,
    KEEP_BOTH_IMPORT_AS_COPY
}

data class RestorePreview(
    val isValid: Boolean,
    val manifest: BackupManifest?,
    val payload: BackupLibraryPayload?,
    val imageFileCount: Int,
    val validationErrors: List<String> = emptyList(),
    val conflicts: List<RestoreConflict> = emptyList(),
    val stagingDir: File? = null
)

sealed class BackupResult {
    data class Success(val manifest: BackupManifest, val fileSizeBytes: Long) : BackupResult()
    data class Error(val message: String, val throwable: Throwable? = null) : BackupResult()
}

sealed class RestoreResult {
    data class Success(val restoredItemsCount: Int, val mode: RestoreMode) : RestoreResult()
    data class Error(val message: String, val throwable: Throwable? = null) : RestoreResult()
}
