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

    fun loadNativeBannerAd(
        activity: Activity,
        adNativeBanner: String,
        useCachedAd: Boolean = false
    ) {
        loadAd(activity, adNativeBanner, useCachedAd, callback)
    }

    fun loadNativeBannerAd(
        activity: Activity,
        adNativeBanner: String
    ) {
        loadAd(activity, adNativeBanner,false, callback)
    }

    fun loadNativeBannerAd(
        activity: Activity,
        adNativeBanner: String,
        useCachedAd: Boolean = false,
        adCallBack: AdLoadCallback
    ) {
        this.callback = adCallBack
        loadAd(activity, adNativeBanner, useCachedAd, adCallBack)
    }

    private fun loadAd(
        context: Context,
        adUnitId: String,
        useCachedAd: Boolean,
        callback: AdLoadCallback?
    ) {
        this.adUnitId = adUnitId

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

        // Check if cached ad should be used
        if (useCachedAd && NativeAdManager.enableCachingNativeAds) {
            val cachedAd = NativeAdManager.getCachedNativeAd(adUnitId)
            if (cachedAd != null) {
                displayAd(cachedAd)
                callback?.onAdLoaded()
                return
            } else {
                Log.d(TAG, "No valid cached ad available for adUnitId: $adUnitId")
            }
        }

        // Proceed to load a new ad
        val nativeAdView = LayoutInflater.from(context)
            .inflate(R.layout.layout_native_banner_small, null) as NativeAdView
        val adPlaceholder: FrameLayout = binding.flAdplaceholder

        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)

        val builder = AdLoader.Builder(context, adUnitId).forNativeAd { nativeAd ->
            adPlaceholder.removeAllViews()
            adPlaceholder.addView(nativeAdView)
            binding.root.visibility = VISIBLE
            adPlaceholder.visibility = VISIBLE
            if (NativeAdManager.enableCachingNativeAds) {
                NativeAdManager.setCachedNativeAd(adUnitId, nativeAd)
            }
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
                Log.d(TAG, "onAdLoaded: NativeBannerSmall")
                callback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Try to use cached ad if available and not explicitly requesting a new ad
                if (NativeAdManager.enableCachingNativeAds && !useCachedAd) {
                    val cachedAd = NativeAdManager.getCachedNativeAd(adUnitId)
                    if (cachedAd != null) {
                        displayAd(cachedAd)
                        callback?.onAdLoaded()
                        return
                    }
                }

                Log.d(TAG, "onAdFailedToLoad: NativeBannerSmall, Error: ${adError.message}")
                adPlaceholder.visibility = GONE
                shimmerFrameLayout.visibility = GONE

                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", adError.code.toString())
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
                callback?.onAdImpression()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback?.onAdClicked()
            }

            override fun onAdOpened() {
                super.onAdOpened()
                callback?.onAdLoaded()
            }

            override fun onAdClosed() {
                super.onAdClosed()
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