package com.i2hammad.admanagekit.waterfall

import android.app.Activity
import android.content.Context
import android.view.View
import com.i2hammad.admanagekit.core.ad.AdKitAdError
import com.i2hammad.admanagekit.core.ad.AdProvider
import com.i2hammad.admanagekit.core.ad.BannerAdProvider
import com.i2hammad.admanagekit.core.ad.InterstitialAdProvider
import com.i2hammad.admanagekit.core.ad.RewardedAdProvider

/**
 * Hand-rolled fake providers for waterfall tests.
 *
 * Each fake records every loadAd/showAd/destroy call and never invokes callbacks
 * on its own; tests trigger callbacks manually via triggerLoadSuccess()/
 * triggerLoadFailure() and by driving the recorded show callbacks. This makes
 * hung providers, late callbacks, and duplicate callbacks trivial to simulate.
 */

fun testError(
    code: Int = AdKitAdError.ERROR_CODE_NO_FILL,
    message: String = "no fill"
): AdKitAdError = AdKitAdError(code, message, "test")

// =================== INTERSTITIAL FAKES ===================

/**
 * Fake interstitial provider that behaves like a SINGLE-SLOT network (e.g. Yandex):
 * it deliberately does NOT override the keyed [InterstitialAdProvider.isAdReady] /
 * [InterstitialAdProvider.showAd] default methods, so keyed calls from the waterfall
 * must fall through to the un-keyed single-slot implementations.
 */
class FakeSingleSlotInterstitialProvider(
    override val provider: AdProvider = AdProvider("fake-single", "Fake Single-Slot")
) : InterstitialAdProvider {

    class LoadCall(val adUnitId: String, val callback: InterstitialAdProvider.InterstitialAdCallback)

    val loadCalls = mutableListOf<LoadCall>()
    val showCallbacks = mutableListOf<InterstitialAdProvider.InterstitialShowCallback>()
    var adReady = false
    var destroyCount = 0

    override fun loadAd(context: Context, adUnitId: String, callback: InterstitialAdProvider.InterstitialAdCallback) {
        loadCalls += LoadCall(adUnitId, callback)
    }

    /** Marks the ad ready, then delivers onAdLoaded for the given recorded load call. */
    fun triggerLoadSuccess(index: Int = loadCalls.lastIndex) {
        adReady = true
        loadCalls[index].callback.onAdLoaded()
    }

    /** Delivers onAdFailedToLoad for the given recorded load call. */
    fun triggerLoadFailure(error: AdKitAdError = testError(), index: Int = loadCalls.lastIndex) {
        loadCalls[index].callback.onAdFailedToLoad(error)
    }

    override fun showAd(activity: Activity, callback: InterstitialAdProvider.InterstitialShowCallback) {
        adReady = false
        showCallbacks += callback
    }

    override fun isAdReady(): Boolean = adReady

    override fun destroy() {
        destroyCount++
    }
}

/**
 * Fake interstitial provider that tracks ads PER AD UNIT (e.g. AdMob):
 * it overrides the keyed isAdReady/showAd methods and records the adUnitId
 * passed to showAd, so tests can assert the waterfall shows the exact ad
 * unit it resolved at load time.
 */
class FakeKeyedInterstitialProvider(
    override val provider: AdProvider = AdProvider("fake-keyed", "Fake Keyed")
) : InterstitialAdProvider {

    class LoadCall(val adUnitId: String, val callback: InterstitialAdProvider.InterstitialAdCallback)

    val loadCalls = mutableListOf<LoadCall>()
    val readyAdUnits = mutableSetOf<String>()
    val shownAdUnitIds = mutableListOf<String>()
    val showCallbacks = mutableListOf<InterstitialAdProvider.InterstitialShowCallback>()

    /** Number of times the un-keyed showAd overload was used (should stay 0). */
    var unkeyedShowCount = 0
    var destroyCount = 0

    override fun loadAd(context: Context, adUnitId: String, callback: InterstitialAdProvider.InterstitialAdCallback) {
        loadCalls += LoadCall(adUnitId, callback)
    }

    fun triggerLoadSuccess(index: Int = loadCalls.lastIndex) {
        readyAdUnits += loadCalls[index].adUnitId
        loadCalls[index].callback.onAdLoaded()
    }

    fun triggerLoadFailure(error: AdKitAdError = testError(), index: Int = loadCalls.lastIndex) {
        loadCalls[index].callback.onAdFailedToLoad(error)
    }

    override fun showAd(activity: Activity, callback: InterstitialAdProvider.InterstitialShowCallback) {
        unkeyedShowCount++
        showCallbacks += callback
    }

    override fun showAd(activity: Activity, adUnitId: String, callback: InterstitialAdProvider.InterstitialShowCallback) {
        shownAdUnitIds += adUnitId
        readyAdUnits -= adUnitId
        showCallbacks += callback
    }

    override fun isAdReady(): Boolean = readyAdUnits.isNotEmpty()

    override fun isAdReady(adUnitId: String): Boolean = adUnitId in readyAdUnits

    override fun destroy() {
        destroyCount++
    }
}

