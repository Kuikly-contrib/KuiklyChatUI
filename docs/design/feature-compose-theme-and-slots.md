---
last_updated: 2026-08-05
status: draft
owner: @kuikly-chat-team
---

# Feature: Compose Chat 主题分层与 Slots/Builders 对齐

## Status: 📝 Draft

## 目标
将扁平 `ChatTheme` 收敛为可由 Compose 与后续 Kuikly DSL 共同消费的语义 token，
并明确 `ChatSlots` 的职责、优先级及 Flutter `Builders` 对照关系（见
`flutter-chat-ui-components.md` §8、§9），使宿主可以系统换肤和局部替换 UI。

## 非目标
- 不追求与 Flutter `Builders` API 名一一相同（保持 Kuikly 命名，但文档对照表清晰）。
- 不把 `com.tencent.kuikly.compose.ui.graphics.Color`、`TextStyle` 等 Compose 类型放入
  `compose/core`；共享 token 继续使用 ARGB `Long` 与 `Float`（sp/dp）。
- 不实现具体 Markdown、媒体、系统消息或列表 chrome；本需求只统一它们使用的 token 与 slot 契约。
- 不预留没有对应设计和调用点的 `linkPreviewContent` 等占位字段。

## 现状与问题

- `compose/core/ChatTheme.kt` 当前是 18 个扁平字段的公开 `data class`，
  `ChatStrings` 与其定义在同一文件但职责独立。
- `compose/ui/ChatSlots.kt` 实际同时定义 `ChatSlots`、`ChatCallbacks` 和
  `ChatScreenConfig`；不存在独立的 `ChatCallbacks.kt` 或 `ChatScreenConfig.kt`。
- `messageContent` 是历史遗留的「整项替换」入口，会绕过 `ChatMessageRow`；
  `messageRowContent` 才是带默认 content 的消息行包装 slot。
- `ChatSlots.modifier` 当前没有任何读取点，不能继续把它描述成生效能力。
- `ChatTheme.merge(other: ChatTheme)` 无法表达局部覆盖：完整 `ChatTheme` 的每个字段都有默认值，
  合并时无法区分「未指定」与「显式恢复默认值」。

## 技术方案

### 涉及的模块
- `chat/compose/core/ChatTheme.kt`：拆分主题 token，并保留 `ChatStrings` 为独立文案契约。
- `chat/compose/ui/ChatSlots.kt`：补齐 KDoc、明确优先级；移除或废弃未使用的 `modifier`。
- 各 draft 子功能文档：只声明自己拥有的 slot/token，不重复定义冲突签名。
- `shared/.../ComposeChatSdkDemoPage.kt`：增加 light/dark 与局部 override 示例。

### 主题结构

```kotlin
data class ChatTheme(
    val colors: ChatColors = ChatColors.light(),
    val typography: ChatTypography = ChatTypography(),
    val shapes: ChatShapes = ChatShapes(),
    val spacing: ChatSpacing = ChatSpacing(),
) {
    fun merge(override: ChatThemeOverride): ChatTheme

    companion object {
        val Default: ChatTheme = light()
        fun light(): ChatTheme = ChatTheme(colors = ChatColors.light())
        fun dark(): ChatTheme = ChatTheme(colors = ChatColors.dark())
    }
}

data class ChatColors(
    val background: Long,
    val surface: Long,
    val incomingBubble: Long,
    val outgoingBubble: Long,
    val incomingText: Long,
    val outgoingText: Long,
    val placeholder: Long,
    val border: Long,
    val accent: Long,
    val error: Long,
    val avatarBackground: Long,
    val avatarForeground: Long,
    val timeText: Long,
    val linkText: Long,
    val codeBackground: Long,
    val codeBorder: Long,
    val systemBackground: Long,
    val systemText: Long,
    val mediaPlaceholder: Long,
    val mediaError: Long,
    val fileBackground: Long,
    val dateSeparatorText: Long,
    val typingIndicator: Long,
    val unreadBadge: Long,
    val onUnreadBadge: Long,
)

data class ChatTypography(
    val bodySizeSp: Float = 16f,
    val bodyLineHeightSp: Float = 22f,
    val labelSizeSp: Float = 12f,
    val codeSizeSp: Float = 13f,
)

data class ChatShapes(
    val bubbleRadiusDp: Float = 18f,
    val groupedBubbleRadiusDp: Float = 4f,
    val composerRadiusDp: Float = 18f,
    val mediaRadiusDp: Float = 12f,
    val systemRadiusDp: Float = 12f,
)

data class ChatSpacing(
    val contentPaddingDp: Float = 12f,
    val messageGroupGapDp: Float = 8f,
    val messageWithinGroupGapDp: Float = 2f,
)

data class ChatThemeOverride(
    val colors: ChatColorsOverride = ChatColorsOverride(),
    val typography: ChatTypographyOverride = ChatTypographyOverride(),
    val shapes: ChatShapesOverride = ChatShapesOverride(),
    val spacing: ChatSpacingOverride = ChatSpacingOverride(),
)

data class ChatColorsOverride(
    val background: Long? = null,
    val accent: Long? = null,
    // 其余 ChatColors 字段一一对应，均为 nullable
)

data class ChatTypographyOverride(
    val bodySizeSp: Float? = null,
    // 其余 ChatTypography 字段一一对应
)

data class ChatShapesOverride(
    val bubbleRadiusDp: Float? = null,
    // 其余 ChatShapes 字段一一对应
)

data class ChatSpacingOverride(
    val contentPaddingDp: Float? = null,
    // 其余 ChatSpacing 字段一一对应
)
```

