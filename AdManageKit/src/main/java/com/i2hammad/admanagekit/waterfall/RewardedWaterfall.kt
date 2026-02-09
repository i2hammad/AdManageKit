package com.i2hammad.admanagekit.waterfall

import android.app.Activity
import android.content.Context
import android.util.Log
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.RewardedAdProvider

/**
 * Waterfall orchestrator for rewarded ads.
 * Tries each provider in order until one succeeds.
 *
 * @param providers Ordered list of providers to try
 * @param adUnitResolver Resolves the ad unit ID for a given provider
 */
class RewardedWaterfall(
    private val providers: List<RewardedAdProvider>,
    private val adUnitResolver: (com.i2hammad.admanagekit.core.ad.AdProvider) -> String?
) {
    private var loadedProvider: RewardedAdProvider? = null

    companion object {
        private const val TAG = "RewardedWaterfall"
    }

    /**
     * Load a rewarded ad using the waterfall chain.
     */
    fun load(context: Context, callback: RewardedAdProvider.RewardedAdCallback) {
        loadedProvider = null
        loadNext(context, 0, callback)
    }

    private fun loadNext(
        context: Context,
        index: Int,
        callback: RewardedAdProvider.RewardedAdCallback
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

        provider.loadAd(context, adUnitId, object : RewardedAdProvider.RewardedAdCallback {
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
     * Show the loaded rewarded ad.
     */
    fun show(activity: Activity, callback: RewardedAdProvider.RewardedShowCallback) {
        val provider = loadedProvider
        if (provider == null || !provider.isAdReady()) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded", "waterfall")
            )
            callback.onAdDismissed()
            return
        }

        provider.showAd(activity, object : RewardedAdProvider.RewardedShowCallback {
            override fun onAdShowed() { callback.onAdShowed() }
            override fun onAdDismissed() {
                loadedProvider = null
                callback.onAdDismissed()
            }
            override fun onAdFailedToShow(error: AdKitAdError) { callback.onAdFailedToShow(error) }
            override fun onAdClicked() { callback.onAdClicked() }
            override fun onAdImpression() { callback.onAdImpression() }
            override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
                callback.onRewardEarned(rewardType, rewardAmount)
            }
            override fun onPaidEvent(adValue: AdKitAdValue) { callback.onPaidEvent(adValue) }
        })
    }

    fun isAdReady(): Boolean = loadedProvider?.isAdReady() == true

    fun destroy() {
        providers.forEach { it.destroy() }
        loadedProvider = null
    }
}
