---
last_updated: 2026-08-02
status: proposed
owner: @kuikly-chat-team
baseline_commit: 9996192
---

# Feature: KuiklyAI LlmChatView 完整能力对标

## Status: Proposed

本文是 KuiklyAI Toolkit 的 v2 设计。`9996192` 已交付 provider、流式会话、基础
Compose UI 和 Demo，但不能据此宣称已经完整对标 Flutter AI Toolkit。v2 以本文列出的
七项 `LlmChatView` 交互能力为完成口径，替代旧文档中“接口存在即算实现”的口径。

对标对象是 Flutter AI Toolkit 的互动聊天组件 `LlmChatView`。本项目对外入口继续使用
`KuiklyAiChatView`，不因对标而破坏已有命名和调用方。

---

## 完成定义

“支持某项功能”同时满足以下条件：

1. 公共 API 能表达该能力，默认 UI 有完整入口、状态、成功和失败反馈。
2. 业务逻辑有自动化测试，不能只留下回调或空接口。
3. 依赖平台能力时，至少在仓库对应宿主中提供可运行适配和 Demo 接线。
4. 不支持该能力的平台必须通过 capability 明确降级，不能展示无响应按钮。
5. README 说明能力边界；设计文档和实现状态一致。

只有接口、只有 Controller 方法、只有自定义 slot，均不算默认组件已经实现。

## 当前交付范围

本轮按 Android-first 验收口径交付：七项交互能力均已接入 Kuikly Compose DSL 的默认
`KuiklyAiChatView`，并由 `androidApp` 的 `kuiklyai_demo` 页面提供真实宿主服务。当前已交付：

| 能力 | Android 状态 |
|---|---|
| 多行文本输入 | 已交付，支持换行、长文本、编辑态复用 |
| 语音输入 | 已交付，系统 SpeechRecognizer、权限、实时/最终结果和失败反馈 |
| 多媒体输入 | 已交付，相机、相册、文件、Provider 能力校验和真实附件编码 |
| 图像缩放 | 已交付，缩略图、全屏预览、双击/双指缩放和平移 |
| 复制到剪贴板 | 已交付，用户消息和 LLM 回复均走 Android ClipboardManager |
| 消息编辑 | 已交付，仅最新用户消息可编辑、取消恢复和重新提交 |
| Material / Cupertino | 已交付，输入控件和主题可显式切换 |

iOS、H5、鸿蒙和 macOS 的宿主适配仍按本文平台矩阵推进；在这些平台未注入能力时，组件
会隐藏对应入口，而不是提供无响应按钮。因此本文整体状态仍为 `proposed`，直到跨端矩阵
和全部验收清单完成。

### v1 基线差距

| 对标能力 | `9996192` 状态 | v2 目标 |
|---|---|---|
| 多行文本输入 | 已实现基础 `TextField(maxLines = 5)` | 补齐换行、粘贴和键盘行为验收 |
| 语音输入 | 只有接口、状态和麦克风入口 | 宿主适配、权限、实时结果、错误和 Demo 全链路 |
| 多媒体输入 | Picker 接口和附件模型；默认 UI 仅文件名 chip | 图片缩略图、相机/相册/文件、类型校验、真实发送 |
| 图像缩放 | 未实现 | 全屏预览、双击缩放、双指缩放和平移 |
| 复制到剪贴板 | 未实现 | 用户消息和 LLM 回复均有默认复制操作及反馈 |
| 消息编辑 | Controller 方法和外部回调；无默认编辑器 | 仅最新用户消息可编辑、取消和重新提交 |
| Material / Cupertino | 只有 Material 3 控件和颜色主题 | 两套真实组件外观，共享同一行为层 |

---

## 目标

1. `KuiklyAiChatView` 默认提供多行输入、消息复制和最新消息编辑。
2. 注入平台服务后提供语音、相机、相册和文件输入，并在 Demo 中可实际操作。
3. 图片附件以缩略图展示，点击后可全屏缩放查看。
4. Provider 明确声明可接收的附件类型；UI 不允许选择后端无法发送的内容。
5. Material 与 Cupertino 只分叉视觉组件，不复制 Controller 和会话逻辑。
6. 保持 v1 的多轮历史、流式输出、取消、重试、Markdown 和序列化能力。
7. 保持核心库无厂商 SDK、无固定网络栈，`commonMain` 平台无关。

## 非目标

