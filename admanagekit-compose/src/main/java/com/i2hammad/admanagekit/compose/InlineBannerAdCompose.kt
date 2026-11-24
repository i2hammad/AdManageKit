package com.i2hammad.admanagekit.compose

import android.view.View
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.BannerAdView
import com.i2hammad.admanagekit.config.BannerAdSize

/**
 * An inline adaptive banner for scrolling content (lists, feeds, article bodies).
 *
 * The ad is sized to this slot's own width (parent padding included), and its height is
 * variable: AdMob picks it at load time, never above [maxHeight]. Until the ad loads the slot
 * takes no space (or a small shimmer when [showShimmer] is true); it then animates open to the
 * ad's height and follows any height change on refresh. On failure it takes no space.
 *
 * Loads once per slot width: recomposition never reloads. A real width change (rotation,
 * foldable or tablet window resize) loads a new ad for the new width.
 *
 * Built on [BannerAdView] with [BannerAdSize.INLINE_ADAPTIVE], so purchase checks (premium users
 * get no ad, no shimmer and no space), consent, retries, test devices and the waterfall behave as
 * for every other banner. Inline banners are never collapsible.
 *
 * Example:
 * ```kotlin
 * LazyColumn {
 *     items(articles) { ArticleCard(it) }
 *     item {
 *         InlineBannerAdCompose(
 *             adUnitId = "ca-app-pub-xxx/yyy",
 *             modifier = Modifier.padding(horizontal = 16.dp),
 *             maxHeight = 250.dp,
 *         )
 *     }
 * }
 * ```
 *
 * @param adUnitId The AdMob ad unit ID (or a waterfall logical name)
 * @param modifier Modifier for the slot; its width decides the ad width
 * @param maxHeight Largest height the ad may take (default 250dp, at least 32dp)
 * @param showShimmer Show a small shimmer while loading instead of taking no space
 * @param onAdLoaded Called with the ad's height in dp once it is known
 * @param onAdFailedToLoad Called when no ad could be loaded (null when there is no host activity)
 *
 * @since 4.4.14
 */
@Composable
fun InlineBannerAdCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    maxHeight: Dp = BannerAdView.DEFAULT_INLINE_MAX_HEIGHT_DP.dp,
    showShimmer: Boolean = false,
    onAdLoaded: (heightDp: Int) -> Unit = {},
    onAdFailedToLoad: (LoadAdError?) -> Unit = {},
) {
    InlineBannerAdComposeImpl(
        adUnitId = adUnitId,
        modifier = modifier,
        maxHeight = maxHeight,
        showShimmer = showShimmer,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
    )
}

/**
 * Shared implementation of [InlineBannerAdCompose]; also used by [BannerAdCompose] when it is
 * asked for [BannerAdSize.INLINE_ADAPTIVE], which forwards its extra event callbacks here.
 */
