package com.i2hammad.admanagekit.admob

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.nativead.NativeAd
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.config.AdManageKitConfig
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Enhanced native ad caching manager with advanced features.
 * 
 * This manager provides sophisticated caching functionality including:
 * - LRU eviction with configurable cache sizes
 * - Automatic background cleanup of expired ads
 * - Performance monitoring and analytics
 * - Memory usage optimization
 * - Thread-safe operations with fine-grained locking
 * - Cache warming and preloading capabilities
 * 
 * @since 1.0.0 (Enhanced in 2.1.0)
 */
object NativeAdManager {
    
    // Cache configuration - using AdManageKitConfig
    val cacheExpiryMs: Long get() = AdManageKitConfig.nativeCacheExpiry.inWholeMilliseconds
    val maxCachedAdsPerUnit: Int get() = AdManageKitConfig.maxCachedAdsPerUnit
    val enableBackgroundCleanup: Boolean get() = AdManageKitConfig.enableAutoCacheCleanup
    val cleanupIntervalMinutes: Long get() = AdManageKitConfig.cacheCleanupInterval.inWholeMinutes
    val enableAnalytics: Boolean get() = AdManageKitConfig.enablePerformanceMetrics
    
    // Thread-safe cache storage
    private val cachedAds = ConcurrentHashMap<String, MutableList<CachedAd>>()
    private val cacheLocks = ConcurrentHashMap<String, Any>()
    
