package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.UnityAdsManager
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    if (activity == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val currentActivity = ctx.findActivity() ?: activity
                val frameLayout = FrameLayout(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                try {
                    UnityAdsManager.initialize(currentActivity)
                    val placementId = UnityAdsManager.getBannerPlacementId()
                    val bannerView = BannerView(currentActivity, placementId, UnityBannerSize(320, 50))

                    bannerView.layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }

                    bannerView.listener = object : BannerView.IListener {
                        override fun onBannerLoaded(bannerAdView: BannerView?) {
                            Log.d("BannerAdView", "Unity Banner loaded for placement: $placementId")
                        }

                        override fun onBannerFailedToLoad(
                            bannerAdView: BannerView?,
                            errorInfo: BannerErrorInfo?
                        ) {
                            Log.w("BannerAdView", "Unity Banner failed to load: ${errorInfo?.errorMessage}. Retrying in 5s...")
                            bannerView.postDelayed({
                                try {
                                    if (currentActivity != null && !currentActivity.isFinishing && !currentActivity.isDestroyed) {
                                        bannerView.load()
                                    }
                                } catch (e: Throwable) {
                                    Log.e("BannerAdView", "Banner retry error: ${e.message}")
                                }
                            }, 5000L)
                        }

                        override fun onBannerShown(bannerAdView: BannerView?) {
                            Log.d("BannerAdView", "Unity Banner displayed successfully")
                        }

                        override fun onBannerClick(bannerAdView: BannerView?) {}
                        override fun onBannerLeftApplication(bannerAdView: BannerView?) {}
                    }

                    frameLayout.addView(bannerView)

                    // Load banner when Unity Ads is initialized
                    UnityAdsManager.addInitializationListener {
                        currentActivity.runOnUiThread {
                            try {
                                if (!currentActivity.isFinishing && !currentActivity.isDestroyed) {
                                    bannerView.load()
                                }
                            } catch (e: Throwable) {
                                Log.e("BannerAdView", "Error loading banner on init: ${e.message}")
                            }
                        }
                    }

                    if (UnityAdsManager.isInitialized()) {
                        bannerView.load()
                    }
                } catch (e: Throwable) {
                    Log.e("BannerAdView", "Error initializing Unity Banner: ${e.message}")
                }

                frameLayout
            },
            onRelease = { frameLayout ->
                try {
                    for (i in 0 until frameLayout.childCount) {
                        val child = frameLayout.getChildAt(i)
                        if (child is BannerView) {
                            child.destroy()
                        }
                    }
                    frameLayout.removeAllViews()
                } catch (e: Throwable) {
                    Log.e("BannerAdView", "Error releasing banner: ${e.message}")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )
    }
}
