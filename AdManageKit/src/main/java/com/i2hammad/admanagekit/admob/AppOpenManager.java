package com.i2hammad.admanagekit.admob;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.i2hammad.admanagekit.billing.AppPurchase;

/**
 * Prefetches App Open Ads.
 */

public class AppOpenManager implements Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private Activity currentActivity;
    private static final String LOG_TAG = "AppOpenManager";


    private AppOpenAd appOpenAd = null;

    private AppOpenAd.AppOpenAdLoadCallback loadCallback;

    private final Application myApplication;

    public static boolean isShowingAd = false;

    public static boolean isShownAd = false;

    private boolean skipNextAd = false;
    private String AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";

    /**
     * Shows the ad if one isn't already showing.
     */

    public void showAdIfAvailable() {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (!isShowingAd && isAdAvailable() && !skipNextAd) {
            Log.e(LOG_TAG, "Will show ad.");

            FullScreenContentCallback fullScreenContentCallback = new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.

                    AppOpenManager.this.appOpenAd = null;
                    isShowingAd = false;

                    fetchAd();


                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {

                }

                @Override
                public void onAdShowedFullScreenContent() {
                    isShowingAd = true;
                    isShownAd = true;
                }
            };

            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);

        } else {
            Log.d(LOG_TAG, "Can not show ad.");
            fetchAd();
        }


        skipNextAd = false;
    }

    public void skipNextAd() {
        skipNextAd = true;
    }

    /**
     * Constructor
     */
    public AppOpenManager(Application myApplication, String adUnitId) {
        this.AD_UNIT_ID = adUnitId;
        this.myApplication = myApplication;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);


    }

    public void setAdUnitId(String adUnitId) {
        this.AD_UNIT_ID = adUnitId;
    }


    @OnLifecycleEvent(ON_START)
    public void onStart() {

        if (!AppPurchase.getInstance().isPurchased()) {
            showAdIfAvailable();
            Log.d(LOG_TAG, "onStart");
        }
    }


    /**
     * Request an ad
     */
    public void fetchAd() {
        // Have unused ad, no need to fetch another.
        if (isAdAvailable()) {
            return;
        }

        loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            /**
             * Called when an app open ad has loaded.
             *
             * @param ad the loaded app open ad.
             */
            @Override
            public void onAdLoaded(@NonNull AppOpenAd ad) {


                AppOpenManager.this.appOpenAd = ad;
            }

            /**
             * Called when an app open ad has failed to load.
             *
             * @param loadAdError the error.
             */
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error.
                Log.e("TAG", "onAdFailedToLoad: " + "failde to load");
            }

        };
        AdRequest request = getAdRequest();

        AppOpenAd.load(myApplication, AD_UNIT_ID, request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
    }

    /**
     * Creates and returns ad request.
     */
    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    /**
     * Utility method that checks if ad exists and can be shown.
     */
    public boolean isAdAvailable() {
        return appOpenAd != null;
    }


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;

    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;

    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        currentActivity = null;
    }
}