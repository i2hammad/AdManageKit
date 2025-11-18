# Native Ads Caching · AdManageKit v2.5.0

## Overview
AdManageKit 2.5.0 combines `NativeAdManager`, `NativeAdIntegrationManager`, and Compose-first components to deliver screen-aware caching, retry-aware loading, and runtime analytics for every native ad format (Small/Medium/Large banners + programmatic layouts). The system keeps backward compatibility with the original APIs while layering on multi-screen cache segregation, LRU eviction, and Compose wrappers.

---

## What’s New Since v1.x
- **Screen-aware caching** with `NativeAdIntegrationManager` to prevent cache collisions across activities, fragments, or Compose destinations.
- **Config-driven behavior** via `AdManageKitConfig` (cache expiry, max ads per unit, cleanup interval, smart preloading toggle).
- **Programmatic + Compose components** (`ProgrammaticNativeAdCompose`, `NativeAdCompose`) that automatically use the same cache and debug hooks.
- **Performance telemetry** through `NativeAdManager.getPerformanceStats()` / `rememberPerformanceStats()` for dashboards and QA tooling.
- **Cache warming API**: `NativeAdManager.warmCache(...)` to prefetch multiple ad units concurrently.
- **Retry integration**: built-in `AdRetryManager` support when cached ads fail and a fresh load is required.

---

## Installation
```groovy
dependencies {
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit:v2.5.0"
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.5.0"
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.5.0"
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.5.0" // for Compose
}
```

---

## Configuration
Control caching from one place:

```kotlin
AdManageKitConfig.apply {
    nativeCacheExpiry = 2.hours
    maxCachedAdsPerUnit = 4
    enableSmartPreloading = true           // unlocks screen-aware cache hits
    enableAutoCacheCleanup = true
    enablePerformanceMetrics = BuildConfig.DEBUG
    maxCacheMemoryMB = 64
}
```

Optional: initialize analytics for cache metrics.

```kotlin
AdManageKitInitEffect() // Compose
// or
NativeAdManager.initialize(FirebaseAnalytics.getInstance(this))
```

---

## Core Components

| Component | Purpose |
|-----------|---------|
| `NativeAdManager` | LRU cache, cleanup scheduler, analytics hooks, cache statistics. |
| `NativeAdIntegrationManager` | Screen-aware loader that stitches together caching + retry logic for `NativeBannerSmall/Medium`, `NativeLarge`, and programmatic views. |
| `NativeBannerSmall/Medium`, `NativeLarge` | Traditional views that opt into caching via `useCachedAd` and integration-manager hooks. |
| `ProgrammaticNativeAdCompose`, `NativeAdCompose` | Compose wrappers that load/cycle native ads without XML templates. |
| `CacheWarmingEffect` | Compose helper that warms multiple ad units declaratively. |

---

## Usage Examples

### XML / ViewBinding (NativeBannerSmall)
```kotlin
class FeedFragment : Fragment(R.layout.fragment_feed) {
    private val nativeSmall by lazy { requireView().findViewById<NativeBannerSmall>(R.id.nativeSmall) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        nativeSmall.loadNativeBannerAd(
            activity = requireActivity(),
            adNativeBanner = getString(R.string.native_feed),
            useCachedAd = AdManageKitConfig.enableSmartPreloading,
            adCallBack = object : AdLoadCallback() {
                override fun onAdLoaded() = log("feed native ready")
                override fun onFailedToLoad(error: AdError?) = log("feed failed ${error?.message}")
            }
        )
    }
}
```

### Screen-Aware Loading
```kotlin
NativeAdIntegrationManager.loadNativeAdWithCaching(
    activity = this,
    baseAdUnitId = getString(R.string.native_article),
    screenType = NativeAdIntegrationManager.ScreenType.MEDIUM,
    useCachedAd = true,
    callback = AdDebugUtils.createDebugCallback("article_native")
) { enhancedUnitId, enhancedCallback ->
    nativeBannerMedium.loadNativeBannerAd(
        activity = this,
        adNativeBanner = enhancedUnitId,
        useCachedAd = true,
        adCallBack = enhancedCallback
    )
}
```

### Compose – Programmatic Native Layout
```kotlin
@Composable
fun ArticleNativeAdCard() {
    AdManageKitInitEffect()

    ProgrammaticNativeAdCompose(
        adUnitId = stringResource(R.string.native_article),
        screenType = NativeAdIntegrationManager.ScreenType.MEDIUM,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        onAdFailedToLoad = { reason -> Log.w("NativeAd", reason) }
    )
}
```

### Cache Warming
```kotlin
CacheWarmingEffect(
    adUnits = mapOf(
        getString(R.string.native_feed) to 3,
        getString(R.string.native_article) to 2
    ),
    onComplete = { warmed, total -> Log.d("CacheWarm", "$warmed/$total warmed") }
)
```

---

## Operational APIs

```kotlin
// Control caching
NativeAdManager.enableCachingNativeAds = true
NativeAdManager.clearCachedAd(adUnitId)
NativeAdManager.clearAllCachedAds()

// Observability
val stats = NativeAdManager.getCacheStatistics()
val performance = NativeAdManager.getPerformanceStats()

// Compose debugging
val cacheStats by rememberCacheStatistics()
val perfStats by rememberPerformanceStats()

// Smart preloading
NativeAdManager.warmCache(
    adUnits = mapOf("feed_native" to 3, "article_native" to 2)
) { warmed, total -> Log.d("Warm", "$warmed/$total") }
```

---

## How Screen-Aware Caching Works
1. **Request arrives:** `NativeAdIntegrationManager` builds a `screenKey` (e.g., `MainActivity_SMALL`) and chooses the right `ScreenType`.
2. **Cache lookup:** tries screen-specific key → base ad unit → generic key per screen type.
3. **Temporary handoff:** when a cached ad is found, it’s placed in a temp slot so the caller can immediately display it (fix for earlier onAdLoaded bug).
4. **Network fallback:** cache miss triggers a fresh load with enhanced callbacks that automatically cache results and schedule retries through `AdRetryManager`.
5. **Cleanup:** `NativeAdManager` periodically removes expired entries, enforces `maxCachedAdsPerUnit`, and logs eviction statistics.

---

## Debugging & Monitoring
- **`AdDebugUtils.createDebugCallback(...)`** surfaces cache hits/misses and onPaid events in Logcat.
- **`rememberPerformanceStats()`** + Compose preview = live dashboard of cache hit rate, memory freed, and active units.
- **Firebase Analytics** – enable `AdManageKitConfig.enablePerformanceMetrics` and call `NativeAdManager.initialize(FirebaseAnalytics.getInstance(context))` to log cache events automatically (`native_ad_cached`, `native_ad_evicted`, etc.).

---

## Best Practices
- Enable `enableSmartPreloading` for production builds to unlock screen-aware caching; keep it off in QA if you need deterministic testing.
- Call `NativeAdManager.clearAllCachedAds()` on logout or account switch to avoid cross-user targeting issues.
- Pair Compose content with `ConditionalAd` to avoid rendering ads for purchased users.
- Warm caches during app start for the first screen to reduce jank.
- Monitor `maxCacheMemoryMB` + `maxCachedAdsPerUnit` when running on low-memory devices; adjust through config rather than custom code.

With these updates, native ads load faster, remain consistent across XML and Compose surfaces, and expose the telemetry you need to tune caching strategies release-over-release.
