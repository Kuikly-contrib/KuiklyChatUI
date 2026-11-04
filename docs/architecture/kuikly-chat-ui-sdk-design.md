---
last_updated: 2026-08-03
status: historical
owner: @kuikly-chat-team
---

# Flutter Chat UI 到 Kuikly Chat UI SDK 设计文档

> 历史架构设计：本文保留 Flutter 抽象和传统 Kuikly DSL 方案，当前首发方向已切换为 Compose DSL。现状、实现路径和阶段进度以 `docs/design/feature-kuikly-chat-ui-sdk-redesign.md` 为准；本文不再作为当前实现接口的唯一依据。

## 1. 文档目标

本文以当前仓库中的 Flyer Chat SDK 为参考，说明其架构、数据流、扩展点和关键实现机制，并给出一个适合 Kuikly/Kotlin Multiplatform 的聊天 UI SDK 设计方案。

目标不是把 Flutter 的类名逐一翻译成 Kotlin，而是保留它的核心思想：

- 核心数据模型与 UI 解耦
- 消息状态由 Controller/Store 统一管理
- UI 只负责渲染和产生用户意图
- 消息类型通过可扩展 Renderer 处理
- 网络、本地存储、上传和 AI 流式输出放在 SDK 外部适配
- 列表、滚动、分页、动画是独立能力，不侵入业务请求层

本文中的 Kuikly 代码为传统 Kuikly DSL 的设计示意，具体 import 和个别 API 名称应以当前工程使用的 Kuikly 版本为准。

## 2. 参考项目的代码分层

当前仓库不是单一 App，而是一个 Dart Workspace monorepo。

```text
flutter_chat_ui/
├── packages/
│   ├── flutter_chat_core/
│   │   └── 消息模型、用户模型、ChatController、主题、工具类型
│   ├── flutter_chat_ui/
│   │   └── Chat、Composer、消息列表、消息外层布局、滚动和分页
│   ├── flyer_chat_text_message/
│   ├── flyer_chat_image_message/
│   ├── flyer_chat_file_message/
│   ├── flyer_chat_system_message/
│   └── flyer_chat_text_stream_message/
│       └── 各消息类型的默认渲染器
└── examples/flyer_chat/
    └── basic、local、api、pagination、gemini 示例
```

### 2.1 `flutter_chat_core`

核心源码位于：

- `packages/flutter_chat_core/lib/src/models/message.dart`
- `packages/flutter_chat_core/lib/src/models/user.dart`
- `packages/flutter_chat_core/lib/src/chat_controller/chat_controller.dart`
- `packages/flutter_chat_core/lib/src/chat_controller/chat_operation.dart`
- `packages/flutter_chat_core/lib/src/chat_controller/in_memory_chat_controller.dart`
- `packages/flutter_chat_core/lib/src/models/builders.dart`
- `packages/flutter_chat_core/lib/src/theme/chat_theme.dart`

该层不负责绘制具体的聊天页面，主要负责：

- 消息和用户的数据契约
- 消息生命周期状态
- 消息列表增删改替换
- UI 更新所需的操作描述
- 主题和消息 Builder 的类型定义

### 2.2 `flutter_chat_ui`

核心源码位于：

- `packages/flutter_chat_ui/lib/src/chat.dart`
- `packages/flutter_chat_ui/lib/src/chat_animated_list/chat_animated_list.dart`
- `packages/flutter_chat_ui/lib/src/composer.dart`
- `packages/flutter_chat_ui/lib/src/chat_message/chat_message.dart`
- `packages/flutter_chat_ui/lib/src/chat_message/chat_message_internal.dart`
- `packages/flutter_chat_ui/lib/src/utils/message_list_diff.dart`

`Chat` 是页面编排器，负责把以下对象组合起来：

```text
Chat
├── ChatAnimatedList
│   ├── 空列表状态
│   ├── 消息插入/删除动画
│   ├── 滚动到最新消息
│   ├── 上拉/下拉分页
│   ├── 键盘和安全区处理
│   └── 消息分组
└── Composer
    ├── 文本输入
    ├── 附件按钮
    ├── 发送按钮
    └── 输入状态和键盘处理
```

### 2.3 `examples/flyer_chat`

示例不是 SDK 内部实现，而是 SDK 的组装方式：

