package com.example.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Modifier extension to clear focus and dismiss soft keyboard
 * whenever the user taps outside an active input field.
 * Safe for scrolling, buttons, and switches without ripple effect.
 */
fun Modifier.clearFocusOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
    ) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
}
