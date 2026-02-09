package com.i2hammad.admanagekit.admob.provider

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdSize

/**
 * AdMob implementation of [NativeAdProvider].
 * Wraps Google AdMob NativeAd behind the provider interface.
 *
 * Returns a minimal [NativeAdView] wrapper. The opaque [nativeAdRef] in the callback
 * is the raw [NativeAd] object — callers can cast it and bind their own custom layout
 * if desired.
 */
class AdMobNativeProvider : NativeAdProvider {

    override val provider: AdProvider = AdProvider.ADMOB

    private var currentNativeAd: NativeAd? = null

    companion object {
        private const val TAG = "AdMobNative"
    }

    override fun loadNativeAd(
        context: Context,
        adUnitId: String,
        callback: NativeAdProvider.NativeAdCallback,
        sizeHint: NativeAdSize
    ) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd

                nativeAd.setOnPaidEventListener { adValue ->
                    callback.onPaidEvent(adValue.toAdKitValue())
                }

                // Create a minimal NativeAdView container.
                // The caller receives the raw NativeAd as nativeAdRef and can
                // bind it to their own layout if needed.
                val adView = NativeAdView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                    setNativeAd(nativeAd)
                }

                Log.d(TAG, "Native ad loaded: $adUnitId")
                callback.onNativeAdLoaded(adView, nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Native ad failed to load: ${error.message}")
                    callback.onNativeAdFailedToLoad(error.toAdKitError())
                }

                override fun onAdClicked() {
                    callback.onNativeAdClicked()
                }

                override fun onAdImpression() {
                    callback.onNativeAdImpression()
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    override fun destroy() {
        currentNativeAd?.destroy()
        currentNativeAd = null
    }
}
