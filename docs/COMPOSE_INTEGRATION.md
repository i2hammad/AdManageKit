# Jetpack Compose Integration Guide

**Module:** `ad-manage-kit-compose`

AdManageKit provides native Jetpack Compose components for all ad types with full feature parity to the View-based components.

## Installation

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.6.0'
```

## Available Components

| Component | Description |
|-----------|-------------|
| `NativeTemplateCompose` | Unified native ad with 17 templates (NEW in 2.6.0) |
| `BannerAdCompose` | Banner ad with lifecycle management |
| `NativeBannerSmallCompose` | Small native banner (80dp) |
| `NativeBannerMediumCompose` | Medium native banner (120dp) |
| `NativeLargeCompose` | Large native ad (300dp) |
| `ProgrammaticNativeAdCompose` | Programmatic native ad loading |
| `rememberInterstitialAd` | Interstitial ad with preloading |
| `rememberInterstitialAdState` | Advanced interstitial state management |
| `ConditionalAd` | Show ads only for non-purchased users |
| `CacheWarmingEffect` | Pre-load ads for better performance |

---

## NativeTemplateCompose (NEW in 2.6.0)

A Compose wrapper for `NativeTemplateView` supporting all 17 template styles.

### Basic Usage

```kotlin
@Composable
fun MyScreen() {
    NativeTemplateCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        template = NativeAdTemplate.CARD_MODERN,
        onAdLoaded = { println("Ad loaded") },
        onAdFailedToLoad = { error -> println("Failed: ${error?.message}") }
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
        loadingStrategy = AdLoadingStrategy.HYBRID, // or ONLY_CACHE, ON_DEMAND
        onAdLoaded = { /* success */ }
    )
}
```

### Available Templates

```kotlin
// Standard Templates
NativeAdTemplate.CARD_MODERN      // General purpose
NativeAdTemplate.MATERIAL3        // Material Design 3
NativeAdTemplate.MINIMAL          // Clean, minimal
NativeAdTemplate.COMPACT_HORIZONTAL // Horizontal layout
NativeAdTemplate.FULL_WIDTH_BANNER // Full width
NativeAdTemplate.LIST_ITEM        // List/RecyclerView items
NativeAdTemplate.GRID_CARD        // Grid layouts
NativeAdTemplate.FEATURED         // Hero sections
NativeAdTemplate.OVERLAY_DARK     // Dark overlay on media
NativeAdTemplate.STORY_STYLE      // Social/story style
NativeAdTemplate.MAGAZINE         // News/blog style

// Video Templates
NativeAdTemplate.VIDEO_SMALL      // 120x120dp
NativeAdTemplate.VIDEO_MEDIUM     // 180dp height
NativeAdTemplate.VIDEO_LARGE      // 250dp height
NativeAdTemplate.VIDEO_SQUARE     // 300x300dp
NativeAdTemplate.VIDEO_VERTICAL   // 9:16 ratio
NativeAdTemplate.VIDEO_FULLSCREEN // Fullscreen
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

---

## Loading Strategies (NEW in 2.6.0)

Native ad Compose components support `loadingStrategy` parameter with `ON_DEMAND` and `HYBRID`:

> **Note:** `ONLY_CACHE` is only available for Interstitial and App Open ads, not for native ads.

```kotlin
// ON_DEMAND - Always fetch fresh ad
NativeBannerMediumCompose(
    adUnitId = adUnitId,
    loadingStrategy = AdLoadingStrategy.ON_DEMAND
)

// HYBRID - Check cache first, fetch if needed (recommended)
NativeBannerMediumCompose(
    adUnitId = adUnitId,
    loadingStrategy = AdLoadingStrategy.HYBRID
)
```

---

## Banner Ads

```kotlin
@Composable
fun BannerExample() {
    Column {
        Text("My App Content")

        BannerAdCompose(
            adUnitId = "ca-app-pub-xxx/yyy",
            onAdLoaded = { println("Banner loaded") },
            onAdFailedToLoad = { error -> println("Failed: ${error?.message}") }
        )
    }
}
```

---

## Native Ads (Traditional Sizes)

```kotlin
@Composable
fun NativeAdExamples() {
    LazyColumn {
        item {
            // Small (80dp height)
            NativeBannerSmallCompose(
                adUnitId = "ca-app-pub-xxx/yyy",
                loadingStrategy = AdLoadingStrategy.ONLY_CACHE
            )
        }

        item {
            // Medium (120dp height)
            NativeBannerMediumCompose(
                adUnitId = "ca-app-pub-xxx/yyy",
                loadingStrategy = AdLoadingStrategy.HYBRID
            )
        }

        item {
            // Large (300dp height)
            NativeLargeCompose(
                adUnitId = "ca-app-pub-xxx/yyy",
                loadingStrategy = AdLoadingStrategy.HYBRID
            )
        }
    }
}
```

### Generic NativeAdCompose

```kotlin
@Composable
fun GenericNativeAd() {
    NativeAdCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        size = NativeAdSize.MEDIUM, // SMALL, MEDIUM, or LARGE
        loadingStrategy = AdLoadingStrategy.HYBRID,
        onAdLoaded = { /* success */ },
        onAdFailedToLoad = { error -> /* handle error */ },
        onAdClicked = { /* track click */ },
        onAdImpression = { /* track impression */ },
        onPaidEvent = { adValue -> /* track revenue */ }
    )
}
```

---

## Interstitial Ads

### Simple Usage

```kotlin
@Composable
fun InterstitialExample() {
    val showInterstitial = rememberInterstitialAd(
        adUnitId = "ca-app-pub-xxx/yyy",
        preloadAd = true,
        onAdShown = { println("Ad shown") },
        onAdDismissed = { println("Ad dismissed") }
    )

    Button(onClick = { showInterstitial() }) {
        Text("Show Interstitial")
    }
}
```

