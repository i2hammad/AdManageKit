package com.i2hammad.admanagekit.core.ad

import android.app.Activity
import android.content.Context

/**
 * Provider interface for interstitial ads.
 * Each ad network implements this to load and show interstitial ads.
 */
interface InterstitialAdProvider {

    /** The ad provider this implementation belongs to. */
    val provider: AdProvider

    /**
     * Load an interstitial ad.
     *
     * @param context Application or activity context
     * @param adUnitId Network-specific ad unit ID
     * @param callback Receives load result
     */
    fun loadAd(context: Context, adUnitId: String, callback: InterstitialAdCallback)

    /**
     * Show a previously loaded interstitial ad.
     *
     * @param activity The activity to show the ad from
     * @param callback Receives show lifecycle events
     */
    fun showAd(activity: Activity, callback: InterstitialShowCallback)

    /** @return true if an ad is loaded and ready to show */
    fun isAdReady(): Boolean

    /** Release resources held by this provider. */
    fun destroy()

    /** Callback for interstitial ad load events. */
    interface InterstitialAdCallback {
        fun onAdLoaded()
        fun onAdFailedToLoad(error: AdKitAdError)
    }

    /** Callback for interstitial ad show lifecycle events. */
    interface InterstitialShowCallback {
        fun onAdShowed() {}
        fun onAdDismissed()
        fun onAdFailedToShow(error: AdKitAdError) {}
        fun onAdClicked() {}
        fun onAdImpression() {}
        fun onPaidEvent(adValue: AdKitAdValue) {}
    }
}
