package com.i2hammad.admanagekit.waterfall

import android.app.Activity
import android.content.Context
import android.util.Log
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AppOpenAdProvider

/**
 * Waterfall orchestrator for app open ads.
 * Tries each provider in order until one succeeds.
 *
 * @param providers Ordered list of providers to try
 * @param adUnitResolver Resolves the ad unit ID for a given provider
 */
class AppOpenWaterfall(
    private val providers: List<AppOpenAdProvider>,
    private val adUnitResolver: (com.i2hammad.admanagekit.core.ad.AdProvider) -> String?
) {
    private var loadedProvider: AppOpenAdProvider? = null

    companion object {
        private const val TAG = "AppOpenWaterfall"
    }

    /**
     * Load an app open ad using the waterfall chain.
     */
    fun load(context: Context, callback: AppOpenAdProvider.AppOpenAdCallback) {
        loadedProvider = null
        loadNext(context, 0, callback)
    }

    private fun loadNext(
        context: Context,
        index: Int,
        callback: AppOpenAdProvider.AppOpenAdCallback
    ) {
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
            loadNext(context, index + 1, callback)
            return
        }

        Log.d(TAG, "Trying ${provider.provider.displayName} ($adUnitId)")

        provider.loadAd(context, adUnitId, object : AppOpenAdProvider.AppOpenAdCallback {
            override fun onAdLoaded() {
                Log.d(TAG, "Loaded from ${provider.provider.displayName}")
                loadedProvider = provider
                callback.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: AdKitAdError) {
                Log.w(TAG, "${provider.provider.displayName} failed: ${error.message}")
                loadNext(context, index + 1, callback)
            }
        })
    }

    /**
     * Show the loaded app open ad.
     */
    fun show(activity: Activity, callback: AppOpenAdProvider.AppOpenShowCallback) {
        val provider = loadedProvider
        if (provider == null || !provider.isAdReady()) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded", "waterfall")
            )
            callback.onAdDismissed()
            return
        }

        provider.showAd(activity, object : AppOpenAdProvider.AppOpenShowCallback {
            override fun onAdShowed() { callback.onAdShowed() }
            override fun onAdDismissed() {
                loadedProvider = null
                callback.onAdDismissed()
            }
            override fun onAdFailedToShow(error: AdKitAdError) { callback.onAdFailedToShow(error) }
            override fun onAdClicked() { callback.onAdClicked() }
            override fun onAdImpression() { callback.onAdImpression() }
            override fun onPaidEvent(adValue: AdKitAdValue) { callback.onPaidEvent(adValue) }
        })
    }

    fun isAdReady(): Boolean = loadedProvider?.isAdReady() == true

    fun destroy() {
        providers.forEach { it.destroy() }
        loadedProvider = null
    }
}
