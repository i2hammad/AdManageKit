# Release Notes - v3.3.2

## Highlights

- **InterstitialAdBuilder Fix**: Ad unit now properly assigned to AdManager on first HYBRID fetch
- **everyNthTime Fix**: Call counter persists across builder instances
- **New Native Templates**: `icon_left` and `top_icon_media` for GridView display
- **Call Counter API**: Manage everyNthTime counters via AdManager

---

## Bug Fixes

### InterstitialAdBuilder Ad Unit Assignment

**Issue**: When using `InterstitialAdBuilder` with HYBRID strategy and no cached ad available, the first "force fetch" path would fail because the ad unit was only stored in `primaryAdUnit` but never assigned to `AdManager.adUnitId`.

**Fix**: The `adUnit()` method now immediately sets the ad unit on AdManager:

```kotlin
// Before (v3.3.1) - Ad unit not available in AdManager
InterstitialAdBuilder.with(activity)
    .adUnit("ca-app-pub-xxx/yyy")  // Only stored in builder
    .strategy(AdLoadingStrategy.HYBRID)
    .showWithDialog { navigateNext() }
// First call with empty cache would fail!

// After (v3.3.2) - Ad unit immediately available
InterstitialAdBuilder.with(activity)
    .adUnit("ca-app-pub-xxx/yyy")  // Now also sets AdManager.adUnitId
    .strategy(AdLoadingStrategy.HYBRID)
    .showWithDialog { navigateNext() }
// Works correctly on first call
```

### everyNthTime Counter Persistence

**Issue**: The `everyNthTime()` feature would not work correctly because the call counter was stored as an instance variable in `InterstitialAdBuilder`. Since a new builder is created for each call, the counter would always reset to 0.

**Fix**: Call counters are now stored in `AdManager` singleton and persist across builder instances:

```kotlin
// Before (v3.3.1) - Counter reset each call
InterstitialAdBuilder.with(activity)
    .adUnit(adUnitId)
    .everyNthTime(3)  // Counter always starts at 0
    .show { }
// Ads shown on EVERY call (counter never reaches 3)

// After (v3.3.2) - Counter persists
InterstitialAdBuilder.with(activity)
    .adUnit(adUnitId)
    .everyNthTime(3)  // Counter increments: 1, 2, 3, 1, 2, 3...
    .show { }
// Ads shown on 3rd, 6th, 9th calls, etc.
```

---

## New Features

### New Native Templates for GridView

Two new templates designed for displaying native ads in GridView or similar layouts:

#### icon_left Template
- Icon positioned on the left side
- MediaView at top for video content support
- Headline and CTA in content section
- Matches `grid_item` styling

#### top_icon_media Template
- Icon and text at top
- MediaView in the middle section
- CTA button at bottom

```kotlin
nativeTemplateView.setTemplate(NativeAdTemplate.ICON_LEFT)
nativeTemplateView.loadNativeAd(activity, adUnitId)

nativeTemplateView.setTemplate(NativeAdTemplate.TOP_ICON_MEDIA)
nativeTemplateView.loadNativeAd(activity, adUnitId)
```

### AdManager Call Counter API

New methods for managing `everyNthTime` counters programmatically:

```kotlin
// Get current counter value for an ad unit
val count = AdManager.getInstance().getCallCount("ca-app-pub-xxx/yyy")

// Manually increment counter
val newCount = AdManager.getInstance().incrementCallCount("ca-app-pub-xxx/yyy")

// Reset counter for specific ad unit
AdManager.getInstance().resetCallCount("ca-app-pub-xxx/yyy")

// Reset all counters (useful when user upgrades to premium)
AdManager.getInstance().resetAllCallCounts()
```

### AdManager.setAdUnitId()

New method to set ad unit ID directly:

```kotlin
// Useful when you need to configure AdManager before using builder
AdManager.getInstance().setAdUnitId("ca-app-pub-xxx/yyy")
```

---

## New API Methods

### AdManager

```kotlin
// Set ad unit ID directly
fun setAdUnitId(adUnitId: String?)

// Increment and return call counter
fun incrementCallCount(adUnitId: String): Int

// Get current call counter value
fun getCallCount(adUnitId: String): Int

// Reset counter for specific ad unit
fun resetCallCount(adUnitId: String)

// Reset all call counters
fun resetAllCallCounts()
```

### NativeAdTemplate (Enum)

```kotlin
ICON_LEFT       // Icon on left, MediaView at top
TOP_ICON_MEDIA  // Icon at top, MediaView in middle
```

---

## Sample App

Added comprehensive **Interstitial Test Suite** activity with tests for:

- **Loading**: Preload, Load with callback, isReady check
- **Strategies**: ON_DEMAND, ONLY_CACHE, HYBRID, FRESH_WITH_FALLBACK
- **Builder Options**: Basic show, force show, timeout, callbacks, everyNthTime, maxShows, minInterval, autoReload
- **AdManager Direct**: forceShowWithDialog, showIfReady, showByTime, showByCount
- **Edge Cases**: Show before load, rapid clicks (duplicate prevention), builder without preload

Access via `InterstitialActivity` > "Interstitial Test Suite" button.

---

## Migration Guide

### From v3.3.1 to v3.3.2

This is a **backward-compatible** release. No code changes required.

**Optional Enhancements**:

#### 1. Use Counter API for everyNthTime Reset

```kotlin
// Reset ad counter when user purchases premium
fun onPremiumPurchased() {
    AdManager.getInstance().resetAllCallCounts()
}
```

#### 2. Use New Native Templates

```kotlin
// For GridView/RecyclerView grid layouts
nativeTemplateView.setTemplate(NativeAdTemplate.ICON_LEFT)

// For vertical scrolling lists
nativeTemplateView.setTemplate(NativeAdTemplate.TOP_ICON_MEDIA)
```

---

## Full Changelog

### Bug Fixes
- Fixed InterstitialAdBuilder not assigning ad unit to AdManager on first HYBRID fetch
- Fixed everyNthTime counter resetting on each builder instance

### New Features
- Added `icon_left` native template with MediaView for GridView
- Added `top_icon_media` native template with MediaView
- Added `setAdUnitId()` to AdManager
- Added call counter API: `incrementCallCount()`, `getCallCount()`, `resetCallCount()`, `resetAllCallCounts()`
- Added Interstitial Test Suite activity to sample app

### Internal Changes
- Call counters moved from InterstitialAdBuilder instance to AdManager singleton
- `InterstitialAdBuilder.adUnit()` now calls `AdManager.getInstance().setAdUnitId()`
