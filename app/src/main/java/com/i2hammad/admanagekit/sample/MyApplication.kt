package com.i2hammad.admanagekit.sample

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.i2hammad.admanagekit.admob.AppOpenManager
import com.i2hammad.admanagekit.admob.InterstitialAdBuilder
import com.i2hammad.admanagekit.billing.AppPurchase
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.billing.BillingPurchaseProvider
import com.i2hammad.admanagekit.billing.PurchaseItem
import com.i2hammad.admanagekit.billing.PurchaseListener
import com.i2hammad.admanagekit.core.NoPurchaseProvider
import com.i2hammad.admanagekit.config.AdLoadingStrategy
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class MyApplication : Application() {

    var appOpenManager: AppOpenManager? = null

    override fun onCreate() {
        super.onCreate()

        //If you want to use billing feature must use billing provider
//        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
//        If you do not want to use billing library for it
        BillingConfig.setPurchaseProvider(NoPurchaseProvider())
        
        // Configure AdManageKit with comprehensive settings
        configureAdManageKit()
        
        initBilling()
        appOpenManager = AppOpenManager(this, "ca-app-pub-3940256099942544/9257395921")
        appOpenManager?.disableAppOpenWithActivity(SplashActivity::class.java)

    }

    /**
     * Configure AdManageKit with comprehensive settings for testing all features
     */
    private fun configureAdManageKit() {
        AdManageKitConfig.apply {
            // =================== DEBUG AND TESTING ===================
            debugMode = true // Enable debug mode for testing
            testMode = true // Use test ads for testing
            privacyCompliantMode = true
            enableDebugOverlay = true // Show debug overlay for testing
            
            // =================== PERFORMANCE SETTINGS ===================
            defaultAdTimeout = 15.seconds
            appOpenAdTimeout = 10.seconds
            nativeCacheExpiry = 2.hours
            maxCachedAdsPerUnit = 3
            maxCacheMemoryMB = 50
            enableWelcomeBackDialog = true
            appOpenFetchFreshAd = true
            welcomeDialogAppIcon = R.mipmap.ic_launcher_round
            
            // =================== RELIABILITY FEATURES ===================
            autoRetryFailedAds = true
            maxRetryAttempts = 3
            circuitBreakerThreshold = 3 // Lower threshold for testing
            circuitBreakerResetTimeout = 60.seconds // Shorter reset for testing
            enableExponentialBackoff = true
            baseRetryDelay = 1.seconds
            maxRetryDelay = 10.seconds
            
            // =================== ADVANCED FEATURES ===================
            enableSmartPreloading = false
            enableAdaptiveIntervals = true
            enablePerformanceMetrics = true
            enableAutoCacheCleanup = true
            enableLRUEviction = true
            
            // =================== AD-SPECIFIC SETTINGS ===================
            defaultInterstitialInterval = 15.seconds
            defaultBannerRefreshInterval = 60.seconds
            enableCollapsibleBannersByDefault = false

            // =================== AD LOADING STRATEGIES ===================
            // Choose strategy based on your app type:

            // Option 1: HYBRID (Recommended) - Balanced approach
            // - Shows cached ad instantly if available
            // - Fetches fresh ad with loading dialog if cache is empty
            // - Good balance between UX and ad coverage
            interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
            appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
            nativeLoadingStrategy = AdLoadingStrategy.HYBRID

            // Option 2: ONLY_CACHE - Best for smooth UX (gaming apps)
            // - Only shows cached/preloaded ads
            // - No loading dialogs
            // - Skips ad if not cached
            // - Best for: Games, frequent ad opportunities
            // interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
            // appOpenLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
            // nativeLoadingStrategy = AdLoadingStrategy.ONLY_CACHE

            // Option 3: ON_DEMAND - Best for maximum ad coverage (utility apps)
            // - Always fetches fresh ads with loading dialog
            // - Best ad coverage but may interrupt flow
            // - Best for: Utility apps, important monetization points
            // interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND
            // appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND
            // nativeLoadingStrategy = AdLoadingStrategy.ON_DEMAND

            // Option 4: FRESH_WITH_CACHE_FALLBACK - Best for RecyclerView/lists
            // - Always tries to load fresh ad first
            // - Falls back to cache if fresh load fails
            // - Shows shimmer while loading fresh (native ads)
            // - Best for: RecyclerViews, feeds with multiple ad slots
            // nativeLoadingStrategy = AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK

            // You can also mix strategies for different ad types:
            // Example: Smooth app opens, aggressive interstitials, instant natives
            // appOpenLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
            // interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND
            // nativeLoadingStrategy = AdLoadingStrategy.ONLY_CACHE

            // =================== CROSS AD UNIT FALLBACK ===================
            // Enable to allow native ads from ANY cached ad unit when requested unit has no cache
            // Useful for RecyclerView scenarios where showing any ad is better than empty space
            enableCrossAdUnitFallback = false

            // =================== CACHE MANAGEMENT ===================
            cacheCleanupInterval = (5 * 60).seconds // 5 minutes for testing
        }
        
        // Validate configuration
        if (!AdManageKitConfig.validate()) {
            android.util.Log.w("MyApplication", "AdManageKit configuration validation failed")
        }
        
        // Log configuration summary
        android.util.Log.d("MyApplication", AdManageKitConfig.getConfigSummary())
        
        // Check production readiness
        if (!AdManageKitConfig.isProductionReady()) {
            android.util.Log.w("MyApplication", "AdManageKit configuration is not production ready!")
        }
        
        // Enable debug overlay in debug mode
        android.util.Log.d("MyApplication", "AdManageKit configured for TESTING mode with enhanced logging and debug features")
    }


    private fun initBilling() {


        val listPurchaseItem = listOf(
            PurchaseItem("life_time", AppPurchase.TYPE_IAP.PURCHASE, PurchaseItem.PurchaseCategory.LIFETIME_PREMIUM), // lifetime premium
            PurchaseItem("sub_monthly", AppPurchase.TYPE_IAP.SUBSCRIPTION),
            PurchaseItem("sub_half_yearly", AppPurchase.TYPE_IAP.SUBSCRIPTION),
            PurchaseItem("sub_yearly", AppPurchase.TYPE_IAP.SUBSCRIPTION)
        )

        AppPurchase.getInstance().initBilling(this, listPurchaseItem)

        AppPurchase.getInstance().setPurchaseListener(object : PurchaseListener {
            override fun displayErrorMessage(errorMessage: String?) {

            }

            override fun onProductPurchased(orderId: String?, originalJson: String?) {

            }

            override fun onUserCancelBilling() {}
        })
    }


    fun initAds() {

        val testDeviceIds: List<String> = mutableListOf(
            "EC60C39375F6619F5C03850A0E440646"
        )
        val configuration: RequestConfiguration =
            RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.initialize(this)

    }

    companion object {
        lateinit var instance: MyApplication
            private set
    }


    init {
        instance = this
    }

}