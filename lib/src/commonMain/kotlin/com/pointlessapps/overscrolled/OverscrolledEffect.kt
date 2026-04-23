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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

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
    onOverscrolled: (finished: Boolean) -> Unit,
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
    onOverscrolled: (finished: Boolean) -> Unit,
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
    onOverscrolled: (finished: Boolean) -> Unit,
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
    onOverscrolled: (finished: Boolean) -> Unit,
    onProgressChanged: (progress: Float) -> Unit,
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
            onProgressChanged = onProgressChanged,
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
            absoluteOffset = currentOffset.value(),
            progress = currentOffset.calculateProgress(),
            direction = if (currentOffset.value() < 0f) {
                OverscrolledProgress.Direction.FromStart
            } else {
                OverscrolledProgress.Direction.FromEnd
            },
        )
    }

    init {
        scope.launch {
            snapshotFlow(offsetAnimatable::value)
                .map { it.calculateProgress() }
                .distinctUntilChanged()
                .onEach { onProgressChanged(it) }
                .filter { it >= 1f }
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
            scope.launch { offsetAnimatable.snapTo(currentOffset + unconsumed) }
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

    private fun Offset.calculateProgress() = value().let {
        (if (it < 0f) -it / endThreshold else it / startThreshold).fastCoerceIn(
            minimumValue = 0f,
            maximumValue = 1f,
        )
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

data class OverscrolledProgress(
    val absoluteOffset: Float,
    val progress: Float,
    val direction: Direction,
) {
    enum class Direction { FromStart, FromEnd }
}

private object NoOpEffectNode : OverscrolledEffectNode {
    override fun node(currentProgress: () -> OverscrolledProgress) = object : Modifier.Node() {}
}

/**
 * An interface that allows the caller to define exactly how the content is transformed when it is
 * being overscrolled.
 */
fun interface OverscrolledEffectNode {
    /**
     * Returns a [Modifier.Node] that applies the overscroll transformation.
     *
     * @param currentProgress A provider for the current overscroll progress (from 0f to 1f).
     */
    fun node(currentProgress: () -> OverscrolledProgress): Modifier.Node
}

/**
 * Creates an [OverscrolledEffectNode] with the provided [nodeBuilder].
 *
 * @param nodeBuilder A builder that returns a [Modifier.Node] that applies the overscroll
 * transformation.
 */
fun createOverscrolledEffectNode(
    nodeBuilder: (currentProgress: () -> OverscrolledProgress) -> Modifier.Node,
) = OverscrolledEffectNode { currentProgress -> nodeBuilder(currentProgress) }
