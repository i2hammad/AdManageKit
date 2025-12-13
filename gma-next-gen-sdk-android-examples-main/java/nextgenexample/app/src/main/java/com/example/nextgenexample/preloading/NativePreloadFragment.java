/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nextgenexample.preloading;

import static com.example.nextgenexample.Constant.TAG;
import static com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult.NativeAdSuccess;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.R;
import com.example.nextgenexample.databinding.FragmentPreloadBinding;
import com.example.nextgenexample.databinding.NativeAdBinding;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration;
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoadResult;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdPreloader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;
import java.util.List;

/** A [Fragment] subclass that preloads native ads. */
public class NativePreloadFragment extends AdFragment<FragmentPreloadBinding> {

  // Replace this test ad unit ID with your own ad unit ID.
  public static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";

  NativeAd currentAd;

  @Override
  protected BindingInflater<FragmentPreloadBinding> getBindingInflater() {
    return FragmentPreloadBinding::inflate;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    // Start preloading.
    startPreloadingWithCallback();

    // Initialize the UI.
    binding.txtTitle.setText(getText(R.string.native_ad));
    binding.btnShow.setOnClickListener(
        sender -> {
          pollAndShowAd();
          updateUI();
        });
    updateUI();
  }

  @Override
  public void onDestroyView() {
    // Always call destroy() on ads on removal.
    destroyCurrentAd();
    super.onDestroyView();
  }

  private void startPreloadingWithCallback() {
    PreloadCallback preloadCallback =
        // [Important] Do not call preload start of poll ad within the callback.
        new PreloadCallback() {
          @Override
          public void onAdFailedToPreload(
              @NonNull String preloadId, @NonNull LoadAdError loadAdError) {
            Log.i(TAG, "Native preload ad failed to load with error: " + loadAdError.getMessage());
            // [Optional] Get the error response info for additional details.
            // ResponseInfo responseInfo =   loadAdError.getResponseInfo();
          }

          @Override
          public void onAdsExhausted(@NonNull String preloadId) {
            Log.i(TAG, "No preloaded native ads available.");
            updateUI();
          }

          @Override
          public void onAdPreloaded(@NonNull String preloadId, @NonNull ResponseInfo responseInfo) {
            Log.i(TAG, "Native ad was preloaded.");
            updateUI();
          }
        };
    VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
    NativeAdRequest adRequest =
        new NativeAdRequest.Builder(AD_UNIT_ID, List.of(NativeAd.NativeAdType.NATIVE))
            .setVideoOptions(videoOptions)
            .build();
    PreloadConfiguration preload = new PreloadConfiguration(adRequest);
    NativeAdPreloader.start(AD_UNIT_ID, preload, preloadCallback);
  }

  private void pollAndShowAd() {
    // Polling returns the next available ad and loads another ad in the background.
    NativeAdLoadResult.NativeAdLoadSuccessResult result = NativeAdPreloader.pollAd(AD_UNIT_ID);
    if (result instanceof NativeAdSuccess) {
      // Destroy the previous native ad.
      destroyCurrentAd();

      // Get the native ad.
      var nativeAd = ((NativeAdSuccess) result).getAd();

      // Interact with the ad object as needed.
      Log.d(TAG, "Native ad response info: " + nativeAd.getResponseInfo());
      nativeAd.setAdEventCallback(
          new NativeAdEventCallback() {
            @Override
            public void onAdImpression() {
              Log.d(TAG, "Native ad recorded an impression.");
            }

            @Override
            public void onAdPaid(@NonNull AdValue value) {
              Log.d(
                  TAG,
                  String.format(
                      "Native ad onPaidEvent: %d %s",
                      value.getValueMicros(), value.getCurrencyCode()));
            }
          });

      // Show the new native ad.
      displayNativeAd(nativeAd);
      currentAd = nativeAd;
    } else {
      Log.i(TAG, "No preloaded native ads available.");
    }
    updateUI();
  }

  private boolean isAdAvailable() {
    return NativeAdPreloader.isAdAvailable(AD_UNIT_ID);
  }

  public synchronized void updateUI() {
    runOnUiThread(
        () -> {
          if (isAdAvailable()) {
            binding.txtStatus.setText(getString(R.string.available));
            binding.btnShow.setEnabled(true);
          } else {
            binding.txtStatus.setText(getString(R.string.exhausted));
            binding.btnShow.setEnabled(false);
          }
        });
  }

  private void destroyCurrentAd() {
    if (binding != null && binding.placeholder != null) {
      binding.placeholder.removeAllViews();
    }
    if (currentAd != null) {
      currentAd.destroy();
      currentAd = null;
    }
  }

  private void displayNativeAd(NativeAd nativeAd) {
    // Inflate the native ad view and add to the active view hierarchy.
    NativeAdBinding nativeAdBinding = NativeAdBinding.inflate(getLayoutInflater());
    NativeAdView nativeAdView = nativeAdBinding.getRoot();
    binding.placeholder.addView(nativeAdView);

    // Populate the view elements with their respective native ad asset.
    nativeAdBinding.adAdvertiser.setText(nativeAd.getAdvertiser());
    nativeAdBinding.adBody.setText(nativeAd.getBody());
    nativeAdBinding.adCallToAction.setText(nativeAd.getCallToAction());
    nativeAdBinding.adHeadline.setText(nativeAd.getHeadline());
    nativeAdBinding.adPrice.setText(nativeAd.getPrice());
    nativeAdBinding.adStore.setText(nativeAd.getStore());
    if (nativeAd.getIcon() != null && nativeAd.getIcon().getDrawable() != null) {
      nativeAdBinding.adIcon.setImageDrawable(nativeAd.getIcon().getDrawable());
    }
    if (nativeAd.getStarRating() != null) {
      float starRating = nativeAd.getStarRating().floatValue();
      nativeAdBinding.adStars.setRating(starRating);
    }

    // Hide views for assets that don't have data.
    nativeAdBinding.adAdvertiser.setVisibility(
        (nativeAd.getAdvertiser() == null ? View.INVISIBLE : View.VISIBLE));
    nativeAdBinding.adPrice.setVisibility(
        (nativeAd.getPrice() == null ? View.INVISIBLE : View.VISIBLE));
    nativeAdBinding.adStore.setVisibility(
        (nativeAd.getStore() == null ? View.INVISIBLE : View.VISIBLE));
    nativeAdBinding.adIcon.setVisibility(
        (nativeAd.getIcon() == null ? View.INVISIBLE : View.VISIBLE));
    nativeAdBinding.adStars.setVisibility(
        (nativeAd.getStarRating() == null ? View.INVISIBLE : View.VISIBLE));

    // Map each asset view property to the corresponding view in your view hierarchy.
    nativeAdView.setAdvertiserView(nativeAdBinding.adAdvertiser);
    nativeAdView.setBodyView(nativeAdBinding.adBody);
    nativeAdView.setCallToActionView(nativeAdBinding.adCallToAction);
    nativeAdView.setHeadlineView(nativeAdBinding.adHeadline);
    nativeAdView.setPriceView(nativeAdBinding.adPrice);
    nativeAdView.setStoreView(nativeAdBinding.adStore);
    nativeAdView.setIconView(nativeAdBinding.adIcon);
    nativeAdView.setStarRatingView(nativeAdBinding.adStars);

    // Inform the Google Mobile Ads SDK that you have finished populating the native ad
    // views with this native ad.
    nativeAdView.registerNativeAd(nativeAd, nativeAdBinding.adMedia);
  }
}
