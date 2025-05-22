# Interstitial Ads - AdManageKit v1.3.2

## Overview
The `AdManageKit` library (version `v1.3.2`) provides robust management of interstitial ads through the `AdManager` class in the `com.i2hammad.admanagekit.admob` package. Interstitial ads are full-screen ads displayed at natural transition points in your app, such as between activities or during pauses in gameplay. The `AdManager` singleton supports loading, caching, and displaying interstitial ads with flexible options like time-based or count-based triggers, dialog support, and Firebase Analytics integration for tracking ad events.

**Library Version**: v1.3.2  
**Last Updated**: May 22, 2025

## Features
- **Ad Loading and Caching**: Load interstitial ads and cache them for later use, with automatic reload after display.
- **Flexible Display Options**:
  - Immediate display with or without a loading dialog.
  - Time-based display (e.g., show every 15 seconds).
  - Count-based display (e.g., show up to a maximum number of times).
- **Purchase Check**: Automatically skips ad display if the user has purchased the app (via `BillingConfig`).
- **Firebase Analytics**: Logs ad impressions, paid events, failures, and dismissals.
- **Dialog Support**: Optional loading dialog to improve user experience during ad display.

## Components
### AdManager
The `AdManager` singleton manages interstitial ads:
- **Key Methods**:
  - `loadInterstitialAd(context: Context, adUnitId: String)`: Loads an interstitial ad for later use.
  - `loadInterstitialAd(context: Context, adUnitId: String, callback: InterstitialAdLoadCallback)`: Loads an ad with a custom callback.
  - `forceShowInterstitial(activity: Activity, callback: AdManagerCallback)`: Displays an ad immediately.
  - `forceShowInterstitialWithDialog(activity: Activity, callback: AdManagerCallback, isReload: Boolean)`: Displays an ad with a loading dialog.
  - `showInterstitialAdByTime(activity: Activity, callback: AdManagerCallback)`: Displays an ad if the time interval has elapsed.
  - `showInterstitialAdByCount(activity: Activity, callback: AdManagerCallback, maxDisplayCount: Int)`: Displays an ad if the display count is below the limit.
  - `isReady(): Boolean`: Checks if an ad is loaded and ready to display.
  - `setAdInterval(intervalMillis: Long)`: Sets the minimum time interval between ad displays.
  - `setAdDisplayCount(count: Int)`: Sets the current ad display count.
- **Configuration**:
  - Default ad interval: 15 seconds (`adIntervalMillis`).
  - Tracks display count (`adDisplayCount`) and last ad show time (`lastAdShowTime`).
- **Callbacks**:
  - Uses `AdManagerCallback` for handling ad dismissal (`onNextAction`) and other events.
  - Supports `InterstitialAdLoadCallback` for custom load handling.

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
- Material Components (for loading dialogs)
- Project resources (`BillingConfig`)

### Loading Interstitial Ads
Load an interstitial ad to cache it for later display:

```kotlin
AdManager.getInstance().loadInterstitialAd(this, "ca-app-pub-3940256099942544/1033173712")
```

Load with a custom callback:

```kotlin
AdManager.getInstance().loadInterstitialAd(this, "ca-app-pub-3940256099942544/1033173712", object : InterstitialAdLoadCallback() {
    override fun onAdLoaded(interstitialAd: InterstitialAd) {
        Log.d("AdManager", "Interstitial ad loaded")
    }
    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
        Log.e("AdManager", "Failed to load: ${loadAdError.message}")
    }
})
```

### Displaying Interstitial Ads
#### Immediate Display
Show an ad immediately:

```kotlin
AdManager.getInstance().forceShowInterstitial(this, object : AdManagerCallback() {
    override fun onNextAction() {
        startActivity(Intent(this@CurrentActivity, NextActivity::class.java))
    }
})
```

#### With Loading Dialog
Show an ad with a 500ms loading dialog:

```kotlin
AdManager.getInstance().forceShowInterstitialWithDialog(this, object : AdManagerCallback() {
    override fun onNextAction() {
        startActivity(Intent(this@CurrentActivity, NextActivity::class.java))
    }
})
```

#### Time-Based Display
Show an ad if at least 15 seconds have passed since the last ad (configurable via `setAdInterval`):

```kotlin
AdManager.getInstance().setAdInterval(30_000) // 30 seconds
AdManager.getInstance().showInterstitialAdByTime(this, object : AdManagerCallback() {
    override fun onNextAction() {
        startActivity(Intent(this@CurrentActivity, NextActivity::class.java))
    }
})
```

#### Count-Based Display
Show an ad up to a maximum number of times:

```kotlin
AdManager.getInstance().showInterstitialAdByCount(this, object : AdManagerCallback() {
    override fun onNextAction() {
        startActivity(Intent(this@CurrentActivity, NextActivity::class.java))
    }, maxDisplayCount = 3)
```

### Checking Ad Availability
Verify if an ad is ready to display:

```kotlin
if (AdManager.getInstance().isReady()) {
    Log.d("AdManager", "Interstitial ad is ready")
} else {
    Log.d("AdManager", "No interstitial ad available")
}
```

