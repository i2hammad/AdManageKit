package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.os.Looper
import android.view.View
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.R
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.config.BannerAdSize
import com.i2hammad.admanagekit.config.CollapsibleBannerPlacement
import com.i2hammad.admanagekit.core.AppPurchaseProvider
import com.i2hammad.admanagekit.core.BillingConfig
import com.i2hammad.admanagekit.core.NoPurchaseProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [BannerAdSize.INLINE_ADAPTIVE] in [BannerAdView]: the load waits for the view's own width,
 * the placeholder reserves no height, and premium users fail fast without ever seeing a shimmer.
 *
 * MobileAds is never initialized under Robolectric, so a load that gets past the layout wait
 * ends in the init gate's TIMEOUT - which is how these tests observe that a load was issued.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BannerAdViewInlineTest {

    private lateinit var activity: Activity
    private val adUnit = "ca-app-pub-3940256099942544/9214589741"

    @Before
    fun setUp() {
        mockkStatic(FirebaseAnalytics::class)
        every { FirebaseAnalytics.getInstance(any()) } returns mockk(relaxed = true)
        MobileAdsGate.readinessCheck = { false }
        AdManageKitConfig.mobileAdsInitTimeout = 2.seconds
        AdManageKitConfig.autoRetryFailedAds = false
        BillingConfig.setPurchaseProvider(NoPurchaseProvider())
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @After
    fun tearDown() {
        MobileAdsGate.readinessCheck = { com.google.android.libraries.ads.mobile.sdk.MobileAds.isInitialized }
        AdManageKitConfig.resetToDefaults()
        BillingConfig.setPurchaseProvider(NoPurchaseProvider())
        unmockkAll()
    }

    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun BannerAdView.layOut(widthPx: Int) {
        measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        layout(0, 0, widthPx, measuredHeight)
    }

    private fun BannerAdView.shimmerHeight(): Int =
        findViewById<View>(R.id.shimmer_frame_layout).layoutParams.height

    @Test
    fun `inline load waits until the view is laid out, then loads`() {
        val view = BannerAdView(activity)
        val failures = mutableListOf<LoadAdError?>()
        view.loadBanner(activity, adUnit, BannerAdSize.INLINE_ADAPTIVE, object : AdLoadCallback() {
            override fun onFailedToLoad(error: AdKitError?) { failures += error }
        })

        idle(5_000)
        assertEquals("no load (and so no init-gate timeout) before layout", 0, failures.size)
        assertEquals("inline placeholder reserves no height", 0, view.shimmerHeight())

        view.layOut(1080)
        idle(3_000)
        assertEquals(1, failures.size)
        assertEquals(LoadAdError.ErrorCode.TIMEOUT, failures.single()?.code)
    }

    @Test
    fun `premium user fails fast on an unlaid inline view and never gets a shimmer`() {
        BillingConfig.setPurchaseProvider(object : AppPurchaseProvider { override fun isPurchased() = true })
        MobileAdsGate.readinessCheck = { true }
        val view = BannerAdView(activity)
        view.setInlineShimmerEnabled(true)
        var failure: LoadAdError? = null
        var failures = 0
        view.loadBanner(activity, adUnit, BannerAdSize.INLINE_ADAPTIVE, object : AdLoadCallback() {
            override fun onFailedToLoad(error: AdKitError?) { failures++; failure = error }
        })
        idle(100)

        assertEquals(1, failures)
        assertEquals(AdManager.PURCHASED_APP_ERROR_MESSAGE, failure?.message)
        assertEquals(View.GONE, view.visibility)
        assertEquals("no shimmer for premium users", 0, view.shimmerHeight())
    }

    @Test
    fun `optional inline shimmer reserves a small height`() {
        val view = BannerAdView(activity)
        view.setBannerAdSize(BannerAdSize.INLINE_ADAPTIVE)
        view.setInlineShimmerEnabled(true)
        val expected = (BannerAdView.INLINE_SHIMMER_HEIGHT_DP * activity.resources.displayMetrics.density).toInt()
        assertEquals(expected, view.shimmerHeight())
    }

    @Test
    fun `collapsible plus inline loads a plain inline banner without crashing`() {
        val view = BannerAdView(activity)
        var failure: LoadAdError? = null
        view.loadCollapsibleBanner(
            activity, adUnit, true, CollapsibleBannerPlacement.BOTTOM,
            object : AdLoadCallback() {
                override fun onFailedToLoad(error: AdKitError?) { failure = error }
            },
            BannerAdSize.INLINE_ADAPTIVE
        )
        idle(1_000)
        assertNull("deferred until layout", failure)
        view.layOut(1080)
        idle(3_000)
        assertEquals(LoadAdError.ErrorCode.TIMEOUT, failure?.code)
    }

    @Test
    fun `destroy while waiting for layout drops the inline load`() {
        val view = BannerAdView(activity)
        var calls = 0
        view.loadBanner(activity, adUnit, BannerAdSize.INLINE_ADAPTIVE, object : AdLoadCallback() {
            override fun onFailedToLoad(error: AdKitError?) { calls++ }
            override fun onAdLoaded() { calls++ }
        })
        view.destroyAd()
        view.layOut(1080)
        idle(5_000)
        assertEquals(0, calls)
    }
}
