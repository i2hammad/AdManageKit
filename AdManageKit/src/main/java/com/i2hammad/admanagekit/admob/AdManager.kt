package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.utils.AdRetryManager

/**
 * AdManager is a singleton class responsible for managing interstitial ads
 * using Google AdMob. It provides functionality to load and show ads, manage
 * display intervals, and handle ad-related callbacks.
 */
class AdManager() {

    private var mInterstitialAd: InterstitialAd? = null
    private var adUnitId: String? = null
    private var isAdLoading = false
    private var isDisplayingAd = false
    private var lastAdShowTime: Long = 0
    private var adIntervalMillis: Long = AdManageKitConfig.defaultInterstitialInterval.inWholeMilliseconds
    private var adDisplayCount = 0 // Track the number of times ads have been displayed
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    
    // Retry and circuit breaker state
    private var failureCount = 0
    private var lastFailureTime = 0L
    private var isCircuitBreakerOpen = false
    private val retryAttempts = mutableMapOf<String, Int>()

    companion object {
        const val PURCHASED_APP_ERROR_CODE = 1001
        const val PURCHASED_APP_ERROR_DOMAIN = "com.i2hammad.admanagekit"
        const val PURCHASED_APP_ERROR_MESSAGE =
            "Ads are not shown because the app has been purchased."

        @Volatile
        private var instance: AdManager? = null

        @JvmStatic
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
        activity: Activity, callback: AdManagerCallback, isReload: Boolean
    ) {
        // Check if the user has already purchased, skip the dialog if true
        var purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            // move to the next action
            callback.onNextAction()
            return
        }

        // Create a Material AlertDialog
        val dialog = MaterialAlertDialogBuilder(activity).setTitle("Please Wait")
            .setMessage("The ad will be shown shortly...")
            .setCancelable(false) // Prevent dismissing the dialog by tapping outside
            .create()

        // Show the dialog
        dialog.show()

