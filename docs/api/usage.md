---
last_updated: 2026-11-09
status: active
owner: @kuikly-chat-team
---

# KuiklyChat API 使用指南

本文给出宿主从添加依赖到渲染两套 Chat UI 的最小完整链路。参数级说明见 [chat-ui.md](chat-ui.md)，公开面基线见 [public-keep-set.md](public-keep-set.md)。

## 1. 添加依赖

标准平台使用 `1.0.5-2.1.21`，鸿蒙使用 `1.0.5-2.0.21-KBA-010`。仓库地址与模块选择见 [host-integration.md](host-integration.md)。

```kotlin
repositories {
    google()
    mavenCentral()
    maven { url = uri("https://mirrors.tencent.com/repository/maven/kuikly-open/") }
    maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
}

dependencies {
    // Compose UI；kuiklychat-core 会随它传递。
    implementation("com.tencent.kuiklybase:kuiklychat-compose:1.0.5-2.1.21")

    // Kuikly DSL 会话页时改用或追加：
    // implementation("com.tencent.kuiklybase:kuiklychat-compat:1.0.5-2.1.21")

    // Preview / Mock 必须显式依赖：
    // implementation("com.tencent.kuiklybase:kuiklychat-testing:1.0.5-2.1.21")
}

// Compose 与 Kuikly DSL 同时接入时，Android 宿主排除重复的 Markdown parser。
configurations.configureEach {
    exclude(group = "org.jetbrains", module = "markdown")
}
```

## 2. 创建 Controller

`ChatController` 是两套 UI 共用的状态所有者。它在进程内工作，不读取网络凭据；真实请求由宿主的 `ChatMessageSender` 注入。

```kotlin
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatMessageSender
import com.tencent.kuiklybase.chat.compose.core.ChatStreamEvent
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flow

val scope = CoroutineScope(SupervisorJob())
val controller = ChatController(
    currentUser = ChatUser(id = "me", name = "我"),
    scope = scope,
    sender = ChatMessageSender { message ->
        flow {
            emit(ChatStreamEvent.TextDelta(text = "收到：${message.text}"))
            emit(ChatStreamEvent.Completed)
        }
    },
)
```

常用操作：`sendText()` / `sendMessage()` 追加消息，`dispatch(ChatIntent.StopGeneration)` 停止生成，`state` / `stateFlow` 观察状态，`effects` 处理滚动、错误与复制。页面销毁时调用 `controller.dispose()` 并取消宿主 `scope`。

## 3. 渲染 Compose UI

```kotlin
import com.tencent.kuiklybase.chat.compose.ui.ChatScreen

ChatScreen(
    controller = controller,
    config = ChatScreenConfig.Default.copy(
        showDateSeparators = true,
    ),
)
```

`ChatScreen` 的两个重载分别对应无用户解析和 `ChatUserResolutionConfig`；后者要求 `cacheNamespace` 非空。自定义整条消息渲染时传入 `rendererRegistry`，默认行为由 `defaultChatRendererRegistry()` 提供。

`ChatScreenConfig.Default` 的 `composerBottomInsetDp` 为 `0f`；宿主自行处理系统底部安全区时才需要传入额外 inset。

## 4. 渲染 Kuikly DSL 会话页

父容器必须有确定高度，例如 `flex(1f)`：

```kotlin
View {
    attr {
        flex(1f)
        flexDirection(FlexDirection.COLUMN)
    }
    ChatControllerSession(
        controller = controller,
    )
}
```

Demo 不向底部输入区传 `pagerData.safeAreaInsets.bottom`；`ChatControllerSession` 与 `ChatScreenConfig` 的默认值都是 `0f`，键盘跟随由组件内部处理。

`renderMessage` 返回 `true` 可跳过默认消息正文；传入 `ChatConversationHost` 后显示会话抽屉。输入框也可单独使用 `ChatDockComposer { ... }`，但通常由 `ChatControllerSession` 统一管理。

## 5. 富消息与附件

消息正文是有序 `MessagePart`，包含 `Text`、`Attachment`、`AgentCardContent`、`Custom` 与 `SystemNotice`。新增卡片数据应扩展共享 `AgentCard` / `MessagePart`，不要增加第二套消息模型。

```kotlin
val message = ChatMessage(
    id = "user-1",
    author = ChatUser(id = "me", name = "我"),
    parts = listOf(
        MessagePart.Text("帮我看看这张图"),
        MessagePart.Attachment(/* ... */),
    ),
    outgoing = true,
)
controller.upsertExternalMessage(message)
```

## 6. 多段对话

`ChatConversationStore` 由宿主实现或测试时用 `MemoryChatConversationStore`；`ChatConversationDirectory` 负责保存、切换和归档。切换后把返回的消息列表交给 `ChatController.showConversation()`，重新生成使用 `regenerateReply(messageId)`。

```kotlin
val directory = ChatConversationDirectory(MemoryChatConversationStore())
directory.reload()
val messages = directory.switchTo("conversation-id")
controller.showConversation(messages)
```

## 7. Preview / Mock

`:kuiklychat-testing` 不随 UI 模块传递。`ChatPreviewData.messages()` / `channels()` 提供静态数据，`MockChatRepository` 提供内存仓库；两者只用于 Demo、Preview 和测试，不应进入生产依赖链。

## 8. 验证

宿主接入后至少验证：空输入与并发发送被拒绝、流式增量落在同一条消息、停止生成后可重试、页面销毁后协程取消、附件和自定义 renderer 在两套 UI 中都能渲染。库侧改动使用 `bash scripts/ci-gates.sh` 跑 layer 与 Android 编译/单测。
