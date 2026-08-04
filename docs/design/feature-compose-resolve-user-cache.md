---
last_updated: 2026-08-04
status: draft
owner: @kuikly-chat-team
---

# Feature: Compose Chat 用户解析与缓存（resolveUser + UserCache）

## Status: 📝 Draft

## 目标
对齐 Flutter `flutter_chat_ui` 的 `resolveUser` + `UserCache`（见 `flutter-chat-ui-components.md` §5、§7.6、§6.5），在 Kuikly Compose Chat SDK 中提供：

1. **按 `userId` 异步解析** `ChatUser`（昵称、头像 URI），消息模型不必内嵌完整用户资料。
2. **LRU 用户缓存**，列表滚动复用行时不重复请求、不闪烁。
3. **与 P0 `ChatAvatar` 衔接**：有缓存走同步路径，无缓存走异步路径 + 首字母占位。
4. **为图片消息共用**头像/缩略图加载契约打基础（与 `feature-compose-image-file-messages.md` 的 `ChatMediaCache` 协同）。

## 非目标
- 不实现完整 `CrossCache` 级跨端二进制缓存库（下载、磁盘、Web 存储后置；首期内存 LRU + 宿主 `ChatImageLoader`）。
- 不在本需求内改 AG-UI 协议或 `agui/` 事件模型。
- 不强制消息模型改为「仅 `authorId`」——`ChatMessage.author` 保留，作为 **嵌入快照 + 降级数据源**。
- 不实现群聊多头像、在线状态、头像裁剪上传。

## 与 Flutter 对照

| Flutter | Kuikly（本设计） |
|---------|------------------|
| `ResolveUserCallback` | `ChatUserResolver` |
| `UserCache`（LRU + `ChangeNotifier`） | `UserCache`（LRU + Compose 可观察） |
| `User.imageSource` | `ChatUser.avatar`（URI / 路径 / asset URL） |
| `CrossCache` + `CachedNetworkImage` | `ChatMediaCache` + `ChatImageLoader`（见 §媒体加载） |
| `Avatar(userId)` | `ChatAvatar(userId, …)` 或行内按 id 解析 |
| `context.read<CrossCache>()` | `LocalChatMediaCache` / `ChatScreen` 参数注入 |

## 技术方案

### 涉及的模块

| 模块 | 职责 |
|------|------|
| `chat/compose/core/ChatUserResolver.kt` | `ChatUserResolver` 接口 |
| `chat/compose/core/UserCache.kt` | LRU 缓存、in-flight 去重 |
| `chat/compose/core/ChatMediaCache.kt` | 可选：URI→已解码资源的轻量内存缓存（与图片消息共用） |
| `chat/compose/ui/ChatUserProvider.kt` | `LocalUserCache`、`rememberUserCache` |
| `chat/compose/ui/ChatAvatar.kt` | 改为 `userId` 驱动 + 解析链路 |
| `chat/compose/ui/ChatMessageRow.kt` | `Username` 同样走 `UserCache` |
| `chat/compose/ui/ChatScreen.kt` | 注入 `resolveUser`、`userCache`、`mediaCache` |
| `chat/compose/ui/ChatSlots.kt` | 保留 `avatarContent`；废弃语义重复的 `onLoadAvatar`（迁移期保留） |

> **分层**：`UserCache` / `ChatUserResolver` 放在 `compose/core`，不依赖 Compose UI；Composable 注入放在 `compose/ui`。

### 数据模型

**不新增消息类型**。沿用 `ChatUser`：

```kotlin
data class ChatUser(
    val id: String,
    val name: String = "",
    val avatar: String = "",      // 对齐 Flutter imageSource
    val metadata: Map<String, String> = emptyMap()
)
```

可选文档别名（实现期二选一，不双字段）：

```kotlin
// 仅当需要与 Flutter 文档对齐时，在 KDoc 中将 avatar 称为 imageSource
```

未来若消息只带 `authorId`，可在 **adapter 边界** 构造最小 `ChatUser(id)`，由 UI 层 `resolveUser` 补齐——本设计不强制本次改 `ChatMessage`。

### 核心 API（compose/core）

#### 1. `ChatUserResolver`

```kotlin
/**
 * 根据 userId 解析展示用用户信息。
 * 返回 null 表示解析失败或用户不存在；UI 降级为首字母 / 默认图标。
 */
fun interface ChatUserResolver {
    suspend fun resolve(userId: String): ChatUser?
}
```

宿主示例：

```kotlin
val resolver = ChatUserResolver { userId ->
    userRepository.findById(userId)?.toChatUser()
}
```

#### 2. `UserCache`

对齐 Flutter `UserCache` 能力，KMP + 协程友好：

