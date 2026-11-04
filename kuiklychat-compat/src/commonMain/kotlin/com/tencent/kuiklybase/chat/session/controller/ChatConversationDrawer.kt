package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chat.compose.core.ChatConversationSummary

private const val DRAWER_ANIMATION_DURATION_S = 0.24f
private const val DRAWER_ANIMATION_DURATION_MS = 240

internal fun ChatControllerSessionView.reloadConversations() {
    val host = sessionAttr().conversationHost ?: return
    sessionAttr().conversationTitle = host.directory.activeTitle
    sessionAttr().activeConversationId = host.directory.activeConversationId
    sessionAttr().conversations.diffUpdate(host.directory.conversations) { old, new -> old == new }
    sessionAttr().archivedConversations.diffUpdate(host.directory.archivedConversations) { old, new -> old == new }
}

internal fun ChatControllerSessionView.openConversation(conversationId: String) {
    sessionAttr().stickToBottom = true
    closeConversationDrawer()
    sessionAttr().menuConversationId = ""
    sessionAttr().conversationHost?.open(conversationId)
    reloadConversations()
}

internal fun ChatControllerSessionView.openConversationDrawer() {
    dismissComposerKeyboard()
    sessionAttr().conversationHost?.rememberCurrent()
    sessionAttr().drawerOpen = true
    sessionAttr().drawerMounted = true
    reloadConversations()
    val pageId = pagerId
    com.tencent.kuikly.core.timer.setTimeout(pageId, 16) {
        if (sessionAttr().drawerOpen) sessionAttr().drawerPresented = true
    }
}

internal fun ChatControllerSessionView.closeConversationDrawer() {
    if (!sessionAttr().drawerMounted) return
    sessionAttr().drawerOpen = false
    sessionAttr().drawerPresented = false
    sessionAttr().menuConversationId = ""
    sessionAttr().renameConversationId = ""
    val pageId = pagerId
    com.tencent.kuikly.core.timer.setTimeout(pageId, DRAWER_ANIMATION_DURATION_MS) {
        if (!sessionAttr().drawerPresented && !sessionAttr().drawerOpen) {
            sessionAttr().drawerMounted = false
        }
    }
}

internal fun ChatControllerSessionView.openConversationMenu(
    conversationId: String,
    anchorX: Float,
    anchorY: Float,
) {
    sessionAttr().menuAnchorX = anchorX
    sessionAttr().menuAnchorY = anchorY
    sessionAttr().menuConversationId = conversationId
}

internal fun ChatControllerSessionView.conversationDrawer(container: ViewContainer<*, *>) {
    val host = this
    container.Modal(inWindow = true) {
        attr {
            absolutePositionAllZero()
            flexDirection(FlexDirection.ROW)
            backgroundColor(Color(0x00000000))
        }
        View {
            attr {
                width(host.pagerData.pageViewWidth * 0.78f)
                height(host.pagerData.pageViewHeight)
                flexDirection(FlexDirection.COLUMN)
                backgroundColor(Color(host.sessionAttr().theme.composerColor))
                paddingTop(maxOf(host.pagerData.statusBarHeight, host.pagerData.safeAreaInsets.top).let { if (it >= 20f) it else 32f })
                transform(Translate(if (host.sessionAttr().drawerPresented) 0f else -1f, 0f))
                animation(Animation.easeOut(DRAWER_ANIMATION_DURATION_S), host.sessionAttr().drawerPresented)
            }
            host.drawerToolbar(this)
            Scroller {
                attr { flex(1f); flexDirection(FlexDirection.COLUMN); paddingLeft(12f); paddingRight(12f) }
                vif({
                    host.sessionAttr().conversations.isEmpty() &&
                        host.sessionAttr().archivedConversations.isEmpty()
                }) {
                    View {
                        attr {
                            height(120f)
                            allCenter()
                        }
                        Text {
                            attr {
                                text("暂无已保存的对话")
                                fontSize(14f)
                                color(Color(host.sessionAttr().theme.placeholderColor))
                            }
                        }
                    }
                }
                vfor({ host.sessionAttr().conversations }) { conversation ->
                    host.conversationRow(this, conversation, archived = false)
                }
                vif({ host.sessionAttr().archivedConversations.isNotEmpty() }) {
                    Text {
                        attr {
                            text("已归档")
                            fontSize(12f)
                            marginTop(16f)
                            marginBottom(6f)
                            color(Color(host.sessionAttr().theme.placeholderColor))
                        }
                    }
                }
            vfor({ host.sessionAttr().archivedConversations }) { conversation ->
                host.conversationRow(this, conversation, archived = true)
            }
        }
        }
        View {
            attr { flex(1f); height(host.pagerData.pageViewHeight) }
            event { click { host.closeConversationDrawer() } }
        }
        vif({ host.sessionAttr().menuConversationId.isNotEmpty() }) {
            host.conversationMenuOverlay(this)
        }
    }
}

internal fun ChatControllerSessionView.conversationDrawerScrim(container: ViewContainer<*, *>) {
    val host = this
    container.View {
        attr {
            absolutePositionAllZero()
            zIndex(9)
            backgroundColor(Color(0xFF141A18))
            opacity(if (host.sessionAttr().drawerPresented) 0.34f else 0f)
            touchEnable(host.sessionAttr().drawerOpen)
            animation(Animation.easeInOut(DRAWER_ANIMATION_DURATION_S), host.sessionAttr().drawerPresented)
        }
        event { click { host.closeConversationDrawer() } }
    }
}

