package com.pointlessapps.overscrolled

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.util.fastForEach
import kotlin.math.sign

fun SnapLayoutInfoProvider(
    lazyListState: LazyListState,
    snapPosition: SnapPosition = SnapPosition.Center,
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {

    private val layoutInfo: LazyListLayoutInfo
        get() = lazyListState.layoutInfo

    override fun calculateApproachOffset(velocity: Float, decayOffset: Float) = velocity.sign

    override fun calculateSnapOffset(velocity: Float): Float {
        var lowerBoundOffset = Float.NEGATIVE_INFINITY
        var upperBoundOffset = Float.POSITIVE_INFINITY

        layoutInfo.visibleItemsInfo.fastForEach { item ->
            val desiredDistance = snapPosition.position(
                layoutSize = layoutInfo.singleAxisViewportSize,
                itemSize = item.size,
                beforeContentPadding = layoutInfo.beforeContentPadding,
                afterContentPadding = layoutInfo.afterContentPadding,
                itemIndex = item.index,
                itemCount = layoutInfo.totalItemsCount,
            ).toFloat()
            val offset = item.offset - desiredDistance

            // Find item that is closest to the center
            if (offset <= 0 && offset > lowerBoundOffset) {
                lowerBoundOffset = offset
            }

            // Find item that is closest to center, but after it
            if (offset >= 0 && offset < upperBoundOffset) {
                upperBoundOffset = offset
            }
        }

        return if (velocity > 0f) {
            upperBoundOffset
        } else {
            lowerBoundOffset
        }
    }

    private val LazyListLayoutInfo.singleAxisViewportSize: Int
        get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width
}
