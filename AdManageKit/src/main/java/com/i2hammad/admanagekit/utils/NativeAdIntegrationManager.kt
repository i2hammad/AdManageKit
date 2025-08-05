package com.i2hammad.admanagekit.utils

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.nativead.NativeAd
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.NativeAdManager
import com.i2hammad.admanagekit.config.AdManageKitConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Enhanced integration manager for NativeBanner components with smart caching.
 * 
 * This manager addresses multi-screen resource sharing by:
 * - Creating screen-specific cache keys
 * - Managing ad distribution across different UI contexts
 * - Providing intelligent preloading strategies
 * - Preventing cache collisions between screens
 * 
 * @since 2.1.0
 */
object NativeAdIntegrationManager {
    
    // Screen type identifiers
    enum class ScreenType(val suffix: String) {
        SMALL("_small"),
        MEDIUM("_medium"), 
        LARGE("_large"),
        GENERIC("_generic")
    }
    
    // Ad request tracking
    private val screenAdRequests = ConcurrentHashMap<String, AtomicLong>()
    private val activeScreens = ConcurrentHashMap<String, ScreenContext>()
    
    /**
     * Context information for each screen using native ads.
     */
    private data class ScreenContext(
        val screenType: ScreenType,
        val adUnitId: String,
        val lastRequestTime: Long = System.currentTimeMillis(),
        var requestCount: Long = 0
    )
    
    /**
     * Enhanced ad loading with screen-aware caching.
     * 
     * @param activity The activity context
     * @param baseAdUnitId The base ad unit ID (without screen suffix)
     * @param screenType The screen type requesting the ad
     * @param useCachedAd Whether to use cached ads
     * @param callback Ad loading callback
     * @param loadNewAd Function to load a new ad if cache miss
     */
    fun loadNativeAdWithCaching(
        activity: Activity,
        baseAdUnitId: String,
        screenType: ScreenType,
        useCachedAd: Boolean = true,
        callback: AdLoadCallback?,
        loadNewAd: (String, AdLoadCallback?) -> Unit
    ) {
        val screenKey = "${activity.javaClass.simpleName}_${screenType.name}"
        val enhancedAdUnitId = createScreenSpecificAdUnitId(baseAdUnitId, screenType, screenKey)
        
        // Track screen usage
        trackScreenUsage(screenKey, baseAdUnitId, screenType)
        
        logDebug("Loading ad for screen: $screenKey, adUnit: $enhancedAdUnitId, useCache: $useCachedAd")
        
        // Try to get cached ad first if requested and caching is enabled
        if (useCachedAd && NativeAdManager.enableCachingNativeAds && AdManageKitConfig.enableSmartPreloading) {
            val cachedAd = tryGetCachedAd(enhancedAdUnitId, baseAdUnitId, screenType)
            
            if (cachedAd != null) {
                logDebug("Cache hit for $screenKey: serving cached ad")
                callback?.onAdLoaded()
                return
            } else {
                logDebug("Cache miss for $screenKey: loading new ad")
            }
        }
        
        // Load new ad with enhanced callback
        val enhancedCallback = createEnhancedCallback(enhancedAdUnitId, screenKey, callback)
        loadNewAd(enhancedAdUnitId, enhancedCallback)
    }
    
    /**
     * Creates screen-specific ad unit ID to prevent cache collisions.
     */
    private fun createScreenSpecificAdUnitId(
        baseAdUnitId: String, 
        screenType: ScreenType, 
        screenKey: String
    ): String {
        // For production ad units, use base ID directly
        // For caching, we'll use a composite key that includes screen context
        return when {
            // Test ad units - keep as is
            baseAdUnitId.contains("ca-app-pub-3940256099942544") -> baseAdUnitId
            
            // Production ad units - create screen-aware cache key but use original for AdMob
            else -> baseAdUnitId // We'll handle screen differentiation in cache key
        }
    }
    
