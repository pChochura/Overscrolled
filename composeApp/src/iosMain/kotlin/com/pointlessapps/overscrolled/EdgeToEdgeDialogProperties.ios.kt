package com.pointlessapps.overscrolled

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
actual val EdgeToEdgeDialogProperties: DialogProperties = DialogProperties(
    dismissOnBackPress = true,
    dismissOnClickOutside = true,
    usePlatformDefaultWidth = false,
    usePlatformInsets = false,
)
