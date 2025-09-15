package com.i2hammad.admanagekit.core

import com.i2hammad.admanagekit.core.AppPurchaseProvider
import com.i2hammad.admanagekit.core.NoPurchaseProvider

object BillingConfig {

    private var purchaseProvider: AppPurchaseProvider = NoPurchaseProvider()
    @JvmStatic
    fun setPurchaseProvider(provider: AppPurchaseProvider) {
        purchaseProvider = provider
    }

    @JvmStatic
    fun getPurchaseProvider(): AppPurchaseProvider {
        return purchaseProvider
    }
}