    /**
     * Tries to get a cached ad with fallback strategies.
     */
    private fun tryGetCachedAd(
        enhancedAdUnitId: String, 
        baseAdUnitId: String, 
        screenType: ScreenType
    ): NativeAd? {
        // Strategy 1: Try screen-specific cache first
        val screenSpecificKey = "${baseAdUnitId}${screenType.suffix}"
        var cachedAd = NativeAdManager.getCachedNativeAd(screenSpecificKey)
        
        if (cachedAd != null) {
            logDebug("Found screen-specific cached ad for $screenSpecificKey")
            return cachedAd
        }
        
        // Strategy 2: Try base ad unit cache (shared across screens)
        cachedAd = NativeAdManager.getCachedNativeAd(baseAdUnitId)
        
        if (cachedAd != null) {
            logDebug("Found shared cached ad for $baseAdUnitId")
            return cachedAd
        }
        
        // Strategy 3: Try generic cache for same screen type
        val genericKey = "generic${screenType.suffix}"
        cachedAd = NativeAdManager.getCachedNativeAd(genericKey)
        
        if (cachedAd != null) {
            logDebug("Found generic cached ad for screen type ${screenType.name}")
            return cachedAd
        }
        
        return null
    }
    
    /**
     * Creates enhanced callback that handles caching with screen awareness.
     */
    private fun createEnhancedCallback(
        enhancedAdUnitId: String,
        screenKey: String,
        originalCallback: AdLoadCallback?
    ): AdLoadCallback {
        return object : AdLoadCallback() {
            override fun onAdLoaded() {
                logDebug("Ad loaded successfully for $screenKey")
                originalCallback?.onAdLoaded()
            }
            
            override fun onFailedToLoad(error: com.google.android.gms.ads.AdError?) {
                logDebug("Ad failed to load for $screenKey: ${error?.message}")
                originalCallback?.onFailedToLoad(error)
            }
            
            override fun onAdClicked() {
                logDebug("Ad clicked for $screenKey")
                originalCallback?.onAdClicked()
            }
            
            override fun onAdClosed() {
                originalCallback?.onAdClosed()
            }
            
            override fun onAdImpression() {
                logDebug("Ad impression for $screenKey")
                originalCallback?.onAdImpression()
            }
            
            override fun onAdOpened() {
                originalCallback?.onAdOpened()
            }
            
            override fun onPaidEvent(adValue: com.google.android.gms.ads.AdValue) {
                logDebug("Paid event for $screenKey: ${adValue.valueMicros}")
                originalCallback?.onPaidEvent(adValue)
            }
        }
    }
    
    /**
     * Caches a native ad with screen-aware strategy.
     */
    fun cacheNativeAdWithScreenContext(
        baseAdUnitId: String,
        screenType: ScreenType,
        screenKey: String,
        nativeAd: NativeAd
    ) {
        if (!NativeAdManager.enableCachingNativeAds || !AdManageKitConfig.enableSmartPreloading) return
        
        // Cache with screen-specific key for dedicated access
        val screenSpecificKey = "${baseAdUnitId}${screenType.suffix}"
        NativeAdManager.setCachedNativeAd(screenSpecificKey, nativeAd)
        
        // Also cache with base key for cross-screen sharing (lower priority)
        NativeAdManager.setCachedNativeAd(baseAdUnitId, nativeAd)
        
        logDebug("Cached ad for screen $screenKey with keys: [$screenSpecificKey, $baseAdUnitId]")
    }
    
    /**
     * Tracks screen usage patterns for optimization.
     */
    private fun trackScreenUsage(screenKey: String, baseAdUnitId: String, screenType: ScreenType) {
        val requestCount = screenAdRequests.getOrPut(screenKey) { AtomicLong(0) }
        val count = requestCount.incrementAndGet()
        
        activeScreens[screenKey] = ScreenContext(screenType, baseAdUnitId, requestCount = count)
        
        logDebug("Screen usage: $screenKey has requested $count ads")
    }
    
