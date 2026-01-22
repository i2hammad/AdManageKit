# Release Notes - v3.3.5

**Release Date**: January 2025

## App Open Loading Strategies

This release brings full loading strategy support to AppOpenManager, aligning it with the existing interstitial and native ad loading patterns.

### New Features

#### Loading Strategy Support
AppOpenManager now properly uses `AdManageKitConfig.appOpenLoadingStrategy`:

- **ON_DEMAND**: Fetches fresh ad with welcome dialog. Uses cached ad if still fresh (within `appOpenAdFreshnessThreshold`).
- **ONLY_CACHE**: Only shows cached ads instantly. Silently loads new ad in background if unavailable.
- **HYBRID**: Shows cached if available, fetches with welcome dialog otherwise (recommended).

```kotlin
AdManageKitConfig.apply {
    appOpenLoadingStrategy = AdLoadingStrategy.HYBRID  // default
}
```

#### Ad Freshness Tracking
Cached ads now track when they were loaded to prevent showing stale content:

```kotlin
// Configure freshness threshold (default: 4 hours per Google recommendation)
AdManageKitConfig.appOpenAdFreshnessThreshold = 4.hours

// Check cached ad age
val ageMs = appOpenManager.getCachedAdAgeMs()
```

#### Auto-Reload Configuration
Control whether ads automatically reload after being dismissed:

```kotlin
AdManageKitConfig.appOpenAutoReload = true  // default: true
```

### Bug Fixes

- **Fixed auto-reload after ad dismissal**: Previously, `fetchAd()` was not being called after ad dismissal. Now properly reloads based on `appOpenAutoReload` setting.
- **Fixed wasted ads**: ON_DEMAND strategy now checks if cached ad is still fresh before discarding it and fetching a new one.

### Deprecations

- **`appOpenFetchFreshAd`**: Deprecated in favor of `appOpenLoadingStrategy`
  - `appOpenFetchFreshAd = true` → `appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND`
  - `appOpenFetchFreshAd = false` → `appOpenLoadingStrategy = AdLoadingStrategy.HYBRID`

### Migration

If you were using `appOpenFetchFreshAd`:

```kotlin
// Before (deprecated)
AdManageKitConfig.appOpenFetchFreshAd = true

// After
AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND
```

### API Changes

#### New Methods in AppOpenManager
- `getCachedAdAgeMs(): Long` - Returns age of cached ad in milliseconds

#### New Config Options in AdManageKitConfig
- `appOpenAdFreshnessThreshold: Duration` - Max age for "fresh" cached ad (default: 4 hours)
- `appOpenAutoReload: Boolean` - Auto-reload after dismissal (default: true)

---

## Installation

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.3.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.3.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.3.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.3.5'
```

## Full Changelog

- Full `AdLoadingStrategy` support for AppOpenManager
- Ad freshness tracking with `adLoadTime` and `appOpenAdFreshnessThreshold`
- Auto-reload configuration with `appOpenAutoReload`
- Deprecated `appOpenFetchFreshAd` in favor of `appOpenLoadingStrategy`
- Fixed auto-reload after ad dismissal
- Fixed wasted ads when using ON_DEMAND strategy
- Added `getCachedAdAgeMs()` method
- Updated documentation for app open ads