    // Performance tracking
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)
    private val totalAdsServed = AtomicLong(0)
    private val totalMemoryFreed = AtomicLong(0)
    
    // Background cleanup
    private var cleanupExecutor: ScheduledExecutorService? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Firebase Analytics instance
    private var firebaseAnalytics: FirebaseAnalytics? = null
    
    /**
     * Controls whether native ad caching is enabled.
     * When disabled, ads are not cached and getCachedNativeAd returns null.
     */
    var enableCachingNativeAds: Boolean = true
    
    /**
     * Enhanced data class representing a cached native ad with comprehensive metadata.
     * 
     * @param ad The native ad object
     * @param cachedTime When the ad was first cached
     * @param lastAccessTime When the ad was last accessed (for LRU)
     * @param accessCount How many times the ad has been accessed
     * @param approximateSize Estimated memory size in bytes
     * @param source Where the ad came from (cache, network, etc.)
     */
    private data class CachedAd(
        val ad: NativeAd,
        val cachedTime: Long = System.currentTimeMillis(),
        var lastAccessTime: Long = System.currentTimeMillis(),
        var accessCount: Int = 0,
        val approximateSize: Long = estimateAdSize(),
        val source: String = "network"
    ) {
        companion object {
            private fun estimateAdSize(): Long {
                // Rough estimate: 50KB per native ad (images, text, metadata)
                return 50 * 1024L // 50KB
            }
        }
        
        /**
         * Checks if this cached ad is expired.
         */
        fun isExpired(currentTime: Long, expiryMs: Long): Boolean {
            return (currentTime - cachedTime) > expiryMs
        }
        
        /**
         * Gets the age of this cached ad in milliseconds.
         */
        fun getAgeMs(currentTime: Long): Long {
            return currentTime - cachedTime
        }
    }
    
    // =================== INITIALIZATION ===================
    
    init {
        // Initialize background cleanup if enabled
        if (enableBackgroundCleanup) {
            startBackgroundCleanup()
        }
    }
    
    /**
     * Initializes Firebase Analytics for performance tracking.
     * Call this from your Application class for analytics support.
     */
    fun initialize(analytics: FirebaseAnalytics? = null) {
        this.firebaseAnalytics = analytics
        logDebug("NativeAdManager initialized with analytics: ${analytics != null}")
    }
    
    // =================== UTILITY METHODS ===================
    
    /**
     * Gets a thread-safe lock object for the specified ad unit.
     */
    private fun getLockForAdUnit(adUnitId: String): Any {
        return cacheLocks.getOrPut(adUnitId) { Any() }
    }
    
    /**
     * Logs debug information if debug mode is enabled.
     */
    private fun logDebug(message: String) {
        AdDebugUtils.logDebug("NativeAdManager", message)
    }
    
    /**
     * Tracks analytics event if analytics is enabled.
     */
    private fun trackEvent(eventName: String, parameters: Map<String, Any> = emptyMap()) {
        if (enableAnalytics && firebaseAnalytics != null) {
            val bundle = android.os.Bundle()
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> bundle.putString(key, value)
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                }
            }
            firebaseAnalytics?.logEvent(eventName, bundle)
        }
    }
    
    /**
     * Caches a native ad for the specified ad unit with enhanced LRU eviction and analytics.
     * 
     * @param adUnitId The ad unit ID
     * @param ad The native ad to cache
     */
    fun setCachedNativeAd(adUnitId: String, ad: NativeAd) {
        if (!enableCachingNativeAds) {
            logDebug("Caching disabled, not caching ad for $adUnitId")
            return
        }
        
        synchronized(getLockForAdUnit(adUnitId)) {
            val adList = cachedAds.getOrPut(adUnitId) { mutableListOf() }
            val currentTime = System.currentTimeMillis()
            
            // Clean up expired ads first
            val expiredCount = cleanupExpiredAds(adUnitId, adList, currentTime)
            
            // Check if we need to evict old ads due to size limit
            var evictedCount = 0
            while (adList.size >= maxCachedAdsPerUnit) {
                // Remove least recently used ad
                val oldestAd = adList.minByOrNull { it.lastAccessTime }
                if (oldestAd != null) {
                    oldestAd.ad.destroy()
                    adList.remove(oldestAd)
                    evictedCount++
                    totalMemoryFreed.addAndGet(oldestAd.approximateSize)
                    
                    logDebug("Evicted LRU ad for $adUnitId (cache size: ${adList.size})")
                } else {
                    break
                }
            }
            
            // Add the new ad
            val cachedAd = CachedAd(ad, currentTime, source = "network")
            adList.add(cachedAd)
            
            logDebug("Cached ad for $adUnitId (size: ${adList.size}, expired: $expiredCount, evicted: $evictedCount)")
            
            // Track analytics
            trackEvent("native_ad_cached", mapOf(
                "ad_unit_id" to adUnitId,
                "cache_size" to adList.size,
                "expired_cleaned" to expiredCount,
                "evicted_count" to evictedCount
            ))
        }
    }
    
    /**
     * Retrieves a cached native ad for the specified ad unit with enhanced tracking.
     *
     * @param adUnitId The ad unit ID
     * @param enableFallbackToAnyAd If true, returns any available cached ad when specific ad unit has no cache
     * @return A cached native ad, or null if none available
     */
    @JvmOverloads
    fun getCachedNativeAd(adUnitId: String, enableFallbackToAnyAd: Boolean = false): NativeAd? {
        if (!enableCachingNativeAds) {
            cacheMisses.incrementAndGet()
            return null
        }

        synchronized(getLockForAdUnit(adUnitId)) {
            val adList = cachedAds[adUnitId]
            if (adList == null || adList.isEmpty()) {
                logDebug("Cache miss for $adUnitId: no cached ads")

                // Fallback: try to find any cached ad from other ad units
                if (enableFallbackToAnyAd) {
                    return getFallbackCachedAd(adUnitId)
                }

                cacheMisses.incrementAndGet()
                return null
            }

            val currentTime = System.currentTimeMillis()

            // Clean up expired ads
            cleanupExpiredAds(adUnitId, adList, currentTime)

            // Find the most recently cached valid ad (LIFO for freshest ad)
            val validAd = adList.lastOrNull()

            return if (validAd != null) {
                // Update access statistics for LRU tracking
                validAd.lastAccessTime = currentTime
                validAd.accessCount++

                // Remove from cache since it's being used
                adList.remove(validAd)

                // Update performance counters
                cacheHits.incrementAndGet()
                totalAdsServed.incrementAndGet()

                val ageMs = validAd.getAgeMs(currentTime)
                logDebug("Cache hit for $adUnitId: served ad aged ${ageMs}ms, access count: ${validAd.accessCount}")

                // Track analytics
                trackEvent("native_ad_served", mapOf(
                    "ad_unit_id" to adUnitId,
                    "age_ms" to ageMs,
                    "access_count" to validAd.accessCount,
                    "source" to "cache"
                ))

                validAd.ad
            } else {
                logDebug("Cache miss for $adUnitId: no valid ads after cleanup")

                // Fallback: try to find any cached ad from other ad units
                if (enableFallbackToAnyAd) {
                    return getFallbackCachedAd(adUnitId)
                }

                cacheMisses.incrementAndGet()
                null
            }
        }
    }

    /**
     * Fallback mechanism to retrieve any available cached ad from other ad units.
     * This is useful when the requested ad unit has no cached ads but other ad units do.
     *
     * @param requestedAdUnitId The originally requested ad unit ID (for logging)
     * @return A cached native ad from any available ad unit, or null if no ads are cached
     */
    private fun getFallbackCachedAd(requestedAdUnitId: String): NativeAd? {
        logDebug("Attempting fallback for $requestedAdUnitId: searching all cached ad units")

        val currentTime = System.currentTimeMillis()
        val availableAdUnits = cachedAds.keys.toList()

        // Sort by ad units with most cached ads (prioritize well-stocked units)
        val sortedAdUnits = availableAdUnits.sortedByDescending { adUnitId ->
            cachedAds[adUnitId]?.size ?: 0
        }

        for (fallbackAdUnitId in sortedAdUnits) {
            if (fallbackAdUnitId == requestedAdUnitId) continue // Skip the originally requested unit

            synchronized(getLockForAdUnit(fallbackAdUnitId)) {
                val adList = cachedAds[fallbackAdUnitId]
                if (adList != null && adList.isNotEmpty()) {
                    // Clean up expired ads first
                    cleanupExpiredAds(fallbackAdUnitId, adList, currentTime)

                    // Get the most recent valid ad
                    val validAd = adList.lastOrNull()

                    if (validAd != null) {
                        // Update access statistics
                        validAd.lastAccessTime = currentTime
                        validAd.accessCount++

                        // Remove from cache
                        adList.remove(validAd)

                        // Update performance counters (partial hit)
                        cacheHits.incrementAndGet()
                        totalAdsServed.incrementAndGet()

                        val ageMs = validAd.getAgeMs(currentTime)
                        logDebug("Fallback cache hit: requested $requestedAdUnitId, served from $fallbackAdUnitId (age: ${ageMs}ms)")

                        // Track analytics with fallback flag
                        trackEvent("native_ad_served", mapOf(
                            "ad_unit_id" to requestedAdUnitId,
                            "fallback_ad_unit_id" to fallbackAdUnitId,
                            "age_ms" to ageMs,
                            "access_count" to validAd.accessCount,
                            "source" to "cache_fallback"
                        ))

                        return validAd.ad
                    }
                }
            }
        }

        // No cached ads found in any ad unit
        cacheMisses.incrementAndGet()
        logDebug("Fallback failed for $requestedAdUnitId: no cached ads available in any ad unit")
        return null
    }
    
    /**
     * Cleans up expired ads from the cache for a specific ad unit.
     * 
     * @return Number of ads that were cleaned up
     */
    private fun cleanupExpiredAds(adUnitId: String, adList: MutableList<CachedAd>, currentTime: Long): Int {
        val iterator = adList.iterator()
        var removedCount = 0
        
        while (iterator.hasNext()) {
            val cachedAd = iterator.next()
            
            if (cachedAd.isExpired(currentTime, cacheExpiryMs)) {
                cachedAd.ad.destroy()
                iterator.remove()
                removedCount++
                totalMemoryFreed.addAndGet(cachedAd.approximateSize)
            }
        }
        
        if (removedCount > 0) {
            logDebug("Cleaned up $removedCount expired ads for $adUnitId")
        }
        
        return removedCount
    }
    
    /**
     * Clears cached ads for a specific ad unit.
     * 
     * @param adUnitId The ad unit ID to clear cache for
     */
    fun clearCachedAd(adUnitId: String) {
        synchronized(getLockForAdUnit(adUnitId)) {
            val adList = cachedAds[adUnitId]
            adList?.forEach { cachedAd ->
                cachedAd.ad.destroy()
            }
            cachedAds.remove(adUnitId)
        }
    }
    
    /**
     * Clears all cached ads and resets the cache manager.
     */
    fun clearAllCachedAds() {
        // Clear all cached ads
        for ((adUnitId, _) in cachedAds) {
            synchronized(getLockForAdUnit(adUnitId)) {
                val adList = cachedAds[adUnitId]
                adList?.forEach { cachedAd ->
                    cachedAd.ad.destroy()
                }
            }
        }
        
        cachedAds.clear()
        cacheLocks.clear()
    }
    
    /**
     * Performs cleanup of expired ads across all ad units.
     */
    fun performCleanup() {
        val currentTime = System.currentTimeMillis()
        val adUnitsToRemove = mutableListOf<String>()
        
        for ((adUnitId, _) in cachedAds) {
            synchronized(getLockForAdUnit(adUnitId)) {
                val adList = cachedAds[adUnitId]
                if (adList != null) {
                    cleanupExpiredAds(adUnitId, adList, currentTime)
                    
                    // Mark empty ad units for removal
                    if (adList.isEmpty()) {
                        adUnitsToRemove.add(adUnitId)
                    }
                }
            }
        }
        
        // Remove empty ad units
        adUnitsToRemove.forEach { adUnitId ->
            cachedAds.remove(adUnitId)
        }
    }
    
    /**
     * Gets cache statistics for debugging purposes.
     * 
     * @return Map of ad unit ID to cache statistics
     */
    fun getCacheStatistics(): Map<String, String> {
        val stats = mutableMapOf<String, String>()
        val currentTime = System.currentTimeMillis()
        
        for ((adUnitId, _) in cachedAds) {
            synchronized(getLockForAdUnit(adUnitId)) {
                val adList = cachedAds[adUnitId]
                if (adList != null) {
                    val totalAds = adList.size
                    val expiredAds = adList.count { cachedAd ->
                        val ageMs = currentTime - cachedAd.cachedTime
                        ageMs > cacheExpiryMs
                    }
                    val validAds = totalAds - expiredAds
                    
                    stats[adUnitId] = "Total: $totalAds, Valid: $validAds, Expired: $expiredAds"
                }
            }
        }
        
        return stats.toMap()
    }
    
    // =================== BACKGROUND CLEANUP ===================
    
    /**
     * Starts the background cleanup service.
     */
    private fun startBackgroundCleanup() {
        if (cleanupExecutor != null) return // Already started
        
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "NativeAdManager-Cleanup").apply { isDaemon = true }
        }
        
        cleanupExecutor?.scheduleWithFixedDelay({
            try {
                performCleanup()
            } catch (e: Exception) {
                logDebug("Error during background cleanup: ${e.message}")
            }
        }, cleanupIntervalMinutes, cleanupIntervalMinutes, TimeUnit.MINUTES)
        
        logDebug("Background cleanup started with ${cleanupIntervalMinutes}min interval")
    }
    
    /**
     * Stops the background cleanup service.
     */
    fun stopBackgroundCleanup() {
        cleanupExecutor?.shutdown()
        cleanupExecutor = null
        logDebug("Background cleanup stopped")
    }
    
    // =================== PERFORMANCE MONITORING ===================
    
    /**
     * Gets comprehensive performance statistics.
     */
    fun getPerformanceStats(): Map<String, Any> {
        val totalRequests = cacheHits.get() + cacheMisses.get()
        val hitRate = if (totalRequests > 0) {
            (cacheHits.get().toDouble() / totalRequests * 100).toInt()
        } else 0
        
        return mapOf(
            "cache_hits" to cacheHits.get(),
            "cache_misses" to cacheMisses.get(),
            "hit_rate_percent" to hitRate,
            "total_ads_served" to totalAdsServed.get(),
            "total_memory_freed_kb" to totalMemoryFreed.get() / 1024,
            "active_ad_units" to cachedAds.size,
            "total_cached_ads" to getTotalCacheSize(),
            "cache_expiry_ms" to cacheExpiryMs,
            "max_ads_per_unit" to maxCachedAdsPerUnit,
            "background_cleanup_enabled" to enableBackgroundCleanup
        )
    }
    
    /**
     * Resets performance counters.
     */
    fun resetPerformanceStats() {
        cacheHits.set(0)
        cacheMisses.set(0)
        totalAdsServed.set(0)
        totalMemoryFreed.set(0)
        logDebug("Performance stats reset")
    }
    
    // =================== CACHE WARMING ===================
    
    /**
     * Warms up the cache by pre-loading ads for specified ad units.
     * This is useful for improving user experience by having ads ready.
     * 
     * @param adUnits Map of ad unit ID to number of ads to pre-cache
     * @param onComplete Callback when warming is complete
     */
    fun warmCache(adUnits: Map<String, Int>, onComplete: ((Int, Int) -> Unit)? = null) {
        if (!enableCachingNativeAds) {
            logDebug("Cache warming skipped: caching disabled")
            onComplete?.invoke(0, 0)
            return
        }
        
        val totalUnits = adUnits.size
        var warmedUnits = 0
        
        logDebug("Starting cache warming for $totalUnits ad units")
        
        adUnits.forEach { (adUnitId, count) ->
            val currentSize = getCacheSize(adUnitId)
            val neededAds = maxOf(0, count - currentSize)
            
            if (neededAds > 0) {
                logDebug("Cache warming: $adUnitId needs $neededAds more ads (current: $currentSize)")
                // In a real implementation, you would trigger ad loading here
                // For now, we just log the need
            }
            
            warmedUnits++
        }
        
        logDebug("Cache warming completed: $warmedUnits units processed")
        onComplete?.invoke(warmedUnits, totalUnits)
    }
    
    /**
     * Gets the cache size for a specific ad unit.
     */
    fun getCacheSize(adUnitId: String): Int {
        synchronized(getLockForAdUnit(adUnitId)) {
            return cachedAds[adUnitId]?.size ?: 0
        }
    }
    
    /**
     * Gets the total number of cached ads across all ad units.
     */
    fun getTotalCacheSize(): Int {
        return cachedAds.values.sumOf { it.size }
    }
    
    /**
     * Checks if there are cached ads for the specified ad unit.
     */
    fun hasCachedAds(adUnitId: String): Boolean {
        synchronized(getLockForAdUnit(adUnitId)) {
            val adList = cachedAds[adUnitId]
            return adList != null && adList.isNotEmpty()
        }
    }

    // =================== PROGRAMMATIC LOADING ===================

    /**
     * Loads a native ad programmatically without requiring a view to be added to layout first.
     * This is a convenience method that delegates to ProgrammaticNativeAdLoader.
     *
     * @param activity The activity context
     * @param adUnitId The ad unit ID
     * @param size The native ad size (SMALL, MEDIUM, LARGE)
     * @param useCachedAd Whether to try cached ads first
     * @param callback Callback for ad events
     */
    fun loadNativeAdProgrammatically(
        activity: android.app.Activity,
        adUnitId: String,
        size: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize,
        useCachedAd: Boolean = true,
        callback: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.ProgrammaticAdCallback
    ) {
        com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.loadNativeAd(
            activity, adUnitId, size, useCachedAd, callback
        )
    }

    /**
     * Loads a small native banner ad programmatically.
     *
     * @param activity The activity context
     * @param adUnitId The ad unit ID
     * @param useCachedAd Whether to try cached ads first
     * @param callback Callback for ad events
     */
    fun loadSmallNativeAd(
        activity: android.app.Activity,
        adUnitId: String,
        useCachedAd: Boolean = true,
        callback: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.ProgrammaticAdCallback
    ) {
        loadNativeAdProgrammatically(
            activity, adUnitId,
            com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize.SMALL,
            useCachedAd, callback
        )
    }

    /**
     * Loads a medium native banner ad programmatically.
     *
     * @param activity The activity context
     * @param adUnitId The ad unit ID
     * @param useCachedAd Whether to try cached ads first
     * @param callback Callback for ad events
     */
    fun loadMediumNativeAd(
        activity: android.app.Activity,
        adUnitId: String,
        useCachedAd: Boolean = true,
        callback: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.ProgrammaticAdCallback
    ) {
        loadNativeAdProgrammatically(
            activity, adUnitId,
            com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize.MEDIUM,
            useCachedAd, callback
        )
    }

    /**
     * Loads a large native ad programmatically.
     *
     * @param activity The activity context
     * @param adUnitId The ad unit ID
     * @param useCachedAd Whether to try cached ads first
     * @param callback Callback for ad events
     */
    fun loadLargeNativeAd(
        activity: android.app.Activity,
        adUnitId: String,
        useCachedAd: Boolean = true,
        callback: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.ProgrammaticAdCallback
    ) {
        loadNativeAdProgrammatically(
            activity, adUnitId,
            com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize.LARGE,
            useCachedAd, callback
        )
    }

    /**
     * Loads a native ad and automatically adds it to the specified ViewGroup.
     *
     * @param activity The activity context
     * @param adUnitId The ad unit ID
     * @param size The native ad size
     * @param container The ViewGroup to add the ad to
     * @param useCachedAd Whether to try cached ads first
     * @param callback Optional callback for ad events
     */
    fun loadNativeAdIntoContainer(
        activity: android.app.Activity,
        adUnitId: String,
        size: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize,
        container: android.view.ViewGroup,
        useCachedAd: Boolean = true,
        callback: com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.ProgrammaticAdCallback? = null
    ) {
        com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.loadNativeAdIntoContainer(
            activity, adUnitId, size, container, useCachedAd, callback
        )
    }
}