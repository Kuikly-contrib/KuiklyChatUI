package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

internal fun ChatControllerSessionView.renameDialog(container: ViewContainer<*, *>) {
    val host = this
    container.Modal(inWindow = true) {
        attr {
            absolutePositionAllZero()
            allCenter()
        }
        View {
            attr {
                absolutePositionAllZero()
                backgroundColor(Color(0x66000000))
            }
            event { click { host.dismissRenameDialog() } }
        }
        View {
            attr {
                width((host.pagerData.pageViewWidth - 32f).coerceAtMost(360f).coerceAtLeast(240f))
                padding(20f)
                borderRadius(16f)
                backgroundColor(Color(host.sessionAttr().theme.composerColor))
                border(Border(0.5f, BorderStyle.SOLID, Color(host.sessionAttr().theme.borderColor)))
                boxShadow(BoxShadow(0f, 8f, 24f, Color(0x30000000)))
            }
            event { click { } }
            Text {
                attr {
                    text("重命名会话")
                    fontSize(18f)
                    fontWeightMedium()
                    color(Color(host.sessionAttr().theme.incomingTextColor))
                    marginBottom(16f)
                }
            }
            Input {
                ref {
                    host.renameInputRef = it
                    it.view?.setText(host.sessionAttr().renameDraft)
                }
                attr {
                    height(44f)
                    fontSize(16f)
                    placeholder("会话名称")
                    color(Color(host.sessionAttr().theme.incomingTextColor))
                    backgroundColor(Color(host.sessionAttr().theme.backgroundColor))
                    border(Border(0.5f, BorderStyle.SOLID, Color(host.sessionAttr().theme.borderColor)))
                    borderRadius(8f)
                }
                event { textDidChange { host.sessionAttr().renameDraft = it.text } }
            }
            View {
                attr {
                    flexDirection(FlexDirection.ROW)
                    justifyContent(FlexJustifyContent.FLEX_END)
                    marginTop(20f)
                }
                renameDialogAction(host, "取消", primary = false) {
                    host.dismissRenameDialog()
                }
                renameDialogAction(host, "保存", primary = true) {
                    val attr = host.sessionAttr()
                    attr.conversationHost?.rename(attr.renameConversationId, attr.renameDraft)
                    host.dismissRenameDialog()
                    host.reloadConversations()
                }
            }
        }
    }
}

private fun ChatControllerSessionView.dismissRenameDialog() {
    renameInputRef?.view?.blur()
    renameInputRef = null
    sessionAttr().renameConversationId = ""
}

private fun ViewContainer<*, *>.renameDialogAction(
    host: ChatControllerSessionView,
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(44f)
            minWidth(72f)
            paddingLeft(14f)
            paddingRight(14f)
            marginLeft(8f)
            borderRadius(10f)
            allCenter()
            backgroundColor(
                Color(
                    if (primary) host.sessionAttr().theme.accentColor
                    else host.sessionAttr().theme.backgroundColor,
                ),
            )
            if (!primary) {
                border(Border(0.5f, BorderStyle.SOLID, Color(host.sessionAttr().theme.borderColor)))
            }
        }
        event { click { onClick() } }
        Text {
            attr {
                text(label)
                fontSize(14f)
                fontWeightMedium()
                color(
                    Color(
                        if (primary) host.sessionAttr().theme.outgoingTextColor
                        else host.sessionAttr().theme.incomingTextColor,
                    ),
                )
            }
        }
    }
}
