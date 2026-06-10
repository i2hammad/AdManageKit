package com.i2hammad.admanagekit.waterfall

import android.content.Context
import android.os.Looper
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.i2hammad.admanagekit.config.AdManageKitConfig
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Pins down the [BannerWaterfall] contracts: provider fallthrough, the loaded
 * view being delivered as-is, and ownsProviders destroy semantics.
 */
@RunWith(RobolectricTestRunner::class)
class BannerWaterfallTest {

    private lateinit var context: Context

    private val providerA = FakeBannerProvider(AdProvider("fake-a", "Fake A"))
    private val providerB = FakeBannerProvider(AdProvider("fake-b", "Fake B"))

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
    }

    @After
    fun tearDown() {
        AdManageKitConfig.resetToDefaults()
    }

    private fun waterfall(
        providers: List<com.i2hammad.admanagekit.core.ad.BannerAdProvider>,
        ownsProviders: Boolean = false
    ) = BannerWaterfall(providers, resolver, ownsProviders, 5_000L)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun `successful load delivers the provider's banner view`() {
        val callback = RecordingBannerCallback()
        val bannerView = View(context)

        waterfall(listOf(providerA)).load(context, callback)
        assertEquals(1, providerA.loadCalls.size)
        assertEquals("unit-a", providerA.loadCalls[0].adUnitId)

        providerA.triggerLoadSuccess(bannerView)
        idle()

        assertEquals(1, callback.loadedViews.size)
        assertSame(bannerView, callback.loadedViews[0])
        assertEquals(0, callback.loadErrors.size)
    }

    @Test
    fun `first provider failure falls through to second provider`() {
        val callback = RecordingBannerCallback()
        val bannerView = View(context)

        waterfall(listOf(providerA, providerB)).load(context, callback)
        providerA.triggerLoadFailure()

        assertEquals(1, providerB.loadCalls.size)
        providerB.triggerLoadSuccess(bannerView)
        idle()

        assertEquals(1, callback.loadedViews.size)
        assertSame(bannerView, callback.loadedViews[0])
        assertEquals(0, callback.loadErrors.size)
    }

    @Test
    fun `all providers failing delivers exactly one NO_FILL failure`() {
        val callback = RecordingBannerCallback()

        waterfall(listOf(providerA, providerB)).load(context, callback)
        providerA.triggerLoadFailure()
        providerB.triggerLoadFailure()
        idle()

        assertEquals(0, callback.loadedViews.size)
        assertEquals(1, callback.loadErrors.size)
        assertEquals(AdKitAdError.ERROR_CODE_NO_FILL, callback.loadErrors[0].code)
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

    @Test
    fun `destroy mid-flight ignores the late banner and stops the chain`() {
        val callback = RecordingBannerCallback()
        val waterfall = waterfall(listOf(providerA, providerB))

        waterfall.load(context, callback)
        waterfall.destroy()

        providerA.triggerLoadSuccess(View(context))
        idle()

        assertEquals(0, callback.loadedViews.size)
        assertEquals(0, callback.loadErrors.size)
        assertEquals(0, providerB.loadCalls.size)
    }

    @Test
    fun `pause and resume are forwarded to the loaded provider only`() {
        val callback = RecordingBannerCallback()
        val waterfall = waterfall(listOf(providerA))

        // Nothing loaded yet: pause/resume are no-ops.
        waterfall.pause()
        waterfall.resume()
        assertEquals(0, providerA.pauseCount)
        assertEquals(0, providerA.resumeCount)

        waterfall.load(context, callback)
        providerA.triggerLoadSuccess(View(context))
        idle()

        waterfall.pause()
        waterfall.resume()
        assertEquals(1, providerA.pauseCount)
        assertEquals(1, providerA.resumeCount)
    }
}
