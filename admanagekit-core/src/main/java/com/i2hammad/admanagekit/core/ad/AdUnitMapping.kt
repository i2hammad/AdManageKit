package com.i2hammad.admanagekit.core.ad

import java.util.concurrent.ConcurrentHashMap

/**
 * Maps logical ad placement names to provider-specific ad unit IDs.
 *
 * Thread-safe: registration commonly happens during background init while
 * resolvers are invoked from SDK callbacks.
 *
 * Example:
 * ```kotlin
 * AdUnitMapping.register("interstitial_main", mapOf(
 *     "admob" to "ca-app-pub-xxx/yyy",
 *     "yandex" to "R-M-12345-67"
 * ))
 *
 * val adUnitId = AdUnitMapping.getAdUnitId("interstitial_main", AdProvider.ADMOB)
 * // Returns "ca-app-pub-xxx/yyy"
 * ```
 */
object AdUnitMapping {

    // logicalName -> (providerName -> adUnitId)
    private val mappings = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

    /**
     * Register ad unit IDs for a logical placement.
     *
     * @param logicalName Logical name for the ad placement (e.g., "interstitial_main")
     * @param providerAdUnits Map of provider name to ad unit ID
     */
    @JvmStatic
    fun register(logicalName: String, providerAdUnits: Map<String, String>) {
        // ConcurrentMap.getOrPut uses putIfAbsent, so concurrent registration is safe.
        mappings.getOrPut(logicalName) { ConcurrentHashMap() }.putAll(providerAdUnits)
    }

    /**
     * Get the ad unit ID for a specific provider and logical placement.
     *
     * @param logicalName Logical name for the ad placement
     * @param provider The ad provider to look up
     * @return The ad unit ID, or null if not registered
     */
    @JvmStatic
    fun getAdUnitId(logicalName: String, provider: AdProvider): String? {
        return mappings[logicalName]?.get(provider.name)
    }

    /**
     * Get the ad unit ID by provider name.
     *
     * @param logicalName Logical name for the ad placement
     * @param providerName Provider name string (e.g., "admob", "yandex")
     * @return The ad unit ID, or null if not registered
     */
    @JvmStatic
    fun getAdUnitId(logicalName: String, providerName: String): String? {
        return mappings[logicalName]?.get(providerName)
    }

    /** Remove all registered mappings. */
    @JvmStatic
    fun clear() {
        mappings.clear()
    }

    /** Remove mappings for a specific logical name. */
    @JvmStatic
    fun remove(logicalName: String) {
        mappings.remove(logicalName)
    }

    /** @return all registered logical names */
    @JvmStatic
    fun getRegisteredPlacements(): Set<String> = mappings.keys.toSet()
}
