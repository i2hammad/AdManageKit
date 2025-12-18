package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.databinding.LayoutNativeBannerMediumPreviewBinding

class NativeBannerMedium @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutNativeBannerMediumPreviewBinding =
        LayoutNativeBannerMediumPreviewBinding.inflate(LayoutInflater.from(context), this)
    private var adUnitId: String = "unknown"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    var callback: AdLoadCallback? = null

    // =================== DEPRECATED METHODS (use loadingStrategy instead) ===================

    @Deprecated(
        message = "Use loadNativeBannerAd with loadingStrategy parameter instead",
        replaceWith = ReplaceWith("loadNativeBannerAd(activity, adNativeBanner, loadingStrategy = AdLoadingStrategy.HYBRID)")
    )
    fun loadNativeBannerAd(
        activity: Activity,
        adNativeBanner: String,
        useCachedAd: Boolean = false
    ) {
        loadAd(activity, adNativeBanner, useCachedAd, callback, null)
    }

    @Deprecated(
        message = "Use loadNativeBannerAd with loadingStrategy parameter instead",
        replaceWith = ReplaceWith("loadNativeBannerAd(activity, adNativeBanner, adCallBack, loadingStrategy = AdLoadingStrategy.HYBRID)")
    )
    fun loadNativeBannerAd(
        activity: Activity,
        adNativeBanner: String,
        useCachedAd: Boolean = false,
        adCallBack: AdLoadCallback
    ) {
        this.callback = adCallBack
        loadAd(activity, adNativeBanner, useCachedAd, adCallBack, null)
    }

    // =================== NEW METHODS (recommended) ===================

    /**
     * Load native banner ad using default global strategy from AdManageKitConfig.nativeLoadingStrategy
     */
    fun loadNativeBannerAd(
        activity: Activity,
        adNativeBanner: String
    ) {
        loadAd(activity, adNativeBanner, false, callback, null)
    }

    /**
     * Load native banner ad with custom loading strategy override
     *
     * @param loadingStrategy Strategy to use (ON_DEMAND or HYBRID). Note: ONLY_CACHE is not supported for native ads.
     */
    fun loadNativeBannerAd(
        activity: Activity,
        adNativeBanner: String,
        loadingStrategy: com.i2hammad.admanagekit.config.AdLoadingStrategy
    ) {
        loadAd(activity, adNativeBanner, false, callback, loadingStrategy)
    }

    /**
     * Load native banner ad with custom loading strategy and callback
     *
     * @param loadingStrategy Strategy to use (ON_DEMAND or HYBRID). Note: ONLY_CACHE is not supported for native ads.
     */
    fun loadNativeBannerAd(
        activity: Activity,
        adNativeBanner: String,
        adCallBack: AdLoadCallback,
        loadingStrategy: com.i2hammad.admanagekit.config.AdLoadingStrategy? = null
    ) {
        this.callback = adCallBack
        loadAd(activity, adNativeBanner, false, adCallBack, loadingStrategy)
    }

    private fun loadAd(
        context: Context,
        adUnitId: String,
        useCachedAd: Boolean,
        callback: AdLoadCallback?,
        loadingStrategy: com.i2hammad.admanagekit.config.AdLoadingStrategy? = null
    ) {
        this.adUnitId = adUnitId
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative
        val purchaseProvider = BillingConfig.getPurchaseProvider()

        if (purchaseProvider.isPurchased()) {
            shimmerFrameLayout.visibility = View.GONE
            callback?.onFailedToLoad(
                AdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN
                )
            )
            return
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        // Use enhanced integration manager for smart caching
        if (context is Activity) {
            val screenKey = "${context.javaClass.simpleName}_MEDIUM"

            com.i2hammad.admanagekit.utils.NativeAdIntegrationManager.loadNativeAdWithCaching(
                activity = context,
                baseAdUnitId = adUnitId,
                screenType = com.i2hammad.admanagekit.utils.NativeAdIntegrationManager.ScreenType.MEDIUM,
                useCachedAd = useCachedAd,
                loadingStrategy = loadingStrategy,
                callback = object : AdLoadCallback() {
                    override fun onAdLoaded() {
                        // Ad was served from cache or newly loaded
                        callback?.onAdLoaded()
                    }

                    override fun onFailedToLoad(error: AdError?) {
                        // Hide shimmer and container when ad fails to load (e.g., ONLY_CACHE with no cache)
                        binding.shimmerContainerNative.visibility = View.GONE
                        binding.adContainer.visibility = View.GONE
                        callback?.onFailedToLoad(error)
                    }

                    override fun onAdClicked() {
                        callback?.onAdClicked()
                    }

                    override fun onAdClosed() {
                        callback?.onAdClosed()
                    }

                    override fun onAdImpression() {
                        callback?.onAdImpression()
                    }

                    override fun onAdOpened() {
                        callback?.onAdOpened()
                    }

                    override fun onPaidEvent(adValue: com.google.android.gms.ads.AdValue) {
                        callback?.onPaidEvent(adValue)
                    }
                }
            ) { enhancedAdUnitId, enhancedCallback ->
                // Load new ad if cache miss
                loadNewAdInternal(context, enhancedAdUnitId, enhancedCallback, useCachedAd)
            }

            // 🔧 FIX: Check if integration manager found a cached ad and display it
            val temporaryCachedAd = com.i2hammad.admanagekit.utils.NativeAdIntegrationManager.getAndClearTemporaryCachedAd(screenKey)
            if (temporaryCachedAd != null) {
                AdDebugUtils.logEvent(adUnitId, "foundCachedAd", "Found cached ad from integration manager - displaying it", true)
                displayAd(temporaryCachedAd)
            }
        } else {
            // Fallback to original loading for non-Activity contexts
            loadNewAdInternal(context, adUnitId, callback, useCachedAd)
        }
    }
    
    /**
     * Internal method to load a new ad from the network.
     */
    private fun loadNewAdInternal(
        context: Context,
        adUnitId: String,
        callback: AdLoadCallback?,
        useCachedAd: Boolean = false
    ) {
        AdDebugUtils.logEvent(adUnitId, "loadNewAdInternal", "Loading new ad - useCachedAd: $useCachedAd", true)
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative
        shimmerFrameLayout.visibility = View.VISIBLE
        val adPlaceholder: FrameLayout = binding.flAdplaceholder

        AdDebugUtils.logDebug("ViewStates", "Before loading - Root: ${binding.root.visibility}, Placeholder: ${adPlaceholder.visibility}, Children: ${adPlaceholder.childCount}")
        val nativeAdView = LayoutInflater.from(context)
            .inflate(R.layout.layout_native_banner_medium, null) as NativeAdView
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)
        nativeAdView.adChoicesView = nativeAdView.findViewById(R.id.ad_choices_view)

        val builder = AdLoader.Builder(context, adUnitId).forNativeAd { nativeAd ->
            AdDebugUtils.logEvent(adUnitId, "forNativeAd", "Ad callback triggered - Headline: '${nativeAd.headline}', CTA: '${nativeAd.callToAction}', AdChoices: ${if (nativeAd.adChoicesInfo == null) "null" else "present"}", true)

            adPlaceholder.removeAllViews()
            AdDebugUtils.logDebug("AdDisplay", "Cleared placeholder views")

            adPlaceholder.addView(nativeAdView)
            AdDebugUtils.logDebug("AdDisplay", "Added NativeAdView to placeholder")

            binding.root.visibility = View.VISIBLE
            adPlaceholder.visibility = View.VISIBLE
            AdDebugUtils.logDebug("AdDisplay", "Set views visible")

            // NOTE: Do NOT cache ad here - it's being displayed immediately
            // Caching is only for preloaded ads that will be shown later
            // Ads expire after 1 hour, so caching displayed ads wastes memory

            AdDebugUtils.logDebug("AdDisplay", "About to populate native ad view")
            populateNativeAdView(nativeAd, nativeAdView)

            AdDebugUtils.logDebug("ViewStates", "After population - Root: ${binding.root.visibility}, Placeholder: ${adPlaceholder.visibility}, Children: ${adPlaceholder.childCount}, Attached: ${nativeAdView.parent != null}")

            shimmerFrameLayout.visibility = View.GONE
            AdDebugUtils.logEvent(adUnitId, "displayComplete", "Ad display pipeline completed successfully", true)

            nativeAd.setOnPaidEventListener { adValue ->
                val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                    putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                }
                firebaseAnalytics!!.logEvent("ad_paid_event", params)
            }
        }.withAdListener(object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "NativeBannerMedium loaded successfully", true)
                callback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Try to use cached ad if available and not explicitly requesting a new ad
                if (NativeAdManager.enableCachingNativeAds && !useCachedAd) {
                    // First try specific ad unit, then fallback to any cached ad
                    val cachedAd = NativeAdManager.getCachedNativeAd(adUnitId, enableFallbackToAnyAd = true)
                    if (cachedAd != null) {
                        AdDebugUtils.logEvent(adUnitId, "usedFallbackCache", "Used fallback cached ad after network failure", true)
                        displayAd(cachedAd)
                        callback?.onAdLoaded()
                        return
                    }
                }

                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "NativeBannerMedium failed: ${adError.message}", false)
                adPlaceholder.visibility = View.GONE
                shimmerFrameLayout.visibility = View.GONE

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", adError.code.toString())
                    if (AdManageKitConfig.enablePerformanceMetrics) {
                        putString("error_message", adError.message)
                    }
                }
                firebaseAnalytics?.logEvent("ad_failed_to_load", params)
                callback?.onFailedToLoad(adError)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                AdDebugUtils.logEvent(adUnitId, "onAdImpression", "NativeBannerMedium impression", true)
                callback?.onAdImpression()
            }

            override fun onAdClosed() {
                super.onAdClosed()
                AdDebugUtils.logEvent(adUnitId, "onAdClosed", "NativeBannerMedium closed", true)
                callback?.onAdClosed()
            }

            override fun onAdOpened() {
                super.onAdOpened()
                AdDebugUtils.logEvent(adUnitId, "onAdOpened", "NativeBannerMedium opened", true)
                callback?.onAdOpened()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                AdDebugUtils.logEvent(adUnitId, "onAdClicked", "NativeBannerMedium clicked", true)
                callback?.onAdClicked()
            }
        })

        builder.build().loadAd(AdRequest.Builder().build())
    }

    fun setAdManagerCallback(callback: AdLoadCallback) {
        this.callback = callback
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
        AdDebugUtils.logDebug("AdPopulate", "Populating native ad view - AdChoices: ${nativeAd.adChoicesInfo ?: "null (normal for test ads)"}")

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
                iconView.visibility = View.INVISIBLE
            } else {
                (iconView as ImageView).setImageDrawable(icon.drawable)
                iconView.visibility = View.VISIBLE
            }
        }

        nativeAdView.advertiserView?.let { advertiserView ->
            val advertiser = nativeAd.advertiser
            if (advertiser == null) {
                advertiserView.visibility = View.GONE
            } else {
                (advertiserView as TextView).text = advertiser
                advertiserView.visibility = View.VISIBLE
            }
        }

        nativeAdView.adChoicesView?.let { adChoicesView ->
            val adChoicesInfo = nativeAd.adChoicesInfo
            if (adChoicesInfo == null) {
                // Hide AdChoices view for test ads or ads without choices
                adChoicesView.visibility = View.GONE
                AdDebugUtils.logDebug("AdChoices", "Hidden - not available for this ad (normal for test ads)")
            } else {
                adChoicesView.visibility = View.VISIBLE
                AdDebugUtils.logDebug("AdChoices", "Displayed successfully")
            }
        }

        // Set native ad and ensure visibility in next frame
        AdDebugUtils.logDebug("AdBind", "Setting native ad on NativeAdView")
        nativeAdView.post {
            try {
                nativeAdView.setNativeAd(nativeAd)
                AdDebugUtils.logEvent(adUnitId, "setNativeAd", "Successfully called setNativeAd()", true)

                binding.shimmerContainerNative.visibility = View.GONE
                binding.root.visibility = View.VISIBLE
                binding.flAdplaceholder.visibility = View.VISIBLE

                AdDebugUtils.logEvent(adUnitId, "adPopulated", "Native ad populated and displayed successfully", true)
                AdDebugUtils.logDebug("FinalState", "Parent: ${nativeAdView.parent != null}, Headline: '${(nativeAdView.headlineView as? TextView)?.text}', Bounds: ${binding.root.width}x${binding.root.height}")

            } catch (e: Exception) {
                AdDebugUtils.logEvent(adUnitId, "setNativeAdError", "Error setting native ad: ${e.message}", false)
                e.printStackTrace()
            }
        }
    }

    fun displayAd(preloadedAd: NativeAd) {
        AdDebugUtils.logEvent(adUnitId, "displayCachedAd", "Displaying cached/preloaded ad", true)
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative
        val purchaseProvider = BillingConfig.getPurchaseProvider()

        AdDebugUtils.logDebug("CacheDisplay", "Initial check - Purchased: ${purchaseProvider.isPurchased()}, Root: ${binding.root.visibility}, Children: ${binding.flAdplaceholder.childCount}")

        if (purchaseProvider.isPurchased()) {
            AdDebugUtils.logEvent(adUnitId, "adBlocked", "User has purchased - hiding ad", true)
            shimmerFrameLayout.visibility = View.GONE
            callback?.onFailedToLoad(
                AdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN
                )
            )
            return
        }

        AdDebugUtils.logDebug("CacheDisplay", "Proceeding with cached ad display")
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        if (!displayCachedAdSafely(preloadedAd)) {
            AdDebugUtils.logEvent(adUnitId, "cacheDisplayFailed", "Failed to display cached ad", false)
            callback?.onFailedToLoad(AdError(0, "Failed to display cached ad", "AdManageKit"))
        } else {
            AdDebugUtils.logEvent(adUnitId, "cacheDisplaySuccess", "Successfully initiated cached ad display", true)
        }
    }

    private fun displayCachedAdSafely(cachedAd: NativeAd): Boolean {
        return try {
            val adPlaceholder: FrameLayout = binding.flAdplaceholder

            // Clear any existing views first
            adPlaceholder.removeAllViews()

            // Create fresh NativeAdView for cached ad
            val nativeAdView = LayoutInflater.from(context)
                .inflate(R.layout.layout_native_banner_medium, null) as NativeAdView

            // Setup all view references
            setupNativeAdViewReferences(nativeAdView)

            // Add to container first
            adPlaceholder.addView(nativeAdView)
            adPlaceholder.visibility = View.VISIBLE

            // Setup paid event listener
            cachedAd.setOnPaidEventListener { adValue ->
                val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                    putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                }
                firebaseAnalytics!!.logEvent("ad_paid_event", params)
            }

            // Then populate - this order is important
            populateNativeAdView(cachedAd, nativeAdView)

            // Verify the ad is actually visible
            verifyAdVisibility(nativeAdView, cachedAd)
        } catch (e: Exception) {
            AdDebugUtils.logEvent(adUnitId, "cacheDisplayException", "Failed to display cached ad: ${e.message}", false)
            false
        }
    }

    private fun setupNativeAdViewReferences(nativeAdView: NativeAdView) {
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)
        nativeAdView.adChoicesView = nativeAdView.findViewById(R.id.ad_choices_view)
    }

    private fun verifyAdVisibility(nativeAdView: NativeAdView, nativeAd: NativeAd): Boolean {
        // Check if essential elements are populated
        val headline = (nativeAdView.headlineView as? TextView)?.text
        val hasVisibleContent = !headline.isNullOrEmpty()

        if (!hasVisibleContent) {
            AdDebugUtils.logEvent(adUnitId, "cacheContentMissing", "Cached ad has no visible content - headline: '$headline'", false)
            return false
        }

        AdDebugUtils.logDebug("DisplayVerify", "Verification - Headline: '${nativeAd.headline}', Body: '${nativeAd.body}', CTA: '${nativeAd.callToAction}', Container visible: ${binding.flAdplaceholder.visibility == View.VISIBLE}, Root visible: ${binding.root.visibility == View.VISIBLE}")

        return true
    }

    fun hideAd() {
        binding.root.visibility = View.GONE
    }

    fun showAd() {
        binding.root.visibility = View.VISIBLE
    }
}