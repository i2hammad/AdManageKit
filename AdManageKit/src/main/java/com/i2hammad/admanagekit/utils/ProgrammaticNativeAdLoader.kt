package com.i2hammad.admanagekit.utils

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.admob.AdKitError
import com.i2hammad.admanagekit.admob.AdKitValue
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.NativeAdManager
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AdProviderConfig
import com.i2hammad.admanagekit.core.ad.AdUnitMapping
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.waterfall.NativeWaterfall
import com.i2hammad.admanagekit.core.ad.NativeAdSize as CoreNativeAdSize
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Utility class for loading native ads programmatically without requiring views to be added to layout first.
 *
 * This class provides methods to:
 * - Load native ads directly into ViewGroups
 * - Create native ad views programmatically
 * - Handle different native ad sizes (Small, Medium, Large)
 * - Manage caching and performance optimization
 *
 * @since 2.1.0
 */
object ProgrammaticNativeAdLoader {

    private const val TAG = "ProgrammaticNativeAd"

    /**
     * Handle for an in-flight programmatic native ad load, returned by the load methods.
     *
     * Call [cancel] (e.g. from `onDestroy`/`onDispose`) to stop delivery of further
     * [ProgrammaticAdCallback] events. After cancellation, any ad that still arrives is
     * destroyed instead of delivered, so a load outliving its screen won't push a view into
     * a dead hierarchy or leak the ad. The underlying AdMob `AdLoader` has no cancel API, so
     * cancellation suppresses delivery rather than aborting the network request; a configured
     * waterfall is actively torn down. [cancel] is idempotent and thread-safe.
     */
    class NativeAdLoadHandle internal constructor() {
        private val cancelledFlag = AtomicBoolean(false)
        @Volatile
        private var onCancel: (() -> Unit)? = null

        /** Whether [cancel] has been called. */
        val isCancelled: Boolean get() = cancelledFlag.get()

        /** Registers the teardown action, running it immediately if already cancelled. */
        internal fun setOnCancel(action: () -> Unit) {
            if (cancelledFlag.get()) {
                action()
            } else {
                onCancel = action
            }
        }

        /** Cancels the in-flight load. Idempotent; safe to call from any thread. */
        fun cancel() {
            if (cancelledFlag.compareAndSet(false, true)) {
                val action = onCancel
                onCancel = null
                action?.invoke()
            }
        }
    }

    /**
     * Returns true if [callback]'s concrete class overrides [ProgrammaticAdCallback.onProviderAdLoaded]
     * (rather than inheriting the interface's no-op default). Used to warn integrators whose
     * callback would silently drop non-AdMob waterfall fills. The interface is not part of a
     * concrete class's superclass chain, so the default impl is never seen here.
     */
    private fun overridesProviderHook(callback: ProgrammaticAdCallback): Boolean {
        var cls: Class<*>? = callback.javaClass
        while (cls != null && cls != Any::class.java) {
            if (cls.declaredMethods.any { it.name == "onProviderAdLoaded" && !it.isSynthetic && !it.isBridge }) {
                return true
            }
            cls = cls.superclass
        }
        return false
    }

    /**
     * Native ad size types
     */
    enum class NativeAdSize {
        SMALL,    // NativeBannerSmall equivalent
        MEDIUM,   // NativeBannerMedium equivalent
        LARGE     // NativeLarge equivalent
    }

    /**
     * Callback interface for programmatic native ad loading.
     * Uses SDK-agnostic type aliases for migration compatibility.
     */
    interface ProgrammaticAdCallback {
        fun onAdLoaded(nativeAdView: NativeAdView, nativeAd: NativeAd)
        fun onAdFailedToLoad(error: AdKitError)
        fun onAdClicked()
        fun onAdImpression()
        fun onAdOpened()
        fun onAdClosed()
        fun onPaidEvent(adValue: AdKitValue)

        /**
         * Called when a non-AdMob waterfall provider (e.g. Yandex) supplies a
         * ready-to-display native ad View. AdMob results continue to arrive through the
         * typed [onAdLoaded]; this hook carries providers whose view/ad types are not
         * AdMob's. Default no-op so existing consumers remain source-compatible.
         *
         * Only invoked when a native provider chain is configured via
         * [com.i2hammad.admanagekit.core.ad.AdProviderConfig.setNativeChain]; otherwise the
         * loader stays on the pure-AdMob path and this is never called.
         *
         * @param adView ready-to-display native ad view (already populated and bound)
         * @param nativeAdRef opaque reference to the underlying ad — hold it to prevent GC
         */
        fun onProviderAdLoaded(adView: View, nativeAdRef: Any) {}
    }

