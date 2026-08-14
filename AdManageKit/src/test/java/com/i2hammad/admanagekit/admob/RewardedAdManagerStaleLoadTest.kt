package com.i2hammad.admanagekit.admob

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.firebase.analytics.FirebaseAnalytics
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.ad.AdProviderConfig
import com.i2hammad.admanagekit.core.ad.AdUnitMapping
import com.i2hammad.admanagekit.waterfall.FakeRewardedProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Pins down what a load that [RewardedAdManager] has given up on may and may not do when it
 * finally reports back.
 *
 * A load cannot be cancelled once it has been handed to the SDK, but `loadRewardedAdWithTimeout`
 * stops waiting for one — so its callbacks can land after a replacement load has started. Left
 * unchecked they acted as if they still spoke for the manager, and the damage was silent: an ad
 * that had arrived in the meantime was thrown away, and the flag telling everyone a load was in
 * progress was cleared out from under the load that was still running.
 *
 * Driven through the waterfall path because its providers are fakes the test can complete by hand;
 * the AdMob path runs the same generation checks over a static SDK entry point.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RewardedAdManagerStaleLoadTest {

    private companion object {
        const val PLACEMENT = "rewarded-placement"
        const val WATERFALL_UNIT = "waterfall-unit"

        /** The manager's own wait, short so tests can step past it. */
        const val LOAD_TIMEOUT_MS = 500L
    }

    private lateinit var context: Context
    private val provider = FakeRewardedProvider()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // The manager reaches for analytics on every load; nothing here asserts on it.
        mockkStatic(FirebaseAnalytics::class)
        every { FirebaseAnalytics.getInstance(any()) } returns mockk(relaxed = true)

        AdManageKitConfig.resetToDefaults()
        // Keep the waterfall's own per-provider watchdog well clear of the manager's timeout, so
        // every fallthrough in these tests is one the test asked for.
        AdManageKitConfig.defaultAdTimeout = 60.seconds

        resetManagerState()
        setPrivate("adUnitId", PLACEMENT)

        AdUnitMapping.register(PLACEMENT, mapOf(provider.provider.name to WATERFALL_UNIT))
        AdProviderConfig.setRewardedChain(listOf(provider))
    }

    @After
    fun tearDown() {
        AdProviderConfig.reset()
        AdUnitMapping.clear()
        resetManagerState()
        AdManageKitConfig.resetToDefaults()
        unmockkAll()
    }

    @Test
    fun `a load abandoned at the timeout cannot discard an ad loaded since`() {
        val first = RecordingLoadCallback()
        RewardedAdManager.loadRewardedAdWithTimeout(context, LOAD_TIMEOUT_MS, first)
        assertEquals("the chain should have reached the provider", 1, provider.loadCalls.size)

        // Nothing comes back in time, so the manager gives up on that request.
        idlePastTimeout()
        assertEquals(1, first.failures)

        // A second request is made and this one fills.
        val second = RecordingLoadCallback()
        RewardedAdManager.loadRewardedAdWithTimeout(context, LOAD_TIMEOUT_MS, second)
        assertEquals(2, provider.loadCalls.size)
        provider.triggerLoadSuccess(index = 1)
        assertEquals(1, second.loaded)
        assertTrue("the second load should leave an ad ready", RewardedAdManager.isAdLoaded())

        // Only now does the abandoned request report its failure.
        provider.triggerLoadFailure(index = 0)

        assertTrue(
            "an abandoned load's failure must not throw away the ad another load produced",
            RewardedAdManager.isAdLoaded()
        )
        assertEquals("the caller waiting on the newer load must not be failed", 0, second.failures)
    }

    @Test
    fun `a load abandoned at the timeout cannot clear the replacement's loading flag`() {
        RewardedAdManager.loadRewardedAdWithTimeout(context, LOAD_TIMEOUT_MS, RecordingLoadCallback())
        idlePastTimeout()

        // The replacement is still in flight when the abandoned one reports back.
        RewardedAdManager.loadRewardedAdWithTimeout(context, LOAD_TIMEOUT_MS, RecordingLoadCallback())
        assertTrue(RewardedAdManager.isLoading())

        provider.triggerLoadFailure(index = 0)

        assertTrue(
            "isLoading belongs to the load that is still running",
            RewardedAdManager.isLoading()
        )
    }

    @Test
    fun `an ad that arrives after the timeout is kept for the next show`() {
        val callback = RecordingLoadCallback()
        RewardedAdManager.loadRewardedAdWithTimeout(context, LOAD_TIMEOUT_MS, callback)

        idlePastTimeout()
        assertEquals(1, callback.failures)
        assertFalse(RewardedAdManager.isAdLoaded())

        // Late, but nothing else is in hand — the ad is real and showable, so it is kept.
        provider.triggerLoadSuccess(index = 0)

        assertTrue(
            "a late ad should be saved rather than dropped when nothing else is loaded",
            RewardedAdManager.isAdLoaded()
        )
    }

    // ==================== helpers ====================

    /** Steps the main looper past the manager's own wait so its timeout handler runs. */
    private fun idlePastTimeout() {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(LOAD_TIMEOUT_MS + 50))
    }

    /**
     * [RewardedAdManager] is a process-lifetime singleton with no reset hook, so its state is
     * cleared straight off the object between tests to keep them independent.
     */
    private fun resetManagerState() {
        setPrivate("rewardedAd", null)
        setPrivate("rewardedWaterfall", null)
        setPrivate("isLoading", false)
        setPrivate("isShowingAd", false)
        setPrivate("adUnitId", "")
        @Suppress("UNCHECKED_CAST")
        (getPrivate("pendingLoadCallbacks") as MutableList<Any>).clear()
        RewardedAdManager.resetAdStats()
    }

    private fun setPrivate(name: String, value: Any?) {
        RewardedAdManager::class.java.getDeclaredField(name).apply { isAccessible = true }
            .set(null, value)
    }

    private fun getPrivate(name: String): Any? =
        RewardedAdManager::class.java.getDeclaredField(name).apply { isAccessible = true }.get(null)

    private class RecordingLoadCallback : RewardedAdManager.OnRewardedAdLoadCallback {
        var loaded = 0
        var failures = 0

        override fun onAdLoaded() {
            loaded++
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
            failures++
        }
    }
}
