# Release Notes — v4.3.2

**Release Date:** 2026-07-21

## Overview

v4.3.2 is a patch release focused on placeholder/shimmer sizing. The size-adaptive banner shimmer introduced in 4.3.0 now reserves the **real** adaptive-banner height from the very first frame — including the Android Studio layout preview — instead of briefly collapsing to the banner template's natural ~50dp with empty space around it. The Compose native ad view also stops clipping its call-to-action button when the ad content is taller than the nominal size.

No API changes; upgrade is a drop-in for 4.3.x.

All modules are bumped to **4.3.2**.

---

## Fixed

### Banner shimmer (`BannerAdView`)

- **The placeholder reserves the real ad height immediately.** `adjustShimmerLayout()` now runs in `init` and again whenever `setBannerAdSize()` is called, so the shimmer no longer starts at the banner template's natural ~50dp — a small shimmer surrounded by empty space — on the first runtime frame and in the design/layout preview, then jump when the taller adaptive ad arrives.
- **Adaptive banners use the SDK's resolved pixel height.** The placeholder is now measured with `AdSize.getHeightInPixels()` / `getWidthInPixels()` instead of `density * nominal-dp`. An anchored-adaptive banner's nominal dp height under-reports the real slot, which previously left the shimmer at ~50dp with empty space below while the taller ad loaded in. Adaptive placeholders also stretch to the full width so they line up edge-to-edge with the loaded ad; fixed sizes (e.g. 300×250) stay centered.
- **The adaptive size resolves without an Activity.** `getAdSize()` now accepts a plain `Context` and falls back to the window/display width before the view has been laid out, so the adaptive size computes correctly during `init` and in the preview instead of dropping to the 50dp `AdSize.BANNER` fallback that only applied when no `Activity` was available.
- **Placeholder sizing can never break rendering.** `adjustShimmerLayout()` is wrapped in a guard: an SDK call made before `MobileAds` is initialized (as happens in the layout preview) is caught and logged, and the shimmer simply keeps its XML height rather than crashing.

### Compose native ads (`NativeAdCompose`)

- **The call-to-action button is no longer clipped.** The per-size height (`SMALL` 80dp, `MEDIUM` 120dp, …) is now applied as a **minimum** via `heightIn(min = …)` rather than a fixed `height()`. Native content is `wrap_content` and can grow taller — for example a 3-line body in the `MEDIUM` layout — and a fixed height clipped the CTA off the bottom, which the native-ad validator flags as a policy violation. Short ads keep the intended size while tall ads expand so the CTA stays fully visible and tappable.

---

## Changed

- **Android Gradle Plugin** bumped to **9.3.0** (from 9.2.1).

---

## Upgrading

Drop-in from 4.3.x — no source or binary changes required.

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.3.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.3.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.3.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.3.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.3.2'
```
