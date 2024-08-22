package com.i2hammad.admanagekit.admob

import com.google.android.gms.ads.AdError

abstract class AdLoadCallback {

    open fun onFailedToLoad(error: AdError?) {
        // Default implementation
    }


    open fun onAdLoaded() {
        // Default implementation
    }

    open fun onAdClicked() {

    }

    open fun onAdClosed() {

    }

    open fun onAdImpression() {

    }

   open fun onAdOpened(){
       // Default implementation
   }

}