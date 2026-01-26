# Jetpack Compose Integration - AdManageKit v2.8.0

## Overview

AdManageKit provides first-class Jetpack Compose support with composable functions for all ad types, state management helpers, and programmatic native ad loading.

**Library Version**: v2.8.0

## Installation

```groovy
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.8.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.8.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.8.0'
}
```

## Banner Ads

```kotlin
@Composable
fun MyScreen() {
    BannerAdCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        modifier = Modifier.fillMaxWidth()
    )
}
```

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
