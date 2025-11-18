# Interstitial Ads - AdManageKit v2.5.0

## Overview
The `AdManageKit` library provides robust management of interstitial ads with a **modern, fluent API** through the `InterstitialAdBuilder` class. Interstitial ads are full-screen ads displayed at natural transition points in your app. The library now features a beautiful builder pattern, automatic fallback support, advanced frequency controls, and seamless Firebase Analytics integration.

**Library Version**: v2.5.0
**Last Updated**: January 2025

## ✨ What's New in v2.5.0

### 🎨 **Modern Builder Pattern**
- Fluent, chainable API
- Extension functions for Kotlin
- One-line ad display
- Backward compatible with v1.3.2

### 🚀 **Advanced Features**
- **Automatic Fallback**: Try multiple ad units automatically
- **Frequency Controls**: `everyNthTime`, `maxShows`, `minInterval`
- **Debug Mode**: Detailed logging for development
- **Loading Dialogs**: Built-in "Please wait..." UI
- **Smart Callbacks**: Success, failure, and show events

### 🔧 **Optimizations**
- Circuit breaker removed for maximum show rate
- Automatic ad reload after display
- Aggressive retry logic
- Better error handling

---

## 🚀 Quick Start

### Simple Usage (Recommended)
```kotlin
// Just 1 line!
showInterstitialAd("ca-app-pub-xxxxx/yyyyy") {
    startNextActivity()
}
```

### With Configuration
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .everyNthTime(3)         // Show every 3rd time
    .maxShows(10)            // Max 10 times total
    .minIntervalSeconds(30)  // 30s between shows
    .show {
        startNextActivity()
    }
```

---

## 📚 Complete API Reference

### InterstitialAdBuilder Methods

#### Configuration Methods

| Method | Description | Example |
|--------|-------------|---------|
| `.adUnit(String)` | Set primary ad unit ID **(required)** | `.adUnit("ca-app-pub-xxx/123")` |
| `.fallback(String)` | Add fallback ad unit | `.fallback("backup-unit")` |
| `.fallbacks(vararg String)` | Add multiple fallbacks | `.fallbacks("unit1", "unit2")` |
| `.force()` | Ignore time interval | `.force()` |
| `.respectInterval(Boolean)` | Respect time interval (default: true) | `.respectInterval(false)` |
| `.autoReload(Boolean)` | Auto-reload after show (default: true) | `.autoReload(true)` |

#### Frequency Control

| Method | Description | Example |
|--------|-------------|---------|
| `.everyNthTime(Int)` | Show every Nth call | `.everyNthTime(3)` |
| `.maxShows(Int)` | Limit total shows | `.maxShows(10)` |
| `.minInterval(Long)` | Minimum ms between shows | `.minInterval(60000)` |
| `.minIntervalSeconds(Int)` | Minimum seconds (convenience) | `.minIntervalSeconds(60)` |

#### Additional Features

| Method | Description | Example |
|--------|-------------|---------|
| `.withLoadingDialog()` | Show loading dialog | `.withLoadingDialog()` |
| `.debug()` | Enable debug logging | `.debug()` |

#### Callbacks

| Method | Description | Parameters |
|--------|-------------|------------|
| `.onAdLoaded(() -> Unit)` | Called when ad loaded | None |
| `.onAdShown(() -> Unit)` | Called when ad displayed | None |
| `.onFailed((LoadAdError) -> Unit)` | Called on failure | error: LoadAdError |

#### Terminal Methods

| Method | Description |
|--------|-------------|
| `.show(() -> Unit)` | Load and show ad, then execute callback |
| `.preload()` | Just preload ad without showing |

---

## 🎯 Usage Examples

### Example 1: Basic Navigation
```kotlin
class GameOverActivity : AppCompatActivity() {

    private fun playAgain() {
        showInterstitialAd(getString(R.string.interstitial_ad_unit)) {
            startActivity(Intent(this, GameActivity::class.java))
            finish()
        }
    }
}
```

### Example 2: With Fallback
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/primary")
    .fallback("ca-app-pub-xxxxx/backup")
    .show {
        goToNextScreen()
    }
```

### Example 3: Frequency Control - Every 3rd Time
```kotlin
class GameActivity : AppCompatActivity() {

    private val adBuilder = InterstitialAdBuilder.with(this)
        .adUnit(getString(R.string.interstitial_game))
        .everyNthTime(3)  // Show on 3rd, 6th, 9th level
        .debug()

    fun onLevelComplete() {
        adBuilder.show {
            loadNextLevel()
        }
    }
}

// Call 1: ❌ Skip (not 3rd)
// Call 2: ❌ Skip (not 3rd)
// Call 3: ✅ SHOW AD
// Call 4: ❌ Skip (not 3rd)
// Call 5: ❌ Skip (not 3rd)
// Call 6: ✅ SHOW AD
```

