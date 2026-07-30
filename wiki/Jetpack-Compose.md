# Jetpack Compose Integration - AdManageKit v4.4.2

## Overview

AdManageKit provides first-class Jetpack Compose support with composable functions for all ad types, state management helpers, and programmatic native ad loading.

**Library Version**: v4.4.2

## Installation

```groovy
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.2'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.2'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.2'
}
```

## Banner Ads

```kotlin
@Composable
fun MyScreen() {
    // Default adaptive banner
    BannerAdCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        modifier = Modifier.fillMaxWidth()
    )

    // Explicit size (v4.3.0+)
    BannerAdCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        adSize = BannerAdSize.MEDIUM_RECTANGLE
    )

    // Custom fixed dimensions
    BannerAdCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        width = 320.dp,
        height = 50.dp
    )
}
```

`adSize` accepts any `BannerAdSize` — `ADAPTIVE` (default), `ADAPTIVE_LARGE`, `BANNER`, `LARGE_BANNER`, `MEDIUM_RECTANGLE`, `FULL_BANNER`, `LEADERBOARD`. The composable reserves the height for the requested size while loading, so content below doesn't shift. See [[Banner Ads]].

## Native Ads

### NativeTemplateCompose (v2.6.0+)

```kotlin
@Composable
fun MyScreen() {
    NativeTemplateCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        template = NativeAdTemplate.MATERIAL3,
        loadingStrategy = AdLoadingStrategy.HYBRID,
        modifier = Modifier.fillMaxWidth()
    )
}
```

### Custom Templates & Shimmer (v4.3.0+)

If none of the 38 built-in templates fit, pass your own layout instead of a `template`:

```kotlin
NativeTemplateCompose(
    adUnitId = "ca-app-pub-xxx/yyy",
    customLayoutResId = R.layout.my_native_ad,
    customShimmerResId = R.layout.my_native_ad_shimmer,  // recommended, see below
    customSizeHint = NativeAdSize.MEDIUM
)
```

`customLayoutResId` takes precedence over `template`. The layout's root must be (or inflate as) a Next-Gen SDK `NativeAdView`, reusing the standard asset ids so the library can bind them: `ad_headline`, `ad_body`, `ad_call_to_action`, `ad_app_icon`, `ad_advertiser`, `ad_media`, `ad_stars`, `ad_choices_view`. Omitted ids are simply not populated, but keep the call-to-action fully visible and tappable — the native ad validator flags clipping as a policy violation.

> **`customShimmerResId` is optional, but omitting it is rarely what you want.** The shimmer then falls back to the one belonging to `template` (default `CARD_MODERN`), so a custom layout still shows a placeholder — just one shaped like a different ad, which visibly jumps when the real ad arrives. Either supply a matching shimmer, or set `template` to whichever built-in most resembles your layout so the fallback is the right shape. Passing `0` is the same as omitting it.

`customSizeHint` classifies the ad for the cache and gives the Yandex waterfall a fallback size, since a custom layout has no built-in size bucket — `SMALL` (icon + title + CTA), `MEDIUM` (adds body), `LARGE` (adds media). It does not affect how your layout is measured.

The XML and programmatic equivalents are covered in [[Native Ads|NativeAdManager]].

### Traditional Native Composables

```kotlin
@Composable
fun MyScreen() {
    // Small format
    NativeBannerSmallCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        loadingStrategy = AdLoadingStrategy.HYBRID
    )

    // Medium format
    NativeBannerMediumCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        loadingStrategy = AdLoadingStrategy.HYBRID
    )

    // Large format
    NativeLargeCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        loadingStrategy = AdLoadingStrategy.HYBRID
    )
}
```

### Programmatic Native Ads

Build your own native ad UI:

```kotlin
@Composable
fun MyScreen() {
    ProgrammaticNativeBannerMediumCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        onAdLoaded = { /* success */ },
        onAdFailed = { /* error */ }
    )
}
```

## Interstitial Ads

### rememberInterstitialAd

