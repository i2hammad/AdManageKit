package com.i2hammad.admanagekit.admob

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdValue

/**
 * Abstract callback class for handling ad lifecycle events.
 * 
 * This class provides callback methods for various ad events including loading, showing,
 * user interactions, and error conditions. All methods have default empty implementations,
 * so you only need to override the methods relevant to your use case.
 * 
 * @since 1.0.0
 * 
 * Example usage:
 * ```kotlin
 * val callback = object : AdLoadCallback() {
 *     override fun onAdLoaded() {
 *         Log.d("Ads", "Ad loaded successfully")
 *         // Show your UI elements that depend on ad being ready
 *     }
 *     
 *     override fun onFailedToLoad(error: AdError?) {
 *         Log.e("Ads", "Failed to load ad: ${error?.message}")
 *         // Handle ad loading failure, maybe show alternative content
 *     }
 *     
 *     override fun onAdImpression() {
 *         // Track ad impression for analytics
 *         analytics.logEvent("ad_impression")
 *     }
 * }
 * 
 * // Use with any ad component
 * bannerAdView.loadBanner(this, "your-ad-unit-id", callback)
 * ```
 */
abstract class AdLoadCallback {

    /**
     * Called when an ad fails to load.
     * 
     * This method is invoked when an ad request fails due to network issues,
     * no fill, invalid ad unit ID, or other errors. You should handle this
     * gracefully by either retrying the request or showing alternative content.
     * 
     * @param error The error that occurred during ad loading. Contains error code,
     *              message, and domain information. May be null in rare cases.
     * 
     * Common error codes:
     * - ERROR_CODE_INTERNAL_ERROR (0): Internal error
     * - ERROR_CODE_INVALID_REQUEST (1): Invalid request  
     * - ERROR_CODE_NETWORK_ERROR (2): Network error
     * - ERROR_CODE_NO_FILL (3): No ad available
     * - ERROR_CODE_INVALID_AD_UNIT_ID (4): Invalid ad unit ID
     * - ERROR_CODE_MEDIATION_DATA_ERROR (5): Mediation error
     * - ERROR_CODE_MEDIATION_ADAPTER_ERROR (6): Mediation adapter error
     * - ERROR_CODE_MEDIATION_INVALID_AD_SIZE (7): Invalid ad size for mediation
     * - ERROR_CODE_INTERNAL_ERROR (8): Internal error
     * - ERROR_CODE_INVALID_ARGUMENT (9): Invalid argument
     * - ERROR_CODE_REQUEST_ID_MISMATCH (10): Request ID mismatch
     * 
     * @since 1.0.0
     */
    open fun onFailedToLoad(error: AdError?) {
        // Default implementation - override to handle failures
    }

    /**
     * Called when an ad is successfully loaded and ready to be displayed.
     * 
     * This is called after the ad content has been downloaded and is ready
     * for presentation. For banner ads, this means the ad can be shown immediately.
     * For interstitial and rewarded ads, this means they're ready to be shown
     * when you call the appropriate show method.
     * 
     * Note: This doesn't mean the ad has been shown to the user yet.
     * Use [onAdImpression] to track when the ad is actually displayed.
     * 
     * @since 1.0.0
     */
    open fun onAdLoaded() {
        // Default implementation - override to handle successful loads
    }

    /**
     * Called when the user clicks on an ad.
     * 
     * This method is invoked when the user taps or clicks on the ad content.
     * It's called before the user is taken to the ad's destination (like a
     * website or app store). This is a good place to pause game logic,
     * music, or other app activities.
     * 
     * Note: This may not be called for all ad formats or networks.
     * 
     * @since 1.0.0
     */
    open fun onAdClicked() {
        // Default implementation - override to handle ad clicks
    }

    /**
     * Called when a full-screen ad is closed.
     * 
     * This method is invoked when the user closes a full-screen ad
     * (interstitial, rewarded, or app open ad) and returns to your app.
     * This is the appropriate time to resume game logic, music,
     * or other app activities that were paused.
     * 
     * For banner ads, this method is called when the expanded ad is closed.
     * 
     * @since 1.0.0
     */
    open fun onAdClosed() {
        // Default implementation - override to handle ad closing
    }

    /**
     * Called when an ad impression is recorded.
     * 
     * This method is invoked when the ad is actually displayed to the user
     * and an impression is counted. This is the most reliable way to track
     * when users have seen your ads for analytics purposes.
     * 
     * An impression is typically recorded when:
     * - Banner ads: When the ad becomes visible on screen
     * - Interstitial ads: When the full-screen ad is shown
     * - Rewarded ads: When the video starts playing
     * - Native ads: When the ad content is rendered and visible
     * 
     * @since 1.0.0
     */
    open fun onAdImpression() {
        // Default implementation - override to track impressions
    }

    /**
     * Called when a full-screen ad is opened.
     * 
     * This method is invoked when a full-screen ad (interstitial, rewarded,
     * or app open ad) is opened and displayed to the user. This happens
     * before [onAdImpression] and is a good time to pause app activities.
     * 
     * For banner ads, this method is called when the ad is expanded to fullscreen.
     * 
     * @since 1.0.0
     */
    open fun onAdOpened() {
        // Default implementation - override to handle ad opening
    }

    /**
     * Called when ad revenue information is available.
     * 
     * This method provides revenue data for the ad impression, which is useful
     * for tracking LTV (Lifetime Value) and ROAS (Return on Ad Spend) metrics.
     * The revenue information includes the estimated earnings and currency.
     * 
     * Note: Revenue data may not be available for all ad networks or formats.
     * This is primarily supported by Google AdMob and some mediation partners.
     * 
     * @param adValue The revenue information including value in micros and currency code.
     *                Value is in micro-units (1/1,000,000 of a currency unit).
     * 
     * Example:
     * ```kotlin
     * override fun onPaidEvent(adValue: AdValue) {
     *     val revenueInCurrency = adValue.valueMicros / 1_000_000.0
     *     Log.d("Revenue", "Earned $revenueInCurrency ${adValue.currencyCode}")
     *     
     *     // Send to analytics
     *     analytics.logRevenue(revenueInCurrency, adValue.currencyCode)
     * }
     * ```
     * 
     * @since 2.1.0
     */
    open fun onPaidEvent(adValue: AdValue) {
        // Default implementation - override to handle revenue tracking
    }

    /**
     * Called when ad loading starts.
     * 
     * This method is invoked when an ad request begins. It's useful for
     * showing loading indicators or preparing your UI for the incoming ad.
     * 
     * Note: This is called before the actual network request, so it doesn't
     * guarantee that an ad will be loaded successfully.
     * 
     * @since 2.1.0
     */
    open fun onAdLoadStarted() {
        // Default implementation - override to handle load start
    }

    /**
     * Called when ad loading is cancelled.
     * 
     * This method is invoked when an ad loading operation is cancelled,
     * either programmatically or due to app lifecycle changes.
     * 
     * @since 2.1.0
     */
    open fun onAdLoadCancelled() {
        // Default implementation - override to handle load cancellation
    }
}