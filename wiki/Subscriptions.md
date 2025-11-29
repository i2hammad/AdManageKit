# Subscriptions

AdManageKit provides comprehensive subscription management including state tracking, upgrade/downgrade support, and lifecycle management.

## Setting Up Subscriptions

### Define Subscription Products

```kotlin
val products = listOf(
    // Subscription with free trial offer
    PurchaseItem("premium_monthly", "free_trial_7d", TYPE_IAP.SUBSCRIPTION),

    // Subscription without trial
    PurchaseItem("premium_yearly", null, TYPE_IAP.SUBSCRIPTION),

    // Multiple tiers
    PurchaseItem("basic_monthly", null, TYPE_IAP.SUBSCRIPTION),
    PurchaseItem("pro_monthly", "intro_offer", TYPE_IAP.SUBSCRIPTION)
)
```

### Initialize

```kotlin
AppPurchase.getInstance().initBilling(application, products)
```

## Subscribing

```kotlin
// Start subscription flow
AppPurchase.getInstance().subscribe(activity, "premium_monthly")
```

## Subscription States

### State Enum

```kotlin
enum class SubscriptionState {
    ACTIVE,          // Subscribed and will renew
    CANCELLED,       // Cancelled but still has access
    GRACE_PERIOD,    // Payment issue, still has access (server-side only)
    ON_HOLD,         // Payment issue, no access (server-side only)
    PAUSED,          // User paused (server-side only)
    EXPIRED,         // Subscription ended
    NOT_SUBSCRIPTION // Product is not a subscription
}
```

### Client-Side Detection

| State | Detectable Client-Side | How |
|-------|----------------------|-----|
| ACTIVE | Yes | `isAutoRenewing = true` |
| CANCELLED | Yes | `isAutoRenewing = false`, still in purchase list |
| GRACE_PERIOD | No | Requires server API |
| ON_HOLD | No | Requires server API |
| PAUSED | No | Requires server API |
| EXPIRED | Yes | Not returned by `queryPurchasesAsync` |

## Checking Subscription Status

### Basic Checks

```kotlin
// Any subscription active?
if (AppPurchase.getInstance().isSubscribed()) {
    showPremiumContent()
}

// Specific subscription active?
if (AppPurchase.getInstance().isSubscribed("premium_yearly")) {
    showYearlyBenefits()
}
```

### Get Subscription State

```kotlin
val state = AppPurchase.getInstance().getSubscriptionState("premium_monthly")

when (state) {
    SubscriptionState.ACTIVE -> {
        // User is subscribed and will renew
        showPremiumUI()
    }
    SubscriptionState.CANCELLED -> {
        // User cancelled but still has access until expiration
        showRenewalPrompt()
    }
    SubscriptionState.EXPIRED -> {
        // Subscription ended
        showSubscribeButton()
    }
    else -> {
        // Not subscribed or other state
    }
}
```

### Additional Checks

```kotlin
// Check if any subscription is cancelled (but still active)
if (AppPurchase.getInstance().hasSubscriptionCancelled()) {
    showResubscribePrompt()
}

// Check if all subscriptions will renew
if (AppPurchase.getInstance().willSubscriptionsRenew()) {
    // User is committed subscriber
}

// Get all active subscriptions
val subscriptions = AppPurchase.getInstance().getActiveSubscriptions()
for (sub in subscriptions) {
    println("${sub.getFirstProductId()}: ${sub.getSubscriptionStateString()}")
}
```

## Subscription Details

### Get Subscription PurchaseResult

```kotlin
val subscription = AppPurchase.getInstance().getSubscription("premium_monthly")
if (subscription != null) {
    // Basic info
    val orderId = subscription.orderId
    val purchaseTime = subscription.getPurchaseTimeFormatted()

    // State info
    val isActive = subscription.isSubscriptionActive()
    val willRenew = subscription.willSubscriptionRenew()
    val isCancelled = subscription.isSubscriptionCancelled()
    val stateString = subscription.getSubscriptionStateString()

    // Display
    showSubscriptionInfo(
        status = stateString,      // "Active", "Cancelled (access until expiration)"
        since = purchaseTime,      // "2024-01-15 14:30:00"
        willRenew = willRenew      // true/false
    )
}
```

## Subscription Pricing

```kotlin
// Get formatted price
val monthlyPrice = AppPurchase.getInstance().getPriceSub("premium_monthly")
// "$9.99"

val yearlyPrice = AppPurchase.getInstance().getPriceSub("premium_yearly")
// "$99.99"
```

## Trial Offers

Specify trial offer ID when creating PurchaseItem:

```kotlin
// The second parameter is the offer ID from Google Play Console
PurchaseItem("premium_monthly", "free_trial_7d", TYPE_IAP.SUBSCRIPTION)
```

The library automatically selects the matching offer when subscribing.

## Handling Subscription Changes

See [[Subscription Upgrades]] for upgrade/downgrade handling.

## Best Practices

### 1. Always Show Subscription State

```kotlin
fun updateSubscriptionUI() {
    val state = AppPurchase.getInstance().getSubscriptionState("premium_monthly")

    subscriptionBadge.text = when (state) {
        SubscriptionState.ACTIVE -> "Premium Member"
        SubscriptionState.CANCELLED -> "Premium (Expires Soon)"
        else -> "Free User"
    }
}
```

### 2. Prompt Cancelled Users to Resubscribe

```kotlin
if (AppPurchase.getInstance().hasSubscriptionCancelled()) {
    showDialog(
        title = "Your subscription is ending",
        message = "Renew now to keep premium features",
        action = "Renew" to { reopenSubscription() }
    )
}

fun reopenSubscription() {
    // This will show the resubscribe flow in Google Play
    AppPurchase.getInstance().subscribe(activity, "premium_monthly")
}
```

### 3. Restore Purchases on App Start

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Verify purchases to restore subscription state
    AppPurchase.getInstance().verifyPurchased(true)
}
```

### 4. Handle Grace Period (Server-Side)

For grace period detection, implement server-side verification using Google Play Developer API and Real-time Developer Notifications (RTDN).
