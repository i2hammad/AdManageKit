package com.i2hammad.admanagekit.sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.i2hammad.admanagekit.AdConfig
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.AdManagerCallback
import com.i2hammad.admanagekit.admob.AppOpenManager
import com.i2hammad.admanagekit.admob.NativeAdManager
import com.i2hammad.admanagekit.billing.AppPurchase
import com.i2hammad.admanagekit.billing.BillingListener
import com.i2hammad.admanagekit.ump.AdsConsentManager

class SplashActivity : AppCompatActivity() {
    lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        statusTextView = findViewById(R.id.message)
        Log.d("SplashActivity", "onCreate: Activity created and UI initialized")

        initBilling()
    }

    private fun initBilling() {
        statusTextView.text = "App Purchase initialization started."
        Log.d("SplashActivity", "initBilling: Starting App Purchase initialization")

        if (AppPurchase.getInstance().isBillingInitialized) {
            Log.d("SplashActivity", "initBilling: Billing already initialized")
            statusTextView.text = "Billing already initialized. Proceeding to UMP request."
            requestUMP()
        } else {
            Log.d("SplashActivity", "initBilling: Initializing billing")
            AppPurchase.getInstance().setBillingListener(object : BillingListener {
                override fun onInitBillingFinished(resultCode: Int) {
                    Log.d("SplashActivity", "initBilling: Billing initialization finished with resultCode: $resultCode")
                    statusTextView.text = "App Purchase initialization finished."
                    requestUMP()
                }
            }, 5 * 1000)
        }
    }

    private fun requestUMP() {
        val adsConsentManager: AdsConsentManager = AdsConsentManager.getInstance(this)

        if (adsConsentManager.canRequestAds()) {
            Log.d("SplashActivity", "requestUMP: Consent already granted, can request ads")
            statusTextView.text = "Consent Already Requested. Ready to serve Ads"
            MyApplication.instance.initAds()
            runOnUiThread {
                Log.d("SplashActivity", "requestUMP: Forcing interstitial ad load")
                forceLoadAppOpen()
            }
        } else {
            Log.d("SplashActivity", "requestUMP: Requesting consent")
            statusTextView.text = "Consent Requested."

            adsConsentManager.requestUMP(
                this@SplashActivity, true, "EC60C39375F6619F5C03850A0E440646", true
            ) { isUserAccepted ->
                if (isUserAccepted) {
                    Log.d("SplashActivity", "requestUMP: Consent accepted by user")
                    statusTextView.text = "Consent shown and user accepted."
                    MyApplication.instance.initAds()
                } else {
                    Log.d("SplashActivity", "requestUMP: Consent not accepted or not shown")
                }

                if (adsConsentManager.canRequestAds()) {
                    Log.d("SplashActivity", "requestUMP: Can request ads after consent process")
                    forceLoadAppOpen()
                } else {
                    // Offline or consent failed - proceed without ads
                    Log.d("SplashActivity", "requestUMP: Cannot request ads (offline?), proceeding to next screen")
                    statusTextView.text = "Cannot load ads. Proceeding..."
                    onNextActionCalled()
                }
            }
        }
    }

    private fun forceLoadInterstitialAd() {
        val adUnitId = "ca-app-pub-3940256099942544/2247696110"
        Log.d("SplashActivity", "forceLoadInterstitialAd: Loading interstitial ad with unit ID: $adUnitId")

        statusTextView.text = "Loading interstitial ad..."

        AdManager.getInstance().loadInterstitialAdForSplash(this,
            "ca-app-pub-3940256099942544/1033173712",
            10 * 1000,
            object : AdManagerCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    Log.d("SplashActivity", "forceLoadInterstitialAd: Interstitial ad loaded, proceeding to show")
                    statusTextView.text = "Interstitial ad loaded."
                    if (!AdManager.getInstance().isDisplayingAd() && !isFinishing && !isDestroyed) {
                        forceShowSplashInterstitialAd()
                    }else{
                        statusTextView.text = "Either displaying ad or Activity is finishing/destroyed."

                        onNextActionCalled()
                    }
                }
            })
    }

    private fun onAdFailedToLoad(error: String) {
        Log.e("SplashActivity", "onAdFailedToLoad: $error")
        statusTextView.text = "Ad failed to load: $error}"
    }

    private fun forceShowSplashInterstitialAd() {
        Log.d("SplashActivity", "forceShowSplashAd: Attempting to show interstitial ad")
        statusTextView.text = "Showing interstitial ad..."

        AdManager.getInstance().showInterstitialIfReady(this, object : AdManagerCallback() {
            override fun onNextAction() {
                super.onNextAction()
                Log.d("SplashActivity", "forceShowSplashAd: Interstitial ad shown or dismissed")
                statusTextView.text = "Interstitial ad completed."
                onNextActionCalled()
            }
        })
    }



   private fun forceLoadAppOpen() {
        val appOpenManager = MyApplication.instance.appOpenManager
        Log.d("SplashActivity", "forceLoadAppOpen: Loading app open ad")
        statusTextView.text = "Loading app open ad..."

        appOpenManager?.fetchAd(object : AdLoadCallback() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                Log.d("SplashActivity", "forceLoadAppOpen: App open ad loaded")
                statusTextView.text = "App open ad loaded."
                showAppOpenAd()
            }

            override fun onFailedToLoad(error: LoadAdError?) {
                super.onFailedToLoad(error)
                Log.e("SplashActivity", "forceLoadAppOpen: Failed to load app open ad: ${error?.message}")
                statusTextView.text = "Failed to load app open ad."
                onNextActionCalled()
            }
        })
    }

    private fun showAppOpenAd() {
        Log.d("SplashActivity", "showAppOpenAd: Attempting to show app open ad")
        statusTextView.text = "Showing app open ad..."

        MyApplication.instance.appOpenManager?.forceShowAdIfAvailable(
            this,
            object : AdManagerCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    if (!AppOpenManager.isShowingAd()){
                        Log.d("SplashActivity", "showAppOpenAd: App open ad shown or dismissed")
                        statusTextView.text = "App open ad completed."
                        onNextActionCalled()
                    }
                }
            })
    }

    private fun onNextActionCalled() {
        Log.d("SplashActivity", "onNextActionCalled: Navigating to InterstitialActivity")
        statusTextView.text = "Navigating to next screen..."

        val nextIntent = Intent(this, InterstitialActivity::class.java)
        startActivity(nextIntent)
        finish()
    }
}