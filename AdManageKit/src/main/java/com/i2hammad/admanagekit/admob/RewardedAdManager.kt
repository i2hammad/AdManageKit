package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manager for Rewarded Ads using GMA Next-Gen SDK Preloader.
 *
 * This implementation uses RewardedAdPreloader for efficient ad loading and caching.
 */
object RewardedAdManager {
    private const val TAG = "RewardedAdManager"

    private lateinit var adUnitId: String
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private val preloaderActive = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    interface OnAdDismissedListener {
        fun onAdDismissed()
    }

    /**
     * Initialize the RewardedAdManager and start preloading.
     *
     * @param context Application or Activity context
     * @param adUnitId The rewarded ad unit ID
     */
    fun initialize(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        startPreloading()
    }

    /**
     * Start the RewardedAdPreloader for this ad unit.
     * The preloader will automatically keep ads ready in the background.
     */
    fun startPreloading() {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            Log.d(TAG, "User has purchased, skipping preloader start.")
            return
        }

        if (!::adUnitId.isInitialized) {
            Log.e(TAG, "Ad unit ID not initialized. Call initialize() first.")
            return
        }

        if (preloaderActive.get()) {
            Log.d(TAG, "Preloader already active for $adUnitId")
            return
        }

        Log.d(TAG, "Starting RewardedAdPreloader for $adUnitId")

        // Note: GMA Next-Gen SDK calls these callbacks on background threads,
        // so we must switch to main thread for any UI operations.
        val preloadCallback = object : PreloadCallback {
            override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                Log.d(TAG, "Rewarded ad preloaded for $adUnitId")
                AdDebugUtils.logEvent(adUnitId, "onPreloaded", "Rewarded ad preloaded", true)
            }

            override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                Log.e(TAG, "Rewarded ad failed to preload for $adUnitId: ${adError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToPreload", "Rewarded ad failed to preload: ${adError.message}", false)

                // Log Firebase event
                firebaseAnalytics?.let { analytics ->
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                        putString("ad_error_code", adError.code.toString())
                        putString("error_message", adError.message)
                    }
                    analytics.logEvent("ad_failed_to_load", params)
                }
            }

            override fun onAdsExhausted(preloadId: String) {
                Log.d(TAG, "Rewarded ads exhausted for $adUnitId")
                AdDebugUtils.logEvent(adUnitId, "onAdsExhausted", "Rewarded ads exhausted", false)
            }
        }

        val adRequest = AdRequest.Builder(adUnitId).build()
        val preloadConfig = PreloadConfiguration(adRequest)

        RewardedAdPreloader.start(adUnitId, preloadConfig, preloadCallback)
        preloaderActive.set(true)

        Log.d(TAG, "RewardedAdPreloader started for $adUnitId")
    }

    /**
     * Stop tracking the preloader (preloader continues in background until app termination).
     */
    fun stopPreloading() {
        if (!preloaderActive.get()) {
            Log.d(TAG, "Preloader not active for $adUnitId")
            return
        }

        preloaderActive.set(false)
        Log.d(TAG, "RewardedAdPreloader tracking stopped for $adUnitId")
    }

    /**
     * Show a rewarded ad if available.
     *
     * @param activity The activity to show the ad from
     * @param onUserEarnedRewardListener Callback when user earns a reward
     * @param onAdDismissedListener Callback when ad is dismissed
     */
    fun showAd(
        activity: Activity,
        onUserEarnedRewardListener: OnUserEarnedRewardListener,
        onAdDismissedListener: OnAdDismissedListener
    ) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            Log.d(TAG, "User has purchased, not showing ad.")
            mainHandler.post { onAdDismissedListener.onAdDismissed() }
            return
        }

        if (!::adUnitId.isInitialized) {
            Log.e(TAG, "Ad unit ID not initialized. Call initialize() first.")
            mainHandler.post { onAdDismissedListener.onAdDismissed() }
            return
        }

        // Poll ad from preloader
        val rewardedAd = RewardedAdPreloader.pollAd(adUnitId)

        if (rewardedAd == null) {
            Log.d(TAG, "No rewarded ad available")
            AdDebugUtils.logEvent(adUnitId, "noAdAvailable", "No rewarded ad available to show", false)
            mainHandler.post { onAdDismissedListener.onAdDismissed() }
            return
        }

        Log.d(TAG, "Showing rewarded ad from preloader")
        AdDebugUtils.logEvent(adUnitId, "showingAd", "Showing rewarded ad from preloader", true)

        // Note: GMA Next-Gen SDK calls these callbacks on background threads,
        // so we must switch to main thread for UI operations and user callbacks.
        rewardedAd.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdClicked() {
                Log.d(TAG, "Rewarded ad was clicked.")
                AdDebugUtils.logEvent(adUnitId, "onAdClicked", "Rewarded ad clicked", true)
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed fullscreen content.")
                AdDebugUtils.logEvent(adUnitId, "onAdDismissed", "Rewarded ad dismissed", true)

                // Switch to main thread for callback
                mainHandler.post {
                    onAdDismissedListener.onAdDismissed()
                }
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.e(TAG, "Rewarded ad failed to show fullscreen content: ${fullScreenContentError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToShow", "Rewarded ad failed to show: ${fullScreenContentError.message}", false)

                // Switch to main thread for callback
                mainHandler.post {
                    onAdDismissedListener.onAdDismissed()
                }
            }

            override fun onAdImpression() {
                Log.d(TAG, "Rewarded ad recorded an impression.")
                AdDebugUtils.logEvent(adUnitId, "onAdImpression", "Rewarded ad impression", true)

                firebaseAnalytics?.let { analytics ->
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    }
                    analytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                }
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed fullscreen content.")
                AdDebugUtils.logEvent(adUnitId, "onAdShowed", "Rewarded ad showed fullscreen", true)
            }

            override fun onAdPaid(value: AdValue) {
                super.onAdPaid(value)

                val adValueInStandardUnits = value.valueMicros / 1_000_000.0

                AdDebugUtils.logEvent(adUnitId, "onAdPaid", "Rewarded ad paid: $adValueInStandardUnits ${value.currencyCode}", true)

                firebaseAnalytics?.let { analytics ->
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                        putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                        putString(FirebaseAnalytics.Param.CURRENCY, value.currencyCode)
                    }
                    analytics.logEvent("ad_paid_event", params)
                }
            }
        }

        // Show the ad
        rewardedAd.show(activity, onUserEarnedRewardListener)
    }

    /**
     * Check if a rewarded ad is available to show.
     *
     * @return true if an ad is available and user hasn't purchased
     */
    fun isAdLoaded(): Boolean {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            return false
        }

        if (!::adUnitId.isInitialized) {
            return false
        }

        return RewardedAdPreloader.isAdAvailable(adUnitId)
    }

    /**
     * Check if the preloader is currently active.
     */
    fun isPreloaderActive(): Boolean = preloaderActive.get()

    // =================== DEPRECATED METHODS (for backward compatibility) ===================

    /**
     * @deprecated Use startPreloading() instead. The preloader handles loading automatically.
     */
    @Deprecated(
        message = "Use startPreloading() instead. The preloader handles loading automatically.",
        replaceWith = ReplaceWith("startPreloading()")
    )
    fun loadRewardedAd(context: Context) {
        startPreloading()
    }
}
