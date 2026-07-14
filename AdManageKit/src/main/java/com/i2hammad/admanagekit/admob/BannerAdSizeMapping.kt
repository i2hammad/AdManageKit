package com.i2hammad.admanagekit.admob

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.i2hammad.admanagekit.config.BannerAdSize

/**
 * Maps a fixed [BannerAdSize] to the Next-Gen SDK [AdSize] constant, or null for
 * [BannerAdSize.ADAPTIVE] (adaptive sizes depend on the available width, so the
 * caller computes them at load time).
 */
internal fun BannerAdSize.toFixedAdMobAdSize(): AdSize? = when (this) {
    BannerAdSize.ADAPTIVE -> null
    BannerAdSize.BANNER -> AdSize.BANNER
    BannerAdSize.LARGE_BANNER -> AdSize.LARGE_BANNER
    BannerAdSize.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
    BannerAdSize.FULL_BANNER -> AdSize.FULL_BANNER
    BannerAdSize.LEADERBOARD -> AdSize.LEADERBOARD
}
