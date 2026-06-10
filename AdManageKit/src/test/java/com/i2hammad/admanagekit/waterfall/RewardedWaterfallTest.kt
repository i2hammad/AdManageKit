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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Pins down the [RewardedWaterfall] contracts: provider fallthrough,
 * reward forwarding, and single-terminal-callback on show.
 */
@RunWith(RobolectricTestRunner::class)
class RewardedWaterfallTest {

    private lateinit var context: Context
    private lateinit var activity: Activity

    private val providerA = FakeRewardedProvider(AdProvider("fake-a", "Fake A"))
    private val providerB = FakeRewardedProvider(AdProvider("fake-b", "Fake B"))

    private val resolver: (AdProvider) -> String? = { provider ->
        when (provider.name) {
            "fake-a" -> "unit-a"
            "fake-b" -> "unit-b"
            else -> null
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
    }

    @After
    fun tearDown() {
        AdManageKitConfig.resetToDefaults()
    }

    private fun waterfall(providers: List<com.i2hammad.admanagekit.core.ad.RewardedAdProvider>) =
        RewardedWaterfall(providers, resolver, false, 5_000L)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun `success path delivers one onAdLoaded and forwards the reward on show`() {
        val waterfall = waterfall(listOf(providerA))
        val loadCallback = RecordingRewardedLoadCallback()
        val showCallback = RecordingRewardedShowCallback()

        waterfall.load(context, loadCallback)
        assertEquals(1, providerA.loadCalls.size)
        assertEquals("unit-a", providerA.loadCalls[0].adUnitId)

        providerA.triggerLoadSuccess()
        idle()
        assertEquals(1, loadCallback.loadedCount)
        assertEquals(0, loadCallback.loadErrors.size)

        waterfall.show(activity, showCallback)
        assertEquals(1, providerA.showCallbacks.size)

        providerA.showCallbacks[0].onAdShowed()
        providerA.showCallbacks[0].onRewardEarned("coins", 25)
        providerA.showCallbacks[0].onAdDismissed()
        idle()

        assertEquals(1, showCallback.showedCount)
        assertEquals(listOf("coins" to 25), showCallback.rewards)
        assertEquals(1, showCallback.dismissedCount)
        assertEquals(0, showCallback.showErrors.size)
    }

    @Test
    fun `first provider failure falls through to second provider`() {
        val loadCallback = RecordingRewardedLoadCallback()

        waterfall(listOf(providerA, providerB)).load(context, loadCallback)
        providerA.triggerLoadFailure()

        assertEquals(1, providerB.loadCalls.size)
        providerB.triggerLoadSuccess()
        idle()

        assertEquals(1, loadCallback.loadedCount)
        assertEquals(0, loadCallback.loadErrors.size)
    }

    @Test
    fun `all providers failing delivers exactly one NO_FILL failure`() {
        val loadCallback = RecordingRewardedLoadCallback()

        waterfall(listOf(providerA, providerB)).load(context, loadCallback)
        providerA.triggerLoadFailure()
        providerB.triggerLoadFailure()
        idle()

        assertEquals(0, loadCallback.loadedCount)
        assertEquals(1, loadCallback.loadErrors.size)
        assertEquals(AdKitAdError.ERROR_CODE_NO_FILL, loadCallback.loadErrors[0].code)
    }

    @Test
    fun `show failure delivers one terminal event and suppresses duplicate dismissal`() {
        val waterfall = waterfall(listOf(providerA))
        val loadCallback = RecordingRewardedLoadCallback()
        val showCallback = RecordingRewardedShowCallback()

        waterfall.load(context, loadCallback)
        providerA.triggerLoadSuccess()
        idle()

        waterfall.show(activity, showCallback)
        providerA.showCallbacks[0].onAdFailedToShow(testError(AdKitAdError.ERROR_CODE_INTERNAL, "show failed"))
        providerA.showCallbacks[0].onAdDismissed()
        idle()

        assertEquals(1, showCallback.showErrors.size)
        assertEquals(0, showCallback.dismissedCount)
        assertEquals(0, showCallback.rewards.size)
    }

    @Test
    fun `show with nothing loaded delivers exactly one onAdFailedToShow`() {
        val showCallback = RecordingRewardedShowCallback()

        waterfall(listOf(providerA)).show(activity, showCallback)
        idle()

        assertEquals(1, showCallback.showErrors.size)
        assertEquals(AdKitAdError.ERROR_CODE_INTERNAL, showCallback.showErrors[0].code)
        assertEquals(0, showCallback.dismissedCount)
    }
}
