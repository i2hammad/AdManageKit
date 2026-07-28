package com.android.billingclient.api

/**
 * Builds real [ProductDetails] instances for unit tests.
 *
 * [ProductDetails] is final with a package-private JSON constructor, so it can be
 * neither mocked nor constructed from outside its own package. This helper lives
 * in `com.android.billingclient.api` precisely so it can reach that constructor,
 * letting tests exercise the library's real parsing rather than a stand-in.
 *
 * The JSON shape mirrors what Google Play returns to the billing library.
 */
object ProductDetailsFactory {

    /** One pricing phase of a subscription offer. */
    data class Phase(
        val priceMicros: Long,
        val currency: String = "USD",
        val formattedPrice: String,
        val billingPeriod: String,
        /** 1 = INFINITE_RECURRING, 2 = FINITE_RECURRING, 3 = NON_RECURRING. */
        val recurrenceMode: Int,
        val billingCycleCount: Int = 0,
    )

    /** One offer attached to a subscription product. */
    data class Offer(
        val basePlanId: String,
        val offerId: String? = null,
        val offerToken: String,
        val offerTags: List<String> = emptyList(),
        val phases: List<Phase>,
        /** Non-null makes this an installments base plan. */
        val installmentCommitment: Int? = null,
        val installmentRenewalCommitment: Int? = null,
    )

    /** One offer attached to a one-time (INAPP) product. */
    data class OneTimeOffer(
        val offerId: String? = null,
        val purchaseOptionId: String? = null,
        val offerToken: String,
        val offerTags: List<String> = emptyList(),
        val priceMicros: Long,
        val formattedPrice: String,
        val currency: String = "USD",
        val fullPriceMicros: Long? = null,
        val discountPercentage: Int? = null,
        val discountAmountMicros: Long? = null,
        val formattedDiscountAmount: String? = null,
        val rentalPeriod: String? = null,
        val rentalExpirationPeriod: String? = null,
        val maximumQuantity: Int? = null,
        val remainingQuantity: Int? = null,
        val validFromMillis: Long? = null,
        val validUntilMillis: Long? = null,
    )

    /** Builds a subscription [ProductDetails] carrying [offers]. */
    fun subscription(
        productId: String,
        offers: List<Offer>,
        title: String = "$productId (Test App)",
        name: String = productId,
        description: String = "Test subscription",
    ): ProductDetails {
        val offersJson = offers.joinToString(",") { offer ->
            val phasesJson = offer.phases.joinToString(",") { phase ->
                """
                {
                  "priceAmountMicros": ${phase.priceMicros},
                  "priceCurrencyCode": "${phase.currency}",
                  "formattedPrice": "${phase.formattedPrice}",
                  "billingPeriod": "${phase.billingPeriod}",
                  "recurrenceMode": ${phase.recurrenceMode},
                  "billingCycleCount": ${phase.billingCycleCount}
                }
                """.trimIndent()
            }
            val tagsJson = offer.offerTags.joinToString(",") { "\"$it\"" }
            val installmentJson = if (offer.installmentCommitment != null) {
                """,
                "installmentPlanDetails": {
                  "commitmentPaymentsCount": ${offer.installmentCommitment},
                  "subsequentCommitmentPaymentsCount": ${offer.installmentRenewalCommitment ?: 0}
                }
                """.trimIndent()
            } else {
                ""
            }
            """
            {
              "basePlanId": "${offer.basePlanId}",
              ${if (offer.offerId != null) "\"offerId\": \"${offer.offerId}\"," else ""}
              "offerIdToken": "${offer.offerToken}",
              "offerTags": [$tagsJson],
              "pricingPhases": [$phasesJson]$installmentJson
            }
            """.trimIndent()
        }
        val json = """
        {
          "productId": "$productId",
          "type": "subs",
          "title": "$title",
          "name": "$name",
          "description": "$description",
          "subscriptionOfferDetails": [$offersJson]
        }
        """.trimIndent()
        return ProductDetails(json)
    }

