---
last_updated: 2026-08-05
status: implemented
owner: @kuikly-chat-team
---

# Feature: Compose Chat 用户解析与缓存（resolveUser + UserCache）

## Status: ✅ Implemented

## 目标
对齐 Flutter `flutter_chat_ui` 的 `resolveUser` + `UserCache`（见 `flutter-chat-ui-components.md` §5、§7.6、§6.5），在 Kuikly Compose Chat SDK 中提供：

1. **按 `userId` 异步解析** `ChatUser`（昵称、头像 URI），消息模型不必内嵌完整用户资料。
2. **有界近似 LRU 用户缓存 + in-flight 去重 + 过期结果屏障**，完成记录、待处理请求和实际并发分别受明确上限约束。
3. **与 P0 `ChatAvatar` 衔接**：有缓存走同步路径，无缓存走异步路径 + 首字母占位。

## 非目标
- **不做头像图片的内置渲染路径**。本需求只负责把 `ChatUser.avatar`（URI）解析到位；把 URI 画成圆形图片依赖跨端媒体加载层，整体归 `feature-compose-image-file-messages.md`（含 `AttachmentLoader` / 缓存 / headers）。在那之前，宿主可用 `ChatSlots.avatarContent` 自行渲染图片。
- **不引入 `ChatMediaCache` / `ChatImageLoader` / `avatarHeaders`**（理由同上，避免与媒体层设计出现两套命名）。
- 不在本需求内改 AG-UI 协议或 `agui/` 事件模型。
- 不强制消息模型改为「仅 `authorId`」——`ChatMessage.author` 保留，作为 **嵌入快照 + 降级数据源**。
- 不改动现有 `avatarInitial` 的取字规则（是否对齐 Flyer 的「首尾词各一字」见「开放问题」）。
- **不做 Kuikly DSL 侧（`chat/agentcard/`、`chat/bubble/`）的用户解析**。本需求仅覆盖 `chat/compose/`。
- 不实现群聊多头像、在线状态、头像裁剪上传。

## 与 Flutter 对照

| Flutter | Kuikly（本设计） |
|---------|------------------|
| `ResolveUserCallback` | `ChatUserResolver` |
| `UserCache`（LRU + `ChangeNotifier`） | `UserCache`（有界近似 LRU + 引用计数观察流 + namespace） |
| `User.imageSource` | `ChatUser.avatar`（URI / 路径 / asset URL） |
| `Avatar(userId)` | `ChatAvatar(user)`（每个消息项在 slot 分发前统一解析，见下） |
| `CrossCache` + `CachedNetworkImage` | ⏭ 后置到 `feature-compose-image-file-messages.md` |
| `Avatar.headers` | ⏭ 同上（由媒体层的宿主 loader 自行携带） |

## 技术方案

### 涉及的模块

| 模块 | 职责 |
|------|------|
| `chat/compose/core/ChatUserResolver.kt` | `ChatUserResolver` 接口 |
| `chat/compose/core/UserCache.kt` | 有界缓存、近似 LRU、namespace、in-flight 去重、负缓存、过期结果屏障与观察版本 |
| `chat/compose/core/DisplayUser.kt` | `resolveDisplayUser` 纯函数（`internal`） |
| `chat/compose/ui/ChatUserState.kt` | `rememberUserCache`、`rememberChatUser`、`rememberResolvedMessage` |
| `chat/compose/ui/ChatMessageList.kt` | 改为稳定 key 的逐消息 lazy item；在 renderer / slot 分发前完成唯一一次解析 |
| `chat/compose/ui/ChatMessageRow.kt` | 消费合并后的 message，分发给头像 / 昵称 / `onAvatarClick` |
| `chat/compose/ui/ChatAvatar.kt` | **签名不变**；slot 与默认内容统一套用尺寸、间距和点击外壳 |
| `chat/compose/ui/ChatScreen.kt` | 保留旧签名，新增接收 `ChatUserResolutionConfig` 的兼容重载 |
| `chat/compose/ui/ChatSlots.kt` | `ChatCallbacks.onLoadAvatar` 标记废弃并保留到下一个 major |

> **分层**：`UserCache` / `ChatUserResolver` 放在 `compose/core`，不依赖 `compose/ui`；Composable 注入放在 `compose/ui`。
> `UserCache` 只依赖 KMP coroutines，不直接依赖 Compose Runtime；行级状态由 `compose/ui` 的
> `produceState` 持有。

### 数据模型

**不新增模型、不改 `ChatMessage`**。沿用现有 `ChatUser`：

```kotlin
data class ChatUser(
    val id: String,
    val name: String = "",
    val avatar: String = "",      // 对齐 Flutter imageSource
    val metadata: Map<String, String> = emptyMap()
)
```

未来若消息只带 `authorId`，可在 **adapter 边界** 构造最小 `ChatUser(id)`，由 UI 层解析补齐。

### 核心 API（compose/core）

#### 1. `ChatUserResolver`

```kotlin
/**
 * 根据 userId 解析展示用用户信息。
 * 返回 null 表示解析失败或用户不存在；UI 降级为嵌入数据 / 首字母。
 *
 * 实现必须是**非阻塞**的 suspend 函数：本模块含 js target，
 * 不存在可用的通用后台调度器（不要假定 Dispatchers.IO 可用）。
 * 需要真正的后台线程时，由宿主在自己的平台代码里完成切换后再挂起返回。
 */
fun interface ChatUserResolver {
    suspend fun resolve(userId: String): ChatUser?
}
```

