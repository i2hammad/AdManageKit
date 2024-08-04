package com.i2hammad.admanagekit.admob
import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardedAdManager {
    private var rewardedAd: RewardedAd? = null
    private var isLoading: Boolean = false
    private lateinit var adUnitId: String
    private const val TAG = "RewardedAdManager"


    public interface OnAdDismissedListener {
        fun onAdDismissed()
    }


    fun initialize(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId
        loadRewardedAd(context)
    }

    public fun loadRewardedAd(context: Context) {
        if (isLoading || rewardedAd != null) {
            return
        }
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {
                isLoading = false
                rewardedAd = null
                Log.d(TAG, "Ad failed to load: ${adError.message}")
            }

            override fun onAdLoaded(ad: RewardedAd) {
                isLoading = false
                rewardedAd = ad
                Log.d(TAG, "Ad was loaded.")
            }
        })
    }

    public fun showAd(
        activity: Activity,
        onUserEarnedRewardListener: OnUserEarnedRewardListener,
        onAdDismissedListener: OnAdDismissedListener
    ) {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed fullscreen content.")
                rewardedAd = null
                loadRewardedAd(activity) // Reload the ad for future use
                onAdDismissedListener.onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e(TAG, "Ad failed to show fullscreen content.")
                rewardedAd = null
                onAdDismissedListener.onAdDismissed()
            }

            override fun onAdImpression() {
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }

        rewardedAd?.let {
            it.show(activity, onUserEarnedRewardListener)
            rewardedAd = null
        } ?: loadRewardedAd(activity)
    }

    fun isAdLoaded(): Boolean {
        return rewardedAd != null
    }
}
