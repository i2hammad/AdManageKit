# Release Notes - v3.0.0

## Highlights

- **Ad Pool System**: Load multiple interstitial ad units into a pool for maximum show rate
- **Smart Splash Ads**: New `showOrWaitForAd()` method handles all splash screen scenarios automatically
- **App Open Ad Prefetching**: Prefetch ads before external intents for instant display on return
- **Cross-Ad-Unit Fallback**: Native ads can fall back to any cached ad when specific unit unavailable
- **New Template**: `MEDIUM_HORIZONTAL` - 55/45 media-content horizontal split layout
- **Modern Window APIs**: Replaced deprecated `systemUiVisibility` with `WindowInsetsController`
- **Enhanced Firebase Analytics**: Session-level tracking for fill rate, show rate, and ad impressions

---

## New Features

### AdManager - Ad Pool System

Load multiple interstitial ad units into a pool. When showing, returns ANY available ad - maximizing show rate:

```kotlin
// Load multiple ad units for redundancy
AdManager.getInstance().loadMultipleAdUnits(context, "high_ecpm", "medium_ecpm", "fallback")

// Or load individually
AdManager.getInstance().loadInterstitialAd(context, "unit_a")
AdManager.getInstance().loadInterstitialAd(context, "unit_b")

// Show ANY available ad from the pool
AdManager.getInstance().showInterstitialIfReady(activity, callback)

// Pool utilities
AdManager.getInstance().getPoolSize()       // Number of ready ads
AdManager.getInstance().getReadyAdUnits()   // Set of ad unit IDs with ready ads
AdManager.getInstance().isReady("unit_a")   // Check specific unit
AdManager.getInstance().clearAdPool()       // Clear all ads (e.g., on purchase)
```

**Key behaviors:**
- Each ad unit loads independently (no blocking)
- Duplicate load requests for same unit are automatically skipped
- Auto-reloads the specific unit that was shown
- Thread-safe with `ConcurrentHashMap`

### AdManager - Smart Splash Ads

New `showOrWaitForAd()` method handles all splash screen scenarios in a single call:

```kotlin
// In your SplashActivity
private fun showSplashAd() {
    AdManager.getInstance().showOrWaitForAd(
        activity = this,
        callback = object : AdManagerCallback() {
            override fun onNextAction() {
                navigateToMainActivity()
            }
        },
        timeoutMillis = 10_000,
        showDialogIfLoading = true
    )
}
```

**Automatic behavior:**
| Scenario | What happens |
|----------|--------------|
| Ad is READY | Shows immediately |
| Ad is LOADING | Waits with optional dialog, shows when ready or times out |
| Neither | Loads fresh ad with loading dialog |

**Recommended splash flow:**
```kotlin
// Option 1: Preload in Application, smart show on splash
class MyApp : Application() {
    override fun onCreate() {
        AdManager.getInstance().loadInterstitialAd(this, adUnitId)
    }
}

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Ad may already be loaded from Application
        AdManager.getInstance().showOrWaitForAd(this, callback, 10_000)
    }
}

// Option 2: Load and show on splash (all-in-one)
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Works even with no preloading
        AdManager.getInstance().showOrWaitForAd(this, callback, 10_000)
    }
}
```

### AppOpenManager - Ad Prefetching

Prefetch app open ads before launching external intents:

```kotlin
// Before launching camera, browser, etc.
appOpenManager.prefetchNextAd { started ->
    if (started) Log.d("ADS", "Prefetch started")
}
startActivityForResult(cameraIntent, REQUEST_CODE)

// When user returns:
// - If ad is ready: shows instantly (no welcome dialog)
// - If still loading: welcome dialog waits for it

// Check loading state
if (appOpenManager.isAdLoading()) {
    // Ad is being fetched
}
```

### NativeAdManager - Cross-Ad-Unit Fallback

When enabled, native ads can fall back to ANY cached ad from ANY ad unit:

```kotlin
// Enable in config
AdManageKitConfig.enableCrossAdUnitFallback = true

// Now getCachedNativeAd will:
// 1. Try exact ad unit match
// 2. Try same base ad unit variants
// 3. Try ANY cached ad from any unit (if enableCrossAdUnitFallback = true)
val ad = NativeAdManager.getCachedNativeAd(adUnitId, enableFallbackToAnyAd = true)
```

### Enhanced Firebase Analytics

Session-level tracking for comprehensive ad performance metrics:

```kotlin
// Get current session stats
val stats = AdManager.getInstance().getAdStats()
// Returns: session_requests, session_fills, session_impressions,
//          fill_rate_percent, show_rate_percent, pool_size, ready_units

// Reset for new session
AdManager.getInstance().resetAdStats()
```

**Logged events:**
| Event | Description |
|-------|-------------|
| `ad_request` | When ad load is initiated |
| `ad_fill` | When ad loads successfully |
| `ad_impression_detailed` | When ad is shown (with fill/show rates) |
| `ad_not_shown` | When ad couldn't be shown |

**User properties updated:**
- `total_ad_requests`, `total_ad_fills`, `total_ads_shown`
- `ad_fill_rate`, `ad_show_rate`

---

## API Changes

### AdManager - New Methods

