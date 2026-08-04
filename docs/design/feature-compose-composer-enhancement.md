---
last_updated: 2026-08-04
status: draft
owner: @kuikly-chat-team
---

# Feature: Compose Chat 输入区增强（Composer Enhancement）

## Status: 📝 Draft

## 目标
将 `ChatComposer` 从 P0 基础版升级为接近 Flutter `Composer`（见 `flutter-chat-ui-components.md` §6.4）的可用输入区：附件入口、键盘行为、安全区与扩展区域。

## 非目标
- 不实现语音输入、富文本编辑器、@提及、表情面板。
- 不实现草稿持久化与多会话切换。
- 附件上传进度 UI 可与图片消息需求分阶段交付。

## 技术方案

### 涉及的模块
- `chat/compose/ui/ChatComposer.kt`
- `chat/compose/ui/ChatSlots.kt`：`composerTopContent`、`attachmentButtonContent`
- `chat/compose/ui/ChatScreen.kt`：SafeArea、Composer 高度
- `chat/compose/core/ChatIntent`：可选 `SendAttachment`（P1）
- `chat/compose/ui/ChatCallbacks.kt`：`onAttachmentTap`

### 配置扩展

```kotlin
data class ChatScreenConfig(
    val maxLines: Int = 5,
    val minLines: Int = 1,
    val maxLength: Int? = null,
    val showAttachmentButton: Boolean = true,
    val clearInputOnSend: Boolean = true,
)

data class ChatSlots(
    val composerTopContent: (@Composable () -> Unit)? = null, // 引用条等
    val attachmentButtonContent: (@Composable (onClick: () -> Unit) -> Unit)? = null,
)
```

### 键盘行为
- `KeyboardOptions` / `KeyboardActions`：`ImeAction.Send` → `submit`（Kuikly Compose API 以实测为准）。
- `ChatComposer` 非受控输入保持；可选 `initialText` 供草稿恢复。

### 附件流程（首期）
1. 用户点附件 → `onAttachmentTap` 由宿主选文件/图。
2. 宿主将 `MessagePart.Attachment` 附在下次 `SendText` 或独立 Intent。
3. `LlmChatAdapter` 已有 `toLlmAttachments()`（✅）。

## 验收标准
- 附件按钮可见且触发宿主回调；无宿主实现时不崩溃。
- 键盘 Send（若平台支持）与发送按钮行为一致。
- Composer 在刘海屏/键盘弹出时不被遮挡（SafeArea 或 padding 策略文档化）。
- 发送后输入清空；停止生成时按钮状态正确。
- 单测：空文本/超长文本、`maxLength` 裁剪（若有）。

## 依赖
- `feature-kuikly-chat-ui-sdk-redesign`（✅ Composer 基础已有）
- `feature-compose-image-file-messages.md`（📋）：附件消息展示

---
> 参考：Flutter `Composer`、`ComposerHeightNotifier`
