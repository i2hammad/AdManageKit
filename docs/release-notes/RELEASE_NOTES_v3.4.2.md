# Release Notes - v3.4.2

**Release Date**: March 6, 2026

## Overview

v3.4.2 improves App Open ad reliability by integrating `autoRetryFailedAds` support and preserving late-loading ads that arrive after a timeout.

---

## What's New

### App Open Ads: `autoRetryFailedAds` Support

App open ads now respect `AdManageKitConfig.autoRetryFailedAds`, matching the behavior already present in interstitial, rewarded, and native ads. When enabled, failed app open ad loads are automatically retried with exponential backoff via `AdRetryManager`.

Previously, `AppOpenManager` had its own hardcoded retry logic that always ran regardless of the `autoRetryFailedAds` setting. This has been unified with the centralized retry system.

```kotlin
AdManageKitConfig.apply {
    autoRetryFailedAds = true   // Now respected by app open ads
    maxRetryAttempts = 3        // Used instead of hardcoded value
}
```

**Affected paths:**
- Background preload (`fetchAd()` / `fetchAdWithRetry()`) — now gated on config
- Waterfall preload (`fetchViaWaterfall()`) — retry added on failure

---

### App Open Ads: Late-Loading Ad Preservation

When an app open ad loads after the timeout has already fired, the ad is now cached for later use instead of being silently discarded.

**Before:** If `fetchAd(timeout=8000)` timed out at 8s but the ad loaded at 9s, the ad was thrown away.

**After:** The ad is stored in `appOpenAd` and available for the next `showAdIfAvailable()` call. The original timeout callback still fires normally (no double-callback).

---

## Full Changelog

- **Fixed**: `AppOpenManager` now respects `AdManageKitConfig.autoRetryFailedAds` (was always retrying with hardcoded values)
- **Fixed**: `AppOpenManager` now uses `AdManageKitConfig.maxRetryAttempts` instead of hardcoded retry limit
- **Fixed**: `AppOpenManager` retry now uses `AdRetryManager` for consistent exponential backoff across all ad types
- **Added**: Auto-retry on failure for waterfall-based app open ad preloading
- **Improved**: App open ads that load after timeout are cached for later use instead of discarded
- **Deprecated**: `AppOpenManager.updateRetryConfiguration()` — use `AdManageKitConfig` properties directly

---

## Installation

```groovy
// Core modules
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.4.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.4.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.4.2'

// Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.4.2'

// Yandex provider (optional)
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.4.2'
```
