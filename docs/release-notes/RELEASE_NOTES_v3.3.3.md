# Release Notes - v3.3.3

## Highlights

- **SDK-Agnostic Type Aliases**: Callbacks now use type aliases for migration compatibility between GMS SDK and Next-Gen SDK versions
- **Migration Ready**: Same callback signatures work on both `main` and `nextgen` branches

---

## New Features

### SDK-Agnostic Type Aliases

Added type aliases that abstract SDK-specific types, enabling seamless migration between the legacy GMS SDK (main branch) and the Next-Gen GMA SDK (nextgen branch).

#### TypeAliases.kt

```kotlin
// Main branch (GMS SDK)
typealias AdKitError = com.google.android.gms.ads.AdError
typealias AdKitLoadError = com.google.android.gms.ads.LoadAdError
typealias AdKitValue = com.google.android.gms.ads.AdValue

// Nextgen branch (Next-Gen GMA SDK)
typealias AdKitError = com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
typealias AdKitValue = com.google.android.libraries.ads.mobile.sdk.common.AdValue
```

#### Updated Callbacks

All callback interfaces now use type aliases:

```kotlin
// AdLoadCallback
open fun onFailedToLoad(error: AdKitError?) { }
open fun onPaidEvent(adValue: AdKitValue) { }

// AdManagerCallback
open fun onFailedToLoad(error: AdKitError?) { }

// OnAdFailedListener / InterstitialAdCallback
fun onAdFailed(error: AdKitError)
```

#### Usage

Your callback implementations work identically on both branches:

```kotlin
nativeTemplateView.loadNativeAd(activity, adUnitId, object : AdLoadCallback() {
    override fun onAdLoaded() {
        Log.d("Ads", "Ad loaded successfully")
    }

    override fun onFailedToLoad(error: AdKitError?) {  // Same on both branches
        Log.e("Ads", "Failed to load: ${error?.message}")
    }

    override fun onPaidEvent(adValue: AdKitValue) {    // Same on both branches
        trackRevenue(adValue.valueMicros)
    }
})
```

---

## Migration Guide

### From v3.3.2 to v3.3.3

This is a **backward-compatible** release. No code changes required.

**Optional Migration**:

If you're preparing to migrate to the Next-Gen SDK version:

1. Update your callback implementations to use type aliases
2. Avoid direct references to SDK-specific types in your callbacks
3. When ready, switch to `nextgen` branch artifacts

```groovy
// Main branch (current)
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit:v3.3.3'

// Next-Gen branch (when ready to migrate)
implementation 'com.github.i2hammad.AdManageKit:ad-manage-kit-nextgen:v4.1.1'
```

---

## Next-Gen SDK Information

The `nextgen` branch offers additional features:

| Feature | Main Branch | Next-Gen Branch |
|---------|-------------|-----------------|
| SDK | GMS play-services-ads | GMA Next-Gen SDK |
| Ad Loading | Traditional load/show | Preloader-based |
| Threading | Manual dispatch | Automatic |
| Buffer System | N/A | Configurable |

See the [README](../../README.md#next-gen-gma-sdk-version) for more details.

---

## Full Changelog

### New Features
- Added `TypeAliases.kt` with SDK-agnostic type aliases
- Updated `AdLoadCallback` to use `AdKitError` and `AdKitValue`
- Updated `AdManagerCallback` to use `AdKitError`
- Updated `AdCallback` interfaces to use `AdKitError`

### Documentation
- Added Next-Gen SDK section to README
- Added migration compatibility information