- `basic.dart`：内存控制器 + 文本消息 + `Chat`
- `local.dart`：图片、文件、系统消息、自定义消息、链接预览和自定义 Builder
- `pagination_older.dart` / `pagination_newer.dart`：分页与滚动锚点
- `api/`：REST/WebSocket 接入示例
- `gemini.dart`：流式 AI 消息示例

因此业务应用应该参考 `example` 的组装方式，而不是把网络请求或数据库逻辑塞进 `Chat` 组件。

## 3. Flutter SDK 的核心架构

## 3.1 数据模型：不可变的消息联合类型

Flutter 使用 Freezed sealed union 表示消息类型。消息共享一组基础字段，每种消息再增加自己的业务字段。

当前支持的核心类型包括：

- `TextMessage`
- `TextStreamMessage`
- `ImageMessage`
- `FileMessage`
- `VideoMessage`
- `AudioMessage`
- `SystemMessage`
- `CustomMessage`
- `UnsupportedMessage`

共享字段包括：

```text
id
 authorId
 replyToMessageId
 createdAt
 deletedAt
 failedAt
 sentAt
 deliveredAt
 seenAt
 updatedAt
 editedAt
 reactions
 pinned
 metadata
 status
```

这种设计有三个重要作用：

1. UI 可以只依赖 `Message` 基类完成列表和分组。
2. 不同消息 Renderer 可以依赖具体子类型完成渲染。
3. JSON 使用 `type` 字段作为 discriminator，便于网络传输和持久化。

消息状态不是简单的布尔值，而是一个生命周期：

```text
created -> sending -> sent -> delivered -> seen
                     └-> error
```

`resolvedStatus` 会根据显式状态和时间字段推导最终状态。Kuikly 版本也应保留显式状态优先、时间字段兜底的规则。

## 3.2 `ChatController`：消息状态的唯一入口

Flutter 的 `ChatController` 只定义消息状态操作，不关心消息来自哪里：

```text
insertMessage(message, index?)
insertAllMessages(messages, index?)
updateMessage(oldMessage, newMessage)
removeMessage(message)
setMessages(messages)
messages
operationsStream
dispose()
```

`InMemoryChatController` 是默认实现，特点是：

- 内存保存消息
- 强制消息 ID 唯一
- 每次变更产生一个 `ChatOperation`
- 支持批量插入
- 支持更新同一个消息，例如链接预览、上传状态和流式文本
- 混入上传进度和滚动到消息能力

这说明 Controller 的职责是“维护消息状态和发出变更”，不是“执行 HTTP 请求”。

## 3.3 `ChatOperation`：把状态变更描述成可消费事件

`ChatOperation` 类型包括：

```text
insert
insertAll
update
remove
set
```

每个 operation 还携带：

- affected message
- old message
- index
- messages
- animated

`ChatAnimatedList` 订阅 operation stream 后，可以把业务状态变更转换为列表动画，而不是每次都重建全部列表。

Kuikly 不应机械复制 Dart Stream。Kuikly 里推荐采用两层模型：

```text
ChatStore.messages: observableList<ChatMessage>
    └── 驱动 List + vfor 的声明式更新

ChatMutation / ChatUiEvent
    └── 驱动滚动锚点、插入动画、上传进度和业务埋点
```

也就是说，`observableList` 是 UI 的事实状态，mutation 是附加语义，不要让两个列表同时成为真相来源。

## 3.4 `Chat`：页面级编排器

`Chat` 接收以下关键依赖：

```text
currentUserId
resolveUser
chatController
builders
theme
onMessageSend
onMessageTap
onMessageLongPress
onAttachmentTap
background / decoration
```

它内部把依赖注入到消息列表、消息包装器、Composer 和消息 Renderer。

值得保留的设计原则：

- `Chat` 是组合根，不是网络层
- `resolveUser` 负责 UserID -> User，UI 不直接访问用户数据库
- `builders` 允许替换局部 UI，而不需要 fork 整个 Chat
- `theme` 统一控制颜色、字体和气泡形状
- 用户行为通过 callback 输出，业务决定如何处理

Kuikly 版本建议使用显式构造参数或 `ChatContext` 注入这些依赖，不要依赖全局单例。

## 3.5 消息 Renderer 和 Builder 扩展点

Flutter 的 `Builders` 为每种消息和主要 UI 节点提供 Builder：

