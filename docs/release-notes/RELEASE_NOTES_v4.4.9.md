# Release Notes — v4.4.9

**Release Date:** 2026-10-21

## Overview

v4.4.9 is a **bug-fix release**. `AdManager.forceShowInterstitialAlways()` is now public, and the build moved to AGP built-in Kotlin. No behavior changed, and no dependency versions changed from v4.4.8. Apps still need **Kotlin 2.3 or newer**.

---

## Fixed

### `forceShowInterstitialAlways()` is now public

The README, the docs and the wiki have described `AdManager.forceShowInterstitialAlways()` since v2.8.0, but both overloads were declared `internal`, so a call from an app failed to compile with a "cannot access ... it is internal" error.

Both overloads are now public, from Kotlin and from Java:

```kotlin
// Always fetches a fresh interstitial (with the loading dialog), ignoring
// AdManageKitConfig.interstitialLoadingStrategy
AdManager.getInstance().forceShowInterstitialAlways(activity, callback)

// Same, with a specific ad unit
AdManager.getInstance().forceShowInterstitialAlways(activity, callback, "ca-app-pub-xxx/yyy")
```

`forceShowInterstitial()` is unchanged: it still follows the global loading strategy (`ON_DEMAND`, `ONLY_CACHE` or `HYBRID`). `InterstitialAdBuilder` already used the "always" path internally, so builder users see no change.

## Changed

### Build: AGP built-in Kotlin and the new DSL

All six modules dropped the `org.jetbrains.kotlin.android` plugin. Kotlin is now compiled by AGP 9's built-in Kotlin support, still on Kotlin 2.4.21. The root build pins the Kotlin Gradle plugin so the built-in compiler doesn't fall back to the Kotlin 2.2 that AGP bundles. The `android.builtInKotlin`, `android.newDsl`, `android.uniquePackageNames` and `android.enableAppCompileTimeRClass` opt-outs were removed from `gradle.properties`.

Published artifacts are unchanged. Compared with v4.4.8, every AAR contains the same entries and classes, and POM dependencies, coordinates and the sources jar all match. The only differences are in Gradle module metadata (variant names now come from AGP) and in the internal Kotlin module name. Neither affects consuming apps.

### README

The [MCP server](../../mcp-server/) section, for using AdManageKit docs and code generation from Claude Code, Cursor and other MCP clients, now sits at the top of the README.

## Notes

- **Verification.** `assembleDebug` and all **176 unit tests** pass. A Kotlin caller and a Java caller of both `forceShowInterstitialAlways` overloads compile from outside the library module. JitPack's publish command, run on JDK 17 into a local repository, produces all five artifacts
- No dependency changed: Next-Gen Ads SDK 1.5.0, Firebase BOM 35.0.2, Kotlin 2.4.21, AGP 9.4.1, Play Billing 9.1.0, UMP 4.0.0, Yandex Mobile Ads 8.6.0

---

## Upgrading

```gradle
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.9'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.9'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.9'

// For Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.9'

// For Yandex Ads multi-provider support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.9'
```

No code changes are required.
