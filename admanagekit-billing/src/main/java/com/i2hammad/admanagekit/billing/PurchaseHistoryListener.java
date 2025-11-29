package com.i2hammad.admanagekit.billing;

/**
 * Listener interface for purchase history events.
 * Implement this to track and persist purchase history in your app.
 *
 * <p>Since Google Play Billing Library 8+ deprecated queryPurchaseHistoryAsync,
 * consumed purchase tracking must be done by your app. Use these callbacks to
 * persist purchase events and build your own purchase history.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * AppPurchase.getInstance().setPurchaseHistoryListener(new PurchaseHistoryListener() {
 *     {@literal @}Override
 *     public void onNewPurchase(String productId, PurchaseResult purchase) {
 *         // Save to your database/preferences
 *         savePurchase(productId, purchase.getQuantity(), purchase.getPurchaseTime(), false);
 *     }
 *
 *     {@literal @}Override
 *     public void onPurchaseConsumed(String productId, PurchaseResult purchase) {
 *         // Mark as consumed in your database
 *         markConsumed(productId, purchase.getOrderId());
 *     }
 * });
 * </pre>
 */
public interface PurchaseHistoryListener {

    /**
     * Called when a new purchase is completed and acknowledged.
     * Use this to track purchase count and purchase time.
     *
     * @param productId The product ID that was purchased.
     * @param purchase  The purchase result containing order details, quantity, and timestamp.
     */
    void onNewPurchase(String productId, PurchaseResult purchase);

    /**
     * Called when a purchase is consumed (for consumable products).
     * After consumption, the product can be purchased again.
     * Use this to mark the purchase as consumed in your tracking system.
     *
     * @param productId The product ID that was consumed.
     * @param purchase  The purchase result that was consumed.
     */
    void onPurchaseConsumed(String productId, PurchaseResult purchase);
}
