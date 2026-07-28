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
fun loadBanner(
    context: Activity?,
    adUnitId: String?,
    adSize: BannerAdSize,           // v4.3.0: ADAPTIVE (default), BANNER, LARGE_BANNER,
    callback: AdLoadCallback? = null //         MEDIUM_RECTANGLE, FULL_BANNER, LEADERBOARD
)
fun loadCollapsibleBanner(context: Activity?, adUnitId: String?, collapsible: Boolean)
fun loadCollapsibleBanner(
    context: Activity?, 
    adUnitId: String?, 
    collapsible: Boolean, 
    callback: AdLoadCallback?
)
fun loadCollapsibleBanner(
    context: Activity?,
    adUnitId: String?,
    collapsible: Boolean,
    placement: CollapsibleBannerPlacement,
    callback: AdLoadCallback? = null,
    adSize: BannerAdSize? = null    // v4.3.0: null keeps XML/previous size
)

// Configuration Methods
fun setBannerAdSize(adSize: BannerAdSize) // v4.3.0: default size for subsequent loads;
                                          // also settable in XML via app:bannerAdSize

// Control Methods
fun hideAd()
fun showAd()
fun destroyAd()
fun resumeAd()
fun pauseAd()
fun setAdCallback(callback: AdLoadCallback?)
```

#### Banner Sizes (v4.3.0)

`com.i2hammad.admanagekit.config.BannerAdSize`:

| Value              | Size (dp)  | Description          | Availability       |
|--------------------|------------|----------------------|--------------------|
| `ADAPTIVE`         | full width | Anchored adaptive    | Phones and tablets |
| `BANNER`           | 320x50     | Banner               | Phones and tablets |
| `LARGE_BANNER`     | 320x100    | Large banner         | Phones and tablets |
| `MEDIUM_RECTANGLE` | 300x250    | IAB medium rectangle | Phones and tablets |
| `FULL_BANNER`      | 468x60     | IAB full-size banner | Tablets            |
| `LEADERBOARD`      | 728x90     | IAB leaderboard      | Tablets            |

The shimmer placeholder reserves the exact requested size, collapsible banners
require `ADAPTIVE`, and the size carries through retries, auto-refresh, and the
multi-provider waterfall (applied to AdMob providers in the chain).

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
     * @param autoReload Whether to reload after dismissal (default: AdManageKitConfig.rewardedAutoReload)
     */
    fun showAd(
        activity: Activity,
        callback: RewardedAdCallback,
        autoReload: Boolean = AdManageKitConfig.rewardedAutoReload
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

    // Product Metadata (v3.4.1+)
    fun getProductTitle(productId: String): String?        // Title with app name
    fun getProductName(productId: String): String?         // Clean name without app name
    fun getProductDescription(productId: String): String?  // Play Console description
    fun getProductDetails(productId: String): ProductDetails?  // Raw ProductDetails object

    // Free Trial & Billing Period (v3.4.1+)
    fun hasFreeTrial(productId: String): Boolean           // Whether subscription has trial
    fun getFreeTrialPeriod(productId: String): String?     // Trial period (e.g. "P7D")
    fun getBillingPeriod(productId: String): String?       // Billing cycle (e.g. "P1M")
    fun getIntroductorySubPrice(productId: String): String // Formatted intro price (or "")
    fun getPricePricingPhaseList(productId: String): List<ProductDetails.PricingPhase>

    // Structured Offer API (v3.5.7+)
    fun getOffers(productId: String): List<OfferInfo>      // All subscription offers
    fun getTrialOffer(productId: String): OfferInfo?       // First offer with a free trial
    fun getBaseOffer(productId: String): OfferInfo?        // Base (non-promo) offer

    // Purchase a specific offer (v4.4.0+)
    fun subscribe(activity: Activity, offer: OfferInfo): String
    fun subscribe(activity: Activity, subsId: String, offerToken: String?): String
    fun purchase(activity: Activity, offer: OneTimeOfferInfo): String
    fun purchase(activity: Activity, productId: String, offerToken: String?): String
    fun updateSubscription(activity: Activity, newSubsId: String, offerToken: String?,
                           oldPurchaseToken: String, mode: SubscriptionReplacementMode): String

    // Offer lookup (v4.4.0+)
    fun getIntroOffer(productId: String): OfferInfo?                    // First with intro price
    fun getOfferById(productId: String, offerId: String): OfferInfo?
    fun getOfferByBasePlanId(productId: String, basePlanId: String): OfferInfo?
    fun getOfferByTag(productId: String, tag: String): OfferInfo?       // Case-insensitive
    fun getOffersByTag(productId: String, tag: String): List<OfferInfo>
    fun getBestValueOffer(productId: String): OfferInfo?                // Lowest per month
    fun getCheapestFirstCycleOffer(productId: String): OfferInfo?       // Cheapest entry

    // Pricing comparison (v4.4.0+)
    fun getSavingsPercent(baseProductId: String, comparedProductId: String): Int
    fun getPricePerMonthMicros(productId: String): Long
    fun getFormattedPricePerMonth(productId: String): String
    fun hasIntroductoryPrice(productId: String): Boolean
    fun getIntroductoryPeriod(productId: String): String?
    fun getIntroductoryCycleCount(productId: String): Int
    companion object { fun formatPrice(priceMicros: Long, currencyCode: String): String }

    // Trial eligibility (v4.4.0+) — Play filters offers per account
    fun isEligibleForFreeTrial(productId: String): Boolean
    fun isEligibleForIntroPrice(productId: String): Boolean

    // One-time product offers (v4.4.0+, Play Billing 9)
    fun getOneTimeOffers(productId: String): List<OneTimeOfferInfo>
    fun getOneTimeOffer(productId: String, offerId: String): OneTimeOfferInfo?
    fun getBestOneTimeOffer(productId: String): OneTimeOfferInfo?

    // Account hold & payment recovery (v4.4.0+)
    fun hasSubscriptionOnHold(): Boolean
    fun hasPendingSubscriptionChange(): Boolean
    fun showInAppMessages(activity: Activity, listener: InAppMessageListener?)

    // Flow configuration (v4.4.0+)
    fun setObfuscatedAccountId(id: String?)       // Hashed; max 64 chars
    fun setObfuscatedProfileId(id: String?)
    fun setOfferPersonalized(personalized: Boolean)  // EU disclosure

    // Product details diagnostics (v4.4.0+)
    fun setProductDetailsListener(listener: ProductDetailsListener?)
    fun getUnfetchedProducts(): List<UnfetchedProduct>
    fun isProductDetailsLoaded(productId: String): Boolean
    fun areAllProductDetailsLoaded(): Boolean

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

### OfferInfo (v3.5.7+)

Typed view over a `SubscriptionOfferDetails`. Each Play subscription offer can
expose up to three pricing phases — free trial, introductory, base — and
`OfferInfo` classifies them by recurrence/price instead of list position, so
multi-offer products (e.g. trial + intro + base) bind cleanly.

```kotlin
data class OfferInfo(
    val productId: String,
    val basePlanId: String?,
    val offerId: String?,
    val offerToken: String,
    val offerTags: List<String>,
    val pricingPhases: List<ProductDetails.PricingPhase>,

    // Free trial
    val isFreeTrial: Boolean,
    val trialPeriod: String?,         // e.g. "P7D"
    val trialPhase: ProductDetails.PricingPhase?,

    // Introductory price
    val hasIntroPrice: Boolean,
    val introPrice: String?,          // formatted, e.g. "$1.99"
    val introPriceMicros: Long,
    val introPeriod: String?,         // e.g. "P1M"
    val introCycleCount: Int,
    val introPhase: ProductDetails.PricingPhase?,

    // Base recurring price
    val basePrice: String,            // formatted, e.g. "$9.99"
    val basePriceMicros: Long,
    val billingPeriod: String?,       // e.g. "P1M", "P1Y"
    val currencyCode: String,
    val basePhase: ProductDetails.PricingPhase?,

    // Installments (v4.4.0+)
    val installmentPlanDetails: ProductDetails.InstallmentPlanDetails? = null,
) {
    // Derived properties (v4.4.0+)
    val isBaseOffer: Boolean                  // No trial, no intro
    val hasPromotion: Boolean
    val isInstallmentPlan: Boolean
    val installmentCommitmentPayments: Int
    val installmentRenewalCommitmentPayments: Int

    val billingPeriodParsed: BillingPeriod?
    val trialPeriodParsed: BillingPeriod?
    val introPeriodParsed: BillingPeriod?
    val trialDays: Int
    val introTotalDays: Int                   // Cycle length × cycle count

    val pricePerMonthMicros: Long             // Normalized for cross-cadence comparison
    val pricePerWeekMicros: Long
    val firstCyclePriceMicros: Long           // 0 on trial, else intro, else base
    val firstCyclePrice: String
    val introDiscountPercent: Int

    fun hasTag(tag: String): Boolean          // Case-insensitive
}
```

Usage:

```kotlin
val billing = AppPurchase.getInstance()

