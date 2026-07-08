package com.i2hammad.admanagekit.admob.provider

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.i2hammad.admanagekit.R
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
        sizeHint: NativeAdSize,
        // AdMob hands back the raw NativeAd; the consumer (NativeTemplateView) re-binds it
        // into the selected template layout itself, so the template id is not needed here.
        templateLayoutResId: Int
    ) {
        // Next-Gen native ads are requested via the static NativeAdLoader, never instantiated.
        val nativeAdRequest = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE)).build()

        NativeAdLoader.load(nativeAdRequest, object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                // Next-Gen SDK callbacks fire on a background thread; all the work below
                // touches views, so it must run on the main thread.
                Handler(Looper.getMainLooper()).post {
                    // Click/impression/paid reporting is no longer a separate AdListener -
                    // it is delivered through the loaded NativeAd's own adEventCallback.
                    nativeAd.adEventCallback = object : NativeAdEventCallback {
                        override fun onAdClicked() {
                            callback.onNativeAdClicked()
                        }

                        override fun onAdImpression() {
                            callback.onNativeAdImpression()
                        }

                        override fun onAdPaid(value: AdValue) {
                            callback.onPaidEvent(value.toAdKitValue())
                        }
                        // NOTE: Next-Gen's NativeAdEventCallback has no onAdOpened()/onAdClosed()
                        // equivalent for native ads (those were legacy AdListener callbacks tied
                        // to full-screen content). callback.onNativeAdOpened()/onNativeAdClosed()
                        // are therefore never invoked from this provider anymore.
                    }

                    // Build a real template view and bind the ad's assets to it before
                    // activating tracking via registerNativeAd(). Consumers that prefer their
                    // own layout can re-bind the raw nativeAdRef instead.
                    val adView = createBoundAdView(context, nativeAd, sizeHint)

                    Log.d(TAG, "Native ad loaded: $adUnitId")
                    // Ownership of nativeAd transfers to the consumer here.
                    callback.onNativeAdLoaded(adView, nativeAd)
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Handler(Looper.getMainLooper()).post {
                    Log.e(TAG, "Native ad failed to load: ${adError.message}")
                    callback.onNativeAdFailedToLoad(adError.toAdKitError())
                }
            }
        })
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

        // setNativeAd() no longer exists on NativeAdView - registerNativeAd() both binds
        // the ad's assets/tracking to this view AND renders media into the MediaView in one
        // call. Do NOT also call mediaView.mediaContent = ... beforehand - it leaves the
        // MediaView blank (the SDK's automatic media rendering only kicks in through
        // registerNativeAd(), per Google's docs; a manual pre-assignment was found to
        // interfere with it).
        //
        // Look the MediaView up directly by id rather than via adView.mediaView: this view
        // is inflated into a temporary, never-window-attached parent (createNativeAdView()),
        // and the getter's auto-discovery was found to return null in that state, leaving
        // media blank for the LARGE template (SMALL/MEDIUM have no MediaView at all, so this
        // is a no-op there either way).
        val mediaView: MediaView? = adView.findViewById(R.id.media_view)
        adView.registerNativeAd(nativeAd, mediaView)
        return adView
    }

    override fun destroy() {
        // Intentionally empty: all loaded ads are handed to consumers, who own them.
        // Destroying them here could kill an ad that is still displayed elsewhere.
    }
}
