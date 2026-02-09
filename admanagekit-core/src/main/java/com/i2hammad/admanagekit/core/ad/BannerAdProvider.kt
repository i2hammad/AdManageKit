package com.i2hammad.admanagekit.core.ad

import android.content.Context
import android.view.View

/**
 * Provider interface for banner ads.
 * Each ad network implements this to load and display banner ads.
 */
interface BannerAdProvider {

    /** The ad provider this implementation belongs to. */
    val provider: AdProvider

    /**
     * Load a banner ad and return the View to display.
     *
     * @param context Activity or application context
     * @param adUnitId Network-specific ad unit ID
     * @param callback Receives the loaded banner View or error
     */
    fun loadBanner(context: Context, adUnitId: String, callback: BannerAdCallback)

    /** Pause the banner ad (e.g., when activity pauses). */
    fun pause()

    /** Resume the banner ad (e.g., when activity resumes). */
    fun resume()

    /** Release resources held by this provider. */
    fun destroy()

    /** Callback for banner ad events. */
    interface BannerAdCallback {
        fun onBannerLoaded(bannerView: View)
        fun onBannerFailedToLoad(error: AdKitAdError)
        fun onBannerClicked() {}
        fun onBannerImpression() {}
        fun onPaidEvent(adValue: AdKitAdValue) {}
    }
}
