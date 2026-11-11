# Release Notes — v4.4.11

**Release Date:** 2026-11-11

## Overview

v4.4.11 is a **bug-fix release** on top of v4.4.10. When `MobileAds.initialize()` finishes after the init wait has given up, native and banner views now load their ad then, instead of leaving the slot empty. No dependency versions changed from v4.4.10. Apps still need **Kotlin 2.3 or newer**.

---

## Fixed

### Ad slots stayed empty after a late init

v4.4.10 made every AdMob load wait for `MobileAds.initialize()` instead of crashing. That wait gives up after `mobileAdsInitTimeout` (10 s), so a screen is never stuck on a shimmer. But if init finished a moment later, the slot stayed empty until the user reopened the screen.

`NativeBannerSmall`, `NativeBannerMedium`, `NativeLarge`, `NativeTemplateView` and `BannerAdView` now keep watching for init after that timeout and load their ad once it completes:

| When the load ran | What happens |
|---|---|
| Init already done (the usual case) | Loads immediately, as before |
| Init finished within 10 s | Load held, then starts as soon as init completes (v4.4.10) |
| Init finished after 10 s | `onFailedToLoad(TIMEOUT)` and the slot hides; once init completes the view loads again and calls `onAdLoaded()` (**new**) |
| Init never finishes | `onFailedToLoad(TIMEOUT)`; the view stops watching after `mobileAdsLateLoadWindow` |

There's no second load if the view is destroyed or its activity is finishing, the user is premium, a new load is started on the view, or the failure happened after init was already done (a real no-fill). A waterfall that already filled the slot from another provider (e.g. Yandex) doesn't trigger one either.

Your callback can see `onFailedToLoad(TIMEOUT)` followed later by `onAdLoaded()`. If your code treats the first failure as final (for example, it removes the view), keep the view in place or set the window to zero.

Interstitial, rewarded and app-open loads are unchanged: a timed-out pool preload is dropped, and the next `forceShowInterstitial` loads a fresh ad behind its loading dialog.

## Added

```kotlin
AdManageKitConfig.mobileAdsLateLoadWindow = 2.minutes // default
AdManageKitConfig.mobileAdsLateLoadWindow = Duration.ZERO // v4.4.10 behaviour
```

The sample app has a new `InitRaceTestActivity` that reproduces the crash and the late-init cases on a device; see its class doc for the `adb` commands.

## Verified

On a Galaxy A71 5G (Android 13), each run a cold start, with init delayed on purpose:

- v4.4.9, init 6 s late: crashes with `MobileAds.initialize must be called before using the Google Mobile Ads SDK`
- v4.4.11, init 6 s late: native, banner and interstitial ads show once init completes
- v4.4.11, init 15 s late: native and banner fail at 10 s, then load once init completes; an interstitial shown at 17 s loads fresh and shows
- v4.4.11, init never runs: no crash; views stay empty, interstitial moves on via `onNextAction()`

## Upgrading

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.11'
```

(and the same version for `-core`, `-billing`, `-compose`, `-yandex`)
