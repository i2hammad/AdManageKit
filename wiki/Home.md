# AdManageKit

[![JitPack](https://jitpack.io/v/i2hammad/AdManageKit.svg)](https://jitpack.io/#i2hammad/AdManageKit)
![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

AdManageKit is a comprehensive Android library designed to simplify the integration and management of Google AdMob ads, Google Play Billing, and User Messaging Platform (UMP) consent.

**Latest Version: `4.4.5`**

## What's New in 4.4.5

Patch release, no API changes.

- **A banner loaded successfully but rendered blank under Compose** — `BannerAdView` swaps the shimmer placeholder for the loaded `AdView`, which raises an ordinary `requestLayout()`. That request cannot cross Compose's `AndroidView` interop boundary once any ancestor already carries a pending layout flag, so Compose never re-measured the subtree and the freshly attached `AdView` was left at 0×0. The ad loaded, `onAdLoaded`/`onAdImpression` fired and the impression was logged and billed, but the slot stayed blank until a rotation or resize forced a full traversal. Both the AdMob and `BannerWaterfall` success paths now force the measure/layout pass themselves; XML-hosted banners are unaffected
- **Dependencies** — Next-Gen GMA SDK 1.4.0, Android Gradle Plugin 9.3.2, Firebase BOM 34.18.0

## What's New in 4.4.4

Critical billing hotfix, no API changes. **If you ship the billing module on 4.4.3, upgrade.**

- **Billing never connected on 4.4.3** — `connectToGooglePlayBilling()` guarded on `billingClient.isReady()`, which reports `true` the instant a client is built, so `startConnection(...)` was skipped on every fresh client. Setup never ran, no product details or entitlement were fetched, and there was no failure callback and no error log to explain it. The guard is back on `isServiceConnected`; the re-query paths keep the 4.4.3 `isReady()` fix
- **A timed-out setup reported itself as initialized** — the `setBillingListener(listener, timeout)` timeout path set `isBillingInitialized = true` before delivering `SERVICE_TIMEOUT`, so host-app guards of the form `if (!initBillingFinish) initBilling()` went permanently quiet and the connection could never be retried. It now sets `false`
- **Billing connection lifecycle logging** — `initBilling`, `connectToGooglePlayBilling` and both `BillingClientStateListener` callbacks now log their state at `DEBUG` under the `AppPurchase` tag, so this class of failure is visible in `logcat`

## What's New in 4.4.3

Bug-fix and dependency release, no API changes. Two silent-failure bugs, both of which cost money.

> ⚠️ 4.4.3's billing regression makes purchases unusable — use 4.4.4 or newer.

- **A timed-out rewarded load could sabotage the one that replaced it** — a request handed to the SDK cannot be cancelled, and when it finally reported back it could discard an ad a *newer* load had just delivered, clear the loading flag out from under a request still in flight, and fail callers waiting on a load that had not finished. Every load path now carries a generation token, and a late ad is kept for the next show instead of dropped
- **Billing could stop acknowledging purchases for the rest of the process** — the connection flag could latch `false` after one disconnect while the client was actually ready, disabling every purchase re-query including the acknowledgment retry. Play auto-refunds an unacknowledged purchase after 3 days
- **Acknowledgment no longer requires a configured product id** — a `PURCHASED` order the current build does not list (promo grants, dropped products, a Console typo) was never acknowledged, and so was auto-refunded on day 3
- **Dependencies** — Next-Gen GMA SDK 1.3.1, Yandex Mobile Ads 8.3.0, Compose BOM 2026.08.00, Firebase BOM 34.17.0, AppCompat 1.8.0

> **Pending purchases need one thing from your app:** call `refreshPurchases()` from your main activity's `onResume()`. See [[Billing Integration]].

## Recent Highlights

- **4.4.2** — Rewarded callbacks marshalled to the main thread (they could crash the app), a completed purchase could fail to disable ads, blank gaps where banner/native slots should have collapsed, and a `BannerAdView` Activity leak. Behavior changes: an account-hold subscription no longer disables ads, and premium users no longer reserve ad space in Compose
- **4.4.1** — Google Mobile Ads Next-Gen SDK 1.3.0 (from 1.2.1), plus repairs to API doc generation and the MCP documentation server
- **4.4.0** — Subscription offers can be **purchased individually** (`subscribe(activity, offer)`), offer lookup by id/base plan/tag, cross-cadence price normalization (`BillingPeriod`, `getSavingsPercent`), trial eligibility, Play Billing 9 one-time product offers, and client-side **account hold** detection. See [[Subscription Offers]]
- **4.3.x** — All standard banner sizes (`BannerAdSize`), custom native templates, and app-open ad freshness enforcement
- **4.2.0** — Migrated to the Google Mobile Ads **Next-Gen SDK** and Play Billing 9. `MobileAds.initialize()` must now be called explicitly before any ad request

> Upgrading from 3.x or earlier? Read the [Migrating to 4.2.0](https://github.com/i2hammad/AdManageKit#migrating-to-420) notes first — it is the one release in the 4.x line that is not source-compatible.

Full details: [Changelog](https://github.com/i2hammad/AdManageKit/blob/main/CHANGELOG.md) · [Release Notes](https://github.com/i2hammad/AdManageKit/tree/main/docs/release-notes)

## Features

### AdMob Ads Management
- **Banner Ads**: Auto-refresh, collapsible banners, smart retry
- **Native Ads**: Small, Medium, Large formats with caching
- **Interstitial Ads**: Time/count-based triggers, dialog support, loading strategies
- **App Open Ads**: Lifecycle-aware with activity exclusion

### NativeTemplateView (v2.6.0+)
- 38 Template Styles: card_modern, material3, minimal, list_item, magazine, app_store, social_feed, spotlight, plus the video_* and flat_* families
- XML & Programmatic: Set templates via `app:adTemplate` or `setTemplate()`
- Custom Templates (v4.3.0+): supply your own layout via `setCustomTemplate()` or `app:customAdLayout`
- Material 3 Theming: Automatic dark/light mode support

### Ad Loading Strategies (v2.6.0+)
- **ON_DEMAND**: Fetch fresh ads with loading dialog
- **ONLY_CACHE**: Instant display from cache
- **HYBRID**: Cache-first with fallback fetch (recommended)
- **FRESH_WITH_CACHE_FALLBACK**: Fetch fresh, fall back to cache on failure

### Centralized Configuration
- **AdManageKitConfig**: Single configuration point
- Environment-specific settings (debug vs production)
- Runtime configuration changes

### Reliability & Performance
- Smart retry with exponential backoff
- Circuit breaker for failing ad units
- Memory leak prevention with WeakReference

### Privacy & Compliance
- UMP consent management (GDPR/CCPA)
- Automatic ad hiding for purchased users

## Getting Started

### Installation

**Step 1:** Add JitPack to your root `build.gradle`:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2:** Add dependencies to your app's `build.gradle`:

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.5'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.5'

// For Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.5'

// For Yandex Ads multi-provider support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.5'
```

**Step 3:** Ensure your app's `compileSdk` is **37 or higher** (required transitively as of 4.2.0).

**Step 4:** Sync your project with Gradle.

### Quick Configuration

Configure AdManageKit in your Application class:

> **Since 4.2.0 you must call `MobileAds.initialize()` yourself.** The Next-Gen SDK removed the legacy SDK's silent lazy-init, so an app that skips it never loads ads. AdManageKit does not call it for you, because it does not own your consent flow.

```kotlin
class MyApp : Application() {
    var appOpenManager: AppOpenManager? = null

    override fun onCreate() {
        super.onCreate()

        initAds()

        // Set up billing
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

        // Configure AdManageKit
        AdManageKitConfig.apply {
            debugMode = BuildConfig.DEBUG
            enableSmartPreloading = true
            autoRetryFailedAds = true

            // Ad Loading Strategies
            interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
            appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
            nativeLoadingStrategy = AdLoadingStrategy.HYBRID

            // Auto-reload interstitial after showing
            interstitialAutoReload = true  // default: true
        }
    }

    private fun initAds() {
        val config = InitializationConfig.Builder(readApplicationIdFromManifest()).build()

        // initialize() blocks, so keep it off the main thread or it can ANR.
        Thread {
            MobileAds.initialize(this, config)

            // Construct AppOpenManager only after initialize() returns. Constructing it
            // arms its ProcessLifecycleOwner observer, and onStart() fires as soon as any
            // activity starts — created earlier, that observer can race ahead of
            // initialization and issue a load the Next-Gen SDK rejects as "not initialized".
            Handler(Looper.getMainLooper()).post {
                appOpenManager = AppOpenManager(this, "your-app-open-ad-unit-id")
            }
        }.start()
    }
}
```

The Next-Gen SDK no longer reads the application id from the manifest automatically, so `readApplicationIdFromManifest()` pulls `com.google.android.gms.ads.APPLICATION_ID` from your `ApplicationInfo` metadata. See the sample app's [`MyApplication.kt`](https://github.com/i2hammad/AdManageKit/blob/main/app/src/main/java/com/i2hammad/admanagekit/sample/MyApplication.kt) for the full version.

## Wiki Pages

### Ad Types
- [[Interstitial Ads]] - Complete guide to interstitial ad integration
- [[Rewarded Ads]] - Rewarded video ads with callbacks and analytics
- [[App Open Ads]] - App open ad implementation
- [[Native Ads|NativeAdManager]] - Native ad caching and NativeTemplateView
- [[Banner Ads]] - Banner ad integration

### Features
- [[Ad Loading Strategies]] - ON_DEMAND, ONLY_CACHE, HYBRID strategies
- [[Configuration]] - Complete AdManageKitConfig reference
- [[Jetpack Compose]] - Compose integration and helpers

### Multi-Provider Ads
- [[Multi-Provider Waterfall]] - Load ads from multiple networks with automatic fallback
- [[Yandex Integration]] - Yandex Ads SDK provider setup and configuration

### Billing
- [[Billing Integration]] - Play Billing setup, products, and purchase flows
- [[Purchase Categories]] - Product type classification
- [[Consumable Products]] - Consumable in-app purchases
- [[Subscriptions]] - Subscription state, renewal, and account hold
- [[Subscription Offers]] - Multi-offer paywalls, price comparison, trial eligibility
- [[Subscription Upgrades]] - Plan changes and proration modes

## Sample Project

The `app` module demonstrates all features. To run:

1. Clone: `git clone https://github.com/i2hammad/AdManageKit.git`
2. Open in Android Studio
3. Replace placeholder AdMob IDs
4. Run on device or emulator

## Support

[Buy me a coffee](https://buymeacoffee.com/i2hammad)

For issues: [GitHub Issues](https://github.com/i2hammad/AdManageKit/issues) or [hammadmughal0001@gmail.com](mailto:hammadmughal0001@gmail.com)

## License

Licensed under the MIT License. See [LICENSE](https://github.com/i2hammad/AdManageKit/blob/main/LICENSE).
