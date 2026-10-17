package com.vp.whattowatch.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "content_category_cross_ref",
    primaryKeys = ["contentId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = ContentEntity::class,
            parentColumns = ["id"],
            childColumns = ["contentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contentId"]),
        Index(value = ["categoryId"])
    ]
)
data class ContentCategoryCrossRef(
    val contentId: String,
    val categoryId: String
)
