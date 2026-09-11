package com.example.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val balance: Int = 0,
    val streak: Int = 0,
    val joinedAt: Long = System.currentTimeMillis(),
    val lastClaimedAt: Long = 0L,
    val videosWatchedToday: Int = 0,
    val lastVideoWatchedAt: Long = 0L,
    val spinsToday: Int = 0,
    val lastSpinAt: Long = 0L,
    val scratchesToday: Int = 0,
    val lastScratchAt: Long = 0L,
    val gamesPlayedToday: Int = 0,
    val lastGamePlayedAt: Long = 0L,
    val referredBy: String = "",
    val referralCode: String = "",
    val lastLogin: Long = 0L,
    val invitedCount: Int = 0,
    val deviceId: String = "",
    val hideNameOnLeaderboard: Boolean = false,
    val blockReason: String = "",
    @get:PropertyName("isBlocked")
    @set:PropertyName("isBlocked")
    var isBlocked: Boolean = false
)
