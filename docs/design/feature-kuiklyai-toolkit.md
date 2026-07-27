---
last_updated: 2026-07-27
status: implemented
owner: @kuikly-chat-team
---

# Feature: KuiklyAI 工具集（对标 Flutter AI Toolkit）

## Status: ✅ Implemented

> 2026-07-27 已按本文档完成实现。公共入口、三种 provider、运行时、平台能力接口、
> 历史序列化、Compose UI、门禁、commonTest 与 `shared` Demo 均已落地。

对标对象：[Flutter AI Toolkit](https://docs.flutter.dev/ai/ai-toolkit)（仓库 `flutter/ai`，包名 `flutter_ai_toolkit`）。

---

## 目标

提供一个 Kuikly Compose DSL 入口 `KuiklyAiChatView`：宿主传入一个 `LlmProvider` 即可获得完整 AI 对话界面（多轮历史、流式输出、输入区、附件、语音、错误重试、会话序列化），后端协议细节全部收敛在 provider 内，UI 层零感知。

一句话验收口径：**「塞一个 provider 就能跑」**，且官方内置实现不绑死任何云厂商 SDK。

---

## 非目标

防止 Agent / 实现者扩大范围：

1. **不替代 `ChatSession`**。`ChatSession`（Kuikly DSL）继续服务 IM / 频道类场景；KuiklyAI 是面向「人机对话」的独立入口，两者并存，本迭代不做互相兼容层。
2. **不重写 `agentcard_compose/` 的 28 个卡片**。富内容渲染复用现有 Compose 卡片，只做接线。
3. **不在库内实现 HTTP**。`commonMain` 不引入网络栈；传输由宿主注入。
4. **不在库内实现平台能力默认实现**。相册 / 文件选择 / 麦克风 / ASR 只定义接口；`androidMain` / `iosMain` / `macosMain` 也不塞默认 SDK，由宿主决定用哪个。
5. **不做会话持久化的存储层**。只提供 JSON 编解码；写文件 / DB / 云端由宿主负责。
6. **不做 function calling / tool 执行编排**。工具调用的展示可复用 AG-UI；执行由后端负责。
7. **不新建 Gradle 模块**（推荐方案）。作为 `:KuiklyChat` 内的包发布，避免多一套 build / publish。若评审改为独立模块，需同步改发布脚本与文档。
8. **不在本迭代改造现有 `ChatSession` / `composer/` 去支持语音与附件 picker**。能力只出现在 KuiklyAI 路径。

---

## 背景与动机

### Flutter AI Toolkit 值得借鉴什么

- 围绕抽象 `LlmProvider` 组织整库；UI（`LlmChatView`）只认接口。
- 开箱能力：多轮、流式、富文本、语音、多媒体附件、自定义样式、会话序列化、可插拔 LLM。
- 与本仓库 `AGENTS.md` 硬性规则 3 同向：**后端协议细节不向下游 UI 泄漏**——差别是他们把「可插拔」做成了对外扩展点。

### Flutter 踩过、我们要避开的坑

官方开箱几乎只有 `FirebaseProvider`；v0.9.0 迁到 `firebase_ai` 时是破坏性变更，并砍掉 API key，强制绑 Firebase 项目。结果是：

> 抽象在接口层成立，唯一实现却带强依赖 → 用户被锁死在单一云厂商。

KuiklyAI 用三条约束规避：

| 约束 | 做法 | 如何机械验证 |
|------|------|-------------|
| 内置实现不含厂商 SDK | 内置 provider 只依赖 `LlmTransport`，不 import 厂商包 | 门禁脚本扫 import |
| 传输层也是注入点 | `LlmTransport` 由宿主提供，库内无 HTTP | `commonMain` 无网络依赖 |
| 抽象须被多个实现验证 | 首版交付 ≥2（推荐 3）种形态迥异的实现 | Code Review + 文档声明 |

---

## 技术方案

### 涉及的模块

全部放在：

`KuiklyChat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/`

放在 `chat/` 下是为了继承现有 `scripts/check-layer-deps.sh` 覆盖范围（脚本只扫 `$CHAT_ROOT`）。

```
kuiklyai/
├── provider/     # 契约 + 传输抽象 + 内置实现（无 Compose、无平台 API）
├── history/      # 会话 JSON 序列化（不碰存储）
├── platform/     # 附件选择 / 语音识别（仅接口）
├── runtime/      # LlmChatController：Compose 状态 + 发送/取消/重试
└── ui/           # KuiklyAiChatView 及子组件（Compose DSL）
```

分层依赖（单向向下）：

```
ui/  →  runtime/  →  provider/ + history/ + platform/
                         ↓ 复用（不修改）
              agui/（协议）    agentcard_compose/（富卡片）
```

| 目录 | 建议文件 | 职责 |
|------|---------|------|
| `provider/` | `LlmChatMessage.kt` | 消息 / 附件 / 状态模型 |
| | `LlmProvider.kt` | 核心契约 + `LlmErrorCode` |
| | `LlmTransport.kt` | 传输抽象 + `SseDecoder` |
| | `BaseLlmProvider.kt` | 骨架：历史簿记 + 流式累积 |
| | `EchoLlmProvider.kt` | 零依赖本地实现（Demo / 单测） |
| | `OpenAiCompatibleProvider.kt` | 通用 OpenAI 兼容端点 |
| | `OpenAiWireFormat.kt` | 协议编解码（纯函数，可单测） |
| | `AgUiLlmProvider.kt` | 桥接现有 `agui/`（可选，见开放问题） |
| `history/` | `LlmChatSerializer.kt` | 历史 JSON 序列化 |
| `platform/` | `LlmAttachmentPicker.kt` / `LlmSpeechRecognizer.kt` | 平台能力接口 |
| `runtime/` | `LlmChatController.kt` + remember 工厂 | UI 状态持有 |
| `ui/` | `KuiklyAiChatView.kt` 及子组件 | Compose DSL 界面 |

约束：单文件 ≤ 400 行（门禁规则 5）；新增逻辑必须有 `commonTest`（只用 `kotlin("test")`）。

### 数据模型变更

不复用 `chat/model/ChatMessage`：IM 字段（`reactions` / `threadCount` / `readBy`）对人机对话是噪音；`STREAMING` / `error` 又是 IM 模型没有的。两套模型各自干净。

```kotlin
enum class LlmMessageOrigin { USER, LLM }

enum class LlmMessageState { STREAMING, DONE, FAILED, CANCELED }

enum class LlmAttachmentKind { IMAGE, FILE, AUDIO }

data class LlmAttachment(
    val id: String,
    val kind: LlmAttachmentKind,
    val name: String = "",
    val mimeType: String = "",
    val url: String = "",
    val bytesBase64: String = "", // 多模态内联；序列化时默认不落盘
    val sizeBytes: Long = 0L,
)

data class LlmChatMessage(
    val id: String,
    val origin: LlmMessageOrigin,
    val text: String = "",
    val attachments: List<LlmAttachment> = emptyList(),
    val createdAt: Long = 0L,
    val state: LlmMessageState = LlmMessageState.DONE,
    val error: String = "",
    val extra: Map<String, String> = emptyMap(),
)
```

### 核心 API 变更

#### `LlmProvider`（对外扩展点）

与 Flutter `LlmProvider` 逐方法对齐，降低迁移心智成本：

```kotlin
interface LlmProvider {
    val history: List<LlmChatMessage>
    fun setHistory(messages: List<LlmChatMessage>)

    /** 一次性生成，不读写历史 */
    fun generateStream(
        prompt: String,
        attachments: List<LlmAttachment> = emptyList(),
    ): Flow<String>

    /** 多轮对话，读写历史；Flow 发射增量片段，非累积全文 */
    fun sendMessageStream(
        prompt: String,
        attachments: List<LlmAttachment> = emptyList(),
    ): Flow<String>

    fun addHistoryListener(listener: LlmHistoryListener): LlmSubscription
}
```

宿主最小接入：

```kotlin
@Composable
fun MyAiPage() {
    val provider = remember { EchoLlmProvider() } // 换成任意实现即可
    KuiklyAiChatView(provider = provider)
}
```

#### 传输抽象（规避厂商绑定的关键）

```kotlin
data class LlmHttpRequest(
    val url: String,
    val method: String = "POST",
    val headers: Map<String, String> = emptyMap(),
    val body: String = "",
    val timeoutMs: Long = 60_000L,
)

interface LlmStreamSink {
    fun onChunk(data: String)
    fun onComplete()
    fun onError(code: LlmErrorCode, message: String)
}

fun interface LlmTransport {
    fun openStream(request: LlmHttpRequest, sink: LlmStreamSink): LlmSubscription
}
```

宿主可用 Kuikly `HttpRequestModule` / Ktor / OkHttp / URLSession 任选实现。库内提供 `streamAsFlow()` 把回调桥成可取消冷 Flow。

`SseDecoder` 单独成类：网络 chunk 边界任意，`data:` 行可能被拆碎；按 chunk 直接解析 JSON 会偶发失败且难复现。

#### 出站协议（库主动适配，非改现有入站 API）

```
POST {baseUrl}/chat/completions          # OpenAiCompatibleProvider
  Request:  { model, stream: true, messages: [...] }
  Response: text/event-stream
            data: {"choices":[{"delta":{"content":"..."}}]}

POST {endpoint}                          # AgUiLlmProvider（若纳入首版）
  Request:  AG-UI RunAgentInput { threadId, runId, messages }
  Response: text/event-stream（AgUiEvent，复用 AgUiEventParser）
```

**不改动**现有 `ChatSession` / `AgUiSession` / `ChatRepository` 对外签名。

### UI 组成（Compose DSL）

| 组件 | 职责 |
|------|------|
| `KuiklyAiChatView` | 主入口：欢迎区 / 消息列表 / 错误条 / 输入区 |
| `LlmMessageList` | sticky-bottom；用户上滑读历史时不打断 |
| `LlmMessageBubbles` | 用户气泡 + 模型 Markdown（复用 `StreamingMarkdownView`） |
| `LlmChatInput` | 文本 + 可选附件 / 语音入口；生成中变为「停止」 |
| `LlmWelcomeSection` | 欢迎语 + suggestions 胶囊 |
| `LlmChatViewStyle` | 颜色 / 圆角 / 间距；显式传参，不用 CompositionLocal |

平台能力未注入时：**不显示**对应按钮（避免点了没反应）。

### 门禁增量（实现阶段才改脚本）

审批通过并进入实现后，在 `scripts/check-layer-deps.sh` 增加：

```bash
# 规则6：kuiklyai/provider 不得依赖任何 UI 包
#        ❌ / ✅ FIX / 📖 See: docs/architecture/boundaries.md

# 规则7：kuiklyai 不得依赖厂商 SDK（firebase / openai / anthropic / google.ai 等）
#        ❌ / ✅ FIX / 📖 See: docs/design/feature-kuiklyai-toolkit.md
```

同步更新 `docs/conventions/mechanical-rules.md`。

---

## 与 Flutter AI Toolkit 能力对照

| Flutter AI Toolkit | KuiklyAI 首版（草案） | 说明 |
|---|---|---|
| `LlmChatView` | `KuiklyAiChatView` | 完整对话界面 |
| `LlmProvider` | `LlmProvider` | 逐方法对齐 |
| `FirebaseProvider`（绑 Firebase） | Echo + OpenAI 兼容（+ 可选 AG-UI） | **核心差异** |
| Multiturn / Streaming / Rich text | ✅ | 富文本复用现有 Compose 卡片与 Markdown |
| Voice / Attachments | 接口 + 宿主实现 | 库内不绑 SDK |
| Custom styling | `LlmChatViewStyle` | |
| Chat serialization | `LlmChatSerializer` | 只序列化，不存储 |
| Custom response widgets | Composable `messageBuilder` slot | |
| Function calling | 展示可走 AG-UI；不做执行 | 非目标 |
| Cross-platform | Android / iOS / 鸿蒙 / JS… | 随 `:KuiklyChat` targets |

---

## 验收标准

具体、可验证：

1. **零后端可运行**：Demo（或预览页）用 `EchoLlmProvider`，无网络、无 API key 完成一轮流式对话。
2. **provider 可替换**：切换 provider 只改构造，UI 代码零改动。
3. **无厂商依赖**：`kuiklyai/` 无厂商 SDK import；门禁规则 7 通过。
4. **分层不倒挂**：`kuiklyai/provider/` 不 import UI 包；门禁规则 6 通过。
5. **流式贴底**：长回复自动贴底；用户上滑读历史时不被打断。
6. **取消真断流**：点「停止」后底层订阅 cancel，不再有 token。
7. **弱网分帧**：`SseDecoder` 在 `data:` 被任意切分时仍正确（单测含逐字节切分）。
8. **错误可分类**：鉴权失败与限流文案不同；仅 `LlmErrorCode.retryable == true` 显示重试。
9. **历史可往返**：`decode(encode(x))` 等价于 `x`（`bytesBase64` 除外，见取舍）；恢复的 `STREAMING` 落为 `CANCELED`。
10. **测试**：`commonTest` 覆盖 `SseDecoder`、`OpenAiWireFormat`、`LlmChatSerializer`、`BaseLlmProvider`；只用 `kotlin("test")`。
11. **门禁全绿**：`bash scripts/check-layer-deps.sh` + `:KuiklyChat:compileDebugKotlinAndroid` + `:KuiklyChat:testDebugUnitTest`。

---

## 已知取舍（请评审确认）

1. **`bytesBase64` 默认不参与序列化**
   一张图能把历史撑到数 MB。恢复对话用 `url` 展示；需要原始字节由宿主另存。

2. **`LlmProvider` 契约是 `Flow<String>`，承载不了结构化事件**
   工具调用 / 思考过程塞不进纯文本流。若纳入 `AgUiLlmProvider`，通过额外 `onEvent: (AgUiEvent) -> Unit` 透传富事件，文本仍走标准契约。
   备选：首版就改成 `Flow<LlmStreamEvent>` 密封类——更干净，但是相对 Flutter 的破坏性差异，且影响所有实现。

3. **`commonMain` 无系统时钟**
   时间戳靠宿主注入 `LlmClock`，默认 `0`（UI 遇 0 不显示，而非假 1970）。

---

## 落地决策

| # | 议题 | 选项 A（推荐） | 选项 B | 影响 |
|---|------|----------------|--------|------|
| Q1 | 包路径 / 模块 | `chat/kuiklyai/`，不新建 Gradle 模块 | 新建 `:KuiklyAi` 模块 | B 需改 settings / publish / 门禁扫路径 |
| Q2 | 首版内置 provider | Echo + OpenAI 兼容 + AG-UI 三个都要 | 只做 Echo + OpenAI；AG-UI 二期 | AG-UI 涉及 `onEvent` 逃生口是否进契约 |
| Q3 | 流式契约 | `Flow<String>` + AG-UI `onEvent` | `Flow<LlmStreamEvent>` 密封类 | B 更干净但偏离 Flutter、实现成本更高 |
| Q4 | 已有草稿代码 | 审批通过后对照本文档审查，能留则留、不能留则删 | 先全部删掉，从零按文档实现 | A 更快；B 更干净、避免「未审批代码」惯性 |
| Q5 | Demo 页 | `shared` 增加 `KuiklyAiDemoPage`，Router 入口 | 首版只库内 + 单测，Demo 二期 | 影响「零后端可运行」的演示载体 |
| Q6 | 命名 | 对外入口 `KuiklyAiChatView`，包名 `kuiklyai` | 入口叫 `LlmChatView`，更贴 Flutter | B 易与现有 Chat 命名撞车，需确认 |

---

## 依赖

| 依赖 | 状态 |
|------|------|
| `feature-agui-protocol` | ✅ 已实现（仅当 Q2 选「含 AG-UI」时硬依赖） |
| `feature-agent-card-system` | ✅ 已实现（富卡片复用） |
| `feature-streaming-ai-text` | ✅ 已实现（`StreamingMarkdownView`） |
| `com.tencent.kuikly:compose` | ✅ 已在 `:KuiklyChat` |
| `kotlinx-coroutines-core` / `kotlinx-serialization-json` | ✅ 已在 `:KuiklyChat` |

**本功能不新增第三方 Maven 依赖。**

---

## 实现结果

1. ✅ 审查并修正原型代码，保留符合设计的实现。
2. ✅ 冻结 `provider/` + `history/` + `platform/` 契约并补齐 commonTest。
3. ✅ 实现 `runtime/` + `ui/`，接入零后端 Echo Demo。
4. ✅ 落地门禁规则 6/7，并更新 `mechanical-rules.md`。
5. ✅ 架构检查、Android/JS/iOS/macOS 编译、Demo 宿主编译与 140 个 Android 单测通过。

---

## 评审记录

| 日期 | 结论 | 备注 |
|------|------|------|
| 2026-07-27 | ✅ Implemented | Q1–Q6 均采用推荐方案 A；已知取舍按文档确认。 |
