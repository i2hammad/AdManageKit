# Release Notes — v4.3.0

**Release Date:** 2026-07-14

## Overview

v4.3.0 is a feature release on top of the Next-Gen SDK migration (v4.2.0), focused on banner and native customization:

- **All standard AdMob banner sizes** — `BannerAdView` and `BannerAdCompose` now support the full size table (320x50, 320x100, 300x250, 468x60, 728x90) alongside the default adaptive banner, selectable programmatically, from XML, or in Compose.
- **Custom native ad templates** — `NativeTemplateView` and `NativeTemplateCompose` can now render a fully custom layout you supply, instead of one of the 37 built-in presets.
- **Redesigned banner shimmer** — the loading placeholder adapts to the requested banner size, matches the loaded ad's position, and supports **night mode**.
- **App open ads: late MobileAds initialization support** — `AppOpenManager` now guards every load path against the Next-Gen SDK's "not initialized" rejection, with a new `isMobileAdsReady()` confirmation API.

All modules are bumped to **4.3.0**. This release is source-compatible; see [Compatibility](#compatibility) for one binary-level note.

---

## Banner Ad Sizes

`BannerAdView` previously always requested a full-width anchored adaptive banner. A new `BannerAdSize` enum (`com.i2hammad.admanagekit.config.BannerAdSize`) exposes every standard AdMob size:

| Value              | Size (dp)  | Description          | Availability       |
|--------------------|------------|----------------------|--------------------|
| `ADAPTIVE`         | full width | Anchored adaptive    | Phones and tablets |
| `BANNER`           | 320x50     | Banner               | Phones and tablets |
| `LARGE_BANNER`     | 320x100    | Large banner         | Phones and tablets |
| `MEDIUM_RECTANGLE` | 300x250    | IAB medium rectangle | Phones and tablets |
| `FULL_BANNER`      | 468x60     | IAB full-size banner | Tablets            |
| `LEADERBOARD`      | 728x90     | IAB leaderboard      | Tablets            |

**Programmatic:**

```kotlin
bannerAdView.loadBanner(this, "ca-app-pub-xxx/yyy", BannerAdSize.MEDIUM_RECTANGLE)

// Or set a default for subsequent loads
bannerAdView.setBannerAdSize(BannerAdSize.LARGE_BANNER)
bannerAdView.loadBanner(this, "ca-app-pub-xxx/yyy")
```

**XML:**

```xml
<com.i2hammad.admanagekit.admob.BannerAdView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:bannerAdSize="medium_rectangle" />
```

**Compose:**

```kotlin
BannerAdCompose(
    adUnitId = "ca-app-pub-xxx/yyy",
    adSize = BannerAdSize.MEDIUM_RECTANGLE
)
```

Behavior notes:

- `ADAPTIVE` (the default everywhere) keeps the exact pre-4.3.0 behavior — nothing changes if you don't opt in.
- The size is preserved across retries, auto-refresh, and manual `refreshAd()` calls.
- In **multi-provider waterfalls**, a fixed size is applied to the AdMob providers in the chain (`AdMobBannerProvider.adSize` is now a settable `var`); non-AdMob providers keep their own sizing.
- **Collapsible banners require `ADAPTIVE`** (AdMob only serves collapsible on anchored adaptive requests); passing a fixed size with `collapsible = true` logs a debug warning.
- `FULL_BANNER` and `LEADERBOARD` are wider than phone screens — tablet layouts only.
- In Compose, fixed sizes reserve their exact height up front (no layout jump) and center horizontally.

## Custom Native Ad Templates

If none of the 37 built-in `NativeAdTemplate` presets fit your design, `NativeTemplateView` can now render a layout you supply. The layout's root must be (or inflate as) a Next-Gen SDK `NativeAdView` reusing the standard asset ids (`ad_headline`, `ad_body`, `ad_call_to_action`, `ad_app_icon`, `ad_advertiser`, `ad_media`, `ad_stars`, `ad_choices_view`).

**Programmatic:**

```kotlin
nativeTemplateView.setCustomTemplate(
    layoutResId = R.layout.my_native_layout,
    shimmerResId = R.layout.my_native_shimmer, // optional, falls back to the template's shimmer
    sizeHint = NativeAdSize.MEDIUM             // cache classification / waterfall fallback size
)
nativeTemplateView.loadNativeAd(activity, adUnitId)

// Revert to built-in templates
nativeTemplateView.clearCustomTemplate()
```

**XML:**

```xml
<com.i2hammad.admanagekit.admob.NativeTemplateView
    app:customAdLayout="@layout/my_native_layout"
    app:customAdShimmerLayout="@layout/my_native_shimmer" />
```

**Compose:** `NativeTemplateCompose` gains `customLayoutResId`, `customShimmerResId`, and `customSizeHint` parameters.

See the [NativeTemplateView guide](../NATIVE_TEMPLATE_VIEW.md#custom-templates) for the full asset-id table and requirements.

## App Open Ads: Late MobileAds Initialization Support

The Next-Gen SDK rejects ad requests made before `MobileAds.initialize()` completes, and `AppOpenManager`'s `ProcessLifecycleOwner` observer fires as soon as the first activity starts — so a manager constructed in `Application.onCreate()` could race ahead of a background-thread (or consent-deferred) initialization and crash. As of 4.3.0 the manager tolerates being constructed before initialization:

- The automatic on-foreground show (`showAdIfAvailable()`) skips the show when the SDK isn't ready and defers a single background prefetch until it is — no surprise full-screen ad seconds later, but the cache is warm for the next opportunity.
- `fetchAd()` parks the prefetch and replays it once initialization completes (bounded by `AdManageKitConfig.appOpenAdTimeout`).
- `fetchAd(callback)` and `forceShowAdIfAvailable(activity, callback)` wait for the SDK **within their existing timeout budget**, then proceed with the remaining time. If initialization never completes, the callback still receives a terminal failure / `onNextAction()` — splash screens gating navigation on the callback are never stranded.
- New **`isMobileAdsReady()`** returns whether the SDK is initialized and ads can be requested (distinct from `isAdAvailable()`, which reports a loaded ad). Waterfall chains without an AdMob provider report ready regardless of MobileAds state.

The recommended construction order is still to create `AppOpenManager` after `MobileAds.initialize()` returns — the sample app's `MyApplication` and the [App Open Ads guide](../app-open-ads.md#initialization-order--late-initialization) show the pattern; the guard exists so apps that can't guarantee that order (or defer initialization until after consent) degrade gracefully instead of crashing.

## Redesigned Banner Shimmer (+ Night Mode)

The banner loading placeholder was rebuilt:

- **Size-adaptive**: one layout serves every `BannerAdSize` — a weighted media block absorbs the extra height on tall formats (a 300x250 request shows a large media placeholder; a 50dp banner shows just the icon + text + CTA row).
- **Exact reservation on every path**: the shimmer is sized to the requested ad size before the request goes out — including the **waterfall path, which previously always showed the ~56dp default row** — so the layout never jumps when the ad arrives.
- **Centered**: the placeholder is horizontally centered, matching where the loaded ad renders (previously it sat at the start edge while the ad centered).
- **Modern look**: rounded card surface, rounded placeholder bars, pill-shaped CTA stub — consistent with the newer native shimmer templates.
- **Night mode**: the shimmer previously rendered a hardcoded white card in dark theme. It now uses the library's day/night colors (`dn_card_background`, new `dn_shimmer_placeholder`) and darkens automatically with the system theme.

## Compatibility

- **Source-compatible** — all new parameters have defaults; `ADAPTIVE` preserves existing behavior.
- **Binary note**: `BannerAdCompose` and `NativeTemplateCompose` gained parameters (Kotlin default-argument functions), and `AdMobBannerProvider.adSize` changed from `val` to `var`. Apps compiled against 4.2.0 should recompile against 4.3.0 rather than hot-swapping the AAR.
- No SDK, billing, or `compileSdk` changes — the v4.2.0 requirements (Next-Gen SDK, `compileSdk 37+`) are unchanged.

## Installation

```gradle
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.3.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.3.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.3.0'

    // Optional
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.3.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.3.0'
}
```
