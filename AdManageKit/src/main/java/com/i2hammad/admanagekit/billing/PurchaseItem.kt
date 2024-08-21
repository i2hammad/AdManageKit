package com.i2hammad.admanagekit.billing

/**
 * Represents an item that can be purchased, including its ID, trial ID, and type.
 */
class PurchaseItem {
    /**
     * Gets the unique identifier for the purchase item.
     *
     * @return The item ID.
     */
    /**
     * Sets the unique identifier for the purchase item.
     *
     * @param itemId The item ID to set.
     */
    @JvmField
    var itemId: String // The unique identifier for the purchase item
    /**
     * Gets the trial ID associated with the purchase.
     *
     * @return The trial ID.
     */
    /**
     * Sets the trial ID associated with the purchase.
     *
     * @param trialId The trial ID to set.
     */
    @JvmField
    var trialId: String? = null // The trial ID associated with the purchase, if applicable
    /**
     * Gets the type of the purchase item.
     *
     * @return The type of the purchase item.
     */
    /**
     * Sets the type of the purchase item.
     *
     * @param type The type to set.
     */
    @JvmField
    var type: Int // The type of the purchase item (e.g., consumable, non-consumable, subscription)

    /**
     * Constructs a new PurchaseItem with the specified item ID and type.
     *
     * @param itemId The unique identifier for the purchase item.
     * @param type   The type of the purchase item.
     */
    constructor(itemId: String, type: Int) {
        this.itemId = itemId
        this.type = type
    }

    /**
     * Constructs a new PurchaseItem with the specified item ID, trial ID, and type.
     *
     * @param itemId  The unique identifier for the purchase item.
     * @param trialId The trial ID associated with the purchase.
     * @param type    The type of the purchase item.
     */
    constructor(itemId: String, trialId: String?, type: Int) {
        this.itemId = itemId
        this.trialId = trialId
        this.type = type
    }
}
