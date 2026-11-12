# Kuikly Chat 的 SSE 长连接与流式输出

Kuikly Chat 现在按 [KuiklyUI PR #1639](https://github.com/Tencent-TDS/KuiklyUI/pull/1639)
接入 POST + SSE。网络层持续回调响应文本，协议层把任意网络分片还原成 SSE 事件，
`ChatController` 再把文本增量追加到同一个消息气泡里。

公开 Demo 有两个运行模式：

- 不传模型配置时，继续使用本地 `Flow + delay`。这个模式不联网，适合检查气泡、Markdown、
  自动滚底和停止生成。
- 传入 `llmBaseUrl` 与 `llmModel` 后，改用真实 POST + SSE。页面标题会显示为
  `Chat · SSE`，第一段 `data:` 到达后就开始更新回复。

这个 Demo 不创建会话仓库，也不恢复或落盘聊天记录。真实模型调用使用无状态的
`generateStream`：每次只发送当前输入，既不保存 provider 对话历史，也不把前几轮消息
带入下一次请求。页面关闭后，当前页面中的消息随 Controller 一起释放。

Demo 源码：

- [KuiklyChat ChatComponentDemoPage（GitHub）](https://github.com/Kuikly-contrib/KuiklyChatUI/blob/main/shared/src/commonMain/kotlin/com/kuikly/kuiklychat/dsl/chat/ChatComponentDemoPage.kt)
- [KuiklyUI 主干的 SSETypewriterDemoPage](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/SSETypewriterDemoPage.kt)
- [本仓库的 Chat Demo 实现](../../shared/src/commonMain/kotlin/com/kuikly/kuiklychat/dsl/chat/ChatComponentDemoPage.kt)

<div align="center">
  <img src="../../assets/chat.png" alt="Kuikly Chat 流式消息演示" width="320">
</div>

## 当前可用范围

PR #1639 已于 2026 年 10 月 19 日合入 KuiklyUI `main`
（merge commit [`28d5519`](https://github.com/Tencent-TDS/KuiklyUI/commit/28d5519369157b94b4a3877ac5bed59723c42f84)）。
本项目当前使用的 `2.17.1-beta` 早于这次合入，仍不包含 `httpStreamRequest` 的 native
实现。因此，在包含该提交的新版 Maven 产物发布前，运行真实 SSE Demo 需要从 KuiklyUI
`main` 构建 `core` 和对应平台的 renderer。

不能只替换 common `core`，也不能只替换 `core-render-android`。两边版本不一致时，
common 层虽然能发出 `httpStreamRequest`，旧 renderer 却不认识这个方法，连接不会建立。

这次能力覆盖 Android、iOS 和 HarmonyOS；H5/JS 不在支持范围内。新版 Maven 产物发布后，
项目可直接升级到包含该能力的正式版本，不再需要自行构建 KuiklyUI。

## Kuikly 是否支持 POST + SSE 或 WebSocket

按 PR #1639 的方案，Kuikly `NetworkModule` 新增的是**流式 HTTP 文本请求**。它可以发 GET
或 POST，并在响应体到达时持续回调 UTF-8 文本，所以可以承载 SSE、NDJSON 等基于 HTTP
的流式协议。

它不是一个“自动解析 SSE”的接口。`data` 回调里的字符串只是网络分片：可能只有半行，
也可能同时包含多条 SSE 事件。SSE 的 `data:`、空行分隔和 `[DONE]` 仍由业务层解析。

WebSocket 是另一种协议，需要 HTTP Upgrade、双向帧和独立的连接生命周期。PR #1639
没有提供 WebSocket API。普通大模型聊天通常是“一次提交问题，服务端持续下发回复”，
POST + SSE 已经足够，而且更容易复用现有鉴权、网关和代理。如果业务需要客户端持续上行、
服务端主动推送等真正的双向通信，再由宿主接入平台 WebSocket，并复用本文后半段的
`Flow<ChatStreamEvent>` 与 UI 链路。

## PR #1639 提供了什么

核心入口是：

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

接口行为有几处需要特别注意：

1. `data` 表示新收到的一段 UTF-8 文本，不代表完整 SSE 事件。
2. `complete` 和 `error` 都是终态，每个请求最多出现一次。
3. `close()` 可以重复调用；主动关闭后不再收到 `complete` 或 `error`。
4. `timeout` 是连接和空闲超时。每次收到数据都会重新计时，不限制整条回复的总时长。
5. 首个 `data` 通常携带响应头和状态码。HarmonyOS 在数据到达时可能还拿不到状态码，
   非 2xx 的最终状态以 `error` 事件为准。
6. POST 且 `Content-Type` 包含 `json` 时，`param` 会被序列化成 JSON；否则按 UTF-8
   表单编码。

PR 的 native 实现还负责跨 chunk 的 UTF-8 解码、空闲超时、非 2xx 错误、资源释放和
取消竞争。业务层不应该再用定时器把完整响应伪装成流式输出。

## 本仓库如何接入

### 1. NetworkModule 到 LlmTransport

[KuiklyNetworkLlmTransport](../../shared/src/commonMain/kotlin/com/kuikly/kuiklychat/dsl/chat/KuiklyNetworkLlmTransport.kt)
把 PR #1639 的 bridge 契约适配到 Kuikly Chat 已有的 `LlmTransport`：

```text
LlmHttpRequest
  -> KuiklyNetworkLlmTransport
     -> NetworkModule/httpStreamRequest
        -> data       -> LlmStreamSink.onChunk
        -> complete   -> LlmStreamSink.onComplete
        -> error      -> LlmStreamSink.onError
        -> cancel     -> closeStreamRequest
```

适配器没有复制 Android、iOS 或 HarmonyOS 的网络代码。真正的连接、读取和取消仍由
PR #1639 的 renderer 完成。它只做四件事：

- 把 URL、POST JSON、header 和超时转换成 NetworkModule 参数；
- 保留长生命周期 callback，让每个网络分片都能返回 common 层；
- 把 HTTP 状态映射成 `AUTH`、`RATE_LIMIT`、`SERVER` 等 LLM 错误；
- 在 Flow 取消时调用 `closeStreamRequest` 并释放 callback。

### 2. 从 callback 转成可取消 Flow

`LlmTransport.streamAsFlow` 使用 `callbackFlow` 把 `onChunk` 转成冷 `Flow<String>`。
`awaitClose` 会调用 `LlmSubscription.cancel()`，所以用户点击停止、页面销毁或新请求替换
旧请求时，取消会一直传到 native 连接，不只是停止刷新 UI。

完整取消路径是：

```text
ChatController 取消发送 Job
  -> streamAsFlow 的 awaitClose
  -> LlmSubscription.cancel()
  -> NetworkModule closeStreamRequest
  -> renderer 关闭 HTTP 连接并释放资源
```

### 3. 增量解析 SSE

[SseDecoder](../../kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/provider/LlmTransport.kt)
处理网络 chunk 和 SSE 事件边界不一致的问题。

例如服务端发送：

```text
data: {"choices":[{"delta":{"content":"今晚"}}]}

data: {"choices":[{"delta":{"content":"见"}}]}

data: [DONE]
```

一次网络回调可能停在 `{"cont` 中间，也可能包含以上全部内容。`SseDecoder` 会缓存未完成
的行，支持 LF、CRLF、注释行、多条 `data:` 合并和流开头的 BOM；只有读到事件空行后才
交出完整 payload。不能对每个 `onChunk` 直接调用 `JSONObject`，否则在弱网或长回复中
会随机出现 JSON 解析失败。

[OpenAiCompatibleProvider](../../kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/provider/OpenAiCompatibleProvider.kt)
继续负责上层协议：

- 发送 `POST {baseUrl}/chat/completions`；
- 请求体设置 `stream: true`；
- 发送 `Content-Type: application/json` 和 `Accept: text/event-stream`；
- 从 `choices[0].delta.content` 提取新增文本；
- 收到 `[DONE]` 后结束收集并关闭仍未主动断开的连接。

### 4. 把模型增量写进同一个气泡

`ChatComponentDemoPage` 把 provider 发出的每个字符串转换为：

```kotlin
ChatStreamEvent.TextDelta(
    text = delta,
    format = TextFormat.MARKDOWN,
)
```

`ChatController` 第一次收到 `TextDelta` 时创建一条 `STREAMING` 状态的助手消息，后续增量
只追加到这条消息最后一个文本 part。收到 `Completed` 后状态变成 `SENT`；发生异常或请求
被取消时进入相应失败/取消状态。列表始终只有一条正在长大的回复，不会每个 token 新建一条
消息。

## 如何运行真实 SSE Demo

先确保宿主使用 PR #1639 构建的 Kuikly `core` 与当前平台 renderer。然后打开
`chat_component_demo` 时传入：

```json
{
  "llmBaseUrl": "https://your-gateway.example.com/v1",
  "llmModel": "your-model",
  "llmApiKey": "runtime-only-token",
  "llmSystemPrompt": "You are a helpful assistant."
}
```

其中只有 `llmBaseUrl` 和 `llmModel` 是启用 SSE 模式的必填项。`llmApiKey` 可以为空，
适用于由 Cookie、设备证书或内网身份完成鉴权的网关；`llmSystemPrompt` 为空时使用 Demo
默认值。

不要把生产 API Key 写进源码、README 或安装包。移动端更合适的部署方式是请求自己的业务
网关：网关保存模型密钥、校验用户身份、控制额度，再把模型的 SSE 响应原样或经过裁剪后转发
给 App。

如果不传 `llmBaseUrl` 或 `llmModel`，Demo 会自动回到本地脚本流。这让公开仓库在没有账号、
没有网络、以及尚未切到 PR renderer 时仍能演示流式 Markdown。

## 异常与排查

**页面一直显示发送中，没有任何分片**

先检查 `core` 与 native renderer 是否都来自 PR #1639。只更新 common 依赖是最常见的原因。
然后确认服务端没有缓冲响应；Nginx 等代理若等 body 攒满再转发，客户端仍看不到实时增量。

**收到 HTTP 200，但最后一次性显示全文**

检查网关是否设置了响应缓冲，以及服务端是否逐条 flush。客户端使用流式 API并不代表中间代理
一定会立即转发。

**偶发 JSON 解析失败**

确认解析对象来自 `SseDecoder`，不是直接来自 NetworkModule 的 `data` chunk。网络分片没有
消息边界保证。

**点击停止后服务端仍在生成**

检查 Flow 取消是否到达 `awaitClose`，以及是否保留并调用了 `StreamRequestHandle.close()`。
只忽略 UI 回调并不会关闭底层连接。

**首字等待很久**

分别记录请求建立、首个 `data` chunk、首个 SSE payload 和首个 `TextDelta` 的时间。这样能
区分模型首 token 慢、代理缓冲、客户端分帧和 UI 更新四类问题。

## 相关代码

- [KuiklyNetworkLlmTransport.kt](../../shared/src/commonMain/kotlin/com/kuikly/kuiklychat/dsl/chat/KuiklyNetworkLlmTransport.kt)：PR #1639 NetworkModule Demo 适配器
- [LlmTransport.kt](../../kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/provider/LlmTransport.kt)：传输接口、callbackFlow 与 SSE 分帧
- [OpenAiCompatibleProvider.kt](../../kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/provider/OpenAiCompatibleProvider.kt)：OpenAI 兼容 POST + SSE
- [ChatController.kt](../../kuiklychat-core/src/commonMain/kotlin/com/tencent/kuiklybase/chat/compose/core/ChatController.kt)：增量追加、完成、失败与取消
- [ChatComponentDemoPage.kt](../../shared/src/commonMain/kotlin/com/kuikly/kuiklychat/dsl/chat/ChatComponentDemoPage.kt)：公开 Chat Demo 的本地/真实 SSE 双模式入口
