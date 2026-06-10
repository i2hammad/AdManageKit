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
import com.i2hammad.admanagekit.yandex.internal.toAdKitError
import com.i2hammad.admanagekit.yandex.internal.toAdKitValue

/**
 * Yandex Ads implementation of [BannerAdProvider].
 * Wraps Yandex BannerAdView behind the provider interface.
 *
 * @param maxAdWidth Maximum banner width in dp. Used for adaptive inline banners.
 *                   If 0, defaults to screen width.
 */
class YandexBannerProvider(
    private val maxAdWidth: Int = 0
) : BannerAdProvider {

    override val provider: AdProvider = AdProvider.YANDEX

    private var bannerAdView: BannerAdView? = null
    private var isDestroyed = false

    companion object {
        private const val TAG = "YandexBanner"
    }

    override fun loadBanner(
        context: Context,
        adUnitId: String,
        callback: BannerAdProvider.BannerAdCallback
    ) {
        // Replace any previous banner so sequential loads don't leak the first view
        bannerAdView?.destroy()
        val bannerView = BannerAdView(context).apply {
            val width = if (maxAdWidth > 0) maxAdWidth else {
                val displayMetrics = context.resources.displayMetrics
                (displayMetrics.widthPixels / displayMetrics.density).toInt()
            }
            setAdSize(BannerAdSize.sticky(context, width))

            setBannerAdEventListener(object : BannerAdEventListener {
                override fun onAdLoaded() {
                    if (isDestroyed) {
                        Log.d(TAG, "Banner ad loaded after destroy, discarding: $adUnitId")
                        return
                    }
                    Log.d(TAG, "Banner ad loaded: $adUnitId")
                    callback.onBannerLoaded(this@apply)
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    if (isDestroyed) return
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
        }

        // Track the view from creation so destroy() during an in-flight load destroys it
        bannerAdView = bannerView
        bannerView.loadAd(AdRequest.Builder(adUnitId).build())
    }

    override fun pause() {
        // Yandex BannerAdView doesn't have explicit pause/resume
    }

    override fun resume() {
        // Yandex BannerAdView doesn't have explicit pause/resume
    }

    override fun destroy() {
        isDestroyed = true
        bannerAdView?.destroy()
        bannerAdView = null
    }
}
