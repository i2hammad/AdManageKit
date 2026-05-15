# Release Notes — v3.5.7

**Release Date:** 2026-05-15

## Overview

v3.5.7 introduces a **structured subscription offer API** in `admanagekit-billing` and fixes long-standing bugs in the pricing helpers that returned wrong values for multi-offer subscriptions.

The headline addition is `OfferInfo` — a typed data class that exposes every pricing phase of a Google Play subscription offer (free trial, introductory, base) as flat fields. Paywalls no longer need to walk `ProductDetails.SubscriptionOfferDetails` manually or guess which list index holds which phase.

All modules are bumped to **3.5.7** (including `admanagekit-core`, previously at 3.4.3).

---

## What's New

### Structured Offer API

Three new public methods on `AppPurchase`:

| Method | Returns | Description |
|--------|---------|-------------|
| `getOffers(productId)` | `List<OfferInfo>` | Every offer attached to a subscription product |
| `getTrialOffer(productId)` | `OfferInfo?` | First offer containing a free-trial phase |
| `getBaseOffer(productId)` | `OfferInfo?` | Non-promo offer (falls back to last offer in the list) |

#### `OfferInfo` schema

Phases are classified by **recurrence mode and price**, not list position:

| Phase | Detection rule | Fields |
|-------|----------------|--------|
| Free trial | `priceAmountMicros == 0` + `FINITE_RECURRING` | `isFreeTrial`, `trialPeriod`, `trialPhase` |
| Introductory | `priceAmountMicros > 0` + `FINITE_RECURRING` | `hasIntroPrice`, `introPrice`, `introPriceMicros`, `introPeriod`, `introCycleCount`, `introPhase` |
| Base | `INFINITE_RECURRING` (falls back to last phase) | `basePrice`, `basePriceMicros`, `billingPeriod`, `currencyCode`, `basePhase` |

Plus offer metadata: `productId`, `basePlanId`, `offerId`, `offerToken`, `offerTags`, raw `pricingPhases`.

#### Usage

```kotlin
val billing = AppPurchase.getInstance()

// Render the trial offer in a paywall
billing.getTrialOffer("premium_yearly")?.let { trial ->
    badge.text = "Free for ${trial.trialPeriod}"             // "P7D"
    price.text = "${trial.basePrice}/${trial.billingPeriod}" // "$59.99/P1Y"
}

// Plain base price when no trial is shown
val base = billing.getBaseOffer("premium_yearly")
priceLabel.text = base?.basePrice.orEmpty()

// Iterate every offer (trial + intro + base + tagged variants)
for (offer in billing.getOffers("premium_yearly")) {
    Log.d("IAP", "${offer.basePlanId}/${offer.offerId} " +
            "trial=${offer.isFreeTrial} (${offer.trialPeriod}) " +
            "intro=${offer.introPrice} base=${offer.basePrice}")
}
```

Java callers can use the same APIs — `OfferInfo` getters follow standard Kotlin-to-Java naming (`isFreeTrial()`, `getHasIntroPrice()`, `getTrialPeriod()`, etc.).

---

## Fixed

Pricing helpers previously assumed a single offer per product and read fixed list positions (`offers[0]` or `offers[last]`), producing wrong values for subscriptions with multiple offers:

| Method | Before | After |
|--------|--------|-------|
| `getIntroductorySubPrice(id)` | Compared the first phase against itself — could never return an intro price | Scans every offer and returns the discounted finite-recurring phase |
| `getPriceSub(id)` | Returned the last phase of the last offer (which is sometimes the intro phase) | Returns the `INFINITE_RECURRING` base phase |
| `getBillingPeriod(id)` | Same list-position bug | Returns the base phase's `billingPeriod` |
| `getPricePricingPhaseList(id)` | Returned phases from the last offer | Returns the base offer's phase list |

`hasFreeTrial()` / `getFreeTrialPeriod()` now route through the same offer classifier — external behavior unchanged, but single source of truth.

---

## Documentation

- Full Javadoc on every pricing, trial, offer, and product-metadata method in `AppPurchase`
- Full KDoc on `OfferInfo` (class-level rules + per-field docs)
- New "OfferInfo" section in `docs/API_REFERENCE.md`
- New "Structured Offer API" section in `docs/APP_PURCHASE_GUIDE.md`
- New "Structured Offers" sections in `wiki/Billing-Integration.md` and `wiki/Subscriptions.md`

---

## Module Versions

| Module | Previous | New |
|--------|----------|-----|
| `ad-manage-kit` | 3.4.6 | **3.5.7** |
| `ad-manage-kit-billing` | 3.4.6 | **3.5.7** |
| `ad-manage-kit-compose` | 3.4.6 | **3.5.7** |
| `ad-manage-kit-yandex` | 3.4.6 | **3.5.7** |
| `ad-manage-kit-core` | 3.4.3 | **3.5.7** |

---

## Compatibility

**No breaking changes.** Every existing `AppPurchase` signature is preserved.

⚠️ The four fixed helpers (`getIntroductorySubPrice`, `getPriceSub`, `getBillingPeriod`, `getPricePricingPhaseList`) now return **more accurate** values on multi-offer subscriptions. If a paywall in your app depended on the previous list-position-based behavior (for example, displaying the intro price where the base price was expected), review the affected UI before shipping.

---

## Installation

```kotlin
dependencies {
    implementation("com.github.i2hammad:ad-manage-kit:3.5.7")
    implementation("com.github.i2hammad:ad-manage-kit-billing:3.5.7")
    // Optional
    implementation("com.github.i2hammad:ad-manage-kit-compose:3.5.7")
    implementation("com.github.i2hammad:ad-manage-kit-yandex:3.5.7")
}
```

---

## References

- [`docs/API_REFERENCE.md`](../API_REFERENCE.md#offerinfo-v357) — full `OfferInfo` schema
- [`docs/APP_PURCHASE_GUIDE.md`](../APP_PURCHASE_GUIDE.md#structured-offer-api-v357) — paywall integration guide
- [`wiki/Billing-Integration.md`](../../wiki/Billing-Integration.md) — billing setup walkthrough
- [`wiki/Subscriptions.md`](../../wiki/Subscriptions.md) — subscription pricing reference
