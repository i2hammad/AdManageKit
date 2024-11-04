package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowMetrics
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.billing.AppPurchase

class BannerAdView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var adView: AdView? = null
    private var shimmerFrameLayout: ShimmerFrameLayout
    private var layBannerAd: FrameLayout
    private var activityContext: Activity? = null
    var callback: AdLoadCallback? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.banner_ad_view, this, true)
        shimmerFrameLayout = findViewById(R.id.shimmer_frame_layout)
        layBannerAd = findViewById(R.id.fl_ad_container)
        if (!isInEditMode) {
            shimmerFrameLayout.startShimmer()
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }


    fun loadBanner(context: Activity?, adUnitId: String?, adLoadCallback: AdLoadCallback? = null) {
        loadCollapsibleBanner(context, adUnitId, false, adLoadCallback)
    }

    fun loadBanner(context: Activity?, adUnitId: String?) {
        loadCollapsibleBanner(context, adUnitId, false, callback)
    }

    fun setAdCallback(callback: AdLoadCallback?) {
        this.callback = callback
    }


    public fun loadCollapsibleBanner(
        context: Activity?, adUnitId: String?, collapsible: Boolean
    ) {
        loadCollapsibleBanner(context, adUnitId, collapsible, callback)
    }

    fun loadCollapsibleBanner(
        context: Activity?,
        adUnitId: String?,
        collapsible: Boolean,
        callback: AdLoadCallback? = null
    ) {
        if (isInEditMode) return
        this.activityContext = context
        if (AppPurchase.getInstance().isPurchased) {
            callback?.onFailedToLoad(
                AdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN
                )
            )
            shimmerFrameLayout.visibility = View.GONE
            return
        }

        adView = AdView(context!!)
        adView?.adUnitId = adUnitId!!
        adView?.setAdSize(getAdSize())

        val builder = AdRequest.Builder()
        if (collapsible) {
            val extras = Bundle()
            extras.putString("collapsible", "bottom")
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
        }

        adView?.loadAd(builder.build())

        adjustShimmerLayout()

        adView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                layBannerAd.removeAllViews()
                adView?.let {
                    val parent = it.parent as? ViewGroup
                    parent?.removeView(it)
                    layBannerAd.addView(it)
                }
                shimmerFrameLayout.stopShimmer()
                shimmerFrameLayout.visibility = View.GONE
                callback?.onAdLoaded()
            }

            override fun onAdClicked() {
                callback?.onAdClicked()
            }

            override fun onAdClosed() {
                callback?.onAdClosed()
            }

            override fun onAdOpened() {
                callback?.onAdOpened()
            }

            override fun onAdImpression() {
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                }
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                callback?.onAdImpression()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                shimmerFrameLayout.stopShimmer()
                shimmerFrameLayout.visibility = View.GONE
                visibility = View.GONE
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", adError.code.toString())
                }
                firebaseAnalytics?.logEvent("ad_failed_to_load", params)
                callback?.onFailedToLoad(adError)
            }
        }

        adView?.onPaidEventListener = OnPaidEventListener { adValue ->
            val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            }
            firebaseAnalytics?.logEvent("ad_paid_event", params)
        }
    }

    private fun adjustShimmerLayout() {
        val params = shimmerFrameLayout.layoutParams
        val adSize = getAdSize()
        val density = resources.displayMetrics.density
        params.height = (density * adSize.height).toInt()
        params.width = (density * adSize.width).toInt()
        shimmerFrameLayout.layoutParams = params
    }

    private fun getAdSize(): AdSize {
        val displayMetrics = DisplayMetrics()
        val windowManager = activityContext?.windowManager

        val adWidthPixels = if (layBannerAd.width > 0) {
            layBannerAd.width.toFloat()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics: WindowMetrics = windowManager?.currentWindowMetrics!!
                windowMetrics.bounds.width().toFloat()
            } else {
                @Suppress("DEPRECATION") val display = windowManager?.defaultDisplay
                display?.getMetrics(displayMetrics)
                displayMetrics.widthPixels.toFloat()
            }
        }

        val density = resources.displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activityContext!!, adWidth)
    }

    fun hideAd() {
        adView?.visibility = View.GONE
        shimmerFrameLayout.visibility = View.GONE
    }

    fun showAd() {
        adView?.visibility = View.VISIBLE
        shimmerFrameLayout.visibility = View.VISIBLE
    }

    fun destroyAd() {
        adView?.destroy()
    }

    fun resumeAd() {
        adView?.resume()
    }

    fun pauseAd() {
        adView?.pause()
    }
}
