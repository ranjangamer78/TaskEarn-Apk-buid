package com.example.data.model

data class PageNotice(
    val id: String = "",
    val pageId: String = "all", // "all", "home", "earn", "wallet", "spin", "scratch", "video", "tasks", "refer", "games", "leaderboard", "profile", "history"
    val title: String = "",
    val message: String = "",
    val type: String = "warning", // "warning", "error", "info", "success"
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
