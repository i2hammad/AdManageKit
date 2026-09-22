# Release Notes — v4.4.7

**Release Date:** 2026-09-22

## Overview

v4.4.7 is a **build and packaging release**. No API was added, removed or changed, and **no library source file changed** — the diff is the five modules' ProGuard rule files, their build scripts, `gradle.properties`, the sample app's release build type, and documentation.

The subject is the library's **consumer ProGuard rules**. A consumer rule is not advice: AGP appends it to the R8 configuration of every app that depends on the module, and the app has no way to opt out. `ad-manage-kit-compose` shipped `-keep class androidx.compose.** { *; }`, which meant every consuming app kept the entire Compose runtime un-shrinkable, un-optimizable and un-obfuscatable — including apps that never call a single AdManageKit composable.

All five modules now ship a wired, **deliberately empty** `consumer-rules.pro`. On the sample app the result is **11.99 MB → 7.89 MB** of APK and **21.3 MB → 12.5 MB** of uncompressed dex.

**If your app runs R8, you get this by upgrading.** There is nothing to configure and nothing to migrate.

---

## Changed

### `ad-manage-kit-compose` no longer pins the Compose runtime in consuming apps

The module's `consumer-rules.pro` contained three package-wide keeps. Measured with the AGP 9 R8 configuration analyzer against the sample app, their keep radius was:

| Rule | Classes | Methods | Fields |
|---|---:|---:|---:|
| `-keep class androidx.compose.** { *; }` | 7221 | 57262 | 23964 |
| `-keep class com.google.android.gms.ads.** { *; }` | 97 | 506 | 329 |
| `-keep class com.i2hammad.admanagekit.compose.** { *; }` | 47 | 446 | 168 |

R8 reads each as `DONT_SHRINK, DONT_OPTIMIZE, DONT_OBFUSCATE`. The first alone retained Compose runtime, foundation, UI and Material3 in their entirety — and it also **defeated Compose's own consumer rules**, which deliberately grant R8 latitude it can no longer use once the package is pinned wholesale:

```
-keep,allowobfuscation,allowshrinking class * extends androidx.compose.ui.node.ModifierNodeElement
```

None of the three rules was load-bearing. The module's composables are ordinary Kotlin functions that reach the view layer through `AndroidView` factory lambdas. There is no reflection, no `Class.forName`, no resource-name lookup and no serialization anywhere in the module, so R8 already resolves every edge from the consuming app's own call sites.

### All five modules now ship a wired, empty `consumer-rules.pro`

`ad-manage-kit-core`, `ad-manage-kit-billing` and `ad-manage-kit-yandex` previously declared no `consumerProguardFiles` at all, and `ad-manage-kit`'s file existed but was blank — so the contract was accidental rather than stated. Each file now carries the reasoning for why it holds no rules, so the next person to hit an R8 problem has something to read rather than a blank file to guess at.

No module in the library uses reflection, `Class.forName`, JNI, `Parcelable`, `Serializable` or reflective serialization.

**The ad views need no keep rule.** `BannerAdView`, `NativeBannerSmall`/`Medium`, `NativeLarge` and `NativeTemplateView` are inflated from XML, but AAPT2 already emits the matching rule into the generated `aapt_rules.txt` for exactly the views each app's own layouts reference:

```
-keep class com.i2hammad.admanagekit.admob.BannerAdView { <init>(android.content.Context, android.util.AttributeSet); }
-keep class com.i2hammad.admanagekit.admob.NativeTemplateView { <init>(android.content.Context, android.util.AttributeSet); }
```

That is strictly better than a library rule, which would pin all five views in all apps regardless of use.

**The native templates survive resource shrinking.** All 38 are referenced statically as `R.layout.*` constants from `NativeAdTemplate`, so R8 and the resource shrinker both keep them — along with the `ad_headline` / `ad_media` / `ad_body` / `ad_call_to_action` / `ad_app_icon` ids inside them, which `YandexNativeProvider` resolves by name through `Resources.getIdentifier`. Verified against the shrunk `resources.arsc`.

