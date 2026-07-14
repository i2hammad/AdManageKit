package com.i2hammad.admanagekit.config

/**
 * SDK-agnostic banner ad size selection for [com.i2hammad.admanagekit.admob.BannerAdView].
 *
 * Mirrors the standard AdMob banner sizes plus the recommended adaptive banner:
 *
 * | Size (dp)  | Description          | Availability       |
 * |------------|----------------------|--------------------|
 * | full width | Anchored adaptive    | Phones and tablets |
 * | 320x50     | Banner               | Phones and tablets |
 * | 320x100    | Large banner         | Phones and tablets |
 * | 300x250    | IAB medium rectangle | Phones and tablets |
 * | 468x60     | IAB full-size banner | Tablets            |
 * | 728x90     | IAB leaderboard      | Tablets            |
 *
 * Example usage:
 * ```kotlin
 * bannerView.loadBanner(activity, "ca-app-pub-xxx", BannerAdSize.MEDIUM_RECTANGLE)
 * ```
 *
 * @property widthDp Fixed width in dp, or null for adaptive sizes (computed at load time).
 * @property heightDp Fixed height in dp, or null for adaptive sizes (computed at load time).
 * @since 4.3.0
 */
enum class BannerAdSize(val widthDp: Int?, val heightDp: Int?) {
    /**
     * Anchored adaptive banner sized to the available width (default).
     * Google-recommended replacement for fixed 320x50 banners; height varies
     * per device (typically 50-90dp). Required for collapsible banners.
     */
    ADAPTIVE(null, null),

    /** Standard banner, 320x50. Phones and tablets. */
    BANNER(320, 50),

    /** Large banner, 320x100. Phones and tablets. */
    LARGE_BANNER(320, 100),

    /** IAB medium rectangle, 300x250. Phones and tablets. */
    MEDIUM_RECTANGLE(300, 250),

    /** IAB full-size banner, 468x60. Tablets. */
    FULL_BANNER(468, 60),

    /** IAB leaderboard, 728x90. Tablets. */
    LEADERBOARD(728, 90);

    /** True for sizes whose dimensions are computed at load time. */
    val isAdaptive: Boolean get() = widthDp == null
}
