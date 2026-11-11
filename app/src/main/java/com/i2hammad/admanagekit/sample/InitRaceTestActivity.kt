package com.i2hammad.admanagekit.sample

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.AdManagerCallback
import com.i2hammad.admanagekit.admob.BannerAdView
import com.i2hammad.admanagekit.admob.NativeBannerSmall
import com.i2hammad.admanagekit.core.ad.AdProviderConfig

/**
 * Reproduces the "MobileAds.initialize must be called before using the Google Mobile Ads SDK"
 * crash: a screen opened directly (shortcut, tile, deep link) that loads ads in onCreate while
 * the app's MobileAds.initialize() is still running.
 *
 * Make init slow before launching, so the load really does start first:
 *   adb shell setprop debug.amk.init_delay_ms 6000   # init finishes 6 s after launch
 *   adb shell setprop debug.amk.init_delay_ms -1     # init never runs (timeout path)
 *   adb shell setprop debug.amk.init_delay_ms 0      # normal
 *   adb shell am force-stop com.i2hammad.admanagekit.sample
 *   adb shell am start -n com.i2hammad.admanagekit.sample/.InitRaceTestActivity
 *
 * Add `--ez direct true` to skip the AdMob → Yandex waterfall and load AdMob directly, the way an
 * app without a provider chain does (no fallback can fill the slot).
 *
 * Logcat tag: InitRaceTest.
 */
class InitRaceTestActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val direct = intent.getBooleanExtra("direct", false)
        if (direct) {
            AdProviderConfig.setNativeChain(emptyList())
            AdProviderConfig.setBannerChain(emptyList())
            AdProviderConfig.setInterstitialChain(emptyList())
        }
        // `--ez noretry true`: library default (the sample app turns auto-retry on).
        if (intent.getBooleanExtra("noretry", false)) {
            com.i2hammad.admanagekit.config.AdManageKitConfig.autoRetryFailedAds = false
        }

        status = TextView(this).apply { setPadding(32, 96, 32, 32); textSize = 15f }
        val nativeBanner = NativeBannerSmall(this)
        val banner = BannerAdView(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(status)
            addView(nativeBanner, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(banner, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })

        report("onCreate: MobileAds.isInitialized=${safeIsInitialized()}, init delay=${MyApplication.initDelayMs} ms, ${if (direct) "AdMob only" else "waterfall"} — loading ads now")

        // Interstitial mode: `--ez inter true [--ez preload true] [--ei show_at_ms N]`.
        // preload  = AdManager.loadInterstitialAd(context, id) in onCreate (the pool preload);
        // show_at_ms = call forceShowInterstitial N ms after onCreate (0 = right away).
        if (intent.getBooleanExtra("inter", false)) {
            runInterstitialTest()
            return
        }

        nativeBanner.loadNativeBannerAd(this, NATIVE_ID, callback("NativeBannerSmall"))
        banner.loadBanner(this, BANNER_ID, callback("BannerAdView"))
    }

    private fun runInterstitialTest() {
        val adManager = AdManager.getInstance()
        adManager.setAdUnitId(INTER_ID)
        if (intent.getBooleanExtra("preload", false)) {
            report("Interstitial: preload requested (isInitialized=${safeIsInitialized()})")
            adManager.loadInterstitialAd(this, INTER_ID)
            // Report when the preload lands in the pool.
            val started = SystemClock.uptimeMillis()
            val poll = object : Runnable {
                override fun run() {
                    if (isDestroyed) return
                    if (adManager.isReady(INTER_ID)) {
                        report("Interstitial: preloaded, ready in pool after ${SystemClock.uptimeMillis() - started} ms")
                    } else if (SystemClock.uptimeMillis() - started < 40_000) {
                        handler.postDelayed(this, 250)
                    } else {
                        report("Interstitial: preload never became ready (40 s)")
                    }
                }
            }
            handler.post(poll)
        }
        val showAt = intent.getIntExtra("show_at_ms", -1)
        if (showAt >= 0) {
            handler.postDelayed({
                if (isDestroyed) return@postDelayed
                report("Interstitial: forceShowInterstitial at +$showAt ms (isInitialized=${safeIsInitialized()}, ready=${adManager.isReady(INTER_ID)})")
                adManager.forceShowInterstitial(this, object : AdManagerCallback() {
                    override fun onAdLoaded() = report("Interstitial: onAdLoaded")
                    override fun onAdShowed() = report("Interstitial: SHOWED")
                    override fun onFailedToLoad(error: LoadAdError?) =
                        report("Interstitial: FAILED code=${error?.code} msg=${error?.message}")
                    override fun onNextAction() = report("Interstitial: onNextAction (closed or skipped)")
                })
            }, showAt.toLong())
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private fun callback(name: String) = object : AdLoadCallback() {
        override fun onAdLoaded() = report("$name LOADED (isInitialized=${safeIsInitialized()})")
        override fun onFailedToLoad(error: LoadAdError?) =
            report("$name FAILED code=${error?.code} msg=${error?.message}")
    }

    private fun report(line: String) {
        Log.i(TAG, line)
        status.append(line + "\n\n")
    }

    private fun safeIsInitialized() = try { MobileAds.isInitialized } catch (t: Throwable) { "error: $t" }

    companion object {
        private const val TAG = "InitRaceTest"
        private const val NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
        private const val BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
        private const val INTER_ID = "ca-app-pub-3940256099942544/1033173712"
    }
}
