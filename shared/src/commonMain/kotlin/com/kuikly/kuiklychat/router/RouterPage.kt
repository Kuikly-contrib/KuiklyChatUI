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
                        backgroundColor(Color(0xFF1D4ED8))
                        allCenter()
                    }
                    Text {
                        attr {
                            text("Chat 组件 Demo")
                            fontSize(17f)
                            fontWeightSemisolid()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            ctx.jumpPage("chat_component_demo")
                        }
                    }
                }
            }

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
                    }
                    Text {
                        attr {
                            text("A2UI Demo")
                            fontSize(17f)
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
