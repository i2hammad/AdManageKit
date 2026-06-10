package com.i2hammad.admanagekit.yandex.internal

import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestError
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [toAdKitError]: every Yandex [AdRequestError.Code] constant must map
 * to the correct [AdKitAdError] code, and message/domain must carry over.
 *
 * Pure JVM test: [AdRequestError] is directly constructible (code, description)
 * and [AdError] is a plain interface, so neither Robolectric nor mocks are needed.
 */
class YandexErrorMapperTest {

    private fun requestError(code: Int, description: String = "desc") =
        AdRequestError(code, description)

    // ==================== AdRequestError code mapping ====================

    @Test
    fun `UNKNOWN_ERROR maps to ERROR_CODE_INTERNAL`() {
        assertEquals(
            AdKitAdError.ERROR_CODE_INTERNAL,
            requestError(AdRequestError.Code.UNKNOWN_ERROR).toAdKitError().code
        )
    }

    @Test
    fun `INTERNAL_ERROR maps to ERROR_CODE_INTERNAL`() {
        assertEquals(
            AdKitAdError.ERROR_CODE_INTERNAL,
            requestError(AdRequestError.Code.INTERNAL_ERROR).toAdKitError().code
        )
    }

    @Test
    fun `INVALID_REQUEST maps to ERROR_CODE_INVALID_REQUEST`() {
        assertEquals(
            AdKitAdError.ERROR_CODE_INVALID_REQUEST,
            requestError(AdRequestError.Code.INVALID_REQUEST).toAdKitError().code
        )
    }

    @Test
    fun `NETWORK_ERROR maps to ERROR_CODE_NETWORK`() {
        assertEquals(
            AdKitAdError.ERROR_CODE_NETWORK,
            requestError(AdRequestError.Code.NETWORK_ERROR).toAdKitError().code
        )
    }

    @Test
    fun `NO_FILL maps to ERROR_CODE_NO_FILL`() {
        assertEquals(
            AdKitAdError.ERROR_CODE_NO_FILL,
            requestError(AdRequestError.Code.NO_FILL).toAdKitError().code
        )
    }

    @Test
    fun `SYSTEM_ERROR maps to ERROR_CODE_INTERNAL`() {
        assertEquals(
            AdKitAdError.ERROR_CODE_INTERNAL,
            requestError(AdRequestError.Code.SYSTEM_ERROR).toAdKitError().code
        )
    }

    @Test
    fun `unrecognized Yandex code maps to ERROR_CODE_INTERNAL`() {
        assertEquals(
            AdKitAdError.ERROR_CODE_INTERNAL,
            requestError(99).toAdKitError().code
        )
    }

    // ==================== message / domain carry-over ====================

    @Test
    fun `AdRequestError description and Yandex domain carry over`() {
        val mapped = requestError(AdRequestError.Code.NO_FILL, "yandex no fill").toAdKitError()

        assertEquals("yandex no fill", mapped.message)
        assertEquals(AdProvider.YANDEX.name, mapped.domain)
    }

    // ==================== AdError (show-time error) mapping ====================

    @Test
    fun `AdError maps to ERROR_CODE_INTERNAL with description and Yandex domain`() {
        val adError = object : AdError {
            override val description: String = "failed to show"
        }

        val mapped = adError.toAdKitError()

        assertEquals(AdKitAdError.ERROR_CODE_INTERNAL, mapped.code)
        assertEquals("failed to show", mapped.message)
        assertEquals(AdProvider.YANDEX.name, mapped.domain)
    }
}
