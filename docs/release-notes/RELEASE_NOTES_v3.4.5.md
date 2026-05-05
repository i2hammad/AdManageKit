# Release Notes — v3.4.5

**Release Date:** 2026-05-05

## Overview

v3.4.5 upgrades the `admanagekit-yandex` module to **Yandex Mobile Ads SDK 8.0.0** and fixes the large native ad layout that was rendering incorrectly after the SDK 8 migration.

---

## Changes

### Yandex Mobile Ads SDK 8.0.0 Migration

The `admanagekit-yandex` module has been fully migrated from SDK 7.18.1 to 8.0.0. All breaking API changes are handled internally — no changes are required to your integration code.

#### API Changes (internal)

| Area | SDK 7.x | SDK 8.0 |
|------|---------|---------|
| Initialization | `MobileAds.initialize(ctx, cb)` | `YandexAds.initialize(ctx, cb)` |
| Ad request | `AdRequestConfiguration.Builder(id).build()` | `AdRequest.Builder(id).build()` |
| Native ad request | `NativeAdRequestConfiguration.Builder(id).build()` | `AdRequest.Builder(id).build()` |
| Load listener | `loader.setAdLoadListener(l)` + `loader.loadAd(req)` | `loader.loadAd(req, l)` |
| Native load listener | `loader.setNativeAdLoadListener(l)` + `loader.loadAd(req)` | `loader.loadAd(req, l)` |
| Banner size | `BannerAdSize.stickySize(ctx, w)` | `BannerAdSize.sticky(ctx, w)` |
| Banner ad unit | `bannerView.setAdUnitId(id)` | Removed — pass `id` to `AdRequest.Builder(id)` |
| Native binding | Returns `Unit`, throws `NativeAdException` on failure | Returns `AdBindingResult` (`Success` or `Failure`) |
| Removed listener methods | `onLeftApplication()`, `onReturnedToApplication()` | Removed from all event listeners |
| Removed template | `NativeBannerView.setAd(nativeAd)` | Replaced by `NativeAdView` + `NativeAdViewBinder` |

---

### Fixed: Yandex Large Native Ad Layout

The large native ad was rendering with only the media image visible — the title, body, CTA button, and other assets were pushed off-screen.

**Root causes and fixes:**

| Issue | Fix |
|-------|-----|
| `MediaView` was the first child in the container and expanded to fill all visible space | Moved `MediaView` below the CTA so all text content is visible first |
| CTA button was below the media image and scrolled out of view | CTA now renders **above** the `MediaView` |
| `price` asset missing — required for app-type native ads | Added `priceView` (TextView) bound via `setPriceView()` |
| `favicon` asset not bound | Added `faviconView` (ImageView) bound via `setFaviconView()` |
| `MediaView` had no height limit and could overflow | Capped at 200dp height |
| `MediaView` started visible, causing blank space when no media | Now starts as `GONE`; SDK controls visibility |
| `AdBindingResult.Failure` was silently ignored | Now throws `IllegalStateException` → caught by outer handler → `onNativeAdFailedToLoad` |

**Final layout order for LARGE native ads:**
```
icon + title + domain/favicon + price + feedback (menu)
body text
CTA button
MediaView (200dp, GONE → SDK shows when ready)
warning + sponsored
```

---

## Upgrade Guide

### No changes required

If you are already using `admanagekit-yandex`, update your dependency version and rebuild:

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.4.5'
```

All other modules also bump to `v3.4.5`:

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.4.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.4.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.4.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.4.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.4.5'
```

---

## Previous Release

[v3.4.4 Release Notes](RELEASE_NOTES_v3.4.4.md)
