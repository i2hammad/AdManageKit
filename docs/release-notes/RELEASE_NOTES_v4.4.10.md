# Release Notes — v4.4.10

**Release Date:** 2026-11-10

## Overview

v4.4.10 is a **bug-fix release**. Every AdMob ad load now waits for `MobileAds.initialize()` to complete instead of crashing. No dependency versions changed from v4.4.9. Apps still need **Kotlin 2.3 or newer**.

---

## Fixed

### Loads before `MobileAds.initialize()` no longer crash

The Google Mobile Ads Next-Gen SDK throws for any request made before it is initialized:

```
java.lang.IllegalStateException: MobileAds.initialize must be called before using the Google Mobile Ads SDK.
    at com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader$Companion.load
    at com.i2hammad.admanagekit.admob.NativeBannerSmall.loadNativeBannerAd
    at com.example.SomeToolActivity.onCreate
```

The app owns initialization and often starts it after the first frame, so a screen opened directly (a launcher shortcut, a Quick Settings tile, a deep link, a Play pre-launch robot) could load its banner first. Only `AppOpenManager` waited for init before; now every AdMob load does, across all native views, `BannerAdView`, `AdManager`, `RewardedAdManager`, `ProgrammaticNativeAdLoader` and the AdMob waterfall providers.

| SDK state when the load starts | What happens |
|---|---|
| Initialized (the usual case) | Loads immediately, exactly as before |
| Still initializing | Held, re-checked every 250 ms, and started as soon as init completes |
| Not initialized within `mobileAdsInitTimeout` | Fails through the normal failure path with `LoadAdError.ErrorCode.TIMEOUT` |
| View destroyed while waiting | Dropped; nothing loads |

Nothing to change in app code. Wrapping ad loads in your own "wait for init" helper still works; it is just no longer required.

## Added

```kotlin
AdManageKitConfig.mobileAdsInitTimeout = 10.seconds // default
```

## Upgrading

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.10'
```

(and the same version for `-core`, `-billing`, `-compose`, `-yandex`)
