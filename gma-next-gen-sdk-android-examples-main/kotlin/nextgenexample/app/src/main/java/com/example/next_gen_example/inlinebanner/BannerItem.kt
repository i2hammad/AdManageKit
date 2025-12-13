package com.example.next_gen_example.inlinebanner

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd

/** A class that represents a banner ad in the list. */
internal class BannerItem {
  // The banner ad to be displayed in the list.
  lateinit var bannerAd: BannerAd

  fun isBannerAdInitialized() = ::bannerAd.isInitialized
}
