package com.i2hammad.admanagekit.config

/**
 * Defines how ads should be loaded and displayed in the app.
 *
 * ## Strategies:
 *
 * ### OnDemand
 * Always fetch and display the ad at the moment it's needed.
 * - Shows loading dialog while fetching
 * - Waits for ad to load within timeout period
 * - Best for: Critical moments where you always want to try showing an ad
 *
 * Example: User completes a level -> fetch fresh ad -> show if loaded within 5-10s
 *
 * ### OnlyCache
 * Only show ads that are already cached/preloaded.
 * - No loading dialog
 * - Instant show if available, skip if not
 * - Best for: Smooth user experience, frequent ad opportunities
 *
 * Example: User performs task -> check cache -> show immediately if ready, skip if not
 *
 * ### Hybrid
 * Check cache first, fetch if cache is empty.
 * - Try cached ad first for instant display
 * - If no cache, fetch with loading dialog and timeout
 * - Best for: Balance between coverage and user experience
 *
 * Example: User performs task -> check cache -> if available show immediately,
 *          if not fetch new ad with timeout -> show if loaded, skip if timeout
 */
enum class AdLoadingStrategy {
    /**
     * Always fetch fresh ad on-demand with loading dialog.
     * Shows loading UI while waiting for ad to load.
     */
    ON_DEMAND,

    /**
     * Only show cached/preloaded ads.
     * No loading dialog, instant show or skip.
     */
    ONLY_CACHE,

    /**
     * Check cache first, fetch if needed.
     * Instant show if cached, loading dialog if fetching.
     */
    HYBRID
}
