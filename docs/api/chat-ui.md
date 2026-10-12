---
last_updated: 2026-09-23
status: active
owner: @kuikly-chat-team
---

# KuiklyChat UI — API Reference

> Assumptions: this is a Kotlin library, not an HTTP service. There is no base URL, bearer token, or REST route in the source. The sections below are public entry points taken from the current source. Version `0.1.0-experimental` comes from `gradle.properties` (`MAVEN_VERSION`). It is not a Stable API.
>
> Coordinates: `com.tencent.kuiklybase:kuiklychat-compose:0.1.0-experimental-2.1.21` and `com.tencent.kuiklybase:kuiklychat-compat:0.1.0-experimental-2.1.21`. HarmonyOS uses `0.1.0-experimental-2.0.21-mini-009`.

## Table of Contents

- [Authentication](#authentication)
- [ChatController](#chatcontroller)
  - [Construct ChatController](#construct-chatcontroller)
  - [sendText](#sendtext)
  - [sendMessage](#sendmessage)
  - [dispatch](#dispatch)
- [Kuikly DSL](#kuikly-dsl)
  - [ChatControllerSession](#chatcontrollersession)
  - [ChatDockComposer](#chatdockcomposer)
  - [ChatWelcomeState](#chatwelcomestate)
- [Kuikly Compose DSL](#kuikly-compose-dsl)
  - [ChatScreen](#chatscreen)
- [ChatMessage](#chatmessage)

## Authentication

None. Callers construct `ChatController` in process and pass it to a UI entry. Network credentials, if any, stay in the host's `ChatMessageSender`.

---

## ChatController

Package: `com.tencent.kuiklybase.chat.compose.core`. Module: `kuiklychat-core` (pulled in by compose and compat).

Single state owner for both UI stacks. It does not render and does not import AG-UI types.

### Construct ChatController

Creates the session state, optional send pipeline, and optional history loader.

**Parameters**

| Name | In | Type | Required | Description |
|------|----|------|----------|-------------|
| `currentUser` | argument | `ChatUser` | yes | The local participant. `id` and `name` are used for outgoing messages. |
| `scope` | argument | `CoroutineScope` | yes | Scope for send and history jobs. Cancel it when the page is destroyed, or call `dispose()`. |
| `sender` | argument | `ChatMessageSender?` | no | Returns `Flow<ChatStreamEvent>`. `null` marks the user message sent immediately. |
| `historyLoader` | argument | `ChatHistoryLoader?` | no | Loads the first page when `initialMessages` is empty. |
| `initialMessages` | argument | `List<ChatMessage>` | no | Seed list. Default `emptyList()`. |
| `assistantUser` | argument | `ChatUser` | no | Author of streamed replies. Default `ChatUser("assistant", "Assistant")`. |
| `clock` | argument | `ChatClock` | no | Timestamps. Default `ChatClock.System`. |

**Responses**

| Result | Description |
|--------|-------------|
| instance | `state` / `stateFlow` hold `ChatState`. `effects` emits `ScrollToBottom`, `ShowError`, or `CopyToClipboard`. |

```kotlin
val scope = CoroutineScope(SupervisorJob())
val controller = ChatController(
    currentUser = ChatUser(id = "me", name = "我"),
    scope = scope,
    sender = ChatMessageSender { message ->
        flow {
            emit(ChatStreamEvent.TextDelta(text = "收到：${message.text}"))
            emit(ChatStreamEvent.Completed)
        }
    },
)
```

### sendText

Sends a trimmed text message. Returns `false` when the text is empty or a send is already in progress.

**Parameters**

| Name | In | Type | Required | Description |
|------|----|------|----------|-------------|
| `rawText` | argument | `String` | yes | User text. Blank input is rejected. |

**Responses**

| Result | Description |
|--------|-------------|
| `true` | Message appended. Sender flow starts when `sender` is set. |
| `false` | Empty text, or `state.isSending` is already true. |

```kotlin
controller.sendText("今晚吃什么")
```

### sendMessage

Same as `sendText`, plus attachments.

**Parameters**

| Name | In | Type | Required | Description |
|------|----|------|----------|-------------|
| `rawText` | argument | `String` | yes | May be empty when `attachments` is not. |
| `attachments` | argument | `List<MessagePart.Attachment>` | no | Image or file parts. Default `emptyList()`. |

```kotlin
controller.sendMessage("看看这张图", listOf(attachment))
```

### dispatch

Applies a `ChatIntent`. UI code should prefer this over mutating `ChatState` directly.

**Parameters**

| Name | In | Type | Required | Description |
|------|----|------|----------|-------------|
| `intent` | argument | `ChatIntent` | yes | `SendText`, `SendMessage`, `StopGeneration`, `RetryMessage`, `LoadEarlier`, `Refresh`, `ClearError`, `ClearUnread`, `DeleteMessage`, `UpsertMessage`, `ReplaceMessages`, `SetListNearBottom`. |

```kotlin
controller.dispatch(ChatIntent.StopGeneration)
controller.dispose()
scope.cancel()
```

Other methods on the same class, taken from source: `stopGeneration()`, `retryMessage(messageId)`, `loadEarlier()`, `refresh()`, `deleteMessage(messageId)`, `clearError()`, `clearUnread()`, `upsertExternalMessage(message)`, `replaceExternalMessages(messages, hasMoreEarlier)`, `dispose()`.

---

## Kuikly DSL

Package: `com.tencent.kuiklybase.chat.session.controller` and `com.tencent.kuiklybase.chat.composer`. Module: `kuiklychat-compat`.

### ChatControllerSession

Renders the AI chat page: header, message list, welcome state, scroll-to-bottom button, dock composer, and image preview. The parent must give it a bounded height (`flex(1f)`).

**Parameters**

| Name | In | Type | Required | Description |
|------|----|------|----------|-------------|
| `controller` | argument | `ChatController` | yes | State source. |
| `theme` | argument | `ChatTheme` | no | Default `ChatTheme.Default`. |
| `strings` | argument | `ChatStrings` | no | Default `ChatStrings.Default`. |
| `showDateSeparators` | argument | `Boolean` | no | Default `true`. |
| `composerBottomInset` | argument | `Float` | no | Safe-area inset passed to the dock. Demo uses `pagerData.safeAreaInsets.bottom`. |
| `headerTitle` | argument | `String` | no | Default `"对话"`. |
| `welcomeTitle` | argument | `String` | no | Default `"今天想聊点什么"`. |
| `welcomeSubtitle` | argument | `String` | no | Default `"点一条日常提示，或自己写"`. |
| `suggestions` | argument | `List<ChatSuggestion>` | no | Four built-in prompts when omitted. |
| `onCopyToClipboard` | argument | `((String) -> Unit)?` | no | Handles `ChatEffect.CopyToClipboard`. |

```kotlin
View {
    attr {
        flex(1f)
        flexDirection(FlexDirection.COLUMN)
    }
    ChatControllerSession(
        controller = controller,
        composerBottomInset = pagerData.safeAreaInsets.bottom,
    )
}
```

### ChatDockComposer

Bottom input used inside `ChatControllerSession`. Hosts can also place it themselves. The add button stays hidden unless `attachmentPicker` or `onPickAttachments` is set.

**Parameters** (`ChatDockComposerConfig`)

| Name | In | Type | Required | Description |
|------|----|------|----------|-------------|
| `placeholder` | config | `String` | no | Default `"输入消息"`. |
| `safeAreaBottom` | config | `Float` | no | Bottom inset. |
| `maxAttachmentCount` | config | `Int` | no | Default `9`. |
| `sending` | config | `() -> Boolean` | no | Return an observable-backed value so the button switches to stop. |
| `onSend` | config | `(String, List<MessagePart.Attachment>) -> Boolean` | no | Return whether the send was accepted. |
| `onStop` | config | `() -> Unit` | no | Stop control. |
| `onPickAttachments` | config | lambda or null | no | Host picker. `null` hides the add button. |

```kotlin
ChatDockComposer {
    placeholder = "输入消息"
    sending = { controller.state.isSending }
    onSend = { text, attachments -> controller.sendMessage(text, attachments) }
    onStop = { controller.dispatch(ChatIntent.StopGeneration) }
}
```

### ChatWelcomeState

Empty-state block. `ChatControllerSession` already shows one. `ChatSuggestion` fields are `text`, `prompt` (defaults to `text`), and optional `accentColor`.

```kotlin
ChatWelcomeState {
    title = "今天想聊点什么"
    subtitle = "点一条日常提示，或自己写"
    suggestions = listOf(ChatSuggestion(text = "今晚吃什么", prompt = "帮我挑三道家常菜"))
    onSuggestionClick = { controller.sendText(it.prompt) }
}
```

Image preview is `ChatZoomImage` (`viewName` `"ChatZoomImage"`). Android, iOS, and Harmony hosts must register a native view of that name. Pinch range in those views is 1x to 4x.

---

## Kuikly Compose DSL

Package: `com.tencent.kuiklybase.chat.compose.ui`. Module: `kuiklychat-compose`.

### ChatScreen

Compose entry bound to the same `ChatController`. Two overloads exist: one without user resolution, and one that takes `ChatUserResolutionConfig` (`cacheNamespace` must be non-blank).

**Parameters**

| Name | In | Type | Required | Description |
|------|----|------|----------|-------------|
| `controller` | argument | `ChatController` | yes | Same controller as the DSL session. |
| `userResolution` | argument | `ChatUserResolutionConfig` | no | Second overload only. |
| `theme` | argument | `ChatTheme` | no | Default `ChatTheme.Default`. |
| `strings` | argument | `ChatStrings` | no | Default `ChatStrings.Default`. |
| `config` | argument | `ChatScreenConfig` | no | Default `ChatScreenConfig.Default`. |
| `slots` | argument | `ChatSlots` | no | Default `ChatSlots.Default`. |
| `callbacks` | argument | `ChatCallbacks` | no | Default `ChatCallbacks.Empty`. |
| `rendererRegistry` | argument | `MessageRendererRegistry` | no | Default `defaultChatRendererRegistry()`. |

```kotlin
ChatScreen(
    controller = controller,
    theme = ChatTheme.Default,
)
```

*Layout and slot fields beyond this signature live on `ChatScreenConfig` and `ChatSlots` in the same package. This page does not restate every token.*

---

## ChatMessage

Package: `com.tencent.kuiklybase.chat.compose.core`.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Stable id. |
| `author` | `ChatUser` | `id`, `name`, `avatar`. |
| `parts` | `List<MessagePart>` | `Text`, `Attachment`, `AgentCardContent`, `Custom`, `SystemNotice`. |
| `status` | `MessageStatus` | `SENDING`, `STREAMING`, `SENT`, `FAILED`, `CANCELLED`. |
| `createdAt` | `Long` | Epoch millis. |
| `outgoing` | `Boolean` | True for the active user's own message. |
| `text` | `String` | Joined `MessagePart.Text` values. Not a constructor argument. |

```kotlin
ChatMessage(
    id = "user-1",
    author = ChatUser(id = "me", name = "我"),
    parts = listOf(MessagePart.Text("你好")),
    status = MessageStatus.SENT,
    createdAt = 1_758_000_000_000L,
    outgoing = true,
)
```
