package com.i2hammad.admanagekit.admob

import com.google.android.gms.ads.nativead.NativeAd
import java.util.Collections

object NativeAdManager {
    var enableCachingNativeAds: Boolean = true
    private data class CachedAd(val ad: NativeAd, val cachedTime: Long)
    private val cachedAds: MutableMap<String, CachedAd> = Collections.synchronizedMap(mutableMapOf())

    fun setCachedNativeAd(adUnitId: String, ad: NativeAd) {
        if (enableCachingNativeAds) {
            cachedAds[adUnitId]?.ad?.destroy()
            cachedAds[adUnitId] = CachedAd(ad, System.currentTimeMillis())
        }
    }

    fun getCachedNativeAd(adUnitId: String): NativeAd? {
        if (!enableCachingNativeAds) return null
        val cachedAd = cachedAds[adUnitId] ?: return null
        val currentTime = System.currentTimeMillis()
        val adAgeSeconds = (currentTime - cachedAd.cachedTime) / 1000
        return if (adAgeSeconds <= 3600) {
            cachedAd.ad
        } else {
            cachedAd.ad.destroy()
            cachedAds.remove(adUnitId)
            null
        }
    }

    fun clearCachedAd(adUnitId: String) {
        cachedAds[adUnitId]?.ad?.destroy()
        cachedAds.remove(adUnitId)
    }

    fun clearAllCachedAds() {
        cachedAds.values.forEach { it.ad.destroy() }
        cachedAds.clear()
    }
}