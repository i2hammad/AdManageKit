# Release Notes - v2.9.0

## Highlights

- **6 New Native Ad Templates**: APP_STORE, SOCIAL_FEED, GRADIENT_CARD, PILL_BANNER, SPOTLIGHT, MEDIA_CONTENT_SPLIT
- **AdChoices Placement Control**: Configure AdChoices position in NativeTemplateView
- **Enhanced Purchase Tracking**: Comprehensive purchase categorization system (Consumable, Lifetime, Remove Ads, Subscription)
- **Purchase History Listener**: Track purchases and consumption events for your own persistence
- **Subscription Lifecycle Management**: Active, Cancelled, Expired state detection with upgrade/downgrade support
- **Improved PurchaseResult**: Full data capture including signature, originalJson, account identifiers
- **No Auto-Consume**: Developers have full control over when to consume purchases
- **Dependency Updates**: Google Play Billing 8.1.0, AdMob 24.8.0, Firebase 34.6.0, Lifecycle 2.10.0

---

## New Features

### Native Ad Templates

#### 6 New Templates

| Template | Description | Best For |
|----------|-------------|----------|
| `APP_STORE` | App listing style with large icon and download button | App promotion |
| `SOCIAL_FEED` | Social media post style | Feed integration |
| `GRADIENT_CARD` | Modern card with gradient background | Premium feel |
| `PILL_BANNER` | Compact pill-shaped banner | Inline placement |
| `SPOTLIGHT` | Hero/spotlight style with emphasis | High visibility |
| `MEDIA_CONTENT_SPLIT` | Split layout with media and content | Balanced display |

```kotlin
// Use new templates in XML
<com.i2hammad.admanagekit.admob.NativeTemplateView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adTemplate="app_store" />

// Or programmatically
nativeTemplateView.setTemplate(NativeAdTemplate.SOCIAL_FEED)
```

#### AdChoices Placement Control

Configure AdChoices icon position:

```kotlin
// Programmatic control
nativeTemplateView.setAdChoicesPlacement(
    NativeAdOptions.ADCHOICES_TOP_LEFT,
    useSDKPlacement = true  // Use SDK auto-placement instead of XML position
)

// Or use template's XML-defined position (default)
nativeTemplateView.setUseCustomAdChoicesView(true)
```

**XML attribute:**
```xml
<com.i2hammad.admanagekit.admob.NativeTemplateView
    app:adChoicesPlacement="top_left" />
```

Options: `top_left`, `top_right` (default), `bottom_left`, `bottom_right`

---

### Billing Module

### 1. Purchase Categories

Products can now be categorized for better tracking and ad-disabling logic:

```kotlin
enum class PurchaseCategory {
    CONSUMABLE,       // Coins, gems (can buy multiple times)
    FEATURE_UNLOCK,   // Unlock level pack (one-time, doesn't disable ads)
    LIFETIME_PREMIUM, // Permanent premium (disables ads)
    REMOVE_ADS        // Remove ads only (disables ads)
}

// Usage
val items = listOf(
    PurchaseItem("coins_100", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE),
    PurchaseItem("remove_ads", TYPE_IAP.PURCHASE, PurchaseCategory.REMOVE_ADS),
    PurchaseItem("lifetime_premium", TYPE_IAP.PURCHASE, PurchaseCategory.LIFETIME_PREMIUM),
    PurchaseItem("premium_monthly", "free_trial", TYPE_IAP.SUBSCRIPTION)
)
```

### 2. Purchase History Listener

Track all purchase events for your own persistence/analytics:

```kotlin
AppPurchase.getInstance().setPurchaseHistoryListener(object : PurchaseHistoryListener {
    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        // Purchase acknowledged - save to your database
        // Includes: orderId, quantity, purchaseTime, originalJson, signature
        savePurchase(productId, purchase)

        // For consumables: grant items then consume
        if (productId == "coins_100") {
            userBalance += 100 * purchase.quantity
            AppPurchase.getInstance().consumePurchase(productId)
        }
    }

    override fun onPurchaseConsumed(productId: String, purchase: PurchaseResult) {
        // Mark as consumed in your tracking system
        markConsumed(productId, purchase.orderId)
    }
})
```

### 3. Subscription State Management

Detect subscription lifecycle states:

```kotlin
// Check subscription state
val state = AppPurchase.getInstance().getSubscriptionState("premium_monthly")
when (state) {
    SubscriptionState.ACTIVE -> showPremiumUI()
    SubscriptionState.CANCELLED -> showCancelledWarning() // Still has access
    SubscriptionState.EXPIRED -> showSubscribeButton()
}

// Helper methods
AppPurchase.getInstance().isSubscribed()           // Any active subscription
AppPurchase.getInstance().hasSubscriptionCancelled() // User cancelled
AppPurchase.getInstance().willSubscriptionsRenew()  // All will renew
```

