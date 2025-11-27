package com.i2hammad.admanagekit.config

/**
 * Defines the placement/direction for collapsible banner ads.
 *
 * Collapsible banners can collapse from either the top or bottom edge of the ad.
 * This allows developers to choose the most appropriate placement based on their UI layout.
 *
 * Example usage:
 * ```kotlin
 * bannerView.loadCollapsibleBanner(
 *     activity = this,
 *     adUnitId = "ca-app-pub-xxx",
 *     collapsible = true,
 *     placement = CollapsibleBannerPlacement.TOP
 * )
 * ```
 *
 * @since 2.2.0
 */
enum class CollapsibleBannerPlacement(val value: String) {
    /**
     * Banner collapses from the top edge.
     * Best for: Banners placed at the top of the screen.
     */
    TOP("top"),

    /**
     * Banner collapses from the bottom edge (default).
     * Best for: Banners placed at the bottom of the screen.
     */
    BOTTOM("bottom")
}
