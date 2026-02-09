package com.i2hammad.admanagekit.admob.provider

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.LoadAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider

/** Map AdMob [LoadAdError] to [AdKitAdError]. */
internal fun LoadAdError.toAdKitError(): AdKitAdError = AdKitAdError(
    code = code,
    message = message,
    domain = AdProvider.ADMOB.name
)

/** Map AdMob [AdError] to [AdKitAdError]. */
internal fun AdError.toAdKitError(): AdKitAdError = AdKitAdError(
    code = code,
    message = message,
    domain = AdProvider.ADMOB.name
)

/** Map AdMob [AdValue] to [AdKitAdValue]. */
internal fun AdValue.toAdKitValue(): AdKitAdValue = AdKitAdValue(
    valueMicros = valueMicros,
    currencyCode = currencyCode,
    precisionType = when (precisionType) {
        AdValue.PrecisionType.ESTIMATED -> AdKitAdValue.PrecisionType.ESTIMATED
        AdValue.PrecisionType.PUBLISHER_PROVIDED -> AdKitAdValue.PrecisionType.PUBLISHER_PROVIDED
        AdValue.PrecisionType.PRECISE -> AdKitAdValue.PrecisionType.PRECISE
        else -> AdKitAdValue.PrecisionType.UNKNOWN
    }
)
