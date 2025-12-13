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

package com.example.next_gen_example.banner

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import com.example.next_gen_example.AdFragment
import com.example.next_gen_example.Constant.Companion.TAG
import com.example.next_gen_example.R
import com.example.next_gen_example.databinding.FragmentCustomTargetingBinding
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/** A [Fragment] subclass that loads an ad configured to have custom targeting. */
class AdManagerCustomTargetingFragment : AdFragment<FragmentCustomTargetingBinding>() {
  override val bindingInflater:
    (LayoutInflater, ViewGroup?, Boolean) -> FragmentCustomTargetingBinding
    get() = FragmentCustomTargetingBinding::inflate

  private var bannerAd: BannerAd? = null
  private lateinit var bannerContainer: FrameLayout
  private val sports =
    listOf<String>(
      "Baseball",
      "Basketball",
      "Bobsled",
      "Football",
      "Ice Hockey",
      "Running",
      "Skiing",
      "Snowboarding",
      "Softball",
    )

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    bannerContainer = binding.bannerViewContainer

    binding.sportPicker.adapter =
      ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sports)

    binding.loadAdButton.setOnClickListener {
      loadAd(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(requireContext(), 360))
    }
  }

  private fun loadAd(adSize: AdSize) {
    val customTargetingValue = binding.sportPicker.selectedItem as String

    // [START create_ad_request]
    // Create an ad request with selected custom targeting string.
    val adRequest =
      BannerAdRequest.Builder(AD_UNIT_ID, adSize)
        .putCustomTargeting(CUSTOM_TARGETING_KEY, customTargetingValue)
        .build()
    // [END create_ad_request]

    BannerAd.Companion.load(
      adRequest,
      object : AdLoadCallback<BannerAd> {
        override fun onAdLoaded(ad: BannerAd) {
          ad.adEventCallback =
            object : BannerAdEventCallback {
              override fun onAdImpression() {
                Log.d(TAG, "Banner ad recorded an impression.")
              }

              override fun onAdClicked() {
                Log.d(TAG, "Banner ad recorded a click.")
              }
            }
          ad.bannerAdRefreshCallback =
            object : BannerAdRefreshCallback {
              override fun onAdRefreshed() {
                showToast("Banner ad refreshed.")
                Log.d(TAG, "Banner ad refreshed.")
              }

              override fun onAdFailedToRefresh(adError: LoadAdError) {
                showToast("Banner ad failed to refresh.")
                Log.w(TAG, "Banner ad failed to refresh: $adError")
              }
            }
          bannerAd = ad
          activity?.runOnUiThread {
            bannerContainer.removeAllViews()
            bannerContainer.addView(ad.getView(requireActivity()))
          }
          showToast("Banner ad loaded.")
          Log.d(TAG, "Banner ad loaded.")
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
          showToast("Banner ad failed to load.")
          Log.w(TAG, "Banner ad failed to load: $adError")
        }
      },
    )
  }

  private companion object {
    // Sample ad unit ID.
    const val AD_UNIT_ID = "/21775744923/example/api-demo/custom-targeting"
    const val CUSTOM_TARGETING_KEY = "sportpref"
  }
}
