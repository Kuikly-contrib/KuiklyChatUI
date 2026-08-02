package com.tencent.kuiklybase.chat.kuiklyai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.gestures.detectTransformGestures
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.ui.window.DialogProperties

/** Pure zoom/pan state so gesture math can be tested without Compose. */
data class LlmImageTransformState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    fun zoomBy(delta: Float): LlmImageTransformState = copy(
        scale = (scale * delta).coerceIn(MIN_SCALE, MAX_SCALE)
    ).clamped()

    fun panBy(dx: Float, dy: Float): LlmImageTransformState = copy(
        offsetX = offsetX + dx,
        offsetY = offsetY + dy
    )

    fun doubleTap(): LlmImageTransformState = if (scale > MIN_SCALE) {
        Reset
    } else {
        copy(scale = DOUBLE_TAP_SCALE)
    }

    fun clamped(viewportWidth: Float = DEFAULT_VIEWPORT, viewportHeight: Float = DEFAULT_VIEWPORT): LlmImageTransformState {
        val maxX = viewportWidth * (scale - 1f) / 2f
        val maxY = viewportHeight * (scale - 1f) / 2f
        return copy(
            offsetX = offsetX.coerceIn(-maxX, maxX),
            offsetY = offsetY.coerceIn(-maxY, maxY)
        )
    }

    companion object {
        const val MIN_SCALE = 1f
        const val DOUBLE_TAP_SCALE = 2.5f
        const val MAX_SCALE = 5f
        private const val DEFAULT_VIEWPORT = 1000f
        val Reset = LlmImageTransformState()
    }
}

@Composable
fun LlmImagePreviewDialog(
    attachment: LlmAttachment,
    onDismiss: () -> Unit
) {
    val source = imageSource(attachment)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            scrimColor = Color.Black.copy(alpha = 0.82f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (source == null) {
                Text(
                    text = "图片无法预览",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                var transform by remember(attachment.id) { mutableStateOf(LlmImageTransformState.Reset) }
                Image(
                    painter = rememberAsyncImagePainter(model = source),
                    contentDescription = attachment.name.ifEmpty { "Image preview" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = transform.scale,
                            scaleY = transform.scale,
                            translationX = transform.offsetX,
                            translationY = transform.offsetY
                        )
                        .pointerInput(attachment.id) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                transform = transform.zoomBy(zoom).panBy(pan.x, pan.y).clamped()
                            }
                        }
                        .pointerInput(attachment.id) {
                            detectTapGestures(onDoubleTap = {
                                transform = transform.doubleTap()
                            })
                        }
                )
            }

            Text(
                text = "×",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .clickable { onDismiss() }
            )
        }
    }
}

internal fun imageSource(attachment: LlmAttachment): String? = when {
    attachment.url.isNotEmpty() -> attachment.url
    attachment.bytesBase64.isNotEmpty() -> {
        "data:${attachment.mimeType.ifEmpty { "image/png" }};base64,${attachment.bytesBase64}"
    }
    else -> null
}
