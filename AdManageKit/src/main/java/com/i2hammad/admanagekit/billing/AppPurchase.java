package com.i2hammad.admanagekit.billing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.datatransport.BuildConfig;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A class to manage in-app purchases and subscriptions using the Google Play Billing Library.
 */
public class AppPurchase {

    // Constants
    private static final String K = null;
    private static final String L = null;
    private static final String Tag = "AppPurchase";
    public static final String PRODUCT_ID_TEST = "android.test.purchased";

    // Singleton instance of AppPurchase
    @SuppressLint({"StaticFieldLeak"})
    private static AppPurchase appPurchase;

    // Member variables

    private String c; // Deprecated item ID
    private ArrayList<QueryProductDetailsParams.Product> d; // Subscription products
    private ArrayList<QueryProductDetailsParams.Product> e; // In-app products
    private List<PurchaseItem> f; // List of purchase items
    private PurchaseListener purchaseListener; // Listener for purchase events
    private UpdatePurchaseListener updatePurchaseListener; // Listener for purchase updates
    private BillingListener billingListener; // Listener for billing events
    private BillingClient billingClient; // Google Play Billing client
    private List<ProductDetails> productDetailsList; // Details of in-app products
    private List<ProductDetails> productDetailsList1; // Details of subscription products
    private boolean p; // Billing availability flag
    private boolean q; // Product details queried flag
    private int v; // Type of purchase (in-app or subscription)
    private Handler handler; // Handler for delayed tasks
    private Runnable runnable; // Runnable task
    private String a = "2.89$"; // Price string
    private String b = "3.50$"; // Old price string
    private Boolean j = Boolean.FALSE; // Billing initialization flag
    private final Map<String, ProductDetails> productDetailsMap = new HashMap<>(); // In-app product details map
    private final Map<String, ProductDetails> o = new HashMap<>(); // Subscription product details map
    private boolean r = false; // Consume purchase flag
    private int s = 0; // State variable
    private int t = 4; // State variable
    private String u = ""; // Current product ID
    private boolean w = false; // Verification flag
    private boolean x = false; // Verification flag
    private boolean y = false; // Verification flag
    private boolean z = false; // Purchase update flag
    private boolean A = false; // Purchase update flag
    private boolean B = false; // Purchase state flag
    private String C = ""; // Purchased product ID
    private List<PurchaseResult> purchaseResultList = new ArrayList<>(); // List of purchase results
    private List<String> stringList = new ArrayList<>(); // List of owned in-app products

