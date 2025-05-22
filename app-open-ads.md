# App Open Ads - AdManageKit v1.3.2

## Overview
The `AdManageKit` library (version `v1.3.2`) provides lifecycle-aware management of app open ads through the `AppOpenManager` class in the `com.i2hammad.admanagekit.admob` package. App open ads are full-screen ads displayed when users launch or return to your app, ideal for monetizing app entry points. The `AppOpenManager` handles ad loading, display, and activity exclusion, with Firebase Analytics integration for tracking ad events.

**Library Version**: v1.3.2  
**Last Updated**: May 22, 2025

## Features
- **Lifecycle-Aware Ads**: Automatically shows app open ads when the app moves to the foreground (via `ProcessLifecycleOwner`).
- **Flexible Display**:
    - Automatic display on app start.
    - Forced display for specific activities.
    - Option to skip ads or exclude activities.
- **Purchase Check**: Skips ad display if the user has purchased the app (via `BillingConfig`).
- **Firebase Analytics**: Logs ad impressions, paid events, and failures.
- **Timeout Support**: Configurable timeout for ad loading to prevent delays.

## Components
### AppOpenManager
The `AppOpenManager` class manages app open ads:
- **Key Methods**:
    - `fetchAd()`: Loads an app open ad.
    - `fetchAd(adLoadCallback: AdLoadCallback, timeoutMillis: Long)`: Loads an ad with a timeout and callback.
    - `showAdIfAvailable()`: Displays an ad if available and not excluded.
    - `forceShowAdIfAvailable(activity: Activity, adManagerCallback: AdManagerCallback)`: Forces ad display for a specific activity.
    - `skipNextAd()`: Skips the next ad display.
    - `disableAppOpenWithActivity(activityClass: Class<*>)`: Excludes an activity from showing ads.
    - `includeAppOpenActivityForAds(activityClass: Class<*>)`: Re-enables an activity for ads.
    - `isAdAvailable(): Boolean`: Checks if an ad is loaded.
- **Configuration**:
    - Tracks ad display state (`isShowingAd`, `isShownAd`).
    - Maintains a set of excluded activities (`excludedActivities`).
    - Default timeout: 5000ms for ad loading.
- **Callbacks**:
    - Uses `AdLoadCallback` for load success/failure.
    - Uses `AdManagerCallback` for display events (`onNextAction`, `onAdLoaded`).

