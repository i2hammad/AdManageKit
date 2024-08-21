package com.i2hammad.admanagekit.billing;
/**
 * Interface for handling billing-related events.
 */
public interface BillingListener {

    /**
     * Called when the initialization of the billing process is finished.
     *
     * @param resultCode The result code indicating the success or failure of the initialization.
     *                   Typically, a value of 0 indicates success, while other values indicate errors.
     */
    void onInitBillingFinished(int resultCode);
}
