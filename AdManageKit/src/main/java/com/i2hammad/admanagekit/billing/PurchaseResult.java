package com.i2hammad.admanagekit.billing;

import java.util.List;


/**
 * Represents the result of a purchase transaction.
 */
public class PurchaseResult {
    private String orderId; // The unique order ID for the purchase
    private String packageName; // The package name of the application
    private List<String> productId; // List of product IDs associated with the purchase
    private long purchaseTime; // The time the purchase was made, in milliseconds since the epoch
    private int purchaseState; // The state of the purchase (e.g., purchased, pending, etc.)
    private String purchaseToken; // A unique token representing the purchase
    private int quantity; // The quantity of items purchased
    private boolean autoRenewing; // Indicates if the purchase is auto-renewing (for subscriptions)
    private boolean acknowledged; // Indicates if the purchase has been acknowledged

    /**
     * Constructs a new PurchaseResult instance.
     *
     * @param orderId       The unique order ID for the purchase.
     * @param packageName   The package name of the application.
     * @param productId     List of product IDs associated with the purchase.
     * @param purchaseTime  The time the purchase was made, in milliseconds since the epoch.
     * @param purchaseState The state of the purchase.
     * @param purchaseToken A unique token representing the purchase.
     * @param quantity      The quantity of items purchased.
     * @param autoRenewing  Whether the purchase is auto-renewing.
     * @param acknowledged  Whether the purchase has been acknowledged.
     */
    public PurchaseResult(String orderId, String packageName, List<String> productId, long purchaseTime, int purchaseState, String purchaseToken, int quantity, boolean autoRenewing, boolean acknowledged) {
        this.orderId = orderId;
        this.packageName = packageName;
        this.productId = productId;
        this.purchaseTime = purchaseTime;
        this.purchaseState = purchaseState;
        this.purchaseToken = purchaseToken;
        this.quantity = quantity;
        this.autoRenewing = autoRenewing;
        this.acknowledged = acknowledged;
    }

    /**
     * Gets the package name of the application.
     *
     * @return The package name.
     */
    public String getPackageName() {
        return this.packageName;
    }

    /**
     * Sets the package name of the application.
     *
     * @param packageName The package name to set.
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Gets the list of product IDs associated with the purchase.
     *
     * @return The list of product IDs.
     */
    public List<String> getProductId() {
        return this.productId;
    }

    /**
     * Sets the list of product IDs associated with the purchase.
     *
     * @param productId The list of product IDs to set.
     */
    public void setProductId(List<String> productId) {
        this.productId = productId;
    }

    /**
     * Gets the state of the purchase.
     *
     * @return The purchase state.
     */
    public int getPurchaseState() {
        return this.purchaseState;
    }

    /**
     * Sets the state of the purchase.
     *
     * @param purchaseState The purchase state to set.
     */
    public void setPurchaseState(int purchaseState) {
        this.purchaseState = purchaseState;
    }

    /**
     * Checks if the purchase is auto-renewing.
     *
     * @return True if the purchase is auto-renewing, false otherwise.
     */
    public boolean isAutoRenewing() {
        return this.autoRenewing;
    }

    /**
     * Sets whether the purchase is auto-renewing.
     *
     * @param autoRenewing True if the purchase should be auto-renewing, false otherwise.
     */
    public void setAutoRenewing(boolean autoRenewing) {
        this.autoRenewing = autoRenewing;
    }

    /**
     * Gets the order ID for the purchase.
     *
     * @return The order ID.
     */
    public String getOrderId() {
        return this.orderId;
    }

    /**
     * Sets the order ID for the purchase.
     *
     * @param orderId The order ID to set.
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Gets the time the purchase was made.
     *
     * @return The purchase time in milliseconds since the epoch.
     */
    public long getPurchaseTime() {
        return this.purchaseTime;
    }

    /**
     * Sets the time the purchase was made.
     *
     * @param purchaseTime The purchase time to set, in milliseconds since the epoch.
     */
    public void setPurchaseTime(long purchaseTime) {
        this.purchaseTime = purchaseTime;
    }

    /**
     * Gets the unique token representing the purchase.
     *
     * @return The purchase token.
     */
    public String getPurchaseToken() {
        return this.purchaseToken;
    }

    /**
     * Sets the unique token representing the purchase.
     *
     * @param purchaseToken The purchase token to set.
     */
    public void setPurchaseToken(String purchaseToken) {
        this.purchaseToken = purchaseToken;
    }

    /**
     * Gets the quantity of items purchased.
     *
     * @return The quantity of items purchased.
     */
    public int getQuantity() {
        return this.quantity;
    }

    /**
     * Sets the quantity of items purchased.
     *
     * @param quantity The quantity of items to set.
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Checks if the purchase has been acknowledged.
     *
     * @return True if the purchase has been acknowledged, false otherwise.
     */
    public boolean isAcknowledged() {
        return this.acknowledged;
    }

    /**
     * Sets whether the purchase has been acknowledged.
     *
     * @param acknowledged True if the purchase should be acknowledged, false otherwise.
     */
    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }
}
