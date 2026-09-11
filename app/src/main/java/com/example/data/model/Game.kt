package com.example.data.model

data class Game(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val link: String = "", // Game URL / HTML5 Web game URL
    val coin: Int = 50, // Reward coin
    val second: Int = 60, // Playing countdown time in seconds
    val icon: String = "", // Thumbnail / icon
    val category: String = "Arcade", // Arcade, Puzzle, Action, Casual, Racing, Sports
    val isActive: Boolean = true,
    val playsCount: Long = 0L,
    val timestamp: Long = 0L
)
