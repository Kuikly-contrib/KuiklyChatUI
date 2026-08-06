---
last_updated: 2026-08-06
status: implemented
owner: @kuikly-chat-team
---

# Feature: Compose Chat 图片与文件消息渲染

## Status: ✅ Implemented

## 目标
为 `MessagePart.Attachment` 提供默认图片/文件 UI，对齐 Flutter
`FlyerChatImageMessage` / `FlyerChatFileMessage`（见 `flutter-chat-ui-components.md`
§10.3–10.4），替代 `TextBubbleContent` 当前的文件名纯文本占位；同时提供头像和附件可复用的图片渲染 slot。

## 非目标
- 不实现视频/音频消息（Flyer 亦为 🚧）。
- 不在本需求内做附件选择、上传或待发送队列（见 `feature-compose-composer-enhancement.md`）。
- 不实现上传百分比。`MessageStatus.SENDING` 是整条消息状态，不能冒充单附件上传进度。
- 不内置图片预览页、文件下载器或系统打开能力；点击只回调宿主。
- 不实现 BlurHash/ThumbHash、原图尺寸探测或完整 `CrossCache`。
- 不让 `compose/ui` 直接依赖 AG-UI 协议类型。

## 现状与关键约束

- `MessagePart.Attachment` 已存在，无需再定义平行模型。`LlmChatAdapter` 与
  `AgUiChatMessageMapper` 目前只完成了部分映射：前者会产生裸 base64，后者未规范化 legacy
  `MessageType.IMAGE/FILE`。
- `MessageRendererRegistry.rendererFor()` 当前只路由整条 `AgentCardContent` 或
  `MessagePart.Custom.type`；`.plus("image", ...)` / `.plus("file", ...)` 不会命中附件。
- 一条 `ChatMessage` 可以同时包含文本和多个附件。用整条消息 renderer 处理某一个附件会吞掉其余 part，
  因此附件必须按 part 渲染。
- Kuikly Compose 已提供 `Image` +
  `com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter(String?)`；默认路径可直接使用，
  并提供 loading/success/error state callback，但该 API 不接收鉴权 header，自定义鉴权/缓存由
  宿主图片 slot 接管。
- `LlmAttachment.thumbnailBase64` 明确用于 UI 缩略图，当前 mapper 未消费；
  Compose attachment 的 data URI 回传 provider 时也不能继续一律塞进 `url`。

## 技术方案

### 涉及的模块
- `chat/compose/ui/TextBubbleContent.kt`：保持 part 顺序，将
  `MessagePart.Attachment` 委托给附件内容组件。
- `chat/compose/ui/AttachmentContent.kt`：附件分类、slot 优先级与点击外壳。
- `chat/compose/ui/ImageAttachmentContent.kt`：图片约束、加载/失败占位。
- `chat/compose/ui/FileAttachmentContent.kt`：文件类型标识、名称和可读大小。
- `chat/compose/ui/ChatImage.kt`：头像与附件共用的默认图片路径。
- `chat/compose/ui/ChatAvatar.kt`：在没有 `avatarContent` 覆盖时接入 `ChatImage`。
- `chat/compose/ui/ChatSlots.kt`：该文件实际承载 `ChatSlots` 与 `ChatCallbacks`，新增附件和图片扩展点。
- `chat/compose/adapter/AttachmentUriCodec.kt`：data URI 编码/解析纯函数，不解码 payload。
- `chat/compose/adapter/LlmChatAdapter.kt`：入站 base64 规范化为 `data:` URI；
  出站 data URI 还原为 provider `bytesBase64`。
- `chat/compose/adapter/AgUiChatMessageMapper.kt`：将 legacy `IMAGE/FILE` 规范化为附件 part，
  避免 URL/文件名误渲染为正文。
- `shared/.../ComposeChatSdkDemoPage.kt`：加入图片、文件和加载失败示例。

### 数据模型与 URI 契约

现有单一 `uri` 无法同时保存「原始可打开/可重试来源」和 `LlmAttachment.thumbnailBase64` /
legacy `thumbnailUrl`。增加可选预览来源：

```kotlin
sealed interface MessagePart {
    data class Attachment(
        val id: String,
        /** 原始发送/打开来源；重试和点击必须使用它。 */
        val uri: String,
        val mimeType: String = "",
        val name: String = "",
        val sizeBytes: Long = 0L,
        /** 仅用于 UI 预览；为空时回退 uri。追加在末尾以保留旧位置参数源码语义。 */
        val previewUri: String = "",
    ) : MessagePart
}
```

- `id` 在同一消息内必须唯一且非空；adapter 负责规范化。宿主直接构造 malformed 数据时，
  UI key 回退为 `message.id + partIndex`，不能因重复 key 崩溃。
- `uri` 是附件的完整来源：`http(s)://`、宿主/平台支持的本地 URI，或
  `data:<mime>;base64,<payload>`。禁止把裸 base64 塞入 `uri`。
