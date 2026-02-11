# Release Notes - v3.3.9

**Release Date**: February 2026

## App Open Ad Callback Improvements

Enhanced `AdManagerCallback` with proper failure and timeout callbacks for app open ads. The welcome dialog is now always dismissed before any callback fires.

### New Callback: `onAdTimedOut()`

New dedicated callback on `AdManagerCallback` for when the ad load times out:

```kotlin
appOpenManager.forceShowAdIfAvailable(activity, object : AdManagerCallback() {
    override fun onNextAction() {
        // Always called after ad flow completes
        navigateNext()
    }

    override fun onFailedToLoad(error: AdKitError?) {
        // Called when ad fails to load (not timeout)
        Log.e("Ads", "Ad failed: ${error?.message}")
    }

    override fun onAdTimedOut() {
        // Called when ad load exceeds timeout duration
        Log.w("Ads", "Ad load timed out")
    }
})
```

### `onFailedToLoad()` Now Called for App Open Ads

Previously, `AdManagerCallback.onFailedToLoad()` was never invoked when app open ads failed to load with a dialog showing. Now it fires correctly (after dialog dismissal) for both AdMob and waterfall paths.

**Callback order on failure:**
1. Dialog dismisses (animated)
2. `onFailedToLoad(error)` fires
3. `onNextAction()` fires

**Callback order on timeout:**
1. Dialog dismisses (animated)
2. `onAdTimedOut()` fires
3. `onNextAction()` fires

## Adaptive Full-Width Banner in Waterfall

Fixed banner ads displaying at 320x50dp (left-aligned) when loaded via the waterfall provider chain. `AdMobBannerProvider` now uses adaptive full-width sizing by default, matching the behavior of the direct `BannerAdView` path.

### What Changed

- **`AdMobBannerProvider`**: Default ad size changed from `AdSize.BANNER` (320x50dp fixed) to adaptive full-width, calculated from the Activity context at load time
- **`AdMobProviderRegistration.create()`**: Default `bannerAdSize` parameter changed from `AdSize.BANNER` to `null` (adaptive)
- **Explicit size still supported**: Pass `AdMobProviderRegistration.create(AdSize.BANNER)` if you need the old fixed-size behavior

### Collapsible Banner Support in Waterfall

`AdMobBannerProvider` now supports collapsible banners when used via the waterfall chain:

- Reads `AdManageKitConfig.enableCollapsibleBannersByDefault` at load time
- `BannerAdView` passes per-call collapsible settings through the waterfall to AdMob providers
- New `collapsible` and `collapsiblePlacement` properties on `AdMobBannerProvider`

---

## Installation

```groovy
// Core modules
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.3.9'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.3.9'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.3.9'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.3.9'

// Yandex provider (optional)
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v3.3.9'
```

## Full Changelog

- **New**: `AdManagerCallback.onAdTimedOut()` callback for ad load timeout events
- **Fix**: `AdManagerCallback.onFailedToLoad()` now fires for app open ad failures (dialog dismissed first)
- **Fix**: Welcome dialog always dismissed before `onFailedToLoad`/`onAdTimedOut`/`onNextAction` callbacks
- **Fix**: Waterfall banner ads now use adaptive full-width sizing instead of fixed 320x50dp
- **Fix**: Collapsible banner settings now passed through waterfall to `AdMobBannerProvider`
- **Change**: `AdMobBannerProvider` default ad size is now adaptive (was `AdSize.BANNER`)
- **Change**: `AdMobProviderRegistration.create()` default banner size is now adaptive