## Usage
### Integration
Add `AdManageKit v1.3.2` to your project via Gradle:

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:1.3.2'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:1.3.2'
```

Ensure dependencies are included:
- Google AdMob SDK
- Firebase Analytics
- AndroidX Lifecycle
- Project resources (`BillingConfig`)

### Initializing AppOpenManager
Initialize in your `Application` class:

```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
        appOpenManager = AppOpenManager(this, "ca-app-pub-3940256099942544/9257395921")
    }
}
```

### Loading App Open Ads
Load an ad:

```kotlin
appOpenManager.fetchAd()
```

Load with a callback and 5-second timeout:

```kotlin
appOpenManager.fetchAd(object : AdLoadCallback() {
    override fun onAdLoaded() {
        Log.d("AppOpenManager", "App open ad loaded")
    }
    override fun onFailedToLoad(error: AdError?) {
        Log.e("AppOpenManager", "Failed to load: ${error?.message}")
    }
}, timeoutMillis = 5000)
```

### Displaying App Open Ads
#### Automatic Display
App open ads are shown automatically when the app moves to the foreground (via `onStart`), unless the user has purchased the app or the activity is excluded.

#### Forced Display
Force show an ad for a specific activity:

```kotlin
appOpenManager.forceShowAdIfAvailable(this, object : AdManagerCallback() {
    override fun onNextAction() {
        Log.d("AppOpenManager", "Ad dismissed or failed")
    }
    override fun onAdLoaded() {
        Log.d("AppOpenManager", "Ad displayed")
    }
})
```

#### Skipping Ads
Skip the next ad display:

```kotlin
appOpenManager.skipNextAd()
```

#### Excluding Activities
Prevent ads from showing in specific activities:

```kotlin
appOpenManager.disableAppOpenWithActivity(MainActivity::class.java)
```

Re-enable an activity:

```kotlin
appOpenManager.includeAppOpenActivityForAds(MainActivity::class.java)
```

### Checking Ad Availability
Verify if an ad is ready:

```kotlin
if (appOpenManager.isAdAvailable()) {
    Log.d("AppOpenManager", "App open ad is ready")
} else {
    Log.d("AppOpenManager", "No app open ad available")
}
```

## Implementation Details
### Ad Loading Workflow
1. **Loading**:
    - `fetchAd` initiates an ad request using `AppOpenAd.load`.
    - The ad is cached in `appOpenAd` if loaded successfully.
    - A timeout (default: 5000ms) ensures the callback is triggered if loading takes too long.
2. **Error Handling**:
    - Failed loads are logged to Firebase Analytics with the ad unit ID and error code.
    - Timeout failures return a custom `LoadAdError` (code 3).

### Ad Display Workflow
1. **Display Check**:
    - `isAdAvailable()` verifies a loaded ad exists.
    - Ads are skipped if:
        - The user has purchased the app (`BillingConfig.getPurchaseProvider().isPurchased()`).
        - The current activity is in `excludedActivities`.
        - Another ad is showing (`isShowingAd` or `AdManager.isDisplayingAd()`).
        - `skipNextAd` is `true`.
2. **Lifecycle Integration**:
    - `AppOpenManager` observes `ProcessLifecycleOwner` to show ads on app foreground (`onStart`).
    - Activity lifecycle callbacks track the current activity (`currentActivity`).
3. **Ad Events**:
    - `FullScreenContentCallback` handles ad show, dismissal, and failure.
    - `OnPaidEventListener` logs revenue data to Firebase Analytics.
    - Analytics events include impressions (`AD_IMPRESSION`) and failures (`ad_failed_to_load`).
4. **Reload**:
    - A new ad is fetched after display or failure via `fetchAd`.

### Key Code Snippet
#### AppOpenManager
```kotlin
fun fetchAd(adLoadCallback: AdLoadCallback, timeoutMillis: Long = 5000) {
    if (isAdAvailable()) {
        adLoadCallback.onAdLoaded()
        return
    }
    val request = getAdRequest()
    val timeoutHandler = Handler(Looper.getMainLooper())
    var hasTimedOut = false
    val timeoutRunnable = Runnable {
        hasTimedOut = true
        val loadAdError = LoadAdError(3, "Ad load timed out", "Google", null, null)
        Log.e(LOG_TAG, "onAdFailedToLoad: timeout after $timeoutMillis ms")
        adLoadCallback.onFailedToLoad(loadAdError)
    }
    timeoutHandler.postDelayed(timeoutRunnable, timeoutMillis)
    AppOpenAd.load(myApplication, adUnitId, request, object : AppOpenAd.AppOpenAdLoadCallback() {
        override fun onAdLoaded(ad: AppOpenAd) {
            if (!hasTimedOut) {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                appOpenAd = ad
                adLoadCallback.onAdLoaded()
            }
        }
        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            if (!hasTimedOut) {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                Log.e(LOG_TAG, "onAdFailedToLoad: failed to load")
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", loadAdError.code.toString())
                }
                firebaseAnalytics.logEvent("ad_failed_to_load", params)
                adLoadCallback.onFailedToLoad(loadAdError)
            }
        }
    })
}
```

## Best Practices
- **Purchase Integration**: Ensure `BillingConfig.setPurchaseProvider` is set in your `Application` class.
- **Activity Exclusion**: Use `disableAppOpenWithActivity` for activities where ads are inappropriate (e.g., splash screens).
- **Ad Timing**: Use `skipNextAd` to avoid ads during critical user flows.
- **Timeout Adjustment**: Adjust `timeoutMillis` based on network conditions (e.g., shorter for fast networks).
- **Testing**:
    - Use AdMob test IDs (e.g., `ca-app-pub-3940256099942544/9257395921`).
    - Test lifecycle events (app foreground/background).
    - Verify exclusion and skip functionality.
- **Analytics**: Monitor Firebase Analytics for ad performance and errors.

## Limitations
- **Single Ad Cache**: Only one app open ad is cached at a time.
- **Lifecycle Dependency**: Relies on `ProcessLifecycleOwner` for automatic display.
- **No Frequency Control**: Lacks built-in time/count-based limits; use `skipNextAd` for manual control.

## Dependencies
- **Google AdMob SDK**: For ad loading and display.
- **Firebase Analytics**: For logging ad events.
- **AndroidX Lifecycle**: For lifecycle-aware ad display.
- **Project Resources**: `BillingConfig` for purchase checks.

## Troubleshooting
- **Ad Not Showing**: Check `isAdAvailable()`, ensure the activity isn’t excluded, and verify purchase status.
- **Ad Not Loading**: Confirm `adUnitId`, network connectivity, and AdMob configuration.
- **Lifecycle Issues**: Ensure `AppOpenManager` is initialized in the `Application` class.
- **Analytics Missing**: Verify Firebase initialization.

## Future Improvements
- Support for multiple cached app open ads.
- Time/count-based display limits.
- Customizable ad display triggers.

## References
- [AdMob App Open Ads Documentation](https://developers.google.com/admob/android/app-open-ads)
- [Firebase Analytics Documentation](https://firebase.google.com/docs/analytics)
- [AdManageKit v1.3.2 Release Notes](release-notes-v1.3.2.md)
- [Native Ads Caching Wiki](native-ads-caching.md)