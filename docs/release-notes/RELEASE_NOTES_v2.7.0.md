# Release Notes - v2.7.0

## Highlights

- **Smart Splash Screen Support**: New `waitForLoading()` for optimal splash ad experience
- **Fixed HYBRID/ONLY_CACHE Strategies**: Now properly use cached ads
- **Premium User Optimization**: Skip ad requests for premium users

---

## New Features

### 1. Smart Splash Screen Pattern

New `waitForLoading()` method in `InterstitialAdBuilder` for intelligent splash screen ad handling:

```kotlin
// In Application.onCreate() - Start loading early
AdManager.getInstance().loadInterstitialAd(this, adUnitId)

// In SplashActivity - Smart wait behavior
InterstitialAdBuilder.with(this)
    .adUnit(adUnitId)
    .waitForLoading()     // Smart: shows cached, waits for loading, or force fetches
    .timeout(5000)        // 5 second max wait
    .show { startMainActivity() }
```

**Behavior:**
| Ad State | Action |
|----------|--------|
| **READY** | Shows immediately (no waiting) |
| **LOADING** | Waits with timeout, shows when ready |
| **NOT STARTED** | Force fetches with loading dialog |

### 2. New AdManager Methods

```kotlin
// Check if ad is currently being loaded
AdManager.getInstance().isLoading(): Boolean

// Smart show for splash screens (used by waitForLoading())
AdManager.getInstance().showOrWaitForAd(
    activity,
    callback,
    timeoutMillis = 5000,
    showDialogIfLoading = true
)
```

### 3. Enhanced Frequency Controls

New methods in `InterstitialAdBuilder`:

```kotlin
InterstitialAdBuilder.with(activity)
    .adUnit(adUnitId)
    .everyNthTime(3)           // Show every 3rd call
    .maxShows(5)               // Maximum 5 shows total
    .minIntervalSeconds(30)    // Minimum 30 seconds between shows
    .timeout(5000)             // 5 second load timeout
    .show { next() }
```

---

## Bug Fixes

### 1. HYBRID/ONLY_CACHE Strategy Fix

**Before (Bug):** When ad was cached, HYBRID and ONLY_CACHE strategies incorrectly called `forceShowInterstitial()` which cleared the cache and fetched fresh.

**After (Fixed):** Now uses `showInterstitialIfReady()` to properly show cached ads.

```kotlin
// HYBRID now correctly:
// - If ready → shows CACHED ad (not fresh fetch)
// - If not ready → fetches fresh with dialog

// ONLY_CACHE now correctly:
// - If ready → shows CACHED ad
// - If not ready → skips (no fetch)
```

### 2. Premium User Optimization

**Before:** `loadInterstitialAd()` made network requests even for premium users.

**After:** Skips ad loading entirely for premium users, saving bandwidth.

```kotlin
fun loadInterstitialAd(context: Context, adUnitId: String) {
    // Skip loading for premium users - no need to request ads
    val purchaseProvider = BillingConfig.getPurchaseProvider()
    if (purchaseProvider.isPurchased()) {
        return  // No network request made
    }
    // ... load ad
}
```

---

## API Changes

### InterstitialAdBuilder - New Methods

| Method | Description |
|--------|-------------|
| `.waitForLoading()` | Smart splash screen behavior |
| `.everyNthTime(n)` | Show every Nth call |
| `.minInterval(ms)` | Minimum ms between shows |
| `.minIntervalSeconds(s)` | Minimum seconds between shows |
| `.timeout(ms)` | Set load timeout in ms |
| `.timeoutSeconds(s)` | Set load timeout in seconds |

### AdManager - New Methods

| Method | Description |
|--------|-------------|
| `isLoading()` | Check if ad is currently loading |
| `showOrWaitForAd()` | Smart show with wait-for-loading support |

---

## Migration Guide

### From 2.6.0 to 2.7.0

**Fully backward compatible.** Optionally adopt new features:

```kotlin
// Old splash pattern (still works)
AdManager.getInstance().loadInterstitialAdForSplash(this, adUnitId, 10000, callback)

// New recommended pattern
AdManager.getInstance().loadInterstitialAd(this, adUnitId)  // In Application
InterstitialAdBuilder.with(this)
    .adUnit(adUnitId)
    .waitForLoading()
    .timeout(5000)
    .show { startMainActivity() }
```

---

## Installation

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.7.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.7.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.7.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.7.0'
```

---

## Full Changelog

- Added `waitForLoading()` to InterstitialAdBuilder for smart splash screen support
- Added `showOrWaitForAd()` to AdManager for intelligent ad display
- Added `isLoading()` to AdManager to check loading state
- Added frequency control methods: `everyNthTime()`, `minInterval()`, `minIntervalSeconds()`
- Added timeout methods: `timeout()`, `timeoutSeconds()`
- Fixed HYBRID strategy to use cached ads instead of fetching fresh
- Fixed ONLY_CACHE strategy to use cached ads instead of fetching fresh
- Optimized `loadInterstitialAd()` to skip loading for premium users
- Updated documentation with splash screen patterns
