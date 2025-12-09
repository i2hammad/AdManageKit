# Release Notes - v3.1.0

## Highlights

- **FRESH_WITH_CACHE_FALLBACK Fix**: Successfully loaded ads are now properly cached for future fallback scenarios
- **New Template**: `MEDIUM_HORIZONTAL` - 55/45 media-content horizontal split layout
- **Test Activity**: Video ad toggle and updated template documentation

---

## New Features

### FRESH_WITH_CACHE_FALLBACK Strategy - Now Caches Successfully Loaded Ads

The `FRESH_WITH_CACHE_FALLBACK` strategy now works as documented. Successfully loaded fresh ads are automatically cached for future fallback use.

**Before v3.1.0:** Fresh ads were displayed but NOT cached, making fallback ineffective over time.

**After v3.1.0:** Fresh ads are displayed AND cached, building up the cache for better fallback availability.

```kotlin
// Perfect for RecyclerView scenarios
AdManageKitConfig.nativeLoadingStrategy = AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK

// How it works now:
// 1. Try to load fresh ad from network
// 2. If SUCCESS → Display ad AND cache it for future fallback
// 3. If FAIL → Fall back to cached ad (if available)
// 4. Result: Over time, cache builds up with successful loads
```

### New Native Template: MEDIUM_HORIZONTAL

A new horizontal split layout with 55% media on the left and 45% content on the right.

```xml
<com.i2hammad.admanagekit.admob.NativeTemplateView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adTemplate="medium_horizontal" />
```

```kotlin
nativeTemplateView.setTemplate(NativeAdTemplate.MEDIUM_HORIZONTAL)
```

**Layout Structure:**
- Left side: MediaView (55% width, 140dp height)
- Right side: Ad badge, app icon, headline, body, CTA button (45% width)

**Total Templates:** 24 (18 standard + 6 video)

---

## Improvements

### NativeTemplateTestActivity

- **Video Ad Toggle**: Switch between standard native ad (`2247696110`) and video native ad (`1044960115`) test units
- **Updated Notes**: Reflects all 24 available templates with accurate counts

---

## Full Changelog

### NativeTemplateView Enhancements

- Added `MEDIUM_HORIZONTAL` template with horizontal 55/45 split layout
- Added `loadingStrategy` parameter to internal `loadNewAdInternal()` method
- Fresh ads are now cached when using `FRESH_WITH_CACHE_FALLBACK` strategy

### NativeAdIntegrationManager

- Updated comment in `createFreshWithCacheFallbackCallback` to accurately describe caching behavior

### Test Activity

- Added `SwitchMaterial` for toggling between standard and video ad units
- Added `tvAdUnitInfo` to display current ad unit type
- Updated template count from 17 to 24 in layout notes

### Attrs

- Added `medium_horizontal` enum value (16)
- Shifted `spotlight` and video template values accordingly

---

## Migration Guide

Version 3.1.0 is **fully backward compatible**. No migration required.

### Recommended: Use FRESH_WITH_CACHE_FALLBACK for RecyclerView

If you're showing native ads in a RecyclerView, consider using `FRESH_WITH_CACHE_FALLBACK`:

```kotlin
// Global configuration
AdManageKitConfig.nativeLoadingStrategy = AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK

// Or per-call
nativeTemplateView.loadNativeAd(
    activity = this,
    adUnitId = "ca-app-pub-xxx/yyy",
    adCallback = callback,
    loadingStrategy = AdLoadingStrategy.FRESH_WITH_CACHE_FALLBACK
)
```

**Benefits:**
- Each item binding tries to get a fresh ad (better revenue)
- If network fails, cached ad appears immediately (no empty space)
- Successfully loaded ads build up the cache over time

---

## Dependencies

No dependency changes from v3.0.0.

---

## Bug Fixes

- **Fixed**: `FRESH_WITH_CACHE_FALLBACK` strategy not caching successfully loaded ads

---

## Known Issues

None.