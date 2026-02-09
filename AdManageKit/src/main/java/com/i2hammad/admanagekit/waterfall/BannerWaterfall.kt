package com.i2hammad.admanagekit.waterfall

import android.content.Context
import android.util.Log
import android.view.View
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.BannerAdProvider

/**
 * Waterfall orchestrator for banner ads.
 * Tries each provider in order until one loads a banner successfully.
 *
 * @param providers Ordered list of providers to try
 * @param adUnitResolver Resolves the ad unit ID for a given provider
 */
class BannerWaterfall(
    private val providers: List<BannerAdProvider>,
    private val adUnitResolver: (com.i2hammad.admanagekit.core.ad.AdProvider) -> String?
) {
    private var loadedProvider: BannerAdProvider? = null

    companion object {
        private const val TAG = "BannerWaterfall"
    }

    /**
     * Load a banner ad using the waterfall chain.
     */
    fun load(context: Context, callback: BannerAdProvider.BannerAdCallback) {
        loadedProvider = null
        loadNext(context, 0, callback)
    }

    private fun loadNext(
        context: Context,
        index: Int,
        callback: BannerAdProvider.BannerAdCallback
    ) {
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
            loadNext(context, index + 1, callback)
            return
        }

        Log.d(TAG, "Trying ${provider.provider.displayName} ($adUnitId)")

        provider.loadBanner(context, adUnitId, object : BannerAdProvider.BannerAdCallback {
            override fun onBannerLoaded(bannerView: View) {
                Log.d(TAG, "Loaded from ${provider.provider.displayName}")
                loadedProvider = provider
                callback.onBannerLoaded(bannerView)
            }

            override fun onBannerFailedToLoad(error: AdKitAdError) {
                Log.w(TAG, "${provider.provider.displayName} failed: ${error.message}")
                loadNext(context, index + 1, callback)
            }

            override fun onBannerClicked() { callback.onBannerClicked() }
            override fun onBannerImpression() { callback.onBannerImpression() }
            override fun onPaidEvent(adValue: AdKitAdValue) { callback.onPaidEvent(adValue) }
        })
    }

    fun pause() { loadedProvider?.pause() }
    fun resume() { loadedProvider?.resume() }

    fun destroy() {
        providers.forEach { it.destroy() }
        loadedProvider = null
    }
}