### 4. Subscription Upgrade/Downgrade

Full support for subscription changes with proration modes:

```kotlin
// Simple upgrade (charges price difference immediately)
AppPurchase.getInstance().upgradeSubscription(activity, "premium_yearly")

// Simple downgrade (takes effect at next renewal)
AppPurchase.getInstance().downgradeSubscription(activity, "premium_basic")

// Full control with custom proration
AppPurchase.getInstance().changeSubscription(
    activity,
    "premium_monthly",    // Current subscription
    "premium_yearly",     // New subscription
    SubscriptionReplacementMode.CHARGE_PRORATED_PRICE
)
```

**Replacement Modes:**

| Mode | Use Case | Effect |
|------|----------|--------|
| `CHARGE_PRORATED_PRICE` | Upgrades | User pays price difference now |
| `DEFERRED` | Downgrades | Change at next renewal |
| `WITH_TIME_PRORATION` | Either | Immediate, remaining value credited |
| `CHARGE_FULL_PRICE` | Either | Immediate, full price charged |
| `WITHOUT_PRORATION` | Either | Immediate, no price adjustment |

### 5. Enhanced PurchaseResult

Comprehensive purchase data from Billing Library v8:

```kotlin
val result = PurchaseResult.fromPurchase(purchase)

// New fields
result.originalJson        // For server-side verification
result.signature           // For signature verification
result.obfuscatedAccountId // Account identifier
result.obfuscatedProfileId // Profile identifier
result.productType         // "inapp" or "subs"
result.isConsumed          // Consumption tracking
result.consumedTime        // When consumed

// Helper methods
result.isPurchased()                // PURCHASED state
result.isPending()                  // PENDING state
result.getFirstProductId()          // Primary product ID
result.getPurchaseTimeFormatted()   // "2024-01-15 14:30:00"
result.hasVerificationData()        // Has json + signature
result.isSubscriptionActive()       // Has access (active or cancelled)
result.willSubscriptionRenew()      // Will auto-renew
result.getSubscriptionStateString() // "Active", "Cancelled", etc.
```

### 6. Smart `isPurchased()` Logic

Only ad-disabling purchases affect `isPurchased()`:

```kotlin
// Returns true for:
// - Active subscriptions
// - LIFETIME_PREMIUM purchases
// - REMOVE_ADS purchases

// Returns false for:
// - Consumable purchases (coins, gems)
// - Feature unlocks (unless configured to disable ads)
// - No purchases
```

---

## API Changes

### NativeAdTemplate - New Templates

| Template | Layout | Shimmer |
|----------|--------|---------|
| `APP_STORE` | `layout_native_app_store` | `layout_shimmer_app_store` |
| `SOCIAL_FEED` | `layout_native_social_feed` | `layout_shimmer_social_feed` |
| `GRADIENT_CARD` | `layout_native_gradient_card` | `layout_shimmer_gradient_card` |
| `PILL_BANNER` | `layout_native_pill_banner` | `layout_shimmer_pill_banner` |
| `SPOTLIGHT` | `layout_native_spotlight` | `layout_shimmer_spotlight` |
| `MEDIA_CONTENT_SPLIT` | `layout_native_media_content_split` | `layout_shimmer_media_content_split` |

### NativeTemplateView - New Methods

| Method | Description |
|--------|-------------|
| `setAdChoicesPlacement(placement, useSDKPlacement)` | Set AdChoices icon position |
| `getAdChoicesPlacement()` | Get current AdChoices placement |
| `setUseCustomAdChoicesView(useCustomView)` | Toggle between template XML and SDK positioning |

### NativeTemplateView - New XML Attributes

| Attribute | Values | Default |
|-----------|--------|---------|
| `app:adChoicesPlacement` | `top_left`, `top_right`, `bottom_left`, `bottom_right` | `top_right` |

### AppPurchase - New Methods

