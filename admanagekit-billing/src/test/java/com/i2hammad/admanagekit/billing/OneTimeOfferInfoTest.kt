package com.i2hammad.admanagekit.billing

import com.android.billingclient.api.ProductDetailsFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [OneTimeOfferInfo] — the Play Billing 9 one-time product offer
 * features: discounts, rentals, limited quantity and validity windows.
 */
class OneTimeOfferInfoTest {

    private fun offerInfo(
        offer: ProductDetailsFactory.OneTimeOffer,
        productId: String = "remove_ads",
    ): OneTimeOfferInfo {
        val details = ProductDetailsFactory.oneTime(productId, listOf(offer))
        val offers = details.oneTimePurchaseOfferDetailsList
        assertNotNull("fixture produced no one-time offers", offers)
        return OneTimeOfferInfo.from(productId, offers!!.first())
    }

    private fun plainOffer(
        priceMicros: Long = 4_990_000L,
        formatted: String = "$4.99",
    ) = ProductDetailsFactory.OneTimeOffer(
        offerToken = "token_full",
        priceMicros = priceMicros,
        formattedPrice = formatted,
    )

    // ==================== Fixture sanity ====================

    @Test
    fun `fixture builds a parseable one-time ProductDetails`() {
        val details = ProductDetailsFactory.oneTime("remove_ads", listOf(plainOffer()))
        assertEquals("remove_ads", details.productId)
        assertEquals("inapp", details.productType)
        assertEquals(1, details.oneTimePurchaseOfferDetailsList?.size)
        assertNotNull(details.oneTimePurchaseOfferDetails)
    }

    // ==================== Basic pricing ====================

    @Test
    fun `maps price, currency and token`() {
        val info = offerInfo(plainOffer())
        assertEquals("remove_ads", info.productId)
        assertEquals("$4.99", info.formattedPrice)
        assertEquals(4_990_000L, info.priceMicros)
        assertEquals("USD", info.currencyCode)
        assertEquals("token_full", info.offerToken)
    }

    @Test
    fun `a plain offer is not discounted, rented, preordered or limited`() {
        val info = offerInfo(plainOffer())
        assertFalse(info.isDiscounted)
        assertFalse(info.isRental)
        assertFalse(info.isPreorder)
        assertFalse(info.isLimitedQuantity)
        assertFalse(info.isSoldOut)
        assertEquals(0, info.effectiveDiscountPercent)
        assertNull(info.rentalPeriod)
        assertNull(info.discountPercentage)
    }

    // ==================== Discounts ====================

    @Test
    fun `reports a percentage discount from Play`() {
        val info = offerInfo(
            ProductDetailsFactory.OneTimeOffer(
                offerId = "launch30",
                offerToken = "token_launch",
                priceMicros = 3_490_000L,
                formattedPrice = "$3.49",
                fullPriceMicros = 4_990_000L,
                discountPercentage = 30,
            ),
        )
        assertTrue(info.isDiscounted)
        assertEquals(30, info.discountPercentage)
        assertEquals(30, info.effectiveDiscountPercent)
        assertEquals(4_990_000L, info.fullPriceMicros)
        assertEquals("launch30", info.offerId)
    }

    @Test
    fun `derives the discount percent when Play reports only a full price`() {
        // $2.49 against a $4.99 list price -> ~50% off.
        val info = offerInfo(
            ProductDetailsFactory.OneTimeOffer(
                offerToken = "token_half",
                priceMicros = 2_490_000L,
                formattedPrice = "$2.49",
                fullPriceMicros = 4_990_000L,
            ),
        )
        assertTrue(info.isDiscounted)
        assertNull(info.discountPercentage)
        assertEquals(50, info.effectiveDiscountPercent)
    }

    @Test
    fun `reports an absolute discount amount`() {
        val info = offerInfo(
            ProductDetailsFactory.OneTimeOffer(
                offerToken = "token_amount",
                priceMicros = 2_990_000L,
                formattedPrice = "$2.99",
                fullPriceMicros = 4_990_000L,
                discountAmountMicros = 2_000_000L,
                formattedDiscountAmount = "$2.00",
            ),
        )
        assertTrue(info.isDiscounted)
        assertEquals(2_000_000L, info.discountAmountMicros)
        assertEquals("$2.00", info.formattedDiscountAmount)
        assertEquals("USD", info.discountCurrencyCode)
    }

