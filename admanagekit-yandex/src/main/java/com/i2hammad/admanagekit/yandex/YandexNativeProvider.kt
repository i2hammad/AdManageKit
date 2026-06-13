package com.i2hammad.admanagekit.yandex

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.yandex.mobile.ads.common.AdBindingResult
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.nativeads.MediaView
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdEventListener
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdProvider
import com.i2hammad.admanagekit.core.ad.NativeAdSize
import com.i2hammad.admanagekit.yandex.internal.toAdKitError
import com.i2hammad.admanagekit.yandex.internal.toAdKitValue

/**
 * Yandex Ads implementation of [NativeAdProvider].
 *
 * Renders different layouts based on [NativeAdSize]:
 * - [NativeAdSize.LARGE]: Custom layout with media + icon + title + body + CTA
 * - [NativeAdSize.MEDIUM]: Custom layout with icon + title + body + CTA (no media)
 * - [NativeAdSize.SMALL]: Custom compact layout with icon + title + CTA (no body, no media)
 *
 * Colors are resolved from the app's Material Design 3 theme attributes
 * (`colorSurface`, `colorOnSurface`, `colorPrimary`, etc.), falling back to
 * standard Android theme attrs, then hardcoded defaults.
 */
class YandexNativeProvider : NativeAdProvider {

    override val provider: AdProvider = AdProvider.YANDEX

    private var currentNativeAd: NativeAd? = null
    private var nativeAdLoader: NativeAdLoader? = null
    private var isDestroyed = false

    companion object {
        private const val TAG = "YandexNative"
    }

    override fun loadNativeAd(
        context: Context,
        adUnitId: String,
        callback: NativeAdProvider.NativeAdCallback,
        sizeHint: NativeAdSize,
        templateLayoutResId: Int
    ) {
        val listener = object : NativeAdLoadListener {
            override fun onAdLoaded(nativeAd: NativeAd) {
                if (isDestroyed) {
                    Log.d(TAG, "Native ad loaded after destroy, discarding: $adUnitId")
                    return
                }
                // Detach the previous ad's listener before replacing it
                currentNativeAd?.setNativeAdEventListener(null)
                currentNativeAd = nativeAd

                nativeAd.setNativeAdEventListener(object : NativeAdEventListener {
                    override fun onAdClicked() {
                        callback.onNativeAdClicked()
                    }

                    override fun onImpression(impressionData: ImpressionData?) {
                        callback.onNativeAdImpression()
                        callback.onPaidEvent(impressionData.toAdKitValue())
                    }
                })

                try {
                    val adView = createNativeAdView(context, nativeAd, sizeHint, templateLayoutResId)
                    Log.d(TAG, "Native ad loaded ($sizeHint): $adUnitId")
                    callback.onNativeAdLoaded(adView, nativeAd)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create native ad view: ${e.message}", e)
                    nativeAd.setNativeAdEventListener(null)
                    if (currentNativeAd === nativeAd) currentNativeAd = null
                    callback.onNativeAdFailedToLoad(
                        com.i2hammad.admanagekit.core.ad.AdKitAdError(
                            0, "Failed to bind native ad: ${e.message}", "yandex"
                        )
                    )
                }
            }

            override fun onAdFailedToLoad(error: AdRequestError) {
                Log.e(TAG, "Native ad failed to load: ${error.description}")
                callback.onNativeAdFailedToLoad(error.toAdKitError())
            }
        }
        val loader = NativeAdLoader(context)
        nativeAdLoader = loader
        loader.loadAd(AdRequest.Builder(adUnitId).build(), listener)
    }

    override fun destroy() {
        isDestroyed = true
        nativeAdLoader?.cancelLoading()
        nativeAdLoader = null
        currentNativeAd?.setNativeAdEventListener(null)
        currentNativeAd = null
    }

    // ======================== View Creation ========================

