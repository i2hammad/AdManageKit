package com.i2hammad.admanagekit.utils

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
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
    }

    /**
     * Loads a native ad programmatically and returns the configured NativeAdView.
     *
     * @param activity The activity context
     * @param adUnitId The ad unit ID
     * @param size The native ad size
     * @param useCachedAd Whether to try cached ads first
     * @param callback Callback for ad events
     */
    fun loadNativeAd(
        activity: Activity,
        adUnitId: String,
        size: NativeAdSize,
        useCachedAd: Boolean = true,
        callback: ProgrammaticAdCallback
    ) {
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
            return
        }

        // Try to get cached ad first if requested
        if (useCachedAd && NativeAdManager.enableCachingNativeAds) {
            val cachedAd = NativeAdManager.getCachedNativeAd(adUnitId)
            if (cachedAd != null) {
                val nativeAdView = createNativeAdView(activity, size)
                populateNativeAdView(cachedAd, nativeAdView, size)
                setupPaidEventListener(cachedAd, adUnitId, activity)
                callback.onAdLoaded(nativeAdView, cachedAd)
                return
            }
        }

        // Load new ad from network
        loadNewNativeAd(activity, adUnitId, size, callback)
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
     */
    fun loadNativeAdIntoContainer(
        activity: Activity,
        adUnitId: String,
        size: NativeAdSize,
        container: ViewGroup,
        useCachedAd: Boolean = true,
        callback: ProgrammaticAdCallback? = null
    ) {
        loadNativeAd(activity, adUnitId, size, useCachedAd, object : ProgrammaticAdCallback {
            override fun onAdLoaded(nativeAdView: NativeAdView, nativeAd: NativeAd) {
                container.removeAllViews()
                container.addView(nativeAdView)
                callback?.onAdLoaded(nativeAdView, nativeAd)
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
            parent.findViewById(R.id.native_ad_view) as NativeAdView
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

    private fun loadNewNativeAd(
        activity: Activity,
        adUnitId: String,
        size: NativeAdSize,
        callback: ProgrammaticAdCallback
    ) {
        val firebaseAnalytics = FirebaseAnalytics.getInstance(activity)

        val builder = AdLoader.Builder(activity, adUnitId).forNativeAd { nativeAd ->
            // NOTE: Do NOT cache ad here - it's passed to callback for immediate display
            // Caching is only for preloaded ads that will be shown later via getCachedNativeAd()
            // Ads expire after 1 hour, so caching displayed ads wastes memory

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
                callback.onAdFailedToLoad(adError)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                AdDebugUtils.logEvent(adUnitId, "onAdImpression", "Programmatic native ad impression", true)
                callback.onAdImpression()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                AdDebugUtils.logEvent(adUnitId, "onAdClicked", "Programmatic native ad clicked", true)
                callback.onAdClicked()
            }

            override fun onAdOpened() {
                super.onAdOpened()
                AdDebugUtils.logEvent(adUnitId, "onAdOpened", "Programmatic native ad opened", true)
                callback.onAdOpened()
            }

            override fun onAdClosed() {
                super.onAdClosed()
                AdDebugUtils.logEvent(adUnitId, "onAdClosed", "Programmatic native ad closed", true)
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