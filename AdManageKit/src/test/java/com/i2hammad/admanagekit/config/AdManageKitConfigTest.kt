package com.i2hammad.admanagekit.config

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [AdManageKitConfig] defaults and [AdManageKitConfig.resetToDefaults].
 *
 * Pure JVM test (no Robolectric): the tested paths never touch android.* APIs
 * as long as debugMode stays false (validate() only logs when debugMode = true).
 */
class AdManageKitConfigTest {

    @Before
    fun setUp() {
        AdManageKitConfig.resetToDefaults()
    }

    @After
    fun tearDown() {
        AdManageKitConfig.resetToDefaults()
    }

    @Test
    fun `documented defaults are in place`() {
        // Performance
        assertEquals(15.seconds, AdManageKitConfig.defaultAdTimeout)
        assertEquals(1.hours, AdManageKitConfig.nativeCacheExpiry)
        assertEquals(3, AdManageKitConfig.maxCachedAdsPerUnit)
        assertFalse(AdManageKitConfig.enableCrossAdUnitFallback)

        // Reliability
        assertFalse(AdManageKitConfig.autoRetryFailedAds)
        assertEquals(3, AdManageKitConfig.maxRetryAttempts)
        assertEquals(5, AdManageKitConfig.circuitBreakerThreshold)
        assertEquals(300.seconds, AdManageKitConfig.circuitBreakerResetTimeout)

        // Advanced
        assertFalse(AdManageKitConfig.enableSmartPreloading)
        assertFalse(AdManageKitConfig.enableAdaptiveIntervals)
        assertFalse(AdManageKitConfig.enablePerformanceMetrics)
        assertTrue(AdManageKitConfig.enableAutoCacheCleanup)

        // Debug / testing
        assertFalse(AdManageKitConfig.debugMode)
        assertFalse(AdManageKitConfig.testMode)
        assertTrue(AdManageKitConfig.privacyCompliantMode)
        assertFalse(AdManageKitConfig.enableDebugOverlay)

        // Ad-specific
        assertEquals(15.seconds, AdManageKitConfig.defaultInterstitialInterval)
        assertTrue(AdManageKitConfig.interstitialAutoReload)
        assertTrue(AdManageKitConfig.appOpenAutoReload)
        assertTrue(AdManageKitConfig.rewardedAutoReload)
        assertEquals(60.seconds, AdManageKitConfig.defaultBannerRefreshInterval)
        assertFalse(AdManageKitConfig.enableCollapsibleBannersByDefault)
        assertEquals(CollapsibleBannerPlacement.BOTTOM, AdManageKitConfig.defaultCollapsiblePlacement)
        assertEquals(4.seconds, AdManageKitConfig.appOpenAdTimeout)
        assertEquals(800.milliseconds, AdManageKitConfig.welcomeDialogDismissDelay)
        assertFalse(AdManageKitConfig.appOpenFetchFreshAd)
        assertEquals(4.hours, AdManageKitConfig.appOpenAdFreshnessThreshold)
        assertEquals(0, AdManageKitConfig.welcomeDialogAppIcon)

        // Dialog customization
        assertEquals(0, AdManageKitConfig.dialogBackgroundColor)
        assertEquals(0x80000000.toInt(), AdManageKitConfig.dialogOverlayColor)
        assertEquals(0, AdManageKitConfig.dialogCardBackgroundColor)
        assertNull(AdManageKitConfig.welcomeDialogTitle)
        assertNull(AdManageKitConfig.welcomeDialogSubtitle)
        assertNull(AdManageKitConfig.welcomeDialogFooter)
        assertNull(AdManageKitConfig.loadingDialogTitle)
        assertNull(AdManageKitConfig.loadingDialogSubtitle)

        // Loading strategies
        assertEquals(AdLoadingStrategy.HYBRID, AdManageKitConfig.interstitialLoadingStrategy)
        assertEquals(AdLoadingStrategy.HYBRID, AdManageKitConfig.appOpenLoadingStrategy)
        assertEquals(AdLoadingStrategy.HYBRID, AdManageKitConfig.nativeLoadingStrategy)

        // Native media / video
        assertEquals(NativeMediaAspect.ANY, AdManageKitConfig.defaultNativeMediaAspect)
        assertTrue(AdManageKitConfig.nativeVideoStartMuted)
        assertFalse(AdManageKitConfig.nativeVideoClickToExpand)
        assertFalse(AdManageKitConfig.nativeVideoCustomControls)

        // Cache management — maxCacheMemoryMB default was bug-fixed to 200
        assertEquals(200, AdManageKitConfig.maxCacheMemoryMB)
        assertTrue(AdManageKitConfig.enableLRUEviction)
        assertEquals(30.minutes, AdManageKitConfig.cacheCleanupInterval)

        // Network / retry
        assertTrue(AdManageKitConfig.enableExponentialBackoff)
        assertEquals(1.seconds, AdManageKitConfig.baseRetryDelay)
        assertEquals(30.seconds, AdManageKitConfig.maxRetryDelay)
    }