| Method | Description |
|--------|-------------|
| `setPurchaseHistoryListener()` | Set listener for purchase events |
| `consumePurchase(productId)` | Manually consume a purchased product |
| `refreshPurchases()` | Refresh purchase state from Google |
| `isSubscribed(subscriptionId)` | Check specific subscription |
| `getSubscriptionState(subscriptionId)` | Get subscription lifecycle state |
| `getActiveSubscriptions()` | Get all active subscriptions |
| `hasSubscriptionCancelled()` | Check if any subscription cancelled |
| `willSubscriptionsRenew()` | Check if all subscriptions will renew |
| `getSubscription(subscriptionId)` | Get subscription PurchaseResult |
| `hasLifetimePurchase()` | Check for lifetime purchases |
| `hasLifetimePremium()` | Check for LIFETIME_PREMIUM category |
| `hasRemoveAdsPurchase()` | Check for REMOVE_ADS category |
| `getActivePurchaseType()` | Get type disabling ads |
| `getPurchaseCategory(productId)` | Get product's category |
| `isProductOwned(productId)` | Check if product is owned |
| `upgradeSubscription()` | Simple upgrade with default proration |
| `downgradeSubscription()` | Simple downgrade with deferred mode |
| `changeSubscription()` | Full upgrade/downgrade control |
| `updateSubscription()` | Low-level subscription update |

### AppPurchase - Deprecated Methods

| Method | Replacement |
|--------|-------------|
| `setConsumePurchase(boolean)` | Use `PurchaseItem.isConsumable` per-product |

### PurchaseItem - New Properties

| Property | Description |
|----------|-------------|
| `category` | PurchaseCategory enum |
| `isConsumable` | Whether product is consumable |

### PurchaseItem - New Methods

| Method | Description |
|--------|-------------|
| `shouldDisableAds()` | Check if purchase should disable ads |
| `isLifetimePurchase()` | Check if lifetime (not subscription) |

### PurchaseResult - New Fields

| Field | Description |
|-------|-------------|
| `originalJson` | Raw JSON for server verification |
| `signature` | Purchase signature |
| `obfuscatedAccountId` | Account identifier |
| `obfuscatedProfileId` | Profile identifier |
| `productType` | "inapp" or "subs" |
| `isConsumed` | Consumption status |
| `consumedTime` | Time of consumption |

### PurchaseResult - New Methods

| Method | Description |
|--------|-------------|
| `fromPurchase(Purchase)` | Factory method from Google Purchase |
| `isPurchased()` | Check if PURCHASED state |
| `isPending()` | Check if PENDING state |
| `getFirstProductId()` | Get primary product ID |
| `containsProduct(productId)` | Check if contains product |
| `getPurchaseTimeFormatted()` | Get formatted date string |
| `getPurchaseDate()` | Get Date object |
| `markAsConsumed()` | Mark as consumed |
| `hasVerificationData()` | Check for json + signature |
| `isSubscription()` | Check if subscription type |
| `isInApp()` | Check if in-app type |
| `getSubscriptionState()` | Get SubscriptionState enum |
| `isSubscriptionActive()` | Check if has access |
| `willSubscriptionRenew()` | Check if will renew |
| `isSubscriptionCancelled()` | Check if cancelled |
| `getSubscriptionStateString()` | Get human-readable state |

### New Enums

```kotlin
// Purchase type for ad-disabling detection
enum class PurchaseType {
    NONE, SUBSCRIPTION, LIFETIME_PREMIUM, REMOVE_ADS, LIFETIME, CONSUMABLE, UNKNOWN
}

// Subscription lifecycle states
enum class SubscriptionState {
    ACTIVE, CANCELLED, GRACE_PERIOD, ON_HOLD, PAUSED, EXPIRED, NOT_SUBSCRIPTION
}

// Subscription upgrade/downgrade modes
enum class SubscriptionReplacementMode {
    WITH_TIME_PRORATION, CHARGE_PRORATED_PRICE, CHARGE_FULL_PRICE, DEFERRED, WITHOUT_PRORATION
}
```

---

## Breaking Changes

### No Auto-Consume

Products are **no longer auto-consumed**. You must call `consumePurchase(productId)` manually after granting items to the user:

```kotlin
// Before v2.9.0: Products could be auto-consumed
AppPurchase.getInstance().setConsumePurchase(true)  // Deprecated

// After v2.9.0: Manual consumption required
AppPurchase.getInstance().setPurchaseHistoryListener(object : PurchaseHistoryListener {
    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        grantItems(productId, purchase.quantity)
        AppPurchase.getInstance().consumePurchase(productId)  // Manual!
    }
    override fun onPurchaseConsumed(productId: String, purchase: PurchaseResult) {}
})
```

### Default Category Change

`PurchaseItem` default category changed from `FEATURE_UNLOCK` to `LIFETIME_PREMIUM`:

```kotlin
// If you were relying on default category, explicitly set it
PurchaseItem("product_id", TYPE_IAP.PURCHASE, PurchaseCategory.FEATURE_UNLOCK)
```

---

## Migration Guide

### From 2.8.0 to 2.9.0

#### 1. Update Consumable Product Handling

