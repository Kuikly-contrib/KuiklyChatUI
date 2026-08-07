---
last_updated: 2026-08-07
status: implemented
owner: @kuikly-chat-team
---

# Feature: Compose Chat 跨平台媒体缓存

## Status: Implemented

## 目标

为头像和图片附件提供与 Flyer `cross_cache` 对齐的 SDK 自建缓存：

- `downloadAndSave` 风格的缓存优先下载；
- `get`、`set`、`contains`、`delete`、`updateKey`、`clear`；
- Android / Apple 文件缓存与 Web IndexedDB；
- header、稳定 cache key、下载进度和同 key 并发去重；
- 继续通过 `ChatMediaLoader` 接入 `ChatScreen`，不让缓存进入 `ChatController`。

## 架构

```text
ChatImage
  -> ChatMediaRequestResolver
  -> ChatCrossCache.load
       -> ChatByteCache.get(cacheKey)  // ChatByteCacheModule → 宿主 Native 存储
       -> miss: ChatMediaFetcher.fetch(headers)  // NetworkModule + readLocalFile
       -> ChatByteCache.set(cacheKey, bytes)
       -> Ready(data:image/...;base64,...)
  -> rememberAsyncImagePainter
```

核心类型位于：

- `compose/core/ChatCrossCache.kt`：Flyer 风格 facade、直接缓存 API、同 key 请求去重。
- `compose/core/ChatByteCache.kt`：字节存储接口与 `MemoryChatByteCache`（单测用）。
- `compose/core/ChatByteCacheModule.kt`：Kuikly Module 桥（跨端侧）。
- `compose/core/ModuleChatByteCache.kt`：Module → `ChatByteCache` 适配与 `createChatByteCache` 工厂。
- `compose/core/ChatByteCacheHostIntegration.kt`：宿主注册辅助（`chatByteCacheExternalModules()`）。
- `compose/core/ChatMedia.kt`：请求、上下文、进度事件与 loader 边界。
- `compose/ui/KuiklyChatMediaFetcher.kt`：使用 Kuikly `NetworkModule` 下载二进制。
- `compose/ui/ChatMediaConfig.kt`：默认缓存创建及 owned/injected 生命周期。

## 模块目录（对齐 KuiklyCamera）

```text
KuiklyChat/              # KMP 跨端库（ChatByteCacheModule、ChatCrossCache）
KuiklyChatAndroid/       # Android 原生桥（Gradle 发布 KuiklyChatAndroid）
KuiklyChatIOS/           # iOS 原生桥（CocoaPods：KuiklyChatIOS.podspec）
KuiklyChatOhos/          # 鸿蒙原生桥（ohpm：@kuikly-open/chat-cache）
shared/                  # Demo 宿主页，依赖 project(:KuiklyChat)
androidApp/              # Android Demo，额外依赖 project(:KuiklyChatAndroid)
iosApp/                  # iOS Demo，Podfile 引入 KuiklyChatIOS
## Demo

宿主 Demo 提供独立页面 `compose_chat_cache_demo`（`ComposeChatCacheDemoPage`）：

- 继承 `ComposeChatDemoContainer`，自动注册 `chatByteCacheExternalModules()`
- 使用 `rememberChatCrossCache` + 默认 `ChatCrossCache`（非自定义 loader）
- 顶部状态栏轮询 `cache.contains`，展示远程图片是否已写入 Module 缓存
- 路由入口：`router` 页 → **Compose Chat Cache Module Demo**

原有 `compose_chat_sdk_demo` 仍演示自定义 `DemoChatMediaLoader`，两者互不干扰。
```

发布：`publish-maven.sh` 会依次发布 `KuiklyChat`（KMP）与 `KuiklyChatAndroid`（Android JVM）。

## 双依赖接入（必须）

持久化存储**只**通过 Kuikly Module 桥落到宿主 Native；未注册 `ChatByteCacheModule` 时
`createChatByteCache` 会失败。单测使用 `MemoryChatByteCache` 或 fake `ChatByteCacheModule`。

| 依赖 / 步骤 | 职责 |
|---|---|
| `KuiklyChat`（Maven / project） | `ChatByteCacheModule`、`ChatCrossCache`、`createChatByteCache` |
| 平台原生包 | `KuiklyChatAndroid` / `KuiklyChatIOS` / `KuiklyChatOhos` |
| 宿主 Kuikly 侧 | `createExternalModules()` 合并 `chatByteCacheExternalModules()` |

**1. Kuikly / Compose 侧注册 Module：**

```kotlin
import com.tencent.kuiklybase.chat.compose.core.chatByteCacheExternalModules

override fun createExternalModules(): Map<String, Module>? {
    val modules = hashMapOf<String, Module>()
    modules.putAll(chatByteCacheExternalModules())
    return modules
}
```

**2. 宿主 Native 侧注册同名 Module（Android 示例）：**

```kotlin
import com.tencent.kuiklybase.chat.android.KRChatByteCacheModule

override fun registerExternalModule(export: IKuiklyRenderExport) {
    super.registerExternalModule(export)
    export.moduleExport(KRChatByteCacheModule.MODULE_NAME) {
        KRChatByteCacheModule()
    }
}
```

