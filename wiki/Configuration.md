# Configuration - AdManageKit v4.4.2

## Overview

`AdManageKitConfig` is the central configuration object for all AdManageKit features. Configure it once in your `Application.onCreate()` and all ad components will respect these settings.

## Basic Setup

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure billing first
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

        // Configure AdManageKit
        AdManageKitConfig.apply {
            // Debug settings
            debugMode = BuildConfig.DEBUG
            testMode = false

            // Loading strategies
            interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
            appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
            nativeLoadingStrategy = AdLoadingStrategy.HYBRID

            // Auto-reload (v2.8.0+)
            interstitialAutoReload = true
        }
    }
}
```

> **Since 4.2.0 you must also call `MobileAds.initialize()` yourself**, before any ad request — the Next-Gen SDK removed the legacy SDK's silent lazy-init. `AdManageKitConfig` only configures this library's behavior; it does not initialize the ads SDK. See [[Home]] for the initialization pattern.

## All Configuration Options

### Performance Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `defaultAdTimeout` | Timeout for ad loading | 15 seconds |
| `nativeCacheExpiry` | Cache expiry for native ads | 1 hour |
| `maxCachedAdsPerUnit` | Max cached ads per unit | 3 |

```kotlin
AdManageKitConfig.apply {
    defaultAdTimeout = 15.seconds
    nativeCacheExpiry = 1.hours
    maxCachedAdsPerUnit = 3
}
```

### Reliability Features

| Setting | Description | Default |
|---------|-------------|---------|
| `autoRetryFailedAds` | Enable auto retry | true |
| `maxRetryAttempts` | Max retry attempts | 3 |
| `circuitBreakerThreshold` | Failures before circuit trips | 5 |
| `circuitBreakerResetTimeout` | Circuit reset timeout | 5 minutes |

```kotlin
AdManageKitConfig.apply {
    autoRetryFailedAds = true
    maxRetryAttempts = 3
    circuitBreakerThreshold = 5
    circuitBreakerResetTimeout = 300.seconds
}
```

### Advanced Features

| Setting | Description | Default |
|---------|-------------|---------|
| `enableSmartPreloading` | Smart preload based on usage | false |
| `enableAdaptiveIntervals` | Adjust intervals by success rate | false |
| `enablePerformanceMetrics` | Enable metrics collection | false |
| `enableAutoCacheCleanup` | Auto cleanup on low memory | true |

```kotlin
AdManageKitConfig.apply {
    enableSmartPreloading = true
    enableAdaptiveIntervals = true
    enablePerformanceMetrics = BuildConfig.DEBUG
    enableAutoCacheCleanup = true
}
```

### Debug & Testing

| Setting | Description | Default |
|---------|-------------|---------|
| `debugMode` | Enable debug logging | false |
| `testMode` | Use test ads | false |
| `privacyCompliantMode` | GDPR/CCPA compliance | true |
| `enableDebugOverlay` | Show debug overlay | false |

```kotlin
AdManageKitConfig.apply {
    debugMode = BuildConfig.DEBUG
    testMode = false
    privacyCompliantMode = true
    enableDebugOverlay = BuildConfig.DEBUG
}
```

### Interstitial Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `defaultInterstitialInterval` | Time between ads | 15 seconds |
| `interstitialAutoReload` | Auto-reload after show | true |
| `interstitialLoadingStrategy` | Loading strategy | HYBRID |

```kotlin
AdManageKitConfig.apply {
    defaultInterstitialInterval = 20.seconds
    interstitialAutoReload = true
    interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
}
```

### Banner Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `defaultBannerRefreshInterval` | Banner refresh interval (fallback when no per-view interval is set) | 60 seconds |
| `enableCollapsibleBannersByDefault` | Enable collapsible | false |
| `defaultCollapsiblePlacement` | Collapsible position | BOTTOM |

Banner **size** is per-view rather than global — set it via `app:bannerAdSize` in XML, `setBannerAdSize()`, the `adSize` load parameter, or the `adSize` argument of `BannerAdCompose`. See [[Banner Ads]].

```kotlin
AdManageKitConfig.apply {
    defaultBannerRefreshInterval = 60.seconds
    enableCollapsibleBannersByDefault = false
    defaultCollapsiblePlacement = CollapsibleBannerPlacement.BOTTOM
}
```

### Native Ad Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `nativeLoadingStrategy` | Loading strategy | HYBRID |
| `nativeCacheExpiry` | Cache expiry for native ads | 1 hour |
| `defaultNativeMediaAspect` | Media aspect-ratio hint sent with every native request (v4.3.3+) | ANY |
| `nativeVideoStartMuted` | Start video creatives muted (v4.3.3+) | true |
| `nativeVideoClickToExpand` | Allow click-to-expand on video creatives (v4.3.3+) | false |
| `nativeVideoCustomControls` | Request custom video controls (v4.3.3+) | false |

`NativeMediaAspect` values: `UNSPECIFIED`, `ANY`, `LANDSCAPE`, `PORTRAIT`, `SQUARE`. `NativeTemplateView.setMediaAspect(...)` overrides the global default per view.

```kotlin
AdManageKitConfig.apply {
    nativeLoadingStrategy = AdLoadingStrategy.HYBRID
    nativeCacheExpiry = 1.hours
    defaultNativeMediaAspect = NativeMediaAspect.ANY
    nativeVideoStartMuted = true
}
```

### App Open Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `appOpenLoadingStrategy` | Loading strategy | HYBRID |
| `appOpenAdFreshnessThreshold` | Max age of a usable cached ad | 4 hours |
| `appOpenAdTimeout` | Load timeout | 10 seconds |
| `appOpenFetchFreshAd` | Disable background prefetch, fetch on foreground | false |
| `appOpenAutoReload` | Reload after dismissal | true |

```kotlin
AdManageKitConfig.apply {
    appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
    appOpenAdFreshnessThreshold = 4.hours
    appOpenAdTimeout = 10.seconds
    appOpenFetchFreshAd = false
    appOpenAutoReload = true
}
```

`appOpenAdFreshnessThreshold` is enforced by **every** strategy as of v4.3.5 — a cached ad older than the threshold is discarded and replaced rather than shown. `appOpenFetchFreshAd` controls fetch *timing* only (background prefetch vs. foreground fetch), not whether a cached ad is used. See [[App Open Ads]].

### Dialog Customization

| Setting | Description | Default |
|---------|-------------|---------|
| `dialogBackgroundColor` | Dialog background | transparent |
| `dialogOverlayColor` | Overlay color | 50% black |
| `dialogCardBackgroundColor` | Card background | theme default |
| `welcomeDialogAppIcon` | App icon resource | 0 |
| `welcomeDialogTitle` | Title text | "Welcome Back!" |
| `welcomeDialogSubtitle` | Subtitle text | "Loading..." |
| `welcomeDialogFooter` | Footer text | "Just a moment..." |
| `welcomeDialogDismissDelay` | Dismiss delay | 0.8 seconds |
| `loadingDialogTitle` | Interstitial title | "Loading Ad" |
| `loadingDialogSubtitle` | Interstitial subtitle | "Please wait..." |

```kotlin
AdManageKitConfig.apply {
    welcomeDialogAppIcon = R.mipmap.ic_launcher
    welcomeDialogTitle = "Welcome Back!"
    dialogOverlayColor = 0x80000000.toInt()
}
```

### Cache Management

| Setting | Description | Default |
|---------|-------------|---------|
| `maxCacheMemoryMB` | Max cache memory | 200 MB |
| `enableLRUEviction` | LRU cache eviction | true |
| `cacheCleanupInterval` | Cleanup interval | 30 minutes |

```kotlin
AdManageKitConfig.apply {
    maxCacheMemoryMB = 200
    enableLRUEviction = true
    cacheCleanupInterval = 30.seconds * 60
}
```

### Network Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `enableExponentialBackoff` | Exponential retry | true |
| `baseRetryDelay` | First retry delay | 1 second |
| `maxRetryDelay` | Max retry delay | 30 seconds |

```kotlin
AdManageKitConfig.apply {
    enableExponentialBackoff = true
    baseRetryDelay = 1.seconds
    maxRetryDelay = 30.seconds
}
```

## Utility Methods

### Reset to Defaults

```kotlin
AdManageKitConfig.resetToDefaults()
```

### Validate Configuration

```kotlin
val isValid = AdManageKitConfig.validate()
// Logs warnings in debug mode for invalid settings
```

### Get Summary

```kotlin
val summary = AdManageKitConfig.getConfigSummary()
Log.d("Config", summary)
```

### Production Ready Check

```kotlin
if (AdManageKitConfig.isProductionReady()) {
    // Configuration is suitable for production
}
```

## Best Practices

1. **Configure early** - Set in `Application.onCreate()`
2. **Use BuildConfig** - Set `debugMode = BuildConfig.DEBUG`
3. **Validate before release** - Use `isProductionReady()`
4. **Different strategies** - Use appropriate strategies per ad type
5. **Privacy first** - Keep `privacyCompliantMode = true`

## References

- [GitHub Repository](https://github.com/i2hammad/AdManageKit)
- [[Ad Loading Strategies]]
- [[Interstitial Ads]]
