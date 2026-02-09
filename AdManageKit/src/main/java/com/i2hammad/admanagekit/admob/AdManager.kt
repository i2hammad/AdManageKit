package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AdProviderConfig
import com.i2hammad.admanagekit.core.ad.AdUnitMapping
import com.i2hammad.admanagekit.core.ad.InterstitialAdProvider
import com.i2hammad.admanagekit.config.AdLoadingStrategy
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.utils.AdRetryManager
import com.i2hammad.admanagekit.waterfall.InterstitialWaterfall
import java.util.concurrent.ConcurrentHashMap
import androidx.core.graphics.drawable.toDrawable

/**
 * AdManager is a singleton class responsible for managing interstitial ads
 * using Google AdMob. It provides functionality to load and show ads, manage
 * display intervals, and handle ad-related callbacks.
 *
 * ## Ad Pool Feature
 * Supports multiple ad units stored in a pool. When showing an ad:
 * - Returns ANY available ad from the pool (maximizes show rate)
 * - Auto-reloads the specific unit that was shown
 * - Each unit loads independently (no blocking)
 *
 * Example:
 * ```kotlin
 * // Load multiple ad units
 * adManager.loadInterstitialAd(context, "unit_a")
 * adManager.loadInterstitialAd(context, "unit_b")
 * adManager.loadInterstitialAd(context, "unit_c")
 *
 * // Show any available ad
 * adManager.showInterstitialIfReady(activity, callback)  // Returns unit_a, unit_b, or unit_c
 * ```
 */
class AdManager() {

    // Ad pool - stores multiple ads by ad unit ID
    private val adPool = ConcurrentHashMap<String, InterstitialAd>()

    // Track which ad units are currently loading
    private val loadingAdUnits = ConcurrentHashMap.newKeySet<String>()

    // Legacy single ad reference (for backward compatibility)
    @Deprecated("Use adPool instead", ReplaceWith("adPool"))
    private var mInterstitialAd: InterstitialAd? = null

    // Primary ad unit ID (used when no specific unit is requested)
    private var adUnitId: String? = null

    @Deprecated("Use loadingAdUnits instead")
    private var isAdLoading = false
    private var isDisplayingAd = false
    private var lastAdShowTime: Long = 0
    private var adIntervalMillis: Long = AdManageKitConfig.defaultInterstitialInterval.inWholeMilliseconds
    private var adDisplayCount = 0 // Track the number of times ads have been displayed
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    // Retry state
    private val retryAttempts = mutableMapOf<String, Int>()

    // Call counters per ad unit (for everyNthTime feature)
    private val callCounters = mutableMapOf<String, Int>()

    // Track current loading dialog to prevent duplicates
    private var currentLoadingDialog: LoadingDialogViews? = null

    // Flag to track if we're currently fetching an ad with dialog displayed
    private var isFetchingWithDialog = false

    // Waterfall support
    private var interstitialWaterfall: InterstitialWaterfall? = null
    private var isWaterfallLoading = false
    private val useWaterfall: Boolean
        get() = AdProviderConfig.getInterstitialChain().isNotEmpty()

