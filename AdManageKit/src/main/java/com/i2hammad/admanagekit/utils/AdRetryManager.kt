package com.i2hammad.admanagekit.utils

import android.os.Handler
import android.os.Looper
import com.i2hammad.admanagekit.config.AdManageKitConfig
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
    private val activeRetries = mutableMapOf<String, RetryInfo>()
    
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
        val retryAction: () -> Unit
    )
    
    /**
     * Schedule a retry with exponential backoff delay
     * 
     * @param adUnitId The ad unit ID to retry
     * @param attempt Current attempt number (0-based)
     * @param maxAttempts Maximum number of retry attempts
     * @param retryAction The action to execute when retrying
     */
    fun scheduleRetry(
        adUnitId: String,
        attempt: Int,
        maxAttempts: Int = AdManageKitConfig.maxRetryAttempts,
        retryAction: () -> Unit
    ) {
        if (!AdManageKitConfig.autoRetryFailedAds) {
            AdDebugUtils.logEvent(adUnitId, "retryDisabled", "Automatic retry is disabled in config", false)
            return
        }
        
        if (attempt >= maxAttempts) {
            AdDebugUtils.logEvent(adUnitId, "retryLimitReached", "Maximum retry attempts ($maxAttempts) reached", false)
            return
        }
        
        val delay = calculateRetryDelay(attempt)
        
        AdDebugUtils.logEvent(
            adUnitId, 
            "retryScheduled", 
            "Retry attempt ${attempt + 1}/$maxAttempts scheduled in ${delay}ms", 
            true
        )
        
        val runnable = Runnable {
            try {
                // Remove from active retries when executing
                activeRetries.remove(adUnitId)
                
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
        
        // Store retry info for tracking
        val retryInfo = RetryInfo(adUnitId, attempt, maxAttempts, runnable, retryAction)
        activeRetries[adUnitId] = retryInfo
        
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
        activeRetries[adUnitId]?.let { retryInfo ->
            handler.removeCallbacks(retryInfo.runnable)
            activeRetries.remove(adUnitId)
            
            AdDebugUtils.logEvent(
                adUnitId, 
                "retryCancelled", 
                "Retry cancelled for ad unit", 
                true
            )
        }
    }
    
    /**
     * Cancel all pending retries
     */
    fun cancelAllRetries() {
        activeRetries.values.forEach { retryInfo ->
            handler.removeCallbacks(retryInfo.runnable)
        }
        
        val cancelledCount = activeRetries.size
        activeRetries.clear()
        
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