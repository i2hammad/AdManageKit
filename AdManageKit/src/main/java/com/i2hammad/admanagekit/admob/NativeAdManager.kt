package com.i2hammad.admanagekit.admob

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.config.AdManageKitConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Native ad manager using GMA Next-Gen SDK's built-in NativeAdPreloader.
 *
 * This manager leverages the SDK's native preloading system which provides:
 * - Automatic background ad loading
 * - Efficient cache management by the SDK
 * - Automatic ad expiration handling
 * - Memory optimization
 * - Seamless ad refresh after polling
 *
 * ## Usage
 * ```kotlin
 * // 1. Start preloading (call once, typically in Application or Activity onCreate)
 * NativeAdManager.startPreloading(adUnitId)
 *
 * // 2. Check if ad is available
 * if (NativeAdManager.isAdAvailable(adUnitId)) {
 *     // Ad is ready
 * }
 *
 * // 3. Poll (get) ad when ready to display
 * val nativeAd = NativeAdManager.pollAd(adUnitId)
 * if (nativeAd != null) {
 *     // Display the ad
 * }
 *
 * // 4. Stop preloading when no longer needed
 * NativeAdManager.stopPreloading(adUnitId)
 * ```
 *
 * @since 4.0.0 (Rewritten for GMA Next-Gen SDK)
 */
object NativeAdManager {

    private const val TAG = "NativeAdManager"

    // Performance tracking
    private val preloadHits = AtomicLong(0)
    private val preloadMisses = AtomicLong(0)
    private val totalAdsServed = AtomicLong(0)

    // Track active preloaders
    private val activePreloaders = ConcurrentHashMap<String, Boolean>()

    // Preload callbacks per ad unit
    private val preloadCallbacks = ConcurrentHashMap<String, PreloadStatusCallback>()

    // Firebase Analytics instance
    private var firebaseAnalytics: FirebaseAnalytics? = null

    // Main handler for UI operations
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Controls whether native ad preloading is enabled.
     * When disabled, startPreloading() does nothing and pollAd() returns null.
     */
    var enablePreloading: Boolean = true

    /**
     * Legacy alias for enablePreloading for backwards compatibility.
     */
    var enableCachingNativeAds: Boolean
        get() = enablePreloading
        set(value) { enablePreloading = value }

    /**
     * Callback interface for preload status updates.
     */
    interface PreloadStatusCallback {
        /** Called when an ad is successfully preloaded and available. */
        fun onAdPreloaded(adUnitId: String)

        /** Called when preloading fails. */
        fun onAdFailedToPreload(adUnitId: String, error: LoadAdError)

        /** Called when all preloaded ads have been consumed and none are available. */
        fun onAdsExhausted(adUnitId: String)
    }

    // =================== INITIALIZATION ===================

    /**
     * Initializes Firebase Analytics for performance tracking.
     * Call this from your Application class for analytics support.
     */
    fun initialize(analytics: FirebaseAnalytics? = null) {
        this.firebaseAnalytics = analytics
        logDebug("NativeAdManager initialized with analytics: ${analytics != null}")
    }

    // =================== PRELOADING API ===================

    /**
     * Starts preloading native ads for the specified ad unit.
     *
     * The SDK will automatically:
     * - Load ads in the background
     * - Maintain a cache of ready-to-show ads
     * - Refresh the cache after ads are polled
     * - Handle ad expiration
     *
     * @param adUnitId The ad unit ID to preload
     * @param startMuted Whether video ads should start muted (default: true)
     * @param callback Optional callback for preload status updates
     */
    @JvmOverloads
    fun startPreloading(
        adUnitId: String,
        startMuted: Boolean = true,
        callback: PreloadStatusCallback? = null
    ) {
        if (!enablePreloading) {
            logDebug("Preloading disabled, skipping for $adUnitId")
            return
        }

        if (activePreloaders[adUnitId] == true) {
            logDebug("Preloader already active for $adUnitId")
            return
        }

        logDebug("Starting preloader for $adUnitId")

        // Store callback
        callback?.let { preloadCallbacks[adUnitId] = it }

        // Build the preload callback
        // Note: GMA Next-Gen SDK calls these callbacks on background threads,
        // so we must switch to main thread for user callbacks.
        val sdkCallback = object : PreloadCallback {
            override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                logDebug("Ad preloaded for $adUnitId")
                trackEvent("native_ad_preloaded", mapOf("ad_unit_id" to adUnitId))
                mainHandler.post {
                    preloadCallbacks[adUnitId]?.onAdPreloaded(adUnitId)
                }
            }

            override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                logDebug("Ad failed to preload for $adUnitId: ${adError.message}")
                trackEvent("native_ad_preload_failed", mapOf(
                    "ad_unit_id" to adUnitId,
                    "error_code" to adError.code.toString(),
                    "error_message" to adError.message
                ))
                mainHandler.post {
                    preloadCallbacks[adUnitId]?.onAdFailedToPreload(adUnitId, adError)
                }
            }

