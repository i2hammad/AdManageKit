# Release Notes - v3.3.8

**Release Date**: February 2026

## Multi-Provider Waterfall & Yandex Ads Support

This release introduces a multi-ad-provider architecture with waterfall mediation, allowing you to chain multiple ad networks (AdMob, Yandex, and custom providers) with automatic fallback. The existing AdMob-only API remains fully backward compatible.

### New Features

#### Multi-Provider Ad Architecture

New core interfaces in `admanagekit-core` enable ad network abstraction with zero external dependencies:

- `InterstitialAdProvider` - Load and show interstitial ads from any network
- `AppOpenAdProvider` - Load and show app open ads from any network
- `BannerAdProvider` - Load banner ads from any network
- `NativeAdProvider` - Load native ads from any network
- `RewardedAdProvider` - Load and show rewarded ads from any network

```kotlin
// Register providers in your Application class
AdProviderConfig.registerProvider(AdProvider.ADMOB, AdMobProviderRegistration())
AdProviderConfig.registerProvider(AdProvider.YANDEX, YandexProviderRegistration())

// Configure waterfall chains
AdProviderConfig.setInterstitialChain(listOf(AdProvider.ADMOB, AdProvider.YANDEX))
AdProviderConfig.setAppOpenChain(listOf(AdProvider.ADMOB, AdProvider.YANDEX))
AdProviderConfig.setBannerChain(listOf(AdProvider.ADMOB, AdProvider.YANDEX))
AdProviderConfig.setNativeChain(listOf(AdProvider.ADMOB, AdProvider.YANDEX))
AdProviderConfig.setRewardedChain(listOf(AdProvider.ADMOB, AdProvider.YANDEX))

// Map ad unit IDs per provider
AdProviderConfig.setAdUnitMapping(AdUnitMapping(
    interstitial = mapOf(
        AdProvider.ADMOB to "ca-app-pub-xxx/yyy",
        AdProvider.YANDEX to "R-M-XXXXX-Y"
    ),
    // ... other ad types
))
```

#### Waterfall Orchestrators

Waterfall classes automatically try the next provider when the current one fails:

- `InterstitialWaterfall` - Interstitial ad waterfall with full AdManager integration
- `AppOpenWaterfall` - App open ad waterfall with lifecycle awareness
- `BannerWaterfall` - Banner ad waterfall
- `NativeWaterfall` - Native ad waterfall
- `RewardedWaterfall` - Rewarded ad waterfall

The waterfall is transparent to existing code - `AdManager`, `AppOpenManager`, `BannerAdView`, native ad views, and `RewardedAdManager` all detect waterfall configuration automatically and delegate to the waterfall when providers are configured.

#### New Module: `admanagekit-yandex`

Full Yandex Mobile Ads SDK integration as a provider:

- `YandexInterstitialProvider`
- `YandexAppOpenProvider`
- `YandexBannerProvider`
- `YandexNativeProvider`
- `YandexRewardedProvider`

```groovy
// Add Yandex provider module
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.3.8'
```

#### AdMob Provider Wrappers

AdMob functionality is now also available as provider interfaces for waterfall use:

- `AdMobInterstitialProvider`
- `AdMobAppOpenProvider`
- `AdMobBannerProvider`
- `AdMobNativeProvider`
- `AdMobRewardedProvider`

### Backward Compatibility

The existing single-network API is completely unchanged. If you don't configure any waterfall chains, everything works exactly as before:

```kotlin
// This still works identically
AdManager.getInstance().loadInterstitialAd(context, adUnitId)
AdManager.getInstance().showInterstitialAd(activity, callback)
```

Waterfall mode activates only when `AdProviderConfig.getInterstitialChain()` (or other chain getters) returns a non-empty list.

---

## Installation

```groovy
// Core modules (same as before)
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.3.8'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.3.8'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.3.8'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.3.8'

// New: Yandex provider (optional)
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.3.8'
```

## Full Changelog

- Multi-provider ad architecture with core interfaces in `admanagekit-core`
- Waterfall mediation for all ad types (interstitial, app open, banner, native, rewarded)
- New `admanagekit-yandex` module for Yandex Mobile Ads SDK integration
- AdMob provider wrappers for waterfall compatibility
- `AdProviderConfig` for registering providers and configuring waterfall chains
- `AdUnitMapping` for per-provider ad unit ID configuration
- Transparent waterfall integration - existing API unchanged
- Sample app updated with waterfall test activity