```text
textMessageBuilder
textStreamMessageBuilder
imageMessageBuilder
fileMessageBuilder
videoMessageBuilder
audioMessageBuilder
systemMessageBuilder
customMessageBuilder
unsupportedMessageBuilder
composerBuilder
chatMessageBuilder
chatAnimatedListBuilder
scrollToBottomBuilder
loadMoreBuilder
emptyChatListBuilder
linkPreviewBuilder
```

这里有两种扩展层级：

### 消息内容 Renderer

只负责气泡内部内容，例如文本、图片和文件。

### 消息外层 Renderer

负责头像、用户名、左右对齐、消息分组、状态和动画。

这两个层级必须分开。否则业务为了改一个图片气泡，就需要复制整行消息布局。

## 3.6 消息分组

SDK 根据相邻消息的作者和时间计算：

```text
isFirst
isMiddle
isLast
```

分组结果影响：

- 是否显示头像
- 是否显示用户名
- 气泡圆角
- 消息间距
- 连续消息的视觉密度

Kuikly 版本建议把分组计算放在纯 Kotlin Core 层：

```kotlin
fun groupStatus(
    messages: List<ChatMessage>,
    index: Int,
    mode: GroupingMode,
    timeoutSeconds: Int
): MessageGroupStatus
```

UI 只消费计算结果，不在 `body()` 中重复编写分组规则。

## 3.7 列表、动画、分页和滚动

Flutter 的 `ChatAnimatedList` 解决了聊天 UI 最复杂的部分：

- 消息动态插入和移除
- 新消息自动滚到底部
- 用户正在阅读历史时不打断当前位置
- 上拉加载更早消息
- 下拉加载更新消息
- 内容高度变化后的滚动修正
- 键盘弹出后的安全区处理
- 滚动到指定 message ID

Kuikly 对应能力应建立在 `List` 而不是普通 `Scroller` 上：

- `List` 继承 `Scroller`
- 支持 `vfor` 和响应式列表
- 支持 `scrollToPosition`
- 支持 `getFirstVisiblePosition`
- 支持 `scroll`、`scrollEnd`、`contentSizeChanged`
- 支持预加载距离和首屏最大加载条数

`Scroller` 适合不确定高度的手工内容容器；聊天消息是大量重复 item，应优先使用 `List`。

## 3.8 Composer

Flutter `Composer` 负责：

- 输入框
- 附件按钮
- 发送按钮
- 空消息校验
- 发送按钮显示/禁用/隐藏
- 最大行数和字符数
- Enter/Shift+Enter 行为
- 键盘和安全区
- 发送后清空策略

Kuikly 的选择建议：

| 场景 | 组件 |
|---|---|
| 单行发送框 | `Input` |
| 多行聊天输入框 | `TextArea` |
| 表情、短码、图片附件插入 | `TextArea` + `textInputState` |
| 发送操作 | `Button` 的 `click` 事件或 `TextArea` 的回车相关事件 |
| 输入变化 | `textDidChange` 或 `textInputStateChange` |

`Input` 是单行输入框；聊天产品通常推荐 `TextArea`，因为它支持多行、滚动和 `lineHeight`。如果需要插入表情，使用 `textInputState` 可以保留 raw text、selection 和 composition 状态。

## 3.9 主题

Flutter `ChatTheme` 包含：

- `ChatColors`
- `ChatTypography`
- 气泡 `shape`

并支持 light、dark、从宿主主题转换以及 merge。

Kuikly 不应把颜色散落在消息 Renderer 中，建议设计：

```kotlin
data class ChatTheme(
    val colors: ChatColors,
    val typography: ChatTypography,
    val bubbleRadius: Float,
    val spacing: ChatSpacing
)
```

建议主题至少覆盖：

```text
surface
surfaceContainer
primary
onPrimary
onSurface
incomingBubble
outgoingBubble
systemMessage
error
placeholder
composerBackground
separator
```

## 4. Flutter 到 Kuikly 的概念映射

