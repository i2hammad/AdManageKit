package com.i2hammad.admanagekit.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Central configuration object for AdManageKit library.
 * 
 * Provides a single point of configuration for all ad management features including:
 * - Performance settings (timeouts, cache sizes, intervals)
 * - Reliability features (retry logic, circuit breaker)
 * - Advanced features (smart preloading, adaptive intervals)
 * - Debug and testing options
 * 
 * Usage:
 * ```kotlin
 * // Configure in Application.onCreate()
 * AdManageKitConfig.apply {
 *     debugMode = BuildConfig.DEBUG
 *     enableSmartPreloading = true
 *     autoRetryFailedAds = true
 *     maxRetryAttempts = 3
 *     enablePerformanceMetrics = true
 * }
 * ```
 * 
 * @since 2.1.0
 */
object AdManageKitConfig {
    
    // =================== PERFORMANCE SETTINGS ===================
    
    /**
     * Default timeout for ad loading operations.
     * Default: 15 seconds
     */
    var defaultAdTimeout: Duration = 15.seconds
    
    /**
     * Cache expiry time for native ads.
     * Default: 1 hour
     */
    var nativeCacheExpiry: Duration = 1.hours
    
    /**
     * Maximum number of cached ads per ad unit.
     * Default: 3
     */
    var maxCachedAdsPerUnit: Int = 3

    /**
     * Enable cross ad unit fallback for native ad caching.
     * When true, if no cached ad is found for the requested ad unit,
     * the system will try to return ANY available cached ad from other ad units.
     *
     * Useful for RecyclerView scenarios where showing any ad is better than empty space.
     * Default: false
     */
    var enableCrossAdUnitFallback: Boolean = false

    // =================== RELIABILITY FEATURES ===================
    
    /**
     * Enable automatic retry for failed ad loads.
     * Default: true
     */
    var autoRetryFailedAds: Boolean = true
    
    /**
     * Maximum number of retry attempts for failed ad loads.
     * Default: 3
     */
    var maxRetryAttempts: Int = 3
    
    /**
     * Number of consecutive failures before circuit breaker trips.
     * Default: 5
     */
    var circuitBreakerThreshold: Int = 5
    
    /**
     * Time to wait before attempting to reset circuit breaker.
     * Default: 300 seconds (5 minutes)
     */
    var circuitBreakerResetTimeout: Duration = 300.seconds
    
    // =================== ADVANCED FEATURES ===================
    
    /**
     * Enable smart preloading of ads based on usage patterns.
     * Default: false
     */
    var enableSmartPreloading: Boolean = false
    
    /**
     * Enable adaptive intervals that adjust based on success rates.
     * Default: false
     */
    var enableAdaptiveIntervals: Boolean = false
    
    /**
     * Enable detailed performance metrics collection.
     * Metrics are sent to Firebase Analytics if available.
     * Default: false
     */
    var enablePerformanceMetrics: Boolean = false
    
    /**
     * Enable automatic cache cleanup on low memory conditions.
     * Default: true
     */
    var enableAutoCacheCleanup: Boolean = true
    
    // =================== DEBUG AND TESTING ===================
    
    /**
     * Enable debug mode with enhanced logging and debug overlays.
     * Should be set to BuildConfig.DEBUG in production apps.
     * Default: false
     */
    var debugMode: Boolean = false
    
    /**
     * Enable test mode which uses test ad units and mock responses.
     * Should only be used during development/testing.
     * Default: false
     */
    var testMode: Boolean = false
    
    /**
     * Enable privacy compliant mode for GDPR/CCPA compliance.
     * Disables some tracking features when user hasn't consented.
     * Default: true
     */
    var privacyCompliantMode: Boolean = true
    
    /**
     * Enable debug overlay showing real-time ad statistics.
     * Only works when debugMode = true.
     * Default: false
     */
    var enableDebugOverlay: Boolean = false
    
    // =================== AD-SPECIFIC SETTINGS ===================
    
    /**
     * Default interval for interstitial ad display timing.
     * Default: 15 seconds
     */
    var defaultInterstitialInterval: Duration = 15.seconds

    /**
     * Enable auto-reload of interstitial ads after showing.
     * When true, a new ad is automatically loaded after the current one is dismissed.
     * When false, you must manually call loadInterstitialAd() to load the next ad.
     *
     * Can be overridden per-call using InterstitialAdBuilder.autoReload()
     *
     * Default: true
     */
    var interstitialAutoReload: Boolean = true
    
