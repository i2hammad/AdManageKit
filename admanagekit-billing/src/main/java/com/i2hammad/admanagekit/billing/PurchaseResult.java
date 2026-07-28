package com.i2hammad.admanagekit.billing;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AccountIdentifiers;
import com.android.billingclient.api.Purchase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Represents the result of a purchase transaction with comprehensive details.
 * This class wraps all information from Google Play Billing Library's Purchase object.
 */
public class PurchaseResult {

    // Core purchase details
    private String orderId;
    private String packageName;
    private List<String> productIds;
    private long purchaseTime;
    private int purchaseState;
    private String purchaseToken;
    private int quantity;
    private boolean autoRenewing;
    private boolean acknowledged;

    // Additional details from Billing Library v8
    private String originalJson;
    private String signature;
    private String obfuscatedAccountId;
    private String obfuscatedProfileId;

    // Custom tracking fields
    private boolean isConsumed;
    private long consumedTime;
    private String productType; // "inapp" or "subs"

    // Subscription expiry (from server-side verification)
    private long expiryTime;
    private boolean expiryVerified;

    // Account hold, reported client-side by Play Billing 9
    private boolean suspended;

    // Pending subscription change awaiting payment (Play Billing 9)
    private List<String> pendingProductIds;
    private String pendingPurchaseToken;

    /**
     * Purchase states from Google Play Billing.
     */
    public static class State {
        /** Purchase is completed and payment is received */
        public static final int PURCHASED = Purchase.PurchaseState.PURCHASED;
        /** Purchase is pending (e.g., waiting for payment approval) */
        public static final int PENDING = Purchase.PurchaseState.PENDING;
        /** Purchase state is unspecified */
        public static final int UNSPECIFIED = Purchase.PurchaseState.UNSPECIFIED_STATE;
    }

    /**
     * Creates a PurchaseResult from a Google Play Purchase object.
     *
     * @param purchase The Purchase object from Google Play Billing.
     * @return A new PurchaseResult with all available details.
     */
    @NonNull
    public static PurchaseResult fromPurchase(@NonNull Purchase purchase) {
        PurchaseResult result = new PurchaseResult();
        result.orderId = purchase.getOrderId();
        result.packageName = purchase.getPackageName();
        result.productIds = purchase.getProducts();
        result.purchaseTime = purchase.getPurchaseTime();
        result.purchaseState = purchase.getPurchaseState();
        result.purchaseToken = purchase.getPurchaseToken();
        result.quantity = purchase.getQuantity();
        result.autoRenewing = purchase.isAutoRenewing();
        result.acknowledged = purchase.isAcknowledged();
        result.originalJson = purchase.getOriginalJson();
        result.signature = purchase.getSignature();
        result.suspended = purchase.isSuspended();

        // A pending upgrade/downgrade the user started but has not paid for yet.
        Purchase.PendingPurchaseUpdate pendingUpdate = purchase.getPendingPurchaseUpdate();
        if (pendingUpdate != null) {
            result.pendingProductIds = pendingUpdate.getProducts();
            result.pendingPurchaseToken = pendingUpdate.getPurchaseToken();
        }

        // Get account identifiers if available
        AccountIdentifiers accountIdentifiers = purchase.getAccountIdentifiers();
        if (accountIdentifiers != null) {
            result.obfuscatedAccountId = accountIdentifiers.getObfuscatedAccountId();
            result.obfuscatedProfileId = accountIdentifiers.getObfuscatedProfileId();
        }

        result.isConsumed = false;
        result.consumedTime = 0;

        return result;
    }

    /**
     * Default constructor for manual creation.
     */
    public PurchaseResult() {
    }

    /**
     * Legacy constructor for backward compatibility.
     */
    public PurchaseResult(String orderId, String packageName, List<String> productIds,
                          long purchaseTime, int purchaseState, String purchaseToken,
                          int quantity, boolean autoRenewing, boolean acknowledged) {
        this.orderId = orderId;
        this.packageName = packageName;
        this.productIds = productIds;
        this.purchaseTime = purchaseTime;
        this.purchaseState = purchaseState;
        this.purchaseToken = purchaseToken;
        this.quantity = quantity;
        this.autoRenewing = autoRenewing;
        this.acknowledged = acknowledged;
    }

    /**
     * Full constructor with all fields.
     */
    public PurchaseResult(String orderId, String packageName, List<String> productIds,
                          long purchaseTime, int purchaseState, String purchaseToken,
                          int quantity, boolean autoRenewing, boolean acknowledged,
                          String originalJson, String signature,
                          String obfuscatedAccountId, String obfuscatedProfileId) {
        this.orderId = orderId;
        this.packageName = packageName;
        this.productIds = productIds;
        this.purchaseTime = purchaseTime;
        this.purchaseState = purchaseState;
        this.purchaseToken = purchaseToken;
        this.quantity = quantity;
        this.autoRenewing = autoRenewing;
        this.acknowledged = acknowledged;
        this.originalJson = originalJson;
        this.signature = signature;
        this.obfuscatedAccountId = obfuscatedAccountId;
        this.obfuscatedProfileId = obfuscatedProfileId;
    }

