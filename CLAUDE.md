# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AdManageKit is an Android library for simplifying Google AdMob ads, Google Play Billing, and User Messaging Platform (UMP) consent management, with optional multi-provider waterfall support (Yandex) and Jetpack Compose wrappers. Modules:

- **AdManageKit** (main module): Ad managers, native ad views, waterfall orchestrators, AdMob providers, UMP consent
- **admanagekit-core**: Zero-dependency core — purchase provider interfaces, multi-provider ad interfaces (`com.i2hammad.admanagekit.core.ad`), provider registry
- **admanagekit-billing**: Google Play Billing Library v8 integration (`AppPurchase`)
- **admanagekit-compose**: Jetpack Compose wrappers for all ad formats
- **admanagekit-yandex**: Yandex Mobile Ads SDK 8 providers for the waterfall
- **app**: Sample application demonstrating library usage; also hosts integration-style unit tests

## Build System

Multi-module Android project using Gradle with Kotlin DSL. Key commands:

```bash
# Compile everything (preferred quick check; plain `build` runs lint)
./gradlew assembleDebug

# Run all unit tests
./gradlew testDebugUnitTest

# Build/test a specific module
./gradlew :AdManageKit:assembleDebug
./gradlew :AdManageKit:testDebugUnitTest
```

Versions are centralized in `gradle/libs.versions.toml`. The published Maven version is set in each module's `build.gradle.kts` publication block (all five modules must stay in sync).

CI (`.github/workflows/ci.yml`) runs `assembleDebug` + `testDebugUnitTest` on JDK 17 for pushes/PRs to main.

## Key Architecture Components

### Core Module (admanagekit-core)
- `BillingConfig`: Service locator for the purchase provider (reference pattern for the library)
- `AppPurchaseProvider` / `NoPurchaseProvider`: Purchase status abstraction
- `core.ad` package: provider interfaces (`InterstitialAdProvider`, `BannerAdProvider`, ...), `AdProviderConfig` (global provider chains), `AdUnitMapping` (placement → per-provider ad unit ids), `AdKitAdError` / `AdKitAdValue`

### Ad Management (AdManageKit)
- `AdManager`: Singleton interstitial manager (time/count-based gating, ad pool, splash flow)
- `AppOpenManager`: Lifecycle-aware app open ads (welcome-back dialog flow included)
- `NativeAdManager`: Native ad cache (per-unit lists, LRU, expiry; destructive reads)
- `NativeAdIntegrationManager`: Screen-aware caching strategies (HYBRID / ONLY_CACHE / ...)
- Views: `BannerAdView`, `NativeBannerSmall/Medium`, `NativeLarge`, `NativeTemplateView` (37 templates); all native views expose `destroy()`
- `waterfall` package: `InterstitialWaterfall` etc. — try providers in order; generation tokens + per-attempt timeout guarantee exactly one terminal callback per load/show
- `AdManageKitConfig`: global mutable config object; `resetToDefaults()` must cover every field (tested)

### Billing Module (admanagekit-billing)
- `AppPurchase`: Billing client wrapper (v8). Entitlement requires `PurchaseState.PURCHASED`; restore paths acknowledge unacknowledged purchases; listener callbacks are main-thread and fire once per init
- Debug behavior is injected via `AppPurchase.setDebugMode(boolean)` (a library cannot read the host app's `BuildConfig.DEBUG`)

### Callback Contracts (load-bearing — preserve when editing)
- `onNextAction()` fires on every skip/failure/dismiss path — callers gate navigation on it; it must fire exactly once per flow
- Waterfall show paths deliver a single terminal event: `onAdFailedToShow` XOR `onAdDismissed`
- Native ad cache reads are destructive: a retrieved ad must be displayed or destroyed by the taker
- Never cache a `NativeAd` that is currently bound to a view (eviction destroys cached ads)

### Firebase Integration
All ad types log Firebase Analytics events for tROAS tracking: `ad_impression`, `ad_paid_event`, `ad_failed_to_load`.

## Development Workflow

### Testing
Unit tests live in `app/src/test` (Robolectric + MockK + hand-rolled fake providers). Robolectric is pinned to SDK 35 via `robolectric.properties` (SDK 36 needs a Java 21 runtime; Gradle runs on 17 to match JitPack). Singletons retain state across tests — every test class must reset config/caches/retries in `@Before`/`@After`.

### Release Process
JitPack distribution; `jitpack.yml` pins OpenJDK 17. Release steps: bump `version = "x.y.z"` in all five module publication blocks, add a `CHANGELOG.md` entry, update README (header, stable-version table, dependency snippets, release-notes index), add `docs/release-notes/RELEASE_NOTES_vX.Y.Z.md`, commit (`fix:/feat: Release vX.Y.Z: ...` style), tag `vX.Y.Z`, push, create the GitHub release. Published artifact ids use the `ad-manage-kit*` naming (see publication blocks), not module directory names.

## Important Implementation Notes

### Purchase Status Integration
All ad components check `BillingConfig.getPurchaseProvider().isPurchased()` to disable ads for purchased users. Purchase-blocked loads fail fast with error code 1001 and must skip retry loops.

### Ad Lifecycle Management
- InterstitialAd: time-based intervals (default 15s) and count-based limits
- AppOpenAd: lifecycle-aware with activity exclusion; 4-hour freshness rule
- Native ads: global caching toggled via `NativeAdManager.enableCachingNativeAds`

### Error Handling
Error code 1001 = purchase-blocked ad request (distinct from AdMob codes). Yandex error codes must be translated to `AdKitAdError` constants, never passed through raw.

## Testing and Development

### Ad Unit IDs
Sample app uses Google's test ad unit IDs. Replace with production IDs before release.

### Billing Testing
Use Google Play Console test tracks. In debug builds call `AppPurchase.setDebugMode(true)` to enable the dev purchase bottom sheet.

### UMP Testing
UMP consent testing requires test device IDs configured in the AdMob dashboard.