#### 2. `UserCache`

缓存 key 必须包含宿主提供的稳定 namespace：

```kotlin
internal data class UserCacheKey(
    val namespace: String,
    val userId: String,
)

const val DEFAULT_USER_CACHE_NAMESPACE = "default"
```

namespace 表示用户目录的身份边界，建议使用账号 ID、租户 ID 或数据源版本，例如
`"account:$accountId"`。只要 resolver 的语义发生变化，namespace 就必须变化；禁止用
resolver 对象引用充当 key，因为宿主内联 lambda 会在重组时反复创建实例。

六条硬约束（实现时不得省略）：

| 约束 | 原因 |
|------|------|
| 成功与失败记录进入同一个 `maxSize` LRU | 负缓存不能独立无限增长 |
| cache-owned in-flight 受 `maxPendingResolutions` 限制，实际 resolver 调用再受 `maxConcurrentResolutions` 限制 | 等待执行和正在执行是两个不同维度，必须分别有界 |
| `getSync` 无副作用 | 组合期首帧同步读不能 touch LRU、创建条目或改变失败次数 |
| resolver 作为 `getOrResolve` 参数传入，缓存不持有 resolver | 避免共享缓存被最后一次重组覆盖 resolver，也避免长生命周期缓存泄漏页面对象 |
| 每个请求使用全局递增的 `requestToken`，提交和清理都校验当前 in-flight 身份 | `put` / `remove` / `clear` / `updateUser` 后，旧请求不得回写或删除新请求 |
| LRU 淘汰不推进观察版本；显式更新推进按 key 引用计数的 `StateFlow` | 已组合行保留自己的最后解析结果；StateFlow 又能消除订阅前丢事件窗口 |

```kotlin
class UserCache(
    parentScope: CoroutineScope,
    private val maxSize: Int = 100,
    private val maxAttempts: Int = 2,
    private val maxPendingResolutions: Int = 32,
    private val maxConcurrentResolutions: Int = 8,
    private val resolveTimeoutMillis: Long = 15_000L,
) {
    fun getSync(
        userId: String,
        namespace: String = DEFAULT_USER_CACHE_NAMESPACE,
    ): ChatUser?

    suspend fun getOrResolve(
        userId: String,
        resolver: ChatUserResolver,
        namespace: String = DEFAULT_USER_CACHE_NAMESPACE,
    ): ChatUser?

    fun put(user: ChatUser, namespace: String = DEFAULT_USER_CACHE_NAMESPACE)
    fun updateUser(
        userId: String,
        user: ChatUser?,
        namespace: String = DEFAULT_USER_CACHE_NAMESPACE,
    )
    fun remove(userId: String, namespace: String = DEFAULT_USER_CACHE_NAMESPACE)
    fun retryFailed(namespace: String = DEFAULT_USER_CACHE_NAMESPACE)
    fun clear(namespace: String = DEFAULT_USER_CACHE_NAMESPACE)
    fun clearAll()
    fun close()
}
```

构造时校验 `maxSize > 0`、`maxAttempts > 0`、`maxPendingResolutions > 0`、
`maxConcurrentResolutions > 0`、`resolveTimeoutMillis > 0`。内部创建挂在 `parentScope` 下的
`SupervisorJob`；`close()` 取消该 job 和全部 in-flight。

记录至少包含：

```kotlin
private data class CacheRecord(
    val user: ChatUser?,
    val failureCount: Int,
)

private data class InFlightRequest(
    val token: Long,
    val deferred: Deferred<ChatUser?>,
    var cancellationReason: RequestCancellationReason? = null,
)

private enum class RequestCancellationReason {
    INVALIDATED,
    CACHE_CLOSED,
}

private data class ObserverNode(
    var refCount: Int,
    val revision: MutableStateFlow<Long>,
)
```

`observe(key): Flow<Long>` 为 `internal`：开始 collect 时取得 / 创建 `ObserverNode` 并增加引用，
`emitAll(node.revision)`，结束时在 `finally` 中减引用并删除无人观察的 node。注册、初始
StateFlow 值读取和缓存修改都在主线程串行执行，因此：

- 修改发生在注册前：collector 的首个 revision 到达后读取最新缓存。
- 修改发生在注册后：StateFlow revision 递增并可靠唤醒 collector。
- 多次快速修改可合并，因为 collector 每次都读取缓存的**当前最终状态**。

resolver 成功、手动 `put/update`、`remove`、`retryFailed`、`clear/clearAll` 推进对应活动
ObserverNode；LRU 内部淘汰不推进。观察表只保存当前正在组合的不同 key，最后一个 collector 离开后
立即删除，不形成历史 ID 泄漏。

`close()` 把所有活动 node 的 revision 置为内部 `CLOSED_REVISION`；`observe` 对该哨兵
`takeWhile` 后完成，确保 collector 的 finally 释放引用。close 后的新 `observe` 直接返回空 flow。

`getOrResolve` 的实现顺序：

