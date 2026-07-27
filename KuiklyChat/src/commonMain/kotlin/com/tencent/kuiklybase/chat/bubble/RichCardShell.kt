package com.tencent.kuiklybase.chat.bubble

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.*
import com.tencent.kuiklybase.chat.agui.LoadingStyle

// ============================
// 通用工具：AI 卡片外壳
// ============================

/**
 * 统一的 AI 卡片外壳：左侧头像占位 + 右侧卡片主体。
 *
 * 提供一致的气泡左右布局，避免每个卡片重复写 `View { width(48f) } View { flex(1f) ... }`。
 *
 * @param accentColor 主题强调色（左侧竖条 + 标题）
 * @param borderColor 卡片边框色（null 不画边框）
 * @param backgroundColor 卡片背景色
 * @param header 头部 slot（图标 + 标题）
 * @param body 主体 slot（展示核心内容）
 */
internal fun ViewContainer<*, *>.agUiCardShell(
    accentColor: Color,
    borderColor: Color? = null,
    backgroundColor: Color = Color(0xFFFFFFFF),
    header: ViewContainer<*, *>.() -> Unit,
    body: ViewContainer<*, *>.() -> Unit
) {
    View {
        attr {
            flexDirectionRow()
            alignSelfStretch()
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(4f)
            paddingBottom(4f)
        }
        // 左侧头像占位
        View { attr { width(48f) } }

        // 右侧卡片
        View {
            attr {
                flex(1f)
                borderRadius(10f)
                backgroundColor(backgroundColor)
                borderColor?.let { border(Border(1f, BorderStyle.SOLID, it)) }
                paddingLeft(12f)
                paddingRight(12f)
                paddingTop(10f)
                paddingBottom(10f)
            }
            // Header
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                    marginBottom(8f)
                }
                // 左侧强调色竖条
                View {
                    attr {
                        width(3f)
                        height(14f)
                        borderRadius(1.5f)
                        backgroundColor(accentColor)
                        marginRight(8f)
                    }
                }
                header()
            }
            body()
        }
    }
}

/**
 * Loading 小图标（根据风格渲染不同样式）。
 *
 * 注：真正的动画效果需要业务方结合 Animator 实现，这里先提供视觉占位。
 */
internal fun ViewContainer<*, *>.LoadingIndicator(style: String, color: Color) {
    when (style) {
        LoadingStyle.SPINNER -> {
            Text {
                attr {
                    text("⏳")
                    fontSize(14f)
                }
            }
        }
        LoadingStyle.PROGRESS -> {
            Text {
                attr {
                    text("▰")
                    fontSize(14f)
                    color(color)
                }
            }
        }
        LoadingStyle.SKELETON -> {
            View {
                attr {
                    width(14f)
                    height(14f)
                    borderRadius(3f)
                    backgroundColor(Color(0xFFE2E8F0))
                }
            }
        }
        else -> {
            // DOTS：静态三个点
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                }
                repeat(3) { i ->
                    View {
                        attr {
                            size(5f, 5f)
                            borderRadius(2.5f)
                            backgroundColor(color)
                            if (i < 2) marginRight(2f)
                        }
                    }
                }
            }
        }
    }
}
