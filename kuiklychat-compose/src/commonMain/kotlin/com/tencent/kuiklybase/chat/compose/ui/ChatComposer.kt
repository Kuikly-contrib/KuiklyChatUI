package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.material3.TextFieldDefaults
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.focus.focusRequester
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.extension.keyboardHeightChange
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ChatComposer(
    controller: ChatController,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    slots: ChatSlots,
    callbacks: ChatCallbacks,
    onKeyboardHeightChange: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember(controller) { FocusRequester() }
    LaunchedEffect(controller, config.autofocus) {
        if (config.autofocus) focusRequester.requestFocus()
    }
    var isPickingAttachment by remember(controller) { mutableStateOf(false) }
    val state = controller.collectChatState()
    val input = state.draftText
    val pendingAttachments = state.draftAttachments
    val isSending = state.isSending
    val canSend = (input.isNotBlank() || pendingAttachments.isNotEmpty()) && !isSending
    val sessionError = state.error
    val attachmentRequest = callbacks.onAttachmentRequest
    val commandPick = callbacks.pickAttachmentsFromCommand
    val commandTiles = callbacks.attachmentCommandTiles.ifEmpty {
        ChatAttachmentCommandTile.entries.toList()
    }
    var showAttachmentSheet by remember(controller) { mutableStateOf(false) }
    val useCommandSheet = commandPick != null

    fun sendOrStop() {
        if (isSending) {
            controller.dispatch(ChatIntent.StopGeneration)
        } else if (canSend) {
            submit(input, pendingAttachments, controller) {
                controller.clearDraft()
                callbacks.onTextChanged?.invoke("")
            }
        }
    }

    fun pickFromCommand(tile: ChatAttachmentCommandTile) {
        val pick = commandPick ?: return
        scope.launch {
            isPickingAttachment = true
            try {
                val picked = pick(tile)
                controller.setDraftAttachments(mergePickedAttachments(pendingAttachments, picked))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                callbacks.onAttachmentRequestError?.invoke(error)
            } finally {
                isPickingAttachment = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(theme.composerColor))
            .padding(
                start = theme.contentPadding.dp,
                end = theme.contentPadding.dp,
                top = theme.contentPadding.dp,
                bottom = theme.contentPadding.dp + config.composerBottomInsetDp.dp,
            )
    ) {
        if (sessionError != null) {
            Text(
                text = "${strings.failed}: ${sessionError.message}",
                color = Color(theme.errorColor),
                fontSize = theme.smallFontSize.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { controller.dispatch(ChatIntent.ClearError) }
                    .padding(bottom = 8.dp)
            )
        }
        slots.composerHeaderContent?.invoke()
        if (slots.composerHeaderContent != null) {
            Spacer(Modifier.height(8.dp))
        }
        if (useCommandSheet) {
            ChatComposerDshInputCard(
                input = input,
                onInputChange = {
                    controller.setDraft(it)
                    callbacks.onTextChanged?.invoke(it)
                },
                pendingAttachments = pendingAttachments,
                onRemoveAttachment = { attachmentId ->
                    controller.setDraftAttachments(pendingAttachments.filterNot { it.id == attachmentId })
                },
                theme = theme,
                strings = strings,
                config = config,
                isSending = isSending,
                canSend = canSend,
                isPickingAttachment = isPickingAttachment,
                onOpenAttachmentSheet = { showAttachmentSheet = true },
                onSendOrStop = { sendOrStop() },
                onKeyboardHeightChange = onKeyboardHeightChange,
                pendingContent = slots.pendingAttachmentsContent,
            )
            ChatAttachmentCommandSheet(
                visible = showAttachmentSheet,
                tiles = commandTiles,
                theme = theme,
                strings = strings,
                onDismiss = { showAttachmentSheet = false },
                onPickTile = { tile -> pickFromCommand(tile) },
            )
        } else {
        ChatComposerPendingAttachments(
            attachments = pendingAttachments,
            theme = theme,
            strings = strings,
            onRemove = { attachmentId ->
                    controller.setDraftAttachments(pendingAttachments.filterNot { it.id == attachmentId })
            },
            content = slots.pendingAttachmentsContent,
        )
        if (pendingAttachments.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            slots.composerLeadingContent?.invoke()
            if (slots.composerLeadingContent != null) Spacer(Modifier.width(8.dp))

            if (attachmentRequest != null) {
                ChatComposerAttachmentButton(
                    enabled = !isSending && !isPickingAttachment,
                    isPicking = isPickingAttachment,
                    theme = theme,
                    onClickLabel = CHAT_ACCESSIBILITY_ADD_ATTACHMENT,
                    onClick = {
                        scope.launch {
                            isPickingAttachment = true
                            try {
                                val picked = attachmentRequest()
                                controller.setDraftAttachments(
                                    mergePickedAttachments(pendingAttachments, picked),
                                )
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Throwable) {
                                callbacks.onAttachmentRequestError?.invoke(error)
                            } finally {
                                isPickingAttachment = false
                            }
                        }
                    },
                    content = slots.attachmentButtonContent,
                )
                Spacer(Modifier.width(8.dp))
            }

            TextField(
                value = input,
                onValueChange = { value ->
                    val safeValue = config.maxTextLength?.let { value.take(it) } ?: value
                    controller.setDraft(safeValue)
                    callbacks.onTextChanged?.invoke(safeValue)
                },
                placeholder = {
                    Text(
                        text = strings.inputPlaceholder,
                        color = Color(theme.placeholderColor),
                        fontSize = theme.fontSize.sp
                    )
                },
                textStyle = TextStyle(
                    color = Color(theme.incomingTextColor),
                    fontSize = theme.fontSize.sp
                ),
                maxLines = config.maxLines,
                shape = RoundedCornerShape(theme.bubbleRadius.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color(theme.incomingTextColor),
                    unfocusedTextColor = Color(theme.incomingTextColor),
                    focusedContainerColor = Color(theme.composerColor),
                    unfocusedContainerColor = Color(theme.composerColor),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color(theme.accentColor)
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .border(1.dp, Color(theme.borderColor), RoundedCornerShape(theme.bubbleRadius.dp))
                    .then(
                        if (onKeyboardHeightChange != null) {
                            Modifier.keyboardHeightChange { onKeyboardHeightChange(it.height) }
                        } else {
                            Modifier
                        },
                    ),
            )
            Spacer(Modifier.width(8.dp))

            if (slots.composerTrailingContent != null) {
                slots.composerTrailingContent.invoke(isSending) {
                    submit(input, pendingAttachments, controller) {
                        controller.clearDraft()
                        callbacks.onTextChanged?.invoke("")
                    }
                }
            } else {
                val sendEnabled = isSending || canSend
                val sendBaseColor = when {
                    isSending -> theme.errorColor
                    canSend -> theme.accentColor
                    else -> theme.borderColor
                }
                ChatPressableFilledButton(
                    enabled = sendEnabled,
                    onClick = {
                        if (isSending) {
                            controller.dispatch(ChatIntent.StopGeneration)
                        } else if (canSend) {
                            submit(input, pendingAttachments, controller) {
                                controller.clearDraft()
                                callbacks.onTextChanged?.invoke("")
                            }
                        }
                    },
                    theme = theme,
                    baseColor = sendBaseColor,
                    modifier = Modifier.height(48.dp),
                    onClickLabel = if (isSending) CHAT_ACCESSIBILITY_STOP else CHAT_ACCESSIBILITY_SEND,
                    shapeRadius = theme.bubbleRadius.dp,
                ) {
                    Text(
                        text = if (isSending) strings.stop else strings.send,
                        color = Color(
                            if (canSend || isSending) {
                                theme.outgoingTextColor
                            } else {
                                theme.placeholderColor
                            },
                        ),
                        fontSize = theme.smallFontSize.sp,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                }
            }
        }
        }
    }
}

private fun submit(
    input: String,
    attachments: List<MessagePart.Attachment>,
    controller: ChatController,
    onAccepted: () -> Unit,
) {
    if (controller.sendMessage(input, attachments)) {
        onAccepted()
    }
}