    companion object {
        const val PURCHASED_APP_ERROR_CODE = 1001
        const val PURCHASED_APP_ERROR_DOMAIN = "com.i2hammad.admanagekit"
        const val PURCHASED_APP_ERROR_MESSAGE =
            "Ads are not shown because the app has been purchased."

        @Volatile
        private var instance: AdManager? = null

        @JvmStatic
        fun getInstance(): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager().also { instance = it }
            }
        }
    }

    fun initializeFirebase(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }


    /**
     * Data class to hold dialog and its animated views
     */
    private data class LoadingDialogViews(
        val dialog: Dialog,
        val overlay: View,
        val contentCard: View
    )

    /**
     * Creates and shows a beautiful full-screen loading dialog with animations
     */
    private fun showBeautifulLoadingDialog(activity: Activity): LoadingDialogViews {
        // Dismiss any existing dialog first to prevent duplicates
        currentLoadingDialog?.let { existing ->
            try {
                if (existing.dialog.isShowing) {
                    existing.dialog.dismiss()
                }
            } catch (e: Exception) {
                Log.w("AdManager", "Error dismissing existing dialog: ${e.message}")
            }
            currentLoadingDialog = null
        }

        // Create beautiful full-screen loading dialog
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_loading_ad_fullscreen, null)
        dialog.setContentView(dialogView)
        dialog.window?.apply {
            // Make truly full screen
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            // Full screen flags
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            // Configure edge-to-edge display using modern API
            WindowCompat.setDecorFitsSystemWindows(this, false)

            // Use WindowInsetsControllerCompat for backward-compatible immersive mode
            WindowCompat.getInsetsController(this, decorView).apply {
                // Hide system bars for immersive experience
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }


        }
        dialog.setCancelable(false)

        // Get views for animations
        val overlay = dialogView.findViewById<View>(R.id.overlay)
        val contentCard = dialogView.findViewById<View>(R.id.contentCard)
        val outerCircle = dialogView.findViewById<View>(R.id.outerCircle)
        val innerCircle = dialogView.findViewById<View>(R.id.innerCircle)
        val dot1 = dialogView.findViewById<View>(R.id.dot1)
        val dot2 = dialogView.findViewById<View>(R.id.dot2)
        val dot3 = dialogView.findViewById<View>(R.id.dot3)

        // Apply dialog customizations
        applyDialogCustomizations(dialogView, overlay, contentCard)

        // Apply custom text if configured
        AdManageKitConfig.loadingDialogTitle?.let { title ->
            dialogView.findViewById<android.widget.TextView>(R.id.loadingTitle)?.text = title
        }
        AdManageKitConfig.loadingDialogSubtitle?.let { subtitle ->
            dialogView.findViewById<android.widget.TextView>(R.id.loadingMessage)?.text = subtitle
        }

        // Show dialog
        dialog.show()

        // Animate overlay fade in
        overlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        // Animate content card
        contentCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(100)
            .start()

        // Start rotating animations for circles
        val rotateClockwise = AnimationUtils.loadAnimation(activity, R.anim.rotate_clockwise)
        val rotateCounterClockwise = AnimationUtils.loadAnimation(activity, R.anim.rotate_counterclockwise)
        outerCircle.startAnimation(rotateClockwise)
        innerCircle.startAnimation(rotateCounterClockwise)

        // Animate dots with sequential delay
        animateDot(dot1, 0)
        animateDot(dot2, 200)
        animateDot(dot3, 400)

        val dialogViews = LoadingDialogViews(dialog, overlay, contentCard)
        currentLoadingDialog = dialogViews
        return dialogViews
    }


    /**
     * Loads an interstitial ad with a specified timeout, to be used on the splash screen.
     *
     * @param context The context in which the ad is being loaded.
     * @param adUnitId The ad unit ID for the interstitial ad.
     * @param timeoutMillis The timeout in milliseconds to wait for the ad to load.
     * @param callback The callback to handle actions after the ad loading is complete.
     */
    fun loadInterstitialAdForSplash(
        context: Context, adUnitId: String, timeoutMillis: Long = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds, callback: AdManagerCallback
    ) {
        if (useWaterfall) { loadWaterfallForSplash(context, adUnitId, timeoutMillis, callback); return }

        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            // User has purchased, no ads should be shown
            callback.onNextAction()
            return
        }

        // Skip if ad is already loaded and ready
        if (mInterstitialAd != null || adPool.containsKey(adUnitId)) {
            Log.d("AdManager", "Ad already loaded for splash, skipping load request")
            AdDebugUtils.logEvent(adUnitId, "skipAlreadyLoaded", "Ad already loaded for splash", true)
            this.adUnitId = adUnitId
            callback.onNextAction()
            callback.onAdLoaded()
            return
        }

        // Skip if already loading this ad unit
        if (loadingAdUnits.contains(adUnitId) || isAdLoading) {
            Log.d("AdManager", "Ad unit $adUnitId already loading for splash, skipping duplicate request")
            AdDebugUtils.logEvent(adUnitId, "skipDuplicateLoad", "Ad already loading for splash", true)
            // Still set timeout to call callback if current load takes too long
            this.adUnitId = adUnitId
            var callbackCalled = false
            Handler(Looper.getMainLooper()).postDelayed({
                if (!callbackCalled && !isReady()) {
                    callbackCalled = true
                    callback.onNextAction()
                }
            }, timeoutMillis)
            return
        }

        this.adUnitId = adUnitId
        initializeFirebase(context)
        val adRequest = AdRequest.Builder().build()

        // Cancel any pending retry since we're manually loading
        AdRetryManager.getInstance().cancelRetry(adUnitId)
        retryAttempts.remove(adUnitId)

        // Mark this unit as loading
        loadingAdUnits.add(adUnitId)
        isAdLoading = true
        var callbackCalled = false  // Prevent double callbacks

        // Load the interstitial ad
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                // Always save the ad (improves show rate even if timeout already fired)
                mInterstitialAd = interstitialAd
                loadingAdUnits.remove(adUnitId)
                isAdLoading = loadingAdUnits.isNotEmpty()
                Log.d("AdManager", "Interstitial ad loaded for splash")
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Interstitial ad loaded for splash", true)

                // Only call callback if not already called by timeout
                if (!callbackCalled) {
                    callbackCalled = true
                    callback.onNextAction()
                    callback.onAdLoaded()
                } else {
                    // Ad loaded after timeout - it's saved for next use (not wasted!)
                    AdDebugUtils.logEvent(adUnitId, "onAdLoadedAfterTimeout", "Ad saved for next show", true)
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                loadingAdUnits.remove(adUnitId)
                isAdLoading = loadingAdUnits.isNotEmpty()

                Log.e(
                    "AdManager", "Failed to load interstitial ad for splash: ${loadAdError.message}"
                )
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Interstitial failed for splash: ${loadAdError.message}", false)

                // Attempt automatic retry if enabled
                if (AdManageKitConfig.autoRetryFailedAds && shouldAttemptRetry(adUnitId)) {
                    val currentAttempt = retryAttempts[adUnitId] ?: 0
                    retryAttempts[adUnitId] = currentAttempt + 1

                    AdRetryManager.getInstance().scheduleRetry(
                        adUnitId = adUnitId,
                        attempt = currentAttempt,
                        maxAttempts = AdManageKitConfig.maxRetryAttempts
                    ) {
                        // Retry loading the ad
                        loadInterstitialAdForSplash(context, adUnitId, timeoutMillis, callback)
                    }
                } else {
                    mInterstitialAd = null
                }

                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", loadAdError.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)

                // Only call callback if not already called by timeout
                if (!callbackCalled) {
                    callbackCalled = true
                    callback.onNextAction()
                    callback.onFailedToLoad(loadAdError)
                }
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            if (loadingAdUnits.contains(adUnitId) && !callbackCalled) {
                Log.d("AdManager", "Ad loading timed out for splash")
                AdDebugUtils.logEvent(adUnitId, "onTimeout", "Interstitial ad loading timed out for splash", false)
                callbackCalled = true

                // Note: Don't remove from loadingAdUnits here - the ad callback will still fire
                // and save the ad if it arrives later. Just allow new UI flows to proceed.
                // The in-flight load callback will clean up loadingAdUnits when it completes.

                // Ensure the callback is called if the ad loading is taking too long
                callback.onNextAction()
            }
        }, timeoutMillis)
    }

    fun loadInterstitialAd(context: Context, adUnitId: String) {
        if (useWaterfall) { loadViaWaterfall(context, adUnitId); return }

        // Skip loading for premium users - no need to request ads
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            AdDebugUtils.logEvent(adUnitId, "skipLoad", "Skipping ad load - user is premium", true)
            return
        }

        // Guard: Prevent duplicate concurrent loads for SAME ad unit
        if (loadingAdUnits.contains(adUnitId)) {
            Log.d("AdManager", "Ad unit $adUnitId already loading, skipping duplicate request")
            AdDebugUtils.logEvent(adUnitId, "skipDuplicateLoad", "Ad unit already loading", true)
            return
        }

        // Guard: Skip if THIS ad unit already has ad in pool
        if (adPool.containsKey(adUnitId)) {
            Log.d("AdManager", "Ad unit $adUnitId already in pool, skipping load request")
            AdDebugUtils.logEvent(adUnitId, "skipAlreadyLoaded", "Ad already in pool", true)
            return
        }

        if (AdManageKitConfig.testMode) {
            AdDebugUtils.logEvent(adUnitId, "testMode", "Using test mode for interstitial ads", true)
        }

        // Set primary ad unit if not set
        if (this.adUnitId == null) {
            this.adUnitId = adUnitId
        }

        initializeFirebase(context)
        val adRequest = AdRequest.Builder().build()

        // Cancel any pending retry since we're manually loading
        AdRetryManager.getInstance().cancelRetry(adUnitId)
        retryAttempts.remove(adUnitId)

        // Mark this unit as loading
        loadingAdUnits.add(adUnitId)
        isAdLoading = true  // Legacy flag

        Log.d("AdManager", "Loading interstitial ad for unit: $adUnitId (pool size: ${adPool.size})")

        // Firebase: Log ad request
        logAdRequest(adUnitId, "interstitial")

        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                // Add to pool
                adPool[adUnitId] = interstitialAd
                loadingAdUnits.remove(adUnitId)

                // Legacy compatibility
                mInterstitialAd = interstitialAd
                isAdLoading = loadingAdUnits.isNotEmpty()

                Log.d("AdManager", "Interstitial ad loaded for unit: $adUnitId (pool size: ${adPool.size})")
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Ad loaded, pool size: ${adPool.size}", true)

                // Firebase: Log ad fill (successful load)
                logAdFill(adUnitId, "interstitial")

                // Reset retry attempts on success
                retryAttempts.remove(adUnitId)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                loadingAdUnits.remove(adUnitId)
                isAdLoading = loadingAdUnits.isNotEmpty()

                Log.e("AdManager", "Failed to load interstitial ad for $adUnitId: ${loadAdError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Interstitial failed: ${loadAdError.message}", false)

                // Attempt automatic retry if enabled
                if (AdManageKitConfig.autoRetryFailedAds && shouldAttemptRetry(adUnitId)) {
                    val currentAttempt = retryAttempts[adUnitId] ?: 0
                    retryAttempts[adUnitId] = currentAttempt + 1

                    AdRetryManager.getInstance().scheduleRetry(
                        adUnitId = adUnitId,
                        attempt = currentAttempt,
                        maxAttempts = AdManageKitConfig.maxRetryAttempts
                    ) {
                        // Retry loading the ad
                        loadInterstitialAd(context, adUnitId)
                    }
                }

                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", loadAdError.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)
            }
        })
    }

    /**
     * Load multiple interstitial ad units into the pool.
     * Each unit loads independently (non-blocking).
     *
     * Example:
     * ```kotlin
     * adManager.loadMultipleAdUnits(context, listOf("unit_a", "unit_b", "unit_c"))
     * ```
     *
     * @param context The context
     * @param adUnitIds List of ad unit IDs to load
     */
    fun loadMultipleAdUnits(context: Context, adUnitIds: List<String>) {
        Log.d("AdManager", "Loading ${adUnitIds.size} ad units into pool")
        adUnitIds.forEach { unitId ->
            loadInterstitialAd(context, unitId)
        }
    }

    /**
     * Load multiple interstitial ad units into the pool (vararg version).
     *
     * Example:
     * ```kotlin
     * adManager.loadMultipleAdUnits(context, "unit_a", "unit_b", "unit_c")
     * ```
     */
    fun loadMultipleAdUnits(context: Context, vararg adUnitIds: String) {
        loadMultipleAdUnits(context, adUnitIds.toList())
    }

    /**
     * Clear all ads from the pool.
     * Useful for cleanup or when user purchases premium.
     */
    fun clearAdPool() {
        val count = adPool.size
        adPool.clear()
        mInterstitialAd = null
        Log.d("AdManager", "Cleared $count ads from pool")
        AdDebugUtils.logEvent("", "poolCleared", "Cleared $count ads", true)
    }

    /**
     * Load an interstitial ad with custom ad unit support and callbacks.
     *
     * @param context The context
     * @param adUnitId The ad unit ID (can be different from the default)
     * @param interstitialAdLoadCallback Callback for ad loading events
     */
    fun loadInterstitialAd(
        context: Context, adUnitId: String, interstitialAdLoadCallback: InterstitialAdLoadCallback
    ) {
        if (AdManageKitConfig.testMode) {
            AdDebugUtils.logEvent(adUnitId, "testMode", "Using test mode for interstitial ads with callback", true)
        }
        var purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()){
            // User has purchased, no ads should be shown
            interstitialAdLoadCallback.onAdFailedToLoad(
                LoadAdError(
                    PURCHASED_APP_ERROR_CODE,
                    PURCHASED_APP_ERROR_MESSAGE,
                    PURCHASED_APP_ERROR_DOMAIN,
                    null, // No underlying AdError cause
                    null  // No additional ResponseInfo
                )
            )
            return
        }
        this.adUnitId = adUnitId
        initializeFirebase(context)
        val adRequest = AdRequest.Builder().build()

        // Cancel any pending retry since we're manually loading
        AdRetryManager.getInstance().cancelRetry(adUnitId)
        retryAttempts.remove(adUnitId)

        isAdLoading = true
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                isAdLoading = false
                Log.d("AdManager", "Interstitial ad loaded")
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Interstitial ad loaded with callback", true)

                interstitialAdLoadCallback.onAdLoaded(mInterstitialAd!!)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("AdManager", "Failed to load interstitial ad: ${loadAdError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Interstitial failed with callback: ${loadAdError.message}", false)

                // Attempt automatic retry if enabled
                if (AdManageKitConfig.autoRetryFailedAds && shouldAttemptRetry(adUnitId)) {
                    val currentAttempt = retryAttempts[adUnitId] ?: 0
                    retryAttempts[adUnitId] = currentAttempt + 1
                    
                    AdRetryManager.getInstance().scheduleRetry(
                        adUnitId = adUnitId,
                        attempt = currentAttempt,
                        maxAttempts = AdManageKitConfig.maxRetryAttempts
                    ) {
                        // Retry loading the ad with callback
                        loadInterstitialAd(context, adUnitId, interstitialAdLoadCallback)
                    }
                } else {
                    isAdLoading = false
                    mInterstitialAd = null
                }

                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", loadAdError.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)
                interstitialAdLoadCallback.onAdFailedToLoad(loadAdError)
            }
        })
    }

    /**
     * Shows an interstitial ad respecting the global loading strategy.
     *
     * Behavior based on AdManageKitConfig.interstitialLoadingStrategy:
     * - **ON_DEMAND**: Always fetch fresh ad with loading dialog
     * - **ONLY_CACHE**: Only show if cached ad is ready, skip if not
     * - **HYBRID**: Show cached if ready, otherwise fetch fresh with dialog
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    fun forceShowInterstitial(activity: Activity, callback: AdManagerCallback) {
        val strategy = AdManageKitConfig.interstitialLoadingStrategy
        val effectiveAutoReload = AdManageKitConfig.interstitialAutoReload

        when (strategy) {
            AdLoadingStrategy.ON_DEMAND -> {
                // Always fetch fresh ad with dialog
                forceShowInterstitialAlways(activity, callback)
            }
            AdLoadingStrategy.ONLY_CACHE -> {
                // Only show if cached, skip otherwise
                if (isReady()) {
                    showInterstitialIfReady(activity, callback, effectiveAutoReload)
                } else {
                    callback.onNextAction()
                }
            }
            AdLoadingStrategy.HYBRID -> {
                // Show cached if ready, otherwise fetch fresh
                if (isReady()) {
                    showInterstitialIfReady(activity, callback, effectiveAutoReload)
                } else {
                    forceShowInterstitialAlways(activity, callback)
                }
            }
            AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK -> {
                // Try fresh first, fall back to cache if fails
                // For interstitials, this behaves like ON_DEMAND (always tries fresh)
                // but the internal loading will fall back to cache on failure
                forceShowInterstitialAlways(activity, callback)
            }
        }
    }


    /**
     * Shows an interstitial ad with loading dialog, respecting the global loading strategy.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @see forceShowInterstitial
     */
    fun forceShowInterstitialWithDialog(
        activity: Activity, callback: AdManagerCallback
    ) {
        forceShowInterstitial(activity, callback)
    }

    /**
     * Always forces fetch and display of a fresh interstitial ad.
     * Ignores loading strategy - always loads new ad with dialog.
     * Used internally by InterstitialAdBuilder for explicit force behavior.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    internal fun forceShowInterstitialAlways(activity: Activity, callback: AdManagerCallback) {
        forceShowInterstitialInternal(activity, callback, null)
    }

    /**
     * Always forces fetch and display of a fresh interstitial ad with specified ad unit.
     * Ignores loading strategy - always loads new ad with dialog.
     * Used internally by InterstitialAdBuilder for explicit force behavior.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param adUnitId The ad unit ID to use for loading (overrides the default).
     */
    internal fun forceShowInterstitialAlways(activity: Activity, callback: AdManagerCallback, adUnitId: String) {
        forceShowInterstitialInternal(activity, callback, adUnitId)
    }

    /**
     * Shows an interstitial ad immediately if one is loaded and ready.
     * Does NOT fetch a new ad if none is available - proceeds to next action.
     * Use this when you want to show an ad opportunistically without waiting.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param reloadAd Whether to reload the ad after it's shown. Defaults to AdManageKitConfig.interstitialAutoReload.
     */
    fun showInterstitialIfReady(
        activity: Activity,
        callback: AdManagerCallback,
        reloadAd: Boolean = AdManageKitConfig.interstitialAutoReload
    ) {
        showAd(activity, callback, reloadAd)
    }


    /**
     * Shows an interstitial ad based on the specified time interval criteria.
     * Uses AdManageKitConfig.interstitialAutoReload to determine if ad should reload after showing.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    fun showInterstitialAdByTime(activity: Activity, callback: AdManagerCallback) {
        if (canShowAd()) {
            showAd(activity, callback, AdManageKitConfig.interstitialAutoReload)
        } else {
            callback.onNextAction()
        }
    }

    /**
     * Shows an interstitial ad based on the specified number of times it has been displayed.
     * Uses AdManageKitConfig.interstitialAutoReload to determine if ad should reload after showing.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param maxDisplayCount The maximum number of times the ad can be displayed.
     */
    fun showInterstitialAdByCount(
        activity: Activity, callback: AdManagerCallback, maxDisplayCount: Int
    ) {
        if (adDisplayCount < maxDisplayCount) {
            showAd(activity, callback, AdManageKitConfig.interstitialAutoReload)
        } else {
            callback.onNextAction()
        }
    }

    /**
     * Internal method to force fetch and display a fresh interstitial ad.
     * Tries to load a fresh ad first, falls back to cached ad on timeout/failure.
     *
     * Flow:
     * 1. Save existing cached ad as fallback (don't discard!)
     * 2. Try loading fresh ad
     * 3. On success → show fresh ad
     * 4. On timeout/failure → show cached fallback if available
     * 5. Only if both fail → onNextAction()
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param overrideAdUnitId Optional ad unit ID to use instead of the default (used by InterstitialAdBuilder).
     */
    private fun forceShowInterstitialInternal(activity: Activity, callback: AdManagerCallback, overrideAdUnitId: String? = null) {
        if (useWaterfall) { forceShowWaterfallInternal(activity, callback); return }

        // Prevent duplicate requests if dialog is already showing or ad is being fetched/displayed
        if (isFetchingWithDialog || isDisplayingAd) {
            Log.d("AdManager", "Skipping forceShowInterstitialInternal: already fetching or showing (isFetchingWithDialog=$isFetchingWithDialog, isDisplayingAd=$isDisplayingAd)")
            callback.onNextAction()
            return
        }

        // Check if a dialog is already displayed
        if (currentLoadingDialog?.dialog?.isShowing == true) {
            Log.d("AdManager", "Skipping forceShowInterstitialInternal: dialog already showing")
            callback.onNextAction()
            return
        }

        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onNextAction()
            return
        }

        // Use override ad unit ID if provided, otherwise fall back to default
        val currentAdUnitId = overrideAdUnitId ?: adUnitId ?: ""
        if (currentAdUnitId.isEmpty()) {
            Log.e("AdManager", "Ad unit ID is not set. Cannot force show interstitial.")
            callback.onNextAction()
            return
        }

        // Always update AdManager's adUnitId to the one being used
        // This ensures auto-reload and other operations use the correct ad unit
        adUnitId = currentAdUnitId

        initializeFirebase(activity)

        // IMPORTANT: Save existing cached ad as fallback - DON'T discard it!
        val cachedAdFallback = mInterstitialAd
        val hasCachedFallback = cachedAdFallback != null

        if (hasCachedFallback) {
            Log.d("AdManager", "Cached ad available as fallback while loading fresh")
            AdDebugUtils.logEvent(currentAdUnitId, "cachedFallbackAvailable", "Cached ad saved as fallback", true)
        }

        isFetchingWithDialog = true

        // Show beautiful loading dialog
        val dialogViews = showBeautifulLoadingDialog(activity)

        // Load fresh ad with timeout
        val adRequest = AdRequest.Builder().build()
        val timeoutMillis = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds
        var adLoaded = false
        var timeoutTriggered = false
        var dialogDismissed = false

        // Helper to safely dismiss dialog only once (ensures main thread execution)
        fun dismissDialogOnce(onDismissed: () -> Unit) {
            Handler(Looper.getMainLooper()).post {
                isFetchingWithDialog = false
                if (!dialogDismissed) {
                    dialogDismissed = true
                    animateDialogDismissal(dialogViews, onDismissed)
                } else {
                    onDismissed()
                }
            }
        }

        // Helper to show cached fallback ad
        fun showCachedFallback(): Boolean {
            if (cachedAdFallback != null) {
                Log.d("AdManager", "Showing cached fallback ad")
                AdDebugUtils.logEvent(currentAdUnitId, "showingCachedFallback", "Fresh load failed, showing cached fallback", true)
                mInterstitialAd = cachedAdFallback
                val effectiveAutoReload = AdManageKitConfig.interstitialAutoReload
                dismissDialogOnce {
                    showAd(activity, callback, effectiveAutoReload) // Reload after showing fallback
                }
                return true
            }
            return false
        }

        InterstitialAd.load(activity, currentAdUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                // Always save the ad
                mInterstitialAd = interstitialAd
                adLoaded = true

                if (timeoutTriggered) {
                    // Timeout already triggered, but ad loaded - it's saved for next time
                    Log.d("AdManager", "Ad loaded after timeout - saved for next show")
                    AdDebugUtils.logEvent(currentAdUnitId, "onAdLoadedAfterTimeout", "Ad saved for next show", true)
                    return
                }

                Log.d("AdManager", "Fresh interstitial ad loaded for force show")
                AdDebugUtils.logEvent(currentAdUnitId, "onAdLoaded", "Fresh interstitial loaded for force show", true)

                val effectiveAutoReload = AdManageKitConfig.interstitialAutoReload
                // Animate dialog dismissal and show fresh ad
                dismissDialogOnce {
                    showAd(activity, callback, effectiveAutoReload)
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                if (timeoutTriggered) return
                adLoaded = true
                Log.e("AdManager", "Failed to load fresh interstitial ad: ${loadAdError.message}")
                AdDebugUtils.logEvent(currentAdUnitId, "onFailedToLoad", "Fresh interstitial failed: ${loadAdError.message}", false)

                // Log Firebase event
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, currentAdUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", loadAdError.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)

                // Try cached fallback before giving up
                if (!showCachedFallback()) {
                    dismissDialogOnce {
                        callback.onNextAction()
                    }
                }
            }
        })

        // Timeout handler
        Handler(Looper.getMainLooper()).postDelayed({
            if (!adLoaded) {
                timeoutTriggered = true
                Log.d("AdManager", "Force show interstitial timed out")
                AdDebugUtils.logEvent(currentAdUnitId, "onTimeout", "Force show interstitial timed out", false)

                // Try cached fallback on timeout
                if (!showCachedFallback()) {
                    dismissDialogOnce {
                        callback.onNextAction()
                    }
                }
            }
        }, timeoutMillis)
    }

    /**
     * Apply dialog customizations from config
     */
    private fun applyDialogCustomizations(dialogView: View, overlay: View, contentCard: View) {
        // Apply background color
        if (AdManageKitConfig.dialogBackgroundColor != 0) {
            dialogView.setBackgroundColor(AdManageKitConfig.dialogBackgroundColor)
        }

        // Apply overlay color
        if (AdManageKitConfig.dialogOverlayColor != 0) {
            overlay.setBackgroundColor(AdManageKitConfig.dialogOverlayColor)
            overlay.visibility = View.VISIBLE
        } else {
            // Hide overlay if color is 0
            overlay.visibility = View.GONE
        }

        // Apply card background color
        if (AdManageKitConfig.dialogCardBackgroundColor != 0) {
            contentCard.setBackgroundColor(AdManageKitConfig.dialogCardBackgroundColor)
        }
    }

    /**
     * Helper function to animate loading dots
     */
    private fun animateDot(dot: View, startDelay: Long) {
        dot.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(startDelay)
            .withEndAction {
                dot.animate()
                    .alpha(0.3f)
                    .setDuration(600)
                    .withEndAction {
                        animateDot(dot, 0)
                    }
                    .start()
            }
            .start()
    }

    /**
     * Helper function to animate dialog dismissal
     */
    private fun animateDialogDismissal(dialogViews: LoadingDialogViews, onComplete: () -> Unit) {
        // Animate content card out
        dialogViews.contentCard.animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(250)
            .start()

        // Animate overlay out
        dialogViews.overlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                dismissDialogSafely(dialogViews.dialog)
                // Clear the reference if this is the current dialog
                if (currentLoadingDialog?.dialog == dialogViews.dialog) {
                    currentLoadingDialog = null
                }
                onComplete()
            }
            .start()
    }

    /**
     * Safely dismiss dialog handling window detachment
     */
    private fun dismissDialogSafely(dialog: Dialog) {
        try {
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        } catch (e: IllegalArgumentException) {
            // View not attached to window manager - activity was destroyed
            Log.w("AdManager", "Dialog dismiss failed - window not attached: ${e.message}")
        } catch (e: Exception) {
            Log.e("AdManager", "Error dismissing dialog: ${e.message}")
        }
    }

    /**
     * Shows an interstitial ad based on the specified time interval criteria.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param reloadAd A boolean indicating whether to reload the ad after it's shown.
     */
    fun showInterstitialAdByTime(
        activity: Activity, callback: AdManagerCallback, reloadAd: Boolean
    ) {
        if (canShowAd()) {
            showAd(activity, callback, reloadAd)
        } else {
            callback.onNextAction()
        }
    }

    /**
     * Shows an interstitial ad based on the specified number of times it has been displayed.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param maxDisplayCount The maximum number of times the ad can be displayed.
     * @param reloadAd A boolean indicating whether to reload the ad after it's shown.
     */
    fun showInterstitialAdByCount(
        activity: Activity, callback: AdManagerCallback, maxDisplayCount: Int, reloadAd: Boolean
    ) {
        if (adDisplayCount < maxDisplayCount) {
            showAd(activity, callback, reloadAd)
        } else {
            callback.onNextAction()
        }
    }

    /**
     * Check if ANY interstitial ad is ready in the pool.
     * @return true if at least one ad is available to show
     */
    fun isReady(): Boolean {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) return false
        if (useWaterfall) return interstitialWaterfall?.isAdReady() == true

        // Check pool first, then legacy single ad
        return adPool.isNotEmpty() || mInterstitialAd != null
    }

    /**
     * Check if a specific ad unit has an ad ready.
     * @param adUnitId The ad unit ID to check
     * @return true if this specific ad unit has an ad ready
     */
    fun isReady(adUnitId: String): Boolean {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) return false
        if (useWaterfall) return interstitialWaterfall?.isAdReady() == true

        return adPool.containsKey(adUnitId)
    }

    /**
     * Get the number of ads currently in the pool.
     * @return Number of ready ads
     */
    fun getPoolSize(): Int {
        if (useWaterfall) return if (interstitialWaterfall?.isAdReady() == true) 1 else 0
        return adPool.size
    }

    /**
     * Get all ad unit IDs that currently have ads ready.
     * @return Set of ad unit IDs with ready ads
     */
    fun getReadyAdUnits(): Set<String> = adPool.keys.toSet()

    fun isDisplayingAd(): Boolean {
        return isDisplayingAd
    }

    /**
     * Check if the loading dialog is currently showing.
     * Used to prevent other UI (like app open ads) from appearing on top.
     */
    fun isLoadingDialogShowing(): Boolean {
        return try {
            currentLoadingDialog?.dialog?.isShowing == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if either an ad is displaying OR the loading dialog is showing.
     * Useful for preventing app open ads from appearing during interstitial flow.
     */
    fun isAdOrDialogShowing(): Boolean {
        return isDisplayingAd || isLoadingDialogShowing() || isFetchingWithDialog
    }

    fun setAdInterval(intervalMillis: Long) {
        this.adIntervalMillis = intervalMillis
        if (AdManageKitConfig.debugMode) {
            AdDebugUtils.logEvent("", "setAdInterval", "Ad interval set to ${intervalMillis}ms", true)
        }
    }

    /**
     * Set the primary ad unit ID directly.
     * Used by InterstitialAdBuilder to ensure AdManager has the correct ad unit
     * before force loading operations.
     *
     * @param adUnitId The ad unit ID to set
     */
    fun setAdUnitId(adUnitId: String?) {
        this.adUnitId = adUnitId
    }

    fun getAdDisplayCount(): Int {
        return adDisplayCount
    }

    fun setAdDisplayCount(count: Int) {
        this.adDisplayCount = count
    }

    /**
     * Increment and return the call counter for a specific ad unit.
     * Used by InterstitialAdBuilder for everyNthTime feature.
     *
     * @param adUnitId The ad unit ID
     * @return The new call count after incrementing
     */
    fun incrementCallCount(adUnitId: String): Int {
        val newCount = (callCounters[adUnitId] ?: 0) + 1
        callCounters[adUnitId] = newCount
        return newCount
    }

    /**
     * Get the current call count for a specific ad unit.
     *
     * @param adUnitId The ad unit ID
     * @return The current call count (0 if never called)
     */
    fun getCallCount(adUnitId: String): Int {
        return callCounters[adUnitId] ?: 0
    }

    /**
     * Reset the call counter for a specific ad unit.
     *
     * @param adUnitId The ad unit ID
     */
    fun resetCallCount(adUnitId: String) {
        callCounters.remove(adUnitId)
    }

    /**
     * Reset all call counters.
     */
    fun resetAllCallCounts() {
        callCounters.clear()
    }

    private fun canShowAd(): Boolean {
        val elapsed = System.currentTimeMillis() - lastAdShowTime
        Log.d("AdManager", "Time since last ad: $elapsed milliseconds")
        
        // Use adaptive intervals if enabled
        val effectiveInterval = if (AdManageKitConfig.enableAdaptiveIntervals) {
            // Increase interval if recent failures, decrease if recent successes
            // This is a simplified adaptive logic
            adIntervalMillis
        } else {
            adIntervalMillis
        }
        
        return elapsed > effectiveInterval
    }
    
    /**
     * Get any available ad from the pool.
     * Returns the ad and its ad unit ID, or null if no ad is available.
     */
    private fun getAnyAvailableAd(): Pair<String, InterstitialAd>? {
        // Try pool first
        for ((unitId, ad) in adPool) {
            return Pair(unitId, ad)
        }

        // Fallback to legacy single ad
        mInterstitialAd?.let { ad ->
            adUnitId?.let { unitId ->
                return Pair(unitId, ad)
            }
        }

        return null
    }

    private fun showAd(activity: Activity, callback: AdManagerCallback, reloadAd: Boolean) {
        if (useWaterfall) { showWaterfallAd(activity, callback, reloadAd); return }

        if (!isReady()) {
            callback.onNextAction()
            return
        }

        // Get any available ad from the pool
        val adPair = getAnyAvailableAd()
        if (adPair == null) {
            Log.w("AdManager", "No ad available in pool")
            callback.onNextAction()
            return
        }

        val (shownAdUnitId, interstitialAd) = adPair

        // Remove from pool (it's being shown)
        adPool.remove(shownAdUnitId)

        Log.d("AdManager", "Showing ad from unit: $shownAdUnitId (remaining in pool: ${adPool.size})")
        AdDebugUtils.logEvent(shownAdUnitId, "showingFromPool", "Pool size after: ${adPool.size}", true)

        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isDisplayingAd = false
                mInterstitialAd = null
                AdDebugUtils.logEvent(shownAdUnitId, "onAdDismissed", "Interstitial ad dismissed", true)
                callback.onNextAction()

                // Log Firebase event for ad dismissed
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, shownAdUnitId)
                }
                firebaseAnalytics.logEvent("ad_dismissed", params)

                // CRITICAL: Reload the SPECIFIC unit that was shown
                if (reloadAd) {
                    retryAttempts.remove(shownAdUnitId)
                    Log.d("AdManager", "Auto-reloading ad unit: $shownAdUnitId")
                    loadInterstitialAd(activity, shownAdUnitId)
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isDisplayingAd = false
                mInterstitialAd = null
                Log.e("AdManager", "Failed to show full-screen content: ${adError.message}")
                AdDebugUtils.logEvent(shownAdUnitId, "onFailedToShow", "Interstitial failed to show: ${adError.message}", false)
                callback.onNextAction()

                // Log Firebase event for ad failed to show
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, shownAdUnitId)
                    putString("ad_error_code", adError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", adError.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_show", params)

                // Still try to reload on show failure
                if (reloadAd) {
                    retryAttempts.remove(shownAdUnitId)
                    loadInterstitialAd(activity, shownAdUnitId)
                }
            }

            override fun onAdShowedFullScreenContent() {
                isDisplayingAd = true
                lastAdShowTime = System.currentTimeMillis()
                adDisplayCount++
                AdDebugUtils.logEvent(shownAdUnitId, "onAdImpression", "Interstitial ad shown", true)

                // Notify callback that ad is now showing
                callback.onAdShowed()

                // Log Firebase event for ad impression (standard event)
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, shownAdUnitId)
                }
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)

                // Log detailed impression with per-user tracking
                logAdImpression(shownAdUnitId, "interstitial")
            }
        }

        interstitialAd.onPaidEventListener = OnPaidEventListener { adValue ->
            val adValueInStandardUnits = adValue.valueMicros / 1000000.0

            // Log Firebase event for paid event
            val params = Bundle()
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, shownAdUnitId)
            params.putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
            params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            firebaseAnalytics.logEvent("ad_paid_event", params)
        }

        interstitialAd.show(activity)
    }
    
    /**
     * Check if retry should be attempted for the given ad unit
     */
    private fun shouldAttemptRetry(adUnitId: String): Boolean {
        val currentAttempts = retryAttempts[adUnitId] ?: 0
        return currentAttempts < AdManageKitConfig.maxRetryAttempts &&
               !AdRetryManager.getInstance().hasActiveRetry(adUnitId)
    }

    /**
     * Proactively preload an interstitial ad to improve show rate.
     * Call this during natural pauses in your app (after user actions, screen loads, etc.)
     *
     * @param context The context
     * @param adUnitId The ad unit ID to preload
     */
    fun preloadAd(context: Context, adUnitId: String) {
        if (!isReady()) {
            loadInterstitialAd(context, adUnitId)
            if (AdManageKitConfig.debugMode) {
                AdDebugUtils.logEvent(adUnitId, "preload", "Preloading interstitial ad", true)
            }
        }
    }

    /**
     * Reset ad display counters and intervals for fresh start.
     * Useful when you want to show ads more frequently.
     */
    fun resetAdThrottling() {
        lastAdShowTime = 0
        adDisplayCount = 0
        retryAttempts.clear()
        if (AdManageKitConfig.debugMode) {
            AdDebugUtils.logEvent("", "resetThrottling", "Ad throttling reset", true)
        }
    }

    /**
     * Configure more aggressive ad loading for maximum show rate.
     * Call this in Application onCreate() or before first ad load.
     */
    fun enableAggressiveAdLoading() {
        // Reduce interval to minimum (5 seconds)
        setAdInterval(5000L)

        if (AdManageKitConfig.debugMode) {
            AdDebugUtils.logEvent("", "aggressiveMode", "Aggressive ad loading enabled (5s interval)", true)
        }
    }

    /**
     * Get timestamp of last ad show
     */
    fun getLastAdShowTime(): Long {
        return lastAdShowTime
    }

    /**
     * Get time elapsed since last ad show in milliseconds
     */
    fun getTimeSinceLastAd(): Long {
        return System.currentTimeMillis() - lastAdShowTime
    }

    /**
     * Check if an ad is currently being loaded
     */
    fun isLoading(): Boolean {
        if (useWaterfall) return isWaterfallLoading
        return isAdLoading
    }

    /**
     * Smart show method for splash screens and critical moments.
     *
     * Behavior:
     * 1. If ad is READY → Shows immediately
     * 2. If ad is LOADING → Waits for it with timeout, then shows
     * 3. If NEITHER → Force loads with dialog
     *
     * Perfect for splash screens where you want to:
     * - Preload ad in Application.onCreate()
     * - Show on splash with smart waiting
     *
     * @param activity The activity to show the ad
     * @param callback Callback for ad events
     * @param timeoutMillis Maximum time to wait for loading ad (default: 5 seconds)
     * @param showDialogIfLoading Whether to show loading dialog while waiting (default: true)
     */
    fun showOrWaitForAd(
        activity: Activity,
        callback: AdManagerCallback,
        timeoutMillis: Long = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds,
        showDialogIfLoading: Boolean = true
    ) {
        if (useWaterfall) { waterfallShowOrWait(activity, callback, timeoutMillis, showDialogIfLoading); return }

        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onNextAction()
            return
        }

        val currentAdUnitId = adUnitId ?: ""
        if (currentAdUnitId.isEmpty()) {
            Log.e("AdManager", "Ad unit ID is not set. Call loadInterstitialAd first or use forceShowInterstitial.")
            callback.onNextAction()
            return
        }

        val effectiveAutoReload = AdManageKitConfig.interstitialAutoReload
        when {
            // Case 1: Ad is ready - show immediately
            isReady() -> {
                AdDebugUtils.logEvent(currentAdUnitId, "showOrWait", "Ad ready, showing immediately", true)
                showAd(activity, callback, effectiveAutoReload)
            }

            // Case 2: Ad is loading - wait for it with timeout
            isAdLoading -> {
                AdDebugUtils.logEvent(currentAdUnitId, "showOrWait", "Ad loading, waiting with timeout ${timeoutMillis}ms", true)
                waitForLoadingAd(activity, callback, timeoutMillis, showDialogIfLoading, effectiveAutoReload)
            }

            // Case 3: Neither ready nor loading - force load
            else -> {
                AdDebugUtils.logEvent(currentAdUnitId, "showOrWait", "Ad not loaded/loading, force fetching", true)
                forceShowInterstitialInternal(activity, callback)
            }
        }
    }

    /**
     * Wait for a currently loading ad with timeout
     */
    private fun waitForLoadingAd(
        activity: Activity,
        callback: AdManagerCallback,
        timeoutMillis: Long,
        showDialog: Boolean,
        reloadAd: Boolean = AdManageKitConfig.interstitialAutoReload
    ) {
        val checkIntervalMs = 100L
        var elapsedMs = 0L
        val handler = Handler(Looper.getMainLooper())

        // Show dialog if requested
        var dialogViews: LoadingDialogViews? = null
        if (showDialog) {
            dialogViews = showBeautifulLoadingDialog(activity)
        }

        val checkRunnable = object : Runnable {
            override fun run() {
                when {
                    // Ad became ready - show it
                    isReady() -> {
                        AdDebugUtils.logEvent(adUnitId ?: "", "waitForAd", "Ad loaded after ${elapsedMs}ms, showing", true)
                        if (dialogViews != null) {
                            animateDialogDismissal(dialogViews) {
                                showAd(activity, callback, reloadAd)
                            }
                        } else {
                            showAd(activity, callback, reloadAd)
                        }
                    }

                    // Timeout reached
                    elapsedMs >= timeoutMillis -> {
                        AdDebugUtils.logEvent(adUnitId ?: "", "waitForAd", "Timeout after ${elapsedMs}ms, proceeding", false)
                        if (dialogViews != null) {
                            animateDialogDismissal(dialogViews) {
                                callback.onNextAction()
                            }
                        } else {
                            callback.onNextAction()
                        }
                    }

                    // Still loading and not timed out - check again
                    isAdLoading -> {
                        elapsedMs += checkIntervalMs
                        handler.postDelayed(this, checkIntervalMs)
                    }

                    // Loading failed (not loading anymore, not ready) - force fetch or proceed
                    else -> {
                        AdDebugUtils.logEvent(adUnitId ?: "", "waitForAd", "Loading failed, proceeding", false)
                        if (dialogViews != null) {
                            animateDialogDismissal(dialogViews) {
                                callback.onNextAction()
                            }
                        } else {
                            callback.onNextAction()
                        }
                    }
                }
            }
        }

        handler.post(checkRunnable)
    }

    // =================== WATERFALL HELPERS ===================

    private fun resolveAdUnit(logicalName: String): (com.i2hammad.admanagekit.core.ad.AdProvider) -> String? = { provider ->
        AdUnitMapping.getAdUnitId(logicalName, provider)
            ?: logicalName.takeIf { provider == AdProvider.ADMOB }
    }

    private fun createWaterfall(adUnitId: String): InterstitialWaterfall {
        return InterstitialWaterfall(
            providers = AdProviderConfig.getInterstitialChain(),
            adUnitResolver = resolveAdUnit(adUnitId)
        )
    }

    private fun loadViaWaterfall(context: Context, adUnitId: String) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) return
        if (isWaterfallLoading) return
        if (interstitialWaterfall?.isAdReady() == true) return

        if (this.adUnitId == null) this.adUnitId = adUnitId
        initializeFirebase(context)
        isWaterfallLoading = true
        isAdLoading = true
        logAdRequest(adUnitId, "interstitial")

        val waterfall = createWaterfall(adUnitId)
        interstitialWaterfall = waterfall

        waterfall.load(context, object : InterstitialAdProvider.InterstitialAdCallback {
            override fun onAdLoaded() {
                isWaterfallLoading = false
                isAdLoading = false
                retryAttempts.remove(adUnitId)
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Interstitial waterfall ad loaded", true)
                logAdFill(adUnitId, "interstitial")
            }

            override fun onAdFailedToLoad(error: AdKitAdError) {
                isWaterfallLoading = false
                isAdLoading = false
                interstitialWaterfall = null
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Interstitial waterfall failed: ${error.message}", false)

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", error.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", error.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)

                if (AdManageKitConfig.autoRetryFailedAds && shouldAttemptRetry(adUnitId)) {
                    val currentAttempt = retryAttempts[adUnitId] ?: 0
                    retryAttempts[adUnitId] = currentAttempt + 1
                    AdRetryManager.getInstance().scheduleRetry(
                        adUnitId = adUnitId,
                        attempt = currentAttempt,
                        maxAttempts = AdManageKitConfig.maxRetryAttempts
                    ) {
                        loadViaWaterfall(context, adUnitId)
                    }
                }
            }
        })
    }

    private fun loadWaterfallForSplash(
        context: Context,
        adUnitId: String,
        timeoutMillis: Long,
        callback: AdManagerCallback
    ) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) { callback.onNextAction(); return }
        if (interstitialWaterfall?.isAdReady() == true) {
            this.adUnitId = adUnitId
            callback.onNextAction(); callback.onAdLoaded(); return
        }

        this.adUnitId = adUnitId
        initializeFirebase(context)
        isWaterfallLoading = true
        isAdLoading = true
        var callbackCalled = false

        val waterfall = createWaterfall(adUnitId)
        interstitialWaterfall = waterfall

        waterfall.load(context, object : InterstitialAdProvider.InterstitialAdCallback {
            override fun onAdLoaded() {
                isWaterfallLoading = false
                isAdLoading = false
                if (!callbackCalled) {
                    callbackCalled = true
                    callback.onNextAction(); callback.onAdLoaded()
                }
            }

            override fun onAdFailedToLoad(error: AdKitAdError) {
                isWaterfallLoading = false
                isAdLoading = false
                interstitialWaterfall = null
                if (!callbackCalled) {
                    callbackCalled = true
                    callback.onNextAction()
                    callback.onFailedToLoad(
                        LoadAdError(error.code, error.message, error.domain, null, null)
                    )
                }
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            if (!callbackCalled) {
                callbackCalled = true
                callback.onNextAction()
            }
        }, timeoutMillis)
    }

    private fun showWaterfallAd(activity: Activity, callback: AdManagerCallback, reloadAd: Boolean) {
        val waterfall = interstitialWaterfall
        if (waterfall == null || !waterfall.isAdReady()) {
            callback.onNextAction()
            return
        }

        waterfall.show(activity, object : InterstitialAdProvider.InterstitialShowCallback {
            override fun onAdShowed() {
                isDisplayingAd = true
                lastAdShowTime = System.currentTimeMillis()
                adDisplayCount++
                callback.onAdShowed()
                logAdImpression(adUnitId ?: "", "interstitial")
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId ?: "")
                }
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
            }

            override fun onAdDismissed() {
                isDisplayingAd = false
                interstitialWaterfall = null
                callback.onNextAction()
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId ?: "")
                }
                firebaseAnalytics.logEvent("ad_dismissed", params)
                if (reloadAd) {
                    adUnitId?.let { loadViaWaterfall(activity, it) }
                }
            }

            override fun onAdFailedToShow(error: AdKitAdError) {
                isDisplayingAd = false
                interstitialWaterfall = null
                callback.onNextAction()
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId ?: "")
                    putString("ad_error_code", error.code.toString())
                }
                firebaseAnalytics.logEvent("ad_failed_to_show", params)
                if (reloadAd) {
                    adUnitId?.let { loadViaWaterfall(activity, it) }
                }
            }

            override fun onAdImpression() {}
            override fun onAdClicked() {}

            override fun onPaidEvent(adValue: AdKitAdValue) {
                val adValueInStandardUnits = adValue.valueMicros / 1000000.0
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId ?: "")
                    putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                    putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                }
                firebaseAnalytics.logEvent("ad_paid_event", params)
            }
        })
    }

    private fun forceShowWaterfallInternal(activity: Activity, callback: AdManagerCallback) {
        if (isFetchingWithDialog || isDisplayingAd) { callback.onNextAction(); return }
        if (currentLoadingDialog?.dialog?.isShowing == true) { callback.onNextAction(); return }

        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) { callback.onNextAction(); return }

        val currentAdUnitId = adUnitId ?: ""
        if (currentAdUnitId.isEmpty()) { callback.onNextAction(); return }

        initializeFirebase(activity)

        // Check if waterfall ad is already ready
        if (interstitialWaterfall?.isAdReady() == true) {
            val effectiveAutoReload = AdManageKitConfig.interstitialAutoReload
            showWaterfallAd(activity, callback, effectiveAutoReload)
            return
        }

        isFetchingWithDialog = true
        val dialogViews = showBeautifulLoadingDialog(activity)
        val timeoutMillis = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds
        var resolved = false
        var dialogDismissed = false

        fun dismissDialogOnce(onDismissed: () -> Unit) {
            Handler(Looper.getMainLooper()).post {
                isFetchingWithDialog = false
                if (!dialogDismissed) {
                    dialogDismissed = true
                    animateDialogDismissal(dialogViews, onDismissed)
                } else {
                    onDismissed()
                }
            }
        }

        val waterfall = createWaterfall(currentAdUnitId)
        interstitialWaterfall = waterfall
        isWaterfallLoading = true
        isAdLoading = true

        waterfall.load(activity, object : InterstitialAdProvider.InterstitialAdCallback {
            override fun onAdLoaded() {
                isWaterfallLoading = false
                isAdLoading = false
                if (resolved) return
                resolved = true
                val effectiveAutoReload = AdManageKitConfig.interstitialAutoReload
                dismissDialogOnce {
                    showWaterfallAd(activity, callback, effectiveAutoReload)
                }
            }

            override fun onAdFailedToLoad(error: AdKitAdError) {
                isWaterfallLoading = false
                isAdLoading = false
                interstitialWaterfall = null
                if (resolved) return
                resolved = true
                dismissDialogOnce { callback.onNextAction() }
            }
        })

        Handler(Looper.getMainLooper()).postDelayed({
            if (!resolved) {
                resolved = true
                dismissDialogOnce { callback.onNextAction() }
            }
        }, timeoutMillis)
    }

    private fun waterfallShowOrWait(
        activity: Activity,
        callback: AdManagerCallback,
        timeoutMillis: Long,
        showDialogIfLoading: Boolean
    ) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) { callback.onNextAction(); return }

        val currentAdUnitId = adUnitId ?: ""
        if (currentAdUnitId.isEmpty()) { callback.onNextAction(); return }

        val effectiveAutoReload = AdManageKitConfig.interstitialAutoReload

        when {
            interstitialWaterfall?.isAdReady() == true -> {
                showWaterfallAd(activity, callback, effectiveAutoReload)
            }
            isWaterfallLoading -> {
                // Wait for current load with timeout
                val checkIntervalMs = 100L
                var elapsedMs = 0L
                val handler = Handler(Looper.getMainLooper())
                var dialogViews: LoadingDialogViews? = null
                if (showDialogIfLoading) {
                    dialogViews = showBeautifulLoadingDialog(activity)
                }
                val checkRunnable = object : Runnable {
                    override fun run() {
                        when {
                            interstitialWaterfall?.isAdReady() == true -> {
                                if (dialogViews != null) {
                                    animateDialogDismissal(dialogViews) {
                                        showWaterfallAd(activity, callback, effectiveAutoReload)
                                    }
                                } else {
                                    showWaterfallAd(activity, callback, effectiveAutoReload)
                                }
                            }
                            elapsedMs >= timeoutMillis -> {
                                if (dialogViews != null) {
                                    animateDialogDismissal(dialogViews) { callback.onNextAction() }
                                } else { callback.onNextAction() }
                            }
                            isWaterfallLoading -> {
                                elapsedMs += checkIntervalMs
                                handler.postDelayed(this, checkIntervalMs)
                            }
                            else -> {
                                if (dialogViews != null) {
                                    animateDialogDismissal(dialogViews) { callback.onNextAction() }
                                } else { callback.onNextAction() }
                            }
                        }
                    }
                }
                handler.post(checkRunnable)
            }
            else -> {
                forceShowWaterfallInternal(activity, callback)
            }
        }
    }

    // =================== FIREBASE ANALYTICS TRACKING ===================

    // Counters for session-level tracking
    private var sessionAdRequests = 0
    private var sessionAdFills = 0
    private var sessionAdImpressions = 0

    /**
     * Log when an ad is requested (load initiated).
     * Use this to calculate fill rate = fills / requests
     */
    private fun logAdRequest(adUnitId: String, adType: String) {
        if (!::firebaseAnalytics.isInitialized) return

        sessionAdRequests++

        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
            putString("ad_type", adType)
            putLong("session_requests", sessionAdRequests.toLong())
            putInt("pool_size", adPool.size)
        }
        firebaseAnalytics.logEvent("ad_request", params)

        // Update user property for total lifetime requests
        firebaseAnalytics.setUserProperty("total_ad_requests", sessionAdRequests.toString())
    }

    /**
     * Log when an ad is successfully loaded (fill).
     * Fill rate = fills / requests
     */
    private fun logAdFill(adUnitId: String, adType: String) {
        if (!::firebaseAnalytics.isInitialized) return

        sessionAdFills++

        val fillRate = if (sessionAdRequests > 0) {
            (sessionAdFills.toFloat() / sessionAdRequests * 100).toInt()
        } else 0

        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
            putString("ad_type", adType)
            putLong("session_fills", sessionAdFills.toLong())
            putLong("session_requests", sessionAdRequests.toLong())
            putInt("fill_rate_percent", fillRate)
            putInt("pool_size", adPool.size)
        }
        firebaseAnalytics.logEvent("ad_fill", params)

        // Update user properties
        firebaseAnalytics.setUserProperty("total_ad_fills", sessionAdFills.toString())
        firebaseAnalytics.setUserProperty("ad_fill_rate", "$fillRate%")
    }

    /**
     * Log when an ad is actually shown to the user (impression).
     * Show rate = impressions / fills
     */
    private fun logAdImpression(adUnitId: String, adType: String) {
        if (!::firebaseAnalytics.isInitialized) return

        sessionAdImpressions++

        val showRate = if (sessionAdFills > 0) {
            (sessionAdImpressions.toFloat() / sessionAdFills * 100).toInt()
        } else 0

        val fillRate = if (sessionAdRequests > 0) {
            (sessionAdFills.toFloat() / sessionAdRequests * 100).toInt()
        } else 0

        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
            putString("ad_type", adType)
            putLong("session_impressions", sessionAdImpressions.toLong())
            putLong("session_fills", sessionAdFills.toLong())
            putLong("session_requests", sessionAdRequests.toLong())
            putInt("show_rate_percent", showRate)
            putInt("fill_rate_percent", fillRate)
            putLong("total_ads_shown", adDisplayCount.toLong())
        }
        firebaseAnalytics.logEvent("ad_impression_detailed", params)

        // Update user properties for segmentation
        firebaseAnalytics.setUserProperty("total_ads_shown", adDisplayCount.toString())
        firebaseAnalytics.setUserProperty("ad_show_rate", "$showRate%")
    }

    /**
     * Log when an ad is not shown (skipped, no fill, timeout, etc.)
     */
    private fun logAdNotShown(adUnitId: String, reason: String) {
        if (!::firebaseAnalytics.isInitialized) return

        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
            putString("reason", reason)
            putInt("pool_size", adPool.size)
            putLong("session_requests", sessionAdRequests.toLong())
            putLong("session_fills", sessionAdFills.toLong())
        }
        firebaseAnalytics.logEvent("ad_not_shown", params)
    }

    /**
     * Get current session ad statistics.
     * Useful for debugging and in-app analytics display.
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
            "total_ads_shown" to adDisplayCount,
            "pool_size" to adPool.size,
            "ready_units" to getReadyAdUnits()
        )
    }

    /**
     * Reset session ad statistics.
     * Call this at the start of a new session if needed.
     */
    fun resetAdStats() {
        sessionAdRequests = 0
        sessionAdFills = 0
        sessionAdImpressions = 0
    }
}
