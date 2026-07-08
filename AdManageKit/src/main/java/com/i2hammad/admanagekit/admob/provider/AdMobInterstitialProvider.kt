package com.i2hammad.admanagekit.admob.provider

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.InterstitialAdProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * AdMob implementation of [InterstitialAdProvider].
 * Wraps Google AdMob InterstitialAd behind the provider interface.
 *
 * Loaded ads are stored per ad unit ID so a single shared provider instance can
 * serve multiple placements (waterfalls) without one placement's load clobbering
 * or consuming another placement's ready ad.
 */
class AdMobInterstitialProvider : InterstitialAdProvider {

    override val provider: AdProvider = AdProvider.ADMOB

    /** Ready ads keyed by ad unit ID. */
    private val loadedAds = ConcurrentHashMap<String, InterstitialAd>()

    companion object {
        private const val TAG = "AdMobInterstitial"
    }

    override fun loadAd(
        context: Context,
        adUnitId: String,
        callback: InterstitialAdProvider.InterstitialAdCallback
    ) {
        val adRequest = AdRequest.Builder(adUnitId).build()

        InterstitialAd.load(adRequest, object : AdLoadCallback<InterstitialAd> {
            override fun onAdLoaded(ad: InterstitialAd) {
                Handler(Looper.getMainLooper()).post {
                    loadedAds[adUnitId] = ad
                    Log.d(TAG, "Interstitial ad loaded: $adUnitId")
                    callback.onAdLoaded()
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Handler(Looper.getMainLooper()).post {
                    // Do NOT discard a previously loaded ad for this (or any other) unit:
                    // the failure only concerns this load request.
                    Log.e(TAG, "Interstitial ad failed to load: ${adError.message}")
                    callback.onAdFailedToLoad(adError.toAdKitError())
                }
            }
        })
    }

    override fun showAd(activity: Activity, callback: InterstitialAdProvider.InterstitialShowCallback) {
        // No-arg fallback: show any ready ad.
        val adUnitId = loadedAds.keys.firstOrNull()
        if (adUnitId == null) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded", provider.name)
            )
            callback.onAdDismissed()
            return
        }
        showAd(activity, adUnitId, callback)
    }

    override fun showAd(
        activity: Activity,
        adUnitId: String,
        callback: InterstitialAdProvider.InterstitialShowCallback
    ) {
        val ad = loadedAds[adUnitId]
        if (ad == null) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded for $adUnitId", provider.name)
            )
            callback.onAdDismissed()
            return
        }

        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Handler(Looper.getMainLooper()).post {
                    Log.d(TAG, "Interstitial ad showed: $adUnitId")
                    callback.onAdShowed()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                Handler(Looper.getMainLooper()).post {
                    Log.d(TAG, "Interstitial ad dismissed: $adUnitId")
                    loadedAds.remove(adUnitId, ad)
                    callback.onAdDismissed()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                Handler(Looper.getMainLooper()).post {
                    Log.e(TAG, "Interstitial ad failed to show: ${adError.message}")
                    loadedAds.remove(adUnitId, ad)
                    callback.onAdFailedToShow(adError.toAdKitError())
                    callback.onAdDismissed()
                }
            }

            override fun onAdClicked() {
                Handler(Looper.getMainLooper()).post {
                    callback.onAdClicked()
                }
            }

            override fun onAdImpression() {
                Handler(Looper.getMainLooper()).post {
                    callback.onAdImpression()
                }
            }

            override fun onAdPaid(value: AdValue) {
                Handler(Looper.getMainLooper()).post {
                    callback.onPaidEvent(value.toAdKitValue())
                }
            }
        }

        ad.show(activity)
    }

    override fun isAdReady(): Boolean = loadedAds.isNotEmpty()

    override fun isAdReady(adUnitId: String): Boolean = loadedAds.containsKey(adUnitId)

    override fun destroy() {
        loadedAds.clear()
    }
}
