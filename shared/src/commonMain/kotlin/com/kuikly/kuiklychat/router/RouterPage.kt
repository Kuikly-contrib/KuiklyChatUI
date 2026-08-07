package com.kuikly.kuiklychat.router

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.utils.urlParams
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.reactive.handler.*
import com.kuikly.kuiklychat.base.BasePager
import com.kuikly.kuiklychat.base.bridgeModule

@Page("router", supportInLocal = true)
internal class RouterPage : BasePager() {

    var inputText: String = ""
    lateinit var inputRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }


            View {
                attr {
                    allCenter()
                    margin(20f)
                }
                Text {
                    attr {
                        fontSize(20f)
                        text("ImageAdapter基准测试")
                        textDecorationUnderLine()
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFFAD37FE), 0f),
                            ColorStop(Color(0xFF23D3FD), 1f)
                        )
                    }
                }
                event {
                    click {
                        ctx.jumpPage("image_adapter")
                    }
                }
            }

            // 聊天会话组件 Demo 入口
            View {
                attr {
                    allCenter()
                    marginLeft(20f)
                    marginRight(20f)
                    marginBottom(20f)
                }
                View {
                    attr {
                        size(220f, 48f)
                        borderRadius(24f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF4F8FFF), 0f),
                            ColorStop(Color(0xFF6C5CE7), 1f)
                        )
                        allCenter()
                        boxShadow(BoxShadow(0f, 4f, 12f, Color(0x334F8FFF)))
                    }
                    Text {
                        attr {
                            text("💬 聊天会话 Demo")
                            fontSize(18f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("chat")
                        }
                    }
                }
            }

            // KuiklyAI Compose DSL Demo 入口
            View {
                attr {
                    allCenter()
                    marginLeft(20f)
                    marginRight(20f)
                    marginBottom(20f)
                }
                View {
                    attr {
                        size(220f, 48f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF176B5B))
                        allCenter()
                        boxShadow(BoxShadow(0f, 3f, 10f, Color(0x33176B5B)))
                    }
                    Text {
                        attr {
                            text("KuiklyAI Demo")
                            fontSize(17f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("kuiklyai_demo")
                        }
                    }
                }
            }

            // AI 打字指示器 Demo 入口
            View {
                attr {
                    allCenter()
                    marginLeft(20f)
                    marginRight(20f)
                    marginBottom(20f)
                }
                View {
                    attr {
                        size(220f, 48f)
                        borderRadius(24f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF6C5CE7), 0f),
                            ColorStop(Color(0xFFA29BFE), 1f)
                        )
                        allCenter()
                        boxShadow(BoxShadow(0f, 4f, 12f, Color(0x336C5CE7)))
                    }
                    Text {
                        attr {
                            text("✨ AI 打字指示器 Demo")
                            fontSize(18f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("ai_typing_demo")
                        }
                    }
                }
            }

            // AI 消息文本 Demo 入口
            View {
                attr {
                    allCenter()
                    marginLeft(20f)
                    marginRight(20f)
                    marginBottom(20f)
                }
                View {
                    attr {
                        size(260f, 48f)
                        borderRadius(24f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF00B894), 0f),
                            ColorStop(Color(0xFF00CEC9), 1f)
                        )
                        allCenter()
                        boxShadow(BoxShadow(0f, 4f, 12f, Color(0x3300B894)))
                    }
                    Text {
                        attr {
                            text("📝 AI 消息文本 Demo")
                            fontSize(18f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("ai_message_text_demo")
                        }
                    }
                }
            }

            // 频道列表 Demo 入口
            View {
                attr {
                    allCenter()
                    marginLeft(20f)
                    marginRight(20f)
                    marginBottom(20f)
                }
                View {
                    attr {
                        size(260f, 48f)
                        borderRadius(24f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFFE17055), 0f),
                            ColorStop(Color(0xFFD63031), 1f)
                        )
                        allCenter()
                        boxShadow(BoxShadow(0f, 4f, 12f, Color(0x33E17055)))
                    }
                    Text {
                        attr {
                            text("📋 频道列表 Demo")
                            fontSize(18f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("channel_list_demo")
                        }
                    }
                }
            }

            // 频道详情 Demo 入口
            View {
                attr {
                    allCenter()
                    marginLeft(20f)
                    marginRight(20f)
                    marginBottom(20f)
                }
                View {
                    attr {
                        size(260f, 48f)
                        borderRadius(24f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF0984E3), 0f),
                            ColorStop(Color(0xFF74B9FF), 1f)
                        )
                        allCenter()
                        boxShadow(BoxShadow(0f, 4f, 12f, Color(0x330984E3)))
                    }
                    Text {
                        attr {
                            text("👥 频道详情 Demo")
                            fontSize(18f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("channel_detail_demo")
                        }
                    }
                }
            }
            // AG-UI 协议 Demo 入口
            View {
                attr {
                    allCenter()
                    marginLeft(20f)
                    marginRight(20f)
                    marginBottom(20f)
                }
                View {
                    attr {
                        size(260f, 48f)
                        borderRadius(24f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF8E44AD), 0f),
                            ColorStop(Color(0xFF3498DB), 1f)
                        )
                        allCenter()
                        boxShadow(BoxShadow(0f, 4f, 12f, Color(0x338E44AD)))
                    }
                    Text {
                        attr {
                            text("🤖 AG-UI 协议 Demo")
                            fontSize(18f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("agui_demo")
                        }
                    }
                }
            }

            // AI-Agent 富卡片 Demo 入口
            View {
                attr {
                    allCenter()
                    marginLeft(20f)
                    marginRight(20f)
                    marginBottom(20f)
                }
                View {
                    attr {
                        size(260f, 48f)
                        borderRadius(24f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF6366F1), 0f),
                            ColorStop(Color(0xFFE11D48), 1f)
                        )
                        allCenter()
                        boxShadow(BoxShadow(0f, 4f, 12f, Color(0x336366F1)))
                    }
                    Text {
                        attr {
                            text("🤖✨ AI-Agent 富卡片 Demo")
                            fontSize(18f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("agent_rich_card_demo")
                        }
                    }
                }
            }

            // Agent 卡片 UI（独立 Demo） 入口
            View {
                attr {
                    allCenter()
                    marginLeft(20f)
                    marginRight(20f)
                    marginBottom(20f)
                }
                View {
                    attr {
                        size(260f, 48f)
                        borderRadius(24f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF8B5CF6), 0f),
                            ColorStop(Color(0xFF06B6D4), 1f)
                        )
                        allCenter()
                        boxShadow(BoxShadow(0f, 4f, 12f, Color(0x338B5CF6)))
                    }
                    Text {
                        attr {
                            text("🎨 Agent 卡片 UI")
                            fontSize(18f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("agent_card_demo")
                        }
                    }
                }
            }


            // Compose 聊天ui sdk Demo 入口
            View {
                attr {
                    allCenter()
                    marginLeft(20f)
                    marginRight(20f)
                    marginBottom(20f)
                }
                View {
                    attr {
                        size(260f, 48f)
                        borderRadius(24f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF6366F1), 0f),
                            ColorStop(Color(0xFFE11D48), 1f)
                        )
                        allCenter()
                        boxShadow(BoxShadow(0f, 4f, 12f, Color(0x336366F1)))
                        marginBottom(12f)
                    }
                    Text {
                        attr {
                            text("Compose Chat Cache Module Demo")
                            fontSize(16f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("compose_chat_cache_demo")
                        }
                    }
                }
                View {
                    attr {
                        size(260f, 48f)
                        borderRadius(24f)
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF6366F1), 0f),
                            ColorStop(Color(0xFFE11D48), 1f)
                        )
                        allCenter()
                        boxShadow(BoxShadow(0f, 4f, 12f, Color(0x336366F1)))
                    }
                    Text {
                        attr {
                            text("Compose 聊天ui sdk Demo")
                            fontSize(18f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("compose_chat_sdk_demo")
                        }
                    }
                }
            }



        }

    }

    override fun created() {
        super.created()
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        val cacheInputText =
            acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME).getItem(
                CACHE_KEY
            )
        if (cacheInputText.isNotEmpty()) {
            inputRef.view?.setText(cacheInputText)
        }
    }

    private fun jumpPage(inputText: String) {
        val params = urlParams("pageName=$inputText")
        val pageData = JSONObject()
        params.forEach {
            pageData.put(it.key, it.value)
        }
        val pageName = pageData.optString("pageName")
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName, pageData)
    }

    companion object {
        const val PLACEHOLDER = "输入pageName"
        const val TIP = "输入规则：router 或者 router&key=value (&后面为页面参数)"
        const val CACHE_KEY = "router_last_input_key2"
        const val BG_URL =
            "https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg"
        const val LOGO = "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/62394e19.png"
        const val JUMP_TEXT = "跳转"
        const val TEXT_KEY = "text"
        const val TITLE = "Kuikly页面路由"
        private const val AAR_MODE_TIP = "如：router 或者 router&key=value （&后面为页面参数）"
    }

}