    /** Builds a one-time (INAPP) [ProductDetails] carrying [offers]. */
    fun oneTime(
        productId: String,
        offers: List<OneTimeOffer>,
        title: String = "$productId (Test App)",
        name: String = productId,
        description: String = "Test product",
    ): ProductDetails {
        val offersJson = offers.joinToString(",") { offer -> oneTimeOfferJson(offer) }
        // Play sends both the list (Billing 9) and the legacy single offer.
        val json = """
        {
          "productId": "$productId",
          "type": "inapp",
          "title": "$title",
          "name": "$name",
          "description": "$description",
          "oneTimePurchaseOfferDetails": ${oneTimeOfferJson(offers.first())},
          "oneTimePurchaseOfferDetailsList": [$offersJson]
        }
        """.trimIndent()
        return ProductDetails(json)
    }

    /** Builds a legacy one-time product exposing only the single offer field. */
    fun legacyOneTime(
        productId: String,
        offer: OneTimeOffer,
    ): ProductDetails {
        val json = """
        {
          "productId": "$productId",
          "type": "inapp",
          "title": "$productId (Test App)",
          "name": "$productId",
          "description": "Test product",
          "oneTimePurchaseOfferDetails": ${oneTimeOfferJson(offer)}
        }
        """.trimIndent()
        return ProductDetails(json)
    }

    private fun oneTimeOfferJson(offer: OneTimeOffer): String {
        val parts = mutableListOf(
            "\"priceAmountMicros\": ${offer.priceMicros}",
            "\"priceCurrencyCode\": \"${offer.currency}\"",
            "\"formattedPrice\": \"${offer.formattedPrice}\"",
            "\"offerIdToken\": \"${offer.offerToken}\"",
            "\"offerTags\": [${offer.offerTags.joinToString(",") { "\"$it\"" }}]",
        )
        offer.offerId?.let { parts += "\"offerId\": \"$it\"" }
        offer.purchaseOptionId?.let { parts += "\"purchaseOptionId\": \"$it\"" }
        offer.fullPriceMicros?.let { parts += "\"fullPriceMicros\": $it" }

        if (offer.discountPercentage != null || offer.discountAmountMicros != null) {
            val discountParts = mutableListOf<String>()
            offer.discountPercentage?.let { discountParts += "\"percentageDiscount\": $it" }
            if (offer.discountAmountMicros != null) {
                discountParts += """
                "discountAmount": {
                  "discountAmountMicros": ${offer.discountAmountMicros},
                  "discountAmountCurrencyCode": "${offer.currency}",
                  "formattedDiscountAmount": "${offer.formattedDiscountAmount.orEmpty()}"
                }
                """.trimIndent()
            }
            parts += "\"discountDisplayInfo\": {${discountParts.joinToString(",")}}"
        }
        if (offer.rentalPeriod != null) {
            val rentalParts = mutableListOf("\"rentalPeriod\": \"${offer.rentalPeriod}\"")
            offer.rentalExpirationPeriod?.let {
                rentalParts += "\"rentalExpirationPeriod\": \"$it\""
            }
            parts += "\"rentalDetails\": {${rentalParts.joinToString(",")}}"
        }
        if (offer.maximumQuantity != null) {
            parts += """
            "limitedQuantityInfo": {
              "maximumQuantity": ${offer.maximumQuantity},
              "remainingQuantity": ${offer.remainingQuantity ?: 0}
            }
            """.trimIndent()
        }
        if (offer.validFromMillis != null || offer.validUntilMillis != null) {
            val windowParts = mutableListOf<String>()
            offer.validFromMillis?.let { windowParts += "\"startTimeMillis\": $it" }
            offer.validUntilMillis?.let { windowParts += "\"endTimeMillis\": $it" }
            parts += "\"validTimeWindow\": {${windowParts.joinToString(",")}}"
        }
        return "{${parts.joinToString(",")}}"
    }

    // Recurrence mode constants, mirroring ProductDetails.RecurrenceMode.
    const val INFINITE_RECURRING = 1
    const val FINITE_RECURRING = 2
    const val NON_RECURRING = 3
}
