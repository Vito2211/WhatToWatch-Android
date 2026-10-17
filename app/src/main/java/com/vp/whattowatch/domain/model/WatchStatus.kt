package com.vp.whattowatch.domain.model

enum class WatchStatus {
    NOT_WATCHED,
    WATCHING,
    WATCHED;

    companion object {
        fun fromString(value: String?): WatchStatus =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: NOT_WATCHED
    }
}
