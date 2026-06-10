package com.i2hammad.admanagekit.admob

import com.google.android.gms.ads.nativead.NativeAd
import com.i2hammad.admanagekit.config.AdManageKitConfig
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [NativeAdManager] caching behavior: destructive reads, LRU eviction,
 * per-unit and global clearing, the enable flag, and fallback lookups.
 *
 * Runs against Robolectric's default Application, so no ad SDKs are initialized
 * and AdManageKitConfig is never mutated outside the tests. Firebase analytics is
 * never touched: NativeAdManager only logs analytics when initialize(analytics)
 * was called AND enablePerformanceMetrics is true — both stay off here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NativeAdManagerCacheTest {

    private fun newAd(): NativeAd = mockk(relaxed = true)

    @Before
    fun setUp() {
        AdManageKitConfig.resetToDefaults()
        NativeAdManager.clearAllCachedAds()
        NativeAdManager.enableCachingNativeAds = true
    }

    @After
    fun tearDown() {
        NativeAdManager.clearAllCachedAds()
        NativeAdManager.enableCachingNativeAds = false
        AdManageKitConfig.resetToDefaults()
    }

    // ==================== Destructive read ====================

    @Test
    fun `getCachedNativeAd is a destructive read`() {
        val ad = newAd()
        NativeAdManager.setCachedNativeAd("unit-a", ad)

        val first = NativeAdManager.getCachedNativeAd("unit-a")
        assertSame(ad, first)

        // Second read must return null: the ad was consumed
        assertNull(NativeAdManager.getCachedNativeAd("unit-a"))
        assertFalse(NativeAdManager.hasCachedAds("unit-a"))
    }

    @Test
    fun `getCachedNativeAd returns most recently cached ad first - LIFO`() {
        val older = newAd()
        val newer = newAd()
        NativeAdManager.setCachedNativeAd("unit-lifo", older)
        NativeAdManager.setCachedNativeAd("unit-lifo", newer)

        assertSame(newer, NativeAdManager.getCachedNativeAd("unit-lifo"))
        assertSame(older, NativeAdManager.getCachedNativeAd("unit-lifo"))
        assertNull(NativeAdManager.getCachedNativeAd("unit-lifo"))
    }

    // ==================== Cache size / presence ====================

    @Test
    fun `getCacheSize and hasCachedAds reflect cache content`() {
        assertEquals(0, NativeAdManager.getCacheSize("unit-size"))
        assertFalse(NativeAdManager.hasCachedAds("unit-size"))

        NativeAdManager.setCachedNativeAd("unit-size", newAd())
        NativeAdManager.setCachedNativeAd("unit-size", newAd())

        assertEquals(2, NativeAdManager.getCacheSize("unit-size"))
        assertTrue(NativeAdManager.hasCachedAds("unit-size"))

        NativeAdManager.getCachedNativeAd("unit-size")
        assertEquals(1, NativeAdManager.getCacheSize("unit-size"))

        NativeAdManager.getCachedNativeAd("unit-size")
        assertEquals(0, NativeAdManager.getCacheSize("unit-size"))
        assertFalse(NativeAdManager.hasCachedAds("unit-size"))
    }

    @Test
    fun `getTotalCacheSize sums across ad units`() {
        NativeAdManager.setCachedNativeAd("unit-t1", newAd())
        NativeAdManager.setCachedNativeAd("unit-t1", newAd())
        NativeAdManager.setCachedNativeAd("unit-t2", newAd())

        assertEquals(3, NativeAdManager.getTotalCacheSize())
    }

    // ==================== LRU eviction ====================

    @Test
    fun `cache evicts least recently used ad when exceeding maxCachedAdsPerUnit`() {
        // Default maxCachedAdsPerUnit is 3
        assertEquals(3, NativeAdManager.maxCachedAdsPerUnit)

        val ad1 = newAd()
        val ad2 = newAd()
        val ad3 = newAd()
        val ad4 = newAd()
        NativeAdManager.setCachedNativeAd("unit-lru", ad1)
        NativeAdManager.setCachedNativeAd("unit-lru", ad2)
        NativeAdManager.setCachedNativeAd("unit-lru", ad3)
        // Fourth insert exceeds the limit: oldest (ad1) is evicted and destroyed
        NativeAdManager.setCachedNativeAd("unit-lru", ad4)

        assertEquals(3, NativeAdManager.getCacheSize("unit-lru"))
        verify(exactly = 1) { ad1.destroy() }
        verify(exactly = 0) { ad2.destroy() }
        verify(exactly = 0) { ad3.destroy() }
        verify(exactly = 0) { ad4.destroy() }
    }

    // ==================== Clearing ====================

    @Test
    fun `clearAllCachedAds destroys every cached ad and empties cache`() {
        val adA = newAd()
        val adB = newAd()
        NativeAdManager.setCachedNativeAd("unit-clear-a", adA)
        NativeAdManager.setCachedNativeAd("unit-clear-b", adB)

        NativeAdManager.clearAllCachedAds()

        verify(exactly = 1) { adA.destroy() }
        verify(exactly = 1) { adB.destroy() }
        assertEquals(0, NativeAdManager.getTotalCacheSize())
        assertNull(NativeAdManager.getCachedNativeAd("unit-clear-a"))
        assertNull(NativeAdManager.getCachedNativeAd("unit-clear-b"))
    }

    @Test
    fun `clearCachedAd destroys only the specified unit's ads`() {
        val targetAd = newAd()
        val otherAd = newAd()
        NativeAdManager.setCachedNativeAd("unit-target", targetAd)
        NativeAdManager.setCachedNativeAd("unit-other", otherAd)

        NativeAdManager.clearCachedAd("unit-target")

        verify(exactly = 1) { targetAd.destroy() }
        verify(exactly = 0) { otherAd.destroy() }
        assertNull(NativeAdManager.getCachedNativeAd("unit-target"))
        assertSame(otherAd, NativeAdManager.getCachedNativeAd("unit-other"))
    }

    // ==================== Enable flag ====================

    @Test
    fun `setCachedNativeAd is a no-op when caching is disabled`() {
        NativeAdManager.enableCachingNativeAds = false

        NativeAdManager.setCachedNativeAd("unit-disabled", newAd())

        assertEquals(0, NativeAdManager.getCacheSize("unit-disabled"))
        assertFalse(NativeAdManager.hasCachedAds("unit-disabled"))
    }

    @Test
    fun `getCachedNativeAd returns null when caching is disabled even if ads were cached`() {
        val ad = newAd()
        NativeAdManager.setCachedNativeAd("unit-toggle", ad)
        assertTrue(NativeAdManager.hasCachedAds("unit-toggle"))

        NativeAdManager.enableCachingNativeAds = false
        assertNull(NativeAdManager.getCachedNativeAd("unit-toggle"))

        // Re-enabling makes the previously cached ad retrievable again
        NativeAdManager.enableCachingNativeAds = true
        assertSame(ad, NativeAdManager.getCachedNativeAd("unit-toggle"))
    }

    // ==================== Fallback reads ====================

    @Test
    fun `fallback serves ad cached under another size variant of the same base unit`() {
        val ad = newAd()
        NativeAdManager.setCachedNativeAd("ca-app-pub-123/456_SMALL", ad)

        // Without the fallback flag: strict miss
        assertNull(NativeAdManager.getCachedNativeAd("ca-app-pub-123/456_MEDIUM"))

        // With fallback: same base ad unit ("ca-app-pub-123/456") matches
        val served = NativeAdManager.getCachedNativeAd(
            "ca-app-pub-123/456_MEDIUM",
            enableFallbackToAnyAd = true
        )
        assertSame(ad, served)

        // Fallback read is destructive too
        assertEquals(0, NativeAdManager.getCacheSize("ca-app-pub-123/456_SMALL"))
    }

    @Test
    fun `fallback does not cross base ad units when cross-unit fallback is disabled`() {
        assertFalse(AdManageKitConfig.enableCrossAdUnitFallback)

        NativeAdManager.setCachedNativeAd("ca-app-pub-AAA/111", newAd())

        assertNull(
            NativeAdManager.getCachedNativeAd("ca-app-pub-BBB/222", enableFallbackToAnyAd = true)
        )
        // The unrelated unit's ad must remain cached
        assertEquals(1, NativeAdManager.getCacheSize("ca-app-pub-AAA/111"))
    }

    @Test
    fun `cross ad unit fallback serves any cached ad when enabled`() {
        AdManageKitConfig.enableCrossAdUnitFallback = true

        val ad = newAd()
        NativeAdManager.setCachedNativeAd("ca-app-pub-AAA/111", ad)

        val served = NativeAdManager.getCachedNativeAd(
            "ca-app-pub-BBB/222",
            enableFallbackToAnyAd = true
        )
        assertSame(ad, served)
        assertEquals(0, NativeAdManager.getTotalCacheSize())
    }
}
