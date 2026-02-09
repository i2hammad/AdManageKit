package com.i2hammad.admanagekit.core.ad

/**
 * Network-agnostic ad error representation.
 * Replaces AdMob's AdError / Yandex's AdRequestError in provider APIs.
 *
 * @param code Numeric error code (network-specific codes are mapped here)
 * @param message Human-readable error description
 * @param domain Error domain identifying the source (e.g., "admob", "yandex")
 */
data class AdKitAdError(
    val code: Int,
    val message: String,
    val domain: String
) {
    companion object {
        const val ERROR_CODE_INTERNAL = 0
        const val ERROR_CODE_INVALID_REQUEST = 1
        const val ERROR_CODE_NETWORK = 2
        const val ERROR_CODE_NO_FILL = 3
        const val ERROR_CODE_TIMEOUT = -1
        const val ERROR_CODE_PURCHASED = 1001
    }
}
