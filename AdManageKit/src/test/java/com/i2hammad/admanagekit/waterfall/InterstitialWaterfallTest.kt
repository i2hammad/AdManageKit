package com.i2hammad.admanagekit.waterfall

import android.app.Activity
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
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
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Pins down the [InterstitialWaterfall] contracts: per-ad-unit tracking,
 * generation-token cancellation of stale chains, per-attempt watchdog timeouts,
 * ownsProviders destroy semantics, and single-terminal-callback guarantees on
 * both load and show.
 */
@RunWith(RobolectricTestRunner::class)
class InterstitialWaterfallTest {

    private lateinit var context: Context
    private lateinit var activity: Activity

    private val providerA = FakeSingleSlotInterstitialProvider(AdProvider("fake-a", "Fake A"))
    private val providerB = FakeSingleSlotInterstitialProvider(AdProvider("fake-b", "Fake B"))

    /** Resolver mapping each fake provider to its own ad unit ID. */
    private val resolver: (AdProvider) -> String? = { provider ->
        when (provider.name) {
            "fake-a" -> "unit-a"
            "fake-b" -> "unit-b"
            "fake-keyed" -> "unit-keyed"
            "fake-single" -> "unit-single"
            else -> null
        }
    }

    private val timeoutMillis = 5_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
    }

    @After
    fun tearDown() {
        AdManageKitConfig.resetToDefaults()
    }

    private fun waterfall(
        providers: List<com.i2hammad.admanagekit.core.ad.InterstitialAdProvider>,
        ownsProviders: Boolean = false,
        attemptTimeoutMillis: Long = timeoutMillis
    ) = InterstitialWaterfall(providers, resolver, ownsProviders, attemptTimeoutMillis)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun idleFor(millis: Long) =
        shadowOf(Looper.getMainLooper()).idleFor(millis, TimeUnit.MILLISECONDS)

    // =================== LOAD ===================

    @Test
    fun `load with empty provider list delivers exactly one NO_FILL failure`() {
        val callback = RecordingInterstitialLoadCallback()

        waterfall(emptyList()).load(context, callback)
        idle()

        assertEquals(0, callback.loadedCount)
        assertEquals(1, callback.loadErrors.size)
        assertEquals(AdKitAdError.ERROR_CODE_NO_FILL, callback.loadErrors[0].code)
    }

    @Test
    fun `first provider failure falls through to second provider success`() {
        val callback = RecordingInterstitialLoadCallback()

        waterfall(listOf(providerA, providerB)).load(context, callback)

        assertEquals(1, providerA.loadCalls.size)
        assertEquals("unit-a", providerA.loadCalls[0].adUnitId)
        assertEquals(0, providerB.loadCalls.size)

        providerA.triggerLoadFailure()
        assertEquals(1, providerB.loadCalls.size)
        assertEquals("unit-b", providerB.loadCalls[0].adUnitId)

        providerB.triggerLoadSuccess()
        idle()

        assertEquals(1, callback.loadedCount)
        assertEquals(0, callback.loadErrors.size)
    }

    @Test
    fun `provider with no resolved ad unit is skipped`() {
        val unresolved = FakeSingleSlotInterstitialProvider(AdProvider("unmapped", "Unmapped"))
        val callback = RecordingInterstitialLoadCallback()

        waterfall(listOf(unresolved, providerB)).load(context, callback)

        assertEquals(0, unresolved.loadCalls.size)
        assertEquals(1, providerB.loadCalls.size)

        providerB.triggerLoadSuccess()
        idle()

        assertEquals(1, callback.loadedCount)
    }

    @Test
    fun `all providers failing delivers exactly one terminal failure`() {
        val callback = RecordingInterstitialLoadCallback()

        waterfall(listOf(providerA, providerB)).load(context, callback)
        providerA.triggerLoadFailure()
        providerB.triggerLoadFailure()
        idle()

        assertEquals(0, callback.loadedCount)
        assertEquals(1, callback.loadErrors.size)
        assertEquals(AdKitAdError.ERROR_CODE_NO_FILL, callback.loadErrors[0].code)
    }

    @Test
    fun `duplicate late callbacks from a settled attempt are ignored`() {
        val callback = RecordingInterstitialLoadCallback()

        waterfall(listOf(providerA, providerB)).load(context, callback)
        providerA.triggerLoadSuccess()

        // Misbehaving provider fires again after the attempt has settled.
        providerA.loadCalls[0].callback.onAdLoaded()
        providerA.loadCalls[0].callback.onAdFailedToLoad(testError())
        idle()

        assertEquals(1, callback.loadedCount)
        assertEquals(0, callback.loadErrors.size)
        // The settled failure must not advance the chain either.
        assertEquals(0, providerB.loadCalls.size)
    }

    // =================== GENERATION TOKEN ===================

    @Test
    fun `reload cancels in-flight chain - stale success is ignored, new chain delivers`() {
        val firstCallback = RecordingInterstitialLoadCallback()
        val secondCallback = RecordingInterstitialLoadCallback()
        val waterfall = waterfall(listOf(providerA))

        waterfall.load(context, firstCallback)
        // Provider A hangs; caller re-loads while the first chain is in flight.
        waterfall.load(context, secondCallback)
        assertEquals(2, providerA.loadCalls.size)

        // New chain resolves first.
        providerA.triggerLoadSuccess(index = 1)
        // Stale chain's late success must be ignored.
        providerA.triggerLoadSuccess(index = 0)
        idle()

        assertEquals(0, firstCallback.loadedCount)
        assertEquals(0, firstCallback.loadErrors.size)
        assertEquals(1, secondCallback.loadedCount)
        assertEquals(0, secondCallback.loadErrors.size)
    }

    @Test
    fun `stale chain failure does not advance the chain or call back`() {
        val firstCallback = RecordingInterstitialLoadCallback()
        val secondCallback = RecordingInterstitialLoadCallback()
        val waterfall = waterfall(listOf(providerA, providerB))

        waterfall.load(context, firstCallback)
        waterfall.load(context, secondCallback)
        assertEquals(2, providerA.loadCalls.size)

        // Stale chain's late failure: must not move on to provider B
        // and must not surface a failure to either callback.
        providerA.triggerLoadFailure(index = 0)
        idle()

        assertEquals(0, providerB.loadCalls.size)
        assertEquals(0, firstCallback.loadErrors.size)
        assertEquals(0, secondCallback.loadErrors.size)

        providerA.triggerLoadSuccess(index = 1)
        idle()
        assertEquals(1, secondCallback.loadedCount)
    }

    // =================== WATCHDOG ===================

    @Test
    fun `hung provider times out and chain advances - its late callback is ignored`() {
        val callback = RecordingInterstitialLoadCallback()

        waterfall(listOf(providerA, providerB)).load(context, callback)
        assertEquals(1, providerA.loadCalls.size)

        // Provider A never calls back; the watchdog fires and advances to B.
        idleFor(timeoutMillis)
        assertEquals(1, providerB.loadCalls.size)

        providerB.triggerLoadSuccess()
        // Late success from the hung provider must be ignored.
        providerA.triggerLoadSuccess(index = 0)
        idle()

        assertEquals(1, callback.loadedCount)
        assertEquals(0, callback.loadErrors.size)
    }

    @Test
    fun `watchdog defaults to AdManageKitConfig defaultAdTimeout`() {
        AdManageKitConfig.defaultAdTimeout = 2.seconds
        val callback = RecordingInterstitialLoadCallback()
        // No explicit timeout: the waterfall must read the config default.
        val waterfall = InterstitialWaterfall(listOf(providerA, providerB), resolver)

        waterfall.load(context, callback)

        idleFor(1_999)
        assertEquals(0, providerB.loadCalls.size)

        idleFor(1)
        assertEquals(1, providerB.loadCalls.size)
    }

    // =================== DESTROY ===================

    @Test
    fun `destroy mid-flight stops callbacks and prevents further provider loads`() {
        val callback = RecordingInterstitialLoadCallback()
        val waterfall = waterfall(listOf(providerA, providerB))

        waterfall.load(context, callback)
        waterfall.destroy()

        // Late result from the in-flight attempt is ignored.
        providerA.triggerLoadSuccess()
        // Even the pending watchdog must not advance to the next provider.
        idleFor(timeoutMillis)
        idle()

        assertEquals(0, callback.loadedCount)
        assertEquals(0, callback.loadErrors.size)
        assertEquals(0, providerB.loadCalls.size)
        assertFalse(waterfall.isAdReady())
    }

    @Test
    fun `destroy does not destroy shared providers when ownsProviders is false`() {
        val waterfall = waterfall(listOf(providerA, providerB), ownsProviders = false)

        waterfall.destroy()

        assertEquals(0, providerA.destroyCount)
        assertEquals(0, providerB.destroyCount)
    }

    @Test
    fun `destroy destroys owned providers when ownsProviders is true`() {
        val waterfall = waterfall(listOf(providerA, providerB), ownsProviders = true)

        waterfall.destroy()

        assertEquals(1, providerA.destroyCount)
        assertEquals(1, providerB.destroyCount)
    }

    // =================== SHOW ===================

    @Test
    fun `show passes the resolved ad unit ID to a keyed provider`() {
        val keyed = FakeKeyedInterstitialProvider()
        val waterfall = waterfall(listOf(keyed))
        val loadCallback = RecordingInterstitialLoadCallback()
        val showCallback = RecordingInterstitialShowCallback()

        waterfall.load(context, loadCallback)
        keyed.triggerLoadSuccess()
        idle()
        assertEquals(1, loadCallback.loadedCount)
        assertTrue(waterfall.isAdReady())

        waterfall.show(activity, showCallback)

        assertEquals(listOf("unit-keyed"), keyed.shownAdUnitIds)
        assertEquals(0, keyed.unkeyedShowCount)

        // Successful show: provider dismisses, exactly one terminal callback.
        keyed.showCallbacks[0].onAdShowed()
        keyed.showCallbacks[0].onAdDismissed()
        idle()

        assertEquals(1, showCallback.showedCount)
        assertEquals(1, showCallback.dismissedCount)
        assertEquals(0, showCallback.showErrors.size)
    }

    @Test
    fun `show on a single-slot provider delegates through the keyed default methods`() {
        val waterfall = waterfall(listOf(providerA))
        val loadCallback = RecordingInterstitialLoadCallback()
        val showCallback = RecordingInterstitialShowCallback()

        waterfall.load(context, loadCallback)
        providerA.triggerLoadSuccess()
        idle()

        waterfall.show(activity, showCallback)

        // Keyed defaults must fall through to the single-slot showAd.
        assertEquals(1, providerA.showCallbacks.size)
        // The ad is consumed by the show attempt.
        assertFalse(waterfall.isAdReady())
    }

    @Test
    fun `show with nothing loaded delivers exactly one onAdFailedToShow`() {
        val showCallback = RecordingInterstitialShowCallback()

        waterfall(listOf(providerA)).show(activity, showCallback)
        idle()

        assertEquals(1, showCallback.showErrors.size)
        assertEquals(AdKitAdError.ERROR_CODE_INTERNAL, showCallback.showErrors[0].code)
        assertEquals(0, showCallback.dismissedCount)
        assertEquals(0, showCallback.showedCount)
    }

    @Test
    fun `show failure suppresses the duplicate onAdDismissed terminal event`() {
        val waterfall = waterfall(listOf(providerA))
        val loadCallback = RecordingInterstitialLoadCallback()
        val showCallback = RecordingInterstitialShowCallback()

        waterfall.load(context, loadCallback)
        providerA.triggerLoadSuccess()
        idle()

        waterfall.show(activity, showCallback)

        // Some networks emit both onAdFailedToShow and onAdDismissed on failure.
        providerA.showCallbacks[0].onAdFailedToShow(testError(AdKitAdError.ERROR_CODE_INTERNAL, "show failed"))
        providerA.showCallbacks[0].onAdDismissed()
        idle()

        assertEquals(1, showCallback.showErrors.size)
        assertEquals(0, showCallback.dismissedCount)
    }

    @Test
    fun `second show without reloading fails because the ad was consumed`() {
        val waterfall = waterfall(listOf(providerA))
        val loadCallback = RecordingInterstitialLoadCallback()

        waterfall.load(context, loadCallback)
        providerA.triggerLoadSuccess()
        idle()

        val firstShow = RecordingInterstitialShowCallback()
        waterfall.show(activity, firstShow)
        providerA.showCallbacks[0].onAdDismissed()

        val secondShow = RecordingInterstitialShowCallback()
        waterfall.show(activity, secondShow)
        idle()

        assertEquals(1, secondShow.showErrors.size)
        assertEquals(0, secondShow.dismissedCount)
    }
}
