package com.i2hammad.admanagekit.waterfall

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.InterstitialAdProvider
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Waterfall orchestrator for interstitial ads.
 * Tries each provider in order until one succeeds.
 *
 * Each provider attempt is guarded by a watchdog timeout; a hung provider counts
 * as failed and the chain advances. Each [load] cancels any previous in-flight
 * chain (its late callbacks are ignored), so exactly one terminal load callback
 * is delivered per [load] call. On [show], exactly one terminal callback is
 * delivered: [InterstitialAdProvider.InterstitialShowCallback.onAdDismissed]
 * after a successful show, or
 * [InterstitialAdProvider.InterstitialShowCallback.onAdFailedToShow] when the
 * show fails.
 *
 * Example:
 * ```kotlin
 * val waterfall = InterstitialWaterfall(
 *     providers = AdProviderConfig.getInterstitialChain(),
 *     adUnitResolver = { provider -> AdUnitMapping.getAdUnitId("interstitial_main", provider) }
 * )
 * waterfall.load(context) { success -> ... }
 * waterfall.show(activity, callback)
 * ```
 *
 * @param providers Ordered list of providers to try
 * @param adUnitResolver Resolves the ad unit ID for a given provider
 * @param ownsProviders Whether this waterfall owns the provider instances. Only owned
 *        providers are destroyed by [destroy]; chains obtained from the global
 *        [com.i2hammad.admanagekit.core.ad.AdProviderConfig] are shared and must not
 *        be destroyed by individual waterfalls (default: false).
 * @param attemptTimeoutMillis Watchdog timeout for each provider load attempt
 */
class InterstitialWaterfall @JvmOverloads constructor(
    private val providers: List<InterstitialAdProvider>,
    private val adUnitResolver: (com.i2hammad.admanagekit.core.ad.AdProvider) -> String?,
    private val ownsProviders: Boolean = false,
    private val attemptTimeoutMillis: Long = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds
) {
    @Volatile
    private var loadedProvider: InterstitialAdProvider? = null

    @Volatile
    private var loadedAdUnitId: String? = null

    /** Incremented on every load() and destroy(); stale callbacks no-op. */
    private val generation = AtomicInteger(0)

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "InterstitialWaterfall"
    }

    /**
     * Load an interstitial ad using the waterfall chain.
     * Tries each provider in order until one loads successfully.
     * Cancels any previous in-flight load of this waterfall.
     */
    fun load(context: Context, callback: InterstitialAdProvider.InterstitialAdCallback) {
        val token = generation.incrementAndGet()
        loadedProvider = null
        loadedAdUnitId = null
        loadNext(context, 0, token, callback)
    }

    private fun loadNext(
        context: Context,
        index: Int,
        token: Int,
        callback: InterstitialAdProvider.InterstitialAdCallback
    ) {
        if (token != generation.get()) {
            Log.d(TAG, "Load chain cancelled (stale token), aborting")
            return
        }

        if (index >= providers.size) {
            Log.e(TAG, "All providers exhausted")
            callback.onAdFailedToLoad(
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

        provider.loadAd(context, adUnitId, object : InterstitialAdProvider.InterstitialAdCallback {
            override fun onAdLoaded() {
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
                loadedAdUnitId = adUnitId
                callback.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: AdKitAdError) {
                if (!settled.compareAndSet(false, true)) {
                    Log.d(TAG, "${provider.provider.displayName} failed after timeout, ignoring")
                    return
                }
                handler.removeCallbacks(watchdog)
                Log.w(TAG, "${provider.provider.displayName} failed: ${error.message}")
                loadNext(context, index + 1, token, callback)
            }
        })
    }

    /**
     * Show the loaded interstitial ad.
     *
     * Exactly one terminal callback is delivered per call: [InterstitialAdProvider.InterstitialShowCallback.onAdDismissed]
     * after a successful show, or [InterstitialAdProvider.InterstitialShowCallback.onAdFailedToShow] on failure.
     */
    fun show(activity: Activity, callback: InterstitialAdProvider.InterstitialShowCallback) {
        val provider = loadedProvider
        val adUnitId = loadedAdUnitId
        if (provider == null || adUnitId == null || !provider.isAdReady(adUnitId)) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded", "waterfall")
            )
            return
        }

        // The ad is consumed by this show attempt.
        loadedProvider = null
        loadedAdUnitId = null

        // Providers may emit both onAdFailedToShow and onAdDismissed on a failed
        // show; deliver only the first terminal event to the caller.
        val terminalDelivered = AtomicBoolean(false)

        provider.showAd(activity, adUnitId, object : InterstitialAdProvider.InterstitialShowCallback {
            override fun onAdShowed() { callback.onAdShowed() }
            override fun onAdDismissed() {
                if (terminalDelivered.compareAndSet(false, true)) {
                    callback.onAdDismissed()
                } else {
                    Log.d(TAG, "Suppressing duplicate terminal onAdDismissed")
                }
            }
            override fun onAdFailedToShow(error: AdKitAdError) {
                if (terminalDelivered.compareAndSet(false, true)) {
                    callback.onAdFailedToShow(error)
                } else {
                    Log.d(TAG, "Suppressing duplicate terminal onAdFailedToShow: ${error.message}")
                }
            }
            override fun onAdClicked() { callback.onAdClicked() }
            override fun onAdImpression() { callback.onAdImpression() }
            override fun onPaidEvent(adValue: AdKitAdValue) { callback.onPaidEvent(adValue) }
        })
    }

    /** @return true if the ad loaded by this waterfall is still ready */
    fun isAdReady(): Boolean {
        val provider = loadedProvider ?: return false
        val adUnitId = loadedAdUnitId ?: return false
        return provider.isAdReady(adUnitId)
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
        loadedAdUnitId = null
    }
}
