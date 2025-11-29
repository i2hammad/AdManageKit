# Billing Integration

AdManageKit provides a comprehensive billing integration module (`admanagekit-billing`) that simplifies Google Play Billing Library v8 implementation.

## Quick Start

### 1. Add Dependency

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.9.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.9.0'
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
        AppPurchase.getInstance().initBilling(this, products)
    }
}
```

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

## Pages

- [[Purchase Categories]] - Product classification system
- [[Consumable Products]] - Handling consumables with manual consumption
- [[Subscriptions]] - Subscription lifecycle management
- [[Subscription Upgrades]] - Upgrade/downgrade handling
- [[Purchase History]] - Tracking purchases with PurchaseHistoryListener
- [[PurchaseResult API]] - Full API reference for PurchaseResult
- [[Server Verification]] - Server-side purchase verification
