# Rewarded Ads Guide

This guide covers the complete integration of rewarded video ads using `RewardedAdManager`.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Quick Start](#quick-start)
- [Callback Interfaces](#callback-interfaces)
- [Loading Methods](#loading-methods)
- [Display Methods](#display-methods)
- [State Management](#state-management)
- [Preloading Strategy](#preloading-strategy)
- [Analytics & Debugging](#analytics--debugging)
- [Automatic Retry](#automatic-retry)
- [Configuration](#configuration)
- [Best Practices](#best-practices)
- [Java Usage](#java-usage)
- [Troubleshooting](#troubleshooting)

## Overview

`RewardedAdManager` is a singleton class that simplifies rewarded ad integration with features like:

- Automatic retry with exponential backoff
- Premium user handling (ads disabled for purchased users)
- Timeout support for splash screens
- Firebase Analytics integration
- Full lifecycle callbacks

## Features

| Feature | Description |
|---------|-------------|
| **Auto-Retry** | Exponential backoff on failures (configurable) |
| **Premium Handling** | Automatically skips ads for premium users |
| **Timeout Support** | Load with timeout for time-sensitive scenarios |
| **Analytics** | Firebase tracking for requests, fills, impressions |
| **Auto-Reload** | Configurable reload after ad dismissal |
| **Lifecycle Callbacks** | Full callbacks: reward, show, dismiss, click, fail |
| **Preloading** | Preload during natural pauses |
| **Manual Load Cancel** | Manual loads cancel pending retries |

## Quick Start

### 1. Initialize

Initialize once in your `Application` class or main `Activity`:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure AdManageKit (optional)
        AdManageKitConfig.apply {
            autoRetryFailedAds = true
            interstitialAutoReload = true
        }

        // Initialize rewarded ads
        RewardedAdManager.initialize(this, "ca-app-pub-xxx/yyy")
    }
}
```

### 2. Check and Show

```kotlin
fun onWatchAdClicked() {
    if (RewardedAdManager.isAdLoaded()) {
        RewardedAdManager.showAd(this, object : RewardedAdManager.RewardedAdCallback {
            override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
                // Grant reward
                userCoins += rewardAmount
                saveUserData()
            }

            override fun onAdDismissed() {
                // Continue flow
                updateUI()
            }
        })
    } else {
        Toast.makeText(this, "Ad not ready", Toast.LENGTH_SHORT).show()
    }
}
```

## Callback Interfaces

### RewardedAdCallback (Recommended)

Full lifecycle callback for all ad events:

```kotlin
interface RewardedAdCallback {
    /**
     * Called when user earns a reward.
     * @param rewardType The type of reward (e.g., "coins", "gems")
     * @param rewardAmount The amount of reward earned
     */
    fun onRewardEarned(rewardType: String, rewardAmount: Int)

    /**
     * Called when ad is dismissed (user closed it).
     * This is always called after onRewardEarned (if reward was earned).
     */
    fun onAdDismissed()

    /**
     * Called when ad is shown successfully.
     * Use this to pause game/music.
     */
    fun onAdShowed() {}

    /**
     * Called when ad fails to show.
     * @param error The error that occurred
     */
    fun onAdFailedToShow(error: AdError) {}

    /**
     * Called when user clicks the ad.
     */
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

### Full Callback Example

```kotlin
RewardedAdManager.showAd(activity, object : RewardedAdManager.RewardedAdCallback {
    override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
        Log.d("Ads", "Reward: $rewardAmount $rewardType")
        viewModel.addReward(rewardType, rewardAmount)
    }

    override fun onAdDismissed() {
        Log.d("Ads", "Ad dismissed")
        enableGameControls()
    }

    override fun onAdShowed() {
        Log.d("Ads", "Ad showing")
        pauseGame()
        muteBackgroundMusic()
    }

    override fun onAdFailedToShow(error: AdError) {
        Log.e("Ads", "Failed to show: ${error.message}")
        showErrorToast("Couldn't show ad")
        enableGameControls()
    }

    override fun onAdClicked() {
        Log.d("Ads", "Ad clicked")
        analytics.logAdClick("rewarded")
    }
})
```

## Loading Methods

### Basic Load

```kotlin
// Simple load - retries automatically on failure
RewardedAdManager.loadRewardedAd(context)
```

### Load with Callback

```kotlin
RewardedAdManager.loadRewardedAd(context, object : RewardedAdManager.OnRewardedAdLoadCallback {
    override fun onAdLoaded() {
        // Update UI - show "Watch Ad" button
        watchAdButton.isEnabled = true
        watchAdButton.alpha = 1f
    }

    override fun onAdFailedToLoad(error: LoadAdError) {
        Log.e("Ads", "Load failed: ${error.code} - ${error.message}")
        watchAdButton.isEnabled = false
        watchAdButton.alpha = 0.5f
    }
})
```

### Load with Timeout

For splash screens or time-sensitive scenarios:

```kotlin
RewardedAdManager.loadRewardedAdWithTimeout(
    context = this,
    timeoutMillis = 5000, // 5 seconds
    callback = object : RewardedAdManager.OnRewardedAdLoadCallback {
        override fun onAdLoaded() {
            // Show optional reward opportunity
            showRewardedOffer()
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            // Timeout or error - proceed without ad
            if (error.code == -1) {
                Log.d("Ads", "Timed out")
            } else {
                Log.e("Ads", "Failed: ${error.message}")
            }
            navigateToMain()
        }
    }
)
```

**Key Behavior**: If the ad loads after timeout, it's saved for later use (not wasted).

## Display Methods

### Show with Callback

```kotlin
RewardedAdManager.showAd(
    activity = this,
    callback = myCallback,
    autoReload = true // Reload after dismissal (default)
)
```

### Control Auto-Reload

```kotlin
// Disable for this show only
RewardedAdManager.showAd(activity, callback, autoReload = false)

// Or configure globally
AdManageKitConfig.interstitialAutoReload = false
```

### Show Only If Ready

```kotlin
fun tryShowRewardedAd(): Boolean {
    return if (RewardedAdManager.isAdLoaded()) {
        RewardedAdManager.showAd(activity, callback)
        true
    } else {
        RewardedAdManager.loadRewardedAd(context) // Start loading
        false
    }
}
```

## State Management

### State Methods

```kotlin
// Is ad loaded and ready to show?
val ready = RewardedAdManager.isAdLoaded()

// Is ad currently loading?
val loading = RewardedAdManager.isLoading()

// Is ad currently being displayed?
val showing = RewardedAdManager.isShowingAd()
```

### State-Based UI

```kotlin
fun updateWatchAdButton() {
    when {
        RewardedAdManager.isAdLoaded() -> {
            watchAdButton.isEnabled = true
            watchAdButton.text = "Watch Ad for Coins"
        }
        RewardedAdManager.isLoading() -> {
            watchAdButton.isEnabled = false
            watchAdButton.text = "Loading..."
        }
        else -> {
            watchAdButton.isEnabled = false
            watchAdButton.text = "Ad Unavailable"
        }
    }
}
```

## Preloading Strategy

Preload during natural pauses to maximize show rate:

```kotlin
// In onResume
override fun onResume() {
    super.onResume()
    RewardedAdManager.preload(this)
}

// After user actions
fun onLevelComplete() {
    showResults()
    RewardedAdManager.preload(this) // Ready for next opportunity
}

// After showing other ads
fun onInterstitialDismissed() {
    RewardedAdManager.preload(this)
}
```

## Analytics & Debugging

### Session Statistics

```kotlin
val stats = RewardedAdManager.getAdStats()

// Returns Map<String, Any> with:
// - session_requests: Int
// - session_fills: Int
// - session_impressions: Int
// - fill_rate_percent: Float
// - show_rate_percent: Float
// - is_loaded: Boolean
// - is_loading: Boolean
// - retry_attempts: Int

// Display stats
Log.d("Ads", """
    Rewarded Ad Stats:
    - Requests: ${stats["session_requests"]}
    - Fills: ${stats["session_fills"]}
    - Impressions: ${stats["session_impressions"]}
    - Fill Rate: ${stats["fill_rate_percent"]}%
    - Show Rate: ${stats["show_rate_percent"]}%
""".trimIndent())
```

### Reset Statistics

```kotlin
// Reset at app launch or session start
RewardedAdManager.resetAdStats()
```

### Firebase Events

The following events are automatically logged:

| Event | When | Parameters |
|-------|------|------------|
| `ad_request` | Load requested | ad_unit_name, ad_type, session_requests |
| `ad_fill` | Load successful | ad_unit_name, fill_rate_percent |
| `ad_failed_to_load` | Load failed | ad_unit_name, ad_error_code |
| `ad_impression` | Ad shown | ad_unit_name |
| `ad_impression_detailed` | Ad shown | show_rate_percent, fill_rate_percent |
| `ad_dismissed` | User closed ad | ad_unit_name |
| `ad_failed_to_show` | Show failed | ad_unit_name, error_code |
| `ad_paid_event` | Revenue event | ad_unit_name, value, currency |
| `rewarded_ad_reward` | Reward earned | reward_type, reward_amount |

## Automatic Retry

### How It Works

When a load fails, the manager automatically schedules a retry with exponential backoff:

```
Attempt 1: Wait 1 second → retry
Attempt 2: Wait 2 seconds → retry
Attempt 3: Wait 4 seconds → retry
(capped at maxRetryDelay)
```

### Configuration

```kotlin
AdManageKitConfig.apply {
    autoRetryFailedAds = true       // Enable auto-retry (default: true)
    maxRetryAttempts = 3            // Max attempts (default: 3)
    baseRetryDelay = 1.seconds      // Initial delay
    maxRetryDelay = 30.seconds      // Maximum delay cap
    enableExponentialBackoff = true // Use exponential backoff
}
```

### Manual Load Behavior

When you manually call `loadRewardedAd()`:
1. Any pending retry is **cancelled**
2. Retry counter is **reset to 0**
3. Fresh load starts immediately

This ensures manual requests always take priority.

## Configuration

### Global Settings

```kotlin
AdManageKitConfig.apply {
    // Retry settings
    autoRetryFailedAds = true
    maxRetryAttempts = 3

    // Auto-reload after showing
    interstitialAutoReload = true

    // Timeout for load operations
    defaultAdTimeout = 10.seconds

    // Debug logging
    debugMode = BuildConfig.DEBUG
}
```

### Test Mode

```kotlin
AdManageKitConfig.apply {
    testMode = true
    testDeviceId = "YOUR_TEST_DEVICE_ID"
}
```

## Best Practices

### 1. Initialize Early

```kotlin
// In Application.onCreate() - before any Activity
RewardedAdManager.initialize(applicationContext, adUnitId)
```

### 2. Don't Block User Flow

```kotlin
// BAD: Blocking while loading
fun showAd() {
    showLoadingDialog()
    RewardedAdManager.loadRewardedAd(context, callback)
    // User waits...
}

// GOOD: Use preloading
fun showAd() {
    if (RewardedAdManager.isAdLoaded()) {
        RewardedAdManager.showAd(activity, callback)
    } else {
        // Offer alternative or show message
        showNoAdAvailableMessage()
    }
}
```

### 3. Preload Strategically

```kotlin
// After user completes an action (likely to watch ad next)
fun onTaskComplete() {
    RewardedAdManager.preload(context)
}

// In natural pauses
override fun onResume() {
    super.onResume()
    RewardedAdManager.preload(this)
}
```

### 4. Handle All Cases

```kotlin
fun onWatchAdClicked() {
    when {
        RewardedAdManager.isShowingAd() -> {
            // Already showing, ignore
        }
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

### 5. Always Grant Rewards

```kotlin
override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
    // ALWAYS grant the reward - user watched the ad
    grantReward(rewardType, rewardAmount)

    // Then update UI
    showRewardAnimation()
}
```

## Java Usage

```java
// Initialize
RewardedAdManager.INSTANCE.initialize(context, "ca-app-pub-xxx/yyy");

// Check and show
if (RewardedAdManager.INSTANCE.isAdLoaded()) {
    RewardedAdManager.INSTANCE.showAd(activity, new RewardedAdManager.RewardedAdCallback() {
        @Override
        public void onRewardEarned(@NonNull String rewardType, int rewardAmount) {
            grantReward(rewardType, rewardAmount);
        }

        @Override
        public void onAdDismissed() {
            continueFlow();
        }

        @Override
        public void onAdShowed() {
            // Optional
        }

        @Override
        public void onAdFailedToShow(@NonNull AdError error) {
            // Optional
        }

        @Override
        public void onAdClicked() {
            // Optional
        }
    }, true);
}

// Load with callback
RewardedAdManager.INSTANCE.loadRewardedAd(context, new RewardedAdManager.OnRewardedAdLoadCallback() {
    @Override
    public void onAdLoaded() {
        updateButton();
    }

    @Override
    public void onAdFailedToLoad(@NonNull LoadAdError error) {
        handleError(error);
    }
});

// Preload
RewardedAdManager.INSTANCE.preload(context);

// Get stats
Map<String, Object> stats = RewardedAdManager.INSTANCE.getAdStats();
```

## Troubleshooting

### Ad Not Loading

| Issue | Solution |
|-------|----------|
| No internet | Check connectivity |
| Wrong ad unit | Verify ad unit ID in AdMob console |
| Premium user | Check `BillingConfig.getPurchaseProvider().isPurchased()` |
| Rate limited | Wait for retry or check `getAdStats()["retry_attempts"]` |

### Ad Not Showing

| Issue | Solution |
|-------|----------|
| Not loaded | Check `isAdLoaded()` before showing |
| Already showing | Check `isShowingAd()` |
| Activity finishing | Ensure activity is valid |

### Low Fill Rate

1. Check stats: `getAdStats()["fill_rate_percent"]`
2. Use mediation for multiple ad sources
3. Verify ad unit is set up correctly
4. Test with test ad units first

### Rewards Not Granted

```kotlin
// Ensure you're handling the callback correctly
override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
    Log.d("Ads", "Reward: $rewardAmount $rewardType") // Debug

    // Grant reward FIRST
    grantReward(rewardAmount)

    // Then update UI
    updateUI()
}
```

## See Also

- [API Reference](API_REFERENCE.md#rewardedadmanager)
- [Interstitial Ads](interstitial-ads.md)
- [Ad Loading Strategies](AD_LOADING_STRATEGIES.md)
- [Configuration Guide](CONFIGURATION_USAGE.md)
- [Wiki: Rewarded Ads](../wiki/Rewarded-Ads.md)
