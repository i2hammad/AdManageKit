// Copyright 2025 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example.snippets;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.example.nextgenexample.databinding.CustomNativeAdBinding;
import com.google.android.gms.ads.nativead.NativeAdAssetNames;
import com.google.android.libraries.ads.mobile.sdk.common.Image;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaContent;
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import com.google.android.libraries.ads.mobile.sdk.nativead.OnCustomClickListener;
import java.util.Arrays;
import java.util.List;

/** Java code snippets for the developer guide. */
public class CustomNativeSnippets {

  // Sample custom native ad unit ID for video ads.
  private static final String AD_UNIT_ID = "/21775744923/example/native";
  // Sample custom native format IDs.
  private static final String CUSTOM_NATIVE_FORMAT_ID = "12387226";
  private CustomNativeAdBinding customNativeAdBinding;

  public void loadCustomNativeAd() {
    // [START load_ad]
    NativeAdRequest adRequest =
        new NativeAdRequest.Builder(AD_UNIT_ID, List.of(NativeAd.NativeAdType.CUSTOM_NATIVE))
            .setCustomFormatIds(Arrays.asList(CUSTOM_NATIVE_FORMAT_ID))
            .build();

    // Load the native ad with the ad request and callback.
    NativeAdLoader.load(
        adRequest,
        new NativeAdLoaderCallback() {
          @Override
          public void onCustomNativeAdLoaded(@NonNull CustomNativeAd customNativeAd) {
            // TODO: Store the custom native ad.
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError adError) {}
        });
    // [END load_ad]
  }

  // [START display_ad]
  private void displayCustomNativeAd(CustomNativeAd customNativeAd, Context context) {
    // Render the text elements.

    // The `customNativeAdBinding` is the layout binding for the ad container that
    // contains all `CustomNativeAd` assets.
    if (customNativeAdBinding != null) {
      customNativeAdBinding.headline.setText(customNativeAd.getText("Headline"));
      customNativeAdBinding.caption.setText(customNativeAd.getText("Caption"));

      ImageView imageView = new ImageView(context);
      imageView.setAdjustViewBounds(true);
      imageView.setImageDrawable(customNativeAd.getImage("MainImage").getDrawable());
      // [START perform_click]
      imageView.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              customNativeAd.performClick("MainImage");
            }
          });
      // [END perform_click]
      customNativeAdBinding.mediaPlaceholder.addView(imageView);

      // Render the ad choices icon.
      renderAdChoices(customNativeAd);

      // [START record_impression]
      // Record an impression.
      customNativeAd.recordImpression();
      // [END record_impression]
    }
  }

  // [END display_ad]

  // [START display_video_ad]
  private void displayVideoCustomNativeAd(CustomNativeAd customNativeAd, Context context) {
    // Check whether the custom native ad has video content.
    MediaContent mediaContent = customNativeAd.getMediaContent();
    if (mediaContent != null && mediaContent.getHasVideoContent()) {
      // Render the media content in a MediaView.
      MediaView mediaView = new MediaView(context);
      mediaView.setMediaContent(mediaContent);
      customNativeAdBinding.mediaPlaceholder.addView(mediaView);
    } else {
      // Fall back to other assets defined on your custom native ad.
      ImageView imageView = new ImageView(context);
      imageView.setAdjustViewBounds(true);
      imageView.setImageDrawable(customNativeAd.getImage("MainImage").getDrawable());
      customNativeAdBinding.mediaPlaceholder.addView(imageView);
    }

    // Record an impression.
    customNativeAd.recordImpression();
  }

  // [END display_video_ad]

  // [START render_ad_choices]
  private void renderAdChoices(CustomNativeAd customNativeAd) {
    // Render the AdChoices image.
    Image adChoiceAsset =
        customNativeAd.getImage(NativeAdAssetNames.ASSET_ADCHOICES_CONTAINER_VIEW);
    if (adChoiceAsset != null) {
      if (customNativeAdBinding != null) {
        customNativeAdBinding.adchoices.setImageDrawable(adChoiceAsset.getDrawable());
        customNativeAdBinding.adchoices.setVisibility(View.VISIBLE);
        customNativeAdBinding.adchoices.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                // Handle click.
                customNativeAd.performClick(NativeAdAssetNames.ASSET_ADCHOICES_CONTAINER_VIEW);
              }
            });
      }
    } else {
      if (customNativeAdBinding != null) {
        customNativeAdBinding.adchoices.setVisibility(View.GONE);
      }
    }
  }

  // [END render_ad_choices]

  public void setOnCustomClickListener(CustomNativeAd customNativeAd) {
    // [START set_custom_click_listener]
    customNativeAd.setOnCustomClickListener(
        new OnCustomClickListener() {
          @Override
          public void onCustomClick(@NonNull String assetName) {
            // Perform your custom action.
          }
        });
    // [END set_custom_click_listener]
  }
}
