package com.tencent.kuiklybase.chat.session

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.bubble.*
import com.tencent.kuiklybase.chat.model.*

/**
 * 默认图片 / 视频消息渲染（参考 Stream Chat 的 ImageAttachmentContent）。
 *
 * 自己发的消息与对方发的消息布局镜像：对方是 `头像 → 图片`，自己是 `重发按钮 → 图片 → 头像`。
 * 图片下方的置顶标记、线程入口、反应栏两侧共用，抽成了 [renderImageFooter]。
 */
internal fun renderDefaultImageBubble(
    container: ViewContainer<*, *>,
    msgContext: MessageContext,
    cfg: ChatSessionConfig
) {
    val message = msgContext.message
    val theme = cfg.theme
    val themeColors = theme.resolvedColors()
    val listOptions = cfg.messageListOptions
    val layout = msgContext.resolveGroupLayout(cfg)
    val size = resolveImageDisplaySize(message, theme)
    val isVideo = message.type == MessageType.VIDEO

    container.apply {
        View {
            attr {
                flexDirectionRow()
                padding(layout.verticalPadding, theme.rowPaddingH, layout.verticalPadding, theme.rowPaddingH)
                if (message.isSelf) {
                    justifyContent(FlexJustifyContent.FLEX_END)
                } else {
                    justifyContent(FlexJustifyContent.FLEX_START)
                }
            }

            if (!message.isSelf) {
                // 对方消息：名字在第一组消息的上方，与气泡左边缘对齐
                View {
                    attr {
                        flexDirection(FlexDirection.COLUMN)
                        flex(1f)
                    }
                    if (layout.showSenderName) {
                        View {
                            attr {
                                if (listOptions.showAvatar) {
                                    marginLeft(theme.avatarSize + theme.avatarBubbleGap)
                                }
                            }
                            cfg.componentFactory.renderSenderName(
                                this@View, message.senderName, themeColors.senderNameColor, 12f
                            )
                        }
                    }
                    // 头像 + 图片行
                    View {
                        attr {
                            flexDirectionRow()
                        }
                        if (layout.showAvatar) {
                            cfg.componentFactory.renderAvatar(
                                this@View,
                                message.senderAvatar.ifEmpty { ChatBubbleView.DEFAULT_AVATAR },
                                theme.avatarSize,
                                theme.avatarRadius,
                                themeColors.avatarPlaceholderColor
                            ) { cfg.onAvatarClick?.invoke(message) }
                        } else if (listOptions.showAvatar) {
                            cfg.componentFactory.renderAvatarPlaceholder(this@View, theme.avatarSize)
                        }
                        View {
                            attr {
                                marginLeft(if (listOptions.showAvatar) theme.avatarBubbleGap else 0f)
                                flexDirection(FlexDirection.COLUMN)
                            }
                            renderImageThumbnail(message, cfg, size, isVideo)
                            renderImageFooter(message, cfg, themeColors)
                        }
                    }
                }
            } else {
                // 自己的消息：重发按钮 + 图片 + 头像在右
                if (message.status == MessageStatus.FAILED) {
                    renderResendButton(message, cfg, themeColors)
                }
                View {
                    attr {
                        flexDirection(FlexDirection.COLUMN)
                        alignItems(FlexAlign.FLEX_END)
                    }
                    renderImageThumbnail(message, cfg, size, isVideo)
                    renderImageFooter(message, cfg, themeColors)
                }
                if (layout.showAvatar) {
                    View {
                        attr { marginLeft(theme.avatarBubbleGap) }
                        cfg.componentFactory.renderAvatar(
                            this@View,
                            cfg.selfAvatarUrl.ifEmpty { ChatBubbleView.SELF_AVATAR },
                            theme.avatarSize,
                            theme.avatarRadius,
                            themeColors.avatarPlaceholderColor
                        ) { cfg.onAvatarClick?.invoke(message) }
                    }
                } else if (listOptions.showAvatar) {
                    View {
                        attr { marginLeft(theme.avatarBubbleGap) }
                        cfg.componentFactory.renderAvatarPlaceholder(this@View, theme.avatarSize)
                    }
                }
            }
        }
    }
}

/** 图片显示尺寸：优先按原始宽高等比缩放到主题上限，缺少尺寸信息时退化为上限值。 */
private fun resolveImageDisplaySize(message: ChatMessage, theme: ChatThemeOptions): Pair<Float, Float> {
    val attachment = message.attachments.firstOrNull()
    val rawWidth = attachment?.width?.toFloat() ?: message.extra["width"]?.toFloatOrNull() ?: 0f
    val rawHeight = attachment?.height?.toFloat() ?: message.extra["height"]?.toFloatOrNull() ?: 0f
    val maxW = theme.imageMaxWidth
    val maxH = theme.imageMaxHeight

    return if (rawWidth > 0 && rawHeight > 0) {
        val scale = minOf(maxW / rawWidth, maxH / rawHeight, 1f)
        (rawWidth * scale) to (rawHeight * scale)
    } else {
        maxW to maxH
    }
}

