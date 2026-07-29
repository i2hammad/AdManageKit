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
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    // Holds a non-AdMob (e.g. Yandex) waterfall view when the native chain falls back.
    var providerAdView by remember { mutableStateOf<android.view.View?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    // Handle for the in-flight load, so it can be cancelled on dispose / reload.
    var loadHandle by remember { mutableStateOf<ProgrammaticNativeAdLoader.NativeAdLoadHandle?>(null) }

    // Keep the latest callbacks available to long-lived async ad events
    val currentOnAdLoaded by rememberUpdatedState(onAdLoaded)
    val currentOnAdFailedToLoad by rememberUpdatedState(onAdFailedToLoad)
    val currentOnAdClicked by rememberUpdatedState(onAdClicked)
    val currentOnAdImpression by rememberUpdatedState(onAdImpression)
    val currentOnAdOpened by rememberUpdatedState(onAdOpened)
    val currentOnAdClosed by rememberUpdatedState(onAdClosed)
    val currentOnPaidEvent by rememberUpdatedState(onPaidEvent)

    // Premium users get no ad and no reserved space, and no load is issued.
    if (rememberPurchaseStatus()) return

    // Load the ad when the composable is first composed
    LaunchedEffect(adUnitId, size, useCachedAd) {
        isLoading = true
        hasError = false
        // Cancel any prior in-flight load before starting a new one.
        loadHandle?.cancel()

        val activity = context.findComponentActivity()
        if (activity != null) {
            loadHandle = ProgrammaticNativeAdLoader.loadNativeAd(
                activity = activity,
                adUnitId = adUnitId,
                size = size,
                useCachedAd = useCachedAd,
                callback = object : ProgrammaticNativeAdLoader.ProgrammaticAdCallback {
                    override fun onAdLoaded(loadedNativeAdView: NativeAdView, loadedNativeAd: NativeAd) {
                        // Destroy the previously held ad before replacing it
                        nativeAd?.destroy()
                        nativeAd = loadedNativeAd
                        nativeAdView = loadedNativeAdView
                        providerAdView = null
                        isLoading = false
                        hasError = false
                        currentOnAdLoaded?.invoke(loadedNativeAdView, loadedNativeAd)
                    }

                    override fun onProviderAdLoaded(adView: android.view.View, nativeAdRef: Any) {
                        // Non-AdMob waterfall fallback (e.g. Yandex): attach the provided view.
                        // It has its own opaque ad ref, so release any held AdMob ad.
                        nativeAd?.destroy()
                        nativeAd = null
                        nativeAdView = null
                        providerAdView = adView
                        isLoading = false
                        hasError = false
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
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
        } else {
            android.util.Log.w(
                "ProgrammaticNativeAdCompose",
                "No hosting ComponentActivity for ad unit $adUnitId; skipping load"
            )
            isLoading = false
            hasError = true
            currentOnAdFailedToLoad?.invoke(
                LoadAdError(
                    LoadAdError.ErrorCode.INTERNAL_ERROR,
                    "No hosting ComponentActivity for this composable",
                    null
                )
            )
        }
    }

    // Determine height based on ad size
    val adHeight = when (size) {
        ProgrammaticNativeAdLoader.NativeAdSize.SMALL -> 80.dp
        ProgrammaticNativeAdLoader.NativeAdSize.MEDIUM -> 120.dp
        ProgrammaticNativeAdLoader.NativeAdSize.LARGE -> 300.dp
    }

    // Once the load has failed there is nothing to show, so the slot must occupy no
    // space at all. Previously the Box kept its fixed 80-300dp height and simply
    // rendered empty content inside it, leaving a blank block on screen.
    // Collapsing the height (rather than returning early) keeps the cleanup
    // DisposableEffect below composed, so a held ad is still released on dispose.
    val hasAd = nativeAdView != null || providerAdView != null
    val slotHeight = if (hasError && !hasAd) 0.dp else adHeight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(slotHeight),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading && showLoadingIndicator -> {
                CircularProgressIndicator()
            }

            nativeAdView != null || providerAdView != null -> {
                val adView = providerAdView ?: nativeAdView!!
                // key() ensures the AndroidView node is recreated when a new
                // view replaces the old one, so the new view actually gets attached
                key(adView) {
                    AndroidView(
                        factory = {
                            // Detach from any temporary parent before Compose attaches it.
                            (adView.parent as? android.view.ViewGroup)?.removeView(adView)
                            adView
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Clean up when the composable is disposed
    DisposableEffect(adUnitId) {
        onDispose {
            // Cancel an in-flight load so a late fill isn't pushed into a dead hierarchy.
            loadHandle?.cancel()
            loadHandle = null
            // Destroy the displayed NativeAd to release its media/assets. Non-AdMob
            // provider views expose no destroy() here — dropping the reference is enough.
            nativeAd?.destroy()
            nativeAd = null
            nativeAdView = null
            providerAdView = null
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