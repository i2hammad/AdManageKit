/*
 * Copyright 2024 Google LLC
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

package com.example.nextgenexample.nativead;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.Constant;
import com.example.nextgenexample.R;
import com.example.nextgenexample.databinding.FragmentNativeBinding;
import com.example.nextgenexample.databinding.NativeAdBinding;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.VideoController;
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaContent;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd.NativeAdType;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;
import java.util.List;

/** An [AdFragment] subclass that loads a native ad. */
public class NativeFragment extends AdFragment<FragmentNativeBinding> {
  // Default constructor required for fragment instantiation.
  public NativeFragment() {}

  // Sample native image ad unit ID.
  private static final String IMAGE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";
  // Sample native video ad unit ID.
  private static final String VIDEO_AD_UNIT_ID = "ca-app-pub-3940256099942544/1044960115";

  private NativeAd lastNativeAd;
  private boolean isUIEnabled = true;
  private CustomVideoControlsView customControls;

  @Override
  protected BindingInflater<FragmentNativeBinding> getBindingInflater() {
    return FragmentNativeBinding::inflate;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    binding.refreshAdButton.setOnClickListener(
        unusedView -> {
          loadAd();
        });
    binding.checkRequestVideo.setOnClickListener(
        unusedView -> {
          updateUI();
        });
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();

    // Always call destroy() on ads on removal.
    destroyNativeAd();
    }

  private void destroyNativeAd() {
    if (lastNativeAd != null) {
      lastNativeAd.destroy();
      lastNativeAd = null;
    }
    customControls = null;
  }

  private void loadAd() {
    setUIEnabled(false);

    // Build an ad request with native ad options to customize the user experience.
    String adUnitID = binding.checkRequestVideo.isChecked() ? VIDEO_AD_UNIT_ID : IMAGE_AD_UNIT_ID;

    // Build an ad request with native ad options to customize the user experience.
    VideoOptions videoOptions =
        new VideoOptions.Builder().setStartMuted(binding.checkStartMuted.isChecked()).build();

    NativeAdRequest adRequest =
        new NativeAdRequest.Builder(adUnitID, List.of(NativeAdType.NATIVE))
            .setVideoOptions(videoOptions)
            .build();

    // Define the callback to handle successful or failed ad loading.
    NativeAdLoaderCallback adLoaderCallback =
        new NativeAdLoaderCallback() {
          @Override
          public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
            Log.d(Constant.TAG, "Native ad loaded.");
            showToast("Native ad loaded.");
            runOnUiThread(
                () -> {
                  // Remove all old ad views when loading a new native ad.
                  binding.nativeViewContainer.removeAllViews();
                  // Always call destroy() on ads on removal.
                  destroyNativeAd();
                  setNativeAdEventCallback(nativeAd);
                  displayNativeAd(nativeAd);
                  lastNativeAd = nativeAd;
                  setUIEnabled(true);
                });
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            Log.d(Constant.TAG, "Native ad failed to load :" + adError);
            runOnUiThread(
                () -> {
                  showToast("Native ad failed to load with error code: " + adError.getCode());
                  setUIEnabled(true);
                });
          }
        };

    // Load the native ad with the ad request and callback.
    NativeAdLoader.load(adRequest, adLoaderCallback);
  }

  private void setNativeAdEventCallback(NativeAd nativeAd) {
    nativeAd.setAdEventCallback(
        new NativeAdEventCallback() {
          @Override
          public void onAdShowedFullScreenContent() {
            Log.d(Constant.TAG, "Native ad showed full screen content.");
          }

          @Override
          public void onAdDismissedFullScreenContent() {
            Log.d(Constant.TAG, "Native ad dismissed full screen content.");
          }

          @Override
          public void onAdFailedToShowFullScreenContent(
              @NonNull FullScreenContentError fullScreenContentError) {
            Log.d(
                Constant.TAG,
                "Native ad failed to show full screen content with error : "
                    + fullScreenContentError);
          }

          @Override
          public void onAdImpression() {
            Log.d(Constant.TAG, "Native ad recorded an impression.");
          }

          @Override
          public void onAdClicked() {
            Log.d(Constant.TAG, "Native ad recorded a click.");
          }
        });
  }

  private void displayNativeAd(NativeAd nativeAd) {
    // Inflate the native ad view and add to the active view hierarchy.
    NativeAdBinding nativeAdBinding = NativeAdBinding.inflate(getLayoutInflater());
    NativeAdView nativeAdView = nativeAdBinding.getRoot();
    binding.nativeViewContainer.addView(nativeAdView);

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

    MediaContent mediaContent = nativeAd.getMediaContent();
    VideoController videoController = mediaContent.getVideoController();
    if (videoController != null && mediaContent.getHasVideoContent()) {
      // If the main asset is a video, set the videoLifecycleCallbacks.
      binding.textVideoStatus.setText(getString(R.string.nativead_video_play));
      videoController.setVideoLifecycleCallbacks(getVideoCallbacks());
    } else {
      // The main asset is an image
      binding.textVideoStatus.setText(getString(R.string.nativead_video_none));
    }
  }

  private VideoController.VideoLifecycleCallbacks getVideoCallbacks() {
    return new VideoController.VideoLifecycleCallbacks() {
      @Override
      public void onVideoStart() {
        binding.textVideoStatus.setText(getString(R.string.nativead_video_started));
        if (customControls != null) {
          customControls.onVideoStart();
        }
      }

      @Override
      public void onVideoEnd() {
        // Publishers should allow native ads to complete video playback before
        // refreshing or replacing them with another ad in the same UI location.
        binding.textVideoStatus.setText(getString(R.string.nativead_video_ended));
        if (customControls != null) {
          customControls.onVideoEnd();
        }
      }

      @Override
      public void onVideoPause() {
        binding.textVideoStatus.setText(getString(R.string.nativead_video_pause));
        if (customControls != null) {
          customControls.onVideoPause();
        }
      }

      @Override
      public void onVideoPlay() {
        binding.textVideoStatus.setText(getString(R.string.nativead_video_play));
        if (customControls != null) {
          customControls.onVideoPlay();
        }
      }

      @Override
      public void onVideoMute(boolean muted) {
        if (customControls != null) {
          customControls.onVideoMute(muted);
        }
      }
    };
  }

  private void setUIEnabled(boolean enableUI) {
    isUIEnabled = enableUI;
    updateUI();
  }

  private void updateUI() {
    runOnUiThread(
        () -> {
          binding.refreshAdButton.setEnabled(isUIEnabled);
          binding.checkRequestVideo.setEnabled(isUIEnabled);
          boolean videoUIEnabled = isUIEnabled && binding.checkRequestVideo.isChecked();
          binding.checkStartMuted.setEnabled(videoUIEnabled);
        });
  }

  public void runOnUiThread(Runnable action) {
    Activity activity = getActivity();
    if (activity == null) {
      return;
    }
    activity.runOnUiThread(action);
  }
}