| Flutter Chat UI | Kuikly 建议实现 |
|---|---|
| Widget | `View`、`Text`、`Image`、`Button`、`List`、`TextArea` |
| StatefulWidget | `Pager`/组件对象 + `observable` 字段 |
| `List<Message>` 状态 | `observableList<ChatMessage>()` |
| `setState` | 给响应式字段重新赋值 |
| `Stream<ChatOperation>` | `observableList` + 可选 `ChatMutation` 回调 |
| `SliverAnimatedList` | `List` + `vfor` + `diffUpdate` + `animate` |
| `ScrollController` | `List.scrollToPosition`、`setContentOffset`、`getFirstVisiblePosition` |
| `TextEditingController` | `TextAreaView`/`InputView` ref + `textInputState` |
| `ImageProvider` / cache | `rememberAsyncImagePainter` + 根级 `ChatCrossCache`；Android/Apple 文件缓存、Web IndexedDB |
| Freezed union | Kotlin `sealed interface` + `data class` |
| JSON serialization | KMP serializer 或明确的 `decode/encode` codec |
| Provider | 显式 `ChatContext`、Pager 作用域字段或构造注入 |
| callback builder | Renderer registry / 显式消息分支 / 组件参数 |
| Dio / WebSocket | `NetworkModule` 或平台 Module/KMP adapter |
| Flutter plugin | Kuikly Module 或 `expect/actual` |
| Material Theme | `ChatTheme` 数据对象 |
| Future/async | Kuikly 内建协程、NetworkModule 回调或 `kuiklyx.coroutines` |

## 5. Kuikly SDK 推荐分层

建议不要只创建一个巨大模块，而是拆成以下包：

```text
kuikly-chat-core
├── ChatUser
├── ChatMessage sealed hierarchy
├── MessageStatus
├── ChatStore contract
├── pagination contract
├── grouping algorithm
├── message codec
└── pure utility functions

kuikly-chat-ui
├── Chat
├── ChatList
├── ChatMessageItem
├── ChatComposer
├── ChatTheme
├── ChatContext
├── empty/loading/error states
└── scroll and keyboard behavior

kuikly-chat-message-text
kuikly-chat-message-image
kuikly-chat-message-file
kuikly-chat-message-system
kuikly-chat-message-stream
├── 默认消息 Renderer
└── 不依赖具体业务 API

kuikly-chat-adapter
├── REST adapter
├── WebSocket adapter
├── upload adapter
├── media picker adapter
└── local persistence adapter

kuikly-chat-demo
└── basic/local/pagination/api/streaming 示例
```

依赖方向必须保持单向：

```text
chat-ui -> chat-core
message packages -> chat-core
adapters -> chat-core
host app -> ui + adapters + message packages
```

`chat-core` 不能依赖 UI；`chat-ui` 不能直接依赖业务后端。

## 6. Kuikly Core 数据模型设计

推荐使用公共元数据 + 类型专属字段的方式，避免每个消息类重复几十个基础字段。

```kotlin
data class MessageMeta(
    val id: String,
    val authorId: String,
    val createdAtMillis: Long? = null,
    val updatedAtMillis: Long? = null,
    val sentAtMillis: Long? = null,
    val deliveredAtMillis: Long? = null,
    val seenAtMillis: Long? = null,
    val failedAtMillis: Long? = null,
    val replyToMessageId: String? = null,
    val status: MessageStatus? = null,
    val pinned: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
)

sealed interface ChatMessage {
    val meta: MessageMeta
    val kind: String
}

data class TextMessage(
    override val meta: MessageMeta,
    val text: String,
    val linkPreview: LinkPreviewData? = null
) : ChatMessage {
    override val kind: String = "text"
}

data class ImageMessage(
    override val meta: MessageMeta,
    val source: String,
    val width: Float? = null,
    val height: Float? = null,
    val caption: String? = null,
    val thumbnail: String? = null
) : ChatMessage {
    override val kind: String = "image"
}

data class FileMessage(
    override val meta: MessageMeta,
    val source: String,
    val name: String,
    val size: Long? = null,
    val mimeType: String? = null
) : ChatMessage {
    override val kind: String = "file"
}

data class SystemMessage(
    override val meta: MessageMeta,
    val text: String
) : ChatMessage {
    override val kind: String = "system"
}

data class CustomMessage(
    override val meta: MessageMeta,
    override val kind: String,
    val payload: Map<String, String>
) : ChatMessage
```

消息设计要求：

1. `meta.id` 全局唯一，禁止复用。
2. 更新消息时创建新对象，不直接修改旧对象内部字段。
3. `kind` 是稳定协议字段，不能使用 Kotlin 类名作为网络协议。
4. 未识别的 `kind` 必须降级为 Unsupported，而不是让整页解析失败。
5. 自定义 payload 应有版本字段，例如 `payloadVersion`。
6. 时间统一使用 UTC epoch millis，显示时再本地化。

## 7. Kuikly Store 设计

### 7.1 Store 接口

