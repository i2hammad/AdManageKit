# Subscription Offers

*Available since v4.4.0 (the `OfferInfo` type itself since v3.5.7).*

A single Play subscription product routinely carries several **offers** — a base
plan, a 7-day free trial, a 50%-off introductory offer, a win-back offer. Play
returns only the offers the signed-in account is actually eligible for.

This page covers reading those offers, buying a specific one, comparing prices
across billing cadences, and the equivalent APIs for one-time products.

---

## Reading offers

```kotlin
val billing = AppPurchase.getInstance()

for (offer in billing.getOffers("premium_sub")) {
    Log.d("IAP", "${offer.basePlanId}/${offer.offerId} " +
            "trial=${offer.isFreeTrial} intro=${offer.introPrice} base=${offer.basePrice}")
}
```

`OfferInfo` classifies pricing phases by **recurrence mode and price**, not list
position, so it is correct even when an offer lists its phases unusually:

| Phase | Detected by |
|---|---|
| Free trial | `priceAmountMicros == 0` + `FINITE_RECURRING` |
| Introductory | paid + `FINITE_RECURRING` |
| Base (recurring) | `INFINITE_RECURRING` |

---

## Buying a specific offer

This is the part a multi-offer paywall needs. `subscribe(activity, subsId)`
resolves the offer token itself — the offer whose id matches the configured
`trialId`, and failing that **whichever offer Play listed last**. On a product
with three plans, a user tapping "Yearly" could be charged for the monthly one.

```kotlin
// Preferred — pass the offer object
billing.subscribe(activity, offer)

// Or the raw token
billing.subscribe(activity, "premium_sub", offer.offerToken)

// One-time products
billing.purchase(activity, oneTimeOffer)
billing.purchase(activity, "remove_ads", token)

// Upgrade onto a specific base plan
billing.updateSubscription(
    activity, "premium_yearly", offer.offerToken, oldPurchaseToken,
    AppPurchase.SubscriptionReplacementMode.CHARGE_PRORATED_PRICE
)
```

The original no-token overloads are unchanged and still resolve the offer as before.

---

## Finding an offer

```kotlin
billing.getIntroOffer("premium_sub")                   // first with an intro price
billing.getTrialOffer("premium_sub")                   // first with a free trial
billing.getBaseOffer("premium_sub")                    // no trial, no intro
billing.getOfferById("premium_sub", "winback_50")      // by Play Console offer id
billing.getOfferByBasePlanId("premium_sub", "yearly")  // by base plan id
billing.getOfferByTag("premium_sub", "popular")        // by Play Console offer tag
billing.getOffersByTag("premium_sub", "seasonal")      // all matching a tag
billing.getBestValueOffer("premium_sub")               // lowest cost per month
billing.getCheapestFirstCycleOffer("premium_sub")      // cheapest way in
```

**Offer tags are the most useful of these.** Tag offers in the Play Console and
select them by tag, and a paywall can be re-targeted without shipping an update.
Tag matching is case-insensitive.

`getBestValueOffer` and `getCheapestFirstCycleOffer` answer different questions:
the first optimizes long-run cost (a yearly plan wins), the second optimizes the
price of entry (a free trial wins).

---

## Comparing prices across cadences

Play returns billing periods as raw ISO-8601 (`"P7D"`, `"P1M"`, `"P1Y"`).
`BillingPeriod` parses them and normalizes prices so plans of different cadences
can be compared fairly.

```kotlin
val period = BillingPeriod.parse("P1Y")!!
period.unit          // Unit.YEAR
period.count         // 1
period.totalMonths   // 12.0
period.totalDays     // 365
period.format()      // "1 year"
```

> **Localization:** `format()` is an English convenience for prototypes. For
> shipped UI, use `unit` + `count` with your own plurals resources —
> `resources.getQuantityString(R.plurals.years, period.count, period.count)`.
> A library cannot resolve the host app's locale rules correctly.

`OfferInfo` exposes the derived values directly:

