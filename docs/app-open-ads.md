# App Open Ads - AdManageKit v4.0.0

## Overview
The `AdManageKit` library (version `v4.0.0`) provides lifecycle-aware management of app open ads through the `AppOpenManager` class in the `com.i2hammad.admanagekit.admob` package. App open ads are full-screen ads displayed when users launch or return to your app, ideal for monetizing app entry points. The `AppOpenManager` handles ad loading, display, activity/screen/fragment exclusion, automatic retry with exponential backoff, preloader support, and Firebase Analytics integration for tracking ad events.

**Library Version**: v4.0.0
**Last Updated**: December 2025

## What's New in v4.0.0

### GMA Next-Gen SDK Migration
- **Modern SDK**: Full migration to Google Mobile Ads Next-Gen SDK
- **Background Thread Safety**: All callbacks now automatically dispatch to main thread
- **Preloader Support**: Uses `AppOpenAdPreloader` for efficient ad loading

### Single-Activity App Support
- **Screen Tag Exclusions**: Control ads by screen/destination name
- **Fragment Tag Exclusions**: Exclude specific fragments from showing ads
- **Fragment Tag Provider**: Automatic fragment detection
- **Temporary Disable**: Pause/resume ads during critical flows

### Preloader System
- **Auto-Loading**: SDK automatically keeps ads ready in background
- **Efficient**: Reduces ad load time when showing
- **Configurable**: Enable/disable via `usePreloader` property

---

## What's New in v2.5.0

### Fully Automatic Reload System
- **No manual `fetchAd()` calls needed** in most cases
- Ads automatically reload on app foreground, after dismissal, after failures
- Just initialize and forget - the lifecycle handles everything

### Custom Ad Unit Support
- Load ads from custom ad units on-demand without changing the default configuration
- Perfect for A/B testing different ad units or seasonal campaigns
- Fallback to default ad unit for automatic reloads

### Circuit Breaker Removed
- **Maximized Show Rates**: Removed circuit breaker logic that was blocking ads after failures
- Ads now attempt to load every time without being blocked by previous failures
- Optimized for achieving 75%+ show rates

### Enhanced Retry Logic
- Exponential backoff retry with configurable parameters
- Automatic retry for network errors and no-fill scenarios
- Works seamlessly with automatic reload system
- Better ad availability and user experience

### Performance Metrics
- Track average load times
- Monitor total successful loads
- Debug and optimize ad performance

---

## Quick Start

### Simple Usage
```kotlin
class MyApp : Application() {
    lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        // Initialize with default ad unit
        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy")

        // Optional: Preload for faster first show
        appOpenManager.fetchAd()
    }
}
```

That's it! Ads will **automatically load and show** when users open or return to your app.

**Important**: Manual `fetchAd()` is **optional** here. The system automatically reloads ads when needed. The initial `fetchAd()` just ensures the first ad is ready faster.

---

## Automatic Reload Behavior

**AppOpenManager handles ad reloading automatically!** You rarely need to call `fetchAd()` manually.

### When Ads Reload Automatically:

1. **On app foreground** (`onStart` lifecycle):
   ```kotlin
   override fun onStart(owner: LifecycleOwner) {
       if (!purchaseProvider.isPurchased()) {
           showAdIfAvailable()  // ← Automatically calls fetchAd() if needed
       }
   }
   ```

2. **After ad is dismissed**:
   - Ad shows → User dismisses → Automatically calls `fetchAd()`

3. **After ad fails to show**:
   - Ad fails → Automatically calls `fetchAd()` with retry logic

4. **When activity is excluded**:
   - Activity excluded → Automatically calls `fetchAd()`

5. **When ad is not available**:
   - No ad loaded → Automatically calls `fetchAd()`

### When Manual `fetchAd()` IS Needed:

✅ **Initial preload** - Call once in `Application.onCreate()` for faster first show
✅ **Custom ad units** - Load different ad units for A/B testing, seasonal campaigns
✅ **Custom configuration** - Specific timeout, retry settings, or callbacks

### When Manual `fetchAd()` is NOT Needed:

❌ **After showing an ad** - Already automatic
❌ **After ad fails** - Already automatic with retry
❌ **On app foreground** - Already automatic via lifecycle
❌ **When ad not available** - Already automatic
❌ **After activity excluded** - Already automatic

**Bottom line**: Just initialize `AppOpenManager` and it handles everything automatically. Manual `fetchAd()` is only for optimization and special cases.

---

### Load Custom Ad Unit
```kotlin
// Load a different ad unit for special occasions
appOpenManager.fetchAd(
    adLoadCallback = object : AdLoadCallback() {
        override fun onAdLoaded() {
            Log.d("AppOpen", "Custom ad loaded")
        }
        override fun onFailedToLoad(error: AdError?) {
            Log.e("AppOpen", "Failed: ${error?.message}")
        }
    },
    timeoutMillis = 8000,
    customAdUnitId = "ca-app-pub-xxxxx/seasonal-ad"
)
```

---

## Features

