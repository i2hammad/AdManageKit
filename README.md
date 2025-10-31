# AdManageKit
[![JitPack](https://jitpack.io/v/i2hammad/AdManageKit.svg)](https://jitpack.io/#i2hammad/AdManageKit)
![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

AdManageKit is a comprehensive Android library designed to simplify the integration and management of Google AdMob ads, Google Play Billing, and User Messaging Platform (UMP) consent.

**Latest Version `2.3.0`** introduces **Jetpack Compose support**, major performance improvements, enhanced reliability features, and comprehensive debugging tools while maintaining full backward compatibility.

## What's New in 2.3.0 🚀

### 🎨 **Jetpack Compose Support (NEW)**
- **Native Compose Components**: Purpose-built Composables for all ad types
- **Programmatic Loading**: Load ads without predefined layouts using new ProgrammaticNativeAdLoader
- **Compose-Native State Management**: Proper state handling and lifecycle management
- **Conditional Ad Display**: Built-in purchase status integration with ConditionalAd composable

### 🎯 **Centralized Configuration with AdManageKitConfig**
- **Single Configuration Point**: Control all ad behavior from one place
- **Environment-Specific Settings**: Easy debug vs production configuration
- **Runtime Configuration**: Change settings without code modifications
- **Configuration Validation**: Built-in validation and production readiness checks

### 🚀 **Performance & Reliability**
- **Smart Retry System**: Exponential backoff with configurable retry attempts
- **Memory Leak Prevention**: WeakReference holders and lifecycle-aware components
- **Enhanced Caching**: Screen-aware native ad caching with intelligent preloading
- **Banner Ad Improvements**: Fixed display issues, better retry logic, enhanced auto-refresh

### 🧠 **Intelligent Native Ad Management**
- **NativeAdIntegrationManager**: Screen-specific caching prevents cache collisions
- **Smart Preloading**: Usage pattern-based cache warming
- **Cache Optimization**: LRU eviction, configurable sizes, automatic cleanup
- **Multi-Screen Support**: Dedicated caching per screen type (Small/Medium/Large)

### 🐛 **Advanced Debug & Testing Tools**
- **AdDebugUtils**: Enhanced logging with event tracking and performance metrics
- **Debug Overlays**: Real-time ad statistics and monitoring
- **Test Ad Units**: Safe testing with automatic test ad unit switching
- **Mock Ad Responses**: Unit testing support with injectable mock responses

### 📊 **Enhanced Analytics & Monitoring**
- **Performance Metrics**: Optional detailed analytics collection
- **Circuit Breaker**: Prevents repeated failures and improves performance
- **Debug Statistics**: Real-time monitoring of cache, retries, and ad states
- **Configuration Summary**: Easy configuration debugging and validation

## Features

### 🎨 **Jetpack Compose Integration (NEW)**
- **BannerAdCompose**: Native Compose banner ad component with automatic lifecycle management
- **NativeAdCompose**: Small, Medium, and Large native ad Composables with caching integration
- **ProgrammaticNativeAdCompose**: Load ads without predefined layouts (recommended for Compose)
- **InterstitialAdCompose**: Declarative interstitial ad management with multiple show modes
- **Compose Utilities**: AdManageKitInitEffect, ConditionalAd, CacheWarmingEffect, and more

### 🚀 **AdMob Ads Management (Traditional Views)**
- **Banner Ads**: Enhanced BannerAdView with auto-refresh, collapsible banners, and smart retry logic
- **Native Ads**: Three formats (Small, Medium, Large) with intelligent caching and screen-aware management
- **Interstitial Ads**: Flexible loading with time/count-based triggers and dialog support
- **App Open Ads**: Lifecycle-aware management with activity exclusion support

### 🎯 **Centralized Configuration System**
- **AdManageKitConfig**: Single configuration point for all ad behavior
- **Environment-Specific Settings**: Easy debug vs production configuration switching
- **Runtime Configuration**: Change settings without code modifications
- **Validation & Monitoring**: Built-in configuration validation and production readiness checks

### 🧠 **Intelligent Native Ad Caching**
- **Screen-Aware Caching**: Prevents cache collisions between different screen contexts
- **Smart Preloading**: Usage pattern-based cache warming with configurable preload strategies
- **LRU Cache Management**: Automatic cleanup with configurable size limits and expiration
- **Multi-Screen Support**: Dedicated caching per screen type (Small/Medium/Large)

### 🛡️ **Enhanced Reliability & Performance**
- **Smart Retry System**: Exponential backoff with configurable retry attempts and circuit breaker
- **Memory Leak Prevention**: WeakReference holders and lifecycle-aware components
- **Performance Monitoring**: Optional detailed analytics and real-time performance metrics
- **Auto-Recovery**: Circuit breaker pattern prevents repeated failures and improves stability

### 🐛 **Advanced Debug & Testing Tools**
- **AdDebugUtils**: Enhanced logging with event tracking and performance metrics
- **Debug Overlays**: Real-time ad statistics and monitoring dashboards
- **Test Ad Units**: Safe testing environment with automatic test ad unit switching
- **Mock Ad Responses**: Unit testing support with injectable mock responses and scenarios

### 📊 **Analytics & Monitoring**
- **Firebase Auto-Tracking**: Automatic tROAS tracking for all ad types via Firebase Analytics
- **Performance Metrics**: Detailed analytics collection with configurable granularity
- **Event Logging**: Comprehensive ad lifecycle event tracking and debugging
- **Configuration Summary**: Easy configuration debugging and validation tools

### 💰 **Multi-Module Architecture**
- **Core Module**: Shared interfaces and configuration management
- **Compose Module**: Jetpack Compose integration with native Composables (NEW in v2.3.0)
- **Billing Module**: Google Play Billing Library v8 with enhanced purchase flows
- **Purchase Provider Pattern**: Flexible billing implementations with easy switching

### 🔒 **Privacy & Compliance**
- **UMP Consent Management**: GDPR/CCPA compliance with Google's User Messaging Platform
- **Privacy-Compliant Mode**: Configurable privacy settings for different markets
- **Ad Blocking**: Automatic ad hiding for purchased users with custom error handling

### 📱 **Sample Project & Documentation**
- **Comprehensive Examples**: Fully functional sample demonstrating all features
- **API Documentation**: Detailed usage guides and configuration examples
- **Migration Guides**: Step-by-step upgrade instructions with backward compatibility notes

## Screenshots

Below are screenshots showcasing key features of `AdManageKit`:

| NativeBannerSmall Ad | Interstitial Ad | App Open Ad | UMP Consent Form |
|----------------------|-----------------|-----------------|------------------|
| ![NativeBannerSmall ad displayed in app](docs/assets/native_ad_small_screenshot.png) | ![Interstitial ad with loading dialog](docs/assets/interstitial_ad_screenshot.png) | ![App open ad on app launch](docs/assets/app_open_ad_screenshot.png) | ![UMP consent form](docs/assets/ump_consent_screenshot.png) |

## Demo Video

Watch a short demo of `AdManageKit` in action, showcasing ad loading, caching, and billing:

<video width="100%" controls>
  <source src="docs/assets/demo_video.mp4" type="video/mp4">
  Your browser does not support the video tag.
</video>

[Watch on YouTube](https://youtube.com/shorts/h_872tOARpU)

## Getting Started

### Installation

1. **Add the library to your project**:

   In your root `build.gradle`, add JitPack to the repositories:

   ```groovy
   dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories {
           mavenCentral()
           maven { url 'https://jitpack.io' }
       }
   }
   ```

   In your app's `build.gradle`, add the dependencies:

   **Latest Stable Version (Recommended):**
   ```groovy
   implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.3.0'
   implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.3.0'
   implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.3.0'

   // For Jetpack Compose support (NEW in v2.3.0)
   implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.3.0'
   ```

   **Previous Stable Version:**
   ```groovy
   implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v1.3.2'
   implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v1.3.2'

2. **Sync your project** with Gradle.

### Quick Configuration

Configure AdManageKit in your Application class for optimal performance:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Configure AdManageKit with new centralized configuration
        AdManageKitConfig.apply {
            debugMode = BuildConfig.DEBUG
            enableSmartPreloading = true
            autoRetryFailedAds = true
            maxRetryAttempts = 3
            enablePerformanceMetrics = true
            
            // Advanced features (new in 2.3.0)
            enableAdaptiveIntervals = true
            circuitBreakerThreshold = 5
            nativeCacheExpiry = 2.hours
            maxCachedAdsPerUnit = 3
        }
        
        // Set up billing and ads
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
        
        // Initialize app open ads
        appOpenManager = AppOpenManager(this, "your-app-open-ad-unit-id")
        
        // Enable debug tools (development only)
        if (BuildConfig.DEBUG) {
            AdDebugUtils.enableDebugOverlay(this, true)
        }
    }
}
```

### Enhanced Integration Verification

AdManageKit 2.3.0 includes comprehensive integration across all ad components:

✅ **AdManageKitConfig** - Integrated in all ad components (BannerAdView, NativeBannerSmall, NativeBannerMedium, NativeLarge)  
✅ **NativeAdIntegrationManager** - Screen-aware caching in all native ad formats  
✅ **AdDebugUtils** - Enhanced logging and monitoring across all components  

The library automatically applies configuration settings to:
- **BannerAdView**: Retry logic, auto-refresh intervals, and performance monitoring
- **Native Ads**: Smart caching, preloading strategies, and screen-aware management
- **All Components**: Debug logging, error handling, and analytics integration

## 🎨 Jetpack Compose Usage (NEW)

The new Compose module provides native Composables for seamless integration with Jetpack Compose applications.

### Quick Start with Compose

#### 1. Initialize AdManageKit in Compose

```kotlin
@Composable
fun MyApp() {
    // Initialize AdManageKit with Firebase Analytics
    AdManageKitInitEffect()

    // Your app content
    MyAppContent()
}
```

#### 2. Banner Ads in Compose

```kotlin
@Composable
fun BannerAdExample() {
    Column {
        Text("Welcome to my app!")

        // Simple banner ad
        BannerAdCompose(
            adUnitId = "ca-app-pub-3940256099942544/6300978111",
            onAdLoaded = { println("Banner ad loaded") },
            onAdFailedToLoad = { error -> println("Banner failed: ${error?.message}") }
        )

        Text("More content...")
    }
}
```

#### 3. Native Ads in Compose

```kotlin
@Composable
fun NativeAdExamples() {
    LazyColumn {
        item {
            // Small native ad
            NativeBannerSmallCompose(
                adUnitId = "ca-app-pub-3940256099942544/2247696110",
                useCachedAd = true
            )
        }

        item {
            // Medium native ad
            NativeBannerMediumCompose(
                adUnitId = "ca-app-pub-3940256099942544/2247696110"
            )
        }

        item {
            // Large native ad
            NativeLargeCompose(
                adUnitId = "ca-app-pub-3940256099942544/2247696110"
            )
        }
    }
}
```

#### 4. Programmatic Native Ads (Recommended)

```kotlin
@Composable
fun ProgrammaticNativeAds() {
    Column {
        // Programmatic small native ad with loading indicator
        ProgrammaticNativeBannerSmallCompose(
            adUnitId = "ca-app-pub-3940256099942544/2247696110",
            showLoadingIndicator = true,
            onAdLoaded = { nativeAdView, nativeAd ->
                println("Programmatic ad loaded successfully")
            },
            onAdFailedToLoad = { error ->
                println("Ad failed to load: ${error.message}")
            }
        )

        // Medium programmatic native ad
        ProgrammaticNativeBannerMediumCompose(
            adUnitId = "ca-app-pub-3940256099942544/2247696110"
        )

        // Large programmatic native ad
        ProgrammaticNativeLargeCompose(
            adUnitId = "ca-app-pub-3940256099942544/2247696110"
        )
    }
}
```

#### 5. Interstitial Ads in Compose

```kotlin
@Composable
fun InterstitialAdExample() {
    // Simple interstitial with automatic preloading
    val showInterstitial = rememberInterstitialAd(
        adUnitId = "ca-app-pub-3940256099942544/1033173712",
        preloadAd = true,
        onAdShown = { println("Interstitial shown") },
        onAdDismissed = { println("Interstitial dismissed") }
    )

    Button(onClick = { showInterstitial() }) {
        Text("Show Interstitial Ad")
    }
}

@Composable
fun AdvancedInterstitialExample() {
    // Advanced interstitial with state management
    val interstitialState = rememberInterstitialAdState(
        adUnitId = "ca-app-pub-3940256099942544/1033173712",
        autoLoad = true
    )

    Column {
        Text("Ad Status: ${if (interstitialState.isLoaded) "Ready" else "Loading"}")

        Button(
            onClick = {
                // Show with time-based logic
                interstitialState.showAdByTime()
            },
            enabled = interstitialState.isLoaded
        ) {
            Text("Show Ad (Time-based)")
        }

        Button(
            onClick = {
                // Force show with dialog
                interstitialState.forceShowAdWithDialog()
            },
            enabled = interstitialState.isLoaded
        ) {
            Text("Force Show with Dialog")
        }

        if (interstitialState.lastError != null) {
            Text(
                text = "Error: ${interstitialState.lastError}",
                color = Color.Red
            )
        }
    }
}
```

#### 6. Conditional Ad Display

```kotlin
@Composable
fun ConditionalAdExample() {
    Column {
        Text("My App Content")

        // Only show ads if user hasn't purchased
        ConditionalAd {
            ProgrammaticNativeBannerMediumCompose(
                adUnitId = "ca-app-pub-3940256099942544/2247696110"
            )
        }

        Text("More content...")
    }
}
```

#### 7. Advanced Features

```kotlin
@Composable
fun AdvancedComposeFeatures() {
    // Cache warming for better performance
    CacheWarmingEffect(
        adUnits = mapOf(
            "ca-app-pub-3940256099942544/2247696110" to 2,
            "ca-app-pub-3940256099942544/6300978111" to 1
        ),
        onComplete = { warmedUnits, totalUnits ->
            println("Warmed $warmedUnits out of $totalUnits ad units")
        }
    )

    // Monitor purchase status
    val isPurchased = rememberPurchaseStatus()

    // Monitor performance
    val cacheStats by rememberCacheStatistics()
    val perfStats by rememberPerformanceStats()

    Column {
        Text("Purchase Status: ${if (isPurchased) "Purchased" else "Free"}")
        Text("Cache Hit Rate: ${perfStats["hit_rate_percent"]}%")
        Text("Total Ads Served: ${perfStats["total_ads_served"]}")

        cacheStats.forEach { (adUnit, stats) ->
            Text("$adUnit: $stats")
        }
    }
}
```

### Compose Component Reference

| Component | Description |
|-----------|-------------|
| `BannerAdCompose` | Banner ad with automatic lifecycle management |
| `NativeBannerSmallCompose` | Small native banner (80dp height) |
| `NativeBannerMediumCompose` | Medium native banner (120dp height) |
| `NativeLargeCompose` | Large native ad (300dp height) |
| `ProgrammaticNativeAdCompose` | Generic programmatic native ad (recommended) |
| `ProgrammaticNativeBannerSmallCompose` | Small programmatic native banner |
| `ProgrammaticNativeBannerMediumCompose` | Medium programmatic native banner |
| `ProgrammaticNativeLargeCompose` | Large programmatic native ad |
| `rememberInterstitialAd` | Returns function to show interstitial ads |
| `rememberInterstitialAdState` | Advanced interstitial state management |
| `InterstitialAdEffect` | Declarative interstitial ad management |
| `AdManageKitInitEffect` | Initialize AdManageKit with Firebase Analytics |
| `ConditionalAd` | Conditionally display ads based on purchase status |
| `CacheWarmingEffect` | Pre-load ads for better performance |

### Migration from Traditional Views to Compose

```kotlin
// Old View system
val nativeBannerSmall = NativeBannerSmall(context)
nativeBannerSmall.loadNativeBannerAd(activity, adUnitId)
container.addView(nativeBannerSmall)

// New Compose system
@Composable
fun MyScreen() {
    NativeBannerSmallCompose(
        adUnitId = adUnitId,
        onAdLoaded = { /* handle success */ }
    )
}
```

---

## Traditional View Usage

For traditional Android View system integration, use the following approaches:

### Usage

#### Initializing the Library

Initialize `AdsConsentManager` in your app's first activity to handle UMP consent:

```java
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdsConsentManager adsConsentManager = AdsConsentManager.getInstance(this);
        adsConsentManager.requestUMP(this, true, "TEST_DEVICE_ID", false, new UMPResultListener() {
            @Override
            public void onCheckUMPSuccess(boolean isConsentGiven) {
                if (isConsentGiven && adsConsentManager.canRequestAds()) {
                    // Initialize and load ads
                }
            }
        });
    }
}
```

#### Set Up Purchase Provider Globally

In your `Application` class, configure the billing provider and initialize `AppOpenManager`:

```kotlin
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.billing.BillingPurchaseProvider
import com.i2hammad.admanagekit.admob.AppOpenManager

class MyApp : Application() {
    private lateinit var appOpenManager: AppOpenManager

    override fun onCreate() {
        super.onCreate()
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
        appOpenManager = AppOpenManager(this, "ca-app-pub-3940256099942544/9257395921")
    }
}
```

#### Managing AdMob Ads

##### Banner Ads
Add a banner ad to your layout:

```xml
<com.i2hammad.admanagekit.admob.BannerAdView
    android:id="@+id/bannerAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Load a banner ad:

```kotlin
bannerAdView.loadBanner(this, "ca-app-pub-3940256099942544/9214589741")
// For collapsible banner ad
bannerAdView.loadCollapsibleBanner(this, "ca-app-pub-3940256099942544/2014213617", true)
```

##### Native Ads with Caching
The library supports caching for `NativeBannerSmall`, `NativeBannerMedium`, and `NativeLarge` ads, with per-ad-unit caching and a 1-hour expiration.

###### NativeBannerSmall
Add to your layout:

```xml
<com.i2hammad.admanagekit.admob.NativeBannerSmall
    android:id="@+id/nativeBannerSmall"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Load the ad:

```kotlin
nativeBannerSmall.loadNativeBannerAd(this, "ca-app-pub-3940256099942544/2247696110")
// Load cached ad (if available and not expired)
nativeBannerSmall.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = true)
// With callback
nativeBannerSmall.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = false, object : AdLoadCallback {
    override fun onAdLoaded() { Log.d("Ad", "Small Ad loaded") }
    override fun onFailedToLoad(adError: AdError) { Log.d("Ad", "Small Ad failed: ${adError.message}") }
    override fun onAdImpression() { Log.d("Ad", "Small Ad impression") }
    override fun onAdClicked() { Log.d("Ad", "Small Ad clicked") }
    override fun onAdClosed() { Log.d("Ad", "Small Ad closed") }
    override fun onAdOpened() { Log.d("Ad", "Small Ad opened") }
})
```

###### NativeBannerMedium
Add to your layout:

```xml
<com.i2hammad.admanagekit.admob.NativeBannerMedium
    android:id="@+id/nativeBannerMedium"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Load the ad:

```kotlin
nativeBannerMedium.loadNativeBannerAd(this, "ca-app-pub-3940256099942544/2247696110")
// Load cached ad
nativeBannerMedium.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = true)
// With callback
nativeBannerMedium.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = false, object : AdLoadCallback {
    override fun onAdLoaded() { Log.d("Ad", "Medium Ad loaded") }
    override fun onFailedToLoad(adError: AdError) { Log.d("Ad", "Medium Ad failed: ${adError.message}") }
    override fun onAdImpression() { Log.d("Ad", "Medium Ad impression") }
    override fun onAdClicked() { Log.d("Ad", "Medium Ad clicked") }
    override fun onAdClosed() { Log.d("Ad", "Medium Ad closed") }
    override fun onAdOpened() { Log.d("Ad", "Medium Ad opened") }
})
```

###### NativeLarge
Add to your layout:

```xml
<com.i2hammad.admanagekit.admob.NativeLarge
    android:id="@+id/nativeLarge"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Load the ad:

```kotlin
nativeLarge.loadNativeAds(this, "ca-app-pub-3940256099942544/2247696110")
// Load cached ad
nativeLarge.loadNativeAds(activity, "your-ad-unit-id", useCachedAd = true)
// With callback
nativeLarge.loadNativeAds(activity, "your-ad-unit-id", useCachedAd = false, object : AdLoadCallback {
    override fun onAdLoaded() { Log.d("Ad", "Large Ad loaded") }
    override fun onFailedToLoad(adError: AdError) { Log.d("Ad", "Large Ad failed: ${adError.message}") }
    override fun onAdImpression() { Log.d("Ad", "Large Ad impression") }
    override fun onAdClicked() { Log.d("Ad", "Large Ad clicked") }
    override fun onAdClosed() { Log.d("Ad", "Large Ad closed") }
    override fun onAdOpened() { Log.d("Ad", "Large Ad opened") }
})
```

###### Managing Native Ads Caching
Control caching via `NativeAdManager`:

```kotlin
// Enable caching (default)
NativeAdManager.enableCachingNativeAds = true
// Disable caching
NativeAdManager.enableCachingNativeAds = false
// Clear cache for a specific ad unit
NativeAdManager.clearCachedAd("your-ad-unit-id")
// Clear all cached ads
NativeAdManager.clearAllCachedAds()
```

For detailed documentation, see the [Native Ads Caching Wiki](docs/native-ads-caching.md).

##### Interstitial Ads
Load an interstitial ad (automatically cached for later use):

```kotlin
AdManager.getInstance().loadInterstitialAd(this, "ca-app-pub-3940256099942544/1033173712")
```

Display the interstitial ad immediately:

```kotlin
AdManager.getInstance().forceShowInterstitial(this, object : AdManagerCallback() {
    override fun onNextAction() {
        val intent = Intent(this@InterstitialActivity, MainActivity::class.java)
        startActivity(intent)
    }
})
```

Display with a loading dialog:

```kotlin
AdManager.getInstance().forceShowInterstitialWithDialog(this, object : AdManagerCallback() {
    override fun onNextAction() {
        val intent = Intent(this@InterstitialActivity, MainActivity::class.java)
        startActivity(intent)
    }
})
```

Display based on time interval (e.g., every 15 seconds):

```kotlin
AdManager.getInstance().showInterstitialAdByTime(this, object : AdManagerCallback() {
    override fun onNextAction() {
        val intent = Intent(this@InterstitialActivity, MainActivity::class.java)
        startActivity(intent)
    }
})
```

Display based on ad display count (e.g., up to 3 times):

```kotlin
AdManager.getInstance().showInterstitialAdByCount(this, object : AdManagerCallback() {
    override fun onNextAction() {
        val intent = Intent(this@InterstitialActivity, MainActivity::class.java)
        startActivity(intent)
    }, maxDisplayCount = 3)
```

For detailed documentation, see the [Interstitial Ads Wiki](docs/interstitial-ads.md).

##### App Open Ads
Initialize `AppOpenManager` in your `Application` class (see above). Exclude specific activities from showing app open ads:

```kotlin
appOpenManager.disableAppOpenWithActivity(MainActivity::class.java)
```

Force show an app open ad:

```kotlin
appOpenManager.forceShowAdIfAvailable(activity, object : AdManagerCallback() {
    override fun onNextAction() {
        Log.d("AppOpenAd", "Ad dismissed or failed, proceed with next action")
    }
    override fun onAdLoaded() {
        Log.d("AppOpenAd", "Ad displayed successfully")
    }
})
```

Skip the next app open ad:

```kotlin
appOpenManager.skipNextAd()
```

For detailed documentation, see the [App Open Ads Wiki](docs/app-open-ads.md).

#### Handling In-App Purchases

1. **Initialize the Billing Client**:

   For current version:
   ```java
   AppPurchase.getInstance().initBilling(getApplication(), Arrays.asList(new PurchaseItem("your_product_id", "", AppPurchase.TYPE_IAP.PURCHASE)));
   ```

2. **Start a Purchase Flow**:

   ```java
   AppPurchase.getInstance().purchase(activity, "your_product_id");
   ```

3. **Handle Purchase Results**:

   ```java
   AppPurchase.getInstance().setPurchaseListener(new PurchaseListener() {
       @Override
       public void onProductPurchased(String productId, String originalJson) {
           // Handle successful purchase
       }
       @Override
       public void displayErrorMessage(String errorMessage) {
           // Handle error
       }
       @Override
       public void onUserCancelBilling() {
           // Handle cancellation
       }
   });
   ```

4. **Consume Purchases** (if consumable):

   ```java
   AppPurchase.getInstance().consumePurchase("your_product_id");
   ```

5. **Query Product Details ( v2.0.1)**:

   ```java
   AppPurchase.getInstance().queryProductDetails(Arrays.asList("your_product_id"), BillingClient.ProductType.INAPP);
   ```

6. **Get Price Information ( v2.0.1**:

   ```java
   String price = AppPurchase.getInstance().getPrice("your_product_id");
   String currency = AppPurchase.getInstance().getCurrency("your_product_id", AppPurchase.TYPE_IAP.PURCHASE);
   double priceWithoutCurrency = AppPurchase.getInstance().getPriceWithoutCurrency("your_product_id", AppPurchase.TYPE_IAP.PURCHASE);
   ```

For detailed billing documentation, see the [Billing Management Wiki](docs/billing-management.md).

## Advanced Features (New in 2.3.0)

### Smart Configuration Management

AdManageKit now provides centralized configuration for optimal performance:

```kotlin
// Configure in Application.onCreate()
AdManageKitConfig.apply {
    // Performance settings
    defaultAdTimeout = 15.seconds
    nativeCacheExpiry = 2.hours
    maxCachedAdsPerUnit = 5
    
    // Reliability features
    autoRetryFailedAds = true
    maxRetryAttempts = 3
    circuitBreakerThreshold = 5
    
    // Advanced features
    enableSmartPreloading = true
    enableAdaptiveIntervals = true
    enablePerformanceMetrics = true
    
    // Debug and testing
    debugMode = BuildConfig.DEBUG
    testMode = false
    privacyCompliantMode = true
}
```

### Enhanced Error Handling with Circuit Breaker

The library now includes automatic failure detection and recovery:

```kotlin
// Circuit breaker automatically handles failing ad units
val circuitBreaker = AdCircuitBreaker.getInstance()

// Check if an ad unit should be attempted
if (circuitBreaker.shouldAttemptLoad("your-ad-unit-id")) {
    // Load ad
    loadAd()
} else {
    // Ad unit is temporarily blocked due to failures
    showAlternativeContent()
}

// Manual circuit breaker control
circuitBreaker.reset("your-ad-unit-id") // Reset a specific ad unit
circuitBreaker.resetAll() // Reset all ad units
```

### Smart Retry System

Automatic retry with exponential backoff:

```kotlin
// Retry system works automatically, but you can also use it manually
val retryManager = AdRetryManager.getInstance()

retryManager.scheduleRetry(
    adUnitId = "your-ad-unit-id",
    attempt = 0,
    maxAttempts = 3
) {
    // Retry action - load ad again
    loadAd()
}
```

### Memory Leak Prevention

Use WeakReference holders to prevent memory leaks:

```kotlin
class MyActivity : AppCompatActivity() {
    private val activityHolder = weakActivity()
    
    private fun loadAd() {
        // Safe activity access
        activityHolder.withValidActivity { activity ->
            bannerAdView.loadBanner(activity, "ad-unit-id")
        }
    }
}
```

### Debug and Testing Tools

Comprehensive debugging features for development:

```kotlin
// Enable debug overlay (shows real-time ad statistics)
AdDebugUtils.enableDebugOverlay(this, true)

// Set test ad units for safe testing
AdDebugUtils.setTestAdUnits(mapOf(
    "prod-banner-id" to "ca-app-pub-3940256099942544/6300978111",
    "prod-interstitial-id" to "ca-app-pub-3940256099942544/1033173712"
))

// Inject mock ad responses for unit testing
AdDebugUtils.injectMockAds(listOf(
    MockAdResponse("test-ad-unit", shouldSucceed = true, delayMs = 1000),
    MockAdResponse("failing-ad-unit", shouldSucceed = false, errorCode = 3)
))

// Enhanced logging with debug callbacks
val debugCallback = AdDebugUtils.createDebugCallback("ad-unit-id") {
    // Your original callback logic
}
bannerAdView.loadBanner(this, "ad-unit-id", debugCallback)
```

### Enhanced Native Ad Caching

Improved caching with LRU eviction and statistics:

```kotlin
// Configure caching globally
AdManageKitConfig.apply {
    maxCachedAdsPerUnit = 3 // Limit cache size per ad unit
    nativeCacheExpiry = 1.hours // Set expiry time
}

// Monitor cache performance
val cacheStats = NativeAdManager.getCacheStatistics()
cacheStats.forEach { (adUnit, stats) ->
    Log.d("Cache", "$adUnit: $stats")
}

// Manual cache management
NativeAdManager.performCleanup() // Clean expired ads
val cacheSize = NativeAdManager.getTotalCacheSize()
```

### Performance Monitoring

Built-in performance metrics and monitoring:

```kotlin
// Enable performance metrics (sent to Firebase Analytics)
AdManageKitConfig.enablePerformanceMetrics = true

// Get performance statistics
val debugInfo = AdDebugUtils.exportDebugInfo()
Log.d("Performance", debugInfo)

// Monitor active retries and circuit breaker states
val retryStats = AdRetryManager.getInstance().getActiveRetriesSummary()
val circuitStats = AdCircuitBreaker.getInstance().getStateSummary()
```

#### User Messaging Platform (UMP) Consent

Request user consent:

```java
AdsConsentManager.getInstance(this).requestUMP(this, true, "TEST_DEVICE_ID", false, new UMPResultListener() {
    @Override
    public void onCheckUMPSuccess(boolean isConsentGiven) {
        if (isConsentGiven && adsConsentManager.canRequestAds()) {
            // Load ads
        }
    }
});
```

### Sample Project

The sample project in the `app` directory demonstrates ad management (banner, interstitial, app open, native with caching), billing, and UMP consent. The sample has been updated for `2.0.0-alpha01` to include new billing features. To run it:

1. Clone the repository:

   ```bash
   git clone https://github.com/i2hammad/AdManageKit.git
   ```

2. Open in Android Studio.

3. Replace placeholder AdMob IDs and configure in-app purchases in the Google Play Console.

4. Run on a device or emulator.

## Migration Guide

### Migrating to 2.3.0

Version 2.3.0 is **fully backward compatible**. All existing method signatures and behaviors are preserved. However, you can opt-in to new features:

#### Optional: Enable New Features
```kotlin
// In your Application.onCreate()
AdManageKitConfig.apply {
    // Enable new performance features
    autoRetryFailedAds = true
    enableSmartPreloading = true
    
    // Configure for your needs
    maxRetryAttempts = 3
    defaultAdTimeout = 15.seconds
}
```

#### Optional: Use Enhanced Callbacks
```kotlin
// Old way (still works)
val callback = object : AdLoadCallback() {
    override fun onAdLoaded() { /* handle */ }
    override fun onFailedToLoad(error: AdError?) { /* handle */ }
}

// New way (with additional methods)
val enhancedCallback = object : AdLoadCallback() {
    override fun onAdLoaded() { /* handle */ }
    override fun onFailedToLoad(error: AdError?) { /* handle */ }
    override fun onPaidEvent(adValue: AdValue) { /* track revenue */ }
    override fun onAdLoadStarted() { /* show loading */ }
}
```

### Migration Notes
- **Billing Compatibility**: All billing methods are fully supported in 2.3.0
- **New Core Module**: Add `ad-manage-kit-core` dependency for new features
- **Configuration**: Replace manual configurations with `AdManageKitConfig`
- **Deprecated Methods**: Replace `initBilling(Application, List<String>, List<String>)`, `purchase(Activity)`, and `getPrice()` with their recommended counterparts (see [Billing Management Wiki](docs/billing-management.md))

### Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a branch (`git checkout -b feature/YourFeature`).
3. Commit changes (`git commit -m 'Add YourFeature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a Pull Request.

### License

Licensed under the MIT License. See the [LICENSE](LICENSE) file.

### Support

Support `AdManageKit` development:  
[Buy me a coffee](https://buymeacoffee.com/i2hammad)

For issues or questions, open an issue on GitHub or email [hammadmughal0001@gmail.com](mailto:hammadmughal0001@gmail.com).