1. 不替代现有 `ChatSession`，不改造 Kuikly DSL 的 IM 会话组件。
2. 不在核心库绑定某个语音云服务、图片选择 SDK 或文件上传服务。
3. 不实现 function calling 或工具执行编排。
4. 不承诺所有 LLM 协议原生接受任意文件；Provider 必须准确暴露自身能力。
5. 不用单纯换颜色冒充 Cupertino；两套样式必须有独立控件结构。
6. 不持久化图片二进制；历史仍默认忽略 `bytesBase64`。

---

## 总体架构

核心依赖保持单向：

```text
ui/material ─┐
             ├─> ui/common ─> runtime ─> provider
ui/cupertino ┘                    └─────> platform contracts

host adapters (androidApp / iosApp / H5 / ohosApp)
             └──────────────────> platform contracts
```

- `provider/` 只负责模型能力、附件传输和消息历史，不依赖 Compose。
- `runtime/` 持有输入、编辑、语音、附件、预览和反馈状态。
- `ui/common/` 负责消息列表、缩略图、预览和行为接线。
- `ui/material/` 与 `ui/cupertino/` 只实现各自的输入框、按钮、菜单和反馈外观。
- 平台采集和剪贴板实现放平台 source set 或宿主 App，禁止进入 `commonMain`。

### 计划文件

```text
kuiklyai/
├── platform/
│   ├── LlmClipboard.kt
│   ├── LlmAttachmentPicker.kt
│   └── LlmSpeechRecognizer.kt
├── provider/
│   ├── LlmProviderCapabilities.kt
│   └── LlmAttachmentWireEncoder.kt
├── runtime/
│   ├── LlmChatController.kt
│   ├── LlmChatEditState.kt
│   └── LlmChatUiEvent.kt
└── ui/
    ├── common/
    │   ├── LlmChatContent.kt
    │   ├── LlmAttachmentThumbnail.kt
    │   ├── LlmImagePreviewDialog.kt
    │   └── LlmMessageActions.kt
    ├── material/MaterialLlmChatChrome.kt
    └── cupertino/CupertinoLlmChatChrome.kt
```

每个 Kotlin 文件不超过 400 行。拆文件时保留现有公共符号的转发入口，避免无必要的
源码不兼容。

---

## 公共 API

### 入口参数

```kotlin
enum class LlmChatDesignSystem {
    MATERIAL,
    CUPERTINO,
}

data class LlmChatPlatformServices(
    val clipboard: LlmClipboard? = null,
    val attachmentPicker: LlmAttachmentPicker? = null,
    val speechRecognizer: LlmSpeechRecognizer? = null,
)

@Composable
fun KuiklyAiChatView(
    provider: LlmProvider,
    designSystem: LlmChatDesignSystem = LlmChatDesignSystem.MATERIAL,
    platformServices: LlmChatPlatformServices = LlmChatPlatformServices(),
    style: LlmChatViewStyle = LlmChatViewStyle.forDesignSystem(designSystem),
    strings: LlmChatViewStrings = LlmChatViewStrings.Default,
    suggestions: List<String> = emptyList(),
    safeAreaBottom: Float = 0f,
    messageBuilder: (@Composable (LlmChatMessage) -> Boolean)? = null,
    modifier: Modifier = Modifier,
)
```

兼容规则：

- 保留现有 `attachmentPicker`、`speechRecognizer` 和 Controller 重载，内部转成
  `LlmChatPlatformServices`。
- `MATERIAL` 是默认值，现有调用方视觉不突变。
- 现有 `onEditMessage` 保留为高级自定义入口；未传时启用内置编辑器，传入时由宿主
  接管编辑动作。
- `LlmChatViewStyle.Default` 保留并等价于 Material 默认 token；新增
  `Material`、`Cupertino`、`MaterialDark`、`CupertinoDark` 预设。

### 平台服务

```kotlin
fun interface LlmClipboard {
    suspend fun copyText(label: String, text: String)
}

data class LlmProviderCapabilities(
    val attachmentKinds: Set<LlmAttachmentKind> = emptySet(),
    val acceptedMimeTypes: Set<String> = emptySet(),
    val maxAttachmentBytes: Long? = null,
    val maxAttachmentCount: Int? = null,
)

interface LlmProvider {
    val capabilities: LlmProviderCapabilities
        get() = LlmProviderCapabilities()
    // v1 API 保持不变
}
```

附件入口取 Picker 与 Provider 能力的交集。无交集时隐藏附件按钮；附件超过数量、大小或
MIME 限制时在发送前给出明确错误，不发起网络请求。

