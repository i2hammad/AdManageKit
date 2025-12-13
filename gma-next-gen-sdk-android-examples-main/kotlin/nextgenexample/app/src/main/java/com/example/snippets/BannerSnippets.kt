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

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest

/** Kotlin code snippets for the developer guide. */
private class BannerSnippets {

  private fun createCustomAdSize() {
    // [START create_custom_ad_size]
    val customAdSize = AdSize(250, 250)
    val adRequest = BannerAdRequest.Builder(AD_UNIT_ID, customAdSize).build()
    // [END create_custom_ad_size]
  }

  private fun createMultipleAdSizes() {
    // [START create_multiple_ad_sizes]
    val adSizes = listOf(AdSize(120, 20), AdSize.BANNER, AdSize.MEDIUM_RECTANGLE)
    val adRequest = BannerAdRequest.Builder(AD_UNIT_ID, adSizes).build()
    // [END create_multiple_ad_sizes]
  }

  private companion object {
    const val AD_UNIT_ID = "/21775744923/example/api-demo/ad-sizes"
  }
}
