package com.i2hammad.admanagekit.ump;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages user consent for ads using the User Messaging Platform (UMP).
 */
public class AdsConsentManager {

    // Logger tag
    private static final String TAG = AdsConsentManager.class.getName();

    // Consent information object from UMP
    private ConsentInformation consentInformation;

    // Atomic flag to check if ads can be requested
    private AtomicBoolean canRequestAds = new AtomicBoolean(false);

    // Singleton instance of AdsConsentManager
    private static AdsConsentManager instance;

    /**
     * Private constructor to initialize AdsConsentManager with context.
     *
     * @param context The application context.
     */
    private AdsConsentManager(Context context) {
        this.canRequestAds = new AtomicBoolean(false);
        this.consentInformation = UserMessagingPlatform.getConsentInformation(context);
    }

    /**
     * Gets the singleton instance of AdsConsentManager.
     *
     * @param context The application context.
     * @return The instance of AdsConsentManager.
     */
    public static AdsConsentManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdsConsentManager(context);
        }
        return instance;
    }

    /**
     * Gets the user's consent result for ads.
     *
     * @param context The application context.
     * @return True if the user has consented to ads, false otherwise.
     */
    public static boolean getConsentResult(Context context) {
        String consentString = context.getSharedPreferences(context.getPackageName() + "_preferences", 0).getString("IABTCF_PurposeConsents", "");
        return consentString.isEmpty() || consentString.charAt(0) == '1';
    }

    /**
     * Requests user consent for ads using the User Messaging Platform.
     *
     * @param activity          The activity context.
     * @param umpResultListener The listener to receive the result of the consent request.
     */
    public void requestUMP(Activity activity, UMPResultListener umpResultListener) {
        this.requestUMP(activity, false, "", false, umpResultListener);
    }

    /**
     * Requests user consent for ads using the User Messaging Platform with additional parameters.
     *
     * @param activity          The activity context.
     * @param enableDebug       Whether to enable debug mode for consent requests.
     * @param testDevice        The test device ID for debugging.
     * @param resetData         Whether to reset consent data.
     * @param umpResultListener The listener to receive the result of the consent request.
     */
    public void requestUMP(Activity activity, Boolean enableDebug, String testDevice, Boolean resetData, UMPResultListener umpResultListener) {

        ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder();

        // Set debug settings if debugging is enabled
        if (enableDebug) {
            paramsBuilder.setConsentDebugSettings(new ConsentDebugSettings.Builder(activity).setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA).addTestDeviceHashedId(testDevice).build());
        }

        // Build consent request parameters
        ConsentRequestParameters params = paramsBuilder.setTagForUnderAgeOfConsent(false).build();
        this.consentInformation = UserMessagingPlatform.getConsentInformation(activity);

        // Reset consent data if requested
        if (resetData) {
            this.consentInformation.reset();
        }

        // Request consent information update
        consentInformation.requestConsentInfoUpdate(activity, params, () -> {
            // Load and show the consent form if required
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                if (formError != null) {
                    Log.e(TAG, "Error loading consent form: " + formError.getMessage());
                } else {
                    Log.d(TAG, "Consent form loaded and shown");
                    if (!this.canRequestAds.getAndSet(true)) {
                        umpResultListener.onCheckUMPSuccess(getConsentResult(activity));
                    }
                }
            });
        }, requestConsentError -> {
            // Handle consent info update failure
            Log.e(TAG, "Consent info update failure: " + requestConsentError.getMessage());
            if (!this.canRequestAds.getAndSet(true)) {
                umpResultListener.onCheckUMPSuccess(getConsentResult(activity));
            }
        });

        // Check if ads can be requested and inform the listener
        if (this.consentInformation.canRequestAds() && !this.canRequestAds.getAndSet(true)) {
            Log.d(TAG, "Ads can be requested");
            umpResultListener.onCheckUMPSuccess(getConsentResult(activity));
        }
    }

    /**
     * Shows the privacy options form for the user to manage consent settings.
     *
     * @param activity          The activity context.
     * @param umpResultListener The listener to receive the result after showing the form.
     */
    public void showPrivacyOption(Activity activity, UMPResultListener umpResultListener) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (getConsentResult(activity)) {
            }
            umpResultListener.onCheckUMPSuccess(getConsentResult(activity));
        });
    }

    /**
     * Checks if ads can be requested based on the user's consent.
     *
     * @return True if ads can be requested, false otherwise.
     */
    public boolean canRequestAds() {
        return canRequestAds.get();
    }
}
