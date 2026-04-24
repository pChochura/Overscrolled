package com.pointlessapps.overscrolled

/**
 * A data class that holds information about the current overscroll progress.
 *
 * @property absoluteOffset The absolute offset in pixels.
 * @property progress The progress of the overscroll (from 0.0 to 1.0).
 * @property direction The direction from which the overscroll happened.
 */
data class OverscrolledProgress(
    val absoluteOffset: Float,
    val progress: Float,
    val direction: Direction,
)

/**
 * An enum that defines the direction from which the overscroll happened.
 */
enum class Direction {
    /**
     * Indicates the overscroll happened from the start/top side.
     */
    FromStart,

    /**
     * Indicates the overscroll happened from the end/bottom side.
     */
    FromEnd
}
