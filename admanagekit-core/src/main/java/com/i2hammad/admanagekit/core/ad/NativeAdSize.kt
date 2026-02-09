package com.i2hammad.admanagekit.core.ad

/**
 * Size hint for native ad providers, allowing them to return
 * appropriately sized views for different placements.
 */
enum class NativeAdSize {
    /** Compact: icon + title + CTA. No body text or media. */
    SMALL,
    /** Medium: icon + title + body + CTA. No media. */
    MEDIUM,
    /** Full: icon + title + body + media + CTA. */
    LARGE
}
