package com.i2hammad.admanagekit.admob

import com.google.android.gms.ads.AdError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * Interface defining a callback for ad management actions.
 * Implement this interface to handle actions that should occur
 * after an ad is shown or dismissed.
 */
abstract class AdManagerCallback {
    /**
     * Called to perform the next action after an ad is shown or dismissed.
     * Override this method to implement custom behavior when the ad flow completes.
     */

    open fun onNextAction() {
        // Default implementation
    }

    open fun onFailedToLoad(error: LoadAdError?) {
        // Default implementation

    }

    open fun onAdLoaded() {
        // Default implementation
    }

    /**
     * Called when the ad is shown and covers the full screen.
     * Use this to pause app content, mute audio, etc.
     */
    open fun onAdShowed() {
        // Default implementation
    }
}
