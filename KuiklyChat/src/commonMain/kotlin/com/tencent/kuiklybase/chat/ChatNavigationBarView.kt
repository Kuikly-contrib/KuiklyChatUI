package com.tencent.kuiklybase.chat

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.*

// ============================
// 聊天导航栏组件
// ============================

class ChatNavigationBarView : ComposeView<ChatNavBarAttr, ChatNavBarEvent>() {
    override fun createAttr(): ChatNavBarAttr = ChatNavBarAttr()
    override fun createEvent(): ChatNavBarEvent = ChatNavBarEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    paddingTop(ctx.pagerData.statusBarHeight)
                    backgroundLinearGradient(
                        Direction.TO_RIGHT,
                        ColorStop(Color(ctx.attr.primaryColor), 0f),
                        ColorStop(Color(ctx.attr.primaryGradientEndColor), 1f)
                    )
                }
                // 导航栏内容区
                View {
                    attr {
                        height(48f)
                        flexDirectionRow()
                        alignItemsCenter()
                    }
                    // 返回按钮
                    if (ctx.attr.showBackButton) {
                        View {
                            attr {
                                size(48f, 48f)
                                allCenter()
                            }
                            Image {
                                attr {
                                    size(10f, 17f)
                                    tintColor(Color.WHITE)
                                    src(BACK_ARROW_ICON)
                                }
                            }
                            event {
                                click {
                                    if (ctx.event.onBackClick != null) {
                                        ctx.event.onBackClick?.invoke()
                                    } else {
                                        ctx.getPager()
                                            .acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                                            .closePage()
                                    }
                                }
                            }
                        }
                    } else {
                        // 不显示返回按钮时，左侧填充
                        View {
                            attr {
                                size(48f, 48f)
                            }
                        }
                    }
                    // 标题
                    Text {
                        attr {
                            flex(1f)
                            text(ctx.attr.title)
                            fontSize(17f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                            textAlignCenter()
                        }
                    }
                    // 右侧占位（保持标题居中）
                    View {
                        attr {
                            size(48f, 48f)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val BACK_ARROW_ICON =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII="
    }
}

class ChatNavBarAttr : ComposeAttr() {
    var title: String by observable("聊天")
    var showBackButton: Boolean by observable(true)
    var primaryColor: Long by observable(0xFF4F8FFF)
    var primaryGradientEndColor: Long by observable(0xFF6C5CE7)
}

class ChatNavBarEvent : ComposeEvent() {
    var onBackClick: (() -> Unit)? = null
}

fun ViewContainer<*, *>.ChatNavigationBar(init: ChatNavigationBarView.() -> Unit) {
    addChild(ChatNavigationBarView(), init)
}