`OpenAiCompatibleProvider` 默认声明支持图片；文件和音频通过可插拔
`LlmAttachmentWireEncoder` 转成后端协议。`AgUiLlmProvider` 只有在完成对应协议编码后
才能声明支持该类型，禁止静默丢弃附件。

---

## 功能设计

### 1. 多行文本输入

- 默认输入框支持粘贴、选区和换行；Enter 插入换行，发送只能通过发送按钮或明确的
  `Ctrl/Cmd + Enter`。
- 高度从 1 行增长到 5 行，超过后输入框内部滚动，不能把消息列表持续顶出屏幕。
- 发送时保留用户输入中的内部换行，只移除首尾空白。
- 空文本可以在有附件时发送；空文本且无附件时发送按钮禁用。
- 编辑模式复用同一个输入框和多行行为。

验收：粘贴 2,000 字符和 20 行文本后可继续编辑；发送内容的内部换行不丢失；Android、
iOS 和 H5 的软键盘均不会遮挡发送按钮。

### 2. 语音输入

状态机：

```text
Unavailable -> Ready -> RequestingPermission -> Listening -> Finalizing -> Ready
                                      └───────> Failed -> Ready
                                      └───────> Canceled -> Ready
```

- `isAvailable()` 为真才显示麦克风入口。
- 首次使用由宿主适配器申请权限；拒绝、系统限制、识别错误都进入结构化错误反馈。
- 中间识别结果实时写入输入框，但始终基于录音前文本替换本段结果，不能重复累加。
- 最终结果只结束录音，不自动发送，用户仍可修改。
- 录音中提供停止和取消；发送、清空、页面销毁会正确释放会话。
- 正在生成或编辑提交期间不可开始新的录音。

实现阶段必须交付仓库宿主适配：Android、iOS、H5/JS 和鸿蒙分别接系统或宿主已有
语音能力；平台确实不可用时返回 `false`。Demo 另提供确定性的 Fake Recognizer，保证
无权限、无网络环境也能演示中间结果与最终结果。

### 3. 多媒体输入

支持来源：相机、相册、文件管理器。支持模型类型：`IMAGE`、`FILE`、`AUDIO`。

- Picker 负责权限、采集、压缩和临时文件读取，返回 `LlmAttachment`。
- 图片附件必须能提供 URL 或 `bytesBase64`，以便默认缩略图和 Provider 使用。
- 文件展示名称、MIME、大小和类型图标；图片展示真实缩略图，而不是通用图标。
- 待发送附件可逐个删除；失败项不能进入 pending 列表。
- Controller 在选择后和发送前各校验一次 Provider capability，防止切换 Provider 后
  发送不支持的附件。
- Provider 不得忽略无法编码的附件；必须发送成功或抛出 `INVALID_REQUEST`。

缩略图用 Kuikly Compose 的 `rememberAsyncImagePainter` 加载 URL 或 data URI，固定尺寸和
宽高比，提供加载中与失败占位，避免图片加载引起消息列表跳动。

### 4. 图像缩放

点击任意已选或已发送的图片缩略图打开 `LlmImagePreviewDialog`：

- 使用全屏 `Dialog(usePlatformDefaultWidth = false)`，支持返回键、点关闭按钮和点遮罩退出。
- 双击在 `1x` 与 `2.5x` 间切换；双指缩放范围为 `1x..5x`。
- 放大后支持平移，位移按当前图片与 viewport 边界约束，不能把图片永久拖出屏幕。
- 回到 `1x` 时自动归零平移；切换图片或关闭时清除缩放状态。
- 使用 `detectTransformGestures` 和 `graphicsLayer`，不把手势逻辑下沉到宿主。
- 加载失败显示错误占位和关闭操作，不能出现黑屏且无法退出。

缩放计算抽成纯状态对象 `LlmImageTransformState`，对 scale clamp、pan clamp、reset 和
double-tap 写 `commonTest`。

### 5. 复制到剪贴板

- 每条非空用户消息和 LLM 回复都有默认复制操作；复制的是原始纯文本，保留 Markdown
  标记和换行，不复制状态文案。
- Material 用消息操作菜单 + Snackbar；Cupertino 用 action sheet + 轻量成功提示。
- `LlmClipboard` 缺失时不显示复制操作；已注入但执行失败时显示错误，不能假报成功。
- 连续点击只保留最新反馈，反馈自动消失且不进入会话历史。
- Markdown 代码块原有复制能力继续通过同一 Clipboard 接线。