    @Test
    fun `discount percent never leaves the zero to one hundred range`() {
        // Price above list price must not produce a negative discount.
        val info = offerInfo(
            ProductDetailsFactory.OneTimeOffer(
                offerToken = "token_odd",
                priceMicros = 9_990_000L,
                formattedPrice = "$9.99",
                fullPriceMicros = 4_990_000L,
            ),
        )
        assertEquals(0, info.effectiveDiscountPercent)
    }

    // ==================== Rentals ====================

    @Test
    fun `maps rental periods`() {
        val info = offerInfo(
            ProductDetailsFactory.OneTimeOffer(
                offerToken = "token_rental",
                priceMicros = 1_990_000L,
                formattedPrice = "$1.99",
                rentalPeriod = "P2D",
                rentalExpirationPeriod = "P30D",
            ),
        )
        assertTrue(info.isRental)
        assertEquals("P2D", info.rentalPeriod)
        assertEquals("P30D", info.rentalExpirationPeriod)
        assertEquals(2, info.rentalPeriodParsed?.totalDays)
        assertEquals(30, info.rentalExpirationPeriodParsed?.totalDays)
    }

    // ==================== Limited quantity ====================

    @Test
    fun `tracks remaining quantity`() {
        val info = offerInfo(
            ProductDetailsFactory.OneTimeOffer(
                offerToken = "token_limited",
                priceMicros = 4_990_000L,
                formattedPrice = "$4.99",
                maximumQuantity = 100,
                remainingQuantity = 7,
            ),
        )
        assertTrue(info.isLimitedQuantity)
        assertFalse(info.isSoldOut)
        assertEquals(100, info.maximumQuantity)
        assertEquals(7, info.remainingQuantity)
    }

    @Test
    fun `reports sold out when nothing remains`() {
        val info = offerInfo(
            ProductDetailsFactory.OneTimeOffer(
                offerToken = "token_sold_out",
                priceMicros = 4_990_000L,
                formattedPrice = "$4.99",
                maximumQuantity = 50,
                remainingQuantity = 0,
            ),
        )
        assertTrue(info.isLimitedQuantity)
        assertTrue(info.isSoldOut)
    }

    // ==================== Validity window ====================

    @Test
    fun `an offer without a window is always valid`() {
        val info = offerInfo(plainOffer())
        assertEquals(0L, info.validFromMillis)
        assertEquals(0L, info.validUntilMillis)
        assertTrue(info.isValidAt(0L))
        assertTrue(info.isValidAt(Long.MAX_VALUE))
    }

    @Test
    fun `honours the validity window bounds`() {
        val info = offerInfo(
            ProductDetailsFactory.OneTimeOffer(
                offerToken = "token_window",
                priceMicros = 4_990_000L,
                formattedPrice = "$4.99",
                validFromMillis = 1_000L,
                validUntilMillis = 2_000L,
            ),
        )
        assertFalse(info.isValidAt(999L))
        assertTrue(info.isValidAt(1_000L))
        assertTrue(info.isValidAt(1_500L))
        assertTrue(info.isValidAt(2_000L))
        assertFalse(info.isValidAt(2_001L))
    }

    // ==================== Tags ====================

    @Test
    fun `matches offer tags case-insensitively`() {
        val info = offerInfo(
            ProductDetailsFactory.OneTimeOffer(
                offerToken = "token_tagged",
                offerTags = listOf("launch"),
                priceMicros = 4_990_000L,
                formattedPrice = "$4.99",
            ),
        )
        assertTrue(info.hasTag("launch"))
        assertTrue(info.hasTag("LAUNCH"))
        assertFalse(info.hasTag("holiday"))
    }

    // ==================== Legacy products ====================

    @Test
    fun `maps a legacy single-offer product`() {
        val details = ProductDetailsFactory.legacyOneTime("lifetime", plainOffer(19_990_000L, "$19.99"))
        val legacy = details.oneTimePurchaseOfferDetails
        assertNotNull(legacy)
        val info = OneTimeOfferInfo.from("lifetime", legacy!!)
        assertEquals(19_990_000L, info.priceMicros)
        assertEquals("$19.99", info.formattedPrice)
        assertFalse(info.isDiscounted)
    }

    @Test
    fun `the billing library synthesizes an offer list for legacy products`() {
        // Play Billing 9 back-fills the list from the single legacy offer, so a
        // product configured before one-time offers existed still enumerates.
        val details = ProductDetailsFactory.legacyOneTime("lifetime", plainOffer(19_990_000L, "$19.99"))
        val list = details.oneTimePurchaseOfferDetailsList
        assertNotNull(list)
        assertEquals(1, list!!.size)
        assertEquals(19_990_000L, OneTimeOfferInfo.from("lifetime", list.first()).priceMicros)
    }
}
