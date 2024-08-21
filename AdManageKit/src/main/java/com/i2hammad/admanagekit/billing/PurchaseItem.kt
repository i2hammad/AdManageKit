package com.i2hammad.admanagekit.billing;

/**
 * Represents an item that can be purchased, including its ID, trial ID, and type.
 */
public class PurchaseItem {
    private String itemId; // The unique identifier for the purchase item
    private String trialId; // The trial ID associated with the purchase, if applicable
    private int type; // The type of the purchase item (e.g., consumable, non-consumable, subscription)

    /**
     * Constructs a new PurchaseItem with the specified item ID and type.
     *
     * @param itemId The unique identifier for the purchase item.
     * @param type   The type of the purchase item.
     */
    public PurchaseItem(String itemId, int type) {
        this.itemId = itemId;
        this.type = type;
    }

    /**
     * Constructs a new PurchaseItem with the specified item ID, trial ID, and type.
     *
     * @param itemId  The unique identifier for the purchase item.
     * @param trialId The trial ID associated with the purchase.
     * @param type    The type of the purchase item.
     */
    public PurchaseItem(String itemId, String trialId, int type) {
        this.itemId = itemId;
        this.trialId = trialId;
        this.type = type;
    }

    /**
     * Gets the unique identifier for the purchase item.
     *
     * @return The item ID.
     */
    public String getItemId() {
        return this.itemId;
    }

    /**
     * Sets the unique identifier for the purchase item.
     *
     * @param itemId The item ID to set.
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    /**
     * Gets the trial ID associated with the purchase.
     *
     * @return The trial ID.
     */
    public String getTrialId() {
        return this.trialId;
    }

    /**
     * Sets the trial ID associated with the purchase.
     *
     * @param trialId The trial ID to set.
     */
    public void setTrialId(String trialId) {
        this.trialId = trialId;
    }

    /**
     * Gets the type of the purchase item.
     *
     * @return The type of the purchase item.
     */
    public int getType() {
        return this.type;
    }

    /**
     * Sets the type of the purchase item.
     *
     * @param type The type to set.
     */
    public void setType(int type) {
        this.type = type;
    }
}