    // ==================== State Helper Methods ====================

    /**
     * Checks if the purchase is in PURCHASED state.
     */
    public boolean isPurchased() {
        return purchaseState == State.PURCHASED;
    }

    /**
     * Checks if the purchase is in PENDING state.
     */
    public boolean isPending() {
        return purchaseState == State.PENDING;
    }

    /**
     * Gets the purchase state as a readable string.
     */
    @NonNull
    public String getPurchaseStateString() {
        switch (purchaseState) {
            case State.PURCHASED:
                return "PURCHASED";
            case State.PENDING:
                return "PENDING";
            default:
                return "UNSPECIFIED";
        }
    }

    // ==================== Product Helper Methods ====================

    /**
     * Gets the first (primary) product ID.
     */
    @Nullable
    public String getFirstProductId() {
        return (productIds != null && !productIds.isEmpty()) ? productIds.get(0) : null;
    }

    /**
     * Checks if this purchase contains a specific product ID.
     */
    public boolean containsProduct(String productId) {
        return productIds != null && productIds.contains(productId);
    }

    /**
     * Gets the product IDs as a comma-separated string.
     */
    @NonNull
    public String getProductIdsString() {
        if (productIds == null || productIds.isEmpty()) {
            return "";
        }
        return TextUtils.join(", ", productIds);
    }

    // ==================== Date Helper Methods ====================

    /**
     * Gets the purchase time as a formatted date string.
     *
     * @param pattern The date format pattern (e.g., "yyyy-MM-dd HH:mm:ss").
     * @return The formatted date string.
     */
    @NonNull
    public String getPurchaseTimeFormatted(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(new Date(purchaseTime));
    }

