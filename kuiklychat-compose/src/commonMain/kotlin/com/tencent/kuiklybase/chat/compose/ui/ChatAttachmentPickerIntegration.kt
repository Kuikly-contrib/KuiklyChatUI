package com.tencent.kuiklybase.chat.compose.ui

/**
 * Host-facing result of attachment-picker wiring.
 *
 * Platform picker implementations live in adapter / KuiklyAI; Compose UI only consumes
 * [callbacks] and [slots].
 */
public data class ChatAttachmentPickerIntegration(
    val callbacks: ChatCallbacks,
    val slots: ChatSlots,
    val supportedTiles: List<ChatAttachmentCommandTile>,
)
