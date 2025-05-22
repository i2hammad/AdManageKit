# Native Ads Caching - AdManageKit v1.3.2

## Overview
The `AdManageKit` library (version `v1.3.2`) provides a robust native ads caching system in the `com.i2hammad.admanagekit.admob` package. This feature enables efficient ad delivery by caching native ads per ad unit ID, with a 1-hour expiration policy and a `useCachedAd` boolean option to choose between cached or new ads. It reduces network requests, improves ad load times, and ensures reliable ad display across `NativeBannerSmall`, `NativeBannerMedium`, and `NativeLarge` ad formats. The caching logic is centrally managed by the `NativeAdManager` class, shipped as part of the library.

**Library Version**: v1.3.2  
**Last Updated**: May 22, 2025

## Features
- **Per-Ad-Unit Caching**: Caches one `NativeAd` per `adUnitId`, supporting multiple ad units with independent caches.
- **1-Hour Ad Expiration**: Cached ads expire after 1 hour (3600 seconds) to ensure freshness, with automatic cleanup of expired ads.
- **Boolean Control**: The `useCachedAd` parameter (default: `false`) allows developers to prioritize cached ads (if valid) or fetch new ones.
- **Fallback Mechanism**: If a new ad fails to load and `useCachedAd` is `false`, a valid cached ad for the same `adUnitId` is served if available.
- **Memory Management**: Automatically destroys old or expired ads to prevent memory leaks.
- **Unified Caching**: `NativeAdManager` ensures consistent caching behavior across all supported ad formats.

## Components
### NativeAdManager
The `NativeAdManager` singleton, included in the `v1.3.2` library, manages ad caching:
- **Key Methods**:
    - `setCachedNativeAd(adUnitId: String, ad: NativeAd)`: Stores a `NativeAd` for the specified `adUnitId` with a timestamp.
    - `getCachedNativeAd(adUnitId: String): NativeAd?`: Retrieves a cached ad if it exists and is not expired.
    - `clearCachedAd(adUnitId: String)`: Removes and destroys the cached ad for a specific `adUnitId`.
    - `clearAllCachedAds()`: Clears and destroys all cached ads.
- **Configuration**:
    - `enableCachingNativeAds: Boolean`: Globally enables or disables caching (default: `true`).
- **Expiration Logic**:
    - Ads older than 3600 seconds are destroyed and removed from the cache when accessed via `getCachedNativeAd`.
- **Storage**:
    - Uses a `MutableMap<String, CachedAd>` where `CachedAd` is an internal data class storing the `NativeAd` and its cache timestamp.

### Ad Classes
The library includes three ad format classes:
- **`NativeBannerSmall`**: For small native banner ads.
- **`NativeBannerMedium`**: For medium native banner ads.
- **`NativeLarge`**: For large native ads with media content.

Each class supports:
- Loading ads with the `useCachedAd` option via `loadNativeBannerAd` (`NativeBannerSmall`, `NativeBannerMedium`) or `loadNativeAds` (`NativeLarge`).
- Displaying cached ads using `displayAd`.
- Fallback to cached ads on load failure.
- Integration with `NativeAdManager` for caching and expiration.

## Integration
### Adding the Library
Add `AdManageKit v1.3.2` to your project via your build system (e.g., Gradle):
```groovy
implementation 'com.i2hammad.admanagekit:admob:1.3.2'
```

Ensure the following dependencies are included in your app:
- Google AdMob SDK
- Firebase Analytics (for event logging)
- Shimmer (for loading placeholders)

### Usage
#### Loading Ads
Use the `loadNativeBannerAd` or `loadNativeAds` method to load ads, specifying whether to use a cached ad.

```kotlin
// Initialize NativeBannerSmall
val nativeBannerSmall = NativeBannerSmall(context)
nativeBannerSmall.loadNativeBannerAd(
    activity = activity,
    adNativeBanner = "your-ad-unit-id",
    useCachedAd = false, // Fetch new ad
    adCallBack = object : AdLoadCallback {
        override fun onAdLoaded() { Log.d("Ad", "Ad loaded") }
        override fun onFailedToLoad(adError: LoadAdError) { Log.d("Ad", "Ad failed: ${adError.message}") }
        override fun onAdImpression() { Log.d("Ad", "Ad impression") }
        override fun onAdClicked() { Log.d("Ad", "Ad clicked") }
        override fun onAdClosed() { Log.d("Ad", "Ad closed") }
        override fun onAdOpened() { Log.d("Ad", "Ad opened") }
    }
)

// Load cached ad if available and not expired
nativeBannerSmall.loadNativeBannerAd(activity, "your-ad-unit-id", useCachedAd = true)
```

#### Configuring Caching
Control caching globally via `NativeAdManager`:

```kotlin
// Enable caching (default)
NativeAdManager.enableCachingNativeAds = true

// Disable caching
NativeAdManager.enableCachingNativeAds = false
```

#### Clearing Cache
Clear cached ads to manage memory:

```kotlin
// Clear cache for a specific ad unit
NativeAdManager.clearCachedAd("your-ad-unit-id")

// Clear all cached ads
NativeAdManager.clearAllCachedAds()
```

