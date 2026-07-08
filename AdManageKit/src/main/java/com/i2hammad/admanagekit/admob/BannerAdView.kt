package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowMetrics
import android.view.Gravity
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AdProviderConfig
import com.i2hammad.admanagekit.core.ad.AdUnitMapping
import com.i2hammad.admanagekit.core.ad.BannerAdProvider
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.config.CollapsibleBannerPlacement
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.waterfall.BannerWaterfall
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Enhanced BannerAdView with improved reliability, performance, and lifecycle management.
 * 
 * This view provides advanced banner ad functionality including:
 * - Memory leak prevention with WeakReference holders
 * - Automatic retry logic
 * - Lifecycle-aware ad refresh
 * - Performance monitoring and debug integration
 * - Thread-safe operations
 * 
 * @since 1.0.0 (Enhanced in 2.1.0)
 */
class BannerAdView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), LifecycleEventObserver {

    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var adView: AdView? = null
    private var shimmerFrameLayout: ShimmerFrameLayout
    private var layBannerAd: FrameLayout
    
    // Enhanced memory management with WeakReference
    private var activityRef: WeakReference<Activity>? = null 
    var callback: AdLoadCallback? = null
    
    // Enhanced features
    private var currentAdUnitId: String? = null
    // Original load configuration, preserved so auto-refresh reloads keep the same behavior
    private var currentCollapsible: Boolean = false
    private var currentPlacement: CollapsibleBannerPlacement = CollapsibleBannerPlacement.BOTTOM
    private var isAdLoading = AtomicBoolean(false)
    private var loadAttempt = AtomicInteger(0)
    private var refreshHandler: Handler? = null
    private var refreshRunnable: Runnable? = null
    private var autoRefreshEnabled = false
    private var customRefreshIntervalSeconds: Int? = null // Per-view interval set via enableAutoRefresh()
    private val refreshIntervalSeconds get() = customRefreshIntervalSeconds
        ?: AdManageKitConfig.defaultBannerRefreshInterval.inWholeSeconds.toInt()
    
    // Waterfall support
    private var bannerWaterfall: BannerWaterfall? = null
    private val useWaterfall: Boolean
        get() = AdProviderConfig.getBannerChain().isNotEmpty()

    // Performance tracking
    private var loadStartTime: Long = 0
    private val maxRetryAttempts get() = AdManageKitConfig.maxRetryAttempts

