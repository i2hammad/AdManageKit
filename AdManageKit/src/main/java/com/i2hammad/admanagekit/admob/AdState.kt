package com.i2hammad.admanagekit.admob

import com.google.android.gms.ads.nativead.NativeAd

sealed class AdState {
    object Idle : AdState()
    object Loading : AdState()
    data class Ready(val nativeAd: NativeAd) : AdState()
    object Showed : AdState()
    data class Error(val errorMessage: String) : AdState()
}