private fun ChatControllerSessionView.drawerToolbar(container: ViewContainer<*, *>) {
    val host = this
    container.View {
        attr {
            height(48f)
            flexDirection(FlexDirection.ROW)
            alignItemsCenter()
            paddingLeft(16f)
            paddingRight(16f)
        }
        Text {
            attr {
                text("会话")
                fontSize(17f)
                fontWeightMedium()
                color(Color(host.sessionAttr().theme.incomingTextColor))
                flex(1f)
            }
        }
        Text {
            attr {
                text("新对话")
                fontSize(14f)
                color(Color(host.sessionAttr().theme.accentColor))
            }
            event {
                click {
                    host.sessionAttr().stickToBottom = true
                    host.closeConversationDrawer()
                    host.sessionAttr().conversationHost?.startNew()
                    host.reloadConversations()
                }
            }
        }
    }
}

private fun ChatControllerSessionView.conversationRow(
    container: ViewContainer<*, *>,
    conversation: ChatConversationSummary,
    archived: Boolean,
) {
    val host = this
    container.View {
        attr {
            alignSelfStretch()
            paddingTop(10f)
            paddingBottom(10f)
            paddingLeft(10f)
            paddingRight(10f)
            borderRadius(12f)
            marginBottom(6f)
            backgroundColor(
                if (!archived && conversation.id == host.sessionAttr().activeConversationId) {
                    Color(host.sessionAttr().theme.systemBackgroundColor)
                } else {
                    Color(0x00000000)
                }
            )
        }
        View {
            attr {
                alignSelfStretch()
                flexDirection(FlexDirection.ROW)
                alignItemsCenter()
            }
            event { click { if (!archived) host.openConversation(conversation.id) } }
            Text {
                attr {
                    text(conversation.title.ifBlank { "新对话" })
                    fontSize(15f)
                    color(Color(host.sessionAttr().theme.incomingTextColor))
                    flex(1f)
                    lines(1)
                }
            }
            View {
                attr { size(36f, 36f); allCenter() }
                event {
                    click { params -> host.openConversationMenu(conversation.id, params.pageX, params.pageY) }
                }
                Text {
                    attr {
                        text("···")
                        fontSize(18f)
                        color(Color(host.sessionAttr().theme.placeholderColor))
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.drawerAction(
    host: ChatControllerSessionView,
    label: String,
    onClick: () -> Unit,
) {
    View {
        attr {
            alignSelfStretch()
            height(38f)
            borderRadius(8f)
            paddingLeft(10f)
            justifyContent(com.tencent.kuikly.core.layout.FlexJustifyContent.CENTER)
        }
        event { click { onClick() } }
        Text {
            attr {
                text(label)
                fontSize(13f)
                color(Color(host.sessionAttr().theme.accentColor))
            }
        }
    }
}

internal fun ChatControllerSessionView.conversationMenuOverlay(container: ViewContainer<*, *>) {
    val host = this
    val targetId = host.sessionAttr().menuConversationId
    val target = host.sessionAttr().conversations.firstOrNull { it.id == targetId }
        ?: host.sessionAttr().archivedConversations.firstOrNull { it.id == targetId }
        ?: return
    val archived = host.sessionAttr().archivedConversations.any { it.id == targetId }
    container.View {
        attr {
            absolutePositionAllZero()
            zIndex(30)
        }
        View {
            attr {
                absolutePositionAllZero()
            }
            event { click { host.sessionAttr().menuConversationId = "" } }
        }
        View {
            attr {
                positionAbsolute()
                left((host.sessionAttr().menuAnchorX - 132f).coerceAtLeast(12f))
                top((host.sessionAttr().menuAnchorY + 4f).coerceAtMost(host.pagerData.pageViewHeight - 150f))
                width(132f)
                padding(4f)
                flexDirection(FlexDirection.COLUMN)
                alignItemsStretch()
                borderRadius(12f)
                backgroundColor(Color(host.sessionAttr().theme.composerColor))
                border(Border(0.5f, BorderStyle.SOLID, Color(host.sessionAttr().theme.borderColor)))
                boxShadow(BoxShadow(0f, 4f, 14f, Color(0x24000000)))
            }
            drawerAction(host, "重命名") {
                host.sessionAttr().renameDraft = target.title
                host.sessionAttr().renameConversationId = targetId
                host.sessionAttr().menuConversationId = ""
            }
            if (!archived) {
                drawerAction(host, "归档") {
                    host.sessionAttr().menuConversationId = ""
                    host.sessionAttr().conversationHost?.archive(targetId)
                    host.reloadConversations()
                }
            } else {
                drawerAction(host, "恢复") {
                    host.sessionAttr().menuConversationId = ""
                    host.sessionAttr().conversationHost?.restoreArchived(targetId)
                    host.reloadConversations()
                }
            }
            drawerAction(host, "删除") {
                val conversationId = host.sessionAttr().menuConversationId
                host.sessionAttr().menuConversationId = ""
                host.sessionAttr().stickToBottom = true
                host.sessionAttr().conversationHost?.delete(if (conversationId.isEmpty()) targetId else conversationId)
                host.reloadConversations()
            }
        }
    }
}
