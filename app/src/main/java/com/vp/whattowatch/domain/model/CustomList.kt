package com.vp.whattowatch.domain.model

data class CustomList(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val itemCount: Int = 0
)
