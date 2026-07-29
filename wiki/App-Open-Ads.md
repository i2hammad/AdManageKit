# App Open Ads - AdManageKit v4.4.2

## Overview

AdManageKit provides lifecycle-aware app open ad management through `AppOpenManager`. App open ads display when users launch or return to your app, with support for loading strategies, ad freshness enforcement, welcome dialogs, activity/screen exclusion, and the multi-provider waterfall.

**Library Version**: v4.4.2

## Features

- **Lifecycle-Aware**: Automatically shows ads when the app moves to foreground
- **Loading Strategies**: `ON_DEMAND`, `ONLY_CACHE`, `HYBRID`, `FRESH_WITH_CACHE_FALLBACK`
- **Freshness Enforcement**: Cached ads older than `appOpenAdFreshnessThreshold` (4h) are discarded, never shown (v4.3.5)
- **Background Prefetch**: Loads the next ad when the app goes to background, so returns are instant
- **Welcome Dialog**: Animated loading UI while an ad is fetched
- **Activity / Screen / Fragment Exclusion**: Skip ads for splash screens, specific screens, or single-activity destinations
- **Late-Init Safe**: Every load path is guarded if `MobileAds.initialize()` hasn't finished (v4.3.0)
- **Multi-Provider Waterfall**: Falls back through a configured provider chain (e.g. AdMob → Yandex)
- **Purchase Check**: Automatically skips for premium users
- **Firebase Analytics**: `ad_impression`, `ad_paid_event`, `ad_failed_to_load` tracking

## Installation

```groovy
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.2'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.2'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.2'
}
```

## Basic Setup

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
            appOpenAdTimeout = 10.seconds
            welcomeDialogAppIcon = R.mipmap.ic_launcher
        }

        // Initialize the Google Mobile Ads SDK, then the app open manager
        Thread {
            MobileAds.initialize(this, InitializationConfig.Builder(APPLICATION_ID).build())
            Handler(Looper.getMainLooper()).post {
                appOpenManager = AppOpenManager(this, "ca-app-pub-xxx/yyy")
            }
        }.start()
    }
}
```

That's it — ads load and show automatically when users open or return to your app.

## Initialization Order

The Next-Gen Google Mobile Ads SDK **rejects ad requests made before `MobileAds.initialize()` completes** (the legacy SDK self-initialized on first use). Constructing `AppOpenManager` arms a lifecycle observer that fires as soon as the first activity starts, so a manager created while initialization runs on a background thread can race ahead of it.

**Recommended**: construct the manager after initialization returns, as in the setup above.

**Built-in protection (v4.3.0+)**: even if the manager is constructed early, it guards every load path instead of crashing — `showAdIfAvailable()` skips the show and defers a prefetch, `fetchAd()` parks and replays the prefetch, and the callback-taking `fetchAd(callback)` / `forceShowAdIfAvailable()` wait within their existing timeout budget and always deliver a terminal callback, so splash flows are never stranded.

```kotlin
if (appOpenManager.isMobileAdsReady()) {
    // SDK initialized — ads can be requested
}
```

`isMobileAdsReady()` = the SDK can *accept requests*; `isAdAvailable()` = an ad is *loaded and ready to show*.

## Loading Strategies

Configure via `AdManageKitConfig.appOpenLoadingStrategy`:

| Strategy | Behavior |
|----------|----------|
| `ON_DEMAND` | Uses a fresh cached ad, otherwise fetches with the welcome dialog |
| `ONLY_CACHE` | Shows only a fresh cached ad, skips otherwise (never waits) |
| `HYBRID` *(default)* | Shows cached if fresh, fetches with dialog if not |
| `FRESH_WITH_CACHE_FALLBACK` | Same show behavior as `HYBRID` |

```kotlin
AdManageKitConfig.appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
```

## Ad Freshness

Google recommends not caching an app open ad for more than 4 hours — a stale creative fills poorly and monetizes badly.

```kotlin
AdManageKitConfig.appOpenAdFreshnessThreshold = 4.hours  // default
```

A cached ad older than the threshold is **discarded and replaced**, never shown:

- `ON_DEMAND` / `HYBRID` — drop the stale ad and fetch a fresh one with the welcome dialog
- `ONLY_CACHE` — skip the show and silently prefetch a replacement for next time
- The background prefetch on `onStop` also refreshes a cached ad that has gone stale

Set `Duration.ZERO` to never use a cached ad (always fetch fresh).

```kotlin
appOpenManager.getCachedAdAgeMs()  // age of the currently cached ad
```

> **v4.3.5**: the threshold is now enforced by every strategy and on the multi-provider waterfall path. Earlier versions applied it only to `ON_DEMAND`, so `HYBRID`/`ONLY_CACHE` could show an arbitrarily old cached ad.

## Fetch Timing

```kotlin
AdManageKitConfig.appOpenFetchFreshAd = false  // default
```

`appOpenFetchFreshAd` controls **when** the ad is fetched — not whether a cached ad is shown (that's the strategy plus the freshness threshold):

- `false` *(default)* — prefetch when the app goes to background (`onStop`), so the ad is ready on return with no dialog
- `true` — no background prefetch; fetch on foreground when no usable cached ad exists, showing the loading dialog while it loads

## Welcome Dialog Customization

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

> `enableWelcomeBackDialog` is deprecated and has no effect — the dialog is always shown on fetch-with-dialog paths.

## Usage

### Automatic Display

```kotlin
// Initialize in Application.onCreate() — ads then show automatically on foreground
appOpenManager = AppOpenManager(this, "ca-app-pub-xxx/yyy")
```

### Force Show

```kotlin
appOpenManager.forceShowAdIfAvailable(activity, object : AdManagerCallback() {
    override fun onNextAction() {
        // Always fires on skip/failure/dismiss — gate navigation here
        navigateToMain()
    }
    override fun onAdLoaded() {
        // Ad was displayed
    }
})
```

`onNextAction()` fires exactly once on every path, so it's the safe place to continue your flow.

### Splash Screen Fetch

```kotlin
appOpenManager.fetchAd(object : AdLoadCallback() {
    override fun onAdLoaded() { showAdAndContinue() }
    override fun onFailedToLoad(error: AdKitError?) { continueToApp() }
}, timeoutMillis = 10_000)
```

The callback receives a terminal event even if `MobileAds` never initializes.

### Background Prefetch

```kotlin
// Warm the cache before a detour that will re-foreground the app
appOpenManager.prefetchNextAd { started ->
    // started == false when skipped (purchased user, ad already cached, or load in flight)
}
startActivityForResult(cameraIntent, REQUEST_CODE)

