package com.i2hammad.admanagekit.admob

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.i2hammad.admanagekit.config.AdLoadingStrategy
import com.i2hammad.admanagekit.config.AdManageKitConfig

/**
 * Modern, fluent API for loading and showing interstitial ads with comprehensive configuration.
 *
 * ## Basic Usage:
 * ```kotlin
 * // Simple show
 * InterstitialAdBuilder.with(activity)
 *     .adUnit("ca-app-pub-xxxxx/yyyyy")
 *     .show { /* next action */ }
 * ```
 *
 * ## Advanced Features:
 * ```kotlin
 * // Full configuration
 * InterstitialAdBuilder.with(activity)
 *     .adUnit("primary-unit")
 *     .fallback("backup-unit")                    // Fallback ad units
 *     .force()                                     // Force show (ignores interval)
 *     .loadingStrategy(AdLoadingStrategy.HYBRID)   // Set loading strategy
 *     .timeout(5000)                               // 5 second load timeout
 *     .onAdShown { /* analytics */ }               // Ad shown callback
 *     .onAdDismissed { /* cleanup */ }             // Ad dismissed callback
 *     .onFailed { error -> /* handle */ }          // Failure callback
 *     .show { /* next action */ }
 * ```
 *
 * ## Loading Strategies:
 * ```kotlin
 * // ON_DEMAND: Always fetch fresh ad with loading dialog
 * .loadingStrategy(AdLoadingStrategy.ON_DEMAND)
 *
 * // ONLY_CACHE: Only show if ad is ready, skip otherwise
 * .loadingStrategy(AdLoadingStrategy.ONLY_CACHE)
 *
 * // HYBRID: Show cached instantly, or fetch with dialog if needed (recommended)
 * .loadingStrategy(AdLoadingStrategy.HYBRID)
 * ```
 *
 * ## Frequency Controls:
 * ```kotlin
 * // Show every 3rd time
 * .everyNthTime(3)
 *
 * // Maximum 5 shows total
 * .maxShows(5)
 *
 * // Minimum 30 seconds between shows
 * .minIntervalSeconds(30)
 * ```
 *
 * ## Extension Functions:
 * ```kotlin
 * // Simple extension
 * activity.showInterstitialAd("ad-unit-id") { /* done */ }
 *
 * // Preload for later
 * activity.preloadInterstitialAd("ad-unit-id")
 * ```
 */
class InterstitialAdBuilder private constructor(private val activity: Activity) {

    private var primaryAdUnit: String? = null
    private val fallbackAdUnits = mutableListOf<String>()
    private var forceShow = false
    private var onAdShownCallback: (() -> Unit)? = null
    private var onAdDismissedCallback: (() -> Unit)? = null
    private var onAdFailedCallback: ((LoadAdError) -> Unit)? = null
    private var onAdLoadedCallback: (() -> Unit)? = null
    private var showLoadingDialog = false
    private var debugMode = false
    private var timeoutMs: Long? = null
    private var loadingStrategy: AdLoadingStrategy? = null

    // Count/Time configurations
    private var everyNthCall: Int? = null       // Show ad every Nth time show() is called
    private var maxShows: Int? = null           // Maximum total times to show ad
    private var minIntervalMs: Long? = null     // Minimum milliseconds between shows
    private var callCount = 0                   // Track calls to show() method
    private var waitForLoading = false          // Wait for loading ad before force fetching
    private var autoReload: Boolean? = null     // Auto reload after showing (null = use global config)

    companion object {
        /**
         * Start building an interstitial ad configuration
         */
        @JvmStatic
        fun with(activity: Activity): InterstitialAdBuilder {
            return InterstitialAdBuilder(activity)
        }
    }

    /**
     * Set the primary ad unit ID
     */
    fun adUnit(adUnitId: String): InterstitialAdBuilder {
        this.primaryAdUnit = adUnitId
        return this
    }

    /**
     * Add a fallback ad unit in case primary fails
     */
    fun fallback(adUnitId: String): InterstitialAdBuilder {
        fallbackAdUnits.add(adUnitId)
        return this
    }

    /**
     * Add multiple fallback ad units
     */
    fun fallbacks(vararg adUnitIds: String): InterstitialAdBuilder {
        fallbackAdUnits.addAll(adUnitIds)
        return this
    }

    /**
     * Force show the ad regardless of time interval
     */
    fun force(): InterstitialAdBuilder {
        this.forceShow = true
        return this
    }

    /**
     * Set timeout for ad loading (in milliseconds)
     */
    fun timeout(millis: Long): InterstitialAdBuilder {
        require(millis > 0) { "Timeout must be greater than 0" }
        this.timeoutMs = millis
        return this
    }

    /**
     * Set timeout for ad loading (in seconds, convenience method)
     */
    fun timeoutSeconds(seconds: Int): InterstitialAdBuilder {
        return timeout(seconds * 1000L)
    }

    /**
     * Callback when ad is successfully shown (Kotlin lambda)
     */
    fun onAdShown(callback: () -> Unit): InterstitialAdBuilder {
        this.onAdShownCallback = callback
        return this
    }

    /**
     * Callback when ad is successfully shown (Java-friendly)
     */
    fun onAdShown(listener: OnAdShownListener): InterstitialAdBuilder {
        this.onAdShownCallback = { listener.onAdShown() }
        return this
    }

    /**
     * Callback when ad is dismissed/closed (Kotlin lambda)
     */
    fun onAdDismissed(callback: () -> Unit): InterstitialAdBuilder {
        this.onAdDismissedCallback = callback
        return this
    }

    /**
     * Callback when ad is dismissed/closed (Java-friendly)
     */
    fun onAdDismissed(listener: OnAdDismissedListener): InterstitialAdBuilder {
        this.onAdDismissedCallback = { listener.onAdDismissed() }
        return this
    }

    /**
     * Callback when ad fails to load or show (Kotlin lambda)
     */
    fun onFailed(callback: (LoadAdError) -> Unit): InterstitialAdBuilder {
        this.onAdFailedCallback = callback
        return this
    }

    /**
     * Callback when ad fails to load or show (Java-friendly)
     */
    fun onFailed(listener: OnAdFailedListener): InterstitialAdBuilder {
        this.onAdFailedCallback = { error -> listener.onAdFailed(error) }
        return this
    }

    /**
     * Callback when ad is loaded and ready (Kotlin lambda)
     */
    fun onAdLoaded(callback: () -> Unit): InterstitialAdBuilder {
        this.onAdLoadedCallback = callback
        return this
    }

    /**
     * Callback when ad is loaded and ready (Java-friendly)
     */
    fun onAdLoaded(listener: OnAdLoadedListener): InterstitialAdBuilder {
        this.onAdLoadedCallback = { listener.onAdLoaded() }
        return this
    }

    /**
     * Show a loading dialog while ad is being fetched
     *
     * @deprecated Use loadingStrategy(AdLoadingStrategy.ON_DEMAND) instead
     */
    @Deprecated(
        message = "Use loadingStrategy(AdLoadingStrategy.ON_DEMAND) instead",
        replaceWith = ReplaceWith("loadingStrategy(AdLoadingStrategy.ON_DEMAND)")
    )
    fun withLoadingDialog(): InterstitialAdBuilder {
        this.showLoadingDialog = true
        return this
    }

    /**
     * Set the loading strategy for this interstitial ad.
     *
     * - **ON_DEMAND**: Always fetch fresh ad with loading dialog
     * - **ONLY_CACHE**: Only show if ad is ready (cached), skip if not available
     * - **HYBRID**: Check cache first, fetch with dialog if needed (recommended)
     *
     * Example:
     * ```kotlin
     * // For smooth gameplay - only show cached ads
     * .loadingStrategy(AdLoadingStrategy.ONLY_CACHE)
     *
     * // For important moments - always try to show
     * .loadingStrategy(AdLoadingStrategy.ON_DEMAND)
     *
     * // Balanced approach (default)
     * .loadingStrategy(AdLoadingStrategy.HYBRID)
     * ```
     *
     * @param strategy The loading strategy to use
     */
    fun loadingStrategy(strategy: AdLoadingStrategy): InterstitialAdBuilder {
        this.loadingStrategy = strategy
        return this
    }

    /**
     * Show ad only every Nth time show() is called.
     * Example: .everyNthTime(3) means show on 3rd, 6th, 9th call, etc.
     *
     * @param n Show ad every Nth call (must be > 0)
     */
    fun everyNthTime(n: Int): InterstitialAdBuilder {
        require(n > 0) { "everyNthTime must be greater than 0" }
        this.everyNthCall = n
        return this
    }

    /**
     * Limit maximum number of times ad can be shown globally.
     * Uses AdManager's global counter.
     *
     * @param max Maximum times to show ad
     */
    fun maxShows(max: Int): InterstitialAdBuilder {
        require(max > 0) { "maxShows must be greater than 0" }
        this.maxShows = max
        return this
    }

    /**
     * Set minimum time interval between ad shows.
     * Ad won't show if less time has passed since last show.
     *
     * @param millis Minimum milliseconds between shows
     */
    fun minInterval(millis: Long): InterstitialAdBuilder {
        require(millis > 0) { "minInterval must be greater than 0" }
        this.minIntervalMs = millis
        return this
    }

    /**
     * Set minimum time interval between ad shows (convenience method).
     *
     * @param seconds Minimum seconds between shows
     */
    fun minIntervalSeconds(seconds: Int): InterstitialAdBuilder {
        return minInterval(seconds * 1000L)
    }

    /**
     * Enable debug logging for this ad
     */
    fun debug(): InterstitialAdBuilder {
        this.debugMode = true
        return this
    }

    /**
     * Control whether to automatically reload the next ad after showing.
     * Default is true.
     *
     * @param reload true to auto-reload after showing (default), false to disable
     */
    fun autoReload(reload: Boolean = true): InterstitialAdBuilder {
        this.autoReload = reload
        return this
    }

    /**
     * Smart wait behavior for splash screens and critical moments.
     *
     * When enabled:
     * 1. If ad is READY → Shows immediately
     * 2. If ad is LOADING → Waits for it with timeout, then shows
     * 3. If NEITHER → Force loads with dialog
     *
     * Perfect for splash screens where you preload in Application.onCreate()
     * and want to show on splash with smart waiting.
     *
     * Example:
     * ```kotlin
     * // In Application.onCreate()
     * AdManager.getInstance().loadInterstitialAd(this, adUnitId)
     *
     * // In SplashActivity
     * InterstitialAdBuilder.with(activity)
     *     .adUnit(adUnitId)
     *     .waitForLoading()      // Wait if loading, force if not
     *     .timeout(5000)         // 5 second max wait
     *     .show { startMainActivity() }
     * ```
     */
    fun waitForLoading(): InterstitialAdBuilder {
        this.waitForLoading = true
        return this
    }

    /**
     * Show the ad and execute next action when done (Kotlin lambda)
     */
    fun show(onComplete: () -> Unit) {
        showInternal(onComplete)
    }

    /**
     * Show the ad and execute next action when done (Java-friendly)
     */
    fun show(listener: OnAdCompleteListener) {
        showInternal { listener.onComplete() }
    }

    /**
     * Show the ad with combined callback (Java-friendly)
     */
    fun show(callback: InterstitialAdCallback) {
        this.onAdLoadedCallback = { callback.onAdLoaded() }
        this.onAdShownCallback = { callback.onAdShown() }
        this.onAdFailedCallback = { error -> callback.onAdFailed(error) }
        showInternal { callback.onComplete() }
    }

    /**
     * Internal show implementation
     */
    private fun showInternal(onComplete: () -> Unit) {
        require(primaryAdUnit != null) { "Ad unit ID must be set using adUnit()" }

        val adManager = AdManager.getInstance()
        callCount++  // Increment call counter

        // Check everyNthTime - skip if not Nth call
        everyNthCall?.let { nth ->
            if (callCount % nth != 0) {
                if (debugMode) {
                    android.util.Log.d(
                        "InterstitialBuilder",
                        "Not Nth time (call #$callCount, showing every ${nth}th), skipping ad"
                    )
                }
                onComplete()
                return
            }
        }

        // Check maxShows limit
        maxShows?.let { max ->
            if (adManager.getAdDisplayCount() >= max) {
                if (debugMode) {
                    android.util.Log.d(
                        "InterstitialBuilder",
                        "Max shows limit reached (${adManager.getAdDisplayCount()}/$max), skipping ad"
                    )
                }
                onComplete()
                return
            }
        }

        // Check minimum interval
        minIntervalMs?.let { minInterval ->
            val lastShowTime = adManager.getLastAdShowTime()
            val timeSinceLastAd = System.currentTimeMillis() - lastShowTime
            if (!forceShow && timeSinceLastAd < minInterval) {
                if (debugMode) {
                    val remainingMs = minInterval - timeSinceLastAd
                    android.util.Log.d(
                        "InterstitialBuilder",
                        "Min interval not met (${timeSinceLastAd}ms < ${minInterval}ms, ${remainingMs}ms remaining), skipping ad"
                    )
                }
                onComplete()
                return
            }
        }

        // Create callback for showing the ad
        val showCallback = object : AdManagerCallback() {
            override fun onAdLoaded() {
                if (debugMode) android.util.Log.d("InterstitialBuilder", "Ad shown successfully")
                onAdShownCallback?.invoke()
            }

            override fun onNextAction() {
                onAdDismissedCallback?.invoke()
                onComplete()
            }

            override fun onFailedToLoad(error: com.google.android.libraries.ads.mobile.sdk.common.AdError?) {
                if (debugMode) android.util.Log.e("InterstitialBuilder", "Ad failed: ${error?.message}")
                error?.let {
                    val loadError = LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, it.message, null)
                    onAdFailedCallback?.invoke(loadError)
                }
                onComplete()
            }
        }

        // Handle waitForLoading mode (smart splash screen behavior)
        if (waitForLoading) {
            if (debugMode) android.util.Log.d("InterstitialBuilder", "WAIT_FOR_LOADING: Using smart wait behavior")
            val effectiveTimeout = timeoutMs ?: AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds
            adManager.showOrWaitForAd(activity, showCallback, effectiveTimeout, showLoadingDialog)
            return
        }

        // Determine effective loading strategy
        val effectiveStrategy = loadingStrategy ?: AdManageKitConfig.interstitialLoadingStrategy

        if (debugMode) {
            android.util.Log.d("InterstitialBuilder", "Using loading strategy: $effectiveStrategy")
        }

        // Handle different loading strategies
        when (effectiveStrategy) {
            AdLoadingStrategy.ON_DEMAND -> {
                // Always fetch fresh ad with loading dialog
                if (debugMode) android.util.Log.d("InterstitialBuilder", "ON_DEMAND: Fetching fresh ad with dialog")
                showWithDialog(showCallback, onComplete)
            }

            AdLoadingStrategy.ONLY_CACHE -> {
                // Only show if ad is already ready (cached), skip if not
                val effectiveAutoReload = autoReload ?: AdManageKitConfig.interstitialAutoReload
                if (adManager.isReady()) {
                    if (debugMode) android.util.Log.d(
                        "InterstitialBuilder",
                        "ONLY_CACHE: Ad ready, showing cached ad (autoReload=$effectiveAutoReload)"
                    )
                    // Use showInterstitialIfReady to show CACHED ad (not forceShow which fetches fresh!)
                    adManager.showInterstitialIfReady(activity, showCallback, effectiveAutoReload)
                } else {
                    if (debugMode) android.util.Log.d("InterstitialBuilder", "ONLY_CACHE: No ad ready, skipping")
                    onComplete()
                }
            }

            AdLoadingStrategy.HYBRID -> {
                // Check cache first, fetch with dialog if needed
                val effectiveAutoReload = autoReload ?: AdManageKitConfig.interstitialAutoReload
                if (adManager.isReady()) {
                    // Ad is ready, show cached ad instantly
                    if (debugMode) android.util.Log.d(
                        "InterstitialBuilder",
                        "HYBRID: Ad cached, showing cached ad (autoReload=$effectiveAutoReload)"
                    )
                    // Use showInterstitialIfReady to show CACHED ad (not forceShow which fetches fresh!)
                    adManager.showInterstitialIfReady(activity, showCallback, effectiveAutoReload)
                } else {
                    // Ad not ready, fetch with dialog
                    if (debugMode) android.util.Log.d(
                        "InterstitialBuilder",
                        "HYBRID: Ad not cached, fetching with dialog"
                    )
                    showWithDialog(showCallback, onComplete)
                }
            }

            AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK -> {
                // Try fresh first, fall back to cache if fresh load fails/times out
                // AdManager.forceShowInterstitialInternal now preserves cached ad as fallback
                if (debugMode) android.util.Log.d(
                    "InterstitialBuilder",
                    "FRESH_WITH_CACHE_FALLBACK: Fetching fresh, cached ad as fallback"
                )
                showWithDialog(showCallback, onComplete)
            }
        }
    }

    /**
     * Show ad with loading dialog (always forces fresh fetch, ignores global strategy)
     */
    private fun showWithDialog(callback: AdManagerCallback, onComplete: () -> Unit) {
        if (debugMode) android.util.Log.d("InterstitialBuilder", "Showing ad with loading dialog (force fetch)")

        // Use forceShowInterstitialAlways to bypass global strategy - Builder has its own strategy logic
        AdManager.getInstance().forceShowInterstitialAlways(
            activity,
            callback
        )
    }

    /**
     * Just preload the ad without showing
     */
    fun preload() {
        require(primaryAdUnit != null) { "Ad unit ID must be set using adUnit()" }

        ensureAdLoaded(
            onSuccess = {
                // Ad preloaded successfully
                onAdLoadedCallback?.invoke()
            },
            onFailure = { error ->
                // Failed to preload
                onAdFailedCallback?.invoke(error)
            }
        )
    }

    /**
     * Ensure ad is loaded, with fallback support
     */
    private fun ensureAdLoaded(onSuccess: () -> Unit, onFailure: (LoadAdError) -> Unit) {
        val adManager = AdManager.getInstance()

        // If ad already loaded, use it immediately
        if (adManager.isReady()) {
            onSuccess()
            return
        }

        // Load with fallback chain
        loadWithFallback(0, onSuccess, onFailure)
    }

    /**
     * Load ad with fallback chain
     */
    private fun loadWithFallback(
        index: Int,
        onSuccess: () -> Unit,
        onFailure: (LoadAdError) -> Unit
    ) {
        val adManager = AdManager.getInstance()
        val allUnits = listOf(primaryAdUnit!!) + fallbackAdUnits

        if (index >= allUnits.size) {
            // All ad units failed
            val error = LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, "All ad units failed to load (tried ${allUnits.size} units)", null)
            onFailure(error)
            return
        }

        val currentUnit = allUnits[index]
        val isLastUnit = index == allUnits.size - 1

        val adRequest = AdRequest.Builder(currentUnit).build()

        InterstitialAd.load(adRequest, object : AdLoadCallback<InterstitialAd> {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                // Ad loaded successfully
                onAdLoadedCallback?.invoke()
                onSuccess()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                if (isLastUnit) {
                    // This was the last fallback, fail completely
                    onFailure(error)
                } else {
                    // Try next fallback
                    loadWithFallback(index + 1, onSuccess, onFailure)
                }
            }
        })
    }
}

/**
 * Extension function for even cleaner syntax
 */
fun Activity.showInterstitialAd(
    adUnitId: String,
    force: Boolean = false,
    onComplete: () -> Unit
) {
    val builder = InterstitialAdBuilder.with(this)
        .adUnit(adUnitId)

    if (force) {
        builder.force()
    }

    builder.show(onComplete)
}

/**
 * Extension function with fallback support
 */
fun Activity.showInterstitialAdWithFallback(
    primaryUnit: String,
    fallbackUnit: String,
    onComplete: () -> Unit
) {
    InterstitialAdBuilder.with(this)
        .adUnit(primaryUnit)
        .fallback(fallbackUnit)
        .force()
        .show(onComplete)
}

/**
 * Simple preload extension
 */
fun Activity.preloadInterstitialAd(adUnitId: String) {
    InterstitialAdBuilder.with(this)
        .adUnit(adUnitId)
        .preload()
}

// Java-friendly listener interface for onAdDismissed (others exist in AdCallback.kt)
interface OnAdDismissedListener {
    fun onAdDismissed()
}
