package com.i2hammad.admanagekit.core.ad

/**
 * Network-agnostic ad revenue value.
 * Replaces AdMob's AdValue in provider APIs.
 *
 * @param valueMicros Revenue value in micro-units (1/1,000,000 of currency unit)
 * @param currencyCode ISO 4217 currency code (e.g., "USD", "RUB")
 * @param precisionType Revenue precision level
 */
data class AdKitAdValue(
    val valueMicros: Long,
    val currencyCode: String,
    val precisionType: PrecisionType = PrecisionType.UNKNOWN
) {
    enum class PrecisionType {
        UNKNOWN,
        ESTIMATED,
        PUBLISHER_PROVIDED,
        PRECISE
    }
}
