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

// GMA Next-Gen SDK types.
// Both aliases point at LoadAdError: every call site across this public API
// surface is a load-failure callback (onFailedToLoad/onAdFailedToLoad) - the
// Next-Gen SDK's show-failure type, common.FullScreenContentError, is a
// separate, unrelated type and is handled internally by AdManager/
// AppOpenManager/the AdMob providers, not exposed through these aliases.
typealias AdKitError = com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
typealias AdKitLoadError = com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
typealias AdKitValue = com.google.android.libraries.ads.mobile.sdk.common.AdValue