            override fun onAdsExhausted(preloadId: String) {
                logDebug("Ads exhausted for $adUnitId")
                trackEvent("native_ads_exhausted", mapOf("ad_unit_id" to adUnitId))
                mainHandler.post {
                    preloadCallbacks[adUnitId]?.onAdsExhausted(adUnitId)
                }
            }
        }

        // Build video options
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(startMuted)
            .build()

        // Build ad request
        val adRequest = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
            .setVideoOptions(videoOptions)
            .build()

        // Create preload configuration
        val preloadConfig = PreloadConfiguration(adRequest)

        // Start preloading
        NativeAdPreloader.start(adUnitId, preloadConfig, sdkCallback)
        activePreloaders[adUnitId] = true

        logDebug("Preloader started for $adUnitId")
    }

    /**
     * Marks preloading as inactive for the specified ad unit.
     * Note: GMA Next-Gen SDK preloaders don't have a stop method - they run until app termination.
     * This method just removes tracking, preloader continues in background.
     *
     * @param adUnitId The ad unit ID to mark as inactive
     */
    fun stopPreloading(adUnitId: String) {
        if (activePreloaders[adUnitId] != true) {
            logDebug("No active preloader for $adUnitId")
            return
        }

        // Note: GMA Next-Gen SDK preloaders don't have stop() - they run until app termination
        // We just remove our tracking
        activePreloaders.remove(adUnitId)
        preloadCallbacks.remove(adUnitId)

        logDebug("Preloader marked inactive for $adUnitId (SDK preloader continues in background)")
    }

    /**
     * Marks all preloaders as inactive.
     * Note: SDK preloaders continue running - this just clears our tracking.
     */
    fun stopAllPreloading() {
        activePreloaders.clear()
        preloadCallbacks.clear()
        logDebug("All preloaders marked inactive")
    }

    /**
     * Checks if a preloaded ad is available for the specified ad unit.
     *
     * @param adUnitId The ad unit ID to check
     * @return true if an ad is available, false otherwise
     */
    fun isAdAvailable(adUnitId: String): Boolean {
        if (!enablePreloading) return false
        return NativeAdPreloader.isAdAvailable(adUnitId)
    }

    /**
     * Polls (retrieves) a preloaded native ad.
     *
     * **Important:** This removes the ad from the preload cache.
     * The SDK will automatically load another ad in the background.
     *
     * @param adUnitId The ad unit ID
     * @param eventCallback Optional callback for ad events (impression, click, paid, etc.)
     * @return The native ad if available, null otherwise
     */
    @JvmOverloads
    fun pollAd(adUnitId: String, eventCallback: NativeAdEventCallback? = null): NativeAd? {
        if (!enablePreloading) {
            preloadMisses.incrementAndGet()
            logDebug("Preloading disabled, cannot poll ad for $adUnitId")
            return null
        }

        val result = NativeAdPreloader.pollAd(adUnitId)

        return when (result) {
            is NativeAdLoadResult.NativeAdSuccess -> {
                val nativeAd = result.ad
                preloadHits.incrementAndGet()
                totalAdsServed.incrementAndGet()

                logDebug("Polled ad successfully for $adUnitId")

                // Set up event callback with analytics
                nativeAd.adEventCallback = object : NativeAdEventCallback {
                    override fun onAdImpression() {
                        logDebug("Ad impression for $adUnitId")
                        trackEvent("native_ad_impression", mapOf("ad_unit_id" to adUnitId))
                        eventCallback?.onAdImpression()
                    }

                    override fun onAdClicked() {
                        logDebug("Ad clicked for $adUnitId")
                        trackEvent("native_ad_clicked", mapOf("ad_unit_id" to adUnitId))
                        eventCallback?.onAdClicked()
                    }

                    override fun onAdPaid(value: AdValue) {
                        val adValueInStandardUnits = value.valueMicros / 1_000_000.0
                        logDebug("Ad paid for $adUnitId: $adValueInStandardUnits ${value.currencyCode}")

                        val params = Bundle().apply {
                            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                            putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                            putString(FirebaseAnalytics.Param.CURRENCY, value.currencyCode)
                        }
                        firebaseAnalytics?.logEvent("ad_paid_event", params)

                        eventCallback?.onAdPaid(value)
                    }

                    override fun onAdShowedFullScreenContent() {
                        logDebug("Ad showed full screen for $adUnitId")
                        eventCallback?.onAdShowedFullScreenContent()
                    }

                    override fun onAdDismissedFullScreenContent() {
                        logDebug("Ad dismissed full screen for $adUnitId")
                        eventCallback?.onAdDismissedFullScreenContent()
                    }

                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError) {
                        logDebug("Ad failed to show full screen for $adUnitId: ${error.message}")
                        eventCallback?.onAdFailedToShowFullScreenContent(error)
                    }
                }

                trackEvent("native_ad_served", mapOf(
                    "ad_unit_id" to adUnitId,
                    "source" to "preloader"
                ))

                nativeAd
            }
            else -> {
                // No ad available (pollAd returns non-success result when no ad is preloaded)
                preloadMisses.incrementAndGet()
                logDebug("No ad available for $adUnitId")
                null
            }
        }
    }

    // =================== LEGACY API (Backwards Compatibility) ===================

    /**
     * Legacy method - now delegates to pollAd().
     *
     * @deprecated Use pollAd() instead for clearer semantics.
     */
    @Deprecated(
        message = "Use pollAd() instead",
        replaceWith = ReplaceWith("pollAd(adUnitId)")
    )
    fun getCachedNativeAd(adUnitId: String, enableFallbackToAnyAd: Boolean = false): NativeAd? {
        return pollAd(adUnitId)
    }

    /**
     * Legacy method - manually caching is not needed with SDK preloader.
     * The SDK handles caching automatically.
     *
     * @deprecated SDK preloader handles caching automatically. Use startPreloading() instead.
     */
    @Deprecated(
        message = "SDK preloader handles caching automatically. Use startPreloading() instead.",
        replaceWith = ReplaceWith("startPreloading(adUnitId)")
    )
    fun setCachedNativeAd(adUnitId: String, ad: NativeAd) {
        logDebug("setCachedNativeAd() is deprecated. SDK preloader handles caching automatically.")
        // No-op - SDK handles caching
    }

    /**
     * Legacy method - checks if preloaded ad is available.
     */
    fun hasCachedAds(adUnitId: String): Boolean = isAdAvailable(adUnitId)

    /**
     * Legacy method - returns 1 if ad available, 0 otherwise.
     * SDK preloader doesn't expose exact cache size.
     */
    fun getCacheSize(adUnitId: String): Int = if (isAdAvailable(adUnitId)) 1 else 0

    /**
     * Legacy method - returns count of ad units with available ads.
     */
    fun getTotalCacheSize(): Int = activePreloaders.keys.count { isAdAvailable(it) }

    /**
     * Legacy method - clears preloaded ads for the ad unit by stopping and restarting preloader.
     */
    fun clearCachedAd(adUnitId: String) {
        stopPreloading(adUnitId)
    }

    /**
     * Legacy method - stops all preloaders.
     */
    fun clearAllCachedAds() {
        stopAllPreloading()
    }

    // =================== PERFORMANCE MONITORING ===================

    /**
     * Gets performance statistics.
     */
    fun getPerformanceStats(): Map<String, Any> {
        val totalRequests = preloadHits.get() + preloadMisses.get()
        val hitRate = if (totalRequests > 0) {
            (preloadHits.get().toDouble() / totalRequests * 100).toInt()
        } else 0

        return mapOf(
            "preload_hits" to preloadHits.get(),
            "preload_misses" to preloadMisses.get(),
            "hit_rate_percent" to hitRate,
            "total_ads_served" to totalAdsServed.get(),
            "active_preloaders" to activePreloaders.size,
            "preloading_enabled" to enablePreloading
        )
    }

    /**
     * Resets performance counters.
     */
    fun resetPerformanceStats() {
        preloadHits.set(0)
        preloadMisses.set(0)
        totalAdsServed.set(0)
        logDebug("Performance stats reset")
    }

    /**
     * Gets list of ad units with active preloaders.
     */
    fun getActivePreloaders(): List<String> = activePreloaders.keys.toList()

    // =================== UTILITY METHODS ===================

    private fun logDebug(message: String) {
        AdDebugUtils.logDebug(TAG, message)
    }

    private fun trackEvent(eventName: String, parameters: Map<String, Any> = emptyMap()) {
        if (AdManageKitConfig.enablePerformanceMetrics && firebaseAnalytics != null) {
            val bundle = Bundle()
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> bundle.putString(key, value)
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                }
            }
            firebaseAnalytics?.logEvent(eventName, bundle)
        }
    }

    // =================== DEPRECATED METHODS ===================

    @Deprecated("Background cleanup not needed with SDK preloader")
    fun stopBackgroundCleanup() {
        // No-op - SDK handles cleanup
    }

    @Deprecated("Use getPerformanceStats() instead")
    fun getCacheStatistics(): Map<String, String> {
        return activePreloaders.keys.associateWith { adUnitId ->
            "Available: ${isAdAvailable(adUnitId)}"
        }
    }

    @Deprecated("Use startPreloading() instead")
    fun preloadNativeAd(
        activity: android.app.Activity,
        adUnitId: String,
        size: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        startPreloading(adUnitId, callback = object : PreloadStatusCallback {
            override fun onAdPreloaded(adUnitId: String) {
                mainHandler.post { onSuccess?.invoke() }
            }
            override fun onAdFailedToPreload(adUnitId: String, error: LoadAdError) {
                mainHandler.post { onFailure?.invoke(error.message) }
            }
            override fun onAdsExhausted(adUnitId: String) {
                mainHandler.post { onFailure?.invoke("Ads exhausted") }
            }
        })
    }

    @Deprecated("Use startPreloading() instead - SDK manages multiple ads automatically")
    fun preloadMultipleNativeAds(
        activity: android.app.Activity,
        adUnitId: String,
        size: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize,
        count: Int = 2,
        onComplete: ((successCount: Int, failureCount: Int) -> Unit)? = null
    ) {
        startPreloading(adUnitId, callback = object : PreloadStatusCallback {
            override fun onAdPreloaded(adUnitId: String) {
                mainHandler.post { onComplete?.invoke(1, 0) }
            }
            override fun onAdFailedToPreload(adUnitId: String, error: LoadAdError) {
                mainHandler.post { onComplete?.invoke(0, 1) }
            }
            override fun onAdsExhausted(adUnitId: String) {
                mainHandler.post { onComplete?.invoke(0, 1) }
            }
        })
    }

    @Deprecated("Use startPreloading() instead")
    fun warmCache(adUnits: Map<String, Int>, onComplete: ((Int, Int) -> Unit)? = null) {
        adUnits.keys.forEach { adUnitId ->
            startPreloading(adUnitId)
        }
        mainHandler.post { onComplete?.invoke(adUnits.size, adUnits.size) }
    }

    @Deprecated("Not needed with SDK preloader")
    fun performCleanup() {
        // No-op - SDK handles cleanup
    }

    // =================== PROGRAMMATIC LOADING (Still available) ===================

    /**
     * Loads a native ad programmatically without requiring a view to be added to layout first.
     * This is a convenience method that delegates to ProgrammaticNativeAdLoader.
     *
     * Note: For better performance, consider using startPreloading() and pollAd() instead.
     */
    fun loadNativeAdProgrammatically(
        activity: android.app.Activity,
        adUnitId: String,
        size: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize,
        useCachedAd: Boolean = true,
        callback: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.ProgrammaticAdCallback
    ) {
        // If preloader has an ad available and useCachedAd is true, use it
        if (useCachedAd && isAdAvailable(adUnitId)) {
            val ad = pollAd(adUnitId)
            if (ad != null) {
                val nativeAdView = com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.createNativeAdView(activity, size)
                // Note: The view needs to be populated by the caller
                callback.onAdLoaded(nativeAdView, ad)
                return
            }
        }

        // Fall back to direct loading
        com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.loadNativeAd(
            activity, adUnitId, size, false, callback
        )
    }

    fun loadSmallNativeAd(
        activity: android.app.Activity,
        adUnitId: String,
        useCachedAd: Boolean = true,
        callback: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.ProgrammaticAdCallback
    ) {
        loadNativeAdProgrammatically(
            activity, adUnitId,
            com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize.SMALL,
            useCachedAd, callback
        )
    }

    fun loadMediumNativeAd(
        activity: android.app.Activity,
        adUnitId: String,
        useCachedAd: Boolean = true,
        callback: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.ProgrammaticAdCallback
    ) {
        loadNativeAdProgrammatically(
            activity, adUnitId,
            com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize.MEDIUM,
            useCachedAd, callback
        )
    }

    fun loadLargeNativeAd(
        activity: android.app.Activity,
        adUnitId: String,
        useCachedAd: Boolean = true,
        callback: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.ProgrammaticAdCallback
    ) {
        loadNativeAdProgrammatically(
            activity, adUnitId,
            com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize.LARGE,
            useCachedAd, callback
        )
    }

    fun loadNativeAdIntoContainer(
        activity: android.app.Activity,
        adUnitId: String,
        size: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize,
        container: android.view.ViewGroup,
        useCachedAd: Boolean = true,
        callback: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.ProgrammaticAdCallback? = null
    ) {
        com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.loadNativeAdIntoContainer(
            activity, adUnitId, size, container, useCachedAd, callback
        )
    }
}
