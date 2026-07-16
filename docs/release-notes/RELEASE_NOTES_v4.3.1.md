# Release Notes — v4.3.1

**Release Date:** 2026-07-16

## Overview

v4.3.1 is a patch release addressing crashes caused by the Next-Gen Google Mobile Ads SDK delivering its callbacks on a background thread. Under the legacy SDK these callbacks arrived on the main thread; the Next-Gen SDK does not guarantee that, so any handler that touched a `View` — or any code path that constructed the next provider's ad `View` — could crash with `CalledFromWrongThreadException` or `Can't create handler inside thread that has not called Looper.prepare()`.

All affected callback and chain-advance paths are now marshalled to the main thread. No API changes; upgrade is a drop-in for 4.3.0.

All modules are bumped to **4.3.1**.

---

## Fixed

### Interstitial ads (`AdManager`)

- **Dismiss and failed-to-show callbacks delivered on the main thread.** GMA fires `onAdDismissedFullScreenContent` and `onAdFailedToShowFullScreenContent` on a background thread. `callback.onNextAction()` is now posted to the main thread on both paths, so every `InterstitialAdBuilder.show { }` call site whose `onNextAction` handler touches views (navigation, dismissing a dialog, updating UI) no longer risks a `CalledFromWrongThreadException`.
- **`onAdLoaded` no longer risks a `NullPointerException`.** The load callback now forwards the non-null local `interstitialAd` parameter instead of re-reading the shared `mInterstitialAd` field. A concurrent show/dismiss/load on another thread could null the field between assignment and use, making the previous `mInterstitialAd!!` throw.

### Banner waterfall (`BannerWaterfall`)

- **The provider chain now advances on the main thread.** A provider's failure callback (e.g. AdMob's `onAdFailedToLoad`) fires on a background thread; advancing the chain there constructed the next provider's banner `View` off the main thread and crashed with `Can't create handler inside thread that has not called Looper.prepare()`. The chain now hops to the main thread before continuing.
- **`onBannerLoaded` delivered on the main thread**, since the app adds the returned `bannerView` to a `ViewGroup`.

### Native waterfall (`NativeWaterfall`)

- **The provider chain now advances on the main thread** — same fix as the banner waterfall, so the next provider's native `View` is never constructed off the main thread.
- **`onNativeAdLoaded` delivered on the main thread**, since the consumer inflates/adds the native `View`.

### Native templates (`NativeTemplateView`)

- **`onFailedToLoad` delivered on the main thread.** `NativeAdLoader` reports load failures on a background thread; the placeholder/shimmer hide and the app's `onFailedToLoad(adError)` callback now run together on the main thread, since failure handlers commonly hide a spinner or container.

### App open ads (`AppOpenManager`)

- **Defense-in-depth load guard.** `fetchAdWithRetry` now skips the load if `MobileAds` is not yet initialized, protecting the actual load site regardless of which path reaches it. This complements the existing guards on `fetchAd` / `showAdIfAvailable` and prevents an `IllegalStateException` if the retry path is ever entered through another route.

---

## Upgrading

Drop-in from 4.3.0 — no source or binary changes required.

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.3.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.3.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.3.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.3.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.3.1'
```
