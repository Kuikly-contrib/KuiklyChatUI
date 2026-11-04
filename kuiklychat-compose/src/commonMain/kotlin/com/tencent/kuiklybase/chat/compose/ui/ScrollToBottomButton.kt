package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.StrokeJoin
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.base.Color as KuiklyColor
import com.tencent.kuikly.core.views.BlurView
import com.tencent.kuiklybase.chat.compose.core.ChatTheme

@Composable
internal fun ScrollToBottomButton(
    unreadCount: Int,
    theme: ChatTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showBadge = unreadCount > 0
    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        ChatPressableIconBox(
            enabled = true,
            onClick = onClick,
            modifier = Modifier
                .padding(top = if (showBadge) 8.dp else 0.dp)
                .size(40.dp)
                .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(20.dp)),
            shapeRadius = 20.dp,
        ) { pressed ->
            FrostedGlassDisc(pressed = pressed, modifier = Modifier.fillMaxSize())
            ChevronsDownIcon(
                color = Color(theme.incomingTextColor),
                modifier = Modifier.size(22.dp),
            )
        }
        if (showBadge) {
            UnreadCountBadge(count = unreadCount, theme = theme)
        }
    }
}

/** Blurs the list behind the button, then tints it with translucent white. */
@Composable
private fun FrostedGlassDisc(pressed: Boolean, modifier: Modifier = Modifier) {
    val tintAlpha = if (pressed) 0.68f else 0.5f
    MakeKuiklyComposeNode<BlurView>(
        factory = { BlurView() },
        modifier = modifier,
        viewUpdate = { view ->
            view.getViewAttr().apply {
                blurRadius(10f)
                borderRadius(20f)
                backgroundColor(
                    KuiklyColor(red255 = 255, green255 = 255, blue255 = 255, alpha01 = tintAlpha),
                )
            }
        },
    )
}

/** Lucide `chevrons-down`: two 24×24 chevrons, 2px round stroke. */
@Composable
private fun ChevronsDownIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val style = Stroke(
            width = 2f * sx,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawPath(chevronPath(sx, sy, 6f), color, style = style)
        drawPath(chevronPath(sx, sy, 13f), color, style = style)
    }
}

private fun chevronPath(sx: Float, sy: Float, y: Float): Path = Path().apply {
    moveTo(7f * sx, y * sy)
    lineTo(12f * sx, (y + 5f) * sy)
    lineTo(17f * sx, y * sy)
}

@Composable
private fun UnreadCountBadge(count: Int, theme: ChatTheme) {
    val label = if (count > 99) "99+" else count.toString()
    Text(
        text = label,
        color = Color.White,
        fontSize = 10.sp,
        modifier = Modifier
            .background(Color(theme.accentColor), RoundedCornerShape(8.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}
