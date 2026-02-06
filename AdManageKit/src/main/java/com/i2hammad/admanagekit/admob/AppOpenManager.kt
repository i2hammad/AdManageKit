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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.config.AdLoadingStrategy
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
    @Volatile
    private var adLoadTime: Long = 0L  // Track when ad was loaded for freshness check

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

    // Track foreground/background state
    private val isAppInForeground = AtomicBoolean(false)

    // Track if ad was loaded while app was in background - needs to show when coming back
    private val pendingAdToShow = AtomicBoolean(false)
    private var pendingAdCallback: AdManagerCallback? = null

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
            set(value) { isShowingAd.set(value) }

        @JvmStatic
        @Deprecated("Use instance methods instead", ReplaceWith("instance.isShownAd.get()"))
        var isShownAdLegacy: Boolean
            get() = isShownAd.get()
            set(value) { isShownAd.set(value) }
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
     * Prevents duplicate dialog/fetch requests
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
        // Dismiss any existing dialog first to prevent duplicates
        currentWelcomeDialog?.let { existing ->
            try {
                if (existing.dialog.isShowing) {
                    existing.dialog.dismiss()
                }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Error dismissing existing welcome dialog: ${e.message}")
            }
            currentWelcomeDialog = null
        }

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
        val dialogViews = WelcomeBackDialogViews(dialog, overlay, contentCard)
        currentWelcomeDialog = dialogViews
        return dialogViews
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

        // Skip if interstitial ad or its loading dialog is showing
        if (AdManager.getInstance().isAdOrDialogShowing()) {
            Log.d(LOG_TAG, "Skipping app open ad: interstitial ad or loading dialog is showing")
            return
        }

        val currentActivity = getCurrentActivity()
        if (currentActivity != null && isActivityExcluded(currentActivity::class.java)) {
            Log.d(LOG_TAG, "Ad display is skipped for this activity.")
            // Prefetch in background for HYBRID/ONLY_CACHE strategies
            if (AdManageKitConfig.appOpenLoadingStrategy != AdLoadingStrategy.ON_DEMAND) {
                fetchAd()
            }
            return
        }

        // Handle based on loading strategy
        val strategy = AdManageKitConfig.appOpenLoadingStrategy
        Log.d(LOG_TAG, "showAdIfAvailable with strategy: $strategy")

        when (strategy) {
            AdLoadingStrategy.ON_DEMAND -> {
                // Always fetch fresh ad, but use cached if still fresh (avoid waste)
                if (currentActivity != null && !isShowingAd.get() && !skipNextAd.get()) {
                    if (isCachedAdFresh()) {
                        // Use fresh cached ad
                        val adAgeSeconds = getCachedAdAgeMs() / 1000
                        Log.d(LOG_TAG, "ON_DEMAND: Using fresh cached ad (age: ${adAgeSeconds}s)")
                        showCachedAd(currentActivity)
                    } else {
                        // Fetch fresh ad with dialog
                        val adAgeSeconds = if (adLoadTime > 0) getCachedAdAgeMs() / 1000 else -1
                        Log.d(LOG_TAG, "ON_DEMAND: Cached ad stale (age: ${adAgeSeconds}s), fetching fresh.")
                        appOpenAd = null
                        adLoadTime = 0L
                        showAdWithWelcomeDialog(currentActivity, null)
                    }
                } else {
                    Log.d(LOG_TAG, "ON_DEMAND: Cannot show ad.")
                }
            }

            AdLoadingStrategy.ONLY_CACHE -> {
                // Only show if cached ad available, never fetch with dialog
                if (!isShowingAd.get() && isAdAvailable() && !skipNextAd.get() && currentActivity != null) {
                    Log.d(LOG_TAG, "ONLY_CACHE: Showing cached ad.")
                    showCachedAd(currentActivity)
                } else {
                    Log.d(LOG_TAG, "ONLY_CACHE: No cached ad, skipping.")
                    // Prefetch for next time
                    fetchAd()
                }
            }

            AdLoadingStrategy.HYBRID, AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK -> {
                // Use cached if available, fetch with dialog if not
                if (!isShowingAd.get() && isAdAvailable() && !skipNextAd.get()) {
                    if (currentActivity == null) {
                        Log.e(LOG_TAG, "HYBRID: Cannot show ad - activity is null")
                        return
                    }
                    Log.d(LOG_TAG, "HYBRID: Showing cached ad.")
                    showCachedAd(currentActivity)
                } else if (!isAdAvailable() && currentActivity != null && !skipNextAd.get()) {
                    Log.d(LOG_TAG, "HYBRID: No cached ad, showing welcome dialog.")
                    showAdWithWelcomeDialog(currentActivity, null)
                } else {
                    Log.d(LOG_TAG, "HYBRID: Cannot show ad.")
                    fetchAd()
                }
            }
        }

        skipNextAd.set(false)
    }

    /**
     * Show the cached ad with welcome dialog.
     */
    private fun showCachedAd(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) {
            Log.d(LOG_TAG, "Cannot show cached ad: activity not valid")
            return
        }

        // Show welcome dialog first, then show ad on top
        val dialogViews = showWelcomeBackDialog(activity)
        currentWelcomeDialog = dialogViews

        // Brief delay to let dialog appear before showing ad
        Handler(Looper.getMainLooper()).postDelayed({
            if (activity.isFinishing || activity.isDestroyed) {
                animateDialogDismissal(dialogViews) {
                    currentWelcomeDialog = null
                }
                return@postDelayed
            }

            isShowingAd.set(true)
            val fullScreenContentCallback = createFullScreenContentCallback("regular", null)

            appOpenAd?.apply {
                setOnPaidEventListener(createPaidEventListener())
                setFullScreenContentCallback(fullScreenContentCallback)
                show(activity)
            } ?: run {
                isShowingAd.set(false)
                Log.e(LOG_TAG, "showCachedAd: appOpenAd became null")
                animateDialogDismissal(dialogViews) {
                    currentWelcomeDialog = null
                }
            }
        }, 500)
    }

    /**
     * Show ad with welcome back dialog
     */
    private fun showAdWithWelcomeDialog(activity: Activity, callback: AdManagerCallback?) {
        // Check if ad is already being displayed - most important check
        if (isShowingAd.get()) {
            Log.d(LOG_TAG, "Skipping showAdWithWelcomeDialog: ad already showing")
            callback?.onNextAction()
            return
        }

        // Skip if interstitial ad or its loading dialog is showing
        if (AdManager.getInstance().isAdOrDialogShowing()) {
            Log.d(LOG_TAG, "Skipping showAdWithWelcomeDialog: interstitial ad or loading dialog is showing")
            callback?.onNextAction()
            return
        }

        // Check if a dialog is already displayed (use actual dialog state, not flag)
        // This is more reliable as it checks the actual UI state
        val dialogShowing = try {
            currentWelcomeDialog?.dialog?.isShowing == true
        } catch (e: Exception) {
            false
        }

        if (dialogShowing) {
            Log.d(LOG_TAG, "Skipping showAdWithWelcomeDialog: dialog already showing")
            callback?.onNextAction()
            return
        }

        if (activity.isFinishing) {
            callback?.onNextAction()
            return
        }

        // Reset flag (in case stuck from previous interrupted fetch) and set for this fetch
        isFetchingWithDialog = true
        val dialogViews = showWelcomeBackDialog(activity)
        val timeoutMillis = AdManageKitConfig.appOpenAdTimeout.inWholeMilliseconds

        var hasTimedOut = false
        val timeoutRunnable = scheduleTimeout(timeoutMillis) {
            hasTimedOut = true
            isFetchingWithDialog = false
            animateDialogDismissal(dialogViews) {
                Log.e(LOG_TAG, "App open ad load timed out")
                callback?.onNextAction()
            }
        }

        val request = getAdRequest()
        AppOpenAd.load(
            myApplication,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Handler(Looper.getMainLooper()).post {
                        if (!hasTimedOut) {
                            cancelTimeout(timeoutRunnable)
                            isFetchingWithDialog = false
                            appOpenAd = ad
                            adLoadTime = System.currentTimeMillis()

                            Log.d(LOG_TAG, "Ad loaded, isAppInForeground=${isAppInForeground.get()}")

                            // Check if app is in foreground before showing ad
                            if (!isAppInForeground.get()) {
                                // App is in background - save ad for later, dismiss dialog
                                Log.d(LOG_TAG, "App in background, saving ad for when user returns")
                                pendingAdToShow.set(true)
                                pendingAdCallback = callback
                                animateDialogDismissal(dialogViews) {
                                    currentWelcomeDialog = null
                                }
                                return@post
                            }

                            // Keep dialog showing - ad will be displayed on top
                            // Dialog will be dismissed after ad is closed with delay
                            currentWelcomeDialog = dialogViews
                            Log.d(LOG_TAG, "Ad loaded, showing on top of welcome dialog")

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
                    Handler(Looper.getMainLooper()).post {
                        if (!hasTimedOut) {
                            cancelTimeout(timeoutRunnable)
                            isFetchingWithDialog = false
                            logFailedToLoadEvent(error)
                            animateDialogDismissal(dialogViews) {
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

        val request = getAdRequest()
        AppOpenAd.load(
            myApplication,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Handler(Looper.getMainLooper()).post {
                        appOpenAd = ad
                            adLoadTime = System.currentTimeMillis()
                        if (!activity.isFinishing) {
                            showLoadedAd(activity, callback)
                        } else {
                            callback?.onNextAction()
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Handler(Looper.getMainLooper()).post {
                        logFailedToLoadEvent(error)
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

        // Check if we have an ad to show
        if (appOpenAd == null) {
            Log.d(LOG_TAG, "Cannot show ad: appOpenAd is null")
            callback?.onNextAction()
            return
        }

        // Set isShowingAd BEFORE calling show() to prevent race conditions
        // This prevents another showLoadedAd call from starting while we're about to show
        isShowingAd.set(true)

        val fullScreenContentCallback = createFullScreenContentCallback(
            if (callback != null) "forced" else "regular",
            callback
        )

        appOpenAd?.apply {
            setOnPaidEventListener(createPaidEventListener())
            setFullScreenContentCallback(fullScreenContentCallback)
            show(activity)
        } ?: run {
            // Ad became null (shouldn't happen but handle it)
            Log.d(LOG_TAG, "Cannot show ad: appOpenAd became null")
            isShowingAd.set(false)
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
            if (AdManageKitConfig.appOpenAutoReload) {
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
     * Create reusable full screen content callback
     * All callbacks are posted to main thread to ensure UI operations are safe
     */
    private fun createFullScreenContentCallback(type: String, callback: AdManagerCallback?): FullScreenContentCallback {
        return object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Handler(Looper.getMainLooper()).post {
                    appOpenAd = null
                    isShowingAd.set(false)
                    val logMessage = when (type) {
                        "forced" -> "App open ad dismissed (forced)"
                        else -> "App open ad dismissed"
                    }
                    AdDebugUtils.logEvent(adUnitId, "onAdDismissed", logMessage, true)

                    // Clear dialog reference if still set
                    currentWelcomeDialog = null

                    // Auto-reload next ad if enabled
                    // Even with fetchFreshAd=true, auto-reload is useful because:
                    // - If user returns quickly, the "fresh" cached ad will be used
                    // - If user returns after threshold, a new fresh ad will be fetched
                    if (AdManageKitConfig.appOpenAutoReload) {
                        fetchAd()
                    }
                    callback?.onNextAction()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Handler(Looper.getMainLooper()).post {
                    // Reset isShowingAd since we set it to true before show()
                    isShowingAd.set(false)
                    appOpenAd = null

                    val logMessage = when (type) {
                        "forced" -> "App open ad failed to show (forced): ${adError.message}"
                        else -> "App open ad failed to show: ${adError.message}"
                    }
                    AdDebugUtils.logEvent(adUnitId, "onFailedToShow", logMessage, false)
                    logFailedToLoadEvent(adError)

                    // Dismiss dialog immediately if ad fails to show
                    currentWelcomeDialog?.let { dialogViews ->
                        dismissDialogSafely(dialogViews.dialog)
                        currentWelcomeDialog = null
                    }

                    callback?.onNextAction()
                }
            }

            override fun onAdShowedFullScreenContent() {
                Handler(Looper.getMainLooper()).post {
                    isShowingAd.set(true)
                    isShownAd.set(true)

                    // Dismiss welcome dialog with delay NOW (while ad is showing)
                    dismissWelcomeDialogWithDelay(currentWelcomeDialog)
                    val logMessage = when (type) {
                        "forced" -> "App open ad shown (forced)"
                        else -> "App open ad shown"
                    }
                    AdDebugUtils.logEvent(adUnitId, "onAdImpression", logMessage, true)

                    if (type == "forced") {
                        callback?.onAdLoaded()
                    }

                    logAdImpressionEvent()
                }
            }
        }
    }

    /**
     * Create reusable paid event listener
     */
    private fun createPaidEventListener(): OnPaidEventListener {
        return OnPaidEventListener { adValue ->
            val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
            val revenueParams = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            }
            firebaseAnalytics.logEvent("ad_paid_event", revenueParams)
        }
    }

    /**
     * Log Firebase analytics for failed ad loads
     */
    private fun logFailedToLoadEvent(adError: AdError) {
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
        excludedScreenTags.clear()
        excludedFragmentTags.clear()
        currentScreenTag = null
        fragmentTagProvider = null
        loadTimes.clear()

        // Reset state
        isShowingAd.set(false)
        skipNextAd.set(false)
        isLoading.set(false)

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
     * Request an ad
     */
    fun fetchAd() {
        // Don't fetch ads if user has purchased
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            Log.d(LOG_TAG, "User has purchased, skipping ad fetch.")
            return
        }
        fetchAdWithRetry(0)
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
        val request = getAdRequest()

        AppOpenAd.load(myApplication,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoading.set(false)  // Reset loading state
                    appOpenAd = ad
                            adLoadTime = System.currentTimeMillis()

                    // Track loading performance
                    val loadTime = System.currentTimeMillis() - lastLoadStartTime
                    synchronized(loadTimes) {
                        loadTimes.add(loadTime)
                        if (loadTimes.size > 100) {
                            loadTimes.removeAt(0)
                        }
                    }

                    AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "App open ad loaded successfully (${loadTime}ms, retry: $retryCount)", true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(LOG_TAG, "onAdFailedToLoad: failed to load (retry: $retryCount)")
                    AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "App open ad failed to load: ${loadAdError.message} (retry: $retryCount)", false)

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
            AdRequest.ERROR_CODE_NETWORK_ERROR,
            AdRequest.ERROR_CODE_NO_FILL -> true
            AdRequest.ERROR_CODE_INTERNAL_ERROR -> true
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
        val request = getAdRequest()
        var hasTimedOut = false

        // Use enhanced timeout handling
        val timeoutRunnable = scheduleTimeout(timeoutMillis) {
            hasTimedOut = true
            val loadAdError = LoadAdError(3, "Ad load timed out", "Google", null, null)
            Log.e(LOG_TAG, "onAdFailedToLoad: timeout after $timeoutMillis ms")
            adLoadCallback.onFailedToLoad(loadAdError)
        }

        AppOpenAd.load(
            myApplication,
            effectiveAdUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    if (!hasTimedOut) {
                        cancelTimeout(timeoutRunnable)
                        appOpenAd = ad
                            adLoadTime = System.currentTimeMillis()

                        // Track loading performance
                        val loadTime = System.currentTimeMillis() - lastLoadStartTime
                        synchronized(loadTimes) {
                            loadTimes.add(loadTime)
                            if (loadTimes.size > 100) { // Keep last 100 load times
                                loadTimes.removeAt(0)
                            }
                        }

                        AdDebugUtils.logEvent(effectiveAdUnitId, "onAdLoaded", "App open ad loaded with timeout (${loadTime}ms)", true)
                        adLoadCallback.onAdLoaded()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (!hasTimedOut) {
                        cancelTimeout(timeoutRunnable)
                        Log.e(LOG_TAG, "onAdFailedToLoad: failed to load")
                        AdDebugUtils.logEvent(effectiveAdUnitId, "onFailedToLoad", "App open ad failed with timeout: ${loadAdError.message}", false)
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
    private fun getAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }

    /**
     * Utility method that checks if ad exists and can be shown.
     * Thread-safe implementation.
     */
    fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }

    /**
     * Check if the cached ad is still "fresh" (not too old).
     * Uses appOpenAdFreshnessThreshold from config.
     *
     * @return true if ad exists and is younger than the freshness threshold
     */
    private fun isCachedAdFresh(): Boolean {
        if (appOpenAd == null || adLoadTime == 0L) return false
        val threshold = AdManageKitConfig.appOpenAdFreshnessThreshold.inWholeMilliseconds
        if (threshold == 0L) return false  // Duration.ZERO means always fetch fresh
        val adAge = System.currentTimeMillis() - adLoadTime
        return adAge < threshold
    }

    /**
     * Get the age of the cached ad in milliseconds.
     * @return age in ms, or -1 if no ad cached
     */
    fun getCachedAdAgeMs(): Long {
        if (adLoadTime == 0L) return -1
        return System.currentTimeMillis() - adLoadTime
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

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground.set(true)

        val purchaseProvider = BillingConfig.getPurchaseProvider()
        // Use isAdOrDialogShowing() to also check for interstitial loading dialog
        if (!purchaseProvider.isPurchased() && !AdManager.getInstance().isAdOrDialogShowing()) {
            currentActivity?.let { activity ->
                Log.d(LOG_TAG, "onStart - showing ad on: ${activity.javaClass.simpleName}")

                // Check if we have a pending ad that was loaded while in background
                if (pendingAdToShow.getAndSet(false) && appOpenAd != null) {
                    Log.d(LOG_TAG, "Showing pending ad that was loaded while in background")
                    val callback = pendingAdCallback
                    pendingAdCallback = null

                    // Show welcome dialog again briefly before showing the ad
                    showPendingAdWithDialog(activity, callback)
                } else {
                    showAdIfAvailable()
                }
            }
        } else if (AdManager.getInstance().isAdOrDialogShowing()) {
            Log.d(LOG_TAG, "onStart - skipping app open ad: interstitial ad or dialog is showing")
            // Clear pending ad state since interstitial takes priority
            if (pendingAdToShow.get()) {
                Log.d(LOG_TAG, "Clearing pending app open ad - interstitial has priority")
                pendingAdToShow.set(false)
                pendingAdCallback?.onNextAction()
                pendingAdCallback = null
            }
        }
    }

    /**
     * Show pending ad with a brief welcome dialog.
     * Used when ad was loaded while app was in background.
     */
    private fun showPendingAdWithDialog(activity: Activity, callback: AdManagerCallback?) {
        if (activity.isFinishing || activity.isDestroyed) {
            callback?.onNextAction()
            return
        }

        // Double-check interstitial isn't showing (could have started between onStart check and now)
        if (AdManager.getInstance().isAdOrDialogShowing()) {
            Log.d(LOG_TAG, "Skipping pending ad dialog - interstitial is showing")
            callback?.onNextAction()
            return
        }

        // Show welcome dialog again
        val dialogViews = showWelcomeBackDialog(activity)
        currentWelcomeDialog = dialogViews

        // Show the ad after a brief delay (let dialog appear first)
        Handler(Looper.getMainLooper()).postDelayed({
            // Check again before showing - interstitial might have appeared
            if (AdManager.getInstance().isAdOrDialogShowing()) {
                Log.d(LOG_TAG, "Cancelling pending ad - interstitial appeared")
                animateDialogDismissal(dialogViews) {
                    currentWelcomeDialog = null
                    callback?.onNextAction()
                }
                return@postDelayed
            }

            if (!activity.isFinishing && !activity.isDestroyed && appOpenAd != null) {
                Log.d(LOG_TAG, "Showing pending ad after dialog")
                showLoadedAd(activity, callback)
            } else {
                // Dismiss dialog if can't show ad
                animateDialogDismissal(dialogViews) {
                    currentWelcomeDialog = null
                    callback?.onNextAction()
                }
            }
        }, 500) // 500ms delay to let dialog appear
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground.set(false)
        Log.d(LOG_TAG, "onStop - app went to background")

        // Prefetch ad in background so it's ready when user returns
        // Only when appOpenFetchFreshAd is false (not fetching fresh on start)
        if (!AdManageKitConfig.appOpenFetchFreshAd &&
            !BillingConfig.getPurchaseProvider().isPurchased() &&
            !isAdAvailable() && !isLoading.get()
        ) {
            Log.d(LOG_TAG, "onStop - prefetching ad for next foreground")
            fetchAd()
        }
    }

    // =================== SCREEN/FRAGMENT TAG EXCLUSIONS (v3.2.0+) ===================

    /**
     * Set the current screen tag for single-activity apps.
     * Call this when navigating between screens/fragments.
     *
     * Example with Navigation Component:
     * ```kotlin
     * navController.addOnDestinationChangedListener { _, destination, _ ->
     *     appOpenManager.setCurrentScreenTag(destination.label?.toString())
     * }
     * ```
     *
     * @param tag The current screen tag (e.g., destination label, fragment name)
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
     * Exclude a screen tag from showing app open ads.
     *
     * @param tag The screen tag to exclude
     */
    fun excludeScreenTag(tag: String) {
        synchronized(excludedScreenTags) {
            excludedScreenTags.add(tag)
        }
        Log.d(LOG_TAG, "Excluded screen tag: $tag")
    }

    /**
     * Exclude multiple screen tags from showing app open ads.
     *
     * @param tags The screen tags to exclude
     */
    fun excludeScreenTags(vararg tags: String) {
        synchronized(excludedScreenTags) {
            excludedScreenTags.addAll(tags)
        }
        Log.d(LOG_TAG, "Excluded screen tags: ${tags.joinToString()}")
    }

    /**
     * Re-include a previously excluded screen tag.
     *
     * @param tag The screen tag to include
     */
    fun includeScreenTag(tag: String) {
        synchronized(excludedScreenTags) {
            excludedScreenTags.remove(tag)
        }
        Log.d(LOG_TAG, "Included screen tag: $tag")
    }

    /**
     * Clear all screen tag exclusions.
     */
    fun clearScreenTagExclusions() {
        synchronized(excludedScreenTags) {
            excludedScreenTags.clear()
        }
        Log.d(LOG_TAG, "Cleared all screen tag exclusions")
    }

    /**
     * Set a provider function that returns the current fragment tag.
     * This is called automatically when checking if ads should be shown.
     *
     * Example:
     * ```kotlin
     * appOpenManager.setFragmentTagProvider {
     *     supportFragmentManager.fragments.lastOrNull()?.tag
     * }
     * ```
     *
     * @param provider A function that returns the current fragment tag, or null
     */
    fun setFragmentTagProvider(provider: (() -> String?)?) {
        fragmentTagProvider = provider
        Log.d(LOG_TAG, "Fragment tag provider ${if (provider != null) "set" else "cleared"}")
    }

    /**
     * Exclude a fragment tag from showing app open ads.
     *
     * @param tag The fragment tag to exclude
     */
    fun excludeFragmentTag(tag: String) {
        synchronized(excludedFragmentTags) {
            excludedFragmentTags.add(tag)
        }
        Log.d(LOG_TAG, "Excluded fragment tag: $tag")
    }

    /**
     * Exclude multiple fragment tags from showing app open ads.
     *
     * @param tags The fragment tags to exclude
     */
    fun excludeFragmentTags(vararg tags: String) {
        synchronized(excludedFragmentTags) {
            excludedFragmentTags.addAll(tags)
        }
        Log.d(LOG_TAG, "Excluded fragment tags: ${tags.joinToString()}")
    }

    /**
     * Re-include a previously excluded fragment tag.
     *
     * @param tag The fragment tag to include
     */
    fun includeFragmentTag(tag: String) {
        synchronized(excludedFragmentTags) {
            excludedFragmentTags.remove(tag)
        }
        Log.d(LOG_TAG, "Included fragment tag: $tag")
    }

    // =================== TEMPORARY DISABLE/ENABLE ===================

    /**
     * Temporarily disable app open ads.
     * Useful during critical user flows (e.g., payment, onboarding).
     *
     * Call [enableAppOpenAds] to re-enable.
     */
    fun disableAppOpenAdsTemporarily() {
        skipNextAd.set(true)
        Log.d(LOG_TAG, "App open ads temporarily disabled")
    }

    /**
     * Re-enable app open ads after temporary disable.
     */
    fun enableAppOpenAds() {
        skipNextAd.set(false)
        Log.d(LOG_TAG, "App open ads re-enabled")
    }

    /**
     * Check if app open ads are currently enabled.
     */
    fun areAppOpenAdsEnabled(): Boolean = !skipNextAd.get()

}
