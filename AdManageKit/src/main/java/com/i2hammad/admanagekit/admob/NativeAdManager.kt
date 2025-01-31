package com.i2hammad.admanagekit.admob

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.i2hammad.admanagekit.billing.BillingConfig
//import com.i2hammad.admanagekit.billing.AppPurchase

object NativeAdManager {

    // Data class to hold ad-related information
    private data class AdInfo(
        val adUnitId: String,
        var adState: AdState = AdState.Idle,
        var retryCount: Int = 0,
        var isLoading: Boolean = false,
        var nativeAd: NativeAd? = null
    )

    // LiveData to hold the current AdStates mapped by customKey
    private val adStatesLiveData: MutableLiveData<Map<String, AdState>> = MutableLiveData(emptyMap())

    // Internal map to manage ad information with custom keys
    private val adsMap: MutableMap<String, AdInfo> = mutableMapOf()

    // Retry configuration
    private var maxRetries: Int = 3 // Default number of retries

    /**
     * LiveData to observe the ad states.
     * The key is the customKey, and the value is the AdState.
     */
    fun getAdStatesLiveData(): LiveData<Map<String, AdState>> = adStatesLiveData

    /**
     * Set the maximum number of retries for ad loading.
     */
    fun setMaxRetries(maxRetries: Int) {
        this.maxRetries = maxRetries
    }

    /**
     * Preload an ad with a custom key and its corresponding ad unit ID.
     *
     * @param context The application context.
     * @param customKey A unique identifier for the ad (e.g., "homepage_ad").
     * @param adUnitId The AdMob ad unit ID.
     */
    fun preloadAd(context: Context, customKey: String, adUnitId: String) {
//        Log.d("NativeAdManager", "Preloading ad for customKey: $customKey with adUnitId: $adUnitId")

        var purchaseProvider = BillingConfig.getPurchaseProvider()
        // Check if the user has purchased the ad-free experience
        if (purchaseProvider.isPurchased()) {
//            Log.d("NativeAdManager", "App is purchased. Skipping ad load for customKey: $customKey")
            updateAdState(customKey, AdState.Showed)
            return
        }

        val adInfo = adsMap.getOrPut(customKey) { AdInfo(adUnitId) }

        // Prevent multiple concurrent ad loads for the same customKey
        if (adInfo.isLoading) {
            Log.d("NativeAdManager", "Ad is already loading for customKey: $customKey")
            return
        }

        adInfo.isLoading = true
        updateAdState(customKey, AdState.Loading)

        val adLoader = AdLoader.Builder(context.applicationContext, adInfo.adUnitId)
            .forNativeAd { loadedNativeAd ->
                Log.d("NativeAdManager", "Ad successfully loaded for customKey: $customKey")
                adInfo.nativeAd?.destroy() // Destroy previous ad if any
                adInfo.nativeAd = loadedNativeAd
                updateAdState(customKey, AdState.Ready(loadedNativeAd))
                adInfo.retryCount = 0
                adInfo.isLoading = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("NativeAdManager", "Failed to load ad for customKey: $customKey: ${adError.message}")
                    updateAdState(customKey, AdState.Error(adError.message))
                    adInfo.isLoading = false

                    if (adInfo.retryCount < maxRetries) {
                        adInfo.retryCount++
                        Log.d(
                            "NativeAdManager",
                            "Retrying ad load for customKey: $customKey, attempt: ${adInfo.retryCount}"
                        )
                        preloadAd(context, customKey, adUnitId)
                    } else {
                        Log.d(
                            "NativeAdManager",
                            "Max retries reached for customKey: $customKey, ad failed to load."
                        )
                        updateAdState(customKey, AdState.Error("Max retries reached"))
                    }
                }

                override fun onAdOpened() {
                    super.onAdOpened()
//                    Log.d("NativeAdManager", "Ad opened for customKey: $customKey")
                    updateAdState(customKey, AdState.Showed)
                }

                override fun onAdClosed() {
                    super.onAdClosed()
//                    Log.d("NativeAdManager", "Ad closed for customKey: $customKey")
                    // Optionally, reset the state or preload a new ad
                    updateAdState(customKey, AdState.Idle)
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * Update the ad state for a given customKey and notify observers.
     */
    private fun updateAdState(customKey: String, newState: AdState) {
        val adInfo = adsMap[customKey]
        if (adInfo != null) {
            adInfo.adState = newState
            // Create a new map to trigger LiveData observers
            adStatesLiveData.postValue(adsMap.mapValues { it.value.adState })
        } else {
            Log.e("NativeAdManager", "Attempted to update state for unknown customKey: $customKey")
        }
    }

    /**
     * Retrieve the current state of a specific ad using its customKey.
     *
     * @param customKey The unique identifier for the ad.
     * @return The current AdState or null if the customKey doesn't exist.
     */
    fun getAdState(customKey: String): AdState? {
        return adsMap[customKey]?.adState
    }

    /**
     * Retrieve the NativeAd for a specific ad using its customKey.
     *
     * @param customKey The unique identifier for the ad.
     * @return The NativeAd instance or null if not available.
     */
    fun getNativeAd(customKey: String): NativeAd? {
        return adsMap[customKey]?.nativeAd
    }

    /**
     * Manually mark an ad as shown.
     *
     * @param customKey The unique identifier for the ad.
     */
    fun markAdAsShown(customKey: String) {
        updateAdState(customKey, AdState.Showed)
    }

    /**
     * Reset the ad status to Idle.
     *
     * @param customKey The unique identifier for the ad.
     */
    fun resetAdStatus(customKey: String) {
        updateAdState(customKey, AdState.Idle)
    }

    /**
     * Release resources for a specific ad to prevent memory leaks.
     *
     * @param customKey The unique identifier for the ad.
     */
    fun releaseAd(customKey: String) {
        adsMap[customKey]?.nativeAd?.destroy()
        adsMap.remove(customKey)
        updateAdState(customKey, AdState.Idle)
    }

    /**
     * Release all ads managed by NativeAdManager.
     */
    fun releaseAllAds() {
        adsMap.values.forEach { it.nativeAd?.destroy() }
        adsMap.clear()
        adStatesLiveData.postValue(emptyMap())
    }
}
