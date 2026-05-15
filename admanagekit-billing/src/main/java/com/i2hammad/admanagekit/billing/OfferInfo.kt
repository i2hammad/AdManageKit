package com.i2hammad.admanagekit.billing

import com.android.billingclient.api.ProductDetails

/**
 * Typed view over a subscription [ProductDetails.SubscriptionOfferDetails], surfacing
 * the trial / intro / base pricing phases as flat fields for easy UI binding.
 *
 * Each Play subscription offer can have up to three pricing phases:
 *  - **free trial**   — `priceAmountMicros == 0`, `RecurrenceMode.FINITE_RECURRING`
 *  - **introductory** — `priceAmountMicros > 0`, `RecurrenceMode.FINITE_RECURRING`
 *  - **base**         — `RecurrenceMode.INFINITE_RECURRING`
 *
 * Use [AppPurchase.getOffers], [AppPurchase.getTrialOffer], or
 * [AppPurchase.getBaseOffer] to obtain instances — do not construct directly.
 *
 * @since 3.5.7
 */
data class OfferInfo(
    /** Play product id this offer belongs to. */
    val productId: String,
    /** Base plan id from Play Console (null on legacy or simple offers). */
    val basePlanId: String?,
    /** Offer id from Play Console (null when this row represents the base plan itself). */
    val offerId: String?,
    /** Opaque token passed to [com.android.billingclient.api.BillingFlowParams] when launching purchase. */
    val offerToken: String,
    /** Free-form tags configured for the offer in Play Console. */
    val offerTags: List<String>,
    /** Raw phase list — use the typed `trial*` / `intro*` / `base*` fields where possible. */
    val pricingPhases: List<ProductDetails.PricingPhase>,

    // ---- Free trial ----
    /** `true` when the offer has a free-trial phase. */
    val isFreeTrial: Boolean,
    /** ISO-8601 trial duration (e.g. `"P7D"`), or null. */
    val trialPeriod: String?,
    /** Raw trial phase, or null. */
    val trialPhase: ProductDetails.PricingPhase?,

    // ---- Introductory price ----
    /** `true` when the offer has a paid finite-recurring (intro) phase. */
    val hasIntroPrice: Boolean,
    /** Locale-formatted intro price (e.g. `"$1.99"`), or null. */
    val introPrice: String?,
    /** Intro price in micros (price × 1,000,000), or 0. */
    val introPriceMicros: Long,
    /** ISO-8601 intro period (e.g. `"P1M"`), or null. */
    val introPeriod: String?,
    /** Number of billing cycles the intro phase covers, or 0. */
    val introCycleCount: Int,
    /** Raw intro phase, or null. */
    val introPhase: ProductDetails.PricingPhase?,

    // ---- Base recurring price ----
    /** Locale-formatted base price (e.g. `"$9.99"`); empty if no base phase exists. */
    val basePrice: String,
    /** Base price in micros, or 0. */
    val basePriceMicros: Long,
    /** ISO-8601 base billing cycle (e.g. `"P1M"`, `"P1Y"`), or null. */
    val billingPeriod: String?,
    /** ISO-4217 currency code (e.g. `"USD"`); falls back to intro/trial currency if base is absent. */
    val currencyCode: String,
    /** Raw base phase, or null. */
    val basePhase: ProductDetails.PricingPhase?,
) {
    companion object {
        /**
         * Classifies the pricing phases of a [ProductDetails.SubscriptionOfferDetails]
         * and returns a populated [OfferInfo]. Phases are matched by recurrence mode
         * and price, not by list position, so an offer with phases in any order is
         * handled correctly.
         */
        @JvmStatic
        fun from(productId: String, offer: ProductDetails.SubscriptionOfferDetails): OfferInfo {
            val phases = offer.pricingPhases.pricingPhaseList
            var trial: ProductDetails.PricingPhase? = null
            var intro: ProductDetails.PricingPhase? = null
            var base: ProductDetails.PricingPhase? = null

            for (phase in phases) {
                when {
                    phase.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING ->
                        base = phase
                    phase.priceAmountMicros == 0L &&
                        phase.recurrenceMode == ProductDetails.RecurrenceMode.FINITE_RECURRING ->
                        trial = phase
                    phase.recurrenceMode == ProductDetails.RecurrenceMode.FINITE_RECURRING ->
                        intro = phase
                }
            }
            // Fallback: if no INFINITE_RECURRING phase, treat the last phase as base.
            if (base == null && phases.isNotEmpty()) base = phases.last()

            return OfferInfo(
                productId = productId,
                basePlanId = offer.basePlanId,
                offerId = offer.offerId,
                offerToken = offer.offerToken,
                offerTags = offer.offerTags ?: emptyList(),
                pricingPhases = phases,
                isFreeTrial = trial != null,
                trialPeriod = trial?.billingPeriod,
                trialPhase = trial,
                hasIntroPrice = intro != null,
                introPrice = intro?.formattedPrice,
                introPriceMicros = intro?.priceAmountMicros ?: 0L,
                introPeriod = intro?.billingPeriod,
                introCycleCount = intro?.billingCycleCount ?: 0,
                introPhase = intro,
                basePrice = base?.formattedPrice.orEmpty(),
                basePriceMicros = base?.priceAmountMicros ?: 0L,
                billingPeriod = base?.billingPeriod,
                currencyCode = base?.priceCurrencyCode
                    ?: intro?.priceCurrencyCode
                    ?: trial?.priceCurrencyCode
                    ?: "",
                basePhase = base,
            )
        }
    }
}
