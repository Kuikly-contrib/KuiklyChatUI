package com.tencent.kuiklybase.chat.kuiklyai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmChatPlatformServices
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmClipboard
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmSpeechRecognizer
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmChatMessage
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmProvider
import com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatController
import com.tencent.kuiklybase.chat.kuiklyai.runtime.LlmChatUiEvent
import com.tencent.kuiklybase.chat.kuiklyai.runtime.rememberLlmChatController

/**
 * KuiklyAI 的主入口
 *
 * 传入一个 [LlmProvider] 就能得到完整的 AI 对话界面：消息历史、流式输出、
 * 富文本渲染、输入区、错误重试、欢迎语与建议提示。后端协议细节全在
 * provider 内部，这一层完全不感知。
 *
 * ## 最小用法
 *
 * ```kotlin
 * @Composable
 * fun MyAiPage() {
 *     // 换成 OpenAiCompatibleProvider / AgUiLlmProvider 即可接真实后端，
 *     // 这里的 UI 代码一行都不用改
 *     val provider = remember { EchoLlmProvider() }
 *     KuiklyAiChatView(provider = provider)
 * }
 * ```
 *
 * ## 带附件与语音
 *
 * 平台能力由宿主注入；没传就不显示对应按钮，不会出现点了没反应的入口。
 *
 * ```kotlin
 * KuiklyAiChatView(
 *     provider = provider,
 *     attachmentPicker = MyAndroidAttachmentPicker(activity),
 *     speechRecognizer = MySpeechRecognizer(),
 *     suggestions = listOf("帮我总结这份文档", "这段代码有什么问题？"),
 * )
 * ```
 *
 * @param provider 后端实现，唯一必填参数
 * @param style 视觉配置
 * @param strings 文案，宿主做多语言时替换
 * @param suggestions 空会话时展示的建议提示，点击即发送
 * @param safeAreaBottom 底部安全区高度，传 `pagerData.safeAreaInsets.bottom`
 *
 * 键盘适配：输入框通过 `keyboardHeightChange` 上报键盘高度，页面根布局用 bottom padding
 * 预留同等空间。Android 宿主需要使用 `adjustNothing`，避免系统缩放与组件的手动避让叠加。
 * @param attachmentPicker 附件选择实现；为 null 时不显示附件按钮
 * @param speechRecognizer 语音识别实现；为 null 时不显示麦克风按钮
 * @param platformServices 平台能力聚合参数；与旧的单项参数同时传入时聚合参数优先
 * @param onEditMessage 用户消息点击回调；配合
 *        [LlmChatController.editAndResend] 实现编辑重发
 * @param messageBuilder 自定义消息渲染，返回 true 表示已接管该条消息
 */
@Composable
fun KuiklyAiChatView(
    provider: LlmProvider,
    style: LlmChatViewStyle = LlmChatViewStyle.Default,
    strings: LlmChatViewStrings = LlmChatViewStrings.Default,
    suggestions: List<String> = emptyList(),
    safeAreaBottom: Float = 0f,
    attachmentPicker: LlmAttachmentPicker? = null,
    speechRecognizer: LlmSpeechRecognizer? = null,
    onEditMessage: ((LlmChatMessage) -> Unit)? = null,
    messageBuilder: (@Composable (LlmChatMessage) -> Boolean)? = null,
    modifier: Modifier = Modifier,
    clipboard: LlmClipboard? = null,
    designSystem: LlmChatDesignSystem = style.designSystem,
    platformServices: LlmChatPlatformServices = LlmChatPlatformServices()
) {
    val resolvedServices = LlmChatPlatformServices(
        clipboard = platformServices.clipboard ?: clipboard,
        attachmentPicker = platformServices.attachmentPicker ?: attachmentPicker,
        speechRecognizer = platformServices.speechRecognizer ?: speechRecognizer
    )
    val controller = rememberLlmChatController(
        provider = provider,
        attachmentPicker = resolvedServices.attachmentPicker,
        speechRecognizer = resolvedServices.speechRecognizer,
        clipboard = resolvedServices.clipboard
    )

    KuiklyAiChatView(
        controller = controller,
        style = style,
        strings = strings,
        suggestions = suggestions,
        safeAreaBottom = safeAreaBottom,
        onEditMessage = onEditMessage,
        messageBuilder = messageBuilder,
        modifier = modifier,
        designSystem = designSystem
    )
}