| Method | Description |
|--------|-------------|
| `loadMultipleAdUnits(context, adUnitIds)` | Load multiple ad units into pool |
| `loadMultipleAdUnits(context, vararg adUnitIds)` | Vararg version |
| `showOrWaitForAd(activity, callback, timeout, showDialog)` | Smart splash ad showing |
| `getPoolSize()` | Number of ready ads in pool |
| `getReadyAdUnits()` | Set of ad unit IDs with ready ads |
| `isReady(adUnitId)` | Check if specific unit has ad ready |
| `clearAdPool()` | Remove all ads from pool |
| `getAdStats()` | Get session ad statistics |
| `resetAdStats()` | Reset session statistics |
| `preloadAd(context, adUnitId)` | Proactively preload an ad |
| `resetAdThrottling()` | Reset display counters and intervals |
| `enableAggressiveAdLoading()` | Set 5s interval for max show rate |
| `getLastAdShowTime()` | Timestamp of last ad show |
| `getTimeSinceLastAd()` | Milliseconds since last ad |
| `isLoading()` | Check if any ad is loading |

### AppOpenManager - New Methods

| Method | Description |
|--------|-------------|
| `prefetchNextAd(onPrefetchStarted)` | Prefetch ad before external intent |
| `isAdLoading()` | Check if ad is currently loading |

### AdManageKitConfig - New Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enableCrossAdUnitFallback` | Boolean | `false` | Allow native ad fallback across ad units |

---

## Improvements

### Modern Window APIs

Replaced deprecated `systemUiVisibility` with `WindowInsetsController` for Android 11+ compatibility:

```kotlin
// Old (deprecated)
decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or ...

// New (v3.0.0)
WindowCompat.setDecorFitsSystemWindows(window, false)
WindowCompat.getInsetsController(window, decorView).apply {
    hide(WindowInsetsCompat.Type.systemBars())
    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
```

### Duplicate Load Prevention

`loadInterstitialAd()` now prevents duplicate requests:
- Skips if ad unit is already loading
- Skips if ad unit already has ad in pool
- Logs debug events for tracking

### Improved Timeout Handling

`loadInterstitialAdForSplash()` timeout now:
- Resets `isAdLoading` flag to allow new requests
- Still saves ad if it arrives after timeout (not wasted)
- Prevents blocking of subsequent load calls

### Thread-Safe Ad Pool

Ad pool uses `ConcurrentHashMap` for thread safety:
- Safe concurrent access from multiple threads
- `loadingAdUnits` tracks in-flight requests
- No race conditions on load/show

---

## Migration Guide

### From 2.9.0 to 3.0.0

v3.0.0 is **fully backward compatible**. All existing code continues to work.

#### Recommended: Adopt Smart Splash Ads

Replace separate load + show calls with single `showOrWaitForAd()`:

```kotlin
// Before (v2.9.0) - Two-step approach
AdManager.getInstance().loadInterstitialAdForSplash(this, adUnitId, 10_000, object : AdManagerCallback() {
    override fun onNextAction() {
        if (!AdManager.getInstance().isDisplayingAd()) {
            AdManager.getInstance().forceShowInterstitial(this@SplashActivity, callback)
        }
    }
})

// After (v3.0.0) - Single smart call
AdManager.getInstance().showOrWaitForAd(
    activity = this,
    callback = object : AdManagerCallback() {
        override fun onNextAction() { navigateNext() }
    },
    timeoutMillis = 10_000
)
```

#### Recommended: Use Ad Pool for Higher Show Rate

```kotlin
// Before (single ad unit)
AdManager.getInstance().loadInterstitialAd(context, "single_unit")

// After (multiple ad units for redundancy)
AdManager.getInstance().loadMultipleAdUnits(context, "high_ecpm", "medium_ecpm", "fallback")
```

#### Optional: Enable Cross-Ad-Unit Fallback

```kotlin
// In Application.onCreate()
AdManageKitConfig.enableCrossAdUnitFallback = true
```

---

## Installation

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.0.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.0.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.0.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.0.0'
```

---

## Full Changelog

### AdManager Enhancements

- Added ad pool system with `ConcurrentHashMap` for storing multiple ad units
- Added `loadMultipleAdUnits()` for loading multiple ad units at once
- Added `showOrWaitForAd()` smart method for splash screens
- Added duplicate load prevention (skips if already loading or in pool)
- Added session-level Firebase analytics (requests, fills, impressions, rates)
- Added `getAdStats()` and `resetAdStats()` for analytics access
- Added pool utility methods: `getPoolSize()`, `getReadyAdUnits()`, `clearAdPool()`
- Added `preloadAd()`, `resetAdThrottling()`, `enableAggressiveAdLoading()`
- Added `getLastAdShowTime()`, `getTimeSinceLastAd()`, `isLoading()`
- Improved `loadInterstitialAdForSplash()` timeout handling (resets loading flag)
- Updated loading dialog to use modern `WindowInsetsController` API
- Auto-reload now reloads the specific ad unit that was shown

### AppOpenManager Enhancements

- Added `prefetchNextAd()` for prefetching before external intents
- Added `isAdLoading()` to check loading state
- Changed `currentActivityRef: WeakReference<Activity>` to direct reference for reliability
- Updated welcome dialog to use modern `WindowInsetsController` API

### NativeAdManager Enhancements

- Added cross-ad-unit fallback when `enableCrossAdUnitFallback` is true
- Added documentation for destructive read pattern (ad removed from cache on retrieve)
- Improved fallback priority: same base unit variants → cross ad unit

### NativeTemplateView Enhancements

- Added `MEDIUM_HORIZONTAL` template: 55% media (left) / 45% content (right) horizontal split
- Total templates now: 24 (18 standard + 6 video)

### Config Enhancements

- Added `enableCrossAdUnitFallback` property to `AdManageKitConfig`

### Technical Improvements

- Replaced deprecated `systemUiVisibility` with `WindowInsetsController` throughout
- Added `WindowCompat.setDecorFitsSystemWindows()` for edge-to-edge display
- Thread-safe ad pool with `ConcurrentHashMap` and `ConcurrentHashMap.newKeySet()`
- Improved logging with pool size and ad unit tracking