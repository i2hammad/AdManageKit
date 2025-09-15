# AdManageKit Compose

A Jetpack Compose module for AdManageKit that provides native Compose components for displaying ads in modern Android applications.

## Features

- 🎯 **Native Compose Components** - Purpose-built Composables for ads
- 🚀 **Programmatic Loading** - Load ads without predefined layouts
- 🔄 **Automatic Caching** - Integrated with AdManageKit's caching system
- 💰 **Purchase Awareness** - Automatically respects user purchase status
- 📊 **Analytics Integration** - Full Firebase Analytics support
- ⚡ **Performance Optimized** - Efficient recomposition and memory management

## Installation

Add the Compose module to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.i2hammad:ad-manage-kit-compose:2.1.0")
}
```

## Quick Start

### 1. Initialize AdManageKit in Compose

```kotlin
@Composable
fun MyApp() {
    // Initialize AdManageKit with Firebase Analytics
    AdManageKitInitEffect()

    // Your app content
    MyContent()
}
```

### 2. Banner Ads

```kotlin
@Composable
fun MyScreen() {
    Column {
        Text("My Content")

        // Simple banner ad
        BannerAdCompose(
            adUnitId = "ca-app-pub-3940256099942544/6300978111",
            onAdLoaded = { println("Banner ad loaded") },
            onAdFailedToLoad = { error -> println("Banner failed: ${error?.message}") }
        )
    }
}
```

### 3. Native Ads

#### Traditional Native Ads
```kotlin
@Composable
fun MyContent() {
    LazyColumn {
        item {
            // Small native ad
            NativeBannerSmallCompose(
                adUnitId = "ca-app-pub-3940256099942544/2247696110",
                useCachedAd = true,
                onAdLoaded = { println("Native ad loaded") }
            )
        }

        item {
            // Medium native ad
            NativeBannerMediumCompose(
                adUnitId = "ca-app-pub-3940256099942544/2247696110"
            )
        }

        item {
            // Large native ad
            NativeLargeCompose(
                adUnitId = "ca-app-pub-3940256099942544/2247696110"
            )
        }
    }
}
```

#### Programmatic Native Ads (Recommended)
```kotlin
@Composable
fun ModernNativeAds() {
    Column {
        // Small programmatic native ad with loading indicator
        ProgrammaticNativeBannerSmallCompose(
            adUnitId = "ca-app-pub-3940256099942544/2247696110",
            showLoadingIndicator = true,
            onAdLoaded = { nativeAdView, nativeAd ->
                println("Programmatic ad loaded successfully")
            }
        )

        // Medium programmatic native ad
        ProgrammaticNativeBannerMediumCompose(
            adUnitId = "ca-app-pub-3940256099942544/2247696110"
        )

        // Large programmatic native ad
        ProgrammaticNativeLargeCompose(
            adUnitId = "ca-app-pub-3940256099942544/2247696110"
        )
    }
}
```

### 4. Interstitial Ads

#### Simple Interstitial
```kotlin
@Composable
fun InterstitialExample() {
    val showInterstitial = rememberInterstitialAd(
        adUnitId = "ca-app-pub-3940256099942544/1033173712",
        preloadAd = true,
        onAdLoaded = { println("Interstitial loaded") },
        onAdDismissed = { println("Interstitial dismissed") }
    )

    Button(onClick = { showInterstitial() }) {
        Text("Show Interstitial Ad")
    }
}
```

#### Advanced Interstitial with State
```kotlin
@Composable
fun AdvancedInterstitialExample() {
    val interstitialState = rememberInterstitialAdState(
        adUnitId = "ca-app-pub-3940256099942544/1033173712",
        autoLoad = true
    )

    Column {
        Text("Ad Status: ${if (interstitialState.isLoaded) "Ready" else "Loading"}")

        Button(
            onClick = { interstitialState.showAdWithCheck() },
            enabled = interstitialState.isLoaded
        ) {
            Text("Show Ad")
        }

        if (interstitialState.lastError != null) {
            Text(
                text = "Error: ${interstitialState.lastError?.message}",
                color = Color.Red
            )
        }
    }
}
```

## Advanced Features

### Conditional Ad Display

```kotlin
@Composable
fun ConditionalAdExample() {
    // Only show ads if user hasn't purchased
    ConditionalAd {
        NativeBannerSmallCompose(
            adUnitId = "ca-app-pub-3940256099942544/2247696110"
        )
    }
}
```

### Cache Warming

```kotlin
@Composable
fun CacheWarmingExample() {
    // Warm cache for multiple ad units
    CacheWarmingEffect(
        adUnits = mapOf(
            "ca-app-pub-3940256099942544/2247696110" to 2,
            "ca-app-pub-3940256099942544/6300978111" to 1
        ),
        onComplete = { warmedUnits, totalUnits ->
            println("Warmed $warmedUnits out of $totalUnits ad units")
        }
    )
}
```

### Performance Monitoring

```kotlin
@Composable
fun PerformanceMonitoring() {
    val cacheStats by rememberCacheStatistics()
    val perfStats by rememberPerformanceStats()

    Column {
        Text("Cache Hit Rate: ${perfStats["hit_rate_percent"]}%")
        Text("Total Ads Served: ${perfStats["total_ads_served"]}")

        cacheStats.forEach { (adUnit, stats) ->
            Text("$adUnit: $stats")
        }
    }
}
```

## Component Reference

### Banner Ads

| Component | Description |
|-----------|-------------|
| `BannerAdCompose` | Standard banner ad with default 50dp height |
| `BannerAdCompose` (with dimensions) | Custom-sized banner ad |

### Native Ads

| Component | Description |
|-----------|-------------|
| `NativeAdCompose` | Generic native ad with size parameter |
| `NativeBannerSmallCompose` | Small native banner (80dp height) |
| `NativeBannerMediumCompose` | Medium native banner (120dp height) |
| `NativeLargeCompose` | Large native ad (300dp height) |

### Programmatic Native Ads

| Component | Description |
|-----------|-------------|
| `ProgrammaticNativeAdCompose` | Generic programmatic native ad |
| `ProgrammaticNativeBannerSmallCompose` | Small programmatic native banner |
| `ProgrammaticNativeBannerMediumCompose` | Medium programmatic native banner |
| `ProgrammaticNativeLargeCompose` | Large programmatic native ad |

### Interstitial Ads

| Function/Component | Description |
|-------------------|-------------|
| `rememberInterstitialAd` | Returns a function to show interstitial ads |
| `InterstitialAdEffect` | Effect for automatic interstitial management |
| `rememberInterstitialAdState` | State holder for advanced interstitial control |

### Utilities

| Function | Description |
|----------|-------------|
| `AdManageKitInitEffect` | Initialize AdManageKit with Firebase Analytics |
| `rememberPurchaseStatus` | Get current user purchase status |
| `rememberCacheStatistics` | Get cache performance statistics |
| `rememberPerformanceStats` | Get overall performance metrics |
| `CacheWarmingEffect` | Pre-load ads for better performance |
| `ConditionalAd` | Conditionally display ads based on purchase status |

## Best Practices

1. **Initialize Early**: Call `AdManageKitInitEffect()` in your main `@Composable` function
2. **Use Programmatic Ads**: Prefer programmatic native ads for better Compose integration
3. **Handle Purchase Status**: Always respect user purchase status to disable ads
4. **Cache Warming**: Warm cache for frequently used ad units to improve user experience
5. **Error Handling**: Implement proper error handling for failed ad loads
6. **Performance**: Monitor cache hit rates and performance metrics

## Migration from View System

If you're migrating from the traditional View-based AdManageKit:

```kotlin
// Old View system
val nativeBannerSmall = NativeBannerSmall(context)
nativeBannerSmall.loadNativeBannerAd(activity, adUnitId)
container.addView(nativeBannerSmall)

// New Compose system
@Composable
fun MyScreen() {
    NativeBannerSmallCompose(
        adUnitId = adUnitId,
        onAdLoaded = { /* handle success */ }
    )
}
```

## Requirements

- Minimum SDK: 23
- Jetpack Compose BOM: 2024.12.01
- Kotlin: 2.1.0
- AdManageKit: 2.1.0+

## License

This module follows the same license as the main AdManageKit library.