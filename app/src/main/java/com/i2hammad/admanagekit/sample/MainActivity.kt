package com.i2hammad.admanagekit.sample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.i2hammad.admanagekit.admob.BannerAdView
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

        val adsConsentManager: AdsConsentManager = AdsConsentManager.getInstance(this)

        AppPurchase.getInstance().setBillingListener(object : BillingListener {
            override fun onInitBillingFinished(resultCode: Int) {
                adsConsentManager.requestUMP(this@MainActivity) {
                    loadAds()
                }
            }
        }, 5 * 1000)


    }


    fun loadAds() {


        var bannerAdView: BannerAdView = findViewById(R.id.bannerAdView)
        bannerAdView.loadBanner(this, "ca-app-pub-3940256099942544/9214589741")

        // for Collapsible Banner Ad
        bannerAdView.loadCollapsibleBanner(this, "ca-app-pub-3940256099942544/2014213617", true)

    }
}