/**
 * 接受外部 [LlmChatController] 的重载。
 *
 * 当宿主需要在 Composable 之外操作会话时用这个——比如导航栏的"清空对话"
 * 按钮、从外部注入一条消息、或把 controller 提到 ViewModel 里跨页面复用。
 * 用 [rememberLlmChatController] 自行创建后传进来即可。
 */
@Composable
fun KuiklyAiChatView(
    controller: LlmChatController,
    style: LlmChatViewStyle = LlmChatViewStyle.Default,
    strings: LlmChatViewStrings = LlmChatViewStrings.Default,
    suggestions: List<String> = emptyList(),
    safeAreaBottom: Float = 0f,
    onEditMessage: ((LlmChatMessage) -> Unit)? = null,
    messageBuilder: (@Composable (LlmChatMessage) -> Boolean)? = null,
    modifier: Modifier = Modifier,
    designSystem: LlmChatDesignSystem = style.designSystem
) {
    val effectiveStyle = if (style.designSystem == designSystem) {
        style
    } else {
        style.copy(designSystem = designSystem)
    }
    var previewedImage by remember { mutableStateOf<LlmAttachment?>(null) }
    var keyboardHeight by remember { mutableStateOf(0f) }
    // Kuikly 的 keyboardHeightChange 返回的是布局坐标单位，直接按 dp 使用。
    // 真机实测 258 对应约 258dp 的键盘高度，不能再除以 Android density。
    val keyboardHeightDp = keyboardHeight.dp
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(effectiveStyle.backgroundColor))
            .padding(bottom = keyboardHeightDp)
    ) {
        LlmMessageList(
            controller = controller,
            style = effectiveStyle,
            strings = strings,
            suggestions = suggestions,
            onEditMessage = onEditMessage,
            onPreviewImage = { attachment ->
                previewedImage = attachment
            },
            messageBuilder = messageBuilder,
            modifier = Modifier.weight(1f)
        )

        // 会话级错误横幅（区别于挂在单条消息上的失败态）：
        // 附件选择失败、语音不可用这类错误不属于任何一条消息。
        controller.lastError?.let { error ->
            LlmErrorBanner(
                message = error.message,
                retryable = error.retryable,
                style = effectiveStyle,
                strings = strings,
                onRetry = { controller.retryLast() },
                onDismiss = { controller.dismissError() }
            )
        }

        controller.transientEvent?.let { event ->
            LlmTransientEventBanner(
                event = event,
                style = effectiveStyle,
                onDismiss = controller::consumeTransientEvent
            )
        }

        LlmChatInput(
            controller = controller,
            style = effectiveStyle,
            strings = strings,
            safeAreaBottom = safeAreaBottom,
            onPreviewImage = { previewedImage = it },
            onKeyboardHeightChange = { keyboardHeight = it }
        )
    }
    previewedImage?.let { attachment ->
        LlmImagePreviewDialog(
            attachment = attachment,
            onDismiss = { previewedImage = null }
        )
    }
}

/**
 * 会话级错误横幅。
 */
@Composable
private fun LlmErrorBanner(
    message: String,
    retryable: Boolean,
    style: LlmChatViewStyle,
    strings: LlmChatViewStrings,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(style.errorBackgroundColor))
            .padding(horizontal = style.contentPaddingH.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            fontSize = 12.sp,
            color = Color(style.errorColor),
            modifier = Modifier.weight(1f)
        )
        if (retryable) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = strings.retryLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(style.errorColor),
                modifier = Modifier.clickable {
                    onDismiss()
                    onRetry()
                }
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "\u00D7",
            fontSize = 15.sp,
            color = Color(style.errorColor),
            modifier = Modifier.clickable { onDismiss() }
        )
    }
}

@Composable
private fun LlmTransientEventBanner(
    event: LlmChatUiEvent,
    style: LlmChatViewStyle,
    onDismiss: () -> Unit
) {
    val text = when (event) {
        is LlmChatUiEvent.TextCopied -> "已复制到剪贴板"
        is LlmChatUiEvent.AttachmentRejected -> event.message
        is LlmChatUiEvent.PlatformOperationFailed -> event.message
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(style.suggestionBackgroundColor))
            .padding(horizontal = style.contentPaddingH.dp, vertical = 8.dp)
            .clickable { onDismiss() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(style.suggestionTextColor),
            modifier = Modifier.weight(1f)
        )
        Text(text = "×", fontSize = 15.sp, color = Color(style.mutedColor))
    }
}
