package com.i2hammad.admanagekit.yandex.internal

import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.yandex.mobile.ads.common.ImpressionData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [toAdKitValue]: the native revenue+currency pair must be preferred,
 * revenueUSD (labeled "USD") is the fallback, and malformed/missing data must
 * produce the defensive zero-USD default.
 *
 * Runs under Robolectric because the mapper parses [ImpressionData.getRawData]
 * with org.json.JSONObject, which is a non-functional stub in plain unit tests.
 * [ImpressionData] itself is a plain interface, so no mocks are needed.
 */
@RunWith(RobolectricTestRunner::class)
class YandexValueMapperTest {

    private fun impressionData(raw: String): ImpressionData = object : ImpressionData {
        override val rawData: String = raw
    }

    // ==================== native revenue + currency pair ====================

    @Test
    fun `native revenue and currency pair is preferred`() {
        val value = impressionData(
            """{"revenue": 1.5, "currency": "RUB", "revenueUSD": 0.015}"""
        ).toAdKitValue()

        assertEquals(1_500_000L, value.valueMicros)
        assertEquals("RUB", value.currencyCode)
        assertEquals(AdKitAdValue.PrecisionType.ESTIMATED, value.precisionType)
    }

    @Test
    fun `native revenue without currency defaults the currency to USD`() {
        val value = impressionData("""{"revenue": 0.42}""").toAdKitValue()

        assertEquals(420_000L, value.valueMicros)
        assertEquals("USD", value.currencyCode)
        assertEquals(AdKitAdValue.PrecisionType.ESTIMATED, value.precisionType)
    }

    // ==================== revenueUSD fallback ====================

    @Test
    fun `revenueUSD fallback is used and labeled USD when native pair is absent`() {
        val value = impressionData(
            """{"revenueUSD": 0.25, "currency": "RUB"}"""
        ).toAdKitValue()

        assertEquals(250_000L, value.valueMicros)
        assertEquals("USD", value.currencyCode)
        assertEquals(AdKitAdValue.PrecisionType.ESTIMATED, value.precisionType)
    }

    @Test
    fun `neither revenue field present yields zero USD with ESTIMATED precision`() {
        val value = impressionData("""{"adType": "banner"}""").toAdKitValue()

        assertEquals(0L, value.valueMicros)
        assertEquals("USD", value.currencyCode)
        assertEquals(AdKitAdValue.PrecisionType.ESTIMATED, value.precisionType)
    }

    // ==================== defensive defaults ====================

    @Test
    fun `malformed JSON yields zero USD with UNKNOWN precision`() {
        val value = impressionData("not valid json {{").toAdKitValue()

        assertEquals(0L, value.valueMicros)
        assertEquals("USD", value.currencyCode)
        assertEquals(AdKitAdValue.PrecisionType.UNKNOWN, value.precisionType)
    }

    @Test
    fun `null ImpressionData yields zero USD with UNKNOWN precision`() {
        val value = (null as ImpressionData?).toAdKitValue()

        assertEquals(0L, value.valueMicros)
        assertEquals("USD", value.currencyCode)
        assertEquals(AdKitAdValue.PrecisionType.UNKNOWN, value.precisionType)
    }
}
