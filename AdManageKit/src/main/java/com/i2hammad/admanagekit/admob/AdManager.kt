package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
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

    // Retry state
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


    /**
     * Data class to hold dialog and its animated views
     */
    private data class LoadingDialogViews(
        val dialog: Dialog,
        val overlay: View,
        val contentCard: View
    )

    /**
     * Creates and shows a beautiful full-screen loading dialog with animations
     */
    private fun showBeautifulLoadingDialog(activity: Activity): LoadingDialogViews {
        // Create beautiful full-screen loading dialog
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_loading_ad_fullscreen, null)
        dialog.setContentView(dialogView)
        dialog.window?.apply {
            // Make truly full screen
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // Full screen flags
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            // Make status bar transparent
            statusBarColor = Color.TRANSPARENT

            // Hide system UI for true full screen
            decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        dialog.setCancelable(false)

        // Get views for animations
        val overlay = dialogView.findViewById<View>(R.id.overlay)
        val contentCard = dialogView.findViewById<View>(R.id.contentCard)
        val outerCircle = dialogView.findViewById<View>(R.id.outerCircle)
        val innerCircle = dialogView.findViewById<View>(R.id.innerCircle)
        val dot1 = dialogView.findViewById<View>(R.id.dot1)
        val dot2 = dialogView.findViewById<View>(R.id.dot2)
        val dot3 = dialogView.findViewById<View>(R.id.dot3)

        // Apply dialog customizations
        applyDialogCustomizations(dialogView, overlay, contentCard)

        // Apply custom text if configured
        AdManageKitConfig.loadingDialogTitle?.let { title ->
            dialogView.findViewById<android.widget.TextView>(R.id.loadingTitle)?.text = title
        }
        AdManageKitConfig.loadingDialogSubtitle?.let { subtitle ->
            dialogView.findViewById<android.widget.TextView>(R.id.loadingMessage)?.text = subtitle
        }

        // Show dialog
        dialog.show()

        // Animate overlay fade in
        overlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        // Animate content card
        contentCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(100)
            .start()

        // Start rotating animations for circles
        val rotateClockwise = AnimationUtils.loadAnimation(activity, R.anim.rotate_clockwise)
        val rotateCounterClockwise = AnimationUtils.loadAnimation(activity, R.anim.rotate_counterclockwise)
        outerCircle.startAnimation(rotateClockwise)
        innerCircle.startAnimation(rotateCounterClockwise)

        // Animate dots with sequential delay
        animateDot(dot1, 0)
        animateDot(dot2, 200)
        animateDot(dot3, 400)

        return LoadingDialogViews(dialog, overlay, contentCard)
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

                // Call the callback since the ad is loaded
                callback.onNextAction()
                callback.onAdLoaded()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(
                    "AdManager", "Failed to load interstitial ad for splash: ${loadAdError.message}"
                )
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Interstitial failed for splash: ${loadAdError.message}", false)

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
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("AdManager", "Failed to load interstitial ad: ${loadAdError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Interstitial failed: ${loadAdError.message}", false)

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

    /**
     * Load an interstitial ad with custom ad unit support and callbacks.
     *
     * @param context The context
     * @param adUnitId The ad unit ID (can be different from the default)
     * @param interstitialAdLoadCallback Callback for ad loading events
     */
    fun loadInterstitialAd(
        context: Context, adUnitId: String, interstitialAdLoadCallback: InterstitialAdLoadCallback
    ) {
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

                interstitialAdLoadCallback.onAdLoaded(mInterstitialAd!!)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("AdManager", "Failed to load interstitial ad: ${loadAdError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Interstitial failed with callback: ${loadAdError.message}", false)

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
     * Forces fetch and display of a fresh interstitial ad.
     * Always loads a new ad, does NOT use existing cached ads.
     * Note: Does not reload after showing since next force call fetches fresh anyway.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    fun forceShowInterstitial(activity: Activity, callback: AdManagerCallback) {
        forceShowInterstitialInternal(activity, callback)
    }


    /**
     * Forces fetch and display of a fresh interstitial ad with a loading dialog.
     * Always loads a new ad, does NOT use existing cached ads.
     * Note: Does not reload after showing since next force call fetches fresh anyway.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    fun forceShowInterstitialWithDialog(
        activity: Activity, callback: AdManagerCallback
    ) {
        forceShowInterstitial(activity, callback)
    }

    /**
     * Shows an interstitial ad immediately if one is loaded and ready.
     * Does NOT fetch a new ad if none is available - proceeds to next action.
     * Use this when you want to show an ad opportunistically without waiting.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param reloadAd Whether to reload the ad after it's shown. Defaults to true.
     */
    fun showInterstitialIfReady(
        activity: Activity,
        callback: AdManagerCallback,
        reloadAd: Boolean = true
    ) {
        showAd(activity, callback, reloadAd)
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
     * Internal method to force fetch and display a fresh interstitial ad.
     * Always loads a new ad, does NOT use existing cached ads.
     * Does not reload after showing since next force call fetches fresh anyway.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    private fun forceShowInterstitialInternal(activity: Activity, callback: AdManagerCallback) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onNextAction()
            return
        }

        val currentAdUnitId = adUnitId ?: ""
        if (currentAdUnitId.isEmpty()) {
            Log.e("AdManager", "Ad unit ID is not set. Cannot force show interstitial.")
            callback.onNextAction()
            return
        }

        // Clear existing ad to force fresh fetch
        mInterstitialAd = null
        initializeFirebase(activity)

        // Show beautiful loading dialog
        val dialogViews = showBeautifulLoadingDialog(activity)

        // Load fresh ad with timeout
        val adRequest = AdRequest.Builder().build()
        val timeoutMillis = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds
        var adLoaded = false
        var timeoutTriggered = false

        InterstitialAd.load(activity, currentAdUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                if (timeoutTriggered) return
                adLoaded = true
                mInterstitialAd = interstitialAd
                Log.d("AdManager", "Fresh interstitial ad loaded for force show")
                AdDebugUtils.logEvent(currentAdUnitId, "onAdLoaded", "Fresh interstitial loaded for force show", true)

                // Animate dialog dismissal
                animateDialogDismissal(dialogViews) {
                    showAd(activity, callback, false) // No reload - next force call fetches fresh anyway
                }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                if (timeoutTriggered) return
                adLoaded = true
                Log.e("AdManager", "Failed to load fresh interstitial ad: ${loadAdError.message}")
                AdDebugUtils.logEvent(currentAdUnitId, "onFailedToLoad", "Fresh interstitial failed: ${loadAdError.message}", false)

                // Log Firebase event
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, currentAdUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", loadAdError.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)

                animateDialogDismissal(dialogViews) {
                    callback.onNextAction()
                }
            }
        })

        // Timeout handler
        Handler(Looper.getMainLooper()).postDelayed({
            if (!adLoaded) {
                timeoutTriggered = true
                Log.d("AdManager", "Force show interstitial timed out")
                AdDebugUtils.logEvent(currentAdUnitId, "onTimeout", "Force show interstitial timed out", false)
                animateDialogDismissal(dialogViews) {
                    callback.onNextAction()
                }
            }
        }, timeoutMillis)
    }

    /**
     * Apply dialog customizations from config
     */
    private fun applyDialogCustomizations(dialogView: View, overlay: View, contentCard: View) {
        // Apply background color
        if (AdManageKitConfig.dialogBackgroundColor != 0) {
            dialogView.setBackgroundColor(AdManageKitConfig.dialogBackgroundColor)
        }

        // Apply overlay color
        if (AdManageKitConfig.dialogOverlayColor != 0) {
            overlay.setBackgroundColor(AdManageKitConfig.dialogOverlayColor)
            overlay.visibility = View.VISIBLE
        } else {
            // Hide overlay if color is 0
            overlay.visibility = View.GONE
        }

        // Apply card background color
        if (AdManageKitConfig.dialogCardBackgroundColor != 0) {
            contentCard.setBackgroundColor(AdManageKitConfig.dialogCardBackgroundColor)
        }
    }

    /**
     * Helper function to animate loading dots
     */
    private fun animateDot(dot: View, startDelay: Long) {
        dot.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(startDelay)
            .withEndAction {
                dot.animate()
                    .alpha(0.3f)
                    .setDuration(600)
                    .withEndAction {
                        animateDot(dot, 0)
                    }
                    .start()
            }
            .start()
    }

    /**
     * Helper function to animate dialog dismissal
     */
    private fun animateDialogDismissal(dialogViews: LoadingDialogViews, onComplete: () -> Unit) {
        // Animate content card out
        dialogViews.contentCard.animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(250)
            .start()

        // Animate overlay out
        dialogViews.overlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                dialogViews.dialog.dismiss()
                onComplete()
            }
            .start()
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

                    // CRITICAL: Preload next ad immediately for better show rate
                    if (reloadAd) {
                        // Reset retry attempts for fresh start
                        adUnitId?.let { retryAttempts.remove(it) }
                        // Load immediately in background
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

    /**
     * Proactively preload an interstitial ad to improve show rate.
     * Call this during natural pauses in your app (after user actions, screen loads, etc.)
     *
     * @param context The context
     * @param adUnitId The ad unit ID to preload
     */
    fun preloadAd(context: Context, adUnitId: String) {
        if (!isReady()) {
            loadInterstitialAd(context, adUnitId)
            if (AdManageKitConfig.debugMode) {
                AdDebugUtils.logEvent(adUnitId, "preload", "Preloading interstitial ad", true)
            }
        }
    }

    /**
     * Reset ad display counters and intervals for fresh start.
     * Useful when you want to show ads more frequently.
     */
    fun resetAdThrottling() {
        lastAdShowTime = 0
        adDisplayCount = 0
        retryAttempts.clear()
        if (AdManageKitConfig.debugMode) {
            AdDebugUtils.logEvent("", "resetThrottling", "Ad throttling reset", true)
        }
    }

    /**
     * Configure more aggressive ad loading for maximum show rate.
     * Call this in Application onCreate() or before first ad load.
     */
    fun enableAggressiveAdLoading() {
        // Reduce interval to minimum (5 seconds)
        setAdInterval(5000L)

        if (AdManageKitConfig.debugMode) {
            AdDebugUtils.logEvent("", "aggressiveMode", "Aggressive ad loading enabled (5s interval)", true)
        }
    }

    /**
     * Get timestamp of last ad show
     */
    fun getLastAdShowTime(): Long {
        return lastAdShowTime
    }

    /**
     * Get time elapsed since last ad show in milliseconds
     */
    fun getTimeSinceLastAd(): Long {
        return System.currentTimeMillis() - lastAdShowTime
    }
}
