# Release Notes — v4.4.3

**Release Date:** 2026-08-15

## Overview

v4.4.3 is a bug-fix and dependency release. No API was added, removed or changed. Two of the fixes are silent-failure bugs that cost real money:

- **A rewarded load the manager had given up on could sabotage the load that replaced it** — throwing away an ad that had just arrived, and clearing the "loading" flag out from under a request that was still running.
- **Billing could stop acknowledging purchases for the rest of the process**, which lets Play auto-refund a completed purchase on day 3.

It also picks up the Next-Gen GMA SDK **1.3.1**, Yandex Mobile Ads **8.3.0**, and refreshed AndroidX/Firebase/Compose BOMs.

Upgrading is a drop-in from 4.4.x.

---

## Fixed

### A timed-out rewarded load could sabotage the one that replaced it

`RewardedAdManager.loadRewardedAdWithTimeout(...)` stops waiting for a load once its timeout elapses — but a request handed to the SDK **cannot be cancelled**, so it keeps running and reports back whenever it finishes. Its callbacks then ran the full success/failure path as though they still spoke for the manager, even though a replacement load had started in the meantime.

Concretely, a late callback from an abandoned request could:

- **Throw away a perfectly good ad.** The failure path ran `rewardedAd = null` unconditionally, so a stale failure discarded an ad a *later* load had just delivered. The next `showAd()` found nothing and skipped the reward.
- **Clear `isLoading` out from under the load that was still running**, so the manager reported itself idle while a request was in flight — letting a duplicate load start and burning an extra ad request.
- **Fail callers queued behind a load that had not finished yet**, via `notifyPendingLoadFailure`.
- **Schedule a retry for a request nobody was waiting on.**

Every load path now claims a generation token and checks it before touching anything shared — the same mechanism `RewardedWaterfall` already used internally. A stale callback answers *its own* caller (which is still owed a result and has no other source of one) and otherwise leaves shared state alone.

A late ad is **kept rather than dropped**: it is real and showable no matter which request produced it, so it is adopted for the next show whenever nothing better is already in hand. This applies to both the AdMob path and the waterfall path.

Two related unconditional writes were tightened at the same time:

- The direct load's failure path no longer does `rewardedAd = null` at all. A failed request means *this* request produced nothing — not that an ad obtained elsewhere has stopped being showable.
- The waterfall failure path now clears `rewardedWaterfall` only when the chain being reported on is still the one it installed (`if (rewardedWaterfall === waterfall)`), so a newer load's chain — or a late chain adopted in the meantime — is not discarded by an older failure.

> This is the same class of bug as the v4.4.2 `AdManager` duplicate-callback fix, in the one manager that still resolved in-flight state with plain flags.

### Billing could stop acknowledging purchases for the rest of the process

`AppPurchase` gated every re-query on `isServiceConnected`, an `AtomicBoolean` written **only** by `onBillingSetupFinished` and cleared by `onBillingServiceDisconnected`.

The library enables `enableAutoServiceReconnection()`, and the SDK restores the connection on its own without necessarily re-firing `onBillingSetupFinished`. So after a single disconnect the flag could stay `false` permanently — while the client itself was perfectly ready — silently disabling:

- `verifyPurchased(...)` and `updatePurchaseStatus()` — entitlement re-checks
- `refreshPurchases(...)` — including **the acknowledgment retry**
- `consumePurchase(...)`, `queryProductDetails(...)`
- `connectToGooglePlayBilling()`, which skipped reconnecting because it believed it was still connected

That last one matters most: **Play auto-refunds any purchase left unacknowledged for 3 days.** A user who bought while the flag was stuck could be charged, granted the entitlement, and then silently refunded — with the app still treating them as a paying customer.

All of these now consult the client's own live state (`billingClient.isReady()`) instead of the latched flag. `onBillingServiceDisconnected` also stops clearing `isBillingAvailable`: the client *was* set up successfully, and `isAvailable()` ANDs that fact with the live readiness check, so it now reports `false` only while the service is actually down rather than latching `false` for the rest of the process.

