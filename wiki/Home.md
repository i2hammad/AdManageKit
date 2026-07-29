# AdManageKit

[![JitPack](https://jitpack.io/v/i2hammad/AdManageKit.svg)](https://jitpack.io/#i2hammad/AdManageKit)
![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

AdManageKit is a comprehensive Android library designed to simplify the integration and management of Google AdMob ads, Google Play Billing, and User Messaging Platform (UMP) consent.

**Latest Version: `4.4.1`**

## What's New in 4.4.1

- **Google Mobile Ads Next-Gen SDK 1.3.0** (from 1.2.1). No AdManageKit API changed
- Repairs to API doc generation and the MCP documentation server

## Recent Highlights

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
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.1'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.1'

// For Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.1'

// For Yandex Ads multi-provider support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.1'
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
