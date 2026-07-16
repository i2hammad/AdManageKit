package com.i2hammad.admanagekit.waterfall

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.BannerAdProvider
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Waterfall orchestrator for banner ads.
 * Tries each provider in order until one loads a banner successfully.
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
class BannerWaterfall @JvmOverloads constructor(
    private val providers: List<BannerAdProvider>,
    private val adUnitResolver: (com.i2hammad.admanagekit.core.ad.AdProvider) -> String?,
    private val ownsProviders: Boolean = false,
    private val attemptTimeoutMillis: Long = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds
) {
    @Volatile
    private var loadedProvider: BannerAdProvider? = null

    /** Incremented on every load() and destroy(); stale callbacks no-op. */
    private val generation = AtomicInteger(0)

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "BannerWaterfall"
    }

    /**
     * Load a banner ad using the waterfall chain.
     * Cancels any previous in-flight load of this waterfall.
     */
    fun load(context: Context, callback: BannerAdProvider.BannerAdCallback) {
        val token = generation.incrementAndGet()
        loadedProvider = null
        loadNext(context, 0, token, callback)
    }

    private fun loadNext(
        context: Context,
        index: Int,
        token: Int,
        callback: BannerAdProvider.BannerAdCallback
    ) {
        // A provider's failure callback (e.g. AdMob's onAdFailedToLoad) fires on a background
        // thread. Advancing the chain there would construct the next provider's banner View off
        // the main thread and crash ("Can't create handler inside thread that has not called
        // Looper.prepare()"). Always run the chain on the main thread.
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { loadNext(context, index, token, callback) }
            return
        }

        if (token != generation.get()) {
            Log.d(TAG, "Load chain cancelled (stale token), aborting")
            return
        }

        if (index >= providers.size) {
            Log.e(TAG, "All providers exhausted")
            callback.onBannerFailedToLoad(
                AdKitAdError(AdKitAdError.ERROR_CODE_NO_FILL, "All providers exhausted", "waterfall")
            )
            return
        }

        val provider = providers[index]
        val adUnitId = adUnitResolver(provider.provider)

        if (adUnitId == null) {
            Log.w(TAG, "No ad unit ID for ${provider.provider.displayName}, skipping")
            loadNext(context, index + 1, token, callback)
            return
        }

        Log.d(TAG, "Trying ${provider.provider.displayName} ($adUnitId)")

        // One-shot guard shared by the provider callback and the watchdog.
        val settled = AtomicBoolean(false)
        val watchdog = Runnable {
            if (settled.compareAndSet(false, true)) {
                Log.w(TAG, "${provider.provider.displayName} timed out after ${attemptTimeoutMillis}ms (code ${AdKitAdError.ERROR_CODE_TIMEOUT})")
                loadNext(context, index + 1, token, callback)
            }
        }
        if (attemptTimeoutMillis > 0) handler.postDelayed(watchdog, attemptTimeoutMillis)

        provider.loadBanner(context, adUnitId, object : BannerAdProvider.BannerAdCallback {
            override fun onBannerLoaded(bannerView: View) {
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
                // Deliver on the main thread — the app adds bannerView to a ViewGroup.
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    callback.onBannerLoaded(bannerView)
                } else {
                    handler.post { callback.onBannerLoaded(bannerView) }
                }
            }

            override fun onBannerFailedToLoad(error: AdKitAdError) {
                if (!settled.compareAndSet(false, true)) {
                    Log.d(TAG, "${provider.provider.displayName} failed after timeout, ignoring")
                    return
                }
                handler.removeCallbacks(watchdog)
                Log.w(TAG, "${provider.provider.displayName} failed: ${error.message}")
                loadNext(context, index + 1, token, callback)
            }

            override fun onBannerClicked() { callback.onBannerClicked() }
            override fun onBannerImpression() { callback.onBannerImpression() }
            override fun onPaidEvent(adValue: AdKitAdValue) { callback.onPaidEvent(adValue) }
        })
    }

    fun pause() { loadedProvider?.pause() }
    fun resume() { loadedProvider?.resume() }

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