        // Use a Handler to add a delay before displaying the ad
        Handler(Looper.getMainLooper()).postDelayed({
            // Dismiss the dialog and show the ad
            dialog.dismiss()
            if (isReady()) {
                showAd(activity, callback, isReload)
            } else {
                callback.onNextAction()
            }
        }, 500)
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
        context: Context, adUnitId: String, timeoutMillis: Long = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds, callback: AdManagerCallback
    ) {
        var purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased())
        {
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
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Interstitial ad loaded for splash", true)
                
                handleAdSuccess()

                // Call the callback since the ad is loaded
                callback.onNextAction()
                callback.onAdLoaded()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(
                    "AdManager", "Failed to load interstitial ad for splash: ${loadAdError.message}"
                )
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Interstitial failed for splash: ${loadAdError.message}", false)
                
                handleAdFailure()
                
                // Attempt automatic retry if enabled
                if (AdManageKitConfig.autoRetryFailedAds && shouldAttemptRetry(adUnitId)) {
                    val currentAttempt = retryAttempts[adUnitId] ?: 0
                    retryAttempts[adUnitId] = currentAttempt + 1
                    
                    AdRetryManager.getInstance().scheduleRetry(
                        adUnitId = adUnitId,
                        attempt = currentAttempt,
                        maxAttempts = AdManageKitConfig.maxRetryAttempts
                    ) {
                        // Retry loading the ad
                        loadInterstitialAdForSplash(context, adUnitId, timeoutMillis, callback)
                    }
                } else {
                    isAdLoading = false
                    mInterstitialAd = null
                }

                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", loadAdError.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)

                // Call the callback on failure
                callback.onNextAction()
                callback.onFailedToLoad(loadAdError)
            }
        })


        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdLoading) {
                Log.d("AdManager", "Ad loading timed out for splash")
                AdDebugUtils.logEvent(adUnitId, "onTimeout", "Interstitial ad loading timed out for splash", false)
                isAdLoading = false

                // Ensure the callback is called if the ad loading is taking too long
                callback.onNextAction()
            }
        }, timeoutMillis)
    }

    fun loadInterstitialAd(context: Context, adUnitId: String) {
        if (!shouldAttemptLoad()) {
            AdDebugUtils.logEvent(adUnitId, "circuitBreakerBlocked", "Ad loading blocked by circuit breaker", false)
            return
        }
        
        if (AdManageKitConfig.testMode) {
            AdDebugUtils.logEvent(adUnitId, "testMode", "Using test mode for interstitial ads", true)
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
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Interstitial ad loaded successfully", true)
                
                handleAdSuccess()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("AdManager", "Failed to load interstitial ad: ${loadAdError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Interstitial failed: ${loadAdError.message}", false)
                
                handleAdFailure()
                
                // Attempt automatic retry if enabled
                if (AdManageKitConfig.autoRetryFailedAds && shouldAttemptRetry(adUnitId)) {
                    val currentAttempt = retryAttempts[adUnitId] ?: 0
                    retryAttempts[adUnitId] = currentAttempt + 1
                    
                    AdRetryManager.getInstance().scheduleRetry(
                        adUnitId = adUnitId,
                        attempt = currentAttempt,
                        maxAttempts = AdManageKitConfig.maxRetryAttempts
                    ) {
                        // Retry loading the ad
                        loadInterstitialAd(context, adUnitId)
                    }
                } else {
                    isAdLoading = false
                    mInterstitialAd = null
                }

                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", loadAdError.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)
            }
        })
    }

    fun loadInterstitialAd(
        context: Context, adUnitId: String, interstitialAdLoadCallback: InterstitialAdLoadCallback
    ) {
        if (!shouldAttemptLoad()) {
            AdDebugUtils.logEvent(adUnitId, "circuitBreakerBlocked", "Ad loading blocked by circuit breaker", false)
            return
        }
        
        if (AdManageKitConfig.testMode) {
            AdDebugUtils.logEvent(adUnitId, "testMode", "Using test mode for interstitial ads with callback", true)
        }
        var purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()){
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
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Interstitial ad loaded with callback", true)
                
                handleAdSuccess()
                interstitialAdLoadCallback.onAdLoaded(mInterstitialAd!!)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("AdManager", "Failed to load interstitial ad: ${loadAdError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Interstitial failed with callback: ${loadAdError.message}", false)
                
                handleAdFailure()
                
                // Attempt automatic retry if enabled
                if (AdManageKitConfig.autoRetryFailedAds && shouldAttemptRetry(adUnitId)) {
                    val currentAttempt = retryAttempts[adUnitId] ?: 0
                    retryAttempts[adUnitId] = currentAttempt + 1
                    
                    AdRetryManager.getInstance().scheduleRetry(
                        adUnitId = adUnitId,
                        attempt = currentAttempt,
                        maxAttempts = AdManageKitConfig.maxRetryAttempts
                    ) {
                        // Retry loading the ad with callback
                        loadInterstitialAd(context, adUnitId, interstitialAdLoadCallback)
                    }
                } else {
                    isAdLoading = false
                    mInterstitialAd = null
                }

                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", loadAdError.message)
                    }
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
        showAd(activity, callback, true)
    }


    /**
     * Shows an interstitial ad immediately, regardless of the time interval.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    fun forceShowInterstitialWithDialog(
        activity: Activity, callback: AdManagerCallback, isReload: Boolean = true
    ) {
        showLoadingDialog(activity, callback, isReload)
    }


    /**
     * Shows an interstitial ad based on the specified time interval criteria.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    fun showInterstitialAdByTime(activity: Activity, callback: AdManagerCallback) {
        if (canShowAd()) {
            showAd(activity, callback, true)
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
        activity: Activity, callback: AdManagerCallback, maxDisplayCount: Int
    ) {
        if (adDisplayCount < maxDisplayCount) {
            showAd(activity, callback, true)
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
        showAd(activity, callback, reloadAd)
    }

    /**
     * Shows an interstitial ad based on the specified time interval criteria.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param reloadAd A boolean indicating whether to reload the ad after it's shown.
     */
    fun showInterstitialAdByTime(
        activity: Activity, callback: AdManagerCallback, reloadAd: Boolean
    ) {
        if (canShowAd()) {
            showAd(activity, callback, reloadAd)
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
        activity: Activity, callback: AdManagerCallback, maxDisplayCount: Int, reloadAd: Boolean
    ) {
        if (adDisplayCount < maxDisplayCount) {
            showAd(activity, callback, reloadAd)
        } else {
            callback.onNextAction()
        }
    }

    fun isReady(): Boolean {
        var purchaseProvider = BillingConfig.getPurchaseProvider()
        return mInterstitialAd != null && !purchaseProvider.isPurchased()
    }

    fun isDisplayingAd(): Boolean {
        return isDisplayingAd
    }

    fun setAdInterval(intervalMillis: Long) {
        this.adIntervalMillis = intervalMillis
        if (AdManageKitConfig.debugMode) {
            AdDebugUtils.logEvent("", "setAdInterval", "Ad interval set to ${intervalMillis}ms", true)
        }
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
        
        // Use adaptive intervals if enabled
        val effectiveInterval = if (AdManageKitConfig.enableAdaptiveIntervals) {
            // Increase interval if recent failures, decrease if recent successes
            // This is a simplified adaptive logic
            adIntervalMillis
        } else {
            adIntervalMillis
        }
        
        return elapsed > effectiveInterval
    }
    
    /**
     * Handle ad loading failure for circuit breaker logic
     */
    private fun handleAdFailure() {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        
        if (failureCount >= AdManageKitConfig.circuitBreakerThreshold) {
            isCircuitBreakerOpen = true
            AdDebugUtils.logEvent("", "circuitBreakerOpen", "Circuit breaker opened after $failureCount failures", false)
        }
    }
    
    /**
     * Handle ad loading success for circuit breaker logic
     */
    private fun handleAdSuccess() {
        if (failureCount > 0) {
            AdDebugUtils.logEvent("", "circuitBreakerReset", "Circuit breaker reset after success", true)
        }
        failureCount = 0
        isCircuitBreakerOpen = false
        
        // Reset retry attempts for the successful ad unit
        adUnitId?.let { unitId ->
            if (retryAttempts.containsKey(unitId)) {
                AdDebugUtils.logEvent(unitId, "retryReset", "Retry attempts reset after successful load", true)
                retryAttempts.remove(unitId)
                AdRetryManager.getInstance().cancelRetry(unitId)
            }
        }
    }
    
    /**
     * Check if circuit breaker should allow ad loading
     */
    private fun shouldAttemptLoad(): Boolean {
        if (!isCircuitBreakerOpen) return true
        
        val timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime
        if (timeSinceLastFailure > AdManageKitConfig.circuitBreakerResetTimeout.inWholeMilliseconds) {
            isCircuitBreakerOpen = false
            failureCount = 0
            AdDebugUtils.logEvent("", "circuitBreakerReset", "Circuit breaker reset after timeout", true)
            return true
        }
        
        return false
    }

    private fun showAd(activity: Activity, callback: AdManagerCallback, reloadAd: Boolean) {
        if (isReady()) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isDisplayingAd = false
                    mInterstitialAd = null
                    AdDebugUtils.logEvent(adUnitId ?: "", "onAdDismissed", "Interstitial ad dismissed", true)
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
                    AdDebugUtils.logEvent(adUnitId ?: "", "onFailedToShow", "Interstitial failed to show: ${adError.message}", false)
                    callback.onNextAction()

                    // Log Firebase event for ad failed to show
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                        putString("ad_error_code", adError.code.toString())
                        if (AdManageKitConfig.enablePerformanceMetrics) {
                            putString("error_message", adError.message)
                        }
                    }
                    firebaseAnalytics.logEvent("ad_failed_to_show", params)
                }

                override fun onAdShowedFullScreenContent() {
                    isDisplayingAd = true
                    lastAdShowTime = System.currentTimeMillis()
                    adDisplayCount++
                    AdDebugUtils.logEvent(adUnitId ?: "", "onAdImpression", "Interstitial ad shown", true)

                    // Log Firebase event for ad impression
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    }
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                }
            }
            mInterstitialAd?.onPaidEventListener =
                OnPaidEventListener { adValue -> // Convert the value from micros to the standard currency unit
                    val adValueInStandardUnits = adValue.valueMicros / 1000000.0

                    // Log Firebase event for paid event
                    val params = Bundle()
                    params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    params.putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                    params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                    firebaseAnalytics!!.logEvent("ad_paid_event", params)
                }
            mInterstitialAd?.show(activity)
        } else {
            callback.onNextAction()
        }
    }
    
    /**
     * Check if retry should be attempted for the given ad unit
     */
    private fun shouldAttemptRetry(adUnitId: String): Boolean {
        val currentAttempts = retryAttempts[adUnitId] ?: 0
        return currentAttempts < AdManageKitConfig.maxRetryAttempts && 
               !AdRetryManager.getInstance().hasActiveRetry(adUnitId)
    }
}
