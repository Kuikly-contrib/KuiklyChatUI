# KuiklyChatOhos

HarmonyOS native bridge for `ChatByteCacheModule` (`KRChatByteCacheModule`).

## 接入

1. 在 `oh-package.json5` 添加依赖：

```json
"@kuikly-open/chat-cache": "file:../KuiklyChatOhos"
```

2. 在 `KuiklyViewDelegate.ets` 注册：

```typescript
import { KRChatByteCacheModule } from '@kuikly-open/chat-cache';

map.set(KRChatByteCacheModule.MODULE_NAME, () => new KRChatByteCacheModule());
```

3. Kotlin 侧合并 `chatByteCacheExternalModules()`。
