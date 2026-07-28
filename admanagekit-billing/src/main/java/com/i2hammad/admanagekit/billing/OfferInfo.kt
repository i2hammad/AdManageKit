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

    /**
     * Installment plan details when this base plan is a Play *installments*
     * subscription, or null for standard auto-renewing plans.
     *
     * @since 4.4.0
     */
    val installmentPlanDetails: ProductDetails.InstallmentPlanDetails? = null,
) {

    // ---- Derived: classification -------------------------------------------

    /**
     * `true` when this offer has no promotional phase — neither a free trial nor
     * an introductory price — i.e. the user pays the recurring price from day one.
     *
     * @since 4.4.0
     */
    val isBaseOffer: Boolean
        get() = !isFreeTrial && !hasIntroPrice

    /**
     * `true` when this offer carries any promotional phase (trial or intro price).
     * The inverse of [isBaseOffer].
     *
     * @since 4.4.0
     */
    val hasPromotion: Boolean
        get() = !isBaseOffer

    /**
     * `true` when this offer is an installments base plan — the user commits to a
     * number of payments up front. See [installmentCommitmentPayments].
     *
     * @since 4.4.0
     */
    val isInstallmentPlan: Boolean
        get() = installmentPlanDetails != null

    /**
     * Number of payments the user commits to when first purchasing an
     * installments plan, or 0 when this is not an installments plan.
     *
     * @since 4.4.0
     */
    val installmentCommitmentPayments: Int
        get() = installmentPlanDetails?.installmentPlanCommitmentPaymentsCount ?: 0

    /**
     * Number of payments the user commits to on each renewal of an installments
     * plan, or 0 when this is not an installments plan.
     *
     * @since 4.4.0
     */
    val installmentRenewalCommitmentPayments: Int
        get() = installmentPlanDetails?.subsequentInstallmentPlanCommitmentPaymentsCount ?: 0

    // ---- Derived: parsed periods -------------------------------------------

    /**
     * [billingPeriod] parsed into components, or null when absent/unparseable.
     * Use [BillingPeriod.unit] and [BillingPeriod.count] to render a localized
     * label such as "per month".
     *
     * @since 4.4.0
     */
    val billingPeriodParsed: BillingPeriod?
        get() = BillingPeriod.parse(billingPeriod)

    /**
     * [trialPeriod] parsed into components, or null when this offer has no trial.
     *
     * @since 4.4.0
     */
    val trialPeriodParsed: BillingPeriod?
        get() = BillingPeriod.parse(trialPeriod)

    /**
     * [introPeriod] parsed into components, or null when this offer has no intro
     * phase. Note this is the length of a *single* intro cycle — multiply by
     * [introCycleCount] for the full promotional window, or use
     * [introTotalDays].
     *
     * @since 4.4.0
     */
    val introPeriodParsed: BillingPeriod?
        get() = BillingPeriod.parse(introPeriod)

    /**
     * Length of the free trial in days, or 0 when there is no trial.
     *
     * @since 4.4.0
     */
    val trialDays: Int
        get() = trialPeriodParsed?.totalDays ?: 0

    /**
     * Total length of the introductory window in days — one intro cycle
     * multiplied by [introCycleCount]. Returns 0 when there is no intro phase.
     *
     * @since 4.4.0
     */
    val introTotalDays: Int
        get() {
            val perCycle = introPeriodParsed?.totalDays ?: return 0
            return perCycle * introCycleCount.coerceAtLeast(1)
        }

    // ---- Derived: normalized pricing ---------------------------------------

    /**
     * Recurring price normalized to one average month, in micros. Lets a paywall
     * compare a yearly plan against a monthly one on equal terms, and render
     * "only $4.16/month, billed yearly".
     *
     * Returns 0 when the base price or billing period is unavailable.
     *
     * @since 4.4.0
     */
    val pricePerMonthMicros: Long
        get() = BillingPeriod.pricePerMonthMicros(basePriceMicros, billingPeriod)

    /**
     * Recurring price normalized to one week, in micros. Returns 0 when the base
     * price or billing period is unavailable.
     *
     * @since 4.4.0
     */
    val pricePerWeekMicros: Long
        get() = BillingPeriod.pricePerWeekMicros(basePriceMicros, billingPeriod)

    /**
     * The price the user actually pays for the *first* billing cycle, in micros:
     * 0 during a free trial, the intro price when one applies, otherwise the base
     * price. Use this to render the "you pay now" line of a paywall.
     *
     * @since 4.4.0
     */
    val firstCyclePriceMicros: Long
        get() = when {
            isFreeTrial -> 0L
            hasIntroPrice -> introPriceMicros
            else -> basePriceMicros
        }

    /**
     * Locale-formatted counterpart of [firstCyclePriceMicros] — the trial phase's
     * formatted price (usually a localized "Free"), the intro price, or the base
     * price. Empty when no phase is available.
     *
     * @since 4.4.0
     */
    val firstCyclePrice: String
        get() = when {
            isFreeTrial -> trialPhase?.formattedPrice.orEmpty()
            hasIntroPrice -> introPrice.orEmpty()
            else -> basePrice
        }

    /**
     * Whole-percent discount of the introductory price against the recurring
     * price, normalized per month so a "3 months for the price of 1" offer scores
     * correctly against a monthly base.
     *
     * Returns 0 when this offer has no intro phase, when either price/period is
     * missing, or when the intro phase is not actually cheaper.
     *
     * @since 4.4.0
     */
    val introDiscountPercent: Int
        get() {
            if (!hasIntroPrice) return 0
            return BillingPeriod.savingsPercent(
                baseMicros = basePriceMicros,
                basePeriod = billingPeriod,
                comparedMicros = introPriceMicros,
                comparedPeriod = introPeriod,
            )
        }

    // ---- Derived: tags ------------------------------------------------------

    /**
     * `true` when this offer carries the given Play Console offer tag.
     * Tags are the supported way to mark an offer for a specific paywall slot
     * (e.g. tag `"popular"` and look it up with [AppPurchase.getOffersByTag]).
     *
     * @param tag tag to test; matched case-insensitively.
     * @since 4.4.0
     */
    fun hasTag(tag: String): Boolean = offerTags.any { it.equals(tag, ignoreCase = true) }

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
                installmentPlanDetails = offer.installmentPlanDetails,
            )
        }
    }
}
