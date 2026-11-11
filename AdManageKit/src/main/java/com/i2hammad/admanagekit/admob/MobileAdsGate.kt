package com.i2hammad.admanagekit.admob

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.i2hammad.admanagekit.config.AdManageKitConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Holds every AdMob load until MobileAds.initialize() has completed.
 *
 * The Next-Gen SDK throws `IllegalStateException: MobileAds.initialize must be called before
 * using the Google Mobile Ads SDK` for any request made before initialization (the legacy SDK
 * initialized itself). The host app owns initialization and often defers it past the first
 * frame, so a screen that loads a banner in onCreate — opened from a shortcut, a tile, a deep
 * link or a pre-launch robot — used to crash. Every load entry point now goes through here.
 *
 * Exactly one of `onReady` / `onTimeout` runs per wait, or neither if it is cancelled.
 * When the SDK is already initialized (the usual case) `onReady` runs synchronously on the
 * calling thread, so the load behaves exactly as before. Otherwise readiness is re-checked on
 * the main looper every [CHECK_INTERVAL_MS], and the outcome runs on the main thread.
 *
 * @since 4.4.10
 */
internal object MobileAdsGate {

    private const val TAG = "MobileAdsGate"

    /** Re-check cadence while waiting for MobileAds.initialize() to complete. */
    internal const val CHECK_INTERVAL_MS = 250L

    /** Slower cadence for [LateInitReload]: nothing is on screen waiting for it. */
    internal const val LATE_CHECK_INTERVAL_MS = 1_000L

    internal const val TIMEOUT_MESSAGE = "MobileAds not initialized within the wait window"

    /** Test seam: the real check is [MobileAds.isInitialized]. */
    @VisibleForTesting
    internal var readinessCheck: () -> Boolean = { MobileAds.isInitialized }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /** A pending wait. Cancelling it means neither callback runs. */
    class Wait internal constructor() {
        @Volatile
        var isCancelled: Boolean = false
            private set

        fun cancel() {
            isCancelled = true
        }
    }

    /**
     * True once ads can be requested. A broken check must never block ads for good, so a
     * check that throws counts as ready (the behaviour before this gate existed).
     */
    fun isReady(): Boolean = try {
        readinessCheck()
    } catch (t: Throwable) {
        Log.w(TAG, "MobileAds readiness check failed, assuming ready: ${t.message}")
        true
    }

    /** The error a timed-out wait reports through the load's normal failure path. */
    fun timeoutError(): LoadAdError =
        LoadAdError(LoadAdError.ErrorCode.TIMEOUT, TIMEOUT_MESSAGE, null)

    /**
     * Runs [onReady] once MobileAds is initialized, or [onTimeout] if that takes longer than
     * [timeoutMillis]. Returns a [Wait] the caller can cancel when its view or screen goes away.
     */
    fun runWhenReady(
        timeoutMillis: Long = AdManageKitConfig.mobileAdsInitTimeout.inWholeMilliseconds,
        onTimeout: () -> Unit,
        checkIntervalMillis: Long = CHECK_INTERVAL_MS,
        onReady: () -> Unit,
    ): Wait {
        val wait = Wait()
        if (isReady()) {
            onReady()
            return wait
        }
        Log.d(TAG, "MobileAds not initialized yet, holding the ad load (up to ${timeoutMillis}ms)")
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        val check = object : Runnable {
            override fun run() {
                if (wait.isCancelled) return
                when {
                    isReady() -> onReady()
                    SystemClock.uptimeMillis() >= deadline -> {
                        Log.w(TAG, "MobileAds not initialized after ${timeoutMillis}ms, giving up this load")
                        onTimeout()
                    }
                    else -> mainHandler.postDelayed(this, checkIntervalMillis)
                }
            }
        }
        mainHandler.postDelayed(check, checkIntervalMillis)
        return wait
    }

    /** Coroutine form of [runWhenReady]: true once ready, false if [timeoutMillis] passes first. */
    suspend fun awaitReady(
        timeoutMillis: Long = AdManageKitConfig.mobileAdsInitTimeout.inWholeMilliseconds,
    ): Boolean {
        if (isReady()) return true
        return withTimeoutOrNull(timeoutMillis) {
            while (!isReady()) delay(CHECK_INTERVAL_MS)
            true
        } ?: false
    }
}

/**
 * Gives an ad view a second chance when MobileAds.initialize() finishes late.
 *
 * [MobileAdsGate] stops waiting after [AdManageKitConfig.mobileAdsInitTimeout] so a screen is
 * never stuck on a shimmer, and the load fails with TIMEOUT. Without this, that slot then stays
 * empty for the life of the screen even if init completes a second later.
 *
 * [wrap] returns a callback that forwards everything unchanged. When a load fails while MobileAds
 * is still not initialized, it also starts watching for init (for up to
 * [AdManageKitConfig.mobileAdsLateLoadWindow]) and runs `reload` once when init completes. The
 * caller therefore sees `onFailedToLoad(TIMEOUT)` first and `onAdLoaded()` later if the second
 * load succeeds. One view owns one instance; [cancel] it on destroy and before each new load.
 *
 * @since 4.4.11
 */
internal class LateInitReload(private val owner: String) {

    private var wait: MobileAdsGate.Wait? = null

    /** True while a reload is scheduled for when init completes. */
    val isPending: Boolean get() = wait?.isCancelled == false

    fun cancel() {
        wait?.cancel()
        wait = null
    }

    fun wrap(context: Context, callback: AdLoadCallback?, reload: () -> Unit): AdLoadCallback =
        object : AdLoadCallback() {
            override fun onFailedToLoad(error: AdKitError?) {
                callback?.onFailedToLoad(error)
                scheduleIfInitPending(context, error, reload)
            }
            override fun onAdLoaded() { callback?.onAdLoaded() }
            override fun onAdClicked() { callback?.onAdClicked() }
            override fun onAdClosed() { callback?.onAdClosed() }
            override fun onAdImpression() { callback?.onAdImpression() }
            override fun onAdOpened() { callback?.onAdOpened() }
            override fun onPaidEvent(adValue: AdKitValue) { callback?.onPaidEvent(adValue) }
            override fun onAdLoadStarted() { callback?.onAdLoadStarted() }
            override fun onAdLoadCancelled() { callback?.onAdLoadCancelled() }
        }

    private fun scheduleIfInitPending(context: Context, error: AdKitError?, reload: () -> Unit) {
        val window = AdManageKitConfig.mobileAdsLateLoadWindow.inWholeMilliseconds
        if (window <= 0 || MobileAdsGate.isReady()) return
        // Premium users fail on purpose; loading again can never show them an ad.
        if (error?.message == AdManager.PURCHASED_APP_ERROR_MESSAGE) return
        if (isHostGone(context)) return

        cancel()
        Log.d(TAG, "$owner: load failed before MobileAds init; will load again once init completes (watching ${window}ms)")
        wait = MobileAdsGate.runWhenReady(
            timeoutMillis = window,
            checkIntervalMillis = MobileAdsGate.LATE_CHECK_INTERVAL_MS,
            onTimeout = {
                wait = null
                Log.w(TAG, "$owner: MobileAds still not initialized after ${window}ms, slot stays empty")
            },
        ) {
            wait = null
            if (isHostGone(context)) return@runWhenReady
            Log.d(TAG, "$owner: MobileAds initialized late, loading the ad now")
            reload()
        }
    }

    private fun isHostGone(context: Context): Boolean {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx.isFinishing || ctx.isDestroyed
            ctx = ctx.baseContext
        }
        return false
    }

    private companion object {
        const val TAG = "MobileAdsGate"
    }
}
