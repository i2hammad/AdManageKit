# Release Notes - v3.4.1

**Release Date**: February 25, 2026

## Overview

v3.4.1 adds product metadata and billing period APIs to `AppPurchase`, allowing you to display Play Console localized product titles, descriptions, trial info, and billing periods directly in your UI without manual translation.

---

## What's New

### Product Metadata APIs

New methods on `AppPurchase` expose the localized product information configured in the Google Play Console:

```kotlin
val billing = AppPurchase.getInstance()

// Title with app name appended (e.g. "Monthly Premium (My App)")
val title = billing.getProductTitle("premium_monthly")

// Clean name without app name (e.g. "Monthly Premium")
val name = billing.getProductName("premium_monthly")

// Description from Play Console
val description = billing.getProductDescription("premium_monthly")

// Raw ProductDetails for anything not covered above
val details = billing.getProductDetails("premium_monthly")
```

All methods check subscriptions first, then fall back to in-app products. Returns `null` if the product hasn't been loaded yet.

**Why this matters**: `getProductTitle()` and `getProductName()` return Google-localized strings based on the user's device language. You no longer need to maintain local translations for product names — set them once in Play Console, and the library returns the correct localization automatically.

---

### Free Trial Detection

Detect whether a subscription offers a free trial and retrieve its duration:

```kotlin
val billing = AppPurchase.getInstance()

if (billing.hasFreeTrial("premium_monthly")) {
    val trialPeriod = billing.getFreeTrialPeriod("premium_monthly")
    // trialPeriod = "P3D" (3 days), "P7D" (7 days), "P1M" (1 month), etc.
    showTrialBadge("Free trial: $trialPeriod")
}
```

Trial detection works by scanning offer phases for a zero-price phase with finite recurrence — matching Google's standard free trial offer structure.

---

### Billing Period

Get the subscription's billing cycle duration:

```kotlin
val period = AppPurchase.getInstance().getBillingPeriod("premium_monthly")
// "P1M" = monthly, "P1Y" = yearly, "P1W" = weekly, "P3M" = quarterly
```

Returns the billing period of the last pricing phase from the last subscription offer, which represents the regular billing cycle after any introductory phases.

---

## API Summary

| Method | Returns | Example |
|--------|---------|---------|
| `getProductTitle(productId)` | `String?` | `"Monthly Premium (My App)"` |
| `getProductName(productId)` | `String?` | `"Monthly Premium"` |
| `getProductDescription(productId)` | `String?` | `"Unlock all premium features"` |
| `getProductDetails(productId)` | `ProductDetails?` | Raw object |
| `hasFreeTrial(productId)` | `boolean` | `true` / `false` |
| `getFreeTrialPeriod(productId)` | `String?` | `"P7D"` |
| `getBillingPeriod(productId)` | `String?` | `"P1M"` |

---

## Example: Dynamic Paywall UI

```kotlin
val billing = AppPurchase.getInstance()

// Display product info from Play Console (auto-localized)
titleTextView.text = billing.getProductName("premium_monthly")
descriptionTextView.text = billing.getProductDescription("premium_monthly")
priceTextView.text = billing.getPrice("premium_monthly")

// Show trial badge if available
if (billing.hasFreeTrial("premium_monthly")) {
    trialBadge.visibility = View.VISIBLE
    trialBadge.text = "Free trial available"
}

// Show billing period
val period = billing.getBillingPeriod("premium_monthly")
periodTextView.text = when (period) {
    "P1M" -> "Billed monthly"
    "P1Y" -> "Billed yearly"
    "P1W" -> "Billed weekly"
    else -> ""
}
```

---

## Installation

```groovy
// Core modules
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.4.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.4.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.4.1'

// Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.4.1'

// Yandex provider (optional)
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.4.1'
```

---

## Full Changelog

- **New**: `AppPurchase.getProductTitle(productId)` — Play Console localized title with app name
- **New**: `AppPurchase.getProductName(productId)` — Clean product name without app name
- **New**: `AppPurchase.getProductDescription(productId)` — Play Console product description
- **New**: `AppPurchase.getProductDetails(productId)` — Raw `ProductDetails` object access
- **New**: `AppPurchase.hasFreeTrial(productId)` — Check if subscription has a free trial offer
- **New**: `AppPurchase.getFreeTrialPeriod(productId)` — Get trial period in ISO 8601 format
- **New**: `AppPurchase.getBillingPeriod(productId)` — Get subscription billing period