// All offers attached to the product
val offers = billing.getOffers("premium_yearly")

// Direct accessors
billing.getTrialOffer("premium_yearly")?.let { trial ->
    showTrialBadge(trial.trialPeriod)        // "P7D"
}
billing.getBaseOffer("premium_yearly")?.let { base ->
    priceLabel.text = base.basePrice         // "$59.99"
    cycleLabel.text = base.billingPeriod     // "P1Y"
}

// Buy exactly the offer the user tapped (v4.4.0+)
billing.subscribe(activity, offers[selectedIndex])
```

### BillingPeriod (v4.4.0+)

Parses the ISO-8601 periods Play returns and normalizes prices across cadences.

```kotlin
data class BillingPeriod(
    val years: Int, val months: Int, val weeks: Int, val days: Int,
    val iso: String,
) {
    enum class Unit { DAY, WEEK, MONTH, YEAR }

    val unit: Unit          // Largest non-zero component
    val count: Int          // Its magnitude — pair with plurals resources
    val totalMonths: Double // "P1Y" -> 12.0
    val totalWeeks: Double
    val totalDays: Int      // "P1M" -> 30
    val isZero: Boolean

    fun format(abbreviated: Boolean = false): String   // "1 month" / "1mo" (English)

    companion object {
        fun parse(iso: String?): BillingPeriod?
        fun totalMonthsOf(iso: String?): Double
        fun totalDaysOf(iso: String?): Int
        fun formatOf(iso: String?, abbreviated: Boolean = false): String
        fun savingsPercent(baseMicros: Long, basePeriod: String?,
                           comparedMicros: Long, comparedPeriod: String?): Int
        fun pricePerMonthMicros(priceMicros: Long, period: String?): Long
        fun pricePerWeekMicros(priceMicros: Long, period: String?): Long
        fun isSameDuration(first: String?, second: String?): Boolean
    }
}
```

`format()` is an English convenience. For shipped UI, use `unit` + `count` with
your own plurals resources — a library cannot resolve the host app's locale rules.

### OneTimeOfferInfo (v4.4.0+)

Typed view over a Play Billing 9 `OneTimePurchaseOfferDetails`. A one-time
product can carry several offers; the legacy
`getOneTimePurchaseOfferDetails()` exposes only one.

```kotlin
data class OneTimeOfferInfo(
    val productId: String,
    val offerId: String?,
    val purchaseOptionId: String?,
    val offerToken: String,
    val offerTags: List<String>,

    val formattedPrice: String,
    val priceMicros: Long,
    val currencyCode: String,
    val fullPriceMicros: Long?,          // Strike-through price

    val discountPercentage: Int?,
    val discountAmountMicros: Long?,
    val formattedDiscountAmount: String?,
    val discountCurrencyCode: String?,

    val rentalPeriod: String?,
    val rentalExpirationPeriod: String?,
    val preorderReleaseTimeMillis: Long,
    val preorderPresaleEndTimeMillis: Long,
    val maximumQuantity: Int,
    val remainingQuantity: Int,
    val validFromMillis: Long,
    val validUntilMillis: Long,
    val raw: ProductDetails.OneTimePurchaseOfferDetails,
) {
    val isDiscounted: Boolean
    val effectiveDiscountPercent: Int    // From Play, or derived from fullPriceMicros
    val isRental: Boolean
    val isPreorder: Boolean
    val isLimitedQuantity: Boolean
    val isSoldOut: Boolean
    val rentalPeriodParsed: BillingPeriod?
    val rentalExpirationPeriodParsed: BillingPeriod?

    fun hasTag(tag: String): Boolean
    fun isValidAt(nowMillis: Long = System.currentTimeMillis()): Boolean
}
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