### Example: Loading Different Ad Formats
```kotlin
// NativeBannerMedium
val nativeBannerMedium = NativeBannerMedium(context)
nativeBannerMedium.loadNativeBannerAd(activity, "medium-ad-unit-id", useCachedAd = true)

// NativeLarge
val nativeLarge = NativeLarge(context)
nativeLarge.loadNativeAds(activity, "large-ad-unit-id", useCachedAd = false, object : AdLoadCallback {
    override fun onAdLoaded() { Log.d("Ad", "Large Ad loaded") }
    override fun onFailedToLoad(adError: LoadAdError) { Log.d("Ad", "Large Ad failed") }
    override fun onAdImpression() { Log.d("Ad", "Large Ad impression") }
    override fun onAdClicked() { Log.d("Ad", "Large Ad clicked") }
    override fun onAdClosed() { Log.d("Ad", "Large Ad closed") }
    override fun onAdOpened() { Log.d("Ad", "Large Ad opened") }
})
```

## Technical Details
### Caching Workflow
1. **Ad Loading**:
    - If `useCachedAd` is `true` and a valid cached ad exists (via `NativeAdManager.getCachedNativeAd(adUnitId)`), it is displayed using `displayAd`.
    - Otherwise, a new ad is fetched using `AdLoader`.
    - On successful load, the ad is cached via `NativeAdManager.setCachedNativeAd(adUnitId, nativeAd)` if `enableCachingNativeAds` is `true`.
2. **Expiration Check**:
    - `getCachedNativeAd` checks the ad’s age. If older than 3600 seconds, the ad is destroyed, removed from the cache, and `null` is returned.
3. **Fallback Logic**:
    - If a new ad fails to load and `useCachedAd` is `false`, a valid cached ad is served before triggering `onFailedToLoad`.
4. **Memory Management**:
    - Old ads are destroyed when replaced or expired.
    - `clearAllCachedAds` ensures all ads are destroyed during cleanup.

### Key Code Snippets
#### NativeAdManager
```kotlin
package com.i2hammad.admanagekit.admob

object NativeAdManager {
    var enableCachingNativeAds: Boolean = true
    private data class CachedAd(val ad: NativeAd, val cachedTime: Long)
    private val cachedAds: MutableMap<String, CachedAd> = mutableMapOf()

    fun setCachedNativeAd(adUnitId: String, ad: NativeAd) {
        if (enableCachingNativeAds) {
            cachedAds[adUnitId]?.ad?.destroy()
            cachedAds[adUnitId] = CachedAd(ad, System.currentTimeMillis())
        }
    }

    fun getCachedNativeAd(adUnitId: String): NativeAd? {
        if (!enableCachingNativeAds) return null
        val cachedAd = cachedAds[adUnitId] ?: return null
        val adAgeSeconds = (System.currentTimeMillis() - cachedAd.cachedTime) / 1000
        return if (adAgeSeconds <= 3600) cachedAd.ad else {
            cachedAd.ad.destroy()
            cachedAds.remove(adUnitId)
            null
        }
    }
}
```

#### Loading Logic (e.g., NativeBannerSmall)
```kotlin
fun loadAd(context: Context, adUnitId: String, useCachedAd: Boolean, callback: AdLoadCallback?) {
    this.adUnitId = adUnitId
    if (useCachedAd && NativeAdManager.enableCachingNativeAds) {
        val cachedAd = NativeAdManager.getCachedNativeAd(adUnitId)
        if (cachedAd != null) {
            displayAd(cachedAd)
            callback?.onAdLoaded()
            return
        }
    }
    // Proceed to load new ad
}
```

## Best Practices
- **Thread Safety**: For concurrent ad loading, make `NativeAdManager.cachedAds` thread-safe:
  ```kotlin
  private val cachedAds: MutableMap<String, CachedAd> = Collections.synchronizedMap(mutableMapOf())
  ```
- **Lifecycle Management**: Call `NativeAdManager.clearAllCachedAds()` in `Activity.onDestroy` or `Application.onTerminate` to free resources.
- **Testing**:
    - Test ad expiration by waiting past 1 hour.
    - Verify fallback behavior when new ads fail.
    - Test multiple `adUnitId` values for per-ad-unit caching.
    - Toggle `useCachedAd` and `enableCachingNativeAds`.
- **Logging**: Enable verbose logging (via `Log.d`) to debug caching and expiration issues.
- **Ad Provider Compliance**: Check AdMob policies for caching and expiration constraints.

## Limitations
- **Manual Expiration**: The 1-hour expiration is managed manually, as `NativeAd` lacks built-in expiration metadata.
- **Single Ad per Unit**: Only one ad is cached per `adUnitId`. For multiple ads, extend `NativeAdManager`.
- **No Auto-Refresh**: Apps must manually request new ads when needed.

## Dependencies
- **Google AdMob SDK**: For ad loading and rendering.
- **Firebase Analytics**: For logging ad events (impressions, paid events, failures).
- **Shimmer**: For loading placeholders.
- **Project Resources**: Layouts (`layout_native_banner_small`, `layout_native_banner_medium`, `layout_native_large`), `BillingConfig`, `AdManager`.

## Troubleshooting
- **Cached Ad Not Displaying**: Ensure `enableCachingNativeAds` is `true` and the ad is not expired.
- **Memory Leaks**: Call `clearAllCachedAds` during app cleanup.
- **Ad Load Failures**: Verify `adUnitId`, network connectivity, and AdMob configuration.
- **AdChoices Issues**: Ensure `adChoicesView` is defined in layout files and properly handled.

## Future Improvements
- Support for multiple cached ads per `adUnitId`.
- Configurable expiration durations.
- Automatic cache refresh based on app-specific policies.
- Enhanced analytics for cache hit/miss rates.

## References
- [AdMob Native Ads Documentation](https://developers.google.com/admob/android/native/start)
- [Firebase Analytics Documentation](https://firebase.google.com/docs/analytics)
- [AdManageKit v1.3.2 Release Notes](release-notes-v1.3.2.md)