/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.Constant;
import com.example.nextgenexample.databinding.FragmentFluidSizeBinding;
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/** A [Fragment] subclass that loads a FLUID size ad. */
public class AdManagerFluidSizeFragment extends AdFragment<FragmentFluidSizeBinding> {
  public AdManagerFluidSizeFragment() {}

  private static final String AD_UNIT_ID = "/21775744923/example/api-demo/fluid";

  private FrameLayout fluidAdContainer;

  @Override
  protected BindingInflater<FragmentFluidSizeBinding> getBindingInflater() {
    return FragmentFluidSizeBinding::inflate;
  }

  private final Queue<Integer> adViewWidths = new LinkedList<>(Arrays.asList(200, 250, 320, 360));

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    fluidAdContainer = binding.adViewContainer;

    binding.fluidWidthChangeBtn.setOnClickListener(
        v -> {
          // Cycle to the next width in the queue.
          int newWidth = adViewWidths.poll();
          adViewWidths.add(newWidth);

          // Change the ad view container's width.
          ViewGroup.LayoutParams layoutParams = binding.adViewContainer.getLayoutParams();
          float scale = getResources().getDisplayMetrics().density;
          layoutParams.width = (int) (newWidth * scale + 0.5f);
          binding.adViewContainer.setLayoutParams(layoutParams);

          // Update the TextView with the new width.
          binding.fluidCurrentWidthText.setText(newWidth + " dp");
        });

    // [START fluid_size_ad_request]
    // Be sure to specify Fluid as the ad size in the Ad Manager UI and create
    // an ad request with FLUID size.
    BannerAdRequest adRequest = new BannerAdRequest.Builder(AD_UNIT_ID, AdSize.FLUID).build();
    // [END fluid_size_ad_request]

    loadAd(adRequest);
  }

  private void loadAd(BannerAdRequest adRequest) {
    BannerAd.load(
        adRequest,
        new AdLoadCallback<>() {
          @Override
          public void onAdLoaded(@NonNull BannerAd ad) {
            ad.setAdEventCallback(
                new BannerAdEventCallback() {
                  @Override
                  public void onAdImpression() {
                    Log.d(Constant.TAG, "Fluid size ad recorded an impression.");
                  }

                  @Override
                  public void onAdClicked() {
                    Log.d(Constant.TAG, "Fluid size ad recorded a click.");
                  }
                });

            Activity activity = requireActivity();
            activity.runOnUiThread(
                () -> {
                  fluidAdContainer.removeAllViews();
                  fluidAdContainer.addView(ad.getView(activity));
                });

            showToast("Fluid size ad loaded.");
            Log.d(Constant.TAG, "Fluid size ad loaded.");
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            showToast("Fluid size ad failed to load.");
            Log.w(Constant.TAG, "Fluid size ad failed to load: " + adError);
          }
        });
  }
}
