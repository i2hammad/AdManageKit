# Banner Ads - AdManageKit v4.4.1

## Overview

AdManageKit provides `BannerAdView` for banner ad integration with every standard AdMob size, adaptive banners, collapsible banners, a size-aware loading shimmer, auto-refresh, and smart retry logic.

**Library Version**: v4.4.1

## Features

- **All Standard Sizes**: `BannerAdSize` — adaptive, large adaptive, 320x50, 320x100, 300x250, 468x60, 728x90
- **Adaptive Banners**: Full-width anchored adaptive sizing (Google-recommended default)
- **Collapsible Banners**: Expandable/collapsible banner support, top or bottom placement
- **Loading Shimmer**: Placeholder reserves the real ad height from the first frame — no layout jump
- **Auto-Refresh**: Configurable refresh intervals (30s minimum per AdMob policy)
- **Smart Retry**: Automatic retry with exponential backoff + circuit breaker
- **Multi-Provider Waterfall**: Falls back through a configured provider chain (e.g. AdMob → Yandex)
- **Purchase Check**: Auto-hide for premium users
- **Firebase Analytics**: `ad_impression`, `ad_paid_event`, `ad_failed_to_load` tracking
- **Lifecycle Aware**: Automatic pause/resume/cleanup

## Installation

```groovy
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.1'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.1'
}
```

## Banner Sizes

`com.i2hammad.admanagekit.config.BannerAdSize` (since v4.3.0):

| Enum | XML value | Size (dp) | Availability |
|------|-----------|-----------|--------------|
| `ADAPTIVE` *(default)* | `adaptive` | Full width, ~50-90dp tall | Phones and tablets |
| `ADAPTIVE_LARGE` | `adaptive_large` | Full width, taller than `ADAPTIVE` | Phones and tablets |
| `BANNER` | `banner` | 320x50 | Phones and tablets |
| `LARGE_BANNER` | `large_banner` | 320x100 | Phones and tablets |
| `MEDIUM_RECTANGLE` | `medium_rectangle` | 300x250 | Phones and tablets |
| `FULL_BANNER` | `full_banner` | 468x60 | Tablets |
| `LEADERBOARD` | `leaderboard` | 728x90 | Tablets |

**Adaptive sizes** (`ADAPTIVE`, `ADAPTIVE_LARGE`) compute their dimensions at load time from the available width; the fixed sizes use the dimensions above. `BannerAdSize.isAdaptive` reports which kind you have.

`ADAPTIVE_LARGE` (added in v4.3.4) is the Next-Gen SDK's *large anchored adaptive* format — a taller slot with higher viewability. It's opt-in; `ADAPTIVE` remains the default and matches pre-4.2.0 banner height.

> **Upgrading from 4.2.0-4.3.3?** Those versions accidentally used the large adaptive format for `ADAPTIVE`, making default banners noticeably taller. v4.3.4+ restores the standard height automatically — request `ADAPTIVE_LARGE` explicitly if you preferred the taller banner.

## Usage

### XML Layout

```xml
<com.i2hammad.admanagekit.admob.BannerAdView
    android:id="@+id/bannerAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

With an explicit size:

```xml
<com.i2hammad.admanagekit.admob.BannerAdView
    android:id="@+id/bannerAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:bannerAdSize="medium_rectangle" />
```

Keep `android:layout_height="wrap_content"` — the view sizes itself (and its shimmer placeholder) to the requested format.

### Load Banner

```kotlin
// Default adaptive banner
bannerAdView.loadBanner(activity, "ca-app-pub-xxx/yyy")

// With an explicit size
bannerAdView.loadBanner(activity, "ca-app-pub-xxx/yyy", BannerAdSize.MEDIUM_RECTANGLE)

// With callback
bannerAdView.loadBanner(activity, "ca-app-pub-xxx/yyy", object : AdLoadCallback() {
    override fun onAdLoaded() { /* success */ }
    override fun onFailedToLoad(error: AdKitError?) { /* error */ }
    override fun onAdImpression() { /* impression */ }
    override fun onAdClicked() { /* clicked */ }
    override fun onPaidEvent(adValue: AdKitValue) { /* revenue */ }
})

