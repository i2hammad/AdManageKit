# AppPurchase - Complete Billing Integration Guide

`AppPurchase` is a comprehensive wrapper for Google Play Billing Library v8, providing simplified APIs for in-app purchases, subscriptions, and purchase management.

## Table of Contents

- [Setup](#setup)
- [Product Configuration](#product-configuration)
- [Making Purchases](#making-purchases)
- [Consumable Products](#consumable-products)
- [Subscriptions](#subscriptions)
- [Subscription Upgrade/Downgrade](#subscription-upgradedowngrade)
- [Purchase State Checking](#purchase-state-checking)
- [Purchase History Tracking](#purchase-history-tracking)
- [Server-Side Verification](#server-side-verification)
- [Best Practices](#best-practices)

---

## Setup

### 1. Add Dependencies

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.9.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.9.0'
```

### 2. Initialize in Application Class

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Define your products
        val purchaseItems = listOf(
            // Consumables
            PurchaseItem("coins_100", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE),
            PurchaseItem("coins_500", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE),

            // One-time purchases
            PurchaseItem("remove_ads", TYPE_IAP.PURCHASE, PurchaseCategory.REMOVE_ADS),
            PurchaseItem("lifetime_premium", TYPE_IAP.PURCHASE, PurchaseCategory.LIFETIME_PREMIUM),
            PurchaseItem("unlock_levels", TYPE_IAP.PURCHASE, PurchaseCategory.FEATURE_UNLOCK),

            // Subscriptions (with optional trial offer ID)
            PurchaseItem("premium_monthly", "free_trial_7d", TYPE_IAP.SUBSCRIPTION),
            PurchaseItem("premium_yearly", "free_trial_14d", TYPE_IAP.SUBSCRIPTION)
        )

        // Initialize billing
        AppPurchase.getInstance().initBilling(this, purchaseItems)
    }
}
```

---

## Product Configuration

### Purchase Categories

| Category | Description | Disables Ads | Can Re-purchase |
|----------|-------------|--------------|-----------------|
| `CONSUMABLE` | Coins, gems, credits | No | Yes (after consume) |
| `FEATURE_UNLOCK` | Level packs, themes | No | No |
| `LIFETIME_PREMIUM` | Permanent premium | Yes | No |
| `REMOVE_ADS` | Ad removal only | Yes | No |

### Creating PurchaseItems

```kotlin
// Consumable product
PurchaseItem("coins_100", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE)

// Remove ads
PurchaseItem("remove_ads", TYPE_IAP.PURCHASE, PurchaseCategory.REMOVE_ADS)

// Lifetime premium
PurchaseItem("lifetime_premium", TYPE_IAP.PURCHASE, PurchaseCategory.LIFETIME_PREMIUM)

// Feature unlock (doesn't disable ads)
PurchaseItem("unlock_themes", TYPE_IAP.PURCHASE, PurchaseCategory.FEATURE_UNLOCK)

// Subscription with free trial offer
PurchaseItem("premium_monthly", "free_trial_offer", TYPE_IAP.SUBSCRIPTION)

// Subscription without trial
PurchaseItem("premium_yearly", null, TYPE_IAP.SUBSCRIPTION)
```

---

## Making Purchases

### In-App Products

```kotlin
// Set purchase listener
AppPurchase.getInstance().setPurchaseListener(object : PurchaseListener {
    override fun onProductPurchased(orderId: String?, originalJson: String?) {
        // Purchase successful
        Log.d("Billing", "Purchased! Order: $orderId")
    }

    override fun displayErrorMessage(errorMessage: String?) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onUserCancelBilling() {
        Log.d("Billing", "User cancelled")
    }
})

// Make purchase
val result = AppPurchase.getInstance().purchase(activity, "remove_ads")
```

### Subscriptions

```kotlin
// Subscribe
val result = AppPurchase.getInstance().subscribe(activity, "premium_monthly")
```

### Get Product Prices

```kotlin
// Get formatted price
val price = AppPurchase.getInstance().getPrice("remove_ads")  // "$2.99"
val subPrice = AppPurchase.getInstance().getPriceSub("premium_monthly")  // "$9.99/month"
```

---

## Consumable Products

Consumable products require manual consumption after granting items to the user.

### Flow

```
Purchase → Acknowledge → onNewPurchase() → Grant Items → consumePurchase() → onPurchaseConsumed()
```

### Implementation

```kotlin
AppPurchase.getInstance().setPurchaseHistoryListener(object : PurchaseHistoryListener {
    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        when (productId) {
            "coins_100" -> {
                // Grant coins to user
                userBalance += 100 * purchase.quantity
                saveUserBalance()

                // Now consume so they can buy again
                AppPurchase.getInstance().consumePurchase(productId)
            }
            "coins_500" -> {
                userBalance += 500 * purchase.quantity
                saveUserBalance()
                AppPurchase.getInstance().consumePurchase(productId)
            }
        }
    }

    override fun onPurchaseConsumed(productId: String, purchase: PurchaseResult) {
        // Optional: Track in analytics
        analytics.logEvent("purchase_consumed", mapOf(
            "product_id" to productId,
            "order_id" to purchase.orderId,
            "quantity" to purchase.quantity
        ))
    }
})
```

### Why Manual Consumption?

1. **Server verification**: Verify purchase on your server before granting items
2. **Error handling**: If granting fails, you can retry without losing the purchase
3. **Analytics**: Track exactly when items are granted vs purchased
4. **Multi-quantity**: Handle purchases with quantity > 1 correctly

---

## Subscriptions

### Checking Subscription Status

```kotlin
// Check if any subscription is active
if (AppPurchase.getInstance().isSubscribed()) {
    showPremiumContent()
}

// Check specific subscription
if (AppPurchase.getInstance().isSubscribed("premium_yearly")) {
    showYearlyBenefits()
}

// Get subscription state
val state = AppPurchase.getInstance().getSubscriptionState("premium_monthly")
when (state) {
    SubscriptionState.ACTIVE -> {
        // Active and will renew
        showPremiumUI()
    }
    SubscriptionState.CANCELLED -> {
        // Cancelled but still has access until expiration
        showRenewalPrompt()
    }
    SubscriptionState.EXPIRED -> {
        // No longer subscribed
        showSubscribeButton()
    }
}
```

### Subscription State Details

| State | Has Access | Will Renew | Detection |
|-------|------------|------------|-----------|
| ACTIVE | Yes | Yes | Client-side |
| CANCELLED | Yes | No | Client-side |
| GRACE_PERIOD | Yes | Pending | Server-side only |
| ON_HOLD | No | Pending | Server-side only |
| PAUSED | No | When resumed | Server-side only |
| EXPIRED | No | No | Client-side |

### Getting Subscription Details

```kotlin
val subscription = AppPurchase.getInstance().getSubscription("premium_monthly")
if (subscription != null) {
    println("Order ID: ${subscription.orderId}")
    println("Purchase Time: ${subscription.getPurchaseTimeFormatted()}")
    println("Will Renew: ${subscription.willSubscriptionRenew()}")
    println("State: ${subscription.getSubscriptionStateString()}")
}

// Get all active subscriptions
val allSubs = AppPurchase.getInstance().getActiveSubscriptions()
for (sub in allSubs) {
    println("${sub.getFirstProductId()}: ${sub.getSubscriptionStateString()}")
}
```

---

## Subscription Upgrade/Downgrade

### Simple Methods

```kotlin
// Upgrade to higher tier (charges price difference immediately)
AppPurchase.getInstance().upgradeSubscription(activity, "premium_yearly")

// Downgrade to lower tier (takes effect at next renewal)
AppPurchase.getInstance().downgradeSubscription(activity, "premium_basic")
```

### Full Control

```kotlin
// Specify source and target subscriptions with proration mode
AppPurchase.getInstance().changeSubscription(
    activity,
    "premium_monthly",    // Current subscription ID
    "premium_yearly",     // New subscription ID
    SubscriptionReplacementMode.CHARGE_PRORATED_PRICE
)
```

### Replacement Modes

| Mode | Best For | Effect |
|------|----------|--------|
| `CHARGE_PRORATED_PRICE` | Upgrades | User pays difference immediately |
| `DEFERRED` | Downgrades | Change at next renewal date |
| `WITH_TIME_PRORATION` | Either | Immediate, remaining value credited |
| `CHARGE_FULL_PRICE` | Either | Immediate, full price charged |
| `WITHOUT_PRORATION` | Either | Immediate, no price change |

### Example: Subscription Tier UI

```kotlin
class SubscriptionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentSub = AppPurchase.getInstance().getActiveSubscriptions().firstOrNull()

        if (currentSub == null) {
            // No subscription - show all options
            showAllSubscriptionOptions()
        } else {
            // Has subscription - show upgrade/downgrade options
            val currentId = currentSub.getFirstProductId()
            when (currentId) {
                "premium_basic" -> showUpgradeOptions()
                "premium_yearly" -> showDowngradeOptions()
                else -> showAllOptions()
            }
        }
    }

    fun onUpgradeClick() {
        AppPurchase.getInstance().changeSubscription(
            this,
            "premium_basic",
            "premium_yearly",
            SubscriptionReplacementMode.CHARGE_PRORATED_PRICE
        )
    }

    fun onDowngradeClick() {
        AppPurchase.getInstance().changeSubscription(
            this,
            "premium_yearly",
            "premium_basic",
            SubscriptionReplacementMode.DEFERRED
        )
    }
}
```

---

## Purchase State Checking

### Check If Ads Should Be Disabled

```kotlin
// Returns true for subscriptions, lifetime premium, or remove ads
if (AppPurchase.getInstance().isPurchased()) {
    hideAds()
}

// Same as isPurchased()
if (AppPurchase.getInstance().shouldDisableAds()) {
    hideAds()
}
```

### Get Active Purchase Type

```kotlin
when (AppPurchase.getInstance().getActivePurchaseType()) {
    PurchaseType.SUBSCRIPTION -> {
        showBadge("Subscriber")
    }
    PurchaseType.LIFETIME_PREMIUM -> {
        showBadge("Lifetime Member")
    }
    PurchaseType.REMOVE_ADS -> {
        // Just ad removal, no premium features
    }
    PurchaseType.NONE -> {
        showPurchasePrompt()
    }
}
```

### Check Specific Products

```kotlin
// Check if product is owned
if (AppPurchase.getInstance().isProductOwned("unlock_themes")) {
    enableThemes()
}

// Check lifetime purchases
if (AppPurchase.getInstance().hasLifetimePremium()) {
    enableAllFeatures()
}

if (AppPurchase.getInstance().hasRemoveAdsPurchase()) {
    hideAds()
}
```

---

## Purchase History Tracking

### Setting Up the Listener

```kotlin
AppPurchase.getInstance().setPurchaseHistoryListener(object : PurchaseHistoryListener {
    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        // Called when any purchase is acknowledged
        trackPurchase(productId, purchase)
    }

    override fun onPurchaseConsumed(productId: String, purchase: PurchaseResult) {
        // Called when consumePurchase() completes
        trackConsumption(productId, purchase)
    }
})
```

### PurchaseResult Data Available

```kotlin
override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
    // Basic info
    val orderId = purchase.orderId
    val quantity = purchase.quantity
    val purchaseTime = purchase.purchaseTime

    // Formatted helpers
    val dateStr = purchase.getPurchaseTimeFormatted("yyyy-MM-dd")
    val stateStr = purchase.getPurchaseStateString()

    // Verification data (for server)
    val json = purchase.originalJson
    val signature = purchase.signature

    // Account identifiers
    val accountId = purchase.obfuscatedAccountId
    val profileId = purchase.obfuscatedProfileId

    // Product type
    val isSubscription = purchase.isSubscription()
    val isInApp = purchase.isInApp()
}
```

### Persisting Purchase History

```kotlin
class MyPurchaseTracker : PurchaseHistoryListener {

    private val prefs: SharedPreferences

    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        val history = getPurchaseHistory(productId).toMutableList()
        history.add(PurchaseRecord(
            orderId = purchase.orderId ?: "",
            quantity = purchase.quantity,
            purchaseTime = purchase.purchaseTime,
            consumed = false
        ))
        savePurchaseHistory(productId, history)

        // Track total purchases
        val totalPurchases = prefs.getInt("total_$productId", 0)
        prefs.edit().putInt("total_$productId", totalPurchases + purchase.quantity).apply()
    }

    override fun onPurchaseConsumed(productId: String, purchase: PurchaseResult) {
        val history = getPurchaseHistory(productId).toMutableList()
        history.find { it.orderId == purchase.orderId }?.consumed = true
        savePurchaseHistory(productId, history)
    }

    fun getTotalPurchaseCount(productId: String): Int {
        return prefs.getInt("total_$productId", 0)
    }
}
```

---

## Server-Side Verification

### Get Verification Data

```kotlin
AppPurchase.getInstance().setPurchaseHistoryListener(object : PurchaseHistoryListener {
    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        if (purchase.hasVerificationData()) {
            // Send to your server
            verifyOnServer(
                productId = productId,
                purchaseToken = purchase.purchaseToken,
                originalJson = purchase.originalJson!!,
                signature = purchase.signature!!
            )
        }
    }
})
```

### Server Verification Flow

```kotlin
suspend fun verifyOnServer(
    productId: String,
    purchaseToken: String,
    originalJson: String,
    signature: String
) {
    val result = api.verifyPurchase(
        productId = productId,
        token = purchaseToken,
        receipt = originalJson,
        signature = signature
    )

    if (result.isValid) {
        // Grant entitlement
        grantProduct(productId)

        // Consume if needed
        if (isConsumable(productId)) {
            AppPurchase.getInstance().consumePurchase(productId)
        }
    } else {
        // Handle invalid purchase
        showError("Purchase verification failed")
    }
}
```

---

## Best Practices

### 1. Always Handle Edge Cases

```kotlin
AppPurchase.getInstance().setPurchaseListener(object : PurchaseListener {
    override fun onProductPurchased(orderId: String?, originalJson: String?) {
        // Success
    }

    override fun displayErrorMessage(errorMessage: String?) {
        when {
            errorMessage?.contains("ITEM_ALREADY_OWNED") == true -> {
                // Restore purchase
                AppPurchase.getInstance().verifyPurchased(true)
            }
            errorMessage?.contains("SERVICE_UNAVAILABLE") == true -> {
                // Retry later
                showRetryDialog()
            }
            else -> {
                showError(errorMessage)
            }
        }
    }

    override fun onUserCancelBilling() {
        // User cancelled - don't show error
    }
})
```

### 2. Restore Purchases on App Start

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verify purchases to restore state
        AppPurchase.getInstance().verifyPurchased(true)
    }
}
```

### 3. Use Correct Categories

```kotlin
// DON'T: Use wrong category
PurchaseItem("coins", TYPE_IAP.PURCHASE)  // Defaults to LIFETIME_PREMIUM!

// DO: Explicitly set category
PurchaseItem("coins", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE)
```

### 4. Handle Pending Purchases

```kotlin
override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
    if (purchase.isPending()) {
        // Don't grant yet - payment is pending
        showPendingMessage("Your purchase is being processed")
        return
    }

    if (purchase.isPurchased()) {
        // Safe to grant
        grantProduct(productId)
    }
}
```

### 5. Test with Google Play Console

Use test accounts and test products:
- License testers can make test purchases
- Use static response product IDs for specific scenarios
- Test all proration modes for subscriptions

---

## Troubleshooting

### "You already own this item"

The product wasn't consumed. Call `consumePurchase(productId)` after granting items.

### Subscription not showing as active

Call `verifyPurchased(true)` to refresh purchase state from Google.

### Purchase callback not firing

Ensure `setPurchaseListener()` is called before `purchase()`.

### Price not available

Products may not be loaded yet. Check `getPrice()` returns non-empty string before showing UI.

```kotlin
val price = AppPurchase.getInstance().getPrice("product_id")
if (price.isNotEmpty()) {
    button.text = "Buy for $price"
} else {
    button.text = "Buy"
}
```