    /**
     * Default auto-refresh interval for banner ads.
     * Minimum value is 30 seconds per AdMob policies.
     * Default: 60 seconds
     */
    var defaultBannerRefreshInterval: Duration = 60.seconds
    
    /**
     * Enable collapsible banner ads by default.
     * Default: false
     */
    var enableCollapsibleBannersByDefault: Boolean = false

    /**
     * Default placement/direction for collapsible banners.
     * Only used when enableCollapsibleBannersByDefault is true.
     *
     * Example:
     * ```kotlin
     * AdManageKitConfig.defaultCollapsiblePlacement = CollapsibleBannerPlacement.TOP
     * ```
     *
     * Default: BOTTOM
     */
    var defaultCollapsiblePlacement: CollapsibleBannerPlacement = CollapsibleBannerPlacement.BOTTOM
    
    /**
     * Default app open ad timeout before showing alternative content.
     * Default: 4 seconds
     */
    var appOpenAdTimeout: Duration = 4.seconds

    /**
     * Delay before dismissing welcome dialog after app open ad is shown.
     * The dialog stays visible in the background when the ad first appears,
     * then dismisses after this delay (while ad continues to show).
     *
     * Set to 0 for immediate dismissal when ad shows.
     * Default: 800 milliseconds (0.8 seconds)
     */
    var welcomeDialogDismissDelay: Duration = 0.8.seconds

    /**
     * Enable beautiful welcome back dialog when loading app open ads.
     * Shows animated full-screen loading UI while ad is being fetched.
     *
     * Note: Welcome dialog is now always shown when fetching ads.
     * This setting is kept for backward compatibility but has no effect.
     *
     * @deprecated Welcome dialog is always shown. This setting will be removed in future versions.
     * Default: false
     */
    @Deprecated("Welcome dialog is always shown. This setting has no effect.")
    var enableWelcomeBackDialog: Boolean = false

    /**
     * Control automatic background fetching of app open ads.
     * When false: Automatically fetches and caches ads in background for instant display
     * When true: Only fetches ads on-demand when needed (no background prefetching)
     *
     * Note: Existing cached ads are always used if available regardless of this setting.
     * This setting only controls whether to automatically prefetch/cache ads.
     *
     * Default: false (enable background prefetching for better performance)
     */
    var appOpenFetchFreshAd: Boolean = false

    /**
     * App icon resource ID to display in the welcome back dialog.
     * Set this to your app's launcher icon resource.
     *
     * Example:
     * ```kotlin
     * AdManageKitConfig.welcomeDialogAppIcon = R.mipmap.ic_launcher
     * ```
     *
     * Default: 0 (uses default icon)
     */
    var welcomeDialogAppIcon: Int = 0

    // =================== DIALOG CUSTOMIZATION ===================

    /**
     * Dialog background color. Set to 0 for transparent background.
     * Applies to both interstitial and app open ad loading dialogs.
     *
     * Example:
     * ```kotlin
     * AdManageKitConfig.dialogBackgroundColor = Color.WHITE
     * AdManageKitConfig.dialogBackgroundColor = 0 // Transparent
     * ```
     *
     * Default: 0 (transparent)
     */
    var dialogBackgroundColor: Int = 0

    /**
     * Dialog overlay color (semi-transparent layer behind the card).
     * Set to 0 to hide overlay completely.
     *
     * Example:
     * ```kotlin
     * AdManageKitConfig.dialogOverlayColor = 0x80000000.toInt() // 50% black
     * AdManageKitConfig.dialogOverlayColor = 0xCCFFFFFF.toInt() // 80% white
     * AdManageKitConfig.dialogOverlayColor = 0 // No overlay
     * ```
     *
     * Default: 0x80000000 (50% black overlay)
     */
    var dialogOverlayColor: Int = 0x80000000.toInt()

    /**
     * Dialog card background color (the main content card).
     * Set to 0 to use theme's default background color.
     *
     * Example:
     * ```kotlin
     * AdManageKitConfig.dialogCardBackgroundColor = Color.WHITE
     * AdManageKitConfig.dialogCardBackgroundColor = 0 // Use theme color
     * ```
     *
     * Default: 0 (uses theme's colorBackground)
     */
    var dialogCardBackgroundColor: Int = 0

