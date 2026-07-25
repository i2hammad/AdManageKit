package com.i2hammad.admanagekit.config

import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.i2hammad.admanagekit.admob.toFixedAdMobAdSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [BannerAdSize] dimensions and its mapping to Next-Gen SDK [AdSize]
 * constants. Robolectric because AdSize's static initializer runs on class load.
 */
@RunWith(RobolectricTestRunner::class)
class BannerAdSizeTest {

    @Test
    fun `fixed sizes match the standard AdMob banner dimensions`() {
        assertEquals(320 to 50, BannerAdSize.BANNER.widthDp to BannerAdSize.BANNER.heightDp)
        assertEquals(320 to 100, BannerAdSize.LARGE_BANNER.widthDp to BannerAdSize.LARGE_BANNER.heightDp)
        assertEquals(300 to 250, BannerAdSize.MEDIUM_RECTANGLE.widthDp to BannerAdSize.MEDIUM_RECTANGLE.heightDp)
        assertEquals(468 to 60, BannerAdSize.FULL_BANNER.widthDp to BannerAdSize.FULL_BANNER.heightDp)
        assertEquals(728 to 90, BannerAdSize.LEADERBOARD.widthDp to BannerAdSize.LEADERBOARD.heightDp)
    }

    @Test
    fun `only the adaptive sizes are adaptive and have no fixed dimensions`() {
        val adaptiveSizes = setOf(BannerAdSize.ADAPTIVE, BannerAdSize.ADAPTIVE_LARGE)
        adaptiveSizes.forEach {
            assertTrue("$it must be adaptive", it.isAdaptive)
            assertNull("$it width", it.widthDp)
            assertNull("$it height", it.heightDp)
        }
        BannerAdSize.entries.filterNot { it in adaptiveSizes }.forEach {
            assertFalse("$it must not be adaptive", it.isAdaptive)
        }
    }

    @Test
    fun `every size maps to the matching SDK constant, adaptive sizes to null`() {
        assertNull(BannerAdSize.ADAPTIVE.toFixedAdMobAdSize())
        assertNull(BannerAdSize.ADAPTIVE_LARGE.toFixedAdMobAdSize())
        assertEquals(AdSize.BANNER, BannerAdSize.BANNER.toFixedAdMobAdSize())
        assertEquals(AdSize.LARGE_BANNER, BannerAdSize.LARGE_BANNER.toFixedAdMobAdSize())
        assertEquals(AdSize.MEDIUM_RECTANGLE, BannerAdSize.MEDIUM_RECTANGLE.toFixedAdMobAdSize())
        assertEquals(AdSize.FULL_BANNER, BannerAdSize.FULL_BANNER.toFixedAdMobAdSize())
        assertEquals(AdSize.LEADERBOARD, BannerAdSize.LEADERBOARD.toFixedAdMobAdSize())
    }

    @Test
    fun `SDK constants agree with the documented dp dimensions`() {
        BannerAdSize.entries.filter { !it.isAdaptive }.forEach { size ->
            val sdkSize = size.toFixedAdMobAdSize()!!
            assertEquals("$size width", size.widthDp, sdkSize.width)
            assertEquals("$size height", size.heightDp, sdkSize.height)
        }
    }

    @Test
    fun `XML attr enum values align with BannerAdSize ordinals`() {
        // attrs.xml bannerAdSize enum values are resolved via BannerAdSize.entries[ordinal]
        assertEquals(0, BannerAdSize.ADAPTIVE.ordinal)
        assertEquals(1, BannerAdSize.ADAPTIVE_LARGE.ordinal)
        assertEquals(2, BannerAdSize.BANNER.ordinal)
        assertEquals(3, BannerAdSize.LARGE_BANNER.ordinal)
        assertEquals(4, BannerAdSize.MEDIUM_RECTANGLE.ordinal)
        assertEquals(5, BannerAdSize.FULL_BANNER.ordinal)
        assertEquals(6, BannerAdSize.LEADERBOARD.ordinal)
    }
}
