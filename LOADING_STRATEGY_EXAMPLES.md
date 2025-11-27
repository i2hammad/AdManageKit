# Loading Strategy Examples

This guide provides comprehensive examples for using AdManageKit's loading strategies in different scenarios.

## Table of Contents
1. [Quick Start](#quick-start)
2. [Configuration Examples](#configuration-examples)
3. [Usage Examples](#usage-examples)
4. [Real-World Scenarios](#real-world-scenarios)
5. [Testing Strategies](#testing-strategies)

---

## Quick Start

### Step 1: Configure Global Strategy in Application

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AdManageKitConfig.apply {
            // Set default strategies for all ad types
            interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
            appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
            nativeLoadingStrategy = AdLoadingStrategy.HYBRID
        }
    }
}
```

### Step 2: Load Ads

```kotlin
// Option 1: Use global strategy (no parameter needed)
nativeLargeView.loadNativeAds(this, "ca-app-pub-xxx")

// Option 2: Override with custom strategy
nativeLargeView.loadNativeAds(
    this,
    "ca-app-pub-xxx",
    loadingStrategy = AdLoadingStrategy.ONLY_CACHE
)
```

---

## Configuration Examples

### Example 1: Gaming App (Smooth UX)

```kotlin
class GameApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AdManageKitConfig.apply {
            // Prioritize smooth gameplay - only show cached ads
            interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
            nativeLoadingStrategy = AdLoadingStrategy.ONLY_CACHE

            // App opens can have slight delay
            appOpenLoadingStrategy = AdLoadingStrategy.HYBRID

            // Enable background preloading for better cache hit rate
            enableSmartPreloading = true
            maxCachedAdsPerUnit = 3
        }
    }
}
```

### Example 2: Utility App (Maximum Revenue)

```kotlin
class UtilityApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AdManageKitConfig.apply {
            // Always try to show ads - maximize revenue
            interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND
            appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND
            nativeLoadingStrategy = AdLoadingStrategy.ON_DEMAND

            // Use shorter timeouts for faster flow
            defaultAdTimeout = 5.seconds
            appOpenAdTimeout = 4.seconds
        }
    }
}
```

### Example 3: Content/News App (Balanced)

```kotlin
class ContentApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AdManageKitConfig.apply {
            // Balanced approach - instant when possible, load when needed
            interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
            appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
            nativeLoadingStrategy = AdLoadingStrategy.HYBRID

            // Enable smart features
            enableSmartPreloading = true
            enableAdaptiveIntervals = true
        }
    }
}
```

### Example 4: Mixed Strategy (Advanced)

```kotlin
class AdvancedApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AdManageKitConfig.apply {
            // Different strategies for different ad types
            appOpenLoadingStrategy = AdLoadingStrategy.ONLY_CACHE  // Instant app opens
            interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND  // Aggressive interstitials
            nativeLoadingStrategy = AdLoadingStrategy.HYBRID  // Balanced natives
        }
    }
}
```

---

## Usage Examples

### Native Ads

#### Example 1: Using Global Strategy
```kotlin
// Load native ad using the global strategy from AdManageKitConfig
nativeLargeView.loadNativeAds(this, "ca-app-pub-xxx")
```

#### Example 2: Force Cached Only (Feed/List)
```kotlin
// Perfect for RecyclerView - only show if already cached
nativeBannerView.loadNativeBannerAd(
    this,
    "ca-app-pub-xxx",
    loadingStrategy = AdLoadingStrategy.ONLY_CACHE
)
```

#### Example 3: Always Fresh (Important Placement)
```kotlin
// Always fetch fresh ad for important monetization points
nativeLargeView.loadNativeAds(
    this,
    "ca-app-pub-xxx",
    object : AdLoadCallback() {
        override fun onAdLoaded() {
            // Ad loaded and displayed
        }
        override fun onFailedToLoad(error: AdError?) {
            // Handle failure
        }
    },
    loadingStrategy = AdLoadingStrategy.ON_DEMAND
)
```

### Interstitial Ads

#### Example 1: Using Global Strategy
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxx")
    .show {
        // Next action after ad
        navigateToNextScreen()
    }
```

#### Example 2: Level Complete (Balanced)
```kotlin
fun onLevelComplete() {
    InterstitialAdBuilder.with(this)
        .adUnit("ca-app-pub-xxx")
        .timeout(5000)
        .show {
            showLevelCompletedScreen()
        }
}
```

#### Example 3: During Gameplay (Cached Only)
```kotlin
// Configure globally for smooth gameplay
AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE

// Show interstitial during natural breaks
fun onGamePaused() {
    InterstitialAdBuilder.with(this)
        .adUnit("ca-app-pub-xxx")
        .show {
            // Continue game
        }
}
```

### App Open Ads

#### Example 1: Splash Screen (Fresh)
```kotlin
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure for fresh ad on app start
        AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND

        appOpenManager?.showAdIfAvailable(
            this,
            onAdDismissed = { navigateToMain() }
        )
    }
}
```