@Composable
internal fun InlineBannerAdComposeImpl(
    adUnitId: String,
    modifier: Modifier,
    maxHeight: Dp,
    showShimmer: Boolean,
    onAdLoaded: (heightDp: Int) -> Unit,
    onAdFailedToLoad: (LoadAdError?) -> Unit,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null,
    onAdOpened: (() -> Unit)? = null,
    onAdClosed: (() -> Unit)? = null,
    onPaidEvent: ((AdValue) -> Unit)? = null,
) {
    val context = LocalContext.current
    val currentOnAdLoaded by rememberUpdatedState(onAdLoaded)
    val currentOnAdFailedToLoad by rememberUpdatedState(onAdFailedToLoad)
    val currentOnAdClicked by rememberUpdatedState(onAdClicked)
    val currentOnAdImpression by rememberUpdatedState(onAdImpression)
    val currentOnAdOpened by rememberUpdatedState(onAdOpened)
    val currentOnAdClosed by rememberUpdatedState(onAdClosed)
    val currentOnPaidEvent by rememberUpdatedState(onPaidEvent)

    // Premium users get no ad, no shimmer and no space; no load is issued at all.
    if (rememberPurchaseStatus()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // The slot width, padding included. Unbounded (e.g. a horizontally scrolling row)
        // falls back to the screen width.
        val widthDp = if (constraints.hasBoundedWidth) {
            maxWidth.value.toInt()
        } else {
            context.resources.configuration.screenWidthDp
        }
        val maxHeightDp = maxOf(maxHeight.value.toInt(), BannerAdView.MIN_INLINE_MAX_HEIGHT_DP)
        // Not measured yet: load nothing until there is a real width
        if (widthDp <= 0) return@BoxWithConstraints

        // One ad per (unit, width, max height). Recomposition with the same keys reuses it;
        // a real width change disposes the old banner and loads one for the new width.
        key(adUnitId, widthDp, maxHeightDp) {
            var slotState by remember { mutableStateOf(AdSlotState.LOADING) }
            var adHeightDp by remember { mutableIntStateOf(0) }
            val bannerAdView = remember {
                BannerAdView(context).apply {
                    setBannerAdSize(BannerAdSize.INLINE_ADAPTIVE)
                    setInlineMaxHeight(maxHeightDp)
                    setInlineShimmerEnabled(showShimmer)
                }
            }

            DisposableEffect(Unit) {
                var loadedReported = false
                bannerAdView.onInlineAdHeightChanged = { heightDp ->
                    adHeightDp = heightDp
                    if (!loadedReported) {
                        loadedReported = true
                        currentOnAdLoaded(heightDp)
                    }
                }
                val activity = context.findComponentActivity()
                if (activity != null) {
                    bannerAdView.loadBanner(
                        activity,
                        adUnitId,
                        BannerAdSize.INLINE_ADAPTIVE,
                        object : AdLoadCallback() {
                            override fun onAdLoaded() {
                                slotState = AdSlotState.SHOWN
                            }

                            override fun onFailedToLoad(error: LoadAdError?) {
                                slotState = AdSlotState.HIDDEN
                                currentOnAdFailedToLoad(error)
                            }

                            override fun onAdClicked() { currentOnAdClicked?.invoke() }
                            override fun onAdImpression() { currentOnAdImpression?.invoke() }
                            override fun onAdOpened() { currentOnAdOpened?.invoke() }
                            override fun onAdClosed() { currentOnAdClosed?.invoke() }
                            override fun onPaidEvent(adValue: AdValue) { currentOnPaidEvent?.invoke(adValue) }
                        }
                    )
                } else {
                    android.util.Log.w(
                        "InlineBannerAdCompose",
                        "No hosting ComponentActivity for ad unit $adUnitId; skipping load"
                    )
                    slotState = AdSlotState.HIDDEN
                    currentOnAdFailedToLoad(null)
                }
                onDispose {
                    bannerAdView.onInlineAdHeightChanged = null
                    // Destroys the AdView and stops refreshes for this width
                    bannerAdView.destroyAd()
                }
            }

            val heightModifier = when (slotState) {
                AdSlotState.HIDDEN -> Modifier.height(0.dp)
                AdSlotState.LOADING -> Modifier.height(
                    if (showShimmer) BannerAdView.INLINE_SHIMMER_HEIGHT_DP.dp else 0.dp
                )
                AdSlotState.SHOWN -> if (adHeightDp > 0) {
                    Modifier.height(min(adHeightDp.dp, maxHeight))
                } else {
                    // Height not reported yet (waterfall path): let the view wrap the ad,
                    // which reports its height on the next layout
                    Modifier.wrapContentHeight().heightIn(max = maxHeight)
                }
            }

            AndroidView(
                factory = { bannerAdView },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .then(heightModifier),
                update = { view ->
                    view.setInlineShimmerEnabled(showShimmer)
                    if (slotState == AdSlotState.HIDDEN) {
                        view.visibility = View.GONE
                    } else if (view.visibility != View.VISIBLE && slotState == AdSlotState.SHOWN) {
                        view.visibility = View.VISIBLE
                    }
                }
            )
        }
    }
}
