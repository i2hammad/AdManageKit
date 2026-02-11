package com.i2hammad.admanagekit.admob

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

    /**
     * Called when the ad failed to load.
     * @param error The error that occurred during ad loading.
     */
    open fun onFailedToLoad(error: AdKitError?) {
        // Default implementation
    }

    /**
     * Called when the ad has been loaded successfully.
     */
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

    /**
     * Called when the ad load timed out before an ad could be loaded.
     */
    open fun onAdTimedOut() {
        // Default implementation
    }
}
