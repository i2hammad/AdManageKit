package com.i2hammad.admanagekit.core.ad

/**
 * Singleton service locator for ad provider chains.
 * Stores the ordered list of providers per ad type for waterfall resolution.
 *
 * Example:
 * ```kotlin
 * AdProviderConfig.setInterstitialChain(listOf(yandexProvider, admobProvider))
 * val chain = AdProviderConfig.getInterstitialChain()
 * ```
 */
object AdProviderConfig {

    private var interstitialChain: List<InterstitialAdProvider> = emptyList()
    private var bannerChain: List<BannerAdProvider> = emptyList()
    private var nativeChain: List<NativeAdProvider> = emptyList()
    private var appOpenChain: List<AppOpenAdProvider> = emptyList()
    private var rewardedChain: List<RewardedAdProvider> = emptyList()

    // --- Interstitial ---

    @JvmStatic
    fun setInterstitialChain(providers: List<InterstitialAdProvider>) {
        interstitialChain = providers.toList()
    }

    @JvmStatic
    fun getInterstitialChain(): List<InterstitialAdProvider> = interstitialChain

    // --- Banner ---

    @JvmStatic
    fun setBannerChain(providers: List<BannerAdProvider>) {
        bannerChain = providers.toList()
    }

    @JvmStatic
    fun getBannerChain(): List<BannerAdProvider> = bannerChain

    // --- Native ---

    @JvmStatic
    fun setNativeChain(providers: List<NativeAdProvider>) {
        nativeChain = providers.toList()
    }

    @JvmStatic
    fun getNativeChain(): List<NativeAdProvider> = nativeChain

    // --- App Open ---

    @JvmStatic
    fun setAppOpenChain(providers: List<AppOpenAdProvider>) {
        appOpenChain = providers.toList()
    }

    @JvmStatic
    fun getAppOpenChain(): List<AppOpenAdProvider> = appOpenChain

    // --- Rewarded ---

    @JvmStatic
    fun setRewardedChain(providers: List<RewardedAdProvider>) {
        rewardedChain = providers.toList()
    }

    @JvmStatic
    fun getRewardedChain(): List<RewardedAdProvider> = rewardedChain

    // --- Utility ---

    /** Reset all chains to empty. */
    @JvmStatic
    fun reset() {
        interstitialChain = emptyList()
        bannerChain = emptyList()
        nativeChain = emptyList()
        appOpenChain = emptyList()
        rewardedChain = emptyList()
    }
}
