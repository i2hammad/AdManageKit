# Release Notes - v3.2.0

## Highlights

- **Single-Activity App Support**: Screen and fragment tag-based exclusions for app open ads

---

## New Features

### Single-Activity App Support for App Open Ads

For apps using a single activity with multiple fragments (Navigation Component, Jetpack Compose Navigation), you can now control app open ads per screen:

#### Screen Tag Exclusions

```kotlin
// In Application class
appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy").apply {
    excludeScreenTags("Payment", "Onboarding", "Checkout")
}

// In MainActivity - track current screen on navigation
navController.addOnDestinationChangedListener { _, destination, _ ->
    (application as MyApp).appOpenManager.setCurrentScreenTag(
        destination.label?.toString()
    )
}
```

#### Fragment Tag Exclusions

```kotlin
// Set provider for automatic fragment detection
appOpenManager.setFragmentTagProvider {
    supportFragmentManager.fragments.lastOrNull()?.tag
}

// Exclude specific fragment tags
appOpenManager.excludeFragmentTags("PaymentFragment", "OnboardingFragment")
```

#### Temporary Disable/Enable

```kotlin
// Disable during critical flows
appOpenManager.disableAppOpenAdsTemporarily()

// ... perform operation ...

// Re-enable when done
appOpenManager.enableAppOpenAds()

// Check current state
if (appOpenManager.areAppOpenAdsEnabled()) { }
```

---

## New API Methods

### AppOpenManager

#### Screen Tag Exclusions
```kotlin
fun setCurrentScreenTag(tag: String?)
fun getCurrentScreenTag(): String?
fun excludeScreenTag(tag: String)
fun excludeScreenTags(vararg tags: String)
fun includeScreenTag(tag: String)
fun clearScreenTagExclusions()
```

#### Fragment Tag Exclusions
```kotlin
fun setFragmentTagProvider(provider: (() -> String?)?)
fun excludeFragmentTag(tag: String)
fun excludeFragmentTags(vararg tags: String)
fun includeFragmentTag(tag: String)
```

#### Temporary Control
```kotlin
fun disableAppOpenAdsTemporarily()
fun enableAppOpenAds()
fun areAppOpenAdsEnabled(): Boolean
```

---

## Migration Guide

### From v3.1.0 to v3.2.0

This is a **backward-compatible** release. No code changes are required.

**Optional**: If you have a single-activity app with multiple fragments, you can now use the new exclusion methods:

```kotlin
// Before (v3.1.0) - No way to exclude specific screens in single-activity apps

// After (v3.2.0) - Exclude specific screens
appOpenManager.excludeScreenTags("Payment", "Onboarding")
navController.addOnDestinationChangedListener { _, destination, _ ->
    appOpenManager.setCurrentScreenTag(destination.label?.toString())
}
```

---

## Full Changelog

- Added screen tag exclusions (`setCurrentScreenTag`, `excludeScreenTags`, `includeScreenTag`)
- Added fragment tag exclusions (`setFragmentTagProvider`, `excludeFragmentTags`, `includeFragmentTag`)
- Added temporary disable/enable (`disableAppOpenAdsTemporarily`, `enableAppOpenAds`, `areAppOpenAdsEnabled`)
- Updated `cleanup()` to clear new exclusion collections
