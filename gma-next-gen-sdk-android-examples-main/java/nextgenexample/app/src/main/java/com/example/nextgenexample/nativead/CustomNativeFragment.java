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
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.Constant;
import com.example.nextgenexample.R;
import com.example.nextgenexample.databinding.CustomNativeAdBinding;
import com.example.nextgenexample.databinding.FragmentCustomNativeBinding;
import com.example.nextgenexample.databinding.NativeAdBinding;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.Image;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.common.VideoController;
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions;
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaContent;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd.NativeAdType;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdAssetNames;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView;
import java.util.Collections;
import java.util.List;

/** An [AdFragment] subclass that loads a custom native ad. */
public class CustomNativeFragment extends AdFragment<FragmentCustomNativeBinding> {
  // Default constructor required for fragment instantiation.
  public CustomNativeFragment() {}

  // Sample custom native ad unit ID for non-video ads.
  public static final String IMAGE_AD_UNIT_ID = "/21775744923/example/native";
  // Sample custom native format ID.
  public static final String IMAGE_FORMAT_ID = "12387226";
  // Sample custom native ad unit ID for video ads.
  public static final String VIDEO_AD_UNIT_ID = "/21775744923/example/native-video";
  // Sample custom native format ID.
  public static final String VIDEO_FORMAT_ID = "12406343";

  private CustomNativeAd lastCustomNativeAd;
  private NativeAd lastNativeAd;
  private CustomVideoControlsView customControls;
  private boolean isUIEnabled = true;

  @Override
  protected BindingInflater<FragmentCustomNativeBinding> getBindingInflater() {
    return FragmentCustomNativeBinding::inflate;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    binding.refreshAdButton.setOnClickListener(unusedView -> loadAd());
    binding.checkRequestVideo.setOnClickListener(unusedView -> updateUI());
  }

  public void onDestroyView() {
    super.onDestroyView();

    // Always call destroy() on ads on removal.
    destroyNativeAds();
  }

  private void destroyNativeAds() {
    if (lastCustomNativeAd != null) {
      lastCustomNativeAd.destroy();
      lastCustomNativeAd = null;
    }
    if (lastNativeAd != null) {
      lastNativeAd.destroy();
      lastNativeAd = null;
    }
    if (customControls != null) {
      customControls = null;
    }
  }

