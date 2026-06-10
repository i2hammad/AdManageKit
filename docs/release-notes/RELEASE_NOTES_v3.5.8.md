# Release Notes — v3.5.8

**Release Date:** 2026-06-10

## Overview

v3.5.8 is a **stability release**: it fixes the findings of a full-library code audit (issues #2–#35) across all six modules, and ships the project's first unit test suite (83 tests).

The most important fixes are in **billing** — two bugs that directly cost revenue — and in the **native ad caching layer**, which was broken on its default configuration:

- Purchases restored after an interrupted acknowledgment were **never acknowledged**, so Google Play silently auto-refunded them after 3 days. All restore paths now acknowledge.
- **PENDING (unpaid) purchases granted entitlement** — ads were removed for users who never completed payment. Entitlement now requires `PURCHASED` state.
- `NativeTemplateView` cache hits were consumed but **never displayed** (key mismatch), and network-error load failures were **silently swallowed** with default config, leaving blank ad slots with no callback.

All modules are bumped to **3.5.8**. The release is source- and binary-compatible with 3.5.7 — no public signatures were removed or changed; see *Behavior changes* for the two intentional behavioral corrections.

---

## Billing (`admanagekit-billing`)

- Restored purchases are acknowledged in `verifyPurchased()` / `updatePurchaseStatus()` / `refreshPurchases()` — stops 3-day auto-refunds
- Entitlement requires `PurchaseState.PURCHASED`; pending purchases no longer disable ads or unlock premium
- Ownership state is rebuilt from each query: refunds / cancellations / expiry now clear `isPurchased()` within the session; consumables no longer flip the global purchased flag
- `onInitBillingFinished` fires **exactly once** per init (was up to 3×), on the **main thread**; all listener callbacks are main-thread; shared state is thread-safe
- `getSubscriptionState()` / `isSubscriptionActive()` / `isSubscriptionCancelled()` now work — `productType` was never set on stored results, so every subscription reported `NOT_SUBSCRIPTION`
- Removed a wrong `BuildConfig` import that left every debug branch dead. **New:** `AppPurchase.setDebugMode(boolean)` to inject your app's debug state — the dev purchase sheet and test product now actually work in debug builds
- Subscription price/currency analytics resolve via the base offer (no more free-trial prices of 0 in revenue events); removed obfuscated `Product.zza()` usage; `endConnection()` on re-init

## Native ads (`AdManageKit`)

- `NativeTemplateView` caching works again: retrieval key now matches the storage key (cached ads were destroyed-on-read and never shown, or shown in the wrong view)
- `onFailedToLoad` always fires when no retry is scheduled — network/internal errors were swallowed with `autoRetryFailedAds = false` (the default)
- HYBRID strategy cache hits fire `onAdLoaded`
- The displayed ad is never simultaneously cached (eviction could destroy an on-screen ad)
- `NativeAdManager`: lock-ordering deadlock fixed, `clearAllCachedAds()` / cleanup races fixed, init no longer crashes for sub-minute cleanup intervals
- Displayed `NativeAd`s are destroyed when replaced; **new `destroy()` method** on all four native ad views (auto-invoked when detached while the Activity is finishing — safe in RecyclerViews)
- Preloading no longer binds throwaway `NativeAdView`s; programmatic LARGE native ads no longer crash with "child already has a parent"

## Interstitial / App Open / Banner / Rewarded / UMP (`AdManageKit`)

- Splash interstitial flow: the caller is notified **exactly once** — no more hung splash screens (duplicate in-flight load) or double navigation (auto-retry firing `onNextAction()` twice)
- Double-show guards on interstitials and rewarded ads; force/fresh flows show the exact requested ad instead of an arbitrary pooled unit
- `InterstitialAdBuilder.onAdShown` actually fires (was wired to the load callback)
- `AppOpenManager`: no more destroyed-Activity retention; pending callbacks are never stranded; concurrent load clobbering and the 500 ms double-show window are fixed
- `AdsConsentManager`: the UMP listener fires on **every** consent request — previously never after the first success, which could hang consent-gated startups
- `BannerAdView`: replaced AdViews are destroyed (one WebView leaked per auto-refresh cycle before); `enableAutoRefresh(intervalSeconds)` honors its parameter; collapsible config survives auto-refresh
- `RewardedAdManager` queues load callbacks against in-flight loads instead of dropping them
- `AdRetryManager` cancels superseded retries; `resetToDefaults()` restores all documented defaults

## Waterfall (`AdManageKit`, `admanagekit-core`)

- AdMob full-screen providers store ads **per ad unit** — no more cross-placement clobbering or wrong revenue attribution when multiple waterfalls share the global provider chain
- `waterfall.destroy()` no longer destroys globally shared providers. **New:** `ownsProviders` constructor parameter (default `false`) for waterfalls that should own their chain
- **New:** per-attempt watchdog timeout (default `AdManageKitConfig.defaultAdTimeout`, 15 s) — a hung provider advances the chain instead of stalling it forever
- Exactly one terminal callback per `load()` (stale chains are cancelled by generation tokens) and per `show()`
- `AdMobNativeProvider` returns a real populated template (previously an empty `NativeAdView` with impression tracking activated); 4-hour app-open freshness enforced; registries are thread-safe

## Yandex (`admanagekit-yandex`)

- Error codes are translated to `AdKitAdError` constants — previously raw Yandex codes were passed through, so `NETWORK_ERROR` was reported as `NO_FILL`, etc.
- Impression revenue is paired with its correct currency — non-USD accounts were reporting USD values tagged with the account currency (off by the full exchange rate)
- Loaders cancel on `destroy()`; a preloaded next ad survives dismissal; native bind failures are reported as failures on all sizes

## Compose (`admanagekit-compose`)

- `InterstitialAdEffect` shows the ad from the load result — it previously checked `isReady()` one line after starting an async load and effectively never showed the requested ad
- `onAdShown` fires from the real show callback, never before a skipped show
- Ad views are recreated and attached when composable parameters change; banner disposal destroys the underlying `AdView` (stops invisible background ad requests); native composables destroy their ads on dispose
- `rememberPurchaseStatus()` refreshes on resume/recomposition instead of freezing at first composition — `ConditionalAd` now reacts to mid-session purchases

---

## Behavior changes

1. **Waterfall show failures deliver a single terminal event**: `onAdFailedToShow` on failure, `onAdDismissed` only after a successful show. Previously both could fire for one failed show, running navigation twice. If your code relied on `onAdDismissed` after a failure, handle `onAdFailedToShow` as terminal instead.
2. **Billing entitlement is live**: refunds/expiry now clear `isPurchased()` on the next query cycle, and a manual `setPurchase(true)` override is overwritten by real Play Billing results.

## Testing

- New: 83 JVM unit tests (`./gradlew :app:testDebugUnitTest`) covering waterfall contracts, retry management, native cache semantics, config, ad-unit mapping, and subscription state
- Full audit trail: issues #2–#35, fixed via PR #37

## Installation

```gradle
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.5.8'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.5.8'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.5.8'

    // Optional
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.5.8'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.5.8'
}
```
