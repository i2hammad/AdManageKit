# Release Notes - v3.4.0

**Release Date**: February 17, 2026

## Overview

v3.4.0 fixes two `AppOpenManager` correctness issues — a ~50% splash show-rate bug and a tablet layout regression — and consolidates all improvements introduced in the 3.3.x series (multi-provider waterfall, Yandex Ads, app open ad callback improvements, and adaptive banner sizing).

---

## What's New

### Fix: App Open Ad ~50% Show Rate on Splash

`forceShowAdIfAvailable()` was silently skipping the ad roughly half the time when no cached ad was available. The fix closes a race condition between the lifecycle-driven `showAdIfAvailable()` and the explicit splash call.

**Root cause**: On every foreground return, `AppOpenManager.onStart()` calls `showAdIfAvailable()`, which — when no cached ad exists — starts a dialog-based ad fetch with a **null callback**. If `forceShowAdIfAvailable()` was then called (e.g. from `onResume()`), it found the dialog already showing and immediately fired `onNextAction()` via the guard, causing navigation to proceed before the ad could be shown. The ad would eventually load and appear over the wrong screen with no callback wired up.

**Why ~50%**: Roughly half of app opens have a cached ad (background prefetch from `onStop`) and half don't. Cached-ad opens worked fine; no-cache opens always missed.

**Fixes applied** (`AppOpenManager.kt`):

1. **`showAdIfAvailable()` now guards against `isFetchingWithDialog`** — prevents `onStart()`'s automatic path from starting a competing dialog fetch when `forceShowAdIfAvailable()` has already started one.

2. **Added `dialogFetchCallback` field** — the callback for the current dialog-based fetch is stored as a mutable field rather than only in a captured local variable, allowing it to be replaced at any time before the ad loads.

3. **`forceShowAdIfAvailable()` takes over an in-progress fetch** — when called while `isFetchingWithDialog` is `true`, it replaces `dialogFetchCallback` with the splash callback. The ongoing load then delivers its result (ad shown / failed / timed out) to the correct caller.

```kotlin
// Works correctly regardless of whether the ad is cached or loading
appOpenManager.forceShowAdIfAvailable(this, object : AdManagerCallback() {
    override fun onNextAction() { navigateToMain() }        // always fires
    override fun onAdTimedOut() { navigateToMain() }        // timeout path
    override fun onFailedToLoad(error: AdKitError?) { navigateToMain() } // failure path
})
```

No API changes — existing integration code is unaffected.

---

### Fix: Native Large Ad Tablet Layout

Fixed a layout rendering issue in the native large ad view on tablet-sized screens (sw600dp+).

**Root cause**: `layout_native_large.xml` (sw600dp) used `LinearLayout` as the root container, which caused incorrect measurement and layout-pass behaviour for the native ad content on larger screens.

**Fix**: Container changed from `LinearLayout` to `FrameLayout`, matching the intended single-child wrapping semantics.

This change is **transparent** — no API or integration changes are required.

---

## Summary of 3.3.x Series (Included in This Release)

### Multi-Provider Waterfall & Yandex Ads (3.3.8)

A new provider abstraction layer allows multiple ad networks to work as a fallback chain with zero changes to existing ad-loading code.

```kotlin
// Register providers once (e.g. in Application.onCreate)
AdProviderConfig.registerProvider(AdProvider.ADMOB, AdMobProviderRegistration())
AdProviderConfig.registerProvider(AdProvider.YANDEX, YandexProviderRegistration())

// Configure waterfall chains per ad type
AdProviderConfig.setInterstitialChain(listOf(AdProvider.ADMOB, AdProvider.YANDEX))
AdProviderConfig.setAppOpenChain(listOf(AdProvider.ADMOB, AdProvider.YANDEX))
AdProviderConfig.setBannerChain(listOf(AdProvider.ADMOB, AdProvider.YANDEX))
AdProviderConfig.setNativeChain(listOf(AdProvider.ADMOB, AdProvider.YANDEX))
AdProviderConfig.setRewardedChain(listOf(AdProvider.ADMOB, AdProvider.YANDEX))
```

See [Multi-Provider Waterfall](../MULTI_PROVIDER_WATERFALL.md) and [Yandex Integration](../YANDEX_INTEGRATION.md) for the full guide.

### App Open Ad Callback Improvements (3.3.9)

`AdManagerCallback` now properly fires failure and timeout callbacks for app open ads, and the welcome dialog is always dismissed before any callback fires.

```kotlin
appOpenManager.forceShowAdIfAvailable(activity, object : AdManagerCallback() {
    override fun onNextAction() { navigateNext() }
    override fun onFailedToLoad(error: AdKitError?) { /* load failure */ }
    override fun onAdTimedOut() { /* timeout */ }
})
```

### Adaptive Full-Width Banner in Waterfall (3.3.9)

`AdMobBannerProvider` now uses adaptive full-width sizing by default (was fixed 320×50dp). Collapsible banner settings pass through the waterfall chain to the provider.

---

## Installation

```groovy
// Core modules
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.4.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.4.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.4.0'

// Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.4.0'

// Yandex provider (optional)
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.4.0'
```

---

## Full Changelog

- **Fix**: `AppOpenManager` splash ad show rate — `forceShowAdIfAvailable()` now correctly takes over an in-progress dialog fetch instead of firing `onNextAction()` prematurely
- **Fix**: `showAdIfAvailable()` skips early when a dialog-based fetch is already in progress (`isFetchingWithDialog` guard)
- **Fix**: `dialogFetchCallback` field ensures the most recent `forceShowAdIfAvailable()` caller always receives the ad result
- **Fix**: `layout_native_large.xml` (sw600dp) container changed from `LinearLayout` to `FrameLayout` for correct tablet rendering
- **Fix**: `AdManagerCallback.onFailedToLoad()` now fires for app open ad failures (dialog dismissed first)
- **Fix**: Welcome dialog always dismissed before any callback fires (`onFailedToLoad`, `onAdTimedOut`, `onNextAction`)
- **Fix**: Waterfall banner ads now use adaptive full-width sizing (was fixed 320×50dp)
- **Fix**: Collapsible banner settings now passed through waterfall to `AdMobBannerProvider`
- **New**: `AdManagerCallback.onAdTimedOut()` callback for app open ad load timeout events
- **New**: `admanagekit-yandex` module with full Yandex Mobile Ads SDK integration
- **New**: Multi-provider waterfall for interstitial, app open, banner, native, and rewarded ad types
- **Change**: `AdMobBannerProvider` default ad size is now adaptive full-width (was `AdSize.BANNER`)
- **Change**: `AdMobProviderRegistration.create()` default banner size is now adaptive
