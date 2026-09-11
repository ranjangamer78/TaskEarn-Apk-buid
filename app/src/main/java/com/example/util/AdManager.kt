package com.example.util

import android.app.Activity
import android.util.Log

object AdManager {
    /**
     * Unity Ads is the exclusive ad network for this application.
     * All other ad networks (including AdMob) have been removed.
     */
    private var lastInterstitialTime: Long = System.currentTimeMillis()
    private var interstitialIntervalMinutes: Int = 5 // default 5 minutes

    fun setIntervalMinutes(minutes: Int) {
        if (minutes >= 1) {
            interstitialIntervalMinutes = minutes
        }
    }

    fun getIntervalMinutes(): Int = interstitialIntervalMinutes

    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
        UnityAdsManager.showRewardedAd(
            activity = activity,
            onComplete = onRewarded,
            onFailed = {
                Log.w("AdManager", "Unity rewarded ad failed to load/show or was closed early")
                onFailed()
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onComplete: () -> Unit = {}) {
        UnityAdsManager.showInterstitialAd(
            activity = activity,
            onComplete = {
                lastInterstitialTime = System.currentTimeMillis()
                onComplete()
            },
            onFailed = {
                Log.w("AdManager", "Unity interstitial ad failed to load/show")
                lastInterstitialTime = System.currentTimeMillis()
                onComplete()
            }
        )
    }

    /**
     * Checks if the configured interval (e.g. 5 minutes) has elapsed since the last interstitial.
     * If yes, displays an interstitial ad and resets the timer.
     */
    fun checkAndShowTimedInterstitial(activity: Activity, onComplete: () -> Unit = {}) {
        val now = System.currentTimeMillis()
        val intervalMs = interstitialIntervalMinutes * 60 * 1000L
        if (now - lastInterstitialTime >= intervalMs) {
            Log.d("AdManager", "5-min interstitial interval reached ($interstitialIntervalMinutes min). Showing interstitial ad.")
            showInterstitialAd(activity, onComplete)
        } else {
            onComplete()
        }
    }
}

