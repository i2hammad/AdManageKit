package com.i2hammad.admanagekit.admob.provider

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowMetrics
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.config.CollapsibleBannerPlacement
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.BannerAdProvider

/**
 * AdMob implementation of [BannerAdProvider].
 * Wraps Google AdMob AdView behind the provider interface.
 *
 * Ownership: a successfully loaded AdView is handed to the caller via
 * [BannerAdProvider.BannerAdCallback.onBannerLoaded]; from that point the caller
 * owns the view (and its destruction). The provider keeps a reference to the most
 * recently loaded view only to service [pause]/[resume], and never destroys
 * handed-out views (they may be attached in another screen). Views whose load
 * fails — or that are still in flight when [destroy] is called — were never
 * handed out and are destroyed by the provider.
 *
 * @param adSize Explicit ad size, or null to use adaptive full-width banner (default).
 * @param collapsible Whether to load collapsible banners.
 * @param collapsiblePlacement Collapsible direction (TOP or BOTTOM).
 */
class AdMobBannerProvider(
    private val adSize: AdSize? = null,
    var collapsible: Boolean = false,
    var collapsiblePlacement: CollapsibleBannerPlacement = CollapsibleBannerPlacement.BOTTOM
) : BannerAdProvider {

    override val provider: AdProvider = AdProvider.ADMOB

    /** Most recently handed-out view; kept only for pause/resume. */
    @Volatile
    private var adView: AdView? = null

    /** Views created for in-flight loads, not yet handed out. */
    private val pendingViews = java.util.Collections.synchronizedSet(mutableSetOf<AdView>())

    companion object {
        private const val TAG = "AdMobBanner"
    }

    private fun getAdaptiveAdSize(context: Context): AdSize {
        val activity = context as? Activity
            ?: return AdSize.BANNER

        val adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics: WindowMetrics = activity.windowManager.currentWindowMetrics
            windowMetrics.bounds.width().toFloat()
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay?.getMetrics(displayMetrics)
            displayMetrics.widthPixels.toFloat()
        }

        val density = context.resources.displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    override fun loadBanner(
        context: Context,
        adUnitId: String,
        callback: BannerAdProvider.BannerAdCallback
    ) {
        val resolvedAdSize = adSize ?: getAdaptiveAdSize(context)

        val bannerView = AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(resolvedAdSize)
        }
        pendingViews.add(bannerView)

        bannerView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                if (!pendingViews.remove(bannerView)) {
                    // destroy() already destroyed this in-flight view; do not hand it out.
                    Log.d(TAG, "Banner ad loaded after destroy, discarding: $adUnitId")
                    return
                }
                Log.d(TAG, "Banner ad loaded: $adUnitId")
                // Do NOT destroy the previously stored view: it was handed to a
                // consumer on its own onBannerLoaded and the consumer owns it.
                adView = bannerView
                callback.onBannerLoaded(bannerView)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Banner ad failed to load: ${error.message}")
                // The view was never handed out — destroy it to release the
                // underlying WebView and the captured context.
                if (pendingViews.remove(bannerView)) {
                    bannerView.destroy()
                }
                callback.onBannerFailedToLoad(error.toAdKitError())
            }

            override fun onAdClicked() {
                callback.onBannerClicked()
            }

            override fun onAdImpression() {
                callback.onBannerImpression()
            }
        }

        bannerView.onPaidEventListener = OnPaidEventListener { adValue ->
            callback.onPaidEvent(adValue.toAdKitValue())
        }

        val requestBuilder = AdRequest.Builder()
        if (collapsible || AdManageKitConfig.enableCollapsibleBannersByDefault) {
            val extras = Bundle()
            extras.putString("collapsible", collapsiblePlacement.value)
            requestBuilder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
        }
        bannerView.loadAd(requestBuilder.build())
    }

    override fun pause() {
        adView?.pause()
    }

    override fun resume() {
        adView?.resume()
    }

    override fun destroy() {
        // Destroy only views that were never handed to a consumer (in-flight loads).
        // Handed-out views are owned by their consumers and may still be displayed.
        synchronized(pendingViews) {
            pendingViews.forEach { it.destroy() }
            pendingViews.clear()
        }
        adView = null
    }
}
