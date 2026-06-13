# Changelog

All notable changes to AdManageKit will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.6.0] - 2026-06-13

Native-ad waterfall improvements: Yandex now renders the same templates as AdMob, and the programmatic native loader honours the provider chain.

### Added

#### Yandex native ads match AdMob templates
- `YandexNativeProvider` now renders the **exact AdMob native template** selected on `NativeTemplateView` (all 37 templates) instead of a generic size-based view. The chosen template's layout is inflated and Yandex assets are bound to the standard asset ids (`ad_headline`, `ad_body`, `ad_call_to_action`, `ad_app_icon`, `ad_advertiser`, `ad_media`), with the Google `MediaView` swapped for a Yandex `MediaView` and a small mandatory Yandex compliance row (feedback + sponsored + warning) appended. Falls back to the built-in size-based view if a template can't be bound
- `NativeAdProvider.loadNativeAd` gains an optional `templateLayoutResId: Int = 0` parameter carrying the template layout down the waterfall (kept an `Int` so the core module stays dependency-free)

#### Programmatic native loader respects the waterfall
- `ProgrammaticNativeAdLoader` (and `NativeAdManager.loadNativeAdProgrammatically` / `loadSmall|Medium|LargeNativeAd` / `loadNativeAdIntoContainer`) now route through the provider chain when one is configured via `AdProviderConfig.setNativeChain`, so AdMob no-fill falls back to other providers (e.g. Yandex). With no chain configured the pure-AdMob path is unchanged
- `ProgrammaticAdCallback.onProviderAdLoaded(adView, nativeAdRef)` — new default-no-op hook delivering non-AdMob (e.g. Yandex) views; AdMob fills still arrive via the typed `onAdLoaded`. `ProgrammaticNativeAdCompose` and `loadNativeAdIntoContainer` display these automatically
- `NativeAdProvider.NativeAdCallback` gains `onNativeAdOpened()` / `onNativeAdClosed()` default hooks, forwarded by `AdMobNativeProvider` and the waterfall so open/close events fire consistently whether or not a chain is configured

#### Cancellable loads
- The programmatic native load methods now return a `ProgrammaticNativeAdLoader.NativeAdLoadHandle` whose `cancel()` stops delivery of further callbacks; a fill arriving after cancellation is destroyed instead of pushed into a dead view hierarchy. `ProgrammaticNativeAdCompose` cancels automatically on dispose/reload

### Fixed
- `loadNativeAdIntoContainer` no longer leaks the previously displayed `NativeAd` — it destroys the prior ad (tracked via a container view tag) before replacing the container's content
- An AdMob cached ad no longer short-circuits a configured waterfall when AdMob is **not** first in the chain (e.g. a Yandex-first chain), which previously violated the configured provider order
- Non-AdMob waterfall fills delivered through the raw `loadNativeAd` callback no longer fail silently: the loader logs a warning when the callback doesn't override `onProviderAdLoaded`

### Changed
- The programmatic native load methods changed their return type from `Unit` to `NativeAdLoadHandle`. **Source-compatible** (callers ignoring the return value need no change) but **binary-incompatible** — recompile against 3.6.0 (JitPack builds from source, so consumers are unaffected)

## [3.5.9] - 2026-06-11

### Fixed

#### Compose adaptive banner sizing and alignment (#39)
- `BannerAdCompose` no longer clips anchored-adaptive banners to a fixed 50dp box — it reserves the **real adaptive height** for the available width (adaptive banners are 50–90dp tall depending on the device)
- The banner load is deferred until the Compose slot is measured, so the ad is sized to the **actual slot width** (parent padding included) instead of the full window width
- `CollapsibleBannerAdCompose` reserves the same anchored-adaptive height (its collapsed state is an anchored adaptive banner), eliminating the layout jump on load
- `BannerAdView` centers the `AdView` horizontally in its container — the adaptive width is floored to whole dp, which left the ad visibly start-aligned
- The explicit-`height` `BannerAdCompose` overload is unchanged (caller-controlled)

