package com.i2hammad.admanagekit.billing

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.i2hammad.admanagekit.R

class PurchaseDevBottomSheet(
    private val typeIap: Int,
    private val productDetails: ProductDetails,
    context: Context,
    private val purchaseListener: PurchaseListener?
) : BottomSheetDialog(context) {
    private lateinit var txtTitle: TextView
    private lateinit var txtDescription: TextView
    private lateinit var txtId: TextView
    private lateinit var txtPrice: TextView
    private lateinit var txtContinuePurchase: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_billing_test)

        txtTitle = findViewById(R.id.txtTitle)!!
        txtDescription = findViewById(R.id.txtDescription)!!
        txtId = findViewById(R.id.txtId)!!
        txtPrice = findViewById(R.id.txtPrice)!!
        txtContinuePurchase = findViewById(R.id.txtContinuePurchase)!!
        productDetails?.let { details ->
            txtTitle.text = details.title
            txtDescription.text = details.description
            txtId.text = details.productId

            txtPrice.text = if (typeIap == 1) {
                details.oneTimePurchaseOfferDetails?.formattedPrice
            } else {
                val subscriptionDetails = details.subscriptionOfferDetails?.get(0)
                subscriptionDetails?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice
            }

            txtContinuePurchase.setOnClickListener {
                purchaseListener?.let {
                    AppPurchase.getInstance().setPurchase(true)
                    it.onProductPurchased(
                        details.productId,
                        "{\"productId\":\"android.test.purchased\",\"type\":\"inapp\",\"title\":\"Tiêu đề mẫu\",\"description\":\"Mô tả mẫu về sản phẩm: android.test.purchased.\",\"skuDetailsToken\":\"AEuhp4Izz50wTvd7YM9wWjPLp8hZY7jRPhBEcM9GAbTYSdUM_v2QX85e8UYklstgqaRC\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":23207002450,\"priceCurrencyCode\":\"VND\",\"formattedPrice\":\"23.207 ₫\"}}"
                    )
                }
                dismiss()
            }
        }
    }

    override fun onStart() {
//        super.onStart();
//        getWindow().setLayout(-1, -2);
        super.onStart()
    }
}