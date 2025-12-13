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

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import java.util.Arrays;
import java.util.List;

/** Java code snippets for the developer guide. */
final class BannerSnippets {

  private static final String AD_UNIT_ID = "/21775744923/example/api-demo/ad-sizes";

  private void createCustomAdSize() {
    // [START create_custom_ad_size]
    AdSize customAdSize = new AdSize(250, 250);
    BannerAdRequest adRequest = new BannerAdRequest.Builder(AD_UNIT_ID, customAdSize).build();
    // [END create_custom_ad_size]
  }

  private void createMultipleAdSizes() {
    // [START create_multiple_ad_sizes]
    List<AdSize> adSizes =
        Arrays.asList(new AdSize(120, 20), AdSize.BANNER, AdSize.MEDIUM_RECTANGLE);
    BannerAdRequest adRequest = new BannerAdRequest.Builder(AD_UNIT_ID, adSizes).build();
    // [END create_multiple_ad_sizes]
  }
}
