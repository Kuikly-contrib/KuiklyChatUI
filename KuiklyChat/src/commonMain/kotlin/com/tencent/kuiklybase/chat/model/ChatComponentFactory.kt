package com.tencent.kuiklybase.chat.model

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.bubble.ChatBubbleView

// ============================
// 组件工厂接口（参考 Stream Chat 的 ChatComponentFactory）
// ============================

/**
 * 聊天组件工厂接口
 *
 * 参考 Stream Chat Android SDK 的 ChatComponentFactory 设计。
 * 提供全局可替换的原子组件渲染方法，用户可以通过实现此接口来自定义任何原子组件的渲染方式，
 * 而无需为每个使用场景都设置 Slot。
 *
 * 使用方式：
 * ```kotlin
 * config.componentFactory = object : DefaultChatComponentFactory() {
 *     override fun renderAvatar(container, avatarUrl, size, radius, placeholderColor, onClick) {
 *         // 自定义头像渲染
 *     }
 * }
 * ```
 *
 * 优先级链：Slot API > ComponentFactory > 内置默认渲染
 */
interface ChatComponentFactory {

    /**
     * 渲染用户头像
     *
     * @param container 父容器
     * @param avatarUrl 头像 URL（为空时显示占位）
     * @param size 头像尺寸（宽高相同）
     * @param radius 头像圆角半径
     * @param placeholderColor 占位背景色
     * @param onClick 点击回调（null 表示不可点击）
     */
    fun renderAvatar(
        container: ViewContainer<*, *>,
        avatarUrl: String,
        size: Float,
        radius: Float,
        placeholderColor: Long,
        onClick: (() -> Unit)?
    )

    /**
     * 渲染头像占位（分组内非最后一条消息时，保留头像空间但不显示头像）
     *
     * @param container 父容器
     * @param size 占位尺寸
     */
    fun renderAvatarPlaceholder(
        container: ViewContainer<*, *>,
        size: Float
    )

    /**
     * 渲染发送者名称
     *
     * @param container 父容器
     * @param name 发送者名称
     * @param color 文字颜色
     * @param fontSize 字号（默认 12f）
     */
    fun renderSenderName(
        container: ViewContainer<*, *>,
        name: String,
        color: Long,
        fontSize: Float
    )

    /**
     * 渲染消息状态指示器（发送中/已发送/已读/失败）
     *
     * @param container 父容器
     * @param status 消息状态
     * @param textColor 文字颜色
     * @param errorColor 错误颜色
     */
    fun renderMessageStatus(
        container: ViewContainer<*, *>,
        status: MessageStatus,
        textColor: Long,
        errorColor: Long
    )

    /**
     * 渲染重发按钮（发送失败时显示在气泡旁边）
     *
     * @param container 父容器
     * @param errorColor 错误颜色
     * @param onClick 点击回调
     */
    fun renderResendButton(
        container: ViewContainer<*, *>,
        errorColor: Long,
        onClick: () -> Unit
    )

    /**
     * 渲染未读消息计数徽章
     *
     * @param container 父容器
     * @param count 未读数量
     * @param bgColor 背景色
     * @param textColor 文字颜色
     * @param fontSize 字号
     */
    fun renderUnreadBadge(
        container: ViewContainer<*, *>,
        count: Int,
        bgColor: Long,
        textColor: Long,
        fontSize: Float
    )

    /**
     * 渲染在线状态指示器
     *
     * @param container 父容器
     * @param isOnline 是否在线
     * @param color 在线状态颜色
     */
    fun renderOnlineIndicator(
        container: ViewContainer<*, *>,
        isOnline: Boolean,
        color: Long
    )

    /**
     * 渲染置顶标记
     *
     * @param container 父容器
     * @param color 置顶标记颜色
     */
    fun renderPinnedIndicator(
        container: ViewContainer<*, *>,
        color: Long
    )

    /**
     * 渲染已编辑标记
     *
     * @param container 父容器
     * @param color 文字颜色
     */
    fun renderEditedLabel(
        container: ViewContainer<*, *>,
        color: Long
    )

    /**
     * 渲染线程回复入口
     *
     * @param container 父容器
     * @param threadCount 回复数量
     * @param color 文字颜色
     * @param onClick 点击回调
     */
    fun renderThreadReplyIndicator(
        container: ViewContainer<*, *>,
        threadCount: Int,
        color: Long,
        onClick: () -> Unit
    )

    /**
     * 渲染文件图标（文件消息中的图标区域）
     *
     * @param container 父容器
     * @param mimeType 文件 MIME 类型
     * @param primaryColor 主色
     */
    fun renderFileIcon(
        container: ViewContainer<*, *>,
        mimeType: String,
        primaryColor: Long
    )
}

// ============================
// 默认实现
// ============================

/**
 * 默认组件工厂实现
 *
 * 提供所有原子组件的默认渲染方式。
 * 用户可继承此类并只覆盖需要自定义的方法。
 */
open class DefaultChatComponentFactory : ChatComponentFactory {

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
                            src(ChatBubbleView.DEFAULT_AVATAR)
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
        errorColor: Long
    ) {
        if (status == MessageStatus.SENT) return
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
                        MessageStatus.FAILED -> {
                            text("发送失败，点击重试")
                            color(Color(errorColor))
                        }
                        MessageStatus.READ -> {
                            text("已读")
                            color(Color(textColor))
                        }
                        else -> {}
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