// Size + callback
bannerAdView.loadBanner(activity, "ca-app-pub-xxx/yyy", BannerAdSize.LEADERBOARD, callback)
```

`AdKitError` / `AdKitValue` are AdManageKit type aliases for the Next-Gen SDK's `LoadAdError` / `AdValue` (see [[Configuration]]).

### Set Size Without Loading

```kotlin
// Applies to subsequent loads; resizes a visible shimmer immediately
bannerAdView.setBannerAdSize(BannerAdSize.LARGE_BANNER)
```

Precedence for the requested size: the `adSize` argument on the load call → `setBannerAdSize()` → the XML `bannerAdSize` attribute → `ADAPTIVE`.

### Collapsible Banner

```kotlin
// Collapses from the bottom
bannerAdView.loadCollapsibleBanner(
    activity, "ca-app-pub-xxx/yyy",
    collapsible = true,
    placement = CollapsibleBannerPlacement.BOTTOM
)

// Collapses from the top, with a callback and explicit size
bannerAdView.loadCollapsibleBanner(
    activity, "ca-app-pub-xxx/yyy",
    collapsible = true,
    placement = CollapsibleBannerPlacement.TOP,
    callback = myCallback,
    adSize = BannerAdSize.ADAPTIVE
)
```

> **Collapsible requires an adaptive size.** AdMob only serves the collapsible format for anchored adaptive requests. Both `ADAPTIVE` and `ADAPTIVE_LARGE` qualify; a fixed size (e.g. `MEDIUM_RECTANGLE`) loads as a normal banner and logs a warning instead of collapsing.

### Auto-Refresh

```kotlin
bannerAdView.enableAutoRefresh(intervalSeconds = 60)  // clamped to 30s minimum
bannerAdView.enableAutoRefresh()                      // no argument → 30 seconds
bannerAdView.disableAutoRefresh()
bannerAdView.refreshAd()                              // manual one-off refresh
```

The interval passed to `enableAutoRefresh()` is a **per-view** setting and takes precedence over the global `defaultBannerRefreshInterval`, which is the fallback when no per-view interval is set. Both are clamped to AdMob's 30-second minimum.

The requested size carries through refreshes and retries.

### Lifecycle & Visibility

```kotlin
bannerAdView.hideAd()
bannerAdView.showAd()
bannerAdView.pauseAd()      // pause when leaving the screen
bannerAdView.resumeAd()     // resume when returning
bannerAdView.destroyAd()    // release the ad
```

If the hosting `Activity` is a `LifecycleOwner`, `BannerAdView` observes it and cleans up on `ON_DESTROY` automatically.

### State Inspection

```kotlin
bannerAdView.isAdLoaded()        // an ad is loaded and displayable
bannerAdView.isLoading()         // a request is in flight
bannerAdView.getCurrentAttempt() // current retry attempt number
```

## Multi-Provider Waterfall

If a banner provider chain is registered via `AdProviderConfig`, `BannerAdView` routes loads through it automatically — no code change at the call site:

```kotlin
AdProviderConfig.setBannerChain(listOf(AdMobBannerProvider(), YandexBannerProvider()))
```

Collapsible mode, placement, and the requested `BannerAdSize` are forwarded to AdMob providers in the chain; `pauseAd()` / `resumeAd()` apply to the active waterfall banner. See [[Multi-Provider Waterfall]] and [[Yandex Integration]].

## Jetpack Compose

```kotlin
@Composable
fun MyScreen() {
    // Default adaptive banner
    BannerAdCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        modifier = Modifier.fillMaxWidth()
    )

    // Explicit size + callbacks
    BannerAdCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        adSize = BannerAdSize.MEDIUM_RECTANGLE,
        onAdLoaded = { },
        onAdFailedToLoad = { error -> },
        onAdClicked = { },
        onAdImpression = { },
        onPaidEvent = { adValue -> }
    )

    // Custom fixed dimensions
    BannerAdCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        width = 320.dp,
        height = 50.dp
    )
}
```

The composable reserves the height for the requested size while loading, so content below it doesn't shift when the ad arrives. See [[Jetpack Compose]].

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

    // Retry behavior for failed loads
    autoRetryFailedAds = false
    maxRetryAttempts = 3
    enableExponentialBackoff = true
    baseRetryDelay = 1.seconds
    maxRetryDelay = 30.seconds
}
```