- `previewUri` 只能影响图片显示，不能替代点击、重试或 provider 发送来源。
- Kuikly 内置图片路径只承诺 `http(s)`、`data:image/*;base64,...` 和各平台 ImageView
  已支持的本地 scheme；其他来源稳定进入失败占位。文件 data URI 只交给宿主点击回调，不由
  默认图片组件解析。
- `mimeType.startsWith("image/")` 才走图片 UI；空/未知 MIME 稳定回退为文件 UI，不根据扩展名猜测。
- `sizeBytes <= 0L` 表示未知大小，文件卡片隐藏大小而不是显示 `0 KB`。

data URI codec 必须验证 `data:`、MIME、`;base64,` 分隔符及非空 payload，并限制为单次线性扫描；
不得解码、复制到日志或在错误文本中输出 payload。

`LlmChatAdapter` 入站分别构造原始来源与预览来源：

```kotlin
val displayMimeType = mimeType.ifEmpty {
    if (kind == LlmAttachmentKind.IMAGE) "image/unknown" else "application/octet-stream"
}
val sourceUri = when {
    url.isNotEmpty() -> url
    bytesBase64.isNotEmpty() ->
        AttachmentUriCodec.toDataUri(displayMimeType, bytesBase64)
    else -> ""
}
val previewUri = if (
    kind == LlmAttachmentKind.IMAGE && thumbnailBase64.isNotEmpty()
) {
    AttachmentUriCodec.toDataUri(displayMimeType, thumbnailBase64)
} else {
    ""
}
```

构造 `MessagePart.Attachment` 时同时写入 `displayMimeType`，否则 `kind == IMAGE` 且原 MIME 为空
仍会被 UI 错分成文件。默认图片加载使用 `previewUri.ifEmpty { uri }`，
点击与重试始终使用 `uri`。

出站 `toLlmAttachments()` 遇到合法 data URI 时把 payload 写入 `bytesBase64`、`url = ""`，
并在 attachment MIME 为空时使用 URI header MIME；普通 URI 继续写入 `url`。
这样符合 `LlmAttachment`「内联数据走 bytesBase64」的现有契约，也不依赖某个 provider
恰好接受 data URL。

### Adapter 规范化

`AgUiChatMessageMapper` 不能继续对所有非 `CUSTOM` 的 content 无条件创建
`MessagePart.Text`，按 legacy `MessageType` 处理，同时不能丢掉真实 caption：

- `IMAGE`：先映射显式 attachments；列表为空且 content 非空时，用 content 合成图片附件，
  MIME 缺失时使用可识别的 `image/unknown`。
- `FILE`：先映射显式 attachments；为空时从 `extra["fileUrl"]`、
  `fileName/mimeType/fileSize` 合成附件。
- legacy attachment 的 `thumbnailUrl` 进入 `previewUri`，不覆盖原始 `url`。
- 有显式附件时，content 仅在它既不等于附件 URL、也不等于附件文件名时作为 caption Text
  保留，并放在附件前；这样既不重复 `createImageMessage/createFileMessage` 的占位字段，
  也不会误删业务真实说明文字。
- `TEXT` 等其他类型维持现有正文映射；视频/音频仍按本需求非目标处理。
- legacy `Attachment.type == IMAGE` 且 `mimeType` 为空时补图片 MIME fallback；所有合成 ID
  均由消息 ID + 稳定索引生成。

该规范同时覆盖 `ChatMessageHelper.createImageMessage()` 只有 `content=imageUrl` 的现状，
以及 `createFileMessage()` 同时带 `content=fileName` 和附件导致重复展示的问题。

### Slots 与回调

```kotlin
data class ChatSlots(
    // 现有字段保持不变
    val attachmentContent: (
        @Composable (
            message: ChatMessage,
            attachment: MessagePart.Attachment,
            isMine: Boolean,
            modifier: Modifier,
            onClick: () -> Unit,
        ) -> Unit
    )? = null,
    /** 头像和图片附件共用；宿主实现必须应用传入的 modifier。 */
    val imageContent: (
        @Composable (
            source: String,
            contentDescription: String?,
            modifier: Modifier,
        ) -> Unit
    )? = null,
)

data class ChatCallbacks(
    // 现有字段保持不变
    val onAttachmentClick: ((
        message: ChatMessage,
        attachment: MessagePart.Attachment,
    ) -> Unit)? = null,
)
```

只保留一个 `onAttachmentClick`；图片预览、文件下载或打开由宿主根据 MIME 决定，
避免 `onImageClick` 与通用回调同时触发。

优先级如下：

1. 整条消息已被 `MessageRendererRegistry` 命中时，由该 renderer 负责全部内容。
2. 默认气泡内若提供 `attachmentContent`，由它完整替换单个附件，并通过传入的
   modifier 接受父约束、通过 `onClick` 接入统一宿主回调。
3. 否则按 MIME 选择内置图片/文件组件。
4. 内置图片组件优先调用 `imageContent`；未提供时使用
   `Image(rememberAsyncImagePainter(source))`。
