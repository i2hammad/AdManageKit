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

    /**
     * Show the ad that was loaded for [adUnitId].
     *
     * Default implementation ignores the ad unit and delegates to [showAd], which is
     * correct for single-slot providers. Providers that cache ads per ad unit should
     * override this to show the exact ad loaded for [adUnitId].
     */
    fun showAd(activity: Activity, adUnitId: String, callback: AppOpenShowCallback) {
        showAd(activity, callback)
    }

    /** @return true if an ad is loaded and ready to show */
    fun isAdReady(): Boolean

    /**
     * @return true if an ad loaded for [adUnitId] is ready to show.
     * Default implementation ignores the ad unit and delegates to [isAdReady].
     */
    fun isAdReady(adUnitId: String): Boolean = isAdReady()

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
