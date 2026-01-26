# Native Ads - AdManageKit v2.8.0

## Overview

AdManageKit provides comprehensive native ad support with caching, multiple formats, and the new `NativeTemplateView` (v2.6.0+). Native ads blend seamlessly with your app's content while maximizing engagement.

**Library Version**: v2.8.0

## Features

- **Multiple Formats**: Small, Medium, Large, and 17+ template styles
- **Smart Caching**: Per-unit caching with 1-hour expiration
- **Loading Strategies**: ON_DEMAND, ONLY_CACHE, HYBRID
- **Shimmer Loading**: Beautiful loading placeholders
- **NativeTemplateView**: Unified component with 17 templates (v2.6.0+)
- **Video Support**: All templates support video ads

## Installation

```groovy
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.8.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.8.0'
}
```

## Native Ad Formats

### Traditional Views

| Class | Best For |
|-------|----------|
| `NativeBannerSmall` | Compact spaces, lists |
| `NativeBannerMedium` | General content areas |
| `NativeLarge` | Featured placements |

### NativeTemplateView (v2.6.0+)

17 template styles in one unified component:

| Template | Best For |
|----------|----------|
| `card_modern` | General use |
| `material3` | Material 3 apps |
| `minimal` | Content-focused |
| `compact_horizontal` | Lists |
| `list_item` | RecyclerView items |
| `magazine` | News/blog apps |
| `video_small/medium/large` | Video content |
| `video_square/vertical/fullscreen` | Social feeds |
| `featured`, `grid_card`, `overlay_dark`, `story_style` | Various layouts |

## Usage

### NativeTemplateView (Recommended)

**XML:**
```xml
<com.i2hammad.admanagekit.admob.NativeTemplateView
    android:id="@+id/nativeTemplateView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adTemplate="material3" />
```

**Kotlin:**
```kotlin
// Load with default template
nativeTemplateView.loadNativeAd(activity, "ca-app-pub-xxx/yyy")

// Change template
nativeTemplateView.setTemplate(NativeAdTemplate.MAGAZINE)
nativeTemplateView.loadNativeAd(activity, "ca-app-pub-xxx/yyy")

// With callback
nativeTemplateView.loadNativeAd(activity, adUnitId, object : AdLoadCallback() {
    override fun onAdLoaded() { /* success */ }
    override fun onFailedToLoad(error: AdError?) { /* error */ }
})

// With strategy override
nativeTemplateView.loadNativeAd(activity, adUnitId, callback, AdLoadingStrategy.ONLY_CACHE)
```

### Traditional Views

```kotlin
// NativeBannerSmall
val nativeBannerSmall = NativeBannerSmall(context)
nativeBannerSmall.loadNativeBannerAd(activity, "ca-app-pub-xxx/yyy")

// NativeBannerMedium
val nativeBannerMedium = NativeBannerMedium(context)
nativeBannerMedium.loadNativeBannerAd(activity, "ca-app-pub-xxx/yyy", useCachedAd = true)

// NativeLarge
val nativeLarge = NativeLarge(context)
nativeLarge.loadNativeAds(activity, "ca-app-pub-xxx/yyy")
```

## Caching System

### Configuration

```kotlin
AdManageKitConfig.apply {
    nativeLoadingStrategy = AdLoadingStrategy.HYBRID
    nativeCacheExpiry = 1.hours
    maxCachedAdsPerUnit = 3
    enableLRUEviction = true
    maxCacheMemoryMB = 200
}
```

### NativeAdManager API

```kotlin
// Enable/disable caching globally
NativeAdManager.enableCachingNativeAds = true

// Clear cache for specific unit
NativeAdManager.clearCachedAd("ad-unit-id")

// Clear all cached ads
NativeAdManager.clearAllCachedAds()

// Get cached ad
val cachedAd = NativeAdManager.getCachedNativeAd("ad-unit-id")
```

### Cache Behavior

- **Per-Unit Caching**: Each ad unit has its own cache
- **1-Hour Expiration**: Cached ads expire after 1 hour
- **Automatic Cleanup**: Expired ads are automatically destroyed
- **Memory Management**: LRU eviction when cache is full

## Loading Strategies

| Strategy | Behavior |
|----------|----------|
| ON_DEMAND | Show shimmer, fetch fresh ad |
| ONLY_CACHE | Show cached or hide container |
| HYBRID | Show cached if ready, fetch with shimmer otherwise |

```kotlin
// Global strategy
AdManageKitConfig.nativeLoadingStrategy = AdLoadingStrategy.HYBRID

// Per-call override
nativeTemplateView.loadNativeAd(activity, adUnitId, callback, AdLoadingStrategy.ONLY_CACHE)
```

### Strategy Flow

**ON_DEMAND/HYBRID (not cached):**
```
Load → Show shimmer → Fetch ad →
    Success: Show ad, hide shimmer
    Failure: Hide container
```

**ONLY_CACHE:**
```
Load → Check cache →
    Cached: Show immediately
    Not cached: Hide container
```

## API Reference

### NativeTemplateView Methods

| Method | Description |
|--------|-------------|
| `setTemplate(template)` | Set template style |
| `loadNativeAd(activity, adUnitId)` | Load with defaults |
| `loadNativeAd(activity, adUnitId, callback)` | Load with callback |
| `loadNativeAd(activity, adUnitId, callback, strategy)` | Load with strategy |

### NativeAdManager Methods

| Method | Description |
|--------|-------------|
| `enableCachingNativeAds` | Enable/disable caching |
| `getCachedNativeAd(adUnitId)` | Get cached ad |
| `setCachedNativeAd(adUnitId, ad)` | Store ad in cache |
| `clearCachedAd(adUnitId)` | Clear specific cache |
| `clearAllCachedAds()` | Clear all caches |

### AdManageKitConfig Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `nativeLoadingStrategy` | Loading strategy | HYBRID |
| `nativeCacheExpiry` | Cache expiration | 1 hour |
| `maxCachedAdsPerUnit` | Ads per unit | 3 |
| `maxCacheMemoryMB` | Max cache memory | 200 MB |
| `enableLRUEviction` | LRU eviction | true |
| `enableAutoCacheCleanup` | Auto cleanup | true |

## Best Practices

1. **Use NativeTemplateView** - Unified API with 17 templates
2. **Enable Caching** - Improves load times and reduces requests
3. **Use HYBRID Strategy** - Best balance for most cases
4. **Clear on Destroy** - Call `clearAllCachedAds()` in `onDestroy()`
5. **Match Template to Context** - Use appropriate template for placement

## Troubleshooting

- **Ad Not Showing**: Check cache status, strategy, network
- **Shimmer Forever**: Verify adUnitId and network connectivity
- **Memory Issues**: Enable `enableLRUEviction` and set `maxCacheMemoryMB`

## References

- [AdMob Native Ads](https://developers.google.com/admob/android/native/start)
- [NativeTemplateView Guide](https://github.com/i2hammad/AdManageKit/blob/main/docs/NATIVE_TEMPLATE_VIEW.md)
- [GitHub Repository](https://github.com/i2hammad/AdManageKit)
