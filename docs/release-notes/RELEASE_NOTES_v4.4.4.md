# Release Notes — v4.4.4

**Release Date:** 2026-08-15

## Overview

v4.4.4 is a **critical billing hotfix**. No API was added, removed or changed.

It repairs a regression introduced in **v4.4.3**: on that version the Play Billing connection is never started at all, so billing is dead for the life of the process — no product details, no offers, no entitlement, and no error anywhere to explain it. **Anyone on 4.4.3 who ships the billing module should upgrade immediately.**

It also fixes a long-standing bug where a billing setup that *timed out* reported itself as initialized, which permanently suppressed the host app's retry.

Upgrading is a drop-in from 4.4.x.

---

## Fixed

### Billing never connected on 4.4.3 (regression)

v4.4.3 replaced the latched `isServiceConnected` flag with the client's own `billingClient.isReady()` everywhere, to stop `enableAutoServiceReconnection()` from leaving re-queries permanently disabled. That was right for the *re-query* paths — but it was also applied to `connectToGooglePlayBilling()`, which is the path that starts the connection in the first place:

```java
// 4.4.3
if (!isClientReady()) {           // isReady() is already true on a fresh client
    billingClient.startConnection(...);
}
```

Since `enableAutoServiceReconnection()` was added, a `BillingClient` reports `isReady() == true` **the instant it is built**, before any setup has run. So the guard was false on every fresh client and `startConnection(...)` was skipped:

- setup never began, so **neither branch of `onBillingSetupFinished` ever fired**
- `isBillingAvailable` and `isBillingInitialized` stayed `false`
- `queryProductDetails(...)` and `verifyPurchased(...)` — both invoked from the success branch — never ran, so `productDetailsMap` stayed empty
- `isAvailable()` returned `false` forever, and `setBillingListener(...)` only ever reported `SERVICE_TIMEOUT`

The user-visible result is a paywall that renders no prices and a purchase flow that cannot be launched, with **no failure callback and no error log** to point at the cause.

`connectToGooglePlayBilling()` now guards on `isServiceConnected` — our own flag, written only by `onBillingSetupFinished` and cleared by `onBillingServiceDisconnected` — which is the right question for "has setup run?", as distinct from "can the client serve a call right now?" that `isClientReady()` answers for the re-query paths. Those paths are unchanged from 4.4.3 and keep the acknowledgment fix intact.

> Both flags are load-bearing and they are not interchangeable: `isServiceConnected` guards *starting* the connection, `isClientReady()` guards *using* it.

### A timed-out billing setup reported itself as initialized

In `setBillingListener(listener, timeout)`, the timeout runnable set `isBillingInitialized = TRUE` before delivering `SERVICE_TIMEOUT`:

```java
// before
this.isBillingInitialized = Boolean.TRUE;
billingListener.onInitBillingFinished(BillingClient.BillingResponseCode.SERVICE_TIMEOUT);
```

That path means Play did **not** answer in time — setup did not finish. It is the same outcome the `onBillingSetupFinished` failure branch reports, and that branch sets `FALSE`.

Reporting `TRUE` told every caller billing was up when it was not. The common host-app guard —

```kotlin
if (!AppPurchase.getInstance().initBillingFinish) { /* retry init */ }
```

— went permanently quiet, so a connection that timed out once could never be retried for the life of the process, and screens rendered "no offers" for what was really a dead client. It now sets `FALSE`. The listener still receives `SERVICE_TIMEOUT` either way, so callers that only wait on the callback are unaffected.

This bug predates 4.4.3 (it dates to the June 2026 billing listener work) but was largely masked until the regression above made timeouts the normal outcome.

## Changed

- **Billing connection lifecycle logging.** `initBilling`, `connectToGooglePlayBilling`, `onBillingSetupFinished` and `onBillingServiceDisconnected` now log their state at `DEBUG` (`Log.d`) under the existing `AppPurchase` tag — client built/null, `isReady()`, `isServiceConnected`, and the setup response code plus debug message. The failure above produced complete silence; this class of problem is now visible in `logcat` without attaching a debugger. No behavior depends on it, and it costs nothing beyond the log calls.

---

## Upgrading

```gradle
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.4'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.4'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.4'
```

No code changes are required. Nothing else in the library changed — no dependency updates, no ad-side changes.

To confirm the fix on device, filter `logcat` for the `AppPurchase` tag: you should see `connectToGooglePlayBilling: ... serviceConnected=false` followed by `onBillingSetupFinished: code=0`.

## Notes

- Neither fix is covered by an automated test — both depend on `BillingClient` connection-state transitions the current harness cannot drive (the same limitation noted for the 4.4.3 billing changes), so on-device verification against a Play test track is recommended. The existing 176 tests are unaffected and still pass
