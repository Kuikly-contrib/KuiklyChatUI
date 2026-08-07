package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTextRenderConfig
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.ChatUserResolver
import com.tencent.kuiklybase.chat.compose.core.MessageRendererRegistry
import com.tencent.kuiklybase.chat.compose.core.UserCache
import com.tencent.kuiklybase.chat.compose.core.groupStatus
import com.tencent.kuiklybase.chat.compose.core.systemNoticeOrNull

@Composable
internal fun ChatMessageListItem(
    rawMessage: ChatMessage,
    index: Int,
    messages: List<ChatMessage>,
    controller: ChatController,
    theme: ChatTheme,
    strings: ChatStrings,
    config: ChatScreenConfig,
    slots: ChatSlots,
    callbacks: ChatCallbacks,
    textRenderConfig: ChatTextRenderConfig,
    mediaRuntime: ChatMediaRuntime,
    rendererRegistry: MessageRendererRegistry,
    userResolution: ChatUserResolutionConfig?,
    userCache: UserCache?,
    latestResolver: State<ChatUserResolver?>?,
    resolverAvailable: Boolean,
    reserveSenderNameSpace: Boolean,
) {
    val previous = messages.getOrNull(index - 1)
    val next = messages.getOrNull(index + 1)
    val nowMillis = config.dateSeparatorClock.nowMillis()
    val dateLabel = resolveDateSeparatorLabel(
        previous = previous,
        current = rawMessage,
        config = config,
        strings = strings,
        nowMillis = nowMillis,
    )
    val effectivePrevious = if (
        breaksMessageGroupingAtDateBoundary(previous, rawMessage, config, strings, nowMillis)
    ) {
        null
    } else {
        previous
    }
    val effectiveNext = if (
        next != null && breaksMessageGroupingAtDateBoundary(rawMessage, next, config, strings, nowMillis)
    ) {
        null
    } else {
        next
    }
    val group = rawMessage.groupStatus(
        previous = effectivePrevious,
        next = effectiveNext,
        groupingWindowMillis = config.groupingWindowMillis,
    )
    val customRenderer = rendererRegistry.rendererFor(rawMessage)
    if (customRenderer != null) {
        val message = resolveListMessage(
            rawMessage = rawMessage,
            userResolution = userResolution,
            userCache = userCache,
            latestResolver = latestResolver,
            resolverAvailable = resolverAvailable,
        )
        val rendererContent: @Composable () -> Unit = {
            val messageTap = callbacks.onMessageClick?.let { handler -> { handler(message) } }
            customRenderer.Render(
                message,
                group,
                theme,
                Modifier.chatMessageGestures(
                    message = message,
                    callbacks = callbacks,
                    onTap = messageTap,
                ),
            )
        }
        ChatMessageListItemBody(
            dateLabel = dateLabel,
            messageCreatedAt = rawMessage.createdAt,
            theme = theme,
            slots = slots,
            content = {
                if (slots.messageRowContent != null) {
                    slots.messageRowContent.invoke(message, group, rendererContent)
                } else {
                    ChatMessageRow(
                        message = message,
                        groupStatus = group,
                        controller = controller,
                        theme = theme,
                        strings = strings,
                        config = config,
                        slots = slots,
                        callbacks = callbacks,
                        mediaRuntime = mediaRuntime,
                        content = rendererContent,
                        reserveSenderNameSpace = reserveSenderNameSpace,
                    )
                }
            },
        )
        return
    }

    val systemNotice = rawMessage.systemNoticeOrNull
    if (systemNotice != null) {
        ChatMessageListItemBody(
            dateLabel = dateLabel,
            messageCreatedAt = rawMessage.createdAt,
            theme = theme,
            slots = slots,
            content = {
                if (slots.systemMessageContent != null) {
                    slots.systemMessageContent.invoke(rawMessage, systemNotice)
                } else {
                    SystemMessageContent(
                        message = rawMessage,
                        notice = systemNotice,
                        theme = theme,
                    )
                }
            },
        )
        return
    }

    val message = resolveListMessage(
        rawMessage = rawMessage,
        userResolution = userResolution,
        userCache = userCache,
        latestResolver = latestResolver,
        resolverAvailable = resolverAvailable,
    )

    ChatMessageListItemBody(
        dateLabel = dateLabel,
        messageCreatedAt = rawMessage.createdAt,
        theme = theme,
        slots = slots,
        content = {
            if (slots.messageContent != null) {
                slots.messageContent.invoke(message, group)
            } else {
                DefaultChatMessageRow(
                    message = message,
                    groupStatus = group,
                    controller = controller,
                    theme = theme,
                    strings = strings,
                    config = config,
                    slots = slots,
                    callbacks = callbacks,
                    textRenderConfig = textRenderConfig,
                    mediaRuntime = mediaRuntime,
                    reserveSenderNameSpace = reserveSenderNameSpace,
                )
            }
        },
    )
}

@Composable
private fun ChatMessageListItemBody(
    dateLabel: String?,
    messageCreatedAt: Long,
    theme: ChatTheme,
    slots: ChatSlots,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (dateLabel != null) {
            ChatMessageDateHeader(
                label = dateLabel,
                theme = theme,
                slots = slots,
                timestampMillis = messageCreatedAt,
            )
        }
        content()
    }
}

@Composable
private fun resolveListMessage(
    rawMessage: ChatMessage,
    userResolution: ChatUserResolutionConfig?,
    userCache: UserCache?,
    latestResolver: State<ChatUserResolver?>?,
    resolverAvailable: Boolean,
): ChatMessage {
    if (
        userResolution != null &&
        userCache != null &&
        latestResolver != null
    ) {
        return rememberResolvedMessage(
            message = rawMessage,
            cache = userCache,
            namespace = userResolution.cacheNamespace,
            latestResolver = latestResolver,
            resolverAvailable = resolverAvailable,
        )
    }
    return rawMessage
}
