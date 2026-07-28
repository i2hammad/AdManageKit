package com.i2hammad.admanagekit.billing

/**
 * Receives the outcome of a Play in-app messaging surface shown by
 * [AppPurchase.showInAppMessages].
 *
 * In-app messages are how Google Play recovers subscriptions whose payment was
 * declined: Play shows the user a fix-your-payment-method flow inside your app.
 * If the user resolves the problem, Play returns the token of the recovered
 * purchase and the subscription becomes active again — so the app must refresh
 * its entitlement state when [onSubscriptionRecovered] fires.
 *
 * Callbacks are delivered on the main thread.
 *
 * @since 4.4.0
 */
interface InAppMessageListener {

    /**
     * Called when Play displayed a message and the user recovered a subscription
     * that was in a payment-declined state.
     *
     * [AppPurchase] has already refreshed its own purchase state by the time this
     * fires; update any cached entitlement in the app here.
     *
     * @param purchaseToken token of the purchase that was recovered.
     */
    fun onSubscriptionRecovered(purchaseToken: String)

    /**
     * Called when Play had nothing to show, or the user dismissed the message
     * without changing anything. No entitlement change occurred.
     */
    fun onNoActionNeeded()
}