#### Other
- Fixed the long-standing `MissingTranslation` lint error (the `install` layout-preview placeholder, missing from all 41 locales, is now `translatable="false"` — it is always overwritten at runtime with `nativeAd.callToAction`); plain `./gradlew build` passes again
- `BannerAdView.refreshAd()` (manual refresh) now preserves the collapsible/placement configuration, matching auto-refresh
- `AdsConsentManager` queues concurrent `requestUMP` callers and notifies them all when the in-flight consent flow completes, instead of answering early with possibly-stale state

### Added
- **Continuous integration**: GitHub Actions workflow running `assembleDebug` + `testDebugUnitTest` on every push/PR to main
- `AdKitAdError.ERROR_CODE_PURCHASE_BLOCKED` — named constant for the purchase-blocked error code 1001 (`AdManager.PURCHASED_APP_ERROR_CODE` now references it; value unchanged)
- 15 new unit tests for the Yandex module's internal error/value mappers (SDK-8 error-code translation, revenue/currency pairing)
- `docs/V4_API_PLAN.md` — design document for the planned v4 breaking improvements

### Changed
- Unit tests moved from the sample app into their owning library modules (AdManageKit, core, billing, yandex); 98 tests total
- Repository hygiene: `.DS_Store` ignored and untracked; `CLAUDE.md` refreshed

## [3.5.8] - 2026-06-10

