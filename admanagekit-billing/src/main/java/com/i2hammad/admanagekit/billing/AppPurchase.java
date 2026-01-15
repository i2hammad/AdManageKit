package com.i2hammad.admanagekit.billing;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.datatransport.BuildConfig;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppPurchase {

    private static final String Tag = "AppPurchase";
    public static final String PRODUCT_ID_TEST = "android.test.purchased";

    private static volatile AppPurchase appPurchase;

    private Application application;
    private BillingClient billingClient;
    private AtomicBoolean isServiceConnected = new AtomicBoolean(false);
    private PurchaseListener purchaseListener;
    private UpdatePurchaseListener updatePurchaseListener;
    private BillingListener billingListener;

    private Map<String, ProductDetails> productDetailsMap = new HashMap<>();
    private final Map<String, ProductDetails> inAppProductDetailsMap = new HashMap<>();
    private final Map<String, ProductDetails> subProductDetailsMap = new HashMap<>();

    public boolean isBillingAvailable;
    public Boolean isBillingInitialized = Boolean.FALSE;

    private List<PurchaseResult> purchaseResultList = new ArrayList<>();
    private List<String> stringList = new ArrayList<>();
    private List<PurchaseItem> purchaseItemList = new ArrayList<>();
    private ArrayList<QueryProductDetailsParams.Product> subProductArrayList = new ArrayList<>();
    private ArrayList<QueryProductDetailsParams.Product> inAppProductArrayList = new ArrayList<>();

    private Handler handler;
    private Runnable runnable;
    private String price = "2.89$";
    @Deprecated
    private boolean consumePurchase = false;
    private String oldPrice = "3.50$";

    private PurchaseHistoryListener purchaseHistoryListener;
    private SubscriptionVerificationCallback subscriptionVerificationCallback;
    private boolean isPurchased = false;
    private double discount = 1.0d;
    private String idPurchased = "";
    private String currentProductId = "";
    private int currentTypeIAP = 0;

    private AppPurchase() {
    }

    public static AppPurchase getInstance() {
        if (appPurchase == null) {
            synchronized (AppPurchase.class) {
                if (appPurchase == null) {
                    appPurchase = new AppPurchase();
                }
            }
        }
        return appPurchase;
    }

    public void initBilling(Application application, List<PurchaseItem> purchaseItemList) {
        if (BuildConfig.DEBUG) {
            purchaseItemList.add(new PurchaseItem(PRODUCT_ID_TEST, "", TYPE_IAP.PURCHASE));
        }
        this.application = application;
        this.purchaseItemList = purchaseItemList;
        addItemsToList(purchaseItemList);
        billingClient = BillingClient.newBuilder(application)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
        connectToGooglePlayBilling();
    }

    @Deprecated
    public void initBilling(Application application, List<String> listINAPId, List<String> listSubsId) {
        List<PurchaseItem> purchaseItems = new ArrayList<>();
        if (BuildConfig.DEBUG) {
            listINAPId.add(PRODUCT_ID_TEST);
        }
        for (String id : listINAPId) {
            purchaseItems.add(new PurchaseItem(id, "", TYPE_IAP.PURCHASE));
        }
        for (String id : listSubsId) {
            purchaseItems.add(new PurchaseItem(id, "", TYPE_IAP.SUBSCRIPTION));
        }
        initBilling(application, purchaseItems);
    }

    public void setBillingListener(BillingListener billingListener, int timeout) {
        Log.d(Tag, "setBillingListener: timeout " + timeout);
        this.billingListener = billingListener;
        if (this.isBillingAvailable) {
            Log.d(Tag, "setBillingListener: finish");
            billingListener.onInitBillingFinished(BillingClient.BillingResponseCode.OK);
            this.isBillingInitialized = Boolean.TRUE;
            return;
        }
        this.handler = new Handler();
        Runnable runnable = () -> {
            Log.d(Tag, "setBillingListener: timeout run");
            this.isBillingInitialized = Boolean.TRUE;
            billingListener.onInitBillingFinished(BillingClient.BillingResponseCode.SERVICE_TIMEOUT);
        };
        this.runnable = runnable;
        this.handler.postDelayed(runnable, timeout);
    }

    public void setBillingListener(BillingListener billingListener) {
        this.billingListener = billingListener;
        if (this.isBillingAvailable) {
            billingListener.onInitBillingFinished(BillingClient.BillingResponseCode.OK);
            this.isBillingInitialized = Boolean.TRUE;
        }
    }

    public void setPurchaseListener(PurchaseListener purchaseListener) {
        this.purchaseListener = purchaseListener;
    }

    public void setUpdatePurchaseListener(UpdatePurchaseListener listener) {
        this.updatePurchaseListener = listener;
    }

    public boolean isAvailable() {
        return isBillingAvailable && isServiceConnected.get();
    }

    public Boolean getInitBillingFinish() {
        return isBillingInitialized;
    }

    public void setEventConsumePurchaseTest(View view) {
        view.setOnClickListener(v -> {
            if (BuildConfig.DEBUG) {
                Log.d(Tag, "setEventConsumePurchaseTest: success");
                consumePurchase(PRODUCT_ID_TEST);
            }
        });
    }

    public void setPrice(String price) {
        this.price = price;
    }

    /**
     * @deprecated Use {@link PurchaseItem#isConsumable} per-product configuration instead.
     * Set isConsumable=true when creating PurchaseItem for consumable products.
     */
    @Deprecated
    public void setConsumePurchase(boolean consumePurchase) {
        this.consumePurchase = consumePurchase;
    }

    /**
     * Sets a listener to receive purchase history events.
     * Use this to persist purchase history in your app if needed.
     *
     * @param listener The listener to receive history events.
     */
    public void setPurchaseHistoryListener(PurchaseHistoryListener listener) {
        this.purchaseHistoryListener = listener;
    }

    /**
     * Sets a callback for server-side subscription verification.
     * This callback is used to verify subscriptions and retrieve expiry dates
     * from your backend server using Google Play Developer API.
     *
     * <p>Example:</p>
     * <pre>
     * AppPurchase.getInstance().setSubscriptionVerificationCallback((packageName, subscriptionId, purchaseToken, listener) -> {
     *     // Call your backend API
     *     yourApi.verifySubscription(packageName, subscriptionId, purchaseToken, new Callback() {
     *         public void onSuccess(long expiryTime) {
     *             SubscriptionVerificationCallback.SubscriptionDetails details =
     *                 new SubscriptionVerificationCallback.SubscriptionDetails.Builder()
     *                     .setExpiryTimeMillis(expiryTime)
     *                     .build();
     *             listener.onVerified(details);
     *         }
     *         public void onError(String error) {
     *             listener.onVerificationFailed(error);
     *         }
     *     });
     * });
     * </pre>
     *
     * @param callback The verification callback implementation.
     */
    public void setSubscriptionVerificationCallback(SubscriptionVerificationCallback callback) {
        this.subscriptionVerificationCallback = callback;
    }

    /**
     * Checks if a product is configured as consumable.
     *
     * @param productId The product ID to check.
     * @return true if the product is consumable, false otherwise.
     */
    private boolean isProductConsumable(String productId) {
        if (purchaseItemList == null) return consumePurchase; // fallback to legacy
        for (PurchaseItem item : purchaseItemList) {
            if (productId.equals(item.itemId)) {
                return item.isConsumable;
            }
        }
        return consumePurchase; // fallback to legacy global setting
    }

    public void setOldPrice(String oldPrice) {
        this.oldPrice = oldPrice;
    }

    public List<PurchaseResult> getOwnerIdSubs() {
        return purchaseResultList;
    }

    public List<String> getOwnerIdInapps() {
        return stringList;
    }

    public void setPurchase(boolean purchase) {
        this.isPurchased = purchase;
    }

    /**
     * Checks if user has any active purchase that should disable ads.
     * This includes:
     * - Active subscriptions
     * - Lifetime premium purchases
     * - Remove ads purchases
     *
     * Note: Consumable and feature unlock purchases do NOT affect this result.
     *
     * @return true if user has an ad-disabling purchase, false otherwise.
     */
    public boolean isPurchased() {
        // Check subscriptions
        if (!purchaseResultList.isEmpty()) {
            return true;
        }
        // Check for ad-disabling INAPP purchases only
        for (String productId : stringList) {
            PurchaseItem item = getPurchaseItem(productId);
            if (item != null && item.shouldDisableAds()) {
                return true;
            }
            // Fallback for items not in list - check if not consumable
            if (item == null && !isProductConsumable(productId)) {
                return true;
            }
        }
        // Fallback to manual flag
        return isPurchased;
    }

    public boolean isPurchased(Context context) {
        return isPurchased();
    }

    /**
     * Checks if a specific product is currently owned (purchased and not consumed).
     *
     * @param productId The product ID to check.
     * @return true if the product is owned, false otherwise.
     */
    public boolean isProductOwned(String productId) {
        // Check INAPP purchases
        if (stringList.contains(productId)) {
            return true;
        }
        // Check subscriptions
        for (PurchaseResult result : purchaseResultList) {
            if (result.getProductId().contains(productId)) {
                return true;
            }
        }
        return false;
    }

    public String getIdPurchased() {
        return idPurchased != null ? idPurchased : "";
    }

    public void verifyPurchased(boolean isCallback) {
        if (!isServiceConnected.get()) {
            Log.e(Tag, "Billing client not connected. Cannot verify purchases.");
            return;
        }
        if (inAppProductArrayList != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                    (billingResult, list) -> {
                        Log.d(Tag, "verifyPurchased INAPP code: " + billingResult.getResponseCode() + " === size: " + list.size());
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list) {
                                for (QueryProductDetailsParams.Product product : inAppProductArrayList) {
                                    if (purchase.getProducts().contains(product.zza())) {
                                        Log.d(Tag, "verifyPurchased INAPP: true");
                                        if (!stringList.contains(product.zza())) {
                                            stringList.add(product.zza());
                                        }
                                        isPurchased = true;
                                        idPurchased = product.zza();
                                    }
                                }
                            }
                            if (isCallback && billingListener != null) {
                                billingListener.onInitBillingFinished(billingResult.getResponseCode());
                            }
                            if (handler != null && runnable != null) {
                                handler.removeCallbacks(runnable);
                            }
                        }
                    }
            );
        }
        if (subProductArrayList != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                    (billingResult, list) -> {
                        Log.d(Tag, "verifyPurchased SUBS code: " + billingResult.getResponseCode() + " === size: " + list.size());
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list) {
                                for (QueryProductDetailsParams.Product product : subProductArrayList) {
                                    if (purchase.getProducts().contains(product.zza())) {
                                        Log.d(Tag, "verifyPurchased SUBS: true");
                                        handlePurchase(
                                                new PurchaseResult(
                                                        purchase.getOrderId(),
                                                        purchase.getPackageName(),
                                                        purchase.getProducts(),
                                                        purchase.getPurchaseTime(),
                                                        purchase.getPurchaseState(),
                                                        purchase.getPurchaseToken(),
                                                        purchase.getQuantity(),
                                                        purchase.isAutoRenewing(),
                                                        purchase.isAcknowledged()
                                                ),
                                                product.zza()
                                        );
                                        isPurchased = true;
                                        idPurchased = product.zza();
                                    }
                                }
                            }
                            if (isCallback && billingListener != null) {
                                billingListener.onInitBillingFinished(billingResult.getResponseCode());
                            }
                            if (handler != null && runnable != null) {
                                handler.removeCallbacks(runnable);
                            }
                        }
                    }
            );
        }
    }

    public void updatePurchaseStatus() {
        if (!isServiceConnected.get()) {
            Log.e(Tag, "Billing client not connected. Cannot update purchase status.");
            return;
        }
        if (inAppProductArrayList != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list) {
                                for (QueryProductDetailsParams.Product product : inAppProductArrayList) {
                                    if (purchase.getProducts().contains(product.zza()) && !stringList.contains(product.zza())) {
                                        stringList.add(product.zza());
                                        isPurchased = true;
                                        idPurchased = product.zza();
                                    }
                                }
                            }
                            if (updatePurchaseListener != null) {
                                updatePurchaseListener.onUpdateFinished();
                            }
                        }
                    }
            );
        }
        if (subProductArrayList != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list) {
                                for (QueryProductDetailsParams.Product product : subProductArrayList) {
                                    if (purchase.getProducts().contains(product.zza())) {
                                        handlePurchase(
                                                new PurchaseResult(
                                                        purchase.getOrderId(),
                                                        purchase.getPackageName(),
                                                        purchase.getProducts(),
                                                        purchase.getPurchaseTime(),
                                                        purchase.getPurchaseState(),
                                                        purchase.getPurchaseToken(),
                                                        purchase.getQuantity(),
                                                        purchase.isAutoRenewing(),
                                                        purchase.isAcknowledged()
                                                ),
                                                product.zza()
                                        );
                                        isPurchased = true;
                                        idPurchased = product.zza();
                                    }
                                }
                            }
                            if (updatePurchaseListener != null) {
                                updatePurchaseListener.onUpdateFinished();
                            }
                        }
                    }
            );
        }
    }

    @Deprecated
    public void purchase(Activity activity) {
        if (currentProductId == null || currentProductId.isEmpty()) {
            Log.e(Tag, "Purchase false: productId null");
            Toast.makeText(activity, "Product id must not be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        purchase(activity, currentProductId);
    }

    public String purchase(Activity activity, String productId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.e(Tag, "Invalid activity context");
            notifyListener("Invalid activity");
            return "Invalid activity";
        }
        if (productDetailsMap == null || productDetailsMap.isEmpty()) {
            Log.e(Tag, "Product details not initialized");
            notifyListener("Billing not initialized");
            return "Billing not initialized";
        }
        ProductDetails productDetails = productDetailsMap.get(productId);
        if (productDetails == null) {
            Log.e(Tag, "Product details not found: " + productId);
            notifyListener("Product not found");
            return "Product not found";
        }
        if (billingClient == null || !billingClient.isReady()) {
            Log.e(Tag, "BillingClient not ready");
            notifyListener("Billing client not ready");
            return "Billing client not ready";
        }
        if (BuildConfig.DEBUG) {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                new PurchaseDevBottomSheet(
                        TYPE_IAP.PURCHASE,
                        productDetails,
                        activity,
                        purchaseListener
                ).show();
            }
            return "Debug purchase simulated";
        }
        this.currentProductId = productId;
        this.currentTypeIAP = TYPE_IAP.PURCHASE;
        String offerToken = null;
        if (productDetails.getProductType().equals(BillingClient.ProductType.SUBS)) {
            List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
            if (offers != null && !offers.isEmpty()) {
                offerToken = findBestOfferToken(productId, offers);
            } else {
                Log.e(Tag, "No subscription offer details found for product: " + productId);
                notifyListener("No available offers");
                return "No subscription offers available";
            }
        } else {
            ProductDetails.OneTimePurchaseOfferDetails oneTimeOffer = productDetails.getOneTimePurchaseOfferDetails();
            if (oneTimeOffer != null) {
                offerToken = oneTimeOffer.getOfferToken();
            } else {
                Log.e(Tag, "No one-time purchase offer details found for product: " + productId);
                notifyListener("No purchase offer available");
                return "No purchase offer available";
            }
        }
        BillingFlowParams.ProductDetailsParams params = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build();
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(params))
                .build();
        BillingResult result = billingClient.launchBillingFlow(activity, billingFlowParams);
        return handleBillingResult(result);
    }

    public String subscribe(Activity activity, String subsId) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.e(Tag, "Invalid activity context");
            notifyListener("Invalid activity");
            return "Invalid activity context";
        }
        if (subProductDetailsMap == null || subProductDetailsMap.isEmpty()) {
            Log.e(Tag, "Subscription products not initialized");
            notifyListener("Billing not initialized");
            return "Billing not initialized";
        }
        if (BuildConfig.DEBUG) {
            ProductDetails productDetails = subProductDetailsMap.get(subsId);
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                new PurchaseDevBottomSheet(
                        TYPE_IAP.SUBSCRIPTION,
                        productDetails,
                        activity,
                        purchaseListener
                ).show();
            }
            return "Debug subscription simulated";
        }
        ProductDetails productDetails = subProductDetailsMap.get(subsId);
        if (productDetails == null) {
            Log.e(Tag, "Subscription not found: " + subsId);
            notifyListener("Subscription not available");
            return "Invalid subscription ID";
        }
        if (billingClient == null || !billingClient.isReady()) {
            Log.e(Tag, "BillingClient not ready");
            notifyListener("Billing service unavailable");
            return "Billing service unavailable";
        }
        List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
        if (offers == null || offers.isEmpty()) {
            Log.e(Tag, "No subscription offers found");
            notifyListener("No available offers");
            return "No subscription offers available";
        }
        String offerToken = findBestOfferToken(subsId, offers);
        BillingFlowParams.ProductDetailsParams params = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build();
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(params))
                .build();
        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        return handleBillingResult(result);
    }

    /**
     * Upgrades or downgrades a subscription.
     *
     * @param activity           The activity context.
     * @param newSubsId          The new subscription product ID.
     * @param oldPurchaseToken   The purchase token of the current subscription to replace.
     * @param replacementMode    The replacement mode (proration mode).
     * @return Result message.
     */
    public String updateSubscription(Activity activity, String newSubsId, String oldPurchaseToken,
                                      SubscriptionReplacementMode replacementMode) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.e(Tag, "Invalid activity context");
            notifyListener("Invalid activity");
            return "Invalid activity context";
        }
        if (subProductDetailsMap == null || subProductDetailsMap.isEmpty()) {
            Log.e(Tag, "Subscription products not initialized");
            notifyListener("Billing not initialized");
            return "Billing not initialized";
        }
        ProductDetails productDetails = subProductDetailsMap.get(newSubsId);
        if (productDetails == null) {
            Log.e(Tag, "Subscription not found: " + newSubsId);
            notifyListener("Subscription not available");
            return "Invalid subscription ID";
        }
        if (billingClient == null || !billingClient.isReady()) {
            Log.e(Tag, "BillingClient not ready");
            notifyListener("Billing service unavailable");
            return "Billing service unavailable";
        }
        List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
        if (offers == null || offers.isEmpty()) {
            Log.e(Tag, "No subscription offers found");
            notifyListener("No available offers");
            return "No subscription offers available";
        }
        String offerToken = findBestOfferToken(newSubsId, offers);

        // Build subscription update params
        BillingFlowParams.SubscriptionUpdateParams updateParams = BillingFlowParams.SubscriptionUpdateParams
                .newBuilder()
                .setOldPurchaseToken(oldPurchaseToken)
                .setSubscriptionReplacementMode(replacementMode.getMode())
                .build();

        BillingFlowParams.ProductDetailsParams params = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build();
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(params))
                .setSubscriptionUpdateParams(updateParams)
                .build();

        Log.d(Tag, "Updating subscription from token: " + oldPurchaseToken + " to: " + newSubsId);
        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        return handleBillingResult(result);
    }

    /**
     * Upgrades a subscription to a higher tier.
     * Uses CHARGE_PRORATED_PRICE mode - user pays the difference immediately.
     *
     * @param activity   The activity context.
     * @param newSubsId  The new (higher tier) subscription product ID.
     * @return Result message.
     */
    public String upgradeSubscription(Activity activity, String newSubsId) {
        PurchaseResult currentSub = getFirstActiveSubscription();
        if (currentSub == null) {
            Log.e(Tag, "No active subscription to upgrade");
            notifyListener("No active subscription");
            return "No active subscription to upgrade";
        }
        return updateSubscription(activity, newSubsId, currentSub.getPurchaseToken(),
                SubscriptionReplacementMode.CHARGE_PRORATED_PRICE);
    }

    /**
     * Downgrades a subscription to a lower tier.
     * Uses DEFERRED mode - change takes effect at next renewal.
     *
     * @param activity   The activity context.
     * @param newSubsId  The new (lower tier) subscription product ID.
     * @return Result message.
     */
    public String downgradeSubscription(Activity activity, String newSubsId) {
        PurchaseResult currentSub = getFirstActiveSubscription();
        if (currentSub == null) {
            Log.e(Tag, "No active subscription to downgrade");
            notifyListener("No active subscription");
            return "No active subscription to downgrade";
        }
        return updateSubscription(activity, newSubsId, currentSub.getPurchaseToken(),
                SubscriptionReplacementMode.DEFERRED);
    }

    /**
     * Upgrades or downgrades from a specific subscription.
     *
     * @param activity         The activity context.
     * @param currentSubsId    The current subscription product ID to replace.
     * @param newSubsId        The new subscription product ID.
     * @param replacementMode  The replacement mode.
     * @return Result message.
     */
    public String changeSubscription(Activity activity, String currentSubsId, String newSubsId,
                                      SubscriptionReplacementMode replacementMode) {
        PurchaseResult currentSub = getSubscription(currentSubsId);
        if (currentSub == null) {
            Log.e(Tag, "Subscription not found: " + currentSubsId);
            notifyListener("Subscription not found");
            return "Current subscription not found";
        }
        return updateSubscription(activity, newSubsId, currentSub.getPurchaseToken(), replacementMode);
    }

    /**
     * Gets the first active subscription.
     */
    @Nullable
    private PurchaseResult getFirstActiveSubscription() {
        if (purchaseResultList.isEmpty()) {
            return null;
        }
        return purchaseResultList.get(0);
    }

    /**
     * Subscription replacement modes for upgrade/downgrade.
     */
    public enum SubscriptionReplacementMode {
        /**
         * The new subscription takes effect immediately, and the user is charged full price.
         * The remaining value from the old subscription is prorated for time.
         */
        WITH_TIME_PRORATION(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION),

        /**
         * The new subscription takes effect immediately, and the user is charged the price
         * difference (upgrade only). Best for upgrades.
         */
        CHARGE_PRORATED_PRICE(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE),

        /**
         * The new subscription takes effect immediately with full price charged.
         * Best when you want immediate revenue.
         */
        CHARGE_FULL_PRICE(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE),

        /**
         * The new subscription takes effect at the next renewal date.
         * User keeps current subscription until then. Best for downgrades.
         */
        DEFERRED(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED),

        /**
         * The new subscription takes effect immediately with no proration.
         * The billing date remains the same.
         */
        WITHOUT_PRORATION(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION);

        private final int mode;

        SubscriptionReplacementMode(int mode) {
            this.mode = mode;
        }

        public int getMode() {
            return mode;
        }
    }

    private String findBestOfferToken(String subsId, List<ProductDetails.SubscriptionOfferDetails> offers) {
        String trialId = null;
        if (purchaseItemList != null) {
            for (PurchaseItem item : purchaseItemList) {
                if (subsId.equals(item.itemId)) {
                    trialId = item.trialId;
                    break;
                }
            }
        }
        for (ProductDetails.SubscriptionOfferDetails offer : offers) {
            if (offer.getOfferId() != null && offer.getOfferId().equals(trialId)) {
                return offer.getOfferToken();
            }
        }
        return offers.get(offers.size() - 1).getOfferToken();
    }

    private String handleBillingResult(BillingResult result) {
        switch (result.getResponseCode()) {
            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                return "Timeout";
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return "Error processing request.";
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return "Play Store service is not connected now";
            case BillingClient.BillingResponseCode.OK:
                return "Billing flow started";
            case BillingClient.BillingResponseCode.USER_CANCELED:
                notifyListener("Purchase canceled");
                return "Purchase canceled";
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                notifyListener("Network unavailable");
                return "Network unavailable";
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                notifyListener("Billing not supported for type of request");
                return "Billing not supported for type of request";
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "Item not available";
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                notifyListener("Error completing request");
                return "Error completing request";
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                notifyListener("Item already owned");
                return "Item already owned";
            default:
                notifyListener("Error: " + result.getDebugMessage());
                return "Error code: " + result.getResponseCode();
        }
    }

    private void notifyListener(String message) {
        if (purchaseListener != null) {
            purchaseListener.displayErrorMessage(message);
        }
    }

    public void consumePurchase() {
        if (currentProductId == null || currentProductId.isEmpty()) {
            Log.e(Tag, "Consume Purchase false: productId null");
            return;
        }
        consumePurchase(currentProductId);
    }

    /**
     * Consumes a purchased product, allowing it to be purchased again.
     * Call this after you have granted the user their items/credits.
     *
     * <p>For consumable products (e.g., coins, gems), you must consume the purchase
     * to allow the user to buy it again. The flow should be:</p>
     * <ol>
     *   <li>User purchases product</li>
     *   <li>onProductPurchased callback fires</li>
     *   <li>Grant user their items (update balance, etc.)</li>
     *   <li>Call consumePurchase(productId)</li>
     *   <li>onPurchaseConsumed callback fires (if listener set)</li>
     * </ol>
     *
     * @param productId The product ID to consume.
     */
    public void consumePurchase(String productId) {
        if (!isServiceConnected.get()) {
            Log.e(Tag, "Billing client not connected. Cannot consume purchase.");
            return;
        }
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                (billingResult, list) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                        for (Purchase purchase : list) {
                            if (purchase.getProducts().contains(productId)) {
                                ConsumeParams consumeParams = ConsumeParams.newBuilder()
                                        .setPurchaseToken(purchase.getPurchaseToken())
                                        .build();
                                billingClient.consumeAsync(consumeParams, (consumeResult, purchaseToken) -> {
                                    if (consumeResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                        Log.d(Tag, "Purchase consumed successfully: " + purchaseToken);
                                        // Notify listener about consumption
                                        notifyPurchaseHistoryEvent(purchase, productId, true);
                                        // Remove from owned list
                                        stringList.remove(productId);
                                        verifyPurchased(false);
                                    } else {
                                        Log.e(Tag, "Failed to consume purchase: " + consumeResult.getDebugMessage());
                                    }
                                });
                            }
                        }
                    }
                }
        );
    }

    /**
     * Refreshes and retrieves all current purchases from Google Play.
     * This fetches both INAPP and subscription purchases.
     * Results are available via the existing purchase tracking.
     *
     * Note: For consumed purchases, you must track them yourself using PurchaseHistoryListener.
     * Google's queryPurchaseHistoryAsync is deprecated in Billing Library 8+.
     */
    public void refreshPurchases() {
        refreshPurchases(BillingClient.ProductType.INAPP);
        refreshPurchases(BillingClient.ProductType.SUBS);
    }

    /**
     * Refreshes and retrieves current purchases for a specific product type.
     *
     * @param productType BillingClient.ProductType.INAPP or BillingClient.ProductType.SUBS
     */
    public void refreshPurchases(String productType) {
        if (!isServiceConnected.get()) {
            Log.e(Tag, "Billing client not connected. Cannot refresh purchases.");
            return;
        }
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(productType).build(),
                (billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.d(Tag, "Purchases refreshed: " + purchases.size() + " for " + productType);
                        for (Purchase purchase : purchases) {
                            Log.d(Tag, "Active purchase - Products: " + purchase.getProducts() +
                                    ", Time: " + purchase.getPurchaseTime() +
                                    ", Quantity: " + purchase.getQuantity() +
                                    ", Acknowledged: " + purchase.isAcknowledged());
                            // Update internal tracking
                            if (productType.equals(BillingClient.ProductType.INAPP)) {
                                for (String productId : purchase.getProducts()) {
                                    if (!stringList.contains(productId)) {
                                        stringList.add(productId);
                                    }
                                }
                                isPurchased = true;
                            } else if (productType.equals(BillingClient.ProductType.SUBS)) {
                                String productId = purchase.getProducts().isEmpty() ? "" : purchase.getProducts().get(0);
                                handlePurchase(
                                        new PurchaseResult(
                                                purchase.getOrderId(),
                                                purchase.getPackageName(),
                                                purchase.getProducts(),
                                                purchase.getPurchaseTime(),
                                                purchase.getPurchaseState(),
                                                purchase.getPurchaseToken(),
                                                purchase.getQuantity(),
                                                purchase.isAutoRenewing(),
                                                purchase.isAcknowledged()
                                        ),
                                        productId
                                );
                                isPurchased = true;
                            }
                        }
                    } else {
                        Log.e(Tag, "Failed to refresh purchases: " + billingResult.getDebugMessage());
                    }
                }
        );
    }

    /**
     * Gets the PurchaseItem configuration for a product ID.
     *
     * @param productId The product ID to look up.
     * @return The PurchaseItem or null if not found.
     */
    public PurchaseItem getPurchaseItem(String productId) {
        if (purchaseItemList == null) return null;
        for (PurchaseItem item : purchaseItemList) {
            if (productId.equals(item.itemId)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Checks if user has an active subscription (including cancelled but not expired).
     *
     * @return true if user has any active subscription, false otherwise.
     */
    public boolean isSubscribed() {
        return !purchaseResultList.isEmpty();
    }

    /**
     * Checks if user has a specific active subscription.
     *
     * @param subscriptionId The subscription product ID to check.
     * @return true if the subscription is active, false otherwise.
     */
    public boolean isSubscribed(String subscriptionId) {
        for (PurchaseResult result : purchaseResultList) {
            if (result.getProductId().contains(subscriptionId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the subscription state for a specific subscription.
     *
     * @param subscriptionId The subscription product ID.
     * @return The subscription state, or EXPIRED if not found.
     */
    public PurchaseResult.SubscriptionState getSubscriptionState(String subscriptionId) {
        for (PurchaseResult result : purchaseResultList) {
            if (result.getProductId().contains(subscriptionId)) {
                return result.getSubscriptionState();
            }
        }
        return PurchaseResult.SubscriptionState.EXPIRED;
    }

    /**
     * Gets all active subscriptions with their states.
     *
     * @return List of subscription PurchaseResults.
     */
    public List<PurchaseResult> getActiveSubscriptions() {
        return new ArrayList<>(purchaseResultList);
    }

    /**
     * Checks if any subscription is cancelled but still has access.
     *
     * @return true if user has a cancelled subscription.
     */
    public boolean hasSubscriptionCancelled() {
        for (PurchaseResult result : purchaseResultList) {
            if (!result.isAutoRenewing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if all subscriptions will auto-renew.
     *
     * @return true if all subscriptions are set to auto-renew.
     */
    public boolean willSubscriptionsRenew() {
        if (purchaseResultList.isEmpty()) {
            return false;
        }
        for (PurchaseResult result : purchaseResultList) {
            if (!result.isAutoRenewing()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the subscription result for a specific product.
     *
     * @param subscriptionId The subscription product ID.
     * @return The PurchaseResult or null if not found.
     */
    @Nullable
    public PurchaseResult getSubscription(String subscriptionId) {
        for (PurchaseResult result : purchaseResultList) {
            if (result.getProductIds().contains(subscriptionId)) {
                return result;
            }
        }
        return null;
    }

    // ==================== Subscription Verification Methods ====================

    /**
     * Verifies a subscription with your backend server to get the expiry date.
     * Requires setting a {@link SubscriptionVerificationCallback} via
     * {@link #setSubscriptionVerificationCallback(SubscriptionVerificationCallback)}.
     *
     * <p>After verification, the expiry time will be stored in the PurchaseResult
     * and can be retrieved via {@link #getSubscriptionExpiryTime(String)}.</p>
     *
     * @param subscriptionId The subscription product ID to verify.
     * @param listener       Callback for verification result.
     */
    public void verifySubscription(String subscriptionId, SubscriptionVerificationListener listener) {
        if (subscriptionVerificationCallback == null) {
            Log.e(Tag, "SubscriptionVerificationCallback not set. Call setSubscriptionVerificationCallback first.");
            if (listener != null) {
                listener.onVerificationFailed("Verification callback not configured");
            }
            return;
        }

        PurchaseResult subscription = getSubscription(subscriptionId);
        if (subscription == null) {
            Log.e(Tag, "Subscription not found: " + subscriptionId);
            if (listener != null) {
                listener.onVerificationFailed("Subscription not found");
            }
            return;
        }

        String packageName = application.getPackageName();
        String purchaseToken = subscription.getPurchaseToken();

        subscriptionVerificationCallback.verifySubscription(
                packageName,
                subscriptionId,
                purchaseToken,
                new SubscriptionVerificationCallback.VerificationResultListener() {
                    @Override
                    public void onVerified(@NonNull SubscriptionVerificationCallback.SubscriptionDetails details) {
                        // Update the PurchaseResult with expiry time
                        subscription.setExpiryTime(details.getExpiryTimeMillis());
                        subscription.setAutoRenewing(details.isAutoRenewing());

                        Log.d(Tag, "Subscription verified: " + subscriptionId +
                                ", expiry: " + subscription.getExpiryTimeFormatted());

                        if (listener != null) {
                            listener.onVerified(subscription);
                        }
                    }

                    @Override
                    public void onVerificationFailed(@Nullable String errorMessage) {
                        Log.e(Tag, "Subscription verification failed: " + errorMessage);
                        if (listener != null) {
                            listener.onVerificationFailed(errorMessage);
                        }
                    }
                }
        );
    }

    /**
     * Verifies all active subscriptions with your backend server.
     *
     * @param listener Callback for each verification result.
     */
    public void verifyAllSubscriptions(SubscriptionVerificationListener listener) {
        if (purchaseResultList.isEmpty()) {
            Log.d(Tag, "No subscriptions to verify");
            return;
        }

        for (PurchaseResult result : purchaseResultList) {
            String subscriptionId = result.getFirstProductId();
            if (subscriptionId != null) {
                verifySubscription(subscriptionId, listener);
            }
        }
    }

    /**
     * Gets the subscription expiry time in milliseconds.
     * Returns 0 if not verified or subscription not found.
     *
     * @param subscriptionId The subscription product ID.
     * @return Expiry time in milliseconds, or 0 if not available.
     */
    public long getSubscriptionExpiryTime(String subscriptionId) {
        PurchaseResult subscription = getSubscription(subscriptionId);
        if (subscription != null && subscription.isExpiryVerified()) {
            return subscription.getExpiryTime();
        }
        return 0;
    }

    /**
     * Gets the subscription expiry time as a formatted string.
     *
     * @param subscriptionId The subscription product ID.
     * @return Formatted expiry date, or "Not verified" if not available.
     */
    public String getSubscriptionExpiryTimeFormatted(String subscriptionId) {
        PurchaseResult subscription = getSubscription(subscriptionId);
        if (subscription != null) {
            return subscription.getExpiryTimeFormatted();
        }
        return "Not found";
    }

    /**
     * Gets the subscription expiry time as a formatted string with custom pattern.
     *
     * @param subscriptionId The subscription product ID.
     * @param pattern        The date format pattern (e.g., "yyyy-MM-dd").
     * @return Formatted expiry date, or "Not verified" if not available.
     */
    public String getSubscriptionExpiryTimeFormatted(String subscriptionId, String pattern) {
        PurchaseResult subscription = getSubscription(subscriptionId);
        if (subscription != null) {
            return subscription.getExpiryTimeFormatted(pattern);
        }
        return "Not found";
    }

    /**
     * Gets the remaining days until subscription expiry.
     *
     * @param subscriptionId The subscription product ID.
     * @return Remaining days, 0 if expired, or -1 if not verified/not found.
     */
    public int getSubscriptionRemainingDays(String subscriptionId) {
        PurchaseResult subscription = getSubscription(subscriptionId);
        if (subscription != null) {
            return subscription.getRemainingDays();
        }
        return -1;
    }

    /**
     * Checks if a subscription has expired based on verified expiry time.
     *
     * @param subscriptionId The subscription product ID.
     * @return true if expired, false if not expired or not verified.
     */
    public boolean isSubscriptionExpired(String subscriptionId) {
        PurchaseResult subscription = getSubscription(subscriptionId);
        if (subscription != null && subscription.isExpiryVerified()) {
            return subscription.isExpired();
        }
        return false;
    }

    /**
     * Listener for subscription verification results.
     */
    public interface SubscriptionVerificationListener {
        /**
         * Called when verification succeeds.
         *
         * @param subscription The verified subscription with expiry time set.
         */
        void onVerified(PurchaseResult subscription);

        /**
         * Called when verification fails.
         *
         * @param errorMessage Description of the error.
         */
        void onVerificationFailed(@Nullable String errorMessage);
    }

    /**
     * Checks if user has a lifetime premium purchase.
     * This is an INAPP purchase with LIFETIME_PREMIUM or REMOVE_ADS category.
     *
     * @return true if user has any lifetime purchase, false otherwise.
     */
    public boolean hasLifetimePurchase() {
        for (String productId : stringList) {
            PurchaseItem item = getPurchaseItem(productId);
            if (item != null && item.isLifetimePurchase()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if user has a "Remove Ads" purchase specifically.
     *
     * @return true if user has a remove ads purchase, false otherwise.
     */
    public boolean hasRemoveAdsPurchase() {
        for (String productId : stringList) {
            PurchaseItem item = getPurchaseItem(productId);
            if (item != null && item.category == PurchaseItem.PurchaseCategory.REMOVE_ADS) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if user has a "Lifetime Premium" purchase specifically.
     *
     * @return true if user has a lifetime premium purchase, false otherwise.
     */
    public boolean hasLifetimePremium() {
        for (String productId : stringList) {
            PurchaseItem item = getPurchaseItem(productId);
            if (item != null && item.category == PurchaseItem.PurchaseCategory.LIFETIME_PREMIUM) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a specific product has been purchased and is currently owned.
     *
     * @param productId The product ID to check.
     * @return true if the product is owned, false otherwise.
     */
    public boolean hasLifetimePurchase(String productId) {
        return stringList.contains(productId);
    }

    /**
     * Checks if ads should be disabled based on any purchase type.
     * Returns true for subscriptions, lifetime premium, or remove ads purchases.
     * This is useful for the ads library integration.
     *
     * @return true if ads should be disabled, false otherwise.
     */
    public boolean shouldDisableAds() {
        return isPurchased();
    }

    /**
     * Gets the type of purchase that is disabling ads.
     *
     * @return PurchaseType indicating why ads are disabled, or NONE if not purchased.
     */
    public PurchaseType getActivePurchaseType() {
        if (isSubscribed()) {
            return PurchaseType.SUBSCRIPTION;
        }
        if (hasLifetimePremium()) {
            return PurchaseType.LIFETIME_PREMIUM;
        }
        if (hasRemoveAdsPurchase()) {
            return PurchaseType.REMOVE_ADS;
        }
        if (hasLifetimePurchase()) {
            return PurchaseType.LIFETIME;
        }
        if (isPurchased) {
            return PurchaseType.UNKNOWN;
        }
        return PurchaseType.NONE;
    }

    /**
     * Gets the category of a purchased product.
     *
     * @param productId The product ID to check.
     * @return The PurchaseCategory or null if not found.
     */
    public PurchaseItem.PurchaseCategory getPurchaseCategory(String productId) {
        PurchaseItem item = getPurchaseItem(productId);
        return item != null ? item.category : null;
    }

    /**
     * Enum representing the type of purchase for ad-disabling purposes.
     */
    public enum PurchaseType {
        /** No active purchase */
        NONE,
        /** Active recurring subscription */
        SUBSCRIPTION,
        /** Lifetime premium - one-time purchase with full premium access */
        LIFETIME_PREMIUM,
        /** Remove ads - one-time purchase to remove ads only */
        REMOVE_ADS,
        /** Other lifetime purchase (feature unlock, etc.) */
        LIFETIME,
        /** Consumable purchase - does not disable ads */
        CONSUMABLE,
        /** Legacy or unknown purchase type */
        UNKNOWN
    }

    @Deprecated
    public String getPrice() {
        return getPrice(currentProductId);
    }

    public String getPrice(String productId) {
        ProductDetails productDetails = productDetailsMap.get(productId);
        if (productDetails == null) {
            return "";
        }
        if (productDetails.getProductType().equals(BillingClient.ProductType.INAPP)) {
            ProductDetails.OneTimePurchaseOfferDetails offer = productDetails.getOneTimePurchaseOfferDetails();
            return offer != null ? offer.getFormattedPrice() : "";
        } else {
            return getPriceSub(productId);
        }
    }

    public String getPriceSub(String productId) {
        ProductDetails productDetails = subProductDetailsMap.get(productId);
        if (productDetails == null) {
            return "";
        }
        List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
        if (offers != null && !offers.isEmpty()) {
            List<ProductDetails.PricingPhase> pricingPhases = offers.get(offers.size() - 1).getPricingPhases().getPricingPhaseList();
            return pricingPhases.get(pricingPhases.size() - 1).getFormattedPrice();
        }
        return "";
    }

    public List<ProductDetails.PricingPhase> getPricePricingPhaseList(String productId) {
        ProductDetails productDetails = subProductDetailsMap.get(productId);
        if (productDetails == null) {
            return new ArrayList<>();
        }
        List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
        if (offers != null && !offers.isEmpty()) {
            return offers.get(offers.size() - 1).getPricingPhases().getPricingPhaseList();
        }
        return new ArrayList<>();
    }

    public String getIntroductorySubPrice(String productId) {
        ProductDetails productDetails = subProductDetailsMap.get(productId);
        if (productDetails == null) {
            return "";
        }
        List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
        if (offers != null && !offers.isEmpty()) {
            for (ProductDetails.PricingPhase phase : offers.get(0).getPricingPhases().getPricingPhaseList()) {
                if (phase.getBillingCycleCount() > 0 && phase.getPriceAmountMicros() < offers.get(0).getPricingPhases().getPricingPhaseList().get(0).getPriceAmountMicros()) {
                    return phase.getFormattedPrice();
                }
            }
        }
        return "";
    }

    public String getCurrency(String productId, int typeIAP) {
        ProductDetails productDetails = typeIAP == TYPE_IAP.PURCHASE ? inAppProductDetailsMap.get(productId) : subProductDetailsMap.get(productId);
        if (productDetails == null) {
            return "";
        }
        if (typeIAP == TYPE_IAP.PURCHASE) {
            ProductDetails.OneTimePurchaseOfferDetails offer = productDetails.getOneTimePurchaseOfferDetails();
            return offer != null ? offer.getPriceCurrencyCode() : "";
        } else {
            List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
            if (offers != null && !offers.isEmpty()) {
                return offers.get(offers.size() - 1).getPricingPhases().getPricingPhaseList().get(0).getPriceCurrencyCode();
            }
        }
        return "";
    }

    public double getPriceWithoutCurrency(String productId, int typeIAP) {
        ProductDetails productDetails = typeIAP == TYPE_IAP.PURCHASE ? inAppProductDetailsMap.get(productId) : subProductDetailsMap.get(productId);
        if (productDetails == null) {
            return 0.0;
        }
        if (typeIAP == TYPE_IAP.PURCHASE) {
            ProductDetails.OneTimePurchaseOfferDetails offer = productDetails.getOneTimePurchaseOfferDetails();
            return offer != null ? offer.getPriceAmountMicros() / 1_000_000.0 : 0.0;
        } else {
            List<ProductDetails.SubscriptionOfferDetails> offers = productDetails.getSubscriptionOfferDetails();
            if (offers != null && !offers.isEmpty()) {
                return offers.get(offers.size() - 1).getPricingPhases().getPricingPhaseList().get(0).getPriceAmountMicros() / 1_000_000.0;
            }
        }
        return 0.0;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getDiscount() {
        return discount;
    }

    public void handlePurchase(Purchase purchase) {
        String productId = purchase.getProducts() != null && !purchase.getProducts().isEmpty() ? purchase.getProducts().get(0) : "Unknown Product";
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                    @Override
                    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            Log.d(Tag, "Purchase acknowledged successfully for product: " + productId);
                            if (purchaseListener != null) {
                                purchaseListener.onProductPurchased(purchase.getOrderId(), purchase.getOriginalJson());
                            }
                            idPurchased = productId;

                            // Notify history listener about new purchase
                            notifyPurchaseHistoryEvent(purchase, productId, false);

                            ProductDetails details = productDetailsMap.get(productId);
                            if (details != null) {
                                if (details.getProductType().equals(BillingClient.ProductType.SUBS)) {
                                    // Subscription - add to subscription list, mark as purchased
                                    isPurchased = true;
                                    handlePurchase(
                                            new PurchaseResult(
                                                    purchase.getOrderId(),
                                                    purchase.getPackageName(),
                                                    purchase.getProducts(),
                                                    purchase.getPurchaseTime(),
                                                    purchase.getPurchaseState(),
                                                    purchase.getPurchaseToken(),
                                                    purchase.getQuantity(),
                                                    purchase.isAutoRenewing(),
                                                    purchase.isAcknowledged()
                                            ),
                                            productId
                                    );
                                } else if (details.getProductType().equals(BillingClient.ProductType.INAPP)) {
                                    // INAPP purchase - check if consumable or lifetime
                                    if (!stringList.contains(productId)) {
                                        stringList.add(productId);
                                    }
                                    // Only set isPurchased for non-consumable (lifetime) purchases
                                    // Consumables don't disable ads
                                    if (!isProductConsumable(productId)) {
                                        isPurchased = true;
                                    }
                                }
                            }
                        } else {
                            Log.e(Tag, "Failed to acknowledge purchase: " + billingResult.getDebugMessage());
                            notifyListener("Failed to acknowledge purchase: " + billingResult.getDebugMessage());
                        }
                    }
                });
            } else {
                Log.d(Tag, "Purchase already acknowledged for product: " + productId);
                if (purchaseListener != null) {
                    purchaseListener.onProductPurchased(purchase.getOrderId(), purchase.getOriginalJson());
                }
                idPurchased = productId;

                ProductDetails details = productDetailsMap.get(productId);
                if (details != null) {
                    if (details.getProductType().equals(BillingClient.ProductType.SUBS)) {
                        // Subscription - add to subscription list, mark as purchased
                        isPurchased = true;
                        handlePurchase(
                                new PurchaseResult(
                                        purchase.getOrderId(),
                                        purchase.getPackageName(),
                                        purchase.getProducts(),
                                        purchase.getPurchaseTime(),
                                        purchase.getPurchaseState(),
                                        purchase.getPurchaseToken(),
                                        purchase.getQuantity(),
                                        purchase.isAutoRenewing(),
                                        purchase.isAcknowledged()
                                ),
                                productId
                        );
                    } else if (details.getProductType().equals(BillingClient.ProductType.INAPP)) {
                        // INAPP purchase - check if consumable or lifetime
                        if (!stringList.contains(productId)) {
                            stringList.add(productId);
                        }
                        // Only set isPurchased for non-consumable (lifetime) purchases
                        if (!isProductConsumable(productId)) {
                            isPurchased = true;
                        }
                    }
                }
            }
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            Log.d(Tag, "Purchase is pending for product: " + productId);
            notifyListener("Purchase pending for product: " + productId);
        } else {
            Log.d(Tag, "Purchase state unhandled: " + purchase.getPurchaseState() + " for product: " + productId);
            notifyListener("Purchase unhandled state for product: " + productId);
        }
    }

    /**
     * Notifies the purchase history listener about a purchase event.
     */
    private void notifyPurchaseHistoryEvent(Purchase purchase, String productId, boolean isConsumed) {
        if (purchaseHistoryListener != null) {
            // Use factory method to capture all available data
            PurchaseResult result = PurchaseResult.fromPurchase(purchase);
            // Set product type for better tracking
            ProductDetails details = productDetailsMap.get(productId);
            if (details != null) {
                result.setProductType(details.getProductType());
            }
            if (isConsumed) {
                result.markAsConsumed();
                purchaseHistoryListener.onPurchaseConsumed(productId, result);
            } else {
                purchaseHistoryListener.onNewPurchase(productId, result);
            }
        }
    }

    public void connectToGooglePlayBilling() {
        if (!isServiceConnected.get()) {
            billingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        isServiceConnected.set(true);
                        Log.d(Tag, "Billing setup finished. Connected to Google Play.");
                        isBillingAvailable = true;
                        isBillingInitialized = Boolean.TRUE;
                        if (handler != null && runnable != null) {
                            handler.removeCallbacks(runnable);
                            Log.d(Tag, "setBillingListener: timeout removed callbacks");
                        }
                        if (billingListener != null) {
                            billingListener.onInitBillingFinished(billingResult.getResponseCode());
                        }
                        if (!inAppProductArrayList.isEmpty()) {
                            queryProductDetails(getIdsFromQueryProductList(inAppProductArrayList), BillingClient.ProductType.INAPP);
                        }
                        if (!subProductArrayList.isEmpty()) {
                            queryProductDetails(getIdsFromQueryProductList(subProductArrayList), BillingClient.ProductType.SUBS);
                        }
                        verifyPurchased(true);
                    } else {
                        isServiceConnected.set(false);
                        isBillingAvailable = false;
                        isBillingInitialized = Boolean.FALSE;
                        Log.e(Tag, "Billing setup failed: " + billingResult.getDebugMessage());
                        if (billingListener != null) {
                            billingListener.onInitBillingFinished(billingResult.getResponseCode());
                        }
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    isServiceConnected.set(false);
                    isBillingAvailable = false;
                    Log.w(Tag, "Billing service disconnected. Attempting to reconnect...");
                }
            });
        }
    }

    private List<String> getIdsFromQueryProductList(List<QueryProductDetailsParams.Product> productList) {
        List<String> ids = new ArrayList<>();
        for (QueryProductDetailsParams.Product product : productList) {
            ids.add(product.zza());
        }
        return ids;
    }

    public void queryProductDetails(List<String> productIds, String productType) {
        if (!isServiceConnected.get()) {
            Log.e(Tag, "Billing client not connected. Cannot query product details.");
            return;
        }
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        for (String productId : productIds) {
            productList.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build());
        }
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();
        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    List<ProductDetails> productDetailsList = queryProductDetailsResult.getProductDetailsList();
                    if (productType.equals(BillingClient.ProductType.INAPP)) {
                        inAppProductDetailsMap.clear();
                    } else {
                        subProductDetailsMap.clear();
                    }
                    for (ProductDetails productDetails : productDetailsList) {
                        productDetailsMap.put(productDetails.getProductId(), productDetails);
                        if (productType.equals(BillingClient.ProductType.INAPP)) {
                            inAppProductDetailsMap.put(productDetails.getProductId(), productDetails);
                        } else {
                            subProductDetailsMap.put(productDetails.getProductId(), productDetails);
                        }
                        Log.d(Tag, "Found Product: " + productDetails.getProductId() + " - " + productDetails.getTitle());
                    }
                    if (updatePurchaseListener != null) {
                        updatePurchaseListener.onUpdateFinished();
                    }
                } else {
                    Log.e(Tag, "Error querying product details (" + productType + "): " + billingResult.getDebugMessage());
                    notifyListener("Error getting " + productType + " details: " + billingResult.getDebugMessage());
                }
            }
        });
    }

    PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> list) {
            Log.e(Tag, "onPurchasesUpdated code: " + billingResult.getResponseCode());
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                for (Purchase purchase : list) {
                    handlePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                if (purchaseListener != null) {
                    purchaseListener.onUserCancelBilling();
                }
                Log.d(Tag, "onPurchasesUpdated: USER_CANCELED");
            } else {
                Log.d(Tag, "onPurchasesUpdated: Unexpected response: " + billingResult.getDebugMessage());
                notifyListener("Purchase error: " + billingResult.getDebugMessage());
            }
        }
    };

    private void addItemsToList(List<PurchaseItem> purchaseItems) {
        inAppProductArrayList.clear();
        subProductArrayList.clear();
        for (PurchaseItem purchaseItem : purchaseItems) {
            if (purchaseItem.type == TYPE_IAP.PURCHASE) {
                inAppProductArrayList.add(QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(purchaseItem.itemId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build());
            } else if (purchaseItem.type == TYPE_IAP.SUBSCRIPTION) {
                subProductArrayList.add(QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(purchaseItem.itemId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build());
            }
        }
        Log.d(Tag, "syncPurchaseItemsToListProduct: listINAPId " + inAppProductArrayList.size());
        Log.d(Tag, "syncPurchaseItemsToListProduct: listSubsId " + subProductArrayList.size());
    }

    private void handlePurchase(PurchaseResult purchaseResult, String id) {
        boolean exists = false;
        Iterator<PurchaseResult> it = purchaseResultList.iterator();
        while (it.hasNext()) {
            PurchaseResult next = it.next();
            if (next.getProductId().contains(id)) {
                exists = true;
                purchaseResultList.remove(next);
                purchaseResultList.add(purchaseResult);
                break;
            }
        }
        if (!exists) {
            purchaseResultList.add(purchaseResult);
        }
    }

    private String handlePurchase(double price, String currency) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setCurrency(Currency.getInstance(currency));
        return currencyFormat.format(price);
    }

    @Deprecated
    private ArrayList<QueryProductDetailsParams.Product> handlePurchase(List<String> listId, String styleBilling) {
        ArrayList<QueryProductDetailsParams.Product> arrayList = new ArrayList<>();
        for (String str : listId) {
            arrayList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(str).setProductType(styleBilling).build());
        }
        return arrayList;
    }

    public @interface TYPE_IAP {
        int PURCHASE = 1;
        int SUBSCRIPTION = 2;
    }
}