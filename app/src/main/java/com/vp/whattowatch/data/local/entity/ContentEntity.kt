package com.vp.whattowatch.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contents",
    indices = [
        Index(value = ["title"]),
        Index(value = ["watchStatus"]),
        Index(value = ["contentType"]),
        Index(value = ["updatedAt"])
    ]
)
data class ContentEntity(
    @PrimaryKey
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
