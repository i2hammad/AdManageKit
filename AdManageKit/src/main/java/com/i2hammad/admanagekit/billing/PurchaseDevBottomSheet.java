package com.i2hammad.admanagekit.billing;

import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.billingclient.api.ProductDetails;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.i2hammad.admanagekit.R;

public class PurchaseDevBottomSheet extends BottomSheetDialog {
    private ProductDetails productDetails;
    private int typeIap;
    private TextView txtTitle;
    private TextView txtDescription;
    private TextView txtId;
    private TextView txtPrice;
    private TextView txtContinuePurchase;
    private PurchaseListener purchaseListener;

    public PurchaseDevBottomSheet(int typeIap, ProductDetails productDetails, @NonNull Context context, PurchaseListener purchaseListener) {
        super(context);
        this.productDetails = productDetails;
        this.typeIap = typeIap;
        this.purchaseListener = purchaseListener;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_billing_test);
        this.txtTitle = (TextView) findViewById(R.id.txtTitle);
        this.txtDescription = (TextView) findViewById(R.id.txtDescription);
        this.txtId = (TextView) findViewById(R.id.txtId);
        this.txtPrice = (TextView) findViewById(R.id.txtPrice);
        this.txtContinuePurchase = (TextView) findViewById(R.id.txtContinuePurchase);
        ProductDetails productDetails = this.productDetails;
        if (productDetails == null) {
            return;
        }
        this.txtTitle.setText(productDetails.getTitle());
        this.txtDescription.setText(this.productDetails.getDescription());
        this.txtId.setText(this.productDetails.getProductId());
        if (this.typeIap == 1) {
            this.txtPrice.setText(this.productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice());
        } else {
            this.txtPrice.setText(((ProductDetails.PricingPhase) ((ProductDetails.SubscriptionOfferDetails) this.productDetails.getSubscriptionOfferDetails().get(0)).getPricingPhases().getPricingPhaseList().get(0)).getFormattedPrice());
        }
        this.txtContinuePurchase.setOnClickListener(v -> {
            if (this.purchaseListener != null) {
                AppPurchase.getInstance().setPurchase(true);
                this.purchaseListener.onProductPurchased(this.productDetails.getProductId(), "{\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}', parsedJson={\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}, productId='android.test.purchased', productType='inapp', title='Tiêu đề mẫu', productDetailsToken='AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC', subscriptionOfferDetails=null}");
            }
            dismiss();
        });
    }

    protected void onStart() {
//        super.onStart();
//        getWindow().setLayout(-1, -2);
        super.onStart();
    }
}