### Core Features
- **Fully Automatic**: Ads load and reload automatically via lifecycle - minimal manual intervention needed
- **Lifecycle-Aware**: Automatically shows app open ads when the app moves to foreground (via `ProcessLifecycleOwner`)
- **Auto-Reload**: Automatically reloads ads after display, failure, or when not available - you rarely call `fetchAd()` manually
- **Custom Ad Units**: Load different ad units on-demand for A/B testing or seasonal campaigns
- **Automatic Retry**: Exponential backoff retry logic for failed loads with configurable parameters
- **Configurable Timeout**: Set custom timeout for ad loading to prevent delays
- **Activity Exclusion**: Skip ads for specific activities (e.g., splash screens, payment screens)
- **Purchase Check**: Automatically skips ads if user has purchased the app (via `BillingConfig`)
- **Firebase Analytics**: Logs ad impressions, paid events, and failures for tROAS tracking

### Advanced Features
- **Performance Metrics**: Track load times and success rates
- **Thread-Safe Operations**: All methods are thread-safe for concurrent access
- **Memory Optimized**: Proper cleanup and leak prevention
- **Debug Utilities**: Detailed logging for development and troubleshooting

---

## Installation

### Gradle Setup
Add `AdManageKit v2.5.0` to your project via Gradle:

```groovy
// build.gradle (project level)
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

// build.gradle (app level)
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:AdManageKit:2.5.0'
    implementation 'com.github.i2hammad.AdManageKit:admanagekit-billing:2.5.0'
    implementation 'com.github.i2hammad.AdManageKit:admanagekit-core:2.5.0'
}
```

### Required Dependencies
The library requires:
- Google AdMob SDK
- Firebase Analytics
- AndroidX Lifecycle
- Google Play Billing (for purchase checks)

---

## Complete Usage Guide

### 1. Initialize AppOpenManager

Initialize in your `Application` class:

```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        // Set up billing provider
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

        // Initialize app open manager with default ad unit
        appOpenManager = AppOpenManager(
            this,
            "ca-app-pub-xxxxx/yyyyy"
        )
    }
}
```

### 2. Loading App Open Ads

#### Basic Load (Default Ad Unit)
```kotlin
// Simple load - uses default ad unit
appOpenManager.fetchAd()
```

#### Load with Callback
```kotlin
appOpenManager.fetchAd(object : AdLoadCallback() {
    override fun onAdLoaded() {
        Log.d("AppOpen", "Ad loaded and ready")
    }

    override fun onFailedToLoad(error: AdError?) {
        Log.e("AppOpen", "Failed to load: ${error?.message}")
    }
})
```

#### Load with Custom Timeout
```kotlin
// Load with 8-second timeout
appOpenManager.fetchAd(
    adLoadCallback = object : AdLoadCallback() {
        override fun onAdLoaded() {
            Log.d("AppOpen", "Ad loaded")
        }
        override fun onFailedToLoad(error: AdError?) {
            Log.e("AppOpen", "Failed: ${error?.message}")
        }
    },
    timeoutMillis = 8000
)
```

#### Load Custom Ad Unit (New in v2.5.0)
```kotlin
// Load a different ad unit temporarily
appOpenManager.fetchAd(
    adLoadCallback = object : AdLoadCallback() {
        override fun onAdLoaded() {
            Log.d("AppOpen", "Custom ad loaded")
        }
        override fun onFailedToLoad(error: AdError?) {
            Log.e("AppOpen", "Custom ad failed")
        }
    },
    timeoutMillis = 5000,
    customAdUnitId = "ca-app-pub-xxxxx/custom-unit"
)
```

**Use cases for custom ad units:**
- A/B testing different ad units
- Seasonal campaigns (holidays, events)
- Geographic targeting
- User segment targeting
- Premium vs. standard ad units

### 3. Displaying App Open Ads

#### Automatic Display (Recommended)
Ads are automatically shown when the app comes to the foreground:

```kotlin
// No code needed! Ads show automatically when:
// 1. User opens the app
// 2. User returns from background
// 3. User is not in an excluded activity
// 4. User hasn't purchased the app
```

#### Force Show in Specific Activity
```kotlin
class MainActivity : AppCompatActivity() {

    private fun showAppOpenAd() {
        appOpenManager.forceShowAdIfAvailable(this, object : AdManagerCallback() {
            override fun onNextAction() {
                // Ad dismissed or failed - continue flow
                startMainContent()
            }

            override fun onAdLoaded() {
                // Ad is showing
                Log.d("AppOpen", "App open ad displayed")
            }
        })
    }
}
```

#### Skip Next Ad
```kotlin
// Skip the next automatic ad display
appOpenManager.skipNextAd()

// Useful for:
// - After user completes a purchase
// - During critical user flows
// - After showing another ad type
```

### 4. Activity Exclusion

Prevent ads from showing in specific activities:

```kotlin
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy")

        // Exclude splash screen
        appOpenManager.disableAppOpenWithActivity(SplashActivity::class.java)

        // Exclude payment activities
        appOpenManager.disableAppOpenWithActivity(PaymentActivity::class.java)
        appOpenManager.disableAppOpenWithActivity(CheckoutActivity::class.java)
    }
}
```

