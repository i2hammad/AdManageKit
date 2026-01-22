# Rewarded Ads

RewardedAdManager provides a comprehensive solution for integrating rewarded video ads with automatic retry, analytics tracking, and premium user handling.

## Features

- **Automatic Retry**: Exponential backoff on load failures
- **Premium User Handling**: Automatically skips ads for purchased users
- **Timeout Support**: For splash screens and time-sensitive scenarios
- **Firebase Analytics**: Tracks requests, fills, impressions, and show rates
- **Configurable Auto-Reload**: Automatically reload after ad dismissal
- **Full Lifecycle Callbacks**: Reward earned, shown, dismissed, clicked, failed

## Quick Start

### 1. Initialize

```kotlin
// In Application.onCreate() or Activity
RewardedAdManager.initialize(context, "ca-app-pub-xxx/yyy")
```

### 2. Show Rewarded Ad

```kotlin
if (RewardedAdManager.isAdLoaded()) {
    RewardedAdManager.showAd(activity, object : RewardedAdManager.RewardedAdCallback {
        override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
            // Grant reward to user
            userCoins += rewardAmount
        }

        override fun onAdDismissed() {
            // Continue app flow
        }
    })
} else {
    // Ad not ready - show alternative or wait
    Toast.makeText(context, "Ad not ready yet", Toast.LENGTH_SHORT).show()
}
```

## Callback Interfaces

### RewardedAdCallback (Recommended)

Full lifecycle callback with all events:

```kotlin
interface RewardedAdCallback {
    // Required
    fun onRewardEarned(rewardType: String, rewardAmount: Int)
    fun onAdDismissed()

    // Optional (default empty implementations)
    fun onAdShowed() {}
    fun onAdFailedToShow(error: AdError) {}
    fun onAdClicked() {}
}
```

### OnRewardedAdLoadCallback

For load-specific events:

```kotlin
interface OnRewardedAdLoadCallback {
    fun onAdLoaded()
    fun onAdFailedToLoad(error: LoadAdError)
}
```

## Loading Methods

### Basic Load

```kotlin
// Simple load (auto-retries on failure)
RewardedAdManager.loadRewardedAd(context)
```

### Load with Callback

```kotlin
RewardedAdManager.loadRewardedAd(context, object : RewardedAdManager.OnRewardedAdLoadCallback {
    override fun onAdLoaded() {
        // Ad ready to show
        showWatchAdButton()
    }

    override fun onAdFailedToLoad(error: LoadAdError) {
        // Handle error
        Log.e("Ads", "Failed: ${error.message}")
    }
})
```

### Load with Timeout (Splash Screens)

Perfect for splash screens where you need to proceed after a timeout:

```kotlin
RewardedAdManager.loadRewardedAdWithTimeout(
    context = this,
    timeoutMillis = 5000, // 5 seconds
    callback = object : RewardedAdManager.OnRewardedAdLoadCallback {
        override fun onAdLoaded() {
            // Show ad or proceed
            showRewardedOffer()
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            // Timeout or error - proceed anyway
            navigateToMain()
        }
    }
)
```

**Note**: If the ad loads after timeout, it's saved for next use (not wasted).

## Display Methods

### Show with Callback

```kotlin
RewardedAdManager.showAd(
    activity = this,
    callback = object : RewardedAdManager.RewardedAdCallback {
        override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
            // Grant reward
            addCoins(rewardAmount)
            showRewardAnimation()
        }

        override fun onAdDismissed() {
            // User closed the ad
            continueGame()
        }

        override fun onAdShowed() {
            // Ad is now visible
            pauseGame()
        }

        override fun onAdFailedToShow(error: AdError) {
            // Couldn't show - maybe show alternative
            Log.e("Ads", "Show failed: ${error.message}")
        }

        override fun onAdClicked() {
            // User clicked the ad
        }
    },
    autoReload = true // Reload after dismissal
)
```

### Control Auto-Reload

```kotlin
// Disable auto-reload for this show
RewardedAdManager.showAd(activity, callback, autoReload = false)

// Or configure globally
AdManageKitConfig.rewardedAutoReload = false
```

## State Checking

```kotlin
// Check if ad is ready
if (RewardedAdManager.isAdLoaded()) {
    showWatchAdButton()
}

// Check if loading
if (RewardedAdManager.isLoading()) {
    showLoadingIndicator()
}

// Check if currently showing
if (RewardedAdManager.isShowingAd()) {
    // Don't interrupt
}
```

## Preloading

Preload during natural pauses to improve show rate:

```kotlin
// In Activity.onResume()
override fun onResume() {
    super.onResume()
    RewardedAdManager.preload(this)
}

// After completing a level
fun onLevelComplete() {
    showResults()
    RewardedAdManager.preload(this) // Preload for next opportunity
}
```

## Analytics & Debugging

### Get Statistics

```kotlin
val stats = RewardedAdManager.getAdStats()

// Available stats:
// - session_requests: Int
// - session_fills: Int
// - session_impressions: Int
// - fill_rate_percent: Float
// - show_rate_percent: Float
// - is_loaded: Boolean
// - is_loading: Boolean
// - retry_attempts: Int

Log.d("Ads", """
    Requests: ${stats["session_requests"]}
    Fills: ${stats["session_fills"]}
    Impressions: ${stats["session_impressions"]}
    Fill Rate: ${stats["fill_rate_percent"]}%
    Show Rate: ${stats["show_rate_percent"]}%
""".trimIndent())
```