### Acknowledgment required the purchase to match a configured product id

In both `verifyPurchased(...)` and `updatePurchaseStatus()`, `acknowledgePurchaseIfNeeded(purchase)` was called *inside* the loop that matched the purchase against `inAppProductIdList` / `subProductIdList`. A `PURCHASED` order whose id the current build does not list was never acknowledged — and so was auto-refunded on day 3.

That is not a hypothetical set: promo-code grants, products dropped from a newer release, a Play Console id typo, and a user who purchased on a build that configured more ids than the one they are running now all land here.

Acknowledgment now happens **before** the id match, unconditionally for every `PURCHASED` purchase. Entitlement still requires a configured id — only the acknowledgment was decoupled, since the 3-day clock does not care whether your build recognises the product.

---

## Dependencies

| Dependency | 4.4.2 | 4.4.3 |
| --- | --- | --- |
| Google Mobile Ads Next-Gen SDK | 1.3.0 | **1.3.1** |
| Yandex Mobile Ads | 8.2.0 | **8.3.0** |
| Compose BOM | 2026.06.01 | **2026.08.00** |
| Firebase BOM | 34.15.0 | **34.17.0** |
| AndroidX AppCompat | 1.7.1 | **1.8.0** |
| ConstraintLayout | 2.2.1 | **2.2.2** |
| org.json (test only) | 20250517 | **20260719** |

Google Play Billing stays at **9.1.0** and Kotlin at **2.2.10**.

### Sample app now compiles at JVM 17

The `app` sample module was the only module still pinned to `jvmTarget = "1.8"`, while all five published modules use 17. Compose BOM 2026.08.00 ships artifacts compiled at JVM 11+, which cannot be inlined into 1.8 bytecode — so the bump broke `assembleDebug` on the sample with `Cannot inline bytecode built with JVM target 11 into bytecode that is being built with JVM target 1.8`.

The sample now uses `VERSION_17` / `jvmTarget = "17"`, matching the library modules. **This does not affect the published artifacts** — their compile options were already 17 and are unchanged. It is noted here only because anyone building the repo from source would otherwise hit it.

---

## Documentation

`docs/APP_PURCHASE_GUIDE.md` now documents what pending purchases require from the host app. A pending order (cash, bank transfer, parental approval) can complete hours or days later while the app is closed; `onPurchasesUpdated` only fires if the app happens to be running, so Play requires apps to re-query owned purchases on every foreground. `AppPurchase` queries on billing init, and if the process survives the transition that never runs again — so the guide now shows calling `refreshPurchases()` from the main activity's `onResume()`, and explains that this is what stops a completed pending order from being auto-cancelled on day 3.

---

## Verification

`assembleDebug` succeeds and `testDebugUnitTest --rerun-tasks` passes **176 tests, 0 failures** — 173 existing plus 3 new `RewardedAdManagerStaleLoadTest` cases pinning down the stale-load contract:

- an abandoned load's failure must not discard an ad another load produced
- an abandoned load's failure must not clear the replacement's `isLoading`
- an ad arriving after the timeout is kept for the next show rather than dropped

The tests drive the waterfall path, whose providers are fakes the test can complete by hand; the AdMob path runs the same generation checks over a static SDK entry point.

The billing changes are **not** covered by automated tests — both depend on `BillingClient` connection-state transitions that the existing `ProductDetailsFactory`-based harness cannot drive. They were verified by reading every call site of `isServiceConnected` and every `acknowledgePurchaseIfNeeded` path; on-device verification against a Play test track is recommended.

---

## Upgrading

Drop-in from 4.4.x. No source changes required.

If your app relies on pending purchases, add the `onResume()` refresh described under [Documentation](#documentation) — the library cannot do this for you, since it does not own your activity lifecycle.

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.3'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.3'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.3'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.3'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.3'
```
