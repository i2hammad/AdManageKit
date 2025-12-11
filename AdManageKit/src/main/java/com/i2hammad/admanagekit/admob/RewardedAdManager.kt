package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.core.BillingConfig

object RewardedAdManager {
    private var rewardedAd: RewardedAd? = null
    private var isLoading: Boolean = false
    private lateinit var adUnitId: String
    private const val TAG = "RewardedAdManager"

    private var firebaseAnalytics: FirebaseAnalytics? = null

    public interface OnAdDismissedListener {
        fun onAdDismissed()
    }


    fun initialize(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId
        // Initialize Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        loadRewardedAd(context)
    }

    public fun loadRewardedAd(context: Context) {
        if (isLoading || rewardedAd != null) {
            return
        }
        isLoading = true
        val adRequest = AdRequest.Builder(adUnitId).build()
        RewardedAd.load(adRequest, object : AdLoadCallback<RewardedAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isLoading = false
                rewardedAd = null
                Log.d(TAG, "Ad failed to load: ${adError.message}")


                // Log Firebase event for ad failed to load
                val params = Bundle()
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                params.putString("ad_error_code", "${adError.code}")
                firebaseAnalytics!!.logEvent("ad_failed_to_load", params)

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
        rewardedAd?.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdClicked() {
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed fullscreen content.")
                rewardedAd = null
                loadRewardedAd(activity) // Reload the ad for future use
                onAdDismissedListener.onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                Log.e(TAG, "Ad failed to show fullscreen content: ${fullScreenContentError.message}")
                rewardedAd = null
                onAdDismissedListener.onAdDismissed()
            }

            override fun onAdImpression() {
                Log.d(TAG, "Ad recorded an impression.")

                val params = Bundle()
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                firebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)

            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
            }

            override fun onAdPaid(value: AdValue) {
                super.onAdPaid(value)

                val adValueInStandardUnits = value.valueMicros / 1000000.0

                // Log Firebase event for paid event
                val params = Bundle()
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                params.putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                params.putString(FirebaseAnalytics.Param.CURRENCY, value.currencyCode)
                firebaseAnalytics?.logEvent("ad_paid_event", params)
            }
        }

//        rewardedAd?.onPaidEventListener =
//            OnPaidEventListener { adValue -> // Convert the value from micros to the standard currency unit
//
//            }

        rewardedAd?.let {
            it.show(activity, onUserEarnedRewardListener)
            rewardedAd = null
        } ?: loadRewardedAd(activity)
    }

    fun isAdLoaded(): Boolean {
        var purchaseProvider = BillingConfig.getPurchaseProvider()
        return rewardedAd != null && !purchaseProvider.isPurchased()
    }
}
