# Release Notes — v4.4.6

**Release Date:** 2026-09-14

## Overview

v4.4.6 is a **maintenance release**. No API was added, removed or changed, and **no library source file changed** — the entire diff is `gradle/libs.versions.toml`, the five publication blocks, and documentation.

The one change worth reading is a deliberate **downgrade** of WorkManager to **2.9.1**. WorkManager 2.10 and 2.11 call `JobScheduler.forNamespace` on any device reporting `SDK_INT >= 34`; on modified or spoofed ROMs that report API 34 over an older framework the method is absent, and the resulting `NoSuchMethodError` kills the process at startup via `androidx.startup`'s `WorkManagerInitializer`. 2.9.1 is the last release without that call. It also picks up AGP **9.4.0**, Firebase BOM **34.19.0**, Compose BOM **2026.09.00** and Yandex Mobile Ads **8.4.0**.

Upgrading is a drop-in from 4.4.x.

---

## Changed

### WorkManager pinned back to 2.9.1 (a downgrade, on purpose)

`androidx.work:work-runtime` moves **2.11.2 → 2.9.1**. This is the only dependency in the catalog moving backwards, and it is intentional.

**The defect.** WorkManager **2.10 and 2.11** call `android.app.job.JobScheduler.forNamespace` whenever `Build.VERSION.SDK_INT >= 34`. That method genuinely exists on API 34 — but modified and spoofed ROMs that *report* API 34 while running an older framework underneath do not have it. Constructing the WorkManager singleton on such a device throws `NoSuchMethodError`.

That throw is fatal rather than merely inconvenient because `androidx.startup` auto-initializes WorkManager through `WorkManagerInitializer`, which runs from `InitializationProvider` — a `ContentProvider`. `ActivityThread.handleBindApplication` installs content providers **after** `Application.attachBaseContext` but **before** `Application.onCreate`, so there is no user code on the stack to catch it and nothing in the app has run yet. The process dies during bind:

```
Fatal Exception: java.lang.RuntimeException: Unable to get provider
androidx.startup.InitializationProvider: java.lang.NoSuchMethodError:
No virtual method forNamespace(Ljava/lang/String;)Landroid/app/job/JobScheduler;
in class Landroid/app/job/JobScheduler; or its super classes
(declaration of 'android.app.job.JobScheduler' appears in /system/framework/framework.jar)
    at android.app.ActivityThread.installProvider(ActivityThread.java:7467)
    at android.app.ActivityThread.installContentProviders(ActivityThread.java:6973)
    at android.app.ActivityThread.handleBindApplication(ActivityThread.java:6744)
```

In a minified build the `StartupException` between the two appears under its obfuscated name (`y40:` or similar), so match on the `InitializationProvider` + `forNamespace` pair rather than on the middle frame. `declaration ... appears in /system/framework/framework.jar` is the confirmation that the device's framework really is older than the API level it advertises.

**2.9.1 is the last release without that call**, which is why the pin lands exactly there rather than at some newer 2.10.x or 2.11.x patch — the call is present throughout both lines.

**This is a host-device problem, not an ads problem.** No source file in any of AdManageKit's five modules references `androidx.work`. The dependency is declared purely to control which version lands in the graph — the Next-Gen GMA SDK asks for `work-runtime:2.7.0` transitively, and this entry is what decides where that resolves. The downgrade changes nothing about the library's behavior, surface, or minimum requirements; it only moves a pin.

**The pin does not override your app.** It is an `implementation` dependency, so it lands in the published POM at `runtime` scope, and Gradle's conflict resolution still picks the **highest** version across the whole graph. If your app declares 2.10 or 2.11 directly, or depends on anything that does, you resolve that version and you keep the crash. Check what you actually resolve:

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep androidx.work
```

If that shows 2.10.x or 2.11.x and you are seeing `NoSuchMethodError` on `JobScheduler.forNamespace` in your startup crash reports, pin it in the app as well:

```kotlin
dependencies {
    implementation("androidx.work:work-runtime:2.9.1")
}
```

**If you need 2.10+ APIs**, do not downgrade your app to match the library. Take the auto-initializer out of the startup path instead, so a failure is catchable rather than fatal — remove `WorkManagerInitializer` in the manifest and initialize WorkManager yourself inside a guard:

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="merge">
    <meta-data
        android:name="androidx.work.WorkManagerInitializer"
        android:value="androidx.startup"
        tools:node="remove" />
</provider>
```

Then have your `Application` implement `androidx.work.Configuration.Provider`. WorkManager falls back to on-demand initialization at the first `WorkManager.getInstance(context)` call — which happens on your own thread, in your own code, where a `NoSuchMethodError` can be caught and degrade that one feature instead of killing the process. Nothing in AdManageKit depends on which version resolves, or on WorkManager being initialized at all.

### Other dependency updates

- Android Gradle Plugin **9.3.2 → 9.4.0**
- Firebase BOM **34.18.0 → 34.19.0**
- Compose BOM **2026.08.00 → 2026.09.00**
- Yandex Mobile Ads **8.3.0 → 8.4.0**
- Robolectric (test only) **4.16.1 → 4.17**

Unchanged: Google Mobile Ads Next-Gen SDK **1.4.0**, Play Billing **9.1.0**, Kotlin **2.2.10**, UMP **4.0.0**, Compose compiler **1.5.8**.

## Fixed

### A stray leading space in the `workRuntime` catalog entry

The downgraded version was written as `" 2.9.1"` — with a leading space. Gradle does not trim version strings, so it treats that as a distinct and unresolvable coordinate rather than 2.9.1. Caught before tagging; the published 4.4.6 resolves `androidx.work:work-runtime:2.9.1` cleanly, verified against `:AdManageKit:dependencies`.

## Notes

- No behavioral change in any ad, billing, consent or waterfall path. The existing 176 tests pass unchanged
- Robolectric stays pinned to SDK 35 via `robolectric.properties`; the 4.17 bump does not change that
- There is nothing to migrate — upgrading is a one-line version bump from 4.4.5

---

## Upgrading

```gradle
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v4.4.6'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v4.4.6'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v4.4.6'

// For Jetpack Compose support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v4.4.6'

// For Yandex Ads multi-provider support
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-yandex:v4.4.6'
```

No code changes are required.
