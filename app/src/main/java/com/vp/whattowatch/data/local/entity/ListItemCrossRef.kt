package com.vp.whattowatch.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "list_item_cross_ref",
    primaryKeys = ["listId", "contentId"],
    foreignKeys = [
        ForeignKey(
            entity = CustomListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ContentEntity::class,
            parentColumns = ["id"],
            childColumns = ["contentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["listId"]),
        Index(value = ["contentId"])
    ]
)
data class ListItemCrossRef(
    val listId: String,
    val contentId: String,
    val addedAt: Long = System.currentTimeMillis()
)
