# Release Notes — v3.4.6

**Release Date:** 2026-05-07

## Overview

v3.4.6 adds **10 new flat-design native ad templates** to `NativeTemplateView`. The new templates use a clean, minimal aesthetic — no gradients, no heavy shadows, subtle separators — designed to blend into modern app UIs without disrupting the user experience. All variants are theme-driven (Material 3 / dark mode aware).

This brings the total `NativeTemplateView` template count from **27 to 37**.

---

## What's New

### 10 Flat-Design Native Ad Templates

Each template ships with its own layout and shimmer placeholder, and is available via:

- `app:adTemplate="<name>"` in XML
- `NativeAdTemplate.<NAME>` programmatically
- A dedicated `Native<Name>Compose` composable in `admanagekit-compose`

| # | XML attr | Enum | Compose | Best for |
|---|----------|------|---------|----------|
| 1 | `flat_inline_row` | `FLAT_INLINE_ROW` | `NativeFlatInlineRowCompose` | List/feed rows (top + bottom hairlines) |
| 2 | `flat_card_rating` | `FLAT_CARD_RATING` | `NativeFlatCardRatingCompose` | Card with rating + Get pill |
| 3 | `flat_media_top` | `FLAT_MEDIA_TOP` | `NativeFlatMediaTopCompose` | Vertical card with media on top |
| 4 | `flat_text_minimal` | `FLAT_TEXT_MINIMAL` | `NativeFlatTextMinimalCompose` | Text-only ad with left brand bar |
| 5 | `flat_compact_pill` | `FLAT_COMPACT_PILL` | `NativeFlatCompactPillCompose` | Small icon + headline + CTA |
| 6 | `flat_carousel` | `FLAT_CAROUSEL` | `NativeFlatCarouselCompose` | Header + media + footer rating row |
| 7 | `flat_banner` | `FLAT_BANNER` | `NativeFlatBannerCompose` | Single-line wide banner |
| 8 | `flat_feature_list` | `FLAT_FEATURE_LIST` | `NativeFlatFeatureListCompose` | Title + 3 bullet benefits + Install |
| 9 | `flat_sponsored_story` | `FLAT_SPONSORED_STORY` | `NativeFlatSponsoredStoryCompose` | Editorial-style headline + body |
| 10 | `flat_footer_slim` | `FLAT_FOOTER_SLIM` | `NativeFlatFooterSlimCompose` | Sticky-style slim footer |

#### Design rules

- **Flat design only** — no gradients, no heavy shadows
- **Subtle separators** — 1dp hairlines using `?attr/colorOutlineVariant`
- **Theme-driven** — all colors via `?attr/colorSurface`, `?attr/colorPrimary`, `?attr/colorOnSurface`, `?attr/colorOnSurfaceVariant`, so dark mode and Material 3 theming work out of the box
- **Consistent ad disclosure** — every variant shows an `AD` or `Sponsored` label
- **Pill CTAs** — 999dp-rounded buttons (filled or outlined)

#### XML usage

```xml
<com.i2hammad.admanagekit.admob.NativeTemplateView
    android:id="@+id/native_ad"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adTemplate="flat_card_rating" />
```

#### Programmatic usage

```kotlin
nativeTemplateView.setTemplate(NativeAdTemplate.FLAT_MEDIA_TOP)
nativeTemplateView.loadNativeAd(activity, adUnitId, callback)
```

#### Compose usage

```kotlin
NativeFlatCardRatingCompose(adUnitId = "ca-app-pub-…")

// or generic:
NativeTemplateCompose(
    adUnitId = "ca-app-pub-…",
    template = NativeAdTemplate.FLAT_SPONSORED_STORY
)
```

#### New helpers

- `NativeAdTemplate.flatTemplates()` — returns all `FLAT_*` entries

---

## API additions

### `NativeAdTemplate` (admanagekit-admob)

10 new entries: `FLAT_INLINE_ROW`, `FLAT_CARD_RATING`, `FLAT_MEDIA_TOP`, `FLAT_TEXT_MINIMAL`, `FLAT_COMPACT_PILL`, `FLAT_CAROUSEL`, `FLAT_BANNER`, `FLAT_FEATURE_LIST`, `FLAT_SPONSORED_STORY`, `FLAT_FOOTER_SLIM`.

New companion helper: `flatTemplates(): List<NativeAdTemplate>`.

### `attrs.xml` — `app:adTemplate`

10 new enum values (28–37) corresponding to the templates above.

### `NativeTemplateView`

Internal: `getScreenTypeForTemplate()` extended to bucket the new templates by size (SMALL: inline row / compact pill / banner / footer slim · MEDIUM: card rating / text minimal / feature list · LARGE: media top / carousel / sponsored story).

### `admanagekit-compose`

10 new composables: `NativeFlatInlineRowCompose`, `NativeFlatCardRatingCompose`, `NativeFlatMediaTopCompose`, `NativeFlatTextMinimalCompose`, `NativeFlatCompactPillCompose`, `NativeFlatCarouselCompose`, `NativeFlatBannerCompose`, `NativeFlatFeatureListCompose`, `NativeFlatSponsoredStoryCompose`, `NativeFlatFooterSlimCompose`.

### Resources

- 10 new layouts: `layout_native_flat_*.xml`
- 10 new shimmer placeholders: `layout_shimmer_flat_*.xml`
- 8 new supporting drawables: `ads_flat_pill`, `ads_flat_pill_outline`, `ads_flat_card`, `ads_flat_card_soft`, `ads_flat_label_outline`, `ads_flat_label_filled`, `ads_flat_app_icon_bg`, `ads_flat_media_bg`

---

## Compatibility

This release is fully backward compatible. Existing templates, APIs, and integration code are unchanged.

---

## Upgrade Guide

Update your dependency versions:

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.4.6'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.4.6'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.4.6'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.4.6'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.4.6'
```

---

## Previous Release

[v3.4.5 Release Notes](RELEASE_NOTES_v3.4.5.md)
