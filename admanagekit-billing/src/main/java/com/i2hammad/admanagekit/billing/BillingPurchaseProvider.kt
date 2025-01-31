package com.i2hammad.admanagekit.billing

import com.i2hammad.admanagekit.core.AppPurchaseProvider

class BillingPurchaseProvider : AppPurchaseProvider {
    override fun isPurchased(): Boolean {
        return AppPurchase.getInstance().isPurchased
    }
}