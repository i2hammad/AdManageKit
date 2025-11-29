# Subscription Upgrades & Downgrades

AdManageKit v2.9.0 provides full support for subscription tier changes with configurable proration modes.

## Quick Methods

### Upgrade

Upgrades use `CHARGE_PRORATED_PRICE` mode - user pays the price difference immediately.

```kotlin
// User is on "basic_monthly", upgrade to "premium_monthly"
AppPurchase.getInstance().upgradeSubscription(activity, "premium_monthly")
```

### Downgrade

Downgrades use `DEFERRED` mode - change takes effect at the next renewal date.

```kotlin
// User is on "premium_monthly", downgrade to "basic_monthly"
AppPurchase.getInstance().downgradeSubscription(activity, "basic_monthly")
```

## Full Control Method

For precise control over which subscription to replace and how:

```kotlin
AppPurchase.getInstance().changeSubscription(
    activity,
    "premium_monthly",                              // Current subscription ID
    "premium_yearly",                               // New subscription ID
    SubscriptionReplacementMode.CHARGE_PRORATED_PRICE  // Proration mode
)
```

## Replacement Modes

| Mode | Effect | Best For |
|------|--------|----------|
| `CHARGE_PRORATED_PRICE` | User pays price difference immediately | **Upgrades** |
| `DEFERRED` | Change at next renewal, no immediate charge | **Downgrades** |
| `WITH_TIME_PRORATION` | Immediate change, remaining value credited | Either |
| `CHARGE_FULL_PRICE` | Immediate change, full price charged | Revenue maximization |
| `WITHOUT_PRORATION` | Immediate change, billing date unchanged | Same-tier changes |

### Mode Details

#### CHARGE_PRORATED_PRICE (Recommended for Upgrades)

```kotlin
// User pays difference between old and new subscription
// Example: Monthly $5 → Yearly $50
// User pays: $50 - ($5 * remaining_days/30)
```

#### DEFERRED (Recommended for Downgrades)

```kotlin
// No immediate change - user keeps current subscription until renewal
// At renewal, switches to new (lower) subscription
// Good for user experience - they get what they paid for
```

#### WITH_TIME_PRORATION

```kotlin
// Immediate switch with time-based credit
// Remaining value of old subscription credited toward new
// Useful for same-price tier changes
```

#### CHARGE_FULL_PRICE

```kotlin
// Immediate switch, full price charged
// Old subscription remaining value lost
// Maximum immediate revenue
```

#### WITHOUT_PRORATION

```kotlin
// Immediate switch, no price adjustment
// Billing cycle remains the same
// Use for same-price tier switches
```

## Implementation Example

### Subscription Tier Screen

```kotlin
class SubscriptionTiersActivity : AppCompatActivity() {

    private val tiers = listOf(
        Tier("basic_monthly", "Basic", "$4.99/mo"),
        Tier("premium_monthly", "Premium", "$9.99/mo"),
        Tier("premium_yearly", "Premium Annual", "$99.99/yr")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tiers)

        val currentSub = AppPurchase.getInstance().getActiveSubscriptions().firstOrNull()
        val currentTierId = currentSub?.getFirstProductId()

        tiers.forEach { tier ->
            val button = createTierButton(tier)

            when {
                tier.id == currentTierId -> {
                    button.text = "Current Plan"
                    button.isEnabled = false
                }
                isUpgrade(currentTierId, tier.id) -> {
                    button.text = "Upgrade to ${tier.name}"
                    button.setOnClickListener { upgradeTo(tier.id) }
                }
                isDowngrade(currentTierId, tier.id) -> {
                    button.text = "Downgrade to ${tier.name}"
                    button.setOnClickListener { downgradeTo(tier.id) }
                }
                else -> {
                    button.text = "Subscribe to ${tier.name}"
                    button.setOnClickListener { subscribeTo(tier.id) }
                }
            }
        }
    }

    private fun upgradeTo(tierId: String) {
        AppPurchase.getInstance().upgradeSubscription(this, tierId)
    }

    private fun downgradeTo(tierId: String) {
        AppPurchase.getInstance().downgradeSubscription(this, tierId)
    }

    private fun subscribeTo(tierId: String) {
        AppPurchase.getInstance().subscribe(this, tierId)
    }

    private fun isUpgrade(current: String?, new: String): Boolean {
        val currentIndex = tiers.indexOfFirst { it.id == current }
        val newIndex = tiers.indexOfFirst { it.id == new }
        return currentIndex != -1 && newIndex > currentIndex
    }

    private fun isDowngrade(current: String?, new: String): Boolean {
        val currentIndex = tiers.indexOfFirst { it.id == current }
        val newIndex = tiers.indexOfFirst { it.id == new }
        return currentIndex != -1 && newIndex < currentIndex
    }
}
```