    private fun createNativeAdView(
        context: Context,
        nativeAd: NativeAd,
        sizeHint: NativeAdSize,
        templateLayoutResId: Int
    ): View {
        // When the caller selected an AdMob native template, render that exact layout so
        // the Yandex fallback ad matches the AdMob ad. If anything goes wrong (missing
        // layout, binding failure), fall back to the built-in size-based view.
        if (templateLayoutResId != 0) {
            try {
                return createTemplateAdView(context, nativeAd, templateLayoutResId)
            } catch (e: Exception) {
                Log.w(TAG, "Template render failed ($templateLayoutResId), falling back to $sizeHint: ${e.message}")
            }
        }
        return when (sizeHint) {
            NativeAdSize.LARGE -> createLargeAdView(context, nativeAd)
            NativeAdSize.MEDIUM -> createMediumAdView(context, nativeAd)
            NativeAdSize.SMALL -> createSmallAdView(context, nativeAd)
        }
    }

    /**
     * Renders [nativeAd] into the AdMob native template at [templateLayoutResId] so a
     * Yandex fallback ad looks identical to the AdMob ad for the same template.
     *
     * The AdMob template XML is inflated as-is and its standard asset views are bound to
     * the Yandex SDK by id name (`ad_headline`, `ad_body`, `ad_call_to_action`,
     * `ad_app_icon`, `ad_advertiser`, `ad_media`). The template's Google `MediaView`
     * (`ad_media`) is swapped in-place for a Yandex [MediaView] since the SDKs use
     * incompatible media view types. A compact compliance row (feedback + sponsored +
     * warning) is appended because the Yandex SDK requires those views to be present for
     * the ad to bind and render legally — AdMob templates don't include them.
     *
     * @throws IllegalStateException if the template lacks a title view or Yandex binding fails.
     */
    private fun createTemplateAdView(context: Context, nativeAd: NativeAd, templateLayoutResId: Int): View {
        val colors = resolveColors(context)
        val dp4 = dpToPx(context, 4)
        val dp8 = dpToPx(context, 8)

        // Inflate the AdMob template. Its root is a Google NativeAdView (a FrameLayout
        // subclass); we use it purely as a container and never call setNativeAd() on it.
        val templateView = LayoutInflater.from(context).inflate(templateLayoutResId, null)
            ?: throw IllegalStateException("Template layout inflated to null")

        val nativeAdView = NativeAdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(
            templateView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // Resolve the standard AdMob template asset views by id name (this module has no
        // compile-time access to the AdManageKit R class).
        val titleView = findTemplateView<TextView>(context, templateView, "ad_headline")
            ?: throw IllegalStateException("Template has no ad_headline title view")
        val bodyView = findTemplateView<TextView>(context, templateView, "ad_body")
        val ctaView = findTemplateView<TextView>(context, templateView, "ad_call_to_action")
        val iconView = findTemplateView<ImageView>(context, templateView, "ad_app_icon")
        val advertiserView = findTemplateView<TextView>(context, templateView, "ad_advertiser")

        // Swap the Google MediaView for a Yandex MediaView at the same position.
        val mediaView = swapMediaView(context, templateView)

        // Hide the Google AdChoices view if the template declares one (Yandex provides its
        // own feedback control in the compliance row below).
        findTemplateView<View>(context, templateView, "ad_choices_view")?.visibility = View.GONE

        // Mandatory Yandex compliance row appended under the template.
        val complianceRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp4
                marginStart = dp8
                marginEnd = dp8
                bottomMargin = dp4
            }
            gravity = Gravity.CENTER_VERTICAL
        }
        val feedbackView = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 18), dpToPx(context, 18)).apply {
                marginEnd = dp4
            }
        }
        val sponsoredView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(colors.secondary)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp4 }
        }
        val warningView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setTextColor(colors.secondary)
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        complianceRow.addView(feedbackView)
        complianceRow.addView(sponsoredView)
        complianceRow.addView(warningView)
        container.addView(complianceRow)

        nativeAdView.addView(container)

        val binder = NativeAdViewBinder.Builder(nativeAdView)
            .setTitleView(titleView)
            .setFeedbackView(feedbackView)
            .setSponsoredView(sponsoredView)
            .setWarningView(warningView)
            .apply {
                bodyView?.let { setBodyView(it) }
                ctaView?.let { setCallToActionView(it) }
                iconView?.let { setIconView(it) }
                advertiserView?.let { setDomainView(it) }
                mediaView?.let { setMediaView(it) }
            }
            .build()

        if (nativeAd.bindNativeAd(binder) is AdBindingResult.Failure) {
            throw IllegalStateException("Native ad binding failed for template $templateLayoutResId")
        }
        return nativeAdView
    }

    /**
     * Finds a template asset view by resource id name (e.g. "ad_headline") and returns it
     * as [T], or null if absent or of an incompatible type.
     */
    private inline fun <reified T : View> findTemplateView(context: Context, root: View, idName: String): T? {
        val id = context.resources.getIdentifier(idName, "id", context.packageName)
        if (id == 0) return null
        return root.findViewById<View>(id) as? T
    }

    /**
     * Replaces the AdMob `MediaView` (`ad_media`) in [root] with a Yandex [MediaView] at the
     * same parent position and layout params, returning the new view (or null if the
     * template has no media slot).
     */
    private fun swapMediaView(context: Context, root: View): MediaView? {
        val mediaId = context.resources.getIdentifier("ad_media", "id", context.packageName)
        if (mediaId == 0) return null
        val googleMedia = root.findViewById<View>(mediaId) ?: return null
        val parent = googleMedia.parent as? ViewGroup ?: return null
        val index = parent.indexOfChild(googleMedia)
        val lp = googleMedia.layoutParams
        parent.removeViewAt(index)
        val yandexMedia = MediaView(context).apply { layoutParams = lp }
        parent.addView(yandexMedia, index)
        return yandexMedia
    }

    /** LARGE: MediaView + icon + title + body + CTA — full asset set for app-type ads. */
    private fun createLargeAdView(context: Context, nativeAd: NativeAd): View {
        val colors = resolveColors(context)
        val dp4 = dpToPx(context, 4)
        val dp8 = dpToPx(context, 8)
        val dp12 = dpToPx(context, 12)
        val dp16 = dpToPx(context, 16)
        val dp48 = dpToPx(context, 48)

        val nativeAdView = NativeAdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp12, dp12, dp12, dp12)
            background = createCardBackground(context, colors)
            clipToOutline = true
        }

        // Header row: icon + text column (title / domain+favicon / price) + feedback
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(dp48, dp48).apply { marginEnd = dp8 }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colors.title)
            maxLines = 2
        }

        // Favicon + domain on the same row
        val domainRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        val faviconView = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 14), dpToPx(context, 14)).apply {
                marginEnd = dp4
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val domainView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(colors.secondary)
            maxLines = 1
        }

        domainRow.addView(faviconView)
        domainRow.addView(domainView)

        // Price (required for app-type ads)
        val priceView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(colors.secondary)
            maxLines = 1
        }

        textColumn.addView(titleView)
        textColumn.addView(domainRow)
        textColumn.addView(priceView)

        val feedbackView = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 24), dpToPx(context, 24)).apply {
                marginStart = dp8
            }
        }

        headerRow.addView(iconView)
        headerRow.addView(textColumn)
        headerRow.addView(feedbackView)
        container.addView(headerRow)

        // Body text
        val bodyView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(colors.body)
            maxLines = 3
            setPadding(0, dp8, 0, dp8)
        }
        container.addView(bodyView)

        // CTA above media — always visible regardless of how tall the image is
        val ctaView = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp8 }
            background = createCtaBackground(context, colors)
            setTextColor(colors.ctaText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp16, dp12, dp16, dp12)
        }
        container.addView(ctaView)

        // Media view below CTA; starts GONE, SDK makes it visible when content is ready
        val mediaView = MediaView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 200)
            ).apply {
                topMargin = dp8
            }
            visibility = View.GONE
        }
        container.addView(mediaView)

        // Bottom row: warning + sponsored
        val bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp4 }
            gravity = Gravity.CENTER_VERTICAL
        }

        val warningView = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(colors.secondary)
            maxLines = 2
        }

        val sponsoredView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(colors.secondary)
        }

        bottomRow.addView(warningView)
        bottomRow.addView(sponsoredView)
        container.addView(bottomRow)

        nativeAdView.addView(container)

        val binder = NativeAdViewBinder.Builder(nativeAdView)
            .setTitleView(titleView)
            .setBodyView(bodyView)
            .setCallToActionView(ctaView)
            .setIconView(iconView)
            .setFaviconView(faviconView)
            .setDomainView(domainView)
            .setPriceView(priceView)
            .setFeedbackView(feedbackView)
            .setWarningView(warningView)
            .setSponsoredView(sponsoredView)
            .setMediaView(mediaView)
            .build()

        if (nativeAd.bindNativeAd(binder) is AdBindingResult.Failure) {
            throw IllegalStateException("Native ad binding failed for LARGE")
        }
        return nativeAdView
    }

    /** MEDIUM: Icon + title + body + CTA. No media. */
    private fun createMediumAdView(context: Context, nativeAd: NativeAd): View {
        val colors = resolveColors(context)
        val dp4 = dpToPx(context, 4)
        val dp8 = dpToPx(context, 8)
        val dp12 = dpToPx(context, 12)
        val dp48 = dpToPx(context, 48)

        val nativeAdView = NativeAdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp12, dp12, dp12, dp12)
            background = createCardBackground(context, colors)
            clipToOutline = true
        }

        // Header row: icon + title/domain
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val iconView = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(dp48, dp48).apply { marginEnd = dp8 }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colors.title)
            maxLines = 2
        }

        val domainView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(colors.secondary)
            maxLines = 1
        }

        textColumn.addView(titleView)
        textColumn.addView(domainView)
        headerRow.addView(iconView)
        headerRow.addView(textColumn)
        container.addView(headerRow)

        // Body text
        val bodyView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(colors.body)
            maxLines = 3
            setPadding(0, dp8, 0, dp8)
        }
        container.addView(bodyView)

        // CTA button
        val ctaView = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp8 }
            background = createCtaBackground(context, colors)
            setTextColor(colors.ctaText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dpToPx(context, 16), dp12, dpToPx(context, 16), dp12)
        }
        container.addView(ctaView)

        // Bottom row: feedback + warning + sponsored (required by Yandex SDK)
        val bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp4 }
            gravity = Gravity.CENTER_VERTICAL
        }

        val feedbackView = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 24), dpToPx(context, 24)).apply {
                marginEnd = dp4
            }
        }

        val warningView = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(colors.secondary)
            maxLines = 2
        }

        val sponsoredView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(colors.secondary)
        }

        bottomRow.addView(feedbackView)
        bottomRow.addView(warningView)
        bottomRow.addView(sponsoredView)
        container.addView(bottomRow)

        nativeAdView.addView(container)

        val binder = NativeAdViewBinder.Builder(nativeAdView)
            .setTitleView(titleView)
            .setBodyView(bodyView)
            .setCallToActionView(ctaView)
            .setIconView(iconView)
            .setDomainView(domainView)
            .setFeedbackView(feedbackView)
            .setWarningView(warningView)
            .setSponsoredView(sponsoredView)
            .build()

        if (nativeAd.bindNativeAd(binder) is AdBindingResult.Failure) {
            throw IllegalStateException("Native ad binding failed for MEDIUM")
        }
        return nativeAdView
    }

    /** SMALL: Icon + title + CTA in a compact row. No body, no media. */
    private fun createSmallAdView(context: Context, nativeAd: NativeAd): View {
        val colors = resolveColors(context)
        val dp4 = dpToPx(context, 4)
        val dp8 = dpToPx(context, 8)
        val dp40 = dpToPx(context, 40)

        val nativeAdView = NativeAdView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp8, dp8, dp8, dp8)
            background = createCardBackground(context, colors)
            clipToOutline = true
        }

        // Main row: icon + title + CTA
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(dp40, dp40).apply { marginEnd = dp8 }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val titleColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(colors.title)
            maxLines = 1
        }

        val domainView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(colors.secondary)
            maxLines = 1
        }

        titleColumn.addView(titleView)
        titleColumn.addView(domainView)

        val ctaView = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp8 }
            background = createCtaBackground(context, colors)
            setTextColor(colors.ctaText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dpToPx(context, 12), dpToPx(context, 6), dpToPx(context, 12), dpToPx(context, 6))
        }

        row.addView(iconView)
        row.addView(titleColumn)
        row.addView(ctaView)
        container.addView(row)

        // Bottom: feedback + warning + sponsored (required by Yandex SDK, kept small)
        val bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp4 }
            gravity = Gravity.CENTER_VERTICAL
        }

        val feedbackView = ImageView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 20), dpToPx(context, 20)).apply {
                marginEnd = dp4
            }
        }

        val warningView = TextView(context).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setTextColor(colors.secondary)
            maxLines = 1
        }

        val sponsoredView = TextView(context).apply {
            id = View.generateViewId()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setTextColor(colors.secondary)
        }

        bottomRow.addView(feedbackView)
        bottomRow.addView(warningView)
        bottomRow.addView(sponsoredView)
        container.addView(bottomRow)

        nativeAdView.addView(container)

        val binder = NativeAdViewBinder.Builder(nativeAdView)
            .setTitleView(titleView)
            .setCallToActionView(ctaView)
            .setIconView(iconView)
            .setDomainView(domainView)
            .setFeedbackView(feedbackView)
            .setWarningView(warningView)
            .setSponsoredView(sponsoredView)
            .build()

        if (nativeAd.bindNativeAd(binder) is AdBindingResult.Failure) {
            throw IllegalStateException("Native ad binding failed for SMALL")
        }
        return nativeAdView
    }

    // ======================== Theming ========================

    /**
     * Holds resolved theme colors for Yandex native ad views.
     *
     * Colors are resolved from Material Design 3 attrs (e.g., `colorSurface`),
     * falling back to standard Android attrs, then hardcoded defaults.
     * This ensures Yandex ads match the app's theme, just like AdMob templates do.
     */
    private data class NativeAdColors(
        val background: Int,
        val title: Int,
        val body: Int,
        val secondary: Int,
        val ctaBackground: Int,
        val ctaText: Int,
        val border: Int
    )

    private fun resolveColors(context: Context): NativeAdColors {
        return NativeAdColors(
            background = resolveColorByName(context, "colorSurface", android.R.attr.colorBackground, Color.WHITE),
            title = resolveColorByName(context, "colorOnSurface", android.R.attr.textColorPrimary, Color.BLACK),
            body = resolveColorByName(context, "colorOnSurfaceVariant", android.R.attr.textColorSecondary, Color.DKGRAY),
            secondary = resolveColorByName(context, "colorOnSurfaceVariant", android.R.attr.textColorSecondary, Color.GRAY),
            ctaBackground = resolveColorByName(context, "colorPrimary", android.R.attr.colorAccent, DEFAULT_CTA_COLOR),
            ctaText = resolveColorByName(context, "colorOnPrimary", 0, Color.WHITE),
            border = resolveColorByName(context, "colorOutlineVariant", 0, DEFAULT_BORDER_COLOR)
        )
    }

    /**
     * Resolves a color from the theme, trying Material attr name first,
     * then a standard Android attr, then a hardcoded default.
     */
    private fun resolveColorByName(context: Context, materialAttrName: String, fallbackAttr: Int, defaultColor: Int): Int {
        // Try Material Design attr by name (resolved from app's merged resources)
        val materialId = context.resources.getIdentifier(materialAttrName, "attr", context.packageName)
        if (materialId != 0) {
            val color = resolveColorAttr(context, materialId)
            if (color != null) return color
        }
        // Fallback to standard Android attr
        if (fallbackAttr != 0) {
            val color = resolveColorAttr(context, fallbackAttr)
            if (color != null) return color
        }
        return defaultColor
    }

    private fun resolveColorAttr(context: Context, attrId: Int): Int? {
        val a = context.obtainStyledAttributes(intArrayOf(attrId))
        try {
            // getColor returns defaultColor if the attr is not found in the theme
            val color = a.getColor(0, Int.MIN_VALUE)
            return if (color != Int.MIN_VALUE) color else null
        } finally {
            a.recycle()
        }
    }

    // ======================== Drawables ========================

    private fun createCardBackground(context: Context, colors: NativeAdColors): GradientDrawable {
        return GradientDrawable().apply {
            setColor(colors.background)
            cornerRadius = dpToPx(context, 12).toFloat()
            setStroke(dpToPx(context, 1), colors.border)
        }
    }

    private fun createCtaBackground(context: Context, colors: NativeAdColors): GradientDrawable {
        return GradientDrawable().apply {
            setColor(colors.ctaBackground)
            cornerRadius = dpToPx(context, 20).toFloat()
        }
    }

    // ======================== Utilities ========================

    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}

private val DEFAULT_CTA_COLOR = Color.parseColor("#1976D2")
private val DEFAULT_BORDER_COLOR = Color.parseColor("#C4C4C4")
