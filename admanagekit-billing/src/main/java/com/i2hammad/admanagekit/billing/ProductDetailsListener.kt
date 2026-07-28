package com.i2hammad.admanagekit.billing

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.UnfetchedProduct

/**
 * Receives the outcome of each product-details query.
 *
 * Without this listener, a paywall that renders empty prices gives no signal as
 * to *why*: the id may be misspelled, the product may be inactive in Play
 * Console, the app may not be published on a track the account can see, or the
 * query may simply not have completed yet. Play Billing 9 reports the products
 * it could not fetch alongside the ones it could — this listener surfaces both.
 *
 * Callbacks are delivered on the main thread. Register with
 * [AppPurchase.setProductDetailsListener] before calling
 * [AppPurchase.initBilling].
 *
 * @since 4.4.0
 */
interface ProductDetailsListener {

    /**
     * Called after a successful query, once per product type.
     *
     * @param productType [com.android.billingclient.api.BillingClient.ProductType.INAPP]
     *                    or `SUBS`.
     * @param loaded      products Play returned details for; prices are now
     *                    available from the `AppPurchase` getters.
     * @param unfetched   products Play could not return, each with a status code
     *                    explaining why. Empty on a fully successful query.
     */
    fun onProductDetailsLoaded(
        productType: String,
        loaded: List<ProductDetails>,
        unfetched: List<UnfetchedProduct>,
    )

    /**
     * Called when the query itself failed — no details were returned at all.
     *
     * @param productType  the product type that was queried.
     * @param responseCode a [com.android.billingclient.api.BillingClient.BillingResponseCode].
     * @param debugMessage Play's debug message, for logging only. Never show
     *                     this to users.
     */
    fun onProductDetailsFailed(productType: String, responseCode: Int, debugMessage: String?)
}
