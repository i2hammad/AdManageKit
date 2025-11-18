# Ad Frequency Control Guide

## ✨ Overview

InterstitialAdBuilder now provides **three powerful ways** to control when and how often ads are shown:

1. **📊 everyNthTime** - Show every Nth time method is called
2. **🔢 maxShows** - Limit total number of times ad is shown
3. **⏱️ minInterval** - Enforce minimum time between shows

---

## 🎯 Configuration Methods

### 1. `everyNthTime(n)` - Show Every Nth Call

Show ad only on specific call intervals (3rd, 6th, 9th, etc.)

```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .everyNthTime(3)  // Show on 3rd, 6th, 9th... call
    .show { next() }
```

**When to use:**
- ✅ Show ad every 3 level completions
- ✅ Show after every 5 button clicks
- ✅ Show every Nth game over

**Example - Every 3rd Level:**
```kotlin
class GameActivity : AppCompatActivity() {

    private val adBuilder = InterstitialAdBuilder.with(this)
        .adUnit("ca-app-pub-xxxxx/yyyyy")
        .everyNthTime(3)  // Only every 3rd time
        .debug()

    fun onLevelComplete() {
        // Called every level
        // Ad shows only on levels 3, 6, 9, 12, etc.
        adBuilder.show {
            loadNextLevel()
        }
    }
}
```

**Behavior:**
```
Call 1: ❌ Skip (not 3rd)
Call 2: ❌ Skip (not 3rd)
Call 3: ✅ SHOW AD (3rd call)
Call 4: ❌ Skip (not 3rd)
Call 5: ❌ Skip (not 3rd)
Call 6: ✅ SHOW AD (6th call)
```

---

### 2. `maxShows(n)` - Limit Total Shows

Limit maximum number of times ad can be shown **globally** (across entire app lifecycle).

```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .maxShows(10)  // Max 10 times total
    .show { next() }
```

**When to use:**
- ✅ Show ad maximum 5 times per session
- ✅ Limit annoying ads for power users
- ✅ Gradually reduce ads as user progresses

**Example - First 5 Times Only:**
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .maxShows(5)  // Only first 5 times
    .show { next() }
```

**Behavior:**
```
Show 1: ✅ SHOW AD (1/5)
Show 2: ✅ SHOW AD (2/5)
Show 3: ✅ SHOW AD (3/5)
Show 4: ✅ SHOW AD (4/5)
Show 5: ✅ SHOW AD (5/5)
Show 6: ❌ Skip (limit reached)
Show 7: ❌ Skip (limit reached)
```

---

### 3. `minInterval(millis)` - Time-Based Control

Enforce minimum time between ad shows. Ad won't show if insufficient time has passed.

```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .minInterval(60000)  // 60 seconds minimum
    .show { next() }
```

**Convenience method:**
```kotlin
.minIntervalSeconds(60)  // Same as minInterval(60000)
```

**When to use:**
- ✅ Prevent ad spam
- ✅ Ensure smooth user experience
- ✅ Respect user's time

**Example - 30 Second Intervals:**
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .minIntervalSeconds(30)  // 30 seconds minimum
    .show { next() }
```

**Behavior:**
```
Time 0s:  ✅ SHOW AD (first show)
Time 10s: ❌ Skip (only 10s passed, need 30s)
Time 20s: ❌ Skip (only 20s passed, need 30s)
Time 35s: ✅ SHOW AD (30s+ passed)
Time 40s: ❌ Skip (only 5s since last show)
Time 70s: ✅ SHOW AD (30s+ passed)
```

**Override with `.force()`:**
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxxxx/yyyyy")
    .minIntervalSeconds(60)
    .force()  // Ignore time interval
    .show { next() }
```

---

## 🎨 Complete Examples

### Example 1: Game - Every 3 Levels
```kotlin
class GameActivity : AppCompatActivity() {

    private val levelCompleteAd = InterstitialAdBuilder.with(this)
        .adUnit(getString(R.string.interstitial_game))
        .everyNthTime(3)  // Every 3rd level
        .debug()

    fun onLevelComplete() {
        levelCompleteAd.show {
            loadNextLevel()
        }
    }
}
```

### Example 2: Limited Ad Exposure
```kotlin
class MainActivity : AppCompatActivity() {

    private fun showWelcomeAd() {
        InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_welcome))
            .maxShows(3)  // Only first 3 times
            .show {
                showMainContent()
            }
    }
}
```

### Example 3: Time-Controlled Ads
```kotlin
class ArticleActivity : AppCompatActivity() {

    private fun showArticleAd() {
        InterstitialAdBuilder.with(this)
            .adUnit(getString(R.string.interstitial_article))
            .minIntervalSeconds(120)  // 2 minutes minimum
            .show {
                loadNextArticle()
            }
    }
}
```

### Example 4: Combined Controls (Recommended!)
```kotlin
class AppActivity : AppCompatActivity() {

    private fun showSmartAd() {
        InterstitialAdBuilder.with(this)
            .adUnit("ca-app-pub-xxxxx/yyyyy")
            .everyNthTime(2)         // Every 2nd call
            .maxShows(10)            // Max 10 times total
            .minIntervalSeconds(30)  // 30s minimum between
            .debug()
            .show {
                continueFlow()
            }
    }
}
```

**Combined behavior:**
- ✅ Must be Nth call
- ✅ AND haven't reached maxShows
- ✅ AND minimum time has passed
- ✅ Only then show ad

---

## 📊 Configuration Combinations

### Combination 1: Gentle Introduction
```kotlin
// Show ad 3 times in first session, then stop
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .maxShows(3)
    .minIntervalSeconds(60)  // But space them out
    .show { next() }
