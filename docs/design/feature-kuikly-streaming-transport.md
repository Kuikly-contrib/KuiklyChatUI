---
last_updated: 2026-10-20
status: active
owner: @kuikly-chat-team
---

# Kuikly Chat 的 SSE 长连接与流式输出

Kuikly 的
[NetworkModule](https://github.com/Tencent-TDS/KuiklyUI/blob/main/core/src/commonMain/kotlin/com/tencent/kuikly/core/module/NetworkModule.kt)
提供流式 HTTP 文本请求。Kuikly Chat 用它接入 POST + SSE：网络层持续回调响应文本，
协议层把任意网络分片还原成 SSE 事件，`ChatController` 再把文本增量追加到同一个消息气泡里。

[ChatComponentDemoPage](../../shared/src/commonMain/kotlin/com/kuikly/kuiklychat/dsl/chat/ChatComponentDemoPage.kt)
支持两种运行方式：传入模型配置时使用真实 POST + SSE；未配置时使用本地脚本流，方便在
没有密钥或网络时检查流式 Markdown UI。两种方式都不创建会话仓库，不恢复或落盘聊天记录。
真实模型调用使用无状态的 `generateStream`：每次只发送当前输入，不保存 provider 对话
历史，也不把前几轮消息带入下一次请求。页面关闭后，当前消息随 Controller 一起释放。

相关演示：

- [KuiklyChat Chat Demo](https://github.com/Kuikly-contrib/KuiklyChatUI/blob/main/shared/src/commonMain/kotlin/com/kuikly/kuiklychat/dsl/chat/ChatComponentDemoPage.kt)
- [KuiklyUI 主干 SSE Demo](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/SSETypewriterDemoPage.kt)

<div align="center">
  <img src="../../assets/chat-sse.jpg" alt="Kuikly Chat SSE 流式消息演示" width="320">
</div>

## Kuikly 仓库中的实现位置

流式请求由 common API 和各平台 renderer 共同完成：

- common API：
  [core/.../NetworkModule.kt](https://github.com/Tencent-TDS/KuiklyUI/blob/main/core/src/commonMain/kotlin/com/tencent/kuikly/core/module/NetworkModule.kt)
- Android：
  [core-render-android/.../KRNetworkModule.kt](https://github.com/Tencent-TDS/KuiklyUI/blob/main/core-render-android/src/main/java/com/tencent/kuikly/core/render/android/expand/module/KRNetworkModule.kt)
- iOS：
  [core-render-ios/Extension/Modules/KRNetworkModule.m](https://github.com/Tencent-TDS/KuiklyUI/blob/main/core-render-ios/Extension/Modules/KRNetworkModule.m)
- HarmonyOS：
  [core-render-ohos/src/main/ets/modules/KRNetworkModule.ets](https://github.com/Tencent-TDS/KuiklyUI/blob/main/core-render-ohos/src/main/ets/modules/KRNetworkModule.ets)
- 使用示例：
  [demo/.../SSETypewriterDemoPage.kt](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/SSETypewriterDemoPage.kt)

本仓库调试源码版 Kuikly 时，可以通过环境变量替换 `core` 与 Android renderer：

```bash
KUIKLY_UI_SOURCE=/path/to/KuiklyUI \
  ./gradlew :androidApp:installDebug
```

该能力覆盖 Android、iOS 和 HarmonyOS；H5/JS 当前不在支持范围内。

## Kuikly 是否支持 POST + SSE 或 WebSocket

Kuikly `NetworkModule` 的流式接口可以发送 GET 或 POST，
并在响应体到达时持续回调 UTF-8 文本，因此可以承载 SSE、NDJSON 等基于 HTTP 的流式协议。

它不是自动解析 SSE 的接口。`data` 回调只代表一段网络文本：可能只有半行，也可能包含多条
SSE 事件。`data:` 字段、空行分隔和 `[DONE]` 仍由业务层增量解析。

WebSocket 是另一种协议，需要 HTTP Upgrade、双向帧和独立连接生命周期。`NetworkModule`
当前没有提供 WebSocket API。大模型聊天一般是“一次提交问题，服务端持续下发回复”，POST + SSE
已经足够，也更容易复用现有鉴权、网关和代理。只有确实需要持续双向通信时，才需要由宿主
接入平台 WebSocket；消息 Controller 和 UI 流式更新链路仍可复用。

## NetworkModule 的接口契约

KuiklyUI 主干提供的入口是：

```kotlin
val handle = networkModule.httpStreamRequest(
    url = endpoint,
    isPost = true,
    param = requestBody,
    headers = headers,
    timeout = 60,
) { event, data, response ->
    // event: data | complete | error
}

handle.close()
```

接口行为：

1. `data` 是新收到的一段 UTF-8 文本，不保证对应完整 SSE 事件。
2. `complete` 和 `error` 是终态，每个请求最多收到一次。
3. `close()` 可重复调用；主动关闭后不再回调任何事件。
4. `timeout` 是连接和空闲超时。每次收到数据都会重新计时，不限制整条回复总时长。
5. 首个 `data` 通常携带响应头和状态码。HarmonyOS 在数据到达时可能还拿不到状态码，
   非 2xx 的最终状态以 `error` 为准。
6. POST 且 `Content-Type` 包含 `json` 时，`param` 序列化为 JSON；否则按 UTF-8 表单编码。

native 实现负责跨 chunk UTF-8 解码、空闲超时、HTTP 错误、资源释放和取消竞争。业务层
不应该等完整响应后再用定时器模拟流式效果。

## Chat Demo 的完整链路

### 1. KRSseModule 适配到 LlmTransport

[KuiklyNetworkLlmTransport](../../shared/src/commonMain/kotlin/com/kuikly/kuiklychat/dsl/chat/KuiklyNetworkLlmTransport.kt)
把宿主 `KRSseModule` 适配到 Kuikly Chat 已有的 `LlmTransport`。当前发布的 Kuikly `2.15`
`NetworkModule` 还没有 `httpStreamRequest`，所以 Demo 不走它，而是调用从 KuiklyUI 主干
`KRNetworkModule` 流式实现拷出的 `KRSseModule`（Kotlin + Android / iOS / 鸿蒙三端）：

```text
LlmHttpRequest
  -> KuiklyNetworkLlmTransport
     -> KRSseModule/httpStreamRequest
        -> data       -> LlmStreamSink.onChunk
        -> complete   -> LlmStreamSink.onComplete
        -> error      -> LlmStreamSink.onError
        -> cancel     -> closeStreamRequest
```

连接、持续读取和取消由宿主注册的 native `KRSseModule` 完成。适配器只转换 URL、POST JSON、
header 和超时，保留多次回调，并把 HTTP 状态映射成 LLM 错误。

### 2. callback 转成可取消 Flow

`LlmTransport.streamAsFlow` 使用 `callbackFlow` 把 `onChunk` 转成冷 `Flow<String>`。
`awaitClose` 会调用 `LlmSubscription.cancel()`，因此用户点击停止或页面销毁时，取消会
一直传递到 native HTTP 连接，而不只是停止刷新 UI。

```text
ChatController 取消发送 Job
  -> streamAsFlow.awaitClose
  -> LlmSubscription.cancel()
  -> KRSseModule closeStreamRequest
  -> native KRSseModule 关闭连接并释放资源
```

### 3. 正确处理 SSE 分片

[SseDecoder](../../kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/provider/LlmTransport.kt)
负责把任意网络 chunk 还原成完整 SSE payload。

服务端可能发送：

```text
data: {"choices":[{"delta":{"content":"今晚"}}]}

data: {"choices":[{"delta":{"content":"见"}}]}

data: [DONE]
```

一次网络回调可能停在 `{"cont` 中间，也可能包含上述全部事件。`SseDecoder` 会缓存未完成
的行，支持 LF、CR、跨 chunk 的 CRLF、注释行、多条 `data:` 合并和流开头 BOM；只有遇到
事件空行才交出完整 payload。不能直接对每个网络 chunk 调用 `JSONObject`，否则在弱网和
长回复中会随机解析失败。

[OpenAiCompatibleProvider](../../kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/provider/OpenAiCompatibleProvider.kt)
负责上层协议：

- 发送 `POST {baseUrl}/chat/completions`；
- 请求体设置 `stream: true`；
- 发送 JSON 和 `Accept: text/event-stream` 请求头；
- 从 `choices[0].delta.content` 提取新增文本；
- 收到 `[DONE]` 后结束 Flow 并关闭仍未断开的连接。

### 4. 把文本增量写进同一个气泡

每个模型增量转换为：

```kotlin
ChatStreamEvent.TextDelta(
    text = delta,
    format = TextFormat.MARKDOWN,
)
```

`ChatController` 第一次收到 `TextDelta` 时创建一条 `STREAMING` 状态的助手消息，后续增量
只追加到同一个文本 part。收到 `Completed` 后变成 `SENT`；异常或取消则进入失败/取消状态。
列表中始终是一条逐渐增长的回复，不会每个 token 新建一条消息。

## 运行和安全

模型地址、模型名和 API Key 应由宿主在运行时注入。不要把生产密钥写入 Kotlin 源码、
README、提交记录或安装包。更合适的生产架构是由 App 请求业务网关，网关保存模型密钥、
验证用户身份和控制额度，再将模型 SSE 响应转发给客户端。

### 在 Demo 里接入真实 SSE

首页「Chat 组件 Demo」只打开 `chat_component_demo`，不带模型参数，所以走本地脚本回复，
标题是「Chat」。

真实流式要同时带上页面参数 `llmBaseUrl` 和 `llmModel`。两个都不为空时，
`ChatComponentDemoPage` 才会创建 `OpenAiCompatibleProvider`，向
`{llmBaseUrl}/chat/completions` 发 `stream: true`，再把 SSE 的
`choices[0].delta.content` 追加进同一个气泡。标题会变成「Chat · SSE」。

`llmApiKey` 和 `llmSystemPrompt` 可选。有 key 时带 `Authorization: Bearer …`；内网网关
不需要鉴权就留空。`llmBaseUrl` 是端点前缀，不含 `/chat/completions`，尾部斜杠可有可无。
端点需要兼容 OpenAI Chat Completions 的 SSE。

调试直连时的页面参数：

```json
{
  "llmBaseUrl": "https://api.deepseek.com/v1",
  "llmModel": "deepseek-v4.1",
  "llmApiKey": "runtime-only-token",
  "llmSystemPrompt": "You are a helpful assistant."
}
```

Android 真机可以直接打开这一页：

```bash
adb shell am start -n com.kuikly.kuiklychat/.KuiklyRenderActivity \
  --es pageName chat_component_demo \
  --es pageData '{"llmBaseUrl":"https://api.deepseek.com/v1","llmModel":"deepseek-v4.1","llmApiKey":"sk-your-key"}'
```

路由跳转用同一组参数，`&` 后面是页面参数：

```text
chat_component_demo&llmBaseUrl=https://api.deepseek.com/v1&llmModel=deepseek-v4.1&llmApiKey=sk-your-key
```

API Key 仅通过运行时页面参数提供，不写入源码或提交记录。

Demo 不保存任何会话历史。若产品需要多轮上下文，应由业务明确选择需要发送的历史消息，
并在宿主存储层处理，而不是让演示页面默认持久化。

## 常见问题

**页面一直处于发送中**

确认 Kotlin `KRSseModule` 已在 `createExternalModules()` 注册，并且当前平台的 native
`KRSseModule` 也已注册（Android `registerExternalModule`、iOS 类名 `KRSseModule`、鸿蒙
`getCustomRenderModuleCreatorRegisterMap`）。只有 Kotlin 调用、native 没有对应方法时，
连接不会建立，也不会收到 error。

**HTTP 200 但最后一次性显示全文**

检查服务端和 Nginx 等中间代理是否缓冲响应，并确认服务端每次输出后及时 flush。

**偶发 JSON 解析失败**

确认 JSON 输入来自 `SseDecoder` 输出，而不是直接来自 NetworkModule 的 `data` chunk。

**停止后服务端仍在生成**

确认 Flow 取消到达 `awaitClose`，并最终调用 `closeStreamRequest`。只忽略 UI 回调不会
关闭底层 socket。

**首字很慢**

分别记录连接建立、首个网络 chunk、首个完整 SSE payload 和首个 `TextDelta` 的时间，
从而区分模型延迟、代理缓冲、协议解析和 UI 更新问题。

## 相关代码

- [KRSseModule.kt](../../shared/src/commonMain/kotlin/com/kuikly/kuiklychat/module/KRSseModule.kt)
- [KuiklyNetworkLlmTransport.kt](../../shared/src/commonMain/kotlin/com/kuikly/kuiklychat/dsl/chat/KuiklyNetworkLlmTransport.kt)
- [LlmTransport.kt](../../kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/provider/LlmTransport.kt)
- [OpenAiCompatibleProvider.kt](../../kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/provider/OpenAiCompatibleProvider.kt)
- [ChatController.kt](../../kuiklychat-core/src/commonMain/kotlin/com/tencent/kuiklybase/chat/compose/core/ChatController.kt)
- [ChatComponentDemoPage.kt](../../shared/src/commonMain/kotlin/com/kuikly/kuiklychat/dsl/chat/ChatComponentDemoPage.kt)
