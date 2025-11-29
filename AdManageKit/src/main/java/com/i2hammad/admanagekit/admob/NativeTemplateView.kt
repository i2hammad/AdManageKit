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
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.config.AdLoadingStrategy
import com.i2hammad.admanagekit.databinding.LayoutNativeTemplatePreviewBinding
import com.i2hammad.admanagekit.utils.AdDebugUtils
import com.i2hammad.admanagekit.utils.NativeAdIntegrationManager
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
    private lateinit var adUnitId: String
    var callback: AdLoadCallback? = null

    private var currentTemplate: NativeAdTemplate = NativeAdTemplate.CARD_MODERN
    private var shimmerInflated = false

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

    init {
        // Read template from XML attributes
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.NativeTemplateView)
            try {
                val templateIndex = typedArray.getInt(R.styleable.NativeTemplateView_adTemplate, 0)
                currentTemplate = NativeAdTemplate.fromIndex(templateIndex)

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
                .inflate(currentTemplate.layoutResId, binding.flAdPlaceholder, false)

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
        if (currentTemplate != template) {
            currentTemplate = template
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
                .inflate(currentTemplate.shimmerResId, shimmerContent, false)
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

        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainer
        val purchaseProvider = BillingConfig.getPurchaseProvider()

        if (purchaseProvider.isPurchased()) {
            shimmerFrameLayout.visibility = GONE
            callback?.onFailedToLoad(
                AdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN
                )
            )
            return
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        // Get screen type based on template
        val screenType = getScreenTypeForTemplate()

        if (context is Activity) {
            val screenKey = "${context.javaClass.simpleName}_${currentTemplate.name}"

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

                    override fun onFailedToLoad(error: AdError?) {
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

                    override fun onPaidEvent(adValue: com.google.android.gms.ads.AdValue) {
                        callback?.onPaidEvent(adValue)
                    }
                }
            ) { enhancedAdUnitId, enhancedCallback ->
                loadNewAdInternal(context, enhancedAdUnitId, enhancedCallback, useCachedAd)
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
        return when (currentTemplate) {
            // Small/Compact templates
            NativeAdTemplate.COMPACT_HORIZONTAL,
            NativeAdTemplate.FULL_WIDTH_BANNER,
            NativeAdTemplate.LIST_ITEM,
            NativeAdTemplate.GRID_CARD,
            NativeAdTemplate.MEDIA_CONTENT_SPLIT,
            NativeAdTemplate.PILL_BANNER,
            NativeAdTemplate.VIDEO_SMALL -> NativeAdIntegrationManager.ScreenType.SMALL

            // Medium templates
            NativeAdTemplate.CARD_MODERN,
            NativeAdTemplate.MATERIAL3,
            NativeAdTemplate.MINIMAL,
            NativeAdTemplate.APP_STORE,
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
        }
    }

    private fun loadNewAdInternal(
        context: Context,
        adUnitId: String,
        callback: AdLoadCallback?,
        useCachedAd: Boolean = false
    ) {
        val adPlaceholder: FrameLayout = binding.flAdPlaceholder
        val shimmerFrameLayout: ShimmerFrameLayout = binding.shimmerContainer

        // Build AdLoader on background thread as recommended by Google
        CoroutineScope(Dispatchers.IO).launch {
            // Configure NativeAdOptions with AdChoices placement
            val nativeAdOptions = NativeAdOptions.Builder()
                .setAdChoicesPlacement(adChoicesPlacement)
                .build()

            val builder = AdLoader.Builder(context, adUnitId)
                .withNativeAdOptions(nativeAdOptions)
                .forNativeAd { nativeAd ->
                    // UI operations on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        val nativeAdView = LayoutInflater.from(context)
                            .inflate(currentTemplate.layoutResId, null) as NativeAdView

                        // Setup view references based on template
                        setupNativeAdViewReferences(nativeAdView)

                        adPlaceholder.removeAllViews()
                        adPlaceholder.addView(nativeAdView)
                        binding.root.visibility = VISIBLE
                        adPlaceholder.visibility = VISIBLE

                        // NOTE: Do NOT cache ad here - it's being displayed immediately
                        // Caching is only for preloaded ads that will be shown later
                        // Ads expire after 1 hour, so we should only cache ads we intend to show later

                        populateNativeAdView(nativeAd, nativeAdView)
                        shimmerFrameLayout.visibility = GONE

                        nativeAd.setOnPaidEventListener { adValue ->
                            val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
                            val params = Bundle().apply {
                                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                                putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                                putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
                            }
                            firebaseAnalytics?.logEvent("ad_paid_event", params)
                        }
                    }
                }.withAdListener(object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        AdDebugUtils.logEvent(adUnitId, "onAdLoaded",
                            "NativeTemplateView (${currentTemplate.name}) loaded successfully", true)
                        callback?.onAdLoaded()
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        // Try cached ad fallback
                        if (NativeAdManager.enableCachingNativeAds && !useCachedAd) {
                            val cachedAd = NativeAdManager.getCachedNativeAd(adUnitId, enableFallbackToAnyAd = true)
                            if (cachedAd != null) {
                                AdDebugUtils.logEvent(adUnitId, "usedFallbackCache",
                                    "Used fallback cached ad for ${currentTemplate.name} after network failure", true)
                                displayAd(cachedAd)
                                callback?.onAdLoaded()
                                return
                            }
                        }

                        AdDebugUtils.logEvent(adUnitId, "onFailedToLoad",
                            "NativeTemplateView (${currentTemplate.name}) failed: ${adError.message}", false)

                        // UI operations on main thread
                        CoroutineScope(Dispatchers.Main).launch {
                            adPlaceholder.visibility = GONE
                            shimmerFrameLayout.visibility = GONE
                        }

                        val params = Bundle().apply {
                            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                            putString("ad_error_code", adError.code.toString())
                            if (AdManageKitConfig.enablePerformanceMetrics) {
                                putString("error_message", adError.message)
                            }
                        }
                        firebaseAnalytics?.logEvent("ad_failed_to_load", params)
                        callback?.onFailedToLoad(adError)
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        val params = Bundle().apply {
                            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                        }
                        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params)
                        AdDebugUtils.logEvent(adUnitId, "onAdImpression",
                            "NativeTemplateView (${currentTemplate.name}) impression", true)
                        callback?.onAdImpression()
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        AdDebugUtils.logEvent(adUnitId, "onAdClicked",
                            "NativeTemplateView (${currentTemplate.name}) clicked", true)
                        callback?.onAdClicked()
                    }

                    override fun onAdOpened() {
                        super.onAdOpened()
                        AdDebugUtils.logEvent(adUnitId, "onAdOpened",
                            "NativeTemplateView (${currentTemplate.name}) opened", true)
                        callback?.onAdOpened()
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        AdDebugUtils.logEvent(adUnitId, "onAdClosed",
                            "NativeTemplateView (${currentTemplate.name}) closed", true)
                        callback?.onAdClosed()
                    }
                })

            // Load ad on main thread (required by AdMob)
            withContext(Dispatchers.Main) {
                builder.build().loadAd(AdRequest.Builder().build())
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

        // Optional views that may not exist in all templates
        nativeAdView.mediaView = nativeAdView.findViewById(R.id.ad_media)
        nativeAdView.starRatingView = nativeAdView.findViewById(R.id.ad_stars)

        // AdChoices handling:
        // - If useCustomAdChoicesView is true: Use the template's AdChoicesView position
        // - If useCustomAdChoicesView is false: Let SDK auto-place via setAdChoicesPlacement()
        val adChoicesView: AdChoicesView? = nativeAdView.findViewById(R.id.ad_choices_view)
        if (useCustomAdChoicesView && adChoicesView != null) {
            // Use custom view from template XML - SDK will render AdChoices into this view
            nativeAdView.adChoicesView = adChoicesView
        } else {
            // Hide custom view if exists, let SDK handle placement via NativeAdOptions
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

        // Media
        nativeAdView.mediaView?.let { mediaView ->
            val mediaContent = nativeAd.mediaContent
            if (mediaContent != null) {
                mediaView.mediaContent = mediaContent
                mediaView.visibility = VISIBLE
            } else {
                mediaView.visibility = GONE
            }
        }

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
            // AdChoices content is automatically handled by NativeAdView when setNativeAd is called
            // We just need to make it visible if the view exists
            adChoicesView.visibility = VISIBLE
        }

        nativeAdView.setNativeAd(nativeAd)
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
                AdError(
                    AdManager.PURCHASED_APP_ERROR_CODE,
                    AdManager.PURCHASED_APP_ERROR_MESSAGE,
                    AdManager.PURCHASED_APP_ERROR_DOMAIN
                )
            )
            return
        }

        val nativeAdView = LayoutInflater.from(context)
            .inflate(currentTemplate.layoutResId, null) as NativeAdView
        val adPlaceholder: FrameLayout = binding.flAdPlaceholder

        setupNativeAdViewReferences(nativeAdView)

        adPlaceholder.removeAllViews()
        adPlaceholder.addView(nativeAdView)
        adPlaceholder.visibility = VISIBLE
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)

        preloadedAd.setOnPaidEventListener { adValue ->
            val adValueInStandardUnits = adValue.valueMicros / 1_000_000.0
            val params = Bundle().apply {
                putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
                putDouble(FirebaseAnalytics.Param.VALUE, adValueInStandardUnits)
                putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            }
            firebaseAnalytics?.logEvent("ad_paid_event", params)
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