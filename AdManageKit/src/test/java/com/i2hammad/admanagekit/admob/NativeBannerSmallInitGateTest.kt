package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.config.AdLoadingStrategy
import com.i2hammad.admanagekit.config.AdManageKitConfig
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
 * The crash this guards against: a screen loads a native banner in onCreate before the host app's
 * MobileAds.initialize() has finished, and the Next-Gen SDK throws
 * `IllegalStateException: MobileAds.initialize must be called before using the Google Mobile Ads SDK`.
 *
 * Under Robolectric the real SDK is never initialized, so a load that reached NativeAdLoader
 * would throw here. The view must instead hold the load and, when init never comes, fail through
 * its normal failure callback with a TIMEOUT error.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NativeBannerSmallInitGateTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        mockkStatic(FirebaseAnalytics::class)
        every { FirebaseAnalytics.getInstance(any()) } returns mockk(relaxed = true)
        MobileAdsGate.readinessCheck = { false }
        AdManageKitConfig.mobileAdsInitTimeout = 2.seconds
        NativeAdManager.enableCachingNativeAds = false
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @After
    fun tearDown() {
        MobileAdsGate.readinessCheck = { com.google.android.libraries.ads.mobile.sdk.MobileAds.isInitialized }
        AdManageKitConfig.resetToDefaults()
        unmockkAll()
    }

    @Test
    fun `load before MobileAds init waits, then fails with TIMEOUT instead of crashing`() {
        val view = NativeBannerSmall(activity)
        var failure: LoadAdError? = null
        var failures = 0

        view.loadNativeBannerAd(activity, "ca-app-pub-3940256099942544/2247696110", object : AdLoadCallback() {
            override fun onFailedToLoad(error: AdKitError?) {
                failures++
                failure = error
            }
        }, AdLoadingStrategy.ON_DEMAND)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_000))
        assertNull("still waiting for init, nothing reported yet", failure)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(3_000))
        assertEquals(1, failures)
        assertEquals(LoadAdError.ErrorCode.TIMEOUT, failure?.code)
    }

    @Test
    fun `destroy while waiting drops the load`() {
        val view = NativeBannerSmall(activity)
        var calls = 0
        view.loadNativeBannerAd(activity, "ca-app-pub-3940256099942544/2247696110", object : AdLoadCallback() {
            override fun onFailedToLoad(error: AdKitError?) { calls++ }
            override fun onAdLoaded() { calls++ }
        }, AdLoadingStrategy.ON_DEMAND)

        view.destroy()
        MobileAdsGate.readinessCheck = { true }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5_000))
        assertEquals(0, calls)
    }
}
