package com.pointlessapps.overscrolled

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.absoluteValue

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

@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
) {
    val array = Array(5) { "Screen number: $it" }
    val colors = listOf(
        Color.Cyan, Color.Blue, Color.Green, Color.Yellow, Color.Red,
    )

    var reachedConfirmation by remember { mutableStateOf(false) }
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
            overscrollEffect = rememberOverscrolledEffect(
                orientation = Orientation.Horizontal,
                threshold = 300f,
                effect = { progress ->
                    alpha = 1 - progress.absoluteValue
                    scaleX = 1 - (0.05f * progress.absoluteValue)
                    scaleY = 1 - (0.05f * progress.absoluteValue)
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (progress > 0) 1f else 0f,
                        pivotFractionY = 0.5f,
                    )
                },
                onOverscrolled = { progress, finished ->
                    if (progress.absoluteValue > 0.4) {
                        if (!reachedConfirmation) {
                            reachedConfirmation = true
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.GestureThresholdActivate,
                            )
                        }

                        if (finished) {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.Confirm,
                            )
                            onDismissRequest()
                        }
                    } else {
                        reachedConfirmation = false
                    }
                }
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