    /**
     * Preloads ads for active screens based on usage patterns.
     */
    fun preloadAdsForActiveScreens(onComplete: ((Int, Int) -> Unit)? = null) {
        if (!NativeAdManager.enableCachingNativeAds || !AdManageKitConfig.enableSmartPreloading) {
            onComplete?.invoke(0, 0)
            return
        }
        
        val activeScreensList = activeScreens.values.toList()
        val totalScreens = activeScreensList.size
        var preloadedScreens = 0
        
        logDebug("Starting preload for $totalScreens active screens")
        
        val adUnitsToWarm = mutableMapOf<String, Int>()
        
        activeScreensList.forEach { screenContext ->
            val screenSpecificKey = "${screenContext.adUnitId}${screenContext.screenType.suffix}"
            val currentCacheSize = NativeAdManager.getCacheSize(screenSpecificKey)
            
            // Determine preload count based on usage frequency and configuration
            val recommendedCacheSize = when {
                screenContext.requestCount > 10 -> minOf(3, AdManageKitConfig.maxCachedAdsPerUnit) // High usage
                screenContext.requestCount > 5 -> minOf(2, AdManageKitConfig.maxCachedAdsPerUnit)  // Medium usage
                else -> 1 // Low usage
            }
            
            val neededAds = maxOf(0, recommendedCacheSize - currentCacheSize)
            if (neededAds > 0) {
                adUnitsToWarm[screenSpecificKey] = neededAds
            }
            
            preloadedScreens++
        }
        
        if (adUnitsToWarm.isNotEmpty()) {
            logDebug("Warming cache for ${adUnitsToWarm.size} ad units")
            NativeAdManager.warmCache(adUnitsToWarm) { warmed, total ->
                logDebug("Cache preload completed: $warmed/$total units warmed")
                onComplete?.invoke(preloadedScreens, totalScreens)
            }
        } else {
            logDebug("No cache warming needed - all screens have sufficient ads")
            onComplete?.invoke(preloadedScreens, totalScreens)
        }
    }
    
    /**
     * Gets usage statistics for all active screens.
     */
    fun getScreenUsageStats(): Map<String, Map<String, Any>> {
        val stats = mutableMapOf<String, Map<String, Any>>()
        
        activeScreens.forEach { (screenKey, context) ->
            val screenSpecificKey = "${context.adUnitId}${context.screenType.suffix}"
            val cacheSize = NativeAdManager.getCacheSize(screenSpecificKey)
            val sharedCacheSize = NativeAdManager.getCacheSize(context.adUnitId)
            
            stats[screenKey] = mapOf(
                "screen_type" to context.screenType.name,
                "ad_unit_id" to context.adUnitId,
                "request_count" to context.requestCount,
                "dedicated_cache_size" to cacheSize,
                "shared_cache_size" to sharedCacheSize,
                "last_request_time" to context.lastRequestTime
            )
        }
        
        return stats
    }
    
    /**
     * Clears screen usage tracking and caches for a specific screen.
     */
    fun clearScreenCache(screenKey: String) {
        activeScreens[screenKey]?.let { context ->
            val screenSpecificKey = "${context.adUnitId}${context.screenType.suffix}"
            NativeAdManager.clearCachedAd(screenSpecificKey)
            
            activeScreens.remove(screenKey)
            screenAdRequests.remove(screenKey)
            
            logDebug("Cleared cache and tracking for screen: $screenKey")
        }
    }
    
    /**
     * Optimizes cache distribution across screens.
     */
    fun optimizeCacheDistribution() {
        val stats = getScreenUsageStats()
        logDebug("Cache optimization: analyzing ${stats.size} active screens")
        
        stats.forEach { (screenKey, screenStats) ->
            val requestCount = screenStats["request_count"] as Long
            val cacheSize = screenStats["dedicated_cache_size"] as Int
            
            // Adjust cache size based on usage patterns
            when {
                requestCount > 20 && cacheSize < 3 -> {
                    logDebug("High usage screen $screenKey needs more cache (current: $cacheSize)")
                    // Could trigger preloading here
                }
                requestCount < 2 && cacheSize > 1 -> {
                    logDebug("Low usage screen $screenKey could use less cache (current: $cacheSize)")
                    // Could clear some cache here
                }
            }
        }
    }
    
    private fun logDebug(message: String) {
        AdDebugUtils.logDebug("NativeAdIntegration", message)
    }
}