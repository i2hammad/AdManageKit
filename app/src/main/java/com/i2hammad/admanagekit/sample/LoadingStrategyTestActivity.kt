package com.i2hammad.admanagekit.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.material.switchmaterial.SwitchMaterial
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.InterstitialAdBuilder
import com.i2hammad.admanagekit.admob.NativeAdManager
import com.i2hammad.admanagekit.admob.NativeLarge
import com.i2hammad.admanagekit.admob.NativeBannerMedium
import com.i2hammad.admanagekit.admob.NativeBannerSmall
import com.i2hammad.admanagekit.admob.BannerAdView
import com.i2hammad.admanagekit.config.AdLoadingStrategy
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.config.CollapsibleBannerPlacement

/**
 * Comprehensive test activity for all loading strategies.
 *
 * This activity allows testing:
 * - Different loading strategies (ON_DEMAND, ONLY_CACHE, HYBRID)
 * - Interstitial ads with strategies
 * - Native ads with strategies
 * - Collapsible banners with different placements
 * - Real-time strategy switching
 */
class LoadingStrategyTestActivity : AppCompatActivity() {

    // Test ad unit IDs (Google's test IDs)
    private val interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712"
    private val nativeAdUnitId = "ca-app-pub-3940256099942544/2247696110"
    private val bannerAdUnitId = "ca-app-pub-3940256099942544/9214589741"

    // Views
    private lateinit var strategyRadioGroup: RadioGroup
    private lateinit var statusTextView: TextView
    private lateinit var cacheStatusTextView: TextView
    private lateinit var smartPreloadSwitch: SwitchMaterial
    private lateinit var crossAdUnitFallbackSwitch: SwitchMaterial
    private lateinit var adPoolStatusTextView: TextView
    private lateinit var adStatsTextView: TextView
    private lateinit var nativeLargeView: NativeLarge
    private lateinit var nativeMediumView: NativeBannerMedium
    private lateinit var nativeSmallView: NativeBannerSmall
    private lateinit var bannerView: BannerAdView

    // Multiple ad unit IDs for ad pool testing
    private val interstitialAdUnitIds = listOf(
        "ca-app-pub-3940256099942544/1033173712",  // Test interstitial 1
        "ca-app-pub-3940256099942544/1033173712",  // Test interstitial 2 (same ID for testing)
        "ca-app-pub-3940256099942544/1033173712"   // Test interstitial 3
    )

