# Release Notes — v4.4.2

**Release Date:** 2026-07-29

## Overview

v4.4.2 is a bug-fix release. No API was added, removed or changed — but several of these are user-visible, and two affect revenue directly:

- **Rewarded ads could crash the app.** Every rewarded show-path callback, including `onRewardEarned`, was delivered on a background thread.
- **A completed purchase could fail to disable ads**, leaving a paying customer looking at ads until the next app launch.
- **Blank gaps** where a banner or native ad should have collapsed — for premium users and after a failed load, in both XML and Compose.
- **App open ads could appear over excluded screens**, including flows protected by `disableAppOpenAdsTemporarily()`.

Upgrading is a drop-in from 4.4.x. Two behavior changes are called out under [Behavior changes](#behavior-changes).

---

## Fixed

### Rewarded ads: callbacks delivered off the main thread

`RewardedAdManager` attached the Next-Gen SDK's event callbacks directly, with no marshalling anywhere on the direct (non-waterfall) path. `onAdDismissed`, `onAdShowed`, `onAdFailedToShow`, `onAdClicked` and — most importantly — the `onRewardEarned` reward listener all ran on a background thread.

`onRewardEarned` and `onAdDismissed` are **abstract** on `RewardedAdCallback`: every consumer implements them, and they are exactly where apps grant the reward and update the UI. The result was a `CalledFromWrongThreadException` or a silently corrupted UI update on every rewarded flow that touched a view.

All load, show and reward callbacks now post to the main thread. The waterfall path was already safe via its providers, but is now marshalled explicitly too, so the guarantee holds for third-party providers registered through `AdProviderConfig` rather than depending on their behavior.

> The v4.3.1 threading fix covered interstitial `onNextAction`, the banner/native waterfall chains and `NativeTemplateView` — rewarded was missed.

### A completed purchase could fail to disable ads

`AppPurchase.handlePurchase(Purchase)` wrapped every entitlement mutation in `if (productDetailsMap.get(productId) != null)`. That map is populated **only** by a successful `queryProductDetailsAsync`, and Play declines product ids routinely — which is why `getUnfetchedProducts()` exists.

When details were missing, the purchase was acknowledged, `onProductPurchased` fired, and the entitlement was silently dropped: `isPurchased()` kept returning `false` and **ads kept serving to a paying customer**. Nothing masked it, either — `verifyPurchased()` runs only on billing-service connect, so the state stayed wrong until the next app launch.

Product type is now resolved from the configured `subProductIdList` / `inAppProductIdList` first — matching what `verifyPurchased()` already did — then from `productDetailsMap`, and finally from the purchase's own `isAutoRenewing()` flag. Granting on a reasonable inference beats billing someone and leaving the ads on.

### Blank gaps where an ad should have collapsed

Two independent causes, both producing the same symptom.

**In XML/View usage:** `NativeBannerSmall`, `NativeBannerMedium` and `NativeLarge` hid only their shimmer for premium users and left the root visible, leaving an empty view with its padding and background. `NativeBannerMedium` never collapsed on load failure at all, and `NativeBannerSmall` only did so on the strategy path, not the direct AdMob one. All three now collapse the root, and reset visibility at load start so a later successful load isn't left invisible.

**In Compose:** every wrapper claimed its height with a Compose modifier while the "hide me" logic lived in the View's `visibility`. Setting `visibility = GONE` cannot shrink the Compose node, so the ad vanished and the gap remained. Each `update` block then forced the view back to `VISIBLE`, undoing the hide the library had just performed — and for banners that ran through `showAd()`, which **restarts the shimmer** on an empty slot.

Height is now driven by slot state rather than view visibility (`LOADING` and `SHOWN` reserve the ad's height so a fill doesn't shift the layout; `HIDDEN` occupies nothing), and no `update` block forces visibility unconditionally. `ProgrammaticNativeAdCompose`'s error branch rendered empty content inside a still-reserved 80–300dp box; that box now collapses.

**All five Compose ad composables now gate on `rememberPurchaseStatus()`**, so a premium user issues no ad request and reserves no space. That helper already existed and was reactive — nothing internal used it.

### Compose silently loaded nothing on a wrapped context

Every Compose ad entry point gated on `context is ComponentActivity` — 12 sites, with no `ContextWrapper` unwrapping and, at ten of them, no `else` branch.

`LocalContext.current` is the Activity in the common `setContent { }` case, but it is a `ContextThemeWrapper` inside `Dialog` / `ModalBottomSheet` composables, under per-screen theme wrappers, and in some `AndroidView`-nested compositions. There, the composable loaded nothing, fired no failure callback, and rendered an empty box forever with no diagnostics.

New `Context.findComponentActivity()` unwraps the wrapper chain (mirroring `NativeBannerSmall.findHostActivity()`), and every site now reports a failure instead of failing silently.

### App open ads could appear over excluded screens

When an ad finished loading while the app was backgrounded, `onStart` routed it through a pending-ad path that checked purchase state, interstitial conflicts and activity validity — but **never `isActivityExcluded(...)` and never `skipNextAd`**.

An app that called `disableAppOpenAdsTemporarily()` before a payment flow, or excluded its splash/PIN activity, could still get a full-screen ad there if the user backgrounded the app mid-fetch and returned. Both gates are now honored; the loaded ad stays cached for the next legitimate opportunity.

### Retries could strand the caller waiting on them

`AdRetryManager` keys retries by ad unit id, so scheduling for an id that already has one **replaces** it. `NativeAdIntegrationManager` deliberately withholds the failure callback when it schedules a retry, expecting that retry to deliver the result.

Because the screen-specific key resolves to the *base* ad unit id, two native views sharing one ad unit (a `SMALL` and a `MEDIUM` on the same screen) collide — and the evicted retry's owner was never told anything. Its view held its shimmer forever.

`scheduleRetry` now accepts an `onDropped` callback, invoked when a retry is replaced, cancelled, or refused because retries are disabled/exhausted. It is delivered via `Handler.post` so a handler that reacts by scheduling another retry cannot re-enter `scheduleRetry` mid-mutation. A non-atomic get-then-remove in `cancelRetry` was fixed alongside it.

### Duplicate callbacks and duplicate ad shows

`AdManager`'s force-show and splash flows guarded their outcomes with plain captured `var`s, written from both the SDK's background thread and the main-thread timeout. Neither side could see the other's write reliably, so a timeout landing alongside a load could deliver `onNextAction()` twice — breaking the once-per-flow contract callers gate navigation on — or attempt two shows.

Each flow now uses a single `AtomicBoolean` latch shared across load-success, load-failure and timeout, so exactly one outcome wins.

`AdManager.onAdShowedFullScreenContent` also invoked `callback.onAdShowed()` on the SDK thread while the dismiss and failure paths three lines away marshalled correctly; it now matches them.

### Leaks

- **`BannerAdView` never stopped auto-refresh on detach.** A banner inside a `RecyclerView`, `ViewPager` or a fragment whose view is destroyed kept requesting ads on schedule — indefinitely, since each refresh schedules the next — and the pending `Runnable` retained the Activity. Host `ON_DESTROY` only covers the case where the whole Activity goes away. Refresh now stops on detach and resumes on re-attach, and the lifecycle observer is unregistered on cleanup.
- **`RewardedAdManager`'s retry closures captured the Activity** passed to `showAd()`, parked on the main `Handler` for up to `maxRetryDelay` off a process-lifetime singleton. They now use the application context, as `AdManager` already did at three sites.

### Also hardened

- `AdManager` no longer opens its loading dialog over a finishing or destroyed activity. A `BadTokenException` there left `isFetchingWithDialog` stuck `true`, permanently short-circuiting every later force-show to `onNextAction()`.
- `showWaterfallWithWelcomeDialog` attaches to an in-flight load instead of starting a duplicate one, matching its non-waterfall twin.
- Restored / already-acknowledged purchases now reach the purchase history listener; that branch previously skipped it.

---

## Behavior changes

Both are deliberate, and both follow the library's existing account-hold stance.

1. **An account-hold subscription no longer disables ads.** `getSubscriptionState()` already returned `ON_HOLD` and `isSubscriptionActive()` already returned `false` for these users, but `isPurchased()` still counted them — so they got no premium features *and* no ads. If your app deliberately keeps serving on-hold users ad-free, check `isSuspended()` explicitly. Pair `hasSubscriptionOnHold()` with `showInAppMessages(...)` to prompt a payment fix.

2. **Premium users no longer reserve ad space in Compose.** Composables return before creating the view, so the slot occupies zero height rather than an empty block. If your layout relied on that reserved space, add it explicitly.

---

## Verification

`assembleDebug` succeeds and `testDebugUnitTest --rerun-tasks` passes **173 tests, 0 failures** (168 existing plus 5 new `AdRetryManager` cases covering the `onDropped` contract, including a negative case proving a retry that runs does not also report dropped).

The layout fixes are **not** covered by automated tests — the Compose module has no test source set and the View-layer visibility changes need a real hierarchy. They were verified by reading the visibility paths end to end; confirming them on-device is recommended.

---

## Upgrading

Drop-in from 4.4.x. No source changes required; review the two behavior changes above.

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.2'
```
