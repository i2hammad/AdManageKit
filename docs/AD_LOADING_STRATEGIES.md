# Ad Loading Strategies Guide

**New in v2.6.0** - AdManageKit supports three different ad loading strategies to fit different use cases in your app.

## Strategy Availability

| Strategy | Interstitial | App Open | Native |
|----------|-------------|----------|--------|
| ON_DEMAND | ✅ | ✅ | ✅ |
| ONLY_CACHE | ✅ | ✅ | ❌ |
| HYBRID | ✅ | ✅ | ✅ |

> **Note:** `ONLY_CACHE` is only available for **Interstitial** and **App Open** ads. Native ads display inline with shimmer loading, so they always need to load content.

## Strategy Types

### 1. ON_DEMAND
**Always fetch and display fresh ads when needed**

- ✅ Shows loading dialog while fetching
- ✅ Waits for ad within timeout period (default: 4-15 seconds)
- ✅ Best ad coverage - always tries to show
- ❌ May interrupt user flow with loading
- ❌ Slower if network is poor

**Best for:**
- Important monetization points
- After significant user actions (level completion, purchase flow)
- When you want maximum ad coverage

**How it works:**
```
User triggers ad → Show loading dialog → Fetch fresh ad →
    If loaded within timeout: Show ad → Continue
    If timeout: Skip ad → Continue
```

---

### 2. ONLY_CACHE
**Only show ads that are already preloaded**

- ✅ Instant display - no waiting
- ✅ Smooth user experience
- ✅ No loading dialogs
- ❌ Lower ad coverage - skips if not cached
- ❌ Requires good background preloading

**Best for:**
- Frequent ad opportunities
- During gameplay or critical flows
- When user experience is priority over coverage

**How it works:**
```
User triggers ad → Check cache →
    If cached: Show ad immediately → Continue
    If not cached: Skip → Continue
```

---

### 3. HYBRID (Recommended)
**Check cache first, fetch if needed**

- ✅ Instant when cached (best UX)
- ✅ Still tries fetching if not cached (good coverage)
- ✅ Balanced approach
- ⚠️ May show loading dialog if cache is empty

**Best for:**
- Most general use cases
- Default recommendation
- Good balance between UX and coverage

**How it works:**
```
User triggers ad → Check cache →
    If cached: Show ad immediately → Continue
    If not cached: Show loading dialog → Fetch fresh ad →
        If loaded within timeout: Show ad → Continue
        If timeout: Skip ad → Continue
```

---

## Native Ads Behavior

Native ads work differently from interstitial/app open ads:

### Loading UI
- **No loading dialog** - Native ads use shimmer effect instead
- **Container visibility** - The ad container shows/hides based on strategy

### Strategy Behavior

**ON_DEMAND:**
```
Load native ad → Show shimmer → Fetch ad →
    ✅ Show ad when loaded → Hide shimmer
    ❌ On failure → Hide container
```

**ONLY_CACHE:**
```
Load native ad → Check cache →
    ✅ Show cached ad immediately → No shimmer
    ❌ No cache → Hide container immediately
```

**HYBRID:**
```
Load native ad → Check cache →
    ✅ Show cached ad immediately → No shimmer
    OR
    → Show shimmer → Fetch fresh ad →
        ✅ Show ad when loaded → Hide shimmer
        ❌ On failure → Hide container
```

### Usage Example

```kotlin
// Native ad view respects the configured strategy
nativeLargeView.loadNativeAds(
    activity = this,
    adUnitId = "ca-app-pub-xxx"
    // No need to pass useCachedAd - uses config strategy automatically
)

// Or override for specific case
nativeLargeView.loadNativeAds(
    activity = this,
    adUnitId = "ca-app-pub-xxx",
    useCachedAd = true  // Force ONLY_CACHE for this ad
)

// NativeTemplateView with strategy override (NEW in 2.6.0)
nativeTemplateView.loadNativeAd(
    activity = this,
    adUnitId = "ca-app-pub-xxx",
    loadingStrategy = AdLoadingStrategy.HYBRID
)
```

---

## Configuration

