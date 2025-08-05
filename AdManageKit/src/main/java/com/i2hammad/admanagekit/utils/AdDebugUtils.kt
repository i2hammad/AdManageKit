package com.i2hammad.admanagekit.utils

import android.app.Activity
import android.content.Context
import android.widget.Toast
import android.util.Log
import com.i2hammad.admanagekit.config.AdManageKitConfig

/**
 * Debug utilities for AdManageKit library.
 * 
 * Provides comprehensive debugging tools including:
 * - Enhanced logging with filtering
 * - Test ad unit management
 * - Mock ad response injection
 * - Performance monitoring
 * 
 * @since 2.1.0
 */
object AdDebugUtils {
    
    private const val TAG = "AdManageKit"
    private val testAdUnits = mutableMapOf<String, String>()
    private val mockAdResponses = mutableListOf<MockAdResponse>()
    
    /**
     * Data class representing a mock ad response for testing.
     */
    data class MockAdResponse(
        val adUnitId: String,
        val shouldSucceed: Boolean,
        val delayMs: Long = 0,
        val errorCode: Int = 3
    )
    
    /**
     * Shows a debug toast with ad information.
     * 
     * @param context The context to show toast in
     * @param message The message to show
     */
    fun showDebugToast(context: Context, message: String) {
        if (AdManageKitConfig.debugMode) {
            Toast.makeText(context, "AdManageKit: $message", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Enable or disable debug overlay showing real-time ad statistics.
     * 
     * @param activity The activity to show overlay on
     * @param enable Whether to enable the overlay
     */
    fun enableDebugOverlay(activity: Activity, enable: Boolean) {
        if (!AdManageKitConfig.debugMode) {
            Log.w(TAG, "Debug overlay requires AdManageKitConfig.debugMode = true")
            return
        }
        
        AdManageKitConfig.enableDebugOverlay = enable
        
        if (enable) {
            logDebug("DebugOverlay", "Debug overlay enabled for ${activity.javaClass.simpleName}")
        } else {
            logDebug("DebugOverlay", "Debug overlay disabled")
        }
    }
    
    /**
     * Set test ad units to replace production ad units during testing.
     * 
     * @param testUnits Map of production ad unit ID to test ad unit ID
     */
    fun setTestAdUnits(testUnits: Map<String, String>) {
        testAdUnits.clear()
        testAdUnits.putAll(testUnits)
        
        logDebug("TestAdUnits", "Set ${testUnits.size} test ad units")
        testUnits.forEach { (prod, test) ->
            logDebug("TestAdUnits", "  $prod -> $test")
        }
    }
    
    /**
     * Get test ad unit ID if available, otherwise return original.
     * 
     * @param originalAdUnitId Original production ad unit ID
     * @return Test ad unit ID if available, otherwise original
     */
    fun getTestAdUnitId(originalAdUnitId: String): String {
        return if (AdManageKitConfig.testMode && testAdUnits.containsKey(originalAdUnitId)) {
            testAdUnits[originalAdUnitId] ?: originalAdUnitId
        } else {
            originalAdUnitId
        }
    }
    
    /**
     * Inject mock ad responses for unit testing.
     * 
     * @param responses List of mock responses to inject
     */
    fun injectMockAds(responses: List<MockAdResponse>) {
        mockAdResponses.clear()
        mockAdResponses.addAll(responses)
        
        logDebug("MockAds", "Injected ${responses.size} mock ad responses")
        responses.forEach { response ->
            logDebug("MockAds", "  ${response.adUnitId}: success=${response.shouldSucceed}, delay=${response.delayMs}ms")
        }
    }
    
    /**
     * Check if a mock response exists for the given ad unit.
     * 
     * @param adUnitId Ad unit ID to check
     * @return Mock response if available, null otherwise
     */
    fun getMockResponse(adUnitId: String): MockAdResponse? {
        return mockAdResponses.find { it.adUnitId == adUnitId }
    }
    
    /**
     * Logs a debug message.
     * 
     * @param tag The log tag
     * @param message The message to log
     */
    fun logDebug(tag: String, message: String) {
        if (AdManageKitConfig.debugMode) {
            Log.d("$TAG-$tag", message)
        }
    }
    
    /**
     * Enhanced logging for events with success/failure indication.
     * 
     * @param adUnitId Ad unit ID
     * @param event Event name
     * @param message Event message
     * @param isSuccess Whether the event was successful
     */
    fun logEvent(adUnitId: String, event: String, message: String, isSuccess: Boolean) {
        if (AdManageKitConfig.debugMode) {
            val prefix = if (isSuccess) "✅" else "❌"
            val shortAdUnit = adUnitId.takeLast(8) // Show last 8 chars for readability
            Log.d("$TAG-Event", "$prefix [$shortAdUnit] $event: $message")
        }
    }
    
    /**
     * Log performance metrics.
     * 
     * @param adUnitId Ad unit ID
     * @param operation Operation name
     * @param durationMs Duration in milliseconds
     * @param additionalInfo Additional information
     */
    fun logPerformance(adUnitId: String, operation: String, durationMs: Long, additionalInfo: String = "") {
        if (AdManageKitConfig.debugMode && AdManageKitConfig.enablePerformanceMetrics) {
            val shortAdUnit = adUnitId.takeLast(8)
            val info = if (additionalInfo.isNotEmpty()) " - $additionalInfo" else ""
            Log.d("$TAG-Performance", "⏱️ [$shortAdUnit] $operation: ${durationMs}ms$info")
        }
    }
    
    /**
     * Logs an error message.
     * 
     * @param tag The log tag
     * @param message The message to log
     * @param throwable Optional throwable to log
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG-$tag", message, throwable)
    }
    
    /**
     * Export comprehensive debug information.
     * 
     * @return Debug information string
     */
    fun exportDebugInfo(): String {
        return buildString {
            appendLine("=== AdManageKit Debug Information ===")
            appendLine(AdManageKitConfig.getConfigSummary())
            appendLine()
            
            appendLine("Test Ad Units (${testAdUnits.size}):")
            testAdUnits.forEach { (prod, test) ->
                appendLine("  $prod -> $test")
            }
            appendLine()
            
            appendLine("Mock Ad Responses (${mockAdResponses.size}):")
            mockAdResponses.forEach { response ->
                appendLine("  ${response.adUnitId}: success=${response.shouldSucceed}, delay=${response.delayMs}ms")
            }
            appendLine()
            
            appendLine("System Info:")
            appendLine("  Android API: ${android.os.Build.VERSION.SDK_INT}")
            appendLine("  Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("  Available Memory: ${getAvailableMemoryMB()}MB")
            appendLine("  Debug Mode: ${AdManageKitConfig.debugMode}")
            appendLine("  Test Mode: ${AdManageKitConfig.testMode}")
            appendLine("  Production Ready: ${AdManageKitConfig.isProductionReady()}")
        }
    }
    
    /**
     * Get available memory in MB.
     */
    private fun getAvailableMemoryMB(): Long {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val availableMemory = maxMemory - totalMemory + freeMemory
        return availableMemory / (1024 * 1024)
    }
    
    /**
     * Clear all debug data.
     */
    fun clearDebugData() {
        testAdUnits.clear()
        mockAdResponses.clear()
        logDebug("Debug", "Cleared all debug data")
    }
    
    /**
     * Measure execution time of a block of code.
     * 
     * @param operation Operation name for logging
     * @param adUnitId Ad unit ID for context
     * @param block Code block to measure
     * @return Result of the block execution
     */
    inline fun <T> measureTime(operation: String, adUnitId: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        
        logPerformance(adUnitId, operation, duration)
        return result
    }
    
    /**
     * Checks if debug mode is enabled from configuration.
     */
    fun isDebugEnabled(): Boolean {
        return AdManageKitConfig.debugMode
    }
    
    /**
     * Checks if test mode is enabled from configuration.
     */
    fun isTestMode(): Boolean {
        return AdManageKitConfig.testMode
    }
}