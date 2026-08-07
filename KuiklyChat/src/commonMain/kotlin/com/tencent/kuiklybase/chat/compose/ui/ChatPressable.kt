package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.interaction.collectIsPressedAsState
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuiklybase.chat.compose.core.ChatTheme

internal const val CHAT_PRESS_SCALE = 0.94f
internal const val CHAT_ICON_ENABLED_ALPHA = 0.5f
internal const val CHAT_ICON_PRESSED_ALPHA = 0.32f
internal const val CHAT_ICON_DISABLED_ALPHA = 0.26f
internal const val CHAT_FILLED_BUTTON_PRESSED_ALPHA = 0.82f

internal fun composerIconColor(theme: ChatTheme, enabled: Boolean, pressed: Boolean): Color {
    val base = Color(theme.incomingTextColor)
    val alpha = when {
        !enabled -> CHAT_ICON_DISABLED_ALPHA
        pressed -> CHAT_ICON_PRESSED_ALPHA
        else -> CHAT_ICON_ENABLED_ALPHA
    }
    return base.copy(alpha = alpha)
}

internal fun composerFilledButtonColor(
    theme: ChatTheme,
    baseColor: Long,
    enabled: Boolean,
    pressed: Boolean,
): Color {
    val alpha = when {
        !enabled -> 0.45f
        pressed -> CHAT_FILLED_BUTTON_PRESSED_ALPHA
        else -> 1f
    }
    return Color(baseColor).copy(alpha = alpha)
}

@Composable
internal fun ChatPressableIconBox(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 24.dp,
    pressedOverlayColor: Color = Color(0x14000000),
    content: @Composable (pressed: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressed = enabled && isPressed
    val scale = if (pressed) CHAT_PRESS_SCALE else 1f
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(shapeRadius))
            .background(if (pressed) pressedOverlayColor else Color.Transparent)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content(pressed)
    }
}

@Composable
internal fun ChatPressableFilledButton(
    enabled: Boolean,
    onClick: () -> Unit,
    theme: ChatTheme,
    baseColor: Long,
    modifier: Modifier = Modifier,
    shapeRadius: Dp = 18.dp,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressed = enabled && isPressed
    val scale = if (pressed) CHAT_PRESS_SCALE else 1f
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(shapeRadius))
            .background(
                composerFilledButtonColor(
                    theme = theme,
                    baseColor = baseColor,
                    enabled = enabled,
                    pressed = pressed,
                ),
            )
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun ChatPressableText(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pressedAlpha: Float = 0.55f,
    content: @Composable (pressed: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressed = enabled && isPressed
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = when {
                    !enabled -> 0.45f
                    pressed -> pressedAlpha
                    else -> 1f
                }
            }
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content(pressed)
    }
}
