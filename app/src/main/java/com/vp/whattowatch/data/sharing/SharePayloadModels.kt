package com.vp.whattowatch.data.sharing

import kotlinx.serialization.Serializable

@Serializable
enum class ShareType {
    SINGLE_ITEM,
    CUSTOM_LIST
}

@Serializable
data class ShareContentItem(
    val id: String,
    val title: String,
    val contentType: String,
    val watchStatus: String,
    val externalUrl: String? = null,
    val description: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class ShareCategory(
    val id: String,
    val name: String,
    val createdAt: Long
)

@Serializable
data class ShareCustomList(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class ShareContentCategoryRef(
    val contentId: String,
    val categoryId: String
)

@Serializable
data class ShareListItemRef(
    val listId: String,
    val contentId: String
)

@Serializable
data class SharePayload(
    val version: Int = CURRENT_VERSION,
    val type: ShareType,
    val title: String,
    val contents: List<ShareContentItem> = emptyList(),
    val categories: List<ShareCategory> = emptyList(),
    val customLists: List<ShareCustomList> = emptyList(),
    val contentCategories: List<ShareContentCategoryRef> = emptyList(),
    val listItems: List<ShareListItemRef> = emptyList(),
    val sharedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

enum class ImportConflictReason {
    SAME_ID,
    SAME_TITLE_AND_TYPE
}

data class ImportConflict(
    val incomingItem: ShareContentItem,
    val existingItemId: String,
    val existingItemTitle: String,
    val reason: ImportConflictReason
)

enum class ImportConflictResolution {
    SKIP_EXISTING,
    KEEP_BOTH_AS_COPY,
    OVERWRITE_EXISTING
}

data class ImportPreview(
    val isValid: Boolean,
    val payload: SharePayload?,
    val conflicts: List<ImportConflict> = emptyList(),
    val validationErrors: List<String> = emptyList()
) {
    val totalIncomingCount: Int
        get() = payload?.contents?.size ?: 0
}

sealed class ImportResult {
    data class Success(val importedCount: Int, val skippedCount: Int) : ImportResult()
    data class Error(val message: String, val throwable: Throwable? = null) : ImportResult()
}

sealed class ShareResult {
    data class DeepLink(val url: String, val title: String) : ShareResult()
    data class FileShare(val fileUriString: String, val mimeType: String, val title: String) : ShareResult()
    data class Error(val message: String, val throwable: Throwable? = null) : ShareResult()
}
