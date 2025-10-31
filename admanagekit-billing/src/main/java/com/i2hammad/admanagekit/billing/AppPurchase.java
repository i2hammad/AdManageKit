package com.i2hammad.admanagekit.billing;

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
    private boolean consumePurchase = false;
    private String oldPrice = "3.50$";
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

    public void setConsumePurchase(boolean consumePurchase) {
        this.consumePurchase = consumePurchase;
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

    public boolean isPurchased() {
        return isPurchased || !stringList.isEmpty() || !purchaseResultList.isEmpty();
    }

    public boolean isPurchased(Context context) {
        return isPurchased();
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
                            isPurchased = true;
                            ProductDetails details = productDetailsMap.get(productId);
                            if (details != null && details.getProductType().equals(BillingClient.ProductType.INAPP) && consumePurchase) {
                                consumePurchase(purchase);
                            }
                            if (details != null && details.getProductType().equals(BillingClient.ProductType.SUBS)) {
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
                isPurchased = true;
                if (productDetailsMap.get(productId) != null && productDetailsMap.get(productId).getProductType().equals(BillingClient.ProductType.SUBS)) {
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

    private void consumePurchase(Purchase purchase) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
            @Override
            public void onConsumeResponse(@NonNull BillingResult billingResult, @NonNull String purchaseToken) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(Tag, "Purchase consumed successfully: " + purchaseToken);
                    verifyPurchased(false);
                } else {
                    Log.e(Tag, "Failed to consume purchase: " + billingResult.getDebugMessage());
                }
            }
        });
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