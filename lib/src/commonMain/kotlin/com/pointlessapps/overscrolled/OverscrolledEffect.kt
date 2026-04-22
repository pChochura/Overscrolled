package com.pointlessapps.overscrolled

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.round
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * An [OverscrollEffect] implementation that moves the content in the same direction as the scroll
 * happens with a dampening force.
 *
 * It also exposes a [layerBlock] that allows the caller to control exactly how to style the content
 * when it is being overscrolled. The provided [layerBlock(progress)] is signed to inform in which
 * direction the content is being overscrolled.
 * The [onOverscrolled] callback is invoked once the threshold is met and it contains a parameter
 * that indicates whether the user has lifted the finger.
 *
 * @deprecated Use rememberHorizonalOverscrolledEffect or rememberVerticalOverscrolledEffect instead.
 */
@Deprecated(
    message = "Use rememberHorizonalOverscrolledEffect or rememberVerticalOverscrolledEffect instead.",
    level = DeprecationLevel.WARNING
)
@Composable
fun rememberOverscrolledEffect(
    orientation: Orientation,
    threshold: Float,
    layerBlock: GraphicsLayerScope.(progress: Float) -> Unit,
    onOverscrolled: (finished: Boolean) -> Unit,
) = rememberOverscrolledEffect(
    orientation = orientation,
    startThreshold = threshold,
    endThreshold = threshold,
    onOverscrolled = onOverscrolled,
    effectNode = object : OverscrolledEffectNode {
        private fun Offset.value() = when (orientation) {
            Orientation.Vertical -> y
            Orientation.Horizontal -> x
        }

        override fun node(currentOffset: () -> Offset) = OffsetPxNode(
            layerBlock = { layerBlock(currentOffset().value() / threshold) },
            offset = { currentOffset().round() },
        )
    },
)

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
    onOverscrolled: (finished: Boolean) -> Unit,
    effectNode: OverscrolledEffectNode? = null,
) = rememberHorizonalOverscrolledEffect(
    startThreshold = threshold,
    endThreshold = threshold,
    onOverscrolled = onOverscrolled,
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
    onOverscrolled: (finished: Boolean) -> Unit,
    effectNode: OverscrolledEffectNode? = null,
) = rememberOverscrolledEffect(
    orientation = Orientation.Horizontal,
    startThreshold = startThreshold,
    endThreshold = endThreshold,
    onOverscrolled = onOverscrolled,
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
    onOverscrolled: (finished: Boolean) -> Unit,
    effectNode: OverscrolledEffectNode? = null,
) = rememberVerticalOverscrolledEffect(
    startThreshold = threshold,
    endThreshold = threshold,
    onOverscrolled = onOverscrolled,
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
    onOverscrolled: (finished: Boolean) -> Unit,
    effectNode: OverscrolledEffectNode? = null,
) = rememberOverscrolledEffect(
    orientation = Orientation.Vertical,
    startThreshold = startThreshold,
    endThreshold = endThreshold,
    onOverscrolled = onOverscrolled,
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
    onOverscrolled: (finished: Boolean) -> Unit,
    effectNode: OverscrolledEffectNode? = null,
): OverscrollEffect {
    val scope = rememberCoroutineScope()

    return remember {
        OverscrollEffectImpl(
            effectNode = effectNode ?: NoOpEffectNode,
            orientation = orientation,
            startThreshold = startThreshold,
            endThreshold = endThreshold,
            onOverscrolled = onOverscrolled,
            scope = scope,
        )
    }
}

