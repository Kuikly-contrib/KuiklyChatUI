# KuiklyChat AG-UI 协议接入指南

> 本文档面向需要在 Kuikly 项目中接入 AG-UI（Agent-User Interaction Protocol）的开发者。
> 涵盖协议规范、已实现的组件清单、使用场景决策以及完整的接入示例。

---

## 一、AG-UI 协议规范

### 1.1 协议概述

AG-UI 是开源的 Agent-User Interaction Protocol，采用**事件驱动架构**。AI Agent 与前端 UI 之间通过标准化事件流通信，支持 SSE / WebSocket / HTTP 等多种传输方式。

### 1.2 事件类型（16 种标准 + 扩展）

| 分类 | 事件类型 | 说明 | 对应 UI 效果 |
|------|---------|------|-------------|
| **生命周期** | `RUN_STARTED` | Agent 开始运行 | 顶部 Loading 条激活 |
| | `RUN_FINISHED` | Agent 运行完成 | Loading 条消失，未闭合消息标记为完成 |
| | `RUN_ERROR` | Agent 运行出错 | 产出红色错误卡片 |
| | `STEP_STARTED` | 进入新步骤 | Loading 副标题更新 |
| | `STEP_FINISHED` | 步骤完成 | 步骤计数 +1 |
| **文本消息** | `TEXT_MESSAGE_START` | 开始输出文本 | 创建新消息占位 |
| | `TEXT_MESSAGE_CONTENT` | 文本增量 | 流式追加到消息内容 |
| | `TEXT_MESSAGE_END` | 文本输出结束 | 消息标记为完成 |
| | `TEXT_MESSAGE_CHUNK` | 便捷合并事件 | 自动拆分处理 |
| **工具调用** | `TOOL_CALL_START` | 开始调用工具 | 创建工具调用 Loading 占位 |
| | `TOOL_CALL_ARGS` | 参数增量 | 流式拼接参数 |
| | `TOOL_CALL_END` | 工具调用结束 | 参数拼接完成 |
| | `TOOL_CALL_RESULT` | 工具返回结果 | 产出工具结果卡片 |
| | `TOOL_CALL_CHUNK` | 便捷合并事件 | 自动拆分处理 |
| **状态管理** | `STATE_SNAPSHOT` | 状态全量快照 | 内部状态字典更新 |
| | `STATE_DELTA` | 状态增量（JSON Patch） | 应用 patch 到状态字典 |
| | `MESSAGES_SNAPSHOT` | 消息列表快照 | 全量替换消息列表 |
| **Activity** | `ACTIVITY_SNAPSHOT` | 活动快照 | 产出/更新 Activity 卡片 |
| | `ACTIVITY_DELTA` | 活动增量 | 更新现有 Activity 卡片 |
| **Reasoning** | `REASONING_START/END` | 推理开始/结束 | 创建推理上下文 |
| | `REASONING_MESSAGE_*` | 推理消息流 | 产出推理过程卡片 |
| **特殊** | `CUSTOM` | 业务自定义事件 | 走 `EventToCardMapper` 或 HandlerRegistry |
| | `META` | 元事件（点赞/标签等） | Toast 提示 |
| | `RAW` | 原始透传 | 忽略或日志 |

### 1.3 事件消费方式

```kotlin
// 方式一：直接分发事件对象
session.dispatch(AgUiEvent.TextMessageChunk(messageId = "msg_1", delta = "你好"))

// 方式二：分发 JSON 字符串（SSE 场景最常用）
session.dispatchJson("""{"type":"TEXT_MESSAGE_CHUNK","messageId":"msg_1","delta":"你好"}""")

// 方式三：分发 SSE 单行数据
session.dispatchSseData("data: {\"type\":\"TEXT_MESSAGE_CONTENT\",...}")
```

### 1.4 消息角色（Role）

| 角色 | 常量 | 说明 |
|------|------|------|
| 用户 | `AgUiRole.USER` | 人类用户发送的消息 |
| 助手 | `AgUiRole.ASSISTANT` | AI Agent 的回复 |
| 系统 | `AgUiRole.SYSTEM` | 系统提示/指令 |
| 工具 | `AgUiRole.TOOL` | 工具调用的返回结果 |
| 开发者 | `AgUiRole.DEVELOPER` | 开发者注入的上下文 |
| 活动 | `AgUiRole.ACTIVITY` | Agent 活动状态 |
| 推理 | `AgUiRole.REASONING` | 推理过程内容 |

---

## 二、已实现的 UI 组件清单

### 2.1 基础消息组件（原有）

| 组件 | 类型 | 说明 |
|------|------|------|
| `ChatBubble` | 文本气泡 | 支持左右布局、头像、状态、引用回复 |
| `ChatImageBubble` | 图片/视频气泡 | 支持缩略图、播放按钮、尺寸自适应 |
| `ChatSystemMessage` | 系统消息 | 居中灰色提示文字 |
| `ChatDateSeparator` | 日期分隔符 | 时间分组展示 |
| `ChatTypingIndicator` | 正在输入 | 三个跳动圆点 + 文字 |

