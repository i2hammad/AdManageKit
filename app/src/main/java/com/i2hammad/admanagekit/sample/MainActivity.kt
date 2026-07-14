package com.i2hammad.admanagekit.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.i2hammad.admanagekit.admob.NativeAdManager
import com.i2hammad.admanagekit.admob.NativeBannerMedium
import com.i2hammad.admanagekit.admob.NativeBannerSmall
import com.i2hammad.admanagekit.admob.NativeLarge
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.config.AdLoadingStrategy
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import android.util.Log

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

        // Enable debug overlay to see real-time ad statistics
        if (AdManageKitConfig.debugMode) {
            AdDebugUtils.enableDebugOverlay(this, true)
            Log.d("AdManageKit", "Debug overlay enabled")
        }
        
        // Log current configuration for testing
        Log.d("AdManageKit", "AdManageKit Configuration Summary:")
        Log.d("AdManageKit", AdManageKitConfig.getConfigSummary())

        // Setup button to open Loading Strategy Tester
        findViewById<Button>(R.id.btnOpenStrategyTester).setOnClickListener {
            val intent = Intent(this, LoadingStrategyTestActivity::class.java)
            startActivity(intent)
        }

        // Setup button to open Native Template Tester
        findViewById<Button>(R.id.btnOpenTemplateTester).setOnClickListener {
            val intent = Intent(this, NativeTemplateTestActivity::class.java)
            startActivity(intent)
        }

        // Setup button to open Non-Material Theme Test (for app open ad testing)
        findViewById<Button>(R.id.btnOpenNonMaterialTest).setOnClickListener {
            val intent = Intent(this, NonMaterialTestActivity::class.java)
            startActivity(intent)
        }

        // Setup button to open Fragment Transition Ad Test
        findViewById<Button>(R.id.btnOpenFragmentTransitionTest).setOnClickListener {
            val intent = Intent(this, FragmentTransitionTestActivity::class.java)
            startActivity(intent)
        }

        // Setup button to open Waterfall Ad Test
        findViewById<Button>(R.id.btnOpenWaterfallTest).setOnClickListener {
            val intent = Intent(this, WaterfallTestActivity::class.java)
            startActivity(intent)
        }

        // Setup button to open the Jetpack Compose ads sample
        findViewById<Button>(R.id.btnOpenComposeAdsTest).setOnClickListener {
            val intent = Intent(this, ComposeAdsTestActivity::class.java)
            startActivity(intent)
        }

        loadAds()


    }


    private fun loadAds() {
        // Use logical name for waterfall resolution (maps to provider-specific IDs via AdUnitMapping)
        val adUnitId = "ca-app-pub-3940256099942544/2247696110"

        // =================== EXAMPLE 1: Using Global Strategy (from AdManageKitConfig) ===================
        Log.d("AdManageKit", "📱 Loading NativeBannerSmall with GLOBAL strategy: ${AdManageKitConfig.nativeLoadingStrategy}")
        val nativeBannerSmall: NativeBannerSmall = findViewById(R.id.nativeBannerSmall)

        // Uses global nativeLoadingStrategy from AdManageKitConfig (currently HYBRID)
        nativeBannerSmall.loadNativeBannerAd(this, adUnitId, object : AdLoadCallback() {
            override fun onAdLoaded() {
                Log.d("AdManageKit", "✅ NativeBannerSmall loaded successfully (Global Strategy)")
                Toast.makeText(this@MainActivity, "Small Native Ad Loaded (Global)", Toast.LENGTH_SHORT).show()
            }

            override fun onFailedToLoad(error: LoadAdError?) {
                Log.e("AdManageKit", "❌ NativeBannerSmall failed: ${error?.message} (Global Strategy)")
                Toast.makeText(this@MainActivity, "Small Native Ad Failed: ${error?.code}", Toast.LENGTH_SHORT).show()
            }

            override fun onAdImpression() {
                Log.d("AdManageKit", "👁️ NativeBannerSmall impression recorded")
            }

            override fun onAdClicked() {
                Log.d("AdManageKit", "👆 NativeBannerSmall clicked")
            }
        })

        // =================== EXAMPLE 2: Override with ONLY_CACHE Strategy ===================
        // Perfect for instant display - only shows if ad is already cached
        Log.d("AdManageKit", "📱 Loading NativeBannerMedium with ONLY_CACHE strategy override")
        val nativeBannerMedium: NativeBannerMedium = findViewById(R.id.nativeBannerMedium)

        nativeBannerMedium.loadNativeBannerAd(
            this,
            adUnitId,
            object : AdLoadCallback() {
                override fun onAdLoaded() {
                    Log.d("AdManageKit", "✅ NativeBannerMedium loaded instantly from cache (ONLY_CACHE)")
                    Toast.makeText(this@MainActivity, "Medium Native Ad Loaded (Cached)", Toast.LENGTH_SHORT).show()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
                    Log.e("AdManageKit", "❌ NativeBannerMedium skipped - no cache available (ONLY_CACHE)")
                    Toast.makeText(this@MainActivity, "Medium Native Ad Skipped (No Cache)", Toast.LENGTH_SHORT).show()
                }
            },
            loadingStrategy = AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK  // Override: Only show cached ads
        )

        // =================== EXAMPLE 3: Override with ON_DEMAND Strategy ===================
        // Always fetches fresh ad - shows shimmer while loading
        Log.d("AdManageKit", "📱 Loading NativeLarge with ON_DEMAND strategy override")
        val nativeLarge: NativeLarge = findViewById(R.id.nativeLarge)

        nativeLarge.loadNativeAds(
            this,
            adUnitId,
            object : AdLoadCallback() {
                override fun onAdLoaded() {
                    Log.d("AdManageKit", "✅ NativeLarge loaded fresh (ON_DEMAND)")
                    Toast.makeText(this@MainActivity, "Large Native Ad Loaded (Fresh)", Toast.LENGTH_SHORT).show()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
                    Log.e("AdManageKit", "❌ NativeLarge failed to load: ${error?.message} (ON_DEMAND)")
                    Toast.makeText(this@MainActivity, "Large Native Ad Failed", Toast.LENGTH_SHORT).show()
                }

                override fun onAdImpression() {
                    Log.d("AdManageKit", "👁️ NativeLarge impression recorded")
                }
            },
            loadingStrategy = AdLoadingStrategy.ON_DEMAND  // Override: Always fetch fresh
        )

        // Log strategy information for debugging
        Log.d("AdManageKit", """
            ═══════════════════════════════════════════════════
            📊 AD LOADING STRATEGY EXAMPLES:
            ═══════════════════════════════════════════════════

            1️⃣ NativeBannerSmall: Uses GLOBAL strategy
               → Strategy: ${AdManageKitConfig.nativeLoadingStrategy}
               → Behavior: ${getStrategyDescription(AdManageKitConfig.nativeLoadingStrategy)}

            2️⃣ NativeBannerMedium: ONLY_CACHE override
               → Strategy: ONLY_CACHE (overriding global)
               → Behavior: Instant display if cached, skip if not

            3️⃣ NativeLarge: ON_DEMAND override
               → Strategy: ON_DEMAND (overriding global)
               → Behavior: Always fetches fresh with shimmer

            ═══════════════════════════════════════════════════
        """.trimIndent())
    }

    /**
     * Helper function to describe strategy behavior
     */
    private fun getStrategyDescription(strategy: AdLoadingStrategy): String {
        return when (strategy) {
            AdLoadingStrategy.ON_DEMAND -> "Always fetches fresh ad with shimmer"
            AdLoadingStrategy.ONLY_CACHE -> "Only shows cached ads, skips if not available"
            AdLoadingStrategy.HYBRID -> "Shows cached instantly, or fetches if needed"
            AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK -> "Fresh with Cache Fallback"
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Log cache statistics for testing
        if (AdManageKitConfig.debugMode) {
            logCacheStatistics()
        }
    }
    
    /**
     * Log cache statistics and debug information for testing
     */
    private fun logCacheStatistics() {
        try {
            val cacheStats = NativeAdManager.getCacheStatistics()
            Log.d("AdManageKit", "📊 Cache Statistics:")
            cacheStats.forEach { (adUnit, stats) ->
                Log.d("AdManageKit", "  $adUnit: $stats")
            }
            
            val totalCacheSize = NativeAdManager.getTotalCacheSize()
            Log.d("AdManageKit", "📦 Total cache size: $totalCacheSize ads")
            
            // Log configuration validation
            val isValid = AdManageKitConfig.validate()
            Log.d("AdManageKit", "✓ Configuration valid: $isValid")
            
            val isProductionReady = AdManageKitConfig.isProductionReady()
            Log.d("AdManageKit", "🚀 Production ready: $isProductionReady")
            
        } catch (e: Exception) {
            Log.w("AdManageKit", "Could not retrieve cache statistics: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Log final statistics before clearing
        if (AdManageKitConfig.debugMode) {
//            Log.d("AdManageKit", "🧹 Clearing all cached ads on destroy")
            logCacheStatistics()
        }
        
//        NativeAdManager.clearAllCachedAds()
    }
}