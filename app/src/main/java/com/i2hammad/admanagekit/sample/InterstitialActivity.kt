package com.i2hammad.admanagekit.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.AdManagerCallback
import com.i2hammad.admanagekit.billing.AppPurchase
import com.i2hammad.admanagekit.billing.BillingListener
import com.i2hammad.admanagekit.ump.AdsConsentManager

class InterstitialActivity : AppCompatActivity() {
    lateinit var statusTextView: TextView

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


        val adsConsentManager: AdsConsentManager = AdsConsentManager.getInstance(this)

        AppPurchase.getInstance().setBillingListener(object : BillingListener {
            override fun onInitBillingFinished(resultCode: Int) {
                statusTextView.text = "Request UMP called."


                adsConsentManager.requestUMP(this@InterstitialActivity) {
                    statusTextView.text = "Interstitial ad requested."
                    loadInterstitialAd()
                }
            }
        }, 5 * 1000)


        var btnInterstitialAd: Button = findViewById(R.id.btnShowInterstitialAd)
        btnInterstitialAd.setOnClickListener {
            AdManager.getInstance().forceShowInterstitial(this, object : AdManagerCallback {
                override fun onNextAction() {
                    val nativeAdsIntent =
                        Intent(this@InterstitialActivity, MainActivity::class.java)
                    startActivity(nativeAdsIntent)
                }
            })

        }


    }

    fun loadInterstitialAd() {
        AdManager.getInstance().loadInterstitialAd(this, "ca-app-pub-3940256099942544/1033173712")
    }


}