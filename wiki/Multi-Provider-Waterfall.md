# Multi-Provider Waterfall Ads

## Overview

AdManageKit supports loading ads from multiple ad networks (providers) using a **waterfall** strategy. When the primary provider fails to fill an ad, the system automatically tries the next provider in the chain until one succeeds.

This is fully backward compatible -- if no provider chains are configured, all existing API classes (`AdManager`, `BannerAdView`, `NativeBannerSmall`, etc.) continue to use AdMob directly with zero changes.

## Architecture

```
 App Code (unchanged)
      |
 Old API Classes (AdManager, BannerAdView, NativeBannerSmall, etc.)
      |
  useWaterfall? ----NO----> Direct AdMob (existing code path)
      |
     YES
      |
 Waterfall Orchestrator (InterstitialWaterfall, NativeWaterfall, etc.)
      |
 Provider Chain: [Yandex] -> [AdMob] -> ...
      |
 Core Interfaces (NativeAdProvider, InterstitialAdProvider, etc.)
```

### Modules

| Module | Purpose | Dependencies |
|--------|---------|--------------|
| `admanagekit-core` | Provider interfaces, `AdProviderConfig`, `AdUnitMapping` | None (zero deps) |
| `AdManageKit` | Waterfall orchestrators + AdMob providers | Core + AdMob SDK |
| `admanagekit-yandex` | Yandex Ads providers | Core + Yandex SDK |

## Quick Start

### 1. Add Dependencies

```groovy
dependencies {
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit:VERSION"
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit-core:VERSION"

    // Add Yandex provider
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:VERSION"
}
```

### 2. Configure in Application Class

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ... existing AdManageKitConfig setup ...

        configureMultiProvider()
    }

    private fun configureMultiProvider() {
        // Initialize Yandex SDK
        YandexProviderRegistration.initialize(this)

        // Create provider registrations
        val admob = AdMobProviderRegistration.create()
        val yandex = YandexProviderRegistration.create()

        // Map AdMob ad unit IDs to Yandex equivalents.
        // Your existing code keeps using AdMob IDs as-is.
        AdUnitMapping.register("ca-app-pub-xxx/interstitial", mapOf(
            "yandex" to "R-M-XXXXXX-Y"
        ))
        AdUnitMapping.register("ca-app-pub-xxx/banner", mapOf(
            "yandex" to "R-M-XXXXXX-Y"
        ))
        AdUnitMapping.register("ca-app-pub-xxx/native", mapOf(
            "yandex" to "R-M-XXXXXX-Y"
        ))

        // Configure provider chains (order = priority)
        AdProviderConfig.setInterstitialChain(listOf(admob.interstitialProvider, yandex.interstitialProvider))
        AdProviderConfig.setBannerChain(listOf(admob.bannerProvider, yandex.bannerProvider))
        AdProviderConfig.setNativeChain(listOf(admob.nativeProvider, yandex.nativeProvider))
        AdProviderConfig.setAppOpenChain(listOf(admob.appOpenProvider, yandex.appOpenProvider))
        AdProviderConfig.setRewardedChain(listOf(admob.rewardedProvider, yandex.rewardedProvider))
    }
}
```

### 3. Done -- No Other Code Changes

All your existing ad loading code works automatically:

```kotlin
// These calls now go through the waterfall automatically
AdManager.getInstance().loadInterstitialAd(this, "ca-app-pub-xxx/interstitial")
bannerAdView.loadBanner(this, "ca-app-pub-xxx/banner")
nativeBannerSmall.loadNativeBannerAd(this, "ca-app-pub-xxx/native")
```

## How It Works

### Ad Unit ID Resolution

When a waterfall is active, the ad unit ID you pass (e.g., your AdMob ID) is used as a **lookup key**:

- **For AdMob**: The raw ID is used directly (fallback behavior)
- **For Yandex**: `AdUnitMapping` resolves it to the Yandex ad unit ID

This means you never need to change existing ad unit IDs in your code. Just register the mappings once in your Application class.

### Dual-Path Architecture

Each old API class checks `AdProviderConfig.getXxxChain().isNotEmpty()` at every entry point. If chains are configured, the waterfall path runs. Otherwise, the existing AdMob-direct code runs unchanged.

## Provider Chain Configuration

### AdMob First (Default/Recommended)

```kotlin
AdProviderConfig.setInterstitialChain(listOf(
    admob.interstitialProvider,
    yandex.interstitialProvider
))
```

### Yandex First (e.g., for Russia)

```kotlin
AdProviderConfig.setInterstitialChain(listOf(
    yandex.interstitialProvider,
    admob.interstitialProvider
))
```

### Region-Based Configuration

```kotlin
val isRussia = Locale.getDefault().country == "RU"

if (isRussia) {
    AdProviderConfig.setInterstitialChain(listOf(yandex.interstitialProvider, admob.interstitialProvider))
} else {
    AdProviderConfig.setInterstitialChain(listOf(admob.interstitialProvider, yandex.interstitialProvider))
}
```

## Supported Ad Types

| Ad Type | Waterfall Class | Provider Interface |
|---------|----------------|-------------------|
| Interstitial | `InterstitialWaterfall` | `InterstitialAdProvider` |
| Banner | `BannerWaterfall` | `BannerAdProvider` |
| Native | `NativeWaterfall` | `NativeAdProvider` |
| App Open | `AppOpenWaterfall` | `AppOpenAdProvider` |
| Rewarded | `RewardedWaterfall` | `RewardedAdProvider` |

## Features Preserved in Waterfall Mode

All existing features continue to work when the waterfall is active:

- Time-based and count-based interstitial display
- Loading strategies (ON_DEMAND, ONLY_CACHE, HYBRID)
- Shimmer loading placeholders
- Loading dialogs for interstitials
- Welcome back dialog for app open ads
- Auto-reload after dismissal
- Firebase Analytics events
- Premium user ad suppression
- Debug overlays
- Retry with exponential backoff

## Disabling Waterfall

To disable the waterfall and revert to direct AdMob, simply don't configure any chains:

```kotlin
// Remove or skip the configureMultiProvider() call
// All existing code will use AdMob directly
```

Or clear chains at runtime:

```kotlin
AdProviderConfig.setInterstitialChain(emptyList())
```
