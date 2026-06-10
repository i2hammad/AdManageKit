package com.i2hammad.admanagekit.yandex.internal

import com.yandex.mobile.ads.common.ImpressionData
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import org.json.JSONObject

/**
 * Extracts revenue data from Yandex [ImpressionData].
 * Yandex provides impression data as a JSON string containing revenue info.
 */
internal fun ImpressionData?.toAdKitValue(): AdKitAdValue {
    if (this == null) {
        return AdKitAdValue(
            valueMicros = 0L,
            currencyCode = "USD",
            precisionType = AdKitAdValue.PrecisionType.UNKNOWN
        )
    }

    return try {
        val json = JSONObject(rawData)
        // Prefer the native-currency pair (revenue + currency); fall back to the
        // USD-converted value, which must be labeled as USD.
        val nativeRevenue = json.optDouble("revenue", Double.NaN)
        val revenue: Double
        val currency: String
        if (!nativeRevenue.isNaN()) {
            revenue = nativeRevenue
            currency = json.optString("currency", "USD")
        } else {
            revenue = json.optDouble("revenueUSD", 0.0)
            currency = "USD"
        }
        val valueMicros = (revenue * 1_000_000).toLong()

        AdKitAdValue(
            valueMicros = valueMicros,
            currencyCode = currency,
            precisionType = AdKitAdValue.PrecisionType.ESTIMATED
        )
    } catch (e: Exception) {
        AdKitAdValue(
            valueMicros = 0L,
            currencyCode = "USD",
            precisionType = AdKitAdValue.PrecisionType.UNKNOWN
        )
    }
}
