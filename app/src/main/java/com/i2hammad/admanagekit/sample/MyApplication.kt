package com.i2hammad.admanagekit.sample

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.i2hammad.admanagekit.admob.AppOpenManager
import com.i2hammad.admanagekit.billing.BillingConfig
import com.i2hammad.admanagekit.billing.BillingPurchaseProvider
import com.i2hammad.admanagekit.core.NoPurchaseProvider

class MyApplication : Application() {

    var appOpenManager: AppOpenManager? = null

    override fun onCreate() {
        super.onCreate()

        // you want to use billing feature must use billing provider
        BillingConfig.setPurchaseProvider(BillingPurchaseProvider())
        appOpenManager = AppOpenManager(this, "ca-app-pub-3940256099942544/9257395921")
        appOpenManager?.disableAppOpenWithActivity(SplashActivity::class.java)
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