package com.i2hammad.admanagekit.billing

import com.android.billingclient.api.ProductDetailsFactory
import com.android.billingclient.api.ProductDetailsFactory.FINITE_RECURRING
import com.android.billingclient.api.ProductDetailsFactory.INFINITE_RECURRING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [OfferInfo] — phase classification and the derived pricing a paywall
 * renders. Fixtures are real [com.android.billingclient.api.ProductDetails]
 * parsed from Play-shaped JSON, so these exercise the billing library's own
 * parsing rather than a stand-in.
 */
class OfferInfoTest {

    private fun basePhase(
        priceMicros: Long = 9_990_000L,
        formatted: String = "$9.99",
        period: String = "P1M",
    ) = ProductDetailsFactory.Phase(
        priceMicros = priceMicros,
        formattedPrice = formatted,
        billingPeriod = period,
        recurrenceMode = INFINITE_RECURRING,
    )

    private fun trialPhase(period: String = "P7D") = ProductDetailsFactory.Phase(
        priceMicros = 0L,
        formattedPrice = "Free",
        billingPeriod = period,
        recurrenceMode = FINITE_RECURRING,
        billingCycleCount = 1,
    )

    private fun introPhase(
        priceMicros: Long = 1_990_000L,
        formatted: String = "$1.99",
        period: String = "P1M",
        cycles: Int = 3,
    ) = ProductDetailsFactory.Phase(
        priceMicros = priceMicros,
        formattedPrice = formatted,
        billingPeriod = period,
        recurrenceMode = FINITE_RECURRING,
        billingCycleCount = cycles,
    )

    private fun offerInfo(
        offer: ProductDetailsFactory.Offer,
        productId: String = "premium_sub",
    ): OfferInfo {
        val details = ProductDetailsFactory.subscription(productId, listOf(offer))
        val subscriptionOffers = details.subscriptionOfferDetails
        assertNotNull("fixture produced no subscription offers", subscriptionOffers)
        return OfferInfo.from(productId, subscriptionOffers!!.first())
    }

    // ==================== Fixture sanity ====================

    @Test
    fun `fixture builds a parseable ProductDetails`() {
        val details = ProductDetailsFactory.subscription(
            "premium_sub",
            listOf(
                ProductDetailsFactory.Offer(
                    basePlanId = "monthly",
                    offerToken = "token_monthly",
                    phases = listOf(basePhase()),
                ),
            ),
        )
        assertEquals("premium_sub", details.productId)
        assertEquals("subs", details.productType)
        assertEquals(1, details.subscriptionOfferDetails?.size)
        assertEquals(
            1,
            details.subscriptionOfferDetails!!.first().pricingPhases.pricingPhaseList.size,
        )
    }

    // ==================== Phase classification ====================