```kotlin
// app/build.gradle.kts
implementation("com.tencent.kuiklybase:KuiklyChatAndroid:<version>")
// 本地开发：implementation(project(":KuiklyChatAndroid"))
```

iOS / 鸿蒙分别引入 `KuiklyChatIOS` Pod 与 `@kuikly-open/chat-cache` ohpm 包，详见各目录 `README.md`。

Module 名必须一致：`KRChatByteCacheModule`。

**3. ChatScreen 无需改动**：`createChatByteCache` 自动使用已注册的 Module。

## 平台后端

| Target | 原生包 | 存储 |
|---|---|---|
| Android | `KuiklyChatAndroid` | App cache 目录 SHA-256 文件 |
| iOS / macOS | `KuiklyChatIOS` | `NSCachesDirectory` SHA-256 文件 |
| 鸿蒙 | `KuiklyChatOhos` | 应用 cache 目录 SHA-256 文件 |
| JS / H5 | 宿主自建 Module（可选） | 需自行实现 `readLocalFile` / IndexedDB |

文件写入采用原子写入或替换；`close()` 只释放连接/请求，不清除持久化数据。

## 默认行为

`ChatScreen` 未提供 `ChatMediaConfig.loader` 时自动创建并持有 `ChatCrossCache`：

```kotlin
ChatScreen(
    controller = controller,
    mediaConfig = ChatMediaConfig(
        cacheNamespace = "account_a_chat_media",
        requestResolver = ChatMediaRequestResolver { context ->
            ChatMediaRequest(
                source = context.source,
                cacheKey = "account-a:${stableSourceKey(context.source)}",
                headers = mapOf("Authorization" to accessToken),
                mimeType = (context as? ChatMediaContext.AttachmentPreview)
                    ?.attachment
                    ?.mimeType,
            )
        },
    ),
)
```

SDK 创建的默认 cache 随 `ChatScreen` 关闭；外部注入的 loader/cache 由调用方关闭，可跨页面共享。

## 直接缓存 API

```kotlin
val cache = rememberChatCrossCache("account_a")

ChatScreen(
    controller = controller,
    mediaConfig = ChatMediaConfig(loader = cache),
)

val bytes = cache.downloadAndSave(request)
cache.set("draft-image", bytes)
cache.contains("draft-image")
cache.updateKey("draft-image", "uploaded-image")
cache.get("uploaded-image")
cache.delete("uploaded-image")
cache.clear()
```

`rememberChatCrossCache` 负责关闭；非 Compose 场景也可以通过
`ChatCrossCache(createChatByteCache(namespace, pager), customFetcher)` 构造，并由调用方显式 `close()`。

## Source 支持

| Source | 默认行为 |
|---|---|
| `http://` / `https://` / Web `blob:` | `NetworkModule` 二进制下载并持久化 |
| `data:*;base64,...` | commonMain 解码并持久化 |
| 原始 Base64 | commonMain 解码并持久化 |
| `file://` / 本地路径 | Android / Apple 读取并持久化；Web 不支持 |
| `content://` | 直接交给原生 painter，不重复复制 |
| `assets/...` | 直接交给 Kuikly 资源加载，不重复复制 |

默认 `NetworkModule` 只能在响应完成时提供总字节进度；需要逐块进度或可取消 HTTP 时，注入自定义
`ChatMediaFetcher` / `ChatCrossCache`。

## 与 Flyer 的差异和修正

Flyer 直接使用 source 作为 key，header 不参与 key，可能让不同账号共享同 URL 的缓存。KuiklyChat
保留默认 `cacheKey = source` 以兼容简单场景，但生产鉴权资源必须使用稳定的账号/租户 namespace；
不得把 token 放入 cache key、日志或 message metadata。

Flyer 没有同 key 并发去重；`ChatCrossCache` 会共享一次 fetch。两者当前均不提供容量上限、
LRU、TTL 或自动清理策略，缓存目录仍可由操作系统回收。

Kuikly painter 只接受字符串，不能像 Flutter `ImageProvider` 直接解码字节。因此缓存命中后会生成
data URI。该方式跨端一致，但会产生 Base64 体积和内存开销；超大原图建议由宿主 loader 返回
本地文件 URI 或 Web Blob URL。

## 安全约束

- `ChatMediaRequest.toString()` 不输出 source、cache key 或 header value。
- cache miss 异常只携带 key 的短哈希。
- cache namespace 会过滤路径分隔符，禁止目录穿越。
- 预览图使用 `previewUri` 缓存；点击、下载和重试仍使用附件原始 `uri`。

## 验证

- commonTest：SHA-256、namespace、缓存 CRUD、header/cacheKey 传播、并发去重、进度和 data URI。
- Android：架构门禁、编译与单测。
- JS：Kotlin/JS 编译，IndexedDB backend 静态校验。
- Apple：iOS Simulator 与 macOS 编译。
