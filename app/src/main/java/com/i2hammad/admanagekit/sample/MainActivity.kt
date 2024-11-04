package com.i2hammad.admanagekit.sample

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.i2hammad.admanagekit.AdConfig
import com.i2hammad.admanagekit.admob.AdState
import com.i2hammad.admanagekit.admob.NativeAdManager
import com.i2hammad.admanagekit.admob.NativeBannerMedium
import com.i2hammad.admanagekit.admob.NativeBannerSmall
import com.i2hammad.admanagekit.admob.NativeLarge

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

        // Get the shared ViewModel


        loadAds()


    }


    private fun loadAds() {


        val adUnitId = "ca-app-pub-3940256099942544/2247696110"

//        NativeAdManager.preloadAd(this, adUnitId)

//        var nativeBannerSmall: NativeBannerSmall = findViewById(R.id.nativeBannerSmall)
//        nativeBannerSmall.loadNativeBannerAd(this,"ca-app-pub-3940256099942544/2247696110")
//        NativeAdManager.getAdLiveData(adUnitId).observe(this, { nativeAd ->
//
//            Log.d("NativeAdManager", "LiveData observer triggered for $adUnitId")
//            if (nativeAd != null) {
//                Log.d("NativeAdManager", "Received NativeAd: $nativeAd")
//
//                nativeBannerSmall.displayAd(nativeAd)
//            } else {
//                Log.d("NativeAdManager", "Native ad is null")
//            }
//        })


        // Observe LiveData


        NativeAdManager.getAdState(AdConfig.NATIVE_BANNER_SMALL_AD).let { adState ->
            if (adState !is AdState.Ready) {
                NativeAdManager.preloadAd(this, AdConfig.NATIVE_BANNER_SMALL_AD, adUnitId)
            }
        }




        NativeAdManager.getAdStatesLiveData().observe(this, Observer { adStates ->
            adStates.forEach { (adKey, adState) ->
                handleAdState(adKey, adState)
            }
        })

//        var nativeBannerMedium: NativeBannerMedium = findViewById(R.id.nativeBannerMedium)
//        nativeBannerMedium.loadNativeBannerAd(this, "ca-app-pub-3940256099942544/2247696110")
//
//
//        var nativeLarge: NativeLarge = findViewById(R.id.nativeLarge)
//        nativeLarge.loadNativeAds(this, "ca-app-pub-3940256099942544/2247696110")


    }

    private fun handleAdState(adKey: String, adState: AdState) {
        when (adState) {
            is AdState.Loading -> {
                // Show a loading indicator for the specific ad unit
//                showLoadingIndicator(adUnitId, true)
            }

            is AdState.Ready -> {
                // Hide the loading indicator and display the ad
//                showLoadingIndicator(adUnitId, false)
                when (adKey) {
                    AdConfig.NATIVE_BANNER_SMALL_AD -> {
                        var nativeBannerSmall: NativeBannerSmall =
                            findViewById(R.id.nativeBannerSmall)
                        nativeBannerSmall.displayAd(adState.nativeAd)
                    }

                    AdConfig.NATIVE_BANNER_MEDIUM_AD -> {
                        var nativeBannerMedium: NativeBannerMedium =
                            findViewById(R.id.nativeBannerMedium)
                        nativeBannerMedium.displayAd(adState.nativeAd)
                    }

                    AdConfig.NATIVE_LARGE_AD -> {
                        var nativeLarge: NativeLarge = findViewById(R.id.nativeLarge)
                        nativeLarge.displayAd(adState.nativeAd)
                    }
                }

            }

            is AdState.Showed -> {
                // Handle the ad being shown
                Toast.makeText(this, "Ad $adKey is shown", Toast.LENGTH_SHORT).show()
            }

            is AdState.Error -> {
                // Hide the loading indicator and show an error message
//                showLoadingIndicator(adUnitId, false)
                Toast.makeText(
                    this, "Failed to load ad $adKey: ${adState.errorMessage}", Toast.LENGTH_SHORT
                ).show()
            }

            is AdState.Idle -> {
                // Handle idle state if necessary
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        NativeAdManager.releaseAllAds()
    }
}