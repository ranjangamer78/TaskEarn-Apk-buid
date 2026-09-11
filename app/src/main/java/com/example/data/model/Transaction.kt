package com.example.data.model

data class Transaction(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Int = 0,
    val type: String = "credit", // credit or debit
    val timestamp: Long = System.currentTimeMillis(),
    val icon: String = "star",
    val status: String = "success", // success, pending
    val details: String = "" // payment details if withdraw
)
