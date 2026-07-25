# Release Notes — v4.3.3

**Release Date:** 2026-07-23

## Overview

v4.3.3 is a patch release that improves native ad media quality. Every native ad request now carries two preferences derived from AdManageKit config or the template being shown:

- a **media-aspect-ratio hint** matched to the shape of the template's `MediaView` slot, so served image/video media fits the slot with less cropping; and
- global **`VideoOptions`** (start-muted by default, plus optional click-to-expand and custom-controls requests) that control playback when a video creative is served.

Both are *preferences passed to the ad network, not filters* — the network still decides whether an ad carries an image or a video and what shape it is. Templates with no `MediaView` (icon + text + CTA) send no hint and degrade gracefully.

No breaking API changes. All defaults preserve prior request behavior, so the upgrade is a drop-in for 4.3.x.

All modules are bumped to **4.3.3**.

---

## Added

### Media-aspect hint

- **`NativeMediaAspect` enum** (`com.i2hammad.admanagekit.config`) — `UNSPECIFIED`, `ANY`, `LANDSCAPE`, `PORTRAIT`, `SQUARE`. Maps to the Next-Gen SDK's `NativeAd.NativeMediaAspectRatio`. `UNSPECIFIED` sends no hint (used for media-less templates).
- **`AdManageKitConfig.defaultNativeMediaAspect`** *(default `ANY`)* — the hint applied to requests that don't specify their own: the programmatic loader, `AdMobNativeProvider`, the multi-provider waterfall, and custom templates.
- **`NativeTemplateView.setMediaAspect(NativeMediaAspect?)` / `getMediaAspect()`** — per-view override. Call before loading. Pass `null` to revert to the per-template default, or `UNSPECIFIED` to send no hint at all.

### Native video options

- **`AdManageKitConfig.nativeVideoStartMuted`** *(default `true`)* — muted autoplay, the policy-friendly default.
- **`AdManageKitConfig.nativeVideoClickToExpand`** *(default `false`)* — request click-to-expand-to-fullscreen; honoured only when the creative supports it.
- **`AdManageKitConfig.nativeVideoCustomControls`** *(default `false`)* — request custom video controls (only enable if you actually render your own play/pause UI).

All four new fields are reset by `AdManageKitConfig.resetToDefaults()`.

---

## Changed

- **Native requests now carry media/video preferences.** `NativeTemplateView` derives the aspect hint from the current template's `MediaView` slot shape:
  - **Portrait** — vertical/story/full-screen templates (`VIDEO_VERTICAL`, `VIDEO_FULLSCREEN`, `STORY_STYLE`, `OVERLAY_DARK`)
  - **Square** — grid and square-video templates (`VIDEO_SQUARE`, `GRID_CARD`, `GRID_ITEM`)
  - **Landscape** — wide media templates (`VIDEO_LARGE/MEDIUM/SMALL`, `FEATURED`, `MAGAZINE`, `SOCIAL_FEED`, `CARD_MODERN`, and others)
  - **Any** — small-media templates (`PILL_BANNER`)
  - **No hint** — media-less banner templates (`FULL_WIDTH_BANNER`, `FLAT_BANNER`, and the other `FLAT_*` presets)
- **`NativeLarge`** requests `LANDSCAPE`.
- **The programmatic loader, `AdMobNativeProvider`, and the waterfall** apply `defaultNativeMediaAspect`.
- A shared internal `NativeAdRequest.Builder.applyMediaConfig(...)` extension centralizes applying the hint and `VideoOptions` across all request sites.

---

## Upgrading

Drop-in from 4.3.x — no source or binary changes required. Defaults (`ANY` aspect, start-muted video) match the prior request behavior.

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.3.3'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.3.3'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.3.3'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.3.3'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.3.3'
```

To tune the behavior:

```kotlin
// Global defaults (applied to programmatic / waterfall / custom-template requests)
AdManageKitConfig.defaultNativeMediaAspect = NativeMediaAspect.LANDSCAPE
AdManageKitConfig.nativeVideoStartMuted = true

// Per-view override
nativeTemplateView.setMediaAspect(NativeMediaAspect.SQUARE)
```