// =================== REWARDED FAKE ===================

/** Fake single-slot rewarded provider, manually-triggered like the interstitial fakes. */
class FakeRewardedProvider(
    override val provider: AdProvider = AdProvider("fake-rewarded", "Fake Rewarded")
) : RewardedAdProvider {

    class LoadCall(val adUnitId: String, val callback: RewardedAdProvider.RewardedAdCallback)

    val loadCalls = mutableListOf<LoadCall>()
    val showCallbacks = mutableListOf<RewardedAdProvider.RewardedShowCallback>()
    var adReady = false
    var destroyCount = 0

    override fun loadAd(context: Context, adUnitId: String, callback: RewardedAdProvider.RewardedAdCallback) {
        loadCalls += LoadCall(adUnitId, callback)
    }

    fun triggerLoadSuccess(index: Int = loadCalls.lastIndex) {
        adReady = true
        loadCalls[index].callback.onAdLoaded()
    }

    fun triggerLoadFailure(error: AdKitAdError = testError(), index: Int = loadCalls.lastIndex) {
        loadCalls[index].callback.onAdFailedToLoad(error)
    }

    override fun showAd(activity: Activity, callback: RewardedAdProvider.RewardedShowCallback) {
        adReady = false
        showCallbacks += callback
    }

    override fun isAdReady(): Boolean = adReady

    override fun destroy() {
        destroyCount++
    }
}

// =================== BANNER FAKE ===================

/** Fake banner provider, manually-triggered. */
class FakeBannerProvider(
    override val provider: AdProvider = AdProvider("fake-banner", "Fake Banner")
) : BannerAdProvider {

    class LoadCall(val context: Context, val adUnitId: String, val callback: BannerAdProvider.BannerAdCallback)

    val loadCalls = mutableListOf<LoadCall>()
    var destroyCount = 0
    var pauseCount = 0
    var resumeCount = 0

    override fun loadBanner(context: Context, adUnitId: String, callback: BannerAdProvider.BannerAdCallback) {
        loadCalls += LoadCall(context, adUnitId, callback)
    }

    fun triggerLoadSuccess(bannerView: View, index: Int = loadCalls.lastIndex) {
        loadCalls[index].callback.onBannerLoaded(bannerView)
    }

    fun triggerLoadFailure(error: AdKitAdError = testError(), index: Int = loadCalls.lastIndex) {
        loadCalls[index].callback.onBannerFailedToLoad(error)
    }

    override fun pause() {
        pauseCount++
    }

    override fun resume() {
        resumeCount++
    }

    override fun destroy() {
        destroyCount++
    }
}

// =================== RECORDING CALLBACKS ===================

/** Records terminal load events delivered by a waterfall. */
class RecordingInterstitialLoadCallback : InterstitialAdProvider.InterstitialAdCallback {
    var loadedCount = 0
    val loadErrors = mutableListOf<AdKitAdError>()

    override fun onAdLoaded() {
        loadedCount++
    }

    override fun onAdFailedToLoad(error: AdKitAdError) {
        loadErrors += error
    }
}

/** Records show lifecycle events delivered by a waterfall. */
class RecordingInterstitialShowCallback : InterstitialAdProvider.InterstitialShowCallback {
    var showedCount = 0
    var dismissedCount = 0
    val showErrors = mutableListOf<AdKitAdError>()

    override fun onAdShowed() {
        showedCount++
    }

    override fun onAdDismissed() {
        dismissedCount++
    }

    override fun onAdFailedToShow(error: AdKitAdError) {
        showErrors += error
    }
}

/** Records terminal load events delivered by a rewarded waterfall. */
class RecordingRewardedLoadCallback : RewardedAdProvider.RewardedAdCallback {
    var loadedCount = 0
    val loadErrors = mutableListOf<AdKitAdError>()

    override fun onAdLoaded() {
        loadedCount++
    }

    override fun onAdFailedToLoad(error: AdKitAdError) {
        loadErrors += error
    }
}

/** Records show lifecycle and reward events delivered by a rewarded waterfall. */
class RecordingRewardedShowCallback : RewardedAdProvider.RewardedShowCallback {
    var showedCount = 0
    var dismissedCount = 0
    val showErrors = mutableListOf<AdKitAdError>()
    val rewards = mutableListOf<Pair<String, Int>>()

    override fun onAdShowed() {
        showedCount++
    }

    override fun onAdDismissed() {
        dismissedCount++
    }

    override fun onAdFailedToShow(error: AdKitAdError) {
        showErrors += error
    }

    override fun onRewardEarned(rewardType: String, rewardAmount: Int) {
        rewards += rewardType to rewardAmount
    }
}

/** Records banner events delivered by a banner waterfall. */
class RecordingBannerCallback : BannerAdProvider.BannerAdCallback {
    val loadedViews = mutableListOf<View>()
    val loadErrors = mutableListOf<AdKitAdError>()

    override fun onBannerLoaded(bannerView: View) {
        loadedViews += bannerView
    }

    override fun onBannerFailedToLoad(error: AdKitAdError) {
        loadErrors += error
    }
}
