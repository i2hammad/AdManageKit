package com.i2hammad.admanagekit.compose

import android.view.View
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.i2hammad.admanagekit.admob.BannerAdView
import com.i2hammad.admanagekit.config.CollapsibleBannerPlacement
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * Computes the height of the anchored adaptive banner that AdMob will return
 * for the given available width, so the composable can reserve the exact
 * space instead of guessing. Falls back to the legacy 50dp banner height when
 * the SDK cannot provide a size (e.g. zero width during the first pass).
 */
private fun adaptiveBannerHeightDp(context: android.content.Context, availableWidthDp: Int): Int {
    if (availableWidthDp <= 0) return 50
    val adSize = AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, availableWidthDp)
    return if (adSize.height > 0) adSize.height else 50
}

/**
 * Invokes [load] once the view has a measured width, so the underlying
 * BannerAdView requests an ad sized to the actual slot rather than the full
 * window. Returns the listener to detach on dispose, or null if the view was
 * already measured and the load ran synchronously.
 */
private fun loadWhenMeasured(view: View, load: () -> Unit): View.OnLayoutChangeListener? {
    if (view.width > 0) {
        load()
        return null
    }
    val listener = object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View, l: Int, t: Int, r: Int, b: Int, ol: Int, ot: Int, orr: Int, ob: Int
        ) {
            if (v.width > 0) {
                v.removeOnLayoutChangeListener(this)
                load()
            }
        }
    }
    view.addOnLayoutChangeListener(listener)
    return listener
}

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

    // Keep the latest callbacks available to long-lived async ad events
    val currentOnAdLoaded by rememberUpdatedState(onAdLoaded)
    val currentOnAdFailedToLoad by rememberUpdatedState(onAdFailedToLoad)
    val currentOnAdClicked by rememberUpdatedState(onAdClicked)
    val currentOnAdImpression by rememberUpdatedState(onAdImpression)
    val currentOnAdOpened by rememberUpdatedState(onAdOpened)
    val currentOnAdClosed by rememberUpdatedState(onAdClosed)
    val currentOnPaidEvent by rememberUpdatedState(onPaidEvent)

    // Remember the BannerAdView to prevent recreation on recomposition
    val bannerAdView = remember(adUnitId) {
        BannerAdView(context)
    }

    // Load once the view is measured so the adaptive ad is sized to the
    // actual slot width (parent padding included), not the full window
    DisposableEffect(adUnitId) {
        var pendingLoad: View.OnLayoutChangeListener? = null
        if (context is androidx.activity.ComponentActivity) {
            val callback = object : AdLoadCallback() {
                override fun onAdLoaded() {
                    currentOnAdLoaded?.invoke()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
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
            pendingLoad = loadWhenMeasured(bannerAdView) {
                bannerAdView.loadBanner(context, adUnitId, callback)
            }
        }
        onDispose {
            pendingLoad?.let { bannerAdView.removeOnLayoutChangeListener(it) }
            // Destroy the underlying AdView and stop auto-refresh when removed
            bannerAdView.destroyAd()
        }
    }

    // Reserve the real anchored-adaptive height for the available width —
    // adaptive banners are 50-90dp tall depending on the device, so a fixed
    // 50dp box would clip them
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidthDp = if (constraints.hasBoundedWidth) {
            maxWidth.value.toInt()
        } else {
            context.resources.configuration.screenWidthDp
        }
        val adHeight = remember(availableWidthDp) {
            adaptiveBannerHeightDp(context, availableWidthDp).dp
        }

        // key() ensures the AndroidView node is recreated when a new view instance
        // is created for a new adUnitId, so the new view actually gets attached
        key(adUnitId) {
            AndroidView(
                factory = { bannerAdView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adHeight),
                update = { view ->
                    // Update the view if needed when recomposed
                    if (view.visibility != android.view.View.VISIBLE) {
                        view.showAd()
                    }
                }
            )
        }
    }
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

    // Keep the latest callbacks available to long-lived async ad events
    val currentOnAdLoaded by rememberUpdatedState(onAdLoaded)
    val currentOnAdFailedToLoad by rememberUpdatedState(onAdFailedToLoad)
    val currentOnAdClicked by rememberUpdatedState(onAdClicked)
    val currentOnAdImpression by rememberUpdatedState(onAdImpression)
    val currentOnAdOpened by rememberUpdatedState(onAdOpened)
    val currentOnAdClosed by rememberUpdatedState(onAdClosed)
    val currentOnPaidEvent by rememberUpdatedState(onPaidEvent)

    val bannerAdView = remember(adUnitId) {
        BannerAdView(context)
    }

    DisposableEffect(adUnitId) {
        if (context is androidx.activity.ComponentActivity) {
            val callback = object : AdLoadCallback() {
                override fun onAdLoaded() {
                    currentOnAdLoaded?.invoke()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
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
            bannerAdView.loadBanner(context, adUnitId, callback)
        }
        onDispose {
            // Destroy the underlying AdView and stop auto-refresh when removed
            bannerAdView.destroyAd()
        }
    }

    // Recreate the AndroidView node when a new view is created for a new adUnitId
    key(adUnitId) {
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
 * @since 3.3.3
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

    // Keep the latest callbacks available to long-lived async ad events
    val currentOnAdLoaded by rememberUpdatedState(onAdLoaded)
    val currentOnAdFailedToLoad by rememberUpdatedState(onAdFailedToLoad)
    val currentOnAdClicked by rememberUpdatedState(onAdClicked)
    val currentOnAdImpression by rememberUpdatedState(onAdImpression)
    val currentOnAdOpened by rememberUpdatedState(onAdOpened)
    val currentOnAdClosed by rememberUpdatedState(onAdClosed)
    val currentOnPaidEvent by rememberUpdatedState(onPaidEvent)

    val bannerAdView = remember(adUnitId, placement) {
        BannerAdView(context)
    }

    DisposableEffect(adUnitId, placement) {
        var pendingLoad: View.OnLayoutChangeListener? = null
        if (context is androidx.activity.ComponentActivity) {
            val callback = object : AdLoadCallback() {
                override fun onAdLoaded() {
                    currentOnAdLoaded?.invoke()
                }

                override fun onFailedToLoad(error: LoadAdError?) {
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
            pendingLoad = loadWhenMeasured(bannerAdView) {
                bannerAdView.loadCollapsibleBanner(
                    context = context,
                    adUnitId = adUnitId,
                    collapsible = true,
                    placement = placement,
                    callback = callback
                )
            }
        }
        onDispose {
            pendingLoad?.let { bannerAdView.removeOnLayoutChangeListener(it) }
            // Destroy the underlying AdView and stop auto-refresh when removed
            bannerAdView.destroyAd()
        }
    }

    // The collapsed state of a collapsible banner is an anchored adaptive
    // banner — reserve its real height so the layout doesn't jump on load
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidthDp = if (constraints.hasBoundedWidth) {
            maxWidth.value.toInt()
        } else {
            context.resources.configuration.screenWidthDp
        }
        val adHeight = remember(availableWidthDp) {
            adaptiveBannerHeightDp(context, availableWidthDp).dp
        }

        // Recreate the AndroidView node when a new view is created for new keys
        key(adUnitId, placement) {
            AndroidView(
                factory = { bannerAdView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(adHeight),
                update = { view ->
                    if (view.visibility != android.view.View.VISIBLE) {
                        view.showAd()
                    }
                }
            )
        }
    }
}