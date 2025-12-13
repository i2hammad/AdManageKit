# Release Notes - v3.2.0

## Highlights

- **Single-Activity App Support**: Screen and fragment tag-based exclusions for app open ads
- **Background-Aware Ad Display**: Ads no longer show when app is in background
- **Dialog Stability Fixes**: Prevents duplicate dialogs and threading issues
- **New onAdShowed() Callback**: Know when interstitial ad covers the screen

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

### Background-Aware Ad Display

App open ads now intelligently handle background/foreground transitions:

- **No Background Ads**: Ads won't try to show when app is in background
- **Pending Ad Queue**: If ad loads while app is in background, it's saved for when user returns
- **Welcome Dialog on Return**: When user returns, a welcome dialog appears briefly before showing the saved ad

```kotlin
// This is automatic - no code changes needed!
// When user switches apps during ad loading:
// 1. Ad loads in background -> saved as pending
// 2. Dialog dismisses automatically
// 3. User returns to app
// 4. Welcome dialog appears
// 5. Pending ad shows smoothly
```

### New onAdShowed() Callback

Know exactly when an interstitial ad is displayed on screen:

```kotlin
AdManager.getInstance().forceShowInterstitial(activity, object : AdManagerCallback() {
    override fun onAdShowed() {
        // Ad is now covering the screen
        // Pause game, mute audio, track analytics
        gameEngine.pause()
        audioManager.mute()
    }

    override fun onNextAction() {
        // Ad dismissed - resume normal operation
        gameEngine.resume()
        audioManager.unmute()
    }
})
```

---

## Bug Fixes

### Dialog Duplication Fix

**Issue**: When app was paused and resumed during ad loading dialog, a new dialog would appear without dismissing the old one, causing multiple overlapping dialogs.

**Fix**:
- Track current dialog instance to prevent duplicates
- Dismiss existing dialog before showing new one
- Added `isFetchingWithDialog` flag to prevent concurrent fetch requests

### Threading Issues Fix

**Issue**: Ad SDK callbacks could be called from background threads, causing crashes:
- `Animators may only be run on Looper threads`
- `Can't toast on a thread that has not called Looper.prepare()`

**Fix**: All ad callbacks in both `AdManager` and `AppOpenManager` now properly dispatch to main thread using `Handler(Looper.getMainLooper()).post { }`.

### Interstitial Priority Fix

**Issue**: App open ads would show on top of interstitial loading dialogs when user switched apps and came back.

**Fix**:
- `onStart` now checks `isAdOrDialogShowing()` instead of just `isDisplayingAd()`
- If interstitial dialog is showing, app open ad is skipped
- Pending app open ads are cleared when interstitial has priority

### Race Condition Fix

**Issue**: Multiple ads could show in rapid succession due to flag being set after `show()` instead of before.

**Fix**: `isShowingAd` flag is now set synchronously before calling `show()` to prevent concurrent show attempts.

---

## New API Methods

### AdManagerCallback

```kotlin
// NEW - Called when ad covers the full screen
open fun onAdShowed() { }
```

### AdManager

```kotlin
// NEW - Check if loading dialog is showing
fun isLoadingDialogShowing(): Boolean

// NEW - Check if ad OR loading dialog is showing
fun isAdOrDialogShowing(): Boolean
```

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

**Optional Enhancements**:

#### 1. Use onAdShowed() for better app state management

```kotlin
// Before (v3.1.0)
AdManager.getInstance().forceShowInterstitial(activity, object : AdManagerCallback() {
    override fun onNextAction() { navigateNext() }
})

// After (v3.2.0) - Know when ad is actually showing
AdManager.getInstance().forceShowInterstitial(activity, object : AdManagerCallback() {
    override fun onAdShowed() {
        pauseAppContent()  // Ad is now visible
    }
    override fun onNextAction() {
        resumeAppContent()
        navigateNext()
    }
})
```

#### 2. Single-Activity App Support

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

### New Features
- Added screen tag exclusions (`setCurrentScreenTag`, `excludeScreenTags`, `includeScreenTag`)
- Added fragment tag exclusions (`setFragmentTagProvider`, `excludeFragmentTags`, `includeFragmentTag`)
- Added temporary disable/enable (`disableAppOpenAdsTemporarily`, `enableAppOpenAds`, `areAppOpenAdsEnabled`)
- Added `onAdShowed()` callback to `AdManagerCallback`
- Added `isLoadingDialogShowing()` and `isAdOrDialogShowing()` to `AdManager`
- Added background/foreground detection for app open ads
- Added pending ad queue for ads loaded while in background

### Bug Fixes
- Fixed duplicate dialog display when app is paused/resumed during ad loading
- Fixed threading crashes ("Animators may only be run on Looper threads")
- Fixed app open ads showing on top of interstitial loading dialogs
- Fixed race condition allowing multiple concurrent ad shows
- Fixed `currentWelcomeDialog` not being cleared properly after dismissal

### Internal Improvements
- Added `currentLoadingDialog` tracking in `AdManager`
- Added `isFetchingWithDialog` flag to prevent duplicate fetch requests
- Added `isAppInForeground` tracking in `AppOpenManager`
- Added `onStop` lifecycle callback for background detection
- Wrapped all ad SDK callbacks in main thread handlers
- Updated `cleanup()` to clear new exclusion collections