Re-enable an excluded activity:

```kotlin
// Re-enable ads for this activity
appOpenManager.includeAppOpenActivityForAds(SplashActivity::class.java)
```

### 5. Single-Activity App Support (v4.0.0+)

For apps using a single activity with multiple fragments (e.g., Navigation Component, Jetpack Compose Navigation):

#### Screen Tag Exclusions

```kotlin
class MyApp : Application() {
    lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy").apply {
            // Exclude specific screens by name
            excludeScreenTags("Payment", "Onboarding", "Checkout", "Settings")
        }
    }
}

// In MainActivity - track current screen
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navController = findNavController(R.id.nav_host_fragment)

        // Update screen tag on navigation
        navController.addOnDestinationChangedListener { _, destination, _ ->
            (application as MyApp).appOpenManager.setCurrentScreenTag(
                destination.label?.toString()
            )
        }
    }
}
```

#### Fragment Tag Exclusions

```kotlin
class MyApp : Application() {
    lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy").apply {
            // Exclude specific fragment tags
            excludeFragmentTags("PaymentFragment", "OnboardingFragment", "CheckoutFragment")
        }
    }
}

// In MainActivity - set up fragment tag provider
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set provider for automatic fragment detection
        (application as MyApp).appOpenManager.setFragmentTagProvider {
            supportFragmentManager.fragments.lastOrNull()?.tag
        }
    }
}
```

#### Temporary Disable/Enable

```kotlin
class PaymentFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Disable app open ads during payment flow
        (requireActivity().application as MyApp).appOpenManager.disableAppOpenAdsTemporarily()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Re-enable when leaving payment
        (requireActivity().application as MyApp).appOpenManager.enableAppOpenAds()
    }

    private fun onPaymentComplete() {
        // Re-enable before navigating away
        (requireActivity().application as MyApp).appOpenManager.enableAppOpenAds()
        findNavController().navigate(R.id.action_payment_to_success)
    }
}
```

#### Check Current State

```kotlin
// Check if ads are currently enabled
if (appOpenManager.areAppOpenAdsEnabled()) {
    Log.d("AppOpen", "Ads are enabled")
}

// Get current screen tag
val currentScreen = appOpenManager.getCurrentScreenTag()
Log.d("AppOpen", "Current screen: $currentScreen")
```

#### Combining Approaches

You can combine activity exclusion, screen tags, and fragment tags:

```kotlin
appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy").apply {
    // Exclude activities (for multi-activity apps or specific activities)
    disableAppOpenWithActivity(SplashActivity::class.java)

    // Exclude screens by name (for Navigation Component)
    excludeScreenTags("Payment", "Onboarding")

    // Exclude fragments by tag (for FragmentManager)
    excludeFragmentTags("PaymentFragment")
}
```

### 6. Checking Ad Availability

```kotlin
// Check if ad is loaded and ready
if (appOpenManager.isAdAvailable()) {
    Log.d("AppOpen", "Ad is ready to show")
} else {
    Log.d("AppOpen", "No ad available")
}

// Enhanced check with reason (v2.5.0)
when (val result = appOpenManager.canShowAd()) {
    is AppOpenManager.AdShowResult.CAN_SHOW -> {
        Log.d("AppOpen", "Can show ad")
    }
    is AppOpenManager.AdShowResult.CANNOT_SHOW -> {
        Log.d("AppOpen", "Cannot show: ${result.reason}")
    }
}
```

---

## Complete Examples

### Example 1: Basic Setup
```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
        appOpenManager = AppOpenManager(this, getString(R.string.app_open_ad))
    }
}
```

### Example 2: With Activity Exclusions
```kotlin
class MyApp : Application() {
    lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy").apply {
            // Exclude splash and onboarding
            disableAppOpenWithActivity(SplashActivity::class.java)
            disableAppOpenWithActivity(OnboardingActivity::class.java)
            disableAppOpenWithActivity(PaymentActivity::class.java)
        }
    }
}
```

### Example 3: Custom Ad Unit for Events
```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/default")

        // Load seasonal ad during holidays
        if (isHolidaySeason()) {
            loadSeasonalAd()
        }
    }

    private fun isHolidaySeason(): Boolean {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        return month == Calendar.DECEMBER // December
    }

    private fun loadSeasonalAd() {
        appOpenManager.fetchAd(
            adLoadCallback = object : AdLoadCallback() {
                override fun onAdLoaded() {
                    Log.d("AppOpen", "Seasonal ad loaded")
                }
                override fun onFailedToLoad(error: AdError?) {
                    Log.e("AppOpen", "Seasonal ad failed, using default")
                    appOpenManager.fetchAd() // Fallback to default
                }
            },
            timeoutMillis = 6000,
            customAdUnitId = "ca-app-pub-xxxxx/seasonal"
        )
    }
}
```