```kotlin
class UserCache(
    maxSize: Int = 100,
) {
    /** 同步读缓存；列表首帧无闪烁的关键路径 */
    fun getSync(userId: String): ChatUser?

    /**
     * 缓存命中直接返回；否则调用 resolver 并写入缓存。
     * 同一 userId 并发 resolve 只发起一次请求（in-flight 合并）。
     */
    suspend fun getOrResolve(
        userId: String,
        resolver: ChatUserResolver,
    ): ChatUser?

    fun put(user: ChatUser)
    fun updateUser(userId: String, user: ChatUser?)
    fun remove(userId: String)
    fun clear()

    /** Compose 层订阅版本号，缓存变更时触发 recomposition */
    fun snapshot(): Int
}
```

**LRU 策略**：与 Flutter 一致，`maxSize` 默认 100，淘汰最久未访问项。

**刷新头像 URL**：宿主在 URL 变更后调用 `remove(userId)` 或 `updateUser(userId, newUser)`，与 Flyer 文档一致。

#### 3. 展示用合并策略（纯函数，可单测）

```kotlin
/**
 * 决定 Avatar / Username 最终展示的 ChatUser。
 * 优先级：UserCache > message.author 嵌入字段 > 仅 id 占位。
 */
fun resolveDisplayUser(
    userId: String,
    embedded: ChatUser,
    cached: ChatUser?,
): ChatUser
```

建议规则：

| 字段 | 规则 |
|------|------|
| `id` | 始终 `userId` |
| `name` | `cached.name` 非空 → 用 cached；否则 `embedded.name` |
| `avatar` | `cached.avatar` 非空 → 用 cached；否则 `embedded.avatar` |
| `metadata` | `cached.metadata + embedded.metadata`（cached 优先） |

### Compose 注入（compose/ui）

#### `ChatScreen` 参数扩展

```kotlin
@Composable
fun ChatScreen(
    controller: ChatController,
    // …现有参数…
    resolveUser: ChatUserResolver? = null,
    userCache: UserCache? = null,
    mediaCache: ChatMediaCache? = null,
)
```

内部默认：

```kotlin
@Composable
fun rememberUserCache(
    maxSize: Int = 100,
): UserCache = remember { UserCache(maxSize) }

@Composable
fun ChatScreen(...) {
    val cache = userCache ?: rememberUserCache()
    val media = mediaCache ?: remember { ChatMediaCache() }
    CompositionLocalProvider(
        LocalUserCache provides cache,
        LocalChatUserResolver provides resolveUser,
        LocalChatMediaCache provides media,
    ) { /* 现有 ChatMessageList … */ }
}
```

#### `ChatCallbacks` 迁移

| 现有（P0） | 本设计 | 说明 |
|------------|--------|------|
| `onLoadAvatar(userId, uri) → Any?` | **废弃**，由 `ChatImageLoader` 替代 | 迁移期保留，优先级低于 `avatarContent` |
| `onAvatarClick(ChatUser)` | 保留 | 点击时传入 **展示合并后** 的 `ChatUser` |
| — | `resolveUser` 升为 `ChatScreen` 必填（推荐） | 与 Flutter `Chat(resolveUser: …)` 一致 |

```kotlin
data class ChatCallbacks(
    // …
    val onAvatarClick: ((ChatUser) -> Unit)? = null,
    /** @deprecated 使用 ChatImageLoader + ChatMediaCache */
    val onLoadAvatar: ((userId: String, avatarUri: String) -> Any?)? = null,
)
```

#### `ChatScreenConfig` 扩展

```kotlin
data class ChatScreenConfig(
    // …
    /** 头像 HTTP 请求头（鉴权 Token 等），对齐 Flutter Avatar.headers */
    val avatarHeaders: Map<String, String> = emptyMap(),
)
```

### `ChatAvatar` 渲染流程

对齐 Flutter `Avatar` + `AvatarContent` 双路径：

```text
ChatAvatar(userId, embeddedUser?, …)
  ├─ cached = LocalUserCache.getSync(userId)
  ├─ if cached != null → AvatarContent(resolveDisplayUser(...))
  └─ else if resolveUser != null
        LaunchedEffect(userId) { cache.getOrResolve(userId, resolveUser) }
        → 解析中：AvatarContent(embedded 或 id 占位)
        → 解析后：AvatarContent(merged)
  └─ else → AvatarContent(embedded)   // P0 兼容：无 resolver 时用 message.author
```

`AvatarContent`（内部 Composable）：

```text
if avatar URI 非空 && ChatImageLoader 可用
  → 圆形 Image（经 ChatMediaCache）
else if name 非空
  → 首字母（多空格取首尾词各一字，对齐 Flyer）
else
  → 默认占位（「?」或内置 person 图标资源）
```

### 媒体加载（与图片消息共用）

