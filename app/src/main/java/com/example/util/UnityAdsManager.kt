package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

object UnityAdsManager {
    private var gameId = "5846818"
    private var adUnitId = "Rewarded_Android"
    private var interstitialUnitId = "Interstitial_Android"
    private var bannerUnitId = "Banner_Android"
    private var _isInitialized = false
    private var testMode = true
    private val initListeners = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()

    fun getGameId(): String = gameId
    fun getRewardedPlacementId(): String = adUnitId
    fun getInterstitialPlacementId(): String = interstitialUnitId
    fun getBannerPlacementId(): String = bannerUnitId
    fun isTestMode(): Boolean = testMode
    fun isInitialized(): Boolean = _isInitialized || try { UnityAds.isInitialized } catch (e: Throwable) { false }

    fun addInitializationListener(listener: () -> Unit) {
        if (isInitialized()) {
            listener()
        } else {
            initListeners.add(listener)
        }
    }

    fun updateConfig(
        newGameId: String?,
        newPlacementId: String?,
        newInterstitialId: String? = null,
        newBannerId: String? = null,
        newTestMode: Boolean? = null
    ) {
        if (!newGameId.isNullOrBlank()) {
            gameId = newGameId.trim()
        }
        if (!newPlacementId.isNullOrBlank()) {
            adUnitId = newPlacementId.trim()
        }
        if (!newInterstitialId.isNullOrBlank()) {
            interstitialUnitId = newInterstitialId.trim()
        }
        if (!newBannerId.isNullOrBlank()) {
            bannerUnitId = newBannerId.trim()
        }
        if (newTestMode != null) {
            testMode = newTestMode
        }
    }

    fun initialize(context: Context, customGameId: String? = null, isTest: Boolean? = null) {
        if (!customGameId.isNullOrBlank()) {
            gameId = customGameId.trim()
        }
        if (isTest != null) {
            testMode = isTest
        }
        if (_isInitialized) return
        try {
            UnityAds.initialize(context.applicationContext, gameId, testMode, object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    _isInitialized = true
                    Log.d("UnityAds", "Initialization Complete with Game ID: $gameId (testMode=$testMode)")
                    val listeners = ArrayList(initListeners)
                    initListeners.clear()
                    listeners.forEach {
                        try {
                            it.invoke()
                        } catch (e: Throwable) {
                            Log.e("UnityAds", "Init listener error: ${e.message}")
                        }
                    }
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError?,
                    message: String?
                ) {
                    Log.e("UnityAds", "Initialization Failed: $message")
                }
            })
        } catch (e: Throwable) {
            Log.e("UnityAds", "UnityAds.initialize crashed: ${e.message}")
        }
    }

    fun showRewardedAd(activity: Activity, onComplete: () -> Unit, onFailed: () -> Unit) {
        if (!isInitialized()) {
            initialize(activity)
        }

        UnityAds.load(adUnitId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                val currentUnit = placementId ?: adUnitId
                UnityAds.show(activity, currentUnit, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        placementId: String?,
                        error: UnityAds.UnityAdsShowError?,
                        message: String?
                    ) {
                        Log.e("UnityAds", "Show Failure: $message")
                        onFailed()
                    }

                    override fun onUnityAdsShowStart(placementId: String?) {}
                    override fun onUnityAdsShowClick(placementId: String?) {}

                    override fun onUnityAdsShowComplete(
                        placementId: String?,
                        state: UnityAds.UnityAdsShowCompletionState?
                    ) {
                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            onComplete()
                        } else {
                            onFailed()
                        }
                    }
                })
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                Log.e("UnityAds", "Load Failure: $message")
                onFailed()
            }
        })
    }

    fun showInterstitialAd(activity: Activity, onComplete: () -> Unit, onFailed: () -> Unit = onComplete) {
        if (!isInitialized()) {
            initialize(activity)
        }

        val placement = interstitialUnitId
        UnityAds.load(placement, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String?) {
                val targetPlacement = placementId ?: placement
                UnityAds.show(activity, targetPlacement, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        placementId: String?,
                        error: UnityAds.UnityAdsShowError?,
                        message: String?
                    ) {
                        Log.w("UnityAds", "Interstitial show failed: $message")
                        onFailed()
                    }

                    override fun onUnityAdsShowStart(placementId: String?) {}
                    override fun onUnityAdsShowClick(placementId: String?) {}

                    override fun onUnityAdsShowComplete(
                        placementId: String?,
                        state: UnityAds.UnityAdsShowCompletionState?
                    ) {
                        Log.d("UnityAds", "Interstitial completed / closed: $state")
                        onComplete()
                    }
                })
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String?,
                error: UnityAds.UnityAdsLoadError?,
                message: String?
            ) {
                Log.w("UnityAds", "Interstitial load failed: $message. Falling back to rewarded placement or completing.")
                // Fallback to rewarded placement if interstitial placement is not configured
                if (placement != adUnitId) {
                    UnityAds.load(adUnitId, object : IUnityAdsLoadListener {
                        override fun onUnityAdsAdLoaded(placementId: String?) {
                            UnityAds.show(activity, placementId ?: adUnitId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                                override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) = onFailed()
                                override fun onUnityAdsShowStart(placementId: String?) {}
                                override fun onUnityAdsShowClick(placementId: String?) {}
                                override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) = onComplete()
                            })
                        }
                        override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) = onFailed()
                    })
                } else {
                    onFailed()
                }
            }
        })
    }
}
