# Interstitial Ads · AdManageKit v2.8.0

## Overview
`AdManageKit` 2.8.0 ships a complete interstitial stack that spans `AdManager`, the fluent `InterstitialAdBuilder`, and brand-new Jetpack Compose utilities. The stack now includes automatic retry with exponential backoff, lifecycle-safe splash loading, fallback ad units, activity-aware purchase gating, and Compose state helpers. Everything is fully backward compatible with the traditional view-based APIs while enabling declarative use in Compose apps.

**What's new in v2.8.0**
- `forceShowInterstitial()` now respects global loading strategy
- New `forceShowInterstitialAlways()` for explicit force fetch
- Global `interstitialAutoReload` config with per-call override
- All AdManager methods use global auto-reload config as default

**What's new in v2.7.0**
- Smart splash screen with `waitForLoading()` in InterstitialAdBuilder
- New `isLoading()` and `showOrWaitForAd()` methods
- Frequency controls: `everyNthTime`, `maxShows`, `minInterval`

**What's new since v1.x**
- Jetpack Compose helpers: `rememberInterstitialAd`, `InterstitialAdEffect`, `rememberInterstitialAdState`
- Fluent builder with frequency controls (`everyNthTime`, `maxShows`, `minInterval`, `.force()`) and fallback chains
- Automatic retry via `AdRetryManager` (circuit breaker removed to maximize show rate)
- Splash-friendly `loadInterstitialAdForSplash(...)` with timeouts and callbacks
- Debug overlays, aggressive pre-loading helpers, and test-mode overrides wired through `AdManageKitConfig`

---

## Installation
Add the v2.8.0 artifacts plus Compose (if needed):

```groovy
dependencies {
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit:v2.8.0"
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.8.0"
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.8.0"
    // Optional – Jetpack Compose helpers:
    implementation "com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.8.0"
}
```

---

## Configure Once
Set the new centralized config inside `Application.onCreate()`:

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
            enableAdaptiveIntervals = true
            enablePerformanceMetrics = BuildConfig.DEBUG

            // Loading strategy (v2.6.0+)
            interstitialLoadingStrategy = AdLoadingStrategy.HYBRID

            // Auto-reload after showing (v2.8.0+)
            interstitialAutoReload = true  // default: true
        }
    }
}
```

`AdManageKitConfig` now drives retry timing, adaptive intervals, loading strategy, auto-reload, analytics, and debug overlays for every interstitial entry-point.

---

## Option 1 · Direct `AdManager`

`AdManager` lives in `com.i2hammad.admanagekit.admob` and exposes imperative control with smart defaults.

```kotlin
private val adManager = AdManager.getInstance()

fun preloadInterstitial(activity: Activity) {
    adManager.loadInterstitialAd(activity, AD_UNIT_INTERSTITIAL)
}

fun showInterstitial(activity: Activity) {
    adManager.showInterstitialAdByTime(activity, object : AdManagerCallback() {
        override fun onNextAction() {
            navigateNext()
        }
    })
}
```

### Key APIs
- `loadInterstitialAdForSplash(context, unit, timeoutMs, callback)` – timeouts + callback chain for splash flows
- `forceShowInterstitial(activity, callback)` – respects loading strategy (v2.8.0+)
- `forceShowInterstitialAlways(activity, callback)` – always force fetch (bypasses strategy, v2.8.0+)
- `showInterstitialIfReady(activity, callback, reloadAd)` – show only if cached
- `showInterstitialAdByTime(...)` & `showInterstitialAdByCount(...)` – throttle via interval or count
- `forceShowInterstitialWithDialog(...)` – built-in loading dialog for smoother UX
- `showOrWaitForAd(activity, callback, timeout, showDialog)` – smart splash screen method (v2.7.0+)
- `isLoading()` – check if ad is currently loading (v2.7.0+)
- `preloadAd(context, adUnitId)` – kicks off background loading for next screen
- `enableAggressiveAdLoading()` / `resetAdThrottling()` – quick helpers for tuning show rate

### Advanced Behavior
- **Purchase-aware**: `BillingConfig` short-circuits loading/showing when a user owns the IAP/subscription.
- **Auto retry**: Failed loads automatically use `AdRetryManager` with exponential backoff based on `AdManageKitConfig`.
- **Analytics**: All impressions, failures, dismissals, and paid events are logged through Firebase Analytics when configured.

### Splash Screen Example
```kotlin
AdManager.getInstance().loadInterstitialAdForSplash(
    context = this,
    adUnitId = getString(R.string.interstitial_splash),
    timeoutMillis = 7_000,
    callback = object : AdManagerCallback() {
        override fun onAdLoaded() { startMainFlow() }
        override fun onFailedToLoad(error: AdError?) { startMainFlow() }
        override fun onNextAction() { startMainFlow() }
    }
)
```

---

## Option 2 · `InterstitialAdBuilder`

The builder (also under `admob`) wraps `AdManager` with a fluent API, fallback chains, loading dialogs, and frequency controls.

```kotlin
InterstitialAdBuilder.with(this)
    .adUnit(getString(R.string.interstitial_home))
    .fallbacks(
        getString(R.string.interstitial_backup_a),
        getString(R.string.interstitial_backup_b)
    )
    .everyNthTime(2)         // show every 2nd invocation
    .maxShows(12)            // cap session exposure
    .minIntervalSeconds(45)  // respect UX
    .withLoadingDialog()
    .onAdShown { analytics.logEvent("interstitial_shown", null) }
    .onFailed { error -> Log.w("Ads", "Failed: ${error.message}") }
    .show { continueToNextScreen() }