1. 空 `userId`、cache 已关闭时直接返回；命中成功记录时 touch LRU 并同步返回。
2. 达到 `maxAttempts` 时返回负缓存；同 key 已有 in-flight 时直接 await。
3. 新 key 先以**调用方协程**等待 admission lease；等待可取消，且此时尚未创建 cache-owned
   Deferred。取得 lease 后进入 `try/finally` 并重新检查 closed、缓存和 in-flight。closed、命中
   缓存、改为 await 已有请求、创建失败等所有**未转移**路径都由调用方 finally 释放 lease。
4. 创建全局递增 token 的 `CoroutineStart.LAZY` Deferred，先注册进 `inFlight`，再注册
   completion handler，最后标记 lease 已转移并 `start()`。handler 无条件且仅一次
   `lease.release()`，并仅在 `inFlight[key].token == requestToken` 时移除 map 项。这样 Deferred
   即使在执行协程体前就被取消也不会泄漏 permit 或 map 项。
5. resolver 调用受 `Semaphore(maxConcurrentResolutions)` 和
   `withTimeoutOrNull(resolveTimeoutMillis)` 双重约束。超时、返回 null、ID 不匹配都记一次失败；
   `CancellationException` 原样抛出；其他 `Exception` 写结构化错误日志后记失败，`Error` 不得吞掉。
6. resolver 返回的 `ChatUser.id` 必须等于请求的 `userId`，不允许把 B 用户的数据写到 A 的 key。
7. 写回前校验 `inFlight[key].token == requestToken`；不匹配则丢弃结果并返回 null。成功写入清零
   失败次数并推进该 key 的活动 ObserverNode revision。
8. completion handler 的 map 清理仍受 token 身份保护，旧请求绝不能删掉后来注册的新请求。

`await` 捕获 `CancellationException` 后先调用 `currentCoroutineContext().ensureActive()`：调用方
已取消时继续抛出。调用方仍 active 时再读取当前请求的 `cancellationReason`；只有
`INVALIDATED`（put/remove/update/clear 替换请求）返回 null 并等待观察版本刷新。
`CACHE_CLOSED`、resolver 主动取消、parent scope 取消均继续抛出，不能仅凭 caller active 就误判为
普通失效。

`put` / `remove` / `updateUser` / `clear` / `clearAll` 必须先从 `inFlight` 摘除对应 token，再取消
Deferred 和修改记录；取消前把捕获到的 request 标记为 `INVALIDATED`。`clearAll()` 对全部 key 执行
相同步骤后推进全部活动 ObserverNode。`close()` 必须先原子设置 `closed = true`、把请求标记为
`CACHE_CLOSED`、摘除全部 token、发送 `CLOSED_REVISION`，再取消请求和 scope；排队中的调用方取得
permit 后看到 closed 直接返回，不能创建新请求。

**容量与淘汰**：
- `maxSize` 约束全部已完成 `CacheRecord`，包括成功和失败，不再存在独立且无界的
  `attempts` 表。
- `inFlight.size <= maxPendingResolutions`；超过上限的调用方挂起等待 permit，而不是返回 null，
  因此容量释放后不需要额外的 Compose 重启信号，也不会永久停在占位。
- 实际 resolver 并发数由 `maxConcurrentResolutions` 控制，默认 8。
- 访问序仅由 `getOrResolve` 命中和 `put` / resolver 提交更新；`getSync` 保持纯读。
- `touch` 先从 deque 移除旧 key 再追加到队尾，禁止同一 key 的重复 recency 节点导致误淘汰。
- LRU 淘汰不通知已组合 item；每个 item 持有最后一次解析结果。新组合 item 同步 miss 后才重新解析。
- `maxSize = 1` 且两个用户同时可见时，两行都必须最终得到结果，不能因互相淘汰形成请求 ping-pong。

**刷新头像 URL**：调用 `remove(userId, namespace)` 或
`updateUser(userId, newUser, namespace)`，对应活动 ObserverNode revision 会递增，可见 item
立即刷新或重新解析。

**切换账号 / 换 resolver**：改用新的 namespace。旧请求即使完成也只能写入旧 namespace；
确认旧数据不再复用时可调用 `clear(oldNamespace)` 回收空间。

#### 3. 展示用合并策略（纯函数，可单测）

```kotlin
/**
 * 决定 Avatar / Username 最终展示的 ChatUser。
 * 优先级：UserCache > message.author 嵌入字段 > 仅 id 占位。
 */
internal fun resolveDisplayUser(
    userId: String,
    embedded: ChatUser,
    cached: ChatUser?,
): ChatUser = ChatUser(
    id = userId,
    name = cached?.name?.takeIf { it.isNotEmpty() } ?: embedded.name,
    avatar = cached?.avatar?.takeIf { it.isNotEmpty() } ?: embedded.avatar,
    // 注意顺序：Kotlin 的 a + b 是 b 覆盖 a，cached 优先必须写在右侧
    metadata = embedded.metadata + cached?.metadata.orEmpty(),
)
```

| 字段 | 规则 |
|------|------|
| `id` | 始终 `userId` |
| `name` | `cached.name` 非空 → 用 cached；否则 `embedded.name` |
| `avatar` | `cached.avatar` 非空 → 用 cached；否则 `embedded.avatar` |
| `metadata` | 同名 key 以 cached 为准，其余合并 |