private class OverscrollEffectImpl(
    effectNode: OverscrolledEffectNode,
    private val orientation: Orientation,
    private val startThreshold: Float,
    private val endThreshold: Float,
    private val onOverscrolled: (finished: Boolean) -> Unit,
    private val scope: CoroutineScope,
) : OverscrollEffect {

    private val offsetAnimatable = Animatable(Offset.Zero, Offset.VectorConverter)
    private val currentOffset: Offset
        get() = offsetAnimatable.value

    override val isInProgress: Boolean
        get() = currentOffset != Offset.Zero

    override val node = effectNode.node(::currentOffset)

    init {
        scope.launch {
            snapshotFlow(offsetAnimatable::value)
                .map { it.value() }
                .distinctUntilChangedBy { it <= -endThreshold || it >= startThreshold }
                .filter { it <= -endThreshold || it >= startThreshold }
                .collect { onOverscrolled(false) }
        }
    }

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset {
        if (source != NestedScrollSource.UserInput) {
            return performScroll(delta)
        }

        // 1. First, try to scroll back from an existing overscroll.
        val consumedByOverscroll = if (currentOffset != Offset.Zero) {
            val newOffset = currentOffset + delta
            val consumed = Offset(
                x = if (delta.x.isOppositeSign(currentOffset.x)) {
                    val consumedX =
                        if (abs(delta.x) > abs(currentOffset.x)) currentOffset.x else delta.x
                    consumedX
                } else 0f,
                y = if (delta.y.isOppositeSign(currentOffset.y)) {
                    val consumedY =
                        if (abs(delta.y) > abs(currentOffset.y)) currentOffset.y else delta.y
                    consumedY
                } else 0f,
            )

            // Update the animatable with the new offset as we scroll back.
            scope.launch { offsetAnimatable.snapTo(newOffset.coerceIn(currentOffset, consumed)) }

            consumed
        } else {
            Offset.Zero
        }

        val remainingDelta = delta - consumedByOverscroll
        val consumedByContent = performScroll(remainingDelta)

        val unconsumed = remainingDelta - consumedByContent
        if (unconsumed != Offset.Zero) {
            val dampenedUnconsumed = Offset(dampen(unconsumed.x), dampen(unconsumed.y))
            scope.launch { offsetAnimatable.snapTo(currentOffset + dampenedUnconsumed) }
        }

        return consumedByOverscroll + consumedByContent + unconsumed
    }

    private fun Float.isOppositeSign(other: Float) =
        this.sign != other.sign && this != 0f && other != 0f

    private fun Offset.coerceIn(min: Offset, max: Offset) = Offset(
        x = x.coerceIn(min(min.x, max.x), max(max.x, min.x)),
        y = y.coerceIn(min(min.y, max.y), max(max.y, min.y)),
    )

    private fun Offset.value() = when (orientation) {
        Orientation.Vertical -> y
        Orientation.Horizontal -> x
    }

    private fun dampen(value: Float, multiplier: Float = 0.3f) = value * multiplier

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        // Launching the fling and the overscroll animation at the same time with the same velocity
        // to avoid stuttering
        scope.launch { performFling(velocity) }
        scope.launch {
            if (currentOffset != Offset.Zero) {
                if (currentOffset.value().let { it <= -endThreshold || it >= startThreshold }) {
                    onOverscrolled(true)
                }
                offsetAnimatable.animateTo(
                    Offset.Zero,
                    initialVelocity = Offset(velocity.x, velocity.y),
                )
            }
        }
    }
}

private class OffsetPxNode(
    private val layerBlock: GraphicsLayerScope.() -> Unit,
    private val offset: Density.() -> IntOffset,
) : LayoutModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val offsetValue = offset()
            placeable.placeWithLayer(
                offsetValue.x,
                offsetValue.y,
                layerBlock = layerBlock,
            )
        }
    }
}

private object NoOpEffectNode : OverscrolledEffectNode {
    override fun node(currentOffset: () -> Offset) = object : Modifier.Node() {}
}

/**
 * An interface that allows the caller to define exactly how the content is transformed when it is
 * being overscrolled.
 */
interface OverscrolledEffectNode {
    /**
     * Returns a [Modifier.Node] that applies the overscroll transformation.
     *
     * @param currentOffset A provider for the current overscroll offset.
     */
    fun node(currentOffset: () -> Offset): Modifier.Node
}
