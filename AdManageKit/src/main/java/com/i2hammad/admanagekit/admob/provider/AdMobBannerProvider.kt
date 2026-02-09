package com.i2hammad.admanagekit.admob.provider

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.BannerAdProvider

/**
 * AdMob implementation of [BannerAdProvider].
 * Wraps Google AdMob AdView behind the provider interface.
 */
class AdMobBannerProvider(
    private val adSize: AdSize = AdSize.BANNER
) : BannerAdProvider {

    override val provider: AdProvider = AdProvider.ADMOB

    private var adView: AdView? = null

    companion object {
        private const val TAG = "AdMobBanner"
    }

    override fun loadBanner(
        context: Context,
        adUnitId: String,
        callback: BannerAdProvider.BannerAdCallback
    ) {
        val bannerView = AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(this@AdMobBannerProvider.adSize)
        }

        bannerView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner ad loaded: $adUnitId")
                adView = bannerView
                callback.onBannerLoaded(bannerView)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Banner ad failed to load: ${error.message}")
                callback.onBannerFailedToLoad(error.toAdKitError())
            }

            override fun onAdClicked() {
                callback.onBannerClicked()
            }

            override fun onAdImpression() {
                callback.onBannerImpression()
            }
        }

        bannerView.onPaidEventListener = OnPaidEventListener { adValue ->
            callback.onPaidEvent(adValue.toAdKitValue())
        }

        bannerView.loadAd(AdRequest.Builder().build())
    }

    override fun pause() {
        adView?.pause()
    }

    override fun resume() {
        adView?.resume()
    }

    override fun destroy() {
        adView?.destroy()
        adView = null
    }
}
