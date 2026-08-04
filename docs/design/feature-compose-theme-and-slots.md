---
last_updated: 2026-08-04
status: draft
owner: @kuikly-chat-team
---

# Feature: Compose Chat 主题分层与 Slots/Builders 对齐

## Status: 📝 Draft

## 目标
将扁平 `ChatTheme` 拆分为语义化主题层，并梳理 `ChatSlots` 与 Flutter `Builders` 的对照关系（见 `flutter-chat-ui-components.md` §8、§9），使宿主能系统性换肤与替换 UI，而非零散改颜色。

## 非目标
- 不追求与 Flutter `Builders` API 名一一相同（保持 Kuikly 命名，但文档对照表清晰）。
- 不实现 `UserCache` / `CrossCache` 完整库（仅主题与 slot 契约）。
- 不在本需求内实现具体消息 Renderer（见各 renderer 文档）。

## 技术方案

### 涉及的模块
- `chat/compose/core/ChatTheme.kt`：拆分为多 data class
- `chat/compose/ui/ChatSlots.kt`：补全与 Builders 对照的 slot
- `docs/design/flutter-chat-ui-components.md`：增加 Kuikly 对照列（可选）

### 主题结构

```kotlin
data class ChatTheme(
    val colors: ChatColors = ChatColors.Default,
    val typography: ChatTypography = ChatTypography.Default,
    val shapes: ChatShapes = ChatShapes.Default,
    val spacing: ChatSpacing = ChatSpacing.Default,
) {
    fun merge(other: ChatTheme): ChatTheme
    companion object {
        val Default = ChatTheme()
        fun light(): ChatTheme
        fun dark(): ChatTheme
    }
}

data class ChatColors(
    val background: Long,
    val surface: Long,
    val primary: Long,
    val onPrimary: Long,
    val incomingBubble: Long,
    val outgoingBubble: Long,
    // error, divider, avatarPlaceholder, ...
)

data class ChatTypography(
    val bodyLarge: TextStyleTokens,
    val bodySmall: TextStyleTokens,
    val labelSmall: TextStyleTokens, // 时间戳
)
```

现有 `ChatTheme` 字段通过 `merge` / 兼容 getter 过渡，避免一次性破坏 Demo。

### Slots ↔ Builders 对照表

| Flutter Builder | Kuikly ChatSlots |
|-----------------|------------------|
| `chatMessageBuilder` | `messageRowContent` |
| `textMessageBuilder` | `MessageRendererRegistry` + text renderer |
| `composerBuilder` | 整页替换需包 `ChatScreen`；局部用 `composerTrailingContent` |
| `scrollToBottomBuilder` | `scrollToBottomContent` |
| `loadMoreBuilder` | `loadEarlierContent` |
| `emptyChatListBuilder` | `emptyContent` |
| `linkPreviewBuilder` | 预留 `linkPreviewContent`（P2） |

### API 变更
- `ChatScreen(theme: ChatTheme)` 保持入口；内部组件读 `theme.colors.*`。
- 废弃路径：直接读扁平 `theme.outgoingBubbleColor` 标记 `@Deprecated` 一版后移除。

## 验收标准
- `ChatTheme.light()` / `dark()` 在 Demo 可切换，气泡/背景/Composer 一致变化。
- `theme.merge` 只覆盖局部 token 时其余不变。
- 文档中 Slots↔Builders 对照表与代码一致。
- 现有单测与 Demo 在默认主题下无视觉回归（人工）。

## 依赖
- 各 UI 子功能文档（avatar、time、list chrome 等）落地后统一接入新主题 token
- `feature-kuikly-chat-ui-sdk-redesign`（✅）

---
> 参考：Flutter `ChatTheme`、`ChatColors`、`ChatTypography`、`Builders`
