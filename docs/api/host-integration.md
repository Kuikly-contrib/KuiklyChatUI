---
last_updated: 2026-11-09
status: active
owner: @kuikly-chat-team
---

# 宿主按需接入（1.0.5）

当前发布线是 **1.0.x**，本次版本为 `1.0.5`。宿主按模块坐标接入；public keep-set 是文本基线，不是 binary ABI 承诺。

版本坐标：`1.0.5-2.1.21`（标准平台）/ `1.0.5-2.0.21-KBA-010`（鸿蒙）。patch 号依据已发布版本递增，以 `gradle.properties` 的 `MAVEN_VERSION` + Kotlin 版本为准。已发布构件见外网仓库 `https://mirrors.tencent.com/repository/maven/kuikly-open/`。

标准平台 Compose Markdown 使用外网公开的 `com.tencent.kuiklybase:markdown:0.4.1`；鸿蒙使用 `0.4.1-ohos`。Kuikly DSL 会话页使用外网公开的 `com.tencent.kuiklybase:KuiklyMarkdown:1.0.6-2.1.21`。发布模块不得再携带 `*-internal*` 依赖。

## 选模块

| 你要什么 | 依赖 |
|----------|------|
| 只要 Compose Chat UI（`ChatScreen` / `ChatController`） | `com.tencent.kuiklybase:kuiklychat-compose` |
| 只要 Kuikly DSL 会话页（`ChatControllerSession` / `ChatDockComposer`） | `com.tencent.kuiklybase:kuiklychat-compat` |
| LLM provider（`LlmProvider` / `EchoLlmProvider`） | 再加 `com.tencent.kuiklybase:kuiklychat-ai` |
| Android 原生图片与字节缓存 | 再加 `com.tencent.kuiklybase:KuiklyChatAndroid` |
| Preview / Mock（`ChatPreviewData`） | 再加 `com.tencent.kuiklybase:kuiklychat-testing` |

`kuiklychat-core` 会被 compose / compat 传递出来，一般不必直接写。当前发布脚本发布 `core / compose / compat / ai / testing` 五个 KMP 模块与 `KuiklyChatAndroid`，没有过渡伞模块。

宿主仓库至少包含 `https://mirrors.tencent.com/repository/maven/kuikly-open/` 与 `https://mirrors.tencent.com/nexus/repository/maven-tencent/`：前者提供 ChatUI 与公开 Markdown 产物，后者提供 KBA 专用 kotlinx 产物。底部输入区不需要传 `pagerData.safeAreaInsets.bottom`；默认值 `0f`，键盘跟随由组件内部处理。

## 标准平台

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                // 只做 Compose UI
                implementation("com.tencent.kuiklybase:kuiklychat-compose:1.0.5-2.1.21")

                // 只要 Kuikly DSL 会话页时改用：
                // implementation("com.tencent.kuiklybase:kuiklychat-compat:1.0.5-2.1.21")

                // KuiklyAI / Android 原生能力按需追加：
                // implementation("com.tencent.kuiklybase:kuiklychat-ai:1.0.5-2.1.21")
                // implementation("com.tencent.kuiklybase:KuiklyChatAndroid:1.0.5-2.1.21")

                // Preview / Mock 必须显式写：
                // implementation("com.tencent.kuiklybase:kuiklychat-testing:1.0.5-2.1.21")
            }
        }
    }
}
```

鸿蒙把版本换成 `1.0.5-2.0.21-KBA-010`，Maven 仓库与 `README.md` 鸿蒙段相同。

本地工程：`implementation(project(":kuiklychat-compose"))` 等同替换即可。

## 迁移注意

- **消息模型只有一套。** Compose 与 Kuikly DSL 都用 `compose.core.ChatMessage` + 有序 `MessagePart`。Kuikly 会话页可用 `sessionChatMessage(...)` / `ChatMessageHelper`。不要再造第二套信封。
- **聊天 UI 各留一套。** Kuikly DSL 用 `ChatControllerSession`，Compose 用 `ChatScreen`，两边共用 `ChatController`。入口说明见 [chat-ui.md](chat-ui.md)。
- **Channel 不要再写 `attr {}` / `event {}`。** View 叶子已 internal。宿主走 `ChatChannelList`、`ChatChannelHeader { ... }`。
- **`MessagePart.AgentCardContent` 先留着，不要再往 Core 塞卡片字段。** 纯文本宿主也会带上这个类型；中期再考虑 `Custom` 或可选模块。
- **Preview / Mock 需要显式依赖 `:kuiklychat-testing`。**
