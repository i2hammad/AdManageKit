package com.i2hammad.admanagekit.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.NativeBannerSmall
import com.i2hammad.admanagekit.admob.NativeBannerMedium
import com.i2hammad.admanagekit.admob.NativeLarge
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdValue

/**
 * Native ad size types for Compose
 */
enum class NativeAdSize {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * A Jetpack Compose wrapper for native ads from AdManageKit.
 *
 * This composable provides an easy way to display native ads in Compose UIs
 * while maintaining all the functionality of the original native ad views.
 *
 * @param adUnitId The AdMob ad unit ID
 * @param size The size of the native ad (SMALL, MEDIUM, LARGE)
 * @param modifier Modifier for styling the ad container
 * @param useCachedAd Whether to use cached ads if available
 * @param onAdLoaded Callback when the ad loads successfully
 * @param onAdFailedToLoad Callback when the ad fails to load
 * @param onAdClicked Callback when the ad is clicked
 * @param onAdImpression Callback when the ad impression is recorded
 * @param onAdOpened Callback when the ad opens an overlay
 * @param onAdClosed Callback when the ad overlay is closed
 * @param onPaidEvent Callback when a paid event occurs (for revenue tracking)
 *
 * @since 2.1.0
 */
@Composable
fun NativeAdCompose(
    adUnitId: String,
    size: NativeAdSize = NativeAdSize.SMALL,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((AdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    val context = LocalContext.current

    // Create callback once
    val callback = remember(adUnitId) {
        object : AdLoadCallback() {
            override fun onAdLoaded() {
                onAdLoaded?.invoke()
            }

            override fun onFailedToLoad(error: AdError?) {
                onAdFailedToLoad?.invoke(error)
            }

            override fun onAdClicked() {
                onAdClicked?.invoke()
            }

            override fun onAdImpression() {
                onAdImpression?.invoke()
            }

            override fun onAdOpened() {
                onAdOpened?.invoke()
            }

            override fun onAdClosed() {
                onAdClosed?.invoke()
            }

            override fun onPaidEvent(adValue: AdValue) {
                onPaidEvent?.invoke(adValue)
            }
        }
    }

    // Create the appropriate native ad view based on size
    val nativeAdView = remember(adUnitId, size) {
        when (size) {
            NativeAdSize.SMALL -> NativeBannerSmall(context)
            NativeAdSize.MEDIUM -> NativeBannerMedium(context)
            NativeAdSize.LARGE -> NativeLarge(context)
        }
    }

    // Load the ad when the composable is first composed
    DisposableEffect(adUnitId, size, useCachedAd) {
        when (size) {
            NativeAdSize.SMALL -> {
                if (context is androidx.activity.ComponentActivity) {
                    (nativeAdView as NativeBannerSmall).loadNativeBannerAd(
                        context, adUnitId, useCachedAd, callback
                    )
                }
            }
            NativeAdSize.MEDIUM -> {
                if (context is androidx.activity.ComponentActivity) {
                    (nativeAdView as NativeBannerMedium).loadNativeBannerAd(
                        context, adUnitId, useCachedAd, callback
                    )
                }
            }
            NativeAdSize.LARGE -> {
                (nativeAdView as NativeLarge).loadNativeAds(
                    context, adUnitId, useCachedAd, callback
                )
            }
        }

        onDispose {
            // Clean up when composable is disposed
            // Note: Native ad views handle their own cleanup in onDetachedFromWindow
        }
    }

    // Determine height based on ad size
    val adHeight = when (size) {
        NativeAdSize.SMALL -> 80.dp
        NativeAdSize.MEDIUM -> 120.dp
        NativeAdSize.LARGE -> 300.dp
    }

    AndroidView(
        factory = { nativeAdView },
        modifier = modifier
            .fillMaxWidth()
            .height(adHeight),
        update = { view ->
            // Update view if needed when recomposed
            view.visibility = android.view.View.VISIBLE
        }
    )
}

/**
 * A small native banner ad composable.
 * Convenience function for NativeAdCompose with SMALL size.
 */
@Composable
fun NativeBannerSmallCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((AdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    NativeAdCompose(
        adUnitId = adUnitId,
        size = NativeAdSize.SMALL,
        modifier = modifier,
        useCachedAd = useCachedAd,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression,
        onAdOpened = onAdOpened,
        onAdClosed = onAdClosed,
        onPaidEvent = onPaidEvent
    )
}

/**
 * A medium native banner ad composable.
 * Convenience function for NativeAdCompose with MEDIUM size.
 */
@Composable
fun NativeBannerMediumCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((AdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    NativeAdCompose(
        adUnitId = adUnitId,
        size = NativeAdSize.MEDIUM,
        modifier = modifier,
        useCachedAd = useCachedAd,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression,
        onAdOpened = onAdOpened,
        onAdClosed = onAdClosed,
        onPaidEvent = onPaidEvent
    )
}

/**
 * A large native ad composable.
 * Convenience function for NativeAdCompose with LARGE size.
 */
@Composable
fun NativeLargeCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((AdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    NativeAdCompose(
        adUnitId = adUnitId,
        size = NativeAdSize.LARGE,
        modifier = modifier,
        useCachedAd = useCachedAd,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression,
        onAdOpened = onAdOpened,
        onAdClosed = onAdClosed,
        onPaidEvent = onPaidEvent
    )
}