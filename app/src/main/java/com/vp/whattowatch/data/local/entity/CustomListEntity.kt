package com.vp.whattowatch.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_lists",
    indices = [
        Index(value = ["name"])
    ]
)
data class CustomListEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)