### Example 4: Limited Exposure
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit(getString(R.string.interstitial_onboarding))
    .maxShows(5)              // Only first 5 times
    .minIntervalSeconds(60)   // 60s between shows
    .show {
        continueOnboarding()
    }
```

### Example 5: Time-Based Control
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit(getString(R.string.interstitial_article))
    .minIntervalSeconds(120)  // 2 minutes minimum
    .show {
        loadNextArticle()
    }
```

### Example 6: All Features Combined
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/primary")
    .fallbacks("backup-1", "backup-2")
    .everyNthTime(2)         // Every 2nd call
    .maxShows(15)            // Max 15 times total
    .minIntervalSeconds(45)  // 45s minimum
    .force()                 // Override intervals
    .withLoadingDialog()     // Show "Please wait..."
    .debug()                 // Enable logging
    .onAdShown {
        FirebaseAnalytics.getInstance(this)
            .logEvent("interstitial_shown", null)
    }
    .onFailed { error ->
        Log.e("Ads", "Failed: ${error.message}")
    }
    .show {
        startNextActivity()
    }
```

### Example 7: Preload Pattern
```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preload ad early
        preloadInterstitialAd(getString(R.string.interstitial_ad_unit))
    }

    private fun showAdBeforeExit() {
        // Ad already preloaded, shows instantly
        showInterstitialAd(getString(R.string.interstitial_ad_unit)) {
            finish()
        }
    }
}
```

### Example 8: With Analytics
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit(getString(R.string.interstitial_ad_unit))
    .onAdShown {
        analytics.logEvent("ad_interstitial_shown", null)
    }
    .onFailed { error ->
        analytics.logEvent("ad_failed", Bundle().apply {
            putInt("error_code", error.code)
            putString("message", error.message)
        })
    }
    .show {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
```

---

## 🎨 Extension Functions

### Kotlin Extensions for Cleaner Code

#### `Activity.showInterstitialAd()`
```kotlin
fun Activity.showInterstitialAd(
    adUnitId: String,
    force: Boolean = false,
    onComplete: () -> Unit
)
```

**Usage:**
```kotlin
// Simple
showInterstitialAd("ad-unit-id") { next() }

// Force show
showInterstitialAd("ad-unit-id", force = true) { next() }
```

#### `Activity.showInterstitialAdWithFallback()`
```kotlin
fun Activity.showInterstitialAdWithFallback(
    primaryUnit: String,
    fallbackUnit: String,
    onComplete: () -> Unit
)
```

**Usage:**
```kotlin
showInterstitialAdWithFallback(
    primaryUnit = "ca-app-pub-xxx/primary",
    fallbackUnit = "ca-app-pub-xxx/backup"
) {
    goToNext()
}
```

#### `Activity.preloadInterstitialAd()`
```kotlin
fun Activity.preloadInterstitialAd(adUnitId: String)
```

**Usage:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    preloadInterstitialAd("ad-unit-id")
}
```

---

## 📊 Frequency Control Guide

### 1. `everyNthTime(n)` - Show Every Nth Call

Show ad only on specific call intervals.

```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(3)  // Show on 3rd, 6th, 9th... call
    .show { next() }
```

**Use cases:**
- Show ad every 3 level completions
- Show after every 5 button clicks
- Show every Nth game over

### 2. `maxShows(n)` - Limit Total Shows

Limit maximum number of times ad can be shown globally.

```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .maxShows(10)  // Maximum 10 times total
    .show { next() }
```

**Use cases:**
- Show ad maximum 5 times per session
- Limit annoying ads for power users
- Gradually reduce ads as user progresses

### 3. `minInterval(ms)` / `minIntervalSeconds(s)` - Time Control

Enforce minimum time between ad shows.

```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .minIntervalSeconds(60)  // 60 seconds minimum
    .show { next() }
```

**Use cases:**
- Prevent ad spam
- Ensure smooth user experience
- Respect user's time

### Combined Example
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(2)         // Every 2nd call
    .maxShows(15)            // Max 15 times total
    .minIntervalSeconds(45)  // 45s minimum between
    .show { next() }
```

---

## 🔍 Debug Mode

Enable debug mode to see detailed logs of why ads are skipped:

