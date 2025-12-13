package com.example.nextgenexample.fullscreennative;

import androidx.lifecycle.ViewModel;
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd;

/** A [ViewModel] subclass that manages the native ad. */
public class NativeAdViewModel extends ViewModel {
  private NativeAd nativeAd;

  public NativeAd getNativeAd() {
    return nativeAd;
  }

  public void setNativeAd(NativeAd nativeAd) {
    this.nativeAd = nativeAd;
  }
}
