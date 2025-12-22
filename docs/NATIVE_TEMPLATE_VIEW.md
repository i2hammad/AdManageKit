# NativeTemplateView Guide

**New in v2.6.0** - A unified native ad component supporting 28 different template styles with Material 3 theming and video support.

## Overview

`NativeTemplateView` is a single, flexible component that replaces the need for multiple native ad views. It supports 28 different visual templates that can be set via XML attributes or programmatically.

## Features

- **28 Template Styles**: From minimal to icon-left, compact to fullscreen video
- **Material 3 Theming**: Automatic dark/light mode support
- **Video Support**: All templates support video ads (120dp+ MediaView)
- **Shimmer Loading**: Beautiful loading animation
- **XML Preview**: See actual template in Android Studio designer
- **Smart View Handling**: Auto-hide empty containers, proper alignment
- **AdChoices Support**: Automatic visibility handling
- **Caching Integration**: Works with NativeAdManager caching system
- **Loading Strategy Support**: ON_DEMAND, ONLY_CACHE, or HYBRID

## Available Templates

### Standard Templates

| Template | Enum Value | Description |
|----------|-----------|-------------|
| Card Modern | `CARD_MODERN` | Modern card with rounded corners - general purpose |
| Material 3 | `MATERIAL3` | Material Design 3 style with M3 components |
| Minimal | `MINIMAL` | Clean, minimal design for content-focused apps |
| Compact Horizontal | `COMPACT_HORIZONTAL` | Horizontal layout, 120dp height |
| Story Style | `STORY_STYLE` | Story/feed style for social apps |
| Full Width Banner | `FULL_WIDTH_BANNER` | Full-width banner style |
| Grid Card | `GRID_CARD` | Square card for grid layouts |
| List Item | `LIST_ITEM` | List item style with divider |
| Featured | `FEATURED` | Large featured card for hero sections |
| Overlay Dark | `OVERLAY_DARK` | Dark overlay on media |
| Magazine | `MAGAZINE` | Magazine article style for news/blog apps |
| Media Content Split | `MEDIA_CONTENT_SPLIT` | Media/content split layout |
| App Store | `APP_STORE` | Store listing style with install CTA |
| Social Feed | `SOCIAL_FEED` | Sponsored post layout for feeds |
| Gradient Card | `GRADIENT_CARD` | Hero card with gradient overlay |
| Pill Banner | `PILL_BANNER` | Pill-shaped compact banner |
| Medium Horizontal | `MEDIUM_HORIZONTAL` | 55/45 media-content horizontal split |
| Spotlight | `SPOTLIGHT` | Centered hero with large icon |
| Flexible | `FLEXIBLE` | Adaptive card with media focus |
| Grid Item | `GRID_ITEM` | Compact grid item with centered content |
| Top Icon Media | `TOP_ICON_MEDIA` | Top icon + headline, media center, CTA footer |
| Icon Left | `ICON_LEFT` | Left icon column with stacked content |

### Video Templates

| Template | Enum Value | Description |
|----------|-----------|-------------|
| Video Small | `VIDEO_SMALL` | Small video-optimized (120x120dp) |
| Video Medium | `VIDEO_MEDIUM` | Medium video-optimized (180dp) |
| Video Large | `VIDEO_LARGE` | Large video-optimized (250dp) |
| Video Square | `VIDEO_SQUARE` | Square format (300x300dp) |
| Video Vertical | `VIDEO_VERTICAL` | Vertical format (9:16 ratio) |
| Video Fullscreen | `VIDEO_FULLSCREEN` | Fullscreen video ad |

## XML Usage

### Basic Usage

```xml
<com.i2hammad.admanagekit.admob.NativeTemplateView
    android:id="@+id/nativeTemplateView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adTemplate="card_modern" />
```

### All Template Options

```xml
<!-- Standard templates -->
app:adTemplate="card_modern"
app:adTemplate="material3"
app:adTemplate="minimal"
app:adTemplate="compact_horizontal"
app:adTemplate="story_style"
app:adTemplate="full_width_banner"
app:adTemplate="grid_card"
app:adTemplate="list_item"
app:adTemplate="featured"
app:adTemplate="overlay_dark"
app:adTemplate="magazine"
app:adTemplate="media_content_split"
app:adTemplate="app_store"
app:adTemplate="social_feed"
app:adTemplate="gradient_card"
app:adTemplate="pill_banner"
app:adTemplate="medium_horizontal"
app:adTemplate="spotlight"
app:adTemplate="flexible"
app:adTemplate="grid_item"
app:adTemplate="top_icon_media"
app:adTemplate="icon_left"

<!-- Video templates -->
app:adTemplate="video_small"
app:adTemplate="video_medium"
app:adTemplate="video_large"
app:adTemplate="video_square"
app:adTemplate="video_vertical"
app:adTemplate="video_fullscreen"
```

