# Release Notes — v4.4.5

**Release Date:** 2026-08-27

## Overview

v4.4.5 is a **patch release**. No API was added, removed or changed.

It fixes a `BannerAdView` bug that renders a **blank banner slot inside Jetpack Compose**: the ad loads, reports success, logs its impression — and nothing is drawn, because the freshly attached `AdView` is left measured at 0×0. It also picks up the Google Mobile Ads Next-Gen SDK **1.4.0**, AGP **9.3.2** and Firebase BOM **34.18.0**.

Upgrading is a drop-in from 4.4.x.

---

## Fixed

### Banner ad loaded but rendered blank under Compose interop

`BannerAdView` builds its slot as a shimmer placeholder, then swaps in the real `AdView` when the load succeeds — `layBannerAd.removeAllViews()`, `addView(adView, ...)`, hide the shimmer. Swapping the children of a `ViewGroup` raises an ordinary `requestLayout()`, and in a plain View hierarchy that is enough.

Inside Compose's `AndroidView` interop it is not. `View.requestLayout()` only walks up while each ancestor reports `isLayoutRequested == false`; the moment one already carries a pending layout flag, propagation stops there. For an interop subtree the request has to reach `AndroidViewsHandler` — the **only** place that turns a View layout request into a `LayoutNode.requestRemeasure()`. If any ancestor on the way up (the `ViewFactoryHolder` included) is already flagged, the request dies below that point and Compose never re-measures the subtree.

The consequence is that the banner keeps the measurement it had while the shimmer was mounted, and the `AdView` attached after that pass is never measured at all:

- the ad loads successfully, `onAdLoaded` and `onAdImpression` both fire
- the impression is logged and billed
- the slot renders **blank** — the container is still sized for the shimmer, the ad view is 0×0
- it stays blank until something unrelated forces a full traversal (a rotation, a keyboard, a resize)

Recomposition does not rescue it either: a semantics or `testTag` change does not invalidate measurement.

Both success paths — the AdMob path (`handleAdLoadSuccess`) and the multi-provider `BannerWaterfall` path (`onBannerLoaded`) — now call a new private `forceRelayoutAfterAdSwap()` after the swap. It walks up from the banner flagging each ancestor with `forceLayout()` (which makes `View.measure()` re-run even when the `MeasureSpec`s are unchanged — exactly what a swapped-in child needs), stopping at Compose's interop holder when there is one, and then requests the layout from **above** that holder, where `AndroidViewsHandler.requestLayout()` sees `child.isLayoutRequested == true` and schedules the remeasure. With no Compose holder in the chain, normal propagation works once the chain is flagged and a plain `requestLayout()` is issued.

Because a request raised while a layout pass is already running can still be dropped (`ViewRootImpl.requestLayoutDuringLayout`), the pass is repeated once on the next frame via `post { ... }`, which also logs the resulting container and ad-view dimensions through `AdDebugUtils` for diagnosis.

There is no behavior change for banners hosted in XML — the extra pass is a no-op when the layout is already correct.

## Changed

- **Dependency updates:** Google Mobile Ads Next-Gen SDK **1.3.1 → 1.4.0**, Android Gradle Plugin **9.3.0 → 9.3.2**, Firebase BOM **34.17.0 → 34.18.0**, org.json (test only) **20260719 → 20260814**. Play Billing stays at **9.1.0**, Kotlin at **2.2.10**, Yandex Mobile Ads at **8.3.0**, Compose BOM at **2026.08.00**

## Notes

- The fix is not test-covered: it depends on a real Compose interop measure/layout traversal, which the Robolectric harness cannot drive faithfully. It was verified on-device against a Compose-hosted banner slot. The existing 176 tests are unaffected
- No billing changes in this release

---

## Upgrading

```gradle
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.5'

// For Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.5'

// For Yandex Ads multi-provider support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.5'
```

No code changes are required.
