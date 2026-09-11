package com.example.data.model

data class Banner(
    val id: String = "",
    val imageUrl: String = "",
    val link: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
