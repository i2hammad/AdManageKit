# Banner Ads - AdManageKit v2.8.0

## Overview

AdManageKit provides `BannerAdView` for easy banner ad integration with features like auto-refresh, collapsible banners, and smart retry logic.

**Library Version**: v2.8.0

## Features

- **Auto-Refresh**: Configurable refresh intervals
- **Collapsible Banners**: Expandable/collapsible banner support
- **Smart Retry**: Automatic retry with exponential backoff
- **Purchase Check**: Auto-hide for premium users
- **Firebase Analytics**: Event tracking integration

## Installation

```groovy
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.8.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.8.0'
}
```

## Usage

### XML Layout

```xml
<com.i2hammad.admanagekit.admob.BannerAdView
    android:id="@+id/bannerAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

### Load Banner

```kotlin
// Standard banner
bannerAdView.loadBanner(activity, "ca-app-pub-xxx/yyy")

// With callback
bannerAdView.loadBanner(activity, "ca-app-pub-xxx/yyy", object : AdLoadCallback() {
    override fun onAdLoaded() { /* success */ }
    override fun onFailedToLoad(error: AdError?) { /* error */ }
    override fun onAdImpression() { /* impression */ }
    override fun onAdClicked() { /* clicked */ }
})
```

### Collapsible Banner

```kotlin
// Load collapsible banner at bottom
bannerAdView.loadCollapsibleBanner(activity, "ca-app-pub-xxx/yyy", isBottom = true)

// Load collapsible banner at top
bannerAdView.loadCollapsibleBanner(activity, "ca-app-pub-xxx/yyy", isBottom = false)
```

## Configuration

### Global Settings

```kotlin
AdManageKitConfig.apply {
    // Auto-refresh interval (minimum 30 seconds per AdMob policy)
    defaultBannerRefreshInterval = 60.seconds

    // Enable collapsible by default
    enableCollapsibleBannersByDefault = false

    // Default collapsible placement
    defaultCollapsiblePlacement = CollapsibleBannerPlacement.BOTTOM
}
```

### Collapsible Placement Options

```kotlin
// Available placements
CollapsibleBannerPlacement.TOP
CollapsibleBannerPlacement.BOTTOM
```

## Jetpack Compose

```kotlin
@Composable
fun MyScreen() {
    BannerAdCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        modifier = Modifier.fillMaxWidth()
    )
}
```

## API Reference

### BannerAdView Methods

| Method | Description |
|--------|-------------|
| `loadBanner(activity, adUnitId)` | Load standard banner |
| `loadBanner(activity, adUnitId, callback)` | Load with callback |
| `loadCollapsibleBanner(activity, adUnitId, isBottom)` | Load collapsible |

### AdManageKitConfig Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `defaultBannerRefreshInterval` | Refresh interval | 60 seconds |
| `enableCollapsibleBannersByDefault` | Enable collapsible | false |
| `defaultCollapsiblePlacement` | Collapsible position | BOTTOM |

## Best Practices

1. **Place wisely** - Don't cover important content
2. **Respect refresh** - Minimum 30 seconds per AdMob policy
3. **Test placements** - Try different positions for best performance
4. **Use collapsible** - Better UX for tall banners

## Troubleshooting

- **Banner Not Showing**: Check adUnitId, network, purchase status
- **Refresh Issues**: Ensure interval is 30+ seconds
- **Sizing Issues**: Use `wrap_content` for height

## References

- [AdMob Banner Ads](https://developers.google.com/admob/android/banner)
- [GitHub Repository](https://github.com/i2hammad/AdManageKit)
