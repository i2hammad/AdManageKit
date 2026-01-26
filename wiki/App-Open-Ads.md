# App Open Ads - AdManageKit v2.8.0

## Overview

AdManageKit provides lifecycle-aware app open ad management through `AppOpenManager`. App open ads display when users launch or return to your app, with support for loading strategies, welcome dialogs, and activity exclusion.

**Library Version**: v2.8.0

## Features

- **Lifecycle-Aware**: Automatically shows ads when app moves to foreground
- **Loading Strategies**: ON_DEMAND, ONLY_CACHE, HYBRID support
- **Welcome Dialog**: Beautiful animated loading UI while fetching ads
- **Activity Exclusion**: Skip ads for specific activities
- **Purchase Check**: Automatically skips for premium users
- **Firebase Analytics**: Comprehensive event tracking

## Installation

```groovy
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.8.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.8.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.8.0'
}
```

## Configuration

### Basic Setup

```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        // Set up billing first
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

        // Configure loading strategy
        AdManageKitConfig.apply {
            appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
            appOpenAdTimeout = 4.seconds
            welcomeDialogAppIcon = R.mipmap.ic_launcher
        }

        // Initialize app open manager
        appOpenManager = AppOpenManager(this, "ca-app-pub-xxx/yyy")
    }
}
```

### Welcome Dialog Customization

```kotlin
AdManageKitConfig.apply {
    // App icon in welcome dialog
    welcomeDialogAppIcon = R.mipmap.ic_launcher

    // Custom texts
    welcomeDialogTitle = "Welcome Back!"
    welcomeDialogSubtitle = "Loading your content..."
    welcomeDialogFooter = "Just a moment..."

    // Colors
    dialogOverlayColor = 0x80000000.toInt()  // 50% black
    dialogCardBackgroundColor = Color.WHITE

    // Dismiss delay after ad shows
    welcomeDialogDismissDelay = 0.8.seconds
}
```

## Usage

### Automatic Display

App open ads show automatically when the app comes to foreground:

```kotlin
// Initialize in Application.onCreate()
appOpenManager = AppOpenManager(this, "ca-app-pub-xxx/yyy")
// That's it! Ads show automatically on app foreground
```

### Force Show

```kotlin
appOpenManager.forceShowAdIfAvailable(activity, object : AdManagerCallback() {
    override fun onNextAction() {
        // Continue with your flow
        navigateToMain()
    }
    override fun onAdLoaded() {
        // Ad was displayed
    }
})
```

### Excluding Activities

```kotlin
// Exclude splash screen from showing ads
appOpenManager.disableAppOpenWithActivity(SplashActivity::class.java)

// Re-enable later if needed
appOpenManager.includeAppOpenActivityForAds(SplashActivity::class.java)
```

### Skip Next Ad

```kotlin
// Skip the next ad (e.g., after returning from in-app purchase)
appOpenManager.skipNextAd()
```

### Check Availability

```kotlin
if (appOpenManager.isAdAvailable()) {
    // Ad is ready to show
}
```

## Loading Strategies

Configure via `AdManageKitConfig.appOpenLoadingStrategy`:

| Strategy | Behavior |
|----------|----------|
| ON_DEMAND | Always fetch fresh ad with welcome dialog |
| ONLY_CACHE | Only show if cached, skip otherwise |
| HYBRID | Show cached if ready, fetch with dialog otherwise |

```kotlin
// Set strategy
AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
```

## API Reference

### AppOpenManager Methods

| Method | Description |
|--------|-------------|
| `fetchAd()` | Preload ad in background |
| `fetchAd(callback, timeout)` | Preload with callback and timeout |
| `showAdIfAvailable()` | Show if cached (lifecycle triggered) |
| `forceShowAdIfAvailable(activity, callback)` | Force show with callback |
| `isAdAvailable()` | Check if ad is cached |
| `skipNextAd()` | Skip next ad display |
| `disableAppOpenWithActivity(class)` | Exclude activity |
| `includeAppOpenActivityForAds(class)` | Re-include activity |

### AdManageKitConfig Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `appOpenLoadingStrategy` | Loading strategy | HYBRID |
| `appOpenAdTimeout` | Load timeout | 4 seconds |
| `appOpenFetchFreshAd` | Disable background prefetch | false |
| `welcomeDialogAppIcon` | App icon resource | 0 |
| `welcomeDialogTitle` | Dialog title | "Welcome Back!" |
| `welcomeDialogSubtitle` | Dialog subtitle | "Loading..." |
| `welcomeDialogFooter` | Dialog footer | "Just a moment..." |
| `welcomeDialogDismissDelay` | Delay before dismiss | 0.8 seconds |
| `dialogOverlayColor` | Overlay color | 50% black |
| `dialogCardBackgroundColor` | Card background | Theme default |

## Best Practices

1. **Initialize in Application** - Set up `AppOpenManager` in `Application.onCreate()`
2. **Exclude Splash** - Use `disableAppOpenWithActivity(SplashActivity::class.java)`
3. **Use HYBRID** - Best balance of UX and coverage
4. **Set App Icon** - Configure `welcomeDialogAppIcon` for branded experience
5. **Wire Billing** - Ensure `BillingConfig` is set up to skip premium users

## Troubleshooting

- **Ad Not Showing**: Check `isAdAvailable()`, exclusion list, purchase status
- **Welcome Dialog Issues**: Verify `welcomeDialogAppIcon` is set
- **Strategy Not Working**: Ensure v2.8.0+ and check `appOpenLoadingStrategy`

## References

- [AdMob App Open Ads](https://developers.google.com/admob/android/app-open-ads)
- [GitHub Repository](https://github.com/i2hammad/AdManageKit)
