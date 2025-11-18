package com.i2hammad.admanagekit.admob

import com.google.android.gms.ads.LoadAdError

/**
 * Java-friendly callbacks for InterstitialAdBuilder.
 *
 * These interfaces allow Java code to use the builder pattern without lambda syntax issues.
 */

/**
 * Callback interface for ad completion (when ad is shown/dismissed or failed)
 */
interface OnAdCompleteListener {
    fun onComplete()
}

/**
 * Callback interface for when ad is successfully loaded
 */
interface OnAdLoadedListener {
    fun onAdLoaded()
}

/**
 * Callback interface for when ad is shown to the user
 */
interface OnAdShownListener {
    fun onAdShown()
}

/**
 * Callback interface for when ad fails to load or show
 */
interface OnAdFailedListener {
    fun onAdFailed(error: LoadAdError)
}

/**
 * Combined callback interface for all events
 */
interface InterstitialAdCallback {
    fun onAdLoaded() {}
    fun onAdShown() {}
    fun onAdFailed(error: LoadAdError) {}
    fun onComplete() {}
}