```kotlin
interface ChatStore {
    val messages: List<ChatMessage>

    fun insert(message: ChatMessage, index: Int? = null)
    fun insertAll(messages: List<ChatMessage>, index: Int? = null)
    fun update(message: ChatMessage)
    fun remove(messageId: String)
    fun replaceAll(messages: List<ChatMessage>)
    fun find(messageId: String): ChatMessage?
}
```

实际 Kuikly UI 层可以使用 `observableList` 作为实现：

```kotlin
private var messageList by observableList<ChatMessage>()
private var isLoadingOlder by observable(false)
private var hasMoreOlder by observable(true)
private var isTyping by observable(false)
```

消息列表更新建议：

- 首次加载：`replaceAll` 或一次 `addAll`
- 新消息：追加到末尾
- 历史消息：插入到索引 `0`
- 同一消息变化：按 ID 找到位置后替换
- 大量同步：使用 `diffUpdate`，不要 `clear()` 后全部重建

Kuikly 官方响应式文档明确建议 `observableList` 配合 `List` 和 `vfor`，并提供 `diffUpdate` 来避免无意义地销毁和重建所有 item。

### 7.2 为什么不把网络请求放进 Store

网络层可能是 REST、WebSocket、IM SDK 或 AI 流式接口。Store 只应接收统一的 domain message，避免：

```text
Chat UI -> HTTP/WebSocket -> Chat UI
```

推荐：

```text
Network adapter -> ChatSession/ViewModel -> ChatStore -> Chat UI
```

这样可以：

- 用本地 mock 驱动 UI
- 替换后端协议
- 单元测试消息状态
- 让 WebSocket 和 AI stream 共用同一个 UI

## 8. Kuikly Chat UI 组件设计

### 8.1 `Chat`

负责页面级组合和公共配置：

```text
Chat
├── ChatList
├── Composer
├── empty/loading/error overlay
└── optional scroll-to-bottom button
```

建议参数：

```kotlin
data class ChatConfig(
    val currentUserId: String,
    val theme: ChatTheme,
    val groupingMode: GroupingMode = GroupingMode.TIME_DIFFERENCE,
    val groupingTimeoutSeconds: Int = 60,
    val autoScrollWhenAtBottom: Boolean = true,
    val showScrollToBottom: Boolean = true
)
```

事件建议：

```text
onMessageSend(text)
onMessageTap(message)
onMessageLongPress(message)
onAttachmentTap()
onRetryMessage(message)
onLoadOlder()
onLoadNewer()
```

### 8.2 `ChatList`

应使用 `List`：

```kotlin
List {
    attr {
        flex(1f)
        showScrollerIndicator(false)
        firstContentLoadMaxIndex(30)
        preloadViewDistance(800f)
    }

    vfor({ ctx.messageList }) { message ->
        ctx.renderMessage(message)
    }
}
```

真实实现还需要处理：

- 空状态
- 首次加载状态
- 历史加载状态
- 加载失败和重试
- 当前是否接近底部
- 首次进入时滚到最后一条
- 收到新消息时是否自动滚动
- 上拉分页后的滚动锚点

### 8.3 `ChatMessageItem`

消息 Item 分为两层：

```text
ChatMessageItem
├── group/header layer
│   ├── username
│   ├── avatar
│   └── time
└── content renderer
    ├── text
    ├── image
    ├── file
    ├── system
    ├── stream
    └── custom
```

`ChatMessageItem` 负责：

- 判断是否是当前用户
- 计算 `MessageGroupStatus`
- 左右布局
- 头像和用户名的显示规则
- 状态图标和重试入口
- 统一点击和长按事件

具体 Renderer 只负责内容本身。

### 8.4 Renderer 注册表

推荐提供默认 Renderer，同时允许业务按 `kind` 替换：

```kotlin
interface MessageRenderer {
    fun supports(message: ChatMessage): Boolean
    fun render(message: ChatMessage, context: MessageRenderContext): ViewBuilder
}

class MessageRendererRegistry(
    private val renderers: List<MessageRenderer>
) {
    fun rendererFor(message: ChatMessage): MessageRenderer {
        return renderers.firstOrNull { it.supports(message) }
            ?: UnsupportedMessageRenderer
    }
}
```

生产实现中，Renderer 的注册优先使用稳定的 `kind`，不要只按 Kotlin 类型判断，因为动态化、JSON 反序列化和跨模块时都需要稳定协议。

如果动态化场景不适合跨模块传递 Kotlin 函数 Builder，可以提供两种模式：

