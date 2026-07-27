# Release Notes — v4.3.5

**Release Date:** 2026-07-27

## Overview

v4.3.5 is a patch release that makes **`appOpenAdFreshnessThreshold` actually enforced on every app open show path**.

The threshold (default 4 hours, matching Google's guidance for app open ads) was only consulted by the `ON_DEMAND` strategy. `HYBRID`, `FRESH_WITH_CACHE_FALLBACK`, `ONLY_CACHE`, and the multi-provider waterfall path all checked `isAdAvailable()` — "is an ad object loaded?" — and would happily show an ad that had been sitting in the cache for a day or more. A stale app open ad is a wasted impression (expired creatives fill poorly and monetize badly), and Google explicitly recommends not caching app open ads for more than 4 hours.

As of 4.3.5, a cached ad older than the threshold is **discarded and replaced** instead of shown, on every strategy and on both the direct AdMob and waterfall paths. The background prefetch in `onStop` now also refreshes a cached-but-stale ad rather than seeing "an ad exists" and skipping.

It also fixes a long-standing split-brain default for **`appOpenAdTimeout`**: the property initialized to `4.seconds` while `resetToDefaults()` set `10.seconds`, so the effective timeout depended on whether the app had ever called `resetToDefaults()` (and two config unit tests failed). Four seconds is too short for the fetch-with-dialog paths — a fresh app open ad routinely needs longer on a slow connection, and the dialog would give up before the ad arrived. Both now agree on **10 seconds**.

No API changes and no new configuration. If your cached ads were always fresh, nothing changes for you; if they weren't, you'll see fresh creatives (and, on `ON_DEMAND`/`HYBRID`, the welcome-back dialog) where a stale ad used to appear.

All modules are bumped to **4.3.5**.

---

## Fixed

- **`appOpenAdFreshnessThreshold` is now honored by all loading strategies.** `HYBRID` / `FRESH_WITH_CACHE_FALLBACK` and `ONLY_CACHE` gated on `isAdAvailable()` and would show an arbitrarily old cached ad. Both now gate on freshness: a stale ad is dropped and a fresh one fetched (with the welcome dialog on `HYBRID`, silently in the background on `ONLY_CACHE`).
- **Waterfall app open ads track their load time.** `adLoadTime` was only stamped on the direct AdMob load path, so `isCachedAdFresh()` could never return `true` for a waterfall-loaded ad. All three waterfall load paths (`fetchViaWaterfall`, the splash fetch, and the dialog fetch) now record the load time, and `isCachedAdFresh()` recognizes a ready waterfall ad via `isAdAvailable()`.
- **The waterfall show path checks freshness.** `showAdIfAvailable()` used `appOpenWaterfall?.isAdReady()` alone; it now requires the cached ad to be fresh and destroys a stale chain before re-fetching.
- **`appOpenAdTimeout` default is consistent at 10 seconds.** The property initialized to `4.seconds` but `resetToDefaults()` set `10.seconds`, so the live value depended on whether `resetToDefaults()` had ever run — and the two config tests asserting `4.seconds` failed against it. Both paths (and the docs/wiki tables) now say 10 seconds, which is what the sample app already configured.
- **Background prefetch refreshes stale ads.** The `onStop` prefetch (active when `appOpenFetchFreshAd = false`) skipped loading whenever *any* ad was cached, so a stale ad was never replaced while the app sat in the background. It now prefetches when the cached ad is stale, discarding the old ad first so the fetch isn't short-circuited by the "already have an ad" guard.

## Changed

- **KDoc for `appOpenFetchFreshAd` and `appOpenAdFreshnessThreshold` corrected.** `appOpenFetchFreshAd` controls fetch *timing* only (background prefetch vs. foreground fetch) — it has never governed whether a cached ad is shown, which is `appOpenLoadingStrategy` + `appOpenAdFreshnessThreshold`. `appOpenAdFreshnessThreshold` is documented as applying to every strategy, not just `ON_DEMAND`.

---

## Behavior notes

- `Duration.ZERO` for `appOpenAdFreshnessThreshold` still means "never use a cached ad" — with it set, every foreground shows a freshly fetched ad (or none).
- Raising the threshold restores the old effective behavior for a given window, e.g. `appOpenAdFreshnessThreshold = 12.hours`, but Google recommends staying at or below 4 hours.
- Apps on `ONLY_CACHE` may see slightly fewer app open impressions where a stale ad previously filled the slot — the replacement is prefetched for the next foreground.
- If you relied on the 4-second `appOpenAdTimeout` (i.e. never called `resetToDefaults()`), the welcome/loading dialog can now stay up longer while a fresh ad loads. Set `appOpenAdTimeout = 4.seconds` explicitly to keep the old bound.

---

## Upgrading

Drop-in from 4.3.x. No code changes required.

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.3.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.3.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.3.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.3.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.3.5'
```
