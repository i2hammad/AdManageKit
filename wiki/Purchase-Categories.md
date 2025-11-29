# Purchase Categories

AdManageKit v2.9.0 introduces a category system for classifying in-app products.

## Categories

| Category | Description | Disables Ads | Re-purchasable |
|----------|-------------|--------------|----------------|
| `CONSUMABLE` | Virtual currency, power-ups | No | Yes (after consume) |
| `FEATURE_UNLOCK` | Level packs, themes | No | No |
| `LIFETIME_PREMIUM` | Permanent premium access | Yes | No |
| `REMOVE_ADS` | Ad removal only | Yes | No |

## Usage

```kotlin
// Consumable - coins, gems, credits
PurchaseItem("coins_100", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE)

// Feature unlock - doesn't disable ads
PurchaseItem("dark_theme", TYPE_IAP.PURCHASE, PurchaseCategory.FEATURE_UNLOCK)

// Lifetime premium - disables ads, grants all features
PurchaseItem("lifetime", TYPE_IAP.PURCHASE, PurchaseCategory.LIFETIME_PREMIUM)

// Remove ads only - just removes ads
PurchaseItem("remove_ads", TYPE_IAP.PURCHASE, PurchaseCategory.REMOVE_ADS)

// Subscription (category not applicable)
PurchaseItem("premium_monthly", "trial_offer", TYPE_IAP.SUBSCRIPTION)
```

## How Categories Affect `isPurchased()`

Only certain categories cause `isPurchased()` to return `true`:

```kotlin
// isPurchased() returns true for:
// - Any active subscription
// - LIFETIME_PREMIUM purchases
// - REMOVE_ADS purchases

// isPurchased() returns false for:
// - CONSUMABLE purchases
// - FEATURE_UNLOCK purchases
// - No purchases
```

## Checking Specific Categories

```kotlin
// Check by purchase type
when (AppPurchase.getInstance().getActivePurchaseType()) {
    PurchaseType.SUBSCRIPTION -> showSubscriberUI()
    PurchaseType.LIFETIME_PREMIUM -> showLifetimeUI()
    PurchaseType.REMOVE_ADS -> hideAdsOnly()
    PurchaseType.NONE -> showPurchaseOptions()
}

// Check specific categories
AppPurchase.getInstance().hasLifetimePremium()     // LIFETIME_PREMIUM
AppPurchase.getInstance().hasRemoveAdsPurchase()   // REMOVE_ADS
AppPurchase.getInstance().hasLifetimePurchase()    // Any lifetime (LIFETIME_PREMIUM or REMOVE_ADS)
```

## PurchaseItem Helper Methods

```kotlin
val item = PurchaseItem("product", TYPE_IAP.PURCHASE, PurchaseCategory.LIFETIME_PREMIUM)

item.shouldDisableAds()    // true - LIFETIME_PREMIUM disables ads
item.isLifetimePurchase()  // true - one-time purchase (not subscription)
item.isConsumable          // false - not a consumable
```

## Default Category

When creating a `PurchaseItem` without specifying a category:

```kotlin
// Default is LIFETIME_PREMIUM (v2.9.0+)
PurchaseItem("product", TYPE_IAP.PURCHASE)

// Equivalent to:
PurchaseItem("product", TYPE_IAP.PURCHASE, PurchaseCategory.LIFETIME_PREMIUM)
```

## Best Practices

1. **Always specify category explicitly** for clarity
2. **Use CONSUMABLE** for any product users can buy multiple times
3. **Use FEATURE_UNLOCK** for one-time unlocks that don't affect ads
4. **Use LIFETIME_PREMIUM** for permanent "premium" access
5. **Use REMOVE_ADS** when you have separate ad removal and premium tiers