### Example 4: A/B Testing Different Ad Units
```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/default")

        // A/B test different ad units
        loadABTestAd()
    }

    private fun loadABTestAd() {
        // Get user segment (simplified example)
        val userId = getCurrentUserId()
        val segment = userId.hashCode() % 2

        val adUnitId = when (segment) {
            0 -> "ca-app-pub-xxxxx/variant-a"
            else -> "ca-app-pub-xxxxx/variant-b"
        }

        appOpenManager.fetchAd(
            adLoadCallback = object : AdLoadCallback() {
                override fun onAdLoaded() {
                    // Log to analytics
                    FirebaseAnalytics.getInstance(this@MyApp).logEvent("ab_test_loaded") {
                        param("variant", if (segment == 0) "A" else "B")
                    }
                }
                override fun onFailedToLoad(error: AdError?) {
                    // Fallback to default
                    appOpenManager.fetchAd()
                }
            },
            customAdUnitId = adUnitId
        )
    }

    private fun getCurrentUserId(): String {
        // Your user ID logic
        return "user123"
    }
}
```

### Example 5: Force Show After Purchase Flow
```kotlin
class PurchaseCompleteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip app open ad after purchase
        (application as MyApp).appOpenManager.skipNextAd()

        // Show thank you message without ad interruption
        showThankYouMessage()
    }
}
```

### Example 6: Conditional Loading Based on Network
```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy")

        // Adjust timeout based on network speed
        loadAdWithNetworkAwareness()
    }

    private fun loadAdWithNetworkAwareness() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo

        val timeout = when {
            networkInfo?.type == ConnectivityManager.TYPE_WIFI -> 5000L
            networkInfo?.type == ConnectivityManager.TYPE_MOBILE -> 8000L
            else -> 10000L
        }

        appOpenManager.fetchAd(
            adLoadCallback = object : AdLoadCallback() {
                override fun onAdLoaded() {
                    Log.d("AppOpen", "Ad loaded (timeout: ${timeout}ms)")
                }
                override fun onFailedToLoad(error: AdError?) {
                    Log.e("AppOpen", "Failed with ${timeout}ms timeout")
                }
            },
            timeoutMillis = timeout
        )
    }
}
```

### Example 7: Performance Monitoring
```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy")

        // Monitor performance periodically
        Handler(Looper.getMainLooper()).postDelayed({
            logPerformanceMetrics()
        }, 60000) // After 1 minute
    }

    private fun logPerformanceMetrics() {
        val metrics = appOpenManager.getPerformanceMetrics()
        val avgLoadTime = metrics["averageLoadTime"] as? Double ?: 0.0
        val totalLoads = metrics["totalLoads"] as? Int ?: 0

        Log.d("AppOpen", "Performance: avg=${avgLoadTime}ms, total=$totalLoads")

        // Log to Firebase Analytics
        FirebaseAnalytics.getInstance(this).logEvent("app_open_performance") {
            param("avg_load_time", avgLoadTime)
            param("total_loads", totalLoads.toLong())
        }
    }
}
```

### Example 8: Advanced Retry Configuration
```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy")

        // Configure aggressive retry for better show rates
        appOpenManager.updateRetryConfiguration(
            maxAttempts = 5,        // Try up to 5 times
            baseDelay = 500L,       // Start with 500ms
            maxDelay = 15000L,      // Cap at 15 seconds
            multiplier = 2.0        // Double delay each retry
        )
    }
}
```

---

## Maximizing Show Rates (75%+ Target)

### Strategy 1: Aggressive Retry Configuration
```kotlin
appOpenManager.updateRetryConfiguration(
    maxAttempts = 5,
    baseDelay = 500L,
    maxDelay = 15000L,
    multiplier = 2.0
)
```

**Why it works**: More retry attempts with faster delays means higher chance of successful load.

### Strategy 2: Optional Initial Preload
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy")

        // Optional: Preload for faster first show
        appOpenManager.fetchAd()
    }
}
```

**Why it works**: Ensures ad is ready before first `onStart()` fires. Note: This is **optional** - ads will still load automatically without it.

### Strategy 3: Multiple Ad Unit Fallback
```kotlin
fun loadWithFallback() {
    appOpenManager.fetchAd(
        adLoadCallback = object : AdLoadCallback() {
            override fun onAdLoaded() {
                Log.d("AppOpen", "Primary ad loaded")
            }

            override fun onFailedToLoad(error: AdError?) {
                // Try fallback ad unit
                appOpenManager.fetchAd(
                    adLoadCallback = object : AdLoadCallback() {
                        override fun onAdLoaded() {
                            Log.d("AppOpen", "Fallback ad loaded")
                        }
                        override fun onFailedToLoad(error: AdError?) {
                            Log.e("AppOpen", "All ad units failed")
                        }
                    },
                    customAdUnitId = "ca-app-pub-xxxxx/fallback"
                )
            }
        },
        customAdUnitId = "ca-app-pub-xxxxx/primary"
    )
}
```

**Why it works**: If primary ad unit has no fill, fallback provides second chance. Note: Only use this in initial load if needed - **automatic reload uses default ad unit**.

### Strategy 4: Monitor and Optimize
```kotlin
// Regularly check performance
val metrics = appOpenManager.getPerformanceMetrics()
val avgLoadTime = metrics["averageLoadTime"] as? Double ?: 0.0

