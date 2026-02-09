package com.i2hammad.admanagekit.yandex

import android.content.Context
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AdProviderRegistration
import com.i2hammad.admanagekit.core.ad.AppOpenAdProvider
import com.i2hammad.admanagekit.core.ad.BannerAdProvider
import com.i2hammad.admanagekit.core.ad.InterstitialAdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.core.ad.RewardedAdProvider
import com.yandex.mobile.ads.common.MobileAds

/**
 * Factory that bundles all Yandex Ads provider implementations.
 *
 * Example:
 * ```kotlin
 * val yandex = YandexProviderRegistration.create()
 * AdProviderConfig.setInterstitialChain(listOf(yandex.interstitialProvider!!))
 * ```
 */
class YandexProviderRegistration private constructor(
    override val interstitialProvider: InterstitialAdProvider,
    override val bannerProvider: BannerAdProvider,
    override val nativeProvider: NativeAdProvider,
    override val appOpenProvider: AppOpenAdProvider,
    override val rewardedProvider: RewardedAdProvider
) : AdProviderRegistration {

    override val adProvider: AdProvider = AdProvider.YANDEX

    companion object {
        /**
         * Initialize the Yandex Mobile Ads SDK.
         * Must be called before loading any Yandex ads (typically in Application.onCreate).
         */
        @JvmStatic
        fun initialize(context: Context, onComplete: (() -> Unit)? = null) {
            MobileAds.initialize(context) { onComplete?.invoke() }
        }

        /**
         * Create a Yandex provider registration with all ad types.
         *
         * @param bannerMaxWidth Maximum banner width in dp (0 = screen width)
         */
        @JvmStatic
        @JvmOverloads
        fun create(bannerMaxWidth: Int = 0): YandexProviderRegistration {
            return YandexProviderRegistration(
                interstitialProvider = YandexInterstitialProvider(),
                bannerProvider = YandexBannerProvider(bannerMaxWidth),
                nativeProvider = YandexNativeProvider(),
                appOpenProvider = YandexAppOpenProvider(),
                rewardedProvider = YandexRewardedProvider()
            )
        }
    }
}
