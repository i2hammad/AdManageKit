# Billing Integration

AdManageKit provides a comprehensive billing integration module (`admanagekit-billing`) that simplifies Google Play Billing Library v9 implementation.

## Quick Start

### 1. Add Dependency

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.2'
```

### 2. Define Products

```kotlin
val products = listOf(
    // Consumable (coins, gems)
    PurchaseItem("coins_100", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE),

    // Lifetime premium (disables ads)
    PurchaseItem("lifetime", TYPE_IAP.PURCHASE, PurchaseCategory.LIFETIME_PREMIUM),

    // Remove ads only
    PurchaseItem("remove_ads", TYPE_IAP.PURCHASE, PurchaseCategory.REMOVE_ADS),

    // Subscription with trial
    PurchaseItem("premium_monthly", "free_trial", TYPE_IAP.SUBSCRIPTION)
)
```

### 3. Initialize

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inject your build state (a library AAR always has BuildConfig.DEBUG = false).
        // In debug builds this registers a test product and routes purchase()/subscribe()
        // to the dev purchase bottom sheet instead of the real Play flow. Call BEFORE initBilling.
        AppPurchase.getInstance().setDebugMode(BuildConfig.DEBUG)

        AppPurchase.getInstance().initBilling(this, products)
    }
}
```

> **Acknowledgment is automatic.** `AppPurchase` acknowledges every `PURCHASED`
> purchase before firing its callbacks, on both the new-purchase and restore
> paths — preventing Google Play's 3-day auto-refund of unacknowledged purchases.
> Only `PurchaseState.PURCHASED` grants entitlement (pending purchases do not).
> Consumables still require a manual `consumePurchase(productId)` after granting.

### 4. Make Purchases

```kotlin
// In-app purchase
AppPurchase.getInstance().purchase(activity, "remove_ads")

// Subscription
AppPurchase.getInstance().subscribe(activity, "premium_monthly")
```

### 5. Check Purchase Status

```kotlin
if (AppPurchase.getInstance().isPurchased()) {
    // User has premium (subscription, lifetime, or remove_ads)
}
```

### 6. Product Metadata (v3.4.1+)

```kotlin
val billing = AppPurchase.getInstance()
val name = billing.getProductName("premium_monthly")           // "Monthly Premium"
val description = billing.getProductDescription("premium_monthly")
val hasTrial = billing.hasFreeTrial("premium_monthly")         // true/false
val period = billing.getBillingPeriod("premium_monthly")       // "P1M"
```

### 7. Structured Offers (v3.5.7+)

For multi-offer subscriptions, use `OfferInfo` to read each offer's trial,
introductory, and base phases without parsing `ProductDetails` manually:

```kotlin
val trial = billing.getTrialOffer("premium_yearly")
trial?.let {
    badge.text = "Free for ${it.trialPeriod}"        // "P7D"
    price.text = "${it.basePrice} / ${it.billingPeriod}"
}

val base = billing.getBaseOffer("premium_yearly")    // non-promo offer
val all  = billing.getOffers("premium_yearly")       // every offer
```

### 8. Buy a Specific Offer (v4.4.0+)

`subscribe(activity, subsId)` picks the offer for you (the configured `trialId`,
else Play's *last* offer), so on a multi-offer product it can charge for the
wrong plan. Pass the offer the user actually tapped:

```kotlin
val offers = billing.getOffers("premium_sub")
billing.subscribe(activity, offers[selectedIndex])   // exactly this plan
```

See [[Subscription Offers]] for offer lookup, price normalization, savings
badges, trial eligibility, and one-time product offers.

### 9. Diagnose an Empty Paywall (v4.4.0+)

```kotlin
billing.setProductDetailsListener(object : ProductDetailsListener {
    override fun onProductDetailsLoaded(
        productType: String,
        loaded: List<ProductDetails>,
        unfetched: List<UnfetchedProduct>,
    ) {
        unfetched.forEach { Log.e("Billing", "${it.productId}: status ${it.statusCode}") }
    }
    override fun onProductDetailsFailed(productType: String, responseCode: Int, debugMessage: String?) { }
})
```

Register it **before** `initBilling`. Products land in `unfetched` when the id is
misspelled, the product is inactive in Play Console, or the signed-in account
cannot see the release track. `isProductDetailsLoaded(id)` and
`areAllProductDetailsLoaded()` distinguish "missing" from "not loaded yet".

### 10. Fraud Prevention & EU Disclosure (v4.4.0+)

```kotlin
// Google-recommended hashed identifiers — never raw account data. Max 64 chars.
billing.setObfuscatedAccountId(sha256(userId))
billing.setObfuscatedProfileId(sha256(profileId))

// Required EU disclosure when prices are personalized per user.
billing.setOfferPersonalized(true)
```

Applied to every flow the library launches (`purchase`, `subscribe`,
`updateSubscription`). Set after sign-in; pass `null` on sign-out.

## Pages

- [[Purchase Categories]] - Product classification system
- [[Subscription Offers]] - Offers, trials, intro pricing, price comparison
- [[Consumable Products]] - Handling consumables with manual consumption
- [[Subscriptions]] - Subscription lifecycle management
- [[Subscription Upgrades]] - Upgrade/downgrade handling
- [[Purchase History]] - Tracking purchases with PurchaseHistoryListener
- [[PurchaseResult API]] - Full API reference for PurchaseResult
- [[Server Verification]] - Server-side purchase verification