首期不引入完整 `CrossCache`，定义最小契约：

```kotlin
/** 内存级 URI 缓存；磁盘/Web 后置 */
class ChatMediaCache(maxEntries: Int = 200) {
    fun get(uri: String): Any?
    fun put(uri: String, payload: Any)
    fun remove(uri: String)
    fun clear()
}

/**
 * 宿主或默认实现：把 URI 画到 Modifier 上。
 * source 支持：http(s)、file、asset、data URI（与 CrossCache 文档对齐）。
 */
fun interface ChatImageLoader {
    @Composable
    fun AvatarImage(
        source: String,
        modifier: Modifier,
        headers: Map<String, String> = emptyMap(),
    )
}
```

默认实现（可选）：Kuikly `Image` + `ImageUri`；加载失败回调 `onError` 降级首字母。

> **依赖关系**：`UserCache` 存 **User 元数据**；`ChatMediaCache` 存 **已加载图片 payload**。头像 URL 变更时需同时 `userCache.remove` + `mediaCache.remove(oldUri)`。

### `Username` 组件（与 Avatar 同源）

```kotlin
@Composable
fun ChatUsername(
    userId: String,
    embedded: ChatUser,
    style: …,
)
```

逻辑与 `ChatAvatar` 相同：先 `getSync`，再 `getOrResolve`，展示 `resolveDisplayUser` 的 `name`。

### Adapter / Controller 边界

- `LlmChatAdapter`、`AgUiChatMessageMapper` **继续**映射 `currentUser` / `assistant` 到 `ChatMessage.author`（嵌入快照）。
- **禁止**在 `agui/`、`session/` 调用 `resolveUser`。
- 宿主在 `ChatScreen(resolveUser = …)` 注入；Demo 可继续用静态 `ChatUser`，不强制网络。

### 线程与生命周期

| 项 | 约定 |
|----|------|
| `resolve` | `suspend`，在 `LaunchedEffect` / `rememberCoroutineScope` 内调用 |
| `UserCache` | 非线程安全；仅在 Main + 单 Controller 作用域使用（与 Compose 一致） |
| `dispose` | `ChatScreen` 离开时可选择 `userCache.clear()`；默认保留会话内缓存 |

## 宿主集成示例

```kotlin
@Composable
fun MyChatPage() {
    val userCache = rememberUserCache()
    val controller = rememberLlmChatUiController(provider, currentUser, assistant)

    ChatScreen(
        controller = controller,
        resolveUser = ChatUserResolver { userId ->
            api.getUser(userId)?.let { dto ->
                ChatUser(
                    id = dto.id,
                    name = dto.displayName,
                    avatar = dto.avatarUrl,
                )
            }
        },
        userCache = userCache,
        callbacks = ChatCallbacks(
            onAvatarClick = { user -> openProfile(user.id) },
        ),
        config = ChatScreenConfig(
            avatarHeaders = mapOf("Authorization" to "Bearer $token"),
        ),
    )
}

// 头像 URL 更新后
userCache.remove(userId)
mediaCache.remove(oldAvatarUrl)
```

## 验收标准

- 有 `resolveUser` 时：`ChatAvatar` 首帧用缓存/嵌入数据，无空白闪烁；异步返回后平滑换图/换名。
- 同一 `userId` 在列表中 N 条消息只触发 **1 次** `resolve`（in-flight 去重）。
- 无 `resolveUser` 时：行为与 P0 一致（`message.author` + 首字母）。
- `userCache.remove(id)` 后下次展示重新 `resolve`。
- `avatarHeaders` 传入 `ChatImageLoader`。
- `commonTest`：`UserCache` LRU 淘汰、`getOrResolve` 去重、`resolveDisplayUser` 合并规则。
- `check-layer-deps.sh` 通过；`compose/core` 不依赖 `compose/ui`。

## 依赖

- `feature-compose-chat-avatar.md`（✅ P0 占位与行布局）
- `feature-compose-message-row.md`（✅）
- `feature-compose-image-file-messages.md`（📋）：`ChatImageLoader` / `ChatMediaCache` 完整实现可与此需求并行或紧随其后

## 实施顺序建议

1. `UserCache` + `ChatUserResolver` + `resolveDisplayUser`（纯 Kotlin + 单测）
2. `LocalUserCache` + `ChatScreen` 注入
3. `ChatAvatar` / `ChatUsername` 异步路径
4. `ChatImageLoader` 默认实现 + `avatarHeaders`
5. 废弃 `onLoadAvatar` 文档说明；Demo 增加 `resolveUser` 示例
6. 更新 `feature-compose-chat-avatar.md` 非目标勾选完成项

---
> 参考：Flutter `Avatar`、`AvatarContent`、`UserCache`、`CrossCache`；`flutter-chat-ui` `avatar.dart`
