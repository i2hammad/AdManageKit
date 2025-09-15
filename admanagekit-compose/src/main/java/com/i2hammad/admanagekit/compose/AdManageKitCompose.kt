package com.i2hammad.admanagekit.compose

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.i2hammad.admanagekit.admob.NativeAdManager
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.BillingConfig
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Compose utilities and extensions for AdManageKit.
 *
 * This file provides Compose-specific helpers and effects for managing
 * AdManageKit configuration and state in Compose applications.
 *
 * @since 2.1.0
 */

/**
 * A Compose effect for initializing AdManageKit with Firebase Analytics.
 *
 * This should be called once in your app's main Activity or Application.
 * It initializes the NativeAdManager with Firebase Analytics for performance tracking.
 *
 * @param analytics Optional FirebaseAnalytics instance. If null, will create one from context.
 */
@Composable
fun AdManageKitInitEffect(
    analytics: FirebaseAnalytics? = null
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        @Suppress("MissingPermission")
        val firebaseAnalytics = analytics ?: FirebaseAnalytics.getInstance(context)
        NativeAdManager.initialize(firebaseAnalytics)
    }
}

/**
 * A composable that provides the current purchase status.
 *
 * This can be used to conditionally show or hide ads based on user purchase status.
 *
 * @return true if the user has purchased the app (ads should be disabled), false otherwise
 */
@Composable
fun rememberPurchaseStatus(): Boolean {
    return remember {
        BillingConfig.getPurchaseProvider().isPurchased()
    }
}

/**
 * A composable that provides cache statistics for debugging.
 *
 * @return Map of ad unit ID to cache statistics
 */
@Composable
fun rememberCacheStatistics(): State<Map<String, String>> {
    return produceState(initialValue = emptyMap()) {
        value = NativeAdManager.getCacheStatistics()
    }
}

/**
 * A composable that provides performance statistics for debugging.
 *
 * @return Map of performance metrics
 */
@Composable
fun rememberPerformanceStats(): State<Map<String, Any>> {
    return produceState(initialValue = emptyMap()) {
        value = NativeAdManager.getPerformanceStats()
    }
}

/**
 * A Compose effect for managing native ad cache warming.
 *
 * This effect warms up the cache by pre-loading ads for specified ad units.
 * It's useful for improving user experience by having ads ready.
 *
 * @param adUnits Map of ad unit ID to number of ads to pre-cache
 * @param onComplete Optional callback when warming is complete
 */
@Composable
fun CacheWarmingEffect(
    adUnits: Map<String, Int>,
    onComplete: ((Int, Int) -> Unit)? = null
) {
    LaunchedEffect(adUnits) {
        if (adUnits.isNotEmpty()) {
            NativeAdManager.warmCache(adUnits, onComplete)
        }
    }
}

/**
 * Configuration object for AdManageKit Compose components.
 */
object AdManageKitCompose {

    /**
     * Default configuration for Compose ad components.
     */
    object Defaults {
        const val SHOW_LOADING_INDICATOR = true
        const val USE_CACHED_ADS = true
        const val AUTO_PRELOAD_INTERSTITIAL = true
    }

    /**
     * Helper function to check if caching is enabled.
     */
    val isCachingEnabled: Boolean
        get() = NativeAdManager.enableCachingNativeAds

    /**
     * Helper function to get current cache configuration.
     */
    val cacheConfig: CacheConfig
        get() = CacheConfig(
            expiryMs = NativeAdManager.cacheExpiryMs,
            maxAdsPerUnit = NativeAdManager.maxCachedAdsPerUnit,
            cleanupEnabled = NativeAdManager.enableBackgroundCleanup
        )
}

/**
 * Data class representing cache configuration for display purposes.
 */
data class CacheConfig(
    val expiryMs: Long,
    val maxAdsPerUnit: Int,
    val cleanupEnabled: Boolean
)

/**
 * A Compose modifier extension for conditional ad display based on purchase status.
 *
 * This can be used to conditionally apply modifiers based on whether ads should be shown.
 */
fun androidx.compose.ui.Modifier.conditionalAd(
    showAd: Boolean,
    adModifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
): androidx.compose.ui.Modifier {
    return if (showAd) {
        this.then(adModifier)
    } else {
        this
    }
}

/**
 * Composable function to conditionally render ads based on purchase status.
 *
 * @param showWhenPurchased Whether to show the content when user has purchased (default: false)
 * @param content The ad content to display
 */
@Composable
fun ConditionalAd(
    showWhenPurchased: Boolean = false,
    content: @Composable () -> Unit
) {
    val isPurchased = rememberPurchaseStatus()

    if (!isPurchased || showWhenPurchased) {
        content()
    }
}

/**
 * Composable function that provides ad loading state management.
 *
 * @param initialLoading Initial loading state
 * @return Pair of loading state and error state management
 */
@Composable
fun rememberAdLoadingState(
    initialLoading: Boolean = false
): Pair<Pair<Boolean, (Boolean) -> Unit>, Pair<Boolean, (Boolean) -> Unit>> {
    var isLoading by remember { mutableStateOf(initialLoading) }
    var hasError by remember { mutableStateOf(false) }

    return Pair(
        Pair(isLoading) { isLoading = it },
        Pair(hasError) { hasError = it }
    )
}