    /**
     * Title text for the welcome back dialog (app open ads).
     * Set to null to use default text.
     *
     * Default: null (uses "Welcome Back!")
     */
    var welcomeDialogTitle: String? = null

    /**
     * Subtitle text for the welcome back dialog (app open ads).
     * Set to null to use default text.
     *
     * Default: null (uses "Loading your content...")
     */
    var welcomeDialogSubtitle: String? = null

    /**
     * Footer text for the welcome back dialog (app open ads).
     * Set to null to use default text.
     *
     * Default: null (uses "Just a moment...")
     */
    var welcomeDialogFooter: String? = null

    /**
     * Title text for the loading dialog (interstitial ads).
     * Set to null to use default text.
     *
     * Default: null (uses "Loading Ad")
     */
    var loadingDialogTitle: String? = null

    /**
     * Subtitle text for the loading dialog (interstitial ads).
     * Set to null to use default text.
     *
     * Default: null (uses "Please wait...")
     */
    var loadingDialogSubtitle: String? = null

    // =================== AD LOADING STRATEGIES ===================

    /**
     * Loading strategy for interstitial ads.
     *
     * - **ON_DEMAND**: Always fetch fresh ad with loading dialog when needed
     * - **ONLY_CACHE**: Only show if cached ad is ready, skip if not
     * - **HYBRID**: Check cache first, fetch with dialog if not available (recommended)
     *
     * Example:
     * ```kotlin
     * // For smooth gameplay - only show cached ads
     * AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
     *
     * // For important moments - always try to show
     * AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.ON_DEMAND
     *
     * // Balanced approach (default)
     * AdManageKitConfig.interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
     * ```
     *
     * Default: HYBRID
     */
    var interstitialLoadingStrategy: AdLoadingStrategy = AdLoadingStrategy.HYBRID

    /**
     * Loading strategy for app open ads.
     *
     * - **ON_DEMAND**: Always fetch fresh ad with welcome dialog when app opens
     * - **ONLY_CACHE**: Only show if cached ad is ready, skip if not
     * - **HYBRID**: Check cache first, fetch with welcome dialog if not available (recommended)
     *
     * Default: HYBRID
     */
    var appOpenLoadingStrategy: AdLoadingStrategy = AdLoadingStrategy.HYBRID

    /**
     * Loading strategy for native ads.
     *
     * - **ON_DEMAND**: Always fetch fresh ad, show shimmer while loading
     * - **ONLY_CACHE**: Only show if cached ad is ready, hide ad container if not
     * - **HYBRID**: Check cache first, fetch with shimmer if not available (recommended)
     *
     * Note: Native ads don't have loading dialogs. Instead:
     * - ON_DEMAND/HYBRID: Shows shimmer effect while loading
     * - ONLY_CACHE: Hides container immediately if no cache
     *
     * Example:
     * ```kotlin
     * // For instant display only
     * AdManageKitConfig.nativeLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
     *
     * // Always try to show (may show shimmer)
     * AdManageKitConfig.nativeLoadingStrategy = AdLoadingStrategy.ON_DEMAND
     * ```
     *
     * Default: HYBRID
     */
    var nativeLoadingStrategy: AdLoadingStrategy = AdLoadingStrategy.HYBRID

    // =================== CACHE MANAGEMENT ===================
    
    /**
     * Maximum total memory usage for native ad cache (in MB).
     * Default: 200 MB
     */
    var maxCacheMemoryMB: Int = 200
    
    /**
     * Enable LRU (Least Recently Used) eviction for cache management.
     * Default: true
     */
    var enableLRUEviction: Boolean = true
    
    /**
     * Interval for automatic cache cleanup operations.
     * Default: 30 minutes
     */
    var cacheCleanupInterval: Duration = 30.seconds * 60 // 30 minutes
    
    // =================== NETWORK SETTINGS ===================
    
    /**
     * Enable exponential backoff for retry attempts.
     * Default: true
     */
    var enableExponentialBackoff: Boolean = true
    
    /**
     * Base delay for exponential backoff (first retry delay).
     * Default: 1 second
     */
    var baseRetryDelay: Duration = 1.seconds
    
    /**
     * Maximum delay for exponential backoff.
     * Default: 30 seconds
     */
    var maxRetryDelay: Duration = 30.seconds
    
    // =================== UTILITY METHODS ===================
    