### Managing Ad Frequency
Set a custom time interval:

```kotlin
AdManager.getInstance().setAdInterval(60_000) // 60 seconds
```

Set or reset the display count:

```kotlin
AdManager.getInstance().setAdDisplayCount(0) // Reset count
```

## Implementation Details
### Ad Loading Workflow
1. **Loading**:
   - `loadInterstitialAd` initiates an ad request using `InterstitialAd.load`.
   - The ad is cached in `mInterstitialAd` if loaded successfully.
   - If the user has purchased the app (`BillingConfig.getPurchaseProvider().isPurchased()`), loading is skipped.
2. **Error Handling**:
   - Failed loads are logged to Firebase Analytics with the ad unit ID and error code.
   - Custom errors are triggered for purchased apps (`PURCHASED_APP_ERROR_CODE`).
3. **Callbacks**:
   - `InterstitialAdLoadCallback` handles load success or failure.
   - `AdManagerCallback` ensures the app proceeds (`onNextAction`) after ad display or failure.

### Ad Display Workflow
1. **Display Check**:
   - `isReady()` verifies a loaded ad exists and the user hasn’t purchased the app.
   - Time-based display checks if `adIntervalMillis` has elapsed since `lastAdShowTime`.
   - Count-based display checks if `adDisplayCount` is below `maxDisplayCount`.
2. **Dialog Support**:
   - `forceShowInterstitialWithDialog` shows a non-cancelable Material AlertDialog for 500ms before displaying the ad.
3. **Ad Events**:
   - `FullScreenContentCallback` handles ad show, dismissal, and failure events.
   - `OnPaidEventListener` logs revenue data to Firebase Analytics.
   - Analytics events include impressions (`AD_IMPRESSION`), dismissals (`ad_dismissed`), and failures (`ad_failed_to_show`).
4. **Reload**:
   - By default, a new ad is loaded after display (`reloadAd = true`), ensuring availability for future displays.

### Key Code Snippet
#### AdManager
```kotlin
fun loadInterstitialAd(context: Context, adUnitId: String) {
    this.adUnitId = adUnitId
    initializeFirebase(context)
    val adRequest = AdRequest.Builder().build()
    isAdLoading = true
    InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(interstitialAd: InterstitialAd) {
            mInterstitialAd = interstitialAd
            isAdLoading = false
            Log.d("AdManager", "Interstitial ad loaded")
        }
        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            Log.e("AdManager", "Failed to load interstitial ad: ${loadAdError.message}")
            isAdLoading = false
            mInterstitialAd = null
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putString("ad_error_code", loadAdError.code.toString())
            }
            firebaseAnalytics.logEvent("ad_failed_to_load", params)
        }
    })
}

fun forceShowInterstitial(activity: Activity, callback: AdManagerCallback) {
    showAd(activity, callback, true)
}
```

## Best Practices
- **Purchase Integration**: Ensure `BillingConfig.setPurchaseProvider` is called in your `Application` class to respect in-app purchases.
- **Ad Frequency**: Use `setAdInterval` or `showInterstitialAdByCount` to avoid overwhelming users with frequent ads.
- **Dialog Usage**: Use `forceShowInterstitialWithDialog` for smoother transitions in critical flows (e.g., activity changes).
- **Error Handling**: Implement `AdManagerCallback` and `InterstitialAdLoadCallback` to handle load/display failures gracefully.
- **Testing**:
  - Test with AdMob test IDs (e.g., `ca-app-pub-3940256099942544/1033173712`).
  - Verify time-based and count-based triggers.
  - Test purchase scenarios to ensure ads are skipped for premium users.
- **Analytics**: Review Firebase Analytics logs to monitor ad performance and errors.

## Limitations
- **Single Ad Cache**: Only one interstitial ad is cached at a time per `AdManager` instance.
- **Manual Frequency Control**: Time and count limits are managed manually; adjust `adIntervalMillis` or `maxDisplayCount` as needed.
- **Dialog Dependency**: The loading dialog requires Material Components; ensure it’s included in your app.

## Dependencies
- **Google AdMob SDK**: For ad loading and display.
- **Firebase Analytics**: For logging ad events.
- **Material Components**: For loading dialogs.
- **Project Resources**: `BillingConfig` for purchase checks.

## Troubleshooting
- **Ad Not Loading**: Verify `adUnitId`, network connectivity, and AdMob configuration.
- **Ad Not Displaying**: Check `isReady()` and ensure the user hasn’t purchased the app.
- **Dialog Issues**: Ensure Material Components are included and the activity isn’t finishing.
- **Analytics Missing**: Confirm Firebase is initialized and configured.

## Future Improvements
- Support for preloading multiple interstitial ads.
- Configurable dialog duration and styling.
- Automatic frequency optimization based on user engagement.

## References
- [AdMob Interstitial Ads Documentation](https://developers.google.com/admob/android/interstitial)
- [Firebase Analytics Documentation](https://firebase.google.com/docs/analytics)
- [AdManageKit v1.3.2 Release Notes](release-notes-v1.3.2.md)
- [Native Ads Caching Wiki](native-ads-caching.md)