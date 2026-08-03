package com.kuikly.kuiklychat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.Image
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
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.ColorFilter
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuiklybase.chat.kuiklyai.provider.EchoLlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmChatPlatformRegistry
import com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatController
import com.tencent.kuiklybase.chat.kuiklyai.runtime.rememberLlmChatController
import com.tencent.kuiklybase.chat.kuiklyai.ui.KuiklyAiChatView
import com.tencent.kuiklybase.chat.kuiklyai.ui.LlmChatDesignSystem
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
        var designSystem by remember { mutableStateOf(LlmChatDesignSystem.MATERIAL) }
        val services = LlmChatPlatformRegistry.current
        val controller = rememberLlmChatController(
            provider = provider,
            attachmentPicker = services?.attachmentPicker,
            speechRecognizer = services?.speechRecognizer,
            clipboard = services?.clipboard
        )
        val style = LlmChatViewStyle.forDesignSystem(designSystem).copy(
            backgroundColor = if (designSystem == LlmChatDesignSystem.MATERIAL) {
                0xFFF6F8F7
            } else {
                0xFFF2F2F7
            },
            userBubbleGradientStart = if (designSystem == LlmChatDesignSystem.MATERIAL) {
                0xFF176B5B
            } else {
                0xFF007AFF
            },
            userBubbleGradientEnd = if (designSystem == LlmChatDesignSystem.MATERIAL) {
                0xFF125247
            } else {
                0xFF007AFF
            },
            sendButtonColor = if (designSystem == LlmChatDesignSystem.MATERIAL) {
                0xFF176B5B
            } else {
                0xFF007AFF
            },
            suggestionBackgroundColor = if (designSystem == LlmChatDesignSystem.MATERIAL) {
                0xFFEAF4F0
            } else {
                0xFFFFFFFF
            },
            suggestionBorderColor = if (designSystem == LlmChatDesignSystem.MATERIAL) {
                0xFFB9D8CD
            } else {
                0xFFD1D1D6
            },
            suggestionTextColor = if (designSystem == LlmChatDesignSystem.MATERIAL) {
                0xFF125247
            } else {
                0xFF007AFF
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(style.backgroundColor))
        ) {
            Spacer(Modifier.height(pagerData.statusBarHeight.dp))
            DemoNavigationBar(
                controller = controller,
                designSystem = designSystem,
                onDesignSystemChange = { designSystem = it },
                onBack = {
                    getPager()
                        .acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                        .closePage()
                }
            )
            KuiklyAiChatView(
                controller = controller,
                style = style,
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
    designSystem: LlmChatDesignSystem,
    onDesignSystemChange: (LlmChatDesignSystem) -> Unit,
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
            ChevronLeftIcon()
        }
        Text(
            text = "KuiklyAI",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF17211E),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(16.dp))
    }
}

@OptIn(InternalResourceApi::class)
@Composable
private fun ChevronLeftIcon(
    size: Dp = 24.dp,
    color: Color = Color(0xFF26332F),
) {
    val drawable = remember {
        DrawableResource(ImageUri.commonAssets("kuikly_chat_ic_chevron_left.svg").toUrl(""))
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = "chevron-left",
        modifier = Modifier.size(size),
        colorFilter = ColorFilter.tint(color),
    )
}

private val DemoSuggestions = listOf(
    "帮我整理今天的待办事项",
    "用 Markdown 写一份会议纪要",
    "解释 Kotlin Flow 的背压处理"
)
