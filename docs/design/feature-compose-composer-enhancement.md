---
last_updated: 2026-08-07
status: draft
owner: @kuikly-chat-team
---

# Feature: Compose Chat 输入区增强（Composer Enhancement）

## Status: 📝 Draft

## 目标
将现有非受控 `ChatComposer` 升级为接近 Flutter `Composer`（见
`flutter-chat-ui-components.md` §6.4）的可用输入区：明确附件选择/发送契约、输入长度限制、
IME Send、底部安全区与扩展区域。

## 非目标
- 不实现语音输入、富文本编辑器、@提及、表情面板。
- 不实现草稿持久化与多会话切换。
- 不在 SDK 内实现相册/相机/文件系统访问；`commonMain` 只定义宿主回调。
- 不在 SDK 内接入 `LlmAttachmentPicker` 或系统/第三方文件选择器；选择器、来源菜单、权限、
  压缩、上传、合规均由宿主实现（对齐 Flyer：SDK 仅提供按钮 + 回调 + 可选气泡展示）。
- 不实现附件上传与上传进度。宿主回调返回的附件必须已经具备可发送的 URI/数据。
- 不在本需求内实现已发送图片/文件消息 UI（见 `feature-compose-image-file-messages.md`）。

## 现状与问题

- `ChatComposer.kt` 已用 `remember { mutableStateOf("") }` 持有输入，已有
  `maxLines`、`onTextChanged`、leading/trailing slot 和发送/停止按钮。
- `ChatController.sendText()` 只能构造 `MessagePart.Text`，空文本会被拒绝；现有附件无法从
  Composer 进入 `LlmChatAdapter`。
- `ChatIntent` 实际定义在 `compose/core/ChatState.kt`，`ChatCallbacks`、`ChatSlots`、
  `ChatScreenConfig` 实际都定义在 `compose/ui/ChatSlots.kt`；不存在单独的
  `ChatIntent.kt` / `ChatCallbacks.kt` / `ChatScreenConfig.kt`。
- 现有 `composerTrailingContent(isSending, onClick)` 在发送中仍把 `onClick` 绑定到
  `submit`，自定义尾部无法通过该 action 停止生成；增强时必须统一主操作语义。
- 仅增加 `onAttachmentTap: () -> Unit` 无法描述「选择结果如何返回、如何预览、如何随消息发送」，
  因此必须一次性补齐结果契约。

## 技术方案

### 涉及的模块
- `chat/compose/ui/ChatComposer.kt`：输入配置、待发送附件、选择协程、IME action 与清理语义。
- `chat/compose/ui/ChatComposerConfig.kt`：新增 Composer 专属静态配置。
- `chat/compose/ui/ChatSlots.kt`：扩展 Composer slots 和宿主 callbacks，保留现有
  `composerLeadingContent` / `composerTrailingContent`。
- `chat/compose/ui/ChatScreen.kt`：继续把 Composer 放在列表下方，并透传底部安全区配置。
- `chat/compose/core/ChatState.kt`：新增通用 `SendMessage` intent，保留 `SendText`。
- `chat/compose/core/ChatController.kt`：新增 `sendMessage`，允许附件-only 消息并修复附件重试。
- `chat/compose/adapter/LlmChatAdapter.kt`：沿用现有 `MessagePart.Attachment -> LlmAttachment` 映射并补测试。

### 配置、slots 与宿主回调

```kotlin
data class ChatComposerConfig(
    val maxLines: Int = 5,
    val minLines: Int = 1,
    val maxLength: Int? = null,
    val initialComposerText: String = "",
    val clearInputOnSend: Boolean = true,
    /** 由宿主传 pagerData.safeAreaInsets.bottom；不包含键盘高度。 */
    val composerBottomInsetDp: Float = 0f,
)

enum class ComposerActionState {
    SEND_DISABLED,
    SEND_ENABLED,
    STOP,
}

data class ChatSlots(
    // 现有字段保持不变
    /** 输入行上方的引用/编辑提示；不同于已有的行内 composerLeadingContent。 */
    val composerHeaderContent: (@Composable () -> Unit)? = null,
    val attachmentButtonContent: (
        @Composable (isPicking: Boolean, onClick: () -> Unit) -> Unit
    )? = null,
    val pendingAttachmentsContent: (
        @Composable (
            attachments: List<MessagePart.Attachment>,
            onRemove: (attachmentId: String) -> Unit,
        ) -> Unit
    )? = null,
    val composerActionContent: (
        @Composable (
            state: ComposerActionState,
            onPrimaryAction: () -> Unit,
        ) -> Unit
    )? = null,
)

data class ChatCallbacks(
    // 现有字段保持不变
    /**
     * 宿主负责权限、选择 UI、必要的压缩/上传；取消时返回空列表。
     * 回传项必须满足 MessagePart.Attachment 的 URI 契约。
     */
    val onAttachmentRequest: (suspend () -> List<MessagePart.Attachment>)? = null,
    val onAttachmentRequestError: ((error: Throwable) -> Unit)? = null,
)
```

