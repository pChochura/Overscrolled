package com.pointlessapps.overscrolled

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

internal class OverscrollEffectImpl(
    effectNode: OverscrolledEffectNode,
    private val orientation: Orientation,
    private val startThreshold: Float,
    private val endThreshold: Float,
    private val onOverscrolled: (finished: Boolean, direction: Direction) -> Unit,
    private val onProgressChanged: (progress: Float) -> Unit,
    private val scope: CoroutineScope,
) : OverscrollEffect {

    private val offsetAnimatable = Animatable(Offset.Zero, Offset.VectorConverter)
    private val currentOffset: Offset
        get() = offsetAnimatable.value

    override val isInProgress: Boolean
        get() = currentOffset != Offset.Zero

    override val node = effectNode.node {
        OverscrolledProgress(
            absoluteOffset = currentOffset.value(orientation),
            progress = currentOffset.calculateProgress(
                orientation,
                startThreshold,
                endThreshold,
            ).absoluteValue,
            direction = if (currentOffset.value(orientation) < 0f) {
                Direction.FromEnd
            } else {
                Direction.FromStart
            },
        )
    }

    init {
        scope.launch {
            snapshotFlow(offsetAnimatable::value)
                .map { it.calculateProgress(orientation, startThreshold, endThreshold) }
                .distinctUntilChanged()
                .onEach { onProgressChanged(it) }
                .filter { it.absoluteValue >= 1f }
                .collect {
                    onOverscrolled(
                        false,
                        if (it < 0f) Direction.FromEnd else Direction.FromStart,
                    )
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
            scope.launch { offsetAnimatable.snapTo(currentOffset + unconsumed) }
        }

        return consumedByOverscroll + consumedByContent + unconsumed
    }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        // Launching the fling and the overscroll animation at the same time with the same velocity
        // to avoid stuttering
        scope.launch { performFling(velocity) }
        scope.launch {
            if (currentOffset != Offset.Zero) {
                val offsetValue = currentOffset.value(orientation)
                if (offsetValue.let { it <= -endThreshold || it >= startThreshold }) {
                    onOverscrolled(
                        true,
                        if (offsetValue < 0f) Direction.FromEnd else Direction.FromStart,
                    )
                }
                offsetAnimatable.animateTo(
                    Offset.Zero,
                    initialVelocity = Offset(velocity.x, velocity.y),
                )
            }
        }
    }
}

private fun Float.isOppositeSign(other: Float) =
    this.sign != other.sign && this != 0f && other != 0f

private fun Offset.coerceIn(min: Offset, max: Offset) = Offset(
    x = x.coerceIn(min(min.x, max.x), max(max.x, min.x)),
    y = y.coerceIn(min(min.y, max.y), max(max.y, min.y)),
)

private fun Offset.value(orientation: Orientation) = when (orientation) {
    Orientation.Vertical -> y
    Orientation.Horizontal -> x
}

private fun Offset.calculateProgress(
    orientation: Orientation,
    startThreshold: Float,
    endThreshold: Float,
) = value(orientation).let {
    (if (it < 0f) it / endThreshold else it / startThreshold).fastCoerceIn(
        minimumValue = -1f,
        maximumValue = 1f,
    )
}
