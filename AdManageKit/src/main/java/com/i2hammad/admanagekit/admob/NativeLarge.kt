package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
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
import com.i2hammad.admanagekit.databinding.LayoutNativeLargeBinding

class NativeLarge @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "NativeAds"
    private var callback: AdLoadCallback? = null
    private val binding: LayoutNativeLargeBinding =
        LayoutNativeLargeBinding.inflate(LayoutInflater.from(context), this)
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private lateinit var adUnitId: String

    // Waterfall support
    private var nativeWaterfall: NativeWaterfall? = null
    private var waterfallNativeAdRef: Any? = null
    private val useWaterfall: Boolean
        get() = AdProviderConfig.getNativeChain().isNotEmpty()

    // =================== DEPRECATED METHODS (use loadingStrategy instead) ===================

    @Deprecated(
        message = "Use loadNativeAds with loadingStrategy parameter instead",
        replaceWith = ReplaceWith("loadNativeAds(activity, adNativeLarge, loadingStrategy = AdLoadingStrategy.HYBRID)")
    )
    fun loadNativeAds(
        activity: Context,
        adNativeLarge: String,
        useCachedAd: Boolean = false
    ) {
        loadAd(activity, adNativeLarge, useCachedAd, callback, null)
    }

    @Deprecated(
        message = "Use loadNativeAds with loadingStrategy parameter instead",
        replaceWith = ReplaceWith("loadNativeAds(activity, adNativeLarge, callback, loadingStrategy = AdLoadingStrategy.HYBRID)")
    )
    fun loadNativeAds(
        activity: Context,
        adNativeLarge: String,
        useCachedAd: Boolean = false,
        callback: AdLoadCallback
    ) {
        this.callback = callback
        loadAd(activity, adNativeLarge, useCachedAd, callback, null)
    }

    // =================== NEW METHODS (recommended) ===================

    /**
     * Load native ad using default global strategy from AdManageKitConfig.nativeLoadingStrategy
     */
    fun loadNativeAds(
        activity: Context,
        adNativeLarge: String
    ) {
        loadAd(activity, adNativeLarge, false, callback, null)
    }

    /**
     * Load native ad with custom loading strategy override
     *
     * @param loadingStrategy Strategy to use (ON_DEMAND or HYBRID). Note: ONLY_CACHE is not supported for native ads.
     */
    fun loadNativeAds(
        activity: Context,
        adNativeLarge: String,
        loadingStrategy: com.i2hammad.admanagekit.config.AdLoadingStrategy
    ) {
        loadAd(activity, adNativeLarge, false, callback, loadingStrategy)
    }

    /**
     * Load native ad with custom loading strategy and callback
     *
     * @param loadingStrategy Strategy to use (ON_DEMAND or HYBRID). Note: ONLY_CACHE is not supported for native ads.
     */
    fun loadNativeAds(
        activity: Context,
        adNativeLarge: String,
        callback: AdLoadCallback,
        loadingStrategy: com.i2hammad.admanagekit.config.AdLoadingStrategy? = null
    ) {
        this.callback = callback
        loadAd(activity, adNativeLarge, false, callback, loadingStrategy)
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

        val nativeAdView: NativeAdView = binding.nativeAdView
        val viewGroup = binding.adUnit
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
            val screenKey = "${context.javaClass.simpleName}_LARGE"

            com.i2hammad.admanagekit.utils.NativeAdIntegrationManager.loadNativeAdWithCaching(
                activity = context,
                baseAdUnitId = adUnitId,
                screenType = com.i2hammad.admanagekit.utils.NativeAdIntegrationManager.ScreenType.LARGE,
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
                        binding.adUnit.visibility = View.GONE
                        binding.nativeAdView.visibility = View.GONE
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
        val nativeAdView: NativeAdView = binding.nativeAdView
        val viewGroup = binding.adUnit
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative

        nativeAdView.mediaView = binding.mediaView
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.primary)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.secondary)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.cta)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.tertiary)
        nativeAdView.adChoicesView = nativeAdView.findViewById(R.id.ad_choices_view)

        val builder = AdLoader.Builder(context, adUnitId).forNativeAd { nativeAd ->
            viewGroup.visibility = View.VISIBLE
            nativeAdView.visibility = View.VISIBLE
            shimmerFrameLayout.visibility = View.GONE
            binding.root.visibility = View.VISIBLE

            // NOTE: Do NOT cache ad here - it's being displayed immediately
            // Caching is only for preloaded ads that will be shown later
            // Ads expire after 1 hour, so caching displayed ads wastes memory

            populateNativeAdView(nativeAd, nativeAdView)

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
            override fun onAdImpression() {
                super.onAdImpression()
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                AdDebugUtils.logEvent(adUnitId, "onAdImpression", "NativeLarge impression", true)
                callback?.onAdImpression()
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "NativeLarge loaded successfully", true)
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

                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "NativeLarge failed: ${adError.message}", false)
                viewGroup.visibility = View.GONE
                nativeAdView.visibility = View.GONE
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

            override fun onAdOpened() {
                super.onAdOpened()
                AdDebugUtils.logEvent(adUnitId, "onAdOpened", "NativeLarge opened", true)
                callback?.onAdOpened()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                AdDebugUtils.logEvent(adUnitId, "onAdClicked", "NativeLarge clicked", true)
                callback?.onAdClicked()
            }

            override fun onAdClosed() {
                super.onAdClosed()
                AdDebugUtils.logEvent(adUnitId, "onAdClosed", "NativeLarge closed", true)
                callback?.onAdClosed()
            }
        })

        builder.build().loadAd(AdRequest.Builder().build())
    }

    fun displayAd(preloadedNativeAd: NativeAd) {
        val nativeAdView: NativeAdView = binding.nativeAdView
        val viewGroup = binding.adUnit
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
        nativeAdView.mediaView = binding.mediaView
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.primary)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.secondary)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.cta)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.tertiary)
        nativeAdView.adChoicesView = nativeAdView.findViewById(R.id.ad_choices_view)

        preloadedNativeAd.setOnPaidEventListener { adValue ->
            val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            }
            firebaseAnalytics!!.logEvent("ad_paid_event", params)
        }

        populateNativeAdView(preloadedNativeAd, nativeAdView)
        viewGroup.visibility = View.VISIBLE
        nativeAdView.visibility = View.VISIBLE
        shimmerFrameLayout.visibility = View.GONE
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
        Log.d(TAG, "AdChoices info: ${nativeAd.adChoicesInfo}")
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
                advertiserView.visibility = View.INVISIBLE
            } else {
                (advertiserView as TextView).text = advertiser
                advertiserView.visibility = View.VISIBLE
            }
        }

        nativeAdView.adChoicesView?.let { adChoicesView ->
            if (nativeAd.adChoicesInfo == null) {
                adChoicesView.visibility = View.GONE
            } else {
                adChoicesView.visibility = View.VISIBLE
            }
        }

        nativeAdView.setNativeAd(nativeAd)
    }

    // =================== WATERFALL METHODS ===================

    private fun resolveAdUnit(logicalName: String): (AdProvider) -> String? = { provider ->
        AdUnitMapping.getAdUnitId(logicalName, provider)
            ?: logicalName.takeIf { provider == AdProvider.ADMOB }
    }

    private fun loadViaWaterfall(context: Context, adUnitId: String, callback: AdLoadCallback?) {
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative
        val viewGroup = binding.adUnit as FrameLayout
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            shimmerFrameLayout.visibility = View.GONE
            callback?.onFailedToLoad(
                AdError(AdManager.PURCHASED_APP_ERROR_CODE, AdManager.PURCHASED_APP_ERROR_MESSAGE, AdManager.PURCHASED_APP_ERROR_DOMAIN)
            )
            return
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

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
                    viewGroup.removeAllViews()
                    val parent = adView.parent as? android.view.ViewGroup
                    parent?.removeView(adView)
                    viewGroup.addView(adView)
                    binding.root.visibility = View.VISIBLE
                    viewGroup.visibility = View.VISIBLE
                    binding.nativeAdView.visibility = View.GONE
                    shimmerFrameLayout.visibility = View.GONE
                }

                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "NativeLarge waterfall loaded", true)
                callback?.onAdLoaded()
            }

            override fun onNativeAdFailedToLoad(error: AdKitAdError) {
                nativeWaterfall = null
                viewGroup.visibility = View.GONE
                binding.nativeAdView.visibility = View.GONE
                shimmerFrameLayout.visibility = View.GONE

                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "NativeLarge waterfall failed: ${error.message}", false)
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
        }, sizeHint = NativeAdSize.LARGE)
    }

    fun hideAd() {
        binding.root.visibility = View.GONE
    }

    fun setAdManagerCallback(callback: AdLoadCallback) {
        this.callback = callback
    }

    fun showAd() {
        binding.root.visibility = View.VISIBLE
    }
}