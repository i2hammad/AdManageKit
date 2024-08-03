package com.i2hammad.admanagekit.ump;

/**
 * Interface for receiving the result of a User Messaging Platform (UMP) consent check.
 */
public interface UMPResultListener {

    /**
     * Called when the UMP consent check completes successfully.
     *
     * @param isConsentGiven True if the user has given consent for ads, false otherwise.
     */
    void onCheckUMPSuccess(boolean isConsentGiven);
}
