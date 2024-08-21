package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.billing.AppPurchase

/**
 * AdManager is a singleton class responsible for managing interstitial ads
 * using Google AdMob. It provides functionality to load and show ads, manage
 * display intervals, and handle ad-related callbacks.
 */
class AdManager private constructor() {

    private var mInterstitialAd: InterstitialAd? = null
    private var adUnitId: String? = null
    private var isAdLoading = false
    private var isDisplayingAd = false
    private var lastAdShowTime: Long = 0
    private var adIntervalMillis: Long = 15 * 1000 // Default to 15 seconds
    private var adDisplayCount = 0 // Track the number of times ads have been displayed
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    companion object {
        const val PURCHASED_APP_ERROR_CODE = 1001
        const val PURCHASED_APP_ERROR_DOMAIN = "com.i2hammad.admanagekit"
        const val PURCHASED_APP_ERROR_MESSAGE =
            "Ads are not shown because the app has been purchased."

        @Volatile
        private var instance: AdManager? = null

        fun getInstance(): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager().also { instance = it }
            }
        }
    }

    fun initializeFirebase(context: Context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    private fun showLoadingDialog(
        activity: Activity,
        callback: AdManagerCallback,
        isReload: Boolean
    ) {
        if (isReady()) {
            showAd(activity, callback, isReload)
        } else {
            callback.onNextAction()
        }
    }

    /**
     * Loads an interstitial ad with a specified timeout, to be used on the splash screen.
     *
     * @param context The context in which the ad is being loaded.
     * @param adUnitId The ad unit ID for the interstitial ad.
     * @param timeoutMillis The timeout in milliseconds to wait for the ad to load.
     * @param callback The callback to handle actions after the ad loading is complete.
     */
    fun loadInterstitialAdForSplash(
        context: Context,
        adUnitId: String,
        timeoutMillis: Long,
        callback: AdManagerCallback
    ) {
        if (AppPurchase.getInstance().isPurchased()) {
            // User has purchased, no ads should be shown
            callback.onNextAction()
            return
        }

        this.adUnitId = adUnitId
        initializeFirebase(context)
        val adRequest = AdRequest.Builder().build()

        isAdLoading = true

        // Load the interstitial ad
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                isAdLoading = false
                Log.d("AdManager", "Interstitial ad loaded for splash")

                // Call the callback since the ad is loaded
                callback.onNextAction()
                callback.onAdLoaded()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(
                    "AdManager",
                    "Failed to load interstitial ad for splash: ${loadAdError.message}"
                )
                isAdLoading = false
                mInterstitialAd = null

                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)

                // Call the callback on failure
                callback.onNextAction()
                callback.onFailedToLoad(loadAdError)
            }
        })

        // Set a timeout for loading the ad
        Handler().postDelayed({
            if (isAdLoading) {
                Log.d("AdManager", "Ad loading timed out for splash")
                isAdLoading = false

                // Ensure the callback is called if the ad loading is taking too long
                callback.onNextAction()
            }
        }, timeoutMillis)
    }

    fun loadInterstitialAd(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId
        initializeFirebase(context)
        val adRequest = AdRequest.Builder().build()

        isAdLoading = true
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                isAdLoading = false
                Log.d("AdManager", "Interstitial ad loaded")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("AdManager", "Failed to load interstitial ad: ${loadAdError.message}")
                isAdLoading = false
                mInterstitialAd = null

                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)
            }
        })
    }

    fun loadInterstitialAd(
        context: Context,
        adUnitId: String,
        interstitialAdLoadCallback: InterstitialAdLoadCallback
    ) {
        if (AppPurchase.getInstance().isPurchased()) {
            // User has purchased, no ads should be shown
            interstitialAdLoadCallback.onAdFailedToLoad(
                LoadAdError(
                    PURCHASED_APP_ERROR_CODE,
                    PURCHASED_APP_ERROR_MESSAGE,
                    PURCHASED_APP_ERROR_DOMAIN,
                    null, // No underlying AdError cause
                    null  // No additional ResponseInfo
                )
            )
            return
        }
        this.adUnitId = adUnitId
        initializeFirebase(context)
        val adRequest = AdRequest.Builder().build()

        isAdLoading = true
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                isAdLoading = false
                Log.d("AdManager", "Interstitial ad loaded")
                interstitialAdLoadCallback.onAdLoaded(mInterstitialAd!!)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("AdManager", "Failed to load interstitial ad: ${loadAdError.message}")
                isAdLoading = false
                mInterstitialAd = null

                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)
                interstitialAdLoadCallback.onAdFailedToLoad(loadAdError)
            }
        })
    }

    /**
     * Shows an interstitial ad immediately, regardless of the time interval.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    fun forceShowInterstitial(activity: Activity, callback: AdManagerCallback) {
        showLoadingDialog(activity, callback, true)
    }

    /**
     * Shows an interstitial ad based on the specified time interval criteria.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    fun showInterstitialAdByTime(activity: Activity, callback: AdManagerCallback) {
        if (canShowAd()) {
            showLoadingDialog(activity, callback, true)
        } else {
            callback.onNextAction()
        }
    }

    /**
     * Shows an interstitial ad based on the specified number of times it has been displayed.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param maxDisplayCount The maximum number of times the ad can be displayed.
     */
    fun showInterstitialAdByCount(
        activity: Activity,
        callback: AdManagerCallback,
        maxDisplayCount: Int
    ) {
        if (adDisplayCount < maxDisplayCount) {
            showLoadingDialog(activity, callback, true)
        } else {
            callback.onNextAction()
        }
    }

    /**
     * Shows an interstitial ad immediately, regardless of the time interval.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param reloadAd A boolean indicating whether to reload the ad after it's shown.
     */
    fun forceShowInterstitial(activity: Activity, callback: AdManagerCallback, reloadAd: Boolean) {
        showLoadingDialog(activity, callback, reloadAd)
    }

    /**
     * Shows an interstitial ad based on the specified time interval criteria.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param reloadAd A boolean indicating whether to reload the ad after it's shown.
     */
    fun showInterstitialAdByTime(
        activity: Activity,
        callback: AdManagerCallback,
        reloadAd: Boolean
    ) {
        if (canShowAd()) {
            showLoadingDialog(activity, callback, reloadAd)
        } else {
            callback.onNextAction()
        }
    }

    /**
     * Shows an interstitial ad based on the specified number of times it has been displayed.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param maxDisplayCount The maximum number of times the ad can be displayed.
     * @param reloadAd A boolean indicating whether to reload the ad after it's shown.
     */
    fun showInterstitialAdByCount(
        activity: Activity,
        callback: AdManagerCallback,
        maxDisplayCount: Int,
        reloadAd: Boolean
    ) {
        if (adDisplayCount < maxDisplayCount) {
            showLoadingDialog(activity, callback, reloadAd)
        } else {
            callback.onNextAction()
        }
    }

    fun isReady(): Boolean {
        return mInterstitialAd != null && !AppPurchase.getInstance().isPurchased()
    }

    fun isDisplayingAd(): Boolean {
        return isDisplayingAd
    }

    fun setAdInterval(intervalMillis: Long) {
        this.adIntervalMillis = intervalMillis
    }

    fun getAdDisplayCount(): Int {
        return adDisplayCount
    }

    fun setAdDisplayCount(count: Int) {
        this.adDisplayCount = count
    }

    private fun canShowAd(): Boolean {
        val elapsed = System.currentTimeMillis() - lastAdShowTime
        Log.d("AdManager", "Time since last ad: $elapsed milliseconds")
        return elapsed > adIntervalMillis
    }

    private fun showAd(activity: Activity, callback: AdManagerCallback, reloadAd: Boolean) {
        if (isReady()) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isDisplayingAd = false
                    mInterstitialAd = null
                    callback.onNextAction()

                    // Log Firebase event for ad dismissed
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    }
                    firebaseAnalytics.logEvent("ad_dismissed", params)

                    if (reloadAd) {
                        loadInterstitialAd(activity, adUnitId ?: "")
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isDisplayingAd = false
                    mInterstitialAd = null
                    Log.e("AdManager", "Failed to show full-screen content: ${adError.message}")
                    callback.onNextAction()

                    // Log Firebase event for ad failed to show
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                        putString("ad_error_code", adError.code.toString())
                    }
                    firebaseAnalytics.logEvent("ad_failed_to_show", params)
                }

                override fun onAdShowedFullScreenContent() {
                    isDisplayingAd = true
                    lastAdShowTime = System.currentTimeMillis()
                    adDisplayCount++

                    // Log Firebase event for ad impression
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    }
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            callback.onNextAction()
        }
    }
}
