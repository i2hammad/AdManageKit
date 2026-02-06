# Release Notes - v3.3.7

**Release Date**: February 2026

## App Open Ad Improvements & Default Changes

This release improves app open ad UX with welcome dialog on cached ads, adds background prefetching control, and changes default caching behavior.

### New Features

#### Welcome Dialog for Cached App Open Ads

`showCachedAd` now displays the welcome back dialog before showing the cached ad, providing a consistent user experience across all app open ad display paths (ON_DEMAND, ONLY_CACHE, HYBRID).

Previously, cached ads would appear abruptly without any transition. Now all app open ad paths show the welcome dialog first.

#### Background Ad Prefetching (`appOpenFetchFreshAd`)

The `appOpenFetchFreshAd` setting has been repurposed (previously deprecated) to control when app open ads are fetched:

```kotlin
AdManageKitConfig.apply {
    // false (default): Prefetch ad when app goes to background (onStop)
    // Ad is ready immediately when user returns - no loading dialog needed
    appOpenFetchFreshAd = false

    // true: Fetch fresh ad when app comes to foreground (onStart)
    // May show loading dialog while ad loads
    appOpenFetchFreshAd = true
}
```

When `appOpenFetchFreshAd = false`, the library automatically starts loading an app open ad when the app goes to background, so it's ready to show instantly when the user returns.

### Breaking Changes

#### Native Ad Caching Disabled by Default

`NativeAdManager.enableCachingNativeAds` now defaults to `false` (was `true`).

If your app relies on native ad caching, explicitly enable it:

```kotlin
NativeAdManager.enableCachingNativeAds = true
```

#### Auto-Retry Disabled by Default

`AdManageKitConfig.autoRetryFailedAds` now defaults to `false` (was `true`).

If your app relies on automatic retry, explicitly enable it:

```kotlin
AdManageKitConfig.autoRetryFailedAds = true
```

### Configuration Changes

| Setting | Old Default | New Default | Description |
|---------|-------------|-------------|-------------|
| `enableCachingNativeAds` | `true` | `false` | Native ad caching |
| `autoRetryFailedAds` | `true` | `false` | Auto-retry failed ad loads |
| `appOpenFetchFreshAd` | deprecated | `false` | Background prefetch (false) vs foreground fetch (true) |

---

## Installation

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.3.7'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.3.7'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.3.7'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.3.7'
```

## Full Changelog

- Welcome dialog now shown before cached app open ads for consistent UX
- `appOpenFetchFreshAd` un-deprecated and repurposed for background prefetch control
- App open ads prefetch on background (onStop) when `appOpenFetchFreshAd = false`
- `NativeAdManager.enableCachingNativeAds` default changed to `false`
- `AdManageKitConfig.autoRetryFailedAds` default changed to `false`