    // Listener for purchases updated events
    PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> list) {
            Log.e(AppPurchase.Tag, "onPurchasesUpdated code: " + billingResult.getResponseCode());
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                for (Purchase purchase : list) {
                    purchase.getSkus();
                    AppPurchase.this.a(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                if (AppPurchase.this.purchaseListener != null) {
                    AppPurchase.this.purchaseListener.onUserCancelBilling();
                }
                Log.d(AppPurchase.Tag, "onPurchasesUpdated: USER_CANCELED");
            } else {
                Log.d(AppPurchase.Tag, "onPurchasesUpdated: Unexpected response");
            }
        }
    };

    // Listener for billing client state events
    BillingClientStateListener billingClientStateListener = new BillingClientStateListener() {
        public void onBillingServiceDisconnected() {
            AppPurchase.this.p = false;
        }

        public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
            Log.d(AppPurchase.Tag, "onBillingSetupFinished: " + billingResult.getResponseCode());
            if (!AppPurchase.this.j) {
                AppPurchase.this.verifyPurchased(true);
            }
            AppPurchase.this.j = Boolean.TRUE;
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                AppPurchase.this.p = true;
                if (!AppPurchase.this.e.isEmpty()) {
                    AppPurchase.this.billingClient.queryProductDetailsAsync(
                            QueryProductDetailsParams.newBuilder().setProductList(AppPurchase.this.e).build(),
                            new ProductDetailsResponseListener() {
                                public void onProductDetailsResponse(BillingResult billingResult2, List<ProductDetails> productDetailsList) {
                                    if (productDetailsList != null) {
                                        Log.d(AppPurchase.Tag, "onSkuINAPDetailsResponse: " + productDetailsList.size());
                                        AppPurchase.this.productDetailsList = productDetailsList;
                                        AppPurchase.this.q = true;
                                        AppPurchase.this.a(productDetailsList);
                                    }
                                }
                            }
                    );
                }
                if (!AppPurchase.this.d.isEmpty()) {
                    QueryProductDetailsParams build = QueryProductDetailsParams.newBuilder().setProductList(AppPurchase.this.d).build();
                    Iterator<QueryProductDetailsParams.Product> it = AppPurchase.this.d.iterator();
                    while (it.hasNext()) {
                        Log.d(AppPurchase.Tag, "onBillingSetupFinished: " + it.next().zza());
                    }
                    AppPurchase.this.billingClient.queryProductDetailsAsync(
                            build,
                            new ProductDetailsResponseListener() {
                                public void onProductDetailsResponse(BillingResult billingResult2, List<ProductDetails> productDetailsList) {
                                    if (productDetailsList != null) {
                                        Log.d(AppPurchase.Tag, "onSkuSubsDetailsResponse: " + productDetailsList.size());
                                        AppPurchase.this.productDetailsList1 = productDetailsList;
                                        AppPurchase.this.q = true;
                                        AppPurchase.this.b(productDetailsList);
                                    }
                                }
                            }
                    );
                    return;
                }
                Log.d(AppPurchase.Tag, "onBillingSetupFinished: listSubscriptionId empty");
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED || billingResult.getResponseCode() == BillingClient.BillingResponseCode.ERROR) {
                Log.e(AppPurchase.Tag, "onBillingSetupFinished: ERROR");
            }
        }
    };

    private double J = 1.0d; // Discount value

    /**
     * Annotation to define types of in-app purchases.
     */
    public @interface TYPE_IAP {
        int PURCHASE = 1;
        int SUBSCRIPTION = 2;
    }

    /**
     * Gets the singleton instance of AppPurchase.
     *
     * @return The instance of AppPurchase.
     */
    public static AppPurchase getInstance() {
        if (appPurchase == null) {
            appPurchase = new AppPurchase();
        }
        return appPurchase;
    }

    /**
     * Private constructor to prevent direct instantiation.
     */
    private AppPurchase() {
    }

    /**
     * Sets the purchase listener for handling purchase events.
     *
     * @param purchaseListener The purchase listener to set.
     */
    public void setPurchaseListener(PurchaseListener purchaseListener) {
        this.purchaseListener = purchaseListener;
    }

    /**
     * Sets the listener for purchase updates.
     *
     * @param listener The listener for purchase updates.
     */
    public void setUpdatePurchaseListener(UpdatePurchaseListener listener) {
        this.updatePurchaseListener = listener;
    }

    /**
     * Sets the billing listener for handling billing events.
     *
     * @param billingListener The billing listener to set.
     */
    public void setBillingListener(BillingListener billingListener) {
        this.billingListener = billingListener;
        if (this.p) {
            billingListener.onInitBillingFinished(BillingClient.BillingResponseCode.OK);
            this.j = Boolean.TRUE;
        }
    }

    /**
     * Checks if billing is available.
     *
     * @return True if billing is available, false otherwise.
     */
    public boolean isAvailable() {
        return this.p;
    }

    /**
     * Gets the billing initialization status.
     *
     * @return True if billing is initialized, false otherwise.
     */
    public Boolean getInitBillingFinish() {
        return this.j;
    }

    /**
     * Sets an event to consume a test purchase when the view is clicked.
     *
     * @param view The view to set the click event on.
     */
    public void setEventConsumePurchaseTest(View view) {
        view.setOnClickListener(view1 -> {
            if (BuildConfig.DEBUG) {
                Log.d(Tag, "setEventConsumePurchaseTest: success");
                getInstance().consumePurchase(PRODUCT_ID_TEST);
            }
        });
    }

    /**
     * Sets the price string for a product.
     *
     * @param price The price string to set.
     */
    public void setPrice(String price) {
        this.a = price;
    }

    /**
     * Sets the consume purchase flag.
     *
     * @param consumePurchase True to consume purchase automatically, false otherwise.
     */
    public void setConsumePurchase(boolean consumePurchase) {
        this.r = consumePurchase;
    }

    /**
     * Sets the old price string for a product.
     *
     * @param oldPrice The old price string to set.
     */
    public void setOldPrice(String oldPrice) {
        this.b = oldPrice;
    }

    /**
     * Gets the list of subscription IDs owned by the user.
     *
     * @return The list of subscription IDs.
     */
    public List<PurchaseResult> getOwnerIdSubs() {
        return this.purchaseResultList;
    }

    /**
     * Gets the list of in-app product IDs owned by the user.
     *
     * @return The list of in-app product IDs.
     */
    public List<String> getOwnerIdInapps() {
        return this.stringList;
    }

    /**
     * Initializes billing with deprecated method using separate lists for in-app and subscription IDs.
     *
     * @param application The application context.
     * @param listINAPId  The list of in-app product IDs.
     * @param listSubsId  The list of subscription product IDs.
     */
    @Deprecated
    public void initBilling(Application application, List<String> listINAPId, List<String> listSubsId) {
        if (BuildConfig.DEBUG) {
            listINAPId.add(PRODUCT_ID_TEST);
        }
        this.d = a(listSubsId, "subs");
        this.e = a(listINAPId, "inapp");
        BillingClient build = BillingClient.newBuilder(application)
                .setListener(this.purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        this.billingClient = build;
        build.startConnection(this.billingClientStateListener);
    }

    /**
     * Sets the purchase state flag.
     *
     * @param purchase True if purchased, false otherwise.
     */
    public void setPurchase(boolean purchase) {
        this.B = purchase;
    }

    /**
     * Checks if the item has been purchased.
     *
     * @return True if purchased, false otherwise.
     */
    public boolean isPurchased() {
        return this.B;
    }

    /**
     * Gets the ID of the purchased item.
     *
     * @return The ID of the purchased item.
     */
    public String getIdPurchased() {
        return this.C;
    }

    /**
     * Verifies if purchases have been made.
     *
     * @param isCallback True if a callback is required, false otherwise.
     */
    public void verifyPurchased(boolean isCallback) {
        Log.d(Tag, "isPurchased : " + this.d.size());
        this.w = false;
        if (this.e != null) {
            this.billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType("inapp").build(),
                    (billingResult, list) -> {
                        Runnable runnable;
                        Runnable runnable2;
                        Log.d(Tag, "verifyPurchased INAPP code: " + billingResult.getResponseCode() + " === size: " + list.size());
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list) {
                                for (QueryProductDetailsParams.Product product : this.e) {
                                    if (purchase.getProducts().contains(product.zza())) {
                                        Log.d(Tag, "verifyPurchased INAPP: true");
                                        this.stringList.add(product.zza());
                                        this.B = true;
                                    }
                                }
                            }
                            this.x = true;
                            if (this.y && this.billingListener != null && isCallback) {
                                this.billingListener.onInitBillingFinished(billingResult.getResponseCode());
                                Handler handler = this.handler;
                                if (handler != null && (runnable2 = this.runnable) != null) {
                                    handler.removeCallbacks(runnable2);
                                }
                            }
                            this.w = true;
                            return;
                        }
                        this.x = true;
                        if (this.y) {
                            this.billingListener.onInitBillingFinished(billingResult.getResponseCode());
                            Handler handler2 = this.handler;
                            if (handler2 != null && (runnable = this.runnable) != null) {
                                handler2.removeCallbacks(runnable);
                            }
                            this.w = true;
                        }
                    }
            );
        }
        if (this.d != null) {
            this.billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType("subs").build(),
                    (billingResult2, list2) -> {
                        Log.d(Tag, "verifyPurchased SUBS code: " + billingResult2.getResponseCode() + " === size: " + list2.size());
                        if (billingResult2.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list2) {
                                for (QueryProductDetailsParams.Product product : this.d) {
                                    if (purchase.getProducts().contains(product.zza())) {
                                        a(
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
                                        Log.d(Tag, "verifyPurchased SUBS: true");
                                        this.B = true;
                                    }
                                }
                            }
                            this.y = true;
                            if (this.x && this.billingListener != null && isCallback) {
                                this.billingListener.onInitBillingFinished(billingResult2.getResponseCode());
                                Handler handler = this.handler;
                                if (handler != null && (runnable = this.runnable) != null) {
                                    handler.removeCallbacks(runnable);
                                }
                            }
                            this.w = true;
                            return;
                        }
                        this.y = true;
                        if (this.x && this.billingListener != null && isCallback) {
                            this.billingListener.onInitBillingFinished(billingResult2.getResponseCode());
                            Handler handler2 = this.handler;
                            if (handler2 != null && (runnable = this.runnable) != null) {
                                handler2.removeCallbacks(runnable);
                            }
                            this.w = true;
                        }
                    }
            );
        }
    }

    /**
     * Updates the purchase status of the user's owned items.
     */
    public void updatePurchaseStatus() {
        if (this.e != null) {
            this.billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType("inapp").build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list) {
                                for (QueryProductDetailsParams.Product product : this.e) {
                                    if (purchase.getProducts().contains(product.zza()) && !this.stringList.contains(product.zza())) {
                                        this.stringList.add(product.zza());
                                    }
                                }
                            }
                        }
                        this.z = true;
                        if (this.A && this.updatePurchaseListener != null) {
                            this.updatePurchaseListener.onUpdateFinished();
                        }
                    }
            );
        }
        if (this.d != null) {
            this.billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType("subs").build(),
                    (billingResult2, list2) -> {
                        if (billingResult2.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            for (Purchase purchase : list2) {
                                for (QueryProductDetailsParams.Product product : this.d) {
                                    if (purchase.getProducts().contains(product.zza())) {
                                        a(
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
                                    }
                                }
                            }
                        }
                        this.A = true;
                        if (this.z && this.updatePurchaseListener != null) {
                            this.updatePurchaseListener.onUpdateFinished();
                        }
                    }
            );
        }
    }

    /**
     * Initiates a purchase for the currently selected product.
     *
     * @param activity The activity context.
     */
    @Deprecated
    public void purchase(Activity activity) {
        String str = this.c;
        if (str == null) {
            Log.e(Tag, "Purchase false: productId null");
            Toast.makeText(activity, "Product id must not be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        purchase(activity, str);
    }

    /**
     * Subscribes the user to a subscription product.
     *
     * @param activity The activity context.
     * @param subsId   The subscription ID.
     * @return A message indicating the subscription result.
     */
    public String subscribe(Activity activity, String subsId) {
        if (this.productDetailsList1 == null) {
            if (this.purchaseListener != null) {
                this.purchaseListener.displayErrorMessage("Billing error init");
            }
            return "";
        } else if (BuildConfig.DEBUG) {
            purchase(activity, PRODUCT_ID_TEST);
            return "Billing test";
        } else {
            ProductDetails productDetails = this.o.get(subsId);
            if (productDetails == null) {
                return "Product ID invalid";
            }
            List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails = this.o.get(subsId).getSubscriptionOfferDetails();
            if (subscriptionOfferDetails == null || subscriptionOfferDetails.isEmpty()) {
                return "Can't find offer for this subscription!";
            }
            String trialId = null;
            for (PurchaseItem item : this.f) {
                if (item.itemId.equals(subsId)) {
                    trialId = item.trialId;
                    break;
                }
            }
            String offerToken = "";
            for (ProductDetails.SubscriptionOfferDetails offer : subscriptionOfferDetails) {
                String offerId = offer.getOfferId();
                if (offerId != null && offerId.equals(trialId)) {
                    offerToken = offer.getOfferToken();
                    break;
                }
            }
            if (offerToken.isEmpty()) {
                offerToken = subscriptionOfferDetails.get(subscriptionOfferDetails.size() - 1).getOfferToken();
            }
            Log.d(Tag, "subscribe: offerToken: " + offerToken);
            switch (this.billingClient.launchBillingFlow(
                    activity,
                    BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(List.of(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                            .setProductDetails(productDetails)
                                            .setOfferToken(offerToken)
                                            .build()
                            ))
                            .build()
            ).getResponseCode()) {
                case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                    return "Timeout";
                case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                    return "Error processing request.";
                case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                    return "Play Store service is not connected now";
                case BillingClient.BillingResponseCode.OK:
                    return "Subscribed Successfully";
                case BillingClient.BillingResponseCode.USER_CANCELED:
                    if (this.purchaseListener != null) {
                        this.purchaseListener.displayErrorMessage("Request Canceled");
                    }
                    return "Request Canceled";
                case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                    if (this.purchaseListener != null) {
                        this.purchaseListener.displayErrorMessage("Network error.");
                    }
                    return "Network Connection down";
                case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                    if (this.purchaseListener != null) {
                        this.purchaseListener.displayErrorMessage("Billing not supported for type of request");
                    }
                    return "Billing not supported for type of request";
                case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                    return "Item not available";
                case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                    if (this.purchaseListener != null) {
                        this.purchaseListener.displayErrorMessage("Error completing request");
                    }
                    return "Error completing request";
                case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                    return "Selected item is already owned";
                default:
                    return "";
            }
        }
    }

    /**
     * Consumes a purchase for the currently selected product.
     */
    public void consumePurchase() {
        String str = this.c;
        if (str == null) {
            Log.e(Tag, "Consume Purchase false: productId null");
        } else {
            consumePurchase(str);
        }
    }

    /**
     * Gets the price of the currently selected product.
     *
     * @return The price of the selected product.
     */
    @Deprecated
    public String getPrice() {
        return getPrice(this.c);
    }

    /**
     * Gets the price of a subscription product.
     *
     * @param productId The product ID.
     * @return The formatted price of the subscription product.
     */
    public String getPriceSub(String productId) {
        ProductDetails productDetails = this.o.get(productId);
        if (productDetails == null) {
            return "";
        }
        List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails();
        List<ProductDetails.PricingPhase> pricingPhaseList = subscriptionOfferDetails.get(subscriptionOfferDetails.size() - 1).getPricingPhases().getPricingPhaseList();
        Log.e(Tag, "getPriceSub: " + pricingPhaseList.get(pricingPhaseList.size() - 1).getFormattedPrice());
        return pricingPhaseList.get(pricingPhaseList.size() - 1).getFormattedPrice();
    }

    /**
     * Gets the pricing phase list of a subscription product.
     *
     * @param productId The product ID.
     * @return The list of pricing phases.
     */
    public List<ProductDetails.PricingPhase> getPricePricingPhaseList(String productId) {
        ProductDetails productDetails = this.o.get(productId);
        if (productDetails == null) {
            return null;
        }
        List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails();
        return subscriptionOfferDetails.get(subscriptionOfferDetails.size() - 1).getPricingPhases().getPricingPhaseList();
    }

    /**
     * Gets the introductory price of a subscription product.
     *
     * @param productId The product ID.
     * @return The introductory price of the subscription product.
     */
    public String getIntroductorySubPrice(String productId) {
        ProductDetails productDetails = this.o.get(productId);
        if (productDetails == null) {
            return "";
        }
        if (productDetails.getOneTimePurchaseOfferDetails() != null) {
            return productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
        }
        if (productDetails.getSubscriptionOfferDetails() != null) {
            List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails();
            List<ProductDetails.PricingPhase> pricingPhaseList = subscriptionOfferDetails.get(subscriptionOfferDetails.size() - 1).getPricingPhases().getPricingPhaseList();
            return pricingPhaseList.get(pricingPhaseList.size() - 1).getFormattedPrice();
        }
        return "";
    }

    /**
     * Gets the currency of a product based on its type.
     *
     * @param productId The product ID.
     * @param typeIAP   The type of in-app purchase (1 for purchase, 2 for subscription).
     * @return The currency code.
     */
    public String getCurrency(String productId, int typeIAP) {
        ProductDetails productDetails = typeIAP == TYPE_IAP.PURCHASE ? this.productDetailsMap.get(productId) : this.o.get(productId);
        if (productDetails == null) {
            return "";
        }
        if (typeIAP == TYPE_IAP.PURCHASE) {
            return productDetails.getOneTimePurchaseOfferDetails().getPriceCurrencyCode();
        }
        List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails();
        List<ProductDetails.PricingPhase> pricingPhaseList = subscriptionOfferDetails.get(subscriptionOfferDetails.size() - 1).getPricingPhases().getPricingPhaseList();
        return pricingPhaseList.get(pricingPhaseList.size() - 1).getPriceCurrencyCode();
    }

    /**
     * Gets the price of a product without the currency symbol.
     *
     * @param productId The product ID.
     * @param typeIAP   The type of in-app purchase (1 for purchase, 2 for subscription).
     * @return The price without the currency symbol.
     */
    public double getPriceWithoutCurrency(String productId, int typeIAP) {
        ProductDetails productDetails = typeIAP == TYPE_IAP.PURCHASE ? this.productDetailsMap.get(productId) : this.o.get(productId);
        if (productDetails == null) {
            return 0.0d;
        }
        if (typeIAP == TYPE_IAP.PURCHASE) {
            return productDetails.getOneTimePurchaseOfferDetails().getPriceAmountMicros() / 1_000_000.0;
        }
        List<ProductDetails.SubscriptionOfferDetails> subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails();
        List<ProductDetails.PricingPhase> pricingPhaseList = subscriptionOfferDetails.get(subscriptionOfferDetails.size() - 1).getPricingPhases().getPricingPhaseList();
        return pricingPhaseList.get(pricingPhaseList.size() - 1).getPriceAmountMicros() / 1_000_000.0;
    }

    /**
     * Sets the discount value for purchases.
     *
     * @param discount The discount value to set.
     */
    public void setDiscount(double discount) {
        this.J = discount;
    }

    /**
     * Gets the discount value for purchases.
     *
     * @return The discount value.
     */
    public double getDiscount() {
        return this.J;
    }

    /**
     * Checks if the item has been purchased.
     *
     * @param context The context.
     * @return True if purchased, false otherwise.
     */
    public boolean isPurchased(Context context) {
        return this.B;
    }

    /**
     * Gets the price of a product.
     *
     * @param productId The product ID.
     * @return The price of the product.
     */
    public String getPrice(String productId) {
        ProductDetails productDetails = this.productDetailsMap.get(productId);
        if (productDetails == null) {
            return "";
        }
        Log.e(Tag, "getPrice: " + productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice());
        return productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
    }

    /**
     * Syncs purchase items to the list of in-app and subscription products.
     *
     * @param purchaseItems The list of purchase items.
     */
    private void c(List<PurchaseItem> purchaseItems) {
        ArrayList<QueryProductDetailsParams.Product> arrayList = new ArrayList<>();
        ArrayList<QueryProductDetailsParams.Product> arrayList2 = new ArrayList<>();
        for (PurchaseItem purchaseItem : purchaseItems) {
            if (purchaseItem.type == TYPE_IAP.PURCHASE) {
                arrayList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(purchaseItem.itemId).setProductType("inapp").build());
            } else {
                arrayList2.add(QueryProductDetailsParams.Product.newBuilder().setProductId(purchaseItem.itemId).setProductType("subs").build());
            }
        }
        this.e = arrayList;
        Log.d(Tag, "syncPurchaseItemsToListProduct: listINAPId " + this.e.size());
        this.d = arrayList2;
        Log.d(Tag, "syncPurchaseItemsToListProduct: listSubscriptionId " + this.d.size());
    }

    /**
     * Stores the product details for subscription products.
     *
     * @param skuList The list of product details.
     */
    private void b(List<ProductDetails> skuList) {
        for (ProductDetails productDetails : skuList) {
            this.o.put(productDetails.getProductId(), productDetails);
        }
    }

    /**
     * Sets the billing listener with a timeout.
     *
     * @param billingListener The billing listener to set.
     * @param timeout         The timeout in milliseconds.
     */
    public void setBillingListener(BillingListener billingListener, int timeout) {
        Log.d(Tag, "setBillingListener: timeout " + timeout);
        this.billingListener = billingListener;
        if (this.p) {
            Log.d(Tag, "setBillingListener: finish");
            billingListener.onInitBillingFinished(BillingClient.BillingResponseCode.OK);
            this.j = Boolean.TRUE;
            return;
        }
        this.handler = new Handler();
        Runnable runnable = () -> {
            Log.d(Tag, "setBillingListener: timeout run");
            this.j = Boolean.TRUE;
            billingListener.onInitBillingFinished(BillingClient.BillingResponseCode.SERVICE_TIMEOUT);
        };
        this.runnable = runnable;
        this.handler.postDelayed(runnable, timeout);
    }

    /**
     * Consumes a purchase for the specified product ID.
     *
     * @param productId The product ID to consume.
     */
    public void consumePurchase(String productId) {
        this.billingClient.queryPurchasesAsync(
                "inapp",
                (billingResult, list) -> {
                    Purchase exc = null;
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                        for (Purchase purchase : list) {
                            if (purchase.getSkus().contains(productId)) {
                                exc = purchase;
                                break;
                            }
                        }
                    }
                    if (exc == null) {
                        return;
                    }
                    try {
                        ConsumeParams build = ConsumeParams.newBuilder().setPurchaseToken(exc.getPurchaseToken()).build();
                        ConsumeResponseListener consumeResponseListener = new ConsumeResponseListener() {
                            public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                    Log.e(AppPurchase.Tag, "onConsumeResponse: OK");
                                    AppPurchase.this.verifyPurchased(false);
                                }
                            }
                        };
                        appPurchase.billingClient.consumeAsync(build, consumeResponseListener);
                    } catch (Exception unused) {
                        unused.printStackTrace();
                    }
                }
        );
    }

    /**
     * Initiates a purchase for the specified product ID.
     *
     * @param activity  The activity context.
     * @param productId The product ID to purchase.
     * @return A message indicating the purchase result.
     */
    public String purchase(Activity activity, String productId) {
        if (this.productDetailsList == null) {
            if (this.purchaseListener != null) {
                this.purchaseListener.displayErrorMessage("Billing error init");
            }
            return "";
        }
        ProductDetails productDetails = this.productDetailsMap.get(productId);
        if (BuildConfig.DEBUG) {
            new PurchaseDevBottomSheet(TYPE_IAP.PURCHASE, productDetails, activity, this.purchaseListener).show();
            return "";
        }
        this.u = productId;
        this.v = TYPE_IAP.PURCHASE;
        switch (this.billingClient.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(List.of(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).build()))
                        .build()
        ).getResponseCode()) {
            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                return "Timeout";
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return "Error processing request.";
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return "Play Store service is not connected now";
            case BillingClient.BillingResponseCode.OK:
                return "Subscribed Successfully";
            case BillingClient.BillingResponseCode.USER_CANCELED:
                if (this.purchaseListener != null) {
                    this.purchaseListener.displayErrorMessage("Request Canceled");
                }
                return "Request Canceled";
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                if (this.purchaseListener != null) {
                    this.purchaseListener.displayErrorMessage("Network error.");
                }
                return "Network Connection down";
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                if (this.purchaseListener != null) {
                    this.purchaseListener.displayErrorMessage("Billing not supported for type of request");
                }
                return "Billing not supported for type of request";
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "Item not available";
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                if (this.purchaseListener != null) {
                    this.purchaseListener.displayErrorMessage("Error completing request");
                }
                return "Error completing request";
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return "Selected item is already owned";
            default:
                return "";
        }
    }

    /**
     * Stores the product details for in-app products.
     *
     * @param skuList The list of product details.
     */
    private void a(List<ProductDetails> skuList) {
        for (ProductDetails productDetails : skuList) {
            this.productDetailsMap.put(productDetails.getProductId(), productDetails);
        }
    }

    /**
     * Adds a purchase result to the list, updating if it already exists.
     *
     * @param purchaseResult The purchase result to add.
     * @param id             The product ID.
     */
    private void a(PurchaseResult purchaseResult, String id) {
        boolean exists = false;
        Iterator<PurchaseResult> it = this.purchaseResultList.iterator();
        while (it.hasNext()) {
            PurchaseResult next = it.next();
            if (next.getProductId().contains(id)) {
                exists = true;
                this.purchaseResultList.remove(next);
                this.purchaseResultList.add(purchaseResult);
                break;
            }
        }
        if (!exists) {
            this.purchaseResultList.add(purchaseResult);
        }
    }

    /**
     * Initializes billing with a list of purchase items.
     *
     * @param application      The application context.
     * @param purchaseItemList The list of purchase items.
     */
    public void initBilling(Application application, List<PurchaseItem> purchaseItemList) {
        if (BuildConfig.DEBUG) {
            purchaseItemList.add(new PurchaseItem(PRODUCT_ID_TEST, "", TYPE_IAP.PURCHASE));
        }
        this.f = purchaseItemList;
        c(purchaseItemList);
        BillingClient build = BillingClient.newBuilder(application)
                .setListener(this.purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        this.billingClient = build;
        build.startConnection(this.billingClientStateListener);
    }

    /**
     * Gets the list of subscription product IDs.
     *
     * @return The list of subscription product IDs.
     */
    private List<String> b() {
        ArrayList<String> arrayList = new ArrayList<>();
        for (QueryProductDetailsParams.Product product : this.d) {
            arrayList.add(product.zza());
        }
        return arrayList;
    }

    /**
     * Gets the list of in-app product IDs.
     *
     * @return The list of in-app product IDs.
     */
    private List<String> a() {
        ArrayList<String> arrayList = new ArrayList<>();
        for (QueryProductDetailsParams.Product product : this.e) {
            arrayList.add(product.zza());
        }
        return arrayList;
    }

    /**
     * Handles the completion of a purchase.
     *
     * @param purchase The purchase to handle.
     */
    public void a(Purchase purchase) {
        double priceWithoutCurrency = getPriceWithoutCurrency(this.u, this.v);
        if (this.purchaseListener != null) {
            this.B = true;
            this.purchaseListener.onProductPurchased(purchase.getOrderId(), purchase.getOriginalJson());
        }
        if (this.r) {
            this.billingClient.consumeAsync(
                    ConsumeParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
                    new ConsumeResponseListener() {
                        public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                            Log.d(AppPurchase.Tag, "onConsumeResponse: " + billingResult.getDebugMessage());
                        }
                    }
            );
        } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            AcknowledgePurchaseParams build = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
            if (!purchase.isAcknowledged()) {
                this.billingClient.acknowledgePurchase(
                        build,
                        new AcknowledgePurchaseResponseListener() {
                            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                                Log.d(AppPurchase.Tag, "onAcknowledgePurchaseResponse: " + billingResult.getDebugMessage());
                            }
                        }
                );
            }
        }
    }

    /**
     * Formats a price with the given currency.
     *
     * @param price    The price to format.
     * @param currency The currency code.
     * @return The formatted price string.
     */
    private String a(double price, String currency) {
        NumberFormat currencyInstance = NumberFormat.getCurrencyInstance();
        currencyInstance.setMaximumFractionDigits(0);
        currencyInstance.setCurrency(Currency.getInstance(currency));
        return currencyInstance.format(price);
    }

    /**
     * Converts a list of product IDs into a list of query product details params.
     *
     * @param listId      The list of product IDs.
     * @param styleBilling The billing style ("inapp" or "subs").
     * @return The list of query product details params.
     */
    @Deprecated
    private ArrayList<QueryProductDetailsParams.Product> a(List<String> listId, String styleBilling) {
        ArrayList<QueryProductDetailsParams.Product> arrayList = new ArrayList<>();
        for (String str : listId) {
            arrayList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(str).setProductType(styleBilling).build());
        }
        return arrayList;
    }
}
