# Native Ads - AdManageKit v4.4.1

## Overview

AdManageKit provides comprehensive native ad support with caching, multiple formats, and the new `NativeTemplateView` (v2.6.0+). Native ads blend seamlessly with your app's content while maximizing engagement.

**Library Version**: v4.4.1

## Features

- **Multiple Formats**: Small, Medium, Large, and 38 template styles
- **Smart Caching**: Per-unit caching with 1-hour expiration
- **Loading Strategies**: ON_DEMAND, ONLY_CACHE, HYBRID, FRESH_WITH_CACHE_FALLBACK
- **Shimmer Loading**: Beautiful loading placeholders
- **NativeTemplateView**: Unified component with 38 templates (v2.6.0+)
- **Video Support**: All templates support video ads

## Installation

```groovy
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.1'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.1'
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

38 template styles in one unified component. A selection:

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
| `app_store`, `social_feed`, `gradient_card`, `spotlight` | Themed placements |
| `flat_*` family (10 styles) | Flat/minimal design systems |

See [`attrs.xml`](https://github.com/i2hammad/AdManageKit/blob/main/AdManageKit/src/main/res/values/attrs.xml) for the full `adTemplate` enum.

### Custom Templates (v4.3.0+)

If none of the 38 presets fit, supply your own layout. The root must be a Next-Gen SDK `NativeAdView` reusing the standard asset ids (`ad_headline`, `ad_body`, `ad_call_to_action`, `ad_app_icon`, `ad_advertiser`, `ad_media`).

```kotlin
nativeTemplateView.setCustomTemplate(
    layoutResId = R.layout.my_native_ad,
    shimmerResId = R.layout.my_shimmer,   // optional
    sizeHint = NativeAdSize.MEDIUM        // optional, default MEDIUM
)
```

Or in XML via `app:customAdLayout` / `app:customAdShimmerLayout`, or `customLayoutResId` on `NativeTemplateCompose`.

### Media Aspect Ratio (v4.3.3+)

Every native request carries a media aspect-ratio hint matched to the template's `MediaView` slot. Override it globally with `AdManageKitConfig.defaultNativeMediaAspect`, or per view:

```kotlin
nativeTemplateView.setMediaAspect(NativeMediaAspect.LANDSCAPE)
```

Values: `UNSPECIFIED`, `ANY` (default), `LANDSCAPE`, `PORTRAIT`, `SQUARE`.

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
| FRESH_WITH_CACHE_FALLBACK | Fetch fresh, fall back to cache if the load fails |

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

1. **Use NativeTemplateView** - Unified API with 38 templates
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
