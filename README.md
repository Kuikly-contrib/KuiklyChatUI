# KuiklyChat

基于 [Kuikly](https://kuikly.tencent.com) 跨端框架构建的 **聊天会话 UI 组件**，支持 Android、iOS、鸿蒙、H5 多端运行。

开箱即用的同时，提供 Slot 机制实现深度定制——从气泡颜色、背景图到完全替换输入栏/导航栏。

---

## 目录

- [效果预览](#效果预览)
- [快速接入](#快速接入)
  - [1. 添加 Maven 依赖](#1-添加-maven-依赖)
  - [2. 最简用法](#2-最简用法)
- [配置参数一览](#配置参数一览)
  - [导航栏配置](#导航栏配置)
  - [输入栏配置](#输入栏配置)
  - [头像与昵称](#头像与昵称)
  - [主题色与气泡颜色](#主题色与气泡颜色)
  - [背景图](#背景图)
  - [行为配置](#行为配置)
  - [事件回调](#事件回调)
  - [Slot 自定义渲染](#slot-自定义渲染)
- [使用示例](#使用示例)
  - [示例 1：开箱即用](#示例-1开箱即用)
  - [示例 2：背景图 + 自定义气泡颜色](#示例-2背景图--自定义气泡颜色)
  - [示例 3：隐藏头像与昵称（简洁 1v1 聊天）](#示例-3隐藏头像与昵称简洁-1v1-聊天)
  - [示例 4：自定义气泡渲染（Slot）](#示例-4自定义气泡渲染slot)
  - [示例 5：自定义输入栏（Slot）](#示例-5自定义输入栏slot)
  - [示例 6：只读模式（查看聊天记录）](#示例-6只读模式查看聊天记录)
  - [示例 7：自定义空消息占位](#示例-7自定义空消息占位)
- [数据模型](#数据模型)
  - [ChatMessage](#chatmessage)
  - [MessageType](#messagetype)
  - [MessageStatus](#messagestatus)
  - [ChatMessageHelper 工具类](#chatmessagehelper-工具类)
- [组件架构](#组件架构)
- [发布到 Maven](#发布到-maven)
- [注意事项](#注意事项)

---

## 效果预览

| 默认样式 | 背景图 + 自定义颜色 | 隐藏头像模式 |
|:---:|:---:|:---:|
| 渐变气泡 + 头像 + 昵称 | 背景图铺满 + 淡紫色气泡 | 简洁 1v1 风格 |

---

## 快速接入

### 1. 添加 Maven 依赖

在项目的 `settings.gradle.kts` 中添加仓库：

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

在模块的 `build.gradle.kts` 中添加依赖：

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

> **版本号规则**：`{baseVersion}-{kotlinVersion}`，例如 `1.0.0-2.0.21`。
> 鸿蒙平台使用 `1.0.0-2.0.21-KBA-010` 格式。

### 2. 最简用法

只需 **3 步**：声明消息列表 → 放置 `ChatSession` → 处理发送消息。

```kotlin
import com.tencent.kuiklybase.chat.*

@Page("chat")
class MyChatPage : BasePager() {

    // 第 1 步：声明响应式消息列表
    var messageList by observableList<ChatMessage>()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 第 2 步：放置 ChatSession
            ChatSession({ ctx.messageList }) {
                title = "聊天"
                onSendMessage = { text ->
                    // 第 3 步：处理消息发送
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
```

就这么简单！`ChatSession` 会自动渲染导航栏、消息列表、输入栏，并支持自动滚动到底部。

---

## 配置参数一览

所有配置通过 `ChatSession` 的尾部 lambda（`ChatSessionConfig`）设置：

```kotlin
ChatSession({ ctx.messageList }) {
    // 在这里设置配置...
}
```

### 导航栏配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `title` | `String` | `"聊天"` | 导航栏标题 |
| `showNavigationBar` | `Boolean` | `true` | 是否显示导航栏 |
| `showBackButton` | `Boolean` | `true` | 是否显示返回按钮 |

### 输入栏配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `inputPlaceholder` | `String` | `"输入消息..."` | 输入框占位文案 |
| `showInputBar` | `Boolean` | `true` | 是否显示输入栏（`false` 可用于只读聊天记录） |
| `showSendButton` | `Boolean` | `true` | 是否显示发送按钮 |
| `sendButtonText` | `String` | `"发送"` | 发送按钮文案 |

### 头像与昵称

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `selfAvatarUrl` | `String` | `""` | 自己的头像 URL（为空则使用默认头像） |
| `showAvatar` | `Boolean` | `true` | 是否显示头像 |
| `showSenderName` | `Boolean` | `true` | 是否显示发送者昵称 |

### 主题色与气泡颜色

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `primaryColor` | `Long` | `0xFF4F8FFF` | 主色（自己气泡渐变起始色、发送按钮、导航栏） |
| `primaryGradientEndColor` | `Long` | `0xFF6C5CE7` | 渐变结束色 |
| `backgroundColor` | `Long` | `0xFFF0F2F5` | 页面背景色（无背景图时生效） |
| `otherBubbleColor` | `Long` | `0xFFFFFFFF` | 对方消息气泡背景色 |
| `otherTextColor` | `Long` | `0xFF333333` | 对方消息文字颜色 |
| `selfTextColor` | `Long` | `0xFFFFFFFF` | 自己消息文字颜色 |

### 背景图

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `backgroundImage` | `String` | `""` | 聊天区域背景图 URL（设置后铺满消息列表区域） |

### 行为配置

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `autoScrollToBottom` | `Boolean` | `true` | 新消息时是否自动滚动到底部 |

### 事件回调

| 参数 | 类型 | 说明 |
|------|------|------|
| `onSendMessage` | `(String) -> Unit` | 发送消息回调 |
| `onBackClick` | `() -> Unit` | 返回按钮点击（不设置则默认 `closePage()`） |
| `onMessageClick` | `(ChatMessage) -> Unit` | 消息气泡点击 |
| `onMessageLongPress` | `(ChatMessage) -> Unit` | 消息气泡长按 |

### Slot 自定义渲染

Slot 是高级定制能力，允许替换组件的某个部分的默认渲染。

| Slot | 类型 | 说明 |
|------|------|------|
| `messageBubble` | `MessageBubbleSlot` | 替换消息气泡渲染 |
| `systemMessage` | `MessageBubbleSlot` | 替换系统消息渲染 |
| `inputBar` | `InputBarSlot` | 替换输入栏渲染 |
| `navigationBar` | `NavigationBarSlot` | 替换导航栏渲染 |
| `navigationBarTrailing` | `ViewSlot` | 导航栏右侧操作区域 |
| `emptyView` | `ViewSlot` | 消息列表为空时的占位渲染 |

**Slot 类型定义：**

```kotlin
// 消息气泡/系统消息 Slot
typealias MessageBubbleSlot = (container: ViewContainer<*, *>, message: ChatMessage, config: ChatSessionConfig) -> Unit

// 输入栏 Slot
typealias InputBarSlot = (container: ViewContainer<*, *>, onSend: (String) -> Unit) -> Unit

// 导航栏 Slot
typealias NavigationBarSlot = (container: ViewContainer<*, *>, config: ChatSessionConfig) -> Unit

// 通用 Slot（导航栏右侧、空消息占位等）
typealias ViewSlot = (container: ViewContainer<*, *>) -> Unit
```

---

## 使用示例

### 示例 1：开箱即用

最简写法，使用全部默认配置：

```kotlin
ChatSession({ ctx.messageList }) {
    title = "客服助手"
    onSendMessage = { text -> ctx.onSendMessage(text) }
}
```

### 示例 2：背景图 + 自定义气泡颜色

```kotlin
ChatSession({ ctx.messageList }) {
    title = chatTitle

    // 聊天背景图
    backgroundImage = "https://example.com/chat-bg.jpg"

    // 自定义气泡颜色
    primaryColor = 0xFF6C5CE7            // 自己气泡渐变起始色
    primaryGradientEndColor = 0xFFA29BFE  // 自己气泡渐变结束色
    otherBubbleColor = 0xFFF5F0FF         // 对方气泡（淡紫色）
    otherTextColor = 0xFF2D3436           // 对方文字颜色
    selfTextColor = 0xFFFFFFFF            // 自己文字颜色

    onSendMessage = { text -> ctx.onSendMessage(text) }
}
```

### 示例 3：隐藏头像与昵称（简洁 1v1 聊天）

```kotlin
ChatSession({ ctx.messageList }) {
    title = "与 Alice 的对话"
    showAvatar = false       // 隐藏头像
    showSenderName = false   // 隐藏昵称
    onSendMessage = { text -> ctx.onSendMessage(text) }
}
```

### 示例 4：自定义气泡渲染（Slot）

通过 `messageBubble` Slot 完全控制气泡的渲染方式：

```kotlin
ChatSession({ ctx.messageList }) {
    title = chatTitle
    onSendMessage = { text -> ctx.onSendMessage(text) }

    // 自定义气泡
    messageBubble = { container, message, config ->
        container.ChatBubble {
            attr {
                content = message.content
                isSelf = message.isSelf
                avatarUrl = message.senderAvatar
                senderName = if (!message.isSelf) message.senderName else ""
                // 使用红色系主题
                primaryColor = 0xFFFF6B6B
                primaryGradientEndColor = 0xFFEE5A24
            }
        }
    }
}
```

> **注意**：Slot 中的 `container` 参数是 `vfor` 内部容器的引用，必须通过 `container.XXX { }` 添加子组件，不要使用外层的 `this@List`。

### 示例 5：自定义输入栏（Slot）

```kotlin
ChatSession({ ctx.messageList }) {
    title = chatTitle
    onSendMessage = { text -> ctx.onSendMessage(text) }

    // 自定义输入栏
    inputBar = { container, onSend ->
        container.ChatInputBar {
            attr {
                placeholder = "说点什么吧..."
                primaryColor = 0xFFFF6B6B
                primaryGradientEndColor = 0xFFEE5A24
                sendButtonText = "GO"
            }
            event {
                onSendMessage = onSend
            }
        }
    }
}
```

### 示例 6：只读模式（查看聊天记录）

```kotlin
ChatSession({ ctx.messageList }) {
    title = "聊天记录"
    showInputBar = false    // 隐藏输入栏
    showBackButton = true
}
```

### 示例 7：自定义空消息占位

```kotlin
ChatSession({ ctx.messageList }) {
    title = "新对话"
    onSendMessage = { text -> ctx.onSendMessage(text) }

    // 自定义空消息提示
    emptyView = { container ->
        container.Text {
            attr {
                text("快来打个招呼吧～ 👋")
                fontSize(16f)
                color(Color(0xFF999999))
            }
        }
    }
}
```

---

## 数据模型

### ChatMessage

聊天消息的数据模型：

```kotlin
data class ChatMessage(
    val id: String,                              // 消息唯一 ID
    val content: String,                         // 消息内容（文本或图片 URL）
    val isSelf: Boolean,                         // 是否为自己发送
    val type: MessageType = MessageType.TEXT,     // 消息类型
    val status: MessageStatus = MessageStatus.SENT, // 发送状态
    val senderName: String = "",                 // 发送者名称
    val senderAvatar: String = "",               // 发送者头像 URL
    val timestamp: Long = 0L,                    // 时间戳（毫秒）
    val extra: Map<String, String> = emptyMap()  // 扩展数据
)
```

### MessageType

```kotlin
enum class MessageType {
    TEXT,    // 文本消息
    IMAGE,   // 图片消息
    SYSTEM   // 系统消息（如时间提示、通知）
}
```

### MessageStatus

```kotlin
enum class MessageStatus {
    SENDING,  // 发送中（显示 "发送中..."）
    SENT,     // 已发送（不显示状态）
    FAILED,   // 发送失败（显示 "发送失败"，红色）
    READ      // 已读（显示 "已读"）
}
```

### ChatMessageHelper 工具类

提供快捷创建消息的方法：

```kotlin
// 创建文本消息
val msg = ChatMessageHelper.createTextMessage(
    content = "Hello!",
    isSelf = true,
    senderName = "我",
    senderAvatar = "https://example.com/avatar.png",
    status = MessageStatus.SENT
)

// 创建图片消息
val imgMsg = ChatMessageHelper.createImageMessage(
    imageUrl = "https://example.com/photo.jpg",
    isSelf = false,
    senderName = "小助手",
    width = 300,
    height = 200
)

// 创建系统消息
val sysMsg = ChatMessageHelper.createSystemMessage("以下是新的聊天")
```

---

## 组件架构

```
KuiklyChat/
└── src/commonMain/kotlin/com/tencent/kuiklybase/chat/
    ├── ChatMessage.kt          # 数据模型 + Slot 类型定义 + ChatSessionConfig
    ├── ChatSessionView.kt      # ChatSession DSL 入口（组合所有子组件）
    ├── ChatBubbleView.kt       # 消息气泡组件 + 系统消息组件
    ├── ChatInputBarView.kt     # 输入栏组件
    └── ChatNavigationBarView.kt # 导航栏组件
```

**组件层级关系：**

```
ChatSession (DSL 扩展函数)
├── ChatNavigationBar (或自定义 navigationBar Slot)
├── 消息区域 (含背景图层)
│   ├── Image (backgroundImage，绝对定位铺满)
│   ├── List
│   │   └── vfor(messageList)
│   │       └── View (itemRoot)
│   │           ├── ChatSystemMessage (系统消息)
│   │           └── ChatBubble (普通消息气泡)
│   └── 空消息占位 (emptyView Slot)
└── ChatInputBar (或自定义 inputBar Slot)
```

---

## 发布到 Maven

项目内置了发布脚本 `publish-maven.sh`，支持一键发布到 Maven 仓库：

```bash
# 发布到远程 Maven 仓库
./publish-maven.sh -v 1.0.0

# 发布 SNAPSHOT 版本
./publish-maven.sh -v 1.0.0 -s true

# 发布到本地 Maven 仓库（调试用）
./publish-maven.sh -v 1.0.0 -l true

# 指定 Kotlin 版本
./publish-maven.sh -v 1.0.0 -k 2.0.21

# 查看完整参数
./publish-maven.sh -h
```

**发布配置**（在 `gradle.properties` 中）：

```properties
# 版本号
mavenVersion=1.0.0
# Group ID
GROUP_ID=com.tencent.kuiklybase
# Kotlin 版本列表
KOTLIN_VERSION_LIST=2.0.21
# 鸿蒙 Kotlin 版本列表
OHOS_KOTLIN_VERSION_LIST=2.0.21-KBA-010
# Maven 仓库地址
MAVEN_REPO_URL=https://mirrors.tencent.com/repository/maven/kuikly-open/
```

发布后，其他项目通过以下 Maven 坐标引用：

```
com.tencent.kuiklybase:KuiklyChat:{version}-{kotlinVersion}
```

---

## 注意事项

### 1. 每个页面只放一个 ChatSession

`ChatSession` 内部包含完整的消息列表和状态管理。如果在同一个 `body()` 中放置多个 `ChatSession` 并共享同一个 `messageList`，会导致 `vfor` 响应式更新冲突，引发闪退。

```kotlin
// ❌ 错误：同一页面放两个 ChatSession 共享列表
ChatSession({ ctx.messageList }) { ... }
ChatSession({ ctx.messageList }) { ... }  // 会闪退！

// ✅ 正确：每个页面只放一个 ChatSession
ChatSession({ ctx.messageList }) { ... }
```

### 2. Slot 中使用 container 参数添加子组件

在 `messageBubble` 等 Slot 回调中，必须通过 `container` 参数来添加子组件，不要使用外层作用域的引用：

```kotlin
// ✅ 正确
messageBubble = { container, message, config ->
    container.ChatBubble { ... }
}

// ❌ 错误：使用外层引用，会导致 vfor 子节点检测失败
messageBubble = { container, message, config ->
    this@List.ChatBubble { ... }  // 闪退！
}
```

### 3. 消息列表必须是 ObservableList

`ChatSession` 的第一个参数是 `() -> ObservableList<ChatMessage>` 类型。必须使用 Kuikly 的 `observableList` 声明，以便触发响应式更新：

```kotlin
// ✅ 正确
var messageList by observableList<ChatMessage>()

// ❌ 错误：普通列表不会触发 UI 更新
val messageList = mutableListOf<ChatMessage>()
```

### 4. 颜色值格式

所有颜色参数使用 `Long` 类型的 ARGB 格式，需要包含 Alpha 通道：

```kotlin
primaryColor = 0xFF4F8FFF   // ✅ 包含 FF（不透明）
primaryColor = 0x4F8FFF     // ❌ 缺少 Alpha 通道
```

---

## License

MIT License
