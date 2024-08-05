package com.i2hammad.admanagekit.admob;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.i2hammad.admanagekit.billing.AppPurchase;

/**
 * AdManager is a singleton class responsible for managing interstitial ads
 * using Google AdMob. It provides functionality to load and show ads, manage
 * display intervals, and handle ad-related callbacks.
 */
public class AdManager {

    private static AdManager instance;
    private InterstitialAd mInterstitialAd;
    private String adUnitId;
    private boolean isAdLoading = false;
    private boolean isDisplayingAd = false;
    private long lastAdShowTime = 0;
    private long adIntervalMillis = 15 * 1000; // Default to 15 seconds

    /**
     * Private constructor to enforce singleton usage.
     */
    private AdManager() {
    }

    /**
     * Returns the singleton instance of AdManager.
     *
     * @return The singleton instance of AdManager.
     */
    public static AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    /**
     * Loads an interstitial ad using the specified ad unit ID.
     *
     * @param context  The context used for loading the ad.
     * @param adUnitId The ad unit ID used to load the interstitial ad.
     */
    public void loadInterstitialAd(Context context, String adUnitId) {
        this.adUnitId = adUnitId;
        AdRequest adRequest = new AdRequest.Builder().build();

        isAdLoading = true;
        InterstitialAd.load(context, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                isAdLoading = false;
                Log.d("AdManager", "Interstitial ad loaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e("AdManager", "Failed to load interstitial ad: " + loadAdError.getMessage());
                isAdLoading = false;
                mInterstitialAd = null;
            }
        });
    }


    /**
     * Loads an interstitial ad using the specified ad unit ID.
     *
     * @param context  The context used for loading the ad.
     * @param adUnitId The ad unit ID used to load the interstitial ad.
     * @param interstitialAdLoadCallback The callback used to inform user about the load status of the interstitial ad.
     */
    public void loadInterstitialAd(Context context, String adUnitId, InterstitialAdLoadCallback interstitialAdLoadCallback) {
        this.adUnitId = adUnitId;
        AdRequest adRequest = new AdRequest.Builder().build();

        isAdLoading = true;
        InterstitialAd.load(context, adUnitId, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                isAdLoading = false;
                Log.d("AdManager", "Interstitial ad loaded");
                interstitialAdLoadCallback.onAdLoaded(mInterstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e("AdManager", "Failed to load interstitial ad: " + loadAdError.getMessage());
                isAdLoading = false;
                mInterstitialAd = null;
                interstitialAdLoadCallback.onAdFailedToLoad(loadAdError);
            }

        });
    }

    /**
     * Shows an interstitial ad immediately, regardless of the time interval.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    public void forceShowInterstitial(Activity activity, AdManagerCallback callback) {
        showAd(activity, callback, true);
    }

    /**
     * Shows an interstitial ad based on the specified time interval criteria.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    public void showInterstitialAdByTimes(Activity activity, AdManagerCallback callback) {
        if (canShowAd()) {
            showAd(activity, callback, false);
        } else {
            callback.onNextAction();
        }
    }

    /**
     * Checks if an interstitial ad is ready to be shown.
     *
     * @return True if an ad is ready and no purchase is detected, otherwise false.
     */
    public boolean isReady() {
        return mInterstitialAd != null && !AppPurchase.getInstance().isPurchased();
    }

    /**
     * Checks if an interstitial ad is currently being displayed.
     *
     * @return True if an ad is being displayed, otherwise false.
     */
    public boolean isDisplayingAd() {
        return isDisplayingAd;
    }

    /**
     * Sets the custom time interval for displaying interstitial ads. default is 15 secs
     *
     * @param intervalMillis The time interval in milliseconds.
     */
    public void setAdInterval(long intervalMillis) {
        this.adIntervalMillis = intervalMillis;
    }

    /**
     * Checks if enough time has passed since the last ad was shown.
     *
     * @return True if the time elapsed since the last ad is greater than the set interval.
     */
    private boolean canShowAd() {
        long elapsed = System.currentTimeMillis() - lastAdShowTime;
        Log.d("AdManager", "Time since last ad: " + elapsed + " milliseconds");
        return elapsed > adIntervalMillis;
    }

    /**
     * Handles the display of interstitial ads, setting callbacks for ad events.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param reloadAd Indicates if the ad should be reloaded after being shown.
     */
    private void showAd(Activity activity, AdManagerCallback callback, boolean reloadAd) {
        if (isReady()) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    isDisplayingAd = false;
                    mInterstitialAd = null;
                    callback.onNextAction();
                    if (reloadAd) {
                        loadInterstitialAd(activity, adUnitId);
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    isDisplayingAd = false;
                    mInterstitialAd = null;
                    Log.e("AdManager", "Failed to show full-screen content: " + adError.getMessage());
                    callback.onNextAction();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    isDisplayingAd = true;
                    lastAdShowTime = System.currentTimeMillis();
                }
            });
            mInterstitialAd.show(activity);
        } else {
            callback.onNextAction();
        }
    }
}
