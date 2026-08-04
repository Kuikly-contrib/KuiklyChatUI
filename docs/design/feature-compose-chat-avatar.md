---
last_updated: 2026-08-04
status: implemented
owner: @kuikly-chat-team
---

# Feature: Compose Chat 用户头像（Avatar）

## Status: ✅ Implemented

## 目标
在 Compose Chat SDK 消息行中展示用户头像，对齐 Flutter `Avatar` / `AvatarContent`（见 `flutter-chat-ui-components.md` §6.5），使 IM 会话具备基本的「谁在说」视觉识别。

## 非目标
- 不实现完整的 `UserCache` LRU 与异步 `resolveUser` 网络层（首期用消息内嵌 `ChatUser` + 可选宿主回调；**后续见 `feature-compose-resolve-user-cache.md`**）。
- 不实现头像编辑、群聊多头像、在线状态角标。
- 不在本需求内调整消息行整体布局（见 `feature-compose-message-row.md`）。

## 技术方案

### 涉及的模块
- `chat/compose/ui/`：新增 `ChatAvatar.kt`
- `chat/compose/ui/ChatSlots.kt`：新增 `avatarContent` slot
- `chat/compose/ui/ChatScreenConfig.kt`：新增 `showAvatar`、`avatarSizeDp`
- `chat/compose/core/ChatTheme.kt`：头像占位背景色、前景色 token
- `chat/compose/ui/ChatCallbacks.kt`：可选 `onAvatarClick`

### 数据模型变更
无新增模型。使用现有 `ChatUser.avatar`（URL 或宿主约定的本地路径）与 `ChatUser.name`（首字母占位）。

```kotlin
data class ChatScreenConfig(
    // ...
    val showAvatar: Boolean = true,
    val avatarSizeDp: Float = 32f,
    val hideAvatarWhenGrouped: Boolean = true, // MIDDLE/LAST 同组隐藏，对齐 Flyer
)
```

### API 变更（宿主回调）

```kotlin
data class ChatCallbacks(
    // ...
    /** 宿主提供图片加载；返回 null 时显示首字母占位 */
    val onLoadAvatar: ((userId: String, avatarUri: String) -> Any?)? = null,
    val onAvatarClick: ((ChatUser) -> Unit)? = null,
)
```

> `onLoadAvatar` 首期可用 Kuikly 图片组件能消费的 URL/资源 id；跨端缓存抽象后置到 `feature-compose-image-file-messages.md` 的媒体加载层。

### 组件行为
| 场景 | 行为 |
|------|------|
| 有 `avatar` URI 且加载成功 | 圆形裁剪图片 |
| 无图 / 加载失败 | 圆形背景 + 昵称首字母（或默认图标） |
| `hideAvatarWhenGrouped` 且 `MessageGroupStatus` 为 MIDDLE/LAST | 不渲染头像，保留占位宽度或折叠（可配置） |
| 己方消息 | 头像在气泡右侧（由 `ChatMessageRow` 布局决定） |

## 验收标准
- 对方首条/单条消息左侧显示头像；连续消息中间条按配置隐藏头像。
- 己方消息右侧显示头像（或按 `showAvatar` 全局关闭）。
- `ChatSlots.avatarContent` 可完全替换默认头像 UI。
- Demo（`ComposeChatSdkDemoPage`）中 assistant 与用户均有可见头像或占位。
- `commonTest`：头像显隐逻辑（按 `groupStatus`）可用纯函数或 Composable 逻辑测试覆盖。

## 依赖
- `feature-kuikly-chat-ui-sdk-redesign`（✅ Core Phase 1–3 已完成）
- `feature-compose-message-row.md`（📋 待审批）：头像需嵌入消息行布局

---
> 参考：Flutter `Avatar`、`User.imageSource`、`ResolveUserCallback`
