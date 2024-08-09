package com.i2hammad.admanagekit.admob;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.analytics.FirebaseAnalytics;
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
    private int adDisplayCount = 0; // Track the number of times ads have been displayed
    private FirebaseAnalytics firebaseAnalytics;

    private AdManager() {
    }

    public static AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    public void initializeFirebase(Context context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    private void showLoadingDialog(Activity activity, AdManagerCallback callback, boolean isReload) {
//        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
//        builder.setTitle("Please Wait");
//        builder.setMessage("Loading ad, please wait a moment...");
//        builder.setCancelable(false);
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//
//        // Delay to dismiss the dialog and show the ad
//        new Handler().postDelayed(() -> {
//            dialog.dismiss();
//
//        }, 1000); // Delay time (e.g., 1 seconds)

        if (isReady()) {
            showAd(activity, callback, isReload);
        } else {
            callback.onNextAction();
        }
    }

    public void loadInterstitialAd(Context context, String adUnitId) {
        this.adUnitId = adUnitId;
        initializeFirebase(context);
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

                // Log Firebase event for ad failed to load
                Bundle params = new Bundle();
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                params.putString("ad_error_code", loadAdError.getCode() + "");
                firebaseAnalytics.logEvent("ad_failed_to_load", params);
            }
        });
    }

    public void loadInterstitialAd(Context context, String adUnitId, InterstitialAdLoadCallback interstitialAdLoadCallback) {
        this.adUnitId = adUnitId;
        initializeFirebase(context);
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

                // Log Firebase event for ad failed to load
                Bundle params = new Bundle();
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                params.putString("ad_error_code", loadAdError.getCode() + "");
                firebaseAnalytics.logEvent("ad_failed_to_load", params);
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
        showLoadingDialog(activity,callback,true);
    }

    /**
     * Shows an interstitial ad based on the specified time interval criteria.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     */
    public void showInterstitialAdByTime(Activity activity, AdManagerCallback callback) {
        if (canShowAd()) {
            showLoadingDialog(activity,callback,true);
        } else {
            callback.onNextAction();
        }
    }

    /**
     * Shows an interstitial ad based on the specified number of times it has been displayed.
     *
     * @param activity The activity used to display the ad.
     * @param callback The callback to handle actions after the ad is closed.
     * @param maxDisplayCount The maximum number of times the ad can be displayed.
     */
    public void showInterstitialAdByCount(Activity activity, AdManagerCallback callback, int maxDisplayCount) {
        if (adDisplayCount < maxDisplayCount) {
            showLoadingDialog(activity,callback,true);
        } else {
            callback.onNextAction();
        }
    }

    public boolean isReady() {
        return mInterstitialAd != null && !AppPurchase.getInstance().isPurchased();
    }

    public boolean isDisplayingAd() {
        return isDisplayingAd;
    }

    public void setAdInterval(long intervalMillis) {
        this.adIntervalMillis = intervalMillis;
    }

    public int getAdDisplayCount() {
        return adDisplayCount;
    }

    public void setAdDisplayCount(int count) {
        this.adDisplayCount = count;
    }

    private boolean canShowAd() {
        long elapsed = System.currentTimeMillis() - lastAdShowTime;
        Log.d("AdManager", "Time since last ad: " + elapsed + " milliseconds");
        return elapsed > adIntervalMillis;
    }

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

                    // Log Firebase event for ad dismissed
                    Bundle params = new Bundle();
                    params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                    firebaseAnalytics.logEvent("ad_dismissed", params);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    isDisplayingAd = false;
                    mInterstitialAd = null;
                    Log.e("AdManager", "Failed to show full-screen content: " + adError.getMessage());
                    callback.onNextAction();

                    // Log Firebase event for ad failed to show
                    Bundle params = new Bundle();
                    params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                    params.putString("ad_error_code", adError.getCode() + "");
                    firebaseAnalytics.logEvent("ad_failed_to_show", params);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    isDisplayingAd = true;
                    lastAdShowTime = System.currentTimeMillis();
                    adDisplayCount++;

                    // Log Firebase event for ad impression
                    Bundle params = new Bundle();
                    params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);
                }
            });
            mInterstitialAd.show(activity);
        } else {
            callback.onNextAction();
        }
    }
}