### In RecyclerView

```xml
<!-- item_native_ad.xml -->
<com.i2hammad.admanagekit.admob.NativeTemplateView
    android:id="@+id/nativeTemplateView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adTemplate="list_item" />
```

## Programmatic Usage

### Basic Loading

```kotlin
// Simple load with default template
nativeTemplateView.loadNativeAd(activity, "ca-app-pub-xxx/yyy")
```

### Setting Template Programmatically

```kotlin
// Using enum
nativeTemplateView.setTemplate(NativeAdTemplate.MAGAZINE)
nativeTemplateView.loadNativeAd(activity, adUnitId)

// Using string
nativeTemplateView.setTemplate("material3")
nativeTemplateView.loadNativeAd(activity, adUnitId)
```

### With Callback

```kotlin
nativeTemplateView.loadNativeAd(activity, adUnitId, object : AdLoadCallback() {
    override fun onAdLoaded() {
        Log.d("Ad", "Native ad loaded successfully")
    }

    override fun onFailedToLoad(error: AdError?) {
        Log.e("Ad", "Failed to load: ${error?.message}")
    }

    override fun onAdImpression() {
        Log.d("Ad", "Ad impression recorded")
    }

    override fun onAdClicked() {
        Log.d("Ad", "Ad clicked")
    }
})
```

### With Loading Strategy Override

```kotlin
// Override the global loading strategy for this specific ad
nativeTemplateView.loadNativeAd(
    activity = activity,
    adUnitId = "ca-app-pub-xxx/yyy",
    adCallback = myCallback,
    loadingStrategy = AdLoadingStrategy.ONLY_CACHE
)
```

### Display Preloaded Ad

```kotlin
// Get a cached ad
val preloadedAd = NativeAdManager.getCachedNativeAd("ad-unit-id")

// Display it in the template view
preloadedAd?.let { nativeAd ->
    nativeTemplateView.displayAd(nativeAd)
}
```

### Utility Methods

```kotlin
// Get current template
val currentTemplate = nativeTemplateView.getTemplate()

// Hide ad
nativeTemplateView.hideAd()

// Show ad
nativeTemplateView.showAd()

// Get all available template names
val templates = NativeTemplateView.getAvailableTemplates()

// Get video template names
val videoTemplates = NativeTemplateView.getVideoTemplates()

// Get standard (non-video) template names
val standardTemplates = NativeTemplateView.getStandardTemplates()
```

## Jetpack Compose Usage

NativeTemplateView is also available as a Compose component via the `ad-manage-kit-compose` module.

### Basic Compose Usage

```kotlin
@Composable
fun MyScreen() {
    NativeTemplateCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        template = NativeAdTemplate.CARD_MODERN,
        onAdLoaded = { println("Ad loaded") }
    )
}
```

### With Loading Strategy

```kotlin
@Composable
fun MyScreen() {
    NativeTemplateCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        template = NativeAdTemplate.MATERIAL3,
        loadingStrategy = AdLoadingStrategy.HYBRID,
        onAdLoaded = { /* success */ },
        onAdFailedToLoad = { error -> /* handle error */ }
    )
}
```

### Convenience Functions

```kotlin
// Use specific templates directly
NativeCardModernCompose(adUnitId = adUnitId)
NativeMaterial3Compose(adUnitId = adUnitId)
NativeMinimalCompose(adUnitId = adUnitId)
NativeCompactHorizontalCompose(adUnitId = adUnitId)
NativeListItemCompose(adUnitId = adUnitId)
NativeMagazineCompose(adUnitId = adUnitId)
NativeFeaturedCompose(adUnitId = adUnitId)
NativeVideoMediumCompose(adUnitId = adUnitId)
NativeVideoLargeCompose(adUnitId = adUnitId)
NativeVideoSquareCompose(adUnitId = adUnitId)
```

### In LazyColumn

```kotlin
@Composable
fun AdFeed() {
    LazyColumn {
        items(contentItems) { item ->
            ContentItem(item)
        }

        item {
            NativeListItemCompose(
                adUnitId = "ca-app-pub-xxx/yyy",
                loadingStrategy = AdLoadingStrategy.ONLY_CACHE // Best for scrolling
            )
        }
    }
}
```

For full Compose documentation, see [COMPOSE_INTEGRATION.md](COMPOSE_INTEGRATION.md).

## Material 3 Theme Support

All templates use Material 3 theme attributes for automatic dark/light mode support:

