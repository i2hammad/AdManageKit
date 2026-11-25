package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
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

/**
 * [BannerAdView]'s post-swap relayout must not stall the window's layout.
 *
 * It used to flag every ancestor with forceLayout() and then call requestLayout() on itself.
 * requestLayout() stops climbing at the first ancestor that already reports isLayoutRequested,
 * so the request never reached ViewRootImpl. When no traversal was pending (an inline banner's
 * refresh), the flags stayed set and every later requestLayout() in the window stopped at them:
 * views added afterwards (a new fragment, a new tab) stayed 0x0 and the screen went blank.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BannerAdViewRelayoutTest {

    private lateinit var activity: Activity
    private lateinit var content: FrameLayout

    @Before
    fun setUp() {
        mockkStatic(FirebaseAnalytics::class)
        every { FirebaseAnalytics.getInstance(any()) } returns mockk(relaxed = true)
        activity = Robolectric.buildActivity(Activity::class.java).setup().visible().get()
        content = FrameLayout(activity)
        activity.setContentView(content)
        idle()
    }

    @After
    fun tearDown() = unmockkAll()

    private fun idle() = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))

    private fun BannerAdView.forceRelayout() {
        BannerAdView::class.java.getDeclaredMethod("forceRelayoutAfterAdSwap")
            .apply { isAccessible = true }
            .invoke(this)
    }

    private fun ancestors(view: View): List<View> =
        generateSequence(view) { it.parent as? View }.toList()

    @Test
    fun `relayout with no traversal pending leaves the window able to lay out`() {
        val banner = BannerAdView(activity)
        content.addView(banner, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        idle()
        assertTrue("setup: window laid out", ancestors(banner).none { it.isLayoutRequested })

        // An inline refresh: nothing else asked for a layout.
        banner.forceRelayout()
        idle()

        assertTrue(
            "every ancestor is laid out again after the relayout",
            ancestors(banner).none { it.isLayoutRequested }
        )

        // What the user saw: a view added later (a new tab) must still get a size.
        val later = TextView(activity).apply { text = "next tab" }
        content.addView(later)
        idle()
        assertTrue("a view added after the relayout is laid out", later.width > 0)
    }

    @Test
    fun `relayout on a detached banner leaves the window untouched`() {
        val holder = FrameLayout(activity)
        val banner = BannerAdView(activity)
        holder.addView(banner)

        banner.forceRelayout()
        idle()

        assertFalse(content.isLayoutRequested)
        val later = TextView(activity).apply { text = "next tab" }
        content.addView(later)
        idle()
        assertTrue(later.width > 0)
    }
}
