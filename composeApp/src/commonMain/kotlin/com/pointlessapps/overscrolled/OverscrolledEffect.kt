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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * An [OverscrollEffect] implementation that moves the content with a dampening force
 * and returns the offset on every frame to the caller with the information whether it was finished.
 */
@Composable
fun rememberOverscrolledEffect(
    orientation: Orientation,
    threshold: Float,
    effect: GraphicsLayerScope.(progress: Float) -> Unit,
    onOverscrolled: (progress: Float, finished: Boolean) -> Unit,
): OverscrollEffect {
    val scope = rememberCoroutineScope()

    return remember {
        OverscrollEffectImpl(
            orientation = orientation,
            threshold = threshold,
            effect = effect,
            onOverscrolled = onOverscrolled,
            scope = scope,
        )
    }
}

private class OverscrollEffectImpl(
    private val orientation: Orientation,
    private val threshold: Float,
    private val effect: GraphicsLayerScope.(progress: Float) -> Unit,
    private val onOverscrolled: (progress: Float, finished: Boolean) -> Unit,
    private val scope: CoroutineScope,
) : OverscrollEffect {

    private val offsetAnimatable = Animatable(Offset.Zero, Offset.VectorConverter)
    private val currentOffset: Offset
        get() = offsetAnimatable.value

    override val isInProgress: Boolean
        get() = currentOffset != Offset.Zero

    override val node = OffsetPxNode(
        graphicsLayerCallback = { effect(currentOffset.value() / threshold) },
        offset = { offsetAnimatable.value.round() },
    )

    init {
        scope.launch {
            snapshotFlow(offsetAnimatable::value).collect {
                onOverscrolled(currentOffset.value() / threshold, false)
            }
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
                onOverscrolled(currentOffset.value() / threshold, true)
                offsetAnimatable.animateTo(
                    Offset.Zero,
                    initialVelocity = Offset(velocity.x, velocity.y),
                )
            }
        }
    }
}

private class OffsetPxNode(
    private val graphicsLayerCallback: GraphicsLayerScope.() -> Unit,
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
                layerBlock = graphicsLayerCallback,
            )
        }
    }
}
