package com.kuikly.kuiklychat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.adapter.toComposeAttachment
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.ui.ChatCallbacks
import com.tencent.kuiklybase.chat.compose.ui.ChatSlots
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentSource
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmChatPlatformRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * Demo-only bridge from [LlmAttachmentPicker] to Compose Chat [ChatCallbacks].
 *
 * Production hosts should implement their own picker/menu/upload flow and pass
 * [ChatCallbacks.onAttachmentRequest] directly to [com.tencent.kuiklybase.chat.compose.ui.ChatScreen].
 */
data class DemoChatAttachmentIntegration(
    val callbacks: ChatCallbacks,
    val slots: ChatSlots,
)

@Composable
fun rememberDemoChatAttachmentIntegration(
    attachmentPicker: LlmAttachmentPicker? = LlmChatPlatformRegistry.current?.attachmentPicker,
    theme: ChatTheme = ChatTheme.Default,
    strings: ChatStrings = ChatStrings.Default,
    onError: ((Throwable) -> Unit)? = null,
): DemoChatAttachmentIntegration {
    val picker = attachmentPicker
    if (picker == null || picker.supportedSources.isEmpty()) {
        return DemoChatAttachmentIntegration(ChatCallbacks.Empty, ChatSlots.Default)
    }

    var showSourceMenu by remember(picker) { mutableStateOf(false) }
    var sourceDeferred by remember(picker) { mutableStateOf<CompletableDeferred<LlmAttachmentSource>?>(null) }

    val callbacks = ChatCallbacks(
        onAttachmentRequest = {
            if (showSourceMenu) {
                showSourceMenu = false
                sourceDeferred?.cancel(CancellationException("Attachment pick cancelled"))
                sourceDeferred = null
                return@ChatCallbacks emptyList()
            }
            val source = picker.preferredSource() ?: run {
                val deferred = CompletableDeferred<LlmAttachmentSource>()
                sourceDeferred = deferred
                showSourceMenu = true
                try {
                    deferred.await()
                } finally {
                    showSourceMenu = false
                    sourceDeferred = null
                }
            }
            picker.pick(source).map { it.toComposeAttachment() }
        },
        onAttachmentRequestError = onError,
    )

    val slots = ChatSlots(
        composerHeaderContent = if (showSourceMenu) {
            {
                DemoAttachmentSourceMenu(
                    sources = picker.supportedSources.toList(),
                    theme = theme,
                    strings = strings,
                    onSelect = { source ->
                        showSourceMenu = false
                        sourceDeferred?.complete(source)
                    },
                )
            }
        } else {
            null
        },
    )

    return DemoChatAttachmentIntegration(callbacks, slots)
}

private fun LlmAttachmentPicker.preferredSource(): LlmAttachmentSource? {
    if (LlmAttachmentSource.PHOTO_LIBRARY in supportedSources) {
        return LlmAttachmentSource.PHOTO_LIBRARY
    }
    if (supportedSources.size == 1) {
        return supportedSources.first()
    }
    return null
}

@Composable
private fun DemoAttachmentSourceMenu(
    sources: List<LlmAttachmentSource>,
    theme: ChatTheme,
    strings: ChatStrings,
    onSelect: (LlmAttachmentSource) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        sources.forEachIndexed { index, source ->
            if (index > 0) Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(theme.incomingBubbleColor))
                    .border(1.dp, Color(theme.borderColor), RoundedCornerShape(8.dp))
                    .clickable { onSelect(source) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = when (source) {
                        LlmAttachmentSource.PHOTO_LIBRARY -> strings.photoLibraryLabel
                        LlmAttachmentSource.CAMERA -> strings.cameraLabel
                        LlmAttachmentSource.FILE -> strings.fileLabel
                    },
                    fontSize = 12.sp,
                    color = Color(theme.incomingTextColor),
                )
            }
        }
    }
}
