package com.tencent.kuiklybase.chat.agentcard_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/*
 * 原生 macOS 异步 Markdown 渲染组件。
 *
 * 动机：
 *   Compose 版 [StreamingMarkdownView] 每次流式追加都要重新解析 + 重新布局，
 *   在几 KB 内容 + 30~60 Hz 追加场景下会出现明显掉帧。
 *   此组件把解析与排版搬到 macOS 原生侧（Swift + Down/cmark），Kuikly 跨端层
 *   只负责把 Markdown 字符串透传过去 + 接收回传的内容高度。
 *
 * 映射关系：
 *   Kotlin  viewName = "KRMarkdownView"
 *   macOS   KRMarkdownView (ObjC) + KRMarkdownRenderer (Swift, Down)
 *
 * 数据流：
 *   Compose state (content) 变化
 *     → viewUpdate { attr.content(...) }
 *     → ObjC setCss_content: → 串行队列异步调用 Down 渲染
 *     → 主线程 assign attributedString 到 NSTextView
 *     → sendWithEvent("onContentSizeChanged", {height})
 *     → Kotlin 侧 measuredHeight 更新 → Modifier.height() 生效
 */

// ── Attr ────────────────────────────────────────────────────────────

internal class NativeMarkdownAttr : Attr() {
    fun content(value: String): NativeMarkdownAttr {
        "content" with value
        return this
    }

    fun fontSize(value: Float): NativeMarkdownAttr {
        "fontSize" with value
        return this
    }

    fun streaming(value: Boolean): NativeMarkdownAttr {
        "streaming" with if (value) "1" else "0"
        return this
    }

    /** 正文字色（ARGB 整数，例如 0xFF334155 = slate-700） */
    fun textColor(argb: Long): NativeMarkdownAttr {
        "textColor" with argb
        return this
    }

    /** "markdown"（默认）或 "code" */
    fun mode(value: String): NativeMarkdownAttr {
        "mode" with value
        return this
    }

    /** code 模式下的语言提示（如 "kotlin"），影响语法高亮 */
    fun codeLanguage(value: String): NativeMarkdownAttr {
        "codeLanguage" with value
        return this
    }
}

// ── Event ───────────────────────────────────────────────────────────

internal class NativeMarkdownEvent : Event() {
    /** 原生侧内容实际高度变化时回调（单位 pt，与 dp 等价）。 */
    fun onContentSizeChanged(handler: (Float) -> Unit) {
        register("onContentSizeChanged") { param ->
            val json = param as? JSONObject ?: return@register
            val h = json.optDouble("height", 0.0).toFloat()
            if (h > 0f) handler(h)
        }
    }
}

// ── DSL View ────────────────────────────────────────────────────────

/**
 * 映射到原生 macOS 侧 `KRMarkdownView`（类名由 `NSClassFromString` 自动发现）。
 * Android / iOS 等其他平台**未实现**，调用方应自行做 `expect/actual` 或平台兜底。
 */
internal class NativeMarkdownDSLView
    : DeclarativeBaseView<NativeMarkdownAttr, NativeMarkdownEvent>() {
    override fun createAttr() = NativeMarkdownAttr()
    override fun createEvent() = NativeMarkdownEvent()
    override fun viewName() = "KRMarkdownView"
}

// ── Compose 包装 ────────────────────────────────────────────────────

/**
 * 在 Compose 渲染树中嵌入原生 Markdown 组件。
 *
 * @param content       Markdown 原文，流式场景直接传最新的完整文本即可，
 *                      原生侧会合并串行 + 异步解析。
 * @param streaming     是否流式中（末尾追加 `▎` 光标）。
 * @param modifier      外层 Modifier（宽度由调用方决定）。
 * @param fontSize      正文字号，默认 14。
 * @param textColor     正文字色 ARGB，默认 `0xFF334155`。
 * @param codeMode      true 时把 content 当作纯代码（包装为 ```lang\ncode\n```），
 *                      使用代码块样式（等宽字体 + 浅灰底），消除代码流式卡顿。
 * @param codeLanguage  codeMode 下的语言标识（如 "kotlin"），仅作语义标记。
 * @param initialHeight Native 首次测量完成前的占位高度，避免 0 高度导致未渲染。
 */
@Composable
internal fun NativeMarkdownView(
    content: String,
    streaming: Boolean = false,
    modifier: Modifier = Modifier,
    fontSize: Float = 14f,
    textColor: Long = 0xFF334155L,
    codeMode: Boolean = false,
    codeLanguage: String = "",
    initialHeight: Dp = 200.dp,
) {
    // Native 回传的实际内容高度
    var measuredHeight by remember { mutableStateOf(initialHeight) }

    MakeKuiklyComposeNode<NativeMarkdownDSLView>(
        factory = { NativeMarkdownDSLView() },
        // 高度由 state 驱动；宽度由调用方的 modifier 决定
        modifier = modifier.height(measuredHeight),
        viewInit = {
            // 事件只注册一次：回调里更新 state → recompose → 高度更新
            getViewEvent().onContentSizeChanged { h ->
                val newHeight = h.dp
                if (newHeight != measuredHeight) {
                    measuredHeight = newHeight
                }
            }
        },
        viewUpdate = { view ->
            // 每次 recompose 都下发最新属性；属性值相同时框架会自动跳过
            view.getViewAttr()
                .fontSize(fontSize)
                .textColor(textColor)
                .mode(if (codeMode) "code" else "markdown")
                .codeLanguage(codeLanguage)
                .streaming(streaming)
                .content(content)
        }
    )
}

/**
 * 原生代码块组件：`NativeMarkdownView` 的 `codeMode = true` 快捷封装。
 *
 * 用于 CodeBlockCard / ToolCallCard 里需要流式输出代码的场景——原生侧用
 * cmark fenced code block 统一样式（等宽字体 + 浅灰底），彻底消除 Compose
 * 侧代码块流式追加的主线程卡顿。
 *
 * @param code      纯代码原文（**不需要**自己加 ```）
 * @param language  语言提示（可选，如 "kotlin" / "swift"）
 * @param streaming 流式中
 * @param modifier  外层 Modifier
 * @param fontSize  字号（代码块统一等宽字体，此参数只影响字号大小）
 */
@Composable
internal fun NativeCodeBlock(
    code: String,
    language: String = "",
    streaming: Boolean = false,
    modifier: Modifier = Modifier,
    fontSize: Float = 12f,
) {
    NativeMarkdownView(
        content = code,
        streaming = streaming,
        modifier = modifier,
        fontSize = fontSize,
        textColor = 0xFF24292FL,   // GitHub Light 代码文字色
        codeMode = true,
        codeLanguage = language,
    )
}
