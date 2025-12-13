package com.i2hammad.admanagekit.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.i2hammad.admanagekit.utils.ProgrammaticNativeAdLoader

/**
 * A Jetpack Compose component for programmatically loaded native ads.
 *
 * This composable uses the ProgrammaticNativeAdLoader to load native ads
 * without requiring predefined views in layouts, providing a more flexible
 * and Compose-friendly approach to native ad integration.
 *
 * @param adUnitId The AdMob ad unit ID
 * @param size The size of the native ad
 * @param modifier Modifier for styling the ad container
 * @param useCachedAd Whether to use cached ads if available
 * @param showLoadingIndicator Whether to show a loading indicator while the ad loads
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
fun ProgrammaticNativeAdCompose(
    adUnitId: String,
    size: ProgrammaticNativeAdLoader.NativeAdSize = ProgrammaticNativeAdLoader.NativeAdSize.SMALL,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    showLoadingIndicator: Boolean = true,
    onAdLoaded: ((NativeAdView, NativeAd) -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    val context = LocalContext.current
    var nativeAdView by remember { mutableStateOf<NativeAdView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Load the ad when the composable is first composed
    LaunchedEffect(adUnitId, size, useCachedAd) {
        isLoading = true
        hasError = false

        if (context is androidx.activity.ComponentActivity) {
            ProgrammaticNativeAdLoader.loadNativeAd(
                activity = context,
                adUnitId = adUnitId,
                size = size,
                useCachedAd = useCachedAd,
                callback = object : ProgrammaticNativeAdLoader.ProgrammaticAdCallback {
                    override fun onAdLoaded(loadedNativeAdView: NativeAdView, nativeAd: NativeAd) {
                        nativeAdView = loadedNativeAdView
                        isLoading = false
                        hasError = false
                        onAdLoaded?.invoke(loadedNativeAdView, nativeAd)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoading = false
                        hasError = true
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
            )
        }
    }

    // Determine height based on ad size
    val adHeight = when (size) {
        ProgrammaticNativeAdLoader.NativeAdSize.SMALL -> 80.dp
        ProgrammaticNativeAdLoader.NativeAdSize.MEDIUM -> 120.dp
        ProgrammaticNativeAdLoader.NativeAdSize.LARGE -> 300.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adHeight),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading && showLoadingIndicator -> {
                CircularProgressIndicator()
            }

            hasError -> {
                // Don't show anything if ad failed to load
                // This maintains the same behavior as the original ad views
            }

            nativeAdView != null -> {
                AndroidView(
                    factory = { nativeAdView!! },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Clean up when the composable is disposed
    DisposableEffect(adUnitId) {
        onDispose {
            nativeAdView = null
        }
    }
}

/**
 * A small programmatic native banner ad composable.
 * Convenience function for ProgrammaticNativeAdCompose with SMALL size.
 */
@Composable
fun ProgrammaticNativeBannerSmallCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    showLoadingIndicator: Boolean = true,
    onAdLoaded: ((NativeAdView, NativeAd) -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    ProgrammaticNativeAdCompose(
        adUnitId = adUnitId,
        size = ProgrammaticNativeAdLoader.NativeAdSize.SMALL,
        modifier = modifier,
        useCachedAd = useCachedAd,
        showLoadingIndicator = showLoadingIndicator,
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
 * A medium programmatic native banner ad composable.
 * Convenience function for ProgrammaticNativeAdCompose with MEDIUM size.
 */
@Composable
fun ProgrammaticNativeBannerMediumCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    showLoadingIndicator: Boolean = true,
    onAdLoaded: ((NativeAdView, NativeAd) -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    ProgrammaticNativeAdCompose(
        adUnitId = adUnitId,
        size = ProgrammaticNativeAdLoader.NativeAdSize.MEDIUM,
        modifier = modifier,
        useCachedAd = useCachedAd,
        showLoadingIndicator = showLoadingIndicator,
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
 * A large programmatic native ad composable.
 * Convenience function for ProgrammaticNativeAdCompose with LARGE size.
 */
@Composable
fun ProgrammaticNativeLargeCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    showLoadingIndicator: Boolean = true,
    onAdLoaded: ((NativeAdView, NativeAd) -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    ProgrammaticNativeAdCompose(
        adUnitId = adUnitId,
        size = ProgrammaticNativeAdLoader.NativeAdSize.LARGE,
        modifier = modifier,
        useCachedAd = useCachedAd,
        showLoadingIndicator = showLoadingIndicator,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression,
        onAdOpened = onAdOpened,
        onAdClosed = onAdClosed,
        onPaidEvent = onPaidEvent
    )
}