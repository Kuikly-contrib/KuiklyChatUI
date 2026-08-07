package com.kuikly.kuiklychat

import com.tencent.kuiklybase.chat.compose.adapter.AttachmentUriCodec
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.compose.core.SYSTEM_CHAT_USER
import com.tencent.kuiklybase.chat.compose.core.TextFormat

private val DEMO_TINY_PNG = AttachmentUriCodec.toDataUri(
    mimeType = "image/png",
    base64Payload = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
)

/** Synchronous seed data for [ComposeChatSdkDemoPage] (avoids LaunchedEffect empty flash). */
internal fun composeChatSdkDemoMessages(): List<ChatMessage> {
    val user = ChatUser(id = "user", name = "You")
    val assistant = ChatUser(id = "assistant", name = "Assistant")
    return listOf(
        ChatMessage(
            id = "demo-system-before",
            author = user,
            parts = listOf(MessagePart.Text("分组示例：第一条")),
            createdAt = 1L,
        ),
        ChatMessage(
            id = "demo-system-notice",
            author = SYSTEM_CHAT_USER,
            parts = listOf(MessagePart.SystemNotice("Alice 加入了会话")),
            createdAt = 2L,
        ),
        ChatMessage(
            id = "demo-system-after",
            author = user,
            parts = listOf(MessagePart.Text("分组示例：第二条（应与上条断开）")),
            createdAt = 3L,
        ),
        ChatMessage(
            id = "demo-system-assistant",
            author = assistant,
            parts = listOf(MessagePart.Text("助手普通消息")),
            createdAt = 4L,
        ),
        ChatMessage(
            id = "demo-markdown-plain",
            author = assistant,
            parts = listOf(MessagePart.Text("纯文本消息（PLAIN）")),
            createdAt = 5L,
        ),
        ChatMessage(
            id = "demo-markdown-complete",
            author = assistant,
            parts = listOf(
                MessagePart.Text(
                    value = """
                        ## Markdown 示例

                        支持 **粗体**、*斜体*、`行内代码` 和 [链接](https://example.com)。

                        - 无序列表
                        - 第二项

                        ```kotlin
                        fun hello() = "world"
                        ```
                    """.trimIndent(),
                    format = TextFormat.MARKDOWN,
                ),
            ),
            createdAt = 6L,
        ),
        ChatMessage(
            id = "demo-markdown-mixed",
            author = assistant,
            parts = listOf(
                MessagePart.Text("上文说明：", format = TextFormat.PLAIN),
                MessagePart.Text("**Markdown 段落**", format = TextFormat.MARKDOWN),
            ),
            createdAt = 7L,
        ),
        ChatMessage(
            id = "demo-attachment-remote",
            author = assistant,
            parts = listOf(
                MessagePart.Text("远程图片（需网络）："),
                MessagePart.Attachment(
                    id = "demo-img-remote",
                    uri = "https://picsum.photos/320/200",
                    mimeType = "image/jpeg",
                    name = "sample.jpg",
                ),
            ),
            createdAt = 8L,
        ),
        ChatMessage(
            id = "demo-attachment-data",
            author = user,
            parts = listOf(
                MessagePart.Attachment(
                    id = "demo-img-data",
                    uri = DEMO_TINY_PNG,
                    mimeType = "image/png",
                    name = "pixel.png",
                ),
            ),
            createdAt = 9L,
        ),
        ChatMessage(
            id = "demo-attachment-file",
            author = assistant,
            parts = listOf(
                MessagePart.Attachment(
                    id = "demo-file",
                    uri = "https://cdn.example/spec.pdf",
                    mimeType = "application/pdf",
                    name = "KuiklyChat-Spec.pdf",
                    sizeBytes = 1536L,
                ),
            ),
            createdAt = 10L,
        ),
        ChatMessage(
            id = "demo-attachment-fail",
            author = assistant,
            parts = listOf(
                MessagePart.Attachment(
                    id = "demo-img-bad",
                    uri = "https://invalid.local/not-found.png",
                    mimeType = "image/png",
                    name = "broken.png",
                ),
            ),
            createdAt = 11L,
        ),
        ChatMessage(
            id = "demo-attachment-mixed",
            author = assistant,
            parts = listOf(
                MessagePart.Text("混合：说明 + 图 + 文件"),
                MessagePart.Attachment(
                    id = "demo-mixed-img",
                    uri = DEMO_TINY_PNG,
                    mimeType = "image/png",
                    name = "inline.png",
                ),
                MessagePart.Attachment(
                    id = "demo-mixed-cached-img",
                    uri = "https://picsum.photos/320/200",
                    mimeType = "image/jpeg",
                    name = "cached-sample.jpg",
                ),
                MessagePart.Attachment(
                    id = "demo-mixed-file",
                    uri = "https://cdn.example/notes.txt",
                    mimeType = "text/plain",
                    name = "notes.txt",
                    sizeBytes = 420L,
                ),
            ),
            createdAt = 12L,
        ),
    )
}

/** Synchronous seed data for [ComposeChatCacheDemoPage]. */
internal fun composeChatCacheDemoMessages(
    assistant: ChatUser,
    remoteImageUrl: String,
): List<ChatMessage> = listOf(
    ChatMessage(
        id = "cache-demo-intro",
        author = assistant,
        parts = listOf(
            MessagePart.Text(
                "本页使用 KRChatByteCacheModule + ChatCrossCache。下方两张图使用同一远程 URL，第二次应命中持久化缓存。",
            ),
        ),
        createdAt = 1L,
    ),
    ChatMessage(
        id = "cache-demo-remote-1",
        author = assistant,
        parts = listOf(
            MessagePart.Text("远程图片 #1"),
            MessagePart.Attachment(
                id = "cache-demo-img-1",
                uri = remoteImageUrl,
                mimeType = "image/jpeg",
                name = "cache-demo-1.jpg",
            ),
        ),
        createdAt = 2L,
    ),
    ChatMessage(
        id = "cache-demo-remote-2",
        author = assistant,
        parts = listOf(
            MessagePart.Text("远程图片 #2（同 URL，应复用缓存）"),
            MessagePart.Attachment(
                id = "cache-demo-img-2",
                uri = remoteImageUrl,
                mimeType = "image/jpeg",
                name = "cache-demo-2.jpg",
            ),
        ),
        createdAt = 3L,
    ),
    ChatMessage(
        id = "cache-demo-inline",
        author = assistant,
        parts = listOf(
            MessagePart.Text("内联 data URI（写入同一 Module 缓存）"),
            MessagePart.Attachment(
                id = "cache-demo-inline-img",
                uri = DEMO_TINY_PNG,
                mimeType = "image/png",
                name = "inline.png",
            ),
        ),
        createdAt = 4L,
    ),
)
