package com.tencent.kuiklybase.chat.session.controller

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuiklybase.chat.compose.core.ChatConversationSummary
import com.tencent.kuiklybase.chat.compose.core.ChatClock
import com.tencent.kuiklybase.chat.compose.core.ChatController
import com.tencent.kuiklybase.chat.compose.core.ChatDateTimeFormatter
import com.tencent.kuiklybase.chat.compose.core.ChatEffect
import com.tencent.kuiklybase.chat.compose.core.ChatIntent
import com.tencent.kuiklybase.chat.compose.core.ChatMessage
import com.tencent.kuiklybase.chat.compose.core.ChatState
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.DEFAULT_GROUPING_WINDOW_MILLIS
import com.tencent.kuiklybase.chat.compose.core.LocalChatDateTimeFormatter
import com.tencent.kuiklybase.chat.composer.ChatDockComposer
import com.tencent.kuiklybase.chat.composer.ChatDockComposerView
import com.tencent.kuiklybase.chat.experience.ChatSuggestion
import com.tencent.kuiklybase.chat.experience.ScrollDownFrost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Kuikly DSL session bound to the same [ChatController] as Compose [com.tencent.kuiklybase.chat.compose.ui.ChatScreen].
 *
 * Place it in a column that has a determined height (`flex(1f)`). State updates are applied on the
 * Kuikly thread. The parent must give this view a bounded height.
 *
 * [renderMessage] draws a message inside its bubble and returns true to skip the default text body.
 */
public fun ViewContainer<*, *>.ChatControllerSession(
    controller: ChatController,
    theme: ChatTheme = ChatTheme.Default,
    strings: ChatStrings = ChatStrings.Default,
    showDateSeparators: Boolean = true,
    dateTimeFormatter: ChatDateTimeFormatter = LocalChatDateTimeFormatter.Default,
    dateSeparatorClock: ChatClock = ChatClock.System,
    composerBottomInset: Float = 0f,
    loadEarlierThreshold: Float = 100f,
    onCopyToClipboard: ((String) -> Unit)? = null,
    headerTitle: String = "对话",
    welcomeTitle: String = "今天想聊点什么",
    welcomeSubtitle: String = "点一条日常提示，或自己写",
    suggestions: List<ChatSuggestion> = defaultControllerSuggestions(),
    renderMessage: (ViewContainer<*, *>.(ChatMessage) -> Boolean)? = null,
    conversationHost: ChatConversationHost? = null,
) {
    addChild(ChatControllerSessionView()) {
        attr {
            flex(1f)
            this.controller = controller
            this.theme = theme
            this.strings = strings
            this.showDateSeparators = showDateSeparators
            this.dateTimeFormatter = dateTimeFormatter
            this.dateSeparatorClock = dateSeparatorClock
            this.composerBottomInset = composerBottomInset
            this.loadEarlierThreshold = loadEarlierThreshold
            this.onCopyToClipboard = onCopyToClipboard
            this.headerTitle = headerTitle
            this.welcomeTitle = welcomeTitle
            this.welcomeSubtitle = welcomeSubtitle
            this.suggestions = suggestions
            this.renderMessage = renderMessage
            this.conversationHost = conversationHost
        }
    }
}

internal class ChatControllerSessionView : ComposeView<ChatControllerSessionAttr, ComposeEvent>() {
    internal var listRef: ViewRef<ListView<*, *>>? = null
    internal val markdownViews = mutableMapOf<String, SessionMarkdownView>()
    internal var typingTimer: com.tencent.kuikly.core.timer.Timer? = null
    private var composerRef: ViewRef<ChatDockComposerView>? = null
    internal var renameInputRef: ViewRef<InputView>? = null
    private var collectScope: CoroutineScope? = null
    private var savedSizeBeforeLoad = -1

    internal fun sessionAttr(): ChatControllerSessionAttr = attr

    override fun createAttr(): ChatControllerSessionAttr = ChatControllerSessionAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun viewDidLoad() {
        super.viewDidLoad()
        val pageId = pagerId
        val scope = CoroutineScope(SupervisorJob() + KuiklyPagerDispatcher { block ->
            com.tencent.kuikly.core.timer.setTimeout(pageId, 0) { block.run() }
        })
        collectScope = scope
        attr.controller.dispatch(ChatIntent.SetListNearBottom(true))
        reloadConversations()
        scope.launch {
            attr.controller.stateFlow.collect { state -> applyState(state) }
        }
        scope.launch {
            attr.controller.effects.collect { effect -> applyEffect(effect) }
        }
    }