    /**
     * Gets the purchase time as a formatted date string in default format.
     */
    @NonNull
    public String getPurchaseTimeFormatted() {
        return getPurchaseTimeFormatted("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Gets the purchase time as a Date object.
     */
    @NonNull
    public Date getPurchaseDate() {
        return new Date(purchaseTime);
    }

    /**
     * Gets the consumed time as a formatted date string.
     */
    @NonNull
    public String getConsumedTimeFormatted() {
        if (consumedTime <= 0) {
            return "Not consumed";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(consumedTime));
    }

    // ==================== Consumption Methods ====================

    /**
     * Marks this purchase as consumed.
     */
    public void markAsConsumed() {
        this.isConsumed = true;
        this.consumedTime = System.currentTimeMillis();
    }

    /**
     * Checks if this purchase has been consumed.
     */
    public boolean isConsumed() {
        return isConsumed;
    }

    /**
     * Gets the time when this purchase was consumed.
     */
    public long getConsumedTime() {
        return consumedTime;
    }

    // ==================== Verification Helper Methods ====================

    /**
     * Checks if this purchase has the necessary data for server-side verification.
     */
    public boolean hasVerificationData() {
        return !TextUtils.isEmpty(originalJson) && !TextUtils.isEmpty(signature);
    }

    /**
     * Gets the original JSON for server-side verification.
     */
    @Nullable
    public String getOriginalJson() {
        return originalJson;
    }

    /**
     * Gets the signature for server-side verification.
     */
    @Nullable
    public String getSignature() {
        return signature;
    }

    // ==================== Account Identifier Methods ====================

    /**
     * Gets the obfuscated account ID set during purchase.
     */
    @Nullable
    public String getObfuscatedAccountId() {
        return obfuscatedAccountId;
    }

    /**
     * Gets the obfuscated profile ID set during purchase.
     */
    @Nullable
    public String getObfuscatedProfileId() {
        return obfuscatedProfileId;
    }

    /**
     * Checks if account identifiers are available.
     */
    public boolean hasAccountIdentifiers() {
        return !TextUtils.isEmpty(obfuscatedAccountId) || !TextUtils.isEmpty(obfuscatedProfileId);
    }

    // ==================== Standard Getters and Setters ====================

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * @deprecated Use {@link #getProductIds()} instead.
     */
    @Deprecated
    public List<String> getProductId() {
        return productIds;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }

    /**
     * @deprecated Use {@link #setProductIds(List)} instead.
     */
    @Deprecated
    public void setProductId(List<String> productIds) {
        this.productIds = productIds;
    }

    public long getPurchaseTime() {
        return purchaseTime;
    }

    public void setPurchaseTime(long purchaseTime) {
        this.purchaseTime = purchaseTime;
    }

    public int getPurchaseState() {
        return purchaseState;
    }

    public void setPurchaseState(int purchaseState) {
        this.purchaseState = purchaseState;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }

    public void setPurchaseToken(String purchaseToken) {
        this.purchaseToken = purchaseToken;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isAutoRenewing() {
        return autoRenewing;
    }

    public void setAutoRenewing(boolean autoRenewing) {
        this.autoRenewing = autoRenewing;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public void setOriginalJson(String originalJson) {
        this.originalJson = originalJson;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setObfuscatedAccountId(String obfuscatedAccountId) {
        this.obfuscatedAccountId = obfuscatedAccountId;
    }

    public void setObfuscatedProfileId(String obfuscatedProfileId) {
        this.obfuscatedProfileId = obfuscatedProfileId;
    }

    public void setConsumed(boolean consumed) {
        this.isConsumed = consumed;
    }

    public void setConsumedTime(long consumedTime) {
        this.consumedTime = consumedTime;
    }

    @Nullable
    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    /**
     * Checks if this is a subscription purchase.
     */
    public boolean isSubscription() {
        return "subs".equals(productType);
    }

    /**
     * Checks if this is an in-app (one-time) purchase.
     */
    public boolean isInApp() {
        return "inapp".equals(productType);
    }

    // ==================== Subscription State Methods ====================

    /**
     * Subscription lifecycle states.
     * Note: Some states (GRACE_PERIOD, ON_HOLD, PAUSED) require server-side
     * verification using Google Play Developer API as they're not available client-side.
     */
    public enum SubscriptionState {
        /** Subscription is active and auto-renewing */
        ACTIVE,
        /** User cancelled but still has access until expiration */
        CANCELLED,
        /** Payment issue but still has access (requires server-side check) */
        GRACE_PERIOD,
        /** Payment issue, no access (requires server-side check) */
        ON_HOLD,
        /** User paused subscription (requires server-side check) */
        PAUSED,
        /** Subscription expired, no access */
        EXPIRED,
        /** Not a subscription or state unknown */
        NOT_SUBSCRIPTION
    }

    /**
     * Gets the subscription state based on available client-side data.
     *
     * <p><b>Important:</b> This only provides ACTIVE, CANCELLED, or NOT_SUBSCRIPTION states.
     * For GRACE_PERIOD, ON_HOLD, PAUSED states, you need server-side verification
     * using Google Play Developer API.</p>
     *
     * @return The subscription state based on client-side data.
     */
    public SubscriptionState getSubscriptionState() {
        if (!isSubscription()) {
            return SubscriptionState.NOT_SUBSCRIPTION;
        }

        // Account hold: Play reports this client-side from Billing 9 onward, so it
        // no longer needs a server round-trip. The user has no access until they
        // fix their payment method.
        if (suspended) {
            return SubscriptionState.ON_HOLD;
        }

        // If we have a valid purchase in the list, it's either active or cancelled
        if (isPurchased() && acknowledged) {
            if (autoRenewing) {
                return SubscriptionState.ACTIVE;
            } else {
                // Not auto-renewing means user cancelled, but still has access
                return SubscriptionState.CANCELLED;
            }
        }

        // If pending, treat as active (payment processing)
        if (isPending()) {
            return SubscriptionState.ACTIVE;
        }

        return SubscriptionState.EXPIRED;
    }

    /**
     * Checks if the subscription is active (user has access).
     * This includes ACTIVE and CANCELLED states (cancelled users still have access until expiration).
     */
    public boolean isSubscriptionActive() {
        SubscriptionState state = getSubscriptionState();
        return state == SubscriptionState.ACTIVE || state == SubscriptionState.CANCELLED;
    }

    /**
     * Checks if the subscription will renew.
     * Returns false if user cancelled (even if still has access).
     */
    public boolean willSubscriptionRenew() {
        return isSubscription() && autoRenewing;
    }

    /**
     * Checks if the user cancelled their subscription.
     * Note: Cancelled users still have access until expiration date.
     */
    public boolean isSubscriptionCancelled() {
        return getSubscriptionState() == SubscriptionState.CANCELLED;
    }

    /**
     * Whether this subscription is in <b>account hold</b> — Google Play could not
     * charge the user's payment method and has suspended the subscription.
     *
     * <p>The user keeps the subscription record but must not receive premium
     * access until they fix payment. Call
     * {@link AppPurchase#showInAppMessages(android.app.Activity, InAppMessageListener)}
     * to let Play walk them through recovery.
     *
     * <p>Populated from Play Billing 9's client-side signal; earlier versions of
     * this library required server-side verification to detect it.
     *
     * @return true when the subscription is on hold.
     * @since 4.4.0
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Sets the account-hold flag. Populated automatically from the Play purchase.
     *
     * @since 4.4.0
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * Whether the user has started a subscription change (upgrade or downgrade)
     * that has not been paid for yet.
     *
     * <p>Until it completes, the <i>current</i> plan remains the entitlement in
     * force. Use this to show "plan change pending" rather than switching the UI
     * to the new tier prematurely.
     *
     * @return true when a pending change exists.
     * @since 4.4.0
     */
    public boolean hasPendingPurchaseUpdate() {
        return pendingPurchaseToken != null && !pendingPurchaseToken.isEmpty();
    }

    /**
     * Product ids of the pending subscription change, or {@code null} when there
     * is none. See {@link #hasPendingPurchaseUpdate()}.
     *
     * @since 4.4.0
     */
    @Nullable
    public List<String> getPendingProductIds() {
        return pendingProductIds;
    }

    /**
     * Purchase token of the pending subscription change, or {@code null} when
     * there is none. See {@link #hasPendingPurchaseUpdate()}.
     *
     * @since 4.4.0
     */
    @Nullable
    public String getPendingPurchaseToken() {
        return pendingPurchaseToken;
    }

    // ==================== Expiry Time Methods ====================

    /**
     * Gets the subscription expiry time in milliseconds.
     * This value is only available after server-side verification.
     *
     * @return Expiry time in milliseconds, or 0 if not verified.
     */
    public long getExpiryTime() {
        return expiryTime;
    }

    /**
     * Sets the subscription expiry time.
     * Call this after verifying the subscription with your backend server.
     *
     * @param expiryTime Expiry time in milliseconds since epoch.
     */
    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
        this.expiryVerified = true;
    }

    /**
     * Checks if the expiry time has been verified via server-side verification.
     */
    public boolean isExpiryVerified() {
        return expiryVerified;
    }

    /**
     * Gets the expiry time as a Date object.
     *
     * @return Expiry date, or null if not verified.
     */
    @Nullable
    public Date getExpiryDate() {
        if (!expiryVerified || expiryTime <= 0) {
            return null;
        }
        return new Date(expiryTime);
    }

    /**
     * Gets the expiry time as a formatted string.
     *
     * @param pattern The date format pattern (e.g., "yyyy-MM-dd HH:mm:ss").
     * @return Formatted expiry date, or "Not verified" if not available.
     */
    @NonNull
    public String getExpiryTimeFormatted(String pattern) {
        if (!expiryVerified || expiryTime <= 0) {
            return "Not verified";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(new Date(expiryTime));
    }

    /**
     * Gets the expiry time as a formatted string in default format.
     */
    @NonNull
    public String getExpiryTimeFormatted() {
        return getExpiryTimeFormatted("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Checks if the subscription has expired based on verified expiry time.
     * Returns false if expiry time has not been verified.
     */
    public boolean isExpired() {
        if (!expiryVerified || expiryTime <= 0) {
            return false;
        }
        return System.currentTimeMillis() > expiryTime;
    }

    /**
     * Gets the remaining time until expiry in milliseconds.
     *
     * @return Remaining time in milliseconds, 0 if expired, or -1 if not verified.
     */
    public long getRemainingTime() {
        if (!expiryVerified || expiryTime <= 0) {
            return -1;
        }
        long remaining = expiryTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * Gets the remaining days until expiry.
     *
     * @return Remaining days, 0 if expired, or -1 if not verified.
     */
    public int getRemainingDays() {
        long remaining = getRemainingTime();
        if (remaining < 0) {
            return -1;
        }
        return (int) (remaining / (24 * 60 * 60 * 1000));
    }

    /**
     * Gets a human-readable subscription status string.
     */
    @NonNull
    public String getSubscriptionStateString() {
        switch (getSubscriptionState()) {
            case ACTIVE:
                return "Active";
            case CANCELLED:
                return "Cancelled (access until expiration)";
            case GRACE_PERIOD:
                return "Grace Period";
            case ON_HOLD:
                return "On Hold";
            case PAUSED:
                return "Paused";
            case EXPIRED:
                return "Expired";
            default:
                return "Not a subscription";
        }
    }

    // ==================== Object Methods ====================

    @NonNull
    @Override
    public String toString() {
        return "PurchaseResult{" +
                "orderId='" + orderId + '\'' +
                ", products=" + getProductIdsString() +
                ", state=" + getPurchaseStateString() +
                ", quantity=" + quantity +
                ", time=" + getPurchaseTimeFormatted() +
                ", acknowledged=" + acknowledged +
                ", autoRenewing=" + autoRenewing +
                ", consumed=" + isConsumed +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurchaseResult that = (PurchaseResult) o;
        return orderId != null ? orderId.equals(that.orderId) : that.orderId == null;
    }

    @Override
    public int hashCode() {
        return orderId != null ? orderId.hashCode() : 0;
    }
}
