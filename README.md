# AdManageKit
[![JitPack](https://jitpack.io/v/i2hammad/AdManageKit.svg)](https://jitpack.io/#i2hammad/AdManageKit)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

AdManageKit is a comprehensive Android library designed to simplify the integration and management of Google AdMob ads, Google Play Billing, and User Messaging Platform (UMP) consent.

**Latest Version `2.6.0`** introduces **NativeTemplateView** with 17 unified templates, **Ad Loading Strategies**, Material 3 theme support, and enhanced native ad handling.

## What's New in 2.6.0

- **NativeTemplateView**: Single view supporting 17 template styles with XML preview
- **Ad Loading Strategies**: ON_DEMAND, ONLY_CACHE, HYBRID for different use cases
- **Material 3 Theming**: Automatic dark/light mode support
- **Video Support**: 120dp+ MediaView for video ads
- **Smart View Handling**: Auto-hide empty containers, proper alignment

## Screenshots

| NativeBannerSmall Ad | Interstitial Ad | App Open Ad | UMP Consent Form |
|----------------------|-----------------|-----------------|------------------|
| ![NativeBannerSmall ad displayed in app](docs/assets/native_ad_small_screenshot.png) | ![Interstitial ad with loading dialog](docs/assets/interstitial_ad_screenshot.png) | ![App open ad on app launch](docs/assets/app_open_ad_screenshot.png) | ![UMP consent form](docs/assets/ump_consent_screenshot.png) |

## Demo Video

Watch a short demo of `AdManageKit` in action:

[Watch on YouTube](https://youtube.com/shorts/h_872tOARpU)

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
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.6.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.6.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.6.0'

// For Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.6.0'
```

**Step 3:** Sync your project with Gradle.

## Features

### NativeTemplateView (NEW in 2.6.0)
- **17 Template Styles**: card_modern, material3, minimal, list_item, magazine, video templates, and more
- **XML & Programmatic**: Set templates via `app:adTemplate` or `setTemplate()`
- **Material 3 Theming**: Automatic dark/light mode support
- **Video-Ready**: All templates support video ads (120dp+ MediaView)
- [View Documentation](docs/NATIVE_TEMPLATE_VIEW.md)

### Ad Loading Strategies (NEW in 2.6.0)
- **ON_DEMAND**: Fetch fresh ads with loading dialog
- **ONLY_CACHE**: Instant display from cache
- **HYBRID**: Cache-first with fallback fetch (recommended)
- [View Documentation](docs/AD_LOADING_STRATEGIES.md)

### Jetpack Compose Integration
- BannerAdCompose, NativeAdCompose, InterstitialAdCompose
- Programmatic native ads without predefined layouts
- ConditionalAd, CacheWarmingEffect utilities

### AdMob Ads Management
- **Banner Ads**: Auto-refresh, collapsible banners, smart retry
- **Native Ads**: Small, Medium, Large formats with caching
- **Interstitial Ads**: Time/count-based triggers, dialog support
- **App Open Ads**: Lifecycle-aware with activity exclusion

### Centralized Configuration
- **AdManageKitConfig**: Single configuration point
- Environment-specific settings (debug vs production)
- Runtime configuration changes

### Intelligent Native Ad Caching
- Screen-aware caching prevents collisions
- Smart preloading with usage patterns
- LRU cache with configurable expiration

### Reliability & Performance
- Smart retry with exponential backoff
- Circuit breaker for failing ad units
- Memory leak prevention with WeakReference

### Privacy & Compliance
- UMP consent management (GDPR/CCPA)
- Automatic ad hiding for purchased users

### Multi-Module Architecture
- **Core Module**: Shared interfaces and configuration
- **Compose Module**: Jetpack Compose integration
- **Billing Module**: Google Play Billing Library v8

---

## Usage Guide

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

            // Ad Loading Strategies (NEW in 2.6.0)
            interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
            appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
            nativeLoadingStrategy = AdLoadingStrategy.HYBRID
        }

        // Set up billing
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())

        // Initialize app open ads
        appOpenManager = AppOpenManager(this, "your-app-open-ad-unit-id")
    }
}
```

### NativeTemplateView (NEW in 2.6.0)

#### XML Usage

```xml
<com.i2hammad.admanagekit.admob.NativeTemplateView
    android:id="@+id/nativeTemplateView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adTemplate="material3" />
```

#### Available Templates

| Template | Best For |
|----------|----------|
| `card_modern` | General use |
| `material3` | M3 apps |
| `minimal` | Content-focused |
| `compact_horizontal` | Lists |
| `list_item` | RecyclerView items |
| `magazine` | News/blog apps |
| `video_small/medium/large` | Video content |
| `video_square/vertical/fullscreen` | Social feeds |

#### Programmatic Usage

```kotlin
// Load with default template
nativeTemplateView.loadNativeAd(activity, "ca-app-pub-xxx/yyy")

// Change template
nativeTemplateView.setTemplate(NativeAdTemplate.MAGAZINE)
nativeTemplateView.loadNativeAd(activity, "ca-app-pub-xxx/yyy")

// With callback
nativeTemplateView.loadNativeAd(activity, adUnitId, object : AdLoadCallback() {
    override fun onAdLoaded() { /* success */ }
    override fun onFailedToLoad(error: AdError?) { /* error */ }
})

// With strategy override
nativeTemplateView.loadNativeAd(activity, adUnitId, callback, AdLoadingStrategy.ONLY_CACHE)
```

### Banner Ads

```xml
<com.i2hammad.admanagekit.admob.BannerAdView
    android:id="@+id/bannerAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
bannerAdView.loadBanner(this, "ca-app-pub-xxx/yyy")
// Collapsible banner
bannerAdView.loadCollapsibleBanner(this, "ca-app-pub-xxx/yyy", true)
```

### Native Ads (Traditional Views)

```xml
<com.i2hammad.admanagekit.admob.NativeBannerSmall
    android:id="@+id/nativeBannerSmall"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
nativeBannerSmall.loadNativeBannerAd(this, "ca-app-pub-xxx/yyy")
// With caching
nativeBannerSmall.loadNativeBannerAd(activity, adUnitId, useCachedAd = true)
```

### Interstitial Ads

```kotlin
// Load
AdManager.getInstance().loadInterstitialAd(this, "ca-app-pub-xxx/yyy")

// Show immediately
AdManager.getInstance().forceShowInterstitial(this, object : AdManagerCallback() {
    override fun onNextAction() { navigateNext() }
})

// Show with dialog
AdManager.getInstance().forceShowInterstitialWithDialog(this, callback)

// Time-based (every 15 seconds)
AdManager.getInstance().showInterstitialAdByTime(this, callback)

// Count-based
AdManager.getInstance().showInterstitialAdByCount(this, callback, maxDisplayCount = 3)
```

### App Open Ads

```kotlin
// In Application class
appOpenManager = AppOpenManager(this, "ca-app-pub-xxx/yyy")

// Exclude activities
appOpenManager.disableAppOpenWithActivity(MainActivity::class.java)

// Force show
appOpenManager.forceShowAdIfAvailable(activity, callback)

// Skip next ad
appOpenManager.skipNextAd()
```

### Jetpack Compose

```kotlin
@Composable
fun MyScreen() {
    // Banner
    BannerAdCompose(adUnitId = "ca-app-pub-xxx/yyy")

    // NativeTemplateView with any template (NEW in 2.6.0)
    NativeTemplateCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        template = NativeAdTemplate.MATERIAL3,
        loadingStrategy = AdLoadingStrategy.HYBRID
    )

    // Native with loading strategy
    NativeBannerMediumCompose(
        adUnitId = "ca-app-pub-xxx/yyy",
        loadingStrategy = AdLoadingStrategy.ONLY_CACHE
    )

    // Interstitial
    val showInterstitial = rememberInterstitialAd(
        adUnitId = "ca-app-pub-xxx/yyy",
        preloadAd = true
    )
    Button(onClick = { showInterstitial() }) {
        Text("Show Ad")
    }

    // Conditional (hides for purchased users)
    ConditionalAd {
        ProgrammaticNativeBannerMediumCompose(adUnitId = "ca-app-pub-xxx/yyy")
    }
}
```

### UMP Consent

```kotlin
AdsConsentManager.getInstance(this).requestUMP(
    activity = this,
    isDebug = true,
    testDeviceId = "TEST_DEVICE_ID",
    resetConsent = false,
    listener = object : UMPResultListener {
        override fun onCheckUMPSuccess(isConsentGiven: Boolean) {
            if (isConsentGiven) {
                // Initialize and load ads here
                AdManager.getInstance().loadInterstitialAd(activity, adUnitId)
            }
        }
    }
)
```

### In-App Purchases

```kotlin
// Initialize
AppPurchase.getInstance().initBilling(
    application,
    listOf(PurchaseItem("product_id", "", AppPurchase.TYPE_IAP.PURCHASE))
)

// Purchase
AppPurchase.getInstance().purchase(activity, "product_id")

// Listen for results
AppPurchase.getInstance().setPurchaseListener(object : PurchaseListener {
    override fun onProductPurchased(productId: String, originalJson: String) { }
    override fun displayErrorMessage(errorMessage: String) { }
    override fun onUserCancelBilling() { }
})
```

---

## Documentation

- [NativeTemplateView Guide](docs/NATIVE_TEMPLATE_VIEW.md)
- [Ad Loading Strategies](docs/AD_LOADING_STRATEGIES.md)
- [Jetpack Compose Integration](docs/COMPOSE_INTEGRATION.md)
- [Native Ads Caching](docs/native-ads-caching.md)
- [Interstitial Ads](docs/interstitial-ads.md)
- [App Open Ads](docs/app-open-ads.md)
- [API Reference](docs/API_REFERENCE.md)

---

## Migration Guide

### Migrating to 2.6.0

Version 2.6.0 is **fully backward compatible**. Optionally adopt new features:

```kotlin
// Old way (still works)
val nativeBannerMedium = NativeBannerMedium(context)
nativeBannerMedium.loadNativeBannerAd(activity, adUnitId)

// New unified approach
val nativeTemplateView = NativeTemplateView(context)
nativeTemplateView.setTemplate(NativeAdTemplate.CARD_MODERN)
nativeTemplateView.loadNativeAd(activity, adUnitId)
```

---

## Sample Project

The `app` module demonstrates all features. To run:

1. Clone: `git clone https://github.com/i2hammad/AdManageKit.git`
2. Open in Android Studio
3. Replace placeholder AdMob IDs
4. Run on device or emulator

---

## Contributing

1. Fork the repository
2. Create a branch (`git checkout -b feature/YourFeature`)
3. Commit changes (`git commit -m 'Add YourFeature'`)
4. Push (`git push origin feature/YourFeature`)
5. Open a Pull Request

## License

Licensed under the MIT License. See [LICENSE](LICENSE).

## Support

[Buy me a coffee](https://buymeacoffee.com/i2hammad)

For issues: [GitHub Issues](https://github.com/i2hammad/AdManageKit/issues) or [hammadmughal0001@gmail.com](mailto:hammadmughal0001@gmail.com)