- SDK 内置 Renderer + 宿主通过 `kind` 配置样式
- 宿主继承/组合 `ChatMessageItem`，在 common DSL 中显式处理自定义类型

### 8.5 `Composer`

推荐以 `TextArea` 为主：

```kotlin
TextArea {
    ref { ctx.inputRef = it }
    attr {
        flex(1f)
        placeholder("输入消息")
        lineHeight(22f)
        textInputState(ctx.inputState)
    }
    event {
        textInputStateChange { state ->
            ctx.inputState = state
            ctx.canSend = state.text.trim().isNotEmpty()
        }
    }
}

Button {
    attr {
        titleAttr { text("发送") }
        enabled(ctx.canSend)
    }
    event {
        click { ctx.sendCurrentText() }
    }
}
```

实现时要注意：

- 不要只保存一个普通 `String`，否则 selection 和 composition 容易丢失。
- 发送前 trim 只用于校验，是否保留原始空格要由业务配置决定。
- 发送成功后调用 `setTextInputState` 清空输入框。
- 发送中应锁定重复发送，或通过 client message ID 做幂等。
- 键盘高度变化不要直接修改列表数据，应只影响 Composer 和底部 inset。

## 9. 滚动与分页设计

## 9.1 消息顺序

推荐在 Store 中统一使用：

```text
旧消息 -> 新消息
index 0 -> index n
```

UI 可以通过滚动到最后一个 item 呈现“最新消息在底部”的聊天体验。不要让 Store 在不同页面使用两种顺序。

## 9.2 首次进入

```text
1. 请求第一页历史消息
2. 写入 messageList
3. 等待列表完成布局
4. scrollToPosition(lastIndex, animate = false)
```

如果历史消息为空，显示 empty state，不要强行滚动。

## 9.3 新消息策略

计算：

```text
isNearBottom = offsetY + viewHeight >= contentHeight - threshold
```

- 用户在底部：插入消息后自动滚到底部
- 用户正在看历史：插入消息但不改变当前位置，显示“有新消息”按钮
- 当前用户发送：通常强制滚到底部
- AI 流式更新：仅在当前消息接近底部时跟随滚动

## 9.4 加载更早消息

```text
1. 防抖并检查 loadingOlder / hasMoreOlder
2. 记录 firstVisiblePosition 和 offset
3. 请求更早消息
4. 在 index 0 插入
5. 计算原锚点消息的新索引
6. layout 后 scrollToPosition(新索引, 原 offset)
```

如果无法保证稳定锚点，插入历史消息后会发生明显跳动，这是聊天列表最常见的体验问题之一。

## 9.5 Kuikly API 对应关系

`List`/`Scroller` 文档提供：

- `scrollToPosition(index, offset, animate)`
- `getFirstVisiblePosition`
- `setContentOffset`
- `scroll`
- `scrollEnd`
- `contentSizeChanged`
- `preloadViewDistance`
- `firstContentLoadMaxIndex`

因此 Kuikly 第一版可以用 `List` 的位置和偏移实现滚动策略，不需要一开始就编写原生滚动组件。

## 10. 流式 AI 消息设计

Flutter 参考项目使用独立的 `TextStreamMessage`。Kuikly 也建议把流式消息视为“同一个 message ID 的多次更新”，而不是每个 token 创建一条消息：

```text
开始：TextStreamMessage(id = "stream-1", text = "")
片段：update(id = "stream-1", text = "你好")
片段：update(id = "stream-1", text = "你好，我是...")
完成：replace 为 TextMessage 或保留 stream 类型并设置 sent
失败：status = error
```

关键要求：

- stream ID 稳定
- 更新按 ID 替换
- UI 更新节流，避免每个 token 都触发高频重排
- 页面退出时取消请求和流订阅
- 流失败要能重试或显示错误状态
- 网络线程完成后，必须切回 Kuikly 线程再更新 observable 字段

Kuikly 文档要求 UI、`observable`、`setTimeout` 等操作只能在 Kuikly 线程执行。若采用 KMP 多线程或 `kuiklyx.coroutines`，需要显式切回 `Dispatchers.Kuikly[ctx]`；如果要兼容动态化，优先考虑 `NetworkModule` 或平台 Module。

## 11. 网络、存储和原生能力边界

### 11.1 HTTP

Kuikly 可使用 `NetworkModule` 发起 HTTP 请求。请求结果回调中：