5. `ChatAvatar` 仍以 `avatarContent` 为最高优先级；未提供且 `ChatUser.avatar` 非空时走同一个
   `imageContent` / 默认图片路径。只有 SDK 默认图片路径能检测失败并回退首字母；
   自定义 `imageContent` 必须自行实现 loading/error/fallback。

不修改 `MessageRendererRegistry` 的类型 key，也不注册虚假的 `"image"` / `"file"` renderer。

### 默认渲染行为

| 类型/状态 | 默认 UI |
|-----------|---------|
| `image/*` 且预览/原始 URI 有效 | 优先 preview、固定兜底宽高比、最大宽高约束、圆角裁剪、`ContentScale.Crop` |
| 图片加载中 | 与最终尺寸一致的占位背景，避免消息行跳高 |
| 图片加载失败或 URI 为空 | 可读的失败占位；保留附件点击回调 |
| 其他文件 | 文本/Canvas 文件类型标识 + 单行省略文件名 + 可读大小 |

Kuikly Compose 不支持通用 `Icon` 组件；文件标识使用 `Text`、`Canvas` 或本地 `Image`，
不能在实现时引入 `androidx.compose.material.icons`。

文件大小格式化使用纯函数并明确单位策略：小于 1024 显示 `B`，其后按 1024 进位显示
`KB / MB / GB`，未知大小返回空字符串。

多 part 消息严格保持原始顺序；合法附件以 `attachment.id` 建稳定 key，malformed 空/重复 ID
用 `message.id + partIndex` 防御性回退。图片尺寸约束必须同时接受消息行父约束，不能用屏幕固定宽度
撑破列表。

`TextBubbleContent` 当前整泡有 `onMessageClick`。附件子节点点击必须消费该次交互并只调用
`onAttachmentClick`；不得让同一次点击同时触发 attachment 与 message 两个 callback。
callback 为空时不添加伪 clickable 语义。

### 缓存与宿主定制

默认实现依赖 Kuikly `rememberAsyncImagePainter` 的平台图片加载行为，不再虚构一个未定义的
「简单内存缓存接口」。需要鉴权 header、自定义磁盘缓存、解密 URI 或埋点时，宿主通过
`ChatSlots.imageContent` 提供实现；头像和附件会走同一扩展点。已废弃的
`ChatCallbacks.onLoadAvatar` 不恢复使用。宿主 slot 必须应用传入 modifier，并自行负责
加载状态与失败回退；SDK 无法观察一个任意 composable 的加载结果。

## 验收标准

- Demo 展示远程/`data:` 图片、普通文件、图片加载失败各至少一个场景。
- 同一消息中的文本、两张图片和文件均按 part 顺序出现，不被整条消息 renderer 吞掉。
- legacy 图片/文件占位 content 不重复显示，真实 caption 保留在附件前。
- 图片在窄屏和有头像的消息行内均受约束；加载前后不发生明显行高跳变。
- 文件消息显示名称与可读大小；未知大小不显示伪造的 `0 KB`。
- 点击只触发一次 `onAttachmentClick`，Core/UI 不直接打开系统能力。
- 有 `ChatUser.avatar` 时默认头像图片可加载；失败时回退首字母；
  `avatarContent` 与 `imageContent` 的覆盖优先级符合设计。
- 自定义 `attachmentContent` 和 `imageContent` 可替换默认实现且仍受消息行尺寸约束。
- data URI 入站/出站可往返且 payload 不进入日志；缩略图只影响预览，点击/重试仍使用原始来源。
- `commonTest` 覆盖 MIME 分类、URI codec、文件大小格式化、空字段/key fallback、caption 去重、part 顺序，
  以及 `AgUiChatMessageMapper` 的 IMAGE/FILE 去重与合成规则。
- 按 `AGENTS.md` 三条门禁验证，并补跑 JS 编译与 Android/H5 人工图片验收。

## 兼容性

`MessagePart.Attachment`、`ChatSlots`、`ChatCallbacks` 都是公开 `data class`。
增加 `previewUri` / slot 字段会改变构造器、`copy` 和 `componentN` ABI；与主题/slots 分层一起按
major 交付，不能仅依赖默认参数宣称二进制兼容。

## 依赖
- `feature-compose-message-row.md`（✅）
- `feature-compose-chat-avatar.md`（✅ 首字母占位；本需求补默认图片路径）
- `feature-compose-resolve-user-cache.md`（✅ 用户解析；图片加载不进入 `UserCache`）
- `LlmChatAdapter` / `AgUiChatMessageMapper` 已有部分 Attachment 映射（🚧，需按本文规范化）

`feature-compose-composer-enhancement.md` 可并行实施；本需求用历史/mock 消息独立验收，
两者不建立循环硬依赖。

---
> 参考：`flutter-chat-ui-components.md` §10.3–10.4、§11.1；KuiklyUI
> `docs/Compose/core-components.md` 与 `compose/.../coil3/AsyncImagePainter.kt`
