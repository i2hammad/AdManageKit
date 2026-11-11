# Release Notes — v4.4.12

**Release Date:** 2026-11-11

## Overview

v4.4.12 is a **dependency release**. It moves to **Google Mobile Ads Next-Gen SDK 1.6.0**. No AdManageKit API or behaviour changed, and no library source file changed. Apps still need **Kotlin 2.3 or newer**.

---

## Changed

| Dependency | Before | After |
|---|---|---|
| Google Mobile Ads Next-Gen SDK (`ads-mobile-sdk`) | 1.5.0 | **1.6.0** |
| Firebase BOM | 35.0.2 | **35.1.0** |
| Android Gradle Plugin (build only) | 9.4.1 | **9.5.1** |
| Dokka (docs only) | 2.2.0 | **2.3.0** |
| MockK (tests only) | 1.14.11 | **1.14.12** |

Play Billing stays at 9.1.0, UMP at 4.0.0, Yandex Mobile Ads at 8.6.0 and WorkManager stays pinned at 2.9.1 (see v4.4.6).

### What Next-Gen SDK 1.6.0 brings

Google lists no breaking changes, deprecations or new minimum requirements for 1.6.0 (released 2026-10-22). The fixes that reach apps using AdManageKit:

- **R8 full mode:** a `RuntimeException` crash in `androidx.work.WorkManagerInitializer` is fixed by updated consumer ProGuard rules.
- **Rewarded ads:** `onUserEarnedReward()` could fire after `onAdDismissedFullScreenContent()`. It now arrives first, so code that grants the reward on dismiss sees it in time.
- **Crash fix:** an `IllegalStateException` is fixed by updating `com.google.android.play:hsdp` to 2.2.0.
- **Mediation:** mediated full-screen ads now pass an available `Activity` context to third-party SDKs.

It also improves the ad inspector's share sheet.

## Verified

- `assembleDebug` for all modules and the sample app; all 194 unit tests pass.
- On a Galaxy A71 5G (Android 13), the sample app's native banner, banner and interstitial ads load and show on SDK 1.6.0.

## Upgrading

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.12'
```

(and the same version for `-core`, `-billing`, `-compose`, `-yandex`)
