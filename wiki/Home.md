# AdManageKit

[![JitPack](https://jitpack.io/v/i2hammad/AdManageKit.svg)](https://jitpack.io/#i2hammad/AdManageKit)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

AdManageKit is a comprehensive Android library designed to simplify the integration and management of Google AdMob ads, Google Play Billing, and User Messaging Platform (UMP) consent.

**Latest Version: `2.8.0`**

## What's New in 2.8.0

- **Loading Strategy for AdManager**: `forceShowInterstitial()` now respects global loading strategy
- **Global Auto-Reload Config**: New `interstitialAutoReload` setting with per-call override
- **New Method**: `forceShowInterstitialAlways()` for explicit force fetch behavior

## Features

### AdMob Ads Management
- **Banner Ads**: Auto-refresh, collapsible banners, smart retry
- **Native Ads**: Small, Medium, Large formats with caching
- **Interstitial Ads**: Time/count-based triggers, dialog support, loading strategies
- **App Open Ads**: Lifecycle-aware with activity exclusion

### NativeTemplateView (v2.6.0+)
- 17 Template Styles: card_modern, material3, minimal, list_item, magazine, video templates
- XML & Programmatic: Set templates via `app:adTemplate` or `setTemplate()`
- Material 3 Theming: Automatic dark/light mode support

### Ad Loading Strategies (v2.6.0+)
- **ON_DEMAND**: Fetch fresh ads with loading dialog
- **ONLY_CACHE**: Instant display from cache
- **HYBRID**: Cache-first with fallback fetch (recommended)

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
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.8.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.8.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.8.0'

// For Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.8.0'
```

**Step 3:** Sync your project with Gradle.

### Quick Configuration

Configure AdManageKit in your Application class:

```kotlin
class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()

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

        // Set up billing
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

        // Initialize app open ads
        appOpenManager = AppOpenManager(this, "your-app-open-ad-unit-id")
    }
}
```

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
