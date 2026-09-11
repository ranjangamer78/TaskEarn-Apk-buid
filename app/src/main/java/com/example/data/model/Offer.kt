package com.example.data.model

data class Offer(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val reward: Int = 0,
    val url: String = "",
    val timer: Int = 0,
    val frequency: String = "ONCE", // "ONCE" (1 Day Only / One-time), "DAILY" (Daily repeat), "REPEATABLE"
    val maxCompletions: Int = 1,     // How many times user can complete: 1 for ONCE, or N times per day for DAILY
    val isActive: Boolean = true,
    val hideWhenCompleted: Boolean = true, // If true, hide after completion unless reset
    val resetVersion: Long = 0L,     // When admin resets offer, this timestamp is updated so all users can do it again
    val category: String = "General",
    val timestamp: Long = 0L
) {
    fun isDaily(): Boolean = frequency.equals("DAILY", ignoreCase = true)
    fun isOnce(): Boolean = frequency.equals("ONCE", ignoreCase = true) || frequency.isBlank()
    fun isRepeatable(): Boolean = frequency.equals("REPEATABLE", ignoreCase = true)
    fun getEffectiveMaxCompletions(): Int = if (maxCompletions > 0) maxCompletions else 1
}

data class UserTaskCompletion(
    val offerId: String = "",
    val totalCompletions: Int = 0,
    val dailyCompletions: Int = 0,
    val lastCompletedDate: String = "",
    val lastCompletedTimestamp: Long = 0L,
    val resetVersion: Long = 0L
)