```

### Combination 2: Frequent But Fair
```kotlin
// Show every time, but respect intervals
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .minIntervalSeconds(45)  // 45 second minimum
    .show { next() }
```

### Combination 3: Occasional Ads
```kotlin
// Show every 5th action, max 20 times
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(5)
    .maxShows(20)
    .show { next() }
```

### Combination 4: Conservative Approach
```kotlin
// Rare, time-spaced ads
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(3)
    .maxShows(5)
    .minIntervalSeconds(120)  // 2 minutes
    .show { next() }
```

---

## 🔧 Advanced Usage

### Reset Counter Strategy
```kotlin
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Reset counters on app restart
        if (isNewSession()) {
            AdManager.getInstance().resetAdThrottling()
        }
    }
}
```

### Per-Screen Configuration
```kotlin
class ScreenManager {

    fun getAdForScreen(activity: Activity, screenType: String): InterstitialAdBuilder {
        return when (screenType) {
            "game_over" -> InterstitialAdBuilder.with(activity)
                .adUnit("ca-app-pub-xxx/game")
                .everyNthTime(2)  // Every other time
                .minIntervalSeconds(30)

            "level_complete" -> InterstitialAdBuilder.with(activity)
                .adUnit("ca-app-pub-xxx/level")
                .everyNthTime(3)  // Every 3rd level
                .maxShows(10)

            "menu" -> InterstitialAdBuilder.with(activity)
                .adUnit("ca-app-pub-xxx/menu")
                .minIntervalSeconds(120)  // Rare

            else -> InterstitialAdBuilder.with(activity)
                .adUnit("ca-app-pub-xxx/default")
                .minIntervalSeconds(60)
        }
    }
}
```

### Debug Monitoring
```kotlin
// See detailed logs of all checks
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(3)
    .maxShows(10)
    .minIntervalSeconds(30)
    .debug()  // See all skip reasons in logcat
    .show { next() }
```

**Debug output:**
```
D/InterstitialBuilder: Not Nth time (call #1, showing every 3rd), skipping ad
D/InterstitialBuilder: Not Nth time (call #2, showing every 3rd), skipping ad
D/InterstitialBuilder: Ad shown successfully
D/InterstitialBuilder: Min interval not met (15000ms < 30000ms, 15000ms remaining), skipping ad
```

---

## 📈 Best Practices

### 1. Always Combine Methods
```kotlin
// ✅ GOOD: Multiple controls
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(2)
    .maxShows(15)
    .minIntervalSeconds(45)
    .show { next() }

// ❌ BAD: No frequency control
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .show { next() }  // Shows every time!
```

### 2. Use Debug Mode in Development
```kotlin
if (BuildConfig.DEBUG) {
    builder.debug()  // See why ads are skipped
}
```

### 3. Different Strategies Per Context
```kotlin
// Game over: More frequent
.everyNthTime(2).minIntervalSeconds(30)

// Menu: Less frequent
.everyNthTime(5).minIntervalSeconds(120)
```

### 4. Respect User Experience
```kotlin
// ✅ GOOD: Balanced
.everyNthTime(3)
.maxShows(10)
.minIntervalSeconds(60)

// ❌ BAD: Too aggressive
.everyNthTime(1)  // Every time
.minIntervalSeconds(5)  // Every 5 seconds
```

---

## 🎯 Recommended Configurations

### For Games:
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(3)         // Every 3rd game over
    .maxShows(20)            // Reasonable limit
    .minIntervalSeconds(45)  // 45s between
    .show { restart() }
```

### For News/Content Apps:
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(2)         // Every other article
    .minIntervalSeconds(90)  // 90s minimum
    .show { nextArticle() }
```

### For Utility Apps:
```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ad-unit")
    .everyNthTime(5)          // Every 5th action
    .maxShows(10)             // Limited exposure
    .minIntervalSeconds(120)  // 2 minutes
    .show { next() }
```

---

## 🐛 Troubleshooting

### Ad Not Showing?

Enable debug mode:
```kotlin
.debug()  // See exact reason in logcat
```

Common reasons:
- ❌ "Not Nth time" - Increase everyNthTime or remove
- ❌ "Max shows reached" - Increase maxShows or reset counter
- ❌ "Min interval not met" - Reduce minInterval or wait longer

### Reset Counters:
```kotlin
AdManager.getInstance().resetAdThrottling()
```

---

## 📊 Summary Table

| Method | Purpose | Example | Use When |
|--------|---------|---------|----------|
| `everyNthTime(n)` | Show every Nth call | `.everyNthTime(3)` | Spacing out ad frequency |
| `maxShows(n)` | Limit total shows | `.maxShows(10)` | Capping ad exposure |
| `minInterval(ms)` | Time between shows | `.minInterval(60000)` | Preventing ad spam |
| `minIntervalSeconds(s)` | Time (convenience) | `.minIntervalSeconds(60)` | Same as above, easier |
| `.force()` | Override all | `.force()` | Critical moments only |
| `.debug()` | See logs | `.debug()` | Development/testing |

---

**With these controls, you can create a perfect ad experience that maximizes revenue while respecting users!** 🚀
