package com.i2hammad.admanagekit.billing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Callback interface for server-side subscription verification.
 *
 * <p>Implement this interface to verify subscriptions with your backend server.
 * Your backend should use the Google Play Developer API to fetch subscription details.</p>
 *
 * <p>Example backend endpoint:</p>
 * <pre>
 * GET https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{packageName}/purchases/subscriptionsv2/tokens/{token}
 * </pre>
 *
 * <p>Usage:</p>
 * <pre>
 * AppPurchase.getInstance().setSubscriptionVerificationCallback(new SubscriptionVerificationCallback() {
 *     &#64;Override
 *     public void verifySubscription(String packageName, String subscriptionId, String purchaseToken,
 *                                    VerificationResultListener listener) {
 *         // Call your backend API
 *         yourApi.verifySubscription(packageName, subscriptionId, purchaseToken, new Callback() {
 *             &#64;Override
 *             public void onSuccess(SubscriptionDetails details) {
 *                 listener.onVerified(details);
 *             }
 *
 *             &#64;Override
 *             public void onError(String error) {
 *                 listener.onVerificationFailed(error);
 *             }
 *         });
 *     }
 * });
 * </pre>
 */
public interface SubscriptionVerificationCallback {

    /**
     * Called when subscription verification is needed.
     *
     * <p>Implement this method to call your backend server and verify the subscription
     * using Google Play Developer API. Your server should return the subscription details
     * including the expiry time.</p>
     *
     * @param packageName    The package name of the app.
     * @param subscriptionId The subscription product ID.
     * @param purchaseToken  The purchase token from Google Play.
     * @param listener       Callback to report verification results.
     */
    void verifySubscription(@NonNull String packageName,
                            @NonNull String subscriptionId,
                            @NonNull String purchaseToken,
                            @NonNull VerificationResultListener listener);

    /**
     * Listener for verification results.
     */
    interface VerificationResultListener {
        /**
         * Called when verification succeeds.
         *
         * @param details The verified subscription details.
         */
        void onVerified(@NonNull SubscriptionDetails details);

        /**
         * Called when verification fails.
         *
         * @param errorMessage Description of the error.
         */
        void onVerificationFailed(@Nullable String errorMessage);
    }

    /**
     * Subscription details returned from server-side verification.
     */
    class SubscriptionDetails {
        private final long expiryTimeMillis;
        private final String subscriptionState;
        private final boolean isAutoRenewing;
        private final String linkedPurchaseToken;
        private final long startTimeMillis;

        private SubscriptionDetails(Builder builder) {
            this.expiryTimeMillis = builder.expiryTimeMillis;
            this.subscriptionState = builder.subscriptionState;
            this.isAutoRenewing = builder.isAutoRenewing;
            this.linkedPurchaseToken = builder.linkedPurchaseToken;
            this.startTimeMillis = builder.startTimeMillis;
        }

        /**
         * Gets the subscription expiry time in milliseconds since epoch.
         */
        public long getExpiryTimeMillis() {
            return expiryTimeMillis;
        }

        /**
         * Gets the subscription state from Google Play.
         * Possible values: SUBSCRIPTION_STATE_ACTIVE, SUBSCRIPTION_STATE_CANCELED,
         * SUBSCRIPTION_STATE_IN_GRACE_PERIOD, SUBSCRIPTION_STATE_ON_HOLD,
         * SUBSCRIPTION_STATE_PAUSED, SUBSCRIPTION_STATE_EXPIRED
         */
        @Nullable
        public String getSubscriptionState() {
            return subscriptionState;
        }

        /**
         * Whether the subscription is set to auto-renew.
         */
        public boolean isAutoRenewing() {
            return isAutoRenewing;
        }

        /**
         * Gets the linked purchase token if this subscription was upgraded/downgraded.
         */
        @Nullable
        public String getLinkedPurchaseToken() {
            return linkedPurchaseToken;
        }

        /**
         * Gets the subscription start time in milliseconds since epoch.
         */
        public long getStartTimeMillis() {
            return startTimeMillis;
        }

        /**
         * Checks if the subscription has expired.
         */
        public boolean isExpired() {
            return expiryTimeMillis > 0 && System.currentTimeMillis() > expiryTimeMillis;
        }

        /**
         * Builder for SubscriptionDetails.
         */
        public static class Builder {
            private long expiryTimeMillis;
            private String subscriptionState;
            private boolean isAutoRenewing;
            private String linkedPurchaseToken;
            private long startTimeMillis;

            public Builder setExpiryTimeMillis(long expiryTimeMillis) {
                this.expiryTimeMillis = expiryTimeMillis;
                return this;
            }

            public Builder setSubscriptionState(String subscriptionState) {
                this.subscriptionState = subscriptionState;
                return this;
            }

            public Builder setAutoRenewing(boolean autoRenewing) {
                this.isAutoRenewing = autoRenewing;
                return this;
            }

            public Builder setLinkedPurchaseToken(String linkedPurchaseToken) {
                this.linkedPurchaseToken = linkedPurchaseToken;
                return this;
            }

            public Builder setStartTimeMillis(long startTimeMillis) {
                this.startTimeMillis = startTimeMillis;
                return this;
            }

            public SubscriptionDetails build() {
                return new SubscriptionDetails(this);
            }
        }
    }
}
