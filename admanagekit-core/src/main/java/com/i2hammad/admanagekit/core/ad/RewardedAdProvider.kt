package com.i2hammad.admanagekit.core.ad

import android.app.Activity
import android.content.Context

/**
 * Provider interface for rewarded ads.
 * Each ad network implements this to load and show rewarded ads.
 */
interface RewardedAdProvider {

    /** The ad provider this implementation belongs to. */
    val provider: AdProvider

    /**
     * Load a rewarded ad.
     *
     * @param context Application or activity context
     * @param adUnitId Network-specific ad unit ID
     * @param callback Receives load result
     */
    fun loadAd(context: Context, adUnitId: String, callback: RewardedAdCallback)

    /**
     * Show a previously loaded rewarded ad.
     *
     * @param activity The activity to show the ad from
     * @param callback Receives show lifecycle events and reward
     */
    fun showAd(activity: Activity, callback: RewardedShowCallback)

    /** @return true if an ad is loaded and ready to show */
    fun isAdReady(): Boolean

    /** Release resources held by this provider. */
    fun destroy()

    /** Callback for rewarded ad load events. */
    interface RewardedAdCallback {
        fun onAdLoaded()
        fun onAdFailedToLoad(error: AdKitAdError)
    }

    /** Callback for rewarded ad show lifecycle events. */
    interface RewardedShowCallback {
        fun onAdShowed() {}
        fun onAdDismissed()
        fun onAdFailedToShow(error: AdKitAdError) {}
        fun onAdClicked() {}
        fun onAdImpression() {}
        fun onRewardEarned(rewardType: String, rewardAmount: Int)
        fun onPaidEvent(adValue: AdKitAdValue) {}
    }
}
