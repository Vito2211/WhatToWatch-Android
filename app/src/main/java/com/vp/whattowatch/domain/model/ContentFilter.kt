package com.vp.whattowatch.domain.model

data class ContentFilter(
    val query: String = "",
    val watchStatus: WatchStatus? = null,
    val contentType: ContentType? = null,
    val categoryId: String? = null,
    val listId: String? = null
) {
    val isActive: Boolean
        get() = query.isNotBlank() || watchStatus != null || contentType != null || categoryId != null || listId != null
}
