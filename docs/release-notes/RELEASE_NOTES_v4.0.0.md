# Release Notes - v4.0.0

## Highlights

- **GMA Next-Gen SDK Migration**: Full migration to Google Mobile Ads Next-Gen SDK with preloader support
- **Background Thread Safety**: All ad callbacks now properly dispatch to main thread
- **Preloader System**: Efficient ad preloading for Interstitial, App Open, Native, Rewarded, and Banner ads
- **Single-Activity App Support**: Screen/fragment tag exclusions for app open ads
- **Preloader Configuration**: Centralized configuration for all preloader settings

---

## Breaking Changes

### GMA Next-Gen SDK Migration

This release migrates from the legacy Google Mobile Ads SDK to the **GMA Next-Gen SDK**. The import packages have changed:

```kotlin
// Before (Legacy SDK)
import com.google.android.gms.ads.*

// After (Next-Gen SDK)
import com.google.android.libraries.ads.mobile.sdk.*
```

**Note**: The public API remains largely compatible. Internal changes handle the new SDK's threading model automatically.

---

## New Features

### 1. Preloader System

All ad types now support the GMA Next-Gen SDK preloader system for efficient ad loading:

#### Interstitial Ads
```kotlin
// Preloader starts automatically when loading ads
AdManager.getInstance().loadInterstitialAd(context, adUnitId)

// Or manually control preloading
AdManager.getInstance().startPreloading(adUnitId)
```

#### App Open Ads
```kotlin
// Preloader enabled by default
appOpenManager = AppOpenManager(this, adUnitId)
appOpenManager.usePreloader = true // default

// Check preloaded ad availability
if (appOpenManager.isPreloadedAdAvailable()) {
    appOpenManager.showPreloadedAd(activity, callback)
}
```

#### Rewarded Ads
```kotlin
// Initialize starts preloading automatically
RewardedAdManager.initialize(context, adUnitId)

// Check availability
if (RewardedAdManager.isAdLoaded()) {
    RewardedAdManager.showAd(activity, rewardListener, dismissListener)
}
```

#### Banner Ads
```kotlin
// Start global preloading
BannerAdView.startGlobalPreloading(context, adUnitId)

// Load from preloader
bannerAdView.loadBannerFromPreloader(activity, adUnitId, callback)

// Check availability
if (BannerAdView.isPreloadedAdAvailable(adUnitId)) { }
```

#### Native Ads
```kotlin
// Preloading with NativeAdManager
NativeAdManager.startPreloading(adUnitId, context, callback)

// Check preloaded ad availability
if (NativeAdManager.isPreloadedAdAvailable(adUnitId)) { }
```

### 2. Preloader Configuration

New configuration options in `AdManageKitConfig`:

```kotlin
AdManageKitConfig.apply {
    // Enable/disable preloaders per ad type
    enableInterstitialPreloader = true   // default: true
    enableAppOpenPreloader = true        // default: true
    enableNativePreloader = true         // default: true
    enableRewardedPreloader = true       // default: true
    enableBannerPreloader = false        // default: false (opt-in)

    // Buffer sizes (how many ads to keep ready)
    interstitialPreloaderBufferSize = 2  // default: 2
    appOpenPreloaderBufferSize = 1       // default: 1
    nativePreloaderBufferSize = 3        // default: 3
    rewardedPreloaderBufferSize = 1      // default: 1
    bannerPreloaderBufferSize = 2        // default: 2
}
```

### 3. Screen/Fragment Tag Exclusions for App Open Ads

New exclusion system for single-activity apps with multiple fragments:

#### Screen Tag Exclusions
```kotlin
// Set current screen when navigating
navController.addOnDestinationChangedListener { _, destination, _ ->
    appOpenManager.setCurrentScreenTag(destination.label?.toString())
}

// Exclude specific screens
appOpenManager.excludeScreenTag("PaymentScreen")
appOpenManager.excludeScreenTags("Onboarding", "Checkout", "Settings")

// Re-include screen
appOpenManager.includeScreenTag("Settings")

// Clear all exclusions
appOpenManager.clearScreenTagExclusions()
```

#### Fragment Tag Exclusions
```kotlin
// Set fragment tag provider for automatic detection
appOpenManager.setFragmentTagProvider {
    supportFragmentManager.fragments.lastOrNull()?.tag
}

// Exclude specific fragment tags
appOpenManager.excludeFragmentTag("PaymentFragment")
appOpenManager.excludeFragmentTags("OnboardingFragment", "CheckoutFragment")
```

#### Temporary Disable/Enable
```kotlin
// Disable during critical flows
appOpenManager.disableAppOpenAdsTemporarily()

// ... perform sensitive operation ...

// Re-enable when done
appOpenManager.enableAppOpenAds()

// Check current state
if (appOpenManager.areAppOpenAdsEnabled()) { }
```

### 4. Background Thread Safety

The GMA Next-Gen SDK calls all ad callbacks on background threads (`GMA(BG)`). This release adds automatic main thread dispatch for all callbacks:

- **Interstitial**: `InterstitialAdEventCallback` callbacks now dispatch to main thread
- **App Open**: `AppOpenAdEventCallback` callbacks now dispatch to main thread
- **Rewarded**: `RewardedAdEventCallback` callbacks now dispatch to main thread
- **Banner**: `AdViewEventCallback` callbacks now dispatch to main thread
- **Native**: Preload callbacks dispatch user callbacks to main thread

**You don't need to change your code** - all user-facing callbacks are automatically called on the main thread.

---

## API Changes

### New Methods