Stability release: fixes the findings of a full-library code audit (issues #2–#35) across all six modules, and adds the project's first unit test suite.

### Fixed

#### Billing (`admanagekit-billing`)
- **Restored purchases are now acknowledged** in `verifyPurchased()` / `updatePurchaseStatus()` / `refreshPurchases()` — previously a purchase whose acknowledgment was interrupted was never acknowledged again and Google Play auto-refunded it after 3 days
- **PENDING purchases no longer grant entitlement** — all restore paths now require `PurchaseState.PURCHASED`
- Ownership state is rebuilt from each billing query, so refunds, cancellations, and expiry now clear `isPurchased()` within the session; consumables no longer flip the global purchased flag
- `onInitBillingFinished` fires exactly once per init (was up to 3×) and all billing listener callbacks are delivered on the main thread; shared purchase state is thread-safe
- `PurchaseResult.productType` is set on every stored result, fixing `getSubscriptionState()` / `isSubscriptionActive()` / `isSubscriptionCancelled()` (always returned `NOT_SUBSCRIPTION` before)
- Removed wrong `com.google.android.datatransport.BuildConfig` import that left every debug branch permanently dead; debug behavior is now injected via `AppPurchase.setDebugMode(boolean)`
- Removed obfuscated `Product.zza()` usage; subscription price/currency analytics resolve via the base offer instead of the trial phase; `endConnection()` is called on re-init; the caller's purchase list is no longer mutated

#### Native ads (`AdManageKit`)
- `NativeTemplateView` cache key now matches `NativeAdIntegrationManager` storage — cached ads were previously consumed but never displayed (or shown in the wrong view)
- Native ad load failures are no longer silently swallowed with default config: `onFailedToLoad` fires whenever no retry is actually scheduled
- HYBRID strategy cache hits now fire `onAdLoaded` (previously only ONLY_CACHE did)
- The ad being displayed is no longer also placed in the cache (destroy-while-displayed / double-bind risk)
- `NativeAdManager`: fixed lock-ordering deadlock in fallback lookups, non-atomic `clearAllCachedAds()` / `performCleanup()`, and an init crash when `cacheCleanupInterval` was under 1 minute
- Displayed `NativeAd` objects are now destroyed when replaced; preloading no longer binds throwaway `NativeAdView`s; LARGE programmatic native ads no longer crash with "child already has a parent"

#### Ad managers (`AdManageKit`)
- `loadInterstitialAdForSplash`: the caller is always notified exactly once — previously a duplicate in-flight load could leave the splash screen hanging forever, and auto-retry could fire `onNextAction()` twice (double navigation)
- `showAd` guards against double-show, clears the legacy ad mirror when consumed, and force/fresh flows show the exact requested ad instead of an arbitrary pooled unit
- `InterstitialAdBuilder.onAdShown` now actually fires (was wired to the load callback)
- `AppOpenManager`: destroyed activities are no longer retained; pending fetch/show callbacks are never stranded; concurrent loads no longer clobber each other; closed the 500 ms double-show window
- `AdsConsentManager`: the UMP listener now fires on every consent request (previously never after the first success — hung consent-gated startups); concurrent requesters are answered instead of dropped
- `BannerAdView`: the replaced `AdView` is destroyed on reload/auto-refresh (one WebView leaked per refresh cycle before); `enableAutoRefresh(intervalSeconds)` honors its parameter; purchase-blocked loads skip the retry loop; collapsible config survives auto-refresh
- `RewardedAdManager`: load callbacks are queued against in-flight loads instead of silently dropped; no duplicate concurrent loads; double-show guard
- `AdRetryManager`: rescheduling a retry cancels the superseded runnable (previously both ran)
- `AdManageKitConfig.resetToDefaults()` now restores `maxCacheMemoryMB` (200) and `appOpenAdFreshnessThreshold` (4h) to their documented defaults

#### Waterfall (`AdManageKit`, `admanagekit-core`)
- AdMob full-screen providers store ads **per ad unit**; waterfalls show exactly the ad they loaded — cross-placement clobbering and wrong-revenue attribution fixed
- A failed load no longer discards a previously loaded, still-valid ad
- `waterfall.destroy()` no longer destroys globally shared providers (see `ownsProviders` below)
- Generation tokens + per-attempt watchdog timeout (`AdManageKitConfig.defaultAdTimeout`): a hung provider can no longer stall the chain, and exactly one terminal callback is delivered per `load()` and per `show()`
- `AdMobNativeProvider` returns a real populated template instead of an empty `NativeAdView` with tracking activated, and no longer destroys ads handed to consumers; `AdMobBannerProvider` destroys failed/undelivered AdViews; `AdMobAppOpenProvider` enforces the 4-hour freshness rule; `AdUnitMapping` / `AdProviderConfig` are thread-safe

#### Yandex (`admanagekit-yandex`)
- Yandex error codes are translated to `AdKitAdError` constants (previously passed through raw — `NETWORK_ERROR` was reported as `NO_FILL`, etc.)
- Impression revenue is paired with its correct currency (previously USD values were tagged with the account currency — off by the full exchange rate for non-USD accounts)
- `destroy()` cancels in-flight loads; a preloaded next ad survives dismissal of the current one; banner views are tracked from creation; native bind failures are reported as load failures on all sizes

#### Compose (`admanagekit-compose`)
- `InterstitialAdEffect` shows the ad from the load result instead of checking `isReady()` immediately after an async load (previously it effectively never showed the requested ad)
- `onAdShown` fires from the real show callback, not before paths that may skip the show
- Ad views are recreated and attached when composable parameters change (`key()`-wrapped `AndroidView`); banner disposal destroys the underlying `AdView` (stops invisible background refreshes); caller lambdas are no longer captured stale
- `rememberPurchaseStatus()` re-reads purchase state on resume and recomposition instead of freezing at first composition
- Native/template composables destroy their views and `NativeAd`s on dispose

### Added
- **Unit test suite** (83 tests) in `app/src/test` covering the waterfall contracts, `AdRetryManager`, `NativeAdManager` cache semantics, `AdManageKitConfig`, `AdUnitMapping`, and `PurchaseResult` subscription state
- `AppPurchase.setDebugMode(boolean)` — inject the host app's debug state (replaces the broken `BuildConfig.DEBUG` check)
- `destroy()` on `NativeBannerSmall` / `NativeBannerMedium` / `NativeLarge` / `NativeTemplateView` — destroys the displayed ad; called automatically when the view is detached while its Activity is finishing
- `ownsProviders` constructor parameter (default `false`) on all five waterfalls — providers are only destroyed by waterfalls that own them
- Default per-ad-unit `isAdReady(adUnitId)` / `showAd(activity, adUnitId, callback)` methods on the full-screen provider interfaces (source-compatible; existing implementations are unaffected)
- Per-attempt waterfall timeout, configurable via the `attemptTimeoutMillis` constructor parameter (defaults to `AdManageKitConfig.defaultAdTimeout`)

### Changed
- Waterfall show failures now deliver a **single terminal event**: `onAdFailedToShow` on failure XOR `onAdDismissed` after a successful show (previously both could fire, running navigation twice)
- `PurchaseDevBottomSheet` accepts a nullable `ProductDetails`, so the dev purchase sheet works for products not yet configured in Play

## [3.5.7] - 2026-05-15

### Added
- **Structured subscription offer API** in `admanagekit-billing`
  - New `OfferInfo` data class exposing trial / intro / base pricing phases of a Play subscription offer as flat fields (`isFreeTrial`, `trialPeriod`, `hasIntroPrice`, `introPrice`, `introPeriod`, `introCycleCount`, `basePrice`, `billingPeriod`, `currencyCode`, plus raw `pricingPhases`, `offerToken`, `offerTags`, `basePlanId`, `offerId`)
  - `AppPurchase.getOffers(productId)` — every offer attached to a subscription product
  - `AppPurchase.getTrialOffer(productId)` — first offer that contains a free-trial phase
  - `AppPurchase.getBaseOffer(productId)` — non-promo offer (falls back to the last offer)

### Fixed
- `AppPurchase.getIntroductorySubPrice()` now correctly walks all offers and returns the discounted finite-recurring phase instead of comparing the first phase against itself
- `AppPurchase.getPriceSub()` / `getBillingPeriod()` / `getPricePricingPhaseList()` now return the **base** (INFINITE_RECURRING) phase rather than relying on list position, so products with multiple offers report the correct base price/period
- `AppPurchase.hasFreeTrial()` / `getFreeTrialPeriod()` route through the new offer classifier — same external behavior, single source of truth

## [3.4.6] - 2026-05-07

### Added
- **10 Flat-Design Native Ad Templates** for `NativeTemplateView` (total templates: 27 → 37)
  - `FLAT_INLINE_ROW`, `FLAT_CARD_RATING`, `FLAT_MEDIA_TOP`, `FLAT_TEXT_MINIMAL`, `FLAT_COMPACT_PILL`, `FLAT_CAROUSEL`, `FLAT_BANNER`, `FLAT_FEATURE_LIST`, `FLAT_SPONSORED_STORY`, `FLAT_FOOTER_SLIM`
  - Flat-design rules: no gradients, no heavy shadows, subtle separators, consistent `Ad` / `Sponsored` disclosure, pill-shaped CTAs
  - Theme-driven via `?attr/colorSurface`, `?attr/colorPrimary`, `?attr/colorOnSurface`, `?attr/colorOnSurfaceVariant`, `?attr/colorOutlineVariant` — full Material 3 / dark-mode support
- New `app:adTemplate` enum values (28–37) for XML usage: `flat_inline_row`, `flat_card_rating`, `flat_media_top`, `flat_text_minimal`, `flat_compact_pill`, `flat_carousel`, `flat_banner`, `flat_feature_list`, `flat_sponsored_story`, `flat_footer_slim`
- 10 new Compose helpers in `admanagekit-compose`: `NativeFlatInlineRowCompose`, `NativeFlatCardRatingCompose`, `NativeFlatMediaTopCompose`, `NativeFlatTextMinimalCompose`, `NativeFlatCompactPillCompose`, `NativeFlatCarouselCompose`, `NativeFlatBannerCompose`, `NativeFlatFeatureListCompose`, `NativeFlatSponsoredStoryCompose`, `NativeFlatFooterSlimCompose`
- New companion helper `NativeAdTemplate.flatTemplates(): List<NativeAdTemplate>`
- 8 new supporting drawables (`ads_flat_pill`, `ads_flat_pill_outline`, `ads_flat_card`, `ads_flat_card_soft`, `ads_flat_label_outline`, `ads_flat_label_filled`, `ads_flat_app_icon_bg`, `ads_flat_media_bg`)

### Changed
- `NativeTemplateView.getScreenTypeForTemplate()` extended to bucket the new flat templates (SMALL / MEDIUM / LARGE) for size-aware ad requests and integration-manager bookkeeping

## [3.4.5] - 2026-05-05

### Changed
- **Yandex Mobile Ads SDK 8.0.0**: Upgraded `admanagekit-yandex` from SDK 7.18.1 to 8.0.0
  - `MobileAds.initialize` → `YandexAds.initialize`
  - `AdRequestConfiguration` / `NativeAdRequestConfiguration` → `AdRequest` (ad unit ID now passed to `AdRequest.Builder` constructor)
  - Loader pattern: `setAdLoadListener` + `loadAd(config)` → `loadAd(request, listener)` for interstitial, rewarded, app open, and native
  - `BannerAdSize.stickySize` → `BannerAdSize.sticky`; `BannerAdView.setAdUnitId()` removed — ID passed via `AdRequest.Builder`
  - `onLeftApplication()` / `onReturnedToApplication()` removed from all ad event listeners
  - `bindNativeAd(binder)` now returns `AdBindingResult` instead of throwing — throws `IllegalStateException` on failure to propagate through existing error paths

### Fixed
- **Yandex Large Native Ad Layout**: Ad was showing only the media image with title/body/CTA pushed off-screen
  - Added required `price` view (required for app-type native ads) and `favicon` view
  - Layout order changed to: header → body → CTA → MediaView → footer (CTA is always visible regardless of media height)
  - `MediaView` starts as `GONE`; SDK manages visibility when content is available
  - `MediaView` capped at 200dp height to prevent overflow
  - Binding failure now propagates as `onNativeAdFailedToLoad` instead of silently showing a blank view

## [3.4.4] - 2026-03-13

### Fixed
- **Native Ad Show Rate**: Parent container visibility not restored after previous load failure — ads loaded but stayed invisible (NativeBannerSmall, NativeBannerMedium, NativeLarge, NativeTemplateView)
- **NativeBannerMedium setNativeAd()**: `populateNativeAdView()` deferred `setNativeAd()` via `post {}` — changed to synchronous call for reliable impression registration
- **NativeBannerMedium Shimmer Overlap**: Shimmer animation not hidden when displaying cached/waterfall ads via `displayCachedAdSafely()`
- **RewardedAdManager Crash**: `lateinit var adUnitId` crashed with `UninitializedPropertyAccessException` when methods called before `initialize()` — changed to empty string default with guards
- **Native Views lateinit Crash**: `NativeBannerSmall`, `NativeLarge`, `NativeTemplateView` `adUnitId` changed from `lateinit` to empty string default

### Added
- **Empty adUnitId Validation**: All native views and `RewardedAdManager` now validate against empty ad unit IDs with warning logs and appropriate callback responses

## [3.4.3] - 2026-03-06

### Fixed
- **Duplicate Ad Loads**: `fetchViaWaterfall(callback, timeout)` and `fetchAd(callback, timeout)` now check `isLoading` before starting, attaching to the in-progress fetch instead of creating a duplicate (both waterfall and non-waterfall paths)
- **Splash Auto-Navigation**: Orphaned `pendingFetchTimeoutRunnable` no longer fires stale `onFailedToLoad` callbacks after the ad has already loaded and shown
- **Resume Ad Show Blocked**: `showAdIfAvailable()` no longer skips when a background auto-reload preload is in progress (only skips for dialog-based fetches)
- **ON_DEMAND Cold Start**: `onStart` no longer triggers automatic ad load for ON_DEMAND strategy on cold start; explicit `fetchAd`/`forceShow` handles it
- **Hot-Start Duplicate Loads**: Fixed concurrent loads when returning to SplashActivity via hot start

### Added
- **Background Fetch Callback Attachment**: `pendingFetchCallback` mechanism allows splash `fetchAd` to receive results from an already-running background preload
- **Cold Start Detection**: `hasBeenBackgrounded` flag distinguishes cold start from resume for ON_DEMAND strategy
- **Load Start Logging**: `AdDebugUtils.logEvent` `loading` event added to all ad load start paths
- **Cached Ad Logging**: `AdDebugUtils.logEvent` `showCachedAd` event when cached ads are shown
- **Comprehensive Logging**: `AdDebugUtils.logEvent` added to all previously missing load/failure paths in `AppOpenManager`

## [3.4.2] - 2026-03-06

### Fixed
- **App Open Auto-Retry**: `AppOpenManager` now respects `AdManageKitConfig.autoRetryFailedAds` (was always retrying with hardcoded values, ignoring the config flag)
- **App Open Retry Config**: `AppOpenManager` now uses `AdManageKitConfig.maxRetryAttempts` instead of a hardcoded retry limit, and delegates to `AdRetryManager` for consistent exponential backoff across all ad types
- **Late-Loading Ad Preservation**: App open ads that load after the timeout has fired are now cached for later use instead of being silently discarded

### Added
- **Waterfall App Open Retry**: Background waterfall preload (`fetchViaWaterfall()`) now auto-retries on failure when `autoRetryFailedAds` is enabled

### Deprecated
- `AppOpenManager.updateRetryConfiguration()` — use `AdManageKitConfig` properties directly

## [3.4.1] - 2026-02-25

### Added
- **Product Metadata APIs**: New `AppPurchase` methods to retrieve Play Console product information:
  - `getProductTitle(productId)` — Localized title with app name (e.g. "Monthly Premium (My App)")
  - `getProductName(productId)` — Clean product name without app name (e.g. "Monthly Premium")
  - `getProductDescription(productId)` — Product description from Play Console
  - `getProductDetails(productId)` — Raw `ProductDetails` object for full access
- **Free Trial Detection**: New methods to check trial offers on subscriptions:
  - `hasFreeTrial(productId)` — Whether the subscription has a free trial offer
  - `getFreeTrialPeriod(productId)` — Trial period in ISO 8601 format (e.g. "P3D", "P7D")
- **Billing Period**: `getBillingPeriod(productId)` — Subscription billing period (e.g. "P1M", "P1Y")

## [3.4.0] - 2026-02-17

### Fixed
- **App Open Splash Show Rate (~50% miss)**: `forceShowAdIfAvailable()` now correctly takes over a dialog-based fetch already started by `onStart()`'s automatic `showAdIfAvailable()`, instead of firing `onNextAction()` prematurely via the dialog guard
- **`showAdIfAvailable()` no longer races against `forceShowAdIfAvailable()`**: Added `isFetchingWithDialog` guard so the lifecycle-driven auto-show path skips when a splash fetch is already in progress
- **Native Large Ad Tablet Layout**: `layout_native_large.xml` (sw600dp) container changed from `LinearLayout` to `FrameLayout`, fixing layout rendering issues on tablet screens

### Changed
- `dialogFetchCallback` field added to `AppOpenManager` — the active dialog fetch callback is now replaceable, allowing `forceShowAdIfAvailable()` to attach to an already-running fetch
- Stable release consolidating multi-provider waterfall (3.3.8) and app open ad callback improvements (3.3.9)

## [3.3.9] - 2026-02-10

### Fixed
- **App Open Ad Callbacks**: `AdManagerCallback.onFailedToLoad()` now fires correctly for app open ad failures (previously never called when a dialog was showing)
- **Dialog-first guarantee**: Welcome dialog is always dismissed before `onFailedToLoad`, `onAdTimedOut`, or `onNextAction` callbacks fire
- **Waterfall Banner Sizing**: Banner ads via waterfall now use adaptive full-width sizing (was fixed 320×50dp)
- **Collapsible Banner Passthrough**: Collapsible banner settings now pass through waterfall to `AdMobBannerProvider`

### Added
- **`onAdTimedOut()` callback**: New dedicated `AdManagerCallback` event when app open ad load exceeds timeout duration (distinct from `onFailedToLoad`)

### Changed
- `AdMobBannerProvider` default ad size changed from `AdSize.BANNER` (fixed 320×50dp) to adaptive full-width
- `AdMobProviderRegistration.create()` default `bannerAdSize` parameter changed from `AdSize.BANNER` to `null` (adaptive)

## [3.3.8] - 2026-01-20

### Added
- **Multi-Ad-Provider Architecture**: New core interfaces (`InterstitialAdProvider`, `AppOpenAdProvider`, `BannerAdProvider`, `NativeAdProvider`, `RewardedAdProvider`) in `admanagekit-core` with zero external dependencies
- **Waterfall Mediation**: Automatic fallback across ad networks for all ad types (interstitial, app open, banner, native, rewarded)
- **Yandex Ads Module**: New `admanagekit-yandex` module with full Yandex Mobile Ads SDK integration
- **`AdProviderConfig`**: Centralized provider registry and chain configuration
- **`AdMobProviderRegistration` / `YandexProviderRegistration`**: Registration helpers for easy provider setup

### Changed
- All existing `AdManager`, `AppOpenManager`, `BannerAdView` calls automatically use the waterfall when providers are registered
- No changes required to existing AdMob-only code (fully backward compatible)

## [3.3.7] - 2026-01-10

### Added
- **Welcome Dialog for Cached App Open Ads**: Cached app open ads now display the welcome back dialog before showing
- **`appOpenFetchFreshAd` config**: Controls whether app open ads are prefetched on background (`false`, default) or fetched fresh on foreground (`true`)

### Changed
- `NativeAdManager.enableCachingNativeAds` now defaults to `false`
- `AdManageKitConfig.autoRetryFailedAds` now defaults to `false`

## [3.3.6] - 2025-01-29

### Added
- **Multi-Language Support**: Added translations for ad loading dialogs in 42 languages including Chinese, Spanish, Hindi, Arabic, Portuguese, Russian, Japanese, German, French, Korean, Turkish, and many more
- **Dark Mode Support**: Ad loading dialog now properly adapts to night mode with theme-aware colors for text, background, and progress indicators

### Fixed
- **Splash Ad Loading Optimization**: `loadInterstitialAdForSplash()` now skips redundant network requests if an ad is already loaded or currently loading

### Changed
- Dialog text strings (`ad_loading_title`, `ad_loading_message`, `welcome_back_title`, etc.) are now translatable string resources
- Loading dialog uses `@color/ad_dialog_*` resources that adapt to light/dark theme

## [3.3.2] - 2024-12-22

### Fixed
- **InterstitialAdBuilder**: Fixed ad unit not being assigned to AdManager on first "force fetch" path with HYBRID strategy
- **everyNthTime Feature**: Fixed call counter resetting on each builder instance; counter now persists in AdManager

### Added
- **New Native Templates**: Added `flexible`, `icon_left`, and `top_icon_media` templates for GridView display with MediaView support
- **AdManager Counter API**: New methods for managing call counters:
  - `incrementCallCount(adUnitId)` - Increment and return counter for ad unit
  - `getCallCount(adUnitId)` - Get current counter value
  - `resetCallCount(adUnitId)` - Reset specific ad unit counter
  - `resetAllCallCounts()` - Reset all counters
- **AdManager.setAdUnitId()**: Allows setting ad unit ID directly from builder
- **Interstitial Test Suite**: Comprehensive test activity for all interstitial ad scenarios

### Changed
- `InterstitialAdBuilder.adUnit()` now directly sets the ad unit on AdManager for immediate availability
- Call counters are now stored in AdManager singleton instead of builder instance

## [3.3.1] - 2024-12-21

### Fixed
- **NativeBannerMedium**: Fixed lateinit property crash when accessing views before inflation

## [3.3.0] - 2024-12-20

### Added
- **Single-Activity App Support**: Screen and fragment tag-based exclusions for app open ads
- **Background-Aware Ad Display**: Ads no longer show when app is in background; pending ads queue for foreground return
- **New `onAdShowed()` Callback**: Know when interstitial ad covers the screen

### Fixed
- Dialog duplication when app is paused/resumed during ad loading
- Threading crashes ("Animators may only be run on Looper threads")
- App open ads showing on top of interstitial loading dialogs
- Race condition allowing multiple concurrent ad shows

## [3.2.0] - 2024-12-19

### Added
- Screen tag exclusions (`setCurrentScreenTag`, `excludeScreenTags`)
- Fragment tag exclusions (`setFragmentTagProvider`, `excludeFragmentTags`)
- Temporary disable/enable (`disableAppOpenAdsTemporarily`, `enableAppOpenAds`)
- `isLoadingDialogShowing()` and `isAdOrDialogShowing()` to AdManager

## [3.1.0] - 2024-12-18

### Fixed
- **FRESH_WITH_CACHE_FALLBACK Strategy**: Successfully loaded ads now properly cached for fallback

### Added
- **MEDIUM_HORIZONTAL Template**: 55% media / 45% content horizontal split layout
- Total of 27 native ad templates (21 standard + 6 video)

## [3.0.0] - 2024-12-17

### Added
- **Ad Pool System**: Load multiple interstitial ad units with auto-selection
- **Smart Splash Ads**: `showOrWaitForAd()` single method for all splash scenarios
- **App Open Ad Prefetching**: `prefetchNextAd()` before external intents
- **Enhanced Analytics**: Session tracking with fill rate, show rate, impression metrics

### Changed
- Modern `WindowInsetsController` API (replaces deprecated systemUiVisibility)
- Thread-safe ad pool with `ConcurrentHashMap`

## [2.9.0] - 2024-12-15

### Added
- 6 new native ad templates: `app_store`, `social_feed`, `gradient_card`, `pill_banner`, `spotlight`, `media_content_split`
- Purchase categories for billing (`CONSUMABLE`, `REMOVE_ADS`, `LIFETIME_PREMIUM`)
- Subscription management (`getSubscriptionState`, `upgradeSubscription`, `downgradeSubscription`)
- AdChoices position control

### Breaking Changes
- Consumable products no longer auto-consumed; manual `consumePurchase()` required

## [2.8.0] - 2024-12-12

### Changed
- `forceShowInterstitial()` now respects loading strategy configuration
- Added `forceShowInterstitialAlways()` for original always-fetch behavior

### Added
- `InterstitialAdBuilder` fluent API with frequency controls
- `everyNthTime()`, `maxShowsPerSession()`, `minIntervalSeconds()` builder methods

## [2.7.0] - 2024-12-10

### Added
- Auto-reload configuration for interstitial ads (`interstitialAutoReload`)
- Per-call reload override via `InterstitialAdBuilder.autoReload()`

## [2.6.0] - 2024-12-08

### Added
- **NativeTemplateView**: Unified native ad view with 17 template styles
- **Ad Loading Strategies**: ON_DEMAND, ONLY_CACHE, HYBRID, FRESH_WITH_CACHE_FALLBACK
- Material 3 theming with automatic dark/light mode support

## [2.5.0] - 2024-12-05

### Added
- Intelligent native ad caching with screen-aware prefetching
- Smart retry with exponential backoff
- Circuit breaker for failing ad units

## [2.3.0] - 2024-12-01

### Added
- Centralized configuration via `AdManageKitConfig`
- Debug/production environment settings
- Runtime configuration changes

## [2.1.0] - 2024-11-28

### Added
- Collapsible banner ads support
- UMP consent management improvements
- Firebase Analytics integration for tROAS tracking

## [2.0.0] - 2024-11-25

### Breaking Changes
- Updated to Google Play Billing Library v8
- Multi-module architecture (core, billing, compose)

### Added
- Jetpack Compose support module
- Google Play Billing v8 integration
