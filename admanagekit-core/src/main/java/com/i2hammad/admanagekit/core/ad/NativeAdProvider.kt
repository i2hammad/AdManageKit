package com.i2hammad.admanagekit.core.ad

import android.content.Context
import android.view.View

/**
 * Provider interface for native ads.
 * Each ad network implements this to load and display native ads.
 */
interface NativeAdProvider {

    /** The ad provider this implementation belongs to. */
    val provider: AdProvider

    /**
     * Load a native ad.
     *
     * @param context Activity or application context
     * @param adUnitId Network-specific ad unit ID
     * @param callback Receives the native ad View and an opaque reference
     * @param sizeHint Hint for the desired ad view size (default: LARGE)
     */
    fun loadNativeAd(
        context: Context,
        adUnitId: String,
        callback: NativeAdCallback,
        sizeHint: NativeAdSize = NativeAdSize.LARGE
    )

    /** Release resources held by this provider. */
    fun destroy()

    /** Callback for native ad events. */
    interface NativeAdCallback {
        /**
         * Called when a native ad is loaded.
         *
         * @param adView The rendered native ad View
         * @param nativeAdRef Opaque reference to the underlying native ad object.
         *                    Hold this reference to prevent garbage collection.
         */
        fun onNativeAdLoaded(adView: View, nativeAdRef: Any)
        fun onNativeAdFailedToLoad(error: AdKitAdError)
        fun onNativeAdClicked() {}
        fun onNativeAdImpression() {}
        fun onPaidEvent(adValue: AdKitAdValue) {}
    }
}
