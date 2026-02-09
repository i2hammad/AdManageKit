package com.i2hammad.admanagekit.sample

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AdProviderConfig
import com.i2hammad.admanagekit.core.ad.AdUnitMapping
import com.i2hammad.admanagekit.core.ad.AppOpenAdProvider
import com.i2hammad.admanagekit.core.ad.BannerAdProvider
import com.i2hammad.admanagekit.core.ad.InterstitialAdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.core.ad.RewardedAdProvider
import com.i2hammad.admanagekit.waterfall.AppOpenWaterfall
import com.i2hammad.admanagekit.waterfall.BannerWaterfall
import com.i2hammad.admanagekit.waterfall.InterstitialWaterfall
import com.i2hammad.admanagekit.waterfall.NativeWaterfall
import com.i2hammad.admanagekit.waterfall.RewardedWaterfall

class WaterfallTestActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView

    private var interstitialWaterfall: InterstitialWaterfall? = null
    private var bannerWaterfall: BannerWaterfall? = null
    private var nativeWaterfall: NativeWaterfall? = null
    private var appOpenWaterfall: AppOpenWaterfall? = null
    private var rewardedWaterfall: RewardedWaterfall? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waterfall_test)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)

        setupInterstitial()
        setupBanner()
        setupNative()
        setupAppOpen()
        setupRewarded()
    }

    // ======================== Interstitial ========================

    private fun setupInterstitial() {
        val btnLoad = findViewById<Button>(R.id.btnLoadInterstitial)
        val btnShow = findViewById<Button>(R.id.btnShowInterstitial)

        btnLoad.setOnClickListener {
            log("--- Interstitial: loading waterfall ---")
            btnLoad.isEnabled = false

            interstitialWaterfall = InterstitialWaterfall(
                providers = AdProviderConfig.getInterstitialChain(),
                adUnitResolver = { provider -> AdUnitMapping.getAdUnitId("ca-app-pub-3940256099942544/1033173712", provider) ?: "ca-app-pub-3940256099942544/1033173712".takeIf { provider == AdProvider.ADMOB } }
            )

            interstitialWaterfall?.load(this, object : InterstitialAdProvider.InterstitialAdCallback {
                override fun onAdLoaded() {
                    log("Interstitial: loaded successfully")
                    btnShow.isEnabled = true
                    btnLoad.isEnabled = true
                }

                override fun onAdFailedToLoad(error: AdKitAdError) {
                    log("Interstitial: all providers failed — ${error.message}")
                    btnLoad.isEnabled = true
                }
            })
        }

        btnShow.setOnClickListener {
            btnShow.isEnabled = false
            interstitialWaterfall?.show(this, object : InterstitialAdProvider.InterstitialShowCallback {
                override fun onAdShowed() {
                    log("Interstitial: showed")
                }

                override fun onAdDismissed() {
                    log("Interstitial: dismissed")
                }

                override fun onAdFailedToShow(error: AdKitAdError) {
                    log("Interstitial: show failed — ${error.message}")
                }

                override fun onAdImpression() {
                    log("Interstitial: impression")
                }

                override fun onPaidEvent(adValue: AdKitAdValue) {
                    log("Interstitial: paid ${adValue.valueMicros} ${adValue.currencyCode}")
                }
            })
        }
    }

    // ======================== Banner ========================

    private fun setupBanner() {
        val btnLoad = findViewById<Button>(R.id.btnLoadBanner)
        val container = findViewById<FrameLayout>(R.id.bannerContainer)

        btnLoad.setOnClickListener {
            log("--- Banner: loading waterfall ---")
            btnLoad.isEnabled = false
            container.removeAllViews()

            bannerWaterfall = BannerWaterfall(
                providers = AdProviderConfig.getBannerChain(),
                adUnitResolver = { provider -> AdUnitMapping.getAdUnitId("ca-app-pub-3940256099942544/9214589741", provider) ?: "ca-app-pub-3940256099942544/9214589741".takeIf { provider == AdProvider.ADMOB } }
            )

            bannerWaterfall?.load(this, object : BannerAdProvider.BannerAdCallback {
                override fun onBannerLoaded(bannerView: View) {
                    log("Banner: loaded successfully")
                    container.removeAllViews()
                    container.addView(bannerView)
                    btnLoad.isEnabled = true
                }

                override fun onBannerFailedToLoad(error: AdKitAdError) {
                    log("Banner: all providers failed — ${error.message}")
                    btnLoad.isEnabled = true
                }

                override fun onBannerImpression() {
                    log("Banner: impression")
                }

                override fun onPaidEvent(adValue: AdKitAdValue) {
                    log("Banner: paid ${adValue.valueMicros} ${adValue.currencyCode}")
                }
            })
        }
    }

    // ======================== Native ========================

    private fun setupNative() {
        val btnLoad = findViewById<Button>(R.id.btnLoadNative)
        val container = findViewById<FrameLayout>(R.id.nativeContainer)

        btnLoad.setOnClickListener {
            log("--- Native: loading waterfall ---")
            btnLoad.isEnabled = false
            container.removeAllViews()

            nativeWaterfall = NativeWaterfall(
                providers = AdProviderConfig.getNativeChain(),
                adUnitResolver = { provider -> AdUnitMapping.getAdUnitId("ca-app-pub-3940256099942544/2247696110", provider) ?: "ca-app-pub-3940256099942544/2247696110".takeIf { provider == AdProvider.ADMOB } }
            )

            nativeWaterfall?.load(this, object : NativeAdProvider.NativeAdCallback {
                override fun onNativeAdLoaded(adView: View, nativeAdRef: Any) {
                    log("Native: loaded successfully")
                    container.removeAllViews()
                    container.addView(adView)
                    btnLoad.isEnabled = true
                }

                override fun onNativeAdFailedToLoad(error: AdKitAdError) {
                    log("Native: all providers failed — ${error.message}")
                    btnLoad.isEnabled = true
                }

                override fun onNativeAdImpression() {
                    log("Native: impression")
                }

                override fun onPaidEvent(adValue: AdKitAdValue) {
                    log("Native: paid ${adValue.valueMicros} ${adValue.currencyCode}")
                }
            })
        }
    }

    // ======================== App Open ========================

    private fun setupAppOpen() {
        val btnLoad = findViewById<Button>(R.id.btnLoadAppOpen)
        val btnShow = findViewById<Button>(R.id.btnShowAppOpen)

        btnLoad.setOnClickListener {
            log("--- App Open: loading waterfall ---")
            btnLoad.isEnabled = false

            appOpenWaterfall = AppOpenWaterfall(
                providers = AdProviderConfig.getAppOpenChain(),
                adUnitResolver = { provider -> AdUnitMapping.getAdUnitId("ca-app-pub-3940256099942544/9257395921", provider) ?: "ca-app-pub-3940256099942544/9257395921".takeIf { provider == AdProvider.ADMOB } }
            )

            appOpenWaterfall?.load(this, object : AppOpenAdProvider.AppOpenAdCallback {
                override fun onAdLoaded() {
                    log("App Open: loaded successfully")
                    btnShow.isEnabled = true
                    btnLoad.isEnabled = true
                }

                override fun onAdFailedToLoad(error: AdKitAdError) {
                    log("App Open: all providers failed — ${error.message}")
                    btnLoad.isEnabled = true
                }
            })
        }

        btnShow.setOnClickListener {
            btnShow.isEnabled = false
            appOpenWaterfall?.show(this, object : AppOpenAdProvider.AppOpenShowCallback {
                override fun onAdShowed() {
                    log("App Open: showed")
                }

                override fun onAdDismissed() {
                    log("App Open: dismissed")
                }

                override fun onAdFailedToShow(error: AdKitAdError) {
                    log("App Open: show failed — ${error.message}")
                }

                override fun onAdImpression() {
                    log("App Open: impression")
                }

                override fun onPaidEvent(adValue: AdKitAdValue) {
                    log("App Open: paid ${adValue.valueMicros} ${adValue.currencyCode}")
                }
            })
        }
    }

    // ======================== Rewarded ========================

    private fun setupRewarded() {
        val btnLoad = findViewById<Button>(R.id.btnLoadRewarded)
        val btnShow = findViewById<Button>(R.id.btnShowRewarded)

        btnLoad.setOnClickListener {
            log("--- Rewarded: loading waterfall ---")
            btnLoad.isEnabled = false

            rewardedWaterfall = RewardedWaterfall(
                providers = AdProviderConfig.getRewardedChain(),
                adUnitResolver = { provider -> AdUnitMapping.getAdUnitId("ca-app-pub-3940256099942544/5224354917", provider) ?: "ca-app-pub-3940256099942544/5224354917".takeIf { provider == AdProvider.ADMOB } }
            )

            rewardedWaterfall?.load(this, object : RewardedAdProvider.RewardedAdCallback {
                override fun onAdLoaded() {
                    log("Rewarded: loaded successfully")
                    btnShow.isEnabled = true
                    btnLoad.isEnabled = true
                }

                override fun onAdFailedToLoad(error: AdKitAdError) {
                    log("Rewarded: all providers failed — ${error.message}")
                    btnLoad.isEnabled = true
                }
            })
        }

        btnShow.setOnClickListener {
            btnShow.isEnabled = false
            rewardedWaterfall?.show(this, object : RewardedAdProvider.RewardedShowCallback {
                override fun onAdShowed() {
                    log("Rewarded: showed")
                }

                override fun onAdDismissed() {
                    log("Rewarded: dismissed")
                }

                override fun onAdFailedToShow(error: AdKitAdError) {
                    log("Rewarded: show failed — ${error.message}")
                }

                override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
                    log("Rewarded: earned $rewardAmount x $rewardType")
                }

                override fun onAdImpression() {
                    log("Rewarded: impression")
                }

                override fun onPaidEvent(adValue: AdKitAdValue) {
                    log("Rewarded: paid ${adValue.valueMicros} ${adValue.currencyCode}")
                }
            })
        }
    }

    // ======================== Logging ========================

    private fun log(message: String) {
        android.util.Log.d("WaterfallTest", message)
        runOnUiThread {
            tvLog.append("$message\n")
            logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        interstitialWaterfall?.destroy()
        bannerWaterfall?.destroy()
        nativeWaterfall?.destroy()
        appOpenWaterfall?.destroy()
        rewardedWaterfall?.destroy()
    }
}
