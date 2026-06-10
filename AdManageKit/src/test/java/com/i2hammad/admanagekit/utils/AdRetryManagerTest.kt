package com.i2hammad.admanagekit.sample

import android.app.Application
import android.os.Looper
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.utils.AdRetryManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [AdRetryManager] scheduling, cancellation, replacement and
 * exponential backoff behavior, driven via Robolectric's paused main looper.
 *
 * Uses a plain [Application] to bypass the sample app's MyApplication,
 * which initializes ad SDKs and mutates AdManageKitConfig.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class AdRetryManagerTest {

    private val retryManager = AdRetryManager.getInstance()
    private val mainLooper get() = shadowOf(Looper.getMainLooper())

    @Before
    fun setUp() {
        AdManageKitConfig.resetToDefaults()
        retryManager.clear()
    }

    @After
    fun tearDown() {
        retryManager.clear()
        AdManageKitConfig.resetToDefaults()
    }

    private fun idleMs(millis: Long) {
        mainLooper.idleFor(millis, TimeUnit.MILLISECONDS)
    }

    @Test
    fun `scheduleRetry does nothing when autoRetryFailedAds is false`() {
        AdManageKitConfig.autoRetryFailedAds = false
        val executions = AtomicInteger(0)

        retryManager.scheduleRetry("unit-disabled", attempt = 0) {
            executions.incrementAndGet()
        }

        assertFalse(retryManager.hasActiveRetry("unit-disabled"))
        assertEquals(0, retryManager.getActiveRetryCount())

        // Idle well past any conceivable backoff delay
        idleMs(60_000)
        assertEquals(0, executions.get())
    }

    @Test
    fun `retry action runs after backoff delay and active state transitions`() {
        AdManageKitConfig.autoRetryFailedAds = true
        AdManageKitConfig.baseRetryDelay = 1.seconds
        val executions = AtomicInteger(0)

        // attempt = 0 -> delay = baseRetryDelay * 2^0 = 1000ms
        retryManager.scheduleRetry("unit-a", attempt = 0) {
            executions.incrementAndGet()
        }

        assertTrue(retryManager.hasActiveRetry("unit-a"))
        assertEquals(1, retryManager.getActiveRetryCount())

        // Just before the delay: nothing must run yet
        idleMs(999)
        assertEquals(0, executions.get())
        assertTrue(retryManager.hasActiveRetry("unit-a"))

        // Crossing the delay boundary: runs exactly once
        idleMs(1)
        assertEquals(1, executions.get())
        assertFalse(retryManager.hasActiveRetry("unit-a"))
        assertEquals(0, retryManager.getActiveRetryCount())
    }

    @Test
    fun `scheduleRetry respects max attempts`() {
        AdManageKitConfig.autoRetryFailedAds = true
        val executions = AtomicInteger(0)

        // attempt >= maxAttempts must not schedule anything
        retryManager.scheduleRetry("unit-max", attempt = 3, maxAttempts = 3) {
            executions.incrementAndGet()
        }
        retryManager.scheduleRetry("unit-max", attempt = 5, maxAttempts = 3) {
            executions.incrementAndGet()
        }

        assertFalse(retryManager.hasActiveRetry("unit-max"))
        idleMs(60_000)
        assertEquals(0, executions.get())
    }

    @Test
    fun `re-scheduling same ad unit cancels previous runnable and only new action runs once`() {
        AdManageKitConfig.autoRetryFailedAds = true
        AdManageKitConfig.baseRetryDelay = 1.seconds
        val firstExecutions = AtomicInteger(0)
        val secondExecutions = AtomicInteger(0)

        retryManager.scheduleRetry("unit-replace", attempt = 0) {
            firstExecutions.incrementAndGet()
        }
        // Re-schedule for the same ad unit before the first fires
        retryManager.scheduleRetry("unit-replace", attempt = 0) {
            secondExecutions.incrementAndGet()
        }

        // Only one active entry should be tracked
        assertEquals(1, retryManager.getActiveRetryCount())

        // Idle far past every possible delay: the OLD action must never run,
        // the NEW action must run exactly once
        idleMs(60_000)
        assertEquals("Replaced retry action must not execute", 0, firstExecutions.get())
        assertEquals("New retry action must execute exactly once", 1, secondExecutions.get())
        assertFalse(retryManager.hasActiveRetry("unit-replace"))
        assertEquals(0, retryManager.getActiveRetryCount())
    }

    @Test
    fun `cancelRetry removes pending retry so action never runs`() {
        AdManageKitConfig.autoRetryFailedAds = true
        val executions = AtomicInteger(0)

        retryManager.scheduleRetry("unit-cancel", attempt = 0) {
            executions.incrementAndGet()
        }
        assertTrue(retryManager.hasActiveRetry("unit-cancel"))

        retryManager.cancelRetry("unit-cancel")
        assertFalse(retryManager.hasActiveRetry("unit-cancel"))

        idleMs(60_000)
        assertEquals(0, executions.get())
    }

    @Test
    fun `cancelAllRetries removes every pending retry`() {
        AdManageKitConfig.autoRetryFailedAds = true
        val executions = AtomicInteger(0)

        retryManager.scheduleRetry("unit-1", attempt = 0) { executions.incrementAndGet() }
        retryManager.scheduleRetry("unit-2", attempt = 0) { executions.incrementAndGet() }
        assertEquals(2, retryManager.getActiveRetryCount())

        retryManager.cancelAllRetries()
        assertEquals(0, retryManager.getActiveRetryCount())

        idleMs(60_000)
        assertEquals(0, executions.get())
    }

    @Test
    fun `exponential backoff delay doubles per attempt`() {
        AdManageKitConfig.autoRetryFailedAds = true
        AdManageKitConfig.enableExponentialBackoff = true
        AdManageKitConfig.baseRetryDelay = 1.seconds
        AdManageKitConfig.maxRetryDelay = 30.seconds
        val executions = AtomicInteger(0)

        // attempt = 2 -> delay = 1000 * 2^2 = 4000ms
        retryManager.scheduleRetry("unit-backoff", attempt = 2, maxAttempts = 5) {
            executions.incrementAndGet()
        }

        idleMs(3_999)
        assertEquals("Must not run before 4000ms backoff", 0, executions.get())

        idleMs(1)
        assertEquals(1, executions.get())
    }

    @Test
    fun `exponential backoff delay is capped at maxRetryDelay`() {
        AdManageKitConfig.autoRetryFailedAds = true
        AdManageKitConfig.enableExponentialBackoff = true
        AdManageKitConfig.baseRetryDelay = 1.seconds
        AdManageKitConfig.maxRetryDelay = 30.seconds
        val executions = AtomicInteger(0)

        // attempt = 9 -> uncapped delay would be 1000 * 2^9 = 512000ms, capped to 30000ms
        retryManager.scheduleRetry("unit-cap", attempt = 9, maxAttempts = 20) {
            executions.incrementAndGet()
        }

        idleMs(29_999)
        assertEquals("Must not run before the 30s cap", 0, executions.get())

        idleMs(1)
        assertEquals("Must run exactly at the capped delay", 1, executions.get())
    }

    @Test
    fun `backoff disabled uses flat baseRetryDelay regardless of attempt`() {
        AdManageKitConfig.autoRetryFailedAds = true
        AdManageKitConfig.enableExponentialBackoff = false
        AdManageKitConfig.baseRetryDelay = 1.seconds
        val executions = AtomicInteger(0)

        // With backoff disabled, even a high attempt uses the flat base delay
        retryManager.scheduleRetry("unit-flat", attempt = 5, maxAttempts = 10) {
            executions.incrementAndGet()
        }

        idleMs(999)
        assertEquals(0, executions.get())

        idleMs(1)
        assertEquals(1, executions.get())
    }

    @Test
    fun `getActiveRetriesSummary reports attempt numbers as one-based`() {
        AdManageKitConfig.autoRetryFailedAds = true

        retryManager.scheduleRetry("unit-s1", attempt = 0) { }
        retryManager.scheduleRetry("unit-s2", attempt = 2, maxAttempts = 5) { }

        val summary = retryManager.getActiveRetriesSummary()
        assertEquals(2, summary.size)
        assertEquals(1, summary["unit-s1"])
        assertEquals(3, summary["unit-s2"])
    }

    @Test
    fun `exception thrown by retry action is swallowed and retry entry is removed`() {
        AdManageKitConfig.autoRetryFailedAds = true
        val executions = AtomicInteger(0)

        retryManager.scheduleRetry("unit-throw", attempt = 0) {
            executions.incrementAndGet()
            throw IllegalStateException("boom")
        }

        // Must not propagate out of the looper
        idleMs(60_000)
        assertEquals(1, executions.get())
        assertFalse(retryManager.hasActiveRetry("unit-throw"))
    }
}
