package com.i2hammad.admanagekit.sample

import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.AdUnitMapping
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [AdUnitMapping] placement-to-ad-unit registration and lookup.
 * Pure JVM test (no Android types involved).
 */
class AdUnitMappingTest {

    @Before
    fun setUp() {
        AdUnitMapping.clear()
    }

    @After
    fun tearDown() {
        AdUnitMapping.clear()
    }

    @Test
    fun `register and getAdUnitId roundtrip by provider and by name`() {
        AdUnitMapping.register(
            "interstitial_main", mapOf(
                "admob" to "ca-app-pub-xxx/yyy",
                "yandex" to "R-M-12345-67"
            )
        )

        assertEquals("ca-app-pub-xxx/yyy", AdUnitMapping.getAdUnitId("interstitial_main", AdProvider.ADMOB))
        assertEquals("R-M-12345-67", AdUnitMapping.getAdUnitId("interstitial_main", AdProvider.YANDEX))
        assertEquals("ca-app-pub-xxx/yyy", AdUnitMapping.getAdUnitId("interstitial_main", "admob"))
        assertEquals("R-M-12345-67", AdUnitMapping.getAdUnitId("interstitial_main", "yandex"))
    }

    @Test
    fun `unknown placement returns null`() {
        assertNull(AdUnitMapping.getAdUnitId("never_registered", AdProvider.ADMOB))
        assertNull(AdUnitMapping.getAdUnitId("never_registered", "admob"))
    }

    @Test
    fun `unknown provider for known placement returns null`() {
        AdUnitMapping.register("banner_home", mapOf("admob" to "ca-app-pub-xxx/banner"))

        assertNull(AdUnitMapping.getAdUnitId("banner_home", AdProvider.YANDEX))
        assertNull(AdUnitMapping.getAdUnitId("banner_home", "unknown_provider"))
    }

    @Test
    fun `re-register overwrites existing provider mapping`() {
        AdUnitMapping.register("native_feed", mapOf("admob" to "old-id"))
        AdUnitMapping.register("native_feed", mapOf("admob" to "new-id"))

        assertEquals("new-id", AdUnitMapping.getAdUnitId("native_feed", AdProvider.ADMOB))
    }

    @Test
    fun `re-register merges new providers without dropping existing ones`() {
        AdUnitMapping.register("native_feed", mapOf("admob" to "admob-id"))
        AdUnitMapping.register("native_feed", mapOf("yandex" to "yandex-id"))

        assertEquals("admob-id", AdUnitMapping.getAdUnitId("native_feed", AdProvider.ADMOB))
        assertEquals("yandex-id", AdUnitMapping.getAdUnitId("native_feed", AdProvider.YANDEX))
    }

    @Test
    fun `getRegisteredPlacements returns all logical names`() {
        AdUnitMapping.register("placement_a", mapOf("admob" to "a"))
        AdUnitMapping.register("placement_b", mapOf("yandex" to "b"))

        assertEquals(setOf("placement_a", "placement_b"), AdUnitMapping.getRegisteredPlacements())
    }

    @Test
    fun `remove deletes a single placement`() {
        AdUnitMapping.register("placement_a", mapOf("admob" to "a"))
        AdUnitMapping.register("placement_b", mapOf("admob" to "b"))

        AdUnitMapping.remove("placement_a")

        assertNull(AdUnitMapping.getAdUnitId("placement_a", AdProvider.ADMOB))
        assertEquals("b", AdUnitMapping.getAdUnitId("placement_b", AdProvider.ADMOB))
        assertEquals(setOf("placement_b"), AdUnitMapping.getRegisteredPlacements())
    }

    @Test
    fun `clear removes all placements`() {
        AdUnitMapping.register("placement_a", mapOf("admob" to "a"))
        AdUnitMapping.register("placement_b", mapOf("yandex" to "b"))

        AdUnitMapping.clear()

        assertTrue(AdUnitMapping.getRegisteredPlacements().isEmpty())
        assertNull(AdUnitMapping.getAdUnitId("placement_a", AdProvider.ADMOB))
    }
}
