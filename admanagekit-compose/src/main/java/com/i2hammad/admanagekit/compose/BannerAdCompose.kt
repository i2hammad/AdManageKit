package com.i2hammad.admanagekit.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.BannerAdView
import com.i2hammad.admanagekit.config.CollapsibleBannerPlacement
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.AdValue

/**
 * A Jetpack Compose wrapper for BannerAdView from AdManageKit.
 *
 * This composable provides an easy way to display banner ads in Compose UIs
 * while maintaining all the functionality of the original BannerAdView.
 *
 * @param adUnitId The AdMob ad unit ID
 * @param modifier Modifier for styling the ad container
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
fun BannerAdCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    val context = LocalContext.current

    // Remember the BannerAdView to prevent recreation on recomposition
    val bannerAdView = remember(adUnitId) {
        BannerAdView(context)
    }

    // Load the ad when the composable is first composed
    DisposableEffect(adUnitId) {
        if (context is androidx.activity.ComponentActivity) {
            val callback = object : AdLoadCallback() {
                override fun onAdLoaded() {
                    onAdLoaded?.invoke()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
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
            bannerAdView.loadBanner(context, adUnitId, callback)
        }
        onDispose {
            // Clean up when the composable is removed
            bannerAdView.hideAd()
        }
    }

    AndroidView(
        factory = { bannerAdView },
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp), // Standard banner height
        update = { view ->
            // Update the view if needed when recomposed
            if (view.visibility != android.view.View.VISIBLE) {
                view.showAd()
            }
        }
    )
}

/**
 * A Jetpack Compose wrapper for BannerAdView with custom dimensions.
 *
 * @param adUnitId The AdMob ad unit ID
 * @param width Width in dp for the banner ad
 * @param height Height in dp for the banner ad
 * @param modifier Modifier for styling the ad container
 * @param onAdLoaded Callback when the ad loads successfully
 * @param onAdFailedToLoad Callback when the ad fails to load
 * @param onAdClicked Callback when the ad is clicked
 * @param onAdImpression Callback when the ad impression is recorded
 * @param onAdOpened Callback when the ad opens an overlay
 * @param onAdClosed Callback when the ad overlay is closed
 * @param onPaidEvent Callback when a paid event occurs (for revenue tracking)
 */
@Composable
fun BannerAdCompose(
    adUnitId: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    val context = LocalContext.current

    val bannerAdView = remember(adUnitId) {
        BannerAdView(context)
    }

    DisposableEffect(adUnitId) {
        if (context is androidx.activity.ComponentActivity) {
            val callback = object : AdLoadCallback() {
                override fun onAdLoaded() {
                    onAdLoaded?.invoke()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
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
            bannerAdView.loadBanner(context, adUnitId, callback)
        }
        onDispose {
            bannerAdView.hideAd()
        }
    }

    AndroidView(
        factory = { bannerAdView },
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        update = { view ->
            if (view.visibility != android.view.View.VISIBLE) {
                view.showAd()
            }
        }
    )
}

/**
 * A Jetpack Compose wrapper for collapsible banner ads.
 *
 * Collapsible banners start in an expanded state and can be collapsed by the user.
 * They provide more ad space initially while allowing users to minimize them.
 *
 * @param adUnitId The AdMob ad unit ID
 * @param placement Direction from which the banner collapses (TOP or BOTTOM)
 * @param modifier Modifier for styling the ad container
 * @param onAdLoaded Callback when the ad loads successfully
 * @param onAdFailedToLoad Callback when the ad fails to load
 * @param onAdClicked Callback when the ad is clicked
 * @param onAdImpression Callback when the ad impression is recorded
 * @param onAdOpened Callback when the ad opens an overlay
 * @param onAdClosed Callback when the ad overlay is closed
 * @param onPaidEvent Callback when a paid event occurs (for revenue tracking)
 *
 * Example usage:
 * ```kotlin
 * // Bottom collapsible banner (most common)
 * CollapsibleBannerAdCompose(
 *     adUnitId = "ca-app-pub-xxx/yyy",
 *     placement = CollapsibleBannerPlacement.BOTTOM,
 *     modifier = Modifier.align(Alignment.BottomCenter)
 * )
 *
 * // Top collapsible banner
 * CollapsibleBannerAdCompose(
 *     adUnitId = "ca-app-pub-xxx/yyy",
 *     placement = CollapsibleBannerPlacement.TOP,
 *     modifier = Modifier.align(Alignment.TopCenter)
 * )
 * ```
 *
 * @since 4.1.1
 */
@Composable
fun CollapsibleBannerAdCompose(
    adUnitId: String,
    placement: CollapsibleBannerPlacement = CollapsibleBannerPlacement.BOTTOM,
    modifier: Modifier = Modifier,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null
) {
    val context = LocalContext.current

    val bannerAdView = remember(adUnitId, placement) {
        BannerAdView(context)
    }

    DisposableEffect(adUnitId, placement) {
        if (context is androidx.activity.ComponentActivity) {
            val callback = object : AdLoadCallback() {
                override fun onAdLoaded() {
                    onAdLoaded?.invoke()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
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
            bannerAdView.loadCollapsibleBanner(
                context = context,
                adUnitId = adUnitId,
                collapsible = true,
                placement = placement,
                callback = callback
            )
        }
        onDispose {
            bannerAdView.hideAd()
        }
    }

    AndroidView(
        factory = { bannerAdView },
        modifier = modifier.fillMaxWidth(),
        update = { view ->
            if (view.visibility != android.view.View.VISIBLE) {
                view.showAd()
            }
        }
    )
}