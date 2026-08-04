---
last_updated: 2026-08-04
status: draft
owner: @kuikly-chat-team
---

# Feature: Compose Chat 图片与文件消息渲染

## Status: 📝 Draft

## 目标
为 `MessagePart.Attachment` 提供专用 UI，对齐 Flutter `FlyerChatImageMessage` / `FlyerChatFileMessage`（见 `flutter-chat-ui-components.md` §10.3–10.4），替代当前「文件名纯文本」占位。

## 非目标
- 不实现视频/音频消息（Flyer 亦为 🚧）。
- 不在本需求内做 Composer 附件选择与上传（见 `feature-compose-composer-enhancement.md`）。
- 不实现完整 `CrossCache` 跨端缓存库（首期宿主注入加载 + 简单内存缓存接口）。

## 技术方案

### 涉及的模块
- `chat/compose/ui/renderer/ImageMessageRenderer.kt`
- `chat/compose/ui/renderer/FileMessageRenderer.kt`
- `chat/compose/core/MessageRendererRegistry`：按 mime / part 类型路由
- `chat/compose/ui/ChatCallbacks.kt`：`onAttachmentClick`、`onImageClick`
- 可选：`chat/compose/media/AttachmentLoader.kt`（跨端加载抽象）

### 数据模型变更
沿用 `MessagePart.Attachment`；发送路径需保证字段完整：

```kotlin
data class Attachment(
    val id: String,
    val uri: String,
    val mimeType: String,
    val name: String,
    val sizeBytes: Long,
    // 可选后续：width, height, thumbhash
)
```

### 渲染行为

| 类型 | UI |
|------|-----|
| `image/*` | 缩略图 + 圆角 + 点击预览 |
| 其他文件 | 图标 + 文件名 + 大小 + 点击下载/打开 |
| 上传中 | 进度指示（依赖 Controller metadata 或 status） |

### Registry 路由

```kotlin
fun defaultChatRendererRegistry(): MessageRendererRegistry =
    agentCardRendererRegistry()
        .withFallback(unknownCustomRenderer())
        .plus("image", ImageMessageRenderer())
        .plus("file", FileMessageRenderer())
// 或在 rendererFor 中按 mimeType 分支
```

## 验收标准
- Demo 可展示至少一张图片 attachment 与一个文件 attachment（本地或 mock URI）。
- 图片在消息行内宽度受限，不撑破列表布局。
- 文件消息显示名称与可读大小（KB/MB）。
- 点击回调到宿主，Core 不直接打开系统能力。
- 单测：mime→renderer 路由、size 格式化纯函数。

## 依赖
- `feature-compose-message-row.md`（📋）
- `feature-compose-composer-enhancement.md`（📋）：附件从输入区发出
- `LlmChatAdapter` 已有 Attachment 映射（✅）

---
> 参考：`FlyerChatImageMessage`、`FlyerChatFileMessage`、`cross_cache`
