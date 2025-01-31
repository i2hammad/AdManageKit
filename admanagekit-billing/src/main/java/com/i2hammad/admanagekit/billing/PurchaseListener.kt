package com.i2hammad.admanagekit.billing

interface PurchaseListener {
    /**
     * Called when a product has been successfully purchased.
     *
     * @param orderId       The order ID of the purchased product.
     * @param originalJson  The original JSON response from the purchase.
     */
    fun onProductPurchased(orderId: String?, originalJson: String?)

    /**
     * Displays an error message when there is an issue with the purchase process.
     *
     * @param errorMessage  The error message to be displayed.
     */
    fun displayErrorMessage(errorMessage: String?)

    /**
     * Called when the user cancels the billing process.
     */
    fun onUserCancelBilling()
}