### 2.2 AG-UI 富卡片组件（新增）

| 组件 | `RichMessageKind` | 触发场景 | 视觉特征 |
|------|-------------------|---------|---------|
| **Loading 占位** | `loading` | `showLoading()` / 工具调用等待 | 四种风格：dots / spinner / progress / skeleton |
| **代码块** | `code_block` | AI 返回 ```language 代码 | 深色背景 + 语言标签 + 文件名 + 复制按钮 |
| **Diff 对比** | `diff` | AI 返回 ```diff 或 diff 格式 | 行级着色：+绿色 / -红色 / @@灰色 |
| **命令执行** | `command_exec` | Agent 执行 shell/python | 终端风格 + 状态点 + 耗时 + exit code |
| **文件编辑** | `file_edit` | Agent 修改文件 | 操作徽章 + +N/-N 差异统计 + diff 预览 |
| **文件读取** | `file_read` | Agent 读取文件 | 蓝色徽章 + 文件内容预览 |
| **工具结果** | `tool_result` | `TOOL_CALL_RESULT` | 🔧 图标 + 工具名徽章 + 参数 + 结果 |
| **工具错误** | `tool_error` | 工具调用失败 | ⚠️ 红色卡片 + 错误信息 |
| **错误卡片** | `error` | `RUN_ERROR` | ❌ 红色卡片 + 错误码徽章 |
| **建议选项** | `suggestions` | 快捷回复推荐 | 💡 绿色 chips，点击触发回调 |
| **表格** | `table` | Markdown \| 表格 \| | 📊 网格布局 + 表头高亮 + 斑马纹 |
| **引用块** | `quote` | Markdown `> ...` | 💬 蓝色竖条 + 斜体文字 |
| **折叠内容** | `collapsible` | HTML `<details>` | ▶ 摘要标题 + 可展开正文 |
| **任务清单** | `checklist` | Markdown `- [ ]` / `- [x]` | ✅/⬜ 复选框 + 完成进度 + 删除线 |
| **Activity** | `activity` | `ACTIVITY_SNAPSHOT` | 📋 步骤清单 + 进度条 |
| **Reasoning** | `reasoning` | `REASONING_MESSAGE_*` | 🧠 灰色虚线边框 + 推理过程 |

### 2.3 组件注册方式

```kotlin
ChatSession({ session.messages }) {
    // 一键注册所有 AG-UI 富卡片渲染器
    messageRenderers.addAll(agUiRichRenderers(
        onCopyCode = { code -> /* 接入剪贴板 */ },
        onSuggestionClick = { value -> sendMessage(value) }
    ))
}
```

---

## 三、使用场景：何时用组件 vs 纯文本

### 3.1 决策树

```
AI 返回的内容是什么格式？
    │
    ├── Markdown 混合文本（含表格/代码/引用等）
    │       └── 使用 MarkdownBlockExtractor 拆分
    │           ├── 普通段落 → 纯文本气泡（KuiklyMarkdown 渲染）
    │           ├── 代码块 → CodeBlockCardRenderer
    │           ├── diff → DiffCardRenderer
    │           ├── 表格 → TableCardRenderer
    │           ├── 引用 → QuoteCardRenderer
    │           ├── 折叠 → CollapsibleCardRenderer
    │           └── 清单 → ChecklistCardRenderer
    │
    ├── 结构化数据（JSON / 工具结果）
    │       └── 使用 RichCardPayload 构造特定卡片
    │           ├── 工具调用 → toolResult / toolError
    │           ├── 命令执行 → commandExec
    │           ├── 文件操作 → fileEdit / fileRead
    │           └── 错误 → error
    │
    ├── 流式文本（打字机效果）
    │       └── 使用 AiMessageTextView（KuiklyStreamingMarkdown）
    │
    └── 状态/进度信息
            └── 使用 Loading 体系
                ├── 会话级 Loading → showLoading()
                ├── 步骤级 Loading → updateLoading()
                └── 工具级 Loading → TOOL_CALL_START 自动触发
```

### 3.2 场景对照表

| 场景 | 推荐组件 | 不推荐 |
|------|---------|--------|
| AI 解释概念、回答问题 | 纯文本气泡（Markdown 渲染） | 代码块卡片 |
| AI 生成代码示例 | 代码块卡片（带语言标签+复制） | 纯文本气泡 |
| AI 展示代码审查结果 | Diff 卡片（行级着色） | 纯文本或代码块 |
| AI 执行命令并返回输出 | 命令执行卡片（终端风格） | 纯文本气泡 |
| AI 修改项目文件 | 文件编辑卡片（+N/-N 统计） | 纯文本 |
| AI 调用外部工具 | 工具结果卡片（🔧 徽章） | 纯文本 |
| AI 返回表格数据 | 表格卡片（网格布局） | Markdown 表格（渲染效果差） |
| AI 引用外部资料 | 引用块卡片（蓝色竖条） | 普通文本 |
| AI 提供详细说明（可折叠） | 折叠内容卡片 | 长文本直接展示 |
| AI 给出任务清单 | 任务清单卡片（☑️/⬜） | 普通列表 |
| AI 思考过程中 | Loading 占位 + 推理卡片 | 空白等待 |
| AI 运行出错 | 错误卡片（红色+错误码） | 普通文本 |
| AI 推荐快捷操作 | 建议选项卡片（chips） | 纯文本列举 |

