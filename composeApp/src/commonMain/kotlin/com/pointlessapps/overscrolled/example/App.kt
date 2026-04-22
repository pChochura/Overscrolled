package com.pointlessapps.overscrolled.example

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pointlessapps.overscrolled.OverscrolledEffectNode
import com.pointlessapps.overscrolled.rememberHorizonalOverscrolledEffect
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showDialog by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Yellow),
            contentAlignment = Alignment.Center,
        ) {
            Text("Hello there")

            Button(
                onClick = { showDialog = true },
            ) {
                Text("Show")
            }
        }

        if (showDialog) {
            Dialog { showDialog = false }
        }
    }
}

private const val startThreshold = 50f
private const val endThreshold = 200f

@Composable
private fun Dialog(
    onDismissRequest: () -> Unit,
) {
    val array = Array(5) { "Screen number: $it" }
    val colors = listOf(
        Color.Cyan, Color.Blue, Color.Green, Color.Yellow, Color.Red,
    )

    val hapticFeedback = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = EdgeToEdgeDialogProperties,
    ) {
        val state = rememberLazyListState()
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f)),
            verticalAlignment = Alignment.CenterVertically,
            overscrollEffect = rememberHorizonalOverscrolledEffect(
                startThreshold = startThreshold,
                endThreshold = endThreshold,
                onOverscrolled = { finished ->
                    if (finished) {
                        hapticFeedback.performHapticFeedback(
                            HapticFeedbackType.Confirm,
                        )
                        onDismissRequest()
                    } else {
                        hapticFeedback.performHapticFeedback(
                            HapticFeedbackType.GestureThresholdActivate,
                        )
                    }
                },
                effectNode = effectNode,
            ),
            flingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(state)),
            contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
        ) {
            itemsIndexed(array) { index, string ->
                Box(
                    modifier = Modifier.fillParentMaxWidth()
                        .height(100.dp)
                        .background(colors[index]),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(string)
                }
            }
        }
    }
}

private val effectNode = object : OverscrolledEffectNode {
    override fun node(currentOffset: () -> Offset): Modifier.Node {
        return object : Modifier.Node(), LayoutModifierNode {
            override fun MeasureScope.measure(
                measurable: Measurable,
                constraints: Constraints,
            ): MeasureResult {
                val placeable = measurable.measure(constraints)
                return layout(placeable.width, placeable.height) {
                    val offsetValue = currentOffset()
                    val threshold =
                        if (offsetValue.x < 0) endThreshold else startThreshold
                    val progress = offsetValue.x / threshold
                    placeable.placeWithLayer(
                        offsetValue.x.roundToInt(),
                        offsetValue.y.roundToInt(),
                        layerBlock = {
                            alpha = (1 - progress.absoluteValue).coerceAtLeast(0.3f)
                            scaleX = 1 - (0.05f * progress.absoluteValue)
                            scaleY = 1 - (0.05f * progress.absoluteValue)
                            transformOrigin = TransformOrigin(
                                pivotFractionX = if (progress > 0) 1f else 0f,
                                pivotFractionY = 0.5f,
                            )
                        },
                    )
                }
            }
        }
    }
}
