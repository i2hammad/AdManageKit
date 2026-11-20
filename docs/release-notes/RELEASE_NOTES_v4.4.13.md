# Release Notes — v4.4.13

**Release Date:** 2026-11-20

## Overview

v4.4.13 is a **billing reliability release**. `AppPurchase` used to ask Play for product details once, at setup, and give up when that one query failed. Play answers it with `ERROR` ("An internal error occurred.") often enough that a real share of installs had no prices for the whole process, and every tap on Buy then failed with "Billing not initialized". One production app saw 6 purchase errors for every purchase that reached Play's sheet, nearly all from this pair.

Only `admanagekit-billing` changed. Dependencies are the same as 4.4.12.

---

## Fixed

- **Failed product-details queries are retried** after 2, 5 and 15 seconds, for transient response codes: `ERROR`, `SERVICE_UNAVAILABLE`, `SERVICE_DISCONNECTED` and `NETWORK_ERROR`. The count resets on success and on `initBilling()`.
- **A failed first billing setup is retried** on the same schedule. `enableAutoServiceReconnection()` only restores a client that connected once and then dropped; a setup that failed outright was never tried again.
- **`purchase()`, `subscribe()` and `updateSubscription()` re-query missing details** when they have to answer "Billing not initialized", so the user's next tap can succeed.
- **`queryProductDetails()` on a disconnected client connects** instead of returning silently. A successful setup queries every configured product.

## Added

- **`AppPurchase.refreshProductDetails()`**: re-queries each product type that has products without details, or reconnects a client whose setup never finished. Results arrive through `ProductDetailsListener`.

```kotlin
// When the paywall opens
val billing = AppPurchase.getInstance()
if (!billing.areAllProductDetailsLoaded()) billing.refreshProductDetails()
```

## Changed

- **A failed product-details query is reported only to `ProductDetailsListener.onProductDetailsFailed`.** It no longer reaches `PurchaseListener.displayErrorMessage` as "Error getting subs details: …". Paywalls showed that message, and often logged it as a purchase error, although nobody had tapped Buy. If your app depended on it, register a listener:

```kotlin
AppPurchase.getInstance().setProductDetailsListener(object : ProductDetailsListener {
    override fun onProductDetailsLoaded(productType: String, loaded: List<ProductDetails>, unfetched: List<UnfetchedProduct>) {
        // redraw prices
    }
    override fun onProductDetailsFailed(productType: String, responseCode: Int, debugMessage: String?) {
        // the library retries transient codes on its own
    }
})
```

---

## Upgrade

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.13'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.13'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.13'
```

No code changes are needed unless you read product-details failures from `PurchaseListener.displayErrorMessage`.