#### Example 2: Background Resume (Instant)
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Show cached ad instantly when returning from background
        AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.ONLY_CACHE

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxx")
    }
}
```

---

## Real-World Scenarios

### Scenario 1: RecyclerView with Native Ads

```kotlin
class FeedAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NativeAdViewHolder) {
            // Use ONLY_CACHE for smooth scrolling
            holder.nativeAdView.loadNativeBannerAd(
                context,
                "ca-app-pub-xxx",
                loadingStrategy = AdLoadingStrategy.ONLY_CACHE
            )
        }
    }
}
```

### Scenario 2: Article Detail Page

```kotlin
class ArticleDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use HYBRID - show cached if available, load if not
        nativeLarge.loadNativeAds(
            this,
            "ca-app-pub-xxx",
            object : AdLoadCallback() {
                override fun onAdLoaded() {
                    // Ad displayed in article
                }
            },
            loadingStrategy = AdLoadingStrategy.HYBRID
        )
    }
}
```

### Scenario 3: Game Level Complete

```kotlin
class GameActivity : AppCompatActivity() {
    fun onLevelComplete() {
        // Check global strategy
        when (AdManageKitConfig.interstitialLoadingStrategy) {
            AdLoadingStrategy.ONLY_CACHE -> {
                // Show cached interstitial instantly if available
                InterstitialAdBuilder.with(this)
                    .adUnit("ca-app-pub-xxx")
                    .show { showRewardScreen() }
            }
            else -> {
                // Show with loading dialog
                InterstitialAdBuilder.with(this)
                    .adUnit("ca-app-pub-xxx")
                    .timeout(5000)
                    .show { showRewardScreen() }
            }
        }
    }
}
```

### Scenario 4: Background Preloading

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable background preloading for ONLY_CACHE strategy
        AdManageKitConfig.apply {
            nativeLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
            enableSmartPreloading = true
            maxCachedAdsPerUnit = 3
        }

        // Manually preload ads in background
        lifecycleScope.launch {
            delay(2000) // Wait for app to settle
            preloadNativeAds()
        }
    }

    private fun preloadNativeAds() {
        val adUnits = listOf(
            "ca-app-pub-xxx/feed",
            "ca-app-pub-xxx/detail",
            "ca-app-pub-xxx/settings"
        )

        adUnits.forEach { adUnitId ->
            NativeAdManager.preloadAd(this, adUnitId)
        }
    }
}
```

---

## Testing Strategies

### Test Different Strategies in Debug Mode

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AdManageKitConfig.apply {
            if (BuildConfig.DEBUG) {
                // Test different strategies easily
                when (TEST_STRATEGY) {
                    "on_demand" -> {
                        interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND
                        nativeLoadingStrategy = AdLoadingStrategy.ON_DEMAND
                    }
                    "only_cache" -> {
                        interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
                        nativeLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
                    }
                    else -> {
                        interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
                        nativeLoadingStrategy = AdLoadingStrategy.HYBRID
                    }
                }
            } else {
                // Production: Use HYBRID
                interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
                nativeLoadingStrategy = AdLoadingStrategy.HYBRID
            }
        }
    }

    companion object {
        const val TEST_STRATEGY = "hybrid" // Change this to test different strategies
    }
}
```

### Monitor Strategy Performance

```kotlin
class StrategyMonitorActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()

        // Log cache statistics to evaluate strategy effectiveness
        val stats = NativeAdManager.getCacheStatistics()
        stats.forEach { (adUnit, metrics) ->
            Log.d("StrategyMonitor", """
                Ad Unit: $adUnit
                Cache Hits: ${metrics["hits"]}
                Cache Misses: ${metrics["misses"]}
                Hit Rate: ${calculateHitRate(metrics)}%
            """.trimIndent())
        }
    }

    private fun calculateHitRate(metrics: Map<String, Int>): Int {
        val hits = metrics["hits"] ?: 0
        val total = hits + (metrics["misses"] ?: 0)
        return if (total > 0) (hits * 100 / total) else 0
    }
}
```

---

## Strategy Comparison Table

| Scenario | Recommended Strategy | Reason |
|----------|---------------------|--------|
| **Gaming - During Gameplay** | `ONLY_CACHE` | No interruptions, smooth UX |
| **Gaming - Level Complete** | `HYBRID` | Balance between UX and revenue |
| **Utility - Task Complete** | `ON_DEMAND` | User expects wait, maximize revenue |
| **News Feed - Inline Ads** | `ONLY_CACHE` | Smooth scrolling |
| **Article Detail** | `HYBRID` | Balance UX and coverage |
| **App Launch** | `HYBRID` or `ON_DEMAND` | Acceptable wait time |
| **Background Resume** | `ONLY_CACHE` | Instant return |
| **Settings Screen** | `ONLY_CACHE` | Static content |
| **Video Completion** | `ON_DEMAND` | User just watched video |

---

## Best Practices

1. **Start with HYBRID** - It's the recommended default for most apps
2. **Use ONLY_CACHE** for smooth UX - Great for gaming and scrolling
3. **Use ON_DEMAND** for important moments - Maximize revenue at key points
4. **Enable Smart Preloading** - Essential for ONLY_CACHE strategy
5. **Monitor Cache Hit Rate** - Adjust strategy based on metrics
6. **Test Different Strategies** - A/B test to find optimal strategy
7. **Mix Strategies** - Different strategies for different ad types is okay

---

## Migration from Old API

### Before (Deprecated)
```kotlin
nativeLarge.loadNativeAds(this, "ca-app-pub-xxx", useCachedAd = true)
```

### After (Recommended)
```kotlin
nativeLarge.loadNativeAds(
    this,
    "ca-app-pub-xxx",
    loadingStrategy = AdLoadingStrategy.HYBRID
)
```

The old `useCachedAd` parameter still works but is deprecated. Migrate to `loadingStrategy` for more control and clarity.
