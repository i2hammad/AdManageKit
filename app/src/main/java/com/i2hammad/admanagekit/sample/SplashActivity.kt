package com.i2hammad.admanagekit.sample

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdError
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.AdManagerCallback
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

//        Log.e("splash", "On Create Called")
        initBilling()

    }

    private fun initBilling() {


        statusTextView.text = "App Purchase initialization started."

//        Log.e("splash", "App Purchase initialization status  ${AppPurchase.getInstance().initBillingFinish}")

        if (AppPurchase.getInstance().initBillingFinish) {
            // Already initialized request UMP
            requestUMP()

        } else {
//            Log.e("splash", "App Purchase initialize requested  ")
            AppPurchase.getInstance().setBillingListener(object : BillingListener {
                override fun onInitBillingFinished(resultCode: Int) {

//                    Log.e("splash", "App Purchase initialization finished. ${AppPurchase.getInstance().initBillingFinish}")
                    statusTextView.text = "App Purchase initialization finished."
                    requestUMP()
                }
            }, 5 * 1000)
        }
    }

    private fun requestUMP() {

        val adsConsentManager: AdsConsentManager = AdsConsentManager.getInstance(this)
//        Log.e("splash", "can Request Ads " + adsConsentManager.canRequestAds )
        if (adsConsentManager.canRequestAds()) {
            // Already requested
//            Log.e("splash", "Consent already requested " )

            statusTextView.text = "Consent Already Requested. Ready to serve Ads"
            MyApplication.instance.initAds()
            runOnUiThread {

                // moveToNext
                // onNextActionCalled()

                // onlyIfAvailable
                // showAppOpenAd()

                // force load app open
                // forceLoadAppOpen()


                //force loadInterstitialAd
                forceLoadInterstitialAd()


            }

        } else {

            statusTextView.text = "Consent Requested."
//            Log.e("splash", "Consent requested " )


            adsConsentManager.requestUMP(
                this@SplashActivity, true, "EC60C39375F6619F5C03850A0E440646", false
            ) { isUserAccepted ->
                if (isUserAccepted) {
//                    Log.e("splash", "Consent shown and accepted by user: ", )
//                    Log.e("splash", "can Request Ads " + adsConsentManager.canRequestAds )


                    MyApplication.instance.initAds()
                    runOnUiThread {
                        statusTextView.text = "Consent shown and user accepted."
                    }
                } else {
                    // not shown

//                    Log.e("splash", "Not Shown Called by ads may be requested: ", )
//                    Log.e("splash", "can Request Ads " + adsConsentManager.canRequestAds )

                }

                // you will be notified when ads can be requested
                if (adsConsentManager.canRequestAds()) {
                    // moveToNext
                    // onNextActionCalled()

                    // onlyIfAvailable
                    // showAppOpenAd()

                    //force show app open
                    forceLoadInterstitialAd()
                }


            }
        }


    }


    private fun forceLoadInterstitialAd() {
        AdManager.getInstance().loadInterstitialAdForSplash(this,
            "ca-app-pub-3940256099942544/1033173712",
            10 * 1000,
            object : AdManagerCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    // ready to server ad
                    if (!AdManager.getInstance().isDisplayingAd() && !isFinishing && !isDestroyed) {
                        forceShowSplashInterstitialAd()
                    }
                }
            })
    }

    private fun forceShowSplashInterstitialAd() {
        AdManager.getInstance().forceShowInterstitial(this, object : AdManagerCallback() {
            override fun onNextAction() {
                super.onNextAction()
                // ready to server ad
                onNextActionCalled()
            }
        })
    }

    private fun forceLoadAppOpen() {
        val appOpenManager = MyApplication.instance.appOpenManager;
        appOpenManager?.fetchAd(object : AdLoadCallback() {
            override fun onAdLoaded() {
                super.onAdLoaded()
                showAppOpenAd()
            }

            override fun onFailedToLoad(error: AdError?) {
                super.onFailedToLoad(error)
                onNextActionCalled()
            }
        })
    }

    private fun showAppOpenAd() {

        // make sure you have skipped SplashActivity on AppOpenManager in MyApplication to avoid default showAppOpenAd() behaviour
        // appOpenManager?.disableAppOpenWithActivity(SplashActivity::class.java)

        MyApplication.instance.appOpenManager?.forceShowAdIfAvailable(this,
            object : AdManagerCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    onNextActionCalled()
                }
            })

    }

    private fun onNextActionCalled() {
        val nextIntent = Intent(this, InterstitialActivity::class.java)
        startActivity(nextIntent)
        finish()
    }
}