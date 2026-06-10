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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
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
    onAdFailedToLoad: ((AdError) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    val context = LocalContext.current
    var nativeAdView by remember { mutableStateOf<NativeAdView?>(null) }
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    // Keep the latest callbacks available to long-lived async ad events
    val currentOnAdLoaded by rememberUpdatedState(onAdLoaded)
    val currentOnAdFailedToLoad by rememberUpdatedState(onAdFailedToLoad)
    val currentOnAdClicked by rememberUpdatedState(onAdClicked)
    val currentOnAdImpression by rememberUpdatedState(onAdImpression)
    val currentOnAdOpened by rememberUpdatedState(onAdOpened)
    val currentOnAdClosed by rememberUpdatedState(onAdClosed)
    val currentOnPaidEvent by rememberUpdatedState(onPaidEvent)

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
                    override fun onAdLoaded(loadedNativeAdView: NativeAdView, loadedNativeAd: NativeAd) {
                        // Destroy the previously held ad before replacing it
                        nativeAd?.destroy()
                        nativeAd = loadedNativeAd
                        nativeAdView = loadedNativeAdView
                        isLoading = false
                        hasError = false
                        currentOnAdLoaded?.invoke(loadedNativeAdView, loadedNativeAd)
                    }

                    override fun onAdFailedToLoad(error: AdError) {
                        isLoading = false
                        hasError = true
                        currentOnAdFailedToLoad?.invoke(error)
                    }

                    override fun onAdClicked() {
                        currentOnAdClicked?.invoke()
                    }

                    override fun onAdImpression() {
                        currentOnAdImpression?.invoke()
                    }

                    override fun onAdOpened() {
                        currentOnAdOpened?.invoke()
                    }

                    override fun onAdClosed() {
                        currentOnAdClosed?.invoke()
                    }

                    override fun onPaidEvent(adValue: AdValue) {
                        currentOnPaidEvent?.invoke(adValue)
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
                // key() ensures the AndroidView node is recreated when a new
                // view replaces the old one, so the new view actually gets attached
                key(nativeAdView) {
                    AndroidView(
                        factory = { nativeAdView!! },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Clean up when the composable is disposed
    DisposableEffect(adUnitId) {
        onDispose {
            // Destroy the displayed NativeAd to release its media/assets
            nativeAd?.destroy()
            nativeAd = null
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
    onAdFailedToLoad: ((AdError) -> Unit)? = null,
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
    onAdFailedToLoad: ((AdError) -> Unit)? = null,
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
    onAdFailedToLoad: ((AdError) -> Unit)? = null,
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