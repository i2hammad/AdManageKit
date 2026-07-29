package com.i2hammad.admanagekit.utils

import android.os.Handler
import android.os.Looper
import com.i2hammad.admanagekit.config.AdManageKitConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlin.math.pow

/**
 * Manages automatic retry logic with exponential backoff for ad loading failures.
 * 
 * Features:
 * - Exponential backoff with configurable base and max delays
 * - Per-ad-unit retry tracking
 * - Automatic retry scheduling
 * - Integration with circuit breaker pattern
 * 
 * Usage:
 * ```kotlin
 * AdRetryManager.getInstance().scheduleRetry(
 *     adUnitId = "your-ad-unit",
 *     attempt = 0,
 *     maxAttempts = 3
 * ) {
 *     // Retry action - load ad again
 *     loadAd()
 * }
 * ```
 * 
 * @since 2.1.0
 */
class AdRetryManager private constructor() {
    
    private val handler = Handler(Looper.getMainLooper())
    private val activeRetries = ConcurrentHashMap<String, RetryInfo>()

    /**
     * Delivers a dropped-retry notification on the main thread, on a later loop turn.
     * Posting rather than invoking inline keeps a handler that reacts by scheduling
     * another retry from re-entering [scheduleRetry] mid-mutation.
     */
    private fun notifyDropped(retryInfo: RetryInfo) {
        val onDropped = retryInfo.onDropped ?: return
        handler.post { onDropped() }
    }
    
    companion object {
        @Volatile
        private var instance: AdRetryManager? = null
        
        fun getInstance(): AdRetryManager {
            return instance ?: synchronized(this) {
                instance ?: AdRetryManager().also { instance = it }
            }
        }
    }
    
    /**
     * Data class to track retry information for each ad unit
     */
    private data class RetryInfo(
        val adUnitId: String,
        val attempt: Int,
        val maxAttempts: Int,
        val runnable: Runnable,
        val retryAction: () -> Unit,
        /** Invoked if this retry is dropped before it runs, so its owner isn't stranded. */
        val onDropped: (() -> Unit)? = null
    )
    
    /**
     * Schedule a retry with exponential backoff delay
     * 
     * Retries are keyed by ad unit id, so scheduling for an id that already has a
     * pending retry replaces it. When a caller depends on its retry to deliver a
     * terminal result (rather than merely warming a cache), it must pass [onDropped]
     * so it is still notified if its retry is replaced or cancelled — otherwise the
     * eviction silently strands that caller's callback.
     *
     * @param adUnitId The ad unit ID to retry
     * @param attempt Current attempt number (0-based)
     * @param maxAttempts Maximum number of retry attempts
     * @param onDropped Invoked (main thread) if this retry is replaced or cancelled
     *        before running, or if it is refused because retries are disabled/exhausted
     * @param retryAction The action to execute when retrying
     */
    @JvmOverloads
    fun scheduleRetry(
        adUnitId: String,
        attempt: Int,
        maxAttempts: Int = AdManageKitConfig.maxRetryAttempts,
        onDropped: (() -> Unit)? = null,
        retryAction: () -> Unit
    ) {
        if (!AdManageKitConfig.autoRetryFailedAds) {
            AdDebugUtils.logEvent(adUnitId, "retryDisabled", "Automatic retry is disabled in config", false)
            onDropped?.let { handler.post(it) }
            return
        }

        if (attempt >= maxAttempts) {
            AdDebugUtils.logEvent(adUnitId, "retryLimitReached", "Maximum retry attempts ($maxAttempts) reached", false)
            onDropped?.let { handler.post(it) }
            return
        }
        
        val delay = calculateRetryDelay(attempt)
        
        AdDebugUtils.logEvent(
            adUnitId, 
            "retryScheduled", 
            "Retry attempt ${attempt + 1}/$maxAttempts scheduled in ${delay}ms", 
            true
        )
        
        var retryInfo: RetryInfo? = null

        val runnable = Runnable {
            try {
                // Remove from active retries only if the entry still belongs to this retry
                // (it may have been replaced by a newer scheduleRetry for the same ad unit)
                if (activeRetries[adUnitId] === retryInfo) {
                    activeRetries.remove(adUnitId)
                }

                AdDebugUtils.logEvent(
                    adUnitId,
                    "retryExecuted",
                    "Executing retry attempt ${attempt + 1}/$maxAttempts",
                    true
                )

                retryAction()
            } catch (e: Exception) {
                AdDebugUtils.logEvent(
                    adUnitId,
                    "retryException",
                    "Retry execution failed: ${e.message}",
                    false
                )
            }
        }

        // Store retry info for tracking, cancelling any previously scheduled retry
        // for the same ad unit so it doesn't execute as a duplicate
        val newRetryInfo = RetryInfo(adUnitId, attempt, maxAttempts, runnable, retryAction, onDropped)
        retryInfo = newRetryInfo
        activeRetries.put(adUnitId, newRetryInfo)?.let { previous ->
            handler.removeCallbacks(previous.runnable)
            AdDebugUtils.logEvent(
                adUnitId,
                "retryReplaced",
                "Cancelled previously scheduled retry before scheduling a new one",
                true
            )
            // The replaced retry will never run. Tell its owner, or a caller waiting on
            // it (e.g. a native ad view holding its shimmer) waits forever.
            notifyDropped(previous)
        }

        // Schedule the retry
        handler.postDelayed(runnable, delay)
    }
    
