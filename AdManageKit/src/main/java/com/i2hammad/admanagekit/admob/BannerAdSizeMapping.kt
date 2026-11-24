package com.i2hammad.admanagekit.admob

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.i2hammad.admanagekit.config.BannerAdSize

/**
 * Maps a fixed [BannerAdSize] to the Next-Gen SDK [AdSize] constant, or null for
 * the adaptive sizes ([BannerAdSize.ADAPTIVE], [BannerAdSize.ADAPTIVE_LARGE],
 * [BannerAdSize.INLINE_ADAPTIVE]) - they depend on the available width, so the
 * caller computes them at load time.
 */
internal fun BannerAdSize.toFixedAdMobAdSize(): AdSize? = when (this) {
    BannerAdSize.ADAPTIVE -> null
    BannerAdSize.ADAPTIVE_LARGE -> null
    BannerAdSize.INLINE_ADAPTIVE -> null
    BannerAdSize.BANNER -> AdSize.BANNER
    BannerAdSize.LARGE_BANNER -> AdSize.LARGE_BANNER
    BannerAdSize.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
    BannerAdSize.FULL_BANNER -> AdSize.FULL_BANNER
    BannerAdSize.LEADERBOARD -> AdSize.LEADERBOARD
}

/**
 * Whether a banner request should carry the collapsible extra.
 *
 * AdMob serves collapsible banners only for anchored sizes, so an inline adaptive
 * request never asks for one - not even when collapsible banners are enabled by
 * default - and loads as a plain inline banner instead.
 */
internal fun shouldRequestCollapsible(
    collapsible: Boolean,
    collapsibleByDefault: Boolean,
    isInlineAdaptive: Boolean
): Boolean = (collapsible || collapsibleByDefault) && !isInlineAdaptive

/**
 * Clamps an inline adaptive max height to what AdMob accepts (at least
 * [BannerAdView.MIN_INLINE_MAX_HEIGHT_DP]).
 */
internal fun clampInlineMaxHeightDp(maxHeightDp: Int): Int =
    maxHeightDp.coerceAtLeast(BannerAdView.MIN_INLINE_MAX_HEIGHT_DP)
