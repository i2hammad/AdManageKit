# InterstitialAdBuilder - Complete Guide

## ✨ Overview

The `InterstitialAdBuilder` provides a modern, fluent API for loading and showing interstitial ads with advanced features like fallback support, automatic retry, and customizable callbacks.

---

## 🚀 Basic Usage

### 1. Simple Show
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .show {
        // Continue with next action
        startNextActivity()
    }
```

### 2. Using Extension Function
```kotlin
showInterstitialAd("ca-app-pub-xxxxx/yyyyy") {
    startNextActivity()
}
```

---

## 🔧 Builder Methods

### Configuration Methods

| Method | Description | Example |
|--------|-------------|---------|
| `.adUnit(String)` | Set primary ad unit ID (**required**) | `.adUnit("ca-app-pub-xxx/123")` |
| `.fallback(String)` | Add single fallback ad unit | `.fallback("ca-app-pub-xxx/456")` |
| `.fallbacks(vararg String)` | Add multiple fallback units | `.fallbacks("unit1", "unit2", "unit3")` |
| `.force()` | Show ad ignoring time interval | `.force()` |
| `.respectInterval(Boolean)` | Respect time interval (default: true) | `.respectInterval(false)` |
| `.timeout(Long)` | Set load timeout in milliseconds | `.timeout(5000)` |
| `.timeoutSeconds(Int)` | Set load timeout in seconds | `.timeoutSeconds(5)` |
| `.debug()` | Enable debug logging | `.debug()` |

### Loading Strategy Methods

| Method | Description | Example |
|--------|-------------|---------|
| `.loadingStrategy(Strategy)` | Set how ads are loaded/shown | `.loadingStrategy(AdLoadingStrategy.HYBRID)` |
| `.waitForLoading()` | Smart wait for splash screens (see below) | `.waitForLoading()` |

### Frequency Control Methods

| Method | Description | Example |
|--------|-------------|---------|
| `.everyNthTime(Int)` | Show only every Nth call | `.everyNthTime(3)` |
| `.maxShows(Int)` | Maximum total shows in session | `.maxShows(5)` |
| `.minInterval(Long)` | Minimum ms between shows | `.minInterval(30000)` |
| `.minIntervalSeconds(Int)` | Minimum seconds between shows | `.minIntervalSeconds(30)` |

### Callback Methods

| Method | Description | Parameters |
|--------|-------------|------------|
| `.onAdLoaded(() -> Unit)` | Called when ad loaded successfully | None |
| `.onAdShown(() -> Unit)` | Called when ad is displayed | None |
| `.onFailed((LoadAdError) -> Unit)` | Called when loading/showing fails | error: LoadAdError |

### Terminal Methods

| Method | Description |
|--------|-------------|
| `.show(() -> Unit)` | Load and show ad, then execute callback |
| `.preload()` | Just preload ad without showing |

---

## 🎯 Loading Strategies

### Available Strategies

| Strategy | Behavior | Best For |
|----------|----------|----------|
| `ON_DEMAND` | Always fetch fresh ad with loading dialog | Important moments, max revenue |
| `ONLY_CACHE` | Only show if cached, skip otherwise | Smooth gameplay, no interruption |
| `HYBRID` | Check cache first, fetch if needed (default) | Balanced UX and fill rate |

### Strategy Examples

```kotlin
// ON_DEMAND: Always fetch fresh (shows loading dialog)
InterstitialAdBuilder.with(activity)
    .adUnit(adUnitId)
    .loadingStrategy(AdLoadingStrategy.ON_DEMAND)
    .show { next() }

// ONLY_CACHE: Show only if ready, no network request
InterstitialAdBuilder.with(activity)
    .adUnit(adUnitId)
    .loadingStrategy(AdLoadingStrategy.ONLY_CACHE)
    .show { next() }

// HYBRID: Try cache first, fetch with dialog if needed
InterstitialAdBuilder.with(activity)
    .adUnit(adUnitId)
    .loadingStrategy(AdLoadingStrategy.HYBRID)
    .show { next() }
```

---

## 🚀 Splash Screen Pattern

Use `.waitForLoading()` for optimal splash screen ad experience:

### How It Works

| Ad State | Action |
|----------|--------|
| **READY** | Shows immediately |
| **LOADING** | Waits with timeout, then shows |
| **NEITHER** | Force fetches with dialog |

### Recommended Pattern

```kotlin
// Step 1: In Application.onCreate() - Start loading early
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdManager.getInstance().loadInterstitialAd(this, "ca-app-pub-xxx/yyy")
    }
}