internal class RouterNavigationBar : ComposeView<RouterNavigationBarAttr, ComposeEvent>() {
    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun createAttr(): RouterNavigationBarAttr {
        return RouterNavigationBarAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    paddingTop(ctx.pagerData.statusBarHeight)
                    backgroundColor(Color.WHITE)
                }
                // nav bar
                View {
                    attr {
                        height(44f)
                        allCenter()
                    }

                    Text {
                        attr {
                            text(ctx.attr.title)
                            fontSize(17f)
                            fontWeightSemisolid()
                            backgroundLinearGradient(
                                Direction.TO_BOTTOM,
                                ColorStop(Color(0xFF23D3FD), 0f),
                                ColorStop(Color(0xFFAD37FE), 1f)
                            )

                        }
                    }

                }

                vif({ !ctx.attr.backDisable }) {
                    Image {
                        attr {
                            absolutePosition(
                                top = 12f + getPager().pageData.statusBarHeight,
                                left = 12f,
                                bottom = 12f,
                                right = 12f
                            )
                            size(10f, 17f)
                            src("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII=")
                        }
                        event {
                            click {
                                getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                                    .closePage()
                            }
                        }
                    }
                }

            }
        }
    }
}

internal class RouterNavigationBarAttr : ComposeAttr() {
    var title: String by observable("")
    var backDisable = false
}

internal fun ViewContainer<*, *>.RouterNavBar(init: RouterNavigationBar.() -> Unit) {
    addChild(RouterNavigationBar(), init)
}