    /**
     * Calculate retry delay using exponential backoff
     * 
     * @param attempt Current attempt number (0-based)
     * @return Delay in milliseconds
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        if (!AdManageKitConfig.enableExponentialBackoff) {
            return AdManageKitConfig.baseRetryDelay.inWholeMilliseconds
        }
        
        // Exponential backoff: baseDelay * (2^attempt)
        val exponentialDelay = AdManageKitConfig.baseRetryDelay.inWholeMilliseconds * 
                               (2.0.pow(attempt.toDouble())).toLong()
        
        // Cap at maximum delay
        return min(exponentialDelay, AdManageKitConfig.maxRetryDelay.inWholeMilliseconds)
    }
    
    /**
     * Cancel any pending retry for the specified ad unit
     * 
     * @param adUnitId The ad unit ID to cancel retry for
     */
    fun cancelRetry(adUnitId: String) {
        activeRetries.remove(adUnitId)?.let { retryInfo ->
            handler.removeCallbacks(retryInfo.runnable)

            AdDebugUtils.logEvent(
                adUnitId,
                "retryCancelled",
                "Retry cancelled for ad unit",
                true
            )
            // Cancelled retries never run either - notify the owner so it can settle
            notifyDropped(retryInfo)
        }
    }

    /**
     * Cancel all pending retries
     */
    fun cancelAllRetries() {
        val cancelledCount = activeRetries.size
        val dropped = activeRetries.values.toList()
        activeRetries.clear()
        dropped.forEach { retryInfo ->
            handler.removeCallbacks(retryInfo.runnable)
            notifyDropped(retryInfo)
        }
        
        if (cancelledCount > 0) {
            AdDebugUtils.logEvent(
                "", 
                "allRetriesCancelled", 
                "Cancelled $cancelledCount pending retries", 
                true
            )
        }
    }
    
    /**
     * Check if there's an active retry for the specified ad unit
     * 
     * @param adUnitId The ad unit ID to check
     * @return True if there's an active retry
     */
    fun hasActiveRetry(adUnitId: String): Boolean {
        return activeRetries.containsKey(adUnitId)
    }
    
    /**
     * Get the number of active retries
     * 
     * @return Number of active retries
     */
    fun getActiveRetryCount(): Int {
        return activeRetries.size
    }
    
    /**
     * Get a summary of active retries for debugging
     * 
     * @return Map of ad unit ID to retry attempt number
     */
    fun getActiveRetriesSummary(): Map<String, Int> {
        return activeRetries.mapValues { it.value.attempt + 1 }
    }
    
    /**
     * Clear all retry state (useful for testing)
     */
    fun clear() {
        cancelAllRetries()
    }
}