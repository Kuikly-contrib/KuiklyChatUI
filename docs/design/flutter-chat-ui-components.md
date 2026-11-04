---
last_updated: 2026-09-23
status: reference
owner: @kuikly-chat-team
---

# Flyer Chat UI SDK 组件参考文档

> 这是 Flutter `flutter_chat_ui` 的外部组件对照，不是 KuiklyChat 现行 API。Compose 设计稿用章节号引用它。KuiklyChat 入口见 `docs/api/chat-ui.md`。
> 官方在线文档：[flyer.chat/docs/flutter/introduction](https://flyer.chat/docs/flutter/introduction)

---

## 目录

- [1. 概述](#1-概述)
- [2. 包结构](#2-包结构)
- [3. 安装与依赖](#3-安装与依赖)
- [4. 架构总览](#4-架构总览)
- [5. 核心入口：Chat](#5-核心入口chat)
- [6. flutter_chat_ui 组件](#6-flutter_chat_ui-组件)
- [7. flutter_chat_core 数据与基础设施](#7-flutter_chat_core-数据与基础设施)
- [8. Builders 自定义体系](#8-builders-自定义体系)
- [9. 主题系统 ChatTheme](#9-主题系统-chattheme)
- [10. 可选消息组件包 flyer_chat_*](#10-可选消息组件包-flyer_chat_)
- [11. 辅助包](#11-辅助包)
- [12. 快速上手示例](#12-快速上手示例)
- [13. 组件状态一览表](#13-组件状态一览表)

---

## 1. 概述

Flyer Chat 是一套 **后端无关** 的 Flutter 聊天 UI SDK，采用 monorepo 管理，核心设计原则：

- **模块化**：核心 UI 与消息类型解耦，按需引入
- **可定制**：通过 `ChatTheme` 主题 + `Builders` 替换任意 UI 部分
- **高性能**：动画列表、Diff 更新、图片缓存
- **跨平台**：iOS / Android / Web / macOS / Windows / Linux

---

## 2. 包结构

| 包名 | Pub 依赖名 | 说明 |
|------|-----------|------|
| `packages/flutter_chat_ui` | `flutter_chat_ui` | 主 UI 包，提供 `Chat` 及列表、输入框等组件 |
| `packages/flutter_chat_core` | `flutter_chat_core` | 核心模型、控制器、主题、Builders 定义 |
| `packages/cross_cache` | `cross_cache` | 跨平台图片/文件缓存 |
| `packages/flutter_link_previewer` | `flutter_link_previewer` | URL 链接预览 |
| `packages/flyer_chat_text_message` | `flyer_chat_text_message` | 文本消息（Markdown） |
| `packages/flyer_chat_text_stream_message` | `flyer_chat_text_stream_message` | 流式文本消息（AI 打字效果） |
| `packages/flyer_chat_image_message` | `flyer_chat_image_message` | 图片消息 |
| `packages/flyer_chat_file_message` | `flyer_chat_file_message` | 文件消息 |
| `packages/flyer_chat_system_message` | `flyer_chat_system_message` | 系统消息 |
| `packages/flyer_chat_audio_message` | `flyer_chat_audio_message` | 音频消息（🚧 规划中） |
| `packages/flyer_chat_video_message` | `flyer_chat_video_message` | 视频消息（🚧 规划中） |
| `packages/flyer_chat_location_message` | `flyer_chat_location_message` | 位置消息（🚧 规划中） |
| `packages/flyer_chat_custom_message` | `flyer_chat_custom_message` | 自定义消息占位包 |

---

## 3. 安装与依赖

### 最小依赖（仅基础聊天）

```yaml
dependencies:
  flutter_chat_core: ^2.0.0
  flutter_chat_ui: ^2.0.0
```

### 完整消息类型（示例应用级别）

```yaml
dependencies:
  flutter_chat_core: ^2.0.0
  flutter_chat_ui: ^2.0.0
  cross_cache: ^1.1.0
  flutter_link_previewer: ^4.2.0
  flyer_chat_text_message: ^2.6.0
  flyer_chat_text_stream_message: ^2.3.0
  flyer_chat_image_message: ^2.3.0
  flyer_chat_file_message: ^2.4.0
  flyer_chat_system_message: ^2.2.0
```

### 本地开发（本仓库）

```bash
cd flutter_chat_ui
dart pub global activate melos   # 可选
melos bootstrap                  # 或 melos bs
# 运行示例
cd examples/flyer_chat && flutter run
```

---

## 4. 架构总览

```
┌─────────────────────────────────────────────────────────┐
│                         Chat                            │
│  ┌─────────────────────┐  ┌──────────────────────────┐  │
│  │  ChatAnimatedList   │  │        Composer          │  │
│  │  ├─ LoadMore        │  │  (输入框 + 附件 + 发送)    │  │
│  │  ├─ ScrollToBottom  │  └──────────────────────────┘  │
│  │  └─ ChatMessage × N │                                 │
│  │      ├─ Avatar      │                                 │
│  │      ├─ Username    │                                 │
│  │      └─ 消息内容组件  │  ← Builders 注入               │
│  └─────────────────────┘                                 │
└─────────────────────────────────────────────────────────┘
         ↑                           ↑
   ChatController              ChatTheme / Builders
   (flutter_chat_core)         (flutter_chat_core)
```

`Chat` 通过 `provider` 向下注入：`ChatController`、`ChatTheme`、`Builders`、`CrossCache`、`UserCache`、各类回调等。

---

## 5. 核心入口：Chat

**包**：`flutter_chat_ui`  
**类**：`Chat`

主聊天界面，组合消息列表与输入框，是所有功能的入口。

### 主要参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `currentUserId` | `UserID` | ✅ | 当前登录用户 ID |
| `resolveUser` | `ResolveUserCallback` | ✅ | 根据 ID 解析 `User`（头像、昵称） |
| `chatController` | `ChatController` | ✅ | 消息状态控制器 |
| `builders` | `Builders?` | | 自定义 UI 构建器集合 |
| `theme` | `ChatTheme?` | | 聊天主题，默认 `ChatTheme.light()` |
| `crossCache` | `CrossCache?` | | 图片缓存，默认自动创建 |
| `userCache` | `UserCache?` | | 用户信息 LRU 缓存 |
| `onMessageSend` | `OnMessageSendCallback?` | | 发送文本回调 |
| `onMessageTap` | `OnMessageTapCallback?` | | 消息点击 |
| `onMessageLongPress` | `OnMessageLongPressCallback?` | | 消息长按 |
| `onMessageSecondaryTap` | `OnMessageSecondaryTapCallback?` | | 消息右键/副点击 |
| `onAttachmentTap` | `OnAttachmentTapCallback?` | | 附件按钮点击 |
| `backgroundColor` | `Color?` | | 背景色 |
| `decoration` | `Decoration?` | | 背景装饰（优先于 backgroundColor） |
| `timeFormat` | `DateFormat?` | | 时间格式，默认 `HH:mm` |

### 回调类型（`flutter_chat_ui`）

| 类型 | 签名 |
|------|------|
| `OnMessageSendCallback` | `void Function(String text)` |
| `OnMessageTapCallback` | 消息点击（含 index、details） |
| `OnMessageLongPressCallback` | 消息长按 |
| `OnMessageSecondaryTapCallback` | 消息副点击 |
| `OnAttachmentTapCallback` | `VoidCallback` |
| `PaginationCallback` | `Future<void> Function()` |
| `MessageAnimationDurationResolver` | `Duration? Function(Message message)` |

---

## 6. flutter_chat_ui 组件

以下组件均从 `package:flutter_chat_ui/flutter_chat_ui.dart` 导出。

### 6.1 ChatAnimatedList

**用途**：带动画的消息列表，支持分页、自动滚底、键盘适配、消息分组。

| 参数 | 说明 |
|------|------|
| `itemBuilder` | 单条消息构建函数（`ChatItem`） |
| `scrollController` | 滚动控制器 |
| `reversed` | 是否倒序布局（新消息在底部向上增长） |
| `insertAnimationDuration` | 插入动画时长，默认 250ms |
| `removeAnimationDuration` | 移除动画时长 |
| `insertAnimationDurationResolver` | 按消息自定义插入动画时长 |
| `removeAnimationDurationResolver` | 按消息自定义移除动画时长 |
| `scrollToEndAnimationDuration` | 滚到底部动画时长 |
| `scrollToBottomAppearanceDelay` | 回底按钮出现延迟 |
| `scrollToBottomAppearanceThreshold` | 回底按钮出现滚动阈值 |
| `topPadding` / `bottomPadding` | 列表上下内边距 |
| `topSliver` / `bottomSliver` | 列表顶部/底部 Sliver |
| `handleSafeArea` | 是否处理安全区 |
| `keyboardDismissBehavior` | 键盘收起行为 |
| `initialScrollToEndMode` | 初始滚底模式：`none` / `jump` / `animate` |
| `shouldScrollToEndWhenSendingMessage` | 发送时自动滚底 |
| `shouldScrollToEndWhenAtBottom` | 在底部时新消息自动滚底 |
| `onEndReached` | 滚到顶部时加载更早消息 |
| `onStartReached` | 滚到底部时加载更新消息 |
| `paginationThreshold` | 顶部翻页触发阈值（0~1） |
| `startPaginationThreshold` | 底部翻页触发阈值 |
| `messagesGroupingMode` | 消息分组模式 |
| `messageGroupingTimeoutInSeconds` | 同作者消息分组时间窗口 |
| `physics` | 滚动物理效果 |
| `cacheExtent` | 列表缓存范围 |

**枚举 `InitialScrollToEndMode`**：`none` | `jump` | `animate`

---

### 6.2 ChatAnimatedListReversed

**用途**：`ChatAnimatedList` 的倒序封装，等价于 `reversed: true` 的便捷组件。

---

### 6.3 ChatMessage

**用途**：单条消息的外层容器，处理对齐、动画、内边距、手势。

| 参数 | 说明 |
|------|------|
| `message` | 消息数据 |
| `index` | 列表索引 |
| `animation` | 父级动画 |
| `child` | 消息内容组件（气泡等） |
| `leadingWidget` / `trailingWidget` | 消息左右附加组件 |
| `topWidget` / `bottomWidget` / `headerWidget` | 消息上/下/头部附加组件 |
| `sentMessageAlignment` / `receivedMessageAlignment` | 发送/接收对齐方式 |
| `padding` | 自定义内边距 |
| `groupStatus` | 消息分组状态 |
| `isRemoved` | 是否正在移除动画中 |

---

### 6.4 Composer

**用途**：底部消息输入框，含文本框、附件按钮、发送按钮。

| 参数 | 说明 |
|------|------|
| `textEditingController` | 文本控制器 |
| `left` / `right` / `top` / `bottom` | 定位 |
| `sigmaX` / `sigmaY` | 毛玻璃模糊 |
| `padding` | 内边距 |
| `attachmentIcon` / `sendIcon` | 自定义图标 |
| `gap` | 元素间距 |
| `inputBorder` / `filled` / `inputFillColor` | 输入框样式 |
| `topWidget` | 输入框上方附加组件 |
| `handleSafeArea` | 安全区适配 |
| `backgroundColor` | 背景色 |
| `attachmentIconColor` / `sendIconColor` / `emptyFieldSendIconColor` | 图标颜色 |
| `hintText` / `hintColor` / `textColor` | 提示与文字颜色 |
| `keyboardAppearance` | 键盘外观 |
| `autocorrect` / `autofocus` | 自动纠错 / 自动聚焦 |
| `textCapitalization` / `keyboardType` / `textInputAction` | 键盘配置 |
| `maxLines` / `minLines` | 行数限制 |
| `textInputAction` | 键盘动作按钮 |

---

### 6.5 Avatar

**用途**：用户头像，通过 `userId` + `ResolveUserCallback` 异步加载。

| 参数 | 说明 |
|------|------|
| `userId` | 用户 ID |
| `size` | 直径，默认 32 |
| `backgroundColor` / `foregroundColor` | 无图时的背景/前景色 |
| `onTap` | 点击回调 |
| `headers` | 图片请求 HTTP 头（如鉴权 Token） |

**子组件 `AvatarContent`**：头像内容渲染（图片 / 首字母 / 默认图标）。

---

### 6.6 Username

**用途**：显示用户名，从 `UserCache` 获取用户信息。

| 参数 | 说明 |
|------|------|
| `userId` | 用户 ID |
| `style` | 文字样式 |

---

### 6.7 SimpleTextMessage

**用途**：内置简单文本消息气泡（无 Markdown），适合最小集成。

| 参数 | 说明 |
|------|------|
| `message` | `TextMessage` |
| `index` | 列表索引 |
| `padding` / `borderRadius` / `constraints` | 布局样式 |
| `onlyEmojiFontSize` | 纯 Emoji 消息字号 |
| `sentBackgroundColor` / `receivedBackgroundColor` | 气泡背景色 |
| `sentTextStyle` / `receivedTextStyle` | 文字样式 |
| `timeStyle` | 时间戳样式 |
| `showStatus` / `showTime` | 是否显示状态/时间 |
| `timeAndStatusPosition` | 时间位置 |
| `linkPreviewPosition` | 链接预览位置 |

**子组件 `TimeAndStatus`**：时间 + 发送状态图标。

---

### 6.8 IsTypingIndicator

**用途**：三点跳动动画，表示对方正在输入。

| 参数 | 说明 |
|------|------|
| `size` | 点大小，默认 6 |
| `color` | 点颜色 |
| `duration` | 单次弹跳周期 |
| `spacing` | 点间距 |

---

### 6.9 ScrollToBottom

**用途**：浮动「回到底部」按钮。

| 参数 | 说明 |
|------|------|
| `animation` | 显隐动画 |
| `onPressed` | 点击回调 |
| `left` / `right` / `top` / `bottom` | 定位 |
| `useComposerHeightForBottomOffset` | 是否根据输入框高度偏移 |
| `mini` | 是否使用 mini FAB |
| `shape` / `icon` / `backgroundColor` / `foregroundColor` | 外观 |

---

### 6.10 LoadMore

**用途**：列表顶部/底部的加载更多指示器（`CircularProgressIndicator`）。

| 参数 | 说明 |
|------|------|
| `color` | 指示器颜色 |
| `padding` | 垂直内边距 |
| `size` | 指示器大小 |

---

### 6.11 EmptyChatList

**用途**：消息列表为空时的占位 UI。

| 参数 | 说明 |
|------|------|
| `child` | 自定义空状态内容 |

---

### 6.12 内部 / 工具类

| 类 | 说明 |
|----|------|
| `ComposerHeightNotifier` | 监听输入框高度变化（`ChangeNotifier`） |
| `LoadMoreNotifier` | 监听加载更多状态（`ChangeNotifier`） |
| `MessageListDiff` | 消息列表 Diff 工具（`diffutil`） |
| `SliverSpacing` | 列表 Sliver 间距（内部使用） |
| `ChatMessageInternal` | `Chat` 内部使用的消息包装（不对外导出） |

---

## 7. flutter_chat_core 数据与基础设施

从 `package:flutter_chat_core/flutter_chat_core.dart` 导出。

### 7.1 消息模型 Message

`Message` 是 **sealed class**（Freezed），通过 `type` 字段区分类型：

| 工厂构造 | 类型类 | 说明 |
|---------|--------|------|
| `Message.text()` | `TextMessage` | 文本消息 |
| `Message.textStream()` | `TextStreamMessage` | 流式文本占位（AI 场景） |
| `Message.image()` | `ImageMessage` | 图片消息 |
| `Message.file()` | `FileMessage` | 文件消息 |
| `Message.video()` | `VideoMessage` | 视频消息 |
| `Message.audio()` | `AudioMessage` | 音频消息 |
| `Message.system()` | `SystemMessage` | 系统消息 |
| `Message.custom()` | `CustomMessage` | 自定义消息 |
| `Message.unsupported()` | `UnsupportedMessage` | 不支持的消息类型 |

#### 通用字段（所有消息类型）

| 字段 | 说明 |
|------|------|
| `id` | 唯一 ID |
| `authorId` | 发送者 ID |
| `replyToMessageId` | 回复的消息 ID |
| `createdAt` / `updatedAt` / `editedAt` / `deletedAt` | 时间戳 |
| `sentAt` / `deliveredAt` / `seenAt` / `failedAt` | 状态时间戳 |
| `status` | 消息状态（优先于时间戳推断） |
| `reactions` | 表情回应 |
| `pinned` | 是否置顶 |
| `metadata` | 自定义元数据 |

#### 类型特有字段

| 类型 | 特有字段 |
|------|---------|
| `TextMessage` | `text`, `linkPreviewData` |
| `TextStreamMessage` | `streamId` |
| `ImageMessage` | `source`, `text`, `thumbhash`, `blurhash`, `width`, `height`, `size`, `hasOverlay` |
| `FileMessage` | `source`, `name`, `size`, `mimeType` |
| `VideoMessage` | `source`, `text`, `name`, `size`, `width`, `height` |
| `AudioMessage` | `source`, `duration`, `text`, `size`, `waveform` |
| `SystemMessage` | `text` |
| `CustomMessage` | 无固定字段，通过 `metadata` 承载 |

#### 枚举

| 枚举 | 值 | 说明 |
|------|-----|------|
| `MessageStatus` | `sending`, `sent`, `delivered`, `seen`, `error` | 消息发送状态 |
| `MessagesGroupingMode` | `timeDifference`, `sameMinute`, `sameHour`, `sameDay` | 连续消息分组策略 |

---

### 7.2 User 模型

| 字段 | 说明 |
|------|------|
| `id` | 用户 ID |
| `name` | 显示名称 |
| `imageSource` | 头像 URL / 路径 |
| `role` | 角色 |
| `metadata` | 自定义元数据 |

---

### 7.3 MessageGroupStatus

表示同组连续消息的位置：

| 字段 | 说明 |
|------|------|
| `isFirst` | 组内第一条 |
| `isLast` | 组内最后一条 |
| `isMiddle` | 组内中间消息 |

---

### 7.4 LinkPreviewData

链接预览数据模型：

| 字段 | 说明 |
|------|------|
| `title` / `description` / `link` | 标题、描述、URL |
| `image` | `ImagePreviewData`（宽、高、URL） |

---

### 7.5 ChatController

消息状态管理抽象接口：

| 方法 | 说明 |
|------|------|
| `insertMessage(message, {index})` | 插入单条消息 |
| `insertAllMessages(messages, {index})` | 批量插入 |
| `updateMessage(old, new)` | 更新消息 |
| `removeMessage(message)` | 删除消息 |
| `setMessages(messages)` | 替换全部消息 |
| `messages` | 当前消息列表 |
| `operationsStream` | 变更事件流（`ChatOperation`） |
| `dispose()` | 释放资源 |

#### InMemoryChatController

默认内存实现，不持久化。混入：

- **`UploadProgressMixin`**：上传进度流（图片/文件）
- **`ScrollToMessageMixin`**：程序化滚动到指定消息/索引

#### ChatOperation

列表变更操作：`insert` / `insertAll` / `update` / `remove` / `set`

---

### 7.6 UserCache

用户信息 LRU 缓存（`ChangeNotifier`），避免重复解析 `resolveUser`。

| 方法 | 说明 |
|------|------|
| `getSync(id)` | 同步获取缓存 |
| `getOrResolve(id, resolveUser)` | 异步获取或解析 |
| `put(user)` | 写入缓存 |
| `remove(id)` / `clear()` | 清除 |

---

### 7.7 工具函数

| 导出 | 说明 |
|------|------|
| `getIconForStatus(MessageStatus)` | 根据状态返回图标 |
| `isOnlyEmoji(String)` | 判断是否为纯 Emoji 文本 |
| `LinkPreviewPosition` | 链接预览位置枚举 |
| `EpochDateTimeConverter` | JSON 时间戳转换 |
| `DurationConverter` | JSON Duration 转换 |

---

## 8. Builders 自定义体系

`Builders`（Freezed 不可变类）允许替换 `Chat` 内任意 UI 部分。通过 `Chat(builders: Builders(...))` 传入。

| Builder 属性 | 类型 | 替换目标 |
|-------------|------|---------|
| `textMessageBuilder` | `TextMessageBuilder` | 文本消息 |
| `textStreamMessageBuilder` | `TextStreamMessageBuilder` | 流式文本消息 |
| `imageMessageBuilder` | `ImageMessageBuilder` | 图片消息 |
| `fileMessageBuilder` | `FileMessageBuilder` | 文件消息 |
| `videoMessageBuilder` | `VideoMessageBuilder` | 视频消息 |
| `audioMessageBuilder` | `AudioMessageBuilder` | 音频消息 |
| `systemMessageBuilder` | `SystemMessageBuilder` | 系统消息 |
| `customMessageBuilder` | `CustomMessageBuilder` | 自定义消息 |
| `unsupportedMessageBuilder` | `UnsupportedMessageBuilder` | 不支持类型 |
| `composerBuilder` | `ComposerBuilder` | 输入框 |
| `chatMessageBuilder` | `ChatMessageBuilder` | 单条消息外层包装 |
| `chatAnimatedListBuilder` | `ChatAnimatedListBuilder` | 消息列表 |
| `scrollToBottomBuilder` | `ScrollToBottomBuilder` | 回底按钮 |
| `loadMoreBuilder` | `LoadMoreBuilder` | 加载更多指示器 |
| `emptyChatListBuilder` | `EmptyChatListBuilder` | 空列表占位 |
| `linkPreviewBuilder` | `LinkPreviewBuilder` | 链接预览 |

### Builder 签名示例

```dart
// 文本消息 Builder
Widget Function(
  BuildContext context,
  TextMessage message,
  int index, {
  required bool isSentByMe,
  MessageGroupStatus? groupStatus,
})

// 消息列表 Builder
Widget Function(BuildContext context, ChatItem itemBuilder)

// 输入框 Builder
Widget Function(BuildContext context)
```

> **注意**：若不提供某类型消息的 Builder，该类型消息将不会渲染。通常需要为使用的消息类型注册对应的 `flyer_chat_*` 组件或自定义实现。

---

## 9. 主题系统 ChatTheme

### ChatTheme

| 属性 | 说明 |
|------|------|
| `colors` | `ChatColors` 配色 |
| `typography` | `ChatTypography` 字体 |
| `shape` | 消息气泡圆角 |

**工厂方法**：

| 方法 | 说明 |
|------|------|
| `ChatTheme.light({fontFamily})` | 浅色主题 |
| `ChatTheme.dark({fontFamily})` | 深色主题 |
| `ChatTheme.fromThemeData(ThemeData)` | 从 Material 主题派生 |
| `theme.merge(other)` | 合并主题 |

**扩展方法**（`ChatThemeExtensions`）：

- `withLightColors(...)` — 覆盖浅色配色
- `withDarkColors(...)` — 覆盖深色配色

### ChatColors

| 属性 | 说明 |
|------|------|
| `primary` / `onPrimary` | 主色（发送消息气泡等） |
| `surface` / `onSurface` | 背景色 / 前景色 |
| `surfaceContainer` | 接收消息气泡背景 |
| `surfaceContainerLow` / `surfaceContainerHigh` | 容器层级色 |

### ChatTypography

| 属性 | 说明 |
|------|------|
| `bodyLarge` / `bodyMedium` / `bodySmall` | 正文 |
| `labelLarge` / `labelMedium` / `labelSmall` | 标签（时间戳等） |

---

## 10. 可选消息组件包 flyer_chat_*

这些包提供开箱即用的消息气泡组件，通过 `Builders` 注册到 `Chat`。

### 10.1 FlyerChatTextMessage ✅

**包**：`flyer_chat_text_message`  
**消息类型**：`TextMessage`  
**特性**：Markdown 渲染（`GptMarkdown`）、纯 Emoji 放大、链接预览、时间/状态

| 主要参数 | 说明 |
|---------|------|
| `message` / `index` | 消息数据与索引 |
| `padding` / `borderRadius` / `constraints` | 布局 |
| `onlyEmojiFontSize` | 纯 Emoji 字号 |
| `sentBackgroundColor` / `receivedBackgroundColor` | 气泡背景 |
| `sentTextStyle` / `receivedTextStyle` | 文字样式 |
| `timeStyle` | 时间样式 |
| `showStatus` / `showTime` | 状态/时间显隐 |
| `linkPreviewPosition` | 链接预览位置 |
| `onLinkTap` | 链接点击 |

---

### 10.2 FlyerChatTextStreamMessage ✅

**包**：`flyer_chat_text_stream_message`  
**消息类型**：`TextStreamMessage`  
**特性**：AI 流式输出、逐块淡入动画、Markdown 模式

| 主要参数 | 说明 |
|---------|------|
| `message` / `index` | 消息数据与索引 |
| `streamState` | 流状态（见下方） |
| `mode` | `TextStreamMessageMode` |
| `chunkAnimationDuration` | 逐块动画时长 |
| `padding` / `borderRadius` | 布局样式 |
| 颜色/文字样式 | 同 `FlyerChatTextMessage` |

**TextStreamMessageMode**：

| 值 | 说明 |
|----|------|
| `animatedOpacity` | 逐块淡入（默认） |
| `instantMarkdown` | 每次全量 Markdown 渲染 |

**StreamState 子类**：

| 类 | 说明 |
|----|------|
| `StreamStateLoading` | 加载中（显示 "Thinking..." 等） |
| `StreamStateStreaming` | 流式输出中 |
| `StreamStateCompleted` | 流结束 |
| `StreamStateError` | 出错 |

**TextSegment 子类**（流式文本分段）：

| 类 | 说明 |
|----|------|
| `StaticSegment` | 静态已完成文本 |
| `AnimatingSegment` | 正在动画的文本块 |

---

### 10.3 FlyerChatImageMessage ✅

**包**：`flyer_chat_image_message`  
**消息类型**：`ImageMessage`  
**特性**：`CachedNetworkImage` 缓存、BlurHash/ThumbHash 占位、上传进度

| 主要参数 | 说明 |
|---------|------|
| `message` / `index` | 消息数据与索引 |
| `customImageProvider` | 自定义图片 Provider |
| `headers` | 鉴权 HTTP 头 |
| `borderRadius` / `constraints` | 布局 |
| `loadingIndicatorColor` | 加载指示器颜色 |
| `overlayWidget` | NSFW 等内容遮罩 |
| `showStatus` / `showTime` | 状态/时间 |

---

### 10.4 FlyerChatFileMessage ✅

**包**：`flyer_chat_file_message`  
**消息类型**：`FileMessage`  
**特性**：文件名、大小、图标展示

| 主要参数 | 说明 |
|---------|------|
| `message` / `index` | 消息数据与索引 |
| `padding` / `borderRadius` | 布局 |
| `icon` / `iconSize` / `iconColor` | 文件图标 |
| `gap` | 图标与文字间距 |
| `sentBackgroundColor` / `receivedBackgroundColor` | 气泡背景 |
| `onTap` | 点击回调 |

---

### 10.5 FlyerChatSystemMessage ✅

**包**：`flyer_chat_system_message`  
**消息类型**：`SystemMessage`  
**特性**：居中系统提示（如「用户加入聊天」）

| 主要参数 | 说明 |
|---------|------|
| `message` / `index` | 消息数据与索引 |
| `padding` | 内边距 |
| `backgroundColor` | 背景色 |
| `textStyle` | 文字样式 |

---

### 10.6 规划中 / 占位包 🚧

| 包 | 状态 | 说明 |
|----|------|------|
| `flyer_chat_audio_message` | 🚧 规划中 | 音频消息 Widget 尚未实现 |
| `flyer_chat_video_message` | 🚧 规划中 | 视频消息 Widget 尚未实现 |
| `flyer_chat_location_message` | 🚧 规划中 | 位置消息 Widget 尚未实现 |
| `flyer_chat_custom_message` | 📦 占位 | 无实现，演示如何通过 `customMessageBuilder` 自定义 |

**自定义消息实现方式**（`flyer_chat_custom_message` README）：

1. 使用 `Message.custom()` 创建消息，数据放在 `metadata`
2. 在 `Builders(customMessageBuilder: ...)` 中根据 `metadata` 渲染自定义 Widget

---

## 11. 辅助包

### 11.1 cross_cache

**用途**：跨平台（IO + Web）图片/二进制数据缓存。

| 类 | 说明 |
|----|------|
| `CrossCache` | 下载并缓存数据（URL / 本地路径 / assets / data URI / base64） |
| `CachedNetworkImage` | 基于 `CrossCache` 的 `ImageProvider` |

**CrossCache 主要方法**：

| 方法 | 说明 |
|------|------|
| `downloadAndSave(source, {headers, onReceiveProgress})` | 下载并缓存，返回 `Uint8List` |
| `get(source)` | 读取缓存 |
| `contains(source)` | 是否存在缓存 |
| `remove(source)` / `clear()` | 删除 / 清空缓存 |
| `dispose()` | 释放资源 |

Kuikly Compose 对齐采用 `ChatScreen(mediaConfig)` + `ChatCrossCache`：保留根级注入、
header/cacheKey、进度、取消和 owned/injected 生命周期，并实现 Android/Apple SHA-256
文件缓存及 Web IndexedDB。直接 API 包含 `get/set/contains/delete/updateKey/clear`；
详见 `feature-compose-cross-cache.md`。

---

### 11.2 flutter_link_previewer

**用途**：解析文本中的 URL 并展示链接预览卡片。

| 类 / 函数 | 说明 |
|----------|------|
| `LinkPreview` | 链接预览 Widget |
| `getLinkPreviewData(text, ...)` | 异步获取预览数据 |
| `regexLink` / `regexEmail` | URL / 邮箱正则 |

**LinkPreview 主要参数**：

| 参数 | 说明 |
|------|------|
| `text` | 待解析文本 |
| `linkPreviewData` | 已有预览数据（可跳过网络请求） |
| `onLinkPreviewDataFetched` | 数据获取回调 |
| `minWidth` / `maxWidth` | 卡片尺寸 |
| `backgroundColor` / `borderRadius` | 外观 |
| `titleTextStyle` / `descriptionTextStyle` | 文字样式 |
| `enableAnimation` / `animationDuration` | 动画 |
| `corsProxy` / `headers` / `userAgent` | 网络请求配置 |
| `imageBuilder` | 自定义图片渲染 |

---

## 12. 快速上手示例

### 12.1 最简聊天（仅文本）

```dart
import 'package:flutter/material.dart';
import 'package:flutter_chat_core/flutter_chat_core.dart';
import 'package:flutter_chat_ui/flutter_chat_ui.dart';

class BasicChat extends StatefulWidget {
  @override
  State<BasicChat> createState() => _BasicChatState();
}

class _BasicChatState extends State<BasicChat> {
  final _controller = InMemoryChatController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Chat(
        chatController: _controller,
        currentUserId: 'user1',
        onMessageSend: (text) {
          _controller.insertMessage(
            TextMessage(
              id: DateTime.now().millisecondsSinceEpoch.toString(),
              authorId: 'user1',
              createdAt: DateTime.now().toUtc(),
              text: text,
            ),
          );
        },
        resolveUser: (id) async => User(id: id, name: 'John Doe'),
      ),
    );
  }
}
```

### 12.2 完整消息类型

```dart
import 'package:flutter_chat_core/flutter_chat_core.dart';
import 'package:flutter_chat_ui/flutter_chat_ui.dart';
import 'package:flyer_chat_text_message/flyer_chat_text_message.dart';
import 'package:flyer_chat_image_message/flyer_chat_image_message.dart';
import 'package:flyer_chat_file_message/flyer_chat_file_message.dart';
import 'package:flyer_chat_system_message/flyer_chat_system_message.dart';

Chat(
  chatController: controller,
  currentUserId: 'me',
  resolveUser: resolveUser,
  builders: Builders(
    textMessageBuilder: (context, message, index,
            {required isSentByMe, groupStatus}) =>
        FlyerChatTextMessage(message: message, index: index),
    imageMessageBuilder: (context, message, index,
            {required isSentByMe, groupStatus}) =>
        FlyerChatImageMessage(message: message, index: index),
    fileMessageBuilder: (context, message, index,
            {required isSentByMe, groupStatus}) =>
        FlyerChatFileMessage(message: message, index: index),
    systemMessageBuilder: (context, message, index,
            {required isSentByMe, groupStatus}) =>
        FlyerChatSystemMessage(message: message, index: index),
  ),
  onMessageSend: onSend,
)
```

### 12.3 AI 流式消息

```dart
import 'package:flyer_chat_text_stream_message/flyer_chat_text_stream_message.dart';

builders: Builders(
  textStreamMessageBuilder: (context, message, index,
          {required isSentByMe, groupStatus}) {
    final state = streamManager.getState(message.streamId);
    return FlyerChatTextStreamMessage(
      message: message,
      index: index,
      streamState: state,
    );
  },
),
```

> 完整示例见 `examples/flyer_chat/lib/`：`basic.dart`、`local.dart`、`gemini.dart`、`api.dart`、`pagination_*.dart`。

---

## 13. 组件状态一览表

### UI 组件

| 组件 | 包 | 状态 |
|------|-----|------|
| `Chat` | flutter_chat_ui | ✅ 可用 |
| `ChatAnimatedList` | flutter_chat_ui | ✅ 可用 |
| `ChatAnimatedListReversed` | flutter_chat_ui | ✅ 可用 |
| `ChatMessage` | flutter_chat_ui | ✅ 可用 |
| `Composer` | flutter_chat_ui | ✅ 可用 |
| `Avatar` / `AvatarContent` | flutter_chat_ui | ✅ 可用 |
| `Username` | flutter_chat_ui | ✅ 可用 |
| `SimpleTextMessage` | flutter_chat_ui | ✅ 可用 |
| `IsTypingIndicator` | flutter_chat_ui | ✅ 可用 |
| `ScrollToBottom` | flutter_chat_ui | ✅ 可用 |
| `LoadMore` | flutter_chat_ui | ✅ 可用 |
| `EmptyChatList` | flutter_chat_ui | ✅ 可用 |
| `FlyerChatTextMessage` | flyer_chat_text_message | ✅ 可用 |
| `FlyerChatTextStreamMessage` | flyer_chat_text_stream_message | ✅ 可用 |
| `FlyerChatImageMessage` | flyer_chat_image_message | ✅ 可用 |
| `FlyerChatFileMessage` | flyer_chat_file_message | ✅ 可用 |
| `FlyerChatSystemMessage` | flyer_chat_system_message | ✅ 可用 |
| `FlyerChatAudioMessage` | flyer_chat_audio_message | 🚧 未实现 |
| `FlyerChatVideoMessage` | flyer_chat_video_message | 🚧 未实现 |
| `FlyerChatLocationMessage` | flyer_chat_location_message | 🚧 未实现 |
| `LinkPreview` | flutter_link_previewer | ✅ 可用 |
| `CrossCache` / `CachedNetworkImage` | cross_cache | ✅ 可用 |

### 基础设施

| 类型 | 包 | 状态 |
|------|-----|------|
| `ChatController` / `InMemoryChatController` | flutter_chat_core | ✅ 可用 |
| `Message` 及所有子类型 | flutter_chat_core | ✅ 可用 |
| `User` | flutter_chat_core | ✅ 可用 |
| `Builders` | flutter_chat_core | ✅ 可用 |
| `ChatTheme` / `ChatColors` / `ChatTypography` | flutter_chat_core | ✅ 可用 |
| `UserCache` | flutter_chat_core | ✅ 可用 |
| `UploadProgressMixin` | flutter_chat_core | ✅ 可用 |
| `ScrollToMessageMixin` | flutter_chat_core | ✅ 可用 |
| `MessageGroupStatus` | flutter_chat_core | ✅ 可用 |
| `LinkPreviewData` | flutter_chat_core | ✅ 可用 |

---

## 附录：示例应用入口对照

运行 `examples/flyer_chat` 后，首页按钮对应的功能：

| 按钮 | 文件 | 展示的组件/能力 |
|------|------|----------------|
| basic | `basic.dart` | 最简 `Chat` + `SimpleTextMessage` |
| local | `local.dart` | 文本/图片/文件/系统/自定义消息 + 链接预览 |
| api | `api.dart` | 联网同步 + 文本/图片 |
| gemini | `gemini.dart` | 流式文本 + 图片 + Hive 持久化 |
| pagination (get older) | `pagination_older.dart` | 向上翻页加载 |
| pagination (get newer) | `pagination_newer.dart` | 向下翻页加载 |

---

*文档生成自仓库源码，如有出入以源码为准。*
