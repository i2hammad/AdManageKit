# Release Notes - v3.3.4

## Highlights

- **Subscription Expiry Verification**: Server-side verification API to get accurate subscription expiry dates
- **Expiry Time Methods**: New methods to check remaining days, formatted expiry dates, and expiration status

---

## New Features

### Subscription Expiry Verification

Google Play Billing Library does NOT provide subscription expiry dates client-side. This release adds a server-side verification API to get accurate expiry information.

#### Setting Up Verification Callback

```kotlin
// In Application.onCreate()
AppPurchase.getInstance().setSubscriptionVerificationCallback { packageName, subscriptionId, purchaseToken, listener ->
    // Call your backend API
    yourApi.verifySubscription(packageName, subscriptionId, purchaseToken,
        onSuccess = { expiryTimeMillis ->
            val details = SubscriptionVerificationCallback.SubscriptionDetails.Builder()
                .setExpiryTimeMillis(expiryTimeMillis)
                .setAutoRenewing(true)
                .build()
            listener.onVerified(details)
        },
        onError = { error ->
            listener.onVerificationFailed(error)
        }
    )
}
```

#### Verifying Subscriptions

```kotlin
// Verify single subscription
AppPurchase.getInstance().verifySubscription("premium_monthly",
    object : AppPurchase.SubscriptionVerificationListener {
        override fun onVerified(subscription: PurchaseResult) {
            val expiryDate = subscription.getExpiryTimeFormatted("dd MMM yyyy")
            val daysLeft = subscription.getRemainingDays()
            val isExpired = subscription.isExpired()
        }
        override fun onVerificationFailed(errorMessage: String?) { }
    }
)

// Verify all active subscriptions
AppPurchase.getInstance().verifyAllSubscriptions(listener)
```

#### Accessing Expiry Data

After verification, expiry data is cached and accessible:

```kotlin
// Via AppPurchase
val expiryMillis = AppPurchase.getInstance().getSubscriptionExpiryTime("premium_monthly")
val expiryFormatted = AppPurchase.getInstance().getSubscriptionExpiryTimeFormatted("premium_monthly")
val daysLeft = AppPurchase.getInstance().getSubscriptionRemainingDays("premium_monthly")
val isExpired = AppPurchase.getInstance().isSubscriptionExpired("premium_monthly")

// Via PurchaseResult
val subscription = AppPurchase.getInstance().getSubscription("premium_monthly")
if (subscription?.isExpiryVerified() == true) {
    val expiryDate = subscription.getExpiryDate()           // Date object
    val remainingTime = subscription.getRemainingTime()     // Milliseconds
    val remainingDays = subscription.getRemainingDays()     // Days
}
```

### New PurchaseResult Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getExpiryTime()` | `Long` | Expiry time in milliseconds |
| `getExpiryDate()` | `Date?` | Expiry as Date object |
| `getExpiryTimeFormatted()` | `String` | Formatted expiry date |
| `getExpiryTimeFormatted(pattern)` | `String` | Custom format pattern |
| `isExpiryVerified()` | `Boolean` | Whether expiry has been verified |
| `isExpired()` | `Boolean` | Whether subscription has expired |
| `getRemainingTime()` | `Long` | Milliseconds until expiry |
| `getRemainingDays()` | `Int` | Days until expiry |

### New AppPurchase Methods

| Method | Description |
|--------|-------------|
| `setSubscriptionVerificationCallback()` | Set callback for server verification |
| `verifySubscription(id, listener)` | Verify single subscription |
| `verifyAllSubscriptions(listener)` | Verify all active subscriptions |
| `getSubscriptionExpiryTime(id)` | Get expiry time in milliseconds |
| `getSubscriptionExpiryTimeFormatted(id)` | Get formatted expiry date |
| `getSubscriptionRemainingDays(id)` | Get remaining days |
| `isSubscriptionExpired(id)` | Check if subscription expired |

---

## Backend Implementation

Your backend should call Google Play Developer API:

```
GET https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{packageName}/purchases/subscriptionsv2/tokens/{purchaseToken}
```

Response includes:
- `expiryTime` - RFC 3339 timestamp
- `subscriptionState` - ACTIVE, CANCELED, IN_GRACE_PERIOD, ON_HOLD, PAUSED, EXPIRED
- `linkedPurchaseToken` - For upgrade/downgrade tracking

---

## Migration Guide

### From v3.3.3 to v3.3.4

This is a **backward-compatible** release. No code changes required.

**Optional**: To use subscription expiry verification:

1. Implement `SubscriptionVerificationCallback` with your backend API
2. Call `verifySubscription()` or `verifyAllSubscriptions()` after billing initialization
3. Access expiry data via `PurchaseResult` or `AppPurchase` helper methods

---

## Full Changelog

### New Features
- Added `SubscriptionVerificationCallback` interface for server-side verification
- Added expiry time fields and methods to `PurchaseResult`
- Added `verifySubscription()` and `verifyAllSubscriptions()` to `AppPurchase`
- Added expiry helper methods: `getSubscriptionExpiryTime()`, `getSubscriptionRemainingDays()`, etc.

### Documentation
- Updated wiki/Subscriptions.md with expiry verification guide
- Updated docs/APP_PURCHASE_GUIDE.md with verification section
