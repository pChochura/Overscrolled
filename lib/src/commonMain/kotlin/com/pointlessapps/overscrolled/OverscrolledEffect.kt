package com.pointlessapps.overscrolled

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Remembers a horizontal [OverscrollEffect] that moves the content in the same direction as the
 * scroll happens with a dampening force.
 *
 * @param threshold The threshold in pixels after which the [onOverscrolled] callback is invoked.
 * @param onOverscrolled The callback invoked once the threshold is met and it contains a parameter
 * that indicates whether the user has lifted the finger.
 * @param effectNode The [OverscrolledEffectNode] that defines how the content is transformed.
 */
@Composable
fun rememberHorizonalOverscrolledEffect(
    threshold: Float,
    onOverscrolled: (finished: Boolean, direction: Direction) -> Unit,
    onProgressChanged: (progress: Float) -> Unit = {},
    effectNode: OverscrolledEffectNode? = null,
) = rememberHorizonalOverscrolledEffect(
    startThreshold = threshold,
    endThreshold = threshold,
    onOverscrolled = onOverscrolled,
    onProgressChanged = onProgressChanged,
    effectNode = effectNode,
)

/**
 * Remembers a horizontal [OverscrollEffect] that moves the content in the same direction as the
 * scroll happens with a dampening force.
 *
 * @param startThreshold The threshold in pixels for the start side after which the [onOverscrolled]
 * callback is invoked.
 * @param endThreshold The threshold in pixels for the end side after which the [onOverscrolled]
 * callback is invoked.
 * @param onOverscrolled The callback invoked once the threshold is met and it contains a parameter
 * that indicates whether the user has lifted the finger.
 * @param effectNode The [OverscrolledEffectNode] that defines how the content is transformed.
 */
@Composable
fun rememberHorizonalOverscrolledEffect(
    startThreshold: Float,
    endThreshold: Float,
    onOverscrolled: (finished: Boolean, direction: Direction) -> Unit,
    onProgressChanged: (progress: Float) -> Unit = {},
    effectNode: OverscrolledEffectNode? = null,
) = rememberOverscrolledEffect(
    orientation = Orientation.Horizontal,
    startThreshold = startThreshold,
    endThreshold = endThreshold,
    onOverscrolled = onOverscrolled,
    onProgressChanged = onProgressChanged,
    effectNode = effectNode,
)

/**
 * Remembers a vertical [OverscrollEffect] that moves the content in the same direction as the
 * scroll happens with a dampening force.
 *
 * @param threshold The threshold in pixels after which the [onOverscrolled] callback is invoked.
 * @param onOverscrolled The callback invoked once the threshold is met and it contains a parameter
 * that indicates whether the user has lifted the finger.
 * @param effectNode The [OverscrolledEffectNode] that defines how the content is transformed.
 */
@Composable
fun rememberVerticalOverscrolledEffect(
    threshold: Float,
    onOverscrolled: (finished: Boolean, direction: Direction) -> Unit,
    onProgressChanged: (progress: Float) -> Unit = {},
    effectNode: OverscrolledEffectNode? = null,
) = rememberVerticalOverscrolledEffect(
    startThreshold = threshold,
    endThreshold = threshold,
    onOverscrolled = onOverscrolled,
    onProgressChanged = onProgressChanged,
    effectNode = effectNode,
)

/**
 * Remembers a vertical [OverscrollEffect] that moves the content in the same direction as the
 * scroll happens with a dampening force.
 *
 * @param startThreshold The threshold in pixels for the start side after which the [onOverscrolled]
 * callback is invoked.
 * @param endThreshold The threshold in pixels for the end side after which the [onOverscrolled]
 * callback is invoked.
 * @param onOverscrolled The callback invoked once the threshold is met and it contains a parameter
 * that indicates whether the user has lifted the finger.
 * @param effectNode The [OverscrolledEffectNode] that defines how the content is transformed.
 */
@Composable
fun rememberVerticalOverscrolledEffect(
    startThreshold: Float,
    endThreshold: Float,
    onOverscrolled: (finished: Boolean, direction: Direction) -> Unit,
    onProgressChanged: (progress: Float) -> Unit = {},
    effectNode: OverscrolledEffectNode? = null,
) = rememberOverscrolledEffect(
    orientation = Orientation.Vertical,
    startThreshold = startThreshold,
    endThreshold = endThreshold,
    onOverscrolled = onOverscrolled,
    onProgressChanged = onProgressChanged,
    effectNode = effectNode,
)

/**
 * Internal implementation of the overscrolled effect.
 *
 * @param orientation The orientation of the scroll.
 * @param startThreshold The threshold in pixels for the start side.
 * @param endThreshold The threshold in pixels for the end side.
 * @param onOverscrolled The callback invoked once the threshold is met.
 * @param effectNode The [OverscrolledEffectNode] that defines how the content is transformed.
 */
@Composable
private fun rememberOverscrolledEffect(
    orientation: Orientation,
    startThreshold: Float,
    endThreshold: Float,
    onOverscrolled: (finished: Boolean, direction: Direction) -> Unit,
    onProgressChanged: (progress: Float) -> Unit,
    effectNode: OverscrolledEffectNode? = null,
): OverscrollEffect {
    val scope = rememberCoroutineScope()
    val defaultEffect = rememberOverscrollEffect()

    return remember(defaultEffect) {
        OverscrollEffectImpl(
            effectNode = effectNode ?: PlatformDefaultEffectNode(
                overscrollEffect = defaultEffect,
                coroutineScope = scope,
            ),
            orientation = orientation,
            startThreshold = startThreshold,
            endThreshold = endThreshold,
            onOverscrolled = onOverscrolled,
            onProgressChanged = onProgressChanged,
            scope = scope,
        )
    }
}

private data class PlatformDefaultEffectNode(
    private val overscrollEffect: OverscrollEffect?,
    private val coroutineScope: CoroutineScope,
) : OverscrolledEffectNode {
    override fun node(currentProgress: () -> OverscrolledProgress) =
        overscrollEffect?.node as? Modifier.Node ?: object : Modifier.Node() {}

    override fun onApplyToScroll(delta: Offset) {
        overscrollEffect?.applyToScroll(delta, NestedScrollSource.UserInput) { it - delta }
    }

    override fun onApplyToFling(velocity: Velocity) {
        coroutineScope.launch {
            overscrollEffect?.applyToFling(velocity) { Velocity.Zero }
        }
    }
}
