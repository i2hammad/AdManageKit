package com.i2hammad.admanagekit.sample

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.i2hammad.admanagekit.admob.AppOpenManager

class MyApplication : Application() {


    fun initAds() {

        val testDeviceIds: List<String> = mutableListOf(
            "EC60C39375F6619F5C03850A0E440646"
        )
        val configuration: RequestConfiguration =
            RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)
        MobileAds.initialize(this)

        AppOpenManager(this, "ca-app-pub-3940256099942544/9257395921")
    }

    companion object {
        lateinit var instance: MyApplication
            private set
    }


    init {
        instance = this
    }

}