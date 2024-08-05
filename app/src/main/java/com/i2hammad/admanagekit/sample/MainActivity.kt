package com.i2hammad.admanagekit.sample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.i2hammad.admanagekit.admob.BannerAdView
import com.i2hammad.admanagekit.admob.NativeBannerMedium
import com.i2hammad.admanagekit.admob.NativeBannerSmall
import com.i2hammad.admanagekit.admob.NativeLarge
import com.i2hammad.admanagekit.billing.AppPurchase
import com.i2hammad.admanagekit.billing.BillingListener
import com.i2hammad.admanagekit.ump.AdsConsentManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        loadAds()


    }


    fun loadAds() {



        var nativeBannerSmall: NativeBannerSmall = findViewById(R.id.nativeBannerSmall)
        nativeBannerSmall.loadNativeBannerAd(this,"ca-app-pub-3940256099942544/2247696110")

        var nativeBannerMedium: NativeBannerMedium = findViewById(R.id.nativeBannerMedium)
        nativeBannerMedium.loadNativeBannerAd(this,"ca-app-pub-3940256099942544/2247696110")


        var nativeLarge: NativeLarge = findViewById(R.id.nativeLarge)
        nativeLarge.loadNativeAds(this,"ca-app-pub-3940256099942544/2247696110")


    }
}