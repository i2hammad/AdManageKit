package com.i2hammad.admanagekit.billing

import com.i2hammad.admanagekit.core.AppPurchaseProvider
import com.i2hammad.admanagekit.core.NoPurchaseProvider

object BillingConfig {

    private var purchaseProvider: AppPurchaseProvider = NoPurchaseProvider()
    fun setPurchaseProvider(provider: AppPurchaseProvider) {
        purchaseProvider = provider
    }

    fun getPurchaseProvider(): AppPurchaseProvider {
        return purchaseProvider
    }
}