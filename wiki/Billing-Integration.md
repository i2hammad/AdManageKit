# Billing Integration

AdManageKit provides a comprehensive billing integration module (`admanagekit-billing`) that simplifies Google Play Billing Library v8 implementation.

## Quick Start

### 1. Add Dependency

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.5.9'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.5.9'
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

## Pages

- [[Purchase Categories]] - Product classification system
- [[Consumable Products]] - Handling consumables with manual consumption
- [[Subscriptions]] - Subscription lifecycle management
- [[Subscription Upgrades]] - Upgrade/downgrade handling
- [[Purchase History]] - Tracking purchases with PurchaseHistoryListener
- [[PurchaseResult API]] - Full API reference for PurchaseResult
- [[Server Verification]] - Server-side purchase verification
