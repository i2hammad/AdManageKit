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

    private static final String TAG = "AdsConsentManager";

    private final ConsentInformation consentInformation;
    private final AtomicBoolean canRequestAds = new AtomicBoolean(false);
    private final AtomicBoolean isProcessingConsent = new AtomicBoolean(false); // New flag to prevent concurrent requests
    private static volatile AdsConsentManager instance;

    /**
     * Private constructor to initialize AdsConsentManager with context.
     *
     * @param context The application context.
     */
    private AdsConsentManager(Context context) {
        this.consentInformation = UserMessagingPlatform.getConsentInformation(context);
        this.canRequestAds.set(false);
    }

    /**
     * Gets the singleton instance of AdsConsentManager.
     *
     * @param context The application context.
     * @return The instance of AdsConsentManager.
     */
    public static AdsConsentManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AdsConsentManager.class) {
                if (instance == null) {
                    instance = new AdsConsentManager(context.getApplicationContext());
                }
            }
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
        String consentString = context.getSharedPreferences(context.getPackageName() + "_preferences", 0)
                .getString("IABTCF_PurposeConsents", "");
        Log.d(TAG, "getConsentResult: Consent string = " + consentString);
        return consentString.isEmpty() || consentString.charAt(0) == '1';
    }

    /**
     * Requests user consent for ads using the User Messaging Platform.
     *
     * @param activity          The activity context.
     * @param umpResultListener The listener to receive the result of the consent request.
     */
    public void requestUMP(Activity activity, UMPResultListener umpResultListener) {
        requestUMP(activity, false, "", false, umpResultListener);
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
    public void requestUMP(Activity activity, boolean enableDebug, String testDevice, boolean resetData, UMPResultListener umpResultListener) {
        // Prevent concurrent consent requests
        if (isProcessingConsent.getAndSet(true)) {
            Log.d(TAG, "requestUMP: Consent request already in progress, ignoring");
            return;
        }

        Log.d(TAG, "requestUMP: Starting consent request, canRequestAds=" + canRequestAds.get());

        // Reset consent data if requested
        if (resetData) {
            Log.d(TAG, "requestUMP: Resetting consent data");
            consentInformation.reset();
        }

        // Check if ads can already be requested
        if (consentInformation.canRequestAds() && canRequestAds.get()) {
            Log.d(TAG, "requestUMP: Ads can already be requested, skipping consent form");
            isProcessingConsent.set(false);
            umpResultListener.onCheckUMPSuccess(getConsentResult(activity));
            return;
        }

        ConsentRequestParameters.Builder paramsBuilder = new ConsentRequestParameters.Builder();

        // Set debug settings if debugging is enabled
        if (enableDebug) {
            Log.d(TAG, "requestUMP: Enabling debug mode with test device ID: " + testDevice);
            paramsBuilder.setConsentDebugSettings(
                    new ConsentDebugSettings.Builder(activity)
                            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                            .addTestDeviceHashedId(testDevice)
                            .build()
            );
        }

        ConsentRequestParameters params = paramsBuilder.setTagForUnderAgeOfConsent(false).build();

        // Request consent information update
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    Log.d(TAG, "requestUMP: Consent info updated successfully");
                    // Load and show the consent form if required
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                        isProcessingConsent.set(false);
                        if (formError != null) {
                            Log.e(TAG, "requestUMP: Error loading consent form: " + formError.getMessage());
                        } else {
                            Log.d(TAG, "requestUMP: Consent form loaded and shown or not required");
                        }

                        // Update canRequestAds and notify listener
                        if (!canRequestAds.getAndSet(consentInformation.canRequestAds())) {
                            Log.d(TAG, "requestUMP: Notifying listener, canRequestAds=" + consentInformation.canRequestAds());
                            umpResultListener.onCheckUMPSuccess(getConsentResult(activity));
                        }
                    });
                },
                requestConsentError -> {
                    isProcessingConsent.set(false);
                    Log.e(TAG, "requestUMP: Consent info update failed: " + requestConsentError.getMessage());
                    // Update canRequestAds and notify listener
                    if (!canRequestAds.getAndSet(consentInformation.canRequestAds())) {
                        Log.d(TAG, "requestUMP: Notifying listener after error, canRequestAds=" + consentInformation.canRequestAds());
                        umpResultListener.onCheckUMPSuccess(getConsentResult(activity));
                    }
                }
        );
    }

    /**
     * Shows the privacy options form for the user to manage consent settings.
     *
     * @param activity          The activity context.
     * @param umpResultListener The listener to receive the result after showing the form.
     */
    public void showPrivacyOption(Activity activity, UMPResultListener umpResultListener) {
        Log.d(TAG, "showPrivacyOption: Showing privacy options form");
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (formError != null) {
                Log.e(TAG, "showPrivacyOption: Error showing privacy form: " + formError.getMessage());
            } else {
                Log.d(TAG, "showPrivacyOption: Privacy options form shown");
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
        boolean canRequest = consentInformation.canRequestAds();
        Log.d(TAG, "canRequestAds: Returning " + canRequest);
        return canRequest;
    }

    /**
     * Checks if privacy options are required.
     *
     * @return True if privacy options are required, false otherwise.
     */
    public boolean isPrivacyOptionsRequired() {
        boolean required = consentInformation.getPrivacyOptionsRequirementStatus() == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
        Log.d(TAG, "isPrivacyOptionsRequired: Returning " + required);
        return required;
    }
}

