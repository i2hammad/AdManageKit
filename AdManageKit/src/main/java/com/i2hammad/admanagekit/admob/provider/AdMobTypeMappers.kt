package com.i2hammad.admanagekit.admob.provider

import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider

/**
 * Map Next-Gen GMA [LoadAdError] to [AdKitAdError].
 *
 * [LoadAdError.code] is now a closed [LoadAdError.ErrorCode] enum rather than an Int, but each
 * enum constant carries a [LoadAdError.ErrorCode.value] Int that mirrors the legacy AdMob
 * numeric error codes (e.g. INTERNAL_ERROR=0, INVALID_REQUEST=1, NETWORK_ERROR=2, NO_FILL=3),
 * so it is used directly instead of hashing the ordinal or falling back to a fixed sentinel.
 */
internal fun LoadAdError.toAdKitError(): AdKitAdError = AdKitAdError(
    code = code.value,
    message = message,
    domain = AdProvider.ADMOB.name
)

/**
 * Map Next-Gen GMA [FullScreenContentError] (show-failure) to [AdKitAdError].
 *
 * [FullScreenContentError.code] is a closed [FullScreenContentError.ErrorCode] enum with no
 * numeric value accessor (unlike [LoadAdError.ErrorCode]), so there is no faithful numeric code
 * to carry over - [AdKitAdError.ERROR_CODE_INTERNAL] is used as a fixed sentinel and the real
 * detail lives in [message].
 */
internal fun FullScreenContentError.toAdKitError(): AdKitAdError = AdKitAdError(
    code = AdKitAdError.ERROR_CODE_INTERNAL,
    message = message,
    domain = AdProvider.ADMOB.name
)

/** Map AdMob [AdValue] to [AdKitAdValue]. */
internal fun AdValue.toAdKitValue(): AdKitAdValue = AdKitAdValue(
    valueMicros = valueMicros,
    currencyCode = currencyCode,
    precisionType = when (precisionType) {
        PrecisionType.ESTIMATED -> AdKitAdValue.PrecisionType.ESTIMATED
        PrecisionType.PUBLISHER_PROVIDED -> AdKitAdValue.PrecisionType.PUBLISHER_PROVIDED
        PrecisionType.PRECISE -> AdKitAdValue.PrecisionType.PRECISE
        else -> AdKitAdValue.PrecisionType.UNKNOWN
    }
)
