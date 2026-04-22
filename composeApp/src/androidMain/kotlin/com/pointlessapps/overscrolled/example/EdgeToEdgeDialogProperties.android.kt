package com.pointlessapps.overscrolled.example

import androidx.compose.ui.window.DialogProperties

actual val EdgeToEdgeDialogProperties: DialogProperties = DialogProperties(
    dismissOnBackPress = true,
    dismissOnClickOutside = true,
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)
