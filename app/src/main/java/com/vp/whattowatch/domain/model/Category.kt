package com.vp.whattowatch.domain.model

data class Category(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