### Basic Setup

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Set strategy for interstitial ads
        AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.HYBRID

        // Set strategy for app open ads
        AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.HYBRID

        // Set strategy for native ads
        AdManageKitConfig.nativeLoadingStrategy = AdLoadingStrategy.HYBRID
    }
}
```

### Strategy Selection Guide

| Use Case | Recommended Strategy | Why |
|----------|---------------------|-----|
| **Interstitial Ads** | | |
| After level completion | `HYBRID` or `ON_DEMAND` | Important monetization point |
| During gameplay | `ONLY_CACHE` | Don't interrupt gameplay |
| Navigation between screens | `ONLY_CACHE` | Smooth navigation |
| After long tasks (video render, etc.) | `ON_DEMAND` | User expects wait anyway |
| Frequent button clicks | `ONLY_CACHE` | Don't annoy user |
| **App Open Ads** | | |
| App open/resume | `HYBRID` | Balance UX and coverage |
| Cold start | `ON_DEMAND` | User expects startup time |
| Background return | `ONLY_CACHE` | Quick return to app |
| **Native Ads** | | |
| Feed/List items | `ONLY_CACHE` | Instant display, smooth scrolling |
| Article content | `HYBRID` | Balance coverage and UX |
| Static placements | `ON_DEMAND` | Always fill the space |

---

## Example Configurations

### Gaming App - Maximize UX

```kotlin
class GameApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // During gameplay - only cached ads
        AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE

        // App open - balanced approach
        AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.HYBRID

        // Native ads in feed - instant display
        AdManageKitConfig.nativeLoadingStrategy = AdLoadingStrategy.ONLY_CACHE

        // Enable background prefetching for ONLY_CACHE to work well
        AdManageKitConfig.appOpenFetchFreshAd = false // Enable auto-fetch
    }
}

// In your game code
fun onLevelComplete() {
    // Shows cached ad instantly if available, skips if not
    InterstitialAdBuilder.with(this)
        .adUnit("ca-app-pub-xxx")
        .show {
            navigateToNextLevel()
        }
}
```

### Utility App - Maximize Revenue

```kotlin
class UtilityApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Always try to fetch fresh ads
        AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND
        AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND
        AdManageKitConfig.nativeLoadingStrategy = AdLoadingStrategy.ON_DEMAND

        // Shorter timeout for faster flow
        AdManageKitConfig.defaultAdTimeout = 5.seconds
    }
}

// After task completion
fun onTaskComplete() {
    // Always tries to fetch and show fresh ad
    InterstitialAdBuilder.with(this)
        .adUnit("ca-app-pub-xxx")
        .timeout(5000) // 5 second timeout
        .show {
            showResults()
        }
}
```

### Content App - Balanced

```kotlin
class ContentApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Default HYBRID strategy for all ad types
        AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
        AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
        AdManageKitConfig.nativeLoadingStrategy = AdLoadingStrategy.HYBRID
    }
}

// Navigation
fun onArticleFinished() {
    // Shows cached if available, fetches if not
    InterstitialAdBuilder.with(this)
        .adUnit("ca-app-pub-xxx")
        .show {
            loadNextArticle()
        }
}
```

---

## How It Works Internally

### Current Implementation Mapping

The strategies are currently implemented through the existing AdManageKit APIs:

**For Interstitial Ads:**
- `ON_DEMAND`: Uses `forceShowInterstitial()` with dialog
- `ONLY_CACHE`: Uses `showInterstitialIfReady()`
- `HYBRID`: Checks cache first, falls back to `forceShowInterstitial()` if empty

**For App Open Ads:**
- `ON_DEMAND`: Discards cache, fetches with welcome dialog
- `ONLY_CACHE`: Only shows `if (isAdAvailable())`
- `HYBRID`: Shows cached, fetches with dialog if not available (current default)

### Background Preloading

For `ONLY_CACHE` and `HYBRID` strategies to work effectively:

```kotlin
// Enable background ad preloading
AdManageKitConfig.appOpenFetchFreshAd = false // Enables auto-fetch

// Or manually preload ads
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxx")
    .preload()
```

---

## Migration from Old API

If you're using the old methods:

```kotlin
// Old way
AdManager.getInstance().forceShowInterstitial(activity, callback)
AdManager.getInstance().showInterstitialAdByTime(activity, callback, true)

// New way - just set strategy and use builder
AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND

InterstitialAdBuilder.with(activity)
    .adUnit("ca-app-pub-xxx")
    .show { /* next action */ }
```

---

## Best Practices

1. **Set strategy once** in Application.onCreate()
2. **Use HYBRID** as default for most cases
3. **Use ONLY_CACHE** for frequent interruptions (games, navigation)
4. **Use ON_DEMAND** for critical monetization moments
5. **Enable background preloading** for ONLY_CACHE/HYBRID
6. **Monitor ad coverage** and adjust strategy based on metrics
7. **Different strategies** for different ad types is okay

---

## Testing Strategies

```kotlin
// For development/testing
if (BuildConfig.DEBUG) {
    // Quick testing - only cached ads
    AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
    AdManageKitConfig.testMode = true
} else {
    // Production - balanced approach
    AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
}
```
