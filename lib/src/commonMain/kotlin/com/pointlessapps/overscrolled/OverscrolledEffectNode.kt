package com.pointlessapps.overscrolled

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity

/**
 * Creates an [OverscrolledEffectNode] with the provided [nodeBuilder].
 *
 * @param nodeBuilder A builder that returns a [Modifier.Node] that applies the overscroll
 * transformation.
 */
fun createOverscrolledEffectNode(
    nodeBuilder: (currentProgress: () -> OverscrolledProgress) -> Modifier.Node,
) = OverscrolledEffectNode { currentProgress -> nodeBuilder(currentProgress) }

/**
 * An interface that allows the caller to define exactly how the content is transformed when it is
 * being overscrolled.
 */
fun interface OverscrolledEffectNode {
    /**
     * Returns a [androidx.compose.ui.Modifier.Node] that applies the overscroll transformation.
     *
     * @param currentProgress A provider for the current overscroll progress (from 0f to 1f).
     */
    fun node(currentProgress: () -> OverscrolledProgress): Modifier.Node

    fun onApplyToFling(velocity: Velocity) = Unit
    fun onApplyToScroll(delta: Offset) = Unit
}