> 声明为 `internal`：这是内部合并细节，不需要进公共 API。`commonTest` 与主源集属于同一个 KMP 模块，`internal` 不影响单测。
> 注意 `name` / `avatar` 用「非空才覆盖」，所以 resolver **无法把一个已有昵称清空**；`metadata` 则是 key 级覆盖，空串也算有效值。这是有意的不对称：前者服务于降级展示，后者服务于宿主透传。

### Compose 注入（compose/ui）

```kotlin
@Composable
fun rememberUserCache(
    maxSize: Int = 100,
    maxAttempts: Int = 2,
    maxPendingResolutions: Int = 32,
    maxConcurrentResolutions: Int = 8,
    resolveTimeoutMillis: Long = 15_000L,
): UserCache {
    val parent = rememberCoroutineScope()
    val cache = remember(
        parent,
        maxSize,
        maxAttempts,
        maxPendingResolutions,
        maxConcurrentResolutions,
        resolveTimeoutMillis,
    ) {
        UserCache(
            parentScope = parent,
            maxSize = maxSize,
            maxAttempts = maxAttempts,
            maxPendingResolutions = maxPendingResolutions,
            maxConcurrentResolutions = maxConcurrentResolutions,
            resolveTimeoutMillis = resolveTimeoutMillis,
        )
    }
    // 参数变化会创建新实例；旧实例不能等到 parent 离开时才取消。
    DisposableEffect(cache) {
        onDispose { cache.close() }
    }
    return cache
}

private data class ResolvedUserValue(
    val owner: UserCache,
    val key: UserCacheKey,
    val user: ChatUser?,
)

@Composable
internal fun rememberChatUser(
    userId: String,
    embedded: ChatUser,
    cache: UserCache,
    namespace: String,
    latestResolver: State<ChatUserResolver?>,
    resolverAvailable: Boolean,
): ChatUser {
    val key = remember(namespace, userId) { UserCacheKey(namespace, userId) }
    val produced by produceState(
        ResolvedUserValue(
            owner = cache,
            key = key,
            user = cache.getSync(userId, namespace),
        ),
        cache,
        key,
        resolverAvailable,
    ) {
        cache.observe(key).collectLatest {
            val sync = cache.getSync(userId, namespace)
            value = ResolvedUserValue(cache, key, sync)
            if (sync == null && resolverAvailable) {
                latestResolver.value?.let { resolver ->
                    value = ResolvedUserValue(
                        owner = cache,
                        key = key,
                        user = cache.getOrResolve(userId, resolver, namespace),
                    )
                }
            }
        }
    }
    // produceState 更换 key 时会重启 producer，但可能短暂保留旧 value；
    // owner + key 标签保证切账号 / 换 cache 的同一帧绝不读取旧用户。
    val cached = produced.user.takeIf {
        produced.owner === cache && produced.key == key
    }

    return remember(userId, embedded, cached) {
        resolveDisplayUser(userId, embedded, cached)
    }
}

@Composable
internal fun rememberResolvedMessage(
    message: ChatMessage,
    cache: UserCache,
    namespace: String,
    latestResolver: State<ChatUserResolver?>,
    resolverAvailable: Boolean,
): ChatMessage {
    val displayUser = rememberChatUser(
        userId = message.author.id,
        embedded = message.author,
        cache = cache,
        namespace = namespace,
        latestResolver = latestResolver,
        resolverAvailable = resolverAvailable,
    )
    return remember(message, displayUser) {
        if (displayUser == message.author) message else message.copy(author = displayUser)
    }
}
```

`ResolvedUserValue` 是 `internal` 行级状态，包含 `owner: UserCache`、`key: UserCacheKey` 与
`user`。`collectLatest` 保证观察版本变化会取消旧等待方并立即重读；共享 Deferred 在 cache scope
内，不会随等待方取消。LRU 淘汰不推进观察版本，因此 eager 列表中仍在组合的行保留
`produceState` 里的最后结果；新进入组合的行才会同步 miss 并重新解析。

第一次失败但尚未达到 `maxAttempts` 时不自动连续重试；item 离开后重新进入会再尝试一次，
`retryFailed()` 推进 namespace 下的活动观察版本，立即唤醒仍可见的失败项。

末尾的 `remember` 不能省略：`ChatUser` 含 `Map<String, String>`，属于 unstable 参数；保持合并结果
引用稳定可以让启用 strong skipping 的下游 composable 跳过无关重组。

#### `ChatScreen` 参数扩展

不修改现有 `ChatScreen` 的 JVM / KMP 签名。新增一个第二参数必填的重载，避免给旧函数追加默认
参数造成二进制不兼容，也避免两个全默认重载产生调用歧义：

```kotlin
data class ChatUserResolutionConfig(
    /** 必须能区分账号 / 租户 / resolver 语义，不允许用 resolver 对象引用生成 */
    val cacheNamespace: String,
    val resolver: ChatUserResolver? = null,
    val cache: UserCache? = null,
    /** 入站 FIRST/SINGLE 昵称为空时是否保留一行高度 */
    val reserveSenderNameSpace: Boolean = true,
)

// 旧签名原样保留，内部以 userResolution = null 调用 ChatScreenContent。
@Composable
fun ChatScreen(
    controller: ChatController,
    // …现有参数…
    rendererRegistry: MessageRendererRegistry = defaultChatRendererRegistry(),
)

// 新重载：userResolution 没有默认值。
@Composable
fun ChatScreen(
    controller: ChatController,
    userResolution: ChatUserResolutionConfig,
    // …其余参数与旧签名相同…
)
```