// Step 2: In SplashActivity - Use waitForLoading()
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Smart wait: shows cached, waits for loading, or force fetches
        InterstitialAdBuilder.with(this)
            .adUnit("ca-app-pub-xxx/yyy")
            .waitForLoading()         // Smart splash behavior
            .timeout(5000)            // 5 second max wait
            .show {
                startMainActivity()
            }
    }
}
```

### Alternative: Direct AdManager Usage

```kotlin
// Check states manually
val adManager = AdManager.getInstance()

when {
    adManager.isReady() -> {
        // Ad cached, show immediately
        adManager.forceShowInterstitial(activity, callback)
    }
    adManager.isLoading() -> {
        // Ad loading, use showOrWaitForAd
        adManager.showOrWaitForAd(activity, callback, timeoutMillis = 5000)
    }
    else -> {
        // Nothing happening, force fetch
        adManager.forceShowInterstitialWithDialog(activity, callback)
    }
}
```

---

## 📚 Complete Examples

### Example 1: Basic Navigation
```kotlin
class GameOverActivity : AppCompatActivity() {

    private fun playAgain() {
        InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_ad_unit))
            .show {
                // Ad shown (or failed), restart game
                startActivity(Intent(this, GameActivity::class.java))
                finish()
            }
    }
}
```

### Example 2: With Fallback Chain
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/primary")
    .fallback("ca-app-pub-xxxxx/backup-1")
    .fallback("ca-app-pub-xxxxx/backup-2")
    .force()  // Ignore time interval
    .show {
        goToNextScreen()
    }
```

### Example 3: With All Callbacks
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .onAdLoaded {
        Log.d("Ads", "Ad loaded successfully")
        analytics.log("ad_loaded")
    }
    .onAdShown {
        Log.d("Ads", "Ad shown to user")
        analytics.log("ad_shown")
    }
    .onFailed { error ->
        Log.e("Ads", "Ad failed: ${error.message}")
        analytics.log("ad_failed", "error_code" to error.code)
    }
    .show {
        proceedToNextScreen()
    }
```

### Example 4: With Loading Dialog
```kotlin
// Show "Please wait..." dialog while ad loads
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .withLoadingDialog()
    .show {
        continueFlow()
    }
```

### Example 5: Count-Limited Ads
```kotlin
// Only show ad first 5 times
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .maxShows(5)  // After 5 displays, skip ad
    .show {
        nextAction()
    }
```

### Example 6: Debug Mode
```kotlin
// Enable debug logging for development
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .debug()  // Logs all events
    .show {
        next()
    }
```

### Example 7: Preload Pattern
```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Preload ad on activity create
        preloadInterstitialAd("ca-app-pub-xxxxx/yyyyy")
    }

    private fun showAdBeforeExit() {
        // Ad already preloaded, shows instantly
        showInterstitialAd("ca-app-pub-xxxxx/yyyyy") {
            finish()
        }
    }
}
```

### Example 8: Complex Configuration
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/primary")
    .fallbacks("backup-1", "backup-2", "backup-3")
    .force()
    .autoReload(true)
    .withLoadingDialog()
    .onAdLoaded {
        Toast.makeText(this, "Ad ready!", Toast.LENGTH_SHORT).show()
    }
    .onAdShown {
        FirebaseAnalytics.getInstance(this).logEvent("interstitial_shown", null)
    }
    .onFailed { error ->
        Toast.makeText(this, "No ad available", Toast.LENGTH_SHORT).show()
    }
    .debug()
    .show {
        startActivity(Intent(this, NextActivity::class.java))
        finish()
    }
```

---

## 🎯 Best Practices

### 1. Always Use Fallbacks
```kotlin
// ✅ GOOD: Multiple ad units for higher fill rate
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
class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Preload immediately
        preloadInterstitialAd("ad-unit-id")
    }

    fun showAd() {
        // Ad already loaded, instant display
        showInterstitialAd("ad-unit-id") { next() }
    }
}
```

### 3. Handle Failures Gracefully
```kotlin
// ✅ GOOD: Always handle errors
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .onFailed { error ->
        // Log error but continue
        Log.e("Ads", "Failed: ${error.message}")
    }
    .show {
        // Always proceed regardless of ad result
        continueFlow()
    }
```

