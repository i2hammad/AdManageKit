# Consumer rules for ad-manage-kit-yandex.
#
# Deliberately empty of -keep rules.
#
# The Yandex Mobile Ads SDK ships its own consumer rules (`-keepnames class
# yads.**`, `com.yandex.mobile.ads.**`, `com.monetization.ads.**` and the
# AppMetrica keeps it pulls in). Restating them here would pin the same classes
# twice and take away the `allowshrinking` latitude the SDK's own rules grant.
#
# Our provider classes are registered through `AdProviderConfig` from the app's
# own code, so R8 reaches them from the call site.
#
# One thing worth knowing when editing: `YandexNativeProvider` locates template
# assets with `Resources.getIdentifier("ad_media", "id", ...)` and theme colors
# with `getIdentifier("colorSurface", "attr", ...)`. Those ids live in the
# native template layouts of the `ad-manage-kit` module, which are referenced
# statically as `R.layout.*` constants, so the resource shrinker keeps them;
# the Material attrs come from `com.google.android.material`. Every lookup
# falls back safely when it resolves to 0, so a stripped resource degrades the
# rendering rather than crashing — but if you add a new asset id that is *only*
# reached by name, it needs a `tools:keep` entry in `res/raw/keep.xml`.
