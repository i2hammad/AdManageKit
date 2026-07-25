package com.i2hammad.admanagekit.config

/**
 * Preferred media (image/video) aspect ratio for native ad requests.
 *
 * This is a *hint* passed to the ad network, not a filter: the network still decides whether
 * the returned ad carries an image or a video and what shape it is. Matching the hint to the
 * shape of a template's [com.google.android.libraries.ads.mobile.sdk.nativead.MediaView] slot
 * maximises the chance the served media fits the slot without heavy cropping.
 *
 * Maps to the Next-Gen SDK `NativeAd.NativeMediaAspectRatio` enum
 * (see `NativeAdRequest.Builder.applyMediaConfig`).
 *
 * @since 4.3.3
 */
enum class NativeMediaAspect {
    /** Do not send any media-aspect hint. Used for media-less templates (icon + text + CTA). */
    UNSPECIFIED,

    /** Let the network choose any aspect ratio. */
    ANY,

    /** Prefer landscape (wide) media — the common shape for card/hero templates. */
    LANDSCAPE,

    /** Prefer portrait (tall) media — story / full-screen / vertical-video templates. */
    PORTRAIT,

    /** Prefer square media — grid cards and square-video templates. */
    SQUARE
}
