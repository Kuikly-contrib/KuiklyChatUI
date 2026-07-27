package com.kuikly.kuiklychat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuiklybase.chat.kuiklyai.provider.EchoLlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatController
import com.tencent.kuiklybase.chat.kuiklyai.runtime.rememberLlmChatController
import com.tencent.kuiklybase.chat.kuiklyai.ui.KuiklyAiChatView
import com.tencent.kuiklybase.chat.kuiklyai.ui.LlmChatViewStyle

/**
 * KuiklyAI 零后端 Demo。
 *
 * 页面只创建一个 [EchoLlmProvider] 并传给公共 Compose DSL 入口，替换真实
 * provider 时页面 UI 无需改动。
 */
@Page("kuiklyai_demo", supportInLocal = true)
internal class KuiklyAiDemoPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent { DemoContent() }
    }

    @Composable
    private fun DemoContent() {
        val provider = remember {
            EchoLlmProvider(
                chunkSize = 3,
                delayMillis = 36L
            )
        }
        val controller = rememberLlmChatController(provider)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(DemoStyle.backgroundColor))
        ) {
            Spacer(Modifier.height(pagerData.statusBarHeight.dp))
            DemoNavigationBar(
                controller = controller,
                onBack = {
                    getPager()
                        .acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                        .closePage()
                }
            )
            KuiklyAiChatView(
                controller = controller,
                style = DemoStyle,
                suggestions = DemoSuggestions,
                safeAreaBottom = pagerData.safeAreaInsets.bottom,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DemoNavigationBar(
    controller: LlmChatController,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\u2190", fontSize = 21.sp, color = Color(0xFF26332F))
        }
        Text(
            text = "KuiklyAI",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF17211E),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "清空",
            fontSize = 13.sp,
            color = Color(0xFF176B5B),
            modifier = Modifier.clickable { controller.clear() }
        )
        Spacer(Modifier.width(16.dp))
    }
}

private val DemoSuggestions = listOf(
    "帮我整理今天的待办事项",
    "用 Markdown 写一份会议纪要",
    "解释 Kotlin Flow 的背压处理"
)

private val DemoStyle = LlmChatViewStyle.Default.copy(
    backgroundColor = 0xFFF6F8F7,
    userBubbleGradientStart = 0xFF176B5B,
    userBubbleGradientEnd = 0xFF125247,
    sendButtonColor = 0xFF176B5B,
    suggestionBackgroundColor = 0xFFEAF4F0,
    suggestionBorderColor = 0xFFB9D8CD,
    suggestionTextColor = 0xFF125247
)
