package com.i2hammad.admanagekit.billing

/**
 * Represents an item that can be purchased, including its ID, trial ID, type, and purchase category.
 */
class PurchaseItem {
    /**
     * The unique identifier for the purchase item.
     */
    @JvmField
    var itemId: String

    /**
     * The trial ID associated with the purchase, if applicable (for subscriptions).
     */
    @JvmField
    var trialId: String? = null

    /**
     * The type of the purchase item (TYPE_IAP.PURCHASE or TYPE_IAP.SUBSCRIPTION).
     */
    @JvmField
    var type: Int

    /**
     * Whether this item is consumable and can be purchased multiple times.
     * Consumable items: coins, gems, credits.
     * Non-consumable items: remove ads, premium upgrade, lifetime access.
     */
    @JvmField
    var isConsumable: Boolean = false

    /**
     * The category of this purchase for more specific classification.
     * Use this to distinguish between different non-consumable types.
     */
    @JvmField
    var category: PurchaseCategory = PurchaseCategory.LIFETIME_PREMIUM

    /**
     * Purchase categories for INAPP products.
     */
    enum class PurchaseCategory {
        /** Consumable items like coins, gems, credits */
        CONSUMABLE,
        /** One-time feature unlock (e.g., unlock level pack, remove watermark) */
        FEATURE_UNLOCK,
        /** Lifetime premium - acts like a permanent subscription, disables all ads */
        LIFETIME_PREMIUM,
        /** Remove ads only - one-time purchase to remove ads */
        REMOVE_ADS
    }

    /**
     * Constructs a new PurchaseItem with the specified item ID and type.
     * Default: non-consumable, LIFETIME_PREMIUM category.
     */
    constructor(itemId: String, type: Int) {
        this.itemId = itemId
        this.type = type
        this.isConsumable = false
        this.category = PurchaseCategory.LIFETIME_PREMIUM
    }

    /**
     * Constructs a new PurchaseItem with the specified item ID, trial ID, and type.
     * Default: non-consumable, LIFETIME_PREMIUM category.
     */
    constructor(itemId: String, trialId: String?, type: Int) {
        this.itemId = itemId
        this.trialId = trialId
        this.type = type
        this.isConsumable = false
        this.category = PurchaseCategory.LIFETIME_PREMIUM
    }

    /**
     * Constructs a new PurchaseItem for INAPP with consumable configuration.
     */
    constructor(itemId: String, type: Int, isConsumable: Boolean) {
        this.itemId = itemId
        this.type = type
        this.isConsumable = isConsumable
        this.category = if (isConsumable) PurchaseCategory.CONSUMABLE else PurchaseCategory.FEATURE_UNLOCK
    }

    /**
     * Constructs a new PurchaseItem with full configuration including category.
     *
     * @param itemId   The unique identifier for the purchase item.
     * @param trialId  The trial ID associated with the purchase (null for non-subscriptions).
     * @param type     The type of the purchase item.
     * @param category The purchase category for classification.
     */
    constructor(itemId: String, trialId: String?, type: Int, category: PurchaseCategory) {
        this.itemId = itemId
        this.trialId = trialId
        this.type = type
        this.category = category
        this.isConsumable = (category == PurchaseCategory.CONSUMABLE)
    }

    /**
     * Constructs a new PurchaseItem for INAPP with category.
     *
     * @param itemId   The unique identifier for the purchase item.
     * @param type     The type of the purchase item.
     * @param category The purchase category for classification.
     */
    constructor(itemId: String, type: Int, category: PurchaseCategory) {
        this.itemId = itemId
        this.type = type
        this.category = category
        this.isConsumable = (category == PurchaseCategory.CONSUMABLE)
    }

    /**
     * Checks if this purchase should disable ads.
     * Returns true for LIFETIME_PREMIUM, REMOVE_ADS, and subscriptions.
     */
    fun shouldDisableAds(): Boolean {
        return category == PurchaseCategory.LIFETIME_PREMIUM ||
                category == PurchaseCategory.REMOVE_ADS ||
                type == AppPurchase.TYPE_IAP.SUBSCRIPTION
    }

    /**
     * Checks if this is a lifetime/permanent purchase (not a subscription).
     */
    fun isLifetimePurchase(): Boolean {
        return type == AppPurchase.TYPE_IAP.PURCHASE &&
                (category == PurchaseCategory.LIFETIME_PREMIUM || category == PurchaseCategory.REMOVE_ADS)
    }
}
