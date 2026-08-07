# KuiklyChatIOS

iOS native bridge for `ChatByteCacheModule` (`KRChatByteCacheModule`).

## 接入

```ruby
# Podfile
pod 'KuiklyChatIOS', :path => '..'
```

Kotlin 侧在 `createExternalModules()` 合并 `chatByteCacheExternalModules()`。
iOS 通过 `TDF_EXPORT_MODULE` 自动注册，无需额外 Native 注册代码。
