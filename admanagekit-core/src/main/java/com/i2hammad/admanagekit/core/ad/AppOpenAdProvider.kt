package com.i2hammad.admanagekit.core.ad

import android.app.Activity
import android.content.Context

/**
 * Provider interface for app open ads.
 * Each ad network implements this to load and show app open ads.
 */
interface AppOpenAdProvider {

    /** The ad provider this implementation belongs to. */
    val provider: AdProvider

    /**
     * Load an app open ad.
     *
     * @param context Application context
     * @param adUnitId Network-specific ad unit ID
     * @param callback Receives load result
     */
    fun loadAd(context: Context, adUnitId: String, callback: AppOpenAdCallback)

    /**
     * Show a previously loaded app open ad.
     *
     * @param activity The activity to show the ad from
     * @param callback Receives show lifecycle events
     */
    fun showAd(activity: Activity, callback: AppOpenShowCallback)

    /** @return true if an ad is loaded and ready to show */
    fun isAdReady(): Boolean

    /** Release resources held by this provider. */
    fun destroy()

    /** Callback for app open ad load events. */
    interface AppOpenAdCallback {
        fun onAdLoaded()
        fun onAdFailedToLoad(error: AdKitAdError)
    }

    /** Callback for app open ad show lifecycle events. */
    interface AppOpenShowCallback {
        fun onAdShowed() {}
        fun onAdDismissed()
        fun onAdFailedToShow(error: AdKitAdError) {}
        fun onAdClicked() {}
        fun onAdImpression() {}
        fun onPaidEvent(adValue: AdKitAdValue) {}
    }
}
