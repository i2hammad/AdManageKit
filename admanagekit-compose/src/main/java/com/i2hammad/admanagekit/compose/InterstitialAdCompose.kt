package com.i2hammad.admanagekit.compose

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.i2hammad.admanagekit.admob.AdManager
import com.i2hammad.admanagekit.admob.AdManagerCallback

/**
 * A Compose effect for managing interstitial ads using AdManager.
 *
 * This composable provides a declarative way to handle interstitial ads in Compose UIs.
 * It automatically handles lifecycle events and provides callbacks for ad events.
 *
 * @param adUnitId The AdMob interstitial ad unit ID
 * @param preloadAd Whether to preload the ad when the effect starts
 * @param onAdShown Callback when the ad is shown successfully
 * @param onAdDismissed Callback when the ad is dismissed or no ad available
 * @param onAdFailedToLoad Callback when the ad fails to load
 *
 * @return A function to show the interstitial ad
 *
 * @since 2.1.0
 */
@Composable
fun rememberInterstitialAd(
    adUnitId: String,
    preloadAd: Boolean = true,
    onAdShown: (() -> Unit)? = null,
    onAdDismissed: (() -> Unit)? = null,
    onAdFailedToLoad: ((String) -> Unit)? = null
): () -> Unit {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Preload the ad when the composable is first created
    LaunchedEffect(adUnitId, preloadAd) {
        if (preloadAd && context is androidx.activity.ComponentActivity) {
            try {
                AdManager.getInstance().loadInterstitialAd(context, adUnitId)
            } catch (e: Exception) {
                onAdFailedToLoad?.invoke("Failed to load ad: ${e.message}")
            }
        }
    }

    // Handle lifecycle events for reloading ads
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && preloadAd) {
                if (context is androidx.activity.ComponentActivity) {
                    // Reload ad if it's not ready when resuming
                    if (!AdManager.getInstance().isReady()) {
                        try {
                            AdManager.getInstance().loadInterstitialAd(context, adUnitId)
                        } catch (e: Exception) {
                            onAdFailedToLoad?.invoke("Failed to reload ad: ${e.message}")
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Return function to show the ad
    return remember(adUnitId) {
        {
            if (context is androidx.activity.ComponentActivity) {
                val callback = object : AdManagerCallback() {
                    override fun onNextAction() {
                        onAdDismissed?.invoke()
                    }
                }

                try {
                    // Show ad based on time interval (respects AdManager's built-in logic)
                    if (AdManager.getInstance().isReady()) {
                        onAdShown?.invoke()
                        AdManager.getInstance().showInterstitialAdByTime(context, callback)
                    } else {
                        onAdDismissed?.invoke()
                    }
                } catch (e: Exception) {
                    onAdFailedToLoad?.invoke("Failed to show ad: ${e.message}")
                    onAdDismissed?.invoke()
                }
            } else {
                onAdDismissed?.invoke()
            }
        }
    }
}

/**
 * A Compose effect for managing interstitial ads with automatic display logic.
 *
 * This version automatically manages when to show interstitial ads based on
 * AdManager's built-in display intervals and user interactions.
 *
 * @param adUnitId The AdMob interstitial ad unit ID
 * @param showMode The mode for showing ads (TIME, COUNT, or FORCE)
 * @param maxDisplayCount Maximum display count (only used with COUNT mode)
 * @param onAdShown Callback when the ad is shown
 * @param onAdDismissed Callback when the ad is dismissed
 * @param onAdFailedToLoad Callback when the ad fails to load
 */
@Composable
fun InterstitialAdEffect(
    adUnitId: String,
    showMode: InterstitialShowMode = InterstitialShowMode.TIME,
    maxDisplayCount: Int = 3,
    onAdShown: (() -> Unit)? = null,
    onAdDismissed: (() -> Unit)? = null,
    onAdFailedToLoad: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    LaunchedEffect(adUnitId, showMode, maxDisplayCount) {
        if (context is androidx.activity.ComponentActivity) {
            val callback = object : AdManagerCallback() {
                override fun onNextAction() {
                    onAdDismissed?.invoke()
                }
            }

            try {
                // Load the ad first
                AdManager.getInstance().loadInterstitialAd(context, adUnitId)

                // Show based on the specified mode
                when (showMode) {
                    InterstitialShowMode.TIME -> {
                        if (AdManager.getInstance().isReady()) {
                            onAdShown?.invoke()
                            AdManager.getInstance().showInterstitialAdByTime(context, callback)
                        } else {
                            onAdDismissed?.invoke()
                        }
                    }
                    InterstitialShowMode.COUNT -> {
                        if (AdManager.getInstance().isReady()) {
                            onAdShown?.invoke()
                            AdManager.getInstance().showInterstitialAdByCount(context, callback, maxDisplayCount)
                        } else {
                            onAdDismissed?.invoke()
                        }
                    }
                    InterstitialShowMode.FORCE -> {
                        if (AdManager.getInstance().isReady()) {
                            onAdShown?.invoke()
                            AdManager.getInstance().forceShowInterstitial(context, callback, true)
                        } else {
                            onAdDismissed?.invoke()
                        }
                    }
                    InterstitialShowMode.FORCE_WITH_DIALOG -> {
                        onAdShown?.invoke()
                        AdManager.getInstance().forceShowInterstitialWithDialog(context, callback)
                    }
                }
            } catch (e: Exception) {
                onAdFailedToLoad?.invoke("Ad operation failed: ${e.message}")
                onAdDismissed?.invoke()
            }
        }
    }
}

/**
 * Enum defining different modes for showing interstitial ads.
 */
enum class InterstitialShowMode {
    /** Show ad based on time interval */
    TIME,
    /** Show ad based on display count */
    COUNT,
    /** Force show ad immediately */
    FORCE,
    /** Force show ad with loading dialog */
    FORCE_WITH_DIALOG
}

/**
 * State holder for interstitial ad management in Compose.
 */
@Stable
class InterstitialAdState(
    private val adUnitId: String,
    private val context: androidx.activity.ComponentActivity
) {
    var isLoaded by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    var isDisplaying by mutableStateOf(false)
        private set

    init {
        checkAdStatus()
    }

    private fun checkAdStatus() {
        isLoaded = AdManager.getInstance().isReady()
        isDisplaying = AdManager.getInstance().isDisplayingAd()
    }

    /**
     * Loads an interstitial ad.
     */
    fun loadAd() {
        if (!isLoading && !isLoaded) {
            isLoading = true
            lastError = null

            try {
                AdManager.getInstance().loadInterstitialAd(context, adUnitId)
                // Note: AdManager doesn't provide load callbacks in current implementation
                // So we simulate the loading state
                isLoading = false
                checkAdStatus()
            } catch (e: Exception) {
                isLoading = false
                lastError = "Failed to load ad: ${e.message}"
                isLoaded = false
            }
        }
    }

    /**
     * Shows the ad using time-based logic.
     */
    fun showAdByTime(
        onShown: (() -> Unit)? = null,
        onDismissed: (() -> Unit)? = null
    ) {
        showAdInternal(InterstitialShowMode.TIME, onShown, onDismissed)
    }

    /**
     * Shows the ad using count-based logic.
     */
    fun showAdByCount(
        maxCount: Int = 3,
        onShown: (() -> Unit)? = null,
        onDismissed: (() -> Unit)? = null
    ) {
        showAdInternal(InterstitialShowMode.COUNT, onShown, onDismissed, maxCount)
    }

    /**
     * Forces the ad to show immediately.
     */
    fun forceShowAd(
        onShown: (() -> Unit)? = null,
        onDismissed: (() -> Unit)? = null
    ) {
        showAdInternal(InterstitialShowMode.FORCE, onShown, onDismissed)
    }

    /**
     * Forces the ad to show with a loading dialog.
     */
    fun forceShowAdWithDialog(
        onShown: (() -> Unit)? = null,
        onDismissed: (() -> Unit)? = null
    ) {
        showAdInternal(InterstitialShowMode.FORCE_WITH_DIALOG, onShown, onDismissed)
    }

    private fun showAdInternal(
        mode: InterstitialShowMode,
        onShown: (() -> Unit)? = null,
        onDismissed: (() -> Unit)? = null,
        maxCount: Int = 3
    ) {
        val callback = object : AdManagerCallback() {
            override fun onNextAction() {
                isDisplaying = false
                checkAdStatus()
                onDismissed?.invoke()
            }
        }

        try {
            checkAdStatus()

            when (mode) {
                InterstitialShowMode.TIME -> {
                    if (isLoaded) {
                        isDisplaying = true
                        onShown?.invoke()
                        AdManager.getInstance().showInterstitialAdByTime(context, callback)
                    } else {
                        onDismissed?.invoke()
                    }
                }
                InterstitialShowMode.COUNT -> {
                    if (isLoaded) {
                        isDisplaying = true
                        onShown?.invoke()
                        AdManager.getInstance().showInterstitialAdByCount(context, callback, maxCount)
                    } else {
                        onDismissed?.invoke()
                    }
                }
                InterstitialShowMode.FORCE -> {
                    if (isLoaded) {
                        isDisplaying = true
                        onShown?.invoke()
                        AdManager.getInstance().forceShowInterstitial(context, callback, true)
                    } else {
                        onDismissed?.invoke()
                    }
                }
                InterstitialShowMode.FORCE_WITH_DIALOG -> {
                    isDisplaying = true
                    onShown?.invoke()
                    AdManager.getInstance().forceShowInterstitialWithDialog(context, callback)
                }
            }
        } catch (e: Exception) {
            lastError = "Failed to show ad: ${e.message}"
            isDisplaying = false
            onDismissed?.invoke()
        }
    }
}

/**
 * Creates and remembers an InterstitialAdState.
 */
@Composable
fun rememberInterstitialAdState(
    adUnitId: String,
    autoLoad: Boolean = true
): InterstitialAdState {
    val context = LocalContext.current

    val state = remember(adUnitId) {
        if (context is androidx.activity.ComponentActivity) {
            InterstitialAdState(adUnitId, context)
        } else {
            throw IllegalStateException("InterstitialAdState requires ComponentActivity context")
        }
    }

    LaunchedEffect(autoLoad) {
        if (autoLoad) {
            state.loadAd()
        }
    }

    return state
}