```kotlin
@Composable
fun MyScreen() {
    val showInterstitial = rememberInterstitialAd(
        adUnitId = "ca-app-pub-xxx/yyy",
        preloadAd = true,
        onAdShown = { analytics.log("ad_shown") },
        onAdDismissed = { navigateNext() },
        onAdFailedToLoad = { error -> Log.e("Ad", error) }
    )

    Button(onClick = { showInterstitial() }) {
        Text("Show Ad")
    }
}
```

### InterstitialAdEffect

Declarative effect-based approach:

```kotlin
@Composable
fun MyScreen() {
    var showAd by remember { mutableStateOf(false) }

    InterstitialAdEffect(
        adUnitId = "ca-app-pub-xxx/yyy",
        showMode = InterstitialShowMode.TIME,
        maxDisplayCount = 5,
        onAdDismissed = { navigateNext() }
    )

    Button(onClick = { showAd = true }) {
        Text("Continue")
    }
}
```

### rememberInterstitialAdState

Full state control:

```kotlin
@Composable
fun MyScreen() {
    val adState = rememberInterstitialAdState("ca-app-pub-xxx/yyy")

    // Load manually
    LaunchedEffect(Unit) {
        adState.loadAd()
    }

    // Check state
    if (adState.isLoaded) {
        Button(onClick = { adState.showAd() }) {
            Text("Show Ad")
        }
    }
}
```

## Conditional Ads

Hide ads for premium users:

```kotlin
@Composable
fun MyScreen() {
    ConditionalAd {
        // Only shown if user hasn't purchased
        NativeBannerMediumCompose(adUnitId = "ca-app-pub-xxx/yyy")
    }
}
```

## Cache Warming

Preload ads on screen entry:

```kotlin
@Composable
fun MyScreen() {
    CacheWarmingEffect(
        adUnitId = "ca-app-pub-xxx/yyy",
        adType = AdType.INTERSTITIAL
    )

    // Your screen content
}
```

## API Reference

### Composable Functions

| Function | Description |
|----------|-------------|
| `BannerAdCompose` | Banner ad composable |
| `NativeTemplateCompose` | Native with templates |
| `NativeBannerSmallCompose` | Small native ad |
| `NativeBannerMediumCompose` | Medium native ad |
| `NativeLargeCompose` | Large native ad |
| `ProgrammaticNativeBannerMediumCompose` | Programmatic native |
| `ConditionalAd` | Conditional wrapper |

### State Helpers

| Function | Description |
|----------|-------------|
| `rememberInterstitialAd` | Returns show lambda |
| `rememberInterstitialAdState` | Returns full state |
| `InterstitialAdEffect` | Declarative effect |
| `CacheWarmingEffect` | Preload effect |
| `AdManageKitInitEffect` | Initialization effect |

### InterstitialShowMode

| Mode | Description |
|------|-------------|
| `TIME` | Time-based trigger |
| `COUNT` | Count-based trigger |
| `FORCE` | Always show |

## Best Practices

1. **Preload early** - Use `preloadAd = true` or `CacheWarmingEffect`
2. **Use ConditionalAd** - Respect premium users
3. **Handle callbacks** - Always handle `onAdDismissed`
4. **Choose right helper** - `rememberInterstitialAd` for simple cases
5. **Match template** - Use appropriate native template

## Complete Example

```kotlin
@Composable
fun ContentScreen() {
    val showInterstitial = rememberInterstitialAd(
        adUnitId = stringResource(R.string.interstitial_ad),
        preloadAd = true,
        onAdDismissed = { /* navigate */ }
    )

    Column {
        // Banner at top
        ConditionalAd {
            BannerAdCompose(adUnitId = stringResource(R.string.banner_ad))
        }

        // Content
        LazyColumn {
            items(articles) { article ->
                ArticleItem(article)
            }

            // Native ad in feed
            item {
                ConditionalAd {
                    NativeTemplateCompose(
                        adUnitId = stringResource(R.string.native_ad),
                        template = NativeAdTemplate.LIST_ITEM
                    )
                }
            }
        }

        // Action button
        Button(onClick = { showInterstitial() }) {
            Text("Next Article")
        }
    }
}
```

## References

- [GitHub Repository](https://github.com/i2hammad/AdManageKit)
- [[Interstitial Ads]]
- [[Native Ads|NativeAdManager]]
- [[Banner Ads]]
