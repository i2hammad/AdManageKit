
package com.i2hammad.admanagekit.admob;
/**
 * Interface defining a callback for ad management actions.
 * Implement this interface to handle actions that should occur
 * after an ad is shown or dismissed.
 */
public interface AdManagerCallback {
    /**
     * Called to perform the next action after an ad is shown or dismissed.
     * Override this method to implement custom behavior when the ad flow completes.
     */
    default void onNextAction() {
        // Default implementation does nothing.
        // Override to provide custom behavior.
    }
}