| Attribute | Usage |
|-----------|-------|
| `?attr/colorSurface` | Background |
| `?attr/colorOnSurface` | Primary text |
| `?attr/colorOnSurfaceVariant` | Secondary text |
| `?attr/colorPrimary` | CTA button background |
| `?attr/colorOutlineVariant` | Borders, dividers |
| `?attr/colorSurfaceVariant` | Chips, badges |

No additional configuration needed - templates automatically adapt to your app's theme.

## View Handling

### Smart Visibility

Views use `GONE` (not `INVISIBLE`) when content is missing:
- Body text: Hidden if null
- Advertiser: Hidden if null
- Star rating: Hidden if null or 0
- Icon: Hidden if null
- Media: Hidden if null
- CTA: Hidden if null

### Container Auto-Hide

The advertiser container (containing advertiser name and rating) is automatically hidden when both advertiser and rating are missing, ensuring proper layout alignment.

### AdChoices

AdChoices view is automatically shown when present in the template. The SDK populates it automatically via `setNativeAd()`.

## Integration with Caching

NativeTemplateView integrates with `NativeAdManager` and `NativeAdIntegrationManager`:

```kotlin
// Enable global caching
NativeAdManager.enableCachingNativeAds = true

// Ads are automatically cached when loaded
nativeTemplateView.loadNativeAd(activity, adUnitId)

// Use cached ads later
val cached = NativeAdManager.getCachedNativeAd(adUnitId)
cached?.let { nativeTemplateView.displayAd(it) }
```

## Screen Type Mapping

Templates are automatically mapped to screen types for caching:

| Templates | Screen Type |
|-----------|-------------|
| COMPACT_HORIZONTAL, FULL_WIDTH_BANNER, LIST_ITEM, GRID_CARD, GRID_ITEM, MEDIA_CONTENT_SPLIT, PILL_BANNER, VIDEO_SMALL | SMALL |
| CARD_MODERN, MATERIAL3, MINIMAL, APP_STORE, MEDIUM_HORIZONTAL, TOP_ICON_MEDIA, ICON_LEFT, FLEXIBLE, VIDEO_MEDIUM, VIDEO_SQUARE | MEDIUM |
| STORY_STYLE, FEATURED, OVERLAY_DARK, MAGAZINE, SOCIAL_FEED, GRADIENT_CARD, SPOTLIGHT, VIDEO_LARGE, VIDEO_VERTICAL, VIDEO_FULLSCREEN | LARGE |

## XML Preview

In Android Studio's XML designer, `NativeTemplateView` shows the actual template layout with placeholder data (not shimmer), allowing you to see how the ad will appear in your layout.

## Migration from Individual Views

### From NativeBannerSmall

```kotlin
// Before
val nativeBannerSmall = NativeBannerSmall(context)
nativeBannerSmall.loadNativeBannerAd(activity, adUnitId)

// After
val nativeTemplateView = NativeTemplateView(context)
nativeTemplateView.setTemplate(NativeAdTemplate.COMPACT_HORIZONTAL)
nativeTemplateView.loadNativeAd(activity, adUnitId)
```

### From NativeBannerMedium

```kotlin
// Before
val nativeBannerMedium = NativeBannerMedium(context)
nativeBannerMedium.loadNativeBannerAd(activity, adUnitId)

// After
val nativeTemplateView = NativeTemplateView(context)
nativeTemplateView.setTemplate(NativeAdTemplate.CARD_MODERN)
nativeTemplateView.loadNativeAd(activity, adUnitId)
```

### From NativeLarge

```kotlin
// Before
val nativeLarge = NativeLarge(context)
nativeLarge.loadNativeAds(activity, adUnitId)

// After
val nativeTemplateView = NativeTemplateView(context)
nativeTemplateView.setTemplate(NativeAdTemplate.FEATURED)
nativeTemplateView.loadNativeAd(activity, adUnitId)
```

## Best Practices

1. **Choose the right template** for your layout context
2. **Use video templates** when expecting video ads for better UX
3. **Set template via XML** when possible for XML preview support
4. **Enable caching** for better performance with `NativeAdManager.enableCachingNativeAds = true`
5. **Use ONLY_CACHE strategy** in RecyclerViews for smooth scrolling
6. **Handle callbacks** to show placeholder content on failure

## Troubleshooting

### Ad Not Displaying

1. Check if ad unit ID is correct
2. Verify purchase status (ads hidden for purchased users)
3. Check for ad loading errors in callback
4. Ensure activity is not finishing/destroyed

### Wrong Template Showing

1. Verify `app:adTemplate` attribute in XML
2. Check `setTemplate()` is called before `loadNativeAd()`
3. Verify template enum/string spelling

### Theme Colors Wrong

1. Ensure your theme extends a Material 3 theme
2. Check theme attribute definitions in `themes.xml`
3. Verify night mode theme is properly configured
