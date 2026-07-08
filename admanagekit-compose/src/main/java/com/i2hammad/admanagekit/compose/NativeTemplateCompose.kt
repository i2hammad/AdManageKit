package com.i2hammad.admanagekit.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.i2hammad.admanagekit.admob.AdLoadCallback
import com.i2hammad.admanagekit.admob.NativeAdTemplate
import com.i2hammad.admanagekit.admob.NativeTemplateView
import com.i2hammad.admanagekit.config.AdLoadingStrategy

/**
 * A Jetpack Compose wrapper for NativeTemplateView.
 *
 * Supports all 17 template styles with Material 3 theming.
 *
 * @param adUnitId The AdMob ad unit ID
 * @param template The template style to use (default: CARD_MODERN)
 * @param modifier Modifier for styling the ad container
 * @param loadingStrategy Loading strategy (ON_DEMAND or HYBRID). Note: ONLY_CACHE is not supported for native ads.
 * @param onAdLoaded Callback when the ad loads successfully
 * @param onAdFailedToLoad Callback when the ad fails to load
 * @param onAdClicked Callback when the ad is clicked
 * @param onAdImpression Callback when the ad impression is recorded
 * @param onAdOpened Callback when the ad opens an overlay
 * @param onAdClosed Callback when the ad overlay is closed
 * @param onPaidEvent Callback when a paid event occurs (for revenue tracking)
 *
 * @since 2.6.0
 */
@Composable
fun NativeTemplateCompose(
    adUnitId: String,
    template: NativeAdTemplate = NativeAdTemplate.CARD_MODERN,
    modifier: Modifier = Modifier,
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

    // Create callback
    val callback = remember(adUnitId, template) {
        object : AdLoadCallback() {
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
    }

    // Create the NativeTemplateView
    val nativeTemplateView = remember(adUnitId, template) {
        NativeTemplateView(context).apply {
            setTemplate(template)
        }
    }

    // Load the ad
    DisposableEffect(adUnitId, template, loadingStrategy) {
        if (context is androidx.activity.ComponentActivity) {
            if (loadingStrategy != null) {
                nativeTemplateView.loadNativeAd(context, adUnitId, callback, loadingStrategy)
            } else {
                nativeTemplateView.loadNativeAd(context, adUnitId, callback)
            }
        }

        onDispose {
            // Release the displayed NativeAd when the composable leaves composition
            nativeTemplateView.destroy()
        }
    }

    // key() ensures the AndroidView node is recreated when a new view instance
    // is created for new keys, so the new view actually gets attached
    key(adUnitId, template) {
        AndroidView(
            factory = { nativeTemplateView },
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            update = { view ->
                view.visibility = android.view.View.VISIBLE
            }
        )
    }
}

/**
 * Card Modern template - general purpose native ad
 */
@Composable
fun NativeCardModernCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.CARD_MODERN,
        modifier = modifier,
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression
    )
}

/**
 * Material 3 template - Material Design 3 styled native ad
 */
@Composable
fun NativeMaterial3Compose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.MATERIAL3,
        modifier = modifier,
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression
    )
}

/**
 * Minimal template - clean, minimal design
 */
@Composable
fun NativeMinimalCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.MINIMAL,
        modifier = modifier,
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression
    )
}

/**
 * Compact Horizontal template - horizontal layout for lists
 */
@Composable
fun NativeCompactHorizontalCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.COMPACT_HORIZONTAL,
        modifier = modifier,
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression
    )
}

/**
 * List Item template - ideal for RecyclerView/LazyColumn items
 */
@Composable
fun NativeListItemCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.LIST_ITEM,
        modifier = modifier,
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression
    )
}

/**
 * Magazine template - news/blog style native ad
 */
@Composable
fun NativeMagazineCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.MAGAZINE,
        modifier = modifier,
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression
    )
}

/**
 * Featured template - large featured card for hero sections
 */
@Composable
fun NativeFeaturedCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.FEATURED,
        modifier = modifier,
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression
    )
}

/**
 * Video Medium template - optimized for video ads
 */
@Composable
fun NativeVideoMediumCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.VIDEO_MEDIUM,
        modifier = modifier,
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression
    )
}

/**
 * Video Large template - large video-optimized layout
 */
@Composable
fun NativeVideoLargeCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.VIDEO_LARGE,
        modifier = modifier,
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression
    )
}