### Reset Statistics

```kotlin
// Reset at start of new session
RewardedAdManager.resetAdStats()
```

### Firebase Events

RewardedAdManager automatically logs these Firebase Analytics events:

| Event | Description |
|-------|-------------|
| `ad_request` | Ad load requested |
| `ad_fill` | Ad loaded successfully |
| `ad_failed_to_load` | Ad failed to load |
| `ad_impression` | Standard impression event |
| `ad_impression_detailed` | Detailed with fill/show rates |
| `ad_dismissed` | User closed the ad |
| `ad_failed_to_show` | Ad couldn't be shown |
| `ad_paid_event` | Revenue event for tROAS |
| `rewarded_ad_reward` | User earned reward |

## Automatic Retry

RewardedAdManager automatically retries failed loads using exponential backoff:

```kotlin
// Configure retry behavior
AdManageKitConfig.apply {
    autoRetryFailedAds = true      // Enable auto-retry (default: true)
    maxRetryAttempts = 3           // Max attempts (default: 3)
    baseRetryDelay = 1.seconds     // Base delay (default: 1s)
    maxRetryDelay = 30.seconds     // Max delay (default: 30s)
    enableExponentialBackoff = true // Exponential backoff (default: true)
}
```

**Retry Schedule** (with exponential backoff):
- Attempt 1: 1 second delay
- Attempt 2: 2 seconds delay
- Attempt 3: 4 seconds delay
- (capped at maxRetryDelay)

**Manual Load Cancels Retry**: If you manually call `loadRewardedAd()` while a retry is pending, the pending retry is cancelled and a fresh load starts.

## Premium User Handling

RewardedAdManager automatically checks purchase status:

```kotlin
// If user has purchased premium, these return early:
RewardedAdManager.loadRewardedAd(context)  // Skips loading
RewardedAdManager.isAdLoaded()             // Returns false
RewardedAdManager.showAd(...)              // Calls onAdDismissed() immediately
```

This requires setting up BillingConfig:

```kotlin
// In Application.onCreate()
BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
```

## Best Practices

### 1. Initialize Early

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RewardedAdManager.initialize(this, "ca-app-pub-xxx/yyy")
    }
}
```

### 2. Preload Strategically

```kotlin
// Good places to preload:
// - After showing an ad (if autoReload is off)
// - When entering a screen with "Watch Ad" button
// - After user completes an action
// - In onResume()
```

### 3. Handle All States

```kotlin
fun onWatchAdClicked() {
    when {
        RewardedAdManager.isAdLoaded() -> {
            RewardedAdManager.showAd(activity, callback)
        }
        RewardedAdManager.isLoading() -> {
            showToast("Ad loading, please wait...")
        }
        else -> {
            showToast("No ad available")
            RewardedAdManager.loadRewardedAd(context)
        }
    }
}
```

### 4. Don't Block UI

```kotlin
// DON'T: Block user while loading
fun showAd() {
    showBlockingDialog()
    RewardedAdManager.loadRewardedAd(context, callback) // Bad UX
}

// DO: Use preloading and show when ready
fun showAd() {
    if (RewardedAdManager.isAdLoaded()) {
        RewardedAdManager.showAd(activity, callback)
    } else {
        showToast("Ad not ready")
    }
}
```

## Migration from Legacy API

### Before (Legacy)

```kotlin
RewardedAdManager.showAd(
    activity,
    OnUserEarnedRewardListener { reward ->
        addCoins(reward.amount)
    },
    object : RewardedAdManager.OnAdDismissedListener {
        override fun onAdDismissed() {
            continueFlow()
        }
    }
)
```

### After (New API)

```kotlin
RewardedAdManager.showAd(activity, object : RewardedAdManager.RewardedAdCallback {
    override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
        addCoins(rewardAmount)
    }

    override fun onAdDismissed() {
        continueFlow()
    }

    // Now you also get:
    override fun onAdShowed() { pauseGame() }
    override fun onAdFailedToShow(error: AdError) { handleError(error) }
    override fun onAdClicked() { trackClick() }
})
```

## Troubleshooting

### Ad Not Loading

1. Check internet connection
2. Verify ad unit ID is correct
3. Check if user is premium: `BillingConfig.getPurchaseProvider().isPurchased()`
4. Check retry status: `RewardedAdManager.getAdStats()["retry_attempts"]`

### Ad Not Showing

1. Check if loaded: `RewardedAdManager.isAdLoaded()`
2. Check if already showing: `RewardedAdManager.isShowingAd()`
3. Verify Activity is not finishing

### Low Fill Rate

1. Check `getAdStats()["fill_rate_percent"]`
2. Consider using multiple ad networks via mediation
3. Check if test mode is properly configured

## See Also

- [API Reference](../docs/API_REFERENCE.md#rewardedadmanager)
- [Interstitial Ads](../docs/interstitial-ads.md)
- [Ad Loading Strategies](../docs/AD_LOADING_STRATEGIES.md)
- [Billing Integration](Billing-Integration.md)