---

## 四、快速接入示例

### 4.1 最小接入（SSE 后端）

```kotlin
class AgentChatPage : BasePager() {
    private val session = AgUiSession(threadId = "thread_001")

    override fun body(): ViewBuilder = {
        ChatSession({ session.messages }) {
            title = "AI 助手"
            messageRenderers.addAll(agUiRichRenderers())
            onSend = { text ->
                // 1. 显示用户消息
                session.messages.add(ChatMessage(
                    id = generateId(), content = text,
                    isSelf = true, senderName = "我"
                ))
                // 2. 发送并消费 SSE
                launch {
                    sseClient.send(text).collect { line ->
                        session.dispatchSseData(line)
                    }
                }
            }
        }
    }
}
```

### 4.2 Markdown 混合内容拆分

```kotlin
// 当 AI 返回包含表格、代码、diff 的长 Markdown 时：
val blocks = MarkdownBlockExtractor.extract(
    markdown = aiResponse,
    baseId = "msg_${runCounter++}",
    senderName = session.aiSenderName,
    senderAvatar = session.aiAvatarUrl
)
blocks.forEach { session.upsertMessage(it) }
// 结果：自动拆分为 5-6 条独立消息，每条用对应卡片渲染
```

### 4.3 自定义非标准事件

```kotlin
// 注册映射器：将业务平台的自定义事件转为卡片
session.registerMapper(EventToCardMapper { event, ctx ->
    if (event is AgUiEvent.Custom && event.name == "deploy_status") {
        val map = event.value as? Map<*, *> ?: return@EventToCardMapper null
        RichCardPayload.commandExec(
            id = "deploy_${ctx.currentRunId}",
            command = map["cmd"] as String,
            output = map["output"] as String,
            state = if (map["success"] as Boolean) CommandState.SUCCESS else CommandState.FAILED
        )
    } else null
})
```

### 4.4 Loading 状态管理

```kotlin
// 显示 Loading
session.showLoading("thinking", "正在思考", "拆解需求")

// 更新子任务
session.updateLoading("thinking", subtitle = "查询上下文")

// 完成后替换为实际内容
session.dismissLoading("thinking", replaceWith =
    RichCardPayload.markdown(id = "thinking", markdown = "分析完成！")
)

// 订阅 Loading 变化驱动 UI
session.onLoadingChanged { snapshot ->
    topBarText = snapshot.bestLabel      // "正在思考 · 查询上下文"
    topBarActive = snapshot.isActive     // true / false
}
```

---

## 五、文件结构速查

```
KuiklyChat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/agui/
├── AgUiEvent.kt              # 16+ 种事件密封类定义
├── AgUiEventParser.kt        # JSON → AgUiEvent 解析器
├── AgUiSession.kt            # 核心状态机（事件 → ChatMessage）
├── AgUiHandlerRegistry.kt    # 自定义事件处理器注册表
├── AgUiLoadingState.kt       # Loading 状态快照与监听器
├── AgUiTypes.kt              # 协议数据模型（Message/Tool/Context）
├── EventToCardMapper.kt      # 事件 → 卡片映射器扩展点
├── RichMessageKind.kt        # 卡片类型常量 + Extra Keys
├── RichCardPayload.kt        # 类型安全的卡片构造器
└── MarkdownBlockExtractor.kt # Markdown 混合内容拆分器

KuiklyChat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/bubble/
├── RichCardRenderers.kt      # 12 种富卡片渲染器实现
└── MessageRendererFactory.kt # 渲染器接口（业务方可自定义）
```

---

## 六、向后兼容与扩展

### 6.1 新增卡片类型（三步）

1. **定义 Kind 常量**：`RichMessageKind` 中新增 `const val NEW_TYPE = "new_type"`
2. **构造 Payload**：`RichCardPayload` 中新增 `fun newType(...)` 构造器
3. **实现渲染器**：继承 `MessageRendererFactory`，实现 `canRender` + `render`
4. **注册**：`agUiRichRenderers()` 中添加到返回列表

### 6.2 协议升级容错

- 未知事件类型自动落入 `AgUiEvent.Unknown`
- 通过 `handlerRegistry.unknownEventHandler` 拦截处理
- 所有事件保留 `rawEvent` 字段，便于调试和透传

---

## 七、参考链接

- AG-UI 官方文档：https://docs.ag-ui.com/concepts/events
- TDesign React Chat AG-UI：https://tdesign.tencent.com/react-chat/agui
- KuiklyChat README（项目内）：`README.md` 中「AG-UI 协议集成」章节
