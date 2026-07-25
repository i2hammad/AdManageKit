# Release Notes — v4.3.4

**Release Date:** 2026-07-25

## Overview

v4.3.4 is a patch release that restores the **pre-4.2.0 default adaptive banner height**.

The 4.2.0 Next-Gen SDK migration silently switched every adaptive banner path from the standard anchored adaptive size to the Next-Gen SDK's *large anchored adaptive* format (`AdSize.getLargeAnchoredAdaptiveBannerAdSize`), making the default banner noticeably taller than on 3.x/4.1. As of 4.3.4:

- **`BannerAdSize.ADAPTIVE` (the default)** again requests the standard anchored adaptive size (`AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize`, ~50-90dp depending on device) — matching v3.6.0 behavior.
- **`BannerAdSize.ADAPTIVE_LARGE` (new)** opts into the taller large anchored adaptive format for placements where the extra height (and viewability) is wanted.

The fix applies consistently across `BannerAdView`, the multi-provider waterfall (`AdMobBannerProvider`), and `BannerAdCompose` — including the shimmer placeholder and the Compose height reservation, which size themselves to whichever variant is requested.

Also repairs the 4.3.3 source tree: `NativeMediaAspect.kt` was referenced by the 4.3.3 release commit but never committed, which broke CI and the JitPack `4.3.3` build. The file is now in the tree, and the `v4.3.3` tag was re-pointed to a buildable commit.

No breaking API changes. All modules are bumped to **4.3.4**.

---

## Fixed

- **Default adaptive banner height restored to standard (~50-90dp).** Since 4.2.0, `BannerAdView.getAdSize()`, `AdMobBannerProvider`'s adaptive fallback, and `BannerAdCompose`'s height reservation all used the large anchored adaptive format. `ADAPTIVE` now maps back to the standard anchored adaptive size everywhere; apps upgrading from 4.2.0-4.3.3 get the pre-migration banner footprint back with no code changes.
- **`NativeMediaAspect.kt` restored to the tree** — missing from the 4.3.3 release commit (compile error on CI/JitPack).

## Added

- **`BannerAdSize.ADAPTIVE_LARGE`** — full-width large anchored adaptive banner, taller than `ADAPTIVE`. Available everywhere a `BannerAdSize` is accepted:

  ```kotlin
  // Programmatic
  bannerAdView.loadBanner(activity, "ca-app-pub-xxx/yyy", BannerAdSize.ADAPTIVE_LARGE)
  bannerAdView.setBannerAdSize(BannerAdSize.ADAPTIVE_LARGE)

  // Compose
  BannerAdCompose(adUnitId = "ca-app-pub-xxx/yyy", adSize = BannerAdSize.ADAPTIVE_LARGE)
  ```

  ```xml
  <!-- XML -->
  <com.i2hammad.admanagekit.admob.BannerAdView
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      app:bannerAdSize="adaptive_large" />
  ```

  The size carries through retries, auto-refresh, and the multi-provider waterfall. Collapsible banners accept it (any anchored adaptive size qualifies for AdMob's collapsible format).

## Changed

- The `bannerAdSize` XML attr gained the `adaptive_large` value, and the attr's internal enum values were renumbered to follow the new `BannerAdSize` ordinal order (`ADAPTIVE`, `ADAPTIVE_LARGE`, `BANNER`, ...). XML uses named values only, so existing layouts are unaffected after a recompile — but any code persisting raw `BannerAdSize.ordinal` values should store names instead.

---

## Upgrading

Drop-in from 4.3.x. If your banners looked taller after moving to 4.2.0+, this release restores the old height automatically; if you prefer the taller format, request `BannerAdSize.ADAPTIVE_LARGE` explicitly.

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.3.4'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.3.4'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.3.4'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.3.4'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.3.4'
```