### Advanced State Management

```kotlin
@Composable
fun AdvancedInterstitialExample() {
    val interstitialState = rememberInterstitialAdState(
        adUnitId = "ca-app-pub-xxx/yyy",
        autoLoad = true
    )

    Column {
        Text("Ad Status: ${if (interstitialState.isLoaded) "Ready" else "Loading"}")

        // Time-based display
        Button(
            onClick = { interstitialState.showAdByTime() },
            enabled = interstitialState.isLoaded
        ) {
            Text("Show (Time-based)")
        }

        // Count-based display
        Button(
            onClick = { interstitialState.showAdByCount(maxCount = 3) },
            enabled = interstitialState.isLoaded
        ) {
            Text("Show (Count-based)")
        }

        // Force show
        Button(
            onClick = { interstitialState.forceShowAd() },
            enabled = interstitialState.isLoaded
        ) {
            Text("Force Show")
        }

        // Force show with dialog
        Button(onClick = { interstitialState.forceShowAdWithDialog() }) {
            Text("Show with Dialog")
        }

        // Error display
        interstitialState.lastError?.let { error ->
            Text("Error: $error", color = Color.Red)
        }
    }
}
```

### Declarative Effect

```kotlin
@Composable
fun InterstitialEffectExample() {
    InterstitialAdEffect(
        adUnitId = "ca-app-pub-xxx/yyy",
        showMode = InterstitialShowMode.TIME, // TIME, COUNT, FORCE, FORCE_WITH_DIALOG
        maxDisplayCount = 3,
        onAdShown = { /* shown */ },
        onAdDismissed = { /* dismissed */ }
    )
}
```

---

## Conditional Ads

Show ads only for users who haven't purchased:

```kotlin
@Composable
fun ConditionalAdExample() {
    Column {
        Text("App Content")

        // Only shows if user hasn't purchased
        ConditionalAd {
            NativeTemplateCompose(
                adUnitId = "ca-app-pub-xxx/yyy",
                template = NativeAdTemplate.CARD_MODERN
            )
        }

        Text("More Content")
    }
}
```

---

## Cache Warming

Pre-load ads for better performance:

```kotlin
@Composable
fun CacheWarmingExample() {
    // Warm cache when composable enters composition
    CacheWarmingEffect(
        adUnits = mapOf(
            "ca-app-pub-xxx/native1" to 2,  // Preload 2 ads
            "ca-app-pub-xxx/native2" to 1   // Preload 1 ad
        ),
        onComplete = { warmedUnits, totalUnits ->
            println("Warmed $warmedUnits of $totalUnits ad units")
        }
    )

    // Your content
    MyAppContent()
}
```

---

## Monitoring & Statistics

```kotlin
@Composable
fun MonitoringExample() {
    // Monitor purchase status
    val isPurchased = rememberPurchaseStatus()

    // Monitor cache statistics
    val cacheStats by rememberCacheStatistics()

    // Monitor performance
    val perfStats by rememberPerformanceStats()

    Column {
        Text("Purchased: $isPurchased")
        Text("Cache Hit Rate: ${perfStats["hit_rate_percent"]}%")
        Text("Total Ads Served: ${perfStats["total_ads_served"]}")

        cacheStats.forEach { (adUnit, stats) ->
            Text("$adUnit: $stats")
        }
    }
}
```

---

## Initialization

Initialize AdManageKit in your Compose app:

```kotlin
@Composable
fun MyApp() {
    // Initialize with Firebase Analytics
    AdManageKitInitEffect()

    // Your app content
    MyAppContent()
}
```

---

## Complete Example

```kotlin
@Composable
fun AdDemoScreen() {
    // Initialize
    AdManageKitInitEffect()

    // Warm cache
    CacheWarmingEffect(
        adUnits = mapOf("ca-app-pub-xxx/yyy" to 2)
    )

    val isPurchased = rememberPurchaseStatus()

    LazyColumn {
        item {
            Text("Welcome to My App", style = MaterialTheme.typography.headlineMedium)
        }

        // Only show ads if not purchased
        if (!isPurchased) {
            item {
                // Use NativeTemplateView with Material 3 style
                NativeTemplateCompose(
                    adUnitId = "ca-app-pub-xxx/yyy",
                    template = NativeAdTemplate.MATERIAL3,
                    loadingStrategy = AdLoadingStrategy.HYBRID,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        items(10) { index ->
            Text("Content Item $index")

            // Show ad every 5 items
            if (index % 5 == 4 && !isPurchased) {
                NativeListItemCompose(
                    adUnitId = "ca-app-pub-xxx/yyy",
                    loadingStrategy = AdLoadingStrategy.ONLY_CACHE
                )
            }
        }

        item {
            // Video ad at the end
            NativeVideoMediumCompose(
                adUnitId = "ca-app-pub-xxx/yyy",
                loadingStrategy = AdLoadingStrategy.HYBRID
            )
        }
    }
}
```

---

## Migration from Views to Compose

```kotlin
// View system
val nativeTemplateView = NativeTemplateView(context)
nativeTemplateView.setTemplate(NativeAdTemplate.MATERIAL3)
nativeTemplateView.loadNativeAd(activity, adUnitId)

// Compose equivalent
@Composable
fun MyScreen() {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.MATERIAL3
    )
}
```

---

## Best Practices

1. **Use `ONLY_CACHE` in LazyColumn/LazyRow** for smooth scrolling
2. **Use `HYBRID` for static placements** for balance between coverage and UX
3. **Enable cache warming** at app start for better ad availability
4. **Use `ConditionalAd`** to respect user purchases
5. **Handle callbacks** for analytics and error tracking
6. **Choose appropriate templates** for your layout context
