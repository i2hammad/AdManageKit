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

package com.example.nextgenexample.banner;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.nextgenexample.AdFragment;
import com.example.nextgenexample.Constant;
import com.example.nextgenexample.databinding.FragmentMultipleAdSizesBinding;
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import java.util.ArrayList;

/** An [AdFragment] subclass that loads an ad configured to have multiple ad sizes. */
public class AdManagerMultipleAdSizesFragment extends AdFragment<FragmentMultipleAdSizesBinding> {
  // Default constructor required for fragment instantiation.
  public AdManagerMultipleAdSizesFragment() {}

  // Sample banner ad unit ID.
  private static final String AD_UNIT_ID = "/21775744923/example/api-demo/ad-sizes";
  private BannerAd bannerAd;
  private FrameLayout bannerContainer;

  @Override
  protected BindingInflater<FragmentMultipleAdSizesBinding> getBindingInflater() {
    return FragmentMultipleAdSizesBinding::inflate;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    bannerContainer = binding.bannerViewContainer;

    binding.loadAdButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (!binding.adsizesCb120x20.isChecked()
                && !binding.adsizesCb320x50.isChecked()
                && !binding.adsizesCb300x250.isChecked()) {
              Toast.makeText(getActivity(), "At least one size is required.", Toast.LENGTH_SHORT)
                  .show();
            } else {
              ArrayList<AdSize> sizeList = new ArrayList<>();

              if (binding.adsizesCb120x20.isChecked()) {
                sizeList.add(new AdSize(120, 20));
              }

              if (binding.adsizesCb320x50.isChecked()) {
                sizeList.add(AdSize.BANNER);
              }

              if (binding.adsizesCb300x250.isChecked()) {
                sizeList.add(AdSize.MEDIUM_RECTANGLE);
              }
              BannerAdRequest adRequest = new BannerAdRequest.Builder(AD_UNIT_ID, sizeList).build();

              // Load an ad.
              loadAd(adRequest);
            }
          }
        });
  }

  private void loadAd(BannerAdRequest adRequest) {
    BannerAd.load(
        adRequest,
        new AdLoadCallback<>() {
          @Override
          public void onAdLoaded(@NonNull BannerAd ad) {
            bannerAd = ad;
            ad.setAdEventCallback(
                new BannerAdEventCallback() {
                  @Override
                  public void onAdImpression() {
                    Log.d(Constant.TAG, "Banner ad recorded an impression.");
                  }

                  @Override
                  public void onAdClicked() {
                    Log.d(Constant.TAG, "Banner ad clicked.");
                  }
                });

            ad.setBannerAdRefreshCallback(
                new BannerAdRefreshCallback() {
                  @Override
                  public void onAdRefreshed() {
                    showToast("Banner ad refreshed.");
                    Log.d(Constant.TAG, "Banner ad refreshed.");
                  }

                  @Override
                  public void onAdFailedToRefresh(@NonNull LoadAdError adError) {
                    showToast("Banner ad failed to refresh with error code: " + adError.getCode());
                    Log.w(Constant.TAG, "Banner ad failed to refresh: " + adError);
                  }
                });

            Activity activity = getActivity();
            if (activity != null) {
              activity.runOnUiThread(
                  () -> {
                    bannerContainer.removeAllViews();
                    bannerContainer.addView(bannerAd.getView(activity));
                  });
            }
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError adError) {
            showToast("Banner ad failed to load with error code: " + adError.getCode());
            Log.w(Constant.TAG, "Banner ad failed to load: " + adError);
          }
        });
  }
}