    override fun viewWillUnload() {
        cancelTypingDots()
        collectScope?.cancel()
        collectScope = null
        super.viewWillUnload()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    backgroundColor(Color(ctx.attr.theme.backgroundColor))
                }
                View {
                    attr {
                        absolutePositionAllZero()
                        zIndex(2)
                        flexDirection(FlexDirection.COLUMN)
                        backgroundColor(Color(ctx.attr.theme.backgroundColor))
                        transform(Translate(if (ctx.attr.drawerPresented) 0.78f else 0f, 0f))
                        animation(Animation.easeOut(0.24f), ctx.attr.drawerPresented)
                    }
                    vif({ ctx.attr.messages.isNotEmpty() || ctx.attr.conversationHost != null }) {
                        ctx.sessionHeader(this)
                    }
                    vif({ ctx.attr.errorText.isNotEmpty() }) {
                        View {
                            attr {
                                backgroundColor(Color(ctx.attr.theme.errorColor))
                                padding(10f, 16f, 10f, 16f)
                            }
                            event {
                                click {
                                    ctx.dismissComposerKeyboard()
                                    ctx.attr.errorText = ""
                                    ctx.attr.controller.dispatch(ChatIntent.ClearError)
                                }
                            }
                            Text {
                                attr {
                                    text("${ctx.attr.strings.failed}: ${ctx.attr.errorText}")
                                    fontSize(ctx.attr.theme.smallFontSize)
                                    color(Color(ctx.attr.theme.outgoingTextColor))
                                }
                            }
                        }
                    }
                    View {
                        attr {
                            flex(1f)
                            overflow(true)
                        }
                        List {
                            ref { ctx.listRef = it }
                            attr { flex(1f) }
                            event {
                                click { ctx.dismissComposerKeyboard() }
                                dragBegin { ctx.dismissComposerKeyboard() }
                                scroll { params ->
                                    ctx.updateNearBottom(params.offsetY, params.viewHeight, params.contentHeight)
                                    ctx.maybeLoadEarlier(params.offsetY)
                                }
                                scrollEnd { params ->
                                    ctx.updateNearBottom(params.offsetY, params.viewHeight, params.contentHeight)
                                    ctx.maybeLoadEarlier(params.offsetY)
                                }
                            }
                            View {
                                attr { alignSelfStretch() }
                                vif({ ctx.attr.isLoadingEarlier || ctx.attr.hasMoreEarlier }) {
                                    View {
                                        attr { allCenter(); paddingTop(12f); paddingBottom(8f) }
                                        event {
                                            click { ctx.attr.controller.dispatch(ChatIntent.LoadEarlier) }
                                        }
                                        Text {
                                            attr {
                                                text(
                                                    if (ctx.attr.isLoadingEarlier) ctx.attr.strings.loading
                                                    else ctx.attr.strings.loadEarlier
                                                )
                                                fontSize(ctx.attr.theme.smallFontSize)
                                                color(Color(ctx.attr.theme.placeholderColor))
                                            }
                                        }
                                    }
                                }
                            }
                            vfor({ ctx.attr.messages }) { message ->
                                controllerSessionRow(ctx, message.id)
                            }
                        }
                        vif({ ctx.attr.messages.isEmpty() && ctx.attr.isInitialLoading }) {
                            ctx.centeredOverlay(this, ctx.attr.strings.loading)
                        }
                        vif({ ctx.attr.messages.isEmpty() && !ctx.attr.isInitialLoading }) {
                            ctx.sessionWelcome(this)
                        }
                        vif({ !ctx.attr.nearBottom && ctx.attr.messages.isNotEmpty() }) {
                            View {
                                attr {
                                    positionAbsolute()
                                    right(16f)
                                    bottom(16f)
                                    size(44f, 44f)
                                    borderRadius(22f)
                                    border(Border(0.5f, BorderStyle.SOLID, Color(0xB8FFFFFF)))
                                    allCenter()
                                    accessibility(ctx.attr.strings.scrollToBottom)
                                }
                                event { click { ctx.scrollSessionToLatest(clearUnread = true) } }
                                ScrollDownFrost(44f)
                                scrollDownMark(ctx.attr.theme.incomingTextColor)
                            }
                        }
                    }
                    ChatDockComposer(
                        viewRef = { ctx.composerRef = it },
                    ) {
                        theme = ctx.attr.theme
                        placeholder = ctx.attr.strings.inputPlaceholder
                        expandedPlaceholder = ctx.attr.strings.inputPlaceholder
                        safeAreaBottom = ctx.attr.composerBottomInset
                        attachmentTitle = ctx.attr.strings.attachmentSheetTitle
                        cameraLabel = ctx.attr.strings.cameraLabel
                        galleryLabel = ctx.attr.strings.photoLibraryLabel
                        sending = { ctx.attr.isSending }
                        onSend = { text, attachments -> ctx.attr.controller.sendMessage(text, attachments) }
                        onStop = { ctx.attr.controller.dispatch(ChatIntent.StopGeneration) }
                        onPreviewAttachment = { ctx.openSentImagePreview(it) }
                    }
                    vif({ ctx.attr.previewImageUri.isNotEmpty() }) {
                        ctx.imagePreviewOverlay(this)
                    }
                vif({ ctx.attr.drawerMounted && ctx.attr.conversationHost != null }) {
                    ctx.conversationDrawerScrim(this)
                }
                }
                vif({ ctx.attr.drawerMounted && ctx.attr.conversationHost != null }) {
                    ctx.conversationDrawer(this)
                }
                vif({ ctx.attr.renameConversationId.isNotEmpty() }) {
                    ctx.renameDialog(this)
                }
            }
        }
    }

    private fun applyState(state: ChatState) {
        if (collectScope?.isActive != true) return
        val wasLoading = attr.isLoadingEarlier
        if (!wasLoading && state.isLoadingEarlier) {
            savedSizeBeforeLoad = attr.messages.size
        }
        publishMessageSnapshot(state)
        if (wasLoading && !state.isLoadingEarlier && savedSizeBeforeLoad >= 0) {
            val delta = attr.messages.size - savedSizeBeforeLoad
            if (delta > 0) scrollSessionToMessageIndex(delta)
            savedSizeBeforeLoad = -1
        }
        attr.isSending = state.isSending
        attr.isInitialLoading = state.isInitialLoading
        attr.isLoadingEarlier = state.isLoadingEarlier
        attr.hasMoreEarlier = state.hasMoreEarlier
        attr.unreadCount = state.unreadCount
        attr.errorText = state.error?.message.orEmpty()
    }

    private fun applyEffect(effect: ChatEffect) {
        if (collectScope?.isActive != true) return
        when (effect) {
            ChatEffect.ScrollToBottom -> scrollSessionToLatest(clearUnread = false)
            is ChatEffect.ShowError -> attr.errorText = effect.message
            is ChatEffect.CopyToClipboard -> attr.onCopyToClipboard?.invoke(effect.text)
        }
    }

    private fun maybeLoadEarlier(offsetY: Float) {
        if (offsetY > attr.loadEarlierThreshold) return
        if (!attr.hasMoreEarlier || attr.isLoadingEarlier || attr.isInitialLoading) return
        if (attr.messages.isEmpty()) return
        attr.controller.dispatch(ChatIntent.LoadEarlier)
    }

    private fun centeredOverlay(container: ViewContainer<*, *>, label: String) {
        val theme = attr.theme
        container.View {
            attr {
                positionAbsolute()
                top(0f)
                left(0f)
                right(0f)
                bottom(0f)
                allCenter()
            }
            Text {
                attr {
                    text(label)
                    fontSize(theme.fontSize)
                    color(Color(theme.placeholderColor))
                }
            }
        }
    }

    internal fun dismissComposerKeyboard() {
        composerRef?.view?.dismissKeyboard()
    }

    internal fun sendPrompt(text: String) {
        if (attr.isSending) return
        dismissComposerKeyboard()
        attr.controller.sendText(text)
    }

}