```kotlin
offer.firstCyclePrice       // what the user pays today: "Free", "$1.99" or "$9.99"
offer.firstCyclePriceMicros
offer.pricePerMonthMicros   // yearly plan normalized for comparison
offer.pricePerWeekMicros
offer.introDiscountPercent  // 80
offer.trialDays             // 7
offer.introTotalDays        // full promo window (cycle length × cycle count)
offer.isBaseOffer           // no trial, no intro
offer.hasPromotion
offer.billingPeriodParsed   // BillingPeriod?
offer.hasTag("popular")
```

And the two strings almost every paywall renders:

```kotlin
billing.getSavingsPercent("premium_monthly", "premium_yearly")  // 50 → "Save 50%"
billing.getFormattedPricePerMonth("premium_yearly")             // "$5.00"
```

Both compare **base** offers, so trial and introductory phases don't distort the
badge. Normalization uses the average Gregorian month (30.436875 days), so a
yearly plan normalizes to exactly 12 months.

> `getFormattedPricePerMonth` is composed locally from price micros and currency —
> Play does not provide it. Show it as an approximation ("$5.00/month, billed
> yearly"), never as the amount that will be charged.

---

## Trial eligibility

Google Play filters offers per account: a user who has already used the trial for
a subscription simply does not receive that offer. So the **presence of a trial
offer is the eligibility signal**.

```kotlin
subscribeButton.text = if (billing.isEligibleForFreeTrial("premium_sub")) {
    "Start free trial"
} else {
    "Subscribe"
}

billing.isEligibleForIntroPrice("premium_sub")   // same, for introductory pricing
```

This stops a paywall promising a trial the user cannot claim and then having Play
charge them immediately. Only meaningful once product details have loaded —
check `isProductDetailsLoaded(id)` first.

---

## Installment plans

```kotlin
offer.isInstallmentPlan
offer.installmentCommitmentPayments           // payments committed on purchase
offer.installmentRenewalCommitmentPayments    // payments committed on renewal
```

---

## One-time product offers

Play Billing 9 lets a one-time (INAPP) product carry several offers too — a full
price plus a launch discount, a rental, a pre-order, or a limited-quantity drop.
The legacy `getOneTimePurchaseOfferDetails()` exposes only one of them.

```kotlin
for (offer in billing.getOneTimeOffers("remove_ads")) {
    offer.formattedPrice
    offer.effectiveDiscountPercent  // 30 (from Play, or derived from fullPriceMicros)
    offer.fullPriceMicros           // strike-through price
    offer.isDiscounted
    offer.isRental                  // rentalPeriod / rentalExpirationPeriod
    offer.isPreorder                // preorderReleaseTimeMillis
    offer.isLimitedQuantity         // maximumQuantity / remainingQuantity
    offer.isSoldOut
    offer.isValidAt()               // validity window
    offer.hasTag("launch")
}

billing.getOneTimeOffer("remove_ads", "launch30")   // by offer id
billing.getBestOneTimeOffer("remove_ads")           // cheapest currently purchasable
```

`getBestOneTimeOffer` skips sold-out offers and offers outside their validity
window, so it returns something the user can actually buy.

---

## Example: a three-plan paywall

```kotlin
data class Plan(val offer: OfferInfo, val savings: Int)

val monthly = billing.getBaseOffer("premium_monthly") ?: return
val plans = listOf("premium_monthly", "premium_yearly").mapNotNull { id ->
    billing.getBaseOffer(id)?.let { Plan(it, billing.getSavingsPercent("premium_monthly", id)) }
}

plans.forEach { (offer, savings) ->
    row.title.text = offer.basePrice
    row.subtitle.text = BillingPeriod.formatOf(offer.billingPeriod)
    row.perMonth.text = AppPurchase.formatPrice(offer.pricePerMonthMicros, offer.currencyCode)
    row.badge.isVisible = savings > 0
    row.badge.text = "Save $savings%"
    row.setOnClickListener { billing.subscribe(activity, offer) }
}
```

---

## See also

- [[Billing Integration]] - Setup and quick start
- [[Subscriptions]] - Lifecycle, account hold, payment recovery
- [[Subscription Upgrades]] - Upgrade/downgrade handling
