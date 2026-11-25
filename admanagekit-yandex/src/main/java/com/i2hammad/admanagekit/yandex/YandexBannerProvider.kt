package com.i2hammad.admanagekit.yandex

import android.content.Context
import android.util.Log
import android.view.View
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.BannerAdProvider
import com.i2hammad.admanagekit.core.ad.InlineBannerSize
import com.i2hammad.admanagekit.yandex.internal.toAdKitError
import com.i2hammad.admanagekit.yandex.internal.toAdKitValue

/**
 * Yandex Ads implementation of [BannerAdProvider].
 * Wraps Yandex BannerAdView behind the provider interface.
 *
 * One instance is shared by every banner view in the global chain, so it never destroys a
 * banner it has handed out: that view belongs to the consumer that displays it, and another
 * view's load must not blank it. Only views still loading are destroyed by [destroy].
 *
 * @param maxAdWidth Width in dp for sticky (non-inline) banners. If 0, the screen width.
 */
class YandexBannerProvider(
    private val maxAdWidth: Int = 0
) : BannerAdProvider {

    override val provider: AdProvider = AdProvider.YANDEX

    /** Views created for in-flight loads, not yet handed out. Main thread only. */
    private val pendingViews = mutableSetOf<BannerAdView>()

    companion object {
        private const val TAG = "YandexBanner"
    }

    override fun loadBanner(
        context: Context,
        adUnitId: String,
        callback: BannerAdProvider.BannerAdCallback
    ) {
        val width = if (maxAdWidth > 0) maxAdWidth else {
            val displayMetrics = context.resources.displayMetrics
            (displayMetrics.widthPixels / displayMetrics.density).toInt()
        }
        load(context, adUnitId, BannerAdSize.sticky(context, width), callback)
    }

    /** Yandex's adaptive inline banner: the given width, any height up to the max. */
    override fun loadInlineBanner(
        context: Context,
        adUnitId: String,
        size: InlineBannerSize,
        callback: BannerAdProvider.BannerAdCallback
    ) {
        load(context, adUnitId, BannerAdSize.inline(context, size.widthDp, size.maxHeightDp), callback)
    }

    private fun load(
        context: Context,
        adUnitId: String,
        adSize: BannerAdSize,
        callback: BannerAdProvider.BannerAdCallback
    ) {
        val bannerView = BannerAdView(context)
        pendingViews.add(bannerView)
        bannerView.setAdSize(adSize)
        bannerView.setBannerAdEventListener(object : BannerAdEventListener {
            override fun onAdLoaded() {
                if (!pendingViews.remove(bannerView)) {
                    // destroy() already destroyed this in-flight view; do not hand it out.
                    Log.d(TAG, "Banner ad loaded after destroy, discarding: $adUnitId")
                    return
                }
                Log.d(TAG, "Banner ad loaded: $adUnitId")
                callback.onBannerLoaded(bannerView)
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                // Never handed out: release it here.
                if (!pendingViews.remove(bannerView)) return
                bannerView.destroy()
                Log.e(TAG, "Banner ad failed to load: ${error.description}")
                callback.onBannerFailedToLoad(error.toAdKitError())
            }

            override fun onAdClicked() {
                callback.onBannerClicked()
            }

            override fun onImpression(impressionData: ImpressionData?) {
                callback.onBannerImpression()
                callback.onPaidEvent(impressionData.toAdKitValue())
            }
        })
        bannerView.loadAd(AdRequest.Builder(adUnitId).build())
    }

    override fun pause() {
        // Yandex BannerAdView doesn't have explicit pause/resume
    }

    override fun resume() {
        // Yandex BannerAdView doesn't have explicit pause/resume
    }

    override fun destroy() {
        // Destroy only views that were never handed to a consumer (in-flight loads).
        pendingViews.forEach { it.destroy() }
        pendingViews.clear()
    }
}