1. 解析 JSON
2. 转换为 `ChatMessage`
3. 在 Kuikly 线程更新 Store
4. 由 `observableList` 驱动 UI

### 11.2 WebSocket / IM SDK

WebSocket 和具体 IM SDK 不应该由 `Chat` 组件直接持有，建议抽象为：

```kotlin
interface ChatTransport {
    fun connect(conversationId: String, callback: TransportCallback)
    fun send(message: OutgoingMessage, callback: SendCallback)
    fun disconnect()
}
```

平台侧实现可以是：

- NetworkModule
- 自定义 Kuikly Module
- `expect/actual` KMP adapter
- 宿主注入的现有 IM SDK

### 11.3 图片和文件

Kuikly `Image` 支持：

- 网络图片
- base64
- assets
- file URI

建议消息只保存 source 和媒体元信息；上传过程由 `MediaUploadAdapter` 处理：

```kotlin
interface MediaUploadAdapter {
    fun upload(
        localPath: String,
        onProgress: (Float) -> Unit,
        onResult: (Result<UploadedMedia>) -> Unit
    )
}
```

上传进度应更新消息的 metadata 或独立的 `uploadProgressByMessageId`，不要因为进度变化重建整个消息列表。

## 12. 动画实现建议

Kuikly 声明式动画通过响应式状态和 `animate` 绑定：

```kotlin
private var isNewMessage by observable(true)

View {
    attr {
        opacity(if (ctx.isNewMessage) 0f else 1f)
        animate(Animation.linear(0.25f), ctx.isNewMessage)
    }
}
```

建议使用以下策略：

- 插入：opacity + translate
- 发送失败：颜色或状态图标变化
- “回到底部”按钮：opacity
- Composer 高度变化：布局动画
- 删除：先设置 tombstone 状态，动画完成后从列表移除

声明式动画的关键是：先改变状态，再让 View 根据状态呈现不同属性。不要在业务层直接操作大量 View 引用。

## 13. 生命周期和线程模型

### 13.1 页面创建

```text
created
├── 初始化当前用户和会话 ID
├── 初始化 ChatStore
├── 加载本地缓存
├── 请求远程历史
└── 绑定 transport 回调
```

### 13.2 页面销毁

```text
destroyed / 页面退出
├── 取消分页请求
├── 取消 AI stream
├── 断开 WebSocket 或解绑回调
├── 清理图片/上传任务
└── 释放 Module 和定时器
```

### 13.3 线程规则

- Kuikly View、Attr、Event、observable 只能在 Kuikly 线程调用。
- 纯解析和 IO 可放到平台线程或 KMP dispatcher。
- 回到 UI 前必须切回 Kuikly 线程。
- 页面销毁后回调必须丢弃，避免访问已失效 Pager。
- 共享可变消息列表不要同时由多个线程修改。

## 14. 第一版建议的公共 API

建议先稳定下面这些公共接口，后续再扩展媒体、反应和消息编辑：

```text
ChatMessage
ChatUser
MessageMeta
MessageStatus
MessageGroupStatus
ChatStore
ChatConfig
ChatTheme
MessageRenderer
MessageRendererRegistry
ChatTransport
MediaUploadAdapter
PaginationLoader
```

第一版必须支持：

- 文本消息
- 系统消息
- 图片消息
- 自定义消息
- 发送中/成功/失败
- 重试
- 当前用户和对方用户
- 消息分组
- 首次定位到底部
- 加载更早消息
- 新消息自动滚动策略
- 自定义主题
- 自定义 Renderer
- basic/local/pagination 三个 Demo

第一版暂不建议同时实现：

- 音视频播放
- 富文本编辑器
- 复杂 reaction 面板
- 多层嵌套列表
- 全量本地数据库抽象
- 多种 UI DSL 并行实现
- 将 IM 协议直接内置到 UI SDK

## 15. 推荐的开发顺序

### Phase 1：Core

1. 定义 `ChatMessage`、`MessageMeta`、`ChatUser`
2. 定义消息状态和 JSON codec
3. 实现内存 Store
4. 实现消息 ID 唯一性检查
5. 实现分组计算
6. 为 Store 编写单元测试

### Phase 2：最小 UI

1. `Chat` 页面容器
2. `ChatList`
3. `TextMessageRenderer`
4. `ChatMessageItem`
5. `TextArea` Composer
6. 内存发送 Demo

### Phase 3：交互能力

