# KuiklyChatAndroid

Android native bridge for `ChatByteCacheModule` (`KRChatByteCacheModule`).

Publish artifact: `com.tencent.kuiklybase:KuiklyChatAndroid`.

## 接入

```kotlin
// build.gradle.kts
implementation("com.tencent.kuiklybase:KuiklyChatAndroid:<version>")

// KuiklyRenderActivity.registerExternalModule
moduleExport(KRChatByteCacheModule.MODULE_NAME) {
    KRChatByteCacheModule()
}
```

Kotlin 侧还需在 `createExternalModules()` 合并 `chatByteCacheExternalModules()`。