/** 缩略图本体：图片 + 视频播放按钮覆盖层 + 点击/长按事件。 */
private fun ViewContainer<*, *>.renderImageThumbnail(
    message: ChatMessage,
    cfg: ChatSessionConfig,
    size: Pair<Float, Float>,
    isVideo: Boolean
) {
    val (displayWidth, displayHeight) = size
    val attachment = message.attachments.firstOrNull()
    var imgBubbleRef: ViewRef<DivView>? = null

    View {
        ref { imgBubbleRef = it }
        attr {
            size(displayWidth, displayHeight)
            borderRadius(cfg.theme.imageRadius)
            backgroundColor(Color(cfg.theme.resolvedColors().avatarPlaceholderColor))
        }
        Image {
            attr {
                size(displayWidth, displayHeight)
                borderRadius(cfg.theme.imageRadius)
                // 视频消息优先使用缩略图
                val imgSrc = if (isVideo) {
                    attachment?.thumbnailUrl?.ifEmpty { message.content } ?: message.content
                } else {
                    message.content
                }
                src(imgSrc)
                resizeCover()
            }
        }
        if (isVideo) {
            renderVideoPlayOverlay(displayWidth, displayHeight)
        }
        event {
            click {
                cfg.onMessageClick?.invoke(message)
            }
            longPress {
                if (cfg.onMessageLongPressWithPosition != null) {
                    imgBubbleRef?.view?.let { view ->
                        val frameInRoot = view.convertFrame(view.frame, toView = null)
                        val correctedY = cfg.correctBubbleY(frameInRoot.y)
                        cfg.onMessageLongPressWithPosition?.invoke(
                            message, frameInRoot.x, correctedY,
                            frameInRoot.width, frameInRoot.height
                        )
                    } ?: cfg.onMessageLongPress?.invoke(message)
                } else {
                    cfg.onMessageLongPress?.invoke(message)
                }
            }
        }
    }
}

/** 视频缩略图中央的半透明播放按钮。 */
private fun ViewContainer<*, *>.renderVideoPlayOverlay(width: Float, height: Float) {
    View {
        attr {
            positionAbsolute()
            top(0f)
            left(0f)
            size(width, height)
            allCenter()
        }
        View {
            attr {
                size(48f, 48f)
                borderRadius(24f)
                backgroundColor(Color(0x80000000))
                allCenter()
            }
            Text {
                attr {
                    text("▶")
                    fontSize(20f)
                    color(Color.WHITE)
                    marginLeft(3f) // 视觉居中微调
                }
            }
        }
    }
}

/** 图片下方的置顶标记、线程回复入口与反应栏。 */
private fun ViewContainer<*, *>.renderImageFooter(
    message: ChatMessage,
    cfg: ChatSessionConfig,
    themeColors: ChatThemeColors
) {
    if (message.isPinned) {
        Text {
            attr {
                text("📌 已置顶")
                fontSize(10f)
                color(Color(themeColors.pinnedIndicatorColor))
                marginTop(2f)
            }
        }
    }
    if (message.threadCount > 0) {
        View {
            attr {
                flexDirectionRow()
                alignItems(FlexAlign.CENTER)
                marginTop(4f)
            }
            Text {
                attr {
                    text("💬 ${message.threadCount} 条回复")
                    fontSize(12f)
                    color(Color(cfg.theme.primaryColor))
                    fontWeightMedium()
                }
            }
            event {
                click { cfg.onThreadClick?.invoke(message) }
            }
        }
    }
    if (message.reactions.isNotEmpty()) {
        ChatReactionBar {
            attr {
                reactions = message.reactions
            }
            event {
                onReactionClick = { type ->
                    cfg.onReactionClick?.invoke(message, type)
                }
            }
        }
    }
}

/** 发送失败时图片左侧的重发按钮。 */
private fun ViewContainer<*, *>.renderResendButton(
    message: ChatMessage,
    cfg: ChatSessionConfig,
    themeColors: ChatThemeColors
) {
    View {
        attr {
            size(24f, 24f)
            borderRadius(12f)
            backgroundColor(Color(themeColors.errorColor))
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
            click { cfg.onResend?.invoke(message) }
        }
    }
}
