# Native Ad Preloading (Force Caching) Guide

## Overview

This guide explains how to **forcefully cache native ads** before they're needed. This is essential for:
- **ONLY_CACHE** strategy: Ads must be preloaded to display
- **HYBRID** strategy: Faster initial display (instant from cache)
- **App initialization**: Preload ads during splash screens
- **Background loading**: Prepare ads while user is doing other tasks

---

## Methods for Force Caching Native Ads

### 1. **preloadNativeAd()** - Single Ad Preload

Load and cache a single native ad without displaying it.

```kotlin
NativeAdManager.preloadNativeAd(
    activity = this,
    adUnitId = "ca-app-pub-XXXXX/YYYYY",
    size = ProgrammaticNativeAdLoader.NativeAdSize.LARGE,
    onSuccess = {
        Log.d("Preload", "Native ad cached successfully")
        // Ad is now in cache, ready to display instantly
    },
    onFailure = { error ->
        Log.e("Preload", "Failed to cache: $error")
    }
)
```

**Available Sizes:**
- `NativeAdSize.SMALL` - NativeBannerSmall equivalent
- `NativeAdSize.MEDIUM` - NativeBannerMedium equivalent
- `NativeAdSize.LARGE` - NativeLarge equivalent

**When to use:**
- Preload one ad for immediate display
- During app initialization or splash screen
- Between navigation transitions

---

### 2. **preloadMultipleNativeAds()** - Batch Preload

Load multiple ads for the same ad unit to build up cache.

```kotlin
NativeAdManager.preloadMultipleNativeAds(
    activity = this,
    adUnitId = "ca-app-pub-XXXXX/YYYYY",
    size = ProgrammaticNativeAdLoader.NativeAdSize.LARGE,
    count = 3, // Preload 3 ads
    onComplete = { successCount, failureCount ->
        Log.d("Preload", "Cached $successCount/$count ads")
    }
)
```

**Features:**
- Automatic request staggering (200ms between requests)
- Prevents AdMob rate limiting
- Tracks success/failure counts
- Ideal for list views with multiple ads

**When to use:**
- Building cache for RecyclerView/ListView with multiple native ads
- Warming up cache for high-traffic screens
- Preparing for ONLY_CACHE strategy testing

---

### 3. **Programmatic Loading** - Load & Display or Just Cache

Use programmatic loading methods to load ads with full control.

#### Load without displaying (just cache):

```kotlin
NativeAdManager.loadNativeAdProgrammatically(
    activity = this,
    adUnitId = "ca-app-pub-XXXXX/YYYYY",
    size = ProgrammaticNativeAdLoader.NativeAdSize.MEDIUM,
    useCachedAd = false, // Force fresh load
    callback = object : ProgrammaticNativeAdLoader.ProgrammaticAdCallback {
        override fun onAdLoaded(nativeAdView: NativeAdView, nativeAd: NativeAd) {
            // Ad loaded and cached, but not displayed
            // Don't add nativeAdView to layout if you just want to cache
            Log.d("Cache", "Ad loaded and cached")
        }

        override fun onAdFailedToLoad(error: AdError) {
            Log.e("Cache", "Failed: ${error.message}")
        }

        // Other callbacks...
        override fun onAdClicked() {}
        override fun onAdImpression() {}
        override fun onAdOpened() {}
        override fun onAdClosed() {}
        override fun onPaidEvent(adValue: AdValue) {}
    }
)
```

#### Convenience methods:

```kotlin
// Small native banner
NativeAdManager.loadSmallNativeAd(this, adUnitId, useCachedAd = false, callback)

// Medium native banner
NativeAdManager.loadMediumNativeAd(this, adUnitId, useCachedAd = false, callback)

// Large native ad
NativeAdManager.loadLargeNativeAd(this, adUnitId, useCachedAd = false, callback)
```

---

## Real-World Usage Examples

### Example 1: Splash Screen Preload

```kotlin
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preload ads during splash screen
        preloadAdsForApp()

        // Navigate to main after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }

    private fun preloadAdsForApp() {
        // Preload native ads for main screen
        NativeAdManager.preloadMultipleNativeAds(
            activity = this,
            adUnitId = getString(R.string.native_ad_unit_id),
            size = ProgrammaticNativeAdLoader.NativeAdSize.LARGE,
            count = 2, // Cache 2 ads
            onComplete = { success, failure ->
                Log.d("Splash", "Preloaded $success native ads")
            }
        )
    }
}
```

---