    // Current strategy
    private var currentStrategy: AdLoadingStrategy = AdLoadingStrategy.HYBRID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading_strategy_test)

        initializeViews()
        setupListeners()
        updateStatusText()
        updateCacheStatus()
        updateAdPoolStatus()
        updateAdStats()

        logTestInfo()
    }

    private fun initializeViews() {
        strategyRadioGroup = findViewById(R.id.strategyRadioGroup)
        statusTextView = findViewById(R.id.statusTextView)
        cacheStatusTextView = findViewById(R.id.cacheStatusTextView)
        smartPreloadSwitch = findViewById(R.id.switchSmartPreload)
        crossAdUnitFallbackSwitch = findViewById(R.id.switchCrossAdUnitFallback)
        adPoolStatusTextView = findViewById(R.id.adPoolStatusTextView)
        adStatsTextView = findViewById(R.id.adStatsTextView)
        nativeLargeView = findViewById(R.id.nativeLargeTest)
        nativeMediumView = findViewById(R.id.nativeMediumTest)
        nativeSmallView = findViewById(R.id.nativeSmallTest)
        bannerView = findViewById(R.id.bannerViewTest)

        // Set initial states from config
        smartPreloadSwitch.isChecked = AdManageKitConfig.enableSmartPreloading
        crossAdUnitFallbackSwitch.isChecked = AdManageKitConfig.enableCrossAdUnitFallback

        findViewById<Button>(R.id.btnTestInterstitial).setOnClickListener {
            testInterstitial()
        }

        findViewById<Button>(R.id.btnTestNativeLarge).setOnClickListener {
            testNativeLarge()
        }

        findViewById<Button>(R.id.btnTestNativeMedium).setOnClickListener {
            testNativeMedium()
        }

        findViewById<Button>(R.id.btnTestNativeSmall).setOnClickListener {
            testNativeSmall()
        }

        findViewById<Button>(R.id.btnTestBannerTop).setOnClickListener {
            testCollapsibleBanner(CollapsibleBannerPlacement.TOP)
        }

        findViewById<Button>(R.id.btnTestBannerBottom).setOnClickListener {
            testCollapsibleBanner(CollapsibleBannerPlacement.BOTTOM)
        }

        findViewById<Button>(R.id.btnPreloadCache).setOnClickListener {
            preloadAdsToCache()
        }

        findViewById<Button>(R.id.btnClearCache).setOnClickListener {
            clearCache()
        }

        // Ad Pool buttons
        findViewById<Button>(R.id.btnLoadAdPool).setOnClickListener {
            loadAdPool()
        }

        findViewById<Button>(R.id.btnShowFromPool).setOnClickListener {
            showAdFromPool()
        }
    }

    private fun setupListeners() {
        strategyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentStrategy = when (checkedId) {
                R.id.radioOnDemand -> AdLoadingStrategy.ON_DEMAND
                R.id.radioOnlyCache -> AdLoadingStrategy.ONLY_CACHE
                R.id.radioHybrid -> AdLoadingStrategy.HYBRID
                R.id.radioFreshWithFallback -> AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK
                else -> AdLoadingStrategy.HYBRID
            }

            updateStatusText()
            updateGlobalStrategy()
            updateCacheStatus()

            Toast.makeText(this, "Strategy changed to: $currentStrategy", Toast.LENGTH_SHORT).show()
        }

        smartPreloadSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Override smart preload setting manually
            AdManageKitConfig.enableSmartPreloading = isChecked
            updateCacheStatus()

            val status = if (isChecked) "ENABLED" else "DISABLED"
            Toast.makeText(this, "Smart Preload manually $status", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Smart Preload manually changed to: $isChecked")
        }

        crossAdUnitFallbackSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Toggle cross ad unit fallback for native ads
            AdManageKitConfig.enableCrossAdUnitFallback = isChecked
            updateCacheStatus()

            val status = if (isChecked) "ENABLED" else "DISABLED"
            Toast.makeText(this, "Cross Ad Unit Fallback $status", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Cross Ad Unit Fallback changed to: $isChecked")
        }
    }

    private fun updateGlobalStrategy() {
        AdManageKitConfig.apply {
            interstitialLoadingStrategy = currentStrategy
            appOpenLoadingStrategy = currentStrategy

            // IMPORTANT: ONLY_CACHE not recommended for native ads
            // Native ads use HYBRID or ON_DEMAND only (converted automatically in NativeAdIntegrationManager)
            nativeLoadingStrategy = currentStrategy

            // Smart preload behavior based on strategy (if not manually overridden)
            if (!smartPreloadSwitch.isChecked || currentStrategy == AdLoadingStrategy.ON_DEMAND) {
                when (currentStrategy) {
                    AdLoadingStrategy.ON_DEMAND -> {
                        // ON_DEMAND: Disable smart preload (always fetch fresh)
                        enableSmartPreloading = false
                        smartPreloadSwitch.isChecked = false
                    }
                    AdLoadingStrategy.ONLY_CACHE -> {
                        // ONLY_CACHE: Enable smart preload (need cache to work)
                        enableSmartPreloading = true
                        smartPreloadSwitch.isChecked = true
                    }
                    AdLoadingStrategy.HYBRID -> {
                        // HYBRID: Enable smart preload (use cache when available)
                        enableSmartPreloading = true
                        smartPreloadSwitch.isChecked = true
                    }
                    AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK -> {
                        // HYBRID: Enable smart preload (use cache when available)
                        enableSmartPreloading = true
                        smartPreloadSwitch.isChecked = true
                    }
                }
            }
        }

        Log.d(TAG, """
            ═══════════════════════════════════════════════════
            📊 GLOBAL STRATEGY UPDATED
            ═══════════════════════════════════════════════════
            Strategy: ${AdManageKitConfig.interstitialLoadingStrategy}
            Smart Preload: ${AdManageKitConfig.enableSmartPreloading}

            Interstitial: ${AdManageKitConfig.interstitialLoadingStrategy}
            App Open: ${AdManageKitConfig.appOpenLoadingStrategy}
            Native: ${AdManageKitConfig.nativeLoadingStrategy} (ONLY_CACHE → HYBRID auto)
            ═══════════════════════════════════════════════════
        """.trimIndent())
    }

    private fun updateStatusText() {
        val strategyDescription = when (currentStrategy) {
            AdLoadingStrategy.ON_DEMAND -> """
                ON_DEMAND (Maximum Coverage)
                ✓ Always fetches fresh ads
                ✓ Shows loading dialog
                ✓ Best ad fill rate
                ✓ Smart preload: DISABLED
                ✗ May interrupt user flow
            """.trimIndent()

            AdLoadingStrategy.ONLY_CACHE -> """
                ONLY_CACHE (Best UX)
                ✓ Instant display
                ✓ No loading dialogs
                ✓ Smooth user experience
                ✓ Smart preload: ENABLED
                ✗ Lower fill rate (requires preloading)
            """.trimIndent()

            AdLoadingStrategy.HYBRID -> """
                HYBRID (Balanced) ⭐
                ✓ Instant when cached
                ✓ Fetches if needed
                ✓ Good balance of UX & coverage
                ✓ Smart preload: ENABLED
                ⚠ May show dialog if cache miss
            """.trimIndent()

            AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK -> """
                FRESH_WITH_CACHE_FALLBACK (Best for RecyclerView)
                ✓ Always tries fresh ad first
                ✓ Falls back to cache if fresh fails
                ✓ Shows shimmer while loading (native)
                ✓ Best for lists with multiple ad slots
                ✓ Smart preload: ENABLED
                ⚠ May delay display while fetching fresh
            """.trimIndent()
        }

        statusTextView.text = """
            Current Strategy: $currentStrategy

            $strategyDescription

            Cache Status:
            • Smart Preloading: ${AdManageKitConfig.enableSmartPreloading}
            • Max Cache/Unit: ${AdManageKitConfig.maxCachedAdsPerUnit}
        """.trimIndent()
    }

    /**
     * Update cache status display
     */
    private fun updateCacheStatus() {
        try {
            val adManager = AdManager.getInstance()
            val interstitialReady = adManager.isReady()

            // Get native ad cache statistics
            val cacheStats = NativeAdManager.getCacheStatistics()
            val totalNativeCached = NativeAdManager.getTotalCacheSize()

            // Build cache status text
            val statusBuilder = StringBuilder()

            // Interstitial Status
            statusBuilder.append("Interstitial Ad: ")
            statusBuilder.append(if (interstitialReady) "✅ READY" else "❌ NOT READY")
            statusBuilder.append("\n\n")

            // Native Ads Status
            statusBuilder.append("Native Ads Cached: $totalNativeCached\n")
            if (cacheStats.isNotEmpty()) {
                statusBuilder.append("━━━━━━━━━━━━━━━━━━━━━\n")
                cacheStats.forEach { (adUnit, stats) ->
                    val shortAdUnit = adUnit.takeLast(10)
                    statusBuilder.append("...$shortAdUnit\n")
                    statusBuilder.append("  $stats\n")
                }
            } else {
                statusBuilder.append("  No native ads cached\n")
            }

            statusBuilder.append("\n")
            statusBuilder.append("Max Cache/Unit: ${AdManageKitConfig.maxCachedAdsPerUnit}\n")
            statusBuilder.append("Smart Preload: ${if (AdManageKitConfig.enableSmartPreloading) "ON" else "OFF"}")

            cacheStatusTextView.text = statusBuilder.toString()

            Log.d(TAG, """
                ═══════════════════════════════════════════════════
                📦 CACHE STATUS UPDATE
                ═══════════════════════════════════════════════════
                $statusBuilder
                ═══════════════════════════════════════════════════
            """.trimIndent())

        } catch (e: Exception) {
            cacheStatusTextView.text = "Error loading cache status:\n${e.message}"
            Log.e(TAG, "Failed to update cache status", e)
        }
    }

    // =================== TEST METHODS ===================

    /**
     * Test interstitial ad with current strategy
     */
    private fun testInterstitial() {
        Log.d(TAG, "🎯 Testing Interstitial with strategy: $currentStrategy")

        showToast("Loading Interstitial ($currentStrategy)...")

        InterstitialAdBuilder.with(this)
            .adUnit(interstitialAdUnitId)
            .loadingStrategy(currentStrategy)  // Apply current strategy
            .debug()  // Enable debug logging
            .onAdShown {
                Log.d(TAG, "📺 Interstitial ad shown")
                showToast("Interstitial shown!")
                updateCacheStatus()
            }
            .onAdDismissed {
                showToast("Interstitial dismissed")
                Log.d(TAG, "✅ Interstitial test completed")
                updateCacheStatus()
            }
            .onFailed { error ->
                Log.e(TAG, "❌ Interstitial failed: ${error.message}")
                showToast("Interstitial failed: ${error.code}")
                updateCacheStatus()
            }
            .show {
                // Next action after ad
                Log.d(TAG, "➡️ Proceeding to next action after interstitial")
            }
    }

    /**
     * Test native large ad with current strategy
     */
    private fun testNativeLarge() {
        Log.d(TAG, "🎯 Testing Native Large with strategy: $currentStrategy")

        showToast("Loading Native Large ($currentStrategy)...")

        nativeLargeView.loadNativeAds(
            this,
            nativeAdUnitId,
            object : AdLoadCallback() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Native Large loaded ($currentStrategy)")
                    showToast("Native Large loaded successfully!")
                    updateCacheStatus()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
                    Log.e(TAG, "❌ Native Large failed: ${error?.message} ($currentStrategy)")
                    showToast("Native Large failed: ${error?.message}")
                    updateCacheStatus()
                }

                override fun onAdImpression() {
                    Log.d(TAG, "👁️ Native Large impression")
                }
            },
            loadingStrategy = currentStrategy
        )
    }

    /**
     * Test native medium ad with current strategy
     */
    private fun testNativeMedium() {
        Log.d(TAG, "🎯 Testing Native Medium with strategy: $currentStrategy")

        showToast("Loading Native Medium ($currentStrategy)...")

        nativeMediumView.loadNativeBannerAd(
            this,
            nativeAdUnitId,
            object : AdLoadCallback() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Native Medium loaded ($currentStrategy)")
                    showToast("Native Medium loaded successfully!")
                    updateCacheStatus()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
                    Log.e(TAG, "❌ Native Medium failed: ${error?.message} ($currentStrategy)")
                    showToast("Native Medium failed: ${error?.message}")
                    updateCacheStatus()
                }

                override fun onAdImpression() {
                    Log.d(TAG, "👁️ Native Medium impression")
                }
            },
            loadingStrategy = currentStrategy
        )
    }

    /**
     * Test native small ad with current strategy
     */
    private fun testNativeSmall() {
        Log.d(TAG, "🎯 Testing Native Small with strategy: $currentStrategy")

        showToast("Loading Native Small ($currentStrategy)...")

        nativeSmallView.loadNativeBannerAd(
            this,
            nativeAdUnitId,
            object : AdLoadCallback() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Native Small loaded ($currentStrategy)")
                    showToast("Native Small loaded successfully!")
                    updateCacheStatus()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
                    Log.e(TAG, "❌ Native Small failed: ${error?.message} ($currentStrategy)")
                    showToast("Native Small failed: ${error?.message}")
                    updateCacheStatus()
                }

                override fun onAdImpression() {
                    Log.d(TAG, "👁️ Native Small impression")
                }
            },
            loadingStrategy = currentStrategy
        )
    }

    /**
     * Test collapsible banner with specified placement
     */
    private fun testCollapsibleBanner(placement: CollapsibleBannerPlacement) {
        Log.d(TAG, "🎯 Testing Collapsible Banner (${placement.name})")

        showToast("Loading Collapsible Banner (${placement.name})...")

        bannerView.loadCollapsibleBanner(
            this,
            bannerAdUnitId,
            collapsible = true,
            placement = placement,
            callback = object : AdLoadCallback() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner loaded (${placement.name})")
                    showToast("Banner loaded (${placement.name})")
                    updateCacheStatus()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
                    Log.e(TAG, "❌ Banner failed: ${error?.message}")
                    showToast("Banner failed: ${error?.message}")
                    updateCacheStatus()
                }

                override fun onAdImpression() {
                    Log.d(TAG, "👁️ Banner impression (${placement.name})")
                }
            }
        )
    }

    /**
     * Preload ads to cache for ONLY_CACHE testing
     */
    private fun preloadAdsToCache() {
        Log.d(TAG, "📦 Preloading ads to cache...")
        showToast("Preloading ads to cache...")

        var completedTasks = 0
        val totalTasks = 4 // 1 interstitial + 3 native sizes

        val onTaskComplete = {
            completedTasks++
            if (completedTasks == totalTasks) {
                showToast("✅ All ads preloaded! ($completedTasks/$totalTasks)")
                updateCacheStatus()
                Log.d(TAG, "✅ All ads successfully preloaded to cache")
            }
        }

        // Preload interstitial
        InterstitialAdBuilder.with(this)
            .adUnit(interstitialAdUnitId)
            .onAdLoaded {
                Log.d(TAG, "✅ Interstitial preloaded to cache")
                onTaskComplete()
            }
            .onFailed { error ->
                Log.e(TAG, "❌ Interstitial preload failed: ${error.message}")
                onTaskComplete()
            }
            .preload()

        // Force preload native ads using new NativeAdManager.preloadNativeAd()
        NativeAdManager.preloadNativeAd(
            activity = this,
            adUnitId = nativeAdUnitId,
            size = com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize.LARGE,
            onSuccess = {
                Log.d(TAG, "✅ Native Large preloaded to cache")
                showToast("Native Large preloaded")
                onTaskComplete()
            },
            onFailure = { error ->
                Log.e(TAG, "❌ Native Large preload failed: $error")
                onTaskComplete()
            }
        )

        NativeAdManager.preloadNativeAd(
            activity = this,
            adUnitId = nativeAdUnitId,
            size = com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize.MEDIUM,
            onSuccess = {
                Log.d(TAG, "✅ Native Medium preloaded to cache")
                showToast("Native Medium preloaded")
                onTaskComplete()
            },
            onFailure = { error ->
                Log.e(TAG, "❌ Native Medium preload failed: $error")
                onTaskComplete()
            }
        )

        NativeAdManager.preloadNativeAd(
            activity = this,
            adUnitId = nativeAdUnitId,
            size = com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader.NativeAdSize.SMALL,
            onSuccess = {
                Log.d(TAG, "✅ Native Small preloaded to cache")
                showToast("Native Small preloaded")
                onTaskComplete()
            },
            onFailure = { error ->
                Log.e(TAG, "❌ Native Small preload failed: $error")
                onTaskComplete()
            }
        )

        showToast("Preload started. Wait a few seconds...")

        // Update cache status after a short delay to show progress
        cacheStatusTextView.postDelayed({ updateCacheStatus() }, 3000)

        Log.d(TAG, """
            ═══════════════════════════════════════════════════
            📦 CACHE PRELOAD INITIATED
            ═══════════════════════════════════════════════════
            Interstitial: Preload triggered
            Native Large: Force preload triggered
            Native Medium: Force preload triggered
            Native Small: Force preload triggered

            Wait 3-5 seconds, then test with ONLY_CACHE strategy
            ═══════════════════════════════════════════════════
        """.trimIndent())
    }

    /**
     * Clear all cached ads
     */
    private fun clearCache() {
        Log.d(TAG, "🧹 Clearing cache...")
        showToast("Cache cleared")

        // Clear native ad cache
        NativeAdManager.clearAllCachedAds()

        // Update cache status display
        updateCacheStatus()

        Log.d(TAG, """
            ═══════════════════════════════════════════════════
            🧹 CACHE CLEARED
            ═══════════════════════════════════════════════════
            All cached ads have been cleared.
            ONLY_CACHE strategy will now skip ads until preload.
            ═══════════════════════════════════════════════════
        """.trimIndent())
    }

    // =================== AD POOL METHODS ===================

    /**
     * Load multiple ad units into the ad pool
     */
    private fun loadAdPool() {
        Log.d(TAG, "Loading ${interstitialAdUnitIds.size} ad units into pool...")
        showToast("Loading ${interstitialAdUnitIds.size} ad units into pool...")

        val adManager = AdManager.getInstance()
        adManager.loadMultipleAdUnits(this, interstitialAdUnitIds)

        // Update pool status after a delay
        adPoolStatusTextView.postDelayed({
            updateAdPoolStatus()
            updateAdStats()
        }, 3000)
    }

    /**
     * Show any available ad from the pool
     */
    private fun showAdFromPool() {
        val adManager = AdManager.getInstance()

        if (!adManager.isReady()) {
            showToast("No ads in pool. Load first!")
            Log.d(TAG, "No ads available in pool")
            return
        }

        Log.d(TAG, "Showing ad from pool (size: ${adManager.getPoolSize()})...")
        showToast("Showing ad from pool...")

        adManager.showInterstitialIfReady(this, object : com.i2hammad.admanagekit.admob.AdManagerCallback() {
            override fun onNextAction() {
                Log.d(TAG, "Ad from pool completed")
                updateAdPoolStatus()
                updateAdStats()
            }
        })
    }

    /**
     * Update ad pool status display
     */
    private fun updateAdPoolStatus() {
        val adManager = AdManager.getInstance()
        val poolSize = adManager.getPoolSize()
        val readyUnits = adManager.getReadyAdUnits()

        val statusBuilder = StringBuilder()
        statusBuilder.append("Pool Size: $poolSize\n")

        if (readyUnits.isNotEmpty()) {
            statusBuilder.append("Ready Units:\n")
            readyUnits.forEachIndexed { index, unit ->
                val shortUnit = unit.takeLast(10)
                statusBuilder.append("  ${index + 1}. ...$shortUnit\n")
            }
        } else {
            statusBuilder.append("No ads ready in pool")
        }

        adPoolStatusTextView.text = statusBuilder.toString()
    }

    /**
     * Update ad statistics display
     */
    private fun updateAdStats() {
        val adManager = AdManager.getInstance()
        val stats = adManager.getAdStats()

        val requests = stats["session_requests"] as? Int ?: 0
        val fills = stats["session_fills"] as? Int ?: 0
        val impressions = stats["session_impressions"] as? Int ?: 0
        val fillRate = stats["fill_rate_percent"] as? Float ?: 0f
        val showRate = stats["show_rate_percent"] as? Float ?: 0f
        val totalShown = stats["total_ads_shown"] as? Int ?: 0

        adStatsTextView.text = """
            Requests: $requests | Fills: $fills | Shown: $impressions
            Fill Rate: ${fillRate.toInt()}% | Show Rate: ${showRate.toInt()}%
            Total Ads Shown (lifetime): $totalShown
        """.trimIndent()

        Log.d(TAG, "Ad Stats: $stats")
    }

    // =================== HELPER METHODS ===================

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun logTestInfo() {
        Log.d(TAG, """
            ═══════════════════════════════════════════════════
            🧪 LOADING STRATEGY TEST ACTIVITY
            ═══════════════════════════════════════════════════

            Test Configuration:
            • Interstitial ID: $interstitialAdUnitId
            • Native ID: $nativeAdUnitId
            • Banner ID: $bannerAdUnitId

            Current Global Strategy:
            • Interstitial: ${AdManageKitConfig.interstitialLoadingStrategy}
            • App Open: ${AdManageKitConfig.appOpenLoadingStrategy}
            • Native: ${AdManageKitConfig.nativeLoadingStrategy}

            Test Features:
            1. Strategy Selection (Radio Buttons)
               - ON_DEMAND: Maximum ad coverage (Smart Preload OFF)
               - ONLY_CACHE: Best UX, instant display (Smart Preload ON)
               - HYBRID: Balanced approach ⭐ (Smart Preload ON)

            2. Ad Type Tests
               - Interstitial with strategy override
               - Native with strategy override
               - Collapsible Banner (TOP/BOTTOM)

            3. Cache Management
               - Preload ads to cache
               - Clear cache

            ═══════════════════════════════════════════════════

            📝 TEST SCENARIOS:

            Scenario 1: Test ON_DEMAND
            1. Select ON_DEMAND strategy
            2. Smart Preload: Automatically DISABLED
            3. Click "Test Interstitial"
            4. Observe: Loading dialog appears
            5. Result: Fresh ad always fetched (ignores cache)

            Scenario 2: Test ONLY_CACHE
            1. Select ONLY_CACHE strategy
            2. Smart Preload: Automatically ENABLED
            3. Click "Preload Cache"
            4. Wait 5 seconds
            5. Click "Test Interstitial"
            6. Result: Instant display (no dialog)

            Scenario 3: Test HYBRID
            1. Select HYBRID strategy
            2. Smart Preload: Automatically ENABLED
            2. Click "Test Native"
            3. First load: Shows shimmer, fetches ad
            4. Second load: Instant from cache

            Scenario 4: Test Collapsible Banners
            1. Click "Test Banner Top"
            2. Observe: Banner collapses from top
            3. Click "Test Banner Bottom"
            4. Observe: Banner collapses from bottom

            ═══════════════════════════════════════════════════
        """.trimIndent())
    }

    companion object {
        private const val TAG = "LoadingStrategyTest"
    }
}
