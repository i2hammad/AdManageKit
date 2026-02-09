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
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.RewardedAdProvider

/**
 * AdMob implementation of [RewardedAdProvider].
 * Wraps Google AdMob RewardedAd behind the provider interface.
 */
class AdMobRewardedProvider : RewardedAdProvider {

    override val provider: AdProvider = AdProvider.ADMOB

    private var rewardedAd: RewardedAd? = null

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
                rewardedAd = ad
                Log.d(TAG, "Rewarded ad loaded: $adUnitId")
                callback.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
                Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                callback.onAdFailedToLoad(error.toAdKitError())
            }
        })
    }

    override fun showAd(activity: Activity, callback: RewardedAdProvider.RewardedShowCallback) {
        val ad = rewardedAd
        if (ad == null) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded", provider.name)
            )
            callback.onAdDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed")
                callback.onAdShowed()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                rewardedAd = null
                callback.onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
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

    override fun isAdReady(): Boolean = rewardedAd != null

    override fun destroy() {
        rewardedAd = null
    }
}