### Example 2: Application.onCreate() Preload

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize AdManageKit
        AdManageKitConfig.initialize(this)

        // Enable caching
        NativeAdManager.enableCachingNativeAds = true

        // Preload ads in background
        lifecycleScope.launch {
            delay(2000) // Wait 2 seconds after app start
            preloadAdsInBackground()
        }
    }

    private fun preloadAdsInBackground() {
        // Get current activity
        val activity = (this as? Application)?.let { /* get current activity */ }
        activity?.let {
            NativeAdManager.preloadNativeAd(
                activity = it,
                adUnitId = "ca-app-pub-XXXXX/YYYYY",
                size = ProgrammaticNativeAdLoader.NativeAdSize.LARGE
            )
        }
    }
}
```

---

### Example 3: ONLY_CACHE Strategy Setup

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure ONLY_CACHE strategy
        AdManageKitConfig.apply {
            interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
            nativeLoadingStrategy = AdLoadingStrategy.HYBRID // Auto-converted from ONLY_CACHE
            enableSmartPreloading = true
        }

        // MUST preload before displaying with ONLY_CACHE
        preloadAdsToCache()
    }

    private fun preloadAdsToCache() {
        // Preload interstitial
        InterstitialAdBuilder.with(this)
            .adUnit(interstitialAdUnitId)
            .preload()

        // Preload native ads (all sizes)
        listOf(
            ProgrammaticNativeAdLoader.NativeAdSize.SMALL,
            ProgrammaticNativeAdLoader.NativeAdSize.MEDIUM,
            ProgrammaticNativeAdLoader.NativeAdSize.LARGE
        ).forEach { size ->
            NativeAdManager.preloadNativeAd(
                activity = this,
                adUnitId = nativeAdUnitId,
                size = size,
                onSuccess = {
                    Log.d("Cache", "Preloaded ${size.name}")
                }
            )
        }
    }
}
```

---

### Example 4: RecyclerView with Multiple Native Ads

```kotlin
class NewsActivity : AppCompatActivity() {
    private val nativeAdUnitId = "ca-app-pub-XXXXX/YYYYY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preload 5 native ads for RecyclerView
        NativeAdManager.preloadMultipleNativeAds(
            activity = this,
            adUnitId = nativeAdUnitId,
            size = ProgrammaticNativeAdLoader.NativeAdSize.MEDIUM,
            count = 5,
            onComplete = { success, failure ->
                Toast.makeText(this, "Preloaded $success ads", Toast.LENGTH_SHORT).show()
                setupRecyclerView()
            }
        )
    }

    private fun setupRecyclerView() {
        // RecyclerView will now use cached ads for instant display
        val adapter = NewsAdapter(nativeAdUnitId)
        recyclerView.adapter = adapter
    }
}
```

---

## Cache Management

### Check Cache Status

```kotlin
// Check if ads are cached for an ad unit
val hasCachedAds = NativeAdManager.hasCachedAds(adUnitId)
Log.d("Cache", "Has cached ads: $hasCachedAds")

// Get cache size for specific ad unit
val cacheSize = NativeAdManager.getCacheSize(adUnitId)
Log.d("Cache", "Cache size: $cacheSize")

// Get total cached ads across all units
val totalCached = NativeAdManager.getTotalCacheSize()
Log.d("Cache", "Total cached: $totalCached")

// Get detailed cache statistics
val stats = NativeAdManager.getCacheStatistics()
stats.forEach { (adUnitId, stats) ->
    Log.d("Cache", "$adUnitId: $stats")
}
```

### Clear Cache

```kotlin
// Clear cache for specific ad unit
NativeAdManager.clearCachedAd(adUnitId)

// Clear all cached ads
NativeAdManager.clearAllCachedAds()
```

### Performance Monitoring

```kotlin
// Get performance statistics
val perfStats = NativeAdManager.getPerformanceStats()
val hitRate = perfStats["hit_rate_percent"] as Int
Log.d("Performance", "Cache hit rate: $hitRate%")

// Reset performance counters
NativeAdManager.resetPerformanceStats()
```

---

## Configuration

### Enable/Disable Caching

```kotlin
// Enable native ad caching (default: true)
NativeAdManager.enableCachingNativeAds = true

// Disable caching (all preload methods will skip)
NativeAdManager.enableCachingNativeAds = false
```

### Cache Settings

