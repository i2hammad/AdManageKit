# Release Notes - v4.1.0

## Highlights

- **Background-Aware Ad Display**: Ads no longer show when app is in background
- **Welcome Dialog on Return**: When user returns with pending ad, welcome dialog appears first
- **Interstitial Priority**: App open ads won't show over interstitial loading dialogs
- **Dialog Stability Fixes**: Prevents duplicate dialogs and threading issues

---

## New Features

### Background-Aware Ad Display

App open ads now intelligently handle background/foreground transitions:

- **No Background Ads**: Ads won't try to show when app is in background
- **Pending Ad Queue**: If ad loads while app is in background, it's saved for when user returns
- **Welcome Dialog on Return**: When user returns, a welcome dialog appears briefly before showing the saved ad

```kotlin
// This is automatic - no code changes needed!
// When user switches apps during ad loading:
// 1. Ad loads in background -> saved as pending
// 2. Dialog dismisses automatically
// 3. User returns to app
// 4. Welcome dialog appears
// 5. Pending ad shows smoothly
```

### Interstitial Priority

App open ads now respect interstitial ad loading states:

- If interstitial loading dialog is showing, app open ad is skipped
- Pending app open ads are cleared when interstitial has priority
- Prevents ad overlap issues

---

## Bug Fixes

### Dialog Duplication Fix

**Issue**: When app was paused and resumed during ad loading dialog, a new dialog would appear without dismissing the old one, causing multiple overlapping dialogs.

**Fix**:
- Track current dialog instance to prevent duplicates
- Dismiss existing dialog before showing new one
- Added `isFetchingWithDialog` flag to prevent concurrent fetch requests

### Threading Issues Fix

**Issue**: Ad SDK callbacks could be called from background threads, causing crashes:
- `Animators may only be run on Looper threads`
- `Can't toast on a thread that has not called Looper.prepare()`

**Fix**: All ad callbacks now properly dispatch to main thread using `Handler(Looper.getMainLooper()).post { }`.

### Interstitial Priority Fix

**Issue**: App open ads would show on top of interstitial loading dialogs when user switched apps and came back.

**Fix**:
- `onStart` now checks `isAdOrDialogShowing()` instead of just `isDisplayingAd()`
- If interstitial dialog is showing, app open ad is skipped
- Pending app open ads are cleared when interstitial has priority

### Race Condition Fix

**Issue**: Multiple ads could show in rapid succession due to flag being set after `show()` instead of before.

**Fix**: `isShowingAd` flag is now set synchronously before calling `show()` to prevent concurrent show attempts.

---

## API Changes

### AdManager

```kotlin
// NEW - Check if loading dialog is showing
fun isLoadingDialogShowing(): Boolean

// NEW - Check if ad OR loading dialog is showing
fun isAdOrDialogShowing(): Boolean
```

---

## Migration Guide

### From v4.0.0 to v4.1.0

This is a **backward-compatible** release. No code changes are required.

All improvements are automatic and internal.

---

## Full Changelog

### Bug Fixes
- Fixed duplicate dialog display when app is paused/resumed during ad loading
- Fixed threading crashes ("Animators may only be run on Looper threads")
- Fixed app open ads showing on top of interstitial loading dialogs
- Fixed race condition allowing multiple concurrent ad shows
- Fixed `currentWelcomeDialog` not being cleared properly after dismissal

### Internal Improvements
- Added `isAppInForeground` tracking in `AppOpenManager`
- Added `pendingAdToShow` and `pendingAdCallback` for background ad handling
- Added `onStop` lifecycle callback for background detection
- Added `showPendingAdWithDialog()` for smooth ad display on return
- Wrapped all ad SDK callbacks in main thread handlers
- Updated `onStart` to check `isAdOrDialogShowing()` for interstitial priority
