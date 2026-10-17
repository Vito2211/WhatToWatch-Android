package com.vp.whattowatch.domain.model

data class ContentItem(
    val id: String,
    val title: String,
    val contentType: ContentType = ContentType.MOVIE,
    val posterPath: String? = null,
    val externalUrl: String? = null,
    val description: String? = null,
    val watchStatus: WatchStatus = WatchStatus.NOT_WATCHED,
    val categories: List<Category> = emptyList(),
    val listIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
