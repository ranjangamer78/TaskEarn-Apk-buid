package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.navigation.NavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.util.UnityAdsManager
import com.example.ui.components.NetworkStatusOverlay
import androidx.lifecycle.lifecycleScope
import com.example.data.repository.ConfigRepository
import com.example.util.AdManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val configRepository = ConfigRepository()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
          NetworkStatusOverlay {
            NavGraph()
          }
        }
      }
    }

    // Initialize ads safely in background coroutine so it never hangs UI or causes startup crash
    lifecycleScope.launch {
      try {
        delay(1500L)
        UnityAdsManager.initialize(this@MainActivity)
      } catch (e: Throwable) {
        android.util.Log.w("MainActivity", "UnityAdsManager init error: ${e.message}")
      }
    }

    // Listen to remote AppConfig updates (from Admin Panel) safely
    lifecycleScope.launch {
      try {
        delay(2000L)
        configRepository.getConfigFlow().collect { config ->
          try {
            UnityAdsManager.updateConfig(
              newGameId = config.unity_id,
              newPlacementId = config.unity_placement_id,
              newInterstitialId = config.unity_interstitial_placement,
              newBannerId = config.unity_banner_placement,
              newTestMode = config.unity_test_mode
            )
            AdManager.setIntervalMinutes(config.interstitial_interval_minutes)
          } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "Error applying config updates: ${e.message}")
          }
        }
      } catch (e: Throwable) {
        android.util.Log.w("MainActivity", "Config flow collection error: ${e.message}")
      }
    }

    // Periodic check for timed interstitial ad
    lifecycleScope.launch {
      try {
        delay(60_000L)
        while (true) {
          delay(30_000L)
          if (!isFinishing && !isDestroyed) {
            AdManager.checkAndShowTimedInterstitial(this@MainActivity)
          }
        }
      } catch (e: Throwable) {
        android.util.Log.w("MainActivity", "Timed interstitial loop error: ${e.message}")
      }
    }
  }
}
