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
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.databinding.FragmentFullScreenNativeBinding;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;

/** A [AdFragment] subclass that represents the native ad. */
public class FullScreenNativeFragment extends AdFragment<FragmentFullScreenNativeBinding> {
  // Default constructor required for fragment instantiation.
  public FullScreenNativeFragment() {}

  private NativeAdViewModel viewModel;

  @Override
  protected BindingInflater<FragmentFullScreenNativeBinding> getBindingInflater() {
    return FragmentFullScreenNativeBinding::inflate;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = new ViewModelProvider(requireParentFragment()).get(NativeAdViewModel.class);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    AppCompatActivity appCompatActivity = ((AppCompatActivity) getActivity());
    if (appCompatActivity != null && appCompatActivity.getSupportActionBar() != null) {
      appCompatActivity.getSupportActionBar().hide();
    }

    NativeAd nativeAd = viewModel.getNativeAd();
    if (nativeAd != null) {
      displayNativeAd(nativeAd);
    }
  }

  @Override
  public void onDestroyView() {
    AppCompatActivity appCompatActivity = ((AppCompatActivity) getActivity());
    if (appCompatActivity != null && appCompatActivity.getSupportActionBar() != null) {
      appCompatActivity.getSupportActionBar().show();
    }
    super.onDestroyView();
  }

  private void displayNativeAd(NativeAd nativeAd) {
    if (getActivity() != null) {
      getActivity()
          .runOnUiThread(
              () -> {
                // Set the view element with the native ad assets.
                binding.adBody.setText(nativeAd.getBody());
                binding.adCallToAction.setText(nativeAd.getCallToAction());
                binding.adHeadline.setText(nativeAd.getHeadline());
                if (nativeAd.getIcon() != null) {
                  binding.adAppIcon.setImageDrawable(nativeAd.getIcon().getDrawable());
                }

                // Hide views for assets that don't have data.
                binding.adAppIcon.setVisibility(
                    nativeAd.getIcon() == null ? View.INVISIBLE : View.VISIBLE);

                // Inform the Google Mobile Ads SDK that you have finished populating the native ad
                // views with this native ad.
                binding.fullScreenNativeAd.registerNativeAd(nativeAd, binding.adMedia);
              });
    }
  }
}
