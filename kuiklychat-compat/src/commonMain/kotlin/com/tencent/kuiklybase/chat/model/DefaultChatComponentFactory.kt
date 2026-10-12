package com.tencent.kuiklybase.chat.model

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.*
public open class DefaultChatComponentFactory : ChatComponentFactory {

    override fun renderAvatar(
        container: ViewContainer<*, *>,
        avatarUrl: String,
        size: Float,
        radius: Float,
        placeholderColor: Long,
        onClick: (() -> Unit)?
    ) {
        container.apply {
            View {
                attr {
                    size(size, size)
                    borderRadius(radius)
                    backgroundColor(Color(placeholderColor))
                    marginTop(2f)
                }
                if (avatarUrl.isNotEmpty()) {
                    Image {
                        attr {
                            size(size, size)
                            borderRadius(radius)
                            src(avatarUrl)
                            resizeCover()
                        }
                    }
                } else {
                    Image {
                        attr {
                            size(size, size)
                            borderRadius(radius)
                            src("https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png")
                            resizeCover()
                        }
                    }
                }
                if (onClick != null) {
                    event {
                        click { onClick() }
                    }
                }
            }
        }
    }

    override fun renderAvatarPlaceholder(
        container: ViewContainer<*, *>,
        size: Float
    ) {
        container.apply {
            View {
                attr {
                    size(size, size)
                }
            }
        }
    }

    override fun renderSenderName(
        container: ViewContainer<*, *>,
        name: String,
        color: Long,
        fontSize: Float
    ) {
        container.apply {
            Text {
                attr {
                    text(name)
                    fontSize(fontSize)
                    color(Color(color))
                    marginBottom(4f)
                }
            }
        }
    }

