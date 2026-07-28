# Release Notes — v4.4.0

**Release date:** 2026-07-28

A minor release focused on the billing module. Subscription offers can now be purchased individually, offer pricing can be compared and normalized without hand-parsing ISO-8601, Play Billing 9's one-time product offers are surfaced, and account hold is detected client-side.

Everything is additive — no existing method signature changed — with one deliberate behavior change around account hold, described below.

---

## The problem this release fixes

`AppPurchase` could already *describe* subscription offers (`getOffers`, `getTrialOffer`, `getBaseOffer`, `getIntroductorySubPrice`), but it could not *act* on them. `subscribe(activity, subsId)` always resolved the offer token itself: the offer whose id matched the configured `trialId`, and failing that, **whichever offer Play happened to list last**.

So a paywall showing three plans had no way to buy the one the user tapped. A user selecting "Yearly — save 50%" could be charged for the monthly plan, purely because of list order.

That is now a one-liner:

```kotlin
val offers = AppPurchase.getInstance().getOffers("premium_sub")
// …render them, then buy exactly what the user chose:
AppPurchase.getInstance().subscribe(activity, offers[selectedIndex])
```

---

## Purchasing a specific offer

```kotlin
// By OfferInfo (preferred)
AppPurchase.getInstance().subscribe(activity, offer)

// By raw token
AppPurchase.getInstance().subscribe(activity, "premium_sub", offer.offerToken)

// One-time products
AppPurchase.getInstance().purchase(activity, oneTimeOffer)
AppPurchase.getInstance().purchase(activity, "remove_ads", token)

// Upgrades onto a specific base plan
AppPurchase.getInstance().updateSubscription(
    activity, "premium_yearly", offer.offerToken, oldToken,
    AppPurchase.SubscriptionReplacementMode.CHARGE_PRORATED_PRICE
)
```

The existing no-token overloads are unchanged and still resolve the offer as before.

## Finding the right offer

```kotlin
val kit = AppPurchase.getInstance()

kit.getIntroOffer("premium_sub")                    // first offer with an intro price
kit.getOfferById("premium_sub", "winback_50")       // by Play Console offer id
kit.getOfferByBasePlanId("premium_sub", "yearly")   // by base plan id
kit.getOfferByTag("premium_sub", "popular")         // by Play Console offer tag
kit.getBestValueOffer("premium_sub")                // lowest cost per month
kit.getCheapestFirstCycleOffer("premium_sub")       // cheapest way in (trial > intro > full)
```

Offer **tags** are worth calling out: tagging offers in the Play Console and selecting them by tag lets you re-target a paywall without shipping an app update.

## Pricing that paywalls actually render

`BillingPeriod` parses the ISO-8601 strings Play returns and normalizes across cadences:

```kotlin
val period = BillingPeriod.parse("P1Y")!!
period.unit          // Unit.YEAR
period.count         // 1  → getQuantityString(R.plurals.years, count, count)
period.totalMonths   // 12.0
period.format()      // "1 year"  (English convenience; localize via unit + count)
```

`OfferInfo` exposes the derived numbers directly:

```kotlin
offer.firstCyclePrice        // what the user pays today: "Free", "$1.99", or "$9.99"
offer.firstCyclePriceMicros
offer.pricePerMonthMicros    // yearly plan normalized for comparison
offer.introDiscountPercent   // e.g. 80
offer.trialDays              // 7
offer.introTotalDays         // full promo window (cycle length × cycle count)
offer.isBaseOffer            // no trial, no intro
offer.hasTag("popular")
```

And the two strings every paywall needs:

```kotlin
kit.getSavingsPercent("premium_monthly", "premium_yearly")  // 50  → "Save 50%"
kit.getFormattedPricePerMonth("premium_yearly")             // "$5.00" → "$5.00/month, billed yearly"
```

Both compare **base** offers, so trial and introductory phases don't distort the badge.

## Trial eligibility

Google Play filters offers per account — a user who has already used a trial simply doesn't receive that offer. So the presence of a trial offer *is* the eligibility signal. That's now named explicitly:

