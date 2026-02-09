package com.i2hammad.admanagekit.yandex

import android.app.Activity
import android.content.Context
import android.util.Log
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.RewardedAdProvider
import com.i2hammad.admanagekit.yandex.internal.toAdKitError
import com.i2hammad.admanagekit.yandex.internal.toAdKitValue

/**
 * Yandex Ads implementation of [RewardedAdProvider].
 * Wraps Yandex RewardedAd behind the provider interface.
 */
class YandexRewardedProvider : RewardedAdProvider {

    override val provider: AdProvider = AdProvider.YANDEX

    private var rewardedAd: RewardedAd? = null
    private var rewardedAdLoader: RewardedAdLoader? = null

    companion object {
        private const val TAG = "YandexRewarded"
    }

    override fun loadAd(
        context: Context,
        adUnitId: String,
        callback: RewardedAdProvider.RewardedAdCallback
    ) {
        val loader = RewardedAdLoader(context).apply {
            setAdLoadListener(object : RewardedAdLoadListener {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded: $adUnitId")
                    callback.onAdLoaded()
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded ad failed to load: ${error.description}")
                    callback.onAdFailedToLoad(error.toAdKitError())
                }
            })
        }
        rewardedAdLoader = loader

        val adRequestConfiguration = AdRequestConfiguration.Builder(adUnitId).build()
        loader.loadAd(adRequestConfiguration)
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

        ad.setAdEventListener(object : RewardedAdEventListener {
            override fun onAdShown() {
                Log.d(TAG, "Rewarded ad shown")
                callback.onAdShowed()
            }

            override fun onAdFailedToShow(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.description}")
                rewardedAd?.setAdEventListener(null)
                rewardedAd = null
                callback.onAdFailedToShow(adError.toAdKitError())
                callback.onAdDismissed()
            }

            override fun onAdDismissed() {
                Log.d(TAG, "Rewarded ad dismissed")
                rewardedAd?.setAdEventListener(null)
                rewardedAd = null
                callback.onAdDismissed()
            }

            override fun onAdClicked() {
                callback.onAdClicked()
            }

            override fun onAdImpression(impressionData: ImpressionData?) {
                callback.onAdImpression()
                callback.onPaidEvent(impressionData.toAdKitValue())
            }

            override fun onRewarded(reward: Reward) {
                Log.d(TAG, "User earned reward: ${reward.amount} ${reward.type}")
                callback.onRewardEarned(reward.type, reward.amount)
            }
        })

        ad.show(activity)
    }

    override fun isAdReady(): Boolean = rewardedAd != null

    override fun destroy() {
        rewardedAdLoader?.setAdLoadListener(null)
        rewardedAdLoader = null
        rewardedAd?.setAdEventListener(null)
        rewardedAd = null
    }
}
