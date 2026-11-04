# KuiklyChatOhos

HarmonyOS native bridge for Compose Chat UI：

- `KRChatByteCacheModule` — 媒体字节缓存
- `KRChatAttachmentPickerModule` — 相册 / 相机 / 文件原生选择器

## 接入

1. 在 `oh-package.json5` 添加依赖：

```json
"@kuikly-open/chat-cache": "file:../KuiklyChatOhos"
```

2. 在 `KuiklyViewDelegate.ets` 注册：

```typescript
import { KRChatAttachmentPickerModule, KRChatByteCacheModule } from '@kuikly-open/chat-cache';

map.set(KRChatByteCacheModule.MODULE_NAME, () => new KRChatByteCacheModule());
map.set(KRChatAttachmentPickerModule.MODULE_NAME, () => new KRChatAttachmentPickerModule());
```

3. Kotlin 侧在 `createExternalModules()` 合并：

```kotlin
modules.putAll(chatByteCacheExternalModules())
modules.putAll(chatAttachmentPickerExternalModules())
```

宿主在页面的 `createExternalModules()` 中按上面的方式合并 Module，并提供 ArkTS 原生实现。

4. 相机来源需在 `module.json5` 声明 `ohos.permission.CAMERA`。相册与文档选择走系统 Picker，无需额外存储权限。

5. Compose Chat 内置 SVG 图标需拷贝到宿主 `resfile`：

```bash
bash scripts/sync-kuikly-chat-ohos-assets.sh
```

目标路径：`ohosApp/entry/src/main/resources/resfile/common/`（供 `assets://common/...` 加载）。

## 平台对照

| 能力 | Android | iOS | HarmonyOS |
|------|---------|-----|-----------|
| 字节缓存 | `KuiklyChatAndroid` | `KuiklyChatIOS` | `KuiklyChatOhos` |
| 附件选择 | `AndroidLlmAttachmentPicker` | `KRChatAttachmentPickerModule` | `KRChatAttachmentPickerModule` |
| 图标资源 | AAR assets 自动合并 | Pod `Resources/common` | `resfile/common` 手动同步 |
