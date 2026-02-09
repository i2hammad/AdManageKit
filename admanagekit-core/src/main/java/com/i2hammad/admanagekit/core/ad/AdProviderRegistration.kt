package com.i2hammad.admanagekit.core.ad

/**
 * Bundles all providers from a single ad network into one object.
 * Each ad network module creates an implementation of this interface.
 *
 * Example:
 * ```kotlin
 * val admob = AdMobProviderRegistration.create()
 * admob.interstitialProvider  // AdMobInterstitialProvider
 * admob.bannerProvider        // AdMobBannerProvider
 * ```
 */
interface AdProviderRegistration {

    /** The ad network this registration belongs to. */
    val adProvider: AdProvider

    /** Interstitial ad provider, or null if not supported. */
    val interstitialProvider: InterstitialAdProvider?

    /** Banner ad provider, or null if not supported. */
    val bannerProvider: BannerAdProvider?

    /** Native ad provider, or null if not supported. */
    val nativeProvider: NativeAdProvider?

    /** App open ad provider, or null if not supported. */
    val appOpenProvider: AppOpenAdProvider?

    /** Rewarded ad provider, or null if not supported. */
    val rewardedProvider: RewardedAdProvider?
}
