# AdManageKitConfig Usage Guide

This guide shows how to use the new `AdManageKitConfig` in your ad implementations.

## Basic Configuration

Configure AdManageKit in your Application class:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Configure AdManageKit
        AdManageKitConfig.apply {
            debugMode = BuildConfig.DEBUG
            enableSmartPreloading = true
            autoRetryFailedAds = true
            maxRetryAttempts = 3
            enablePerformanceMetrics = true
            defaultBannerRefreshInterval = 60.seconds
            enableCollapsibleBannersByDefault = false
        }
        
        // Set up billing
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
    }
}
```

## Advanced Configuration

```kotlin
AdManageKitConfig.apply {
    // Performance settings
    defaultAdTimeout = 15.seconds
    nativeCacheExpiry = 2.hours
    maxCachedAdsPerUnit = 5
    
    // Reliability features
    autoRetryFailedAds = true
    maxRetryAttempts = 3
    circuitBreakerThreshold = 5
    enableExponentialBackoff = true
    baseRetryDelay = 1.seconds
    maxRetryDelay = 30.seconds
    
    // Advanced features
    enableSmartPreloading = true
    enableAdaptiveIntervals = true
    enablePerformanceMetrics = true
    enableAutoCacheCleanup = true
    
    // Debug and testing
    debugMode = BuildConfig.DEBUG
    testMode = false
    privacyCompliantMode = true
    enableDebugOverlay = false
}
```

## How Configuration Affects Ads

### Banner Ads
- `autoRetryFailedAds` and `maxRetryAttempts` control retry behavior
- `enableExponentialBackoff` affects retry delays
- `defaultBannerRefreshInterval` sets auto-refresh interval
- `enableCollapsibleBannersByDefault` enables collapsible banners globally
- `enablePerformanceMetrics` adds detailed analytics

### Native Ads
- `enableSmartPreloading` enables intelligent caching
- `maxCachedAdsPerUnit` limits cache size per ad unit
- `nativeCacheExpiry` sets cache expiration time
- `enableAutoCacheCleanup` enables automatic cleanup

### Debug Features
- `debugMode` enables enhanced logging
- `testMode` enables test ad units
- `enableDebugOverlay` shows real-time debug info
- `enablePerformanceMetrics` logs performance data

## Example Usage in Activities

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Debug overlay (only in debug mode)
        if (AdManageKitConfig.debugMode) {
            AdDebugUtils.enableDebugOverlay(this, true)
        }
        
        // Banner ad with configuration-based settings
        val bannerAdView = findViewById<BannerAdView>(R.id.bannerAdView)
        bannerAdView.loadBanner(this, "your-ad-unit-id")
        
        // Auto-refresh is now configurable
        if (AdManageKitConfig.defaultBannerRefreshInterval.inWholeSeconds >= 30) {
            bannerAdView.enableAutoRefresh()
        }
        
        // Native ads with smart caching
        val nativeAdSmall = findViewById<NativeBannerSmall>(R.id.nativeAdSmall)
        nativeAdSmall.loadNativeBannerAd(
            this, 
            "your-native-ad-unit-id", 
            useCachedAd = AdManageKitConfig.enableSmartPreloading
        )
    }
}
```

## Production vs Debug Configuration

### Debug Configuration
```kotlin
AdManageKitConfig.apply {
    debugMode = true
    testMode = true  // Use test ad units
    enableDebugOverlay = true
    enablePerformanceMetrics = true
    maxRetryAttempts = 1  // Fail fast for testing
}
```

### Production Configuration
```kotlin
AdManageKitConfig.apply {
    debugMode = false
    testMode = false
    enableDebugOverlay = false
    privacyCompliantMode = true
    enablePerformanceMetrics = false  // Reduce overhead
    maxRetryAttempts = 3
    autoRetryFailedAds = true
}
```

## Configuration Validation

Always validate your configuration:

```kotlin
if (!AdManageKitConfig.validate()) {
    Log.w("MyApp", "Invalid AdManageKit configuration detected")
}

if (!AdManageKitConfig.isProductionReady()) {
    Log.w("MyApp", "AdManageKit configuration not ready for production")
}

// Print configuration summary
Log.d("MyApp", AdManageKitConfig.getConfigSummary())
```

## Benefits of Using AdManageKitConfig

1. **Centralized Configuration**: Single point to control all ad behavior
2. **Environment-Specific Settings**: Easy debug vs production configuration
3. **Performance Optimization**: Fine-tune caching, retries, and timeouts
4. **Debug Tools**: Enhanced logging and monitoring capabilities
5. **Reliability Features**: Circuit breaker, exponential backoff, smart retries
6. **Flexibility**: Runtime configuration changes without code modifications

The configuration automatically affects all ad components (BannerAdView, NativeBannerSmall, NativeBannerMedium, NativeLarge) and provides consistent behavior across your entire app.