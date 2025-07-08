package com.i2hammad.admanagekit.sample

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.i2hammad.admanagekit.admob.AppOpenManager
import com.i2hammad.admanagekit.billing.AppPurchase
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.billing.BillingPurchaseProvider
import com.i2hammad.admanagekit.billing.PurchaseItem
import com.i2hammad.admanagekit.billing.PurchaseListener
import com.i2hammad.admanagekit.core.NoPurchaseProvider

class MyApplication : Application() {

    var appOpenManager: AppOpenManager? = null

    override fun onCreate() {
        super.onCreate()

        //If you want to use billing feature must use billing provider
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
        //If you do not want to use billing library for it
//        BillingConfig.setPurchaseProvider(NoPurchaseProvider())
        initBilling()
        appOpenManager = AppOpenManager(this, "ca-app-pub-3940256099942544/9257395921")
        appOpenManager?.disableAppOpenWithActivity(SplashActivity::class.java)

    }


    private fun initBilling() {


        val listPurchaseItem = listOf(
            PurchaseItem("life_time", AppPurchase.TYPE_IAP.PURCHASE),
            PurchaseItem("sub_monthly", AppPurchase.TYPE_IAP.SUBSCRIPTION),
            PurchaseItem("sub_half_yearly", AppPurchase.TYPE_IAP.SUBSCRIPTION),
            PurchaseItem("sub_yearly", AppPurchase.TYPE_IAP.SUBSCRIPTION)
        )

        AppPurchase.getInstance().initBilling(this, listPurchaseItem)

        AppPurchase.getInstance().setPurchaseListener(object : PurchaseListener {
            override fun displayErrorMessage(errorMessage: String?) {

            }

            override fun onProductPurchased(orderId: String?, originalJson: String?) {

            }

            override fun onUserCancelBilling() {}
        })
    }


    fun initAds() {

        val testDeviceIds: List<String> = mutableListOf(
            "EC60C39375F6619F5C03850A0E440646"
        )
        val configuration: RequestConfiguration =
            RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.initialize(this)

    }

    companion object {
        lateinit var instance: MyApplication
            private set
    }


    init {
        instance = this
    }

}