package com.i2hammad.admanagekit.admob

/**
 * Type aliases for SDK-agnostic ad types.
 *
 * These aliases allow code to work with both the legacy GMS Ads SDK
 * and the Next-Gen GMA SDK without changes to callback signatures.
 *
 * When migrating between SDK versions, only the library dependency changes -
 * your callback implementations remain the same.
 */

// Legacy GMS SDK types
typealias AdKitError = com.google.android.gms.ads.AdError
typealias AdKitLoadError = com.google.android.gms.ads.LoadAdError
typealias AdKitValue = com.google.android.gms.ads.AdValue