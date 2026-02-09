package com.i2hammad.admanagekit.core.ad

/**
 * Identifies an ad network provider.
 *
 * @param name Internal identifier used in code and mappings (e.g., "admob", "yandex")
 * @param displayName Human-readable name for logging/debugging (e.g., "Google AdMob", "Yandex Ads")
 */
data class AdProvider(
    val name: String,
    val displayName: String
) {
    companion object {
        val ADMOB = AdProvider("admob", "Google AdMob")
        val YANDEX = AdProvider("yandex", "Yandex Ads")
    }
}