新重载内部：

1. 校验 `cacheNamespace.isNotBlank()`。
2. `cache == null` 时才调用 `rememberUserCache()`；该实例由当前组合拥有并在离开组合时
   `close()`。外部传入的 cache 不由 `ChatScreen` 关闭。
3. 用 `rememberUpdatedState(userResolution.resolver)` 保存最新 resolver；只把
   `resolver != null` 的 Boolean 作为 effect 输入。内联 lambda 引用变化不会重启请求，
   null ↔ non-null 会立即启动或停止等待。
4. 将 cache、namespace、`State<ChatUserResolver?>` 显式传到 `ChatMessageList`，不使用
   CompositionLocal，也不把 resolver 写进 cache。

多个 `ChatScreen` 可以安全共享同一个 `UserCache`；resolver 语义相同的页面使用同一 namespace，
语义不同的页面必须使用不同 namespace。

#### `ChatCallbacks` 变更

| 现有（P0） | 本设计 | 说明 |
|------------|--------|------|
| `onLoadAvatar(userId, uri) → Any?` | 标记 `@Deprecated`，保留到下一个 major | 本仓库确实零调用点（只有 `ChatSlots.kt` 里的声明），但 `KuiklyChat` 是发布到 Maven 的对外 SDK，仓库内无调用点不等于外部宿主无调用点；且 `ChatCallbacks` 是 data class，删字段会同时改掉 `copy()` / `componentN()`，属于二进制不兼容变更 |
| `onAvatarClick(ChatUser)` | 保留，语义收紧 | 回调参数改为 **合并后** 的 `ChatUser`，而非 `message.author` |

```kotlin
data class ChatCallbacks(
    // …
    val onAvatarClick: ((ChatUser) -> Unit)? = null,
    @Deprecated(
        "从未接线，头像图片加载归 feature-compose-image-file-messages；改用 ChatSlots.avatarContent",
        level = DeprecationLevel.WARNING,
    )
    val onLoadAvatar: ((userId: String, avatarUri: String) -> Any?)? = null,
)
```

#### 兼容性与版本

本需求按**加法式 minor 版本**发布，不引入已知二进制断裂：

- 现有 `ChatScreen` 签名原样保留；新增第二参数必填的重载。
- 新增 `ChatUserResolutionConfig`、`ChatUserResolver`、`UserCache` 和 `rememberUserCache`。
- 不修改 `ChatScreenConfig`；`reserveSenderNameSpace` 放进新配置对象。
- `ChatCallbacks.onLoadAvatar` 标记废弃（本版本仅告警，不 break）。
- `ChatSlots.messageContent`、`messageRowContent`、`avatarContent` 及消息行回调拿到的
  `ChatMessage.author` / `ChatUser` 改为合并后的展示快照。这是行为变化，必须写进 release note；
  `ChatController.state.messages` 中的原消息不被修改。
- `onLoadAvatar` 只允许在下一个 major 删除；不能以“已废弃一个 minor”为由在 1.x 删除 data class 字段。

### `ChatMessageList` 列表策略与验证门

当前实现把全部消息放在 `LazyColumn` 的单个 `item { Column { forEachIndexed(...) } }` 中，滚动不会
回收单条消息。仓库中 `ComposeAgentCardList.kt` 与 `LlmMessageList.kt` 明确记录过 Kuikly 对
可变高度 chat item 做 lazy 测量时出现“后半段未进入 layoutInfo、列表尾部大段空白”的问题，因此
**不能仅凭 API 存在就直接替换生产路径**。

本需求先做逐消息 `itemsIndexed` spike，并设置实现门：

- 覆盖 100+ 条不同高度消息、流式文本增高、展开/收起 AgentCard、加载更早消息。
- Android / iOS / H5 及当前交付目标逐端从头滚到尾，不得出现空白尾部、跳项或无法触达的 item。
- 前插历史后保持阅读锚点；快速滚动与持续流式更新时无崩溃、无明显闪白。

通过后采用逐消息 lazy 路径，并完整保留列表 chrome：

```kotlin
LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
    item(key = "__load_earlier__") {
        // isInitialLoading / isLoadingEarlier / hasMoreEarlier 三种头部状态
        LoadEarlierHeader(/* ... */)
    }
    if (state.messages.isEmpty() && !state.isInitialLoading) {
        item(key = "__empty__") {
            slots.emptyContent?.invoke() ?: EmptyChatContent(strings, theme)
        }
    }
    itemsIndexed(
        items = state.messages,
        key = { _, message -> message.id },
    ) { index, rawMessage ->
        ResolvedOrRawMessageItem(rawMessage, index, /* ... */)
    }
    item(key = "__chat_footer__") {
        Spacer(Modifier.height(12.dp))
    }
}
```

逐消息 lazy 路径的精确滚动规则：

- 加载更早触发条件为 `firstVisibleItemIndex == 0 &&
  firstVisibleItemScrollOffset <= loadEarlierThresholdPx`，并继续受 `hasMoreEarlier` /
  `isLoadingEarlier` / `isInitialLoading` 防重条件约束。