`*Overrides` 的字段均为 nullable；只有非 null 值覆盖当前主题。这样
`theme.merge(ChatThemeOverride(colors = ChatColorsOverride(accent = ...)))`
只改变 accent，且仍能显式覆盖为默认颜色。不得使用
`fun merge(other: ChatTheme)` 冒充局部合并。

`ChatStrings` 不并入主题。新增「今天 / 昨天 / 正在输入 / 附件加载失败」等文案时继续进入
`ChatStrings`，避免颜色切换意外改变语言。

### 迁移与兼容性

`ChatTheme`、`ChatSlots`、`ChatCallbacks` 和 `ChatScreenConfig` 都是公开 `data class`。
替换主构造器或增加构造参数会改变构造器、`copy`、`componentN` 及 Kotlin/Native 导出面；
兼容 getter 与默认参数不能保持二进制兼容。

本次主题分层按 major 版本交付：

1. 提供旧扁平字段到新 token 的迁移表，并在源码调用点一次性迁移。
2. 如发布策略要求过渡，可保留旧签名的 deprecated 次构造器和只读 getter，但仍须对各 KMP
   产物做 ABI 验证，不能宣称天然兼容。
3. `ChatScreen(theme = ...)` 参数名和入口保持不变；默认 light 主题视觉须与当前版本等价。
4. `ChatSlots.modifier` 若确认无外部读取语义，在同一 major 移除；若暂不能移除则标记
   `@Deprecated("Unused")`，不得继续新增读取规则。

### Slots ↔ Builders 对照表

- `chatMessageBuilder` → `messageRowContent`；接收 SDK 生成的默认 content。
- `textMessageBuilder` → `textPartContent`（由 Markdown 文档定义）；不向
  `MessageRendererRegistry` 注册虚假的 `"text"` 整消息 renderer。
- `systemMessageBuilder` → `systemMessageContent`（由系统消息文档定义）。
- `imageMessageBuilder` / `fileMessageBuilder` → `attachmentContent`，图片底层可再由
  `imageContent` 替换（由媒体文档定义）。
- `composerBuilder` → SDK 暂不提供整块 Composer 替换；宿主可包装 `ChatScreen`，
  局部使用 composer header/action/attachment slots。
- `scrollToBottomBuilder` → `scrollToBottomContent`。
- `loadMoreBuilder` → `loadEarlierContent`。
- `emptyChatListBuilder` → `emptyContent`。
- `isTypingIndicatorBuilder` → `typingIndicatorContent`。
- 日期分隔没有直接 Flutter Builder 对应 → `dateSeparatorContent`。

不在本需求中新增 `linkPreviewContent`；链接预览有独立数据和加载契约后再增加。

### 渲染优先级

目标调用顺序必须固定并写入 `ChatSlots` KDoc：

1. `MessageRendererRegistry` 处理整条 `AgentCardContent` / `Custom` 消息。
2. 纯 `SystemNotice` 消息走 `systemMessageContent` 或默认系统条，绕过普通左右消息行。
3. 其他未命中 registry 的消息若提供历史 `messageContent`，由它整项替换并绕过默认行。
4. 其余消息由 `messageRowContent` 包装 registry/default content；未提供时使用
   `ChatMessageRow`。
5. 默认气泡内部再按 part 调用 `textPartContent` / `attachmentContent`；
   头像、时间、日期、typing 和 Composer 使用各自 slot。

整消息 renderer 或整项 slot 命中后，不得再重复渲染其中的 part。该顺序需由单元测试锁定。

### ChatScreen 参数归并

多个 draft 都需要扩展同一入口，不能各自新增一个「放在末尾」的平行 overload。两个现有
`ChatScreen` 入口统一采用相同后缀顺序：

```kotlin
fun ChatScreen(
    controller: ChatController,
    // 现有 modifier/theme/strings/config/slots/callbacks/rendererRegistry 保持原顺序
    textRenderConfig: ChatTextRenderConfig = ChatTextRenderConfig(),
    composerConfig: ChatComposerConfig = ChatComposerConfig(
        maxLines = config.maxLines,
    ),
    listChromeState: ChatListChromeState = ChatListChromeState.Empty,
)
```

带 `userResolution` 的入口使用同一后缀。该变更只保证命名/默认参数下的源码迁移，
不保证旧二进制；不得再创建默认参数完全重叠、导致 `ChatScreen(controller)` 歧义的 overload。

## 验收标准

- `ChatTheme.light()` / `dark()` 在 Demo 可切换，背景、消息行、Composer、系统/媒体/list chrome
  均读取语义 token，不残留新增硬编码颜色。
- `theme.merge(override)` 只覆盖非 null token；测试覆盖颜色、排版、形状和间距各一项。
- 默认 light 主题与当前版本无明显视觉回归；dark 主题下文字和交互状态可读。
- Slots 对照、签名和优先级与代码一致；整消息、整项、消息行和 part slot 各有路由测试。
- `ChatSlots.modifier` 被删除或明确 deprecated，不再作为伪能力保留。
- 按 `AGENTS.md` 三条门禁验证，并补跑 JS 编译及 Android/H5 人工换肤验收。

## 依赖

- `feature-kuikly-chat-ui-sdk-redesign.md`（✅ 基础主题与 slots 已存在）。
- `feature-compose-message-row.md`、`feature-compose-chat-avatar.md`、
  `feature-compose-message-time-status.md`、`feature-compose-bubble-grouping-shape.md`（✅）。
- Markdown、系统消息、图片/文件、Composer 与 list chrome draft 在批准前必须与本文统一
  token 名和 slot 签名；实现顺序建议先冻结本文契约，再并行实现子功能。

---
> 参考：Flutter `ChatTheme`、`ChatColors`、`ChatTypography`、`Builders`
