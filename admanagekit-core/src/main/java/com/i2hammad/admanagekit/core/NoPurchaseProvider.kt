package com.i2hammad.admanagekit.core

class NoPurchaseProvider : AppPurchaseProvider {
    override fun isPurchased(): Boolean {
        return false // Always show ads by default
    }
}