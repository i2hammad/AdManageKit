package com.i2hammad.admanagekit.admob;

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.core.content.PackageManagerCompat.LOG_TAG
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.admob.AppOpenManager.Companion.isShowingAd
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.config.AdManageKitConfig.maxRetryAttempts
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.utils.AdRetryManager
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.math.pow

//import com.i2hammad.admanagekit.billing.AppPurchase

/**
 * Prefetches App Open Ads.
 */
class AppOpenManager(private val myApplication: Application, private var adUnitId: String) :
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var currentActivity: Activity? = null

    @Volatile
    private var appOpenAd: AppOpenAd? = null

    private val excludedActivities: MutableSet<Class<*>> = HashSet()
    private val excludedActivityNames: MutableSet<String> = HashSet() // Cache for performance

    // Fragment-based and tag-based exclusions for single-activity apps
    private val excludedFragmentTags: MutableSet<String> = HashSet()
    private val excludedScreenTags: MutableSet<String> = HashSet()
    @Volatile
    private var currentScreenTag: String? = null
    private var fragmentTagProvider: (() -> String?)? = null

    private val skipNextAd = AtomicBoolean(false)
    private val isLoading = AtomicBoolean(false)  // Prevents concurrent ad requests
    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(myApplication)

    // Preloader state
    private val preloaderActive = AtomicBoolean(false)

    /**
     * Enable preloader mode for more efficient ad loading.
     * When enabled, uses AppOpenAdPreloader instead of manual loading.
     * The SDK handles caching and background loading automatically.
     */
    var usePreloader: Boolean = true

    // Reusable handler for timeouts
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val pendingTimeouts = mutableSetOf<Runnable>()

    // Performance metrics
    @Volatile
    private var lastLoadStartTime = 0L
    private val loadTimes = mutableListOf<Long>()

    // Enhanced configuration
    private var maxRetryAttempts = 3
    private var baseRetryDelayMs = 1000L
    private var maxRetryDelayMs = 30000L
    private var retryMultiplier = 2.0

    init {
        myApplication.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Clear cache on startup
        excludedActivityNames.clear()
    }

    companion object {
        private const val LOG_TAG = "AppOpenManager"

        // Thread-safe static state
        private val isShowingAd = AtomicBoolean(false)
        private val isShownAd = AtomicBoolean(false)

        @JvmStatic
        fun isShowingAd(): Boolean = isShowingAd.get()

        @JvmStatic
        fun isShownAd(): Boolean = isShownAd.get()

        // Maintain backward compatibility with old static API
        @JvmStatic
        @Deprecated("Use instance methods instead", ReplaceWith("instance.isShowingAd.get()"))
        var isShowingAdLegacy: Boolean
            get() = isShowingAd.get()
            set(value) {
                isShowingAd.set(value)
            }

        @JvmStatic
        @Deprecated("Use instance methods instead", ReplaceWith("instance.isShownAd.get()"))
        var isShownAdLegacy: Boolean
            get() = isShownAd.get()
            set(value) {
                isShownAd.set(value)
            }
    }

    /**
     * Data class to hold dialog view references
     */
    private data class WelcomeBackDialogViews(
        val dialog: Dialog,
        val overlay: View,
        val contentCard: View
    )

    /**
     * Currently showing dialog (kept to dismiss after ad)
     */
    private var currentWelcomeDialog: WelcomeBackDialogViews? = null

    /**
     * Flag to track if we're currently fetching an ad with dialog displayed
     */
    private var isFetchingWithDialog = false

    /**
     * Get themed context for dialog inflation.
     * Uses activity's theme if it has Material attributes (preserves app colors),
     * otherwise wraps with Material3 as fallback for non-Material themes.
     */
    private fun getThemedContextForDialog(activity: Activity): android.content.Context {
        return try {
            // Check if activity's theme has colorPrimary (Material/AppCompat attribute)
            val typedValue = android.util.TypedValue()
            val hasMaterialTheme = activity.theme.resolveAttribute(
                androidx.appcompat.R.attr.colorPrimary,
                typedValue,
                true
            )

            if (hasMaterialTheme) {
                // Activity has Material/AppCompat theme - use it directly to preserve app colors
                activity
            } else {
                // Non-Material theme - wrap with Material3 fallback
                Log.d(LOG_TAG, "Activity doesn't have Material theme, using fallback")
                ContextThemeWrapper(activity, com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
            }
        } catch (e: Exception) {
            // On any error, use Material3 fallback
            Log.w(LOG_TAG, "Error checking theme, using Material3 fallback: ${e.message}")
            ContextThemeWrapper(activity, com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar)
        }
    }

    /**
     * Show beautiful welcome back dialog
     */
    private fun showWelcomeBackDialog(activity: Activity): WelcomeBackDialogViews {
        // Dismiss any existing dialog first
        currentWelcomeDialog?.let { existing ->
            try {
                if (existing.dialog.isShowing) {
                    existing.dialog.dismiss()
                }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Error dismissing existing welcome dialog: ${e.message}")
            }
        }
        currentWelcomeDialog = null

        // Get themed context - use activity's theme if it has Material attributes,
        // otherwise wrap with Material3 as fallback
        val themedContext = getThemedContextForDialog(activity)

        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val dialogView = LayoutInflater.from(themedContext).inflate(R.layout.dialog_welcome_back_fullscreen, null)
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)

        dialog.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            // Use modern WindowInsetsController API (replaces deprecated systemUiVisibility)
            WindowCompat.setDecorFitsSystemWindows(this, false)
            WindowCompat.getInsetsController(this, decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        val overlay = dialogView.findViewById<View>(R.id.overlay)
        val contentCard = dialogView.findViewById<View>(R.id.contentCard)

        // Apply dialog customizations
        applyDialogCustomizations(dialogView, overlay, contentCard)

        // Set app icon if configured
        if (AdManageKitConfig.welcomeDialogAppIcon != 0) {
            val appIconImage = dialogView.findViewById<android.widget.ImageView>(R.id.appIconImage)
            appIconImage?.apply {
                setImageResource(AdManageKitConfig.welcomeDialogAppIcon)
                // Remove tint when using custom app icon
                imageTintList = null
            }
        }

        // Apply custom text if configured
        AdManageKitConfig.welcomeDialogTitle?.let { title ->
            dialogView.findViewById<android.widget.TextView>(R.id.welcomeTitle)?.text = title
        }
        AdManageKitConfig.welcomeDialogSubtitle?.let { subtitle ->
            dialogView.findViewById<android.widget.TextView>(R.id.welcomeSubtitle)?.text = subtitle
        }
        AdManageKitConfig.welcomeDialogFooter?.let { footer ->
            dialogView.findViewById<android.widget.TextView>(R.id.welcomeFooter)?.text = footer
        }

        // Animate entry with subtle fade
        contentCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        dialog.show()
        return WelcomeBackDialogViews(dialog, overlay, contentCard)
    }

    /**
     * Dismiss dialog with subtle animation
     */
    private fun animateDialogDismissal(dialogViews: WelcomeBackDialogViews, onComplete: () -> Unit) {
        dialogViews.contentCard.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(200)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                dismissDialogSafely(dialogViews.dialog)
                onComplete()
            }
            .start()
    }

    /**
     * Safely dismiss dialog handling window detachment
     */
    private fun dismissDialogSafely(dialog: Dialog) {
        try {
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        } catch (e: IllegalArgumentException) {
            // View not attached to window manager - activity was destroyed
            Log.w(LOG_TAG, "Dialog dismiss failed - window not attached: ${e.message}")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error dismissing dialog: ${e.message}")
        }
    }

    /**
     * Dismiss welcome dialog with configured delay
     * Used when app open ad is shown - dismisses dialog while ad is displayed
     */
    private fun dismissWelcomeDialogWithDelay(dialogViews: WelcomeBackDialogViews?) {
        dialogViews ?: return

        val delayMillis = AdManageKitConfig.welcomeDialogDismissDelay.inWholeMilliseconds
        Log.d(LOG_TAG, "Scheduling welcome dialog dismissal with ${delayMillis}ms delay (while ad is showing)")

        if (delayMillis <= 0) {
            // No delay - dismiss immediately
            dismissDialogSafely(dialogViews.dialog)
            currentWelcomeDialog = null
        } else {
            // Dismiss with delay - dialog visible briefly, then fades while ad shows
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    if (dialogViews.dialog.isShowing) {
                        animateDialogDismissal(dialogViews) {
                            Log.d(LOG_TAG, "Welcome dialog dismissed (ad still showing)")
                            currentWelcomeDialog = null
                        }
                    } else {
                        currentWelcomeDialog = null
                    }
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Error dismissing welcome dialog with delay: ${e.message}")
                    currentWelcomeDialog = null
                }
            }, delayMillis)
        }
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
     * Shows the ad if one isn't already showing.
     */
    fun showAdIfAvailable() {
        // Check if user has purchased to remove ads
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            Log.d(LOG_TAG, "User has purchased, skipping app open ad.")
            return
        }

        val currentActivity = getCurrentActivity()
        if (currentActivity != null && isActivityExcluded(currentActivity::class.java)) {
            Log.d(LOG_TAG, "Ad display is skipped for this activity.")
            // Only fetch in background if appOpenFetchFreshAd is false
            if (!AdManageKitConfig.appOpenFetchFreshAd) {
                fetchAd()
            }
            return
        }

        // If ad is available, show it
        // Use isAdOrDialogShowing() to also prevent showing on top of interstitial loading dialog
        if (!isShowingAd.get() && isAdAvailable() && !skipNextAd.get() && !AdManager.getInstance().isAdOrDialogShowing()) {
            if (currentActivity == null) {
                Log.e(LOG_TAG, "Cannot show ad: currentActivity is null (WeakReference cleared)")
                return
            }

            Log.d(LOG_TAG, "Showing ad on activity: ${currentActivity.javaClass.simpleName}")

            // Use preloader if active, otherwise use manually loaded ad
            if (usePreloader && preloaderActive.get() && AppOpenAdPreloader.isAdAvailable(adUnitId)) {
                val preloadedAd = pollPreloadedAd()
                if (preloadedAd != null) {
                    val eventCallback = createAppOpenAdEventCallback("preloaded", null)
                    preloadedAd.adEventCallback = eventCallback
                    // Set isShowingAd BEFORE calling show() to prevent race conditions
                    isShowingAd.set(true)
                    try {
                        preloadedAd.show(currentActivity)
                    } catch (e: Exception) {
                        isShowingAd.set(false)
                        Log.e(LOG_TAG, "Error showing preloaded ad: ${e.message}")
                    }
                }
            } else {
                val eventCallback = createAppOpenAdEventCallback("regular", null)
                appOpenAd?.apply {
                    adEventCallback = eventCallback
                    // Set isShowingAd BEFORE calling show() to prevent race conditions
                    isShowingAd.set(true)
                    try {
                        show(currentActivity)
                    } catch (e: Exception) {
                        isShowingAd.set(false)
                        Log.e(LOG_TAG, "Error showing ad: ${e.message}")
                    }
                }
            }
        } else if (!isAdAvailable() && currentActivity != null) {
            // No cached ad available - always show welcome dialog while fetching
            Log.d(LOG_TAG, "No ad available, showing welcome dialog.")
            showAdWithWelcomeDialog(currentActivity, null)
        } else {
            Log.d(LOG_TAG, "Cannot show ad.")
            // Only fetch in background if appOpenFetchFreshAd is false
            if (!AdManageKitConfig.appOpenFetchFreshAd) {
                fetchAd()
            }
        }

        skipNextAd.set(false)
    }

    /**
     * Show ad with welcome back dialog
     */
    private fun showAdWithWelcomeDialog(activity: Activity, callback: AdManagerCallback?) {
        // Guard: Prevent if ad is already showing
        if (isShowingAd.get()) {
            Log.d(LOG_TAG, "Skipping showAdWithWelcomeDialog: ad already showing")
            callback?.onNextAction()
            return
        }

        // Guard: Prevent if interstitial ad or loading dialog is showing
        if (AdManager.getInstance().isAdOrDialogShowing()) {
            Log.d(LOG_TAG, "Skipping showAdWithWelcomeDialog: interstitial ad or loading dialog is showing")
            callback?.onNextAction()
            return
        }

        // Guard: Check actual dialog state to prevent duplicates
        val dialogAlreadyShowing = try {
            currentWelcomeDialog?.dialog?.isShowing == true
        } catch (e: Exception) {
            false
        }

        if (dialogAlreadyShowing || isFetchingWithDialog) {
            Log.d(LOG_TAG, "Skipping showAdWithWelcomeDialog: dialog already showing or fetching")
            callback?.onNextAction()
            return
        }

        if (activity.isFinishing) {
            callback?.onNextAction()
            return
        }

        // Set fetching flag before showing dialog
        isFetchingWithDialog = true

        val dialogViews = showWelcomeBackDialog(activity)
        currentWelcomeDialog = dialogViews
        val timeoutMillis = AdManageKitConfig.appOpenAdTimeout.inWholeMilliseconds

        var hasTimedOut = false
        val timeoutRunnable = scheduleTimeout(timeoutMillis) {
            hasTimedOut = true
            isFetchingWithDialog = false
            Handler(Looper.getMainLooper()).post {
                animateDialogDismissal(dialogViews) {
                    Log.e(LOG_TAG, "App open ad load timed out")
                    currentWelcomeDialog = null
                    callback?.onNextAction()
                }
            }
        }

        val request = getAdRequest(adUnitId)
        val mainHandler = Handler(Looper.getMainLooper())
        AppOpenAd.load(
            request,
            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    if (!hasTimedOut) {
                        cancelTimeout(timeoutRunnable)
                        isFetchingWithDialog = false
                        appOpenAd = ad

                        // Keep dialog showing - ad will be displayed on top
                        // Dialog will be dismissed after ad is closed with delay
                        Log.d(LOG_TAG, "Ad loaded, showing on top of welcome dialog")

                        // Switch to main thread for UI operations
                        mainHandler.post {
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                showLoadedAd(activity, callback)
                            } else {
                                Log.d(LOG_TAG, "Activity not in valid state after ad load")
                                dismissWelcomeDialogWithDelay(dialogViews)
                                currentWelcomeDialog = null
                                callback?.onNextAction()
                            }
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    if (!hasTimedOut) {
                        cancelTimeout(timeoutRunnable)
                        isFetchingWithDialog = false
                        logFailedToLoadEvent(error)
                        // Switch to main thread for UI operations
                        mainHandler.post {
                            animateDialogDismissal(dialogViews) {
                                currentWelcomeDialog = null
                                callback?.onNextAction()
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Fetch and show fresh ad without dialog
     */
    private fun fetchAndShowFresh(activity: Activity, callback: AdManagerCallback?) {
        if (activity.isFinishing) {
            callback?.onNextAction()
            return
        }

        val request = getAdRequest(adUnitId)
        val mainHandler = Handler(Looper.getMainLooper())
        AppOpenAd.load(
            request,
            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    // Switch to main thread for UI operations
                    mainHandler.post {
                        if (!activity.isFinishing) {
                            showLoadedAd(activity, callback)
                        } else {
                            callback?.onNextAction()
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    logFailedToLoadEvent(error)
                    // Switch to main thread for callback
                    mainHandler.post {
                        callback?.onNextAction()
                    }
                }
            }
        )
    }

    /**
     * Show already loaded ad
     */
    private fun showLoadedAd(activity: Activity, callback: AdManagerCallback?) {
        // Check if we can show the ad
        if (isShowingAd.get() || activity.isFinishing) {
            Log.d(LOG_TAG, "Cannot show ad: isShowingAd=${isShowingAd.get()}, isFinishing=${activity.isFinishing}")
            callback?.onNextAction()
            return
        }

        // Additional check: ensure activity is still valid and resumed
        if (activity.isDestroyed) {
            Log.d(LOG_TAG, "Cannot show ad: activity is destroyed")
            callback?.onNextAction()
            return
        }

        val eventCallback = createAppOpenAdEventCallback(
            if (callback != null) "forced" else "regular",
            callback
        )

        appOpenAd?.apply {
            adEventCallback = eventCallback
            // Set isShowingAd BEFORE calling show() to prevent race conditions
            isShowingAd.set(true)
            try {
                show(activity)
            } catch (e: Exception) {
                isShowingAd.set(false)
                Log.e(LOG_TAG, "Error showing ad: ${e.message}")
                callback?.onNextAction()
            }
        } ?: run {
            // No ad available
            Log.d(LOG_TAG, "Cannot show ad: appOpenAd is null")
            callback?.onNextAction()
        }
    }

    /**
     * Force Show the ad on provided activity if one isn't already showing or already user purchased.
     */
    fun forceShowAdIfAvailable(activity: Activity, adManagerCallback: AdManagerCallback) {
        // Check if user has purchased to remove ads
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            Log.d(LOG_TAG, "User has purchased, skipping app open ad.")
            adManagerCallback.onNextAction()
            return
        }

        if (activity.isFinishing) {
            adManagerCallback.onNextAction()
            return
        }

        // If cached ad is available, use it
        if (!isShowingAd.get() && isAdAvailable()) {
            Log.e(LOG_TAG, "Will show ad (cached).")
            showLoadedAd(activity, adManagerCallback)
        } else if (!isAdAvailable()) {
            // No cached ad - always show welcome dialog while fetching
            Log.d(LOG_TAG, "No cached ad, fetching with welcome dialog.")
            showAdWithWelcomeDialog(activity, adManagerCallback)
        } else {
            // Already showing or other condition
            adManagerCallback.onNextAction()
            Log.d(LOG_TAG, "Cannot show ad.")
            // Only fetch in background if appOpenFetchFreshAd is false
            if (!AdManageKitConfig.appOpenFetchFreshAd) {
                fetchAd()
            }
        }

        skipNextAd.set(false)
    }


    fun skipNextAd() {
        skipNextAd.set(true)
    }

    /**
     * Get current activity safely
     */
    private fun getCurrentActivity(): Activity? = currentActivity

    /**
     * Check if activity is excluded with performance optimization.
     * Also checks screen/fragment tags for single-activity apps.
     */
    private fun isActivityExcluded(activityClass: Class<*>): Boolean {
        val className = activityClass.name

        // Fast path: check activity cache first
        synchronized(excludedActivities) {
            if (excludedActivityNames.contains(className)) {
                return true
            }

            // Slow path: check actual set and update cache
            val isExcluded = excludedActivities.contains(activityClass)
            if (isExcluded) {
                excludedActivityNames.add(className) // Cache for future lookups
                return true
            }
        }

        // Check screen/fragment tag exclusions for single-activity apps
        if (isCurrentScreenExcluded()) {
            return true
        }

        return false
    }

    /**
     * Check if the current screen (fragment/destination) is excluded.
     * Used for single-activity apps with multiple fragments.
     */
    private fun isCurrentScreenExcluded(): Boolean {
        // Check manually set current screen tag
        val screenTag = currentScreenTag
        if (screenTag != null) {
            synchronized(excludedScreenTags) {
                if (excludedScreenTags.contains(screenTag)) {
                    Log.d(LOG_TAG, "Screen tag '$screenTag' is excluded from app open ads")
                    return true
                }
            }
        }

        // Check fragment tag provider if set
        val fragmentTag = fragmentTagProvider?.invoke()
        if (fragmentTag != null) {
            synchronized(excludedFragmentTags) {
                if (excludedFragmentTags.contains(fragmentTag)) {
                    Log.d(LOG_TAG, "Fragment tag '$fragmentTag' is excluded from app open ads")
                    return true
                }
            }
        }

        return false
    }

    /**
     * Create reusable app open ad event callback
     * Note: GMA Next-Gen SDK calls these callbacks on background threads,
     * so we must switch to main thread for UI operations and user callbacks.
     */
    private fun createAppOpenAdEventCallback(type: String, callback: AdManagerCallback?): AppOpenAdEventCallback {
        val mainHandler = Handler(Looper.getMainLooper())
        return object : AppOpenAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd.set(false)
                val logMessage = when (type) {
                    "forced" -> "App open ad dismissed (forced)"
                    else -> "App open ad dismissed"
                }
                AdDebugUtils.logEvent(adUnitId, "onAdDismissed", logMessage, true)

                // Switch to main thread for UI operations and callbacks
                mainHandler.post {
                    // Clear dialog reference if still set
                    currentWelcomeDialog = null

//                fetchAd()
                    callback?.onNextAction()
                }
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                val logMessage = when (type) {
                    "forced" -> "App open ad failed to show (forced): ${fullScreenContentError.message}"
                    else -> "App open ad failed to show: ${fullScreenContentError.message}"
                }
                AdDebugUtils.logEvent(adUnitId, "onFailedToShow", logMessage, false)
                logFailedToLoadEvent(
                    LoadAdError(
                        LoadAdError.ErrorCode.INTERNAL_ERROR,
                        fullScreenContentError.message,
                        null
                    )
                )

                // Switch to main thread for UI operations and callbacks
                mainHandler.post {
                    // Dismiss dialog immediately if ad fails to show
                    currentWelcomeDialog?.let { dialogViews ->
                        dismissDialogSafely(dialogViews.dialog)
                        currentWelcomeDialog = null
                    }

                    callback?.onNextAction()
                }
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd.set(true)
                isShownAd.set(true)

                val logMessage = when (type) {
                    "forced" -> "App open ad shown (forced)"
                    else -> "App open ad shown"
                }
                AdDebugUtils.logEvent(adUnitId, "onAdImpression", logMessage, true)

                // Switch to main thread for UI operations and callbacks
                mainHandler.post {
                    // Dismiss welcome dialog with delay NOW (while ad is showing)
                    dismissWelcomeDialogWithDelay(currentWelcomeDialog)

                    if (type == "forced") {
                        callback?.onAdLoaded()
                    }
                }

                logAdImpressionEvent()
            }

            override fun onAdImpression() {
                // Usually redundant if onAdShowedFullScreenContent covers impression logic,
                // but good for tracking if checking pure impressions
            }

            override fun onAdClicked() {
                AdDebugUtils.logEvent(adUnitId, "onAdClicked", "App open ad clicked", true)
            }

            override fun onAdPaid(value: AdValue) {
                super.onAdPaid(value)

                createPaidEventListener(value)
            }
        }
    }

    /**
     * Create reusable paid event listener
     */
    private fun createPaidEventListener(adValue: AdValue) {
            val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
            val revenueParams = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            }
            firebaseAnalytics.logEvent("ad_paid_event", revenueParams)
    }

    /**
     * Log Firebase analytics for failed ad loads
     */
    private fun logFailedToLoadEvent(adError: LoadAdError) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
            putString("ad_error_code", adError.code.toString())
            if (AdManageKitConfig.enablePerformanceMetrics) {
                putString("error_message", adError.message)
            }
        }
        firebaseAnalytics.logEvent("ad_failed_to_load", params)
    }

    /**
     * Log Firebase analytics for ad impressions
     */
    private fun logAdImpressionEvent() {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
    }

    /**
     * Clean up resources and prevent memory leaks
     */
    fun cleanup() {
        // Stop preloader if active
        if (preloaderActive.get()) {
            stopPreloading()
        }

        // Cancel all pending timeouts
        synchronized(pendingTimeouts) {
            pendingTimeouts.forEach { timeoutHandler.removeCallbacks(it) }
            pendingTimeouts.clear()
        }

        // Clear ad reference
        appOpenAd = null

        // Clear activity reference
        currentActivity = null

        // Clear caches
        excludedActivityNames.clear()
        loadTimes.clear()

        // Reset state
        isShowingAd.set(false)
        skipNextAd.set(false)
        isLoading.set(false)
        preloaderActive.set(false)

        // Unregister lifecycle callbacks
        try {
            myApplication.unregisterActivityLifecycleCallbacks(this)
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error during cleanup: ${e.message}")
        }
    }

    /**
     * Enhanced timeout handling with proper cleanup
     */
    private fun scheduleTimeout(timeoutMillis: Long, onTimeout: () -> Unit): Runnable {
        val timeoutRunnable = Runnable {
            onTimeout()
        }

        synchronized(pendingTimeouts) {
            pendingTimeouts.add(timeoutRunnable)
        }

        timeoutHandler.postDelayed(timeoutRunnable, timeoutMillis)
        return timeoutRunnable
    }

    /**
     * Cancel a scheduled timeout
     */
    private fun cancelTimeout(timeoutRunnable: Runnable) {
        synchronized(pendingTimeouts) {
            if (pendingTimeouts.remove(timeoutRunnable)) {
                timeoutHandler.removeCallbacks(timeoutRunnable)
            }
        }
    }

    /**
     * Request an ad.
     * If usePreloader is true, starts the preloader; otherwise uses traditional loading.
     */
    fun fetchAd() {
        // Don't fetch ads if user has purchased
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            Log.d(LOG_TAG, "User has purchased, skipping ad fetch.")
            return
        }

        if (usePreloader) {
            startPreloading()
        } else {
            fetchAdWithRetry(0)
        }
    }

    // =================== PRELOADER API ===================

    /**
     * Starts the AppOpenAdPreloader for this ad unit.
     * The SDK will automatically load ads in the background and keep them ready.
     */
    fun startPreloading() {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            Log.d(LOG_TAG, "User has purchased, skipping preloader start.")
            return
        }

        if (preloaderActive.get()) {
            Log.d(LOG_TAG, "Preloader already active for $adUnitId")
            return
        }

        Log.d(LOG_TAG, "Starting AppOpenAdPreloader for $adUnitId")

        val preloadCallback = object : PreloadCallback {
            override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                Log.d(LOG_TAG, "App open ad preloaded for $adUnitId")
                AdDebugUtils.logEvent(adUnitId, "onPreloaded", "App open ad preloaded", true)
            }

            override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                Log.e(LOG_TAG, "App open ad failed to preload for $adUnitId: ${adError.message}")
                AdDebugUtils.logEvent(adUnitId, "onFailedToPreload", "App open ad failed to preload: ${adError.message}", false)
                logFailedToLoadEvent(adError)
            }

            override fun onAdsExhausted(preloadId: String) {
                Log.d(LOG_TAG, "App open ads exhausted for $adUnitId")
                AdDebugUtils.logEvent(adUnitId, "onAdsExhausted", "App open ads exhausted", false)
            }
        }

        val adRequest = AdRequest.Builder(adUnitId).build()
        val preloadConfig = PreloadConfiguration(adRequest)

        AppOpenAdPreloader.start(adUnitId, preloadConfig, preloadCallback)
        preloaderActive.set(true)

        Log.d(LOG_TAG, "AppOpenAdPreloader started for $adUnitId")
    }

    /**
     * Marks the AppOpenAdPreloader as inactive for this ad unit.
     * Note: GMA Next-Gen SDK preloaders don't have a stop method - they run until app termination.
     * This method just clears our tracking state.
     */
    fun stopPreloading() {
        if (!preloaderActive.get()) {
            Log.d(LOG_TAG, "Preloader not active for $adUnitId")
            return
        }

        // Note: GMA Next-Gen SDK preloaders don't have stop() - they run until app termination
        preloaderActive.set(false)
        Log.d(LOG_TAG, "AppOpenAdPreloader marked inactive for $adUnitId (SDK preloader continues in background)")
    }

    /**
     * Checks if a preloaded ad is available.
     */
    fun isPreloadedAdAvailable(): Boolean {
        return AppOpenAdPreloader.isAdAvailable(adUnitId)
    }

    /**
     * Polls (retrieves) a preloaded app open ad.
     * The SDK will automatically load another ad in the background after polling.
     *
     * @return The app open ad if available, null otherwise
     */
    fun pollPreloadedAd(): AppOpenAd? {
        val ad = AppOpenAdPreloader.pollAd(adUnitId)
        if (ad != null) {
            Log.d(LOG_TAG, "Polled preloaded app open ad for $adUnitId")
            AdDebugUtils.logEvent(adUnitId, "pollAd", "Polled preloaded app open ad", true)
        } else {
            Log.d(LOG_TAG, "No preloaded app open ad available for $adUnitId")
        }
        return ad
    }

    /**
     * Shows a preloaded ad if available.
     * Uses the preloader's pollAd to get the ad and shows it.
     *
     * @param activity The activity to show the ad on
     * @param callback Optional callback for ad events
     * @return true if ad was shown, false if no ad available
     */
    fun showPreloadedAd(activity: Activity, callback: AdManagerCallback? = null): Boolean {
        val ad = pollPreloadedAd()
        if (ad == null) {
            Log.d(LOG_TAG, "No preloaded ad to show")
            callback?.onNextAction()
            return false
        }

        // Set up event callback
        val eventCallback = createAppOpenAdEventCallback("preloaded", callback)
        ad.adEventCallback = eventCallback

        // Set isShowingAd BEFORE calling show() to prevent race conditions
        isShowingAd.set(true)
        try {
            ad.show(activity)
            return true
        } catch (e: Exception) {
            isShowingAd.set(false)
            Log.e(LOG_TAG, "Error showing preloaded ad: ${e.message}")
            callback?.onNextAction()
            return false
        }
    }

    /**
     * Prefetch the next app open ad in background.
     * Call this when you know the user will leave and return to the app.
     *
     * Use cases:
     * - Before launching external intent (camera, browser, etc.)
     * - Before starting an activity that will return
     * - When user is about to leave for a known reason
     *
     * When user returns:
     * - If ad is ready: shows instantly (no dialog)
     * - If still loading: welcome dialog waits for it
     *
     * @param onPrefetchStarted Optional callback when prefetch starts (true) or skipped (false)
     *
     * Example:
     * ```kotlin
     * // Before launching camera
     * appOpenManager.prefetchNextAd()
     * startActivityForResult(cameraIntent, REQUEST_CODE)
     * ```
     */
    @JvmOverloads
    fun prefetchNextAd(onPrefetchStarted: ((Boolean) -> Unit)? = null) {
        // Don't prefetch if user has purchased
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            Log.d(LOG_TAG, "User has purchased, skipping prefetch.")
            onPrefetchStarted?.invoke(false)
            return
        }

        // Don't prefetch if ad already available or currently loading
        if (isAdAvailable()) {
            Log.d(LOG_TAG, "Ad already available, skipping prefetch.")
            onPrefetchStarted?.invoke(false)
            return
        }

        if (isLoading.get()) {
            Log.d(LOG_TAG, "Ad already loading, skipping prefetch.")
            onPrefetchStarted?.invoke(false)
            return
        }

        Log.d(LOG_TAG, "Prefetching next app open ad...")
        AdDebugUtils.logEvent(adUnitId, "prefetch", "Prefetching next app open ad", true)
        onPrefetchStarted?.invoke(true)
        fetchAd()
    }

    /**
     * Check if an ad is currently being loaded.
     * Useful to know if prefetch is in progress.
     *
     * @return true if ad is currently loading
     */
    fun isAdLoading(): Boolean = isLoading.get()

    /**
     * Fetch ad with exponential backoff retry logic.
     * Uses isLoading flag to prevent concurrent ad requests.
     */
    private fun fetchAdWithRetry(retryCount: Int) {
        if (isAdAvailable()) {
            return
        }

        // Prevent concurrent ad requests - only check on first attempt
        if (retryCount == 0 && !isLoading.compareAndSet(false, true)) {
            Log.d(LOG_TAG, "fetchAdWithRetry: Already loading, skipping duplicate request")
            return
        }

        if (retryCount >= maxRetryAttempts) {
            isLoading.set(false)  // Reset loading state
            AdDebugUtils.logEvent(adUnitId, "maxRetriesExceeded", "App open ad max retry attempts exceeded", false)
            return
        }

        if (AdManageKitConfig.testMode) {
            AdDebugUtils.logEvent(adUnitId, "testMode", "Using test mode for app open ads (retry: $retryCount)", true)
        }

        lastLoadStartTime = System.currentTimeMillis()
        val request = getAdRequest(adUnitId)

        AppOpenAd.load(
            request,
            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading.set(false)  // Reset loading state
                    appOpenAd = ad

                    // Track loading performance
                    val loadTime = System.currentTimeMillis() - lastLoadStartTime
                    synchronized(loadTimes) {
                        loadTimes.add(loadTime)
                        if (loadTimes.size > 100) {
                            loadTimes.removeAt(0)
                        }
                    }

                    AdDebugUtils.logEvent(
                        adUnitId,
                        "onAdLoaded",
                        "App open ad loaded successfully (${loadTime}ms, retry: $retryCount)",
                        true
                    )
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(LOG_TAG, "onAdFailedToLoad: failed to load (retry: $retryCount)")
                    AdDebugUtils.logEvent(
                        adUnitId,
                        "onFailedToLoad",
                        "App open ad failed to load: ${loadAdError.message} (retry: $retryCount)",
                        false
                    )

                    logFailedToLoadEvent(loadAdError)

                    // Determine if we should retry based on error type
                    if (shouldRetryForError(loadAdError) && retryCount < maxRetryAttempts) {
                        val retryDelay = calculateRetryDelay(retryCount)
                        AdDebugUtils.logEvent(adUnitId, "schedulingRetry", "Scheduling retry in ${retryDelay}ms", true)

                        scheduleTimeout(retryDelay) {
                            fetchAdWithRetry(retryCount + 1)
                        }
                    } else {
                        isLoading.set(false)  // Reset loading state only if not retrying
                    }
                }
            })
    }

    /**
     * Calculate exponential backoff delay
     */
    private fun calculateRetryDelay(retryCount: Int): Long {
        val delay = (baseRetryDelayMs * retryMultiplier.pow(retryCount)).toLong()
        return min(delay, maxRetryDelayMs)
    }

    /**
     * Determine if we should retry based on error type
     */
    private fun shouldRetryForError(error: LoadAdError): Boolean {
        return when (error.code) {
            LoadAdError.ErrorCode.NETWORK_ERROR,
            LoadAdError.ErrorCode.NO_FILL -> true
            else -> false
        }
    }

    /**
     * Request an ad with a timeout.
     *
     * @param adLoadCallback The callback to be invoked when the ad is loaded or fails to load.
     * @param timeoutMillis The timeout duration in milliseconds. If the ad does not load within this time, it will trigger the onFailedToLoad callback.
     * @param customAdUnitId Optional custom ad unit ID. If null, uses the default ad unit ID passed in constructor.
     */
    @JvmOverloads
    fun fetchAd(
        adLoadCallback: AdLoadCallback,
        timeoutMillis: Long = AdManageKitConfig.appOpenAdTimeout.inWholeMilliseconds,
        customAdUnitId: String? = null
    ) {
        // Don't fetch ads if user has purchased
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            Log.d(LOG_TAG, "User has purchased, skipping ad fetch.")
            adLoadCallback.onAdLoaded() // Call success to continue app flow
            return
        }

        // Use custom ad unit if provided, otherwise use default
        val effectiveAdUnitId = customAdUnitId ?: adUnitId

        if (isAdAvailable()) {
            adLoadCallback.onAdLoaded()
            return
        }

        if (AdManageKitConfig.testMode) {
            AdDebugUtils.logEvent(effectiveAdUnitId, "testMode", "Using test mode for app open ads with timeout", true)
        }

        lastLoadStartTime = System.currentTimeMillis()
        lastLoadStartTime = System.currentTimeMillis()
        val request = getAdRequest(effectiveAdUnitId)
        var hasTimedOut = false

        // Use enhanced timeout handling
        val timeoutRunnable = scheduleTimeout(timeoutMillis) {
            hasTimedOut = true
            val loadAdError =
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, "Ad load timed out", null)
            Log.e(LOG_TAG, "onAdFailedToLoad: timeout after $timeoutMillis ms")
            adLoadCallback.onFailedToLoad(loadAdError)
        }

        AppOpenAd.load(
            request,
            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    if (!hasTimedOut) {
                        cancelTimeout(timeoutRunnable)
                        appOpenAd = ad

                        // Track loading performance
                        val loadTime = System.currentTimeMillis() - lastLoadStartTime
                        synchronized(loadTimes) {
                            loadTimes.add(loadTime)
                            if (loadTimes.size > 100) { // Keep last 100 load times
                                loadTimes.removeAt(0)
                            }
                        }

                        AdDebugUtils.logEvent(
                            effectiveAdUnitId,
                            "onAdLoaded",
                            "App open ad loaded with timeout (${loadTime}ms)",
                            true
                        )
                        adLoadCallback.onAdLoaded()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (!hasTimedOut) {
                        cancelTimeout(timeoutRunnable)
                        Log.e(LOG_TAG, "onAdFailedToLoad: failed to load")
                        AdDebugUtils.logEvent(
                            effectiveAdUnitId,
                            "onFailedToLoad",
                            "App open ad failed with timeout: ${loadAdError.message}",
                            false
                        )
                        logFailedToLoadEvent(loadAdError)
                        adLoadCallback.onFailedToLoad(loadAdError)
                    }
                }
            }
        )
    }


    /**
     * Creates and returns ad request.
     */
    private fun getAdRequest(adUnitId: String): AdRequest {
        return AdRequest.Builder(adUnitId).build()
    }

    /**
     * Utility method that checks if ad exists and can be shown.
     * Thread-safe implementation.
     * Checks both preloader and manually loaded ad.
     */
    fun isAdAvailable(): Boolean {
        // Check preloader first if active
        if (usePreloader && preloaderActive.get() && AppOpenAdPreloader.isAdAvailable(adUnitId)) {
            return true
        }
        // Fallback to manually loaded ad
        return appOpenAd != null
    }

    /**
     * Thread-safe method to get performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any> {
        return synchronized(loadTimes) {
            mapOf(
                "averageLoadTime" to if (loadTimes.isNotEmpty()) loadTimes.average() else 0.0,
                "totalLoads" to loadTimes.size
            )
        }
    }

    /**
     * Thread-safe configuration updates
     */
    fun updateRetryConfiguration(
        maxAttempts: Int = this.maxRetryAttempts,
        baseDelay: Long = this.baseRetryDelayMs,
        maxDelay: Long = this.maxRetryDelayMs,
        multiplier: Double = this.retryMultiplier
    ) {
        synchronized(this) {
            this.maxRetryAttempts = maxAttempts.coerceIn(1, 10)
            this.baseRetryDelayMs = baseDelay.coerceIn(100L, 10000L)
            this.maxRetryDelayMs = maxDelay.coerceIn(1000L, 300000L)
            this.retryMultiplier = multiplier.coerceIn(1.0, 5.0)
        }
    }

    /**
     * Enhanced API for checking if showing ad with detailed reasons
     */
    fun canShowAd(): AdShowResult {
        val currentActivity = getCurrentActivity()

        return when {
            currentActivity == null -> AdShowResult.CANNOT_SHOW("No current activity")
            isActivityExcluded(currentActivity::class.java) -> AdShowResult.CANNOT_SHOW("Activity excluded")
            isShowingAd.get() -> AdShowResult.CANNOT_SHOW("Ad already showing")
            !isAdAvailable() -> AdShowResult.CANNOT_SHOW("No ad available")
            skipNextAd.get() -> AdShowResult.CANNOT_SHOW("Next ad skipped")
            AdManager.getInstance().isDisplayingAd() -> AdShowResult.CANNOT_SHOW("Other ad displaying")
            else -> AdShowResult.CAN_SHOW
        }
    }

    /**
     * Result class for ad show capability check
     */
    sealed class AdShowResult {
        object CAN_SHOW : AdShowResult()
        data class CANNOT_SHOW(val reason: String) : AdShowResult()
    }

    /**
     * Enhanced frequency capping
     */
    fun setFrequencyCapping(maxShowsPerHour: Int, maxShowsPerDay: Int) {
        // Implementation would require additional state tracking
        // For now, just validate parameters
        require(maxShowsPerHour > 0) { "Max shows per hour must be positive" }
        require(maxShowsPerDay > 0) { "Max shows per day must be positive" }
        require(maxShowsPerDay >= maxShowsPerHour) { "Daily limit must be >= hourly limit" }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        // Only update when not showing ad to avoid capturing AdActivity
        if (!isShowingAd.get()) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Adds an activity class to the set of excluded activities.
     * Optimized with cache invalidation.
     */
    fun disableAppOpenWithActivity(activityClass: Class<*>) {
        synchronized(excludedActivities) {
            excludedActivities.add(activityClass)
            excludedActivityNames.add(activityClass.name) // Pre-populate cache
        }
    }

    /**
     * Removes an activity class from the set of excluded activities.
     * Optimized with cache invalidation.
     */
    fun includeAppOpenActivityForAds(activityClass: Class<*>) {
        synchronized(excludedActivities) {
            excludedActivities.remove(activityClass)
            excludedActivityNames.remove(activityClass.name) // Update cache
        }
    }

    // =================== SCREEN/FRAGMENT TAG EXCLUSIONS ===================
    // For single-activity apps with multiple fragments

    /**
     * Set the current screen tag. Call this when navigating between screens/fragments.
     * If the screen tag is in the excluded list, app open ads won't show.
     *
     * Example usage with Navigation Component:
     * ```kotlin
     * navController.addOnDestinationChangedListener { _, destination, _ ->
     *     appOpenManager.setCurrentScreenTag(destination.label?.toString())
     * }
     * ```
     *
     * @param tag The current screen/fragment tag
     */
    fun setCurrentScreenTag(tag: String?) {
        currentScreenTag = tag
        Log.d(LOG_TAG, "Current screen tag set to: $tag")
    }

    /**
     * Get the current screen tag.
     */
    fun getCurrentScreenTag(): String? = currentScreenTag

    /**
     * Add a screen tag to the exclusion list.
     * App open ads won't show when the current screen tag matches.
     *
     * @param tag The screen tag to exclude
     */
    fun excludeScreenTag(tag: String) {
        synchronized(excludedScreenTags) {
            excludedScreenTags.add(tag)
        }
        Log.d(LOG_TAG, "Screen tag '$tag' excluded")
    }

    /**
     * Add multiple screen tags to the exclusion list.
     */
    fun excludeScreenTags(vararg tags: String) {
        synchronized(excludedScreenTags) {
            excludedScreenTags.addAll(tags)
        }
        Log.d(LOG_TAG, "Screen tags ${tags.toList()} excluded")
    }

    /**
     * Remove a screen tag from the exclusion list.
     */
    fun includeScreenTag(tag: String) {
        synchronized(excludedScreenTags) {
            excludedScreenTags.remove(tag)
        }
    }

    /**
     * Clear all screen tag exclusions.
     */
    fun clearScreenTagExclusions() {
        synchronized(excludedScreenTags) {
            excludedScreenTags.clear()
        }
    }

    /**
     * Set a fragment tag provider for automatic detection.
     *
     * Example:
     * ```kotlin
     * appOpenManager.setFragmentTagProvider {
     *     supportFragmentManager.fragments.lastOrNull()?.tag
     * }
     * ```
     */
    fun setFragmentTagProvider(provider: (() -> String?)?) {
        fragmentTagProvider = provider
    }

    /**
     * Add a fragment tag to the exclusion list.
     */
    fun excludeFragmentTag(tag: String) {
        synchronized(excludedFragmentTags) {
            excludedFragmentTags.add(tag)
        }
    }

    /**
     * Add multiple fragment tags to the exclusion list.
     */
    fun excludeFragmentTags(vararg tags: String) {
        synchronized(excludedFragmentTags) {
            excludedFragmentTags.addAll(tags)
        }
    }

    /**
     * Remove a fragment tag from the exclusion list.
     */
    fun includeFragmentTag(tag: String) {
        synchronized(excludedFragmentTags) {
            excludedFragmentTags.remove(tag)
        }
    }

    /**
     * Temporarily disable app open ads for a flow.
     * Call enableAppOpenAds() when done.
     */
    fun disableAppOpenAdsTemporarily() {
        skipNextAd.set(true)
        Log.d(LOG_TAG, "App open ads temporarily disabled")
    }

    /**
     * Re-enable app open ads.
     */
    fun enableAppOpenAds() {
        skipNextAd.set(false)
        Log.d(LOG_TAG, "App open ads re-enabled")
    }

    /**
     * Check if app open ads are currently enabled.
     */
    fun areAppOpenAdsEnabled(): Boolean = !skipNextAd.get()

    override fun onStart(owner: LifecycleOwner) {
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (!purchaseProvider.isPurchased()) {
            currentActivity?.let {
                Log.d(LOG_TAG, "onStart - showing ad on: ${it.javaClass.simpleName}")
                showAdIfAvailable()
            }
        }
    }

}