1. 图片和文件 Renderer
2. 系统消息
3. 自定义消息 registry
4. 发送失败和重试
5. 消息点击和长按
6. 主题和暗色模式

### Phase 4：聊天体验

1. 首屏滚到底部
2. 新消息自动滚动策略
3. “回到底部”按钮
4. 上拉加载历史
5. 滚动锚点修正
6. 插入和删除动画

### Phase 5：生产能力

1. NetworkModule/Transport adapter
2. WebSocket 或 IM 接入
3. 图片上传和进度
4. 本地缓存
5. 流式 AI 消息
6. Android、iOS、鸿蒙、H5 兼容性验证

## 16. 测试清单

### Core 测试

- 重复消息 ID 是否拒绝
- 插入、批量插入、更新、删除、替换全部消息
- 未知 `kind` 是否降级
- 状态优先级是否正确
- 消息分组在边界时间下是否正确
- JSON 往返是否保持字段

### UI 测试

- 空列表
- 只有一条消息
- 连续同作者消息
- 超长文本
- 图片没有宽高
- 文件名很长
- 自定义消息不可识别
- 发送中、失败、重试
- 键盘弹出和收起
- 当前位于历史位置时收到新消息
- 历史分页后位置不跳动

### 跨端测试

- Android
- iOS
- 鸿蒙
- H5/小程序（如果目标包含）
- 不同字体和屏幕宽度
- 深色模式
- 网络慢、断网、恢复网络

## 17. 设计结论

参考 Flutter Chat UI SDK 时，最应该迁移的不是 `Chat` 这个组件，而是以下架构原则：

```text
消息协议独立
    ↓
Store 统一管理消息状态
    ↓
observableList 驱动 Kuikly List
    ↓
ChatMessageItem 负责外层布局
    ↓
Renderer 负责具体消息内容
    ↓
Transport/Upload/Storage 由宿主注入
```

Kuikly 的实现建议是：

1. 使用传统 DSL 的 `List` 实现消息列表，不用普通 `Scroller` 承担大量 item 渲染。
2. 使用 `observableList` + `vfor` 作为列表真相来源，并用 `diffUpdate` 优化批量更新。
3. 使用 `Input`/`TextArea` 实现 Composer，聊天输入优先使用 `TextArea`。
4. 用稳定 `message.id` 做更新、动画和滚动锚点。
5. 用 `kind` + Renderer Registry 支持自定义消息。
6. 用 `NetworkModule`、自定义 Module 或 KMP adapter 处理网络、上传和原生能力。
7. 所有 UI 状态更新回到 Kuikly 线程。
8. 将分页、滚动锚点和自动滚动作为独立能力设计，不和消息请求代码耦合。
9. 第一版先实现文本、图片、系统、自定义消息和分页，不要一开始复制全部 Flutter 能力。

## 18. 参考源码和官方资料

### Flutter Chat UI 源码

- `packages/flutter_chat_core/lib/src/models/message.dart`
- `packages/flutter_chat_core/lib/src/models/user.dart`
- `packages/flutter_chat_core/lib/src/chat_controller/chat_controller.dart`
- `packages/flutter_chat_core/lib/src/chat_controller/chat_operation.dart`
- `packages/flutter_chat_core/lib/src/chat_controller/in_memory_chat_controller.dart`
- `packages/flutter_chat_core/lib/src/models/builders.dart`
- `packages/flutter_chat_core/lib/src/theme/chat_theme.dart`
- `packages/flutter_chat_ui/lib/src/chat.dart`
- `packages/flutter_chat_ui/lib/src/chat_animated_list/chat_animated_list.dart`
- `packages/flutter_chat_ui/lib/src/composer.dart`
- `examples/flyer_chat/lib/basic.dart`
- `examples/flyer_chat/lib/local.dart`

### Kuikly 官方资料

- `KuiklyUI/docs/API/components/list.md`
- `KuiklyUI/docs/API/components/scroller.md`
- `KuiklyUI/docs/API/components/input.md`
- `KuiklyUI/docs/API/components/text-area.md`
- `KuiklyUI/docs/API/components/image.md`
- `KuiklyUI/docs/DevGuide/reactive-update.md`
- `KuiklyUI/docs/DevGuide/directive.md`
- `KuiklyUI/docs/DevGuide/animation-declarative.md`
- `KuiklyUI/docs/DevGuide/network.md`
- `KuiklyUI/docs/DevGuide/thread-and-coroutines.md`
