package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.utils.AdRetryManager

/**
 * RewardedAdManager is a singleton class responsible for managing rewarded ads
 * using Google AdMob. It provides functionality to load and show ads, handle
 * rewards, and manage ad-related callbacks with Firebase Analytics integration.
 *
 * Features:
 * - Automatic retry with exponential backoff on load failures
 * - Purchase status integration (ads disabled for premium users)
 * - Timeout support for splash screen scenarios
 * - Detailed Firebase Analytics tracking (requests, fills, impressions)
 * - Configurable auto-reload after ad dismissal
 * - Debug utilities integration
 *
 * Example:
 * ```kotlin
 * // Initialize once
 * RewardedAdManager.initialize(context, "ca-app-pub-xxx/xxx")
 *
 * // Show when ready
 * if (RewardedAdManager.isAdLoaded()) {
 *     RewardedAdManager.showAd(activity, object : RewardedAdCallback {
 *         override fun onRewardEarned(type: String, amount: Int) {
 *             // Grant reward
 *         }
 *         override fun onAdDismissed() {
 *             // Continue flow
 *         }
 *     })
 * }
 * ```
 */
object RewardedAdManager {
    private var rewardedAd: RewardedAd? = null
    private var isLoading: Boolean = false
    private var isShowingAd: Boolean = false
    private lateinit var adUnitId: String
    private const val TAG = "RewardedAdManager"

    private var firebaseAnalytics: FirebaseAnalytics? = null

    // Retry tracking
    private var retryAttempts: Int = 0

    // Analytics counters
    private var sessionAdRequests = 0
    private var sessionAdFills = 0
    private var sessionAdImpressions = 0

    /**
     * Callback interface for rewarded ad events.
     * Provides granular control over ad lifecycle.
     */
    interface RewardedAdCallback {
        /**
         * Called when the user earns a reward.
         * @param rewardType The type of reward (e.g., "coins")
         * @param rewardAmount The amount of reward
         */
        fun onRewardEarned(rewardType: String, rewardAmount: Int)

        /**
         * Called when the ad is dismissed (after reward or skip).
         */
        fun onAdDismissed()

        /**
         * Called when the ad is shown successfully.
         */
        fun onAdShowed() {}

        /**
         * Called when the ad fails to show.
         * @param error The error that occurred
         */
        fun onAdFailedToShow(error: com.google.android.gms.ads.AdError) {}

        /**
         * Called when the ad is clicked.
         */
        fun onAdClicked() {}
    }

    /**
     * Callback interface for ad loading events.
     */
    interface OnRewardedAdLoadCallback {
        /**
         * Called when the ad is loaded successfully.
         */
        fun onAdLoaded()

        /**
         * Called when the ad fails to load.
         * @param error The load error
         */
        fun onAdFailedToLoad(error: LoadAdError)
    }

    /**
     * Legacy callback interface for backward compatibility.
     */
    @Deprecated("Use RewardedAdCallback instead", ReplaceWith("RewardedAdCallback"))
    interface OnAdDismissedListener {
        fun onAdDismissed()
    }

    /**
     * Initialize the RewardedAdManager with the given ad unit ID.
     * Automatically starts loading an ad.
     *
     * @param context The context
     * @param adUnitId The AdMob rewarded ad unit ID
     */
    fun initialize(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId
        initializeFirebase(context)
        loadRewardedAd(context)
    }

    private fun initializeFirebase(context: Context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }

    /**
     * Load a rewarded ad.
     * Automatically skips if:
     * - Ad is already loading
     * - Ad is already loaded
     * - User has purchased premium (ads disabled)
     *
     * @param context The context
     */
    fun loadRewardedAd(context: Context) {
        // Guard: Skip loading for premium users
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            AdDebugUtils.logEvent(adUnitId, "skipLoad", "Skipping ad load - user is premium", true)
            return
        }

        // Guard: Prevent duplicate concurrent load requests
        if (isLoading) {
            Log.d(TAG, "Ad already loading, skipping duplicate request")
            AdDebugUtils.logEvent(adUnitId, "skipDuplicateLoad", "Ad already loading", true)
            return
        }

        // Guard: Skip if ad is already loaded
        if (rewardedAd != null) {
            Log.d(TAG, "Ad already loaded, skipping load request")
            AdDebugUtils.logEvent(adUnitId, "skipAlreadyLoaded", "Ad already loaded", true)
            return
        }

