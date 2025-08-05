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
     * Default app open ad timeout before showing alternative content.
     * Default: 4 seconds
     */
    var appOpenAdTimeout: Duration = 4.seconds
    
    // =================== CACHE MANAGEMENT ===================
    
    /**
     * Maximum total memory usage for native ad cache (in MB).
     * Default: 50 MB
     */
    var maxCacheMemoryMB: Int = 50
    
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
        defaultBannerRefreshInterval = 60.seconds
        enableCollapsibleBannersByDefault = false
        appOpenAdTimeout = 4.seconds
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