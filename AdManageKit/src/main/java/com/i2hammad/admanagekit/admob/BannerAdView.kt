package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.billing.AppPurchase

class BannerAdView : FrameLayout {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    private var adView: AdView? = null
    lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private var parent: CardView? = null
    private lateinit var layBannerAd: FrameLayout

    private var context: Activity? = null

    var callback: AdLoadCallback? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.banner_ad_view, this, true)
        parent = findViewById(R.id.parent)
        shimmerFrameLayout = findViewById(R.id.shimmer_frame_layout)
        layBannerAd = findViewById(R.id.fl_ad_container)
        shimmerFrameLayout.startShimmer()

        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    public fun loadBanner(context: Activity?, adUnitId: String?) {
        loadCollapsibleBanner(context, adUnitId, false, callback)
    }

    public fun loadBanner(context: Activity?, adUnitId: String?, adLoadCallback: AdLoadCallback?) {
        loadCollapsibleBanner(context, adUnitId, false, adLoadCallback)
    }


    public fun setAdCallback(callback: AdLoadCallback?) {
        this.callback = callback
    }

    public fun loadCollapsibleBanner(
        context: Activity?, adUnitId: String?, collapsible: Boolean
    ) {
        loadCollapsibleBanner(context, adUnitId, collapsible, callback)
    }

    public fun loadCollapsibleBanner(
        context: Activity?, adUnitId: String?, collapsible: Boolean, callback: AdLoadCallback?
    ) {
        this.context = context
        if (AppPurchase.getInstance().isPurchased) {
            callback?.onFailedToLoad(
                AdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN
                )
            )
            shimmerFrameLayout.visibility = GONE
            return
        }

        // Create a new AdView instance every time you want to load a new ad.
        adView = AdView(context!!)
        adView!!.adUnitId = adUnitId!!
        adView!!.setAdSize(adSize)

        var builder = AdRequest.Builder()

        if (collapsible) {
            val extras = Bundle()
            extras.putString("collapsible", "bottom")
            builder = builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
        }

        adView!!.loadAd(builder.build())


//        val params = layBannerAd?.layoutParams
//        val outMetricsBanner = DisplayMetrics()
//        val displayBanner = context.windowManager.defaultDisplay
//        displayBanner.getMetrics(outMetricsBanner)
//        val densityBanner = outMetricsBanner.density
//        params!!.height = (densityBanner * adSize.height).toInt()
//        params.width = (densityBanner * adSize.width).toInt()
//        layBannerAd?.layoutParams = params

        val paramsShammir = shimmerFrameLayout?.layoutParams
        val outMetrics = DisplayMetrics()
        val display = context.windowManager.defaultDisplay
        display.getMetrics(outMetrics)
        val density = outMetrics.density
        paramsShammir!!.height = (density * adSize.height).toInt()
        paramsShammir.width = (density * adSize.width).toInt()
        shimmerFrameLayout?.layoutParams = paramsShammir

        adView!!.adListener = object : AdListener() {


            override fun onAdLoaded() {
                callback?.onAdLoaded()
                layBannerAd?.removeAllViews() // Remove any existing views first
                if (adView == null) {
                    shimmerFrameLayout.stopShimmer()
                    shimmerFrameLayout.visibility = GONE
                    return
                }
                val parent = adView!!.parent as? ViewGroup
                parent?.removeView(adView)
                layBannerAd?.addView(adView)
                shimmerFrameLayout.stopShimmer()
                shimmerFrameLayout.visibility = GONE


            }

            override fun onAdClicked() {
                super.onAdClicked()
                callback?.onAdClicked()
            }

            override fun onAdClosed() {
                super.onAdClosed()

                callback?.onAdClosed()
            }

            override fun onAdOpened() {
                super.onAdOpened()
                callback?.onAdOpened()
            }


            override fun onAdImpression() {
                super.onAdImpression()

                // Log Firebase event for ad impression
                val params = Bundle()
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                firebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                callback?.onAdImpression()

            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                shimmerFrameLayout.stopShimmer()
                shimmerFrameLayout.visibility = GONE
                parent?.visibility= GONE

                // Log Firebase event for ad failed to load
                val params = Bundle()
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                params.putString("ad_error_code", adError.code.toString() + "")
                firebaseAnalytics!!.logEvent("ad_failed_to_load", params)
                callback?.onFailedToLoad(adError)

            }

        }

        adView!!.onPaidEventListener =
            OnPaidEventListener { adValue -> // Convert the value from micros to the standard currency unit
                val adValueInStandardUnits = adValue.valueMicros / 1000000.0

                // Log Firebase event for paid event
                val params = Bundle()
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                params.putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                firebaseAnalytics!!.logEvent("ad_paid_event", params)
            }
    }

    private val adSize: AdSize
        get() {
            var bounds = Rect()
            var adWidthPixels = layBannerAd!!.width.toFloat()

            // If the ad hasn't been laid out, default to the full screen width.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = context!!.windowManager.currentWindowMetrics
                bounds = windowMetrics.bounds
            } else {
                // Handle the case for older Android versions
                val windowManager =
                    context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val display = windowManager.defaultDisplay
                display.getRectSize(bounds)
            }
            if (adWidthPixels == 0f) {
                adWidthPixels = bounds.width().toFloat()
            }

            val density = resources.displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()

            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context!!, adWidth)
//            return AdSize.getLandscapeAnchoredAdaptiveBannerAdSize(context!!, adWidth)
        }

    fun hideAd() {
        adView!!.visibility = GONE
        shimmerFrameLayout.visibility = GONE
    }

    fun showAd() {
        adView!!.visibility = VISIBLE
        shimmerFrameLayout.visibility = VISIBLE
    }
    fun destoryAd() {
        adView?.destroy()
    }
    fun resumeAd() {
        adView?.resume()
    }
    fun pauseAd() {
        adView?.pause()
    }
}
