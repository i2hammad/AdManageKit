# Changelog

All notable changes to AdManageKit will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
