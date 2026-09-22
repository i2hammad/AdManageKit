# Consumer rules for ad-manage-kit.
#
# Deliberately empty of -keep rules.
#
# Everything this module exposes is reached through ordinary compiled call
# sites, so R8 in the consuming app already sees it:
#
# - Managers (`AdManager`, `AppOpenManager`, `NativeAdManager`,
#   `NativeAdIntegrationManager`) and `AdManageKitConfig` are Kotlin objects
#   the app calls directly.
# - Callback contracts (`AdLoadCallback`, `AdManagerCallback`, ...) are
#   interfaces the app implements; the implementations are its own classes.
# - The ad views (`BannerAdView`, `NativeBannerSmall/Medium`, `NativeLarge`,
#   `NativeTemplateView`) only need keeping when an app inflates them from XML,
#   and AAPT2 already emits the matching `-keep class <view> { <init>(...); }`
#   rule into the generated `aapt_rules.txt` for exactly the views each app's
#   layouts reference. Keeping them here would pin all of them in every app.
# - The 38 native templates are referenced statically as `R.layout.*` constants
#   from `NativeAdTemplate`, so both R8 and the resource shrinker keep them and
#   the `ad_headline` / `ad_media` / ... ids inside them.
#
# The module contains no reflection, no `Class.forName`, no JNI, no
# `Parcelable`/`Serializable` types and no reflective serialization, so there is
# nothing here that shrinking can break.
#
# Before adding a rule to this file, confirm the member really is reached
# reflectively at runtime and keep it by exact name — never by package wildcard.
# A rule added here is paid for by every app that depends on the library.
