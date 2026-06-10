package com.i2hammad.admanagekit.yandex.internal

import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestError
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider

/** Map Yandex [AdRequestError] to [AdKitAdError], translating Yandex error codes. */
internal fun AdRequestError.toAdKitError(): AdKitAdError = AdKitAdError(
    code = when (code) {
        AdRequestError.Code.INVALID_REQUEST -> AdKitAdError.ERROR_CODE_INVALID_REQUEST
        AdRequestError.Code.NETWORK_ERROR -> AdKitAdError.ERROR_CODE_NETWORK
        AdRequestError.Code.NO_FILL -> AdKitAdError.ERROR_CODE_NO_FILL
        // INTERNAL_ERROR, SYSTEM_ERROR, UNKNOWN_ERROR and anything else map to internal
        else -> AdKitAdError.ERROR_CODE_INTERNAL
    },
    message = description,
    domain = AdProvider.YANDEX.name
)

/** Map Yandex [AdError] to [AdKitAdError]. */
internal fun AdError.toAdKitError(): AdKitAdError = AdKitAdError(
    code = AdKitAdError.ERROR_CODE_INTERNAL,
    message = description,
    domain = AdProvider.YANDEX.name
)
