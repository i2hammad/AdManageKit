# Release Notes — v4.4.15

**Release Date:** 2026-11-25

## Overview

v4.4.15 fixes blank screens caused by inline adaptive banners in 4.4.14. No API or dependency changes. If you use `BannerAdSize.INLINE_ADAPTIVE` or `InlineBannerAdCompose`, update.

---

## Fixed

### A banner refresh could stop the whole screen from laying out

**Symptom:** after an inline banner had been on screen for a minute or so, tabs, fragments and other views opened afterwards came up empty (laid out at 0x0). Forcing a full relayout, such as rotating the device, brought them back.

**Cause:** after a load, and in 4.4.14 after every inline refresh, `BannerAdView` called `forceLayout()` on each parent view and then `requestLayout()` on itself. `requestLayout()` stops climbing at the first parent that already reports `isLayoutRequested`, so the request never reached the window's `ViewRootImpl`. With no layout already pending, the parents stayed marked, and every later `requestLayout()` in that window stopped at them.

**Fix:** the request is now raised from the top of the parent chain (for a View host, the window's root view), so it always reaches the window. The chain is walked again on the next frame, in case the banner moved to another parent in between. Compose hosts are unchanged.

Anchored banners have run the same code after a load since 4.4.5. A layout pass was normally already pending at that point, so it rarely showed, but it is fixed for them too.

---

## Tests

- `BannerAdViewRelayoutTest`: after the relayout no parent is left marked, and a view added afterwards is laid out; a detached banner leaves the window untouched.

## Upgrade

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.15'
```
