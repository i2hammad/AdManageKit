package com.i2hammad.admanagekit.compose

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

/**
 * Resolves the hosting [ComponentActivity] for a Compose [Context], unwrapping any
 * [ContextWrapper] layers.
 *
 * `LocalContext.current` is the Activity itself in the common `setContent { }` case,
 * but it is a `ContextThemeWrapper` inside a `Dialog`/`ModalBottomSheet` composable,
 * under per-screen theme wrappers, and in some `AndroidView`-nested compositions.
 * Ad loading requires a real Activity, so every composable in this module resolves it
 * through here rather than testing `context is ComponentActivity` directly — that test
 * fails on a wrapped context and, with no else branch, silently loaded nothing and
 * fired no failure callback.
 *
 * Mirrors `NativeBannerSmall.findHostActivity()` in the main module.
 *
 * @return the hosting [ComponentActivity], or null if this context is not backed by one
 *         (e.g. an application or service context).
 */
internal fun Context.findComponentActivity(): ComponentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
