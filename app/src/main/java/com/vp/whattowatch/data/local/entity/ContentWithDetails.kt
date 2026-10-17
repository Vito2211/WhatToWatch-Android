package com.vp.whattowatch.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ContentWithDetails(
    @Embedded
    val content: ContentEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ContentCategoryCrossRef::class,
            parentColumn = "contentId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<CategoryEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ListItemCrossRef::class,
            parentColumn = "contentId",
            entityColumn = "listId"
        )
    )
    val lists: List<CustomListEntity> = emptyList()
)