// If load times are high, increase timeout
if (avgLoadTime > 4000) {
    // Use higher timeout for slower networks
    appOpenManager.fetchAd(
        adLoadCallback = callback,
        timeoutMillis = 8000
    )
}
```

---

## Configuration Options

### Retry Configuration
```kotlin
appOpenManager.updateRetryConfiguration(
    maxAttempts = 3,        // Number of retry attempts (1-10)
    baseDelay = 1000L,      // Initial retry delay (100-10000ms)
    maxDelay = 30000L,      // Maximum retry delay (1000-300000ms)
    multiplier = 2.0        // Delay multiplier (1.0-5.0)
)
```

### Timeout Configuration
```kotlin
// Short timeout for fast networks
appOpenManager.fetchAd(adLoadCallback, timeoutMillis = 3000)

// Default timeout
appOpenManager.fetchAd(adLoadCallback, timeoutMillis = 5000)

// Long timeout for slow networks
appOpenManager.fetchAd(adLoadCallback, timeoutMillis = 10000)
```

---

## Implementation Details

### Ad Loading Workflow

1. **Initial Load**:
   - `fetchAd()` initiates an ad request using `AppOpenAd.load`
   - The ad is cached in `appOpenAd` if loaded successfully
   - A timeout ensures the callback is triggered if loading takes too long

2. **Retry Logic** (New in v2.5.0):
   - Automatic retry with exponential backoff for network errors and no-fill
   - Configurable retry parameters
   - Smart retry only for recoverable errors

3. **Error Handling**:
   - Failed loads are logged to Firebase Analytics with the ad unit ID and error code
   - Timeout failures return a custom `LoadAdError` (code 3)
   - Retry attempts are logged for debugging

### Ad Display Workflow

1. **Display Check**:
   - `isAdAvailable()` verifies a loaded ad exists
   - Ads are skipped if:
     - The user has purchased the app (`BillingConfig.getPurchaseProvider().isPurchased()`)
     - The current activity is in `excludedActivities`
     - Another ad is showing (`isShowingAd` or `AdManager.isDisplayingAd()`)
     - `skipNextAd` is `true`

2. **Lifecycle Integration**:
   - `AppOpenManager` observes `ProcessLifecycleOwner` to show ads on app foreground (`onStart`)
   - Activity lifecycle callbacks track the current activity (`currentActivity`)
   - **On every `onStart()` event**: Calls `showAdIfAvailable()` which automatically calls `fetchAd()` if no ad is available

3. **Ad Events**:
   - `FullScreenContentCallback` handles ad show, dismissal, and failure
   - `OnPaidEventListener` logs revenue data to Firebase Analytics
   - Analytics events include impressions (`AD_IMPRESSION`) and failures (`ad_failed_to_load`)

4. **Automatic Reload** (Key Feature!):
   - `showAdIfAvailable()` automatically calls `fetchAd()` in these scenarios:
     - When no ad is available
     - When current activity is excluded
     - After ad is dismissed
     - After ad fails to show
   - **You rarely need to call `fetchAd()` manually** - the lifecycle handles it!

### Circuit Breaker Removal (v2.5.0)

**What Changed:**
- Removed circuit breaker that was blocking ads after consecutive failures
- Ads now attempt to load every time without being blocked by previous failures

**Why It Matters:**
- Increased ad show rates by 20-30%
- Better user experience (no "dead periods" without ads)
- Maximizes revenue potential

**Before (with circuit breaker):**
```
Load attempt 1: ❌ Failed
Load attempt 2: ❌ Failed
Load attempt 3: ❌ Failed
[Circuit breaker OPEN - blocking future attempts]
Load attempt 4: ❌ Blocked
Load attempt 5: ❌ Blocked
```

**After (without circuit breaker):**
```
Load attempt 1: ❌ Failed (retry in 1s)
Load attempt 2: ❌ Failed (retry in 2s)
Load attempt 3: ❌ Failed (retry in 4s)
Load attempt 4: ✅ Success!
```

---

## Best Practices

### 1. Purchase Integration
Always set up billing provider in your `Application` class:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // REQUIRED: Set purchase provider
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy")
    }
}
```

### 2. Activity Exclusion
Exclude activities where ads are inappropriate:

```kotlin
// Exclude splash screens
appOpenManager.disableAppOpenWithActivity(SplashActivity::class.java)

// Exclude payment flows
appOpenManager.disableAppOpenWithActivity(PaymentActivity::class.java)

// Exclude full-screen video players
appOpenManager.disableAppOpenWithActivity(VideoPlayerActivity::class.java)
```

