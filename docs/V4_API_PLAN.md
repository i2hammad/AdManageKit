# AdManageKit v4.0.0 API Plan

Status: **draft / planning** — no code in this repo implements anything below yet.

This document proposes the breaking API changes for v4.0.0. Every proposal is grounded in
bugs we actually shipped and then fixed in the 3.5.8 audit (issues #2–#35, plus the still-open
#36): the recurring theme is that the 3.x API *allows* the bug classes. v4 should make them
unrepresentable instead of patching them one at a time.

Effort scale: **S** = days, **M** = 1–2 weeks, **L** = 3+ weeks (single maintainer).

| # | Proposal | Effort | Breaks |
|---|----------|--------|--------|
| 1 | Immutable `AdManageKitConfig` snapshot | M | Config mutation after init |
| 2 | Billing rewrite: Connection / EntitlementStore / PurchaseFlow | L | Direct `AppPurchase` field access |
| 3 | `AdProviderConfig` holds factories, not instances | S | Provider registration call sites |
| 4 | Single terminal event: sealed `AdFlowResult` | M | `AdManagerCallback` subclasses |
| 5 | One native cache with check-out/check-in ownership | M | `NativeAdManager`/`NativeAdIntegrationManager` direct use |
| 6 | Smaller items (AppOpenManager split, Kotlin migration, JDK 21, Robolectric) | S–M each | Mostly internal |

---

## 1. Immutable configuration

### Rationale

`AdManageKitConfig` (`AdManageKit/src/main/java/com/i2hammad/admanagekit/config/AdManageKitConfig.kt`)
is a global `object` with **~44 independent mutable `var`s**, readable and writable from any
thread at any time. Concrete costs we have already paid:

- **`resetToDefaults()` drift** — issue #36 item 2 / fixed in 3.5.8: the hand-maintained
  reset list restored `maxCacheMemoryMB = 50` while the declared default was 200, and forgot
  `appOpenAdFreshnessThreshold` entirely. A 500-line `resetToDefaults()` + `validate()` +
  `getConfigSummary()` triplet that re-lists every field by hand *will* drift again on the
  next field addition.
- **Invalid values crash at use time, not set time** — 3.5.8 fixed an init crash in
  `NativeAdManager` when `cacheCleanupInterval` was under 1 minute (truncated to 0, then
  `scheduleWithFixedDelay(..., 0, 0, MINUTES)` threw). `validate()` exists but only *logs*,
  only when `debugMode` is on, and nothing forces anyone to call it.
- **Mid-flight mutation** — managers read config lazily (`NativeAdManager.cacheExpiryMs`
  is a `get()` against the live object), so flipping `nativeLoadingStrategy` or
  `maxCachedAdsPerUnit` while a load is in flight changes behavior halfway through a flow.
- **Dead/legacy fields linger** — `enableWelcomeBackDialog` is `@Deprecated` with "has no
  effect", `enableAdaptiveIntervals` gates two identical branches in `AdManager.canShowAd()`.
  A versioned immutable config is the natural point to drop them.

### New API

```kotlin
// One-time, validated, immutable snapshot. Throws IllegalArgumentException on bad values.
AdManageKit.initialize(
    context,
    AdManageKitConfig(
        debugMode = BuildConfig.DEBUG,
        retry = RetryConfig(autoRetry = true, maxAttempts = 3),
        interstitial = InterstitialConfig(
            interval = 15.seconds,
            loadingStrategy = AdLoadingStrategy.HYBRID,
        ),
        appOpen = AppOpenConfig(
            timeout = 4.seconds,
            welcomeDialog = WelcomeDialogConfig(appIcon = R.mipmap.ic_launcher),
        ),
    )
)

// Read-only access everywhere else:
val config: AdManageKitConfig = AdManageKit.config
```

`AdManageKitConfig` becomes a `data class`; `resetToDefaults()` is deleted (defaults *are*
the constructor defaults — drift is impossible), `validate()` moves into `init {}` and
throws, `getConfigSummary()` becomes `toString()` for free.

### Field grouping (current flat fields → nested configs)

| Nested config | Current fields |
|---|---|
| `InterstitialConfig` | `defaultInterstitialInterval`, `interstitialAutoReload`, `interstitialLoadingStrategy`, `loadingDialogTitle`, `loadingDialogSubtitle` |
| `AppOpenConfig` | `appOpenAutoReload`, `appOpenAdTimeout`, `appOpenLoadingStrategy`, `appOpenFetchFreshAd`, `appOpenAdFreshnessThreshold` |
| `WelcomeDialogConfig` (inside `AppOpenConfig`) | `welcomeDialogAppIcon`, `welcomeDialogTitle`, `welcomeDialogSubtitle`, `welcomeDialogFooter`, `welcomeDialogDismissDelay` |
| `BannerConfig` | `defaultBannerRefreshInterval`, `enableCollapsibleBannersByDefault`, `defaultCollapsiblePlacement` |
| `NativeConfig` | `nativeCacheExpiry`, `maxCachedAdsPerUnit`, `enableCrossAdUnitFallback`, `nativeLoadingStrategy`, `maxCacheMemoryMB`, `enableLRUEviction`, `cacheCleanupInterval`, `enableAutoCacheCleanup`, `enableSmartPreloading` |
| `RewardedConfig` | `rewardedAutoReload` |
| `DialogStyleConfig` | `dialogBackgroundColor`, `dialogOverlayColor`, `dialogCardBackgroundColor` |
| `RetryConfig` | `autoRetryFailedAds`, `maxRetryAttempts`, `enableExponentialBackoff`, `baseRetryDelay`, `maxRetryDelay`, `circuitBreakerThreshold`, `circuitBreakerResetTimeout` |
| Top level | `debugMode`, `testMode`, `enableDebugOverlay`, `privacyCompliantMode`, `enablePerformanceMetrics`, `defaultAdTimeout` |
| **Dropped in v4** | `enableWelcomeBackDialog` (no-op since 3.3.7), `enableAdaptiveIntervals` (never implemented — both branches of `canShowAd()` identical) |

### Migration

- Keep the current `object AdManageKitConfig` as a **deprecated mutable facade** for one
  release cycle: each `var` setter writes into a builder that `AdManageKit.initialize()`
  (no-arg overload) snapshots. `@Deprecated(ReplaceWith(...))` on every field pointing at the
  nested path.
- Breaks: code that mutates config *after* startup (e.g. toggling `nativeLoadingStrategy`
  per screen). Replacement: the per-call overrides that already exist
  (`loadNativeAdWithCaching(loadingStrategy = ...)`, `InterstitialAdBuilder.autoReload()`)
  become the only supported way to vary behavior at runtime — which is what the docs
  recommend today anyway.

**Effort: M** — the config class itself is S; chasing the ~150 read sites and adding the
facade is the bulk.

---

## 2. Billing rewrite in Kotlin

### Rationale

`AppPurchase.java` is **2,034 lines** holding *four* responsibilities behind one singleton:
BillingClient connection lifecycle, product-details queries, the purchase/subscription flow
(including the dev bottom sheet), and entitlement state (`isPurchased`, `purchaseResultList`,
`stringList`, `idPurchased`, ...) — plus two `public` mutable fields (`isBillingAvailable`,
`isBillingInitialized`) that anyone can corrupt.

The 3.5.8 audit fixed **seven** billing bugs that are all symptoms of this structure:

- Restored purchases never re-acknowledged → Google auto-refunded after 3 days (state logic
  buried inside three different query paths: `verifyPurchased()`, `updatePurchaseStatus()`,
  `refreshPurchases()`).
- PENDING purchases granted entitlement (no single place that decides "what is owned").
- Refunds/expiry never cleared `isPurchased` in-session; consumables flipped the global flag.
- `onInitBillingFinished` fired up to 3× and on arbitrary threads (connection lifecycle and
  listener bookkeeping interleaved with everything else).
- `PurchaseResult.productType` unset → `getSubscriptionState()` always `NOT_SUBSCRIPTION`.
- Dead `BuildConfig.DEBUG` branches from a wrong import (now `setDebugMode(boolean)`).
- None of it is unit-testable without a device — the 3.5.8 test suite could only cover
  `PurchaseResult`.

### New API (module `admanagekit-billing`, Kotlin)

```kotlin
// 1. Connection: owns BillingClient, reconnection, init-finished (exactly once, main thread).
class BillingConnection(app: Application, products: List<PurchaseItem>) {
    val state: StateFlow<ConnectionState>           // Disconnected / Connecting / Ready(client)
    suspend fun queryPurchases(): List<Purchase>    // both INAPP + SUBS
    suspend fun queryProductDetails(): Map<String, ProductDetails>
}

// 2. Entitlements: PURE — no BillingClient import. Unit-testable on the JVM.
class EntitlementStore(private val items: List<PurchaseItem>) {
    val entitlement: StateFlow<Entitlement>         // single source of truth
    /** Rebuilds ownership from a full query result. PENDING never grants. */
    fun onPurchasesUpdated(purchases: List<PurchaseInfo>)
    /** @return purchases that still need acknowledgment (the 3-day refund bug, by construction). */
    fun unacknowledged(purchases: List<PurchaseInfo>): List<PurchaseInfo>
}

data class Entitlement(
    val isPremium: Boolean,                         // replaces AppPurchase.isPurchased()
    val ownedProducts: Set<String>,
    val activeSubscriptions: List<SubscriptionInfo>,// state, expiry, willRenew
)

// 3. Purchase flow: launches billing UI, routes results back into the store.
class PurchaseFlow(connection: BillingConnection, store: EntitlementStore) {
    suspend fun purchase(activity: Activity, productId: String): PurchaseOutcome
    suspend fun subscribe(activity: Activity, subsId: String, offerToken: String? = null): PurchaseOutcome
    suspend fun consume(productId: String): Boolean
}
```

`EntitlementStore.onPurchasesUpdated()` is the *only* writer of entitlement state, so the
"three restore paths disagree" bug class is gone; rebuild-from-query (the 3.5.8 fix) is the
only mode. Acknowledgment is computed declaratively from the same input.

### What BillingConfig / Compose gain

- `AppPurchaseProvider` (core) grows `val entitlement: StateFlow<Entitlement>` with a default
  that wraps the boolean for custom providers. `BillingPurchaseProvider` exposes the store's
  flow directly.
- 3.5.8 had to patch `rememberPurchaseStatus()` to "re-read purchase state on resume and
  recomposition instead of freezing at first composition" — polling, because the state is a
  plain boolean. With a `StateFlow` it becomes
  `BillingConfig.getPurchaseProvider().entitlement.collectAsState()` — reactive by
  construction, no lifecycle hooks.
- Ad components can collect the flow once at init instead of calling `isPurchased()` on every
  load path.

### Migration

- `AppPurchase` stays as a **deprecated Java-compatible facade** delegating to the three new
  classes — `getInstance()`, `initBilling()`, `isPurchased()`, `purchase()`, `subscribe()`,
  listeners (`PurchaseListener`, `BillingListener`) all preserved with `@Deprecated`.
- Breaks: the public fields `isBillingAvailable` / `isBillingInitialized` become
  getters; `setPurchase(boolean)` (test hook that fakes entitlement) is removed in favor of a
  `FakeEntitlementStore`; `Tag`-level internals are gone.
- Pricing/offer helpers (`getPrice`, `getOffers`, `getTrialOffer`, `OfferInfo`, 3.5.7 work)
  move mostly unchanged into a `ProductCatalog` helper fed by `queryProductDetails()`.

**Effort: L** — the largest item in v4. The EntitlementStore + JVM tests should land first;
the facade keeps the sample app compiling throughout.

---

## 3. Provider factories instead of shared instances

### Rationale

`AdProviderConfig` (core) stores **lists of live provider instances** shared by every
waterfall. Two 3.5.8 fixes exist *only* because of that sharing:

1. **`ownsProviders` (default `false`)** had to be added to all five waterfalls so
   `waterfall.destroy()` stops destroying providers that other waterfalls (and the global
   chains) still use. Ownership is ambiguous by design.
2. **Keyed-slot workaround**: AdMob full-screen providers were single-slot, so two placements
   sharing one `AdMobInterstitialProvider` instance clobbered each other's ads
   ("cross-placement clobbering and wrong-revenue attribution"). The fix bolted per-ad-unit
   default methods onto the interfaces — `isAdReady(adUnitId)` / `showAd(activity, adUnitId,
   callback)` — and a `loadedAds: ConcurrentHashMap<String, InterstitialAd>` inside
   `AdMobInterstitialProvider`. Every provider author must now know to override the keyed
   variants or silently inherit single-slot bugs.

### New API

```kotlin
object AdProviderConfig {
    fun setInterstitialChain(factories: List<() -> InterstitialAdProvider>)
    fun getInterstitialChain(): List<() -> InterstitialAdProvider>
    // ... same for banner / native / appOpen / rewarded
}

class InterstitialWaterfall(
    factories: List<() -> InterstitialAdProvider> = AdProviderConfig.getInterstitialChain(),
) {
    private val providers = factories.map { it() }  // fresh instances, owned by this waterfall
    fun destroy() = providers.forEach { it.destroy() }   // unconditionally safe
}
```

- Each waterfall instantiates its own providers → one placement, one provider, one ad slot.
- `ownsProviders` flips meaning: waterfalls always own what they create. The constructor
  parameter is **removed** (it was only added in 3.5.8; an escape hatch
  `InterstitialWaterfall(preBuilt = listOf(provider), ownsProviders = false)` can stay one
  release if needed).
- The keyed-slot defaults `isAdReady(adUnitId)` / `showAd(activity, adUnitId, ...)` are
  **deleted** from the provider interfaces; `AdMobInterstitialProvider` drops its
  `loadedAds` map back to a single slot. Interfaces shrink to load / show / isReady / destroy.

### Migration

| 3.x | 4.x |
|---|---|
| `AdProviderConfig.setInterstitialChain(listOf(yandexProvider, admobProvider))` | `AdProviderConfig.setInterstitialChain(listOf({ YandexInterstitialProvider() }, { AdMobInterstitialProvider() }))` |
| `AdMobProviderRegistration.create(...)` returns instances | returns factories (same call shape; mostly source-compatible for users of the registration helpers) |
| Custom provider overriding `showAd(activity, adUnitId, cb)` | delete the override; single-slot is now correct |

Apps that registered via `AdMobProviderRegistration` / `YandexProviderRegistration` (the
documented path in `MULTI_PROVIDER_WATERFALL.md` / `YANDEX_INTEGRATION.md`) recompile with no
changes; only hand-built chains touch code.

**Effort: S** — the waterfalls and registrations are small; deleting the keyed-slot code is
net-negative LOC. Re-point the 3.5.8 waterfall tests at factories.

---

## 4. Single terminal-event contract: `AdFlowResult`

### Rationale

`AdManagerCallback.onNextAction()` is called from **~40 sites** in `AdManager.kt` alone and
means, variously: ad dismissed, ad failed to show, ad failed to load, premium user, interval
not met, no ad in pool, timeout. Downstream code cannot distinguish "user watched the ad"
from "we skipped it", and nothing guarantees the terminal event fires exactly once.

Bugs this contract caused:

- **Issue #36 item 3 (open)**: `InterstitialAdBuilder` wires `onNextAction()` straight to
  `onAdDismissedCallback` (`InterstitialAdBuilder.kt` show-callback), so `.onAdDismissed { }`
  fires on every skip/failure path — "ad watched" false positives. Also `.force()` is only
  read in the builder's own `minIntervalMs` check, so without `minInterval()` it does nothing
  while `AdManager.canShowAd()` still blocks the show — contradicting its docs.
- **3.5.8**: `loadInterstitialAdForSplash` could fire `onNextAction()` twice (double
  navigation) or never (splash hung forever); waterfall show failures delivered *both*
  `onAdFailedToShow` and `onAdDismissed`; `AppOpenManager` stranded pending callbacks;
  `RewardedAdManager` silently dropped queued load callbacks. Four different ad types, one
  root cause: no enforced exactly-once terminal event.

### New API

```kotlin
sealed interface AdFlowResult {
    /** Ad displayed and closed by the user — the only result that means "ad watched". */
    data object Dismissed : AdFlowResult
    /** Ad displayed, but no dismiss signal will arrive (activity finishing under the ad, process handoff). */
    data object Shown : AdFlowResult
    /** No ad displayed; flow continued immediately. */
    data class Skipped(val reason: SkipReason) : AdFlowResult
    /** Load or show failed (includes timeout). */
    data class Failed(val error: AdKitError) : AdFlowResult
}

enum class SkipReason { PREMIUM_USER, INTERVAL_NOT_MET, NOT_NTH_CALL, MAX_SHOWS_REACHED, NO_AD_AVAILABLE, AD_ALREADY_SHOWING, DISABLED }

// Show APIs take one completion handler:
adManager.showInterstitial(activity) { result: AdFlowResult -> navigateNext() }
```

Internally an `AdFlowController` per show-request owns a single `AtomicBoolean completed`
(generalizing the generation-token + watchdog pattern 3.5.8 introduced for waterfalls) —
every path funnels through `complete(result)`, so double-fire and never-fire become
structurally impossible, with a debug-mode assert on second completion.

### How this fixes #36 item 3

- `onAdDismissed` maps **only** to `AdFlowResult.Dismissed`; skips and failures arrive as
  their own results. False positives gone by type, not by auditing call sites.
- `.force()` becomes `force = true` on the show request and is consumed centrally where
  `Skipped` results are produced (AdManager's interval/count gates *and* the builder's),
  instead of one read at one builder line. Documented behavior and actual behavior converge.
- Item 1 of #36 (double-show guard) is the `AD_ALREADY_SHOWING` skip — checked inside the
  controller before any ad is touched, fixed in 3.5.8 for `AdManager` but now uniform across
  interstitial / app open / rewarded.

### Migration

- `AdManagerCallback` stays as a **deprecated adapter**: the library derives the old events
  from the new result — `onNextAction()` ⇐ any terminal result; `onFailedToLoad(e)` ⇐
  `Failed`; `onAdShowed()` ⇐ show-started progress event; `onAdTimedOut()` ⇐
  `Failed(error.code == TIMEOUT)`. Existing subclasses keep working unchanged.
- `InterstitialAdBuilder` keeps its fluent surface; `.onAdDismissed` is re-documented and
  re-wired to `Dismissed` only (a behavior break apps may *notice* — release-notes callout
  required), and gains `.onResult { result -> }` as the preferred terminal hook.
- Progress events (loaded/shown/clicked/impression/paid) remain a separate listener; only the
  *terminal* contract changes.

**Effort: M** — mechanical but wide: AdManager (~40 sites), AppOpenManager, RewardedAdManager,
waterfalls, builder, compose effects. The existing 83-test suite covers the waterfall half.

---

## 5. Native cache consolidation

### Rationale

Native ads currently flow through **three layers with split ownership**:

1. `NativeAdManager` — LRU cache with **destructive reads** (`getCachedNativeAd()` removes
   the ad; the KDoc has a bold "IMPORTANT: Destructive Read Pattern" warning).
2. `NativeAdIntegrationManager` — strategy/retry orchestration plus a **temporary handoff
   map** (`temporarilyCachedAds`): on a cache hit it removes the ad from layer 1, parks it in
   the map, fires `onAdLoaded()`, and trusts the view to synchronously call
   `getAndClearTemporaryCachedAd(screenKey)` before anything else does.
3. The views (`NativeBannerSmall/Medium/Large`, `NativeTemplateView`) — which load, do their
   *own* cache fallback inside their `AdListener`, display, and cache.

3.5.8 fixed five bugs that are all ownership confusion between these layers:

- `NativeTemplateView` cache key didn't match integration-manager storage — **ads were
  consumed (destructively!) but never displayed**, or displayed in the wrong view.
- The ad being displayed was *also* placed in the cache → destroy-while-displayed /
  double-bind risk.
- HYBRID cache hits never fired `onAdLoaded` (handoff protocol mismatch).
- Two long NOTE comments in `NativeAdIntegrationManager` exist solely to stop maintainers
  from consuming a cached ad in an async callback "after the caller's single synchronous read
  of the temporary cache, so a cached ad taken at this point could never be displayed and
  would leak" — the protocol is so fragile it needs warning signs.
- Lock-ordering deadlock between primary and fallback lookups in `NativeAdManager`.

### New API

```kotlin
/** The only native ad cache. Ads are leased, never destructively read. */
object NativeAdCache {
    /** Take exclusive ownership of a cached ad. Null on miss. */
    fun checkOut(key: CacheKey, allowFallback: Boolean = false): NativeAdLease?
    /** Store a preloaded ad. The cache owns it until checked out. */
    fun checkIn(key: CacheKey, ad: NativeAd)
    fun size(key: CacheKey): Int
}

class NativeAdLease internal constructor(val ad: NativeAd) : AutoCloseable {
    /** Return an undisplayed ad to the cache (e.g. view detached before bind). */
    fun release()
    /** Destroy after display lifecycle ends. Idempotent. */
    override fun close()
}

data class CacheKey(val adUnitId: String, val size: NativeAdSize)   // replaces string-suffix keys
```

- **Exactly one owner at all times**: cache → lease → view. The temporary handoff map is
  deleted; a cache hit hands the caller a `NativeAdLease` directly in the load-result
  callback instead of `onAdLoaded()` + a side-channel map keyed by
  `"${activity.javaClass.simpleName}_${screenType.name}"`.
- **Typed keys** replace the suffix-string scheme (`"_small"` vs `"_SMALL"` — today
  `NativeAdManager.ScreenSuffix` enumerates six casing variants to undo what
  `NativeAdIntegrationManager.ScreenType` appends). The 3.5.8 key-mismatch bug cannot recur.
- **Views become pure renderers**: `NativeBannerSmall` etc. expose
  `render(lease: NativeAdLease)` / `render(ad: NativeAd)` and `destroy()`; loading, strategy
  selection, retry, and fallback all live in one `NativeAdLoader` (absorbing
  `NativeAdIntegrationManager`'s strategy `when` and `ProgrammaticNativeAdLoader`). The
  Compose wrappers call the same loader — disposal (added in 3.5.8) becomes `lease.close()`.
- Single lock discipline inside `NativeAdCache` (one lock, or ordered striping) retires the
  deadlock-avoidance comments in `getCachedNativeAd()`.

### Migration

- `NativeAdManager.enableCachingNativeAds`, `getCachedNativeAd()`, `setCachedNativeAd()`,
  `preloadNativeAd()` stay as deprecated shims over `NativeAdCache` (destructive read =
  `checkOut()?.ad` with the lease leaked intentionally — same semantics as today).
- `NativeAdIntegrationManager` is **deleted** (it is documented as internal/`@suppress`
  advanced API; the temporary-map accessors have no legitimate external callers).
- XML attribute surface of the views is unchanged; only programmatic
  `loadNativeBannerAd(...)` entry points move to `NativeAdLoader`.

**Effort: M** — cache + lease is S; rewiring four views, the programmatic loader, and the
Compose wrappers onto `NativeAdLoader` is the long tail. The 3.5.8 cache-semantics tests
port directly.

---

## 6. Smaller v4 items

### 6.1 AppOpenManager split — extract the welcome-dialog flow (M)

`AppOpenManager.kt` is **2,175 lines**: lifecycle observation, fetch strategies, exclusion
lists, *and* a full in-window dialog implementation (`WelcomeBackDialogViews`, icon/text/color
config reads, `dismissWelcomeDialogWithDelay` with four nested `currentWelcomeDialog = null`
paths). The 3.4.0–3.5.8 history is dominated by dialog/fetch interleaving bugs: the ~50%
splash miss (`forceShowAdIfAvailable` vs dialog guard), `isFetchingWithDialog` races,
stranded `pendingFetchCallback`s, the 500 ms double-show window, retained destroyed
activities.

Plan: extract `WelcomeDialogController` (owns dialog views + `WelcomeDialogConfig` from §1,
exposes `show(activity)`, `dismiss(afterDelay)`) and `AppOpenAdFetcher` (load strategies,
freshness threshold, generation-tokened — reusing §4's `AdFlowController`). `AppOpenManager`
keeps only lifecycle + policy and shrinks to a few hundred lines. Public API unchanged except
callbacks moving to `AdFlowResult`.

### 6.2 Kotlin migration of remaining Java (S)

| File | Lines | Why now |
|---|---|---|
| `ump/AdsConsentManager.java` | 204 | 3.5.8 fixed "UMP listener never fired after the first success — hung consent-gated startups" and dropped concurrent requesters. Rewrite as Kotlin with a single `StateFlow<ConsentState>` so multiple collectors are the default, not a patch. |
| `billing/PurchaseResult.java` | 689 | Becomes a `data class` with non-null `productType` required at construction — the 3.5.8 "always `NOT_SUBSCRIPTION`" bug becomes a compile error. Folds into §2. |

After these (and §2), the library is 100% Kotlin; `app/` sample Java files stay as Java-interop
documentation (`JAVA_USAGE_GUIDE.md`).

### 6.3 Java 21 toolchain + jitpack.yml (S)

Currently: modules build with `sourceCompatibility/targetCompatibility = JavaVersion.VERSION_17`,
`jvmTarget = "17"`; `jitpack.yml` pins `openjdk17`. Plan for v4:

- Build with **JDK 21 toolchain** (`jitpack.yml`: `jdk: [openjdk21]`) — required as AGP 8.5+/
  Gradle 8.10+ and SDK 36 tooling assume 17+ and increasingly test on 21.
- **Keep library bytecode at 17** (`jvmTarget = 17`) for one more major so consumers on JDK 17
  CI aren't broken; revisit 21 bytecode in 4.1.
- `scripts/prepareJitpackEnvironment.sh` must be re-validated under 21 (it predates the JDK
  bump); the sample `app` module's `JavaVersion.VERSION_1_8` should be lifted to 17 at the
  same time.

### 6.4 Robolectric SDK 36 unblock (S)

The 3.5.8 test suite pins `@Config(sdk = [35])` (`NativeAdManagerCacheTest`,
`AdRetryManagerTest`, ...) because Robolectric 4.16 (current `libs.versions.toml`) does not
provide SDK 36 jars while `compileSdk = 36`. Action: bump Robolectric to the first release
with API 36 support, delete the `sdk = [35]` pins, and add `@GraphicsMode(NATIVE)` where the
native-view tests need it. Until then, every test silently exercises API 35 behavior on an
API 36 library. Zero API impact — do this in the first alpha so all v4 work is tested at 36.

---

## Suggested phasing

| Milestone | Contents | Gate to next |
|---|---|---|
| **4.0.0-alpha01** | §3 provider factories, §6.3 JDK 21 + jitpack, §6.4 Robolectric 36, drop dead config fields | Sample app + waterfall tests green on factories |
| **4.0.0-alpha02** | §1 immutable config + mutable facade, §6.2 `AdsConsentManager` Kotlin | All read sites on `AdManageKit.config`; facade parity tests |
| **4.0.0-alpha03** | §4 `AdFlowResult` + `AdManagerCallback` adapter, §6.1 AppOpenManager split | Issue #36 closeable; exactly-once tests for all four full-screen types |
| **4.0.0-alpha04** | §5 native cache + lease, delete `NativeAdIntegrationManager` | Cache tests ported; Compose native screens leak-checked |
| **4.0.0-beta01** | §2 billing rewrite behind `AppPurchase` facade, `PurchaseResult` data class, `StateFlow` entitlements in core + compose | EntitlementStore JVM test suite; sample billing flows on a test track |
| **4.0.0** | Sample app migrated off all facades, `MIGRATION_4.0.md`, README, MCP docs refresh | — |

Ordering rationale: alpha01 items are small, independent, and de-risk the build/test
infrastructure everything else relies on. Config (alpha02) must precede the AppOpenManager
split (alpha03) because `WelcomeDialogConfig` feeds it. Billing lands last because it is the
largest and its facade keeps it decoupled from the ad-side milestones. Each alpha keeps the
deprecated facades shipping, so a 3.5.x app can adopt any alpha without source changes —
deprecation warnings are the migration guide.