宿主参考实现复用现有 `BridgeModule.copyToPasteboard`。Android 和 iOS 已有基础桥接；H5、
鸿蒙和其他宿主必须补齐后才能在对应平台验收为完成。

### 6. 最新消息编辑并重新提交

只有最后一条用户消息显示编辑操作。默认流程：

```text
Idle -> Editing(messageId, originalDraft) -> Submitting -> Idle
                         └──────── Cancel -> Idle
```

- 点击编辑后，把该消息文本和附件放入 composer，并显示“正在编辑”提示条。
- 原消息和其后回复在提交前保持不变；用户取消时恢复进入编辑前的草稿和待发附件。
- 提交时先校验文本和附件，再把历史截断到原消息之前，以新内容发起一轮请求。
- 编辑期间允许增删附件；不允许编辑 LLM 消息或更早的用户消息。
- 生成过程中隐藏编辑操作；失败或取消的最近一轮仍允许编辑其用户消息。
- `controller.editAndResend()` 保留为低层原子操作；新增 `beginEdit()`、`cancelEdit()`、
  `submitEdit()` 管理默认 UI 状态。

必须覆盖：成功编辑、取消恢复、空内容、附件保留、历史截断、生成中禁止编辑、旧消息
无编辑入口。

### 7. Material 与 Cupertino

共享 `LlmChatContent`、Controller、消息模型、附件和预览逻辑，只替换 chrome：

| 交互区域 | Material | Cupertino |
|---|---|---|
| Composer | Material 3 TextField、填充发送按钮 | iOS 风格边框输入区、圆形发送按钮 |
| 附件来源 | Modal bottom sheet | Cupertino action sheet |
| 消息操作 | Material 菜单 | Cupertino context/action sheet |
| 编辑提示 | Material surface/banner | Cupertino inset bar |
| 成功/错误反馈 | Snackbar | 顶部轻提示或 action sheet 文案 |
| 尺寸 | Material token，最小触控 48dp | Cupertino token，最小触控 44dp |

提供两个显式入口便于发现：

```kotlin
MaterialKuiklyAiChatView(...)
CupertinoKuiklyAiChatView(...)
```

二者最终委托 `KuiklyAiChatView(designSystem = ...)`。不自动根据 target 切换，避免 iPad、
H5 和品牌化宿主出现不可预测视觉；宿主可以显式选择并覆盖 token。

---

## Controller 状态与事件

`LlmChatController` 新增以下只读状态：

```kotlin
val editState: LlmChatEditState
val voiceState: LlmVoiceState
val previewedImage: LlmAttachment?
val transientEvent: LlmChatUiEvent?
```

事件至少包括 `TextCopied`、`PlatformOperationFailed`、`AttachmentRejected`。一次性事件必须
显式消费，不能因重组重复弹 Snackbar 或 action sheet。

所有平台异步回调在 `dispose()` 后失效；旧语音、Picker 或复制任务不能覆盖新页面状态。

---

## Demo 与宿主适配

扩展现有 `kuiklyai_demo`，而不是新增营销页：

1. 顶部提供 Material/Cupertino 分段切换，切换不清空会话。
2. 使用 `EchoLlmProvider` 演示多行、流式、复制和编辑。
3. 内置一张本地示例图和一个示例文件，演示缩略图、缩放和文件发送。
4. Fake Speech Recognizer 依次回调中间与最终结果，保证自动化环境可复现。
5. 真机模式接宿主 Platform Services，用相机、相册、文件、剪贴板和语音做端到端验证。
6. Demo 页面必须同时展示成功路径和权限拒绝/加载失败路径。

平台交付矩阵：

| 宿主 | Clipboard | Camera / Photos / File | Speech | 验收方式 |
|---|---|---|---|---|
| Android | 必须 | 必须 | 必须或明确 unavailable | debug APK 真机 |
| iOS | 必须 | 必须 | 必须或明确 unavailable | Simulator + 真机权限 |
| H5 / JS | 必须 | 浏览器能力范围内 | capability 检测 | 浏览器手工回归 |
| 鸿蒙 | 必须，移除现有 TODO | 必须 | capability 检测 | ohosApp 真机/模拟器 |
| macOS | 必须 | 文件/相册能力范围内 | capability 检测 | KMP 编译 + 宿主回归 |

某平台没有可用 ASR 不视为编译失败，但必须隐藏入口并记录能力矩阵，不能以空实现冒充。

---

## 测试方案

### `commonTest`

