package com.i2hammad.admanagekit.admob

import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.config.NativeMediaAspect

/**
 * Maps the library-local [NativeMediaAspect] to the Next-Gen SDK
 * [NativeAd.NativeMediaAspectRatio], or `null` when no hint should be sent
 * ([NativeMediaAspect.UNSPECIFIED]).
 */
internal fun NativeMediaAspect.toSdkAspectRatio(): NativeAd.NativeMediaAspectRatio? = when (this) {
    NativeMediaAspect.UNSPECIFIED -> null
    NativeMediaAspect.ANY -> NativeAd.NativeMediaAspectRatio.ANY
    NativeMediaAspect.LANDSCAPE -> NativeAd.NativeMediaAspectRatio.LANDSCAPE
    NativeMediaAspect.PORTRAIT -> NativeAd.NativeMediaAspectRatio.PORTRAIT
    NativeMediaAspect.SQUARE -> NativeAd.NativeMediaAspectRatio.SQUARE
}

/**
 * Applies AdManageKit's global native media/video preferences to this request builder:
 *
 * - a media-aspect-ratio *hint* (only when [aspect] is not [NativeMediaAspect.UNSPECIFIED]) so the
 *   network prefers media matching the template's MediaView slot shape;
 * - [VideoOptions] (start-muted / click-to-expand / custom-controls) from [AdManageKitConfig],
 *   controlling playback when a video creative is served.
 *
 * Both are preferences, not filters — the network still decides image vs. video. Templates with no
 * MediaView should pass [NativeMediaAspect.UNSPECIFIED]; video simply won't render there, and the
 * ad degrades gracefully to icon + headline + CTA.
 *
 * @param aspect per-request aspect hint; defaults to [AdManageKitConfig.defaultNativeMediaAspect].
 */
internal fun NativeAdRequest.Builder.applyMediaConfig(
    aspect: NativeMediaAspect = AdManageKitConfig.defaultNativeMediaAspect
): NativeAdRequest.Builder {
    aspect.toSdkAspectRatio()?.let { setMediaAspectRatio(it) }
    setVideoOptions(
        VideoOptions.Builder()
            .setStartMuted(AdManageKitConfig.nativeVideoStartMuted)
            .setClickToExpandRequested(AdManageKitConfig.nativeVideoClickToExpand)
            .setCustomControlsRequested(AdManageKitConfig.nativeVideoCustomControls)
            .build()
    )
    return this
}