- `scrollToBottom()` 的目标是 footer：`(layoutInfo.totalItemsCount - 1).coerceAtLeast(0)`。
- `isNearBottom()` 以 `visibleItemsInfo.lastOrNull()?.index >= totalItemsCount - 2` 判断 final message
  已进入视口；`totalItemsCount == 0` 时返回 true。
- 稳定 message key 负责前插锚点，删除估算高度补偿；此时
  `estimatedMessageHeightPx` 保留 ABI、标记废弃并停止参与计算。

若任一交付端未通过 spike，则保留现有单 item + eager Column 路径：

- 保留当前像素滚动、估算高度补偿和 `estimatedMessageHeightPx`。
- 仍必须在 renderer / slot 分支前调用 `rememberResolvedMessage`。
- 行级 `produceState` 保存最后解析结果，LRU 内部淘汰不推进观察版本，因此全部行都在组合时也不会
  因 `maxSize` 较小形成反复解析。
- 全历史作者会逐步进入 admission 队列，但 cache-owned pending 与真实网络并发分别受
  `maxPendingResolutions` / `maxConcurrentResolutions` 限制。

两条路径都必须修复现有会话和尾部签名：

- `LazyListState` 改为 `remember(controller)`，禁止继续用第一条消息 ID 作为 conversation
  fingerprint；加载更早消息会改变第一条 ID，但不代表切换会话。
- 删除 `ChatMessageList` 中基于第一条消息 ID 的 `convFingerprint`；自动贴底 effect 改为观察
  `lastMessage.id + lastMessage.text.length`，不再把 `messages.size` 放进尾部签名。

旧 `ChatScreen` 路径必须直接把 raw message 传给现有分支，不创建 cache、不调用
`rememberResolvedMessage`、不启动任何 effect。新路径中的 renderer、`messageContent`、
`messageRowContent` 必须全部位于解析之后。

参考 KuiklyUI：
- `Tencent-TDS/KuiklyUI: docs/Compose/list-and-scroll.md`
- `Tencent-TDS/KuiklyUI: compose/.../foundation/lazy/LazyDsl.kt`
- `KuiklyChat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/compose/agentcard/ComposeAgentCardList.kt`
- `KuiklyChat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/legacy/ai/LlmMessageList.kt`

### `ChatAvatar` / `ChatMessageRow` 渲染流程

解析统一在每个消息项的 slot 分发前完成（lazy / eager 两种列表路径一致）；`ChatAvatar`
**签名不变**（仍收 `ChatUser`）：

```text
raw ChatMessage
  └─ rememberResolvedMessage
       └─ displayMessage(author = merged)
            ├─ renderer / messageContent / messageRowContent
            └─ default ChatMessageRow
                 ├─ ChatAvatar(user = merged)
                 ├─ 昵称 Text(merged.name)
                 └─ callbacks.onAvatarClick?.invoke(merged)
```

`ChatAvatar` 内部需要修正现有 slot 提前返回的问题：无论使用默认首字母还是
`slots.avatarContent`，都先放进统一的 `Box` 外壳，再由外壳应用调用方 modifier、尺寸、圆形裁剪和
`clickable`。因此宿主提供真实图片 slot 时，行间距与 `onAvatarClick` 仍然生效。

**`avatarContent` slot 契约（此前未定义，实现时必须遵守）**：
- slot 拿到的始终是 **合并后** 的 `ChatUser`，与内置渲染同源。
- slot **不等待解析完成**：解析中先以嵌入数据调用一次，解析回来后再以合并数据重组。
- slot 只负责头像内容，不负责重复实现点击；点击由 `ChatAvatar` 外壳统一处理。
- ⚠️ **已知局限**：slot 无法区分「仍在解析」与「该用户确实没有头像」——两种情况下 `user.avatar` 都是空串。因此不要在 slot 里对空 `avatar` 转圈等待，那会让无头像用户永久 loading；应直接降级为首字母占位。若宿主需要区分，约定 resolver 对无头像用户返回一个固定占位 URI。是否给 slot 增加显式解析状态见「开放问题」。

内置渲染在本需求内**仍只画首字母占位**（图片路径见「非目标」第一条）。解析带来的收益是：昵称补齐后首字母更准确，且 `avatar` URI 已就绪供 slot 消费。

### 昵称展示与布局跳动

`ChatMessageRow` 目前是 `showName && message.author.name.isNotEmpty()` 才渲染昵称 `Text`。接入异步解析后，昵称迟到会**凭空插入一行**导致行高跳变。

开关放在新增的 `ChatUserResolutionConfig.reserveSenderNameSpace` 中，不修改已有 public data class。
新重载默认 `true`；旧 `ChatScreen` 入口没有用户解析配置，继续保持“不占位”的原行为。

`reserveSenderNameSpace == true` 时，只有入站 `FIRST` / `SINGLE` 且 `showSenderName` 成立的消息在
name 为空时渲染等高占位（空串 `Text`，保持相同 `fontSize` / `padding`）。己方消息以及
`MIDDLE` / `LAST` 不预留昵称行。

### Adapter / Controller 边界

- `LlmChatAdapter`、`AgUiChatMessageMapper` **继续**映射 `currentUser` / `assistant` 到 `ChatMessage.author`（嵌入快照）。
- **禁止**在 `agui/`、`session/` 调用 `resolveUser`。
- 宿主通过 `ChatScreen(userResolution = ChatUserResolutionConfig(...))` 注入；Demo 可继续用静态
  `ChatUser`，不强制网络。

