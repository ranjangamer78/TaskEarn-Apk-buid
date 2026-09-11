package com.example.data.model

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val icon: String = "bell",
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null
)