appOpenManager.isAdLoading()  // true while a prefetch is in flight
```

### Skip Next Ad

```kotlin
// Skip the next ad (e.g. after returning from an in-app purchase)
appOpenManager.skipNextAd()
```

### Excluding Activities

```kotlin
appOpenManager.disableAppOpenWithActivity(SplashActivity::class.java)
appOpenManager.includeAppOpenActivityForAds(SplashActivity::class.java)
```

### Excluding Screens (Single-Activity Apps)

```kotlin
// Tell the manager which screen is showing
appOpenManager.setCurrentScreenTag("checkout")

// Exclude screens by tag
appOpenManager.excludeScreenTags("checkout", "payment")
appOpenManager.includeScreenTag("checkout")
appOpenManager.clearScreenTagExclusions()
```

### Excluding Fragments

```kotlin
appOpenManager.setFragmentTagProvider {
    supportFragmentManager.fragments.lastOrNull()?.tag
}
appOpenManager.excludeFragmentTags("payment_sheet", "camera")
appOpenManager.includeFragmentTag("camera")
```

### Temporarily Disabling

```kotlin
appOpenManager.disableAppOpenAdsTemporarily()
appOpenManager.enableAppOpenAds()
appOpenManager.areAppOpenAdsEnabled()
```

### Diagnostics

```kotlin
appOpenManager.isAdAvailable()          // an ad is loaded and ready
appOpenManager.isAdLoading()            // a load is in flight
appOpenManager.getCachedAdAgeMs()       // age of the cached ad
appOpenManager.getPerformanceMetrics()  // load/show metrics map

