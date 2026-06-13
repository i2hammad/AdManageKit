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
     * @param templateLayoutResId Optional layout resource id of an AdMob-style native
     *        template (the same XML used by the AdMob renderer). When non-zero, providers
     *        that support it should inflate this exact layout and bind their assets to the
     *        standard asset ids (`ad_headline`, `ad_body`, `ad_call_to_action`,
     *        `ad_app_icon`, `ad_advertiser`, `ad_media`, `ad_stars`) so a fallback
     *        provider renders the same template as AdMob. Pass 0 (default) to use the
     *        provider's built-in [sizeHint]-based layout. It is an Int rather than a typed
     *        reference to keep this core module dependency-free.
     */
    fun loadNativeAd(
        context: Context,
        adUnitId: String,
        callback: NativeAdCallback,
        sizeHint: NativeAdSize = NativeAdSize.LARGE,
        templateLayoutResId: Int = 0
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
        /** The ad opened an overlay/left the app. Not all networks emit this. */
        fun onNativeAdOpened() {}
        /** The ad's overlay closed/returned to the app. Not all networks emit this. */
        fun onNativeAdClosed() {}
    }
}
