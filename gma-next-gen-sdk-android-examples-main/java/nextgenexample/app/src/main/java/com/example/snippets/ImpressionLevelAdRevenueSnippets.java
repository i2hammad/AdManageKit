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

import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.libraries.ads.mobile.sdk.common.AdSourceResponseInfo;
import com.google.android.libraries.ads.mobile.sdk.common.AdValue;
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd;
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback;

/** Java code snippets for the developer guide. */
class ImpressionLevelAdRevenueSnippets {

  private void setOnAdPaid(RewardedAd ad) {
    // [START set_on_ad_paid]
    ad.setAdEventCallback(
        new RewardedAdEventCallback() {
          @Override
          public void onAdPaid(@NonNull AdValue value) {
            // Send the impression-level ad revenue information to your preferred
            // analytics server directly within this callback.

            // Extract the impression-level ad revenue data.
            long valueMicros = value.getValueMicros();
            String currencyCode = value.getCurrencyCode();
            PrecisionType precisionType = value.getPrecisionType();

            AdSourceResponseInfo loadedAdSourceResponseInfo =
                ad.getResponseInfo().getLoadedAdSourceResponseInfo();
            String adSourceName = loadedAdSourceResponseInfo.getName();
            String adSourceId = loadedAdSourceResponseInfo.getId();
            String adSourceInstanceName = loadedAdSourceResponseInfo.getInstanceName();
            String adSourceInstanceId = loadedAdSourceResponseInfo.getInstanceId();

            Bundle extras = ad.getResponseInfo().getResponseExtras();
            String mediationGroupName = extras.getString("mediation_group_name");
            String mediationABTestName = extras.getString("mediation_ab_test_name");
            String mediationABTestVariant = extras.getString("mediation_ab_test_variant");
          }
        });
    // [END set_on_ad_paid]
  }

  // [START get_ad_source_name]
  private String getUniqueAdSourceName(@NonNull AdSourceResponseInfo loadedAdapterResponseInfo) {
    // [START get_ad_source_name]
    String adSourceName = loadedAdapterResponseInfo.getName();
    if (adSourceName.equals("Custom Event")) {
      if (loadedAdapterResponseInfo
          .getAdapterClassName()
          .equals("com.google.ads.mediation.sample.customevent.SampleCustomEvent")) {
        adSourceName = "Sample Ad Network (Custom Event)";
      }
    }
    return adSourceName;
  }
  // [END get_ad_source_name]
}
