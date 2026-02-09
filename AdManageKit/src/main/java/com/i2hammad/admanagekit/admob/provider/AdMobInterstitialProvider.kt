package com.i2hammad.admanagekit.admob.provider

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.InterstitialAdProvider

/**
 * AdMob implementation of [InterstitialAdProvider].
 * Wraps Google AdMob InterstitialAd behind the provider interface.
 */
class AdMobInterstitialProvider : InterstitialAdProvider {

    override val provider: AdProvider = AdProvider.ADMOB

    private var interstitialAd: InterstitialAd? = null
    private var currentShowCallback: InterstitialAdProvider.InterstitialShowCallback? = null

    companion object {
        private const val TAG = "AdMobInterstitial"
    }

    override fun loadAd(
        context: Context,
        adUnitId: String,
        callback: InterstitialAdProvider.InterstitialAdCallback
    ) {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                Log.d(TAG, "Interstitial ad loaded: $adUnitId")
                callback.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
                Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                callback.onAdFailedToLoad(error.toAdKitError())
            }
        })
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

        currentShowCallback = callback

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed")
                callback.onAdShowed()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                interstitialAd = null
                callback.onAdDismissed()
                currentShowCallback = null
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                interstitialAd = null
                callback.onAdFailedToShow(adError.toAdKitError())
                callback.onAdDismissed()
                currentShowCallback = null
            }

            override fun onAdClicked() {
                callback.onAdClicked()
            }

            override fun onAdImpression() {
                callback.onAdImpression()
            }
        }

        ad.onPaidEventListener = OnPaidEventListener { adValue ->
            callback.onPaidEvent(adValue.toAdKitValue())
        }

        ad.show(activity)
    }

    override fun isAdReady(): Boolean = interstitialAd != null

    override fun destroy() {
        interstitialAd = null
        currentShowCallback = null
    }
}
