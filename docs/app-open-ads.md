# App Open Ads - AdManageKit v3.3.5

## Overview

AdManageKit provides lifecycle-aware app open ad management through the `AppOpenManager` class. App open ads display when users launch or return to your app, with full support for loading strategies, welcome dialogs, activity exclusion, and single-activity architecture.

**Library Version**: v3.3.5
**Last Updated**: January 2025

## What's New in v3.3.5

### Loading Strategy Support
- **Full AdLoadingStrategy Integration**: AppOpenManager now properly uses `appOpenLoadingStrategy` config
- **ON_DEMAND**: Fetches fresh ads with welcome dialog, uses cached if still fresh
- **ONLY_CACHE**: Only shows cached ads instantly, silently loads new if unavailable
- **HYBRID**: Shows cached if available, fetches with dialog otherwise (recommended)

### Ad Freshness Tracking
- **Load Time Tracking**: Cached ads now track when they were loaded
- **Freshness Threshold**: Configurable `appOpenAdFreshnessThreshold` (default: 4 hours)
- **Smart Cache Usage**: Prevents wasting pre-loaded ads while ensuring freshness

### Auto-Reload Configuration
- **New Setting**: `appOpenAutoReload` controls automatic reloading after dismissal
- **Default**: `true` - automatically loads next ad after current one is dismissed

### Deprecations
- **`appOpenFetchFreshAd`**: Deprecated in favor of `appOpenLoadingStrategy`
  - `appOpenFetchFreshAd = true` → `appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND`
  - `appOpenFetchFreshAd = false` → `appOpenLoadingStrategy = AdLoadingStrategy.HYBRID`

---

## Quick Start

### Basic Setup

```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        // Configure loading strategy
        AdManageKitConfig.apply {
            appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
            appOpenAutoReload = true
        }

        // Set up billing
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

        // Initialize app open manager
        appOpenManager = AppOpenManager(this, "ca-app-pub-xxx/yyy")
    }
}
```

That's it! Ads will **automatically load and show** when users open or return to your app.

---

## Loading Strategies

Configure via `AdManageKitConfig.appOpenLoadingStrategy`:

| Strategy | Behavior | Best For |
|----------|----------|----------|
| `ON_DEMAND` | Uses cached if fresh, otherwise fetches with welcome dialog | Maximum coverage |
| `ONLY_CACHE` | Only shows cached ads instantly, no waiting | Seamless UX |
| `HYBRID` | Shows cached if available, fetches with dialog if not | Balanced (recommended) |

### ON_DEMAND Strategy

```kotlin
AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND
```

**Behavior:**
1. If cached ad exists and is fresh (within `appOpenAdFreshnessThreshold`), shows it immediately
2. If cached ad is stale or doesn't exist, shows welcome dialog and fetches fresh ad
3. Shows ad when loaded, or continues without ad on timeout/failure

### ONLY_CACHE Strategy

```kotlin
AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
```

**Behavior:**
1. If cached ad exists, shows it immediately
2. If no cached ad, continues without showing (no waiting)
3. Silently loads new ad in background for next time

### HYBRID Strategy (Recommended)

```kotlin
AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
```

**Behavior:**
1. If cached ad exists, shows it immediately
2. If no cached ad, shows welcome dialog and fetches
3. Best balance of coverage and user experience

---

## Configuration

### All App Open Settings

```kotlin
AdManageKitConfig.apply {
    // Loading strategy (ON_DEMAND, ONLY_CACHE, HYBRID)
    appOpenLoadingStrategy = AdLoadingStrategy.HYBRID

    // Freshness threshold - cached ads older than this trigger fresh fetch
    appOpenAdFreshnessThreshold = 4.hours  // Google recommends max 4 hours

    // Auto-reload after ad dismissal
    appOpenAutoReload = true  // default: true

    // Timeout for ad loading
    appOpenAdTimeout = 4.seconds

    // Welcome dialog customization
    welcomeDialogAppIcon = R.mipmap.ic_launcher
    welcomeDialogTitle = "Welcome Back!"
    welcomeDialogSubtitle = "Loading your content..."
    welcomeDialogFooter = "Just a moment..."
    welcomeDialogDismissDelay = 0.8.seconds

    // Dialog colors
    dialogOverlayColor = 0x80000000.toInt()  // 50% black
    dialogCardBackgroundColor = Color.WHITE
}
```