    /**
     * Reset all configuration values to their defaults.
     */
    fun resetToDefaults() {
        defaultAdTimeout = 15.seconds
        nativeCacheExpiry = 1.hours
        maxCachedAdsPerUnit = 3
        enableCrossAdUnitFallback = false
        autoRetryFailedAds = true
        maxRetryAttempts = 3
        circuitBreakerThreshold = 5
        circuitBreakerResetTimeout = 300.seconds
        enableSmartPreloading = false
        enableAdaptiveIntervals = false
        enablePerformanceMetrics = false
        enableAutoCacheCleanup = true
        debugMode = false
        testMode = false
        privacyCompliantMode = true
        enableDebugOverlay = false
        defaultInterstitialInterval = 15.seconds
        interstitialAutoReload = true
        defaultBannerRefreshInterval = 60.seconds
        enableCollapsibleBannersByDefault = false
        defaultCollapsiblePlacement = CollapsibleBannerPlacement.BOTTOM
        appOpenAdTimeout = 4.seconds
        enableWelcomeBackDialog = false
        appOpenFetchFreshAd = false
        welcomeDialogAppIcon = 0
        welcomeDialogDismissDelay = 0.8.seconds
        dialogBackgroundColor = 0
        dialogOverlayColor = 0x80000000.toInt()
        dialogCardBackgroundColor = 0
        welcomeDialogTitle = null
        welcomeDialogSubtitle = null
        welcomeDialogFooter = null
        loadingDialogTitle = null
        loadingDialogSubtitle = null
        interstitialLoadingStrategy = AdLoadingStrategy.HYBRID
        appOpenLoadingStrategy = AdLoadingStrategy.HYBRID
        nativeLoadingStrategy = AdLoadingStrategy.HYBRID
        maxCacheMemoryMB = 50
        enableLRUEviction = true
        cacheCleanupInterval = 30.seconds * 60
        enableExponentialBackoff = true
        baseRetryDelay = 1.seconds
        maxRetryDelay = 30.seconds
    }
    
    /**
     * Validate current configuration and log warnings for invalid settings.
     */
    fun validate(): Boolean {
        var isValid = true
        
        if (maxRetryAttempts < 0 || maxRetryAttempts > 10) {
            if (debugMode) {
                android.util.Log.w("AdManageKitConfig", "maxRetryAttempts should be between 0-10, current: $maxRetryAttempts")
            }
            isValid = false
        }
        
        if (maxCachedAdsPerUnit < 1 || maxCachedAdsPerUnit > 10) {
            if (debugMode) {
                android.util.Log.w("AdManageKitConfig", "maxCachedAdsPerUnit should be between 1-10, current: $maxCachedAdsPerUnit")
            }
            isValid = false
        }
        
        if (defaultBannerRefreshInterval.inWholeSeconds < 30) {
            if (debugMode) {
                android.util.Log.w("AdManageKitConfig", "defaultBannerRefreshInterval should be at least 30 seconds per AdMob policy")
            }
            isValid = false
        }
        
        if (maxCacheMemoryMB < 10 || maxCacheMemoryMB > 200) {
            if (debugMode) {
                android.util.Log.w("AdManageKitConfig", "maxCacheMemoryMB should be between 10-200 MB, current: $maxCacheMemoryMB")
            }
            isValid = false
        }
        
        return isValid
    }
    
    /**
     * Get a summary of current configuration for debugging.
     */
    fun getConfigSummary(): String {
        return buildString {
            appendLine("AdManageKit Configuration:")
            appendLine("- Debug Mode: $debugMode")
            appendLine("- Test Mode: $testMode")
            appendLine("- Auto Retry: $autoRetryFailedAds (max: $maxRetryAttempts)")
            appendLine("- Smart Preloading: $enableSmartPreloading")
            appendLine("- Performance Metrics: $enablePerformanceMetrics")
            appendLine("- Cache Settings: ${maxCachedAdsPerUnit} ads/unit, ${nativeCacheExpiry}")
            appendLine("- Circuit Breaker: threshold=$circuitBreakerThreshold")
            appendLine("- Banner Refresh: ${defaultBannerRefreshInterval}")
            appendLine("- Privacy Compliant: $privacyCompliantMode")
        }
    }
    
    /**
     * Check if current configuration is suitable for production.
     */
    fun isProductionReady(): Boolean {
        return !testMode && 
               !enableDebugOverlay && 
               privacyCompliantMode &&
               validate()
    }
}