```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(3)
    .maxShows(10)
    .minIntervalSeconds(30)
    .debug()  // ← Enable debugging
    .show { next() }
```

**Logcat output:**
```
D/InterstitialBuilder: Not Nth time (call #1, showing every 3rd), skipping ad
D/InterstitialBuilder: Not Nth time (call #2, showing every 3rd), skipping ad
D/InterstitialBuilder: Ad shown successfully
D/InterstitialBuilder: Min interval not met (15000ms < 30000ms, 15000ms remaining), skipping ad
D/InterstitialBuilder: Max shows limit reached (10/10), skipping ad
```

---

## 🔄 Backward Compatibility (v1.3.2 API)

The old API still works for backward compatibility:

### Old API (Still Supported)
```kotlin
// Load ad
AdManager.getInstance().loadInterstitialAd(this, "ad-unit-id")

// Show ad
AdManager.getInstance().forceShowInterstitial(this, object : AdManagerCallback() {
    override fun onNextAction() {
        startNextActivity()
    }
})
```

### Migration to New API
```kotlin
// Old (18 lines)
AdManager.getInstance().loadInterstitialAd(this, "ad-unit",
    object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
            AdManager.getInstance().forceShowInterstitial(this@Activity,
                object : AdManagerCallback() {
                    override fun onNextAction() {
                        next()
                    }
                    override fun onFailedToLoad(error: AdError?) {
                        next()
                    }
                })
        }
        override fun onAdFailedToLoad(error: LoadAdError) {
            next()
        }
    })

// New (3 lines) ✨
showInterstitialAd("ad-unit") {
    next()
}
```

**Reduction: 85% fewer lines of code!**

---

## 🎯 Recommended Configurations

### For Games
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(3)         // Every 3rd game over
    .maxShows(20)            // Reasonable limit
    .minIntervalSeconds(45)  // 45s between
    .show { restart() }
```

### For News/Content Apps
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(2)         // Every other article
    .minIntervalSeconds(90)  // 90s minimum
    .show { nextArticle() }
```

### For Utility Apps
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(5)          // Every 5th action
    .maxShows(10)             // Limited exposure
    .minIntervalSeconds(120)  // 2 minutes
    .show { next() }
```

---

## 🏗️ Setup & Integration

### Installation
Add to your `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:AdManageKit:2.5.0'
    implementation 'com.github.i2hammad.AdManageKit:admanagekit-core:2.5.0'
    implementation 'com.github.i2hammad.AdManageKit:admanagekit-billing:2.5.0' // Optional
}
```

### Application Setup
```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize AdMob
        MobileAds.initialize(this)

        // Configure aggressive loading (optional)
        AdManager.getInstance().enableAggressiveAdLoading()

        // Preload first ad (optional)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.preloadInterstitialAd("ca-app-pub-xxxxx/yyyyy")
                unregisterActivityLifecycleCallbacks(this)
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
```

---

## 📈 Best Practices

### 1. Always Use Fallbacks
```kotlin
// ✅ GOOD: Multiple ad units
InterstitialAdBuilder.with(this)
    .adUnit("primary")
    .fallbacks("backup-1", "backup-2")
    .show { next() }

// ❌ BAD: Single ad unit
InterstitialAdBuilder.with(this)
    .adUnit("primary")
    .show { next() }
```

### 2. Preload for Better UX
```kotlin
// ✅ GOOD: Preload early
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    preloadInterstitialAd("ad-unit-id")
}

// Show later (instant display)
showInterstitialAd("ad-unit-id") { next() }
```

### 3. Control Frequency
```kotlin
// ✅ GOOD: Balanced
.everyNthTime(3)
.maxShows(10)
.minIntervalSeconds(60)

// ❌ BAD: Too aggressive
.everyNthTime(1)  // Every time
.minIntervalSeconds(5)  // Every 5 seconds
```

### 4. Handle Failures Gracefully
```kotlin
// ✅ GOOD: Always proceed
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .onFailed { error ->
        Log.e("Ads", "Failed: ${error.message}")
    }
    .show {
        // Always continue regardless of ad result
        continueFlow()
    }