#### AppOpenManager
```kotlin
// Screen tag exclusions
fun setCurrentScreenTag(tag: String?)
fun getCurrentScreenTag(): String?
fun excludeScreenTag(tag: String)
fun excludeScreenTags(vararg tags: String)
fun includeScreenTag(tag: String)
fun clearScreenTagExclusions()

// Fragment tag exclusions
fun setFragmentTagProvider(provider: (() -> String?)?)
fun excludeFragmentTag(tag: String)
fun excludeFragmentTags(vararg tags: String)
fun includeFragmentTag(tag: String)

// Temporary control
fun disableAppOpenAdsTemporarily()
fun enableAppOpenAds()
fun areAppOpenAdsEnabled(): Boolean

// Preloader
fun isPreloadedAdAvailable(): Boolean
fun pollPreloadedAd(): AppOpenAd?
fun showPreloadedAd(activity: Activity, callback: AdManagerCallback?): Boolean
```

#### BannerAdView
```kotlin
// Preloader support
fun startPreloading(adUnitId: String)
fun loadBannerFromPreloader(context: Activity?, adUnitId: String?, callback: AdLoadCallback?)
fun isPreloadedAdAvailable(): Boolean

// Static methods
companion object {
    fun startGlobalPreloading(context: Context, adUnitId: String)
    fun isPreloadedAdAvailable(adUnitId: String): Boolean
}
```

#### RewardedAdManager
```kotlin
// Completely rewritten with preloader support
fun initialize(context: Context, adUnitId: String)
fun startPreloading()
fun stopPreloading()
fun showAd(activity: Activity, onUserEarnedRewardListener: OnUserEarnedRewardListener, onAdDismissedListener: OnAdDismissedListener)
fun isAdLoaded(): Boolean
fun isPreloaderActive(): Boolean
```

### Deprecated Methods

```kotlin
// RewardedAdManager
@Deprecated("Use startPreloading() instead")
fun loadRewardedAd(context: Context)
```

---

## Migration Guide

### From v3.x to v4.0.0

#### 1. Update Dependencies

Update your build.gradle to use the new GMA SDK:

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.0.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.0.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.0.0'

// Ensure GMA Next-Gen SDK is included
implementation 'com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.0.0'
```

#### 2. Update RewardedAd Usage

```kotlin
// Before (v3.x)
RewardedAdManager.loadRewardedAd(context)

// After (v4.0.0)
RewardedAdManager.initialize(context, adUnitId)
// Preloading starts automatically
```

#### 3. Single-Activity Apps (Optional)

If you have a single-activity app and want to control app open ads per screen:

```kotlin
// In your Application class
appOpenManager = AppOpenManager(this, adUnitId).apply {
    // Exclude specific screens
    excludeScreenTags("Payment", "Onboarding", "Checkout")
}

// In your MainActivity
navController.addOnDestinationChangedListener { _, destination, _ ->
    (application as MyApp).appOpenManager.setCurrentScreenTag(destination.label?.toString())
}
```

#### 4. No Code Changes Required

Most existing code will work without changes:
- Ad loading APIs remain the same
- Callback interfaces remain the same
- Threading is handled automatically

---

## Bug Fixes

- **Fixed**: `CalledFromWrongThreadException` in native ad views when GMA SDK calls callbacks on background thread
- **Fixed**: `AndroidRuntimeException: Animators may only be run on Looper threads` in dialog dismissal
- **Fixed**: `NullPointerException: Can't toast on a thread that has not called Looper.prepare()` in app open ad callbacks
- **Fixed**: Threading issues in all ad type event callbacks

---

## Technical Details

### Threading Model

GMA Next-Gen SDK uses background threads for ad callbacks. AdManageKit v4.0.0 handles this automatically:

```kotlin
// Internal implementation pattern used across all ad types
override fun onAdDismissedFullScreenContent() {
    // Background thread (GMA SDK)
    mainHandler.post {
        // Main thread (safe for UI and user callbacks)
        callback?.onNextAction()
    }
}
```

### Preloader Architecture

The preloader system uses GMA's `*AdPreloader` classes:
- `InterstitialAdPreloader` - Preloads interstitial ads
- `AppOpenAdPreloader` - Preloads app open ads
- `NativeAdPreloader` - Preloads native ads
- `RewardedAdPreloader` - Preloads rewarded ads
- `BannerAdPreloader` - Preloads banner ads

Preloaders automatically:
- Load ads in the background
- Keep a buffer of ready ads
- Refill buffer after ads are consumed

---

## Dependencies

Updated dependencies for v4.0.0:

| Dependency | Version |
|------------|---------|
| GMA Next-Gen SDK | 1.0.0+ |
| AndroidX Lifecycle | 2.6.0+ |
| Firebase Analytics | 21.0.0+ |
| Google Play Billing | 7.0.0+ |

---

## Known Issues

- Preloaders run until app termination (SDK limitation - no stop method)
- Banner preloader is opt-in only (set `enableBannerPreloader = true` to enable)

---

## Full Changelog

### AdManageKit Module
- Migrated all ad types to GMA Next-Gen SDK
- Added preloader support for all ad formats
- Added background thread safety for all callbacks
- Added screen/fragment tag exclusions for app open ads
- Added preloader configuration options

### RewardedAdManager
- Complete rewrite using `RewardedAdPreloader`
- Added `initialize()` method
- Added `startPreloading()` / `stopPreloading()` methods
- Added `isPreloaderActive()` method

### BannerAdView
- Added `BannerAdPreloader` support
- Added static `startGlobalPreloading()` method
- Added `loadBannerFromPreloader()` method

### NativeAdManager
- Added main thread dispatch for preload callbacks
- Improved preloader callback handling

### AdManageKitConfig
- Added preloader enable/disable flags
- Added preloader buffer size configurations
