# Release Notes — v4.4.14

**Release Date:** unreleased

## Overview

v4.4.14 adds Google's **inline adaptive banner** for banners placed inside scrolling content: a card in a list, a feed, an article body. It works with `BannerAdView` and with a new `InlineBannerAdCompose` composable, and is built on the Next-Gen Mobile Ads SDK 1.6.0 that the library already uses.

Existing banner sizes (`ADAPTIVE`, `ADAPTIVE_LARGE` and the fixed sizes) behave exactly as before. Dependencies are the same as 4.4.13.

---

## Added

### `BannerAdSize.INLINE_ADAPTIVE`

- The ad's width is the banner view's own laid-out width, not the window's. A load waits until the view has been laid out, so a banner in a padded card fits the card.
- The height is variable and only known once the ad loads. It is never above the max height: 250dp by default, set with `setInlineMaxHeight(dp)` or the `bannerInlineMaxHeight` XML attribute (minimum 32dp).
- The slot reserves no height while loading. Call `setInlineShimmerEnabled(true)` for a small 50dp shimmer instead.
- Once loaded, the view wraps the ad's real height with the ad centred, and follows height changes when the ad refreshes. `onInlineAdHeightChanged` reports the height in dp.
- `BannerAdSize.isAdaptive` is true for it. XML value: `app:bannerAdSize="inline_adaptive"`.

```kotlin
bannerAdView.setInlineMaxHeight(250)
bannerAdView.onInlineAdHeightChanged = { heightDp -> }
bannerAdView.loadBanner(activity, "ca-app-pub-xxx/yyy", BannerAdSize.INLINE_ADAPTIVE)
```

### `InlineBannerAdCompose`

```kotlin
InlineBannerAdCompose(
    adUnitId = "ca-app-pub-xxx/yyy",
    modifier = Modifier.padding(horizontal = 16.dp),
    maxHeight = 250.dp,
    showShimmer = false,
    onAdLoaded = { heightDp -> },
    onAdFailedToLoad = { error -> },
)
```

- Takes the slot width from `BoxWithConstraints` (padding included) and loads only after it is measured.
- 0dp tall (or the shimmer) until the ad loads, then animates to the ad's height, capped at `maxHeight`. Takes no space on failure.
- Loads once per width. Recomposition never reloads; a real width change (rotation, foldable or tablet resize) loads a new ad. The AdView is destroyed on dispose.
- `BannerAdCompose(adSize = BannerAdSize.INLINE_ADAPTIVE)` now uses the same slot (default max height, no shimmer) instead of reserving an anchored banner's height.

### Sample app

"Test Inline Adaptive Banner" opens `InlineBannerTestActivity`: a scrolling list of cards with one inline banner from the Compose API and one from the View API.

## Behaviour notes

- **Not collapsible.** AdMob serves collapsible banners only for anchored sizes. A collapsible request with `INLINE_ADAPTIVE`, or `enableCollapsibleBannersByDefault`, logs a debug warning and loads a plain inline banner.
- **Waterfall:** the inline size is applied to the AdMob providers only. Other providers (Yandex) keep their own sizing, as with fixed sizes.
- Purchase checks, UMP consent, retries, test devices, the MobileAds init wait and debug logging work as for other banners. Premium users get no ad, no shimmer and no space, and the load fails fast with the purchase-blocked error.

## Upgrading

No changes are needed. To use inline banners, request `BannerAdSize.INLINE_ADAPTIVE` or use `InlineBannerAdCompose`. `INLINE_ADAPTIVE` was added after the existing enum values, so XML `bannerAdSize` ordinals are unchanged.
