# KuiklyChat

基于 [Kuikly](https://kuikly.tencent.com) 跨端框架构建的聊天 UI 组件库，支持 Android、iOS、鸿蒙、H5 多端运行。

设计参考 [Stream Chat Compose SDK](https://getstream.io/chat/sdk/compose/)，提供开箱即用的聊天界面和高度灵活的定制能力。

## 目录

- [接入指南](#接入指南)
- [核心 API](#核心-api)
  - [ChatSession（核心入口）](#chatsession核心入口)
  - [ChatSessionConfig（配置类）](#chatsessionconfig配置类)
  - [ChatMessage（消息模型）](#chatmessage消息模型)
  - [ChatMessageHelper（工具类）](#chatmessagehelper工具类)
  - [MessageRendererFactory（渲染工厂）](#messagerendererfactory渲染工厂)
- [主题系统](#主题系统)
- [Slot 插槽系统](#slot-插槽系统)
- [内置组件](#内置组件)
- [使用示例](#使用示例)
- [发布到 Maven](#发布到-maven)
- [注意事项](#注意事项)

---

## 接入指南

### 1. 添加 Maven 仓库

在 `settings.gradle.kts` 中添加仓库地址：

```kotlin
dependencyResolutionManagement {
    repositories {
        // ... 其他仓库
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}
```

### 2. 添加依赖

在模块的 `build.gradle.kts` 中添加：

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // KuiklyChat 聊天组件
                implementation("com.tencent.kuiklybase:KuiklyChat:{version}-{kotlinVersion}")
                // 例如：
                // implementation("com.tencent.kuiklybase:KuiklyChat:1.0.0-2.0.21")
            }
        }
    }
}
```

> 版本号格式：`{baseVersion}-{kotlinVersion}`，例如 `1.0.0-2.0.21`。鸿蒙平台使用 `1.0.0-2.0.21-KBA-010` 格式。

### 3. 导入包

```kotlin
import com.tencent.kuiklybase.chat.*
```

### 4. 最小化示例

```kotlin
@Page("chat")
class MyChatPage : BasePager() {

    var messageList by observableList<ChatMessage>()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr { flex(1f); flexDirection(FlexDirection.COLUMN) }

                ChatSession({ ctx.messageList }) {
                    title = "聊天"
                    showMessageComposer = true
                    composerSafeAreaBottom = ctx.pagerData.safeAreaInsets.bottom
                    onSendMessage = { text ->
                        ctx.messageList.add(
                            ChatMessageHelper.createTextMessage(
                                content = text,
                                isSelf = true,
                                senderName = "我"
                            )
                        )
                    }
                }
            }
        }
    }
}
```

---

## 核心 API

### ChatSession（核心入口）

聊天界面的顶层入口，是 `ViewContainer` 的扩展函数：

```kotlin
fun ViewContainer<*, *>.ChatSession(
    messageList: () -> ObservableList<ChatMessage>,
    config: ChatSessionConfig.() -> Unit
)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `messageList` | `() -> ObservableList<ChatMessage>` | 响应式消息列表的 Lambda 引用 |
| `config` | `ChatSessionConfig.() -> Unit` | DSL 配置块 |

`ChatSession` 内部自动包含：导航栏、消息列表（含背景图层）、可选的 MessageComposer 输入框。

---

### ChatSessionConfig（配置类）

所有配置通过 `ChatSession` 的尾部 Lambda 设置。支持 **DSL 分组** 和 **平铺** 两种风格。

#### 基础配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `title` | `String` | `"聊天"` | 导航栏标题 |
| `showNavigationBar` | `Boolean` | `true` | 是否显示导航栏 |
| `showBackButton` | `Boolean` | `true` | 是否显示返回按钮 |
| `selfAvatarUrl` | `String` | `""` | 自己的头像 URL |
| `messageActions` | `List<MessageAction>` | `defaultMessageActions()` | 长按消息的操作菜单项 |

#### MessageComposer 输入框配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `showMessageComposer` | `Boolean` | `false` | 是否显示内置输入框 |
| `composerPlaceholder` | `String` | `"输入消息..."` | 输入框提示文字 |
| `composerSendButtonText` | `String` | `"发送"` | 发送按钮文字 |
| `composerSafeAreaBottom` | `Float` | `0f` | 底部安全区域高度（需从外部传入 `pagerData.safeAreaInsets.bottom`） |

#### 事件回调

| 回调 | 类型 | 说明 |
|------|------|------|
| `onBackClick` | `(() -> Unit)?` | 返回按钮点击 |
| `onMessageClick` | `((ChatMessage) -> Unit)?` | 消息点击 |
| `onMessageLongPress` | `((ChatMessage) -> Unit)?` | 消息长按 |
| `onMessageLongPressWithPosition` | `((ChatMessage, Float, Float, Float, Float) -> Unit)?` | 带位置的消息长按 (msg, x, y, width, height) |
| `onResend` | `((ChatMessage) -> Unit)?` | 失败消息重发 |
| `onAvatarClick` | `((ChatMessage) -> Unit)?` | 头像点击 |
| `onSendMessage` | `((String) -> Unit)?` | 发送消息（内置 Composer 用） |
| `onInputValueChange` | `((String) -> Unit)?` | 输入文本变化 |
| `onAttachmentsClick` | `(() -> Unit)?` | 附件按钮点击 |
| `onReactionClick` | `((ChatMessage, String) -> Unit)?` | 反应点击 (消息, 反应类型) |
| `onEditMessage` | `((ChatMessage) -> Unit)?` | 消息编辑 |
| `onDeleteMessage` | `((ChatMessage) -> Unit)?` | 消息删除 |
| `onThreadClick` | `((ChatMessage) -> Unit)?` | 线程回复点击 |
| `onMessageAction` | `((ChatMessage, MessageAction) -> Unit)?` | 操作菜单项点击 |
| `onPinMessage` | `((ChatMessage) -> Unit)?` | 消息置顶 |

#### 暴露的滚动 API

ChatSession 内部初始化时自动设置，业务方通过 config 引用调用：

| API | 类型 | 说明 |
|-----|------|------|
| `scrollToBottomAction` | `((animate: Boolean) -> Unit)?` | 手动滚动到底部 |
| `scrollToMessageAction` | `((messageId: String, animate: Boolean) -> Unit)?` | 滚动到指定消息 |

```kotlin
// 使用方式：保存 config 引用
var chatConfig: ChatSessionConfig? = null

ChatSession({ ctx.messageList }) {
    ctx.chatConfig = this
}

// 后续使用
chatConfig?.scrollToBottomAction?.invoke(true)
chatConfig?.scrollToMessageAction?.invoke(targetMsgId, true)
```

#### DSL 分组配置方法

```kotlin
ChatSession({ ctx.messageList }) {
    // 主题配置
    theme { /* ChatThemeOptions */ }
    // 列表行为配置
    messageListOptions { /* MessageListOptions */ }
    // 渲染插槽配置
    slots { /* ChatSlotOptions */ }
}
```

---

### theme {} — 主题配置（ChatThemeOptions）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `themeMode` | `ChatThemeMode` | `LIGHT` | 主题模式：`LIGHT` / `DARK` / `SYSTEM` |
| `primaryColor` | `Long` | `0xFF4F8FFF` | 主色（自己气泡渐变起始色、导航栏） |
| `primaryGradientEndColor` | `Long` | `0xFF6C5CE7` | 渐变结束色 |
| `backgroundColor` | `Long` | `0xFFF0F2F5` | 页面背景色 |
| `otherBubbleColor` | `Long` | `0xFFFFFFFF` | 对方气泡背景色 |
| `otherTextColor` | `Long` | `0xFF333333` | 对方文字颜色 |
| `selfTextColor` | `Long` | `0xFFFFFFFF` | 自己文字颜色 |
| `avatarRadius` | `Float` | `8f` | 头像圆角（20f=圆形, 8f=微信风格, 0f=方形） |
| `bubbleMaxWidthRatio` | `Float` | `0.65f` | 气泡最大宽度比例 |
| `bubblePaddingH` | `Float` | `12f` | 气泡水平内边距 |
| `bubblePaddingV` | `Float` | `10f` | 气泡垂直内边距 |
| `messageFontSize` | `Float` | `15f` | 消息文字大小 |
| `messageLineHeight` | `Float` | `22f` | 消息行高 |
| `avatarSize` | `Float` | `40f` | 头像尺寸 |
| `rowPaddingV` | `Float` | `6f` | 消息行垂直间距 |
| `rowPaddingH` | `Float` | `12f` | 消息行水平间距 |
| `avatarBubbleGap` | `Float` | `8f` | 头像与气泡间距 |
| `backgroundImage` | `String` | `""` | 聊天区域背景图 URL |
| `imageMaxWidth` | `Float` | `200f` | 图片消息最大宽度 |
| `imageMaxHeight` | `Float` | `200f` | 图片消息最大高度 |
| `imageRadius` | `Float` | `12f` | 图片消息圆角 |
| `composerBackgroundColor` | `Long` | `0xFFF8F8F8` | 输入栏背景色 |
| `composerBorderColor` | `Long` | `0xFFE0E0E0` | 输入栏边框色 |
| `composerInputBackgroundColor` | `Long` | `0xFFFFFFFF` | 输入框背景色 |
| `composerInputBorderColor` | `Long` | `0xFFE0E0E0` | 输入框边框色 |
| `composerInputTextColor` | `Long` | `0xFF333333` | 输入框文字色 |
| `composerPlaceholderColor` | `Long` | `0xFFBBBBBB` | 占位文字色 |
| `composerSendButtonTextColor` | `Long` | `0xFFFFFFFF` | 发送按钮文字色 |

**便捷方法：**

| 方法 | 说明 |
|------|------|
| `useDarkTheme()` | 切换到深色主题 |
| `useLightTheme()` | 切换到浅色主题 |
| `applyThemeColors(colors)` | 应用预定义颜色方案 |

> 所有 theme 属性均可在 Config 顶层直接设置（向后兼容），如 `primaryColor = 0xFF6C5CE7` 等价于 `theme { primaryColor = 0xFF6C5CE7 }`。

---

### messageListOptions {} — 列表行为配置（MessageListOptions）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `autoScrollToBottom` | `Boolean` | `true` | 新消息自动滚动到底部 |
| `showAvatar` | `Boolean` | `true` | 是否显示头像 |
| `showSenderName` | `Boolean` | `true` | 是否显示昵称 |
| `showTimeGroup` | `Boolean` | `true` | 启用时间分组 |
| `timeGroupInterval` | `Long` | `300000`（5分钟） | 时间分组间隔（毫秒） |
| `timeFormatter` | `TimeFormatter?` | `null` | 自定义时间格式化器 |
| `enableMessageGrouping` | `Boolean` | `true` | 启用消息分组（合并头像、缩小间距） |
| `messageGroupingInterval` | `Long` | `120000`（2分钟） | 消息分组间隔阈值 |
| `typingUsers` | `List<String>` | `emptyList()` | 正在输入的用户列表（非空时显示指示器） |
| `onLoadEarlier` | `(() -> Unit)?` | `null` | 滚动到顶部时加载历史消息回调 |
| `isLoadingEarlier` | `Boolean` | `false` | 是否正在加载历史（由业务维护） |
| `hasMoreEarlier` | `Boolean` | `true` | 是否还有更多历史 |
| `loadEarlierThreshold` | `Float` | `100f` | 触发加载的距顶阈值（像素） |

**自动滚动策略：**

| 消息来源 | 策略 |
|----------|------|
| 自己发的消息（`isSelf = true`） | **始终**滚动到底部 |
| 他人的消息（`isSelf = false`） | 当前在底部才滚动，否则保持位置 |

**加载历史消息用法：**

```kotlin
messageListOptions {
    hasMoreEarlier = true
    onLoadEarlier = {
        // 1. 标记加载中
        chatConfig?.isLoadingEarlier = true
        // 2. 异步获取历史消息
        fetchHistory { historyMessages ->
            // 3. 插入到列表头部（组件自动处理位置补偿防跳动）
            messageList.addAll(0, historyMessages)
            // 4. 更新状态
            chatConfig?.isLoadingEarlier = false
            if (noMoreHistory) chatConfig?.hasMoreEarlier = false
        }
    }
}
```

---

### ChatMessage（消息模型）

```kotlin
data class ChatMessage(
    val id: String,                                    // 消息唯一 ID
    val content: String,                               // 消息内容（文本或图片 URL）
    val isSelf: Boolean,                               // 是否为自己发送
    val type: MessageType = MessageType.TEXT,           // 消息类型
    val status: MessageStatus = MessageStatus.SENT,    // 发送状态
    val senderName: String = "",                       // 发送者名称
    val senderAvatar: String = "",                     // 发送者头像 URL
    val timestamp: Long = 0L,                          // 时间戳（毫秒）
    val extra: Map<String, String> = emptyMap(),       // 扩展数据
    val senderId: String = "",                         // 发送者唯一 ID
    val reactions: List<ReactionItem> = emptyList(),   // 消息反应列表
    val threadCount: Int = 0,                          // 线程回复数量
    val isEdited: Boolean = false,                     // 是否已编辑
    val isDeleted: Boolean = false,                    // 是否已删除（软删除）
    val isPinned: Boolean = false,                     // 是否置顶
    val readBy: List<String> = emptyList(),            // 已读用户 ID 列表
    val attachments: List<Attachment> = emptyList()    // 附件列表
)
```

#### MessageType — 消息类型

| 值 | 说明 |
|----|------|
| `TEXT` | 文本消息 |
| `IMAGE` | 图片消息（内置渲染：自动缩放+圆角） |
| `VIDEO` | 视频消息 |
| `FILE` | 文件消息 |
| `SYSTEM` | 系统消息（时间提示、通知等） |
| `CUSTOM` | 自定义消息（需通过 Slot 或 Factory 渲染） |

#### MessageStatus — 发送状态

| 值 | 说明 |
|----|------|
| `SENDING` | 发送中（显示"发送中..."） |
| `SENT` | 已发送（不显示状态） |
| `FAILED` | 发送失败（显示"发送失败，点击重试" + 红色重发按钮） |
| `READ` | 已读（显示"已读"） |

#### ReactionItem — 消息反应

```kotlin
data class ReactionItem(
    val type: String,                  // 反应类型（如 "👍"、"❤️"）
    val count: Int = 1,                // 反应数量
    val isOwnReaction: Boolean = false // 当前用户是否已添加（高亮显示）
)
```

#### Attachment — 附件

```kotlin
data class Attachment(
    val type: AttachmentType,          // IMAGE / VIDEO / FILE / GIPHY / LINK_PREVIEW
    val url: String,                   // 资源 URL
    val title: String = "",            // 标题
    val mimeType: String = "",         // MIME 类型
    val fileSize: Long = 0L,           // 文件大小（字节）
    val width: Int = 0,                // 宽度
    val height: Int = 0,               // 高度
    val duration: Float = 0f,          // 时长（秒）
    val thumbnailUrl: String = "",     // 缩略图 URL
    val extra: Map<String, String> = emptyMap()
)
```

#### MessageAction — 操作菜单项

```kotlin
data class MessageAction(
    val key: String,                              // 操作标识（copy/reply/edit/delete/pin 等）
    val label: String,                            // 显示文本
    val icon: String = "",                        // 图标（base64 或 URL）
    val isDestructive: Boolean = false,           // 是否为危险操作（红色显示）
    val isVisible: (ChatMessage) -> Boolean = { true } // 动态可见性
)
```

#### MessageContext — 消息上下文

Slot 和 Factory 渲染时提供的上下文信息，用于实现连续消息合并头像等效果：

```kotlin
data class MessageContext(
    val message: ChatMessage,           // 当前消息
    val previousMessage: ChatMessage?,  // 上一条消息
    val nextMessage: ChatMessage?,      // 下一条消息
    val index: Int = 0,                 // 索引
    val isFirstInGroup: Boolean = true, // 是否为分组第一条（显示发送者名称）
    val isLastInGroup: Boolean = true   // 是否为分组最后一条（显示头像）
)
```

---

### ChatMessageHelper（工具类）

提供快捷创建消息的静态方法：

```kotlin
// 创建文本消息
ChatMessageHelper.createTextMessage(
    content = "Hello!",
    isSelf = true,
    senderName = "我",
    senderId = "user_001",
    senderAvatar = "https://example.com/avatar.png",
    status = MessageStatus.SENT,
    timestamp = 1711267200000L,
    reactions = listOf(ReactionItem("👍", 1))
)

// 创建图片消息
ChatMessageHelper.createImageMessage(
    imageUrl = "https://example.com/photo.jpg",
    isSelf = false,
    senderName = "小助手",
    width = 600, height = 400,
    timestamp = 1711267200000L
)

// 创建视频消息
ChatMessageHelper.createVideoMessage(
    videoUrl = "https://example.com/video.mp4",
    isSelf = true,
    thumbnailUrl = "https://example.com/thumb.jpg",
    width = 1920, height = 1080, duration = 120f
)

// 创建文件消息
ChatMessageHelper.createFileMessage(
    fileUrl = "https://example.com/doc.pdf",
    fileName = "项目文档.pdf",
    isSelf = false,
    mimeType = "application/pdf",
    fileSize = 1024 * 1024
)

// 创建系统消息
ChatMessageHelper.createSystemMessage("以下是新的聊天", timestamp = 1711267200000L)

// 创建自定义消息
ChatMessageHelper.createCustomMessage(
    content = "location",
    isSelf = true,
    extra = mapOf("lat" to "39.9", "lng" to "116.4")
)

// 生成唯一消息 ID
val id = ChatMessageHelper.generateId()  // "msg_1_1711267200000"

// 构建消息上下文（计算分组信息）
val ctx = ChatMessageHelper.buildMessageContext(messages, index, groupingInterval = 120000L)
```

---

### MessageRendererFactory（渲染工厂）

注册制的消息渲染扩展机制，用于扩展自定义消息类型渲染：

```kotlin
interface MessageRendererFactory {
    fun canRender(message: ChatMessage): Boolean
    fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig)
}
```

**渲染优先级链**：`messageBubble Slot` > `类型独立 Slot` > `MessageRendererFactory` > `默认内置渲染`

**内置渲染器**（通过 `defaultMessageRenderers()` 获取）：

| 渲染器 | 匹配类型 |
|--------|----------|
| `TextMessageRenderer` | `MessageType.TEXT` |
| `ImageMessageRenderer` | `MessageType.IMAGE` |
| `SystemMessageRenderer` | `MessageType.SYSTEM` |
| `VideoMessageRenderer` | `MessageType.VIDEO` |
| `FileMessageRenderer` | `MessageType.FILE` |

**自定义渲染器示例**：

```kotlin
class LocationMessageRenderer : MessageRendererFactory {
    override fun canRender(message: ChatMessage): Boolean {
        return message.type == MessageType.CUSTOM && message.extra["subType"] == "location"
    }
    override fun render(container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) {
        container.View {
            // 自定义地图卡片渲染
        }
    }
}

// 注册
ChatSession({ ctx.messageList }) {
    messageRenderers.add(LocationMessageRenderer())
}
```

---

## 主题系统

### 内置双主题

组件内置 `LightThemeColors` 和 `DarkThemeColors` 两套配色方案：

```kotlin
ChatSession({ ctx.messageList }) {
    theme {
        // 使用深色主题
        useDarkTheme()

        // 或设置主题模式
        themeMode = ChatThemeMode.DARK
    }
}
```

### 自定义主题颜色

```kotlin
theme {
    primaryColor = 0xFF6C5CE7
    primaryGradientEndColor = 0xFFA29BFE
    backgroundColor = 0xFFF0F2F5
    otherBubbleColor = 0xFFF5F0FF
    otherTextColor = 0xFF2D3436
    selfTextColor = 0xFFFFFFFF
}
```

### ChatThemeColors 数据类

包含 30+ 个颜色属性，覆盖所有 UI 元素：

```kotlin
val customColors = ChatThemeColors(
    primaryColor = 0xFF6C5CE7,
    backgroundColor = 0xFF1A1A2E,
    otherBubbleColor = 0xFF2D2D44,
    // ... 更多颜色配置
)

theme {
    applyThemeColors(customColors)
}
```

---

## Slot 插槽系统

Slot 是高级定制能力，允许替换组件的某个部分的渲染。通过 `slots {}` DSL 配置。

### 消息渲染 Slot

| Slot | 类型 | 说明 |
|------|------|------|
| `messageBubble` | `MessageBubbleSlot` | 替换**所有非系统消息**渲染（优先级最高） |
| `textBubble` | `MessageBubbleSlot` | 替换**文本消息**渲染 |
| `imageBubble` | `MessageBubbleSlot` | 替换**图片消息**渲染 |
| `customBubble` | `MessageBubbleSlot` | 替换**自定义消息**渲染 |
| `systemMessage` | `SimpleBubbleSlot` | 替换系统消息渲染 |

**Slot 类型签名：**

```kotlin
typealias MessageBubbleSlot = (container: ViewContainer<*, *>, context: MessageContext, config: ChatSessionConfig) -> Unit
typealias SimpleBubbleSlot = (container: ViewContainer<*, *>, message: ChatMessage, config: ChatSessionConfig) -> Unit
```

### 列表状态 Slot

| Slot | 类型 | 说明 |
|------|------|------|
| `emptyContent` | `ViewSlot` | 空列表占位 |
| `loadingContent` | `ViewSlot` | 首次加载指示器 |
| `loadingMoreContent` | `ViewSlot` | 顶部加载更多指示器 |
| `helperContent` | `ViewSlot` | 辅助内容（如"滚动到底部"按钮） |
| `dateSeparator` | `DateSeparatorSlot` | 日期分隔符 |
| `typingIndicator` | `TypingIndicatorSlot` | 输入指示器 |
| `reactionBar` | `ReactionBarSlot` | 反应栏 |
| `messageOptions` | `MessageOptionsSlot` | 操作菜单 |

### 导航栏 Slot

| Slot | 类型 | 说明 |
|------|------|------|
| `navigationBar` | `NavigationBarSlot` | 替换整个导航栏 |
| `navigationBarTrailing` | `ViewSlot` | 导航栏右侧区域 |

### MessageComposer Slot

| Slot | 类型 | 说明 |
|------|------|------|
| `messageComposer` | `MessageComposerSlot` | 完全替换输入框（设置后其他 Slot 失效） |
| `composerIntegrations` | `ComposerIntegrationsSlot` | 输入框左侧按钮区域 |
| `composerInput` | `ComposerInputSlot` | 核心输入区域 |
| `composerTrailing` | `ComposerTrailingSlot` | 输入框右侧（默认发送按钮） |
| `composerHeader` | `ComposerHeaderSlot` | 输入框顶部（回复/编辑提示） |
| `composerFooter` | `ComposerFooterSlot` | 输入框底部（工具栏） |

**MessageComposer 架构：**

```
┌──────────────────────────────────────────┐
│  composerHeader  (回复/编辑提示条)         │
├──────────────────────────────────────────┤
│        │                      │          │
│integra-│   composerInput      │ trailing  │
│ tions  │   (核心输入框)        │ (发送按钮) │
│(附件等) │                      │          │
├──────────────────────────────────────────┤
│  composerFooter  (工具栏/表情面板入口)      │
├──────────────────────────────────────────┤
│  safeArea  (底部安全区域)                  │
└──────────────────────────────────────────┘
```

---

## 内置组件

组件库提供以下独立可用的子组件（均为 `ViewContainer` 扩展函数）：

### ChatBubble — 消息气泡

```kotlin
container.ChatBubble {
    attr {
        content = "消息内容"
        isSelf = false
        avatarUrl = "https://..."
        senderName = "小助手"
        primaryColor = 0xFF4F8FFF
        showAvatar = true
        status = MessageStatus.SENT
        reactions = listOf(ReactionItem("👍", 3, true))
        isEdited = false
        isPinned = false
    }
    event {
        onClick = { /* 点击 */ }
        onLongPress = { /* 长按 */ }
        onLongPressWithPosition = { x, y, w, h -> /* 带位置的长按 */ }
        onResendClick = { /* 重发 */ }
        onReactionClick = { type -> /* 反应点击 */ }
    }
}
```

### ChatNavigationBar — 导航栏

```kotlin
container.ChatNavigationBar {
    attr {
        title = "聊天"
        showBackButton = true
        primaryColor = 0xFF4F8FFF
        primaryGradientEndColor = 0xFF6C5CE7
    }
    event {
        onBackClick = { /* 返回 */ }
        onTrailingClick = { /* 右侧按钮 */ }
    }
}
```

### ChatMessageOptions — 操作菜单

长按消息弹出的操作菜单，支持高斯模糊背景、气泡镂空、入场动画：

```kotlin
container.ChatMessageOptions {
    attr {
        message = targetMessage
        actions = defaultMessageActions()
        showQuickReactions = true
        quickReactions = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
        targetX = bubbleX; targetY = bubbleY
        targetWidth = bubbleW; targetHeight = bubbleH
        screenWidth = pageWidth; screenHeight = pageHeight
    }
    event {
        onActionClick = { action -> /* 处理操作 */ }
        onReactionSelect = { emoji -> /* 添加反应 */ }
        onDismiss = { /* 关闭菜单 */ }
    }
}
```

### ChatDateSeparator — 日期分隔符

```kotlin
container.ChatDateSeparator {
    attr {
        dateText = "今天 14:30"
        bgColor = 0xFFCECECE
        textColor = 0xFFFFFFFF
    }
}
```

### ChatTypingIndicator — 输入指示器

三个跳动圆点 + 文字提示动画：

```kotlin
container.ChatTypingIndicator {
    attr {
        typingText = "Alice 正在输入..."
        dotColor = 0xFF999999
        textColor = 0xFF999999
    }
}
```

### ChatReactionBar — 反应栏

```kotlin
container.ChatReactionBar {
    attr {
        reactions = listOf(ReactionItem("👍", 3, true), ReactionItem("❤️", 1, false))
        bgColor = 0xFFF0F0F0
        highlightBgColor = 0xFFE3F2FD
    }
    event {
        onReactionClick = { type -> /* 处理点击 */ }
    }
}
```

### ChatSystemMessage — 系统消息

```kotlin
container.ChatSystemMessage {
    attr {
        message = "以下是新的聊天"
    }
}
```

### 工具函数

| 函数 | 说明 |
|------|------|
| `defaultMessageActions()` | 返回默认操作列表（复制、回复、引用、编辑、置顶、删除） |
| `defaultMessageRenderers()` | 返回 5 个内置渲染器列表 |
| `defaultTimeFormat(timestamp)` | 默认时间格式化（"HH:mm"） |
| `shouldShowDateSeparator(prev, current, interval)` | 判断是否需要日期分隔 |
| `formatTypingText(typingUsers)` | 格式化输入提示文字 |
| `resolveThemeColors(mode)` | 根据主题模式获取颜色方案 |

---

## 使用示例

### 示例 1：完整聊天页面（带内置输入框）

```kotlin
@Page("chat")
class ChatPage : BasePager() {
    var messageList by observableList<ChatMessage>()
    private var chatConfig: ChatSessionConfig? = null

    override fun created() {
        super.created()
        messageList.add(ChatMessageHelper.createSystemMessage("欢迎来到聊天"))
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr { flex(1f); flexDirection(FlexDirection.COLUMN) }

                ChatSession({ ctx.messageList }) {
                    title = "KuiklyChat"
                    selfAvatarUrl = "https://example.com/my-avatar.png"

                    // 启用内置输入框
                    showMessageComposer = true
                    composerSafeAreaBottom = ctx.pagerData.safeAreaInsets.bottom
                    onSendMessage = { text -> ctx.sendMessage(text) }

                    // 主题
                    theme {
                        primaryColor = 0xFF6C5CE7
                        primaryGradientEndColor = 0xFFA29BFE
                        otherBubbleColor = 0xFFF5F0FF
                    }

                    // 列表行为
                    messageListOptions {
                        autoScrollToBottom = true
                        enableMessageGrouping = true
                        showTimeGroup = true
                        timeGroupInterval = 3 * 60 * 1000L
                    }

                    // 事件
                    onBackClick = { ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage() }
                    onMessageLongPressWithPosition = { message, x, y, w, h ->
                        // 弹出操作菜单
                    }

                    ctx.chatConfig = this
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        messageList.add(ChatMessageHelper.createTextMessage(
            content = text, isSelf = true, senderName = "我"
        ))
    }
}
```

### 示例 2：自定义输入栏（不使用内置 Composer）

```kotlin
ChatSession({ ctx.messageList }) {
    title = "客服助手"
    showMessageComposer = false  // 不启用内置输入框
}

// 业务自行实现输入栏
View {
    attr { flexDirection(FlexDirection.ROW); padding(8f, 12f, 8f, 12f); alignItemsCenter() }
    View { /* 语音按钮 */ }
    Input {
        attr { flex(1f); placeholder("输入消息...") }
        event { inputReturn { ctx.doSend(it.text) } }
    }
    View { /* 表情按钮 */ }
    View { /* 发送按钮 */ event { click { ctx.doSend() } } }
}
```

### 示例 3：背景图 + 自定义颜色

```kotlin
ChatSession({ ctx.messageList }) {
    theme {
        backgroundImage = "https://example.com/chat-bg.jpg"
        primaryColor = 0xFF6C5CE7
        primaryGradientEndColor = 0xFFA29BFE
        otherBubbleColor = 0xFFF5F0FF
        otherTextColor = 0xFF2D3436
    }
}
```

### 示例 4：深色主题

```kotlin
ChatSession({ ctx.messageList }) {
    theme {
        useDarkTheme()
    }
}
```

### 示例 5：隐藏头像（简洁 1v1 聊天）

```kotlin
ChatSession({ ctx.messageList }) {
    title = "与 Alice 的对话"
    messageListOptions {
        showAvatar = false
        showSenderName = false
    }
}
```

### 示例 6：按类型自定义消息渲染（Slot）

```kotlin
ChatSession({ ctx.messageList }) {
    slots {
        // 自定义文本气泡
        textBubble = { container, context, config ->
            val msg = context.message
            container.ChatBubble {
                attr {
                    content = msg.content
                    isSelf = msg.isSelf
                    showAvatar = context.isLastInGroup
                    senderName = if (context.isFirstInGroup) msg.senderName else ""
                }
            }
        }

        // 自定义图片消息
        imageBubble = { container, context, config ->
            // 自定义渲染逻辑
        }

        // 自定义空列表占位
        emptyContent = { container ->
            container.Text {
                attr { text("快来打个招呼吧"); fontSize(16f); color(Color(0xFF999999)) }
            }
        }
    }
}
```

### 示例 7：加载历史消息

```kotlin
ChatSession({ ctx.messageList }) {
    messageListOptions {
        hasMoreEarlier = true
        onLoadEarlier = {
            ctx.chatConfig?.isLoadingEarlier = true
            setTimeout(1000) {
                val history = loadHistoryFromServer()
                ctx.messageList.addAll(0, history)  // 组件自动补偿位置
                ctx.chatConfig?.isLoadingEarlier = false
                if (history.isEmpty()) ctx.chatConfig?.hasMoreEarlier = false
            }
        }
    }
}
```

### 示例 8：消息操作菜单

```kotlin
// 在 ChatSession 中监听长按事件
onMessageLongPressWithPosition = { message, x, y, w, h ->
    ctx.targetMessage = message
    ctx.targetX = x; ctx.targetY = y
    ctx.targetW = w; ctx.targetH = h
    ctx.showOptions = true
}

// 在页面层级使用 ChatMessageOptions 组件
vif({ ctx.showOptions }) {
    View {
        attr { positionAbsolute(); top(0f); left(0f); width(screenW); height(screenH) }
        ChatMessageOptions {
            attr {
                message = ctx.targetMessage
                actions = defaultMessageActions()
                targetX = ctx.targetX; targetY = ctx.targetY
                targetWidth = ctx.targetW; targetHeight = ctx.targetH
                screenWidth = screenW; screenHeight = screenH
            }
            event {
                onActionClick = { action -> /* 处理操作 */ }
                onReactionSelect = { emoji -> /* 添加反应 */ }
                onDismiss = { ctx.showOptions = false }
            }
        }
    }
}
```

### 示例 9：消息反应

```kotlin
// 创建带反应的消息
ChatMessageHelper.createTextMessage(
    content = "这条消息有反应！",
    isSelf = false,
    reactions = listOf(
        ReactionItem("👍", 3, true),   // 自己点过的高亮
        ReactionItem("❤️", 1, false)
    )
)

// 监听反应点击
onReactionClick = { message, reactionType ->
    // 切换反应逻辑
}
```

### 示例 10：消息编辑/删除/置顶标记

```kotlin
// 已编辑（气泡底部显示"(已编辑)"）
message.copy(isEdited = true)

// 软删除（气泡内容替换为"此消息已被删除"）
message.copy(isDeleted = true)

// 置顶（气泡下方显示"📌 已置顶"）
message.copy(isPinned = true)
```

---

## 发布到 Maven

项目内置发布脚本 `publish-maven.sh`：

```bash
# 发布到远程 Maven 仓库
./publish-maven.sh -v 1.0.0

# 发布 SNAPSHOT 版本
./publish-maven.sh -v 1.0.0 -s true

# 发布到本地 Maven 仓库（调试用）
./publish-maven.sh -v 1.0.0 -l true

# 指定 Kotlin 版本
./publish-maven.sh -v 1.0.0 -k 2.0.21

# 查看帮助
./publish-maven.sh -h
```

发布后 Maven 坐标：`com.tencent.kuiklybase:KuiklyChat:{version}-{kotlinVersion}`

---

## 注意事项

### 1. 消息列表必须是 ObservableList

```kotlin
// ✅ 正确
var messageList by observableList<ChatMessage>()

// ❌ 错误：普通列表不会触发 UI 更新
val messageList = mutableListOf<ChatMessage>()
```

### 2. 每个页面只放一个 ChatSession

多个 ChatSession 共享同一个 messageList 会导致 `vfor` 响应式更新冲突。

### 3. Slot 中使用 container 参数添加子组件

```kotlin
// ✅ 正确
messageBubble = { container, context, config ->
    container.ChatBubble { ... }
}

// ❌ 错误：使用外层引用会导致 vfor 子节点检测失败
messageBubble = { container, context, config ->
    this@List.ChatBubble { ... }
}
```

### 4. 颜色值必须包含 Alpha 通道

```kotlin
primaryColor = 0xFF4F8FFF   // ✅ 包含 FF（不透明）
primaryColor = 0x4F8FFF     // ❌ 缺少 Alpha 通道
```

### 5. 时间分组需要设置 timestamp

只有 `timestamp > 0` 的消息才参与时间分组。默认值 `0L` 不会显示时间标签。

### 6. composerSafeAreaBottom 需从外部传入

由于 ChatSession 是 `ViewContainer` 扩展函数，无法直接访问 `pagerData`。需要在 Pager/ComposeView 中传入安全区域高度：

```kotlin
composerSafeAreaBottom = ctx.pagerData.safeAreaInsets.bottom
```

### 7. 自动滚动无需手动控制

组件内部已处理所有滚动逻辑。只需 `messageList.add(msg)` 即可，无需手动调用 `scrollToBottomAction`。

---

## 源码结构

```
KuiklyChat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/
  ChatMessage.kt               — 数据模型（ChatMessage / MessageType / MessageStatus / ReactionItem / Attachment / MessageAction / MessageContext / ChatMessageHelper）
  ChatSessionConfig.kt         — 配置类（ChatSessionConfig / ChatThemeOptions / MessageListOptions / ChatSlotOptions / MessageComposerState / 所有 Slot 类型别名）
  ChatTheme.kt                 — 主题系统（ChatThemeMode / ChatThemeColors / LightThemeColors / DarkThemeColors）
  ChatSessionView.kt           — ChatSession 入口 + 消息列表渲染 + 加载历史 + 位置补偿 + 自动滚动
  ChatBubbleView.kt            — ChatBubble 气泡组件 + ChatSystemMessage 系统消息组件
  ChatNavigationBarView.kt     — ChatNavigationBar 导航栏组件
  ChatMessageComposerView.kt   — ChatMessageComposer 输入框组件（5-Slot 架构）
  ChatMessageOptionsView.kt    — ChatMessageOptions 操作菜单组件（模糊背景 + 镂空 + 动画）
  ChatDateSeparatorView.kt     — ChatDateSeparator 日期分隔符组件
  ChatTypingIndicatorView.kt   — ChatTypingIndicator 输入指示器组件（三点跳动动画）
  ChatReactionBarView.kt       — ChatReactionBar 反应栏组件
  MessageRendererFactory.kt    — 渲染工厂接口 + 5 个内置渲染器
```

---

## License

MIT License
