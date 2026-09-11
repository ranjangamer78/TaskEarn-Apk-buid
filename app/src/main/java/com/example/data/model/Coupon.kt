package com.example.data.model

data class Coupon(
    val id: String = "",
    val code: String = "",
    val reward: Int = 0,
    val maxUses: Int = 0, // 0 = unlimited
    val usedCount: Int = 0,
    val isActive: Boolean = true,
    val description: String = "",
    val createdAt: Long = 0L
)