    init {
        LayoutInflater.from(context).inflate(R.layout.banner_ad_view, this, true)
        shimmerFrameLayout = findViewById(R.id.shimmer_frame_layout)
        layBannerAd = findViewById(R.id.fl_ad_container)
        if (!isInEditMode) {
            shimmerFrameLayout.startShimmer()
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            refreshHandler = Handler(Looper.getMainLooper())
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                resumeAd()
                if (autoRefreshEnabled) {
                    startAutoRefresh()
                }
            }
            Lifecycle.Event.ON_PAUSE -> {
                pauseAd()
                stopAutoRefresh()
            }
            Lifecycle.Event.ON_DESTROY -> {
                cleanup()
            }
            else -> { /* Handle other events if needed */ }
        }
    }


    fun loadBanner(context: Activity?, adUnitId: String?, adLoadCallback: AdLoadCallback? = null) {
        loadCollapsibleBanner(context, adUnitId, false, CollapsibleBannerPlacement.BOTTOM, adLoadCallback)
    }

    fun loadBanner(context: Activity?, adUnitId: String?) {
        loadCollapsibleBanner(context, adUnitId, false, CollapsibleBannerPlacement.BOTTOM, callback)
    }

    fun setAdCallback(callback: AdLoadCallback?) {
        this.callback = callback
    }

    // =================== DEPRECATED METHODS (kept for backward compatibility) ===================

    @Deprecated(
        message = "Use loadCollapsibleBanner with CollapsibleBannerPlacement parameter instead",
        replaceWith = ReplaceWith("loadCollapsibleBanner(context, adUnitId, collapsible, CollapsibleBannerPlacement.BOTTOM)")
    )
    fun loadCollapsibleBanner(
        context: Activity?, adUnitId: String?, collapsible: Boolean
    ) {
        loadCollapsibleBanner(context, adUnitId, collapsible, CollapsibleBannerPlacement.BOTTOM, callback)
    }

    @Deprecated(
        message = "Use loadCollapsibleBanner with CollapsibleBannerPlacement parameter instead",
        replaceWith = ReplaceWith("loadCollapsibleBanner(context, adUnitId, collapsible, CollapsibleBannerPlacement.BOTTOM, callback)")
    )
    fun loadCollapsibleBanner(
        context: Activity?,
        adUnitId: String?,
        collapsible: Boolean,
        callback: AdLoadCallback? = null
    ) {
        loadCollapsibleBanner(context, adUnitId, collapsible, CollapsibleBannerPlacement.BOTTOM, callback)
    }

    // =================== NEW METHODS (recommended) ===================

    /**
     * Load a collapsible banner ad with custom placement direction.
     *
     * @param context Activity context
     * @param adUnitId Ad unit ID
     * @param collapsible Whether the banner should be collapsible
     * @param placement Direction from which the banner collapses (TOP or BOTTOM)
     * @param callback Optional callback for ad lifecycle events
     */
    fun loadCollapsibleBanner(
        context: Activity?,
        adUnitId: String?,
        collapsible: Boolean,
        placement: CollapsibleBannerPlacement,
        callback: AdLoadCallback? = null
    ) {
        if (isInEditMode || context == null || adUnitId == null) return

        // Enhanced memory management
        this.activityRef = WeakReference(context)
        this.currentAdUnitId = adUnitId
        this.currentCollapsible = collapsible
        this.currentPlacement = placement
        this.callback = callback

        // Register lifecycle observer if possible
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(this)
        }

        loadBannerInternal(adUnitId, collapsible, placement, callback)
    }
    
    /**
     * Internal method that handles the actual ad loading with enhanced features.
     */
    private fun loadBannerInternal(
        adUnitId: String,
        collapsible: Boolean,
        placement: CollapsibleBannerPlacement = CollapsibleBannerPlacement.BOTTOM,
        callback: AdLoadCallback? = null
    ) {
        if (useWaterfall) { loadBannerViaWaterfall(adUnitId, collapsible, placement, callback); return }

        // Prevent concurrent loads
        if (!isAdLoading.compareAndSet(false, true)) {
            AdDebugUtils.logDebug("BannerAdView", "Ad loading already in progress for $adUnitId")
            return
        }
        
        val attempt = loadAttempt.get()
        loadStartTime = System.currentTimeMillis()
        
        // Check purchase status
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            // Next-Gen SDK's LoadAdError.ErrorCode is a closed enum with no
            // purchase-blocked value, so this only carries the message - the
            // numeric PURCHASED_APP_ERROR_CODE (1001) contract lives on
            // AdKitAdError in the SDK-agnostic waterfall/provider path, not here.
            handleAdLoadFailure(
                adUnitId,
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, AdManager.PURCHASED_APP_ERROR_MESSAGE, null),
                callback,
                "User has purchased app",
                isPurchaseBlocked = true
            )
            return
        }

        // Use activity safely with WeakReference
        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            handleAdLoadFailure(
                adUnitId,
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, "Activity not available", null),
                callback,
                "Activity reference is null or invalid"
            )
            return
        }
        
        try {
            // Make sure the banner is visible during loading
            visibility = View.VISIBLE
            shimmerFrameLayout.visibility = View.VISIBLE
            shimmerFrameLayout.startShimmer()
            
            // Destroy the previous AdView (if any) before replacing it to avoid
            // leaking a WebView-backed view on every reload/auto-refresh
            adView?.destroy()

            // Create AdView (Next-Gen SDK: ad unit id and size are no longer set
            // on the AdView itself - setAdUnitId()/setAdSize() are gone - both are
            // now supplied via the request below)
            val calculatedAdSize = getAdSize()
            adView = AdView(activity)

            // Build ad request
            val requestBuilder = BannerAdRequest.Builder(adUnitId, calculatedAdSize)

            // Add collapsible extras if needed
            if (collapsible || AdManageKitConfig.enableCollapsibleBannersByDefault) {
                val extras = Bundle()
                extras.putString("collapsible", placement.value)
                requestBuilder.setGoogleExtrasBundle(extras)

                AdDebugUtils.logDebug("BannerAdView", "Loading collapsible banner with placement: ${placement.value}")
            }

            // Adjust shimmer to match ad size
            adjustShimmerLayout()

            // Load the ad. Click/impression/open/close/paid events are wired onto
            // the BannerAd handed back in onAdLoaded (see createEnhancedAdLoadCallback) -
            // Next-Gen SDK exposes them there, not on the AdView and not via a
            // separate AdListener/OnPaidEventListener.
            val adRequest = requestBuilder.build()
            adView?.loadAd(adRequest, createEnhancedAdLoadCallback(adUnitId, callback))

            // Notify callback of load start
            callback?.onAdLoadStarted()

        } catch (e: Exception) {
            handleAdLoadFailure(
                adUnitId,
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, "Exception during ad load: ${e.message}", null),
                callback,
                "Exception occurred: ${e.message}"
            )
        }
    }

    private fun adjustShimmerLayout() {
        val params = shimmerFrameLayout.layoutParams
        val adSize = getAdSize()
        val density = resources.displayMetrics.density
        params.height = (density * adSize.height).toInt()
        params.width = (density * adSize.width).toInt()
        shimmerFrameLayout.layoutParams = params
    }

    /**
     * Creates the enhanced ad load callback with improved error handling.
     *
     * Next-Gen SDK splits what used to be a single AdListener into two pieces:
     * load success/failure arrive here via [com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback],
     * while click/open/close/impression/paid events are only available on the
     * loaded [BannerAd] itself (wired up inside [onAdLoaded] below) - there is no
     * longer a single listener object that covers both.
     */
    private fun createEnhancedAdLoadCallback(
        adUnitId: String,
        callback: AdLoadCallback?
    ): com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<BannerAd> {
        return object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<BannerAd> {
            override fun onAdLoaded(bannerAd: BannerAd) {
                // Wire click/open/close/impression/paid events on the BannerAd -
                // Next-Gen SDK callbacks fire on a background thread, so each
                // dispatch to the (possibly UI-touching) external callback is
                // wrapped on the main thread, matching the old SDK's guarantee.
                bannerAd.adEventCallback = object : BannerAdEventCallback {
                    override fun onAdClicked() {
                        ensureMainThread {
                            AdDebugUtils.logEvent(adUnitId, "onAdClicked", "Banner ad clicked", true)
                            callback?.onAdClicked()
                        }
                    }

                    override fun onAdDismissedFullScreenContent() {
                        ensureMainThread {
                            AdDebugUtils.logEvent(adUnitId, "onAdClosed", "Banner ad closed", true)
                            callback?.onAdClosed()
                        }
                    }

                    override fun onAdShowedFullScreenContent() {
                        ensureMainThread {
                            AdDebugUtils.logEvent(adUnitId, "onAdOpened", "Banner ad opened", true)
                            callback?.onAdOpened()
                        }
                    }

                    override fun onAdImpression() {
                        ensureMainThread {
                            val loadTime = System.currentTimeMillis() - loadStartTime
                            val params = Bundle().apply {
                                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                                if (AdManageKitConfig.enablePerformanceMetrics) {
                                    putLong("load_time_ms", loadTime)
                                    putInt("attempt_number", loadAttempt.get())
                                }
                            }
                            firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)

                            AdDebugUtils.logEvent(adUnitId, "onAdImpression", "Banner ad impression (load time: ${loadTime}ms)", true)
                            AdDebugUtils.logPerformance(adUnitId, "AdImpression", loadTime)
                            callback?.onAdImpression()
                        }
                    }

                    override fun onAdPaid(value: AdValue) {
                        handlePaidEvent(adUnitId, value, callback)
                    }
                }

                handleAdLoadSuccess(adUnitId, callback)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                handleAdLoadFailure(adUnitId, adError, callback, "AdMob load failure")
            }
        }
    }
    
    /**
     * Handles successful ad loading with enhanced features.
     */
    private fun handleAdLoadSuccess(adUnitId: String, callback: AdLoadCallback?) {
        ensureMainThread {
            // Update UI
            layBannerAd.removeAllViews()
            adView?.let { adView ->
                val parent = adView.parent as? ViewGroup
                parent?.removeView(adView)
                layBannerAd.addView(adView, centeredBannerLayoutParams())
            }
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = View.GONE
            
            // Update state
            isAdLoading.set(false)
            loadAttempt.set(0) // Reset attempt counter on success
            
            // Make sure the banner is visible after successful load
            visibility = View.VISIBLE
            
            // Start auto-refresh if enabled
            if (autoRefreshEnabled) {
                scheduleNextRefresh()
            }
            
            val loadTime = System.currentTimeMillis() - loadStartTime
            AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Banner ad loaded successfully in ${loadTime}ms", true)
            AdDebugUtils.logPerformance(adUnitId, "AdLoad", loadTime)
            
            callback?.onAdLoaded()
        }
    }
    
    /**
     * Handles ad loading failure with basic retry logic.
     */
    private fun handleAdLoadFailure(
        adUnitId: String,
        error: LoadAdError,
        callback: AdLoadCallback?,
        reason: String,
        isPurchaseBlocked: Boolean = false
    ) {
        ensureMainThread {
            // Update UI - stop shimmer but keep view visible for retries
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = View.GONE
            
            // Update state
            isAdLoading.set(false)
            val attempt = loadAttempt.incrementAndGet()
            
            // Log failure analytics
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putString("ad_error_code", error.code.toString())
                putString("failure_reason", reason)
                if (AdManageKitConfig.enablePerformanceMetrics) {
                    putInt("attempt_number", attempt)
                }
            }
            firebaseAnalytics?.logEvent("ad_failed_to_load", params)
            
            AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "$reason (attempt $attempt)", false)

            // Purchase-blocked loads should fail fast - retrying can never succeed for premium users.
            // Next-Gen SDK's LoadAdError.ErrorCode is a closed enum with no purchase-blocked
            // value, so this is signalled explicitly by the caller rather than inferred from error.code.
            if (isPurchaseBlocked) {
                visibility = View.GONE
                callback?.onFailedToLoad(error)
                return@ensureMainThread
            }

            // Retry logic based on configuration
            if (attempt <= maxRetryAttempts && AdManageKitConfig.autoRetryFailedAds) {
                val retryDelay = if (AdManageKitConfig.enableExponentialBackoff) {
                    minOf(
                        AdManageKitConfig.baseRetryDelay.inWholeMilliseconds * (1L shl (attempt - 1)),
                        AdManageKitConfig.maxRetryDelay.inWholeMilliseconds
                    )
                } else {
                    2000L // Default 2 second delay
                }
                
                Handler(Looper.getMainLooper()).postDelayed({
                    AdDebugUtils.logEvent(adUnitId, "RetryAttempt", "Retrying ad load (attempt $attempt) after ${retryDelay}ms", true)
                    loadBannerInternal(adUnitId, false, CollapsibleBannerPlacement.BOTTOM, callback) // Retry without collapsible
                }, retryDelay)
            } else {
                // Max retries reached or retry disabled - hide the entire banner view
                AdDebugUtils.logEvent(adUnitId, "MaxRetriesReached", "Max retries reached for $adUnitId", false)
                visibility = View.GONE
                callback?.onFailedToLoad(error)
            }
        }
    }
    
    /**
     * Handles paid event with enhanced tracking.
     */
    private fun handlePaidEvent(adUnitId: String, adValue: AdValue, callback: AdLoadCallback?) {
        // Next-Gen SDK's onAdPaid fires on a background thread; wrap to preserve
        // the old SDK's main-thread guarantee for this (possibly UI-touching) callback.
        ensureMainThread {
            val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                putString("ad_format", "banner")
                if (AdManageKitConfig.enablePerformanceMetrics) {
                    putLong("session_time", System.currentTimeMillis() - loadStartTime)
                }
            }
            firebaseAnalytics?.logEvent("ad_paid_event", params)

            AdDebugUtils.logEvent(adUnitId, "onPaidEvent", "Ad revenue: $adValueInStandardUnits ${adValue.currencyCode}", true)

            callback?.onPaidEvent(adValue)
        }
    }

    /**
     * The adaptive ad's width is computed in whole dp, so the rendered ad can
     * be slightly narrower than the container — center it instead of letting
     * it sit at the start edge.
     */
    private fun centeredBannerLayoutParams() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.CENTER_HORIZONTAL
    )

    private fun getAdSize(): AdSize {
        val displayMetrics = DisplayMetrics()
        
        return activityRef?.get()?.let { activity ->
            val windowManager = activity.windowManager
            val adWidthPixels = if (layBannerAd.width > 0) {
                layBannerAd.width.toFloat()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val windowMetrics: WindowMetrics = windowManager.currentWindowMetrics
                    windowMetrics.bounds.width().toFloat()
                } else {
                    @Suppress("DEPRECATION") 
                    val display = windowManager.defaultDisplay
                    display?.getMetrics(displayMetrics)
                    displayMetrics.widthPixels.toFloat()
                }
            }

            val density = resources.displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, adWidth)
        } ?: AdSize.BANNER // Fallback to standard banner size
    }
    
    /**
     * Ensures code runs on the main thread.
     */
    private fun ensureMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            Handler(Looper.getMainLooper()).post(action)
        }
    }
    
    // =================== WATERFALL METHODS ===================

    private fun resolveAdUnit(logicalName: String): (com.i2hammad.admanagekit.core.ad.AdProvider) -> String? = { provider ->
        AdUnitMapping.getAdUnitId(logicalName, provider)
            ?: logicalName.takeIf { provider == AdProvider.ADMOB }
    }

    private fun loadBannerViaWaterfall(
        adUnitId: String,
        collapsible: Boolean = false,
        placement: CollapsibleBannerPlacement = CollapsibleBannerPlacement.BOTTOM,
        callback: AdLoadCallback?
    ) {
        if (!isAdLoading.compareAndSet(false, true)) return

        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            isAdLoading.set(false)
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = View.GONE
            // Next-Gen SDK's LoadAdError.ErrorCode is a closed enum with no
            // purchase-blocked value, so this only carries the message.
            callback?.onFailedToLoad(
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, AdManager.PURCHASED_APP_ERROR_MESSAGE, null)
            )
            return
        }

        visibility = View.VISIBLE
        shimmerFrameLayout.visibility = View.VISIBLE
        shimmerFrameLayout.startShimmer()
        loadStartTime = System.currentTimeMillis()

        bannerWaterfall?.destroy()
        val providers = AdProviderConfig.getBannerChain()

        // Configure collapsible on AdMob providers before loading
        providers.filterIsInstance<com.i2hammad.admanagekit.admob.provider.AdMobBannerProvider>()
            .forEach {
                it.collapsible = collapsible
                it.collapsiblePlacement = placement
            }

        val waterfall = BannerWaterfall(
            providers = providers,
            adUnitResolver = resolveAdUnit(adUnitId)
        )
        bannerWaterfall = waterfall

        waterfall.load(activityRef?.get() ?: context, object : BannerAdProvider.BannerAdCallback {
            override fun onBannerLoaded(bannerView: View) {
                ensureMainThread {
                    layBannerAd.removeAllViews()
                    val parent = bannerView.parent as? ViewGroup
                    parent?.removeView(bannerView)
                    layBannerAd.addView(bannerView, centeredBannerLayoutParams())
                    shimmerFrameLayout.stopShimmer()
                    shimmerFrameLayout.visibility = View.GONE
                    isAdLoading.set(false)
                    loadAttempt.set(0)
                    visibility = View.VISIBLE

                    if (autoRefreshEnabled) { scheduleNextRefresh() }

                    val loadTime = System.currentTimeMillis() - loadStartTime
                    AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Banner waterfall ad loaded in ${loadTime}ms", true)
                    callback?.onAdLoaded()
                }
            }

            override fun onBannerFailedToLoad(error: AdKitAdError) {
                ensureMainThread {
                    shimmerFrameLayout.stopShimmer()
                    shimmerFrameLayout.visibility = View.GONE
                    isAdLoading.set(false)
                    bannerWaterfall = null
                    visibility = View.GONE

                    AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Banner waterfall failed: ${error.message}", false)
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                        putString("ad_error_code", error.code.toString())
                    }
                    firebaseAnalytics?.logEvent("ad_failed_to_load", params)
                    // AdKitAdError.code is an arbitrary SDK-agnostic Int (see AdKitAdError.Companion);
                    // Next-Gen SDK's LoadAdError.ErrorCode is a closed enum, so it's mapped rather
                    // than passed through - see waterfallErrorCodeToLoadAdErrorCode().
                    callback?.onFailedToLoad(
                        LoadAdError(error.waterfallErrorCodeToLoadAdErrorCode(), error.message, null)
                    )
                }
            }

            // Waterfall provider callbacks (e.g. AdMobBannerProvider) may now fire on a
            // background thread under the Next-Gen SDK; wrap dispatch to the (possibly
            // UI-touching) external callback to preserve the old SDK's main-thread guarantee.
            override fun onBannerClicked() { ensureMainThread { callback?.onAdClicked() } }
            override fun onBannerImpression() {
                ensureMainThread {
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    }
                    firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                    callback?.onAdImpression()
                }
            }
            override fun onPaidEvent(adValue: AdKitAdValue) {
                ensureMainThread {
                    val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                        putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                        putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                    }
                    firebaseAnalytics?.logEvent("ad_paid_event", params)
                }
            }
        })
    }

    /**
     * Maps the SDK-agnostic waterfall error code (see [AdKitAdError.Companion]) to the
     * Next-Gen SDK's closed [LoadAdError.ErrorCode] enum, since [AdLoadCallback.onFailedToLoad]
     * requires a real LoadAdError for its network-agnostic AdKitError alias.
     */
    private fun AdKitAdError.waterfallErrorCodeToLoadAdErrorCode(): LoadAdError.ErrorCode = when (code) {
        AdKitAdError.ERROR_CODE_INVALID_REQUEST -> LoadAdError.ErrorCode.INVALID_REQUEST
        AdKitAdError.ERROR_CODE_NETWORK -> LoadAdError.ErrorCode.NETWORK_ERROR
        AdKitAdError.ERROR_CODE_NO_FILL -> LoadAdError.ErrorCode.NO_FILL
        AdKitAdError.ERROR_CODE_TIMEOUT -> LoadAdError.ErrorCode.TIMEOUT
        else -> LoadAdError.ErrorCode.INTERNAL_ERROR
    }

    // =================== AUTO-REFRESH METHODS ===================
    
    /**
     * Enables automatic ad refresh with the specified interval.
     * 
     * @param intervalSeconds Refresh interval in seconds (minimum 30 seconds)
     */
    fun enableAutoRefresh(intervalSeconds: Int = 30) {
        this.autoRefreshEnabled = true
        val configuredInterval = maxOf(intervalSeconds, 30) // Minimum 30 seconds per AdMob policy
        this.customRefreshIntervalSeconds = configuredInterval

        AdDebugUtils.logDebug("BannerAdView", "Auto-refresh enabled with ${configuredInterval}s interval")
        scheduleNextRefresh()
    }
    
    /**
     * Disables automatic ad refresh.
     */
    fun disableAutoRefresh() {
        this.autoRefreshEnabled = false
        stopAutoRefresh()
        
        AdDebugUtils.logDebug("BannerAdView", "Auto-refresh disabled")
    }
    
    private fun startAutoRefresh() {
        if (autoRefreshEnabled) {
            scheduleNextRefresh()
        }
    }
    
    private fun stopAutoRefresh() {
        refreshRunnable?.let { runnable ->
            refreshHandler?.removeCallbacks(runnable)
        }
        refreshRunnable = null
    }
    
    private fun scheduleNextRefresh() {
        if (!autoRefreshEnabled) return
        
        stopAutoRefresh() // Clear any existing scheduled refresh
        
        refreshRunnable = Runnable {
            currentAdUnitId?.let { adUnitId ->
                AdDebugUtils.logEvent(adUnitId, "AutoRefresh", "Auto-refreshing banner ad", true)
                // Reuse the original collapsible configuration so refreshes don't silently
                // convert a collapsible banner into a regular one
                loadBannerInternal(adUnitId, currentCollapsible, currentPlacement, callback)
            }
        }
        
        refreshHandler?.postDelayed(refreshRunnable!!, refreshIntervalSeconds * 1000L)
    }
    
    // =================== LIFECYCLE METHODS ===================
    
    private fun cleanup() {
        try {
            stopAutoRefresh()
            adView?.destroy()
            
            AdDebugUtils.logDebug("BannerAdView", "Cleanup completed for ad: $currentAdUnitId")
        } catch (e: Exception) {
            AdDebugUtils.logError("BannerAdView", "Error during cleanup: ${e.message}", e)
        }
    }

    fun hideAd() {
        adView?.visibility = View.GONE
        shimmerFrameLayout.visibility = View.GONE
    }

    fun showAd() {
        visibility = View.VISIBLE
        if (adView != null && !isAdLoading.get()) {
            // Ad is loaded, show the ad
            adView?.visibility = View.VISIBLE
            shimmerFrameLayout.visibility = View.GONE
        } else {
            // No ad loaded or loading in progress, show shimmer
            adView?.visibility = View.GONE
            shimmerFrameLayout.visibility = View.VISIBLE
            shimmerFrameLayout.startShimmer()
        }
    }

    fun destroyAd() {
        bannerWaterfall?.destroy()
        bannerWaterfall = null
        cleanup()
    }

    fun resumeAd() {
        if (useWaterfall) { bannerWaterfall?.resume(); return }
        // Next-Gen SDK's AdView has no resume() - banner lifecycle is managed
        // internally by the SDK now.
        Log.d("BannerAdView", "resumeAd: no-op under Next-Gen SDK (AdView.resume() no longer exists)")
    }

    fun pauseAd() {
        if (useWaterfall) { bannerWaterfall?.pause(); return }
        // Next-Gen SDK's AdView has no pause() - banner lifecycle is managed
        // internally by the SDK now.
        Log.d("BannerAdView", "pauseAd: no-op under Next-Gen SDK (AdView.pause() no longer exists)")
    }
    
    // =================== NEW CONVENIENCE METHODS ===================
    
    /**
     * Refreshes the current ad manually.
     */
    fun refreshAd() {
        currentAdUnitId?.let { adUnitId ->
            AdDebugUtils.logEvent(adUnitId, "ManualRefresh", "Manual refresh triggered", true)
            // Reuse the original collapsible configuration so manual refreshes don't silently
            // convert a collapsible banner into a regular one
            loadBannerInternal(adUnitId, currentCollapsible, currentPlacement, callback)
        }
    }
    
    /**
     * Checks if an ad is currently loaded and ready to display.
     */
    fun isAdLoaded(): Boolean {
        return adView != null && !isAdLoading.get()
    }
    
    /**
     * Gets current loading state.
     */
    fun isLoading(): Boolean {
        return isAdLoading.get()
    }
    
    /**
     * Gets the current load attempt number.
     */
    fun getCurrentAttempt(): Int {
        return loadAttempt.get()
    }
}