    @Test
    fun `classifies a base-only offer`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_monthly",
                phases = listOf(basePhase()),
            ),
        )
        assertFalse(info.isFreeTrial)
        assertFalse(info.hasIntroPrice)
        assertTrue(info.isBaseOffer)
        assertFalse(info.hasPromotion)
        assertEquals("$9.99", info.basePrice)
        assertEquals(9_990_000L, info.basePriceMicros)
        assertEquals("P1M", info.billingPeriod)
        assertEquals("USD", info.currencyCode)
    }

    @Test
    fun `classifies a free-trial offer`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerId = "trial7",
                offerToken = "token_trial",
                phases = listOf(trialPhase(), basePhase()),
            ),
        )
        assertTrue(info.isFreeTrial)
        assertFalse(info.hasIntroPrice)
        assertFalse(info.isBaseOffer)
        assertTrue(info.hasPromotion)
        assertEquals("P7D", info.trialPeriod)
        assertEquals(7, info.trialDays)
        assertEquals("trial7", info.offerId)
        assertEquals("token_trial", info.offerToken)
    }

    @Test
    fun `classifies an introductory offer`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerId = "intro50",
                offerToken = "token_intro",
                phases = listOf(introPhase(), basePhase()),
            ),
        )
        assertFalse(info.isFreeTrial)
        assertTrue(info.hasIntroPrice)
        assertEquals("$1.99", info.introPrice)
        assertEquals(1_990_000L, info.introPriceMicros)
        assertEquals("P1M", info.introPeriod)
        assertEquals(3, info.introCycleCount)
        // Three monthly cycles.
        assertEquals(90, info.introTotalDays)
    }

    @Test
    fun `classifies an offer carrying both trial and intro phases`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerId = "trial_then_intro",
                offerToken = "token_combo",
                phases = listOf(trialPhase(), introPhase(), basePhase()),
            ),
        )
        assertTrue(info.isFreeTrial)
        assertTrue(info.hasIntroPrice)
        assertEquals(3, info.pricingPhases.size)
        assertEquals("P7D", info.trialPeriod)
        assertEquals("$1.99", info.introPrice)
        assertEquals("$9.99", info.basePrice)
    }

    @Test
    fun `classifies phases by recurrence and price rather than list order`() {
        // Base phase listed first, promotional phases after.
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_reordered",
                phases = listOf(basePhase(), trialPhase(), introPhase()),
            ),
        )
        assertTrue(info.isFreeTrial)
        assertTrue(info.hasIntroPrice)
        assertEquals("$9.99", info.basePrice)
        assertEquals("$1.99", info.introPrice)
    }

    @Test
    fun `falls back to the last phase when no infinite-recurring phase exists`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "prepaid",
                offerToken = "token_prepaid",
                phases = listOf(
                    ProductDetailsFactory.Phase(
                        priceMicros = 4_990_000L,
                        formattedPrice = "$4.99",
                        billingPeriod = "P1M",
                        recurrenceMode = FINITE_RECURRING,
                        billingCycleCount = 1,
                    ),
                ),
            ),
        )
        assertEquals("$4.99", info.basePrice)
        assertEquals(4_990_000L, info.basePriceMicros)
    }

    // ==================== First-cycle pricing ====================

    @Test
    fun `first cycle price is zero during a free trial`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_trial",
                phases = listOf(trialPhase(), basePhase()),
            ),
        )
        assertEquals(0L, info.firstCyclePriceMicros)
        assertEquals("Free", info.firstCyclePrice)
    }

    @Test
    fun `first cycle price is the intro price when there is no trial`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_intro",
                phases = listOf(introPhase(), basePhase()),
            ),
        )
        assertEquals(1_990_000L, info.firstCyclePriceMicros)
        assertEquals("$1.99", info.firstCyclePrice)
    }

    @Test
    fun `first cycle price is the base price for a plain offer`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_base",
                phases = listOf(basePhase()),
            ),
        )
        assertEquals(9_990_000L, info.firstCyclePriceMicros)
        assertEquals("$9.99", info.firstCyclePrice)
    }

    // ==================== Normalized pricing ====================

    @Test
    fun `normalizes a yearly plan to a monthly price`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "yearly",
                offerToken = "token_yearly",
                phases = listOf(basePhase(59_990_000L, "$59.99", "P1Y")),
            ),
        )
        assertEquals(4_999_167L, info.pricePerMonthMicros)
    }

    @Test
    fun `monthly plan normalizes to itself`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_monthly",
                phases = listOf(basePhase()),
            ),
        )
        assertEquals(9_990_000L, info.pricePerMonthMicros)
    }

    @Test
    fun `intro discount percent normalizes across differing cadences`() {
        // $1.99/month intro against a $9.99/month base -> ~80% off.
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_intro",
                phases = listOf(introPhase(), basePhase()),
            ),
        )
        assertEquals(80, info.introDiscountPercent)
    }

    @Test
    fun `intro discount percent is zero without an intro phase`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_base",
                phases = listOf(basePhase()),
            ),
        )
        assertEquals(0, info.introDiscountPercent)
    }

    // ==================== Parsed periods ====================

    @Test
    fun `exposes parsed billing and trial periods`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "yearly",
                offerToken = "token_yearly",
                phases = listOf(trialPhase("P14D"), basePhase(59_990_000L, "$59.99", "P1Y")),
            ),
        )
        assertEquals(BillingPeriod.Unit.YEAR, info.billingPeriodParsed?.unit)
        assertEquals(1, info.billingPeriodParsed?.count)
        assertEquals(BillingPeriod.Unit.DAY, info.trialPeriodParsed?.unit)
        assertEquals(14, info.trialPeriodParsed?.count)
        assertEquals(14, info.trialDays)
        assertNull(info.introPeriodParsed)
    }

    // ==================== Tags ====================

    @Test
    fun `matches offer tags case-insensitively`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_tagged",
                offerTags = listOf("popular", "winback"),
                phases = listOf(basePhase()),
            ),
        )
        assertTrue(info.hasTag("popular"))
        assertTrue(info.hasTag("POPULAR"))
        assertTrue(info.hasTag("Winback"))
        assertFalse(info.hasTag("seasonal"))
        assertEquals(listOf("popular", "winback"), info.offerTags)
    }

    @Test
    fun `an offer without tags reports an empty tag list`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_untagged",
                phases = listOf(basePhase()),
            ),
        )
        assertTrue(info.offerTags.isEmpty())
        assertFalse(info.hasTag("popular"))
    }

    // ==================== Identity ====================

    @Test
    fun `carries product, base plan and token identity`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "yearly",
                offerId = "promo",
                offerToken = "token_abc",
                phases = listOf(basePhase()),
            ),
            productId = "pro_sub",
        )
        assertEquals("pro_sub", info.productId)
        assertEquals("yearly", info.basePlanId)
        assertEquals("promo", info.offerId)
        assertEquals("token_abc", info.offerToken)
    }

    @Test
    fun `base plan offers report a null offer id`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_base",
                phases = listOf(basePhase()),
            ),
        )
        assertNull(info.offerId)
    }

    // ==================== Installments ====================

    @Test
    fun `a standard plan is not an installment plan`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly",
                offerToken = "token_monthly",
                phases = listOf(basePhase()),
            ),
        )
        assertFalse(info.isInstallmentPlan)
        assertNull(info.installmentPlanDetails)
        assertEquals(0, info.installmentCommitmentPayments)
        assertEquals(0, info.installmentRenewalCommitmentPayments)
    }

    @Test
    fun `surfaces installment plan commitments`() {
        val info = offerInfo(
            ProductDetailsFactory.Offer(
                basePlanId = "monthly_installments",
                offerToken = "token_installments",
                phases = listOf(basePhase()),
                installmentCommitment = 12,
                installmentRenewalCommitment = 6,
            ),
        )
        assertTrue(info.isInstallmentPlan)
        assertNotNull(info.installmentPlanDetails)
        assertEquals(12, info.installmentCommitmentPayments)
        assertEquals(6, info.installmentRenewalCommitmentPayments)
    }
}
