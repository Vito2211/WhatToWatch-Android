package com.vp.whattowatch.data.local.entity

import androidx.room.Embedded

data class CustomListWithCount(
    @Embedded
    val list: CustomListEntity,
    val itemCount: Int
)
