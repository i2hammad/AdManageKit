package com.i2hammad.admanagekit.admob

import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * Pins down the contract every AdMob load now leans on: exactly one of onReady / onTimeout per
 * wait (neither once cancelled), no wait at all when the SDK is already up, and a readiness check
 * that throws never blocking ads for good.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MobileAdsGateTest {

    private var ready = false

    private fun gateOn(check: () -> Boolean = { ready }) {
        MobileAdsGate.readinessCheck = check
    }

    @After
    fun tearDown() {
        MobileAdsGate.readinessCheck = { MobileAds.isInitialized }
    }

    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test
    fun `already ready runs onReady synchronously and never times out`() {
        ready = true
        gateOn()
        var readyCalls = 0
        var timeouts = 0
        MobileAdsGate.runWhenReady(timeoutMillis = 1_000, onTimeout = { timeouts++ }) { readyCalls++ }

        assertEquals("onReady runs before runWhenReady returns", 1, readyCalls)
        idle(5_000)
        assertEquals(1, readyCalls)
        assertEquals(0, timeouts)
    }

    @Test
    fun `becoming ready later runs onReady exactly once`() {
        gateOn()
        var readyCalls = 0
        var timeouts = 0
        MobileAdsGate.runWhenReady(timeoutMillis = 5_000, onTimeout = { timeouts++ }) { readyCalls++ }

        idle(1_000)
        assertEquals("nothing runs while the SDK is still initializing", 0, readyCalls)

        ready = true
        idle(MobileAdsGate.CHECK_INTERVAL_MS)
        assertEquals(1, readyCalls)

        idle(10_000)
        assertEquals(1, readyCalls)
        assertEquals(0, timeouts)
    }

    @Test
    fun `never ready times out exactly once and never loads`() {
        gateOn()
        var readyCalls = 0
        var timeouts = 0
        MobileAdsGate.runWhenReady(timeoutMillis = 2_000, onTimeout = { timeouts++ }) { readyCalls++ }

        idle(1_500)
        assertEquals(0, timeouts)
        idle(1_000)
        assertEquals(1, timeouts)

        ready = true
        idle(10_000)
        assertEquals("a timed-out wait stays dead even if init finishes afterwards", 0, readyCalls)
        assertEquals(1, timeouts)
    }

    @Test
    fun `cancelled wait runs neither callback`() {
        gateOn()
        var calls = 0
        val wait = MobileAdsGate.runWhenReady(timeoutMillis = 2_000, onTimeout = { calls++ }) { calls++ }

        wait.cancel()
        assertTrue(wait.isCancelled)
        ready = true
        idle(10_000)
        assertEquals(0, calls)
    }

    @Test
    fun `a readiness check that throws counts as ready`() {
        gateOn { throw IllegalStateException("broken") }
        assertTrue(MobileAdsGate.isReady())

        var readyCalls = 0
        MobileAdsGate.runWhenReady(onTimeout = {}) { readyCalls++ }
        assertEquals(1, readyCalls)
    }

    @Test
    fun `awaitReady reports false when init never completes`() = runBlocking {
        gateOn()
        assertFalse(MobileAdsGate.awaitReady(timeoutMillis = 600))
        ready = true
        assertTrue(MobileAdsGate.awaitReady(timeoutMillis = 600))
    }

    @Test
    fun `timeout error carries the TIMEOUT code`() {
        val error = MobileAdsGate.timeoutError()
        assertEquals(LoadAdError.ErrorCode.TIMEOUT, error.code)
        assertEquals(MobileAdsGate.TIMEOUT_MESSAGE, error.message)
    }
}
