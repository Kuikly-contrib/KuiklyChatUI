# KuiklyChatAndroid

Android native bridge for Compose Chat UI：

- `KRChatByteCacheModule` — 媒体字节缓存
- `KRChatImageAdapter` — HTTP / assets / file / SVG 图片加载

Publish artifact: `com.tencent.kuiklybase:KuiklyChatAndroid`.

## 接入

```kotlin
// build.gradle.kts
implementation("com.tencent.kuiklybase:KuiklyChatAndroid:<version>")

// Application 或 KuiklyRenderActivity 初始化时（一次性）
KuiklyChatAndroid.registerDefaultImageAdapter(applicationContext)

// KuiklyRenderActivity.registerExternalModule
moduleExport(KRChatByteCacheModule.MODULE_NAME) {
    KRChatByteCacheModule()
}
```

Kotlin 侧还需在 `createExternalModules()` 合并 `chatByteCacheExternalModules()`。

内置图标资源随 `KuiklyChat` KMP AAR 的 `assets/common/` 自动合并，无需在宿主手抄 SVG。

## 自定义图片加载

若宿主已有 `IKRImageAdapter`，可只参考 `KRChatImageAdapter` 中的 SVG / assets 分支，或在其上组合；默认实现通过 `KuiklyRenderAdapterManager.krImageAdapter` 注册。