```kotlin
// Old approach (auto-consume)
AppPurchase.getInstance().setConsumePurchase(true)

// New approach (manual consume)
AppPurchase.getInstance().setPurchaseHistoryListener(object : PurchaseHistoryListener {
    override fun onNewPurchase(productId: String, purchase: PurchaseResult) {
        when (productId) {
            "coins_100" -> {
                addCoins(100 * purchase.quantity)
                AppPurchase.getInstance().consumePurchase(productId)
            }
        }
    }
    override fun onPurchaseConsumed(productId: String, purchase: PurchaseResult) {
        Log.d("Billing", "Consumed: $productId")
    }
})
```

#### 2. Use Purchase Categories

```kotlin
// Old approach
val items = listOf(
    PurchaseItem("coins_100", TYPE_IAP.PURCHASE, true),  // isConsumable
    PurchaseItem("remove_ads", TYPE_IAP.PURCHASE)
)

// New approach (explicit categories)
val items = listOf(
    PurchaseItem("coins_100", TYPE_IAP.PURCHASE, PurchaseCategory.CONSUMABLE),
    PurchaseItem("remove_ads", TYPE_IAP.PURCHASE, PurchaseCategory.REMOVE_ADS),
    PurchaseItem("lifetime", TYPE_IAP.PURCHASE, PurchaseCategory.LIFETIME_PREMIUM)
)
```

#### 3. Check Purchase Type for UI

```kotlin
// Show appropriate UI based on purchase type
when (AppPurchase.getInstance().getActivePurchaseType()) {
    PurchaseType.SUBSCRIPTION -> showSubscriberBadge()
    PurchaseType.LIFETIME_PREMIUM -> showLifetimeBadge()
    PurchaseType.REMOVE_ADS -> hideAds()
    PurchaseType.NONE -> showPurchaseOptions()
    else -> {}
}
```

---

## Installation

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.9.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.9.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.9.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.9.0'
```

---

## Full Changelog

### Native Ad Module

- Added 6 new native ad templates: APP_STORE, SOCIAL_FEED, GRADIENT_CARD, PILL_BANNER, SPOTLIGHT, MEDIA_CONTENT_SPLIT
- Added shimmer loading layouts for all new templates
- Added AdChoices placement control in NativeTemplateView (`setAdChoicesPlacement()`)
- Added `adChoicesPlacement` XML attribute for declarative configuration
- Added `setUseCustomAdChoicesView()` for SDK vs template positioning control
- Added coroutine support in NativeTemplateView for async operations
- Updated screen type classification for new templates in NativeAdIntegrationManager
- Improved layout consistency across all native templates

### Billing Module Enhancements

- Added `PurchaseCategory` enum for product classification (CONSUMABLE, FEATURE_UNLOCK, LIFETIME_PREMIUM, REMOVE_ADS)
- Added `PurchaseHistoryListener` interface for tracking purchases and consumption
- Added `PurchaseResult.fromPurchase()` factory method with full data capture
- Added subscription state tracking (ACTIVE, CANCELLED, EXPIRED)
- Added subscription upgrade/downgrade with `SubscriptionReplacementMode`
- Added `refreshPurchases()` for manual purchase state refresh
- Enhanced `isPurchased()` to only consider ad-disabling purchases
- Enhanced `PurchaseResult` with originalJson, signature, account identifiers, consumption tracking
- Added helper methods: `hasLifetimePremium()`, `hasRemoveAdsPurchase()`, `getActivePurchaseType()`
- Added subscription methods: `getSubscriptionState()`, `hasSubscriptionCancelled()`, `willSubscriptionsRenew()`
- Removed auto-consume behavior - developers must call `consumePurchase()` manually
- Deprecated `setConsumePurchase()` in favor of per-product `isConsumable` flag

### Dependency Updates

| Dependency | Old Version | New Version |
|------------|-------------|-------------|
| Google Play Billing | 8.0.0 | 8.1.0 |
| Google Mobile Ads | 24.7.0 | 24.8.0 |
| Firebase BOM | 34.5.0 | 34.6.0 |
| Lifecycle (Process, ViewModel, Runtime) | 2.9.4 | 2.10.0 |
| Activity / Activity Compose | 1.11.0 | 1.12.0 |
| Compose BOM | 2025.10.01 | 2025.11.01 |

### Bug Fixes

- Fixed double callback issue in splash interstitial loading with timeout
- Fixed ad loading timeout now saves ad for next use instead of discarding
- Improved screen type detection for new native templates
- Fixed `IllegalArgumentException: View not attached to window manager` crash when dismissing dialogs after activity is destroyed (AppOpenManager, AdManager)
- Fixed welcome back dialog crash on activities without Material theme by wrapping context with `ContextThemeWrapper`
