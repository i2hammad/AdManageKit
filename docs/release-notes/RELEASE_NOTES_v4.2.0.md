# Release Notes — v4.2.0

**Release Date:** 2026-07-08

## Overview

v4.2.0 brings the Google Mobile Ads **Next-Gen SDK** to `main`. Previously this was only available on a separate `nextgen` branch, kept apart because the underlying Google SDK was in beta and the two codebases had diverged. That's no longer the case — the Next-Gen SDK (`ads-mobile-sdk`) reached **stable 1.2.1**, and this release merges Next-Gen support directly into `main`, on top of everything `main` has shipped since (the multi-provider waterfall, the Yandex module, and dozens of fixes the old `nextgen` branch never received). **There is only one supported version of AdManageKit going forward — this one.** The `nextgen` branch is retired.

Alongside the SDK swap, Google Play Billing Library moves to **9.1.0** and `compileSdk` moves to **37**. Two native-ad rendering bugs found while validating the migration are also fixed.

All modules are bumped to **4.2.0**.

---

## Google Mobile Ads Next-Gen SDK

`com.google.android.gms:play-services-ads` is replaced by `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.2.1` everywhere: `AdManager`, `AppOpenManager`, `BannerAdView`, `RewardedAdManager`, `InterstitialAdBuilder`, the native ad views (`NativeLarge`, `NativeBannerMedium`, `NativeBannerSmall`, `NativeTemplateView`), all `AdMob*Provider` waterfall implementations, the Compose module, and the sample app. `admanagekit-core`, `admanagekit-billing`, and `admanagekit-yandex` never depended on the ad SDK directly and are unaffected.

**Why this is worth doing now, not later:** the legacy SDK is in maintenance mode; Google requires new apps and updates to be on Billing/Ads SDK versions with active support, and the Next-Gen SDK is where new AdMob features land first (e.g. the Ad Inspector and mediation improvements are Next-Gen-first now).

**What actually changes for you:**

- If you only call methods on AdManageKit's own callback types (`AdManagerCallback`, `AdLoadCallback`, `AdCallback`, `RewardedAdManager.RewardedAdCallback`, and the Compose composables' lambda parameters) — `onAdLoaded()`, `onFailedToLoad(error)`, `onPaidEvent(value)`, etc. — your code is **source-compatible**, no changes needed.
- If your code reads specific *members* of the error/value objects those callbacks hand you (e.g. the old `AdError.domain`, which doesn't exist on the Next-Gen SDK's `LoadAdError`), or if you import `com.google.android.gms.ads.*` types directly to match against these callback parameter types, you'll need to update those call sites to the Next-Gen SDK's shapes. See [Migrating to 4.2.0](../../README.md#migrating-to-420) in the README for the concrete before/after.
- If you supply a **custom XML layout** to `NativeTemplateView` (or use `NativeAdView`/`MediaView`/`AdChoicesView` directly in your own layouts), retarget those tags — see the migration guide.
- `MobileAds.initialize()` must now be called explicitly, once, before any ad request — the Next-Gen SDK throws `IllegalStateException` instead of the legacy SDK's silent lazy-init on first use. It's fine (and Google-documented as policy-compliant) to call it before UMP consent is collected; only the actual ad *request* needs to wait for consent.
- `compileSdk` must be **37+** in your app module (see below).
- Banner `pause()`/`resume()` are now no-ops — the Next-Gen SDK's `AdView` doesn't expose equivalent methods. If your app relied on banners actually pausing ad refresh while backgrounded via these calls, that behavior is gone; the SDK's own lifecycle handling takes over instead.

## Google Play Billing Library 9.1.0

Upgraded from 8.3.0. **No code changes were required** in `AppPurchase` — the wrapper was already using the v8+ patterns the v9 migration checklist calls for (`enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())`, `enableAutoServiceReconnection()`, and the `(BillingResult, QueryProductDetailsResult)` `queryProductDetailsAsync` callback signature). `BILLING_UNAVAILABLE` — v9's replacement response code for blocked-Play-Store scenarios (e.g. OEM kids mode), previously reported as a generic `ERROR` — was already handled explicitly. This upgrade is **source- and behavior-compatible** for anyone using `AppPurchase`/`PurchaseItem`/`PurchaseListener`.

## compileSdk 37

Required transitively by `androidx-core-ktx` 1.19.0+ (already picked up by this release). All five library modules and the sample app now compile against API 37. Bump your consuming app's `compileSdk` to 37 or higher.

## Fixes

- **Blank `MediaView` in native templates**: `NativeTemplateView` and the programmatic native provider both manually assigned `mediaView.mediaContent = nativeAd.mediaContent` before calling `registerNativeAd()` — a pattern carried over from the legacy SDK. The Next-Gen SDK's automatic media rendering only activates through `registerNativeAd(nativeAd, mediaView)` itself; the manual pre-assignment left the view blank instead. Removed
- **`NativeAdView.mediaView` returning `null`** for templates inflated at runtime (`NativeTemplateView`'s per-template inflation, and the programmatic provider's `LARGE` size): the getter's internal view-discovery only resolves once the view is genuinely attached to a window, which a freshly `LayoutInflater.inflate(layoutRes, null)`-created view isn't yet. Both sites now resolve the `MediaView` via `findViewById()` directly instead of the getter
- **Native Validator false-positive risk** ("asset outside native ad view") on deeply-nested templates like `CARD_MODERN` (advertiser text nested inside a `MaterialCardView`): `registerNativeAd()` was being called the instant the view was added to its placeholder, before Android's next layout pass had measured and positioned the freshly-inflated subtree — so the SDK's bounds-based validator could see stale, pre-layout coordinates for deeply-nested assets. Deferred via `doOnNextLayout {}` so registration only happens once real layout has occurred

## Compatibility

- **Source-compatible** for consumers who only interact with AdManageKit's own callback types (see above) and don't supply custom native-ad XML layouts.
- **Not source-compatible** for code that directly references `com.google.android.gms.ads.*` types to match AdManageKit's callback parameters, or that ships custom native ad layout XML using the legacy SDK's view classes. See [Migrating to 4.2.0](../../README.md#migrating-to-420).
- `compileSdk 37+` is now required.

## Installation

```gradle
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.2.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.2.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.2.0'

    // Optional
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.2.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.2.0'
}
```
