# Release Notes - AdManageKit v2.5.0

**Release Date:** October 31, 2025  
**Version:** 2.5.0  
**Compatibility:** Android API 21+ | Google Play Billing Library 8.x | Jetpack Compose 2024.12.01

---

## 🚀 What's New in v2.5.0

### 🛡️ App Open Stability Overhaul
- **Thread-Safe Lifecycle:** `AppOpenManager` now relies on `AtomicBoolean`, `AtomicInteger`, and `WeakReference` state to prevent race conditions and memory leaks when activities rotate or background.
- **Smarter Activity Exclusion:** Adds a fast-path cache for excluded activities and uses `cleanup()` to unregister lifecycle callbacks when the manager is no longer needed.
- **Force & Conditional Showing:** `forceShowAdIfAvailable()` and the new `canShowAd()` helper guard against duplicate displays and return actionable reasons when an ad cannot be shown.
- **Lifecycle-Ready Cleanup:** `cleanup()` resets cached state, cancels pending timeouts, clears load metrics, and unregisters callbacks to avoid leaks on process shutdown.

### 📡 Intelligent Retry & Circuit Breaking
- **Exponential Backoff:** `fetchAdWithRetry()` automatically retries network and no-fill failures using configurable exponential backoff (`updateRetryConfiguration`).
- **Circuit Breaker Integration:** Tracks consecutive failures, opens a circuit breaker based on `AdManageKitConfig.circuitBreakerThreshold`, and auto-resets after the configured cooldown.
- **Load Analytics:** `getPerformanceMetrics()` surfaces average load time, failure counts, and current breaker state for dashboards or support tooling.
- **Robust Timeout Handling:** All pending timeouts are tracked and cancelled safely, ensuring callbacks never fire after a manager is cleaned up.

### 🧠 Smarter Native Ad Fallback
- **Same-Unit Cache Fallback:** `NativeAdManager.getCachedNativeAd()` now reuses cached ads that were fetched with the same ad unit ID (including screen-specific suffixes) when the primary cache bucket is empty.
- **Automatic Adoption:** `NativeBannerSmall`, `NativeBannerMedium`, and `NativeLarge` opt into the safe fallback path, improving cache hit rates after connectivity hiccups without crossing placements.
- **Analytics Visibility:** Fallback serves are logged with detailed analytics, making it easy to observe cache performance in Firebase.

### 💳 Billing & Sample Improvements
- **Billing Control Surface:** `AppPurchase` exposes `isBillingAvailable` and `connectToGooglePlayBilling()` so apps can manually reconnect or surface billing state.
- **Sample App Hardening:** `SplashActivity` now checks `AppOpenManager.isShowingAd()` before navigating to prevent duplicate navigation while app open ads display.
- **Documentation Fixes:** Updated README examples align `onProductPurchased` with the real callback signature.

---

## 📦 Installation

### Gradle Dependencies (Kotlin DSL)
```kotlin
implementation("com.github.i2hammad.AdManageKit:ad-manage-kit:v2.5.0")
implementation("com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.5.0")
implementation("com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.5.0")
implementation("com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.5.0")
```

### Gradle Dependencies (Groovy)
```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v2.5.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v2.5.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v2.5.0'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v2.5.0'
```

---

## 🔄 Migration from v2.3.0

- **Drop-In Upgrade:** No breaking API changes. Existing integrations continue to work after bumping the dependency version.
- **Optional Tuning:** Call `AppOpenManager.updateRetryConfiguration()` if you want to customize retry counts, base delay, or multipliers.
- **Manual Cleanup:** If you create multiple `AppOpenManager` instances, invoke `cleanup()` when an instance is no longer needed to free lifecycle callbacks promptly.
- **Native Cache Fallback:** The new same-unit fallback behaviour is on by default for bundled widgets; you can opt out by passing `useCachedAd = false` or disabling caching via `AdManageKitConfig`.

---

## 🧪 Testing Recommendations

1. **Cold Start Flow:** Launch from a cold start repeatedly to verify app open ads respect the circuit breaker and never double-display.
2. **Network Failure Drills:** Simulate offline/no-fill scenarios to confirm exponential backoff scheduling and circuit breaker logging.
3. **Native Cache Coverage:** Toggle airplane mode after initial load to ensure fallback cached ads render across banners, medium, and large placements.
4. **Billing Reconnections:** Force-stop Google Play services or revoke billing to validate manual `connectToGooglePlayBilling()` recovery paths.

---

AdManageKit v2.5.0 focuses on resiliency: tougher app open delivery, smarter native caching, and clearer billing controls so monetization keeps flowing even under adverse conditions.