### Handle Result

The upgrade/downgrade uses the same purchase flow:

```kotlin
AppPurchase.getInstance().setPurchaseListener(object : PurchaseListener {
    override fun onProductPurchased(orderId: String?, originalJson: String?) {
        // Subscription changed successfully
        Toast.makeText(this, "Subscription updated!", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    override fun displayErrorMessage(errorMessage: String?) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onUserCancelBilling() {
        // User cancelled - no action needed
    }
})
```

## Low-Level API

For direct access to the purchase token:

```kotlin
val subscription = AppPurchase.getInstance().getSubscription("premium_monthly")
if (subscription != null) {
    AppPurchase.getInstance().updateSubscription(
        activity,
        "premium_yearly",                              // New subscription ID
        subscription.purchaseToken,                    // Current purchase token
        SubscriptionReplacementMode.WITH_TIME_PRORATION
    )
}
```

## Common Scenarios

### Monthly → Yearly (Same Tier)

```kotlin
// User wants annual billing for same features
AppPurchase.getInstance().changeSubscription(
    activity,
    "premium_monthly",
    "premium_yearly",
    SubscriptionReplacementMode.CHARGE_PRORATED_PRICE  // Pay difference for remaining month
)
```

### Yearly → Monthly (Same Tier)

```kotlin
// User wants monthly billing for same features
AppPurchase.getInstance().changeSubscription(
    activity,
    "premium_yearly",
    "premium_monthly",
    SubscriptionReplacementMode.DEFERRED  // Change at renewal
)
```

### Basic → Premium (Upgrade)

```kotlin
AppPurchase.getInstance().changeSubscription(
    activity,
    "basic_monthly",
    "premium_monthly",
    SubscriptionReplacementMode.CHARGE_PRORATED_PRICE
)
```

### Premium → Basic (Downgrade)

```kotlin
AppPurchase.getInstance().changeSubscription(
    activity,
    "premium_monthly",
    "basic_monthly",
    SubscriptionReplacementMode.DEFERRED
)
```

## Error Handling

```kotlin
val result = AppPurchase.getInstance().changeSubscription(
    activity,
    "current_sub",
    "new_sub",
    SubscriptionReplacementMode.CHARGE_PRORATED_PRICE
)

when {
    result == "OK" -> {
        // Billing flow started successfully
    }
    result.contains("not found") -> {
        // Subscription ID invalid or not loaded
        Log.e("Billing", "Invalid subscription ID")
    }
    result.contains("No active subscription") -> {
        // User doesn't have the source subscription
        // Fall back to regular subscribe
        AppPurchase.getInstance().subscribe(activity, "new_sub")
    }
}
```

## Best Practices

1. **Use DEFERRED for downgrades** - Users appreciate getting what they paid for
2. **Use CHARGE_PRORATED_PRICE for upgrades** - Immediate access, fair pricing
3. **Show clear pricing** - Display what user will pay before confirming
4. **Handle multiple subscriptions** - User might have multiple active subscriptions
5. **Test all scenarios** - Use Google Play Console's test tracks
