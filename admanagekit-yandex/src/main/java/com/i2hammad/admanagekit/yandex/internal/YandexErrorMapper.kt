package com.i2hammad.admanagekit.yandex.internal

import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestError
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider

/** Map Yandex [AdRequestError] to [AdKitAdError]. */
internal fun AdRequestError.toAdKitError(): AdKitAdError = AdKitAdError(
    code = code,
    message = description,
    domain = AdProvider.YANDEX.name
)

/** Map Yandex [AdError] to [AdKitAdError]. */
internal fun AdError.toAdKitError(): AdKitAdError = AdKitAdError(
    code = 0,
    message = description,
    domain = AdProvider.YANDEX.name
)