`ChatScreenConfig.maxLines` 已存在，不能直接删除。两个现有 `ChatScreen` 入口按
`feature-compose-theme-and-slots.md` 的统一后缀增加 `composerConfig`，默认值将旧
`config.maxLines` 映射为
`ChatComposerConfig(maxLines = config.maxLines)`，其余字段取默认值。不要新增一个同样「除 controller
外全有默认值」的重载，否则 `ChatScreen(controller)` 会产生重载歧义。

新增末尾参数保持多数源码调用不变，但会改变函数 ABI；二进制兼容策略见下文。

附件按钮不使用默认 `showAttachmentButton = true`：只有
`onAttachmentRequest != null` 时才显示，避免出现「能点但无行为」的假能力。自定义
`attachmentButtonContent` 只替换外观，不能绕过能力判断。

`initialComposerText` 仅在 `ChatComposer` 首次进入组合（以 `controller` 为 key）时读取；
后续配置变化不会覆盖用户正在编辑的内容。宿主通过已有 `onTextChanged` 观察草稿；
受控输入与持久化仍不在本期范围。

主操作优先级为 `composerActionContent` → 旧 `composerTrailingContent` → 默认按钮。
三者的 action 必须一致：`STOP` 时派发 `StopGeneration`，`SEND_ENABLED` 时提交，
`SEND_DISABLED` 时 no-op。旧 slot 继续只收到 `isSending`，但其 `onClick` 语义修正为同一主操作。
状态按 `isSending` → `input.isNotBlank() || pendingAttachments.isNotEmpty()` 的优先级计算，
因此 attachment-only 可发送，发送中始终显示 STOP。

### 附件选择与待发送状态

**SDK / 宿主边界（对齐 Flyer）**

| 职责 | SDK (`ChatComposer` + `ChatCallbacks`) | 宿主 |
|------|------------------------------------------|------|
| 附件按钮 | 仅当 `onAttachmentRequest != null` 时显示 | 注入回调 |
| 来源菜单 / 系统选择器 | 不实现 | 相册 / 相机 / 文件等 |
| 权限、压缩、上传、合规 | 不实现 | 宿主在回调内完成 |
| 待发送预览 | 默认 chips + `pendingAttachmentsContent` slot | 可替换 UI |
| 已发送展示 | `AttachmentContent` / `ImageAttachmentContent` | 可替换 `attachmentContent` slot |

Demo 参考：`shared/.../DemoChatAttachmentIntegration.kt`（将 `LlmAttachmentPicker` 桥接到
`onAttachmentRequest`，**不属于 SDK 公开 API**）。

1. Composer 用 `remember(controller)` 保存待发送 `List<MessagePart.Attachment>` 和
   `isPickingAttachment`。
2. 点击附件按钮后用 `rememberCoroutineScope().launch` 调用 `onAttachmentRequest`；
   选择期间禁用重复点击。
3. 空列表视为取消；空 `id`、空 `uri` 或同批重复 `id` 属于无效结果，整批拒绝并回调错误，
   不静默丢弃部分附件。有效结果按 `id` 与已有列表去重后追加。
4. 普通异常不清草稿并把原始 `Throwable` 交给 `onAttachmentRequestError`；
   `CancellationException` 继续抛出；`isPickingAttachment` 在 `finally` 中复位。
5. `composerHeaderContent` 先渲染引用/编辑提示，其后渲染待发送附件；默认待发送 UI 至少显示
   名称、类型和删除入口，`pendingAttachmentsContent` 可完全替换。
6. 发送被 Controller 接受后，待发送附件总是清空，防止同一附件意外重复发送；
   `clearInputOnSend` 只控制文本是否清空。发送被拒绝时两者都保留。

清空文本时同步调用 `onTextChanged("")`；否则宿主持久化的草稿会与 Composer UI 不一致。
初始值读取本身不触发 callback。

### Controller 发送契约

附件不是单独的 UI intent，而是一次消息提交的一部分：

```kotlin
sealed interface ChatIntent {
    data class SendText(val text: String) : ChatIntent // 保留兼容
    data class SendMessage(
        val text: String,
        val attachments: List<MessagePart.Attachment>,
    ) : ChatIntent
}

fun ChatController.sendMessage(
    rawText: String,
    attachments: List<MessagePart.Attachment> = emptyList(),
): Boolean
```

- 文本 trim 后为空且附件为空时拒绝；有附件时允许 attachment-only 消息。
- Controller 再次校验附件 `id/uri` 非空且同一消息内 id 唯一；直接调用公开 API 的宿主也不能
  绕过 Composer 校验。非法提交返回 false 且不修改 state。
