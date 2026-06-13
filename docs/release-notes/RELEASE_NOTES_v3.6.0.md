# Release Notes — v3.6.0

**Release Date:** 2026-06-13

## Overview

v3.6.0 makes the native-ad **waterfall** a first-class path. Two things change for multi-provider setups:

1. **Yandex native ads now look identical to AdMob.** When `NativeTemplateView` falls back to Yandex, the same AdMob template is rendered — not a generic placeholder.
2. **The programmatic native loader respects the provider chain.** `loadNativeAdProgrammatically` and friends now fall back to other providers on AdMob no-fill, instead of just failing.

It also adds cancellable loads and fixes several correctness issues found while reviewing the new code.

All modules are bumped to **3.6.0**. **Source-compatible** with 3.5.9; one **binary-incompatible** signature change is noted below.

---

## Yandex native ads match AdMob templates

Previously, a `NativeTemplateView` using one of the 37 named AdMob templates (CARD_MODERN, MATERIAL3, the video and flat-design families, …) would fall back to a generic SMALL/MEDIUM/LARGE Yandex view that looked nothing like the chosen template.

Now `YandexNativeProvider` renders the **exact same template**:

- The selected template's layout is inflated as-is, and Yandex assets are bound to the standard template asset ids — `ad_headline`, `ad_body`, `ad_call_to_action`, `ad_app_icon`, `ad_advertiser`, `ad_media`
- The template's Google `MediaView` (`ad_media`) is swapped in-place for a Yandex `MediaView`, since the two SDKs use incompatible media-view types
- A compact, mandatory Yandex compliance row (feedback + sponsored + warning) is appended — the Yandex SDK requires these views to bind and render legally; AdMob templates don't include them
- If a template can't be bound, it gracefully falls back to the built-in size-based Yandex view

Plumbing: `NativeAdProvider.loadNativeAd` gains an optional `templateLayoutResId: Int = 0`. The template id flows `NativeTemplateView` → `NativeWaterfall` → provider. It's an `Int` (not a typed reference) so the zero-dependency `admanagekit-core` module stays dependency-free. AdMob ignores it (it re-binds the raw `NativeAd` into the template itself); a value of `0` means "use the provider's built-in size-based layout."

## Programmatic native loader respects the waterfall

`ProgrammaticNativeAdLoader.loadNativeAd` — and the `NativeAdManager` conveniences (`loadNativeAdProgrammatically`, `loadSmallNativeAd`, `loadMediumNativeAd`, `loadLargeNativeAd`, `loadNativeAdIntoContainer`) — now route through the configured chain:

- When a native chain is set via `AdProviderConfig.setNativeChain`, AdMob no-fill falls back to the next provider (e.g. Yandex). With **no** chain configured, the loader stays on the original pure-AdMob path — zero behaviour change
- New `ProgrammaticAdCallback.onProviderAdLoaded(adView, nativeAdRef)` default-no-op hook delivers non-AdMob views (AdMob fills still arrive through the typed `onAdLoaded`). `loadNativeAdIntoContainer` and `ProgrammaticNativeAdCompose` attach these automatically
- `NativeAdProvider.NativeAdCallback` gains `onNativeAdOpened()` / `onNativeAdClosed()`, forwarded by `AdMobNativeProvider` and `NativeWaterfall`, so open/close events fire consistently whether or not a chain is configured

The programmatic API works in **sizes** (SMALL/MEDIUM/LARGE), not named templates, so Yandex renders its size-based view here — there is no named template in this API to map.

## Cancellable loads

The programmatic native load methods now return a `ProgrammaticNativeAdLoader.NativeAdLoadHandle`:

```kotlin
val handle = NativeAdManager.loadLargeNativeAd(activity, adUnitId, callback = myCallback)
// later, e.g. in onDestroy():
handle.cancel()
```

After `cancel()` no further `ProgrammaticAdCallback` events are delivered, and any fill that arrives late is **destroyed** instead of being pushed into a dead view hierarchy. `cancel()` is idempotent and thread-safe. `ProgrammaticNativeAdCompose` calls it automatically on dispose and before reloading on a key change.

## Fixes

- **Container leak**: `loadNativeAdIntoContainer` now destroys the previously displayed `NativeAd` (tracked via a container view tag) before replacing the container's content, instead of leaking it and its media on every reload
- **Cache vs. chain order**: an AdMob cached ad no longer short-circuits a configured waterfall when AdMob is **not** the first provider (e.g. a Yandex-first chain) — the configured order is honoured
- **Silent drop**: a non-AdMob fill delivered through the raw `loadNativeAd` callback no longer disappears silently — the loader logs a warning when the callback doesn't override `onProviderAdLoaded` (pointing you to it or to `loadNativeAdIntoContainer`)

## Compatibility

- **Source-compatible** with 3.5.9 — existing call sites compile unchanged.
- **One binary-incompatible change**: the programmatic native load methods changed return type from `Unit` to `NativeAdLoadHandle`. JitPack builds from source, so consumers are unaffected on rebuild; anyone linking a prebuilt artifact must recompile against 3.6.0.

## Installation

```gradle
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.6.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.6.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.6.0'

    // Optional
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.6.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.6.0'
}
```
