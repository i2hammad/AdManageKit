package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.doOnNextLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesView
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
// Legacy NativeAdOptions is still on the classpath (bundled via the Next-Gen SDK's play-
// services-ads-lite dependency) and is kept ONLY for its ADCHOICES_* Int constants, which
// back this view's public Int-typed adChoicesPlacement API. It is no longer used to build
// ad requests - see adChoicesPlacementForRequest() for the mapping to the next-gen enum.
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdKitAdValue
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AdProviderConfig
import com.i2hammad.admanagekit.core.ad.AdUnitMapping
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdSize
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.config.AdLoadingStrategy
import com.i2hammad.admanagekit.config.NativeMediaAspect
import com.i2hammad.admanagekit.databinding.LayoutNativeTemplatePreviewBinding
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.utils.NativeAdIntegrationManager
import com.i2hammad.admanagekit.waterfall.NativeWaterfall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A unified native ad view that supports multiple template styles.
 *
 * Usage in XML:
 * ```xml
 * <com.i2hammad.admanagekit.admob.NativeTemplateView
 *     android:id="@+id/nativeTemplateView"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:adTemplate="card_modern" />
 * ```
 *
 * Usage in code:
 * ```kotlin
 * // Set template programmatically
 * nativeTemplateView.setTemplate(NativeAdTemplate.CARD_MODERN)
 * // or using string
 * nativeTemplateView.setTemplate("material3")
 *
 * // Load ad
 * nativeTemplateView.loadNativeAd(activity, "ca-app-pub-xxx/yyy")
 *
 * // Supply your own layout instead of a built-in preset. The layout's root must be
 * // (or inflate as) a NativeAdView and reuse the standard asset ids - ad_headline,
 * // ad_body, ad_call_to_action, ad_app_icon, ad_advertiser, ad_media, ad_stars,
 * // ad_choices_view - all optional except ad_headline.
 * nativeTemplateView.setCustomTemplate(R.layout.my_native_ad, R.layout.my_native_ad_shimmer)
 * nativeTemplateView.loadNativeAd(activity, adUnitId)
 * ```
 */
class NativeTemplateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutNativeTemplatePreviewBinding =
        LayoutNativeTemplatePreviewBinding.inflate(LayoutInflater.from(context), this)

    private val TAG = "NativeTemplateView"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var adUnitId: String = ""
    var callback: AdLoadCallback? = null

    private var currentTemplate: NativeAdTemplate = NativeAdTemplate.CARD_MODERN
    private var shimmerInflated = false

    // Custom (non-preset) template support. When customLayoutResId is set, it takes
    // precedence over currentTemplate.layoutResId everywhere a template layout is
    // inflated - see effectiveLayoutResId/effectiveShimmerResId.
    private var customLayoutResId: Int? = null
    private var customShimmerResId: Int? = null
    private var customSizeHint: NativeAdSize? = null

    /** Layout actually inflated for the ad content - a custom override, or the selected preset. */
    private val effectiveLayoutResId: Int
        get() = customLayoutResId ?: currentTemplate.layoutResId

    /** Layout actually inflated for the shimmer placeholder - a custom override, or the selected preset's. */
    private val effectiveShimmerResId: Int
        get() = customShimmerResId ?: currentTemplate.shimmerResId

    // Currently displayed native ad, destroyed when replaced or via destroy()
    private var currentNativeAd: NativeAd? = null

    // Waterfall support
    private var nativeWaterfall: NativeWaterfall? = null
    private var waterfallNativeAdRef: Any? = null
    private val useWaterfall: Boolean
        get() = AdProviderConfig.getNativeChain().isNotEmpty()

    /**
     * AdChoices placement position. Default is TOP_RIGHT.
     * Note: This only applies when the template doesn't have a custom AdChoicesView in XML.
     * If the template has an AdChoicesView defined, the SDK renders into that view instead.
     * Options: ADCHOICES_TOP_LEFT, ADCHOICES_TOP_RIGHT, ADCHOICES_BOTTOM_RIGHT, ADCHOICES_BOTTOM_LEFT
     */
    private var adChoicesPlacement: Int = NativeAdOptions.ADCHOICES_TOP_RIGHT

    /**
     * Whether to use custom AdChoicesView from template XML (true) or SDK auto-placement (false).
     * When true (default): Uses AdChoicesView position defined in template XML
     * When false: Removes custom view and uses SDK's setAdChoicesPlacement()
     */
    private var useCustomAdChoicesView: Boolean = false

    /**
     * Optional per-view override for the media-aspect-ratio hint sent with the ad request.
     * When null (default), the hint is derived from the current template's slot shape via
     * [mediaAspectForTemplate]. Set via [setMediaAspect].
     */
    private var mediaAspectOverride: NativeMediaAspect? = null

    init {
        // Read template from XML attributes
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.NativeTemplateView)
            try {
                val templateIndex = typedArray.getInt(R.styleable.NativeTemplateView_adTemplate, 0)
                currentTemplate = NativeAdTemplate.fromIndex(templateIndex)

                // Custom template layout takes precedence over adTemplate when supplied
                val customLayout = typedArray.getResourceId(R.styleable.NativeTemplateView_customAdLayout, 0)
                if (customLayout != 0) {
                    customLayoutResId = customLayout
                    val customShimmer = typedArray.getResourceId(R.styleable.NativeTemplateView_customAdShimmerLayout, 0)
                    customShimmerResId = customShimmer.takeIf { it != 0 }
                }

                // Read AdChoices placement
                adChoicesPlacement = typedArray.getInt(
                    R.styleable.NativeTemplateView_adChoicesPlacement,
                    NativeAdOptions.ADCHOICES_TOP_RIGHT
                )
            } finally {
                typedArray.recycle()
            }
        }

        // In edit mode (Android Studio preview), show the actual template with placeholder data
        if (isInEditMode) {
            inflatePreviewTemplate()
        } else {
            // Inflate the shimmer placeholder for the current template
            inflateShimmerPlaceholder()
        }
    }

    /**
     * Inflate the actual template layout for XML preview in Android Studio
     */
    private fun inflatePreviewTemplate() {
        try {
            // Hide shimmer container in preview mode
            binding.shimmerContainer.visibility = GONE

            // Inflate the actual template layout
            val templateView = LayoutInflater.from(context)
                .inflate(effectiveLayoutResId, binding.flAdPlaceholder, false)

            binding.flAdPlaceholder.removeAllViews()
            binding.flAdPlaceholder.addView(templateView)
            binding.flAdPlaceholder.visibility = VISIBLE

            // Populate with placeholder text for preview
            populatePreviewData(templateView)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to inflate preview for ${currentTemplate.name}: ${e.message}")
            // Fallback to shimmer if preview fails
            inflateShimmerPlaceholder()
        }
    }

    /**
     * Populate template with placeholder data for XML preview
     */
    private fun populatePreviewData(templateView: View) {
        // Headline
        templateView.findViewById<TextView>(R.id.ad_headline)?.apply {
            text = "Ad Headline Preview"
        }

        // Body
        templateView.findViewById<TextView>(R.id.ad_body)?.apply {
            text = "This is a preview of the native ad body text. The actual ad content will appear here."
        }

        // Advertiser
        templateView.findViewById<TextView>(R.id.ad_advertiser)?.apply {
            text = "Advertiser Name"
        }

        // Call to action
        templateView.findViewById<View>(R.id.ad_call_to_action)?.apply {
            when (this) {
                is Button -> this.text = "Install"
                is TextView -> this.text = "Install"
            }
        }

        // Rating
        templateView.findViewById<RatingBar>(R.id.ad_stars)?.apply {
            rating = 4.5f
        }
    }

    /**
     * Set the template style programmatically using enum
     */
    fun setTemplate(template: NativeAdTemplate) {
        val changed = currentTemplate != template || customLayoutResId != null
        currentTemplate = template
        // Switching to a built-in preset always overrides a previously set custom template
        clearCustomTemplateState()
        if (changed) {
            inflateShimmerPlaceholder()
        }
    }

    /**
     * Set the template style programmatically using string name
     * Supports: "card_modern", "CARD_MODERN", "Card Modern", "material3", etc.
     */
    fun setTemplate(templateName: String) {
        setTemplate(NativeAdTemplate.fromString(templateName))
    }

    /**
     * Get the current template
     */
    fun getTemplate(): NativeAdTemplate = currentTemplate

    /**
     * Supply a fully custom native ad layout instead of one of the built-in [NativeAdTemplate]
     * presets. Takes effect immediately (or on the next [loadNativeAd]/[displayAd] call) and
     * overrides the currently selected [NativeAdTemplate] until [clearCustomTemplate] or
     * [setTemplate] is called.
     *
     * The layout's root must be (or inflate as) a
     * `com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView` and reuse the
     * standard asset ids so the SDK can bind the ad's assets to it:
     * `ad_headline` (required), `ad_body`, `ad_call_to_action`, `ad_app_icon`,
     * `ad_advertiser`, `ad_media` (a `MediaView`), `ad_stars` (a `RatingBar`),
     * `ad_choices_view` (an `AdChoicesView`) - all optional except `ad_headline`.
     *
     * @param layoutResId Layout resource for the actual ad content.
     * @param shimmerResId Optional loading placeholder layout. Falls back to the currently
     *        selected template's shimmer (default `CARD_MODERN`) if omitted or 0.
     * @param sizeHint Used for cache-key/screen-type classification and as the Yandex
     *        waterfall fallback size hint, since a custom layout has no built-in size bucket.
     *        Defaults to [NativeAdSize.MEDIUM].
     */
    fun setCustomTemplate(
        @LayoutRes layoutResId: Int,
        @LayoutRes shimmerResId: Int = 0,
        sizeHint: NativeAdSize = NativeAdSize.MEDIUM
    ) {
        customLayoutResId = layoutResId
        customShimmerResId = shimmerResId.takeIf { it != 0 }
        customSizeHint = sizeHint
        inflateShimmerPlaceholder()
    }

    /**
     * Clear a previously set [setCustomTemplate] override and revert to the currently
     * selected [NativeAdTemplate] preset.
     */
    fun clearCustomTemplate() {
        if (customLayoutResId != null) {
            clearCustomTemplateState()
            inflateShimmerPlaceholder()
        }
    }

    /** True if a custom (non-preset) template set via [setCustomTemplate] is currently active. */
    fun isUsingCustomTemplate(): Boolean = customLayoutResId != null

    private fun clearCustomTemplateState() {
        customLayoutResId = null
        customShimmerResId = null
        customSizeHint = null
    }

    /**
     * Set AdChoices placement position programmatically.
     * Note: This only takes effect when useCustomAdChoicesView is set to false,
     * otherwise the template's XML-defined AdChoicesView position is used.
     *
     * @param placement One of NativeAdOptions.ADCHOICES_TOP_LEFT, ADCHOICES_TOP_RIGHT,
     *                  ADCHOICES_BOTTOM_RIGHT, ADCHOICES_BOTTOM_LEFT
     * @param useSDKPlacement If true, ignores template's AdChoicesView and uses SDK auto-placement
     */
    fun setAdChoicesPlacement(placement: Int, useSDKPlacement: Boolean = false) {
        adChoicesPlacement = placement
        useCustomAdChoicesView = !useSDKPlacement
    }

    /**
     * Get current AdChoices placement
     */
    fun getAdChoicesPlacement(): Int = adChoicesPlacement

    /**
     * Override the media-aspect-ratio *hint* sent with the ad request for this view, ignoring the
     * shape inferred from the current template.
     *
     * Call before loading. This is a preference passed to the ad network, not a filter — see
     * [NativeMediaAspect]. Pass [NativeMediaAspect.UNSPECIFIED] to send no hint, or `null` to
     * revert to the per-template default ([mediaAspectForTemplate]).
     *
     * @param aspect the aspect hint to force, or null to use the template default.
     */
    fun setMediaAspect(aspect: NativeMediaAspect?) {
        mediaAspectOverride = aspect
    }

    /**
     * Get the current media-aspect override, or null when the per-template default is in effect.
     */
    fun getMediaAspect(): NativeMediaAspect? = mediaAspectOverride

    /**
     * Maps this view's public, legacy-NativeAdOptions-Int-based [adChoicesPlacement] to the
     * Next-Gen [AdChoicesPlacement] enum required by [NativeAdRequest.Builder.setAdChoicesPlacement].
     * The legacy ADCHOICES_* Int constants and the enum share the same ordering
     * (TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT), so this is a direct mapping.
     */
    private fun adChoicesPlacementForRequest(): AdChoicesPlacement = when (adChoicesPlacement) {
        NativeAdOptions.ADCHOICES_TOP_LEFT -> AdChoicesPlacement.TOP_LEFT
        NativeAdOptions.ADCHOICES_BOTTOM_RIGHT -> AdChoicesPlacement.BOTTOM_RIGHT
        NativeAdOptions.ADCHOICES_BOTTOM_LEFT -> AdChoicesPlacement.BOTTOM_LEFT
        else -> AdChoicesPlacement.TOP_RIGHT
    }

    /**
     * Configure whether to use custom AdChoicesView from template XML or SDK auto-placement.
     * @param useCustomView true = use template's AdChoicesView position, false = use SDK's setAdChoicesPlacement()
     */
    fun setUseCustomAdChoicesView(useCustomView: Boolean) {
        useCustomAdChoicesView = useCustomView
    }

    private fun inflateShimmerPlaceholder() {
        val shimmerContent = binding.shimmerContent
        shimmerContent.removeAllViews()

        try {
            val shimmerView = LayoutInflater.from(context)
                .inflate(effectiveShimmerResId, shimmerContent, false)
            shimmerContent.addView(shimmerView)
            shimmerInflated = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inflate shimmer for ${currentTemplate.name}: ${e.message}")
            // Fallback to a basic placeholder
            shimmerInflated = false
        }
    }

    // =================== LOADING METHODS ===================

    /**
     * Load native ad using default global strategy from AdManageKitConfig.nativeLoadingStrategy
     */
    fun loadNativeAd(activity: Activity, adUnitId: String) {
        loadAd(activity, adUnitId, false, callback, null)
    }

    /**
     * Load native ad with custom loading strategy override
     */
    fun loadNativeAd(activity: Activity, adUnitId: String, loadingStrategy: AdLoadingStrategy) {
        loadAd(activity, adUnitId, false, callback, loadingStrategy)
    }

    /**
     * Load native ad with callback
     */
    fun loadNativeAd(activity: Activity, adUnitId: String, adCallback: AdLoadCallback) {
        this.callback = adCallback
        loadAd(activity, adUnitId, false, adCallback, null)
    }

    /**
     * Load native ad with callback and custom loading strategy
     */
    fun loadNativeAd(
        activity: Activity,
        adUnitId: String,
        adCallback: AdLoadCallback,
        loadingStrategy: AdLoadingStrategy? = null
    ) {
        this.callback = adCallback
        loadAd(activity, adUnitId, false, adCallback, loadingStrategy)
    }

    private fun loadAd(
        context: Context,
        adUnitId: String,
        useCachedAd: Boolean,
        callback: AdLoadCallback?,
        loadingStrategy: AdLoadingStrategy? = null
    ) {
        this.adUnitId = adUnitId

        if (adUnitId.isEmpty()) {
            Log.w(TAG, "adUnitId is empty, cannot load ad")
            callback?.onFailedToLoad(
                LoadAdError(LoadAdError.ErrorCode.INVALID_REQUEST, "Ad unit ID is empty", null)
            )
            return
        }

        // Reset root visibility in case a previous error hid it
        binding.root.visibility = VISIBLE

        if (useWaterfall) { loadViaWaterfall(context, adUnitId, callback); return }

        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainer
        val purchaseProvider = BillingConfig.getPurchaseProvider()

        if (purchaseProvider.isPurchased()) {
            shimmerFrameLayout.visibility = GONE
            callback?.onFailedToLoad(
                LoadAdError(
                    LoadAdError.ErrorCode.INTERNAL_ERROR,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    null
                )
            )
            return
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        // Get screen type based on template
        val screenType = getScreenTypeForTemplate()

        if (context is Activity) {
            // Must match the key used by NativeAdIntegrationManager.loadNativeAdWithCaching()
            // (Activity_<ScreenType>), otherwise cached ads are stored and retrieved under different keys
            val screenKey = "${context.javaClass.simpleName}_${screenType.name}"

            NativeAdIntegrationManager.loadNativeAdWithCaching(
                activity = context,
                baseAdUnitId = adUnitId,
                screenType = screenType,
                useCachedAd = useCachedAd,
                loadingStrategy = loadingStrategy,
                callback = object : AdLoadCallback() {
                    override fun onAdLoaded() {
                        callback?.onAdLoaded()
                    }

                    override fun onFailedToLoad(error: LoadAdError?) {
                        binding.shimmerContainer.visibility = GONE
                        binding.root.visibility = GONE
                        callback?.onFailedToLoad(error)
                    }

                    override fun onAdClicked() {
                        callback?.onAdClicked()
                    }

                    override fun onAdClosed() {
                        callback?.onAdClosed()
                    }

                    override fun onAdImpression() {
                        callback?.onAdImpression()
                    }

                    override fun onAdOpened() {
                        callback?.onAdOpened()
                    }

                    override fun onPaidEvent(adValue: AdValue) {
                        callback?.onPaidEvent(adValue)
                    }
                }
            ) { enhancedAdUnitId, enhancedCallback ->
                loadNewAdInternal(context, enhancedAdUnitId, enhancedCallback, useCachedAd, loadingStrategy)
            }

            // Check for cached ad from integration manager
            val temporaryCachedAd = NativeAdIntegrationManager.getAndClearTemporaryCachedAd(screenKey)
            if (temporaryCachedAd != null) {
                AdDebugUtils.logEvent(adUnitId, "foundCachedAd",
                    "Found cached ad for template ${currentTemplate.name} - displaying it", true)
                displayAd(temporaryCachedAd)
            }
        } else {
            // Fallback for non-Activity contexts
            loadNewAdInternal(context, adUnitId, callback, useCachedAd)
        }
    }

    private fun getScreenTypeForTemplate(): NativeAdIntegrationManager.ScreenType {
        // Custom templates have no enum entry to classify by - use the hint supplied to
        // setCustomTemplate() (defaults to MEDIUM) instead of falling through to currentTemplate,
        // which for a custom template is just the last/default preset and unrelated to its size.
        customSizeHint?.let {
            return when (it) {
                NativeAdSize.SMALL -> NativeAdIntegrationManager.ScreenType.SMALL
                NativeAdSize.MEDIUM -> NativeAdIntegrationManager.ScreenType.MEDIUM
                NativeAdSize.LARGE -> NativeAdIntegrationManager.ScreenType.LARGE
            }
        }
        return when (currentTemplate) {
            // Small/Compact templates
            NativeAdTemplate.COMPACT_HORIZONTAL,
            NativeAdTemplate.FULL_WIDTH_BANNER,
            NativeAdTemplate.LIST_ITEM,
            NativeAdTemplate.GRID_CARD,
            NativeAdTemplate.GRID_ITEM,
            NativeAdTemplate.MEDIA_CONTENT_SPLIT,
            NativeAdTemplate.PILL_BANNER,
            NativeAdTemplate.VIDEO_SMALL -> NativeAdIntegrationManager.ScreenType.SMALL

            // Medium templates
            NativeAdTemplate.CARD_MODERN,
            NativeAdTemplate.MATERIAL3,
            NativeAdTemplate.MINIMAL,
            NativeAdTemplate.APP_STORE,
            NativeAdTemplate.MEDIUM_HORIZONTAL,
            NativeAdTemplate.TOP_ICON_MEDIA,
            NativeAdTemplate.ICON_LEFT,
            NativeAdTemplate.FLEXIBLE,
            NativeAdTemplate.VIDEO_MEDIUM,
            NativeAdTemplate.VIDEO_SQUARE -> NativeAdIntegrationManager.ScreenType.MEDIUM

            // Large/Hero templates
            NativeAdTemplate.STORY_STYLE,
            NativeAdTemplate.FEATURED,
            NativeAdTemplate.OVERLAY_DARK,
            NativeAdTemplate.MAGAZINE,
            NativeAdTemplate.SOCIAL_FEED,
            NativeAdTemplate.GRADIENT_CARD,
            NativeAdTemplate.SPOTLIGHT,
            NativeAdTemplate.VIDEO_LARGE,
            NativeAdTemplate.VIDEO_VERTICAL,
            NativeAdTemplate.VIDEO_FULLSCREEN -> NativeAdIntegrationManager.ScreenType.LARGE

            // Flat-design small/banner templates
            NativeAdTemplate.FLAT_INLINE_ROW,
            NativeAdTemplate.FLAT_COMPACT_PILL,
            NativeAdTemplate.FLAT_BANNER,
            NativeAdTemplate.FLAT_FOOTER_SLIM -> NativeAdIntegrationManager.ScreenType.SMALL

            // Flat-design medium templates
            NativeAdTemplate.FLAT_CARD_RATING,
            NativeAdTemplate.FLAT_TEXT_MINIMAL,
            NativeAdTemplate.FLAT_FEATURE_LIST -> NativeAdIntegrationManager.ScreenType.MEDIUM

            // Flat-design large/hero templates (have a media area or long content)
            NativeAdTemplate.FLAT_MEDIA_TOP,
            NativeAdTemplate.FLAT_CAROUSEL,
            NativeAdTemplate.FLAT_SPONSORED_STORY -> NativeAdIntegrationManager.ScreenType.LARGE
        }
    }

    /**
     * Preferred media-aspect-ratio hint for the current template, matching the shape of that
     * layout's MediaView slot so served video/image fits without heavy cropping.
     *
     * Templates whose layout has no MediaView (icon + text + CTA only) return
     * [NativeMediaAspect.UNSPECIFIED] — no hint is sent and any video creative simply doesn't
     * render, the ad degrading gracefully. Custom templates fall back to the global default.
     */
    private fun mediaAspectForTemplate(): NativeMediaAspect {
        mediaAspectOverride?.let { return it }
        if (customSizeHint != null) return AdManageKitConfig.defaultNativeMediaAspect
        return when (currentTemplate) {
            // Tall / full-bleed media -> portrait
            NativeAdTemplate.VIDEO_VERTICAL,
            NativeAdTemplate.VIDEO_FULLSCREEN,
            NativeAdTemplate.STORY_STYLE,
            NativeAdTemplate.OVERLAY_DARK -> NativeMediaAspect.PORTRAIT

            // Square media slots
            NativeAdTemplate.VIDEO_SQUARE,
            NativeAdTemplate.GRID_CARD,
            NativeAdTemplate.GRID_ITEM -> NativeMediaAspect.SQUARE

            // Wide media slots -> landscape
            NativeAdTemplate.VIDEO_LARGE,
            NativeAdTemplate.VIDEO_MEDIUM,
            NativeAdTemplate.VIDEO_SMALL,
            NativeAdTemplate.MEDIUM_HORIZONTAL,
            NativeAdTemplate.COMPACT_HORIZONTAL,
            NativeAdTemplate.TOP_ICON_MEDIA,
            NativeAdTemplate.FEATURED,
            NativeAdTemplate.MAGAZINE,
            NativeAdTemplate.SOCIAL_FEED,
            NativeAdTemplate.GRADIENT_CARD,
            NativeAdTemplate.SPOTLIGHT,
            NativeAdTemplate.MEDIA_CONTENT_SPLIT,
            NativeAdTemplate.CARD_MODERN,
            NativeAdTemplate.MATERIAL3,
            NativeAdTemplate.MINIMAL,
            NativeAdTemplate.APP_STORE,
            NativeAdTemplate.ICON_LEFT,
            NativeAdTemplate.FLEXIBLE,
            NativeAdTemplate.LIST_ITEM,
            NativeAdTemplate.FLAT_MEDIA_TOP,
            NativeAdTemplate.FLAT_CAROUSEL -> NativeMediaAspect.LANDSCAPE

            // Small media slot -> let the network decide
            NativeAdTemplate.PILL_BANNER -> NativeMediaAspect.ANY

            // Media-less templates (no MediaView in the layout) -> no hint
            NativeAdTemplate.FULL_WIDTH_BANNER,
            NativeAdTemplate.FLAT_BANNER,
            NativeAdTemplate.FLAT_INLINE_ROW,
            NativeAdTemplate.FLAT_COMPACT_PILL,
            NativeAdTemplate.FLAT_FOOTER_SLIM,
            NativeAdTemplate.FLAT_CARD_RATING,
            NativeAdTemplate.FLAT_TEXT_MINIMAL,
            NativeAdTemplate.FLAT_FEATURE_LIST,
            NativeAdTemplate.FLAT_SPONSORED_STORY -> NativeMediaAspect.UNSPECIFIED
        }
    }

    private fun getNativeAdSizeForTemplate(): NativeAdSize {
        return when (getScreenTypeForTemplate()) {
            NativeAdIntegrationManager.ScreenType.SMALL -> NativeAdSize.SMALL
            NativeAdIntegrationManager.ScreenType.MEDIUM -> NativeAdSize.MEDIUM
            NativeAdIntegrationManager.ScreenType.LARGE -> NativeAdSize.LARGE
            else -> NativeAdSize.LARGE
        }
    }

    private fun loadNewAdInternal(
        context: Context,
        adUnitId: String,
        callback: AdLoadCallback?,
        useCachedAd: Boolean = false,
        loadingStrategy: AdLoadingStrategy? = null
    ) {
        val adPlaceholder: FrameLayout = binding.flAdPlaceholder
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainer

        // Build the request on background thread as recommended by Google
        CoroutineScope(Dispatchers.IO).launch {
            // Configure AdChoices placement directly on the request (NativeAdOptions no
            // longer exists as a separate options object in the Next-Gen SDK).
            val nativeAdRequest = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
                .setAdChoicesPlacement(adChoicesPlacementForRequest())
                .applyMediaConfig(mediaAspectForTemplate())
                .build()

            val nativeAdLoaderCallback = object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    // UI operations on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        trackDisplayedAd(nativeAd)

                        val nativeAdView = LayoutInflater.from(context)
                            .inflate(effectiveLayoutResId, null) as NativeAdView

                        // Setup view references based on template
                        setupNativeAdViewReferences(nativeAdView)

                        adPlaceholder.removeAllViews()
                        adPlaceholder.addView(nativeAdView)
                        binding.root.visibility = VISIBLE
                        adPlaceholder.visibility = VISIBLE

                        // NOTE: Do NOT cache ad here - it's being displayed immediately
                        // Caching is only for preloaded ads that will be shown later
                        // Ads expire after 1 hour, so caching displayed ads wastes memory

                        populateNativeAdView(nativeAd, nativeAdView)
                        shimmerFrameLayout.visibility = GONE

                        // Click/impression/paid reporting is no longer a separate AdListener -
                        // it is delivered through the loaded NativeAd's own adEventCallback.
                        // NOTE: onAdOpened()/onAdClosed() have no Next-Gen native equivalent
                        // and are no longer forwarded (see migration report).
                        nativeAd.adEventCallback = object : NativeAdEventCallback {
                            override fun onAdImpression() {
                                val params = Bundle().apply {
                                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                                }
                                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                                AdDebugUtils.logEvent(adUnitId, "onAdImpression",
                                    "NativeTemplateView (${currentTemplate.name}) impression", true)
                                callback?.onAdImpression()
                            }

                            override fun onAdClicked() {
                                AdDebugUtils.logEvent(adUnitId, "onAdClicked",
                                    "NativeTemplateView (${currentTemplate.name}) clicked", true)
                                callback?.onAdClicked()
                            }

                            override fun onAdPaid(value: AdValue) {
                                val adValueInStandardUnits = value.valueMicros / 1_000_000.0
                                val params = Bundle().apply {
                                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                                    putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                                    putString(FirebaseAnalytics.Param.CURRENCY, value.currencyCode)
                                }
                                firebaseAnalytics?.logEvent("ad_paid_event", params)
                            }
                        }

                        AdDebugUtils.logEvent(adUnitId, "onAdLoaded",
                            "NativeTemplateView (${currentTemplate.name}) loaded successfully", true)
                        callback?.onAdLoaded()
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Try cached ad fallback
                    if (NativeAdManager.enableCachingNativeAds && !useCachedAd) {
                        val cachedAd = NativeAdManager.getCachedNativeAd(adUnitId, enableFallbackToAnyAd = true)
                        if (cachedAd != null) {
                            AdDebugUtils.logEvent(adUnitId, "usedFallbackCache",
                                "Used fallback cached ad for ${currentTemplate.name} after network failure", true)
                            CoroutineScope(Dispatchers.Main).launch {
                                displayAd(cachedAd)
                                callback?.onAdLoaded()
                            }
                            return
                        }
                    }

                    AdDebugUtils.logEvent(adUnitId, "onFailedToLoad",
                        "NativeTemplateView (${currentTemplate.name}) failed: ${adError.message}", false)

                    val params = Bundle().apply {
                        putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                        putString("ad_error_code", adError.code.toString())
                        if (AdManageKitConfig.enablePerformanceMetrics) {
                            putString("error_message", adError.message)
                        }
                    }
                    firebaseAnalytics?.logEvent("ad_failed_to_load", params)

                    // UI ops AND the app callback on the main thread: onFailedToLoad handlers
                    // commonly touch views (hide a spinner/container). NativeAdLoader delivers
                    // this callback on a background thread, so deliver onFailedToLoad here too.
                    CoroutineScope(Dispatchers.Main).launch {
                        adPlaceholder.visibility = GONE
                        shimmerFrameLayout.visibility = GONE
                        callback?.onFailedToLoad(adError)
                    }
                }
            }

            // Load ad on main thread (required by AdMob)
            withContext(Dispatchers.Main) {
                NativeAdLoader.load(nativeAdRequest, nativeAdLoaderCallback)
            }
        }
    }

    private fun setupNativeAdViewReferences(nativeAdView: NativeAdView) {
        // Common views for all templates
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)

        // Optional views that may not exist in all templates.
        // NativeAdView.mediaView has no setter anymore. Its getter is meant to
        // auto-discover a MediaView descendant, but for a NativeAdView freshly
        // inflated with a null root (our case - templates are chosen at runtime, so
        // there's no ViewBinding) it was found to return null, leaving the MediaView
        // blank. Look it up directly by id instead - see populateNativeAdView().
        nativeAdView.starRatingView = nativeAdView.findViewById(R.id.ad_stars)

        // AdChoices handling:
        // - If useCustomAdChoicesView is true: Use the template's AdChoicesView position
        // - If useCustomAdChoicesView is false: Let SDK auto-place via setAdChoicesPlacement()
        val adChoicesView: AdChoicesView? = nativeAdView.findViewById(R.id.ad_choices_view)
        if (useCustomAdChoicesView && adChoicesView != null) {
            // Use custom view from template XML - SDK will render AdChoices into this view
            nativeAdView.adChoicesView = adChoicesView
        } else {
            // Hide custom view if exists, let SDK handle placement via the request's
            // setAdChoicesPlacement() (see adChoicesPlacementForRequest())
            adChoicesView?.visibility = GONE
            // SDK will automatically place AdChoices based on setAdChoicesPlacement()
        }
    }

    private fun populateNativeAdView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
        // Track visibility for container handling
        var hasAdvertiser = false
        var hasStarRating = false

        // Headline (required - always shown)
        nativeAdView.headlineView?.let { view ->
            (view as? TextView)?.text = nativeAd.headline ?: ""
        }

        // Body
        nativeAdView.bodyView?.let { view ->
            val body = nativeAd.body
            if (body != null) {
                (view as? TextView)?.text = body
                view.visibility = VISIBLE
            } else {
                view.visibility = GONE
            }
        }

        // Call to action
        nativeAdView.callToActionView?.let { view ->
            val cta = nativeAd.callToAction
            if (cta != null) {
                when (view) {
                    is Button -> view.text = cta
                    is TextView -> view.text = cta
                }
                view.visibility = VISIBLE
            } else {
                view.visibility = GONE
            }
        }

        // Icon
        nativeAdView.iconView?.let { view ->
            val icon = nativeAd.icon
            if (icon != null) {
                (view as? ImageView)?.setImageDrawable(icon.drawable)
                view.visibility = VISIBLE
            } else {
                view.visibility = GONE
            }
        }

        // Advertiser
        nativeAdView.advertiserView?.let { view ->
            val advertiser = nativeAd.advertiser
            if (advertiser != null) {
                (view as? TextView)?.text = advertiser
                view.visibility = VISIBLE
                hasAdvertiser = true
            } else {
                view.visibility = GONE
            }
        }

        // Media: look the MediaView up directly by id rather than via nativeAdView.mediaView
        // (see setupNativeAdViewReferences() for why). Visibility only - do NOT set
        // mediaView.mediaContent manually; registerNativeAd() below is what actually
        // renders media into the MediaView, and a manual pre-assignment was found to
        // leave it blank instead.
        val mediaView: MediaView? = nativeAdView.findViewById(R.id.ad_media)
        mediaView?.visibility = if (nativeAd.mediaContent != null) VISIBLE else GONE

        // Star rating
        nativeAdView.starRatingView?.let { view ->
            val starRating = nativeAd.starRating
            if (starRating != null && starRating > 0) {
                (view as? RatingBar)?.rating = starRating.toFloat()
                view.visibility = VISIBLE
                hasStarRating = true
            } else {
                view.visibility = GONE
            }
        }

        // Handle advertiser container visibility (for templates that have it)
        // Hide container when both advertiser and star rating are missing
        nativeAdView.findViewById<View>(R.id.advertiser_container)?.let { container ->
            container.visibility = if (hasAdvertiser || hasStarRating) VISIBLE else GONE
        }

        // AdChoices - show the view if found, AdMob will populate it automatically
        nativeAdView.findViewById<View>(R.id.ad_choices_view)?.let { adChoicesView ->
            // AdChoices content is automatically handled by NativeAdView when registerNativeAd
            // is called. We just need to make it visible if the view exists
            adChoicesView.visibility = VISIBLE
        }

        // setNativeAd() no longer exists on NativeAdView - registerNativeAd() both binds
        // the ad's assets/tracking to this view AND renders media into the MediaView, given
        // the same explicitly-looked-up mediaView from above.
        //
        // Deferred to the next layout pass: nativeAdView was just added to adPlaceholder
        // moments ago and hasn't been measured/positioned yet at this point in the call
        // stack. registerNativeAd() (Native Validator's "asset outside native ad view"
        // check runs as part of it) was found to spuriously flag deeply-nested assets
        // (e.g. the advertiser TextView in CARD_MODERN's MaterialCardView subtree) that
        // still have stale pre-layout bounds. doOnNextLayout waits for a real layout pass
        // to complete first.
        nativeAdView.doOnNextLayout {
            nativeAdView.registerNativeAd(nativeAd, mediaView)
        }
    }

    /**
     * Display a preloaded native ad
     */
    fun displayAd(preloadedAd: NativeAd) {
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainer
        val purchaseProvider = BillingConfig.getPurchaseProvider()

        if (purchaseProvider.isPurchased()) {
            shimmerFrameLayout.visibility = GONE
            callback?.onFailedToLoad(
                LoadAdError(
                    LoadAdError.ErrorCode.INTERNAL_ERROR,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    null
                )
            )
            return
        }

        trackDisplayedAd(preloadedAd)

        val nativeAdView = LayoutInflater.from(context)
            .inflate(effectiveLayoutResId, null) as NativeAdView
        val adPlaceholder: FrameLayout = binding.flAdPlaceholder

        setupNativeAdViewReferences(nativeAdView)

        adPlaceholder.removeAllViews()
        adPlaceholder.addView(nativeAdView)
        binding.root.visibility = VISIBLE
        adPlaceholder.visibility = VISIBLE
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        // setOnPaidEventListener() no longer exists - paid reporting moves to adEventCallback.
        // Matches original scope exactly: only onAdPaid is forwarded here (no click/impression
        // forwarding was ever wired for displayAd()-shown ads).
        preloadedAd.adEventCallback = object : NativeAdEventCallback {
            override fun onAdPaid(value: AdValue) {
                val adValueInStandardUnits = value.valueMicros / 1_000_000.0
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                    putString(FirebaseAnalytics.Param.CURRENCY, value.currencyCode)
                }
                firebaseAnalytics?.logEvent("ad_paid_event", params)
            }
        }

        populateNativeAdView(preloadedAd, nativeAdView)
        binding.shimmerContainer.visibility = GONE
    }

    /**
     * Set the ad manager callback
     */
    fun setAdManagerCallback(callback: AdLoadCallback) {
        this.callback = callback
    }

    // =================== WATERFALL METHODS ===================

    private fun resolveAdUnit(logicalName: String): (AdProvider) -> String? = { provider ->
        AdUnitMapping.getAdUnitId(logicalName, provider)
            ?: logicalName.takeIf { provider == AdProvider.ADMOB }
    }

    private fun loadViaWaterfall(context: Context, adUnitId: String, callback: AdLoadCallback?) {
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainer
        val adPlaceholder: FrameLayout = binding.flAdPlaceholder
        val purchaseProvider = BillingConfig.getPurchaseProvider()
        if (purchaseProvider.isPurchased()) {
            shimmerFrameLayout.visibility = GONE
            callback?.onFailedToLoad(
                LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, AdManager.PURCHASED_APP_ERROR_MESSAGE, null)
            )
            return
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        val waterfall = NativeWaterfall(
            providers = AdProviderConfig.getNativeChain(),
            adUnitResolver = resolveAdUnit(adUnitId)
        )
        nativeWaterfall = waterfall

        waterfall.load(context, callback = object : NativeAdProvider.NativeAdCallback {
            override fun onNativeAdLoaded(adView: android.view.View, nativeAdRef: Any) {
                waterfallNativeAdRef = nativeAdRef

                // AdMob returns raw NativeAd — use existing template layout and population logic
                if (nativeAdRef is NativeAd) {
                    displayAd(nativeAdRef)
                } else {
                    // Non-AdMob provider (e.g. Yandex) returns a pre-built view.
                    // Destroy the previously displayed AdMob ad it replaces (if any).
                    currentNativeAd?.destroy()
                    currentNativeAd = null
                    adPlaceholder.removeAllViews()
                    val parent = adView.parent as? android.view.ViewGroup
                    parent?.removeView(adView)
                    adPlaceholder.addView(adView)
                    binding.root.visibility = VISIBLE
                    adPlaceholder.visibility = VISIBLE
                    shimmerFrameLayout.visibility = GONE
                }

                AdDebugUtils.logEvent(adUnitId, "onAdLoaded", "NativeTemplateView (${currentTemplate.name}) waterfall loaded", true)
                callback?.onAdLoaded()
            }

            override fun onNativeAdFailedToLoad(error: AdKitAdError) {
                nativeWaterfall = null
                adPlaceholder.visibility = GONE
                shimmerFrameLayout.visibility = GONE

                AdDebugUtils.logEvent(adUnitId, "onFailedToLoad", "NativeTemplateView (${currentTemplate.name}) waterfall failed: ${error.message}", false)
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putString("ad_error_code", error.code.toString())
                }
                firebaseAnalytics?.logEvent("ad_failed_to_load", params)
                callback?.onFailedToLoad(LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, error.message, null))
            }

            override fun onNativeAdClicked() { callback?.onAdClicked() }
            override fun onNativeAdImpression() {
                val params = Bundle().apply { putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId) }
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                callback?.onAdImpression()
            }
            override fun onPaidEvent(adValue: AdKitAdValue) {
                val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
                val params = Bundle().apply {
                    putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                    putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                    putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                }
                firebaseAnalytics?.logEvent("ad_paid_event", params)
            }
            // Pass the selected template layout so non-AdMob providers (e.g. Yandex) can
            // render the same template instead of a generic size-based view.
        }, sizeHint = getNativeAdSizeForTemplate(), templateLayoutResId = effectiveLayoutResId)
    }

    /**
     * Hide the ad view
     */
    fun hideAd() {
        binding.root.visibility = GONE
    }

    /**
     * Show the ad view
     */
    fun showAd() {
        binding.root.visibility = VISIBLE
    }

    // =================== LIFECYCLE / CLEANUP ===================

    /**
     * Tracks the currently displayed NativeAd, destroying the previously displayed
     * one when it is replaced. Never destroys the ad that was just handed to us
     * (guards against re-displaying the same instance).
     */
    private fun trackDisplayedAd(newAd: NativeAd) {
        val previous = currentNativeAd
        if (previous !== newAd) {
            previous?.destroy()
        }
        currentNativeAd = newAd
    }

    /**
     * Destroys the currently displayed native ad and clears waterfall state.
     *
     * Call this from your screen's teardown (e.g. Activity.onDestroy / Fragment.onDestroyView)
     * when the ad is no longer needed. It is also invoked automatically from
     * [onDetachedFromWindow] when the host Activity is finishing or destroyed.
     * It is intentionally NOT called on plain detach, so the view survives
     * RecyclerView detach/reattach cycles with its ad intact.
     */
    fun destroy() {
        val displayedAd = currentNativeAd
        currentNativeAd = null
        displayedAd?.destroy()

        // Destroy the waterfall AdMob ad if it is distinct from the displayed one.
        // Non-AdMob refs (e.g. Yandex) expose no destroy API; dropping the reference is enough.
        val waterfallRef = waterfallNativeAdRef
        waterfallNativeAdRef = null
        if (waterfallRef is NativeAd && waterfallRef !== displayedAd) {
            waterfallRef.destroy()
        }

        // Do NOT call nativeWaterfall?.destroy() here: its providers come from the
        // global AdProviderConfig chain and are shared across views, so destroying
        // them could tear down ads owned by other views. Dropping the reference is enough.
        nativeWaterfall = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Auto-destroy only when the host Activity is going away. A plain detach
        // (e.g. RecyclerView recycling) must keep the displayed ad alive.
        val activity = findHostActivity()
        if (activity != null && (activity.isFinishing || activity.isDestroyed)) {
            destroy()
        }
    }

    private fun findHostActivity(): Activity? {
        var ctx: Context? = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    companion object {
        /**
         * Get all available template names
         */
        fun getAvailableTemplates(): List<String> {
            return NativeAdTemplate.entries.map { it.name }
        }

        /**
         * Get all video template names
         */
        fun getVideoTemplates(): List<String> {
            return NativeAdTemplate.videoTemplates().map { it.name }
        }

        /**
         * Get all standard (non-video) template names
         */
        fun getStandardTemplates(): List<String> {
            return NativeAdTemplate.standardTemplates().map { it.name }
        }
    }
}