- 构造 `parts` 时，仅在文本非空时加入 `MessagePart.Text`，随后按顺序加入附件。
- `sendText(text)` 与 `ChatIntent.SendText` 保留并委托给 `sendMessage(text, emptyList())`。
- 重试信息不能继续只保存在 `lastUserPrompt: String?`；保存最后一次已接受提交的完整
  user parts 快照。重试失败 user 消息时优先使用该消息自身 parts；重试失败 assistant 消息时
  通过 `pendingUserMessageId` 找原 user 消息，再回退最后提交快照。重发生成新 message id，
  但保持文本、附件字段与 part 顺序。
- `ChatMessageSender` 已接收完整 `ChatMessage`，`LlmChatAdapter.toLlmAttachments()` 可直接复用，
  但 data URI 必须按图片/文件文档的 codec 还原为 provider `bytesBase64`，不新增
  provider/AG-UI 类型到 UI 层。

### 键盘行为

- 使用 Kuikly 包的 `KeyboardOptions` / `KeyboardActions` 与
  `ImeAction.Send`；`onSend` 调用与按钮完全相同的 `submit`。
- 显式使用 `Modifier.autoHideKeyboardOnImeAction(false)`，统一 Android/iOS/鸿蒙行为，
  发送后保持键盘；不在回调同帧连续调用 `show()` / `hide()`。
- `STOP` 或 `SEND_DISABLED` 状态下触发 IME Send 均为 no-op；IME Send 不承担停止生成语义。
- `minLines` 与 `maxLines` 直接传给 Kuikly Material3 `TextField`，初始化时校验
  `1 <= minLines <= maxLines`。
- `maxLength` 必须通过
  `Modifier.maxLength(length, LengthLimitType.CHARACTER)` 实现；禁止在
  `onValueChange` 中 `take`/`substring` 截断，否则会造成原生输入光标异常。
- `maxLength == null` 时不附加限制 modifier；非 null 时必须大于 0。

### Safe Area 与键盘避让

- `ChatScreen` 在 `commonMain` 无法读取页面 `pagerData`，由宿主把
  `pagerData.safeAreaInsets.bottom` 传入 `composerBottomInsetDp`。
- inset 只增加 Composer 最外层底部 padding；消息列表仍由 `weight(1f)` 自动占据剩余空间。
- 不使用 Kuikly Compose 不支持的 `Modifier.imePadding()` / `WindowInsets.ime`。
- 默认依赖宿主窗口的键盘适配。若宿主采用 `adjustNothing`，键盘高度监听与页面根布局 padding
  由宿主处理；本配置不得把键盘高度和 safe-area 高度重复相加。

## 验收标准

- 未注入 `onAttachmentRequest` 时不显示附件按钮；注入后可选择、去重、删除并发送附件。
- 支持「文本 + 附件」和 attachment-only；发送失败重试保留原附件及顺序。
- 选择取消/失败、重复点击、发送被拒绝均不丢失现有文本和待发送附件。
- Controller 接受发送后附件总是清空；文本按 `clearInputOnSend` 配置清理。
- 文本被自动清空时 `onTextChanged("")` 只回调一次；保留文本时不产生伪变更。
- IME Send 与发送按钮共用提交逻辑；发送后各端键盘显隐策略一致。
- 默认按钮、新 `composerActionContent` 与旧 `composerTrailingContent` 在发送中都执行停止，
  不会尝试再次提交。
- `minLines/maxLines` 生效；最大长度由原生限制拒绝超限输入，不做字符串裁剪。
- 宿主传入底部安全区后 Composer 不遮挡 home indicator；键盘弹出时不出现双重 padding。
- 发送中仍显示停止按钮；停止生成不误清用户尚未提交的新草稿。
- `commonTest` 覆盖 `sendMessage` 的空提交、attachment-only、part 顺序、重试保留附件和 adapter 映射。
- 最大长度、IME action、picker 与安全区属于原生 UI 行为，列入
  `docs/plans/compose-ui-acceptance.md` 做 Android/H5 人工验收。
- 按 `AGENTS.md` 三条门禁验证，并补跑 JS 编译。

## 兼容性

`ChatSlots`、`ChatCallbacks`、`ChatIntent` 均为公开 API，前两者还是 `data class`。
新增构造参数会改变构造器、`copy/componentN` ABI，新增 sealed 子类型也会影响宿主的穷尽
`when`。`ChatComposerConfig` 作为两个现有 `ChatScreen` 函数的末尾默认参数接入，
`ChatScreenConfig.maxLines` 保留一版作为兼容映射。该方案是源码兼容、二进制不兼容，
与主题/slots 分层一起按 major 版本交付。

## 依赖
- `feature-kuikly-chat-ui-sdk-redesign.md`（✅ Composer 与发送闭环已有）

`feature-compose-image-file-messages.md` 可并行实施；本需求的默认待发送 UI 使用名称/类型 chip
即可独立验收，两者不建立循环硬依赖。

---
> 参考：Flutter `Composer`、`ComposerHeightNotifier`；KuiklyUI
> `docs/Compose/core-components.md`、`material3/TextField.kt`、`TextFieldDemo.kt`
