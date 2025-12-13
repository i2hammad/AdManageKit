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

package com.example.nextgenexample.fullscreennative;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.Constant;
import com.example.nextgenexample.R;
import com.example.nextgenexample.databinding.FragmentFullScreenNativeControllerBinding;
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd.NativeAdType;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest;
import java.util.List;

/** An [AdFragment] subclass that provides the UI to load and show a native ad. */
public class FullScreenNativeControllerFragment
    extends AdFragment<FragmentFullScreenNativeControllerBinding> {
  // Default constructor required for fragment instantiation.
  public FullScreenNativeControllerFragment() {}

  // Sample native ad unit ID.
  private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";
  private NativeAdViewModel viewModel;

  @Override
  protected BindingInflater<FragmentFullScreenNativeControllerBinding> getBindingInflater() {
    return FragmentFullScreenNativeControllerBinding::inflate;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = new ViewModelProvider(this).get(NativeAdViewModel.class);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    binding.loadAdButton.setOnClickListener(
        unusedView -> {
          binding.loadAdButton.setEnabled(false);
          binding.showAdButton.setEnabled(false);
          loadAd();
        });

    binding.showAdButton.setOnClickListener(
        unusedView -> {
          binding.loadAdButton.setEnabled(true);
          binding.showAdButton.setEnabled(false);
          showAd();
        });
  }

  private void loadAd() {
    NativeAdRequest adRequest =
        new NativeAdRequest.Builder(AD_UNIT_ID, List.of(NativeAdType.NATIVE)).build();
    // Define the callback to handle successful or failed ad loading.
    NativeAdLoaderCallback adLoaderCallback =
        new NativeAdLoaderCallback() {
          @Override
          public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
            Log.d(Constant.TAG, "Native ad loaded.");
            showToast("Native ad loaded.");
            viewModel.setNativeAd(nativeAd);
            setNativeAdEventCallback(nativeAd);
            if (getActivity() != null) {
              getActivity().runOnUiThread(() -> binding.showAdButton.setEnabled(true));
            }
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            Log.d(Constant.TAG, "Native ad failed to load :" + adError);
            showToast("Native ad failed to load with error code: " + adError.getCode());
            if (getActivity() != null) {
              getActivity().runOnUiThread(() -> binding.loadAdButton.setEnabled(true));
            }
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

  private void showAd() {
    getChildFragmentManager()
        .beginTransaction()
        .add(R.id.full_screen_native_ad_controller, new FullScreenNativeFragment())
        .setReorderingAllowed(true)
        .addToBackStack(null)
        .commit();
  }
}
