package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Dismisses the software keyboard and clears TextField focus.
 *
 * Kuikly Compose does not provide `imePadding()`; hosts rely on [keyboardHeightChange].
 * On iOS the keyboard also does not auto-dismiss when tapping the message list, so ChatScreen
 * calls this on list scroll (Flyer-style drag-to-dismiss).
 */
@Composable
internal fun rememberDismissChatKeyboard(
    onKeyboardHeightReset: (() -> Unit)? = null,
): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController, onKeyboardHeightReset) {
        {
            focusManager.clearFocus()
            keyboardController?.hide()
            onKeyboardHeightReset?.invoke()
        }
    }
}
