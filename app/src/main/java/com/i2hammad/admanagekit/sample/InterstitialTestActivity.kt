package com.i2hammad.admanagekit.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.AdManagerCallback
import com.i2hammad.admanagekit.admob.InterstitialAdBuilder
import com.i2hammad.admanagekit.config.AdLoadingStrategy
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils

/**
 * Comprehensive test activity for Interstitial Ads
 * Tests all loading strategies, builder options, and edge cases
 */
class InterstitialTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "InterstitialTest"
        private const val TEST_AD_UNIT = "ca-app-pub-3940256099942544/1033173712"
    }

    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var statusTextView: TextView

    private var testCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_interstitial_test)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Enable debug overlay
        if (AdManageKitConfig.debugMode) {
            AdDebugUtils.enableDebugOverlay(this, true)
        }

        logTextView = findViewById(R.id.logTextView)
        scrollView = findViewById(R.id.scrollView)
        statusTextView = findViewById(R.id.statusTextView)

        setupButtons()
        log("=== Interstitial Ad Test Activity ===")
        log("Ad Unit: $TEST_AD_UNIT")
    }

    private fun setupButtons() {
        // === LOADING SECTION ===
        findViewById<Button>(R.id.btnPreload).setOnClickListener { testPreload() }
        findViewById<Button>(R.id.btnLoadWithCallback).setOnClickListener { testLoadWithCallback() }
        findViewById<Button>(R.id.btnCheckIsReady).setOnClickListener { testCheckIsReady() }

        // === LOADING STRATEGIES ===
        findViewById<Button>(R.id.btnStrategyOnDemand).setOnClickListener { testStrategyOnDemand() }
        findViewById<Button>(R.id.btnStrategyOnlyCache).setOnClickListener { testStrategyOnlyCache() }
        findViewById<Button>(R.id.btnStrategyHybrid).setOnClickListener { testStrategyHybrid() }
        findViewById<Button>(R.id.btnStrategyFreshWithFallback).setOnClickListener { testStrategyFreshWithFallback() }

        // === BUILDER OPTIONS ===
        findViewById<Button>(R.id.btnBasicShow).setOnClickListener { testBasicShow() }
        findViewById<Button>(R.id.btnForceShow).setOnClickListener { testForceShow() }
        findViewById<Button>(R.id.btnWithTimeout).setOnClickListener { testWithTimeout() }
        findViewById<Button>(R.id.btnWithCallbacks).setOnClickListener { testWithCallbacks() }
        findViewById<Button>(R.id.btnEveryNthTime).setOnClickListener { testEveryNthTime() }
        findViewById<Button>(R.id.btnMaxShows).setOnClickListener { testMaxShows() }
        findViewById<Button>(R.id.btnMinInterval).setOnClickListener { testMinInterval() }
        findViewById<Button>(R.id.btnAutoReloadOff).setOnClickListener { testAutoReloadOff() }
        findViewById<Button>(R.id.btnWaitForLoading).setOnClickListener { testWaitForLoading() }

        // === ADMANAGER DIRECT ===
        findViewById<Button>(R.id.btnForceShowWithDialog).setOnClickListener { testForceShowWithDialog() }
        findViewById<Button>(R.id.btnShowIfReady).setOnClickListener { testShowIfReady() }
        findViewById<Button>(R.id.btnShowByTime).setOnClickListener { testShowByTime() }
        findViewById<Button>(R.id.btnShowByCount).setOnClickListener { testShowByCount() }

        // === EDGE CASES ===
        findViewById<Button>(R.id.btnShowBeforeLoad).setOnClickListener { testShowBeforeLoad() }
        findViewById<Button>(R.id.btnRapidClicks).setOnClickListener { testRapidClicks() }
        findViewById<Button>(R.id.btnBuilderWithoutPreload).setOnClickListener { testBuilderWithoutPreload() }

        // === UTILITIES ===
        findViewById<Button>(R.id.btnClearLog).setOnClickListener { clearLog() }
        findViewById<Button>(R.id.btnResetAdManager).setOnClickListener { resetAdManager() }
    }

    // ==================== LOADING TESTS ====================

    private fun testPreload() {
        log("\n--- Test: Preload ---")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .debug()
            .onAdLoaded { log("✅ Preload: Ad loaded successfully") }
            .onFailed { error -> log("❌ Preload failed: ${error.message}") }
            .preload()
        log("Preload requested...")
    }

    private fun testLoadWithCallback() {
        log("\n--- Test: Load with Callback (AdManager) ---")
        AdManager.getInstance().loadInterstitialAd(
            this,
            TEST_AD_UNIT,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    log("✅ AdManager.loadInterstitialAd: Ad loaded")
                    updateStatus("Ad Loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    log("❌ AdManager.loadInterstitialAd failed: ${error.message}")
                    updateStatus("Load Failed")
                }
            }
        )
        log("Load requested via AdManager...")
    }

    private fun testCheckIsReady() {
        log("\n--- Test: Check isReady ---")
        val isReady = AdManager.getInstance().isReady()
        log("AdManager.isReady() = $isReady")
        updateStatus(if (isReady) "Ad Ready" else "No Ad Ready")
    }

    // ==================== LOADING STRATEGY TESTS ====================

    private fun testStrategyOnDemand() {
        log("\n--- Test: Strategy ON_DEMAND ---")
        log("Always fetches fresh ad with loading dialog")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .loadingStrategy(AdLoadingStrategy.ON_DEMAND)
            .debug()
            .onAdShown { log("✅ ON_DEMAND: Ad shown") }
            .onAdDismissed { log("ON_DEMAND: Ad dismissed") }
            .onFailed { error -> log("❌ ON_DEMAND failed: ${error.message}") }
            .show { log("ON_DEMAND: Next action called") }
    }

    private fun testStrategyOnlyCache() {
        log("\n--- Test: Strategy ONLY_CACHE ---")
        log("Only shows if ad is cached, skips otherwise")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .loadingStrategy(AdLoadingStrategy.ONLY_CACHE)
            .debug()
            .onAdShown { log("✅ ONLY_CACHE: Ad shown") }
            .onAdDismissed { log("ONLY_CACHE: Ad dismissed") }
            .onFailed { error -> log("❌ ONLY_CACHE failed: ${error.message}") }
            .show { log("ONLY_CACHE: Next action called (may skip if not cached)") }
    }

    private fun testStrategyHybrid() {
        log("\n--- Test: Strategy HYBRID ---")
        log("Shows cached ad instantly, or fetches with dialog if needed")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .loadingStrategy(AdLoadingStrategy.HYBRID)
            .debug()
            .onAdShown { log("✅ HYBRID: Ad shown") }
            .onAdDismissed { log("HYBRID: Ad dismissed") }
            .onFailed { error -> log("❌ HYBRID failed: ${error.message}") }
            .show { log("HYBRID: Next action called") }
    }

    private fun testStrategyFreshWithFallback() {
        log("\n--- Test: Strategy FRESH_WITH_CACHE_FALLBACK ---")
        log("Tries fresh first, falls back to cache on failure/timeout")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .loadingStrategy(AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK)
            .debug()
            .onAdShown { log("✅ FRESH_WITH_FALLBACK: Ad shown") }
            .onAdDismissed { log("FRESH_WITH_FALLBACK: Ad dismissed") }
            .onFailed { error -> log("❌ FRESH_WITH_FALLBACK failed: ${error.message}") }
            .show { log("FRESH_WITH_FALLBACK: Next action called") }
    }

    // ==================== BUILDER OPTIONS TESTS ====================

    private fun testBasicShow() {
        log("\n--- Test: Basic Show ---")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .debug()
            .show { log("Basic show: Next action called") }
    }

    private fun testForceShow() {
        log("\n--- Test: Force Show (ignores interval) ---")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .force()
            .debug()
            .show { log("Force show: Next action called") }
    }

    private fun testWithTimeout() {
        log("\n--- Test: With Timeout (3 seconds) ---")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .timeout(3000)
            .loadingStrategy(AdLoadingStrategy.ON_DEMAND)
            .debug()
            .onAdShown { log("✅ Timeout test: Ad shown within timeout") }
            .onFailed { error -> log("❌ Timeout test failed: ${error.message}") }
            .show { log("Timeout test: Next action called") }
    }

    private fun testWithCallbacks() {
        log("\n--- Test: With All Callbacks ---")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .debug()
            .onAdLoaded { log("📦 Callback: onAdLoaded") }
            .onAdShown { log("👁️ Callback: onAdShown") }
            .onAdDismissed { log("👋 Callback: onAdDismissed") }
            .onFailed { error -> log("❌ Callback: onFailed - ${error.message}") }
            .show { log("✅ Callback: onComplete (next action)") }
    }

    private fun testEveryNthTime() {
        val currentCount = AdManager.getInstance().getCallCount(TEST_AD_UNIT)
        log("\n--- Test: Every 3rd Time ---")
        log("Current call count before: $currentCount (will increment to ${currentCount + 1})")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .everyNthTime(3)
            .debug()
            .onAdShown { log("✅ everyNthTime: Ad shown on 3rd call") }
            .show {
                val newCount = AdManager.getInstance().getCallCount(TEST_AD_UNIT)
                log("everyNthTime: Next action (call #$newCount, shows every 3rd)")
            }
    }

    private fun testMaxShows() {
        log("\n--- Test: Max Shows (limit 2) ---")
        log("Current show count: ${AdManager.getInstance().getAdDisplayCount()}")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .maxShows(2)
            .debug()
            .onAdShown { log("✅ maxShows: Ad shown") }
            .show { log("maxShows: Next action (limit: 2 total shows)") }
    }

    private fun testMinInterval() {
        log("\n--- Test: Min Interval (10 seconds) ---")
        log("Last ad time: ${AdManager.getInstance().getLastAdShowTime()}")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .minIntervalSeconds(10)
            .debug()
            .onAdShown { log("✅ minInterval: Ad shown") }
            .show { log("minInterval: Next action (10s minimum between shows)") }
    }

    private fun testAutoReloadOff() {
        log("\n--- Test: Auto Reload OFF ---")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .autoReload(false)
            .debug()
            .onAdShown { log("✅ autoReload(false): Ad shown, will NOT auto-reload") }
            .show { log("autoReload: Next action") }
    }

    private fun testWaitForLoading() {
        log("\n--- Test: Wait For Loading (Splash screen pattern) ---")
        log("If ad is loading, waits for it. If ready, shows. If neither, force loads.")
        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .waitForLoading()
            .timeout(5000)
            .debug()
            .onAdShown { log("✅ waitForLoading: Ad shown") }
            .onFailed { error -> log("❌ waitForLoading failed: ${error.message}") }
            .show { log("waitForLoading: Next action") }
    }

    // ==================== ADMANAGER DIRECT TESTS ====================

    private fun testForceShowWithDialog() {
        log("\n--- Test: AdManager.forceShowInterstitialWithDialog ---")
        AdManager.getInstance().forceShowInterstitialWithDialog(
            this,
            object : AdManagerCallback() {
                override fun onAdLoaded() {
                    log("✅ forceShowWithDialog: Ad loaded/shown")
                }

                override fun onNextAction() {
                    log("forceShowWithDialog: Next action called")
                }

                override fun onFailedToLoad(error: AdError?) {
                    log("❌ forceShowWithDialog failed: ${error?.message}")
                }
            }
        )
    }

    private fun testShowIfReady() {
        log("\n--- Test: AdManager.showInterstitialIfReady ---")
        val wasShown = AdManager.getInstance().showInterstitialIfReady(
            this,
            object : AdManagerCallback() {
                override fun onAdLoaded() {
                    log("✅ showIfReady: Ad shown")
                }

                override fun onNextAction() {
                    log("showIfReady: Next action called")
                }

                override fun onFailedToLoad(error: AdError?) {
                    log("❌ showIfReady failed: ${error?.message}")
                }
            }
        )
        log("showInterstitialIfReady returned: $wasShown")
    }

    private fun testShowByTime() {
        log("\n--- Test: AdManager.showInterstitialAdByTime ---")
        log("Note: Only shows CACHED ads. Preload first!")
        log("isReady: ${AdManager.getInstance().isReady()}")
        log("Last ad time: ${AdManager.getInstance().getLastAdShowTime()}")
        AdManager.getInstance().showInterstitialAdByTime(
            this,
            object : AdManagerCallback() {
                override fun onAdLoaded() {
                    log("✅ showByTime: Ad shown")
                }

                override fun onNextAction() {
                    log("showByTime: Next action (skipped - no cached ad or interval not met)")
                }

                override fun onFailedToLoad(error: AdError?) {
                    log("❌ showByTime failed: ${error?.message}")
                }
            }
        )
    }

    private fun testShowByCount() {
        log("\n--- Test: AdManager.showInterstitialAdByCount ---")
        log("Note: Only shows CACHED ads. Preload first!")
        log("isReady: ${AdManager.getInstance().isReady()}")
        log("Current display count: ${AdManager.getInstance().getAdDisplayCount()}")
        AdManager.getInstance().showInterstitialAdByCount(
            this,
            object : AdManagerCallback() {
                override fun onAdLoaded() {
                    log("✅ showByCount: Ad shown")
                }

                override fun onNextAction() {
                    log("showByCount: Next action (skipped - no cached ad or max count reached)")
                }

                override fun onFailedToLoad(error: AdError?) {
                    log("❌ showByCount failed: ${error?.message}")
                }
            },
            5 // Max 5 displays
        )
    }

    // ==================== EDGE CASE TESTS ====================

    private fun testShowBeforeLoad() {
        log("\n--- Test: Show Before Load (HYBRID should handle) ---")
        // Reset to ensure no cached ad
        resetAdManager()
        log("AdManager reset. isReady: ${AdManager.getInstance().isReady()}")

        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .loadingStrategy(AdLoadingStrategy.HYBRID)
            .debug()
            .onAdShown { log("✅ ShowBeforeLoad: Ad shown (fetched on demand)") }
            .onFailed { error -> log("❌ ShowBeforeLoad failed: ${error.message}") }
            .show { log("ShowBeforeLoad: Next action") }
    }

    private fun testRapidClicks() {
        log("\n--- Test: Rapid Clicks (should prevent duplicates) ---")
        repeat(5) { index ->
            log("Rapid click #${index + 1}")
            InterstitialAdBuilder.with(this)
                .adUnit(TEST_AD_UNIT)
                .loadingStrategy(AdLoadingStrategy.HYBRID)
                .debug()
                .show { log("Rapid click #${index + 1}: Next action") }
        }
    }

    private fun testBuilderWithoutPreload() {
        log("\n--- Test: Builder Without Preload (First Call HYBRID) ---")
        log("This tests the fix for ad unit not being set on first HYBRID call")

        // Reset to simulate first-time use
        resetAdManager()

        InterstitialAdBuilder.with(this)
            .adUnit(TEST_AD_UNIT)
            .loadingStrategy(AdLoadingStrategy.HYBRID)
            .debug()
            .onAdShown { log("✅ BuilderWithoutPreload: Ad shown") }
            .onFailed { error -> log("❌ BuilderWithoutPreload failed: ${error.message}") }
            .show { log("BuilderWithoutPreload: Next action") }
    }

    // ==================== UTILITIES ====================

    private fun clearLog() {
        logTextView.text = ""
        log("=== Log Cleared ===")
    }

    private fun resetAdManager() {
        log("\n--- Resetting AdManager ---")
        AdManager.getInstance().setAdUnitId(null)
        AdManager.getInstance().resetAllCallCounts()
        log("AdManager ad unit and call counters reset")
        log("isReady: ${AdManager.getInstance().isReady()}")
        log("Call count for test unit: ${AdManager.getInstance().getCallCount(TEST_AD_UNIT)}")
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        runOnUiThread {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                .format(java.util.Date())
            logTextView.append("[$timestamp] $message\n")
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            statusTextView.text = status
        }
    }
}