### 4. Use Debug Mode in Development
```kotlin
// ✅ GOOD: Debug mode for testing
if (BuildConfig.DEBUG) {
    InterstitialAdBuilder.with(this)
        .adUnit("ad-unit")
        .debug()  // See all logs
        .show { next() }
}
```

---

## ⚡ Performance Tips

1. **Preload Early**: Load ads when app starts or on previous screen
2. **Use Multiple Fallbacks**: 3-5 ad units for best fill rate
3. **Enable Auto-Reload**: Ensures next ad is always ready
4. **Respect Intervals**: Don't spam users with too many ads
5. **Track Analytics**: Monitor success rates to optimize

---

## 🔍 Comparison: Old vs New API

### Old Approach (Verbose)
```kotlin
val adManager = AdManager.getInstance()
adManager.loadInterstitialAd(this, "ad-unit",
    object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
            adManager.forceShowInterstitial(this@Activity,
                object : AdManagerCallback {
                    override fun onAdLoaded() {}
                    override fun onNextAction() {
                        nextScreen()
                    }
                    override fun onFailedToLoad(error: AdError?) {
                        nextScreen()
                    }
                })
        }
        override fun onAdFailedToLoad(error: LoadAdError) {
            nextScreen()
        }
    })
```
**Lines: 18** | **Readability: Poor**

### New Approach (Builder)
```kotlin
showInterstitialAd("ad-unit") {
    nextScreen()
}
```
**Lines: 3** | **Readability: Excellent** ✨

---

## 📊 Feature Matrix

| Feature | Old API | Builder API |
|---------|---------|-------------|
| Lines of code | 15-20 | **1-5** ✅ |
| Fallback support | Manual | **Built-in** ✅ |
| Loading dialog | Separate method | **.withLoadingDialog()** ✅ |
| Debug mode | N/A | **.debug()** ✅ |
| Count limiting | Manual tracking | **.maxShows()** ✅ |
| Callbacks | Nested | **Fluent** ✅ |
| Type safety | Weak | **Strong** ✅ |
| Extension functions | No | **Yes** ✅ |

---

## 🎓 Advanced Patterns

### Pattern 1: Context-Aware Ads
```kotlin
fun showContextualAd(activity: Activity, context: String) {
    val adUnit = when (context) {
        "game_over" -> "ca-app-pub-xxx/game-over"
        "level_complete" -> "ca-app-pub-xxx/level-complete"
        else -> "ca-app-pub-xxx/default"
    }

    InterstitialAdBuilder.with(activity)
        .adUnit(adUnit)
        .show { activity.finish() }
}
```

### Pattern 2: Retry with Different Unit
```kotlin
var retryCount = 0
fun showAdWithRetry() {
    val units = listOf("primary", "backup-1", "backup-2")

    InterstitialAdBuilder.with(this)
        .adUnit(units[retryCount])
        .onFailed {
            if (retryCount < units.size - 1) {
                retryCount++
                showAdWithRetry()  // Retry with next unit
            }
        }
        .show { next() }
}
```

### Pattern 3: A/B Testing
```kotlin
fun showAdWithABTest() {
    val variant = if (Random.nextBoolean()) "variant-a" else "variant-b"

    InterstitialAdBuilder.with(this)
        .adUnit("ca-app-pub-xxx/$variant")
        .onAdShown {
            analytics.log("ab_test_shown", "variant" to variant)
        }
        .show { next() }
}
```

---

## 🐛 Troubleshooting

### Ad Not Showing?
```kotlin
// Enable debug mode to see logs
InterstitialAdBuilder.with(this)
    .adUnit("your-ad-unit")
    .debug()  // ← Add this
    .show { next() }
```

### Want Loading Indicator?
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("your-ad-unit")
    .withLoadingDialog()  // ← Shows "Please wait..."
    .show { next() }
```

### Need Multiple Attempts?
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("primary")
    .fallback("backup-1")     // ← Try this if primary fails
    .fallback("backup-2")     // ← Try this if backup-1 fails
    .show { next() }
```

---

## 📝 Summary

The `InterstitialAdBuilder` makes showing ads:
- ✅ **Simpler**: 1-3 lines vs 15-20
- ✅ **Safer**: Type-safe, required validation
- ✅ **Smarter**: Built-in fallbacks & retry
- ✅ **Better**: Debug mode, callbacks, customization

**Start using it today for cleaner, more maintainable ad code!** 🚀