internal class ChatControllerSessionAttr : ComposeAttr() {
    lateinit var controller: ChatController
    var theme: ChatTheme = ChatTheme.Default
    var strings: ChatStrings = ChatStrings.Default
    var showDateSeparators: Boolean = true
    var dateTimeFormatter: ChatDateTimeFormatter = LocalChatDateTimeFormatter.Default
    var dateSeparatorClock: ChatClock = ChatClock.System
    var composerBottomInset: Float = 0f
    var loadEarlierThreshold: Float = 100f
    var groupingWindowMillis: Long = DEFAULT_GROUPING_WINDOW_MILLIS
    var onCopyToClipboard: ((String) -> Unit)? = null
    var headerTitle: String = "对话"
    var welcomeTitle: String = "今天想聊点什么"
    var welcomeSubtitle: String = "点一条日常提示，或自己写"
    var suggestions: List<ChatSuggestion> = emptyList()
    var renderMessage: (ViewContainer<*, *>.(ChatMessage) -> Boolean)? = null
    var conversationHost: ChatConversationHost? = null

    var messages by observableList<ChatMessage>()
    var conversations by observableList<ChatConversationSummary>()
    var archivedConversations by observableList<ChatConversationSummary>()
    var snapshotEpoch by observable(0)
    var isSending by observable(false)
    var isInitialLoading by observable(false)
    var isLoadingEarlier by observable(false)
    var hasMoreEarlier by observable(false)
    var unreadCount by observable(0)
    var errorText by observable("")
    var nearBottom by observable(true)
    var stickToBottom by observable(true)
    var previewImageUri by observable("")
    var liveMessageId by observable("")
    var liveText by observable("")
    var typingPhase by observable(0)
    var actionMessageId by observable("")
    var drawerOpen by observable(false)
    var drawerMounted by observable(false)
    var drawerPresented by observable(false)
    var menuConversationId by observable("")
    var menuAnchorX by observable(0f)
    var menuAnchorY by observable(0f)
    var renameConversationId by observable("")
    var renameDraft by observable("")
    var activeConversationId by observable("")
    var conversationTitle by observable("对话")
}
