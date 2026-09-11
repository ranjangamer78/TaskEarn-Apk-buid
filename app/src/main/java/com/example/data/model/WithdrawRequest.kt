package com.example.data.model

data class WithdrawRequest(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val method: String = "",
    val coins: Int = 0,
    val rupees: Int = 0,
    val details: String = "",
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
)
