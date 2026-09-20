package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuiklybase.chat.compose.adapter.toComposeAttachment
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentSource
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmChatPlatformRegistry
import com.tencent.kuiklybase.chat.kuiklyai.platform.createChatAttachmentPicker

/**
 * Host integration for DSH-style attachment picking (bottom sheet + composer preview strip).
 *
 * Wire [callbacks] and [slots] into [ChatScreen]; requires a platform [LlmAttachmentPicker]
 * or [createChatAttachmentPicker] native module.
 */
data class ChatAttachmentPickerIntegration(
    val callbacks: ChatCallbacks,
    val slots: ChatSlots,
    val supportedTiles: List<ChatAttachmentCommandTile>,
)

@Composable
fun rememberChatAttachmentPickerIntegration(
    attachmentPicker: LlmAttachmentPicker? = null,
    theme: ChatTheme = ChatTheme.Default,
    strings: ChatStrings = ChatStrings.Default,
    onError: ((Throwable) -> Unit)? = null,
): ChatAttachmentPickerIntegration {
    val pager = LocalActivity.current.getPager()
    val picker = remember(attachmentPicker, pager) {
        attachmentPicker
            ?: LlmChatPlatformRegistry.current?.attachmentPicker
            ?: createChatAttachmentPicker(pager)
    }
    if (picker == null || picker.supportedSources.isEmpty()) {
        return ChatAttachmentPickerIntegration(
            callbacks = ChatCallbacks.Empty,
            slots = ChatSlots.Default,
            supportedTiles = emptyList(),
        )
    }

    val supportedTiles = remember(picker) {
        ChatAttachmentCommandTile.entries.filter { tile ->
            tile.toLlmSource() in picker.supportedSources
        }
    }

    val callbacks = ChatCallbacks(
        pickAttachmentsFromCommand = { tile ->
            val source = tile.toLlmSource()
            require(source in picker.supportedSources) { "Unsupported attachment source: $source" }
            picker.pick(source).map { it.toComposeAttachment() }
        },
        attachmentCommandTiles = supportedTiles,
        onAttachmentRequestError = onError,
    )

    val slots = ChatSlots(
        pendingAttachmentsContent = { attachments, onRemove ->
            ChatComposerAttachmentPreviewStrip(
                attachments = attachments,
                theme = theme,
                strings = strings,
                onRemove = onRemove,
            )
        },
    )

    return ChatAttachmentPickerIntegration(
        callbacks = callbacks,
        slots = slots,
        supportedTiles = supportedTiles,
    )
}
