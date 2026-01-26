# Ad Loading Strategies - AdManageKit v2.8.0

## Overview

AdManageKit v2.6.0+ introduces three loading strategies that control how ads are fetched and displayed. Choose the right strategy based on your app's needs for user experience vs. ad coverage.

## Strategy Types

### 1. ON_DEMAND

**Always fetch fresh ads with loading UI**

```kotlin
AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND
```

| Pros | Cons |
|------|------|
| Maximum ad coverage | May interrupt user flow |
| Always fresh ads | Loading dialog shown |
| Best for important moments | Slower on poor network |

**Best for:**
- Important monetization points
- After significant user actions
- When you need maximum coverage

### 2. ONLY_CACHE

**Only show ads already preloaded**

```kotlin
AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
```

| Pros | Cons |
|------|------|
| Instant display | Lower ad coverage |
| Smooth UX | Skips if not cached |
| No loading dialogs | Requires good preloading |

**Best for:**
- Frequent ad opportunities
- During gameplay
- When UX is priority

### 3. HYBRID (Recommended)

**Check cache first, fetch if needed**

```kotlin
AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
```

| Pros | Cons |
|------|------|
| Instant when cached | May show dialog if not cached |
| Falls back to fetch | Slightly complex flow |
| Best of both worlds | - |

**Best for:**
- Most general use cases
- Default recommendation
- Balanced UX and coverage

## Strategy Availability

| Ad Type | ON_DEMAND | ONLY_CACHE | HYBRID |
|---------|-----------|------------|--------|
| Interstitial | ✅ | ✅ | ✅ |
| App Open | ✅ | ✅ | ✅ |
| Native | ✅ | ❌ | ✅ |

> **Note:** Native ads use shimmer instead of dialog, so ONLY_CACHE hides container if not cached.

## Configuration

### Global Settings

```kotlin
// In Application.onCreate()
AdManageKitConfig.apply {
    interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
    appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
    nativeLoadingStrategy = AdLoadingStrategy.HYBRID
}
```

### Per-Call Override

```kotlin
// InterstitialAdBuilder
InterstitialAdBuilder.with(activity)
    .adUnit(adUnitId)
    .loadingStrategy(AdLoadingStrategy.ONLY_CACHE)
    .show { next() }

// NativeTemplateView
nativeTemplateView.loadNativeAd(activity, adUnitId, callback, AdLoadingStrategy.ON_DEMAND)
```

## Flow Diagrams

### Interstitial ON_DEMAND
```
User triggers → Show dialog → Fetch ad →
    Loaded: Show ad → Continue
    Timeout: Skip → Continue
```

### Interstitial ONLY_CACHE
```
User triggers → Check cache →
    Cached: Show ad → Continue
    Not cached: Skip → Continue
```

### Interstitial HYBRID
```
User triggers → Check cache →
    Cached: Show ad → Continue
    Not cached: Show dialog → Fetch →
        Loaded: Show ad → Continue
        Timeout: Skip → Continue
```

### Native HYBRID
```
Load → Check cache →
    Cached: Show immediately
    Not cached: Show shimmer → Fetch →
        Success: Show ad
        Failure: Hide container
```

## Strategy Selection Guide

| Use Case | Recommended |
|----------|-------------|
| Level completion | HYBRID |
| During gameplay | ONLY_CACHE |
| Navigation | ONLY_CACHE |
| Long tasks | ON_DEMAND |
| App open/resume | HYBRID |
| Cold start | ON_DEMAND |
| Feed/list items | ONLY_CACHE |
| Article content | HYBRID |

## Example Configurations

### Gaming App

```kotlin
AdManageKitConfig.apply {
    interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
    appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
    nativeLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
}
```

### Utility App

```kotlin
AdManageKitConfig.apply {
    interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND
    appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND
    nativeLoadingStrategy = AdLoadingStrategy.ON_DEMAND
}
```

### Content App

```kotlin
AdManageKitConfig.apply {
    interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
    appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
    nativeLoadingStrategy = AdLoadingStrategy.HYBRID
}
```

## v2.8.0 Updates

**`forceShowInterstitial()` now respects loading strategy!**

```kotlin
// Before v2.8.0: Always fetched fresh
// After v2.8.0: Respects interstitialLoadingStrategy

AdManager.getInstance().forceShowInterstitial(activity, callback)

// To always force fetch (old behavior):
AdManager.getInstance().forceShowInterstitialAlways(activity, callback)
```

## Best Practices

1. **Set once** in `Application.onCreate()`
2. **Use HYBRID** as default
3. **Use ONLY_CACHE** for frequent interruptions
4. **Use ON_DEMAND** for critical moments
5. **Enable background preloading** for ONLY_CACHE/HYBRID
6. **Monitor coverage** and adjust based on metrics

## References

- [GitHub Repository](https://github.com/i2hammad/AdManageKit)
- [[Interstitial Ads]]
- [[App Open Ads]]
- [[Native Ads|NativeAdManager]]
