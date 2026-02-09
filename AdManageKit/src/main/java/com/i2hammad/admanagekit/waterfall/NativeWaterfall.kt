package com.i2hammad.admanagekit.waterfall

import android.content.Context
import android.util.Log
import android.view.View
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdSize

/**
 * Waterfall orchestrator for native ads.
 * Tries each provider in order until one loads a native ad successfully.
 *
 * @param providers Ordered list of providers to try
 * @param adUnitResolver Resolves the ad unit ID for a given provider
 */
class NativeWaterfall(
    private val providers: List<NativeAdProvider>,
    private val adUnitResolver: (com.i2hammad.admanagekit.core.ad.AdProvider) -> String?
) {
    private var loadedProvider: NativeAdProvider? = null

    companion object {
        private const val TAG = "NativeWaterfall"
    }

    /**
     * Load a native ad using the waterfall chain.
     */
    fun load(
        context: Context,
        callback: NativeAdProvider.NativeAdCallback,
        sizeHint: NativeAdSize = NativeAdSize.LARGE
    ) {
        loadedProvider = null
        loadNext(context, 0, callback, sizeHint)
    }

    private fun loadNext(
        context: Context,
        index: Int,
        callback: NativeAdProvider.NativeAdCallback,
        sizeHint: NativeAdSize
    ) {
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
            loadNext(context, index + 1, callback, sizeHint)
            return
        }

        Log.d(TAG, "Trying ${provider.provider.displayName} ($adUnitId)")

        provider.loadNativeAd(context, adUnitId, callback = object : NativeAdProvider.NativeAdCallback {
            override fun onNativeAdLoaded(adView: View, nativeAdRef: Any) {
                Log.d(TAG, "Loaded from ${provider.provider.displayName}")
                loadedProvider = provider
                callback.onNativeAdLoaded(adView, nativeAdRef)
            }

            override fun onNativeAdFailedToLoad(error: AdKitAdError) {
                Log.w(TAG, "${provider.provider.displayName} failed: ${error.message}")
                loadNext(context, index + 1, callback, sizeHint)
            }

            override fun onNativeAdClicked() { callback.onNativeAdClicked() }
            override fun onNativeAdImpression() { callback.onNativeAdImpression() }
            override fun onPaidEvent(adValue: AdKitAdValue) { callback.onPaidEvent(adValue) }
        }, sizeHint = sizeHint)
    }

    fun destroy() {
        providers.forEach { it.destroy() }
        loadedProvider = null
    }
}
