# Release Notes — v4.4.16

**Release Date:** 2026-11-25

## Overview

v4.4.16 lets Yandex serve inline adaptive banners at their inline size, and stops a shared Yandex banner provider from blanking another view's banner. No dependency changes.

---

## Added

### Yandex inline adaptive banners

In 4.4.14 and 4.4.15, `BannerAdSize.INLINE_ADAPTIVE` only reached AdMob providers. When Yandex served the slot, alone or as AdMob's backfill, it loaded a sticky banner at screen width: it ignored the max height and could be wider than an inset slot.

Every provider in the chain is now asked for the inline size. `YandexBannerProvider` loads `BannerAdSize.inline(context, widthDp, maxHeightDp)`, with the width of the `BannerAdView`'s laid-out slot and its `setInlineMaxHeight()` value. The rendered height is reported through `onInlineAdHeightChanged` as for AdMob.

New API, for custom providers:

```kotlin
fun loadInlineBanner(context: Context, adUnitId: String, size: InlineBannerSize, callback: BannerAdCallback)
data class InlineBannerSize(val widthDp: Int, val maxHeightDp: Int)
```

The default implementation does a plain `loadBanner`, so existing providers compile and behave as before. The size is passed with each call because providers in the global chain are shared by every banner on screen.

Map your inline banner units to Yandex, or the waterfall still skips it:

```kotlin
AdUnitMapping.register(inlineUnitId, mapOf("yandex" to yandexBannerUnitId))
```

---

## Fixed

### A second Yandex banner blanked the first

`YandexBannerProvider` destroyed its previous banner at the start of every load. The provider instance in `AdProviderConfig`'s banner chain is shared, so with two Yandex banners on screen each new load destroyed the other's ad. It now destroys only views that are still loading, the way `AdMobBannerProvider` already did; a banner it has handed out belongs to the view that shows it.

---

## Tests

- `BannerWaterfallTest`: the inline size reaches every provider, including a fallback after a failure; without one, providers get a plain load.

## Upgrade

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.16'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.16'
```