// ── Flat Design Templates (10 variants) ─────────────────────────────
// Flat, minimal, no gradients/heavy shadows. Use theme attributes
// (?attr/colorSurface, ?attr/colorPrimary, ?attr/colorOutlineVariant)
// so the templates inherit Material 3 theming and dark-mode support.

/** Flat list-row variant — slips into a feed (top/bottom hairlines, icon + headline + AD label + Install pill). */
@Composable
fun NativeFlatInlineRowCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) = NativeTemplateCompose(
    adUnitId, NativeAdTemplate.FLAT_INLINE_ROW, modifier, loadingStrategy,
    onAdLoaded, onAdFailedToLoad, onAdClicked, onAdImpression
)

/** Flat card with rating row + Get pill. */
@Composable
fun NativeFlatCardRatingCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) = NativeTemplateCompose(
    adUnitId, NativeAdTemplate.FLAT_CARD_RATING, modifier, loadingStrategy,
    onAdLoaded, onAdFailedToLoad, onAdClicked, onAdImpression
)

/** Flat card with media on top + app row below. */
@Composable
fun NativeFlatMediaTopCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) = NativeTemplateCompose(
    adUnitId, NativeAdTemplate.FLAT_MEDIA_TOP, modifier, loadingStrategy,
    onAdLoaded, onAdFailedToLoad, onAdClicked, onAdImpression
)

/** Flat minimal text-only ad with left brand bar (no icon). */
@Composable
fun NativeFlatTextMinimalCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) = NativeTemplateCompose(
    adUnitId, NativeAdTemplate.FLAT_TEXT_MINIMAL, modifier, loadingStrategy,
    onAdLoaded, onAdFailedToLoad, onAdClicked, onAdImpression
)

/** Flat compact ad — small icon + headline + small Install pill. */
@Composable
fun NativeFlatCompactPillCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) = NativeTemplateCompose(
    adUnitId, NativeAdTemplate.FLAT_COMPACT_PILL, modifier, loadingStrategy,
    onAdLoaded, onAdFailedToLoad, onAdClicked, onAdImpression
)

/** Flat carousel-style ad with media preview + footer rating row. */
@Composable
fun NativeFlatCarouselCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) = NativeTemplateCompose(
    adUnitId, NativeAdTemplate.FLAT_CAROUSEL, modifier, loadingStrategy,
    onAdLoaded, onAdFailedToLoad, onAdClicked, onAdImpression
)

/** Flat single-line wide banner. */
@Composable
fun NativeFlatBannerCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) = NativeTemplateCompose(
    adUnitId, NativeAdTemplate.FLAT_BANNER, modifier, loadingStrategy,
    onAdLoaded, onAdFailedToLoad, onAdClicked, onAdImpression
)

/** Flat feature-list ad — title + 3 bullet benefits + Install. */
@Composable
fun NativeFlatFeatureListCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) = NativeTemplateCompose(
    adUnitId, NativeAdTemplate.FLAT_FEATURE_LIST, modifier, loadingStrategy,
    onAdLoaded, onAdFailedToLoad, onAdClicked, onAdImpression
)

/** Flat editorial-style sponsored story (large headline + body + outline CTA). */
@Composable
fun NativeFlatSponsoredStoryCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) = NativeTemplateCompose(
    adUnitId, NativeAdTemplate.FLAT_SPONSORED_STORY, modifier, loadingStrategy,
    onAdLoaded, onAdFailedToLoad, onAdClicked, onAdImpression
)

/** Flat slim footer-style ad — sticky-style row at the bottom. */
@Composable
fun NativeFlatFooterSlimCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) = NativeTemplateCompose(
    adUnitId, NativeAdTemplate.FLAT_FOOTER_SLIM, modifier, loadingStrategy,
    onAdLoaded, onAdFailedToLoad, onAdClicked, onAdImpression
)

/**
 * Video Square template - square format for social feeds
 */
@Composable
fun NativeVideoSquareCompose(
    adUnitId: String,
    modifier: Modifier = Modifier,
    loadingStrategy: AdLoadingStrategy? = null,
    onAdLoaded: (() -> Unit)? = null,
    onAdFailedToLoad: ((LoadAdError?) -> Unit)? = null,
    onAdClicked: (() -> Unit)? = null,
    onAdImpression: (() -> Unit)? = null
) {
    NativeTemplateCompose(
        adUnitId = adUnitId,
        template = NativeAdTemplate.VIDEO_SQUARE,
        modifier = modifier,
        loadingStrategy = loadingStrategy,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad,
        onAdClicked = onAdClicked,
        onAdImpression = onAdImpression
    )
}
