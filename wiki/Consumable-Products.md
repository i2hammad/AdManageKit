# Consumable Products

Consumable products are items that users can purchase multiple times, such as virtual currency, power-ups, or credits.

## Key Concept: Manual Consumption

In v2.9.0+, consumables are **NOT auto-consumed**. You must manually call `consumePurchase()` after granting items to the user.

## Why Manual Consumption?

1. **Server verification**: Verify on your server before granting items
2. **Error recovery**: If granting fails, you don't lose the purchase
3. **Accurate tracking**: Track exactly when items are granted
4. **Multi-quantity**: Handle quantity > 1 correctly

## Implementation

### 1. Define Consumable Products

```kotlin
val products = listOf(
    PurchaseItem("coins_100", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE),
    PurchaseItem("coins_500", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE),
    PurchaseItem("gems_50", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE)
)
```

### 2. Set Up Purchase History Listener

```kotlin
AppPurchase.getInstance().setPurchaseHistoryListener(object : PurchaseHistoryListener {
    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        // Called when purchase is acknowledged
        handlePurchase(productId, purchase)
    }

    override fun onPurchaseConsumed(productId: String, purchase: PurchaseResult) {
        // Called after consumePurchase() completes
        Log.d("Billing", "Consumed: $productId")
    }
})
```

### 3. Handle the Purchase

```kotlin
fun handlePurchase(productId: String, purchase: PurchaseResult) {
    // Check purchase state
    if (!purchase.isPurchased()) {
        // Pending payment - don't grant yet
        return
    }

    // Grant items based on product and quantity
    val quantity = purchase.quantity
    when (productId) {
        "coins_100" -> {
            userBalance.coins += 100 * quantity
        }
        "coins_500" -> {
            userBalance.coins += 500 * quantity
        }
        "gems_50" -> {
            userBalance.gems += 50 * quantity
        }
    }

    // Save user balance
    saveUserBalance()

    // NOW consume so user can buy again
    AppPurchase.getInstance().consumePurchase(productId)
}
```

## Purchase Flow

```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐     ┌─────────────┐
│   Purchase  │ ──► │  Acknowledge │ ──► │ onNewPurchase│ ──► │ Grant Items │
└─────────────┘     └──────────────┘     └──────────────┘     └──────┬──────┘
                                                                     │
                         ┌──────────────┐     ┌───────────────┐     │
                         │onPurchaseCon-│ ◄── │consumePurchase│ ◄───┘
                         │    sumed     │     └───────────────┘
                         └──────────────┘
```

## "You Already Own This Item" Error

If users see this error, the product wasn't consumed. Reasons:

1. App crashed before `consumePurchase()` was called
2. Developer forgot to call `consumePurchase()`
3. Old code using deprecated auto-consume

### Fix: Consume Pending Purchases

```kotlin
// On app start, check for unconsumed purchases
AppPurchase.getInstance().setPurchaseListener(object : PurchaseListener {
    override fun onProductPurchased(orderId: String?, originalJson: String?) {
        // This fires for unconsumed purchases too
        // Your PurchaseHistoryListener will handle it
    }
    // ...
})

// Verify purchases to trigger callbacks
AppPurchase.getInstance().verifyPurchased(true)
```

## Server-Side Verification

For important purchases, verify on your server:

```kotlin
override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
    if (purchase.hasVerificationData()) {
        // Send to server
        verifyPurchaseOnServer(
            productId = productId,
            token = purchase.purchaseToken,
            json = purchase.originalJson!!,
            signature = purchase.signature!!
        ) { isValid ->
            if (isValid) {
                grantItems(productId, purchase.quantity)
                AppPurchase.getInstance().consumePurchase(productId)
            } else {
                // Handle invalid purchase
            }
        }
    }
}
```

## Tracking Purchase Count

```kotlin
class PurchaseTracker : PurchaseHistoryListener {
    private val prefs: SharedPreferences

    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        // Increment total count
        val key = "total_purchased_$productId"
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + purchase.quantity).apply()

        // Grant and consume
        grantItems(productId, purchase.quantity)
        AppPurchase.getInstance().consumePurchase(productId)
    }

    fun getTotalPurchased(productId: String): Int {
        return prefs.getInt("total_purchased_$productId", 0)
    }
}
```

## Testing

1. Use Google Play Console license testers
2. Test with real products in closed testing
3. Test app crash scenarios (purchase acknowledged but not consumed)
4. Test multi-quantity purchases if supported