### 3. Optional Initial Preload
Optionally preload for faster first show:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        appOpenManager = AppOpenManager(this, "ca-app-pub-xxxxx/yyyyy")

        // Optional: Preload for faster first show
        appOpenManager.fetchAd()
    }
}
```

**Note**: This is **optional**. Ads will automatically load on first `onStart()` even without manual preload. This just makes the first ad appear faster.

### 4. Adjust Timeout Based on Network
```kotlin
val timeout = when (networkType) {
    WIFI -> 4000L
    MOBILE -> 7000L
    else -> 10000L
}

appOpenManager.fetchAd(callback, timeoutMillis = timeout)
```

### 5. Use Custom Ad Units Strategically
```kotlin
// Good use cases:
// ✅ A/B testing
// ✅ Seasonal campaigns
// ✅ Geographic targeting
// ✅ User segment targeting

// Bad use cases:
// ❌ Random switching
// ❌ Too frequent changes
// ❌ Without proper fallback
```

### 6. Monitor Performance
```kotlin
// Check metrics regularly
val metrics = appOpenManager.getPerformanceMetrics()
Log.d("AppOpen", "Performance: $metrics")

// Adjust strategy based on data
```

### 7. Handle Edge Cases
```kotlin
// Skip ad after important actions
afterPurchase { appOpenManager.skipNextAd() }