```

### Highlights
- `.everyNthTime(n)` – enforce cadence per trigger point
- `.maxShows(count)` – share the global `AdManager` counter to cap exposure
- `.minInterval(millis)` / `.minIntervalSeconds(seconds)` – override global interval without touching config
- `.force()` or `.respectInterval(false)` – bypass timers when needed (e.g., exit flows)
- `.fallback()` / `.fallbacks()` – try multiple ad units automatically
- `.preload()` – warm up future screens without showing

Builder callbacks have Java-friendly interfaces (`OnAdCompleteListener`, `OnAdShownListener`, etc.), so the same API works across Kotlin and Java modules.

---

## Option 3 · Jetpack Compose

The Compose module brings stateful helpers that wrap the same business logic.

```kotlin
@Composable
fun ContentWithInterstitial() {
    val showAd = rememberInterstitialAd(
        adUnitId = stringResource(R.string.interstitial_feed),
        preloadAd = true,
        onAdShown = { analytics.logEvent("feed_interstitial_shown", null) },
        onAdDismissed = { navigateNext() },
        onAdFailedToLoad = { reason -> Log.w("Ads", reason) }
    )

    Button(onClick = showAd) {
        Text("Open next article")
    }
}
```

Additional helpers:

| Helper | Purpose |
|--------|---------|
| `InterstitialAdEffect(adUnitId, showMode, maxDisplayCount)` | Declarative effect that respects TIME / COUNT / FORCE modes. |
| `rememberInterstitialAdState(adUnitId)` | Mutable state holder exposing `isLoaded`, `loadAd()`, `showAdByTime()`, `forceShowAd()` for custom UI flows. |
| `AdManageKitInitEffect()` | Initializes NativeAdManager/FirebaseAnalytics inside Compose entry points so cache stats stay accurate. |

Compose helpers automatically reload on lifecycle events and integrate with `BillingConfig` purchase checks.

---

## Advanced Controls

- **Retry Tuning** – `AdManageKitConfig.autoRetryFailedAds`, `maxRetryAttempts`, `baseRetryDelay`, `maxRetryDelay`, and `enableExponentialBackoff` control how `AdRetryManager` behaves per ad unit.
- **Aggressive Mode** – call `AdManager.enableAggressiveAdLoading()` during experiments to drop the interval to 5 seconds, then `resetAdThrottling()` before going live.
- **Debug Overlay** – enable via `AdManageKitConfig.debugMode = true` and `AdDebugUtils.enableDebugOverlay(activity, true)` to inspect attempts, cache hits, and paid events in-app.
- **Testing** – set `AdManageKitConfig.testMode = true` or map production IDs to test IDs with `AdDebugUtils.setTestAdUnits(mapOf(prodId to testId))`.

---

## API Cheat Sheet

| API | Description |
|-----|-------------|
| `AdManager.loadInterstitialAd(context, adUnitId)` | Preloads and caches one interstitial per unit. |
| `AdManager.loadInterstitialAd(context, unit, callback)` | Same as above but surfaces `InterstitialAdLoadCallback`. |
| `AdManager.loadInterstitialAdForSplash(...)` | Splash-friendly load with timeout + callback. |
| `AdManager.forceShowInterstitial(activity, callback)` | Respects loading strategy (v2.8.0+). |
| `AdManager.forceShowInterstitialAlways(activity, callback)` | Always force fetch, bypasses strategy (v2.8.0+). |
| `AdManager.showInterstitialIfReady(activity, callback, reloadAd)` | Show only if cached. |
| `AdManager.showInterstitialAdByTime` / `showInterstitialAdByCount` | Throttle via time interval or count. |
| `AdManager.forceShowInterstitialWithDialog` | Uses built-in dialog, respects strategy. |
| `AdManager.showOrWaitForAd(...)` | Smart splash screen method (v2.7.0+). |
| `AdManager.isLoading()` | Check if ad is currently loading (v2.7.0+). |
| `AdManager.preloadAd` / `resetAdThrottling` / `enableAggressiveAdLoading` | Utility helpers for show-rate tuning. |
| `InterstitialAdBuilder.with(activity)` | Entry point for the fluent API (Kotlin + Java). |
| `InterstitialAdBuilder.autoReload(Boolean)` | Override global auto-reload setting (v2.8.0+). |
| `InterstitialAdBuilder.waitForLoading()` | Smart splash screen behavior (v2.7.0+). |
| `rememberInterstitialAd`, `InterstitialAdEffect`, `rememberInterstitialAdState` | Compose-first APIs for declarative UI. |

---

## Best Practices

- **Configure once** in `Application` and rely on builder/Compose APIs per screen—don’t sprinkle config logic in activities.
- **Always preload** on the previous screen (builder `.preload()` or `AdManager.preloadAd`) for maximum fill rate.
- **Use frequency controls** (`everyNthTime`, `maxShows`, `minInterval`) plus `AdManageKitConfig.defaultInterstitialInterval` to protect UX.
- **Respect purchases** by always wiring a `BillingPurchaseProvider` before loading—`AdManager` skips ads automatically.
- **Enable debug overlay** in QA builds to verify retries, cache state, and paid events before shipping.

With these updates, interstitial integration stays identical for legacy XML screens, gains new fluent APIs for Java/Kotlin, and becomes fully declarative in Compose-heavy codebases—all powered by the same resilient core.

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
- [AdManageKit v2.5.0 Release Notes](../RELEASE_NOTES_v2.5.0.md)
- [Native Ads Caching Wiki](native-ads-caching.md)
