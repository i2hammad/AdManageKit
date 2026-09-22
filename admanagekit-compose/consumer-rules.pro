# Consumer rules for ad-manage-kit-compose.
#
# Deliberately empty of -keep rules.
#
# A consumer rule is applied to every app that depends on this module, so
# anything kept here is kept in every consumer's release build and can never be
# shrunk, optimized or obfuscated by their R8. Earlier versions of this file
# shipped three package-wide keeps that cost consumers dearly:
#
#   -keep class androidx.compose.**        7221 classes / 57262 methods pinned
#   -keep class com.google.android.gms.ads.**    97 classes /   506 methods pinned
#   -keep class com.i2hammad.admanagekit.compose.**  47 classes /  446 methods pinned
#
# None of them were load-bearing. The composables in this module are ordinary
# Kotlin functions that reach the view layer through `AndroidView` factory
# lambdas; there is no reflection, no `Class.forName`, no resource-name lookup
# and no serialization anywhere in the module, so R8 can see every edge it
# needs from the consumer's own call sites. Compose ships its own consumer
# rules (`-keep,allowobfuscation,allowshrinking class * extends
# androidx.compose.ui.node.ModifierNodeElement` and friends) and the ads SDK
# ships its own; re-keeping those packages here only defeated them.
#
# Before adding a rule to this file, confirm the member really is reached
# reflectively at runtime and keep it by exact name — never by package wildcard.
