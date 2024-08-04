package com.ads.control.ads.nativeads

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.databinding.LayoutNativeBannerMediumPreviewBinding

class NativeBannerMedium @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val TAG = "NativeAds"

    private val binding: LayoutNativeBannerMediumPreviewBinding =
        LayoutNativeBannerMediumPreviewBinding.inflate(LayoutInflater.from(context), this)

    fun loadNativeBannerAd(activity: Activity, adNativeBanner: String) {
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainerNative
        shimmerFrameLayout.visibility = VISIBLE
        val nativeAdView = LayoutInflater.from(activity)
            .inflate(R.layout.layout_native_banner_medium, null) as NativeAdView
        val adPlaceholder: FrameLayout = binding.flAdplaceholder
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)
        val builder = AdLoader.Builder(activity, adNativeBanner).forNativeAd { nativeAd ->
            adPlaceholder.removeAllViews()
            adPlaceholder.addView(nativeAdView)
            adPlaceholder.visibility = VISIBLE
            populateNativeAdView(nativeAd, nativeAdView)
            shimmerFrameLayout.visibility = GONE
        }.withAdListener(object : AdListener() {
            override fun onAdLoaded() {
                super.onAdLoaded()
//                Log.d(TAG, "onAdLoaded: NativeBannerMedium")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "onAdFailedToLoad: NativeBannerMedium, Error: ${adError.message} ")

                adPlaceholder.visibility = GONE
                shimmerFrameLayout.visibility = GONE
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
}