- `LlmChatControllerTest`：多行内容、复制成功/失败、编辑状态机、草稿恢复、生命周期取消。
- `LlmProviderCapabilitiesTest`：Picker/Provider 交集、MIME、数量、大小限制。
- `OpenAiWireFormatTest`：图片编码、自定义文件编码、不支持附件明确失败。
- `LlmImageTransformStateTest`：缩放和平移边界、双击、reset。
- `LlmAttachmentSerializerTest`：新字段兼容旧历史，仍不序列化二进制。
- Fake Picker / Clipboard / Speech 覆盖成功、取消、拒绝、异常和过期回调。

共享测试继续只用 `kotlin("test")` 与多平台依赖，禁止 JUnit、Mockito 等 JVM-only 库。

### UI 与宿主验证

- Material/Cupertino 各检查空态、长文本、流式、错误、编辑、附件菜单、消息操作菜单。
- 图片预览在横竖屏和窄屏下不溢出，1x/2.5x/5x 及平移边界正确。
- 输入框、按钮、菜单文字在最长中文/英文文案下不重叠。
- Android 与 iOS 真机分别覆盖相机、相册、文件、剪贴板和权限拒绝。
- H5 与鸿蒙至少覆盖 capability 降级和剪贴板。

### 构建门禁

```bash
bash scripts/check-layer-deps.sh
./gradlew :KuiklyChat:compileDebugKotlinAndroid \
          :KuiklyChat:compileDebugUnitTestKotlinAndroid
./gradlew :KuiklyChat:testDebugUnitTest
./gradlew :androidApp:assembleDebug
```

同时编译 `:KuiklyChat` 的 JS、全部 iOS/macOS targets，以及 `:shared` 的 Android、JS、
全部 iOS targets。文档新鲜度和 `git diff --check` 必须通过。

---

## 实施顺序

1. **M1 行为基线**：Provider capabilities、Clipboard、一次性 UI 事件、多行输入验收。
2. **M2 编辑闭环**：默认消息操作、编辑状态机、Material/Cupertino 编辑 UI。
3. **M3 多媒体闭环**：宿主 Picker、真实缩略图、Provider 附件编码与错误校验。
4. **M4 图片预览**：全屏 Dialog、缩放/平移状态和跨端回归。
5. **M5 语音闭环**：宿主适配、权限状态机、Fake 和真机 Demo。
6. **M6 双设计系统**：完成两套 chrome、全量回归、README 和能力矩阵。

每个里程碑单独可编译、可测试，不允许先合并无行为的按钮再长期补实现。

---

## 验收清单

- [ ] 长文本可粘贴、换行和发送，内部换行保持不变。
- [ ] 语音有权限、实时识别、停止、取消、失败和不可用降级。
- [ ] 相机、相册、图片、文件均能进入消息并被支持它们的 Provider 实际发送。
- [ ] 图片显示真实缩略图，可全屏双击/双指缩放和平移。
- [ ] 用户消息和 LLM 回复均能复制，成功与失败反馈准确。
- [ ] 只有最新用户消息可进入内置编辑模式，取消可恢复，提交会截断并重发。
- [ ] Material 与 Cupertino 都覆盖上述功能，不只是颜色预设。
- [ ] Demo 可在无后端环境演示全部交互，并可切换到真机平台服务。
- [ ] 公共 API 兼容 v1 调用方，历史 JSON 可读取旧版本。
- [ ] 所有门禁、测试、跨平台编译和 Android debug APK 通过。

全部勾选且评审通过后，本文 `status` 才能从 `proposed` 改为 `implemented`。

---

## 已确定的设计决策

| 议题 | 决策 | 原因 |
|---|---|---|
| 默认设计系统 | Material | 保持 v1 行为兼容 |
| Cupertino 实现 | 独立 chrome，共享行为层 | 避免只换颜色，也避免复制业务逻辑 |
| 平台能力 | 核心契约 + 仓库宿主可运行适配 | 保持 KMP 边界，同时让功能可验收 |
| 消息编辑 | 未传自定义回调时启用内置编辑器 | 默认组件必须开箱完成交互 |
| 附件支持 | Picker 与 Provider capability 取交集 | 杜绝选择后静默丢弃 |
| 图片预览 | commonMain Dialog + Compose 手势 | 行为跨端一致，不下沉到各宿主重写 |
| 复制内容 | 原始纯文本 | 保留 Markdown 和换行，结果可预测 |
| 设计状态 | v2 完成前保持 `proposed` | 编译通过不等于能力对标完成 |
