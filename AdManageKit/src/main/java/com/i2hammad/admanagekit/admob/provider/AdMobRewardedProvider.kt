package com.i2hammad.admanagekit.admob.provider

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                loadedAds[adUnitId] = ad
                Log.d(TAG, "Rewarded ad loaded: $adUnitId")
                callback.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                // Do NOT discard a previously loaded ad for this (or any other) unit:
                // the failure only concerns this load request.
                Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                callback.onAdFailedToLoad(error.toAdKitError())
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

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed: $adUnitId")
                callback.onAdShowed()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed: $adUnitId")
                loadedAds.remove(adUnitId, ad)
                callback.onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                loadedAds.remove(adUnitId, ad)
                callback.onAdFailedToShow(adError.toAdKitError())
                callback.onAdDismissed()
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

        ad.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            callback.onRewardEarned(rewardItem.type, rewardItem.amount)
        }
    }

    override fun isAdReady(): Boolean = loadedAds.isNotEmpty()

    override fun isAdReady(adUnitId: String): Boolean = loadedAds.containsKey(adUnitId)

    override fun destroy() {
        loadedAds.clear()
    }
}