// Check availability before forcing
if (appOpenManager.isAdAvailable()) {
    appOpenManager.forceShowAdIfAvailable(this, callback)
}
```

### 8. Testing
```kotlin
// Use AdMob test IDs during development
val AD_UNIT_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/9257395921" // Test ID
} else {
    getString(R.string.app_open_ad) // Production ID
}
```

---

## Troubleshooting

### Ad Not Showing?

**Check these common issues:**

1. **Is ad loaded?**
   ```kotlin
   if (!appOpenManager.isAdAvailable()) {
       Log.e("AppOpen", "No ad available")
   }
   ```

2. **Is activity excluded?**
   ```kotlin
   // Make sure you haven't excluded the current activity
   appOpenManager.disableAppOpenWithActivity(CurrentActivity::class.java)
   ```

3. **Did user purchase?**
   ```kotlin
   if (BillingConfig.getPurchaseProvider().isPurchased()) {
       Log.d("AppOpen", "User purchased - ads disabled")
   }
   ```

4. **Is another ad showing?**
   ```kotlin
   if (AppOpenManager.isShowingAd() || AdManager.getInstance().isDisplayingAd()) {
       Log.d("AppOpen", "Another ad is currently showing")
   }
   ```

5. **Did you skip the ad?**
   ```kotlin
   // Check if skipNextAd() was called
   ```

### Ad Not Loading?

1. **Check ad unit ID**:
   ```kotlin
   // Verify your ad unit ID is correct
   Log.d("AppOpen", "Using ad unit: $adUnitId")
   ```

2. **Check network connectivity**:
   ```kotlin
   val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
   val isConnected = cm.activeNetworkInfo?.isConnected == true
   ```

3. **Check AdMob configuration**:
   - Verify AdMob app ID in AndroidManifest.xml
   - Check if ad unit is enabled in AdMob console

4. **Check timeout**:
   ```kotlin
   // Increase timeout for slow networks
   appOpenManager.fetchAd(callback, timeoutMillis = 10000)
   ```

5. **Check Firebase Analytics**:
   - Look for `ad_failed_to_load` events
   - Check error codes

### Low Show Rates?

1. **Enable aggressive retry**:
   ```kotlin
   appOpenManager.updateRetryConfiguration(
       maxAttempts = 5,
       baseDelay = 500L,
       maxDelay = 15000L
   )
   ```

2. **Optional initial preload** (for faster first show only):
   ```kotlin
   // Optional: Load in Application.onCreate() for faster first show
   // Not required - automatic reload handles everything
   appOpenManager.fetchAd()
   ```

3. **Use fallback ad units** (for initial load if needed):
   ```kotlin
   // Implement fallback strategy
   loadWithFallback()
   ```

4. **Monitor metrics**:
   ```kotlin
   val metrics = appOpenManager.getPerformanceMetrics()
   ```

5. **Reduce exclusions**:
   ```kotlin
   // Only exclude critical activities
   ```

### Memory Leaks?

Call cleanup when done:
```kotlin
override fun onDestroy() {
    super.onDestroy()
    appOpenManager.cleanup()
}
```

---

## API Reference

### AppOpenManager

#### Constructor
```kotlin
AppOpenManager(application: Application, adUnitId: String)
```

#### Methods

**fetchAd()**
```kotlin
fun fetchAd()
```
Load ad with default configuration and automatic retry.

**fetchAd(callback, timeout, customAdUnitId)**
```kotlin
@JvmOverloads
fun fetchAd(
    adLoadCallback: AdLoadCallback,
    timeoutMillis: Long = 5000,
    customAdUnitId: String? = null
)
```
Load ad with callback, custom timeout, and optional custom ad unit.

**showAdIfAvailable()**
```kotlin
fun showAdIfAvailable()
```
Show ad if available (called automatically on app foreground).

**forceShowAdIfAvailable()**
```kotlin
fun forceShowAdIfAvailable(activity: Activity, adManagerCallback: AdManagerCallback)
```
Force show ad in specific activity.

**skipNextAd()**
```kotlin
fun skipNextAd()
```
Skip the next automatic ad display.

**isAdAvailable()**
```kotlin
fun isAdAvailable(): Boolean
```
Check if ad is loaded and ready.

**canShowAd()**
```kotlin
fun canShowAd(): AdShowResult
```
Enhanced check with detailed reason if cannot show.

**disableAppOpenWithActivity()**
```kotlin
fun disableAppOpenWithActivity(activityClass: Class<*>)
```
Exclude activity from showing ads.

**includeAppOpenActivityForAds()**
```kotlin
fun includeAppOpenActivityForAds(activityClass: Class<*>)
```
Re-enable ads for excluded activity.

**setCurrentScreenTag()** (v4.0.0+)
```kotlin
fun setCurrentScreenTag(tag: String?)
```
Set current screen tag for single-activity apps.

**excludeScreenTag()** (v4.0.0+)
```kotlin
fun excludeScreenTag(tag: String)
```
Exclude a screen tag from showing ads.

**excludeScreenTags()** (v4.0.0+)
```kotlin
fun excludeScreenTags(vararg tags: String)
```
Exclude multiple screen tags.

**setFragmentTagProvider()** (v4.0.0+)
```kotlin
fun setFragmentTagProvider(provider: (() -> String?)?)
```
Set provider for automatic fragment tag detection.

**excludeFragmentTag()** (v4.0.0+)
```kotlin
fun excludeFragmentTag(tag: String)
```
Exclude a fragment tag from showing ads.

**excludeFragmentTags()** (v4.0.0+)
```kotlin
fun excludeFragmentTags(vararg tags: String)
```
Exclude multiple fragment tags.

**disableAppOpenAdsTemporarily()** (v4.0.0+)
```kotlin
fun disableAppOpenAdsTemporarily()
```
Temporarily disable app open ads.

**enableAppOpenAds()** (v4.0.0+)
```kotlin
fun enableAppOpenAds()
```
Re-enable app open ads after temporary disable.

**areAppOpenAdsEnabled()** (v4.0.0+)
```kotlin
fun areAppOpenAdsEnabled(): Boolean
```
Check if app open ads are currently enabled.

**getPerformanceMetrics()**
```kotlin
fun getPerformanceMetrics(): Map<String, Any>
```
Get performance metrics (average load time, total loads).

**updateRetryConfiguration()**
```kotlin
fun updateRetryConfiguration(
    maxAttempts: Int = 3,
    baseDelay: Long = 1000L,
    maxDelay: Long = 30000L,
    multiplier: Double = 2.0
)
```
Configure retry behavior.

**cleanup()**
```kotlin
fun cleanup()
```
Clean up resources and prevent memory leaks.

#### Static Methods

**isShowingAd()**
```kotlin
@JvmStatic
fun isShowingAd(): Boolean
```
Check if any app open ad is currently showing.

**isShownAd()**
```kotlin
@JvmStatic
fun isShownAd(): Boolean
```
Check if any app open ad has been shown.

---

## Callbacks

### AdLoadCallback
```kotlin
abstract class AdLoadCallback {
    abstract fun onAdLoaded()
    abstract fun onFailedToLoad(error: AdError?)
}
```

### AdManagerCallback
```kotlin
abstract class AdManagerCallback {
    open fun onNextAction() {}
    open fun onAdLoaded() {}
    open fun onFailedToLoad(error: AdError?) {}
}
```

---

## Analytics Events

### Automatic Firebase Events

1. **ad_impression**: When ad is shown
   ```kotlin
   params {
       AD_UNIT_NAME = "ca-app-pub-xxxxx/yyyyy"
   }
   ```

2. **ad_paid_event**: Revenue tracking
   ```kotlin
   params {
       AD_UNIT_NAME = "ca-app-pub-xxxxx/yyyyy"
       VALUE = 0.05
       CURRENCY = "USD"
   }
   ```

3. **ad_failed_to_load**: Error tracking
   ```kotlin
   params {
       AD_UNIT_NAME = "ca-app-pub-xxxxx/yyyyy"
       ad_error_code = "3"
       error_message = "Network error"
   }
   ```

---

## Migration Guide

### From v1.3.2 to v2.5.0

#### No Breaking Changes
All existing code continues to work without modification.

#### Optional Enhancements

**1. Use Custom Ad Units (Optional)**
```kotlin
// Old way (still works)
appOpenManager.fetchAd()

// New way (optional)
appOpenManager.fetchAd(
    callback,
    timeoutMillis = 5000,
    customAdUnitId = "ca-app-pub-xxxxx/custom"
)
```

**2. Configure Retry Logic (Optional)**
```kotlin
// New in v2.5.0
appOpenManager.updateRetryConfiguration(
    maxAttempts = 5,
    baseDelay = 500L
)
```

**3. Monitor Performance (Optional)**
```kotlin
// New in v2.5.0
val metrics = appOpenManager.getPerformanceMetrics()
```

**4. Enhanced Show Check (Optional)**
```kotlin
// Old way (still works)
if (appOpenManager.isAdAvailable()) { }

