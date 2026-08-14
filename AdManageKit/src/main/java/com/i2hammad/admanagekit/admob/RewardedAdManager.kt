package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AdProviderConfig
import com.i2hammad.admanagekit.core.ad.AdUnitMapping
import com.i2hammad.admanagekit.core.ad.RewardedAdProvider
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.utils.AdRetryManager
import com.i2hammad.admanagekit.waterfall.RewardedWaterfall
import java.util.concurrent.atomic.AtomicInteger

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
    private var adUnitId: String = ""
    private const val TAG = "RewardedAdManager"

    private var firebaseAnalytics: FirebaseAnalytics? = null

    // Retry tracking
    private var retryAttempts: Int = 0

    // Callbacks attached to an in-flight load; drained when that load completes
    private val pendingLoadCallbacks = mutableListOf<OnRewardedAdLoadCallback>()

    /**
     * Incremented every time a load is started or abandoned; each load's callbacks carry the
     * token they began with and compare it against this before touching anything shared.
     *
     * A load cannot be cancelled once handed to the SDK, and [loadRewardedAdWithTimeout] gives up
     * on one while it is still running — so its callbacks can arrive long after a replacement load
     * has started, and without a token they would act as if they still spoke for the manager:
     * clearing [isLoading] out from under the newer load, discarding an ad that arrived in the
     * meantime, failing callers who are waiting on a request that has not finished yet, and
     * scheduling a retry for a request nobody is waiting on. Mirrors RewardedWaterfall.generation.
     */
    private val loadGeneration = AtomicInteger(0)

    /** Claims the next load generation, making every earlier in-flight load stale. */
    private fun beginLoad(): Int = loadGeneration.incrementAndGet()

    /**
     * Gives up on the load holding [token] without waiting for it. The request itself keeps
     * running — nothing can stop it — but from here on it speaks only for itself.
     */
    private fun abandonLoad(token: Int) {
        loadGeneration.compareAndSet(token, token + 1)
    }

    /** True once a newer load has taken over, or this one has been abandoned. */
    private fun isStale(token: Int): Boolean = token != loadGeneration.get()

    /**
     * Takes on an ad that arrived from a load the manager had already given up on.
     *
     * The ad is real and perfectly showable no matter which request produced it, so it is kept
     * rather than dropped — but only when nothing better is already in hand, and without touching
     * the state a newer load now owns.
     *
     * @return true if the ad was taken on, so waiting callers can be told an ad is ready
     */
    private fun adoptStaleAd(ad: RewardedAd): Boolean {
        if (rewardedAd != null) {
            AdDebugUtils.logEvent(adUnitId, "onAdLoadedAfterTimeout", "Ad already in hand, dropping the late one", true)
            return false
        }
        rewardedAd = ad
        AdDebugUtils.logEvent(adUnitId, "onAdLoadedAfterTimeout", "Ad saved for next show", true)
        return true
    }

    /**
     * Takes on a chain that finished loading after the manager had given up on it, for the same
     * reason as [adoptStaleAd]: the ad inside it is showable whoever asked for it.
     *
     * @return true if the chain was taken on
     */
    private fun adoptStaleWaterfall(waterfall: RewardedWaterfall): Boolean {
        if (rewardedWaterfall?.isAdReady() == true) {
            AdDebugUtils.logEvent(adUnitId, "onAdLoadedAfterTimeout", "Waterfall ad already ready, dropping the late chain", true)
            return false
        }
        rewardedWaterfall = waterfall
        AdDebugUtils.logEvent(adUnitId, "onAdLoadedAfterTimeout", "Waterfall ad saved for next show", true)
        return true
    }

    // Waterfall support
    private var rewardedWaterfall: RewardedWaterfall? = null
    private val useWaterfall: Boolean
        get() = AdProviderConfig.getRewardedChain().isNotEmpty()

    // Analytics counters
    private var sessionAdRequests = 0
    private var sessionAdFills = 0
    private var sessionAdImpressions = 0

    /**
     * Next-Gen SDK callbacks fire on background threads. Every consumer-facing
     * callback and every mutation of this manager's state runs through here so
     * state stays main-thread-confined and app code can safely touch views in
     * onRewardEarned / onAdDismissed / onAdLoaded etc. (mirrors AppOpenManager).
     */
    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action()
        else Handler(Looper.getMainLooper()).post(action)
    }

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
        fun onAdFailedToShow(error: AdKitAdError) {}

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
        if (adUnitId.isEmpty()) {
            Log.w(TAG, "Ad unit ID not set. Call initialize() first.")
            return
        }

        if (useWaterfall) { loadViaWaterfall(context); return }

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
        val token = beginLoad()
        initializeFirebase(context)

        // Cancel any pending retry since we're manually loading
        AdRetryManager.getInstance().cancelRetry(adUnitId)
        retryAttempts = 0

        // Log ad request for analytics
        logAdRequest()

        val adRequest = AdRequest.Builder(adUnitId).build()

        AdDebugUtils.logEvent(adUnitId, "loadStarted", "Starting rewarded ad load", true)

        RewardedAd.load(adRequest, object : AdLoadCallback<RewardedAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) = runOnMain {
                if (isStale(token)) {
                    // A request the manager has already moved on from. It speaks for nothing that
                    // is still current, so it clears no flags, fails no waiting callers and asks
                    // for no retry — the load that replaced it will answer for all of those.
                    Log.d(TAG, "Stale load failed, ignoring: ${adError.message}")
                    return@runOnMain
                }
                isLoading = false
                // No blind clearing of rewardedAd: a failure means this request produced nothing,
                // not that an ad picked up elsewhere has stopped being showable.
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

                // Notify callbacks that attached to this in-flight load
                notifyPendingLoadFailure(adError)

                // Attempt automatic retry if enabled
                if (AdManageKitConfig.autoRetryFailedAds && shouldAttemptRetry()) {
                    retryAttempts++
                    // Use the application context: the retry closure is parked on the main
                    // Handler for up to maxRetryDelay and re-arms across attempts, so
                    // capturing the Activity passed to showAd()/loadRewardedAd() would keep
                    // it alive off this process-lifetime singleton.
                    val appContext = context.applicationContext
                    AdRetryManager.getInstance().scheduleRetry(
                        adUnitId = adUnitId,
                        attempt = retryAttempts - 1,
                        maxAttempts = AdManageKitConfig.maxRetryAttempts
                    ) {
                        loadRewardedAd(appContext)
                    }
                }
            }

            override fun onAdLoaded(ad: RewardedAd) = runOnMain {
                if (isStale(token)) {
                    // Late, but an ad is an ad — keep it if nothing better is in hand and let
                    // anyone waiting for one have it. isLoading belongs to the newer load now.
                    if (adoptStaleAd(ad)) notifyPendingLoadSuccess()
                    return@runOnMain
                }
                isLoading = false
                rewardedAd = ad
                retryAttempts = 0 // Reset retry count on success
                Log.d(TAG, "Ad was loaded.")
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Rewarded ad loaded successfully", true)

                // Log ad fill for analytics
                logAdFill()

                // Notify callbacks that attached to this in-flight load
                notifyPendingLoadSuccess()
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
        if (adUnitId.isEmpty()) {
            Log.w(TAG, "Ad unit ID not set. Call initialize() first.")
            callback.onAdFailedToLoad(
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, "Ad unit ID not set. Call initialize() first.", null)
            )
            return
        }

        if (useWaterfall) { loadViaWaterfall(context, callback); return }

        // Guard: Skip loading for premium users
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onAdFailedToLoad(
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, AdManager.PURCHASED_APP_ERROR_MESSAGE, null)
            )
            return
        }

        // Guard: If already loaded, return success immediately
        if (rewardedAd != null) {
            callback.onAdLoaded()
            return
        }

        // Guard: Prevent duplicate concurrent load requests, but don't drop the callback -
        // queue it so it fires when the in-flight load completes
        if (isLoading) {
            Log.d(TAG, "Ad already loading, queueing callback for in-flight load")
            synchronized(pendingLoadCallbacks) { pendingLoadCallbacks.add(callback) }
            return
        }

        isLoading = true
        val token = beginLoad()
        initializeFirebase(context)
        logAdRequest()

        val adRequest = AdRequest.Builder(adUnitId).build()

        RewardedAd.load(adRequest, object : AdLoadCallback<RewardedAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) = runOnMain {
                // This caller is owed an answer either way — it has no other source of one — but a
                // stale request answers for itself alone and leaves the shared state to whichever
                // load is current.
                if (isStale(token)) {
                    Log.d(TAG, "Stale load failed, answering its own caller only: ${adError.message}")
                    callback.onAdFailedToLoad(adError)
                    return@runOnMain
                }
                isLoading = false
                Log.d(TAG, "Ad failed to load: ${adError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Rewarded ad failed: ${adError.message}", false)

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", "${adError.code}")
                }
                firebaseAnalytics?.logEvent("ad_failed_to_load", params)

                callback.onAdFailedToLoad(adError)
                notifyPendingLoadFailure(adError)
            }

            override fun onAdLoaded(ad: RewardedAd) = runOnMain {
                if (isStale(token)) {
                    val adopted = adoptStaleAd(ad)
                    callback.onAdLoaded()
                    if (adopted) notifyPendingLoadSuccess()
                    return@runOnMain
                }
                isLoading = false
                rewardedAd = ad
                retryAttempts = 0
                Log.d(TAG, "Ad was loaded.")
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Rewarded ad loaded with callback", true)
                logAdFill()

                callback.onAdLoaded()
                notifyPendingLoadSuccess()
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
        if (adUnitId.isEmpty()) {
            Log.w(TAG, "Ad unit ID not set. Call initialize() first.")
            callback.onAdFailedToLoad(
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, "Ad unit ID not set. Call initialize() first.", null)
            )
            return
        }

        if (useWaterfall) { loadViaWaterfallWithTimeout(context, timeoutMillis, callback); return }

        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onAdFailedToLoad(
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, AdManager.PURCHASED_APP_ERROR_MESSAGE, null)
            )
            return
        }

        // If already loaded, return immediately
        if (rewardedAd != null) {
            callback.onAdLoaded()
            return
        }

        // If already loading, attach to the in-flight load instead of starting a duplicate one
        if (isLoading) {
            Log.d(TAG, "Ad already loading, attaching to in-flight load")
            var pendingCallbackCalled = false
            val pendingCallback = object : OnRewardedAdLoadCallback {
                override fun onAdLoaded() {
                    if (!pendingCallbackCalled) {
                        pendingCallbackCalled = true
                        callback.onAdLoaded()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!pendingCallbackCalled) {
                        pendingCallbackCalled = true
                        callback.onAdFailedToLoad(error)
                    }
                }
            }
            synchronized(pendingLoadCallbacks) { pendingLoadCallbacks.add(pendingCallback) }

            // Honor the timeout for the attached caller as well
            Handler(Looper.getMainLooper()).postDelayed({
                if (!pendingCallbackCalled) {
                    pendingCallbackCalled = true
                    synchronized(pendingLoadCallbacks) { pendingLoadCallbacks.remove(pendingCallback) }
                    Log.d(TAG, "In-flight ad load timed out for attached caller")
                    callback.onAdFailedToLoad(
                        LoadAdError(LoadAdError.ErrorCode.TIMEOUT, "Ad loading timed out", null)
                    )
                }
            }, timeoutMillis)
            return
        }

        isLoading = true
        val token = beginLoad()
        initializeFirebase(context)
        logAdRequest()

        var callbackCalled = false

        val adRequest = AdRequest.Builder(adUnitId).build()

        RewardedAd.load(adRequest, object : AdLoadCallback<RewardedAd> {
            // Both callbacks post to main BEFORE touching callbackCalled/isLoading, so those
            // flags are main-thread-confined and cannot race the (main-thread) timeout below.
            override fun onAdFailedToLoad(adError: LoadAdError) = runOnMain {
                // The timeout below gives up on this request without being able to stop it, so a
                // failure arriving afterwards is speaking about a request nobody is waiting on:
                // it must not clear the replacement load's isLoading, must not throw away an ad
                // that turned up in the meantime, and must not fail callers queued behind a load
                // that is still running. callbackCalled already covers this call's own caller.
                if (isStale(token)) {
                    Log.d(TAG, "Stale load failed, ignoring: ${adError.message}")
                    return@runOnMain
                }
                isLoading = false

                // Notify callbacks that attached to this in-flight load
                notifyPendingLoadFailure(adError)

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

            override fun onAdLoaded(ad: RewardedAd) = runOnMain {
                if (isStale(token)) {
                    // Arrived after the manager gave up on it. The ad is still good, so it is kept
                    // for the next show — but the state a newer load owns is left alone.
                    if (adoptStaleAd(ad)) notifyPendingLoadSuccess()
                    return@runOnMain
                }
                isLoading = false
                rewardedAd = ad
                retryAttempts = 0

                // Notify callbacks that attached to this in-flight load
                notifyPendingLoadSuccess()

                callbackCalled = true
                Log.d(TAG, "Ad was loaded within timeout.")
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Rewarded ad loaded within timeout", true)
                logAdFill()
                callback.onAdLoaded()
            }
        })

        // Timeout handler
        Handler(Looper.getMainLooper()).postDelayed({
            if (!callbackCalled && !isStale(token)) {
                callbackCalled = true
                isLoading = false
                // Stop speaking for this request before walking away from it: it cannot be
                // cancelled, and whatever it reports from here must not be mistaken for the
                // answer to whatever the caller does next.
                abandonLoad(token)
                Log.d(TAG, "Ad loading timed out")
                AdDebugUtils.logEvent(adUnitId, "onTimeout", "Rewarded ad loading timed out", false)

                callback.onAdFailedToLoad(
                    LoadAdError(LoadAdError.ErrorCode.TIMEOUT, "Ad loading timed out", null)
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
        if (adUnitId.isEmpty()) {
            Log.w(TAG, "Ad unit ID not set. Call initialize() first.")
            callback.onAdDismissed()
            return
        }

        // Guard: An ad is already on screen - don't call show() on the same ad again.
        // The callback is intentionally not invoked to avoid double-triggering the
        // post-ad flow of what is typically a duplicate request (e.g. double-tap).
        if (isShowingAd) {
            Log.w(TAG, "Ad is already showing, ignoring duplicate show request")
            AdDebugUtils.logEvent(adUnitId, "skipShow", "Ad already showing", false)
            return
        }

        if (useWaterfall) { showViaWaterfall(activity, callback, autoReload); return }

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

        // Next-Gen SDK event callbacks fire on background threads. Each body posts to
        // main before touching manager state or invoking consumer callbacks - apps
        // grant rewards and navigate/update views in onRewardEarned/onAdDismissed.
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdClicked() = runOnMain {
                Log.d(TAG, "Ad was clicked.")
                AdDebugUtils.logEvent(adUnitId, "onAdClicked", "Rewarded ad clicked", true)
                callback.onAdClicked()
            }

            override fun onAdDismissedFullScreenContent() = runOnMain {
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

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) = runOnMain {
                Log.e(TAG, "Ad failed to show fullscreen content: ${adError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToShow", "Rewarded ad failed to show: ${adError.message}", false)
                // Only reset state owned by this ad - don't clobber a different ad that is showing
                if (rewardedAd === ad) {
                    isShowingAd = false
                    rewardedAd = null
                }

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
                callback.onAdFailedToShow(AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, adError.message, "admob"))
                callback.onAdDismissed()
            }

            override fun onAdImpression() = runOnMain {
                Log.d(TAG, "Ad recorded an impression.")
                AdDebugUtils.logEvent(adUnitId, "onAdImpression", "Rewarded ad impression", true)

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)

                logAdImpression()
            }

            override fun onAdShowedFullScreenContent() = runOnMain {
                Log.d(TAG, "Ad showed fullscreen content.")
                AdDebugUtils.logEvent(adUnitId, "onAdShowed", "Rewarded ad showing", true)
                isShowingAd = true
                callback.onAdShowed()
            }

            override fun onAdPaid(value: AdValue) {
                val adValueInStandardUnits = value.valueMicros / 1000000.0

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                    putString(FirebaseAnalytics.Param.CURRENCY, value.currencyCode)
                }
                firebaseAnalytics?.logEvent("ad_paid_event", params)
            }
        }

        // Claim the showing slot before show() so a concurrent showAd call is rejected
        isShowingAd = true
        ad.show(activity) { rewardItem ->
            // The reward listener also fires on a background thread - marshal before the
            // app grants the reward (typically a UI/state update)
            runOnMain {
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
    }

    /**
     * Check if a rewarded ad is loaded and ready to show.
     * Returns false if user has purchased premium.
     *
     * @return true if ad is ready to show
     */
    fun isAdLoaded(): Boolean {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) return false
        if (useWaterfall) return rewardedWaterfall?.isAdReady() == true
        return rewardedAd != null
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
        if (adUnitId.isEmpty()) {
            Log.w(TAG, "Ad unit ID not set. Call initialize() first.")
            return
        }
        if (!isAdLoaded() && !isLoading) {
            AdDebugUtils.logEvent(adUnitId, "preload", "Preloading rewarded ad", true)
            if (useWaterfall) { loadViaWaterfall(context) } else { loadRewardedAd(context) }
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

    // =================== WATERFALL HELPERS ===================

    private fun resolveAdUnit(logicalName: String): (AdProvider) -> String? = { provider ->
        AdUnitMapping.getAdUnitId(logicalName, provider)
            ?: logicalName.takeIf { provider == AdProvider.ADMOB }
    }

    private fun createWaterfall(): RewardedWaterfall {
        return RewardedWaterfall(
            providers = AdProviderConfig.getRewardedChain(),
            adUnitResolver = resolveAdUnit(adUnitId)
        )
    }

    private fun loadViaWaterfall(context: Context) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            AdDebugUtils.logEvent(adUnitId, "skipLoad", "Skipping waterfall load - user is premium", true)
            return
        }
        if (isLoading) return
        if (rewardedWaterfall?.isAdReady() == true) return

        isLoading = true
        val token = beginLoad()
        initializeFirebase(context)
        logAdRequest()

        val waterfall = createWaterfall()
        rewardedWaterfall = waterfall

        waterfall.load(context, object : RewardedAdProvider.RewardedAdCallback {
            override fun onAdLoaded() {
                if (isStale(token)) {
                    if (adoptStaleWaterfall(waterfall)) notifyPendingLoadSuccess()
                    return
                }
                isLoading = false
                retryAttempts = 0
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Rewarded waterfall ad loaded", true)
                logAdFill()
                notifyPendingLoadSuccess()
            }

            override fun onAdFailedToLoad(error: AdKitAdError) {
                if (isStale(token)) {
                    Log.d(TAG, "Stale waterfall load failed, ignoring: ${error.message}")
                    return
                }
                isLoading = false
                // Only drop the chain if it is still the one this load installed - a newer load,
                // or a late chain adopted in the meantime, is not this failure's to discard.
                if (rewardedWaterfall === waterfall) rewardedWaterfall = null
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Rewarded waterfall failed: ${error.message}", false)

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", "${error.code}")
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", error.message)
                    }
                }
                firebaseAnalytics?.logEvent("ad_failed_to_load", params)

                notifyPendingLoadFailure(
                    LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, error.message, null)
                )

                if (AdManageKitConfig.autoRetryFailedAds && shouldAttemptRetry()) {
                    retryAttempts++
                    // Application context - see the AdMob load path for why
                    val appContext = context.applicationContext
                    AdRetryManager.getInstance().scheduleRetry(
                        adUnitId = adUnitId,
                        attempt = retryAttempts - 1,
                        maxAttempts = AdManageKitConfig.maxRetryAttempts
                    ) {
                        loadViaWaterfall(appContext)
                    }
                }
            }
        })
    }

    private fun loadViaWaterfall(context: Context, callback: OnRewardedAdLoadCallback) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onAdFailedToLoad(
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, AdManager.PURCHASED_APP_ERROR_MESSAGE, null)
            )
            return
        }
        if (rewardedWaterfall?.isAdReady() == true) { callback.onAdLoaded(); return }
        if (isLoading) {
            // Queue the callback so it fires when the in-flight load completes
            Log.d(TAG, "Waterfall ad already loading, queueing callback for in-flight load")
            synchronized(pendingLoadCallbacks) { pendingLoadCallbacks.add(callback) }
            return
        }

        isLoading = true
        val token = beginLoad()
        initializeFirebase(context)
        logAdRequest()

        val waterfall = createWaterfall()
        rewardedWaterfall = waterfall

        waterfall.load(context, object : RewardedAdProvider.RewardedAdCallback {
            override fun onAdLoaded() {
                if (isStale(token)) {
                    val adopted = adoptStaleWaterfall(waterfall)
                    callback.onAdLoaded()
                    if (adopted) notifyPendingLoadSuccess()
                    return
                }
                isLoading = false
                retryAttempts = 0
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Rewarded waterfall ad loaded with callback", true)
                logAdFill()
                callback.onAdLoaded()
                notifyPendingLoadSuccess()
            }

            override fun onAdFailedToLoad(error: AdKitAdError) {
                // Its own caller is still owed an answer; the shared state is not this stale
                // request's to touch.
                if (isStale(token)) {
                    Log.d(TAG, "Stale waterfall load failed, answering its own caller only: ${error.message}")
                    callback.onAdFailedToLoad(
                        LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, error.message, null)
                    )
                    return
                }
                isLoading = false
                if (rewardedWaterfall === waterfall) rewardedWaterfall = null
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Rewarded waterfall failed: ${error.message}", false)

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", "${error.code}")
                }
                firebaseAnalytics?.logEvent("ad_failed_to_load", params)

                val loadAdError = LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, error.message, null)
                callback.onAdFailedToLoad(loadAdError)
                notifyPendingLoadFailure(loadAdError)
            }
        })
    }

    private fun loadViaWaterfallWithTimeout(
        context: Context,
        timeoutMillis: Long,
        callback: OnRewardedAdLoadCallback
    ) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onAdFailedToLoad(
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, AdManager.PURCHASED_APP_ERROR_MESSAGE, null)
            )
            return
        }
        if (rewardedWaterfall?.isAdReady() == true) { callback.onAdLoaded(); return }

        isLoading = true
        val token = beginLoad()
        initializeFirebase(context)
        logAdRequest()

        var callbackCalled = false

        val waterfall = createWaterfall()
        rewardedWaterfall = waterfall

        waterfall.load(context, object : RewardedAdProvider.RewardedAdCallback {
            override fun onAdLoaded() {
                if (isStale(token)) {
                    if (adoptStaleWaterfall(waterfall)) notifyPendingLoadSuccess()
                    return
                }
                isLoading = false
                retryAttempts = 0
                notifyPendingLoadSuccess()
                callbackCalled = true
                logAdFill()
                callback.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: AdKitAdError) {
                // Abandoned at the timeout below and speaking for nothing current - see the
                // AdMob path for the full reasoning. callbackCalled covers its own caller.
                if (isStale(token)) {
                    Log.d(TAG, "Stale waterfall load failed, ignoring: ${error.message}")
                    return
                }
                isLoading = false
                if (rewardedWaterfall === waterfall) rewardedWaterfall = null
                val loadAdError = LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, error.message, null)
                notifyPendingLoadFailure(loadAdError)
                callbackCalled = true
                callback.onAdFailedToLoad(loadAdError)
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            if (!callbackCalled && !isStale(token)) {
                callbackCalled = true
                isLoading = false
                // Stop speaking for this chain before walking away from it - the providers keep
                // going and their late verdict is no longer an answer to anything.
                abandonLoad(token)
                callback.onAdFailedToLoad(
                    LoadAdError(LoadAdError.ErrorCode.TIMEOUT, "Ad loading timed out", null)
                )
            }
        }, timeoutMillis)
    }

    private fun showViaWaterfall(
        activity: Activity,
        callback: RewardedAdCallback,
        autoReload: Boolean
    ) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onAdDismissed()
            return
        }

        val waterfall = rewardedWaterfall
        if (waterfall == null || !waterfall.isAdReady()) {
            loadViaWaterfall(activity)
            callback.onAdDismissed()
            return
        }

        // Bundled providers already marshal to main, but third-party/custom providers
        // registered through AdProviderConfig need not - marshal here so the
        // main-thread guarantee holds for every provider, not just the shipped ones.
        waterfall.show(activity, object : RewardedAdProvider.RewardedShowCallback {
            override fun onAdShowed() = runOnMain {
                isShowingAd = true
                AdDebugUtils.logEvent(adUnitId, "onAdShowed", "Rewarded waterfall ad showing", true)
                logAdImpression()
                callback.onAdShowed()
            }

            override fun onAdDismissed() = runOnMain {
                isShowingAd = false
                rewardedWaterfall = null
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics?.logEvent("ad_dismissed", params)
                if (autoReload) {
                    loadViaWaterfall(activity)
                }
                callback.onAdDismissed()
            }

            override fun onAdFailedToShow(error: AdKitAdError) = runOnMain {
                isShowingAd = false
                rewardedWaterfall = null
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", "${error.code}")
                }
                firebaseAnalytics?.logEvent("ad_failed_to_show", params)
                if (autoReload) {
                    loadViaWaterfall(activity)
                }
                callback.onAdFailedToShow(error)
                callback.onAdDismissed()
            }

            override fun onAdClicked() = runOnMain {
                callback.onAdClicked()
            }

            override fun onAdImpression() = runOnMain {
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
            }

            override fun onRewardEarned(rewardType: String, rewardAmount: Int) = runOnMain {
                AdDebugUtils.logEvent(adUnitId, "onRewardEarned", "Waterfall reward: $rewardAmount $rewardType", true)
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("reward_type", rewardType)
                    putInt("reward_amount", rewardAmount)
                }
                firebaseAnalytics?.logEvent("rewarded_ad_reward", params)
                callback.onRewardEarned(rewardType, rewardAmount)
            }

            override fun onPaidEvent(adValue: AdKitAdValue) {
                val adValueInStandardUnits = adValue.valueMicros / 1000000.0
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                    putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                }
                firebaseAnalytics?.logEvent("ad_paid_event", params)
            }
        })
    }

    // =================== PRIVATE HELPERS ===================

    /**
     * Drain callbacks attached to the in-flight load and notify them of success.
     */
    private fun notifyPendingLoadSuccess() {
        val callbacks = synchronized(pendingLoadCallbacks) {
            val copy = pendingLoadCallbacks.toList()
            pendingLoadCallbacks.clear()
            copy
        }
        callbacks.forEach { it.onAdLoaded() }
    }

    /**
     * Drain callbacks attached to the in-flight load and notify them of failure.
     */
    private fun notifyPendingLoadFailure(error: LoadAdError) {
        val callbacks = synchronized(pendingLoadCallbacks) {
            val copy = pendingLoadCallbacks.toList()
            pendingLoadCallbacks.clear()
            copy
        }
        callbacks.forEach { it.onAdFailedToLoad(error) }
    }

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