```kotlin
if (kit.isEligibleForFreeTrial("premium_sub")) "Start 7-day free trial" else "Subscribe"
```

This stops paywalls promising a trial the user cannot claim and then having Play charge them immediately.

## One-time product offers (Play Billing 9)

A single one-time product can now carry several offers — a full price plus a launch discount, a rental, a pre-order, a limited-quantity drop. The legacy `getOneTimePurchaseOfferDetails()` exposes only one of them.

```kotlin
for (offer in kit.getOneTimeOffers("remove_ads")) {
    offer.effectiveDiscountPercent   // 30
    offer.fullPriceMicros            // strike-through price
    offer.isRental                   // rentalPeriod / rentalExpirationPeriod
    offer.isPreorder
    offer.isSoldOut                  // limited-quantity drops
    offer.isValidAt()                // validity window
}
kit.getBestOneTimeOffer("remove_ads")   // cheapest currently purchasable
```

## Account hold — behavior change

`PurchaseResult.isSuspended()` is now populated from Play Billing 9's client-side signal, and `getSubscriptionState()` returns `ON_HOLD` for those purchases.

**`isSubscriptionActive()` therefore returns `false` during account hold.** This is what Google requires: an on-hold user's payment was declined and they must not keep premium access. Detecting this previously required server-side verification through the Play Developer API.

If your app deliberately keeps serving on-hold users, check `isSuspended()` explicitly rather than relying on the old behavior.

```kotlin
if (kit.hasSubscriptionOnHold()) {
    // Let Play walk the user through fixing their payment method.
    kit.showInAppMessages(activity, object : InAppMessageListener {
        override fun onSubscriptionRecovered(purchaseToken: String) { refreshUi() }
        override fun onNoActionNeeded() {}
    })
}
```

## Payment recovery

`showInAppMessages(activity, listener)` displays Play's recovery flow for declined subscription payments. The library refreshes its own purchase state before invoking `onSubscriptionRecovered`, so `isPurchased()` is already correct inside the callback. Google recommends calling this on foreground entry for apps that sell subscriptions.

## Diagnosing an empty paywall

Previously, a paywall rendering blank prices gave no signal as to why. Now:

```kotlin
kit.setProductDetailsListener(object : ProductDetailsListener {
    override fun onProductDetailsLoaded(
        productType: String,
        loaded: List<ProductDetails>,
        unfetched: List<UnfetchedProduct>,
    ) {
        unfetched.forEach { Log.e("Billing", "${it.productId}: status ${it.statusCode}") }
    }
    override fun onProductDetailsFailed(productType: String, responseCode: Int, debugMessage: String?) { }
})

kit.isProductDetailsLoaded("premium_sub")   // distinguishes "missing" from "not loaded yet"
kit.areAllProductDetailsLoaded()
kit.getUnfetchedProducts()
```

## Fraud prevention and EU disclosure

```kotlin
kit.setObfuscatedAccountId(hashOf(userId))   // Google-recommended; never raw account data, max 64 chars
kit.setObfuscatedProfileId(hashOf(profileId))
kit.setOfferPersonalized(true)               // required EU disclosure when prices are personalized
```

These apply to every billing flow the library launches — `purchase`, `subscribe` and `updateSubscription` all funnel through one internal builder, so they can't be applied inconsistently.

## Pending plan changes

```kotlin
kit.hasPendingSubscriptionChange()
purchaseResult.hasPendingPurchaseUpdate()
purchaseResult.pendingProductIds
```

Use these to show "plan change pending" rather than switching the UI to the new tier before the user has paid for it.

---

## Migration

No action required. Existing code compiles and behaves as before, with two things worth reviewing:

1. **Account hold** — if you rely on `isSubscriptionActive()` and want to keep serving users whose payment was declined, add an explicit `isSuspended()` check.
2. **`OfferInfo`** gained a trailing `installmentPlanDetails` constructor parameter (defaulted to `null`). Source-compatible; obtain instances from `AppPurchase` rather than constructing them directly.

## Dependencies

```gradle
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.0'
```

All five modules are published at `4.4.0`.
