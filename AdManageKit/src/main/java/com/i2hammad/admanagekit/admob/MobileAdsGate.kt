package com.i2hammad.admanagekit.admob

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
                    else -> mainHandler.postDelayed(this, CHECK_INTERVAL_MS)
                }
            }
        }
        mainHandler.postDelayed(check, CHECK_INTERVAL_MS)
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
