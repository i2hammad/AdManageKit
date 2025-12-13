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

package com.example.snippets;

import android.widget.FrameLayout;
import androidx.annotation.MainThread;
import com.google.android.libraries.ads.mobile.sdk.nativead.CustomNativeAd;
import com.google.android.libraries.ads.mobile.sdk.nativead.DisplayOpenMeasurement;

/** Kotlin code snippets for the developer guide. */
public class OpenMeasurementSnippets {

  // [START start_measurement]
  @MainThread
  private void displayCustomNativeAd(
      CustomNativeAd customNativeAd, FrameLayout nativeAdViewContainer) {
    // TODO: Render the custom native ad inside the nativeAdViewContainer.

    // ...

    // Start measuring the ad view.
    DisplayOpenMeasurement displayOpenMeasurement = customNativeAd.getDisplayOpenMeasurement();
    if (displayOpenMeasurement != null) {
      displayOpenMeasurement.setView(nativeAdViewContainer);
      displayOpenMeasurement.start();
    }
  }
  // [END start_measurement]
}
