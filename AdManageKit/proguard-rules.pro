# Rules applied when THIS module is minified.
#
# It is not: `isMinifyEnabled = false` for the release build type, deliberately.
# A published AAR must keep its public API under its real names or consumers
# cannot compile against it, and shrinking here would only remove code the
# consuming app's R8 removes anyway — with far better whole-program
# information. Minification for this library happens in the consuming app.
#
# This file therefore has no effect today; it exists so the build keeps working
# if minification is ever switched on for a local experiment. The rules that
# actually ship to consumers live in `consumer-rules.pro`.
#
# Previously this file ended with `-keep class * { *; }`, which would have
# disabled shrinking, optimization and obfuscation for the entire program the
# moment anyone flipped `isMinifyEnabled` to true.

# Preserve stack traces through the library's frames.
-keepattributes SourceFile,LineNumberTable
