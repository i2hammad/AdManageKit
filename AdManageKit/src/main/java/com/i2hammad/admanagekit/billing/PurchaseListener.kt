package com.i2hammad.admanagekit.billing;

public interface PurchaseListener {

    /**
     * Called when a product has been successfully purchased.
     *
     * @param orderId       The order ID of the purchased product.
     * @param originalJson  The original JSON response from the purchase.
     */
    void onProductPurchased(String orderId, String originalJson);

    /**
     * Displays an error message when there is an issue with the purchase process.
     *
     * @param errorMessage  The error message to be displayed.
     */
    void displayErrorMessage(String errorMessage);

    /**
     * Called when the user cancels the billing process.
     */
    void onUserCancelBilling();
}
