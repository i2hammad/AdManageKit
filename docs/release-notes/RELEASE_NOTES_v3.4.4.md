# Release Notes - v3.4.4

**Release Date**: March 13, 2026

## Overview

v3.4.4 fixes native ad show rate issues across all native ad views (`NativeBannerSmall`, `NativeBannerMedium`, `NativeLarge`, `NativeTemplateView`) and adds null safety to `RewardedAdManager`. The root cause of low show rates was parent container visibility not being restored after a previous ad load failure — children were set to VISIBLE but remained invisible because their parent was still GONE.

---

## What's New

### Native Ad Show Rate Fix (All Views)

After a failed ad load, error callbacks hide the parent container (`binding.root`, `binding.adContainer`, or `binding.adUnit`). On subsequent loads, child views were set to VISIBLE but remained invisible because the parent was still GONE.

**Before:** Error → parent GONE → next load sets children VISIBLE → ad invisible (parent still GONE)

**After:** `loadAd()` now resets parent container visibility to VISIBLE before loading. `displayAd()` also ensures parent visibility is restored for cached/waterfall ad display paths.

**Affected views:**
- **NativeBannerSmall**: `binding.root` now reset in `loadAd()` and `displayAd()`
- **NativeBannerMedium**: `binding.adContainer` now reset in `loadAd()`, `displayAd()`, `displayCachedAdSafely()`, and waterfall success path
- **NativeLarge**: `binding.adUnit` now reset in `loadAd()` and `binding.root` in `displayAd()`
- **NativeTemplateView**: `binding.root` now reset in `loadAd()` and `displayAd()`

---

### NativeBannerMedium: Synchronous setNativeAd()

`populateNativeAdView()` was deferring `setNativeAd()` via `nativeAdView.post {}`, which risked the call never executing if the view was detached. Changed to a synchronous call for reliable impression registration.

---

### NativeBannerMedium: Shimmer Overlap Fix

When displaying cached or waterfall ads via `displayCachedAdSafely()`, the shimmer loading animation was not hidden, causing it to overlap the actual ad content. Shimmer is now explicitly set to GONE in all display paths.

---

### RewardedAdManager Null Safety

`RewardedAdManager.adUnitId` was a `lateinit var` that would crash with `UninitializedPropertyAccessException` if any method was called before `initialize()`. Changed to `var adUnitId: String = ""` with `.isEmpty()` guards on all 5 public entry points:

- `loadRewardedAd(context)`
- `loadRewardedAd(context, callback)`
- `loadRewardedAdWithTimeout(context, timeout, callback)`
- `showAd(activity, callback)`
- `preload(context)`

Each guard logs a warning and returns the appropriate callback (e.g., `onAdFailedToLoad()` or `onAdDismissed()`).

---

### Empty adUnitId Guards (All Native Views)

All native ad views (`NativeBannerSmall`, `NativeBannerMedium`, `NativeLarge`, `NativeTemplateView`) now use `var adUnitId: String = ""` instead of `lateinit var` and guard against empty ad unit IDs in their load methods.

---

## Full Changelog

- **Fixed**: Low native ad show rate caused by parent container visibility not restored after previous load failure (NativeBannerSmall, NativeBannerMedium, NativeLarge, NativeTemplateView)
- **Fixed**: `NativeBannerMedium.populateNativeAdView()` deferred `setNativeAd()` via `post {}` — changed to synchronous call for reliable impression registration
- **Fixed**: Shimmer overlay on cached/waterfall ads in `NativeBannerMedium.displayCachedAdSafely()`
- **Fixed**: `RewardedAdManager` crash when methods called before `initialize()` — `lateinit var adUnitId` changed to empty string default with guards
- **Fixed**: `NativeBannerSmall`, `NativeLarge`, `NativeTemplateView` crash when `adUnitId` accessed before assignment — `lateinit` changed to empty string default
- **Added**: Empty `adUnitId` validation with warning logs and appropriate callback responses across all native views and `RewardedAdManager`

---

## Installation

```groovy
// Core modules
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.4.4'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.4.4'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.4.4'

// Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.4.4'

// Yandex provider (optional)
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.4.4'
```
