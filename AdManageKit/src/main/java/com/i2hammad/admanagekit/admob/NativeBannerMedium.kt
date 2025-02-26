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
import com.i2hammad.admanagekit.databinding.LayoutNativeBannerMediumPreviewBinding

class NativeBannerMedium @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val TAG = "NativeAds"

    private val binding: LayoutNativeBannerMediumPreviewBinding =
        LayoutNativeBannerMediumPreviewBinding.inflate(LayoutInflater.from(context), this)

    private lateinit var adUnitId: String
    private var firebaseAnalytics: FirebaseAnalytics? = null

    var callback: AdLoadCallback? = null


    private fun loadAd(context: Context, adUnitId: String, callback: AdLoadCallback?) {
        this.adUnitId = adUnitId
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative

        var purchaseProvider = BillingConfig.getPurchaseProvider()

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
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);

        shimmerFrameLayout.visibility = VISIBLE
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
            binding.root.visibility = VISIBLE
            adPlaceholder.visibility = VISIBLE
            if (NativeAdManager.enableCachingNativeAds){
                NativeAdManager.setCachedNativeAd(nativeAd)
            }
            populateNativeAdView(nativeAd, nativeAdView)
            shimmerFrameLayout.visibility = GONE

            nativeAd.setOnPaidEventListener { adValue ->
                // Convert the value from micros to the standard currency unit
                val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0

                // Log Firebase event for paid event
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

                callback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {

                val loadedAd = NativeAdManager.getCachedNativeAd()
                if (NativeAdManager.enableCachingNativeAds && loadedAd!= null){
                   displayAd(loadedAd)
               }else{
                    Log.d(TAG, "onAdFailedToLoad: NativeBannerMedium, Error: ${adError.message} ")

                    adPlaceholder.visibility = GONE
                    shimmerFrameLayout.visibility = GONE

                    // Log Firebase event for ad failed to load
                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                        putString("ad_error_code", adError.code.toString())
                    }
                    firebaseAnalytics?.logEvent("ad_failed_to_load", params)
                    callback?.onFailedToLoad(adError)
               }


            }


            override fun onAdImpression() {
                super.onAdImpression()
                // Log Firebase event for ad loaded
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

    public fun loadNativeBannerAd(activity: Activity, adNativeBanner: String) {
        loadAd(activity, adNativeBanner, callback)
    }

    public fun loadNativeBannerAd(
        activity: Activity, adNativeBanner: String, adCallBack: AdLoadCallback
    ) {
        loadAd(activity, adNativeBanner, adCallBack)
    }


    public fun setAdManagerCallback(callback: AdLoadCallback) {
        this.callback = callback
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
        Log.d("NativeAd", "AdChoices info: ${nativeAd.adChoicesInfo}")

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
                advertiserView.visibility = GONE
            } else {
                (advertiserView as TextView).text = advertiser
                advertiserView.visibility = VISIBLE
            }

            nativeAdView.adChoicesView?.let { adChoicesView ->

                val choicesView = nativeAdView.adChoicesView
                if (choicesView == null) {
                    adChoicesView.visibility = GONE
                }else{
                    adChoicesView.visibility = VISIBLE
                    nativeAdView.adChoicesView = choicesView
                }
            }
            }


        nativeAdView.setNativeAd(nativeAd)
    }


    fun displayAd(preloadedAd: NativeAd) {
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative
        var purchaseProvider = BillingConfig.getPurchaseProvider()
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
            .inflate(R.layout.layout_native_banner_medium, null) as NativeAdView
        val adPlaceholder: FrameLayout = binding.flAdplaceholder

        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)

        adPlaceholder.removeAllViews()
        adPlaceholder.addView(nativeAdView)
        adPlaceholder.visibility = VISIBLE
        preloadedAd.setOnPaidEventListener { adValue ->
            // Convert the value from micros to the standard currency unit
            val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0

            // Log Firebase event for paid event
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


    public fun hideAd() {
        binding.root.visibility = GONE

    }

    public fun showAd() {
        binding.root.visibility = VISIBLE

    }
}