        isLoading = true
        initializeFirebase(context)

        // Cancel any pending retry since we're manually loading
        AdRetryManager.getInstance().cancelRetry(adUnitId)
        retryAttempts = 0

        // Log ad request for analytics
        logAdRequest()

        val adRequest = AdRequest.Builder().build()

        AdDebugUtils.logEvent(adUnitId, "loadStarted", "Starting rewarded ad load", true)

        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isLoading = false
                rewardedAd = null
                Log.d(TAG, "Ad failed to load: ${adError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Rewarded ad failed: ${adError.message}", false)

                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", "${adError.code}")
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", adError.message)
                    }
                }
                firebaseAnalytics?.logEvent("ad_failed_to_load", params)

                // Attempt automatic retry if enabled
                if (AdManageKitConfig.autoRetryFailedAds && shouldAttemptRetry()) {
                    retryAttempts++
                    AdRetryManager.getInstance().scheduleRetry(
                        adUnitId = adUnitId,
                        attempt = retryAttempts - 1,
                        maxAttempts = AdManageKitConfig.maxRetryAttempts
                    ) {
                        loadRewardedAd(context)
                    }
                }
            }

            override fun onAdLoaded(ad: RewardedAd) {
                isLoading = false
                rewardedAd = ad
                retryAttempts = 0 // Reset retry count on success
                Log.d(TAG, "Ad was loaded.")
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Rewarded ad loaded successfully", true)

                // Log ad fill for analytics
                logAdFill()
            }
        })
    }

    /**
     * Load a rewarded ad with callback.
     *
     * @param context The context
     * @param callback Callback for load events
     */
    fun loadRewardedAd(context: Context, callback: OnRewardedAdLoadCallback) {
        // Guard: Skip loading for premium users
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onAdFailedToLoad(
                LoadAdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN,
                    null,
                    null
                )
            )
            return
        }

        // Guard: Prevent duplicate concurrent load requests
        if (isLoading) {
            Log.d(TAG, "Ad already loading, skipping duplicate request")
            return
        }

        // Guard: If already loaded, return success immediately
        if (rewardedAd != null) {
            callback.onAdLoaded()
            return
        }

        isLoading = true
        initializeFirebase(context)
        logAdRequest()

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isLoading = false
                rewardedAd = null
                Log.d(TAG, "Ad failed to load: ${adError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Rewarded ad failed: ${adError.message}", false)

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", "${adError.code}")
                }
                firebaseAnalytics?.logEvent("ad_failed_to_load", params)

                callback.onAdFailedToLoad(adError)
            }

            override fun onAdLoaded(ad: RewardedAd) {
                isLoading = false
                rewardedAd = ad
                retryAttempts = 0
                Log.d(TAG, "Ad was loaded.")
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Rewarded ad loaded with callback", true)
                logAdFill()

                callback.onAdLoaded()
            }
        })
    }

    /**
     * Load a rewarded ad with timeout support.
     * Useful for splash screens where you want to proceed after a timeout.
     *
     * @param context The context
     * @param timeoutMillis Maximum time to wait for ad load
     * @param callback Callback for load events (called once - either on load, fail, or timeout)
     */
    fun loadRewardedAdWithTimeout(
        context: Context,
        timeoutMillis: Long = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds,
        callback: OnRewardedAdLoadCallback
    ) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onAdFailedToLoad(
                LoadAdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN,
                    null,
                    null
                )
            )
            return
        }

        // If already loaded, return immediately
        if (rewardedAd != null) {
            callback.onAdLoaded()
            return
        }

        if (isLoading) {
            Log.d(TAG, "Ad already loading, waiting for result")
        }

        isLoading = true
        initializeFirebase(context)
        logAdRequest()

        var callbackCalled = false

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isLoading = false
                rewardedAd = null

                if (!callbackCalled) {
                    callbackCalled = true
                    Log.d(TAG, "Ad failed to load: ${adError.message}")
                    AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Rewarded ad failed with timeout: ${adError.message}", false)

                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                        putString("ad_error_code", "${adError.code}")
                    }
                    firebaseAnalytics?.logEvent("ad_failed_to_load", params)

                    callback.onAdFailedToLoad(adError)
                }
            }

            override fun onAdLoaded(ad: RewardedAd) {
                isLoading = false
                rewardedAd = ad
                retryAttempts = 0

                if (!callbackCalled) {
                    callbackCalled = true
                    Log.d(TAG, "Ad was loaded within timeout.")
                    AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Rewarded ad loaded within timeout", true)
                    logAdFill()
                    callback.onAdLoaded()
                } else {
                    // Ad loaded after timeout - saved for next use
                    AdDebugUtils.logEvent(adUnitId, "onAdLoadedAfterTimeout", "Ad saved for next show", true)
                }
            }
        })

        // Timeout handler
        Handler(Looper.getMainLooper()).postDelayed({
            if (!callbackCalled && isLoading) {
                callbackCalled = true
                isLoading = false
                Log.d(TAG, "Ad loading timed out")
                AdDebugUtils.logEvent(adUnitId, "onTimeout", "Rewarded ad loading timed out", false)

                callback.onAdFailedToLoad(
                    LoadAdError(
                        -1,
                        "Ad loading timed out",
                        "com.i2hammad.admanagekit",
                        null,
                        null
                    )
                )
            }
        }, timeoutMillis)
    }

    /**
     * Show the rewarded ad with full callback support.
     *
     * @param activity The activity to show the ad
     * @param callback Callback for ad events including reward
     * @param autoReload Whether to automatically reload after dismissal (default: true)
     */
    fun showAd(
        activity: Activity,
        callback: RewardedAdCallback,
        autoReload: Boolean = AdManageKitConfig.rewardedAutoReload
    ) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            AdDebugUtils.logEvent(adUnitId, "skipShow", "Skipping ad show - user is premium", true)
            callback.onAdDismissed()
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            AdDebugUtils.logEvent(adUnitId, "showFailed", "No ad loaded to show", false)
            loadRewardedAd(activity)
            callback.onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.d(TAG, "Ad was clicked.")
                AdDebugUtils.logEvent(adUnitId, "onAdClicked", "Rewarded ad clicked", true)
                callback.onAdClicked()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed fullscreen content.")
                AdDebugUtils.logEvent(adUnitId, "onAdDismissed", "Rewarded ad dismissed", true)
                isShowingAd = false
                rewardedAd = null

                // Log Firebase event for ad dismissed
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics?.logEvent("ad_dismissed", params)

                if (autoReload) {
                    loadRewardedAd(activity)
                }
                callback.onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Ad failed to show fullscreen content: ${adError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToShow", "Rewarded ad failed to show: ${adError.message}", false)
                isShowingAd = false
                rewardedAd = null

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", "${adError.code}")
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", adError.message)
                    }
                }
                firebaseAnalytics?.logEvent("ad_failed_to_show", params)

                if (autoReload) {
                    loadRewardedAd(activity)
                }
                callback.onAdFailedToShow(adError)
                callback.onAdDismissed()
            }

            override fun onAdImpression() {
                Log.d(TAG, "Ad recorded an impression.")
                AdDebugUtils.logEvent(adUnitId, "onAdImpression", "Rewarded ad impression", true)

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)

                logAdImpression()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
                AdDebugUtils.logEvent(adUnitId, "onAdShowed", "Rewarded ad showing", true)
                isShowingAd = true
                callback.onAdShowed()
            }
        }

        ad.onPaidEventListener = OnPaidEventListener { adValue ->
            val adValueInStandardUnits = adValue.valueMicros / 1000000.0

            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            }
            firebaseAnalytics?.logEvent("ad_paid_event", params)
        }

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            AdDebugUtils.logEvent(adUnitId, "onRewardEarned", "Reward: ${rewardItem.amount} ${rewardItem.type}", true)

            // Log reward event to Firebase
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putString("reward_type", rewardItem.type)
                putInt("reward_amount", rewardItem.amount)
            }
            firebaseAnalytics?.logEvent("rewarded_ad_reward", params)

            callback.onRewardEarned(rewardItem.type, rewardItem.amount)
        }
    }

    /**
     * Show the rewarded ad with legacy callback support.
     * @deprecated Use showAd(activity, RewardedAdCallback) instead
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use showAd with RewardedAdCallback", ReplaceWith("showAd(activity, callback)"))
    fun showAd(
        activity: Activity,
        onUserEarnedRewardListener: OnUserEarnedRewardListener,
        onAdDismissedListener: OnAdDismissedListener
    ) {
        showAd(activity, object : RewardedAdCallback {
            override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
                val rewardItem = object : com.google.android.gms.ads.rewarded.RewardItem {
                    override fun getAmount(): Int = rewardAmount
                    override fun getType(): String = rewardType
                }
                onUserEarnedRewardListener.onUserEarnedReward(rewardItem)
            }

            override fun onAdDismissed() {
                onAdDismissedListener.onAdDismissed()
            }
        })
    }

    /**
     * Check if a rewarded ad is loaded and ready to show.
     * Returns false if user has purchased premium.
     *
     * @return true if ad is ready to show
     */
    fun isAdLoaded(): Boolean {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        return rewardedAd != null && !purchaseProvider.isPurchased()
    }

    /**
     * Check if an ad is currently being loaded.
     *
     * @return true if a load request is in progress
     */
    fun isLoading(): Boolean {
        return isLoading
    }

    /**
     * Check if an ad is currently being displayed.
     *
     * @return true if ad is showing
     */
    fun isShowingAd(): Boolean {
        return isShowingAd
    }

    /**
     * Preload a rewarded ad if none is loaded.
     * Call this during natural pauses to improve show rate.
     *
     * @param context The context
     */
    fun preload(context: Context) {
        if (!isAdLoaded() && !isLoading) {
            AdDebugUtils.logEvent(adUnitId, "preload", "Preloading rewarded ad", true)
            loadRewardedAd(context)
        }
    }

    /**
     * Get current ad statistics for debugging.
     *
     * @return Map of statistics
     */
    fun getAdStats(): Map<String, Any> {
        val fillRate = if (sessionAdRequests > 0) {
            (sessionAdFills.toFloat() / sessionAdRequests * 100)
        } else 0f

        val showRate = if (sessionAdFills > 0) {
            (sessionAdImpressions.toFloat() / sessionAdFills * 100)
        } else 0f

        return mapOf(
            "session_requests" to sessionAdRequests,
            "session_fills" to sessionAdFills,
            "session_impressions" to sessionAdImpressions,
            "fill_rate_percent" to fillRate,
            "show_rate_percent" to showRate,
            "is_loaded" to (rewardedAd != null),
            "is_loading" to isLoading,
            "retry_attempts" to retryAttempts
        )
    }

    /**
     * Reset session statistics.
     */
    fun resetAdStats() {
        sessionAdRequests = 0
        sessionAdFills = 0
        sessionAdImpressions = 0
    }

    // =================== PRIVATE HELPERS ===================

    private fun shouldAttemptRetry(): Boolean {
        return retryAttempts < AdManageKitConfig.maxRetryAttempts &&
                !AdRetryManager.getInstance().hasActiveRetry(adUnitId)
    }

    private fun logAdRequest() {
        sessionAdRequests++

        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
            putString("ad_type", "rewarded")
            putLong("session_requests", sessionAdRequests.toLong())
        }
        firebaseAnalytics?.logEvent("ad_request", params)
    }

    private fun logAdFill() {
        sessionAdFills++

        val fillRate = if (sessionAdRequests > 0) {
            (sessionAdFills.toFloat() / sessionAdRequests * 100).toInt()
        } else 0

        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
            putString("ad_type", "rewarded")
            putLong("session_fills", sessionAdFills.toLong())
            putLong("session_requests", sessionAdRequests.toLong())
            putInt("fill_rate_percent", fillRate)
        }
        firebaseAnalytics?.logEvent("ad_fill", params)
    }

    private fun logAdImpression() {
        sessionAdImpressions++

        val showRate = if (sessionAdFills > 0) {
            (sessionAdImpressions.toFloat() / sessionAdFills * 100).toInt()
        } else 0

        val fillRate = if (sessionAdRequests > 0) {
            (sessionAdFills.toFloat() / sessionAdRequests * 100).toInt()
        } else 0

        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
            putString("ad_type", "rewarded")
            putLong("session_impressions", sessionAdImpressions.toLong())
            putLong("session_fills", sessionAdFills.toLong())
            putInt("show_rate_percent", showRate)
            putInt("fill_rate_percent", fillRate)
        }
        firebaseAnalytics?.logEvent("ad_impression_detailed", params)

        // Update user properties for segmentation
        firebaseAnalytics?.setUserProperty("rewarded_ads_shown", sessionAdImpressions.toString())
    }
}
