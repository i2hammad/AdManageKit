# Release Notes — v4.4.8

**Release Date:** 2026-10-20

## Overview

v4.4.8 is a **dependency release**. No API was added, removed or changed, and **no library source file changed**. The diff is `gradle/libs.versions.toml`, the six module build scripts, and documentation.

The one change that affects host apps is the **Kotlin requirement**. AdManageKit is now compiled with Kotlin 2.4, and Firebase BOM 35 already ships Kotlin 2.4 metadata. **Your app needs Kotlin 2.3 or newer.**

---

## Changed

### Dependency updates

| Dependency | Before | After |
|---|---|---|
| Google Mobile Ads Next-Gen SDK (`ads-mobile-sdk`) | 1.4.0 | **1.5.0** |
| Firebase BOM | 34.19.0 | **35.0.2** |
| Kotlin (and the Compose compiler plugin) | 2.2.10 | **2.4.21** |
| Android Gradle Plugin | 9.4.0 | **9.4.1** |
| Yandex Mobile Ads | 8.4.0 | **8.6.0** |

Play Billing stays at 9.1.0, UMP at 4.0.0 and WorkManager stays pinned at 2.9.1 (see v4.4.6).

**Next-Gen SDK 1.5.0** adds signal generation and rendering APIs for `SwipeableInterstitialAd` (private beta) and an `excludedAppIds` option on icon ad requests. AdManageKit does not wrap either yet. They're available on the SDK directly.

### Kotlin 2.4 is required by Firebase BOM 35

Firebase BOM 35.0.2 resolves `play-services-measurement` 24.0.0, which is compiled with Kotlin 2.4. On a Kotlin 2.2 compiler the build fails in `compileKotlin`:

```
Module was compiled with an incompatible version of Kotlin.
The binary version of its metadata is 2.4.0, expected version is 2.2.0.
```

Kotlin was raised to 2.4.21 to consume it. A Kotlin compiler can read metadata one minor version ahead, so a consuming app needs **Kotlin 2.3 or newer**. That applies to anything that resolves Firebase BOM 35, with or without AdManageKit.

### Build scripts: `kotlinOptions` → `compilerOptions`

Kotlin 2.4 removed the deprecated `android { kotlinOptions { } }` block. All six modules now set the JVM target through the top-level DSL:

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
```

The bytecode target is unchanged (Java 17), and so is `compileOptions`.

## Notes

- **Verification.** `assembleDebug` and all **176 unit tests** pass. The sample app's minified release build (`:app:assembleRelease`) passes with R8. `:app:analyzeReleaseR8Config` still shows no keep rule tracing back to a project file, so the v4.4.7 empty consumer rules still hold. The minified sample APK's dex is 13.0 MB across 2 files (12.5 MB in v4.4.7); that growth comes from the updated SDKs
- **If your build breaks with "incompatible version of Kotlin"** after upgrading, raise your Kotlin Gradle plugin to 2.3 or newer. If you still use `kotlinOptions { }` in your own build scripts, Kotlin 2.4 needs the `compilerOptions` form shown above
- On-device verification of ad loading against SDK 1.5.0 and Yandex 8.6.0 is recommended before shipping to production

---

## Upgrading

```gradle
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.8'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.8'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.8'

// For Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.8'

// For Yandex Ads multi-provider support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.8'
```

No code changes are required, as long as your app is on Kotlin 2.3 or newer.
