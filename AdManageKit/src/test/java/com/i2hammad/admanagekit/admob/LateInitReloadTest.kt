package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.i2hammad.admanagekit.config.AdManageKitConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
 * An ad view whose load gave up waiting for MobileAds.initialize() must load again, once, when
 * init finishes later — and must not when init was already done, the screen is gone, the user is
 * premium, the watch window has passed, or the feature is turned off.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LateInitReloadTest {

    private var ready = false
    private lateinit var activity: Activity
    private val reload = LateInitReload("test")
    private var reloads = 0
    private var failures = 0

    @Before
    fun setUp() {
        MobileAdsGate.readinessCheck = { ready }
        AdManageKitConfig.mobileAdsLateLoadWindow = 30.seconds
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @After
    fun tearDown() {
        reload.cancel()
        MobileAdsGate.readinessCheck = { MobileAds.isInitialized }
        AdManageKitConfig.resetToDefaults()
    }

    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun fail(error: LoadAdError = MobileAdsGate.timeoutError()) {
        val callback = reload.wrap(activity, object : AdLoadCallback() {
            override fun onFailedToLoad(error: AdKitError?) { failures++ }
        }) { reloads++ }
        callback.onFailedToLoad(error)
    }

    @Test
    fun `init finishing after the timeout loads the ad again exactly once`() {
        fail()
        assertEquals("the caller still hears about the timeout", 1, failures)
        assertTrue(reload.isPending)

        idle(5_000)
        assertEquals("nothing happens while init is still pending", 0, reloads)

        ready = true
        idle(MobileAdsGate.LATE_CHECK_INTERVAL_MS)
        assertEquals(1, reloads)
        assertFalse(reload.isPending)

        idle(60_000)
        assertEquals(1, reloads)
    }

    @Test
    fun `failure after init is done does not reload`() {
        ready = true
        fail(LoadAdError(LoadAdError.ErrorCode.NO_FILL, "no fill", null))
        idle(60_000)
        assertEquals(1, failures)
        assertEquals(0, reloads)
        assertFalse(reload.isPending)
    }

    @Test
    fun `cancel drops the pending reload`() {
        fail()
        reload.cancel()
        ready = true
        idle(60_000)
        assertEquals(0, reloads)
    }

    @Test
    fun `init that never comes within the window gives up`() {
        fail()
        idle(31_000)
        assertFalse(reload.isPending)
        ready = true
        idle(60_000)
        assertEquals(0, reloads)
    }

    @Test
    fun `zero window turns it off`() {
        AdManageKitConfig.mobileAdsLateLoadWindow = kotlin.time.Duration.ZERO
        fail()
        assertFalse(reload.isPending)
        ready = true
        idle(60_000)
        assertEquals(0, reloads)
    }

    @Test
    fun `premium failure never reloads`() {
        fail(LoadAdError(LoadAdError.ErrorCode.INTERNAL_ERROR, AdManager.PURCHASED_APP_ERROR_MESSAGE, null))
        assertFalse(reload.isPending)
    }

    @Test
    fun `host activity finishing before init completes skips the reload`() {
        fail()
        activity.finish()
        ready = true
        idle(60_000)
        assertEquals(0, reloads)
    }
}