    @Test
    fun `resetToDefaults restores all modified values`() {
        // Mutate a broad slice of the config
        AdManageKitConfig.apply {
            defaultAdTimeout = 99.seconds
            nativeCacheExpiry = 9.hours
            maxCachedAdsPerUnit = 7
            enableCrossAdUnitFallback = true
            autoRetryFailedAds = true
            maxRetryAttempts = 9
            circuitBreakerThreshold = 1
            circuitBreakerResetTimeout = 1.seconds
            enableSmartPreloading = true
            enableAdaptiveIntervals = true
            enablePerformanceMetrics = true
            enableAutoCacheCleanup = false
            debugMode = true
            testMode = true
            privacyCompliantMode = false
            enableDebugOverlay = true
            defaultInterstitialInterval = 1.seconds
            interstitialAutoReload = false
            appOpenAutoReload = false
            rewardedAutoReload = false
            defaultBannerRefreshInterval = 31.seconds
            enableCollapsibleBannersByDefault = true
            defaultCollapsiblePlacement = CollapsibleBannerPlacement.TOP
            appOpenAdTimeout = 1.seconds
            appOpenFetchFreshAd = true
            welcomeDialogAppIcon = 42
            welcomeDialogDismissDelay = 5.seconds
            dialogBackgroundColor = 0xFFFFFF
            dialogOverlayColor = 0
            dialogCardBackgroundColor = 0x123456
            welcomeDialogTitle = "title"
            welcomeDialogSubtitle = "subtitle"
            welcomeDialogFooter = "footer"
            loadingDialogTitle = "loading"
            loadingDialogSubtitle = "wait"
            interstitialLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
            appOpenLoadingStrategy = AdLoadingStrategy.ON_DEMAND
            nativeLoadingStrategy = AdLoadingStrategy.ONLY_CACHE
            defaultNativeMediaAspect = NativeMediaAspect.PORTRAIT
            nativeVideoStartMuted = false
            nativeVideoClickToExpand = true
            nativeVideoCustomControls = true
            maxCacheMemoryMB = 50
            enableLRUEviction = false
            cacheCleanupInterval = 5.seconds
            enableExponentialBackoff = false
            baseRetryDelay = 10.seconds
            maxRetryDelay = 99.seconds
        }

        AdManageKitConfig.resetToDefaults()

        assertEquals(15.seconds, AdManageKitConfig.defaultAdTimeout)
        assertEquals(1.hours, AdManageKitConfig.nativeCacheExpiry)
        assertEquals(3, AdManageKitConfig.maxCachedAdsPerUnit)
        assertFalse(AdManageKitConfig.enableCrossAdUnitFallback)
        assertFalse(AdManageKitConfig.autoRetryFailedAds)
        assertEquals(3, AdManageKitConfig.maxRetryAttempts)
        assertEquals(5, AdManageKitConfig.circuitBreakerThreshold)
        assertEquals(300.seconds, AdManageKitConfig.circuitBreakerResetTimeout)
        assertFalse(AdManageKitConfig.enableSmartPreloading)
        assertFalse(AdManageKitConfig.enableAdaptiveIntervals)
        assertFalse(AdManageKitConfig.enablePerformanceMetrics)
        assertTrue(AdManageKitConfig.enableAutoCacheCleanup)
        assertFalse(AdManageKitConfig.debugMode)
        assertFalse(AdManageKitConfig.testMode)
        assertTrue(AdManageKitConfig.privacyCompliantMode)
        assertFalse(AdManageKitConfig.enableDebugOverlay)
        assertEquals(15.seconds, AdManageKitConfig.defaultInterstitialInterval)
        assertTrue(AdManageKitConfig.interstitialAutoReload)
        assertTrue(AdManageKitConfig.appOpenAutoReload)
        assertTrue(AdManageKitConfig.rewardedAutoReload)
        assertEquals(60.seconds, AdManageKitConfig.defaultBannerRefreshInterval)
        assertFalse(AdManageKitConfig.enableCollapsibleBannersByDefault)
        assertEquals(CollapsibleBannerPlacement.BOTTOM, AdManageKitConfig.defaultCollapsiblePlacement)
        assertEquals(4.seconds, AdManageKitConfig.appOpenAdTimeout)
        assertFalse(AdManageKitConfig.appOpenFetchFreshAd)
        assertEquals(0, AdManageKitConfig.welcomeDialogAppIcon)
        assertEquals(800.milliseconds, AdManageKitConfig.welcomeDialogDismissDelay)
        assertEquals(0, AdManageKitConfig.dialogBackgroundColor)
        assertEquals(0x80000000.toInt(), AdManageKitConfig.dialogOverlayColor)
        assertEquals(0, AdManageKitConfig.dialogCardBackgroundColor)
        assertNull(AdManageKitConfig.welcomeDialogTitle)
        assertNull(AdManageKitConfig.welcomeDialogSubtitle)
        assertNull(AdManageKitConfig.welcomeDialogFooter)
        assertNull(AdManageKitConfig.loadingDialogTitle)
        assertNull(AdManageKitConfig.loadingDialogSubtitle)
        assertEquals(AdLoadingStrategy.HYBRID, AdManageKitConfig.interstitialLoadingStrategy)
        assertEquals(AdLoadingStrategy.HYBRID, AdManageKitConfig.appOpenLoadingStrategy)
        assertEquals(AdLoadingStrategy.HYBRID, AdManageKitConfig.nativeLoadingStrategy)
        assertEquals(NativeMediaAspect.ANY, AdManageKitConfig.defaultNativeMediaAspect)
        assertTrue(AdManageKitConfig.nativeVideoStartMuted)
        assertFalse(AdManageKitConfig.nativeVideoClickToExpand)
        assertFalse(AdManageKitConfig.nativeVideoCustomControls)
        // The bug fix: reset must restore maxCacheMemoryMB to 200
        assertEquals(200, AdManageKitConfig.maxCacheMemoryMB)
        assertTrue(AdManageKitConfig.enableLRUEviction)
        assertEquals(30.minutes, AdManageKitConfig.cacheCleanupInterval)
        assertTrue(AdManageKitConfig.enableExponentialBackoff)
        assertEquals(1.seconds, AdManageKitConfig.baseRetryDelay)
        assertEquals(30.seconds, AdManageKitConfig.maxRetryDelay)
    }

    @Test
    fun `validate passes with default configuration`() {
        assertTrue(AdManageKitConfig.validate())
    }

    @Test
    fun `validate fails for out-of-range values`() {
        // debugMode stays false so validate() never calls android.util.Log
        AdManageKitConfig.maxRetryAttempts = 11
        assertFalse(AdManageKitConfig.validate())
        AdManageKitConfig.resetToDefaults()

        AdManageKitConfig.maxCachedAdsPerUnit = 0
        assertFalse(AdManageKitConfig.validate())
        AdManageKitConfig.resetToDefaults()

        AdManageKitConfig.defaultBannerRefreshInterval = 10.seconds
        assertFalse(AdManageKitConfig.validate())
        AdManageKitConfig.resetToDefaults()

        AdManageKitConfig.maxCacheMemoryMB = 201
        assertFalse(AdManageKitConfig.validate())
    }

    @Test
    fun `default configuration is production ready`() {
        assertTrue(AdManageKitConfig.isProductionReady())
    }

    @Test
    fun `test mode is not production ready`() {
        AdManageKitConfig.testMode = true
        assertFalse(AdManageKitConfig.isProductionReady())
    }
}
