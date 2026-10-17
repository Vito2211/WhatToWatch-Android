package com.vp.whattowatch.domain.model

enum class ContentType {
    MOVIE,
    SERIES,
    VIDEO,
    OTHER;

    companion object {
        fun fromString(value: String?): ContentType =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: OTHER
    }
}