```

### 5. Use Debug Mode in Development
```kotlin
if (BuildConfig.DEBUG) {
    builder.debug()
}
```

---

## 🐛 Troubleshooting

### Ad Not Showing?

**Enable debug mode:**
```kotlin
.debug()  // See exact reason in logcat
```

**Common reasons:**
- ❌ "Not Nth time" → Adjust `everyNthTime` or remove
- ❌ "Max shows reached" → Increase `maxShows` or reset counter
- ❌ "Min interval not met" → Reduce `minInterval` or wait

**Reset counters:**
```kotlin
AdManager.getInstance().resetAdThrottling()
```

### Ad Loading Slow?

**Use preloading:**
```kotlin
// Preload early
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    preloadInterstitialAd("ad-unit")
}
```

**Use fallback ad units:**
```kotlin
.adUnit("primary")
.fallbacks("backup-1", "backup-2", "backup-3")
```

### Want Loading Indicator?

```kotlin
.withLoadingDialog()  // Shows "Please wait..."
```

---

## 🔧 Advanced Features

### Custom Ad Management
```kotlin
// Get AdManager instance for advanced control
val adManager = AdManager.getInstance()

// Check if ad is ready
if (adManager.isReady()) {
    // Ad is loaded and ready
}

// Get display count
val count = adManager.getAdDisplayCount()

// Get time since last ad
val timeSince = adManager.getTimeSinceLastAd()

// Reset throttling
adManager.resetAdThrottling()

// Enable aggressive loading
adManager.enableAggressiveAdLoading()
```

### Proactive Preloading
```kotlin
// Preload during app usage
override fun onResume() {
    super.onResume()
    AdManager.getInstance().preloadAd(this, "ad-unit")
}
```

---

## 📊 Performance Metrics

### Show Rate Optimization

With proper configuration, expect:

| Configuration | Expected Show Rate |
|---------------|-------------------|
| Basic (no config) | 40-60% |
| With preloading | 60-75% |
| With fallbacks | 75-85% |
| **Optimized (all features)** | **80-90%** ✅ |

### Optimized Configuration
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("primary")
    .fallbacks("backup-1", "backup-2")  // +15% show rate
    .everyNthTime(3)                     // Balanced frequency
    .minIntervalSeconds(30)              // Good UX
    .autoReload(true)                    // Always ready
    .show { next() }
```

---

## 🎓 Additional Resources

### Documentation Files
- [Interstitial Builder Guide](docs/INTERSTITIAL_BUILDER_GUIDE.md) - Complete builder API reference
- [Ad Frequency Control](docs/AD_FREQUENCY_CONTROL.md) - Detailed frequency control guide
- [API Reference](docs/API_REFERENCE.md) - Full API documentation

### Related Documentation
- [Native Ads](native-ads-caching.md)
- [App Open Ads](app-open-ads.md)
- [Banner Ads](banner-ads.md)
- [Billing Integration](billing-integration.md)

### External Resources
- [AdMob Interstitial Ads Documentation](https://developers.google.com/admob/android/interstitial)
- [Firebase Analytics Documentation](https://firebase.google.com/docs/analytics)

---

## 🔄 Migration Guide

### From v1.3.2 to v2.5.0

#### Step 1: Update Dependencies
```groovy
// Old
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:1.3.2'

// New
implementation 'com.github.i2hammad.AdManageKit:AdManageKit:2.5.0'
```

#### Step 2: Update Code (Optional)

Old code continues to work, but new API is recommended:

```kotlin
// Old API (still works)
AdManager.getInstance().loadInterstitialAd(this, "ad-unit")
AdManager.getInstance().forceShowInterstitial(this, callback)

// New API (recommended)
showInterstitialAd("ad-unit") { next() }
```

#### Step 3: Add Frequency Control (Recommended)
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(3)
    .minIntervalSeconds(60)
    .show { next() }
```

---

## 🎉 Summary

### Why Use InterstitialAdBuilder?

| Feature | Old API | New API |
|---------|---------|---------|
| **Lines of Code** | 15-20 | **1-5** ✅ |
| **Fallback Support** | Manual | **Built-in** ✅ |
| **Frequency Control** | Manual | **Built-in** ✅ |
| **Debug Mode** | N/A | **Yes** ✅ |
| **Loading Dialog** | Separate method | **.withLoadingDialog()** ✅ |
| **Callbacks** | Nested | **Fluent** ✅ |
| **Extension Functions** | No | **Yes** ✅ |
| **Type Safety** | Weak | **Strong** ✅ |

### Key Benefits
- ✅ **85% less code** than old API
- ✅ **Automatic fallback** chain for higher fill rate
- ✅ **Smart frequency controls** for better UX
- ✅ **Debug mode** for easy troubleshooting
- ✅ **Extension functions** for Kotlin
- ✅ **Fully backward compatible**

---

**Start using the new API today for cleaner, more maintainable ad code!** 🚀