If you add a new template asset id that is reached **only** by name, it needs a `tools:keep` entry in `res/raw/keep.xml` — that is the one case in this library where shrinking can outrun the code.

### The sample app now builds release with R8 on

`isMinifyEnabled` and `isShrinkResources` were both `false`. That is why none of the above was noticed: R8 had **never run anywhere in this repository**, so the rules shipped to consumers were never once exercised by the project that ships them. They now are, on every release build.

Measured on the sample app, which depends on all five modules, Yandex included:

| | Before | After | |
|---|---:|---:|---:|
| APK | 11.99 MB | **7.89 MB** | −34% |
| dex (uncompressed) | 21.3 MB / 3 files | **12.5 MB / 2 files** | −41% |
| Live classes | 37713 | **29297** | −22% |
| Live methods | 186338 | **131255** | −30% |
| Live fields | 106589 | **76004** | −29% |
| Classes pinned by keep rules | 19307 | **11856** | −39% |

Your own numbers will differ — the sample app is not a typical consumer, and an app that does not depend on `ad-manage-kit-yandex` starts from a very different baseline.

### Two AGP 9 migration flags returned to their defaults

`gradle.properties` carried:

```properties
android.r8.strictFullModeForKeepRules=false
android.r8.optimizedResourceShrinking=false
```

Both were set in **v3.5.6**, the release that moved the project from AGP 8.13.1 to 9.0.1, described in that commit as being "for improved build stability during the transition". Nothing ever exercised them, because minification was off — the transition they were guarding was over before R8 ran for the first time. Both are now left at their AGP 9 defaults: strict full mode stops a member-less `-keep class X` from silently pinning X's members, and optimized resource shrinking lets unreachable resources actually go.

These are project-local build settings and do not affect consuming apps.

### The library modules still publish unminified AARs

`isMinifyEnabled = false` remains set on all five modules, deliberately — a published AAR must keep its public API under its real names or consumers cannot compile against it, and shrinking there would only remove code the consuming app's R8 removes anyway, with far better whole-program information. Each module's `proguard-rules.pro` now says so.

The one in `ad-manage-kit` previously ended with:

```
-keep class * {
    *;
}
```

Inert while minification was off, but a rule that would have disabled shrinking, optimization and obfuscation for the **entire program** the moment anyone switched it on. It is gone.

## Notes

- **Nothing to migrate.** An app that already enables R8 simply builds smaller after the upgrade. An app carrying its own defensive `-keep class com.i2hammad.admanagekit.** { *; }` can delete it — the library never needed it — though leaving it costs only size
- **Verification boundary.** The removals are backed by a full R8 configuration-analyzer pass (no keep rule in the minified sample app now traces back to a project file — only AGP's own `proguard-android-optimize.txt` defaults), by confirming the shrunk `resources.arsc` still carries every native template layout and every dynamically-resolved `ad_*` id, and by the AAPT2-generated constructor keeps being present as described. The existing **176 tests pass unchanged**, but they run on the unminified debug variant and therefore do not cover R8 output. On-device verification of a minified release build is recommended, particularly for Yandex native templates — the only place in the library that resolves resources by name
- **`ad-manage-kit-yandex` remains the dominant cost in a minified build**, and it is not the library's to remove. The Yandex Mobile Ads SDK's own consumer rules pin 5788 distinct classes (5631 under `-keepnames class yads.**` alone), and the AppMetrica tree it pulls in pins a further 2297. Apps using only the AdMob providers avoid all of it by not depending on the module
- No dependency versions changed in this release

### Checking your own build

To see which rules dominate your app's keep radius (AGP 9.3+):

```bash
./gradlew :app:analyzeReleaseR8Config
```

The report lands in `app/build/reports/r8/`. Rules are ranked by how many classes, methods and fields each one pins, with the originating dependency or file named — which is how the Compose rule above was found.

---

## Upgrading

```gradle
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.7'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.7'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.7'

// For Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.7'

// For Yandex Ads multi-provider support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.7'
```

No code changes are required.
