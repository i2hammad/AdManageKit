package com.i2hammad.admanagekit.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.NativeBannerSmall
import com.i2hammad.admanagekit.admob.NativeBannerMedium
import com.i2hammad.admanagekit.admob.NativeLarge
import com.i2hammad.admanagekit.config.AdLoadingStrategy
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.AdValue

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
 * @param useCachedAd Whether to use cached ads if available (deprecated, use loadingStrategy)
 * @param loadingStrategy Optional loading strategy override (ON_DEMAND, ONLY_CACHE, HYBRID)
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
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    val context = LocalContext.current

    // Keep the latest callbacks available to long-lived async ad events
    val currentOnAdLoaded by rememberUpdatedState(onAdLoaded)
    val currentOnAdFailedToLoad by rememberUpdatedState(onAdFailedToLoad)
    val currentOnAdClicked by rememberUpdatedState(onAdClicked)
    val currentOnAdImpression by rememberUpdatedState(onAdImpression)
    val currentOnAdOpened by rememberUpdatedState(onAdOpened)
    val currentOnAdClosed by rememberUpdatedState(onAdClosed)
    val currentOnPaidEvent by rememberUpdatedState(onPaidEvent)

    // Premium users get no ad and no reserved space.
    if (rememberPurchaseStatus()) return

    // Drives this slot's height: the view hiding itself cannot shrink the Compose node.
    val slotState = rememberAdSlotState(adUnitId, size)

    // Create callback once. Keyed on size as well as adUnitId so it always writes to
    // the slot state belonging to the ad currently being requested.
    val callback = remember(adUnitId, size) {
        object : AdLoadCallback() {
            override fun onAdLoaded() {
                slotState.value = AdSlotState.SHOWN
                currentOnAdLoaded?.invoke()
            }

            override fun onFailedToLoad(error: LoadAdError?) {
                slotState.value = AdSlotState.HIDDEN
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
    DisposableEffect(adUnitId, size, useCachedAd, loadingStrategy) {
        // SMALL/MEDIUM require a real Activity; LARGE accepts any Context.
        val activity = context.findComponentActivity()
        if (activity == null && size != NativeAdSize.LARGE) {
            android.util.Log.w(
                "NativeAdCompose",
                "No hosting ComponentActivity for ad unit $adUnitId; skipping load"
            )
            slotState.value = AdSlotState.HIDDEN
            currentOnAdFailedToLoad?.invoke(null)
        }
        when (size) {
            NativeAdSize.SMALL -> {
                if (activity != null) {
                    (nativeAdView as NativeBannerSmall).loadNativeBannerAd(
                        activity = activity,
                        adNativeBanner = adUnitId,
                        adCallBack = callback,
                        loadingStrategy = loadingStrategy
                    )
                }
            }
            NativeAdSize.MEDIUM -> {
                if (activity != null) {
                    (nativeAdView as NativeBannerMedium).loadNativeBannerAd(
                        activity = activity,
                        adNativeBanner = adUnitId,
                        adCallBack = callback,
                        loadingStrategy = loadingStrategy
                    )
                }
            }
            NativeAdSize.LARGE -> {
                (nativeAdView as NativeLarge).loadNativeAds(
                    activity = context,
                    adNativeLarge = adUnitId,
                    callback = callback,
                    loadingStrategy = loadingStrategy
                )
            }
        }

        onDispose {
            // Clean up when composable is disposed - release the displayed NativeAd
            when (val view = nativeAdView) {
                is NativeBannerSmall -> view.destroy()
                is NativeBannerMedium -> view.destroy()
                is NativeLarge -> view.destroy()
            }
        }
    }

    // Minimum height based on ad size. This is a floor, not a fixed height: the
    // ad content is wrap_content and can grow taller (e.g. a 3-line body in the
    // MEDIUM layout). Clamping to a fixed height clipped the CTA button off the
    // bottom, which the native ad validator flags as a policy violation. Using a
    // minimum lets short ads keep the intended size while tall ones expand so the
    // CTA stays fully visible and tappable.
    val adHeight = when (size) {
        NativeAdSize.SMALL -> 80.dp
        NativeAdSize.MEDIUM -> 120.dp
        NativeAdSize.LARGE -> 300.dp
    }

    // key() ensures the AndroidView node is recreated when a new view instance
    // is created for new keys, so the new view actually gets attached
    key(adUnitId, size) {
        AndroidView(
            factory = { nativeAdView },
            modifier = modifier
                .fillMaxWidth()
                // heightIn(min=) is a floor that never collapses, so a failed or
                // premium-blocked slot kept an 80-300dp blank block on screen. Drop
                // the floor entirely once the slot is hidden.
                .then(
                    if (slotState.value.occupiesSpace) Modifier.heightIn(min = adHeight)
                    else Modifier.height(0.dp)
                ),
            update = { view ->
                // Do not force VISIBLE unconditionally - that overrode the view's own
                // hide on failure/premium, which is what left the gap on screen.
                view.visibility =
                    if (slotState.value.occupiesSpace) android.view.View.VISIBLE
                    else android.view.View.GONE
            }
        )
    }
}

/**
 * A small native banner ad composable.
 * Convenience function for NativeAdCompose with SMALL size.
 *
 * @param loadingStrategy Optional loading strategy override (ON_DEMAND, ONLY_CACHE, HYBRID)
 */
@Composable
fun NativeBannerSmallCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
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
        loadingStrategy = loadingStrategy,
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
 *
 * @param loadingStrategy Optional loading strategy override (ON_DEMAND, ONLY_CACHE, HYBRID)
 */
@Composable
fun NativeBannerMediumCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
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
        loadingStrategy = loadingStrategy,
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
 *
 * @param loadingStrategy Optional loading strategy override (ON_DEMAND, ONLY_CACHE, HYBRID)
 */
@Composable
fun NativeLargeCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    useCachedAd: Boolean = true,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
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
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression,
        onAdOpened = onAdOpened,
        onAdClosed = onAdClosed,
        onPaidEvent = onPaidEvent
    )
}