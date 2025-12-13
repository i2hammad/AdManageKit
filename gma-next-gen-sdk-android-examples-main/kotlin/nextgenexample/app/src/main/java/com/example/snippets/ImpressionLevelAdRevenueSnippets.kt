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

package com.example.snippets

import com.google.android.libraries.ads.mobile.sdk.common.AdSourceResponseInfo
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback

/** Kotlin code snippets for the developer guide. */
class ImpressionLevelAdRevenueSnippets {

  private fun setOnAdPaid(ad: RewardedAd) {
    // [START set_on_ad_paid]
    ad.adEventCallback =
      object : RewardedAdEventCallback {
        override fun onAdPaid(adValue: AdValue) {
          // Send the impression-level ad revenue information to your
          // preferred analytics server directly within this callback.

          // Extract the impression-level ad revenue data.
          val valueMicros = adValue.valueMicros
          val currencyCode = adValue.currencyCode
          val precisionType = adValue.precisionType

          val loadedAdSourceResponseInfo = ad.getResponseInfo().loadedAdSourceResponseInfo
          val adSourceName = loadedAdSourceResponseInfo?.name
          val adSourceId = loadedAdSourceResponseInfo?.id
          val adSourceInstanceName = loadedAdSourceResponseInfo?.instanceName
          val adSourceInstanceId = loadedAdSourceResponseInfo?.instanceId
          val extras = ad.getResponseInfo().responseExtras
          val mediationGroupName = extras.getString("mediation_group_name")
          val mediationABTestName = extras.getString("mediation_ab_test_name")
          val mediationABTestVariant = extras.getString("mediation_ab_test_variant")
        }
      }
    // [END set_on_ad_paid]
  }

  // [START get_ad_source_name]
  private fun getUniqueAdSourceName(loadedAdapterResponseInfo: AdSourceResponseInfo): String {
    var adSourceName = loadedAdapterResponseInfo.name
    if (adSourceName == "Custom Event") {
      if (
        loadedAdapterResponseInfo.adapterClassName ==
          "com.google.ads.mediation.sample.customevent.SampleCustomEvent"
      ) {
        adSourceName = "Sample Ad Network (Custom Event)"
      }
    }
    return adSourceName
  }
  // [END get_ad_source_name]
}
