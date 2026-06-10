package com.i2hammad.admanagekit.waterfall

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdSize
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Waterfall orchestrator for native ads.
 * Tries each provider in order until one loads a native ad successfully.
 *
 * Each provider attempt is guarded by a watchdog timeout; a hung provider counts
 * as failed and the chain advances. Each [load] cancels any previous in-flight
 * chain (its late callbacks are ignored), so exactly one terminal load callback
 * is delivered per [load] call.
 *
 * @param providers Ordered list of providers to try
 * @param adUnitResolver Resolves the ad unit ID for a given provider
 * @param ownsProviders Whether this waterfall owns the provider instances. Only owned
 *        providers are destroyed by [destroy]; chains obtained from the global
 *        [com.i2hammad.admanagekit.core.ad.AdProviderConfig] are shared and must not
 *        be destroyed by individual waterfalls (default: false).
 * @param attemptTimeoutMillis Watchdog timeout for each provider load attempt
 */
class NativeWaterfall @JvmOverloads constructor(
    private val providers: List<NativeAdProvider>,
    private val adUnitResolver: (com.i2hammad.admanagekit.core.ad.AdProvider) -> String?,
    private val ownsProviders: Boolean = false,
    private val attemptTimeoutMillis: Long = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds
) {
    @Volatile
    private var loadedProvider: NativeAdProvider? = null

    /** Incremented on every load() and destroy(); stale callbacks no-op. */
    private val generation = AtomicInteger(0)

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "NativeWaterfall"
    }

    /**
     * Load a native ad using the waterfall chain.
     * Cancels any previous in-flight load of this waterfall.
     */
    fun load(
        context: Context,
        callback: NativeAdProvider.NativeAdCallback,
        sizeHint: NativeAdSize = NativeAdSize.LARGE
    ) {
        val token = generation.incrementAndGet()
        loadedProvider = null
        loadNext(context, 0, token, callback, sizeHint)
    }

    private fun loadNext(
        context: Context,
        index: Int,
        token: Int,
        callback: NativeAdProvider.NativeAdCallback,
        sizeHint: NativeAdSize
    ) {
        if (token != generation.get()) {
            Log.d(TAG, "Load chain cancelled (stale token), aborting")
            return
        }

        if (index >= providers.size) {
            Log.e(TAG, "All providers exhausted")
            callback.onNativeAdFailedToLoad(
                AdKitAdError(AdKitAdError.ERROR_CODE_NO_FILL, "All providers exhausted", "waterfall")
            )
            return
        }

        val provider = providers[index]
        val adUnitId = adUnitResolver(provider.provider)

        if (adUnitId == null) {
            Log.w(TAG, "No ad unit ID for ${provider.provider.displayName}, skipping")
            loadNext(context, index + 1, token, callback, sizeHint)
            return
        }

        Log.d(TAG, "Trying ${provider.provider.displayName} ($adUnitId)")

        // One-shot guard shared by the provider callback and the watchdog.
        val settled = AtomicBoolean(false)
        val watchdog = Runnable {
            if (settled.compareAndSet(false, true)) {
                Log.w(TAG, "${provider.provider.displayName} timed out after ${attemptTimeoutMillis}ms (code ${AdKitAdError.ERROR_CODE_TIMEOUT})")
                loadNext(context, index + 1, token, callback, sizeHint)
            }
        }
        if (attemptTimeoutMillis > 0) handler.postDelayed(watchdog, attemptTimeoutMillis)

        provider.loadNativeAd(context, adUnitId, callback = object : NativeAdProvider.NativeAdCallback {
            override fun onNativeAdLoaded(adView: View, nativeAdRef: Any) {
                if (!settled.compareAndSet(false, true)) {
                    Log.d(TAG, "${provider.provider.displayName} loaded after timeout, ignoring")
                    return
                }
                handler.removeCallbacks(watchdog)
                if (token != generation.get()) {
                    Log.d(TAG, "${provider.provider.displayName} loaded for a cancelled chain, ignoring")
                    return
                }
                Log.d(TAG, "Loaded from ${provider.provider.displayName}")
                loadedProvider = provider
                callback.onNativeAdLoaded(adView, nativeAdRef)
            }

            override fun onNativeAdFailedToLoad(error: AdKitAdError) {
                if (!settled.compareAndSet(false, true)) {
                    Log.d(TAG, "${provider.provider.displayName} failed after timeout, ignoring")
                    return
                }
                handler.removeCallbacks(watchdog)
                Log.w(TAG, "${provider.provider.displayName} failed: ${error.message}")
                loadNext(context, index + 1, token, callback, sizeHint)
            }

            override fun onNativeAdClicked() { callback.onNativeAdClicked() }
            override fun onNativeAdImpression() { callback.onNativeAdImpression() }
            override fun onPaidEvent(adValue: AdKitAdValue) { callback.onPaidEvent(adValue) }
        }, sizeHint = sizeHint)
    }

    /**
     * Cancel any in-flight load and clear waterfall-local state.
     * Providers are destroyed only when this waterfall owns them ([ownsProviders]);
     * globally shared chains are left untouched.
     */
    fun destroy() {
        generation.incrementAndGet()
        if (ownsProviders) providers.forEach { it.destroy() }
        loadedProvider = null
    }
}
