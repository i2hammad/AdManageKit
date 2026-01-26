# Interstitial Ads - AdManageKit v2.8.0

## Overview

AdManageKit provides a complete interstitial ad stack with `AdManager` and the fluent `InterstitialAdBuilder`. Features include loading strategies, automatic retry, frequency controls, splash screen support, and Jetpack Compose utilities.

**Library Version**: v2.8.0

## What's New

### v2.8.0
- `forceShowInterstitial()` now respects global loading strategy
- New `forceShowInterstitialAlways()` for explicit force fetch
- Global `interstitialAutoReload` config with per-call override
- All AdManager methods use global auto-reload config as default

### v2.7.0
- Smart splash screen with `waitForLoading()` in InterstitialAdBuilder
- New `isLoading()` and `showOrWaitForAd()` methods
- Frequency controls: `everyNthTime`, `maxShows`, `minInterval`

## Installation

```groovy
dependencies {
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.8.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.8.0'
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.8.0'
    // Optional - Jetpack Compose
    implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.8.0'
}
```

## Configuration

Set up in your `Application.onCreate()`:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

        AdManageKitConfig.apply {
            debugMode = BuildConfig.DEBUG
            defaultInterstitialInterval = 20.seconds
            autoRetryFailedAds = true
            maxRetryAttempts = 3

            // Loading strategy (v2.6.0+)
            interstitialLoadingStrategy = AdLoadingStrategy.HYBRID

            // Auto-reload after showing (v2.8.0+)
            interstitialAutoReload = true  // default: true
        }
    }
}
```

## Option 1: Direct AdManager

### Basic Usage

```kotlin
private val adManager = AdManager.getInstance()

// Preload
fun preloadInterstitial(activity: Activity) {
    adManager.loadInterstitialAd(activity, AD_UNIT_INTERSTITIAL)
}

// Show
fun showInterstitial(activity: Activity) {
    adManager.forceShowInterstitial(activity, object : AdManagerCallback() {
        override fun onNextAction() {
            navigateNext()
        }
    })
}
```

### Key Methods

| Method | Description |
|--------|-------------|
| `loadInterstitialAd(context, adUnitId)` | Preload and cache ad |
| `loadInterstitialAdForSplash(context, unit, timeout, callback)` | Splash-friendly with timeout |
| `forceShowInterstitial(activity, callback)` | Respects loading strategy (v2.8.0+) |
| `forceShowInterstitialAlways(activity, callback)` | Always force fetch (v2.8.0+) |
| `showInterstitialIfReady(activity, callback, reloadAd)` | Show only if cached |
| `showInterstitialAdByTime(activity, callback)` | Time-interval based |
| `showInterstitialAdByCount(activity, callback, maxCount)` | Count-limited |
| `showOrWaitForAd(activity, callback, timeout, showDialog)` | Smart splash (v2.7.0+) |
| `isLoading()` | Check if currently loading (v2.7.0+) |
| `isReady()` | Check if ad is cached |

### Loading Strategy Behavior (v2.8.0+)

`forceShowInterstitial()` now respects `AdManageKitConfig.interstitialLoadingStrategy`:

| Strategy | Behavior |
|----------|----------|
| ON_DEMAND | Always fetch fresh ad with dialog |
| ONLY_CACHE | Show cached if ready, skip otherwise |
| HYBRID | Show cached if ready, fetch fresh otherwise |

```kotlin
// Set strategy
AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.HYBRID

// Now respects strategy
AdManager.getInstance().forceShowInterstitial(activity, callback)

// Always force fetch (bypasses strategy)
AdManager.getInstance().forceShowInterstitialAlways(activity, callback)
```

### Splash Screen Example

```kotlin
// In Application.onCreate() - start loading early
AdManager.getInstance().loadInterstitialAd(this, adUnitId)

// In SplashActivity - smart wait
AdManager.getInstance().showOrWaitForAd(
    activity = this,
    callback = object : AdManagerCallback() {
        override fun onNextAction() { startMainActivity() }
    },
    timeoutMillis = 5000,
    showDialogIfLoading = true
)
```

## Option 2: InterstitialAdBuilder

The builder provides a fluent API with frequency controls and loading strategies.

### Basic Usage

```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("ca-app-pub-xxx/yyy")
    .show { navigateNext() }
```

### Full Configuration

```kotlin
InterstitialAdBuilder.with(this)
    .adUnit("primary-unit")
    .fallback("backup-unit")
    .loadingStrategy(AdLoadingStrategy.HYBRID)
    .everyNthTime(3)           // Show every 3rd call
    .maxShows(10)              // Maximum 10 shows total
    .minIntervalSeconds(30)    // Minimum 30 seconds between
    .timeout(5000)             // 5 second timeout
    .autoReload(true)          // Auto-reload after showing
    .onAdShown { /* analytics */ }
    .onAdDismissed { /* cleanup */ }
    .onFailed { error -> /* handle */ }
    .show { navigateNext() }
```

### Smart Splash Screen (v2.7.0+)

```kotlin
// In Application.onCreate()
AdManager.getInstance().loadInterstitialAd(this, adUnitId)

