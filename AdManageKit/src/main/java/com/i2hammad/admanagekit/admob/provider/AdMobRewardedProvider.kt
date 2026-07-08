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
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.RewardedAdProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * AdMob implementation of [RewardedAdProvider].
 * Wraps Google AdMob RewardedAd behind the provider interface.
 *
 * Loaded ads are stored per ad unit ID so a single shared provider instance can
 * serve multiple placements (waterfalls) without one placement's load clobbering
 * or consuming another placement's ready ad.
 */
class AdMobRewardedProvider : RewardedAdProvider {

    override val provider: AdProvider = AdProvider.ADMOB

    /** Ready ads keyed by ad unit ID. */
    private val loadedAds = ConcurrentHashMap<String, RewardedAd>()

    companion object {
        private const val TAG = "AdMobRewarded"
    }

    override fun loadAd(
        context: Context,
        adUnitId: String,
        callback: RewardedAdProvider.RewardedAdCallback
    ) {
        val adRequest = AdRequest.Builder(adUnitId).build()

        RewardedAd.load(adRequest, object : AdLoadCallback<RewardedAd> {
            override fun onAdLoaded(ad: RewardedAd) {
                Handler(Looper.getMainLooper()).post {
                    loadedAds[adUnitId] = ad
                    Log.d(TAG, "Rewarded ad loaded: $adUnitId")
                    callback.onAdLoaded()
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Handler(Looper.getMainLooper()).post {
                    // Do NOT discard a previously loaded ad for this (or any other) unit:
                    // the failure only concerns this load request.
                    Log.e(TAG, "Rewarded ad failed to load: ${adError.message}")
                    callback.onAdFailedToLoad(adError.toAdKitError())
                }
            }
        })
    }

    override fun showAd(activity: Activity, callback: RewardedAdProvider.RewardedShowCallback) {
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
        callback: RewardedAdProvider.RewardedShowCallback
    ) {
        val ad = loadedAds[adUnitId]
        if (ad == null) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded for $adUnitId", provider.name)
            )
            callback.onAdDismissed()
            return
        }

        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Handler(Looper.getMainLooper()).post {
                    Log.d(TAG, "Rewarded ad showed: $adUnitId")
                    callback.onAdShowed()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                Handler(Looper.getMainLooper()).post {
                    Log.d(TAG, "Rewarded ad dismissed: $adUnitId")
                    loadedAds.remove(adUnitId, ad)
                    callback.onAdDismissed()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                Handler(Looper.getMainLooper()).post {
                    Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
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

        ad.show(activity) { rewardItem ->
            Handler(Looper.getMainLooper()).post {
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                callback.onRewardEarned(rewardItem.type, rewardItem.amount)
            }
        }
    }

    override fun isAdReady(): Boolean = loadedAds.isNotEmpty()

    override fun isAdReady(adUnitId: String): Boolean = loadedAds.containsKey(adUnitId)

    override fun destroy() {
        loadedAds.clear()
    }
}
