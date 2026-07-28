package com.i2hammad.admanagekit.billing

import com.android.billingclient.api.ProductDetails
import kotlin.math.roundToInt

/**
 * Typed view over a one-time product offer
 * ([ProductDetails.OneTimePurchaseOfferDetails]), surfacing the Play Billing 9
 * features — discounts, rentals, pre-orders and limited quantity — as flat
 * fields for easy UI binding.
 *
 * Play Billing 9 lets a single one-time product carry *multiple* offers (a full
 * price plus, say, a 30%-off launch discount). Older code paths that call
 * [ProductDetails.getOneTimePurchaseOfferDetails] only ever see one of them; use
 * [AppPurchase.getOneTimeOffers] to enumerate all of them and
 * [AppPurchase.purchase] with an explicit offer token to buy a specific one.
 *
 * Obtain instances via [AppPurchase.getOneTimeOffers] or
 * [AppPurchase.getOneTimeOffer] — do not construct directly.
 *
 * @since 4.4.0
 */
data class OneTimeOfferInfo(
    /** Play product id this offer belongs to. */
    val productId: String,
    /** Offer id from Play Console; null for the product's default offer. */
    val offerId: String?,
    /** Purchase option id from Play Console, or null when not configured. */
    val purchaseOptionId: String?,
    /** Opaque token passed to [com.android.billingclient.api.BillingFlowParams] when launching purchase. */
    val offerToken: String,
    /** Free-form tags configured for the offer in Play Console. */
    val offerTags: List<String>,

    // ---- Price ----
    /** Locale-formatted price the user pays (e.g. `"$6.99"`). */
    val formattedPrice: String,
    /** Price the user pays, in micros (price × 1,000,000). */
    val priceMicros: Long,
    /** ISO-4217 currency code (e.g. `"USD"`). */
    val currencyCode: String,
    /**
     * Undiscounted list price in micros when this offer is a discount, or null
     * when the offer has no strike-through price.
     */
    val fullPriceMicros: Long?,

    // ---- Discount ----
    /** Percentage discount configured in Play Console, or null when not a discount offer. */
    val discountPercentage: Int?,
    /** Absolute discount amount in micros, or null when Play reports a percentage instead. */
    val discountAmountMicros: Long?,
    /** Locale-formatted absolute discount amount, or null. */
    val formattedDiscountAmount: String?,
    /** ISO-4217 currency of the absolute discount amount, or null. */
    val discountCurrencyCode: String?,

    // ---- Rental ----
    /** ISO-8601 rental period — how long access lasts after first playback — or null. */
    val rentalPeriod: String?,
    /** ISO-8601 rental expiration period — how long the user has to start — or null. */
    val rentalExpirationPeriod: String?,

    // ---- Pre-order ----
    /** Pre-order release time in epoch millis, or 0 when this is not a pre-order. */
    val preorderReleaseTimeMillis: Long,
    /** Pre-order presale cut-off in epoch millis, or 0 when this is not a pre-order. */
    val preorderPresaleEndTimeMillis: Long,

    // ---- Limited quantity ----
    /** Maximum quantity available for a limited-quantity offer, or 0 when unlimited. */
    val maximumQuantity: Int,
    /** Quantity still available for a limited-quantity offer, or 0 when unlimited. */
    val remainingQuantity: Int,

    // ---- Availability window ----
    /** Start of the offer's validity window in epoch millis, or 0 when unbounded. */
    val validFromMillis: Long,
    /** End of the offer's validity window in epoch millis, or 0 when unbounded. */
    val validUntilMillis: Long,

    /** Raw offer details, for fields not surfaced here. */
    val raw: ProductDetails.OneTimePurchaseOfferDetails,
) {

    /** `true` when this offer is priced below the product's list price. */
    val isDiscounted: Boolean
        get() = discountPercentage != null ||
            discountAmountMicros != null ||
            (fullPriceMicros != null && fullPriceMicros > priceMicros)

    /**
     * Whole-percent discount against [fullPriceMicros]. Prefers the percentage
     * Play reports; otherwise derives it from the full and actual prices.
     * Returns 0 when this offer is not a discount.
     */
    val effectiveDiscountPercent: Int
        get() {
            discountPercentage?.let { return it.coerceIn(0, 100) }
            val full = fullPriceMicros ?: return 0
            if (full <= 0L || priceMicros >= full) return 0
            return ((full - priceMicros).toDouble() / full * 100.0).roundToInt().coerceIn(0, 100)
        }

    /** `true` when this offer is a rental rather than a permanent purchase. */
    val isRental: Boolean
        get() = rentalPeriod != null

    /** `true` when this offer is a pre-order that has not been released yet. */
    val isPreorder: Boolean
        get() = preorderReleaseTimeMillis > 0L

    /** `true` when Play caps how many of this offer can be sold. */
    val isLimitedQuantity: Boolean
        get() = maximumQuantity > 0

    /** `true` when a limited-quantity offer has sold out. */
    val isSoldOut: Boolean
        get() = isLimitedQuantity && remainingQuantity <= 0

    /** [rentalPeriod] parsed into components, or null when this is not a rental. */
    val rentalPeriodParsed: BillingPeriod?
        get() = BillingPeriod.parse(rentalPeriod)

    /** [rentalExpirationPeriod] parsed into components, or null. */
    val rentalExpirationPeriodParsed: BillingPeriod?
        get() = BillingPeriod.parse(rentalExpirationPeriod)

    /**
     * `true` when this offer carries the given Play Console offer tag,
     * matched case-insensitively.
     */
    fun hasTag(tag: String): Boolean = offerTags.any { it.equals(tag, ignoreCase = true) }

    /**
     * `true` when [nowMillis] falls inside this offer's validity window.
     * Unbounded ends are treated as always-valid.
     *
     * @param nowMillis wall-clock time to test, defaulting to the current time.
     */
    @JvmOverloads
    fun isValidAt(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (validFromMillis > 0L && nowMillis < validFromMillis) return false
        if (validUntilMillis > 0L && nowMillis > validUntilMillis) return false
        return true
    }

    companion object {
        /**
         * Maps a raw [ProductDetails.OneTimePurchaseOfferDetails] into a typed
         * [OneTimeOfferInfo]. Optional Play Billing 9 sub-objects that are absent
         * degrade to null/0 rather than throwing.
         */
        @JvmStatic
        fun from(
            productId: String,
            offer: ProductDetails.OneTimePurchaseOfferDetails,
        ): OneTimeOfferInfo {
            val discount = offer.discountDisplayInfo
            val discountAmount = discount?.discountAmount
            val rental = offer.rentalDetails
            val preorder = offer.preorderDetails
            val quantity = offer.limitedQuantityInfo
            val window = offer.validTimeWindow

            return OneTimeOfferInfo(
                productId = productId,
                offerId = offer.offerId,
                purchaseOptionId = offer.purchaseOptionId,
                offerToken = offer.offerToken.orEmpty(),
                offerTags = offer.offerTags ?: emptyList(),
                formattedPrice = offer.formattedPrice,
                priceMicros = offer.priceAmountMicros,
                currencyCode = offer.priceCurrencyCode,
                fullPriceMicros = offer.fullPriceMicros,
                discountPercentage = discount?.percentageDiscount,
                discountAmountMicros = discountAmount?.discountAmountMicros,
                formattedDiscountAmount = discountAmount?.formattedDiscountAmount,
                discountCurrencyCode = discountAmount?.discountAmountCurrencyCode,
                rentalPeriod = rental?.rentalPeriod,
                rentalExpirationPeriod = rental?.rentalExpirationPeriod,
                preorderReleaseTimeMillis = preorder?.preorderReleaseTimeMillis ?: 0L,
                preorderPresaleEndTimeMillis = preorder?.preorderPresaleEndTimeMillis ?: 0L,
                maximumQuantity = quantity?.maximumQuantity ?: 0,
                remainingQuantity = quantity?.remainingQuantity ?: 0,
                validFromMillis = window?.startTimeMillis ?: 0L,
                validUntilMillis = window?.endTimeMillis ?: 0L,
                raw = offer,
            )
        }
    }
}
