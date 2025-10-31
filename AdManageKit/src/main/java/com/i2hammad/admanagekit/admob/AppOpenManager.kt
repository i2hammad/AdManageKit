package com.i2hammad.admanagekit.admob;

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.config.AdManageKitConfig
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

    private var currentActivityRef: WeakReference<Activity>? = null
    @Volatile
    private var appOpenAd: AppOpenAd? = null

    private val excludedActivities: MutableSet<Class<*>> = HashSet()
    private val excludedActivityNames: MutableSet<String> = HashSet() // Cache for performance

    private val skipNextAd = AtomicBoolean(false)
    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(myApplication)

    // Enhanced retry and circuit breaker state with thread safety
    private val failureCount = AtomicInteger(0)
    @Volatile
    private var lastFailureTime = 0L
    private val isCircuitBreakerOpen = AtomicBoolean(false)
    private val retryAttempts = mutableMapOf<String, Int>()

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
     * Shows the ad if one isn't already showing.
     */
    fun showAdIfAvailable() {
        val currentActivity = getCurrentActivity()
        if (currentActivity != null && isActivityExcluded(currentActivity::class.java)) {
            Log.d(LOG_TAG, "Ad display is skipped for this activity.")
            fetchAd()
            return
        }

        if (!isShowingAd.get() && isAdAvailable() && !skipNextAd.get() && !AdManager.getInstance().isDisplayingAd() && shouldAttemptLoad()) {
            Log.e(LOG_TAG, "Will show ad.")

            val fullScreenContentCallback = createFullScreenContentCallback("regular", null)

            currentActivity?.let { activity ->
                appOpenAd?.apply {
                    setOnPaidEventListener(createPaidEventListener())
                    setFullScreenContentCallback(fullScreenContentCallback)
                    show(activity)
                }
            }
        } else {
            Log.d(LOG_TAG, "Cannot show ad.")
            fetchAd()
        }

        skipNextAd.set(false)
    }

    /**
     * Force Show the ad on provided activity if one isn't already showing or already user purchased.
     */
    fun forceShowAdIfAvailable(activity: Activity, adManagerCallback: AdManagerCallback) {
        if (!isShowingAd.get() && isAdAvailable() && !activity.isFinishing) {
            Log.e(LOG_TAG, "Will show ad.")

            val fullScreenContentCallback = createFullScreenContentCallback("forced", adManagerCallback)

            appOpenAd?.apply {
                setOnPaidEventListener(createPaidEventListener())
                setFullScreenContentCallback(fullScreenContentCallback)
                show(activity)
            }
        } else {
            adManagerCallback.onNextAction()
            Log.d(LOG_TAG, "Cannot show ad.")
            fetchAd()
        }

        skipNextAd.set(false)
    }


    fun skipNextAd() {
        skipNextAd.set(true)
    }

    /**
     * Get current activity safely
     */
    private fun getCurrentActivity(): Activity? = currentActivityRef?.get()

    /**
     * Check if activity is excluded with performance optimization
     */
    private fun isActivityExcluded(activityClass: Class<*>): Boolean {
        val className = activityClass.name

        // Fast path: check cache first
        synchronized(excludedActivities) {
            if (excludedActivityNames.contains(className)) {
                return true
            }

            // Slow path: check actual set and update cache
            val isExcluded = excludedActivities.contains(activityClass)
            if (isExcluded) {
                excludedActivityNames.add(className) // Cache for future lookups
            }
            return isExcluded
        }
    }

    /**
     * Create reusable full screen content callback
     */
    private fun createFullScreenContentCallback(type: String, callback: AdManagerCallback?): FullScreenContentCallback {
        return object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd.set(false)
                val logMessage = when (type) {
                    "forced" -> "App open ad dismissed (forced)"
                    else -> "App open ad dismissed"
                }
                AdDebugUtils.logEvent(adUnitId, "onAdDismissed", logMessage, true)
                fetchAd()
                callback?.onNextAction()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                val logMessage = when (type) {
                    "forced" -> "App open ad failed to show (forced): ${adError.message}"
                    else -> "App open ad failed to show: ${adError.message}"
                }
                AdDebugUtils.logEvent(adUnitId, "onFailedToShow", logMessage, false)
                logFailedToLoadEvent(adError)
                callback?.onNextAction()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd.set(true)
                isShownAd.set(true)
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
        currentActivityRef = null

        // Clear caches
        excludedActivityNames.clear()
        loadTimes.clear()

        // Reset state
        isShowingAd.set(false)
        skipNextAd.set(false)

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
        fetchAdWithRetry(0)
    }

    /**
     * Fetch ad with exponential backoff retry logic
     */
    private fun fetchAdWithRetry(retryCount: Int) {
        if (isAdAvailable()) {
            return
        }

        if (!shouldAttemptLoad()) {
            AdDebugUtils.logEvent(adUnitId, "circuitBreakerBlocked", "App open ad loading blocked by circuit breaker", false)
            return
        }

        if (retryCount >= maxRetryAttempts) {
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
                    appOpenAd = ad

                    // Track loading performance
                    val loadTime = System.currentTimeMillis() - lastLoadStartTime
                    synchronized(loadTimes) {
                        loadTimes.add(loadTime)
                        if (loadTimes.size > 100) {
                            loadTimes.removeAt(0)
                        }
                    }

                    AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "App open ad loaded successfully (${loadTime}ms, retry: $retryCount)", true)
                    handleAdSuccess()
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
                        handleAdFailure()
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
     */
    fun fetchAd(adLoadCallback: AdLoadCallback, timeoutMillis: Long = AdManageKitConfig.appOpenAdTimeout.inWholeMilliseconds) {
        if (isAdAvailable()) {
            adLoadCallback.onAdLoaded()
            return
        }

        if (!shouldAttemptLoad()) {
            AdDebugUtils.logEvent(adUnitId, "circuitBreakerBlocked", "App open ad loading blocked by circuit breaker", false)
            val circuitBreakerError = LoadAdError(3, "Circuit breaker is open", "AdManageKit", null, null)
            adLoadCallback.onFailedToLoad(circuitBreakerError)
            return
        }

        if (AdManageKitConfig.testMode) {
            AdDebugUtils.logEvent(adUnitId, "testMode", "Using test mode for app open ads with timeout", true)
        }

        lastLoadStartTime = System.currentTimeMillis()
        val request = getAdRequest()
        var hasTimedOut = false

        // Use enhanced timeout handling
        val timeoutRunnable = scheduleTimeout(timeoutMillis) {
            hasTimedOut = true
            val loadAdError = LoadAdError(3, "Ad load timed out", "Google", null, null)
            Log.e(LOG_TAG, "onAdFailedToLoad: timeout after $timeoutMillis ms")
            handleAdFailure()
            adLoadCallback.onFailedToLoad(loadAdError)
        }

        AppOpenAd.load(
            myApplication,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
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

                        AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "App open ad loaded with timeout (${loadTime}ms)", true)
                        handleAdSuccess()
                        adLoadCallback.onAdLoaded()
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (!hasTimedOut) {
                        cancelTimeout(timeoutRunnable)
                        Log.e(LOG_TAG, "onAdFailedToLoad: failed to load")
                        AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "App open ad failed with timeout: ${loadAdError.message}", false)
                        handleAdFailure()
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
     * Thread-safe method to get performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any> {
        return synchronized(loadTimes) {
            mapOf(
                "averageLoadTime" to if (loadTimes.isNotEmpty()) loadTimes.average() else 0.0,
                "totalLoads" to loadTimes.size,
                "failureCount" to failureCount.get(),
                "circuitBreakerOpen" to isCircuitBreakerOpen.get(),
                "lastFailureTime" to lastFailureTime
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
            !shouldAttemptLoad() -> AdShowResult.CANNOT_SHOW("Circuit breaker open")
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
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (getCurrentActivity() == activity) {
            currentActivityRef = null
        }
    }

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
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (!purchaseProvider.isPurchased()) {
            showAdIfAvailable()
            Log.d(LOG_TAG, "onStart")
        }
    }

    /**
     * Handle ad loading failure for circuit breaker logic
     */
    private fun handleAdFailure() {
        val currentFailures = failureCount.incrementAndGet()
        lastFailureTime = System.currentTimeMillis()

        if (currentFailures >= AdManageKitConfig.circuitBreakerThreshold) {
            isCircuitBreakerOpen.set(true)
            AdDebugUtils.logEvent(adUnitId, "circuitBreakerOpen", "App open circuit breaker opened after $currentFailures failures", false)
        }
    }

    /**
     * Handle ad loading success for circuit breaker logic
     */
    private fun handleAdSuccess() {
        if (failureCount.get() > 0) {
            AdDebugUtils.logEvent(adUnitId, "circuitBreakerReset", "App open circuit breaker reset after success", true)
        }
        failureCount.set(0)
        isCircuitBreakerOpen.set(false)
    }

    /**
     * Check if circuit breaker should allow ad loading
     */
    private fun shouldAttemptLoad(): Boolean {
        if (!isCircuitBreakerOpen.get()) return true

        val timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime
        if (timeSinceLastFailure > AdManageKitConfig.circuitBreakerResetTimeout.inWholeMilliseconds) {
            isCircuitBreakerOpen.set(false)
            failureCount.set(0)
            AdDebugUtils.logEvent(adUnitId, "circuitBreakerReset", "App open circuit breaker reset after timeout", true)
            return true
        }

        AdDebugUtils.logEvent(adUnitId, "circuitBreakerBlocked", "App open ad blocked by circuit breaker, ${AdManageKitConfig.circuitBreakerResetTimeout.inWholeSeconds - (timeSinceLastFailure / 1000)}s remaining", false)
        return false
    }
}
