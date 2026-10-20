---
last_updated: 2026-09-23
status: active
owner: @kuikly-chat-team
---

# 宿主按需接入（0.x experimental）

当前发布线是 **0.x experimental**，不是 Stable。卡片工厂、Slot、AG-UI 映射仍然偏大；先让真实宿主按模块坐标接入，再按他们实际用到的 API 砍 keep-set。不要把现在的 public dump 标成 Stable，也不要再拆 Gradle 模块或改一轮 Config API。

版本坐标示例：`0.1.0-experimental-2.1.21`（标准平台）/ `0.1.0-experimental-2.0.21-KBA-010`（鸿蒙）。以 `gradle.properties` 的 `MAVEN_VERSION` + Kotlin 版本为准。

## 选模块

| 你要什么 | 依赖 |
|----------|------|
| 只要 Compose Chat UI（`ChatScreen` / `ChatController`） | `com.tencent.kuiklybase:kuiklychat-compose` |
| 只要 Kuikly DSL 会话页（`ChatControllerSession` / `ChatDockComposer`） | `com.tencent.kuiklybase:kuiklychat-compat` |
| AG-UI 协议与 Compose adapter | 再加 `com.tencent.kuiklybase:kuiklychat-agui` |
| Agent 卡片双 DSL 渲染 | 再加 `com.tencent.kuiklybase:kuiklychat-agentcard` |
| LLM provider（`LlmProvider` / `EchoLlmProvider`） | 再加 `com.tencent.kuiklybase:kuiklychat-ai` |
| Preview / Mock（`ChatPreviewData`） | 再加 `com.tencent.kuiklybase:kuiklychat-testing` |
| 过渡全家桶（**不含 testing**） | `com.tencent.kuiklybase:KuiklyChat` |

`kuiklychat-core` 会被 compose / compat 传递出来，一般不必直接写。`KuiklyChat` 是旧坐标的过渡伞：core + compose + compat + agui + agentcard + ai。新宿主按需依赖；旧宿主可以暂时继续用伞，但不要指望它再带上 Mock。

## 标准平台

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // 只做 Compose UI
                implementation("com.tencent.kuiklybase:kuiklychat-compose:0.1.0-experimental-2.1.21")

                // 只要 Kuikly DSL 会话页时改用：
                // implementation("com.tencent.kuiklybase:kuiklychat-compat:0.1.0-experimental-2.1.21")

                // 协议 / 卡片 / KuiklyAI 按需追加：
                // implementation("com.tencent.kuiklybase:kuiklychat-agui:0.1.0-experimental-2.1.21")
                // implementation("com.tencent.kuiklybase:kuiklychat-agentcard:0.1.0-experimental-2.1.21")
                // implementation("com.tencent.kuiklybase:kuiklychat-ai:0.1.0-experimental-2.1.21")

                // Preview / Mock 必须显式写，不会随伞形传递：
                // implementation("com.tencent.kuiklybase:kuiklychat-testing:0.1.0-experimental-2.1.21")
            }
        }
    }
}
```

鸿蒙把版本换成 `0.1.0-experimental-2.0.21-KBA-010`，Maven 仓库与 `README.md` 鸿蒙段相同。

本地工程：`implementation(project(":kuiklychat-compose"))` 等同替换即可。

## 迁移注意

- **消息模型只有一套。** Compose 与 Kuikly DSL 都用 `compose.core.ChatMessage` + 有序 `MessagePart`。Kuikly 会话页可用 `sessionChatMessage(...)` / `ChatMessageHelper`。不要再造第二套信封。
- **聊天 UI 各留一套。** Kuikly DSL 用 `ChatControllerSession`，Compose 用 `ChatScreen`，两边共用 `ChatController`。入口说明见 [chat-ui.md](chat-ui.md)。
- **双 DSL 卡片渲染会保留。** 新卡片只扩展共享 `AgentCard` 模型，再在两套渲染层实现。
- **Channel / BouncingDots 不要再写 `attr {}` / `event {}`。** View 叶子已 internal。宿主走 `ChatChannelList`、`ChatChannelHeader { ... }`、`BouncingDots { ... }`。
- **`MessagePart.AgentCardContent` 先留着，不要再往 Core 塞卡片字段。** 纯文本宿主也会带上这个类型；中期再考虑 `Custom` 或可选模块。
- **testing 不进伞形 `api()`。** 需要 Preview 就显式依赖 `:kuiklychat-testing`。
