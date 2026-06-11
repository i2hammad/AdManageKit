# Release Notes — v3.5.9

**Release Date:** 2026-06-11

## Overview

v3.5.9 fixes **Compose adaptive banner clipping and misalignment** (#39) and brings project-health improvements on top of the 3.5.8 stability release: continuous integration, tests relocated into their owning modules with new Yandex coverage, a clean lint run, and several small correctness cleanups.

All modules are bumped to **3.5.9**. Fully source- and binary-compatible with 3.5.8.

---

## Compose adaptive banner fix (#39)

`BannerAdCompose` had two compounding problems:

1. It clamped the `AndroidView` to a fixed `50.dp` — but `BannerAdView` loads **anchored adaptive** banners, which are 50–90dp tall depending on device width (~60–70dp on typical phones). The ad rendered taller than the box and was clipped.
2. The ad was loaded before Compose measured the slot, so the requested ad size fell back to the **full window width**. With any parent padding, the ad didn't match the slot — and since adaptive widths are floored to whole dp, the start-aligned `AdView` sat visibly off-center.

Fixed:

- The composable now reserves the **real anchored-adaptive height** for the available width (`BoxWithConstraints` + `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize`)
- The load is deferred until the slot is measured, so the ad is sized to your actual layout, padding included
- `BannerAdView` centers the `AdView` horizontally in its container (both the AdMob and waterfall attach paths)
- `CollapsibleBannerAdCompose` reserves the same anchored-adaptive height — its collapsed state is an anchored adaptive banner — eliminating the layout jump on load
- The explicit-`height` `BannerAdCompose` overload is unchanged (caller-controlled)

No API changes — existing call sites just start rendering correctly.

## Project health

- **Continuous integration**: GitHub Actions workflow (`.github/workflows/ci.yml`) builds all modules and runs the full unit test suite on JDK 17 (matching `jitpack.yml`) for every push and PR to main, uploading test reports on failure
- **Tests live with their code**: the unit test suite moved from the sample app into the owning modules — waterfalls/retry/cache/config in `AdManageKit`, `AdUnitMapping` in `admanagekit-core`, `PurchaseResult` in `admanagekit-billing` — plus **15 new tests** for the Yandex module's internal mappers (SDK-8 error-code translation, revenue/currency pairing), which were unreachable from the app module. **98 tests total**
- **Lint is clean**: the long-standing `MissingTranslation` error is fixed — the `install` string is a layout-preview placeholder always overwritten at runtime with `nativeAd.callToAction`, so it is now `translatable="false"`. Plain `./gradlew build` passes again

## Small fixes and additions

- `AdKitAdError.ERROR_CODE_PURCHASE_BLOCKED` — named constant for the purchase-blocked error code (1001); `AdManager.PURCHASED_APP_ERROR_CODE` now references it (value unchanged, binary-compatible)
- `BannerAdView.refreshAd()` (manual refresh) preserves the collapsible/placement configuration, matching auto-refresh behavior
- `AdsConsentManager` parks concurrent `requestUMP` callers and notifies all of them when the in-flight consent flow completes (success, form-error, and update-error paths), instead of answering early with possibly-stale state
- `docs/V4_API_PLAN.md` — design document for the planned v4 breaking improvements (immutable config, billing split with observable entitlement, provider factories, sealed terminal-event contract, native cache consolidation), grounded in the 3.5.8 audit findings

## Installation

```gradle
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.5.9'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.5.9'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.5.9'

    // Optional
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.5.9'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.5.9'
}
```
