package com.i2hammad.admanagekit.admob;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.i2hammad.admanagekit.R;
import com.i2hammad.admanagekit.billing.AppPurchase;

public class BannerAdView extends RelativeLayout {

    private FirebaseAnalytics firebaseAnalytics;

    private AdView adView;
    private ShimmerFrameLayout shimmerFrameLayout;
    private RelativeLayout parent;
    private Activity context;

    public BannerAdView(Context context) {
        super(context);
        init(context);
    }

    public BannerAdView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BannerAdView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.banner_ad_view, this, true);

        parent = findViewById(R.id.parent);
        adView = new AdView(context);

        shimmerFrameLayout = findViewById(R.id.shimmer_frame_layout);

        shimmerFrameLayout.startShimmer();

        firebaseAnalytics = FirebaseAnalytics.getInstance(context);

    }


    public void loadBanner(Activity context, String adUnitId) {

        loadCollapsibleBanner(context, adUnitId, false);
    }

    public void loadCollapsibleBanner(Activity context, String adUnitId, Boolean collapsible) {
        this.context = context;
        if (AppPurchase.getInstance().isPurchased()) {
            shimmerFrameLayout.setVisibility(View.GONE);
            return;
        }
        adView.setAdUnitId(adUnitId);
        adView.setAdSize(getAdSize());

        AdRequest.Builder builder = new AdRequest.Builder();


        if (collapsible) {
            Bundle extras = new Bundle();
            extras.putString("collapsible", "bottom");
            builder = builder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
        }

        adView.loadAd(builder.build());


        LayoutParams adLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                parent.addView(adView);
                adView.setLayoutParams(adLayoutParams);
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);

                // Log Firebase event for ad loaded
                Bundle params = new Bundle();
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);

            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                shimmerFrameLayout.stopShimmer();
                shimmerFrameLayout.setVisibility(View.GONE);

                // Log Firebase event for ad failed to load
                Bundle params = new Bundle();
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                params.putString("ad_error_code", adError.getCode() + "");
                firebaseAnalytics.logEvent("ad_failed_to_load", params);




            }
        });
        adView.setOnPaidEventListener(new OnPaidEventListener() {
            @Override
            public void onPaidEvent(@NonNull AdValue adValue) {
                // Convert the value from micros to the standard currency unit
                double adValueInStandardUnits = adValue.getValueMicros() / 1_000_000.0;

                // Log Firebase event for paid event
                Bundle params = new Bundle();
                params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
                params.putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits);
                params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.getCurrencyCode());
                firebaseAnalytics.logEvent("ad_paid_event", params);
            }
        });
    }

    private AdSize getAdSize() {
        Rect bounds = new Rect();
        float adWidthPixels = parent.getWidth();

        // If the ad hasn't been laid out, default to the full screen width.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = context.getWindowManager().getCurrentWindowMetrics();
            bounds = windowMetrics.getBounds();
        } else {
            // Handle the case for older Android versions
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            display.getRectSize(bounds);
        }
        if (adWidthPixels == 0f) {
            adWidthPixels = bounds.width();
        }

        float density = getResources().getDisplayMetrics().density;
        int adWidth = (int) (adWidthPixels / density);

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
    }
}
