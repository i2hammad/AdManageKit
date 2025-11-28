# Release Notes - v2.8.0

## Highlights

- **Loading Strategy for AdManager**: `forceShowInterstitial()` now respects global loading strategy
- **Global Auto-Reload Config**: New `interstitialAutoReload` setting with per-call override
- **Code Cleanup**: Removed unused `respectInterval` from InterstitialAdBuilder

---

## New Features

### 1. Loading Strategy Support in AdManager

`forceShowInterstitial()` and `forceShowInterstitialWithDialog()` now respect `AdManageKitConfig.interstitialLoadingStrategy`:

```kotlin
// Configure globally
AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.HYBRID

// Old method now respects strategy!
AdManager.getInstance().forceShowInterstitial(activity, callback)
```

**Behavior based on strategy:**

| Strategy | Behavior |
|----------|----------|
| ON_DEMAND | Always fetch fresh ad with dialog |
| ONLY_CACHE | Show cached if ready, skip otherwise |
| HYBRID | Show cached if ready, fetch fresh otherwise |

### 2. New `forceShowInterstitialAlways()` Method

For cases where you need to bypass the strategy and always force fetch:

```kotlin
// Always forces fresh fetch (ignores global strategy)
AdManager.getInstance().forceShowInterstitialAlways(activity, callback)
```

This is used internally by `InterstitialAdBuilder` when explicit force behavior is needed.

### 3. Global Auto-Reload Configuration

New `interstitialAutoReload` setting in `AdManageKitConfig`:

```kotlin
// Global config - applies to all AdManager methods
AdManageKitConfig.interstitialAutoReload = false  // Disable auto-reload globally

// Per-call override via InterstitialAdBuilder
InterstitialAdBuilder.with(activity)
    .adUnit(adUnitId)
    .autoReload(true)  // Override global setting for this call
    .show { next() }

// Per-call override via AdManager
AdManager.getInstance().showInterstitialIfReady(activity, callback, reloadAd = false)
```

**Priority:** `InterstitialAdBuilder.autoReload()` > `AdManageKitConfig.interstitialAutoReload`

---

## Code Cleanup

### Removed Unused `respectInterval`

The `respectInterval` property and method in `InterstitialAdBuilder` were never used and have been removed. Use `minInterval()` instead for interval control:

```kotlin
InterstitialAdBuilder.with(activity)
    .adUnit(adUnitId)
    .minIntervalSeconds(30)  // Minimum 30 seconds between shows
    .show { next() }
```

---

## API Changes

### AdManager - Updated Methods

| Method | Change |
|--------|--------|
| `forceShowInterstitial()` | Now respects `interstitialLoadingStrategy` |
| `forceShowInterstitialWithDialog()` | Now respects `interstitialLoadingStrategy` |
| `showInterstitialIfReady()` | Default `reloadAd` now uses global config |
| `showInterstitialAdByTime()` | Now uses global `interstitialAutoReload` |
| `showInterstitialAdByCount()` | Now uses global `interstitialAutoReload` |

### AdManager - New Methods

| Method | Description |
|--------|-------------|
| `forceShowInterstitialAlways()` | Always force fetch (bypasses strategy) |

### AdManageKitConfig - New Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `interstitialAutoReload` | Auto-reload interstitial after showing | `true` |

### InterstitialAdBuilder - Removed

| Method | Reason |
|--------|--------|
| `respectInterval()` | Never used, use `minInterval()` instead |

---

## Migration Guide

### From 2.7.0 to 2.8.0

**Fully backward compatible** with one behavioral change:

**`forceShowInterstitial()` behavior change:**
- **Before (2.7.0):** Always fetched fresh ad
- **After (2.8.0):** Respects `interstitialLoadingStrategy`

If you need the old behavior (always force fetch):
```kotlin
// Option 1: Set strategy to ON_DEMAND
AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND

// Option 2: Use the new always-force method
AdManager.getInstance().forceShowInterstitialAlways(activity, callback)
```

---

## Installation

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.8.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.8.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.8.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.8.0'
```

---

## Full Changelog

- `forceShowInterstitial()` now respects `AdManageKitConfig.interstitialLoadingStrategy`
- `forceShowInterstitialWithDialog()` now respects loading strategy
- Added `forceShowInterstitialAlways()` for explicit force fetch behavior
- Added `interstitialAutoReload` to AdManageKitConfig for global auto-reload control
- Updated AdManager methods to use global `interstitialAutoReload` as default
- Removed unused `respectInterval` property and method from InterstitialAdBuilder
- InterstitialAdBuilder now uses `forceShowInterstitialAlways()` for explicit force behavior
