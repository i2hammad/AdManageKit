package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
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
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AdProviderConfig
import com.i2hammad.admanagekit.core.ad.AdUnitMapping
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdSize
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.waterfall.NativeWaterfall
import com.i2hammad.admanagekit.databinding.LayoutNativeBannerSmallPreviewBinding

class NativeBannerSmall @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutNativeBannerSmallPreviewBinding =
        LayoutNativeBannerSmallPreviewBinding.inflate(LayoutInflater.from(context), this)

    private val TAG = "NativeAds"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private lateinit var adUnitId: String
    var callback: AdLoadCallback? = null

    // Waterfall support
    private var nativeWaterfall: NativeWaterfall? = null
    private var waterfallNativeAdRef: Any? = null
    private val useWaterfall: Boolean
        get() = AdProviderConfig.getNativeChain().isNotEmpty()

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

        if (useWaterfall) { loadViaWaterfall(context, adUnitId, callback); return }

        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            shimmerFrameLayout.visibility = GONE
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
            val screenKey = "${context.javaClass.simpleName}_SMALL"

            com.i2hammad.admanagekit.utils.NativeAdIntegrationManager.loadNativeAdWithCaching(
                activity = context,
                baseAdUnitId = adUnitId,
                screenType = com.i2hammad.admanagekit.utils.NativeAdIntegrationManager.ScreenType.SMALL,
                useCachedAd = useCachedAd,
                loadingStrategy = loadingStrategy,
                callback = object : AdLoadCallback() {
                    override fun onAdLoaded() {
                        // Ad was served from cache or newly loaded
                        callback?.onAdLoaded()
                    }

                    override fun onFailedToLoad(error: AdError?) {
                        // Hide shimmer and container when ad fails to load (e.g., ONLY_CACHE with no cache)
                        binding.shimmerContainerNative.visibility = GONE
                        binding.root.visibility = GONE
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
        val nativeAdView = LayoutInflater.from(context)
            .inflate(R.layout.layout_native_banner_small, null) as NativeAdView
        val adPlaceholder: FrameLayout = binding.flAdplaceholder
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative

        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)

        val builder = AdLoader.Builder(context, adUnitId).forNativeAd { nativeAd ->
            adPlaceholder.removeAllViews()
            adPlaceholder.addView(nativeAdView)
            binding.root.visibility = VISIBLE
            adPlaceholder.visibility = VISIBLE

            // NOTE: Do NOT cache ad here - it's being displayed immediately
            // Caching is only for preloaded ads that will be shown later
            // Ads expire after 1 hour, so caching displayed ads wastes memory

            populateNativeAdView(nativeAd, nativeAdView)
            shimmerFrameLayout.visibility = GONE

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
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "NativeBannerSmall loaded successfully", true)
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

                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "NativeBannerSmall failed: ${adError.message}", false)
                adPlaceholder.visibility = GONE
                shimmerFrameLayout.visibility = GONE

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
                AdDebugUtils.logEvent(adUnitId, "onAdImpression", "NativeBannerSmall impression", true)
                callback?.onAdImpression()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                AdDebugUtils.logEvent(adUnitId, "onAdClicked", "NativeBannerSmall clicked", true)
                callback?.onAdClicked()
            }

            override fun onAdOpened() {
                super.onAdOpened()
                AdDebugUtils.logEvent(adUnitId, "onAdOpened", "NativeBannerSmall opened", true)
                callback?.onAdOpened()
            }

            override fun onAdClosed() {
                super.onAdClosed()
                AdDebugUtils.logEvent(adUnitId, "onAdClosed", "NativeBannerSmall closed", true)
                callback?.onAdClosed()
            }
        })

        builder.build().loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
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
                iconView.visibility = INVISIBLE
            } else {
                (iconView as ImageView).setImageDrawable(icon.drawable)
                iconView.visibility = VISIBLE
            }
        }
        nativeAdView.advertiserView?.let { advertiserView ->
            val advertiser = nativeAd.advertiser
            if (advertiser == null) {
                advertiserView.visibility = INVISIBLE
            } else {
                (advertiserView as TextView).text = advertiser
                advertiserView.visibility = VISIBLE
            }
        }
        nativeAdView.setNativeAd(nativeAd)
    }

    // =================== WATERFALL METHODS ===================

    private fun resolveAdUnit(logicalName: String): (com.i2hammad.admanagekit.core.ad.AdProvider) -> String? = { provider ->
        AdUnitMapping.getAdUnitId(logicalName, provider)
            ?: logicalName.takeIf { provider == AdProvider.ADMOB }
    }

    private fun loadViaWaterfall(context: Context, adUnitId: String, callback: AdLoadCallback?) {
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            shimmerFrameLayout.visibility = GONE
            callback?.onFailedToLoad(
                AdError(AdManager.PURCHASED_APP_ERROR_CODE, AdManager.PURCHASED_APP_ERROR_MESSAGE, AdManager.PURCHASED_APP_ERROR_DOMAIN)
            )
            return
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        val adPlaceholder: FrameLayout = binding.flAdplaceholder

        val waterfall = NativeWaterfall(
            providers = AdProviderConfig.getNativeChain(),
            adUnitResolver = resolveAdUnit(adUnitId)
        )
        nativeWaterfall = waterfall

        waterfall.load(context, callback = object : NativeAdProvider.NativeAdCallback {
            override fun onNativeAdLoaded(adView: android.view.View, nativeAdRef: Any) {
                waterfallNativeAdRef = nativeAdRef

                // AdMob returns raw NativeAd — use existing layout and population logic
                if (nativeAdRef is NativeAd) {
                    displayAd(nativeAdRef)
                } else {
                    // Non-AdMob provider (e.g. Yandex) returns a pre-built view
                    adPlaceholder.removeAllViews()
                    val parent = adView.parent as? android.view.ViewGroup
                    parent?.removeView(adView)
                    adPlaceholder.addView(adView)
                    binding.root.visibility = VISIBLE
                    adPlaceholder.visibility = VISIBLE
                    shimmerFrameLayout.visibility = GONE
                }

                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "NativeBannerSmall waterfall loaded", true)
                callback?.onAdLoaded()
            }

            override fun onNativeAdFailedToLoad(error: AdKitAdError) {
                nativeWaterfall = null
                adPlaceholder.visibility = GONE
                shimmerFrameLayout.visibility = GONE

                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "NativeBannerSmall waterfall failed: ${error.message}", false)
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", error.code.toString())
                }
                firebaseAnalytics?.logEvent("ad_failed_to_load", params)
                callback?.onFailedToLoad(AdError(error.code, error.message, error.domain))
            }

            override fun onNativeAdClicked() { callback?.onAdClicked() }
            override fun onNativeAdImpression() {
                val params = Bundle().apply { putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId) }
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                callback?.onAdImpression()
            }
            override fun onPaidEvent(adValue: AdKitAdValue) {
                val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                    putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                }
                firebaseAnalytics?.logEvent("ad_paid_event", params)
            }
        }, sizeHint = NativeAdSize.SMALL)
    }

    fun displayAd(preloadedAd: NativeAd) {
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            shimmerFrameLayout.visibility = GONE
            callback?.onFailedToLoad(
                AdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN
                )
            )
            return
        }

        val nativeAdView = LayoutInflater.from(context)
            .inflate(R.layout.layout_native_banner_small, null) as NativeAdView
        val adPlaceholder: FrameLayout = binding.flAdplaceholder

        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)

        adPlaceholder.removeAllViews()
        adPlaceholder.addView(nativeAdView)
        adPlaceholder.visibility = VISIBLE
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        preloadedAd.setOnPaidEventListener { adValue ->
            val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            }
            firebaseAnalytics!!.logEvent("ad_paid_event", params)
        }
        populateNativeAdView(preloadedAd, nativeAdView)
        binding.shimmerContainerNative.visibility = GONE
    }

    fun setAdManagerCallback(callback: AdLoadCallback) {
        this.callback = callback
    }

    fun hideAd() {
        binding.root.visibility = GONE
    }

    fun showAd() {
        binding.root.visibility = VISIBLE
    }
}