```kotlin
AdManageKitConfig.apply {
    // Cache expiry time (default: 1 hour)
    nativeCacheExpiry = 60.minutes

    // Max ads per ad unit (default: 3)
    maxCachedAdsPerUnit = 5

    // Enable automatic cache cleanup (default: true)
    enableAutoCacheCleanup = true

    // Cleanup interval (default: 30 minutes)
    cacheCleanupInterval = 30.minutes

    // Enable performance metrics (default: true)
    enablePerformanceMetrics = true
}
```

---

## Best Practices

### ✅ DO:

1. **Preload during idle time** - Splash screens, loading screens, background tasks
2. **Use appropriate sizes** - Match preload size with display size
3. **Check cache status** - Verify ads are cached before ONLY_CACHE strategy
4. **Stagger requests** - Use `preloadMultipleNativeAds()` which auto-staggers
5. **Monitor performance** - Track hit rates and adjust preloading strategy

### ❌ DON'T:

1. **Don't preload too many ads** - Respect AdMob rate limits (max 5 concurrent)
2. **Don't use ONLY_CACHE without preloading** - Ads won't display
3. **Don't forget to clear cache** - Old ads consume memory
4. **Don't preload on metered connections** - Consider user's data plan
5. **Don't block UI thread** - Preloading is async, but don't wait for it

---

## Loading Strategy Recommendations

| Strategy | Preload Required? | When to Use |
|----------|------------------|-------------|
| **ON_DEMAND** | No | Maximum ad coverage, fresh ads always |
| **ONLY_CACHE** | **YES** | Best UX, instant display (must preload) |
| **HYBRID** | Recommended | Balanced approach, preload for faster display |

**Note:** ONLY_CACHE for native ads is automatically converted to HYBRID because native ads use shimmer effects, not dialogs. Native ads work best with HYBRID or ON_DEMAND.

---

## Testing

See `LoadingStrategyTestActivity.kt` for comprehensive testing of:
- All three loading strategies
- Preload functionality
- Cache management
- Real-time cache status monitoring

**Test Scenario: ONLY_CACHE**
1. Click "Preload Cache" button
2. Wait 3-5 seconds for ads to cache
3. Select ONLY_CACHE strategy
4. Click "Test Interstitial" → Instant display!
5. Click "Test Native Large" → Instant from cache!

---

## API Reference

### NativeAdManager Methods

| Method | Description | Since |
|--------|-------------|-------|
| `preloadNativeAd()` | Preload single native ad | v2.2.0 |
| `preloadMultipleNativeAds()` | Preload multiple native ads | v2.2.0 |
| `loadNativeAdProgrammatically()` | Programmatic loading with full control | v2.1.0 |
| `loadSmallNativeAd()` | Convenience for small banner | v2.1.0 |
| `loadMediumNativeAd()` | Convenience for medium banner | v2.1.0 |
| `loadLargeNativeAd()` | Convenience for large native | v2.1.0 |
| `getCacheSize()` | Get cache size for ad unit | v1.0.0 |
| `hasCachedAds()` | Check if ads are cached | v1.0.0 |
| `clearCachedAd()` | Clear specific ad unit cache | v1.0.0 |
| `clearAllCachedAds()` | Clear all cached ads | v1.0.0 |

---

## Summary

**Quick Answer: How to force cache native ads?**

```kotlin
// Simplest way - preload a single ad
NativeAdManager.preloadNativeAd(
    activity = this,
    adUnitId = "ca-app-pub-XXXXX/YYYYY",
    size = ProgrammaticNativeAdLoader.NativeAdSize.LARGE
)

// Or preload multiple ads
NativeAdManager.preloadMultipleNativeAds(
    activity = this,
    adUnitId = "ca-app-pub-XXXXX/YYYYY",
    size = ProgrammaticNativeAdLoader.NativeAdSize.LARGE,
    count = 3
)
```

That's it! The ads are now in cache and will display instantly when needed.

---

## Troubleshooting

### Issue: InflateException with `<merge>` layouts

**Error:**
```
android.view.InflateException: <merge /> can be used only with a valid ViewGroup root and attachToRoot=true
```

**Cause:** The `layout_native_large.xml` uses `<merge>` as the root tag, which requires a parent ViewGroup when inflating.

**Solution:** This has been fixed in `ProgrammaticNativeAdLoader.createNativeAdView()` (lines 171-182):
- For LARGE size: Creates a temporary FrameLayout parent, inflates with merge support, then extracts the NativeAdView
- For SMALL/MEDIUM: Direct inflation (no merge tag)

This fix is already applied, so you shouldn't encounter this error.

---

**Need Help?** Check `LoadingStrategyTestActivity.kt` for working examples!