  private void loadAd() {
    setUIEnabled(false);

    // Build an ad request with native ad options to customize the user experience.
    String adUnitID = binding.checkRequestVideo.isChecked() ? VIDEO_AD_UNIT_ID : IMAGE_AD_UNIT_ID;
    String formatID = binding.checkRequestVideo.isChecked() ? VIDEO_FORMAT_ID : IMAGE_FORMAT_ID;

    NativeAdType adType =
        binding.checkRequestCustomNative.isChecked()
            ? NativeAdType.CUSTOM_NATIVE
            : NativeAdType.NATIVE;

    VideoOptions videoOptions =
        new VideoOptions.Builder()
            .setStartMuted(binding.checkStartMuted.isChecked())
            .setCustomControlsRequested(binding.checkCustomControls.isChecked())
            .build();

    NativeAdRequest adRequest =
        new NativeAdRequest.Builder(adUnitID, List.of(adType))
            .setVideoOptions(videoOptions)
            .setCustomFormatIds(Collections.singletonList(formatID))
            .build();

    // Define the callback to handle successful ad loading or failed ad loading.
    NativeAdLoaderCallback adCallback =
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
                  destroyNativeAds();
                  setNativeAdEventCallback(nativeAd);
                  displayNativeAd(nativeAd);
                  lastNativeAd = nativeAd;
                  setUIEnabled(true);
                });
          }

          @Override
          public void onCustomNativeAdLoaded(@NonNull CustomNativeAd customNativeAd) {
            Log.d(Constant.TAG, "Custom native ad loaded.");
            showToast("Custom native ad loaded.");
            runOnUiThread(
                () -> {
                  // Remove all old ad views when loading a new native ad.
                  binding.nativeViewContainer.removeAllViews();
                  // Always call destroy() on ads on removal.
                  destroyNativeAds();
                  displayCustomNativeAd(customNativeAd);
                  lastCustomNativeAd = customNativeAd;
                  setUIEnabled(true);
                });
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            Log.d(Constant.TAG, "Custom native ad failed to load: " + adError);
            showToast("Native ad failed to load. " + adError.getMessage());
            setUIEnabled(true);
          }
        };

    // Load the native ad with the ad request and callback.
    NativeAdLoader.load(adRequest, adCallback);
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

    MediaView mediaView = nativeAdView.getMediaView();
    MediaContent mediaContent = nativeAd.getMediaContent();
    VideoController videoController = mediaContent.getVideoController();
    if (videoController != null && mediaContent.getHasVideoContent() && mediaView != null) {
      // If the main asset is a video, set the videoLifecycleCallbacks.
      binding.textVideoStatus.setText(getString(R.string.nativead_video_play));
      videoController.setVideoLifecycleCallbacks(getVideoCallbacks());

      // Initialize custom controls if custom controls are enabled.
      if (videoController.isCustomControlsEnabled()) {
        customControls = new CustomVideoControlsView(requireContext());
        customControls.initialize(mediaContent, binding.checkStartMuted.isChecked());
        nativeAdBinding.videoHolder.addView(customControls);
        nativeAdBinding.videoHolder.bringChildToFront(customControls);
      }
    } else {
      // The main asset is an image.
      binding.textVideoStatus.setText(getString(R.string.nativead_video_none));
    }
  }

  private void displayCustomNativeAd(CustomNativeAd customNativeAd) {
    // Inflate the native ad view and add it to the view hierarchy.
    CustomNativeAdBinding customTemplateBinding =
        CustomNativeAdBinding.inflate(getLayoutInflater());
    binding.nativeViewContainer.addView(customTemplateBinding.getRoot());

    // Render the text elements.
    customTemplateBinding.headline.setText(customNativeAd.getText("Headline"));
    customTemplateBinding.caption.setText(customNativeAd.getText("Caption"));

    // Render the AdChoices image.
    String adChoicesKey = NativeAdAssetNames.ASSET_ADCHOICES_CONTAINER_VIEW;
    Image adChoiceAsset = customNativeAd.getImage(adChoicesKey);
    if (adChoiceAsset != null) {
      customTemplateBinding.adchoices.setImageDrawable(adChoiceAsset.getDrawable());
      customTemplateBinding.adchoices.setVisibility(View.VISIBLE);
      customTemplateBinding.adchoices.setOnClickListener(
          view -> customNativeAd.performClick(adChoicesKey));
    } else {
      customTemplateBinding.adchoices.setVisibility(View.GONE);
    }

    // Check the MediaContent.hasVideoContent to determine if the main asset is a video or
    // not.
    MediaContent mediaContent = customNativeAd.getMediaContent();
    if (mediaContent != null && mediaContent.getHasVideoContent()) {
      // If the main asset is a video, render it with a MediaView.
      MediaView mediaView = new MediaView(requireContext());
      mediaView.setMediaContent(mediaContent);
      customTemplateBinding.mediaPlaceholder.addView(mediaView);

      // Get the video controller for the ad and listen to lifecycle events.
      VideoController videoController = mediaContent.getVideoController();
      if (videoController != null) {
        // If the main asset is a video, set the videoLifecycleCallbacks.
        videoController.setVideoLifecycleCallbacks(getVideoCallbacks());
        binding.textVideoStatus.setText(getString(R.string.nativead_video_play));

        // Initialize custom controls.
        if (videoController.isCustomControlsEnabled()) {
          customControls = new CustomVideoControlsView(requireContext());
          customControls.initialize(mediaContent, binding.checkStartMuted.isChecked());
          customTemplateBinding.mediaPlaceholder.addView(customControls);
          customTemplateBinding.mediaPlaceholder.bringChildToFront(customControls);
        }
      }
    } else {
      // If the main asset is an image, render it with an ImageView.
      ImageView imageView = new ImageView(requireContext());
      imageView.setAdjustViewBounds(true);
      imageView.setImageDrawable(customNativeAd.getImage("MainImage").getDrawable());
      imageView.setOnClickListener(view -> customNativeAd.performClick("MainImage"));
      customTemplateBinding.mediaPlaceholder.addView(imageView);

      // If the main asset is an image, enable the UI.
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
          binding.checkCustomControls.setEnabled(videoUIEnabled);
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
