package com.i2hammad.admanagekit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * Whether an ad slot should currently occupy space in the layout.
 *
 * Ad views hide themselves (`visibility = GONE`) when a load fails or the user is
 * premium, but that has no effect on the Compose node wrapping them: the height is
 * claimed by a Compose modifier, so the view disappears and the gap remains. Compose
 * wrappers therefore track the slot's state here and drive their own height from it,
 * rather than relying on the view's visibility.
 */
internal enum class AdSlotState {
    /** Request in flight - reserve the ad's height so the layout doesn't jump on fill. */
    LOADING,

    /** Ad is displayed - reserve its height. */
    SHOWN,

    /** Load failed, or ads are disabled for this user - occupy no space at all. */
    HIDDEN;

    /** True while this slot should reserve its ad height. */
    val occupiesSpace: Boolean get() = this != HIDDEN
}

/**
 * Remembers the [AdSlotState] for one ad slot, resetting to [AdSlotState.LOADING]
 * whenever [keys] change (i.e. a different ad is being requested).
 */
@Composable
internal fun rememberAdSlotState(vararg keys: Any?): MutableState<AdSlotState> =
    remember(*keys) { mutableStateOf(AdSlotState.LOADING) }
