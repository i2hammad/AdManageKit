package com.i2hammad.admanagekit.sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.AdManagerCallback
import com.i2hammad.admanagekit.admob.BannerAdView
import com.i2hammad.admanagekit.admob.NativeAdManager
import com.i2hammad.admanagekit.admob.NativeBannerMedium
import com.i2hammad.admanagekit.admob.NativeBannerSmall
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils
import android.util.Log

class InterstitialActivity : AppCompatActivity() {
    lateinit var statusTextView: TextView
    lateinit var btnInterstitialAd: Button
    lateinit var bannerAdView: BannerAdView
    lateinit var bannerContainer: FrameLayout

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


        // Enable debug overlay for this activity if in debug mode
        if (AdManageKitConfig.debugMode) {
            AdDebugUtils.enableDebugOverlay(this, true)
            Log.d("InterstitialActivity", "Debug overlay enabled for InterstitialActivity")
        }
        
        // Native ad caching is now controlled by configuration
        NativeAdManager.enableCachingNativeAds = AdManageKitConfig.enableSmartPreloading
        var nativeBannerMedium: NativeBannerMedium = findViewById(R.id.nativeBannerMedium)
        nativeBannerMedium.loadNativeBannerAd(this, "ca-app-pub-3940256099942544/2247696110", 
            useCachedAd = AdManageKitConfig.enableSmartPreloading, object : AdLoadCallback() {
                override fun onAdLoaded() {
                    Log.d("InterstitialActivity", "✅ NativeBannerMedium loaded in InterstitialActivity")
                }
                
                override fun onFailedToLoad(error: AdError?) {
                    Log.e("InterstitialActivity", "❌ NativeBannerMedium failed in InterstitialActivity: ${error?.message}")
                }
            })

        statusTextView = findViewById<TextView>(R.id.statusTextView)

        btnInterstitialAd = findViewById(R.id.btnShowInterstitialAd)
        btnInterstitialAd.isEnabled = false


        loadBannerAd()
        loadInterstitialAd()

        btnInterstitialAd.setOnClickListener {
            AdManager.getInstance().forceShowInterstitialWithDialog(this, object : AdManagerCallback() {
                override fun onNextAction() {
                    val nativeAdsIntent =
                        Intent(this@InterstitialActivity, MainActivity::class.java)
                    startActivity(nativeAdsIntent)
                }
            })

        }


    }


    fun loadBannerAd() {
        bannerAdView = findViewById(R.id.bannerAdView)
        bannerContainer = findViewById(R.id.lay_Banner)
        bannerAdView.loadBanner(
            this,
            "ca-app-pub-3940256099942544/9214589741",
            object : AdLoadCallback() {
                override fun onFailedToLoad(error: AdError?) {
                    super.onFailedToLoad(error)
                    bannerContainer.visibility = View.GONE

                }
            })


//
        // for Collapsible Banner Ad
//        bannerAdView.loadCollapsibleBanner(this, "ca-app-pub-3940256099942544/2014213617", true,object :
//            AdLoadCallback(){
//            override fun onFailedToLoad(error: AdError?) {
//                super.onFailedToLoad(error)
//                bannerContainer.visibility= View.GONE
//
//            }
//        })

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

    override fun onResume() {
        super.onResume()
        bannerAdView.resumeAd()
    }

    override fun onPause() {
        bannerAdView.pauseAd()
        super.onPause()
    }

    override fun onDestroy() {
        bannerAdView.destroyAd()
        super.onDestroy()
    }
}