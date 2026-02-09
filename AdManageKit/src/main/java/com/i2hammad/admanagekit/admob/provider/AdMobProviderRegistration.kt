package com.i2hammad.admanagekit.admob.provider

import com.google.android.gms.ads.AdSize
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AdProviderRegistration
import com.i2hammad.admanagekit.core.ad.AppOpenAdProvider
import com.i2hammad.admanagekit.core.ad.BannerAdProvider
import com.i2hammad.admanagekit.core.ad.InterstitialAdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.core.ad.RewardedAdProvider

/**
 * Factory that bundles all AdMob provider implementations.
 *
 * Example:
 * ```kotlin
 * val admob = AdMobProviderRegistration.create()
 * AdProviderConfig.setInterstitialChain(listOf(admob.interstitialProvider!!))
 * ```
 */
class AdMobProviderRegistration private constructor(
    override val interstitialProvider: InterstitialAdProvider,
    override val bannerProvider: BannerAdProvider,
    override val nativeProvider: NativeAdProvider,
    override val appOpenProvider: AppOpenAdProvider,
    override val rewardedProvider: RewardedAdProvider
) : AdProviderRegistration {

    override val adProvider: AdProvider = AdProvider.ADMOB

    companion object {
        /**
         * Create an AdMob provider registration with all ad types.
         *
         * @param bannerAdSize AdMob banner size (default: BANNER)
         */
        @JvmStatic
        @JvmOverloads
        fun create(bannerAdSize: AdSize = AdSize.BANNER): AdMobProviderRegistration {
            return AdMobProviderRegistration(
                interstitialProvider = AdMobInterstitialProvider(),
                bannerProvider = AdMobBannerProvider(bannerAdSize),
                nativeProvider = AdMobNativeProvider(),
                appOpenProvider = AdMobAppOpenProvider(),
                rewardedProvider = AdMobRewardedProvider()
            )
        }
    }
}
