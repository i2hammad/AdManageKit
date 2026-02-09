package com.i2hammad.admanagekit.yandex

import android.app.Activity
import android.content.Context
import android.util.Log
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.InterstitialAdProvider
import com.i2hammad.admanagekit.yandex.internal.toAdKitError
import com.i2hammad.admanagekit.yandex.internal.toAdKitValue

/**
 * Yandex Ads implementation of [InterstitialAdProvider].
 * Wraps Yandex InterstitialAd behind the provider interface.
 */
class YandexInterstitialProvider : InterstitialAdProvider {

    override val provider: AdProvider = AdProvider.YANDEX

    private var interstitialAd: InterstitialAd? = null
    private var interstitialAdLoader: InterstitialAdLoader? = null

    companion object {
        private const val TAG = "YandexInterstitial"
    }

    override fun loadAd(
        context: Context,
        adUnitId: String,
        callback: InterstitialAdProvider.InterstitialAdCallback
    ) {
        val loader = InterstitialAdLoader(context).apply {
            setAdLoadListener(object : InterstitialAdLoadListener {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded: $adUnitId")
                    callback.onAdLoaded()
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial ad failed to load: ${error.description}")
                    callback.onAdFailedToLoad(error.toAdKitError())
                }
            })
        }
        interstitialAdLoader = loader

        val adRequestConfiguration = AdRequestConfiguration.Builder(adUnitId).build()
        loader.loadAd(adRequestConfiguration)
    }

    override fun showAd(activity: Activity, callback: InterstitialAdProvider.InterstitialShowCallback) {
        val ad = interstitialAd
        if (ad == null) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded", provider.name)
            )
            callback.onAdDismissed()
            return
        }

        ad.setAdEventListener(object : InterstitialAdEventListener {
            override fun onAdShown() {
                Log.d(TAG, "Interstitial ad shown")
                callback.onAdShowed()
            }

            override fun onAdFailedToShow(adError: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${adError.description}")
                interstitialAd?.setAdEventListener(null)
                interstitialAd = null
                callback.onAdFailedToShow(adError.toAdKitError())
                callback.onAdDismissed()
            }

            override fun onAdDismissed() {
                Log.d(TAG, "Interstitial ad dismissed")
                interstitialAd?.setAdEventListener(null)
                interstitialAd = null
                callback.onAdDismissed()
            }

            override fun onAdClicked() {
                callback.onAdClicked()
            }

            override fun onAdImpression(impressionData: ImpressionData?) {
                callback.onAdImpression()
                callback.onPaidEvent(impressionData.toAdKitValue())
            }
        })

        ad.show(activity)
    }

    override fun isAdReady(): Boolean = interstitialAd != null

    override fun destroy() {
        interstitialAdLoader?.setAdLoadListener(null)
        interstitialAdLoader = null
        interstitialAd?.setAdEventListener(null)
        interstitialAd = null
    }
}