### Collapsible Placement Options

```kotlin
CollapsibleBannerPlacement.TOP
CollapsibleBannerPlacement.BOTTOM
```

## API Reference

### BannerAdView Methods

| Method | Description |
|--------|-------------|
| `loadBanner(activity, adUnitId)` | Load banner at the current size |
| `loadBanner(activity, adUnitId, callback)` | Load with callback |
| `loadBanner(activity, adUnitId, adSize, callback?)` | Load with an explicit size (v4.3.0+) |
| `loadCollapsibleBanner(activity, adUnitId, collapsible, placement, callback?, adSize?)` | Load collapsible banner |
| `setBannerAdSize(adSize)` | Set the size for subsequent loads (v4.3.0+) |
| `setAdCallback(callback)` | Set the default callback |
| `enableAutoRefresh(intervalSeconds)` | Enable auto-refresh (30s minimum) |
| `disableAutoRefresh()` | Stop auto-refresh |
| `refreshAd()` | Refresh the ad manually |
| `hideAd()` / `showAd()` | Toggle visibility |
| `pauseAd()` / `resumeAd()` | Pause/resume the banner |
| `destroyAd()` | Destroy and release the ad |
| `isAdLoaded()` / `isLoading()` | Load state |
| `getCurrentAttempt()` | Current retry attempt |

### XML Attributes

| Attribute | Values | Default |
|-----------|--------|---------|
| `app:bannerAdSize` | `adaptive`, `adaptive_large`, `banner`, `large_banner`, `medium_rectangle`, `full_banner`, `leaderboard` | `adaptive` |

### AdManageKitConfig Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `defaultBannerRefreshInterval` | Refresh interval | 60 seconds |
| `enableCollapsibleBannersByDefault` | Enable collapsible | false |
| `defaultCollapsiblePlacement` | Collapsible position | BOTTOM |
| `autoRetryFailedAds` | Retry failed loads | false |
| `maxRetryAttempts` | Max retry attempts | 3 |
| `enableExponentialBackoff` | Back off between retries | true |
| `baseRetryDelay` | First retry delay | 1 second |
| `maxRetryDelay` | Retry delay ceiling | 30 seconds |

## Best Practices

1. **Prefer adaptive** - `ADAPTIVE` outperforms fixed 320x50 and adjusts to every screen width
2. **Place wisely** - Don't cover important content
3. **Respect refresh** - Minimum 30 seconds per AdMob policy
4. **Use `wrap_content`** - Let the view size itself; a fixed height fights the shimmer and the loaded ad
5. **Match size to slot** - `MEDIUM_RECTANGLE` for in-content placements, `LEADERBOARD` for tablet headers
6. **Use collapsible** - Better UX for anchored banners; remember it needs an adaptive size

## Troubleshooting

- **Banner Not Showing**: Check adUnitId, network, purchase status, and that `MobileAds.initialize()` has completed
- **Banner Taller Than Expected**: On 4.2.0-4.3.3, `ADAPTIVE` used the large adaptive format — upgrade to 4.3.4+
- **Collapsible Not Collapsing**: Collapsible requires `ADAPTIVE` or `ADAPTIVE_LARGE`; a fixed size loads as a normal banner
- **Refresh Issues**: Ensure interval is 30+ seconds
- **Sizing Issues**: Use `wrap_content` for height; don't wrap the view in a fixed-height container
- **Layout Jump on Load**: Fixed in v4.3.2 — the shimmer now reserves the real ad height from the first frame

## References

- [AdMob Banner Ads](https://developers.google.com/admob/android/banner)
- [[Jetpack Compose]]
- [[Multi-Provider Waterfall]]
- [[Configuration]]
- [GitHub Repository](https://github.com/i2hammad/AdManageKit)