---

## Usage

### Automatic Display

App open ads show automatically when the app comes to foreground:

```kotlin
// Initialize in Application.onCreate()
appOpenManager = AppOpenManager(this, "ca-app-pub-xxx/yyy")
// That's it! Ads show automatically
```

### Force Show

```kotlin
appOpenManager.forceShowAdIfAvailable(activity, object : AdManagerCallback() {
    override fun onNextAction() {
        // Ad dismissed or failed - continue with your flow
        navigateToMain()
    }
    override fun onAdLoaded() {
        // Ad was displayed
    }
})
```

### Preload Ad

```kotlin
// Optional: Preload for faster first show
appOpenManager.fetchAd()

// With callback
appOpenManager.fetchAd(object : AdLoadCallback() {
    override fun onAdLoaded() {
        Log.d("AppOpen", "Ad preloaded and ready")
    }
    override fun onFailedToLoad(error: AdError?) {
        Log.e("AppOpen", "Preload failed: ${error?.message}")
    }
})
```

### Prefetch Before External Intent

```kotlin
// Prefetch before launching external activity (camera, share, etc.)
appOpenManager.prefetchNextAd()
startActivityForResult(cameraIntent, REQUEST_CODE)
// Ad will be ready when user returns
```

### Check Status

```kotlin
// Check if ad is loaded and ready
if (appOpenManager.isAdAvailable()) {
    // Ad is ready to show
}

// Check if ad is currently loading
if (appOpenManager.isAdLoading()) {
    // Ad is being fetched
}

// Get cached ad age
val ageMs = appOpenManager.getCachedAdAgeMs()
if (ageMs > 0) {
    Log.d("AppOpen", "Cached ad is ${ageMs / 1000}s old")
}
```

### Skip Next Ad

```kotlin
// Skip the next automatic ad (e.g., after purchase or important action)
appOpenManager.skipNextAd()
```

---

## Activity/Screen Exclusion

### Exclude Activities

```kotlin
// Exclude specific activities from showing ads
appOpenManager.disableAppOpenWithActivity(SplashActivity::class.java)
appOpenManager.disableAppOpenWithActivity(PaymentActivity::class.java)

// Re-enable later if needed
appOpenManager.includeAppOpenActivityForAds(SplashActivity::class.java)
```

### Single-Activity Architecture (Fragments/Compose)

For apps with one activity and multiple screens:

```kotlin
// Set current screen tag when navigating
navController.addOnDestinationChangedListener { _, destination, _ ->
    appOpenManager.setCurrentScreenTag(destination.label?.toString())
}

// Exclude specific screens by tag
appOpenManager.excludeScreenTags("Payment", "Onboarding", "Checkout")

// Or use fragment tag provider
appOpenManager.setFragmentTagProvider {
    supportFragmentManager.fragments.lastOrNull()?.tag
}
appOpenManager.excludeFragmentTags("PaymentFragment", "OnboardingFragment")
```

### Temporary Disable

```kotlin
// Temporarily disable during critical flows
appOpenManager.disableAppOpenAdsTemporarily()
// ... perform operation ...
appOpenManager.enableAppOpenAds()
```

---

## Migration from appOpenFetchFreshAd

If you were using `appOpenFetchFreshAd`, migrate to `appOpenLoadingStrategy`:

```kotlin
// Before (deprecated)
AdManageKitConfig.appOpenFetchFreshAd = true  // Always fetch fresh

// After
AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND
```

```kotlin
// Before (deprecated)
AdManageKitConfig.appOpenFetchFreshAd = false  // Use cached when available

// After
AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
```

---

## API Reference

### AppOpenManager Methods

