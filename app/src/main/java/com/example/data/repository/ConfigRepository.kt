package com.example.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class AppConfig(
    val daily_bonus: Int = 100,
    val referral_referrer: Int = 5000,
    val referral_referee: Int = 100,
    val video: Int = 20,
    val video_reward: Int = 20,
    val video_limit: Int = 10,
    val video_cooldown_minutes: Int = 5,
    val spin_reward: Int = 10,
    val spin_min_reward: Int = 1,
    val spin_max_reward: Int = 20,
    val spin_limit: Int = 10,
    val spin_cooldown_minutes: Int = 5,
    val scratch_reward: Int = 10,
    val scratch_min_reward: Int = 1,
    val scratch_max_reward: Int = 20,
    val scratch_limit: Int = 10,
    val scratch_cooldown_minutes: Int = 5,
    val game_daily_limit: Int = 10,
    val game_cooldown_minutes: Int = 5,
    val rate: Int = 100, // 100 coins = ₹1 -> 1,000 coins = ₹10
    val min_withdraw: Int = 10, // ₹10 = 1,000 coins
    val unity_id: String = "5846818",
    val privacy_policy: String = "https://example.com/privacy",
    val terms: String = "https://example.com/terms",
    val daily_streak_coins: List<Int> = listOf(10, 20, 30, 40, 50, 60, 100),
    val telegram_link: String = "",
    val youtube_link: String = "",
    val instagram_link: String = "",
    val whatsapp_link: String = "",
    val tiktok_link: String = "",
    val facebook_link: String = "",
    val support_email: String = "",
    val app_download_link: String = "",
    val unity_placement_id: String = "Rewarded_Android",
    val unity_interstitial_placement: String = "Interstitial_Android",
    val unity_banner_placement: String = "Banner_Android",
    val interstitial_interval_minutes: Int = 5,
    val ads_enabled: Boolean = true,
    val unity_ads_enabled: Boolean = true,
    val admob_ads_enabled: Boolean = false,
    val home_banner_url: String = "",
    val home_banner_target: String = "",
    val app_update_enabled: Boolean = false,
    val app_update_title: String = "New Update Available!",
    val app_update_desc: String = "A new version of the app is available with more games, high-reward tasks, and improvements. Please update now to continue earning coins.",
    val app_update_version_code: Int = 1,
    val app_update_version_name: String = "1.0",
    val app_update_link: String = "",
    val app_update_cancelable: Boolean = false,
    val esewa_min_rupees: Int = 100,
    val esewa_max_rupees: Int = 500,
    val ff_diamonds_100_rupees: Int = 115,
    val unity_test_mode: Boolean = false
)

class ConfigRepository {
    private val db = FirebaseFirestore.getInstance()
    
    fun getConfigFlow(): Flow<AppConfig> = callbackFlow {
        val listener = db.collection("settings").document("config")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppConfig())
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val config = snapshot.toObject(AppConfig::class.java) ?: AppConfig()
                    trySend(config)
                } else {
                    trySend(AppConfig())
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun getConfig(): AppConfig {
        return try {
            val doc = db.collection("settings").document("config").get().await()
            doc.toObject(AppConfig::class.java) ?: AppConfig()
        } catch (e: Exception) {
            AppConfig()
        }
    }
}
