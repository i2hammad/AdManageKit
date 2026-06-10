package com.i2hammad.admanagekit.admob.provider

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdSize
import com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader

/**
 * AdMob implementation of [NativeAdProvider].
 * Wraps Google AdMob NativeAd behind the provider interface.
 *
 * Returns a fully populated template view (layout chosen by the sizeHint) with the
 * native ad bound to it, so impression/click tracking is only activated on a view
 * with visible assets. The opaque [nativeAdRef] in the callback is the raw
 * [NativeAd] object — callers can cast it and re-bind their own custom layout
 * if desired (the last bound NativeAdView owns tracking).
 *
 * Ownership: once an ad is delivered via [NativeAdProvider.NativeAdCallback.onNativeAdLoaded],
 * the consumer owns it and is responsible for calling [NativeAd.destroy] when done.
 * The provider never destroys handed-out ads (they may be displayed elsewhere).
 */
class AdMobNativeProvider : NativeAdProvider {

    override val provider: AdProvider = AdProvider.ADMOB

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
                nativeAd.setOnPaidEventListener { adValue ->
                    callback.onPaidEvent(adValue.toAdKitValue())
                }

                // Build a real template view and bind the ad's assets to it before
                // activating tracking via setNativeAd(). Consumers that prefer their
                // own layout can re-bind the raw nativeAdRef instead.
                val adView = createBoundAdView(context, nativeAd, sizeHint)

                Log.d(TAG, "Native ad loaded: $adUnitId")
                // Ownership of nativeAd transfers to the consumer here.
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

    /** Inflate the template for [sizeHint], populate its asset views and bind [nativeAd]. */
    private fun createBoundAdView(context: Context, nativeAd: NativeAd, sizeHint: NativeAdSize): NativeAdView {
        val templateSize = when (sizeHint) {
            NativeAdSize.SMALL -> ProgrammaticNativeAdLoader.NativeAdSize.SMALL
            NativeAdSize.MEDIUM -> ProgrammaticNativeAdLoader.NativeAdSize.MEDIUM
            NativeAdSize.LARGE -> ProgrammaticNativeAdLoader.NativeAdSize.LARGE
        }

        val adView = ProgrammaticNativeAdLoader.createNativeAdView(context, templateSize)
        adView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )

        (adView.headlineView as? TextView)?.text = nativeAd.headline ?: ""
        (adView.bodyView as? TextView)?.text = nativeAd.body ?: ""
        (adView.callToActionView as? TextView)?.text = nativeAd.callToAction ?: ""

        adView.iconView?.let { iconView ->
            val icon = nativeAd.icon
            if (icon == null) {
                iconView.visibility = View.INVISIBLE
            } else {
                (iconView as? ImageView)?.setImageDrawable(icon.drawable)
                iconView.visibility = View.VISIBLE
            }
        }

        adView.advertiserView?.let { advertiserView ->
            val advertiser = nativeAd.advertiser
            if (advertiser == null) {
                advertiserView.visibility = View.INVISIBLE
            } else {
                (advertiserView as? TextView)?.text = advertiser
                advertiserView.visibility = View.VISIBLE
            }
        }

        nativeAd.mediaContent?.let { mediaContent ->
            adView.mediaView?.mediaContent = mediaContent
        }

        adView.setNativeAd(nativeAd)
        return adView
    }

    override fun destroy() {
        // Intentionally empty: all loaded ads are handed to consumers, who own them.
        // Destroying them here could kill an ad that is still displayed elsewhere.
    }
}