### 线程与生命周期

| 项 | 约定 |
|----|------|
| `ChatUserResolver.resolve` | `suspend`，**必须非阻塞**。本模块含 js target，common 代码里没有可用的通用后台调度器，不要假定 `Dispatchers.IO` 存在；需要后台线程由宿主在平台侧完成 |
| `UserCache` 内部状态 | 非线程安全，**只允许主线程访问和取消 parent scope**；结果提交及 completion handler 均在 cache scope 的主调度器执行 |
| in-flight 请求 | 归 `UserCache.scope` 所有；调用方取消不影响共享请求；数量受 `maxPendingResolutions` 约束，实际 resolver 并发受第二个 Semaphore 约束 |
| 取消语义 | caller / resolver / parent 的 `CancellationException` 原样抛出且不计失败；仅带 `INVALIDATED` 原因的共享请求取消转成 null，交给观察版本触发新请求 |
| 超时语义 | `withTimeoutOrNull` 自身超时按一次普通失败处理；parent scope 取消仍抛 `CancellationException` |
| 失效语义 | `put` / `remove` / `updateUser` / `clear` / `clearAll` 先摘除 token 再取消 Deferred；迟到结果无法提交 |
| `UserCache.scope` | `SupervisorJob`，单个解析协程抛出不会带崩整个缓存 |
| `dispose` | `close()` 先设置 closed、摘除全部 token，再取消 scope；`rememberUserCache` 在实例被替换或离开组合时调用它，外部 cache 由宿主关闭 |

## 宿主集成示例

```kotlin
@Composable
fun MyChatPage(
    accountId: String,
    userCache: UserCache, // 由导航宿主 rememberUserCache() 创建并跨页面复用
) {
    val controller = rememberLlmChatUiController(provider, currentUser, assistant)
    val namespace = remember(accountId) { "account:$accountId" }
    val resolver = remember(api, accountId) {
        ChatUserResolver { userId ->
            api.getUser(userId)?.let { dto ->
                ChatUser(id = dto.id, name = dto.displayName, avatar = dto.avatarUrl)
            }
        }
    }
    val userResolution = remember(userCache, namespace, resolver) {
        ChatUserResolutionConfig(
            resolver = resolver,
            cache = userCache,
            cacheNamespace = namespace,
            reserveSenderNameSpace = true,
        )
    }

    ChatScreen(
        controller = controller,
        userResolution = userResolution,
        callbacks = ChatCallbacks(
            onAvatarClick = { user -> openProfile(user.id) },
        ),
        slots = ChatSlots(
            // 图片渲染在媒体层落地前，由宿主自行提供；avatar 为空直接画占位，不要转圈
            avatarContent = { user, size -> MyAvatarImage(user.avatar, size) },
        ),
    )
}

userCache.remove(userId, namespace) // 可见 item 立即失效并重新解析
userCache.retryFailed(namespace)    // 网络恢复：立即唤醒该 namespace 下的可见失败项
userCache.clear(namespace)          // 不再使用该账号时回收旧 namespace；切换本身靠新 namespace 隔离
```

## 验收标准

**可由 `commonTest` 覆盖（纯 Kotlin 逻辑）**：
- `UserCache` 近似 LRU 淘汰：成功、失败记录总数不超过 `maxSize`；`getSync` 不影响淘汰顺序。
- cache-owned `inFlight.size` 不超过 `maxPendingResolutions`；额外调用方等待 permit，容量释放后最终获准而不是永久返回 null。
- admission recheck 的 closed、cache hit、已有 in-flight、创建异常路径都准确释放 lease；取消-before-start
  也不会泄漏 permit。
- `getOrResolve` in-flight 去重：同一 `userId` 并发 N 次只调用 resolver 1 次。
- **调用方取消不影响共享请求**：先发起的调用方取消后，后到的调用方仍能拿到结果。
- **取消原因不混淆**：resolver / caller / parent 的 `CancellationException` 继续抛出且不计负缓存；
  显式 `INVALIDATED` 在 caller 仍 active 时返回 null。
- 负缓存：resolver 连续返回 null / 抛普通异常达到 `maxAttempts` 次后，后续调用不再触达 resolver。
- `retryFailed(namespace)` 重置失败记录并推进该 namespace 的活动观察版本；`remove` 清失败次数并推进对应 key。
- 请求进行中调用 `put` / `remove` / `updateUser` / `clear` / `clearAll` 后，迟到结果不能回填或覆盖手动新值。
- 旧请求的 completion 不能移除同 key 后来创建的新 in-flight。
- 相同 `userId`、不同 namespace 完全隔离；切换 namespace 后旧请求只能写旧 namespace。
- resolver 返回错误 ID 时拒绝写入并计失败。
- 实际 resolver 并发峰值不超过 `maxConcurrentResolutions`；超时计一次失败但 parent 取消不计失败。
- `maxSize = 1`、两个用户同时等待时，两者都最终返回且不形成淘汰重请求循环。
- `observe(key)` 在修改发生于订阅前/后两种时序下都能读到最终状态；最后一个 collector 取消后
  ObserverNode 被移除。
