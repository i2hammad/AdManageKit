package com.i2hammad.admanagekit.admob

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
import com.i2hammad.admanagekit.billing.AppPurchase
import com.i2hammad.admanagekit.databinding.LayoutNativeLargeBinding

class NativeLarge @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private  var callback: AdManagerCallback? = null
    private val binding: LayoutNativeLargeBinding =
        LayoutNativeLargeBinding.inflate(LayoutInflater.from(context), this)
    private var firebaseAnalytics: FirebaseAnalytics? = null

    private lateinit var adUnitId: String

    fun loadNativeAds(activity: Context, adNativeLarge: String) {

        this.adUnitId = adNativeLarge
        val nativeAdView: NativeAdView = binding.nativeAdView
        val viewGroup = binding.adUnit
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative
        if (AppPurchase.getInstance().isPurchased) {
            shimmerFrameLayout.visibility = GONE
            callback?.onFailedToLoad(AdError(
                AdManager.PURCHASED_APP_ERROR_CODE,
                AdManager.PURCHASED_APP_ERROR_MESSAGE,
                AdManager.PURCHASED_APP_ERROR_DOMAIN))

            return
        }
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);

        nativeAdView.mediaView = binding.mediaView
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.primary)
        nativeAdView.bodyView = nativeAdView.findViewById<View>(R.id.secondary)
        nativeAdView.callToActionView = nativeAdView.findViewById<View>(R.id.cta)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.icon)
        nativeAdView.advertiserView = nativeAdView.findViewById<View>(R.id.tertiary)


        val builder = AdLoader.Builder(activity, adNativeLarge).forNativeAd { nativeAd ->
            populateNativeAdView(nativeAd, nativeAdView)
            viewGroup.visibility = View.VISIBLE
            nativeAdView.visibility = View.VISIBLE
            shimmerFrameLayout.visibility = View.GONE

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


                // Log Firebase event for ad loaded
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                callback?.onAdLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                viewGroup.visibility = View.GONE
                nativeAdView.visibility = View.GONE
                shimmerFrameLayout.visibility = View.GONE


                // Log Firebase event for ad failed to load
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", adError.code.toString())
                }
                firebaseAnalytics?.logEvent("ad_failed_to_load", params)
                callback?.onFailedToLoad(adError)

            }
        })
        builder.build().loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
        (nativeAdView.headlineView as TextView?)?.text = nativeAd.headline
        (nativeAdView.bodyView as TextView?)?.text = nativeAd.body
        (nativeAdView.callToActionView as TextView?)?.text = nativeAd.callToAction

        val icon = nativeAd.icon
        if (icon == null) {
            nativeAdView.iconView?.visibility = View.INVISIBLE
        } else {
            (nativeAdView.iconView as ImageView?)?.setImageDrawable(icon.drawable)
            nativeAdView.iconView?.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            nativeAdView.advertiserView?.visibility = View.INVISIBLE
        } else {
            (nativeAdView.advertiserView as TextView?)?.text = nativeAd.advertiser
            nativeAdView.advertiserView?.visibility = View.VISIBLE
        }

        nativeAdView.setNativeAd(nativeAd)
    }

    public fun hideAd() {
        binding.root.visibility = GONE

    }


    private fun setAdManagerCallback(callback: AdManagerCallback) {
        this.callback = callback
    }


    public fun showAd() {
        binding.root.visibility = VISIBLE

    }
}