    override fun renderMessageStatus(
        container: ViewContainer<*, *>,
        status: MessageStatus,
        textColor: Long,
        errorColor: Long,
        isRead: Boolean,
    ) {
        if (status == MessageStatus.SENT && !isRead) return
        container.apply {
            Text {
                attr {
                    marginTop(2f)
                    fontSize(10f)
                    when (status) {
                        MessageStatus.SENDING -> {
                            text("发送中...")
                            color(Color(textColor))
                        }
                        MessageStatus.STREAMING -> {
                            text("生成中...")
                            color(Color(textColor))
                        }
                        MessageStatus.FAILED, MessageStatus.CANCELLED -> {
                            text("发送失败，点击重试")
                            color(Color(errorColor))
                        }
                        MessageStatus.SENT -> {
                            if (isRead) {
                                text("已读")
                                color(Color(textColor))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun renderResendButton(
        container: ViewContainer<*, *>,
        errorColor: Long,
        onClick: () -> Unit
    ) {
        container.apply {
            View {
                attr {
                    size(24f, 24f)
                    borderRadius(12f)
                    backgroundColor(Color(errorColor))
                    allCenter()
                    marginRight(6f)
                    alignSelf(FlexAlign.CENTER)
                }
                Text {
                    attr {
                        text("!")
                        fontSize(14f)
                        fontWeightBold()
                        color(Color.WHITE)
                    }
                }
                event {
                    click { onClick() }
                }
            }
        }
    }

    override fun renderUnreadBadge(
        container: ViewContainer<*, *>,
        count: Int,
        bgColor: Long,
        textColor: Long,
        fontSize: Float
    ) {
        container.apply {
            View {
                attr {
                    minWidth(18f)
                    height(18f)
                    borderRadius(9f)
                    backgroundColor(Color(bgColor))
                    allCenter()
                    paddingLeft(5f)
                    paddingRight(5f)
                }
                Text {
                    attr {
                        text(
                            if (count > 99) "99+"
                            else count.toString()
                        )
                        fontSize(fontSize)
                        color(Color(textColor))
                        fontWeightMedium()
                    }
                }
            }
        }
    }

    override fun renderOnlineIndicator(
        container: ViewContainer<*, *>,
        isOnline: Boolean,
        color: Long
    ) {
        if (!isOnline) return
        container.apply {
            View {
                attr {
                    size(14f, 14f)
                    borderRadius(7f)
                    backgroundColor(Color(0xFFFFFFFF))
                    positionAbsolute()
                    bottom(0f)
                    right(0f)
                    allCenter()
                }
                View {
                    attr {
                        size(10f, 10f)
                        borderRadius(5f)
                        backgroundColor(Color(color))
                    }
                }
            }
        }
    }

    override fun renderPinnedIndicator(
        container: ViewContainer<*, *>,
        color: Long
    ) {
        container.apply {
            Text {
                attr {
                    text("📌 已置顶")
                    fontSize(10f)
                    color(Color(color))
                    marginTop(2f)
                }
            }
        }
    }

    override fun renderEditedLabel(
        container: ViewContainer<*, *>,
        color: Long
    ) {
        container.apply {
            Text {
                attr {
                    text("(已编辑)")
                    fontSize(10f)
                    color(Color(color))
                    marginTop(2f)
                }
            }
        }
    }

    override fun renderThreadReplyIndicator(
        container: ViewContainer<*, *>,
        threadCount: Int,
        color: Long,
        onClick: () -> Unit
    ) {
        container.apply {
            View {
                attr {
                    flexDirectionRow()
                    alignItems(FlexAlign.CENTER)
                    marginTop(4f)
                }
                Text {
                    attr {
                        text("💬 ${threadCount} 条回复")
                        fontSize(12f)
                        color(Color(color))
                        fontWeightMedium()
                    }
                }
                event {
                    click { onClick() }
                }
            }
        }
    }

    override fun renderFileIcon(
        container: ViewContainer<*, *>,
        mimeType: String,
        primaryColor: Long
    ) {
        val (label, bgColor) = getFileStyle(mimeType)
        container.apply {
            View {
                attr {
                    size(40f, 40f)
                    borderRadius(10f)
                    backgroundColor(Color(bgColor))
                    allCenter()
                    marginRight(12f)
                }
                Text {
                    attr {
                        text(label)
                        fontSize(if (label.length > 3) 10f else 12f)
                        fontWeightBold()
                        color(Color.WHITE)
                    }
                }
            }
        }
    }

    override fun renderFileCardContent(
        container: ViewContainer<*, *>,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        fileNameColor: Long,
        fileSizeColor: Long
    ) {
        container.apply {
            View {
                attr {
                    flexDirectionRow()
                    alignItems(FlexAlign.CENTER)
                }
                renderFileIcon(this@apply, mimeType, 0xFF4F8FFF)
                View {
                    attr {
                        flex(1f)
                        flexDirection(FlexDirection.COLUMN)
                        minWidth(0f)
                    }
                    Text {
                        attr {
                            text(fileName)
                            fontSize(14f)
                            color(Color(fileNameColor))
                            lines(2)
                        }
                    }
                    if (fileSize > 0) {
                        Text {
                            attr {
                                text(formatFileSize(fileSize))
                                fontSize(11f)
                                color(Color(fileSizeColor))
                                marginTop(2f)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================
// 辅助函数
// ============================

private data class FileStyle(val label: String, val color: Long)

private fun getFileStyle(mimeType: String): FileStyle {
    return when {
        mimeType.contains("pdf") -> FileStyle("PDF", 0xFFE5484D)
        mimeType.contains("word") || mimeType.contains("document") -> FileStyle("DOC", 0xFF4A90D9)
        mimeType.contains("excel") || mimeType.contains("spreadsheet") -> FileStyle("XLS", 0xFF30A46C)
        mimeType.contains("powerpoint") || mimeType.contains("presentation") -> FileStyle("PPT", 0xFFE5734A)
        mimeType.startsWith("image/") -> FileStyle("IMG", 0xFF9B59B6)
        mimeType.startsWith("video/") -> FileStyle("VID", 0xFF6C5CE7)
        mimeType.startsWith("audio/") -> FileStyle("MP3", 0xFFE08C3B)
        mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("tar") -> FileStyle("ZIP", 0xFF7C8894)
        mimeType.startsWith("text/") -> FileStyle("TXT", 0xFF8E8E93)
        else -> FileStyle("FILE", 0xFF8E8E93)
    }
}

internal fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> "${(bytes / 1024.0 * 10).toLong() / 10.0} KB"
        bytes < 1024 * 1024 * 1024 -> "${(bytes / (1024.0 * 1024.0) * 10).toLong() / 10.0} MB"
        else -> "${(bytes / (1024.0 * 1024.0 * 1024.0) * 10).toLong() / 10.0} GB"
    }
}
