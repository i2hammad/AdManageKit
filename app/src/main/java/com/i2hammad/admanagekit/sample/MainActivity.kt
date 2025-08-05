package com.i2hammad.admanagekit.sample

import android.os.Bundle
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
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.google.android.gms.ads.AdError
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
            Log.d("MainActivity", "Debug overlay enabled")
        }
        
        // Log current configuration for testing
        Log.d("MainActivity", "AdManageKit Configuration Summary:")
        Log.d("MainActivity", AdManageKitConfig.getConfigSummary())
        
        loadAds()


    }


    private fun loadAds() {


        val adUnitId = "ca-app-pub-3940256099942544/2247696110"

//        NativeAdManager.preloadAd(this, adUnitId)

        var nativeBannerSmall: NativeBannerSmall = findViewById(R.id.nativeBannerSmall)
        // Test native banner small with enhanced callback and smart preloading
        nativeBannerSmall.loadNativeBannerAd(this, adUnitId, useCachedAd = AdManageKitConfig.enableSmartPreloading, object : AdLoadCallback() {
            override fun onAdLoaded() {
                Log.d("MainActivity", "✅ NativeBannerSmall loaded successfully")
                Toast.makeText(this@MainActivity, "Small Native Ad Loaded", Toast.LENGTH_SHORT).show()
            }
            
            override fun onFailedToLoad(error: AdError?) {
                Log.e("MainActivity", "❌ NativeBannerSmall failed to load: ${error?.message}")
                Toast.makeText(this@MainActivity, "Small Native Ad Failed: ${error?.code}", Toast.LENGTH_SHORT).show()
            }
            
            override fun onAdImpression() {
                Log.d("MainActivity", "👁️ NativeBannerSmall impression recorded")
            }
            
            override fun onAdClicked() {
                Log.d("MainActivity", "👆 NativeBannerSmall clicked")
            }
        })





        var nativeBannerMedium: NativeBannerMedium = findViewById(R.id.nativeBannerMedium)
        // Test native banner medium with configuration-based caching
        nativeBannerMedium.loadNativeBannerAd(this, adUnitId, useCachedAd = AdManageKitConfig.enableSmartPreloading, object : AdLoadCallback() {
            override fun onAdLoaded() {
                Log.d("MainActivity", "✅ NativeBannerMedium loaded successfully")
                Toast.makeText(this@MainActivity, "Medium Native Ad Loaded", Toast.LENGTH_SHORT).show()
            }
            
            override fun onFailedToLoad(error: AdError?) {
                Log.e("MainActivity", "❌ NativeBannerMedium failed to load: ${error?.message}")
            }
        })

        var nativeLarge: NativeLarge = findViewById(R.id.nativeLarge)
        // Test native large with configuration-based caching
        nativeLarge.loadNativeAds(this, adUnitId, useCachedAd = AdManageKitConfig.enableSmartPreloading, object : AdLoadCallback() {
            override fun onAdLoaded() {
                Log.d("MainActivity", "✅ NativeLarge loaded successfully")
                Toast.makeText(this@MainActivity, "Large Native Ad Loaded", Toast.LENGTH_SHORT).show()
            }
            
            override fun onFailedToLoad(error: AdError?) {
                Log.e("MainActivity", "❌ NativeLarge failed to load: ${error?.message}")
            }
        })


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
            Log.d("MainActivity", "📊 Cache Statistics:")
            cacheStats.forEach { (adUnit, stats) ->
                Log.d("MainActivity", "  $adUnit: $stats")
            }
            
            val totalCacheSize = NativeAdManager.getTotalCacheSize()
            Log.d("MainActivity", "📦 Total cache size: $totalCacheSize ads")
            
            // Log configuration validation
            val isValid = AdManageKitConfig.validate()
            Log.d("MainActivity", "✓ Configuration valid: $isValid")
            
            val isProductionReady = AdManageKitConfig.isProductionReady()
            Log.d("MainActivity", "🚀 Production ready: $isProductionReady")
            
        } catch (e: Exception) {
            Log.w("MainActivity", "Could not retrieve cache statistics: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Log final statistics before clearing
        if (AdManageKitConfig.debugMode) {
            Log.d("MainActivity", "🧹 Clearing all cached ads on destroy")
            logCacheStatistics()
        }
        
        NativeAdManager.clearAllCachedAds()
    }
}