| Method | Description |
|--------|-------------|
| `fetchAd()` | Preload ad in background |
| `fetchAd(callback, timeout, customAdUnitId)` | Preload with options |
| `showAdIfAvailable()` | Show if cached (lifecycle triggered) |
| `forceShowAdIfAvailable(activity, callback)` | Force show with callback |
| `isAdAvailable()` | Check if ad is cached |
| `isAdLoading()` | Check if ad is being fetched |
| `getCachedAdAgeMs()` | Get age of cached ad in milliseconds |
| `prefetchNextAd()` | Prefetch before external intent |
| `skipNextAd()` | Skip next automatic ad |
| `disableAppOpenWithActivity(class)` | Exclude activity |
| `includeAppOpenActivityForAds(class)` | Re-include activity |
| `setCurrentScreenTag(tag)` | Set current screen for single-activity apps |
| `excludeScreenTags(vararg tags)` | Exclude screens by tag |
| `disableAppOpenAdsTemporarily()` | Temporarily disable |
| `enableAppOpenAds()` | Re-enable after temporary disable |

### AdManageKitConfig Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `appOpenLoadingStrategy` | Loading strategy | `HYBRID` |
| `appOpenAdFreshnessThreshold` | Max age for "fresh" cached ad | 4 hours |
| `appOpenAutoReload` | Auto-reload after dismissal | `true` |
| `appOpenAdTimeout` | Load timeout | 4 seconds |
| `welcomeDialogAppIcon` | App icon resource | 0 |
| `welcomeDialogTitle` | Dialog title | "Welcome Back!" |
| `welcomeDialogSubtitle` | Dialog subtitle | "Loading..." |
| `welcomeDialogFooter` | Dialog footer | "Just a moment..." |
| `welcomeDialogDismissDelay` | Delay before dismiss | 0.8 seconds |
| `dialogOverlayColor` | Overlay color | 50% black |
| `dialogCardBackgroundColor` | Card background | Theme default |

---

## Best Practices

1. **Initialize in Application** - Set up `AppOpenManager` in `Application.onCreate()`
2. **Use HYBRID Strategy** - Best balance of coverage and UX
3. **Set App Icon** - Configure `welcomeDialogAppIcon` for branded welcome dialog
4. **Exclude Sensitive Screens** - Payment, onboarding, splash screens
5. **Skip After Important Actions** - Call `skipNextAd()` after purchases
6. **Prefetch Before External Intents** - Use `prefetchNextAd()` before camera/share
7. **Wire Billing** - Ensure premium users don't see ads

---

## Troubleshooting

### Ad Not Showing

1. **Check availability**: `appOpenManager.isAdAvailable()`
2. **Check exclusion list**: Is current activity/screen excluded?
3. **Check purchase status**: `BillingConfig.getPurchaseProvider().isPurchased()`
4. **Check loading**: `appOpenManager.isAdLoading()`

### Stale Ads Showing

Reduce the freshness threshold:
```kotlin
AdManageKitConfig.appOpenAdFreshnessThreshold = 2.hours
```

### Ad Not Reloading

Check auto-reload setting:
```kotlin
AdManageKitConfig.appOpenAutoReload = true
```

### Strategy Not Working

Ensure you're on v3.3.5+ and using `appOpenLoadingStrategy` (not deprecated `appOpenFetchFreshAd`).

---

## Firebase Analytics Events

AppOpenManager automatically logs these events:

| Event | Description |
|-------|-------------|
| `ad_impression` | Ad was shown |
| `ad_paid_event` | Revenue tracking |
| `ad_failed_to_load` | Load failure with error code |
| `ad_request` | Ad request initiated |
| `ad_fill` | Ad successfully loaded |

---

## Changelog

### v3.3.5 (January 2025)
- Full `AdLoadingStrategy` support (ON_DEMAND, ONLY_CACHE, HYBRID)
- Ad freshness tracking with `appOpenAdFreshnessThreshold`
- Auto-reload configuration with `appOpenAutoReload`
- Deprecated `appOpenFetchFreshAd` in favor of `appOpenLoadingStrategy`
- Fixed auto-reload after ad dismissal

### v3.2.0
- Single-activity architecture support
- Screen/fragment tag exclusion
- Temporary disable/enable

### v3.0.0
- Ad prefetching with `prefetchNextAd()`
- `isAdLoading()` status check

### v2.8.0
- Welcome dialog with customization
- Loading strategy configuration

---

## References

- [AdMob App Open Ads](https://developers.google.com/admob/android/app-open-ads)
- [GitHub Repository](https://github.com/i2hammad/AdManageKit)
- [API Documentation](https://i2hammad.github.io/AdManageKit/)
