package com.i2hammad.admanagekit.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.AdManagerCallback
import com.i2hammad.admanagekit.admob.BannerAdView
import com.i2hammad.admanagekit.billing.AppPurchase
import com.i2hammad.admanagekit.billing.BillingListener
import com.i2hammad.admanagekit.ump.AdsConsentManager

class InterstitialActivity : AppCompatActivity() {
    lateinit var statusTextView: TextView
    lateinit var btnInterstitialAd: Button

//    var myApplication: MyApplication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interstitial)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        statusTextView = findViewById<TextView>(R.id.statusTextView)
//        myApplication = MyApplication.getInstance()!!

        btnInterstitialAd = findViewById(R.id.btnShowInterstitialAd)
        btnInterstitialAd.isEnabled = false

        loadBannerAd()
        loadInterstitialAd()

        btnInterstitialAd.setOnClickListener {
            AdManager.getInstance().forceShowInterstitial(this, object : AdManagerCallback() {
                override fun onNextAction() {
                    val nativeAdsIntent =
                        Intent(this@InterstitialActivity, MainActivity::class.java)
                    startActivity(nativeAdsIntent)
                }
            })

        }


    }


    fun loadBannerAd() {
        var bannerAdView: BannerAdView = findViewById(R.id.bannerAdView)
        bannerAdView.loadBanner(this, "ca-app-pub-3940256099942544/9214589741")


//
        // for Collapsible Banner Ad
//        bannerAdView.loadCollapsibleBanner(this, "ca-app-pub-3940256099942544/2014213617", true)

    }

    fun loadInterstitialAd() {
        statusTextView.text = "Interstitial ad requested."
        btnInterstitialAd.isEnabled = true
        AdManager.getInstance().loadInterstitialAd(this,
            "ca-app-pub-3940256099942544/1033173712",
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(onAdLoaded: InterstitialAd) {
                    super.onAdLoaded(onAdLoaded)
                    statusTextView.text = "Interstitial ad loaded successfully."
                    btnInterstitialAd.text = "Show interstitial Ad and Load Native Ads"

                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    statusTextView.text = "Interstitial ad failed to load."
                    btnInterstitialAd.text = "Load Native Ads"
                }
            })
    }


}