when (val result = appOpenManager.canShowAd()) {
    is AppOpenManager.AdShowResult.CAN_SHOW -> { /* ready */ }
    is AppOpenManager.AdShowResult.CANNOT_SHOW -> Log.d(TAG, result.reason)
}
```

## Multi-Provider Waterfall

With an app open provider chain registered, `AppOpenManager` routes loads through it automatically:

```kotlin
AdProviderConfig.setAppOpenChain(listOf(AdMobAppOpenProvider(), YandexAppOpenProvider()))
```

Strategies, the freshness threshold, and the welcome dialog all behave the same on the waterfall path. See [[Multi-Provider Waterfall]] and [[Yandex Integration]].

## API Reference

### AppOpenManager Methods

| Method | Description |
|--------|-------------|
| `fetchAd()` | Preload ad in background |
| `fetchAd(callback, timeoutMillis?, customAdUnitId?)` | Preload with callback and timeout |
| `prefetchNextAd(onPrefetchStarted?)` | Prefetch, reporting whether it actually started |
| `showAdIfAvailable()` | Show if available (lifecycle triggered) |
| `forceShowAdIfAvailable(activity, callback)` | Force show with callback |
| `isAdAvailable()` | An ad is loaded and ready |
| `isAdLoading()` | A load is in flight |
| `isMobileAdsReady()` | The SDK can accept requests |
| `getCachedAdAgeMs()` | Age of the cached ad in ms |
| `canShowAd()` | `AdShowResult` with a reason when blocked |
| `getPerformanceMetrics()` | Load/show metrics |
| `skipNextAd()` | Skip next ad display |
| `disableAppOpenWithActivity(class)` | Exclude activity |
| `includeAppOpenActivityForAds(class)` | Re-include activity |
| `setCurrentScreenTag(tag)` / `getCurrentScreenTag()` | Current screen for single-activity apps |
| `excludeScreenTag(tag)` / `excludeScreenTags(vararg)` | Exclude screens |
| `includeScreenTag(tag)` / `clearScreenTagExclusions()` | Re-include screens |
| `setFragmentTagProvider(provider)` | Supply the current fragment tag |
| `excludeFragmentTag(tag)` / `excludeFragmentTags(vararg)` | Exclude fragments |
| `includeFragmentTag(tag)` | Re-include fragment |
| `disableAppOpenAdsTemporarily()` / `enableAppOpenAds()` | Temporary disable/enable |
| `areAppOpenAdsEnabled()` | Current enabled state |
| `cleanup()` | Release ads and observers |

> `setFrequencyCapping(maxShowsPerHour, maxShowsPerDay)` validates its arguments but does not yet enforce capping. `updateRetryConfiguration(...)` is deprecated — configure retries through `AdManageKitConfig`.

### AdManageKitConfig Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `appOpenLoadingStrategy` | Loading strategy | HYBRID |
| `appOpenAdFreshnessThreshold` | Max age of a usable cached ad | 4 hours |
| `appOpenAdTimeout` | Load timeout | 10 seconds |
| `appOpenFetchFreshAd` | Disable background prefetch, fetch on foreground | false |
| `appOpenAutoReload` | Reload after dismissal | true |
| `welcomeDialogAppIcon` | App icon resource | 0 |
| `welcomeDialogTitle` | Dialog title | "Welcome Back!" |
| `welcomeDialogSubtitle` | Dialog subtitle | "Loading..." |
| `welcomeDialogFooter` | Dialog footer | "Just a moment..." |
| `welcomeDialogDismissDelay` | Delay before dismiss | 0.8 seconds |
| `dialogBackgroundColor` | Dialog background | transparent |
| `dialogOverlayColor` | Overlay color | 50% black |
| `dialogCardBackgroundColor` | Card background | Theme default |

> `loadingDialogTitle` / `loadingDialogSubtitle` apply to the **interstitial** loading dialog, not app open — the app open dialog uses the `welcomeDialog*` texts.

## Best Practices

1. **Initialize after `MobileAds.initialize()`** - Construct `AppOpenManager` once the SDK is ready
2. **Exclude Splash** - `disableAppOpenWithActivity(SplashActivity::class.java)`
3. **Use HYBRID** - Best balance of UX and coverage
4. **Keep the threshold at 4 hours or less** - Matches Google's guidance and keeps fill healthy
5. **Gate navigation on `onNextAction()`** - It fires exactly once on every skip/failure/dismiss path
6. **Set App Icon** - Configure `welcomeDialogAppIcon` for a branded dialog
7. **Wire Billing** - Ensure `BillingConfig` is set up so premium users skip ads

## Troubleshooting

- **Ad Not Showing**: Check `canShowAd()` for the exact reason, plus exclusion lists and purchase status
- **Ad Showing Less Often After 4.3.5**: Expected — stale cached ads are now replaced rather than shown; the replacement is prefetched for the next foreground
- **Crash / No Requests on Cold Start**: `MobileAds.initialize()` hadn't finished — construct the manager after it returns, or check `isMobileAdsReady()`
- **Dialog Appears Too Briefly**: Raise `appOpenAdTimeout` (default 10 seconds); 4s or less often expires before a fresh ad loads
- **Welcome Dialog Issues**: Verify `welcomeDialogAppIcon` is set
- **Ads in Single-Activity App on Wrong Screen**: Use `setCurrentScreenTag()` + `excludeScreenTags()`

## References

- [AdMob App Open Ads](https://developers.google.com/admob/android/app-open-ads)
- [[Ad Loading Strategies]]
- [[Configuration]]
- [[Multi-Provider Waterfall]]
- [GitHub Repository](https://github.com/i2hammad/AdManageKit)