- `close()` 先使 token 失效并用 `CLOSED_REVISION` 完成观察流，再取消全部 in-flight；迟到结果
  不提交、排队调用不能创建新请求、ObserverNode 最终清空。
- 空 `userId` 直接返回 null，不发起请求。
- 单个解析协程抛出异常后，同一 `UserCache` 上其他 `userId` 的解析仍正常工作（SupervisorJob 生效）。
- `resolveDisplayUser` 合并规则，**含 metadata 同名 key 以 cached 为准**。

**只能人工验证**（`commonTest` 是多平台源集，只有 `kotlin("test")`，无 Compose UI 测试能力）：
- lazy spike 在各交付端通过“100+ 可变高度 / 流式增高 / AgentCard 展开”全量滚动，无空白尾部；
  未通过的平台走 eager fallback，现有滚动行为不回退。
- lazy 路径只为可见区及预取范围启动解析；eager fallback 可排队全历史作者，但 cache-owned pending
  与实际网络并发始终不超过配置上限。
- 前插更早消息时保持当前阅读位置；滚到底部、近底判断、load-earlier 与 unread 行为不回退。
- 有 resolver 时首帧用缓存/嵌入数据，异步返回后平滑换名；`reserveSenderNameSpace = true` 时入站首条行高不跳变。
- 旧 `ChatScreen` 重载无用户解析开销，行为与 P0 完全一致（`message.author` + 首字母）。
- 可见 item 上调用 `remove` / `retryFailed` 会立即重新解析，不要求先滚出视口。
- 切换 namespace 时旧请求返回不会污染新账号展示。
- 同一复用 item 同帧切换 cache / namespace / userId 时，`ResolvedUserValue` 标签阻止旧账号用户
  短暂显示。
- `messageContent`、`messageRowContent`、自定义 renderer 与 `avatarContent` 都拿到合并后的
  `message.author`；使用 `avatarContent` 时 `onAvatarClick` 仍触发且间距不丢失。
- 列表快速来回滚动时，同一时刻同 namespace + `userId` 不并发重复请求；LRU 淘汰不会让仍在组合的行退回占位。
- 宿主页面频繁重组（如输入框每次按键）时，消息列表不发生整屏重组、不重复触发解析。

**门禁**（按 `AGENTS.md`「复现与验证」，三条全绿才提交）：

```bash
bash scripts/check-layer-deps.sh
./gradlew :KuiklyChat:compileDebugKotlinAndroid :KuiklyChat:compileDebugUnitTestKotlinAndroid
./gradlew :KuiklyChat:testDebugUnitTest
```

- 本需求同步给 `check-layer-deps.sh` 增加规则：`compose/core` 不得 import `compose/ui`。在该规则落地前，
  脚本跑通不代表这条分层约束成立。

## 开放问题（待定，不阻塞实现）

1. **`avatarInitial` 是否对齐 Flyer 的「多词取首尾各一字」**。这是对现有 public 顶层函数的行为变更，会影响已有单测，本设计默认**不改**。
2. **是否给 `avatarContent` 增加显式解析状态**（如把签名扩成 `(ChatUser, Dp, Boolean) -> Unit`）。能解决「解析中 vs 无头像」不可区分的问题，但会改动 public 数据类的 lambda 类型，属源码破坏性变更；当前先用「约定占位 URI」绕过，等媒体层设计一起定。
3. **负缓存是否需要 TTL**。当前失败记录已受 `maxSize` 约束，且 `retryFailed()` 会推进 namespace
   下的活动观察版本并立即唤醒可见
   item，但达到 `maxAttempts` 后不会按时间自动恢复。若实测需要，再加入可注入时钟和
   `negativeCacheTtlMillis`，不在首版偷偷依赖平台时间 API。

## 依赖

- `feature-compose-chat-avatar.md`（✅ P0 占位与行布局）
- `feature-compose-message-row.md`（✅）
- `feature-compose-image-file-messages.md`（📋）：头像/缩略图的图片加载与缓存统一在该设计落地，本需求不预先定义相关接口

## 实施顺序建议

1. 先做 `ChatMessageList.itemsIndexed` 跨端 spike；通过则落稳定 key lazy 路径并修正滚动，
   未通过则保留单 item eager fallback。
2. `ChatUserResolver` + namespace 化 `UserCache` + `resolveDisplayUser`，先完成容量、并发、
   admission、requestToken stale-write barrier、引用计数观察流、取消与负缓存单测。
3. `rememberUserCache` / `rememberChatUser` / `rememberResolvedMessage`，用行级
   `produceState + collectLatest` 验证显式失效和 LRU 静默淘汰。
4. 保留旧 `ChatScreen` 签名，增加 `ChatUserResolutionConfig` 重载；在所有 renderer / slot 分支前
   生成展示消息副本。
5. 修正 `ChatAvatar` 统一外壳和昵称占位；验证自定义 slot 点击与间距。
6. `ChatCallbacks.onLoadAvatar` 标记 `@Deprecated`；Demo 增加 namespace / resolver 示例。
7. 给 `check-layer-deps.sh` 增加 `compose/core → compose/ui` 禁止规则，更新
   `feature-compose-chat-avatar.md` 与 release note。

---
> 参考：Flutter `Avatar`、`AvatarContent`、`UserCache`；`flutter-chat-ui` `avatar.dart`