// In SplashActivity
InterstitialAdBuilder.with(this)
    .adUnit(adUnitId)
    .waitForLoading()     // Smart: shows cached, waits for loading, or force fetches
    .timeout(5000)        // 5 second max wait
    .show { startMainActivity() }
```

**Behavior:**
| Ad State | Action |
|----------|--------|
| READY | Shows immediately |
| LOADING | Waits with timeout, shows when ready |
| NOT STARTED | Force fetches with dialog |

### Frequency Controls

```kotlin
// Show every 3rd time
.everyNthTime(3)

// Maximum 5 shows total
.maxShows(5)

// Minimum 30 seconds between shows
.minIntervalSeconds(30)

// Force show (bypass interval)
.force()
```

### Auto-Reload Configuration (v2.8.0+)

```kotlin
// Global config
AdManageKitConfig.interstitialAutoReload = false  // Disable globally

// Per-call override
InterstitialAdBuilder.with(activity)
    .adUnit(adUnitId)
    .autoReload(true)  // Override global
    .show { next() }
```

**Priority:** Builder `.autoReload()` > `AdManageKitConfig.interstitialAutoReload`

## Option 3: Jetpack Compose

```kotlin
@Composable
fun ContentWithInterstitial() {
    val showAd = rememberInterstitialAd(
        adUnitId = stringResource(R.string.interstitial_feed),
        preloadAd = true,
        onAdShown = { /* analytics */ },
        onAdDismissed = { navigateNext() },
        onAdFailedToLoad = { /* handle */ }
    )

    Button(onClick = showAd) {
        Text("Show Ad")
    }
}
```

### Compose Helpers

| Helper | Purpose |
|--------|---------|
| `rememberInterstitialAd(...)` | Returns lambda to show ad |
| `InterstitialAdEffect(...)` | Declarative effect with TIME/COUNT/FORCE modes |
| `rememberInterstitialAdState(...)` | Mutable state with `isLoaded`, `loadAd()`, `showAd()` |

## API Reference

### AdManager Methods

| Method | Description |
|--------|-------------|
| `loadInterstitialAd(context, adUnitId)` | Preload ad |
| `loadInterstitialAdForSplash(...)` | Splash with timeout |
| `forceShowInterstitial(activity, callback)` | Respects strategy |
| `forceShowInterstitialAlways(activity, callback)` | Always force fetch |
| `showInterstitialIfReady(activity, callback, reloadAd)` | Show cached |
| `showInterstitialAdByTime(activity, callback)` | Time-based |
| `showInterstitialAdByCount(activity, callback, max)` | Count-based |
| `showOrWaitForAd(...)` | Smart splash |
| `isLoading()` | Check loading state |
| `isReady()` | Check if cached |
| `preloadAd(context, adUnitId)` | Background preload |

### InterstitialAdBuilder Methods

| Method | Description |
|--------|-------------|
| `.adUnit(id)` | Set primary ad unit (required) |
| `.fallback(id)` / `.fallbacks(...)` | Fallback units |
| `.loadingStrategy(strategy)` | Override strategy |
| `.everyNthTime(n)` | Show every Nth call |
| `.maxShows(count)` | Limit total shows |
| `.minInterval(ms)` / `.minIntervalSeconds(s)` | Minimum interval |
| `.timeout(ms)` / `.timeoutSeconds(s)` | Load timeout |
| `.autoReload(boolean)` | Override auto-reload |
| `.waitForLoading()` | Smart splash behavior |
| `.force()` | Bypass interval |
| `.onAdShown {}` | Shown callback |
| `.onAdDismissed {}` | Dismissed callback |
| `.onFailed {}` | Failure callback |
| `.show {}` | Execute and show |
| `.preload()` | Preload without showing |

### AdManageKitConfig Settings

| Setting | Description | Default |
|---------|-------------|---------|
| `interstitialLoadingStrategy` | Loading strategy | HYBRID |
| `interstitialAutoReload` | Auto-reload after showing | true |
| `defaultInterstitialInterval` | Time between ads | 15 seconds |
| `defaultAdTimeout` | Load timeout | 15 seconds |
| `autoRetryFailedAds` | Enable retry | true |
| `maxRetryAttempts` | Max retries | 3 |

## Best Practices

1. **Configure once** in `Application.onCreate()`
2. **Use HYBRID strategy** for balanced UX and coverage
3. **Preload** on previous screen for better fill rate
4. **Use frequency controls** to protect UX
5. **Wire billing** to skip ads for premium users
6. **Enable debug mode** during development

## Troubleshooting

- **Ad Not Loading**: Check adUnitId, network, and AdMob config
- **Ad Not Showing**: Verify `isReady()` and purchase status
- **Strategy Not Working**: Ensure v2.8.0+ and check `interstitialLoadingStrategy`

## References

- [AdMob Interstitial Docs](https://developers.google.com/admob/android/interstitial)
- [Firebase Analytics Docs](https://firebase.google.com/docs/analytics)
- [GitHub Repository](https://github.com/i2hammad/AdManageKit)
