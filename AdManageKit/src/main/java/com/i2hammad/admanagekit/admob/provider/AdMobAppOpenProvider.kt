package com.i2hammad.admanagekit.admob.provider

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AppOpenAdProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * AdMob implementation of [AppOpenAdProvider].
 * Wraps Google AdMob AppOpenAd behind the provider interface.
 *
 * Loaded ads are stored per ad unit ID so a single shared provider instance can
 * serve multiple placements (waterfalls) without one placement's load clobbering
 * or consuming another placement's ready ad.
 *
 * Each ad records its load timestamp; ads older than
 * [AdManageKitConfig.appOpenAdFreshnessThreshold] (default 4 hours, per Google's
 * recommendation) are treated as expired and never shown.
 */
class AdMobAppOpenProvider : AppOpenAdProvider {

    override val provider: AdProvider = AdProvider.ADMOB

    private class TimedAppOpenAd(val ad: AppOpenAd, val loadTimeMillis: Long)

    /** Ready ads keyed by ad unit ID, with their load timestamps. */
    private val loadedAds = ConcurrentHashMap<String, TimedAppOpenAd>()

    companion object {
        private const val TAG = "AdMobAppOpen"
    }

    private fun isFresh(entry: TimedAppOpenAd): Boolean {
        val threshold = AdManageKitConfig.appOpenAdFreshnessThreshold.inWholeMilliseconds
        if (threshold == 0L) return false // Duration.ZERO means always fetch fresh
        return System.currentTimeMillis() - entry.loadTimeMillis < threshold
    }

    /** Returns the fresh ad for [adUnitId], clearing (and ignoring) expired ones. */
    private fun freshAdOrNull(adUnitId: String): TimedAppOpenAd? {
        val entry = loadedAds[adUnitId] ?: return null
        if (!isFresh(entry)) {
            Log.d(TAG, "App open ad expired (older than freshness threshold): $adUnitId")
            loadedAds.remove(adUnitId, entry)
            return null
        }
        return entry
    }

    override fun loadAd(
        context: Context,
        adUnitId: String,
        callback: AppOpenAdProvider.AppOpenAdCallback
    ) {
        val adRequest = AdRequest.Builder(adUnitId).build()

        AppOpenAd.load(adRequest, object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(ad: AppOpenAd) {
                Handler(Looper.getMainLooper()).post {
                    loadedAds[adUnitId] = TimedAppOpenAd(ad, System.currentTimeMillis())
                    Log.d(TAG, "App open ad loaded: $adUnitId")
                    callback.onAdLoaded()
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Handler(Looper.getMainLooper()).post {
                    // Do NOT discard a previously loaded ad for this (or any other) unit:
                    // the failure only concerns this load request.
                    Log.e(TAG, "App open ad failed to load: ${adError.message}")
                    callback.onAdFailedToLoad(adError.toAdKitError())
                }
            }
        })
    }

    override fun showAd(activity: Activity, callback: AppOpenAdProvider.AppOpenShowCallback) {
        // No-arg fallback: show any ready (fresh) ad.
        val adUnitId = loadedAds.keys.firstOrNull { freshAdOrNull(it) != null }
        if (adUnitId == null) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded", provider.name)
            )
            callback.onAdDismissed()
            return
        }
        showAd(activity, adUnitId, callback)
    }

    override fun showAd(
        activity: Activity,
        adUnitId: String,
        callback: AppOpenAdProvider.AppOpenShowCallback
    ) {
        val entry = freshAdOrNull(adUnitId)
        if (entry == null) {
            callback.onAdFailedToShow(
                AdKitAdError(AdKitAdError.ERROR_CODE_INTERNAL, "No ad loaded for $adUnitId", provider.name)
            )
            callback.onAdDismissed()
            return
        }

        val ad = entry.ad

        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                Handler(Looper.getMainLooper()).post {
                    Log.d(TAG, "App open ad showed: $adUnitId")
                    callback.onAdShowed()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                Handler(Looper.getMainLooper()).post {
                    Log.d(TAG, "App open ad dismissed: $adUnitId")
                    loadedAds.remove(adUnitId, entry)
                    callback.onAdDismissed()
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: FullScreenContentError) {
                Handler(Looper.getMainLooper()).post {
                    Log.e(TAG, "App open ad failed to show: ${adError.message}")
                    loadedAds.remove(adUnitId, entry)
                    callback.onAdFailedToShow(adError.toAdKitError())
                    callback.onAdDismissed()
                }
            }

            override fun onAdClicked() {
                Handler(Looper.getMainLooper()).post {
                    callback.onAdClicked()
                }
            }

            override fun onAdImpression() {
                Handler(Looper.getMainLooper()).post {
                    callback.onAdImpression()
                }
            }

            override fun onAdPaid(value: AdValue) {
                Handler(Looper.getMainLooper()).post {
                    callback.onPaidEvent(value.toAdKitValue())
                }
            }
        }

        ad.show(activity)
    }

    override fun isAdReady(): Boolean = loadedAds.keys.any { freshAdOrNull(it) != null }

    override fun isAdReady(adUnitId: String): Boolean = freshAdOrNull(adUnitId) != null

    override fun destroy() {
        loadedAds.clear()
    }
}
