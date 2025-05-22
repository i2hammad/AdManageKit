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
import com.i2hammad.admanagekit.databinding.LayoutNativeBannerMediumPreviewBinding

class NativeBannerMedium @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "NativeAds"
    private val binding: LayoutNativeBannerMediumPreviewBinding =
        LayoutNativeBannerMediumPreviewBinding.inflate(LayoutInflater.from(context), this)
    private lateinit var adUnitId: String
    private var firebaseAnalytics: FirebaseAnalytics? = null
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

        shimmerFrameLayout.visibility = View.VISIBLE
        val adPlaceholder: FrameLayout = binding.flAdplaceholder
        val nativeAdView = LayoutInflater.from(context)
            .inflate(R.layout.layout_native_banner_medium, null) as NativeAdView
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)
        nativeAdView.adChoicesView = nativeAdView.findViewById(R.id.ad_choices_view)

        val builder = AdLoader.Builder(context, adUnitId).forNativeAd { nativeAd ->
            adPlaceholder.removeAllViews()
            adPlaceholder.addView(nativeAdView)
            binding.root.visibility = View.VISIBLE
            adPlaceholder.visibility = View.VISIBLE
            if (NativeAdManager.enableCachingNativeAds) {
                NativeAdManager.setCachedNativeAd(adUnitId, nativeAd)
            }
            populateNativeAdView(nativeAd, nativeAdView)
            shimmerFrameLayout.visibility = View.GONE

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
                Log.d(TAG, "onAdLoaded: NativeBannerMedium")
                callback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                if (NativeAdManager.enableCachingNativeAds && !useCachedAd) {
                    val cachedAd = NativeAdManager.getCachedNativeAd(adUnitId)
                    if (cachedAd != null) {
                        displayAd(cachedAd)
                        callback?.onAdLoaded()
                        return
                    }
                }

                Log.d(TAG, "onAdFailedToLoad: NativeBannerMedium, Error: ${adError.message}")
                adPlaceholder.visibility = View.GONE
                shimmerFrameLayout.visibility = View.GONE

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

            override fun onAdClosed() {
                super.onAdClosed()
                callback?.onAdClosed()
            }

            override fun onAdOpened() {
                super.onAdOpened()
                callback?.onAdOpened()
            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback?.onAdClicked()
            }
        })

        builder.build().loadAd(AdRequest.Builder().build())
    }

    fun setAdManagerCallback(callback: AdLoadCallback) {
        this.callback = callback
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
                advertiserView.visibility = View.GONE
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

    fun displayAd(preloadedAd: NativeAd) {
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
        val adPlaceholder: FrameLayout = binding.flAdplaceholder
        val nativeAdView = LayoutInflater.from(context)
            .inflate(R.layout.layout_native_banner_medium, null) as NativeAdView

        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)
        nativeAdView.adChoicesView = nativeAdView.findViewById(R.id.ad_choices_view)

        adPlaceholder.removeAllViews()
        adPlaceholder.addView(nativeAdView)
        adPlaceholder.visibility = View.VISIBLE
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
        binding.shimmerContainerNative.visibility = View.GONE
    }

    fun hideAd() {
        binding.root.visibility = View.GONE
    }

    fun showAd() {
        binding.root.visibility = View.VISIBLE
    }
}