// New way (optional)
when (val result = appOpenManager.canShowAd()) {
    is AppOpenManager.AdShowResult.CAN_SHOW -> { }
    is AppOpenManager.AdShowResult.CANNOT_SHOW -> {
        Log.d("AppOpen", result.reason)
    }
}
```

---

## Performance Tips

### Tip 1: Optional Initial Preload (For Faster First Show)
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appOpenManager = AppOpenManager(this, adUnitId)

        // Optional: Preload for faster first show
        // Without this, ad will still load automatically on first onStart()
        appOpenManager.fetchAd()
    }
}
```

### Tip 2: Configure Aggressive Retry
```kotlin
appOpenManager.updateRetryConfiguration(
    maxAttempts = 5,
    baseDelay = 500L,
    maxDelay = 15000L
)
```

### Tip 3: Use Fallback Ad Units
```kotlin
fun loadWithFallback() {
    appOpenManager.fetchAd(callback, customAdUnitId = "primary")
        .onError {
            appOpenManager.fetchAd(callback, customAdUnitId = "fallback")
        }
}
```

### Tip 4: Optimize Timeout
```kotlin
val timeout = if (isWiFi) 4000L else 8000L
appOpenManager.fetchAd(callback, timeoutMillis = timeout)
```

### Tip 5: Minimize Exclusions
```kotlin
// Only exclude critical activities
appOpenManager.disableAppOpenWithActivity(SplashActivity::class.java)
appOpenManager.disableAppOpenWithActivity(PaymentActivity::class.java)
// Don't over-exclude!
```

---

## Known Limitations

### Single Ad Cache
Only one app open ad is cached at a time. If you load a custom ad unit, it replaces the previously loaded ad.

**Workaround:**
```kotlin
// Load default ad as fallback after custom ad shows
appOpenManager.fetchAd() // Loads default ad unit
```

### Lifecycle Dependency
Relies on `ProcessLifecycleOwner` for automatic display. Manual control requires `forceShowAdIfAvailable()`.

### No Built-in Frequency Capping
No time/count-based limits. Use `skipNextAd()` for manual control or implement custom logic.

**Example Custom Frequency Capping:**
```kotlin
class MyApp : Application() {
    private var lastAdShowTime = 0L
    private val minIntervalMs = 60000L // 1 minute

    fun shouldShowAd(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastAdShowTime) >= minIntervalMs
    }

    fun onAdShown() {
        lastAdShowTime = System.currentTimeMillis()
    }
}
```

---

## Dependencies

- **Google AdMob SDK**: For ad loading and display
- **Firebase Analytics**: For logging ad events
- **AndroidX Lifecycle**: For lifecycle-aware ad display
- **Google Play Billing**: For purchase checks (via BillingConfig)

---

## Support & Resources

### Documentation
- [AdMob App Open Ads Documentation](https://developers.google.com/admob/android/app-open-ads)
- [Firebase Analytics Documentation](https://firebase.google.com/docs/analytics)
- [AdManageKit v2.5.0 Release Notes](RELEASE_NOTES_v2.5.0.md)

### Sample Code
Check the `app` module for complete working examples.

### Troubleshooting
Enable debug logging:
```kotlin
AdManageKitConfig.enablePerformanceMetrics = true
```

### Issues
Report issues at: [GitHub Issues](https://github.com/i2hammad/AdManageKit/issues)

---

## Changelog

### v4.0.0 (December 2025)
- **GMA Next-Gen SDK Migration**: Full migration to modern SDK
- **Background Thread Safety**: All callbacks now dispatch to main thread
- **Preloader Support**: Uses `AppOpenAdPreloader` for efficient loading
- **Single-Activity App Support**:
  - Added `setCurrentScreenTag()` / `getCurrentScreenTag()`
  - Added `excludeScreenTag()` / `excludeScreenTags()` / `includeScreenTag()` / `clearScreenTagExclusions()`
  - Added `setFragmentTagProvider()`
  - Added `excludeFragmentTag()` / `excludeFragmentTags()` / `includeFragmentTag()`
  - Added `disableAppOpenAdsTemporarily()` / `enableAppOpenAds()` / `areAppOpenAdsEnabled()`
- **Preloader API**:
  - Added `isPreloadedAdAvailable()`
  - Added `pollPreloadedAd()`
  - Added `showPreloadedAd()`
  - Added `usePreloader` property

### v2.5.0 (January 2025)
- Added custom ad unit support to `fetchAd()` method
- Removed circuit breaker for maximized show rates
- Enhanced retry logic with exponential backoff
- Added performance metrics tracking
- Added `canShowAd()` for detailed show capability check
- Improved thread safety across all methods
- Better memory management and leak prevention

### v1.3.2 (May 2025)
- Initial stable release
- Basic app open ad management
- Lifecycle-aware display
- Activity exclusion support
- Firebase Analytics integration

---

## License

See the main AdManageKit LICENSE file for details.
