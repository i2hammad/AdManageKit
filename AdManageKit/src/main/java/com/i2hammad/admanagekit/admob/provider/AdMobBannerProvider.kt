package com.i2hammad.admanagekit.admob.provider

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowMetrics
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.config.CollapsibleBannerPlacement
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.BannerAdProvider

/**
 * AdMob implementation of [BannerAdProvider].
 * Wraps Google AdMob AdView behind the provider interface.
 *
 * Ownership: a successfully loaded AdView is handed to the caller via
 * [BannerAdProvider.BannerAdCallback.onBannerLoaded]; from that point the caller
 * owns the view (and its destruction). The provider keeps a reference to the most
 * recently loaded view, but [pause]/[resume] are no-ops under the Next-Gen SDK
 * (its AdView no longer exposes pause()/resume() - banner lifecycle is managed
 * internally by the SDK). The provider never destroys handed-out views (they may
 * be attached in another screen). Views whose load fails — or that are still in
 * flight when [destroy] is called — were never handed out and are destroyed by
 * the provider.
 *
 * @param adSize Explicit ad size, or null to use adaptive full-width banner (default).
 * @param collapsible Whether to load collapsible banners.
 * @param collapsiblePlacement Collapsible direction (TOP or BOTTOM).
 */
class AdMobBannerProvider(
    var adSize: AdSize? = null,
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
        return AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    /** Map Next-Gen SDK [LoadAdError] to [AdKitAdError]. */
    private fun LoadAdError.toAdKitError(): AdKitAdError = AdKitAdError(
        code = code.value,
        message = message,
        domain = AdProvider.ADMOB.name
    )

    /** Map Next-Gen SDK [AdValue] to [AdKitAdValue]. */
    private fun AdValue.toAdKitValue(): AdKitAdValue = AdKitAdValue(
        valueMicros = valueMicros,
        currencyCode = currencyCode,
        precisionType = when (precisionType) {
            PrecisionType.ESTIMATED -> AdKitAdValue.PrecisionType.ESTIMATED
            PrecisionType.PUBLISHER_PROVIDED -> AdKitAdValue.PrecisionType.PUBLISHER_PROVIDED
            PrecisionType.PRECISE -> AdKitAdValue.PrecisionType.PRECISE
            else -> AdKitAdValue.PrecisionType.UNKNOWN
        }
    )

    override fun loadBanner(
        context: Context,
        adUnitId: String,
        callback: BannerAdProvider.BannerAdCallback
    ) {
        val resolvedAdSize = adSize ?: getAdaptiveAdSize(context)

        // Next-Gen SDK: ad unit id and size are no longer set on the AdView itself
        // (setAdUnitId()/setAdSize() are gone) - both are now supplied via the request.
        val bannerView = AdView(context)
        pendingViews.add(bannerView)

        val requestBuilder = BannerAdRequest.Builder(adUnitId, resolvedAdSize)
        if (collapsible || AdManageKitConfig.enableCollapsibleBannersByDefault) {
            val extras = Bundle()
            extras.putString("collapsible", collapsiblePlacement.value)
            requestBuilder.setGoogleExtrasBundle(extras)
        }

        bannerView.loadAd(requestBuilder.build(), object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(bannerAd: BannerAd) {
                if (!pendingViews.remove(bannerView)) {
                    // destroy() already destroyed this in-flight view; do not hand it out.
                    Log.d(TAG, "Banner ad loaded after destroy, discarding: $adUnitId")
                    return
                }
                Log.d(TAG, "Banner ad loaded: $adUnitId")
                // Do NOT destroy the previously stored view: it was handed to a
                // consumer on its own onBannerLoaded and the consumer owns it.
                adView = bannerView

                // Next-Gen SDK exposes click/impression/paid events on the loaded
                // BannerAd via adEventCallback, not on the AdView and not via a
                // separate AdListener/OnPaidEventListener.
                bannerAd.adEventCallback = object : BannerAdEventCallback {
                    override fun onAdClicked() {
                        callback.onBannerClicked()
                    }

                    override fun onAdImpression() {
                        callback.onBannerImpression()
                    }

                    override fun onAdPaid(value: AdValue) {
                        callback.onPaidEvent(value.toAdKitValue())
                    }
                }

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
        })
    }

    override fun pause() {
        // Next-Gen SDK's AdView has no pause() - banner lifecycle is managed
        // internally by the SDK now.
        Log.d(TAG, "pause: no-op under Next-Gen SDK (AdView.pause() no longer exists)")
    }

    override fun resume() {
        // Next-Gen SDK's AdView has no resume() - banner lifecycle is managed
        // internally by the SDK now.
        Log.d(TAG, "resume: no-op under Next-Gen SDK (AdView.resume() no longer exists)")
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