### v4.4.0
- Purchase a **specific** offer: `subscribe(activity, offer)`, `subscribe(activity, subsId, offerToken)`, `purchase(activity, offer)`, `purchase(activity, productId, offerToken)`, and a 5-argument `updateSubscription(...)`. Previously the library always resolved the token itself and could charge for whichever offer Play listed last
- Offer lookup: `getIntroOffer`, `getOfferById`, `getOfferByBasePlanId`, `getOfferByTag` / `getOffersByTag`, `getBestValueOffer`, `getCheapestFirstCycleOffer`
- New `BillingPeriod` type parses ISO-8601 billing periods and normalizes prices across cadences (`savingsPercent`, `pricePerMonthMicros`)
- `OfferInfo` gained derived pricing (`pricePerMonthMicros`, `firstCyclePrice`, `introDiscountPercent`, `trialDays`, `introTotalDays`, parsed periods, `isBaseOffer`, `hasTag`) and installment-plan details
- `getSavingsPercent(monthlyId, yearlyId)`, `getFormattedPricePerMonth(productId)`, `AppPurchase.formatPrice(micros, currency)`
- Trial eligibility: `isEligibleForFreeTrial()`, `isEligibleForIntroPrice()`
- New `OneTimeOfferInfo` + `getOneTimeOffers()`, `getOneTimeOffer()`, `getBestOneTimeOffer()` for Play Billing 9 one-time product offers (discounts, rentals, pre-orders, limited quantity)
- Account hold detected client-side: `PurchaseResult.isSuspended()`, `hasSubscriptionOnHold()`. **Behavior change** — `getSubscriptionState()` returns `ON_HOLD` and `isSubscriptionActive()` returns `false` for those purchases
- Play payment recovery: `showInAppMessages(activity, listener)` with `InAppMessageListener`
- Pending plan changes: `hasPendingSubscriptionChange()`, `PurchaseResult.hasPendingPurchaseUpdate()`
- Flow configuration: `setObfuscatedAccountId()`, `setObfuscatedProfileId()`, `setOfferPersonalized()`
- Product-details diagnostics: `setProductDetailsListener()`, `getUnfetchedProducts()`, `isProductDetailsLoaded()`, `areAllProductDetailsLoaded()`

### v3.5.7
- Added structured offer API to `AppPurchase`: `getOffers()`, `getTrialOffer()`, `getBaseOffer()` returning typed `OfferInfo`
- New `OfferInfo` data class classifies trial / intro / base pricing phases by recurrence and price (handles multi-offer products correctly)
- Fixed `getIntroductorySubPrice()`, `getPriceSub()`, `getBillingPeriod()`, `getPricePricingPhaseList()` — now walk all offers and return the correct phase instead of relying on list position

### v3.4.1
- Added product metadata APIs to AppPurchase: `getProductTitle()`, `getProductName()`, `getProductDescription()`, `getProductDetails()`
- Added free trial detection: `hasFreeTrial()`, `getFreeTrialPeriod()`
- Added billing period query: `getBillingPeriod()`

### v2.5.0
- Removed circuit breaker to maximize ad show rates
- Added custom ad unit support to AppOpenManager
- Enhanced retry logic with configurable exponential backoff
- Added performance metrics tracking
- Improved thread safety across all components

This API reference covers all major components of AdManageKit. For more detailed examples and usage patterns, refer to the main README and sample project.