# Release Notes - v3.3.6

**Release Date**: January 2025

## Multi-Language Support & Dark Mode

This release adds comprehensive internationalization support for ad loading dialogs and fixes dark mode compatibility issues.

### New Features

#### 42 Language Translations

Ad loading dialogs now display in the user's language. Supported languages include:

| Region | Languages |
|--------|-----------|
| **Asia** | Chinese (Simplified & Traditional), Japanese, Korean, Hindi, Bengali, Tamil, Telugu, Marathi, Thai, Vietnamese, Indonesian, Malay, Urdu |
| **Europe** | German, French, Spanish, Italian, Portuguese, Russian, Ukrainian, Polish, Dutch, Swedish, Norwegian, Danish, Finnish, Czech, Slovak, Hungarian, Romanian, Bulgarian, Greek, Croatian, Serbian |
| **Middle East** | Arabic, Persian, Hebrew, Turkish |
| **Africa** | Swahili |
| **Americas** | Spanish, Portuguese, Catalan, Filipino/Tagalog |

Translated strings:
- `ad_loading_title` - "Loading Ad"
- `ad_loading_message` - "Please wait a moment…"
- `welcome_back_title` - "Welcome Back!"
- `welcome_back_subtitle` - "Loading your content…"
- `welcome_back_footer` - "Just a moment…"

#### Dark Mode Support for Ad Loading Dialog

The ad loading dialog (`dialog_loading_ad_fullscreen.xml`) now properly adapts to night mode:

**Light Mode:**
- Card background: White (#FFFFFF)
- Title text: Dark gray (#212121)
- Message text: Medium gray (#757575)
- Progress indicator: Blue (#2196F3)

**Dark Mode:**
- Card background: Dark (#1E1E1E)
- Title text: Light gray (#ECECEC)
- Message text: Medium gray (#B0B0B0)
- Progress indicator: Light blue (#64B5F6)

### Bug Fixes

#### Splash Ad Loading Optimization

`loadInterstitialAdForSplash()` now prevents redundant ad requests:

```kotlin
// Before: Would make network request even if ad was already loaded
AdManager.getInstance().loadInterstitialAdForSplash(context, adUnitId, timeout, callback)

// After: Skips request if ad is ready, immediately calls callbacks
// - If ad is loaded: Calls onNextAction() + onAdLoaded() immediately
// - If ad is loading: Waits for existing request with timeout
// - If neither: Makes new request (existing behavior)
```

This optimization:
- Reduces unnecessary network requests
- Prevents duplicate ad loads during configuration changes
- Maintains timeout behavior for callback guarantees

### Technical Details

#### New Color Resources

Added to `values/colors.xml` and `values-night/colors.xml`:
- `ad_dialog_overlay`
- `ad_dialog_card_background`
- `ad_dialog_title_color`
- `ad_dialog_message_color`
- `ad_dialog_progress_color`
- `ad_dialog_progress_track`

#### New String Resources

Added to `values/strings.xml` with translations in 42 `values-{locale}/strings.xml` files:
- `ad_loading_title`
- `ad_loading_message`
- `welcome_back_title`
- `welcome_back_subtitle`
- `welcome_back_footer`
- `app_icon_description`

#### loadInterstitialAdForSplash Improvements

- Now tracks loading state with `loadingAdUnits` set (consistent with `loadInterstitialAd`)
- Checks `mInterstitialAd` and `adPool` before making new requests
- Properly cleans up loading state in all callback paths

---

## Installation

```groovy
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.3.6'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-billing:v3.3.6'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-core:v3.3.6'
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-compose:v3.3.6'
```

## Full Changelog

- Added translations for 42 languages
- Added dark mode support for ad loading dialog
- Fixed `loadInterstitialAdForSplash` to skip loading when ad is ready
- Fixed duplicate load requests during splash screen
- Added proper `loadingAdUnits` tracking for splash ad loading
- Updated dialog layouts to use string resources
- Updated dialog layouts to use theme-aware color resources
