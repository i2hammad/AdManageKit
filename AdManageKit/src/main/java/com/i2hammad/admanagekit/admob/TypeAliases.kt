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

// Next-Gen SDK types
typealias AdKitError = com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
typealias AdKitValue = com.google.android.libraries.ads.mobile.sdk.common.AdValue
