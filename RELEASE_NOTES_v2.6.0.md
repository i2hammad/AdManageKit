# AdManageKit v2.6.0 Release Notes

**Release Date:** November 27, 2024

## Highlights

This release introduces **NativeTemplateView** - a unified native ad component supporting 17 different template styles, **Ad Loading Strategies** for fine-grained control over ad loading behavior, and comprehensive **Material 3 theme support** for automatic dark/light mode.

---

## New Features

### NativeTemplateView

A single, flexible component that replaces the need for multiple native ad views.

**17 Template Styles:**

| Standard Templates | Video Templates |
|-------------------|-----------------|
| card_modern | video_small |
| material3 | video_medium |
| minimal | video_large |
| compact_horizontal | video_square |
| full_width_banner | video_vertical |
| list_item | video_fullscreen |
| grid_card | |
| featured | |
| overlay_dark | |
| story_style | |
| magazine | |

**Usage:**

```xml
<com.i2hammad.admanagekit.admob.NativeTemplateView
    android:id="@+id/nativeTemplateView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adTemplate="material3" />
```

```kotlin
// Programmatic usage
nativeTemplateView.setTemplate(NativeAdTemplate.MAGAZINE)
nativeTemplateView.loadNativeAd(activity, adUnitId)

// With loading strategy override
nativeTemplateView.loadNativeAd(activity, adUnitId, callback, AdLoadingStrategy.ONLY_CACHE)
```

**Features:**
- XML attribute support (`app:adTemplate`)
- Programmatic template switching
- XML Preview in Android Studio designer
- Shimmer loading animation
- Integration with NativeAdManager caching

---

### Ad Loading Strategies

Three strategies to control how ads are loaded and displayed:

| Strategy | Behavior | Best For |
|----------|----------|----------|
| `ON_DEMAND` | Always fetch fresh ad with loading dialog | Critical monetization points |
| `ONLY_CACHE` | Instant display from cache, skip if unavailable | Games, frequent triggers |
| `HYBRID` | Check cache first, fetch if needed | General use (recommended) |

**Configuration:**

```kotlin
AdManageKitConfig.apply {
    interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
    appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
    nativeLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
}
```

See [Ad Loading Strategies Guide](docs/AD_LOADING_STRATEGIES.md) for detailed documentation.

---

### Material 3 Theme Support

All templates now use Material 3 theme attributes for automatic dark/light mode support:

- `?attr/colorSurface` - Background
- `?attr/colorOnSurface` - Primary text
- `?attr/colorOnSurfaceVariant` - Secondary text
- `?attr/colorPrimary` - CTA button
- `?attr/colorOutlineVariant` - Borders, dividers

No configuration needed - templates automatically adapt to your app's theme.

---

### Enhanced Video Ad Support

- **Minimum 120dp MediaView**: All templates support video ads with proper sizing
- **6 Video-Optimized Templates**: Designed specifically for video ad content
- **Proper Aspect Ratios**: Square, vertical, and fullscreen options

---

## Improvements

### Smart View Handling

- **GONE vs INVISIBLE**: Views now use `GONE` instead of `INVISIBLE` when content is missing, ensuring proper layout reflow
- **Container Auto-Hide**: Advertiser container automatically hides when both advertiser name and star rating are null
- **Proper Alignment**: RelativeLayout ensures correct alignment when optional items are missing

### AdChoices Support

- Automatic visibility handling for AdChoices view
- Hidden by default, shown when ad has AdChoices content

### Jetpack Compose Support

New Compose components for NativeTemplateView and loading strategies:

**NativeTemplateCompose:**
```kotlin
// Use any of 17 templates in Compose
NativeTemplateCompose(
    adUnitId = "ca-app-pub-xxx/yyy",
    template = NativeAdTemplate.MATERIAL3,
    loadingStrategy = AdLoadingStrategy.HYBRID,
    onAdLoaded = { /* success */ }
)

// Convenience functions for common templates
NativeCardModernCompose(adUnitId = adUnitId)
NativeMaterial3Compose(adUnitId = adUnitId)
NativeMinimalCompose(adUnitId = adUnitId)
NativeListItemCompose(adUnitId = adUnitId)
NativeMagazineCompose(adUnitId = adUnitId)
NativeFeaturedCompose(adUnitId = adUnitId)
NativeVideoMediumCompose(adUnitId = adUnitId)
NativeVideoLargeCompose(adUnitId = adUnitId)
```

**Loading Strategy Support in Existing Compose Components:**
```kotlin
// All native ad Compose functions now support loadingStrategy parameter
NativeBannerSmallCompose(
    adUnitId = adUnitId,
    loadingStrategy = AdLoadingStrategy.ONLY_CACHE
)

NativeBannerMediumCompose(
    adUnitId = adUnitId,
    loadingStrategy = AdLoadingStrategy.HYBRID
)

NativeLargeCompose(
    adUnitId = adUnitId,
    loadingStrategy = AdLoadingStrategy.ON_DEMAND
)
```

### XML Preview

- `NativeTemplateView` shows actual template layout in Android Studio designer
- Preview displays placeholder data instead of shimmer
- Helps visualize ad placement during development

---

## Bug Fixes

### Splash Screen Offline Mode Fix

**Issue:** App would get stuck on splash screen when device is offline
- UMP consent request failed with "Error making request"
- `canRequestAds()` returned `false`
- No fallback path existed, causing the app to freeze

**Fix:** Added fallback navigation when ads cannot be requested, allowing the app to proceed to the next screen in offline mode.

### Native Ad View Fixes

- Fixed duplicate `starRatingView` assignment
- Removed unused `priceView` and `storeView` references
- Fixed hardcoded "4.5" rating text in material3 template
- Improved star rating check (now checks `starRating > 0`)

---

## Breaking Changes

None. This release is fully backward compatible. Existing `NativeBannerSmall`, `NativeBannerMedium`, and `NativeLarge` views continue to work as expected.

---

## Migration Guide

### Optional: Adopt NativeTemplateView

```kotlin
// Old way (still works)
val nativeBannerMedium = NativeBannerMedium(context)
nativeBannerMedium.loadNativeBannerAd(activity, adUnitId)

// New unified approach
val nativeTemplateView = NativeTemplateView(context)
nativeTemplateView.setTemplate(NativeAdTemplate.CARD_MODERN)
nativeTemplateView.loadNativeAd(activity, adUnitId)
```

### Optional: Configure Loading Strategies

```kotlin
// In Application.onCreate()
AdManageKitConfig.apply {
    interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
    appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
    nativeLoadingStrategy = AdLoadingStrategy.HYBRID
}
```

---

## Dependencies

No dependency changes in this release.

---

## Documentation

- [NativeTemplateView Guide](docs/NATIVE_TEMPLATE_VIEW.md)
- [Ad Loading Strategies Guide](docs/AD_LOADING_STRATEGIES.md)
- [README](README.md)

---

## Installation

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.6.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.6.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.6.0'

// For Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.6.0'
```

---

## What's Next

- Additional template styles based on user feedback
- Compose support for NativeTemplateView
- Enhanced analytics for loading strategies

---

## Feedback

For issues or feature requests, please open an issue on [GitHub](https://github.com/i2hammad/AdManageKit/issues).