    /**
     * Loads a native ad programmatically and returns the configured NativeAdView.
     *
     * @param activity The activity context
     * @param adUnitId The ad unit ID
     * @param size The native ad size
     * @param useCachedAd Whether to try cached ads first
     * @param callback Callback for ad events
     * @return a [NativeAdLoadHandle] whose [NativeAdLoadHandle.cancel] stops delivery of
     *         further callbacks (call it when the host screen is destroyed)
     */
    fun loadNativeAd(
        activity: Activity,
        adUnitId: String,
        size: NativeAdSize,
        useCachedAd: Boolean = true,
        callback: ProgrammaticAdCallback
    ): NativeAdLoadHandle {
        val handle = NativeAdLoadHandle()

        // Check if user has purchased (ads should be disabled)
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            callback.onAdFailedToLoad(
                AdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN
                )
            )
            return handle
        }

        // The cache holds AdMob NativeAds, so an AdMob cache hit may only short-circuit the
        // waterfall when AdMob is the first provider in the chain (or no chain is
        // configured). Otherwise serving a cached AdMob ad would violate the configured
        // provider order (e.g. a Yandex-first chain).
        val nativeChain = AdProviderConfig.getNativeChain()
        val adMobIsFirst = nativeChain.isEmpty() || nativeChain.first().provider == AdProvider.ADMOB
        if (useCachedAd && NativeAdManager.enableCachingNativeAds && adMobIsFirst) {
            val cachedAd = NativeAdManager.getCachedNativeAd(adUnitId)
            if (cachedAd != null) {
                if (handle.isCancelled) {
                    cachedAd.destroy()
                    return handle
                }
                val nativeAdView = createNativeAdView(activity, size)
                populateNativeAdView(cachedAd, nativeAdView, size)
                setupPaidEventListener(cachedAd, adUnitId, activity)
                callback.onAdLoaded(nativeAdView, cachedAd)
                return handle
            }
        }

        // If a native provider chain is configured, load through the waterfall so AdMob
        // no-fill falls back to other providers (e.g. Yandex). Otherwise stay on the
        // pure-AdMob path.
        if (nativeChain.isNotEmpty()) {
            loadViaWaterfall(activity, adUnitId, size, callback, handle)
            return handle
        }

        // Load new ad from network
        loadNewNativeAd(activity, adUnitId, size, callback, handle)
        return handle
    }

    /**
     * Loads a native ad through the configured provider waterfall.
     *
     * AdMob results are delivered via the typed [ProgrammaticAdCallback.onAdLoaded]
     * (the ref is a [NativeAd] and the view a [NativeAdView]); non-AdMob results (e.g.
     * Yandex) are delivered via [ProgrammaticAdCallback.onProviderAdLoaded]. Firebase
     * analytics events are logged here to match the pure-AdMob path, since providers
     * only forward callbacks without logging.
     */
    private fun loadViaWaterfall(
        activity: Activity,
        adUnitId: String,
        size: NativeAdSize,
        callback: ProgrammaticAdCallback,
        handle: NativeAdLoadHandle
    ) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(activity)
        val sizeHint = when (size) {
            NativeAdSize.SMALL -> CoreNativeAdSize.SMALL
            NativeAdSize.MEDIUM -> CoreNativeAdSize.MEDIUM
            NativeAdSize.LARGE -> CoreNativeAdSize.LARGE
        }

        val waterfall = NativeWaterfall(
            providers = AdProviderConfig.getNativeChain(),
            adUnitResolver = { provider ->
                AdUnitMapping.getAdUnitId(adUnitId, provider)
                    ?: adUnitId.takeIf { provider == AdProvider.ADMOB }
            }
        )
        // Cancelling tears down the waterfall so in-flight provider attempts are dropped.
        handle.setOnCancel { waterfall.destroy() }

        waterfall.load(activity, object : NativeAdProvider.NativeAdCallback {
            override fun onNativeAdLoaded(adView: View, nativeAdRef: Any) {
                if (handle.isCancelled) {
                    // Late fill after cancellation: don't deliver. Destroy AdMob ads to avoid
                    // a leak; non-AdMob views are owned by their provider.
                    (nativeAdRef as? NativeAd)?.destroy()
                    return
                }
                if (nativeAdRef is NativeAd && adView is NativeAdView) {
                    callback.onAdLoaded(adView, nativeAdRef)
                } else {
                    // Non-AdMob fill is delivered only via onProviderAdLoaded. If the caller
                    // didn't override it, the ad would silently never be displayed — warn so
                    // the integrator can fix it (or use loadNativeAdIntoContainer).
                    if (!overridesProviderHook(callback)) {
                        Log.w(
                            TAG,
                            "A non-AdMob native ad loaded via the waterfall, but the callback does " +
                                "not override onProviderAdLoaded(); the ad will not be displayed. " +
                                "Implement onProviderAdLoaded() or use loadNativeAdIntoContainer()."
                        )
                    }
                    callback.onProviderAdLoaded(adView, nativeAdRef)
                }
            }

            override fun onNativeAdFailedToLoad(error: AdKitAdError) {
                if (handle.isCancelled) return
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", error.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", error.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)
                callback.onAdFailedToLoad(AdError(error.code, error.message, error.domain))
            }

            override fun onNativeAdClicked() {
                if (handle.isCancelled) return
                callback.onAdClicked()
            }

            override fun onNativeAdOpened() {
                if (handle.isCancelled) return
                callback.onAdOpened()
            }

            override fun onNativeAdClosed() {
                if (handle.isCancelled) return
                callback.onAdClosed()
            }

            override fun onNativeAdImpression() {
                if (handle.isCancelled) return
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                callback.onAdImpression()
            }

            override fun onPaidEvent(adValue: AdKitAdValue) {
                if (handle.isCancelled) return
                // The typed onPaidEvent(AdValue) can't be built from the SDK-agnostic value,
                // so log analytics directly here (consistent with NativeTemplateView).
                val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                    putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                }
                firebaseAnalytics.logEvent("ad_paid_event", params)
            }
        }, sizeHint = sizeHint)
    }

    /**
     * Loads a native ad and automatically adds it to the specified ViewGroup.
     *
     * @param activity The activity context
     * @param adUnitId The ad unit ID
     * @param size The native ad size
     * @param container The ViewGroup to add the ad to
     * @param useCachedAd Whether to try cached ads first
     * @param callback Optional callback for ad events
     * @return a [NativeAdLoadHandle] whose [NativeAdLoadHandle.cancel] stops the load
     */
    fun loadNativeAdIntoContainer(
        activity: Activity,
        adUnitId: String,
        size: NativeAdSize,
        container: ViewGroup,
        useCachedAd: Boolean = true,
        callback: ProgrammaticAdCallback? = null
    ): NativeAdLoadHandle {
        return loadNativeAd(activity, adUnitId, size, useCachedAd, object : ProgrammaticAdCallback {
            override fun onAdLoaded(nativeAdView: NativeAdView, nativeAd: NativeAd) {
                // Destroy the ad previously displayed in this container (if any) before
                // replacing it, otherwise repeated loads leak NativeAds and their media.
                destroyTrackedContainerAd(container, keep = nativeAd)
                container.removeAllViews()
                container.addView(nativeAdView)
                container.setTag(R.id.admanagekit_native_ad_tag, nativeAd)
                callback?.onAdLoaded(nativeAdView, nativeAd)
            }

            override fun onProviderAdLoaded(adView: View, nativeAdRef: Any) {
                // Non-AdMob (e.g. Yandex) waterfall result: attach the provider's view.
                // Detach from any temporary parent first to avoid an IllegalStateException.
                destroyTrackedContainerAd(container, keep = null)
                (adView.parent as? ViewGroup)?.removeView(adView)
                container.removeAllViews()
                container.addView(adView)
                // Provider views own their own ad ref; nothing AdMob-destroyable to track.
                container.setTag(R.id.admanagekit_native_ad_tag, null)
                callback?.onProviderAdLoaded(adView, nativeAdRef)
            }

            override fun onAdFailedToLoad(error: AdError) {
                callback?.onAdFailedToLoad(error)
            }

            override fun onAdClicked() {
                callback?.onAdClicked()
            }

            override fun onAdImpression() {
                callback?.onAdImpression()
            }

            override fun onAdOpened() {
                callback?.onAdOpened()
            }

            override fun onAdClosed() {
                callback?.onAdClosed()
            }

            override fun onPaidEvent(adValue: AdKitValue) {
                callback?.onPaidEvent(adValue)
            }
        })
    }

    /**
     * Destroys the [NativeAd] currently tracked on [container] (set by
     * [loadNativeAdIntoContainer]) unless it is the same instance as [keep], then clears the
     * tag. No-op when nothing is tracked. Prevents leaking the previously displayed ad when
     * a container's content is replaced.
     */
    private fun destroyTrackedContainerAd(container: ViewGroup, keep: NativeAd?) {
        val previous = container.getTag(R.id.admanagekit_native_ad_tag) as? NativeAd
        if (previous != null && previous !== keep) {
            previous.destroy()
        }
        container.setTag(R.id.admanagekit_native_ad_tag, null)
    }

    /**
     * Creates a pre-configured native ad view for the specified size.
     *
     * @param context The context
     * @param size The native ad size
     * @return Configured NativeAdView
     */
    fun createNativeAdView(context: Context, size: NativeAdSize): NativeAdView {
        val layoutRes = when (size) {
            NativeAdSize.SMALL -> R.layout.layout_native_banner_small
            NativeAdSize.MEDIUM -> R.layout.layout_native_banner_medium
            NativeAdSize.LARGE -> R.layout.layout_native_large
        }

        // Handle LARGE layout which uses <merge> tag (requires parent container)
        val nativeAdView = if (size == NativeAdSize.LARGE) {
            // Create a temporary parent container for merge inflation
            val parent = FrameLayout(context)
            LayoutInflater.from(context).inflate(layoutRes, parent, true)

            // Find the NativeAdView inside the merged content
            val largeAdView = parent.findViewById(R.id.native_ad_view) as NativeAdView
            // Detach from the temporary inflation hierarchy, otherwise attaching the
            // returned view elsewhere crashes with "specified child already has a parent"
            (largeAdView.parent as? ViewGroup)?.removeView(largeAdView)
            // The large layout declares the NativeAdView as gone until populated
            largeAdView.visibility = android.view.View.VISIBLE
            largeAdView
        } else {
            // SMALL and MEDIUM layouts have NativeAdView as root
            LayoutInflater.from(context).inflate(layoutRes, null) as NativeAdView
        }

        // Configure the view references based on size
        when (size) {
            NativeAdSize.SMALL, NativeAdSize.MEDIUM -> {
                nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
                nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
                nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
                nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
                nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)
            }
            NativeAdSize.LARGE -> {
                nativeAdView.headlineView = nativeAdView.findViewById(R.id.primary)
                nativeAdView.bodyView = nativeAdView.findViewById(R.id.secondary)
                nativeAdView.callToActionView = nativeAdView.findViewById(R.id.cta)
                nativeAdView.iconView = nativeAdView.findViewById(R.id.icon)
                nativeAdView.advertiserView = nativeAdView.findViewById(R.id.tertiary)
                nativeAdView.mediaView = nativeAdView.findViewById(R.id.media_view)
            }
        }

        return nativeAdView
    }

    /**
     * Loads a raw NativeAd WITHOUT inflating or binding any NativeAdView.
     *
     * Intended for preload/cache flows (e.g. [NativeAdManager.preloadNativeAd]) where the
     * ad will be bound to a real view later when displayed. Binding a throwaway view at
     * preload time would leave the ad registered to an orphaned, never-attached
     * NativeAdView for its whole cache lifetime.
     *
     * Analytics events (ad_failed_to_load, ad_impression, ad_paid_event) are kept
     * consistent with the regular loading path.
     *
     * @param activity The activity context
     * @param adUnitId The ad unit ID
     * @param onLoaded Called with the raw NativeAd when loaded
     * @param onFailed Called when the load fails
     */
    fun loadRawNativeAd(
        activity: Activity,
        adUnitId: String,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (AdError) -> Unit
    ) {
        // Check if user has purchased (ads should be disabled)
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            onFailed(
                AdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN
                )
            )
            return
        }

        val firebaseAnalytics = FirebaseAnalytics.getInstance(activity)

        val builder = AdLoader.Builder(activity, adUnitId).forNativeAd { nativeAd ->
            // No view inflation or setNativeAd() here — the ad is handed off raw
            // and bound to a real NativeAdView when it is actually displayed
            setupPaidEventListener(nativeAd, adUnitId, activity)
            onLoaded(nativeAd)
        }.withAdListener(object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Raw native ad loaded successfully", true)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Raw native ad failed: ${adError.message}", false)

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", adError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", adError.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)
                onFailed(adError)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                AdDebugUtils.logEvent(adUnitId, "onAdImpression", "Raw native ad impression", true)
            }
        })

        builder.build().loadAd(AdRequest.Builder().build())
    }

    private fun loadNewNativeAd(
        activity: Activity,
        adUnitId: String,
        size: NativeAdSize,
        callback: ProgrammaticAdCallback,
        handle: NativeAdLoadHandle
    ) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(activity)

        val builder = AdLoader.Builder(activity, adUnitId).forNativeAd { nativeAd ->
            // NOTE: Do NOT cache ad here - it's passed to callback for immediate display
            // Caching is only for preloaded ads that will be shown later via getCachedNativeAd()
            // Ads expire after 1 hour, so caching displayed ads wastes memory

            // Cancelled while the request was in flight: drop and destroy to avoid pushing a
            // view into a dead hierarchy or leaking the ad.
            if (handle.isCancelled) {
                nativeAd.destroy()
                return@forNativeAd
            }

            val nativeAdView = createNativeAdView(activity, size)
            populateNativeAdView(nativeAd, nativeAdView, size)
            setupPaidEventListener(nativeAd, adUnitId, activity)

            callback.onAdLoaded(nativeAdView, nativeAd)

        }.withAdListener(object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "Programmatic native ad loaded successfully", true)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "Programmatic native ad failed: ${adError.message}", false)

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", adError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", adError.message)
                    }
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)
                if (handle.isCancelled) return
                callback.onAdFailedToLoad(adError)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                AdDebugUtils.logEvent(adUnitId, "onAdImpression", "Programmatic native ad impression", true)
                if (handle.isCancelled) return
                callback.onAdImpression()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                AdDebugUtils.logEvent(adUnitId, "onAdClicked", "Programmatic native ad clicked", true)
                if (handle.isCancelled) return
                callback.onAdClicked()
            }

            override fun onAdOpened() {
                super.onAdOpened()
                AdDebugUtils.logEvent(adUnitId, "onAdOpened", "Programmatic native ad opened", true)
                if (handle.isCancelled) return
                callback.onAdOpened()
            }

            override fun onAdClosed() {
                super.onAdClosed()
                AdDebugUtils.logEvent(adUnitId, "onAdClosed", "Programmatic native ad closed", true)
                if (handle.isCancelled) return
                callback.onAdClosed()
            }
        })

        builder.build().loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView, size: NativeAdSize) {
        // Populate basic fields
        nativeAdView.headlineView?.let { headlineView ->
            (headlineView as TextView).text = nativeAd.headline ?: ""
        }
        nativeAdView.bodyView?.let { bodyView ->
            (bodyView as TextView).text = nativeAd.body ?: ""
        }
        nativeAdView.callToActionView?.let { callToActionView ->
            (callToActionView as TextView).text = nativeAd.callToAction ?: ""
        }
        nativeAdView.iconView?.let { iconView ->
            val icon = nativeAd.icon
            if (icon == null) {
                iconView.visibility = android.view.View.INVISIBLE
            } else {
                (iconView as ImageView).setImageDrawable(icon.drawable)
                iconView.visibility = android.view.View.VISIBLE
            }
        }
        nativeAdView.advertiserView?.let { advertiserView ->
            val advertiser = nativeAd.advertiser
            if (advertiser == null) {
                advertiserView.visibility = android.view.View.INVISIBLE
            } else {
                (advertiserView as TextView).text = advertiser
                advertiserView.visibility = android.view.View.VISIBLE
            }
        }

        // Populate additional fields for larger formats
        if (size == NativeAdSize.LARGE) {
            nativeAdView.storeView?.let { storeView ->
                val store = nativeAd.store
                if (store == null) {
                    storeView.visibility = android.view.View.INVISIBLE
                } else {
                    (storeView as TextView).text = store
                    storeView.visibility = android.view.View.VISIBLE
                }
            }
            nativeAdView.priceView?.let { priceView ->
                val price = nativeAd.price
                if (price == null) {
                    priceView.visibility = android.view.View.INVISIBLE
                } else {
                    (priceView as TextView).text = price
                    priceView.visibility = android.view.View.VISIBLE
                }
            }
            nativeAdView.starRatingView?.let { starRatingView ->
                val starRating = nativeAd.starRating
                if (starRating == null) {
                    starRatingView.visibility = android.view.View.INVISIBLE
                } else {
                    starRatingView.visibility = android.view.View.VISIBLE
                }
            }
        }

        nativeAdView.setNativeAd(nativeAd)
    }

    private fun setupPaidEventListener(nativeAd: NativeAd, adUnitId: String, activity: Activity) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(activity)
        nativeAd.setOnPaidEventListener { adValue ->
            val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            }
            firebaseAnalytics.logEvent("ad_paid_event", params)
        }
    }
}