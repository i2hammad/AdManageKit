# Release Notes - v3.4.3

**Release Date**: March 6, 2026

## Overview

v3.4.3 fixes several race conditions in `AppOpenManager` that caused duplicate ad loads, missed ad shows, and auto-navigation on splash screens — with both waterfall and non-waterfall (single-provider) setups.

---

## What's New

### Duplicate Ad Load Prevention

When multiple code paths (e.g. `onStart` background preload and splash `fetchAd`) triggered concurrent waterfall loads, the same ad could be fetched twice. `fetchViaWaterfall(callback, timeout)` now checks if a load is already in progress and attaches to it instead of starting a duplicate.

- If a **dialog-based fetch** is running, the callback is attached to `dialogFetchCallback`
- If a **background preload** is running (excluded-activity preload), the callback is attached to `pendingFetchCallback` with proper timeout handling
- Works for both **waterfall** (`fetchViaWaterfall`) and **non-waterfall** (`fetchAdWithRetry`) paths

---

### Splash Auto-Navigation Fix

When the splash screen's `fetchAd` callback attached to an in-progress background preload, orphaned timeouts could fire `onFailedToLoad` after the ad had already loaded and shown — causing the splash to navigate away while the ad was still displayed.

**Before:** Multiple `fetchViaWaterfall(callback, timeout)` calls each created timeouts, but only the last was tracked. Earlier timeouts fired stale callbacks.

**After:** Previous pending timeouts are cancelled before creating new ones. All completion paths (load, fail, timeout) clear `pendingFetchCallback` state.

---

### Resume App Open Ad Show After Auto-Reload

`showAdIfAvailable()` was blocked by `isLoading` from a background auto-reload preload, preventing app open ads from showing on resume.

**Before:** After dismissing an app open ad, `appOpenAutoReload` triggered `fetchViaWaterfall()` which set `isLoading=true`. The immediate `onStart` → `showAdIfAvailable()` saw `isLoading=true` and skipped.

**After:** `showAdIfAvailable()` only skips for dialog-based fetches (`isFetchingWithDialog`), not background preloads. Background preloads don't block ad display since they're non-interactive.

---

### Comprehensive Ad Lifecycle Logging

All paths in `AppOpenManager` now emit `AdDebugUtils.logEvent` events:
- **`loading`**: Logged when any ad load request starts (`fetchAdWithRetry`, `fetchAd(callback)`, `showAdWithWelcomeDialog`, `fetchAndShowFresh`)
- **`showCachedAd`**: Logged when a cached ad is about to be shown (both regular and forced paths)
- **`onAdLoaded` / `onFailedToLoad`**: Already present, now added to dialog-based and waterfall paths that were previously missing

---

## Full Changelog

- **Fixed**: Duplicate ad loads on cold start when `onStart` preload and splash `fetchAd` run concurrently (both waterfall and non-waterfall)
- **Fixed**: Splash auto-navigation caused by orphaned `pendingFetchTimeoutRunnable` firing stale `onFailedToLoad` callbacks
- **Fixed**: `showAdIfAvailable()` blocked by background auto-reload preload, preventing app open ads on resume
- **Fixed**: ON_DEMAND strategy cold start no longer triggers automatic ad load from `onStart` (explicit `fetchAd`/`forceShow` handles it)
- **Fixed**: Hot-start duplicate loads when returning to SplashActivity
- **Added**: `pendingFetchCallback` mechanism to attach splash callbacks to in-progress background preloads
- **Added**: `hasBeenBackgrounded` flag to distinguish cold start from resume for ON_DEMAND strategy
- **Added**: `loading` log event to all ad load start paths (background preload, dialog fetch, fresh fetch, timeout fetch)
- **Added**: `showCachedAd` log event when cached ads are shown (regular and forced paths)
- **Added**: `AdDebugUtils.logEvent` calls to all previously missing load/failure paths in `AppOpenManager`

---

## Installation

```groovy
// Core modules
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.4.3'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.4.3'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.4.3'

// Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.4.3'

// Yandex provider (optional)
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.4.3'
```
