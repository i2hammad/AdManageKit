package com.i2hammad.admanagekit.yandex

import android.app.Activity
import android.content.Context
import android.util.Log
import com.yandex.mobile.ads.appopenad.AppOpenAd
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AppOpenAdProvider
import com.i2hammad.admanagekit.yandex.internal.toAdKitError
import com.i2hammad.admanagekit.yandex.internal.toAdKitValue

/**
 * Yandex Ads implementation of [AppOpenAdProvider].
 * Wraps Yandex AppOpenAd behind the provider interface.
 */
class YandexAppOpenProvider : AppOpenAdProvider {

    override val provider: AdProvider = AdProvider.YANDEX

    private var appOpenAd: AppOpenAd? = null
    private var appOpenAdLoader: AppOpenAdLoader? = null

    companion object {
        private const val TAG = "YandexAppOpen"
    }

    override fun loadAd(
        context: Context,
        adUnitId: String,
        callback: AppOpenAdProvider.AppOpenAdCallback
    ) {
        val loader = AppOpenAdLoader(context).apply {
            setAdLoadListener(object : AppOpenAdLoadListener {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    Log.d(TAG, "App open ad loaded: $adUnitId")
                    callback.onAdLoaded()
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    appOpenAd = null
                    Log.e(TAG, "App open ad failed to load: ${error.description}")
                    callback.onAdFailedToLoad(error.toAdKitError())
                }
            })
        }
        appOpenAdLoader = loader

        val adRequestConfiguration = AdRequestConfiguration.Builder(adUnitId).build()
        loader.loadAd(adRequestConfiguration)
    }

    override fun showAd(activity: Activity, callback: AppOpenAdProvider.AppOpenShowCallback) {
        val ad = appOpenAd
        if (ad == null) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded", provider.name)
            )
            callback.onAdDismissed()
            return
        }

        ad.setAdEventListener(object : AppOpenAdEventListener {
            override fun onAdShown() {
                Log.d(TAG, "App open ad shown")
                callback.onAdShowed()
            }

            override fun onAdFailedToShow(adError: AdError) {
                Log.e(TAG, "App open ad failed to show: ${adError.description}")
                appOpenAd?.setAdEventListener(null)
                appOpenAd = null
                callback.onAdFailedToShow(adError.toAdKitError())
                callback.onAdDismissed()
            }

            override fun onAdDismissed() {
                Log.d(TAG, "App open ad dismissed")
                appOpenAd?.setAdEventListener(null)
                appOpenAd = null
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

    override fun isAdReady(): Boolean = appOpenAd != null

    override fun destroy() {
        appOpenAdLoader?.setAdLoadListener(null)
        appOpenAdLoader = null
        appOpenAd?.setAdEventListener(null)
        appOpenAd = null
    }
}
