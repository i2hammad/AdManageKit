# AdManageKit API Reference

This document provides comprehensive API documentation for AdManageKit library version 2.5.0.

## Table of Contents
- [Core Configuration](#core-configuration)
- [Ad Management](#ad-management)
- [Billing Management](#billing-management)
- [Retry Logic](#retry-logic)
- [Caching System](#caching-system)
- [Debug & Testing](#debug--testing)
- [Callbacks & Listeners](#callbacks--listeners)
- [Utility Classes](#utility-classes)

## Core Configuration

### AdManageKitConfig

Centralized configuration object for the entire library.

```kotlin
object AdManageKitConfig {
    // Performance Settings
    var debugMode: Boolean
    var defaultAdTimeout: Duration
    var nativeCacheExpiry: Duration
    var maxCachedAdsPerUnit: Int

    // Reliability Features (v2.5.0: Circuit breaker removed)
    var autoRetryFailedAds: Boolean
    var maxRetryAttempts: Int
    var baseRetryDelay: Duration

    // Advanced Features
    var enableSmartPreloading: Boolean
    var enableAdaptiveIntervals: Boolean
    var enablePerformanceMetrics: Boolean

    // Testing & Debug
    var testMode: Boolean
    var testDeviceId: String?
    var privacyCompliantMode: Boolean

    // Utility Methods
    fun resetToDefaults()
    fun validate()
    fun getConfigSummary(): String
}
```

**Usage Example:**
```kotlin
AdManageKitConfig.apply {
    debugMode = BuildConfig.DEBUG
    defaultAdTimeout = 15.seconds
    autoRetryFailedAds = true
    maxRetryAttempts = 3
}
```

### BillingConfig

Configuration for purchase providers.

```kotlin
object BillingConfig {
    fun setPurchaseProvider(provider: AppPurchaseProvider)
    fun getPurchaseProvider(): AppPurchaseProvider
}
```

## Ad Management

### AdManager

Singleton class for managing interstitial ads.

#### Methods

```kotlin
class AdManager {
    companion object {
        fun getInstance(): AdManager
    }
    
    // Loading Methods
    fun loadInterstitialAd(context: Context, adUnitId: String)
    fun loadInterstitialAdForSplash(
        context: Context, 
        adUnitId: String, 
        timeoutMillis: Long, 
        callback: AdManagerCallback
    )
    
    // Display Methods
    fun forceShowInterstitial(activity: Activity, callback: AdManagerCallback)
    fun forceShowInterstitialWithDialog(
        activity: Activity, 
        callback: AdManagerCallback, 
        isReload: Boolean = true
    )
    fun showInterstitialAdByTime(activity: Activity, callback: AdManagerCallback)
    fun showInterstitialAdByCount(
        activity: Activity, 
        callback: AdManagerCallback, 
        maxDisplayCount: Int
    )
    
    // State Methods
    fun isReady(): Boolean
    fun isDisplayingAd(): Boolean
    fun setAdInterval(intervalMillis: Long)
    fun getAdDisplayCount(): Int
    fun setAdDisplayCount(count: Int)
}
```

### AppOpenManager

Manages app open ads with lifecycle awareness.

#### Constructor
```kotlin
class AppOpenManager(
    private val myApplication: Application, 
    private var adUnitId: String
)
```

#### Methods
```kotlin
// Display Methods
fun showAdIfAvailable()
fun forceShowAdIfAvailable(activity: Activity, callback: AdManagerCallback)
fun skipNextAd()

// Loading Methods
fun fetchAd()
fun fetchAd(callback: AdLoadCallback, timeoutMillis: Long = 5000)

// Configuration Methods
fun disableAppOpenWithActivity(activityClass: Class<*>)
fun includeAppOpenActivityForAds(activityClass: Class<*>)

// State Methods
fun isAdAvailable(): Boolean
```

### BannerAdView

Custom view for banner ads with shimmer loading.

#### Methods
```kotlin
// Loading Methods
fun loadBanner(context: Activity?, adUnitId: String?)
fun loadBanner(context: Activity?, adUnitId: String?, callback: AdLoadCallback?)
fun loadCollapsibleBanner(context: Activity?, adUnitId: String?, collapsible: Boolean)
fun loadCollapsibleBanner(
    context: Activity?, 
    adUnitId: String?, 
    collapsible: Boolean, 
    callback: AdLoadCallback?
)

// Control Methods
fun hideAd()
fun showAd()
fun destroyAd()
fun resumeAd()
fun pauseAd()
fun setAdCallback(callback: AdLoadCallback?)
```

### Native Ad Views

#### NativeBannerSmall, NativeBannerMedium, NativeLarge

```kotlin
// Loading Methods
fun loadNativeBannerAd(activity: Activity, adUnitId: String)
fun loadNativeBannerAd(
    activity: Activity, 
    adUnitId: String, 
    useCachedAd: Boolean
)
fun loadNativeBannerAd(
    activity: Activity, 
    adUnitId: String, 
    useCachedAd: Boolean, 
    callback: AdLoadCallback
)

// For NativeLarge
fun loadNativeAds(activity: Activity, adUnitId: String)
fun loadNativeAds(
    activity: Activity, 
    adUnitId: String, 
    useCachedAd: Boolean
)
fun loadNativeAds(
    activity: Activity, 
    adUnitId: String, 
    useCachedAd: Boolean, 
    callback: AdLoadCallback
)
```

### RewardedAdManager

Singleton class for managing rewarded ads with comprehensive lifecycle callbacks, automatic retry, and Firebase Analytics integration.

#### Features (v3.4.0+)
- Automatic retry with exponential backoff on load failures
- Purchase status integration (ads disabled for premium users)
- Timeout support for splash screen scenarios
- Detailed Firebase Analytics tracking (requests, fills, impressions)
- Configurable auto-reload after ad dismissal

#### Callback Interfaces

```kotlin
object RewardedAdManager {
    /**
     * Full lifecycle callback for rewarded ad events.
     */
    interface RewardedAdCallback {
        fun onRewardEarned(rewardType: String, rewardAmount: Int)
        fun onAdDismissed()
        fun onAdShowed() {}        // Optional
        fun onAdFailedToShow(error: AdError) {}  // Optional
        fun onAdClicked() {}       // Optional
    }

    /**
     * Callback for ad loading events.
     */
    interface OnRewardedAdLoadCallback {
        fun onAdLoaded()
        fun onAdFailedToLoad(error: LoadAdError)
    }

    /**
     * Legacy callback (deprecated).
     */
    @Deprecated("Use RewardedAdCallback instead")
    interface OnAdDismissedListener {
        fun onAdDismissed()
    }
}
```

#### Methods

```kotlin
object RewardedAdManager {
    // =================== INITIALIZATION ===================

    /**
     * Initialize with ad unit ID. Automatically starts loading.
     */
    fun initialize(context: Context, adUnitId: String)

    // =================== LOADING ===================

    /**
     * Load a rewarded ad.
     * Skips if: already loading, already loaded, or user is premium.
     */
    fun loadRewardedAd(context: Context)

    /**
     * Load with callback notification.
     */
    fun loadRewardedAd(context: Context, callback: OnRewardedAdLoadCallback)

    /**
     * Load with timeout support (for splash screens).
     * Callback fires once: on load, fail, or timeout.
     */
    fun loadRewardedAdWithTimeout(
        context: Context,
        timeoutMillis: Long = AdManageKitConfig.defaultAdTimeout.inWholeMilliseconds,
        callback: OnRewardedAdLoadCallback
    )

    // =================== DISPLAY ===================

    /**
     * Show with full callback support.
     * @param autoReload Whether to reload after dismissal (default: AdManageKitConfig.interstitialAutoReload)
     */
    fun showAd(
        activity: Activity,
        callback: RewardedAdCallback,
        autoReload: Boolean = AdManageKitConfig.interstitialAutoReload
    )

    /**
     * Legacy show method (deprecated).
     */
    @Deprecated("Use showAd with RewardedAdCallback")
    fun showAd(
        activity: Activity,
        onUserEarnedRewardListener: OnUserEarnedRewardListener,
        onAdDismissedListener: OnAdDismissedListener
    )

    // =================== STATE ===================

    /**
     * Check if ad is loaded and ready (returns false for premium users).
     */
    fun isAdLoaded(): Boolean

    /**
     * Check if a load request is in progress.
     */
    fun isLoading(): Boolean

    /**
     * Check if ad is currently being displayed.
     */
    fun isShowingAd(): Boolean

    // =================== UTILITIES ===================

    /**
     * Preload ad during natural pauses to improve show rate.
     */
    fun preload(context: Context)

    /**
     * Get session statistics for debugging.
     */
    fun getAdStats(): Map<String, Any>

    /**
     * Reset session statistics.
     */
    fun resetAdStats()
}
```

#### Usage Examples

**Basic Usage:**
```kotlin
// Initialize once (e.g., in Application.onCreate())
RewardedAdManager.initialize(context, "ca-app-pub-xxx/yyy")

// Show when ready
if (RewardedAdManager.isAdLoaded()) {
    RewardedAdManager.showAd(activity, object : RewardedAdManager.RewardedAdCallback {
        override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
            grantReward(rewardType, rewardAmount)
        }
        override fun onAdDismissed() {
            continueGameFlow()
        }
    })
}
```

**With Timeout (Splash Screen):**
```kotlin
RewardedAdManager.loadRewardedAdWithTimeout(
    context = this,
    timeoutMillis = 5000,
    callback = object : RewardedAdManager.OnRewardedAdLoadCallback {
        override fun onAdLoaded() {
            // Ad ready, show it
            showRewardedAd()
        }
        override fun onAdFailedToLoad(error: LoadAdError) {
            // Proceed without ad
            navigateToMain()
        }
    }
)
```

**Preloading:**
```kotlin
// Preload during natural pauses
override fun onResume() {
    super.onResume()
    RewardedAdManager.preload(this)
}
```

**Analytics:**
```kotlin
val stats = RewardedAdManager.getAdStats()
Log.d("Ads", "Fill rate: ${stats["fill_rate_percent"]}%")
Log.d("Ads", "Show rate: ${stats["show_rate_percent"]}%")
```

## Billing Management

### AppPurchase

Main billing client wrapper.

#### Key Methods
```kotlin
class AppPurchase {
    companion object {
        fun getInstance(): AppPurchase
    }
    
    // Initialization
    fun initBilling(
        application: Application,
        purchaseItems: List<PurchaseItem>
    )
    
    // Purchase Flow
    fun purchase(activity: Activity, productId: String)
    fun consumePurchase(productId: String)
    
    // Product Information
    fun queryProductDetails(productIds: List<String>, productType: String)
    fun getPrice(productId: String): String
    fun getCurrency(productId: String, type: TYPE_IAP): String
    fun getPriceWithoutCurrency(productId: String, type: TYPE_IAP): Double
    
    // State
    val isBillingInitialized: Boolean
    
    // Listeners
    fun setPurchaseListener(listener: PurchaseListener)
    fun setBillingListener(listener: BillingListener, timeout: Long)
}
```

### PurchaseItem

Data class for purchase items.

```kotlin
data class PurchaseItem(
    val productId: String,
    val offerToken: String = "",
    val type: AppPurchase.TYPE_IAP
)
```

## Retry Logic

**Note**: Circuit breaker pattern was removed in v2.5.0 to maximize ad show rates. Retry logic with exponential backoff is still available.

### AdRetryManager

Manages retry operations with exponential backoff.

```kotlin
class AdRetryManager {
    companion object {
        fun getInstance(): AdRetryManager
    }
    
    // Retry Operations
    fun scheduleRetry(
        adUnitId: String,
        attempt: Int,
        maxAttempts: Int = AdManageKitConfig.maxRetryAttempts,
        retryAction: suspend () -> Unit
    )
    
    // Control
    fun cancelRetry(adUnitId: String)
    fun cancelAllRetries()
    
    // Information
    fun hasActiveRetry(adUnitId: String): Boolean
    fun getCurrentAttempt(adUnitId: String): Int
    fun getActiveRetriesSummary(): Map<String, String>
    
    // Cleanup
    fun cleanup()
}
```

## Caching System

### NativeAdManager

Enhanced caching system for native ads.

```kotlin
object NativeAdManager {
    // Configuration
    var enableCachingNativeAds: Boolean
    
    // Cache Operations
    fun setCachedNativeAd(adUnitId: String, ad: NativeAd)
    fun getCachedNativeAd(adUnitId: String): NativeAd?
    fun clearCachedAd(adUnitId: String)
    fun clearAllCachedAds()
    
    // Maintenance
    fun performCleanup()
    
    // Statistics
    fun getCacheStatistics(): Map<String, String>
    fun getCacheSize(adUnitId: String): Int
    fun getTotalCacheSize(): Int
    fun hasCachedAds(adUnitId: String): Boolean
}
```

## Debug & Testing

### AdDebugUtils

Comprehensive debugging utilities.

```kotlin
object AdDebugUtils {
    // Debug Overlay
    fun enableDebugOverlay(activity: Activity, enabled: Boolean)
    
    // Test Configuration
    fun setTestAdUnits(testUnits: Map<String, String>)
    fun getTestAdUnit(productionAdUnit: String): String
    
    // Mock Responses
    fun injectMockAds(mockResponses: List<MockAdResponse>)
    fun getMockResponse(adUnitId: String): MockAdResponse?
    
    // Debug Callbacks
    fun createDebugCallback(
        adUnitId: String, 
        originalCallback: AdLoadCallback? = null
    ): AdLoadCallback
    
    // Event Logging
    fun logEvent(adUnitId: String, eventType: String, details: String, success: Boolean = true)
    fun showDebugToast(context: Context, message: String)
    
    // Data Export
    fun getAdEvents(): List<AdEvent>
    fun clearAdEvents()
    fun exportDebugInfo(): String
    
    // Data Classes
    data class AdEvent(
        val timestamp: Long,
        val adUnitId: String,
        val eventType: String,
        val details: String,
        val success: Boolean
    )
    
    data class MockAdResponse(
        val adUnitId: String,
        val shouldSucceed: Boolean = true,
        val delayMs: Long = 1000,
        val errorCode: Int = 0,
        val errorMessage: String = "",
        val adValue: AdValue? = null
    )
}
```

## Callbacks & Listeners

### AdLoadCallback

Enhanced callback for ad lifecycle events.

```kotlin
abstract class AdLoadCallback {
    // Core Events
    open fun onAdLoaded()
    open fun onFailedToLoad(error: AdError?)
    open fun onAdClicked()
    open fun onAdClosed()
    open fun onAdImpression()
    open fun onAdOpened()
    
    // Enhanced Events (New in 2.1.0)
    open fun onPaidEvent(adValue: AdValue)
    open fun onAdLoadStarted()
    open fun onAdLoadCancelled()
}
```

### AdManagerCallback

Callback for ad manager operations.

```kotlin
abstract class AdManagerCallback : AdLoadCallback() {
    open fun onNextAction()
}
```

### BillingListener

Callback for billing initialization.

```kotlin
interface BillingListener {
    fun onInitBillingFinished(resultCode: Int)
}
```

### PurchaseListener

Callback for purchase operations.

```kotlin
interface PurchaseListener {
    fun onProductPurchased(orderId: String, originalJson: String)
    fun displayErrorMessage(errorMessage: String)
    fun onUserCancelBilling()
}
```

### UMPResultListener

Callback for UMP consent operations.

```kotlin
interface UMPResultListener {
    fun onCheckUMPSuccess(isConsentGiven: Boolean)
}
```

## Utility Classes

### WeakReferenceHolder

Generic weak reference holder to prevent memory leaks.

```kotlin
class WeakReferenceHolder<T : Any>(referent: T?) {
    fun get(): T?
    fun withReference(action: (T) -> Unit): Boolean
    fun withReferenceOrElse(action: (T) -> Unit, fallback: () -> Unit)
    fun isValid(): Boolean
    fun clear()
}
```

### WeakActivityHolder

Specialized holder for Activity references.

```kotlin
class WeakActivityHolder(activity: Activity?) : WeakReferenceHolder<Activity>(activity) {
    fun withValidActivity(action: (Activity) -> Unit): Boolean
    fun isActivityValid(): Boolean
}
```

### WeakContextHolder

Specialized holder for Context references.

```kotlin
class WeakContextHolder(context: Context?) : WeakReferenceHolder<Context>(context) {
    fun getApplicationContext(): Context?
    fun withApplicationContext(action: (Context) -> Unit): Boolean
}
```

### Extension Functions

```kotlin
// Extension functions for easy weak reference creation
fun <T : Any> T?.weak(): WeakReferenceHolder<T>
fun Activity?.weakActivity(): WeakActivityHolder
fun Context?.weakContext(): WeakContextHolder
```

## Constants & Enums

### Error Codes

```kotlin
// AdManager Error Codes
const val PURCHASED_APP_ERROR_CODE = 1001
const val PURCHASED_APP_ERROR_DOMAIN = "com.i2hammad.admanagekit"
const val PURCHASED_APP_ERROR_MESSAGE = "Ads are not shown because the app has been purchased."
```

### Ad Types

```kotlin
// AppPurchase Types
enum class TYPE_IAP {
    PURCHASE, SUBSCRIPTION
}
```

## Best Practices

### Initialization Order
1. Configure `AdManageKitConfig` first
2. Set up billing provider with `BillingConfig.setPurchaseProvider()`
3. Initialize `AppOpenManager` if using app open ads
4. Initialize MobileAds SDK
5. Request UMP consent

### Memory Management
- Always use WeakReference holders for Activity/Context references
- Call cleanup methods in appropriate lifecycle events
- Use `onDestroy()` to clean up ad resources

### Error Handling
- Always implement `onFailedToLoad()` in callbacks
- Configure retry logic with exponential backoff (circuit breaker removed in v2.5.0)
- Monitor retry statistics in debug builds
- Rely on automatic retry system for failed loads

### Testing
- Use `AdManageKitConfig.testMode = true` for development
- Set test ad units with `AdDebugUtils.setTestAdUnits()`
- Enable debug overlay for real-time monitoring
- Use mock responses for unit testing

## Changelog

### v2.5.0
- Removed circuit breaker to maximize ad show rates
- Added custom ad unit support to AppOpenManager
- Enhanced retry logic with configurable exponential backoff
- Added performance metrics tracking
- Improved thread safety across all components

This API reference covers all major components of AdManageKit 2.5.0. For more detailed examples and usage patterns, refer to the main README and sample project.