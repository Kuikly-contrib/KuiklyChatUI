package com.tencent.kuiklybase.chat.composer

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.MessagePart
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentPicker
import com.tencent.kuiklybase.chat.kuiklyai.platform.LlmAttachmentSource
import com.tencent.kuikly.core.views.TextAreaView
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Floating chat composer for Kuikly DSL pages.
 *
 * The dock expands when focused, grows with the text, lifts with the keyboard, and
 * can attach photos through [ChatDockComposerConfig.attachmentPicker] or
 * [ChatDockComposerConfig.onPickAttachments]. The add button opens a modal for
 * camera and gallery. Pass neither and the add button stays hidden.
 *
 * [sending] is read while the dock renders. Return an observable-backed value so the
 * send control switches to stop while a reply is in flight.
 */
public class ChatDockComposerConfig {
    public var theme: ChatTheme = ChatTheme.Default
    public var placeholder: String = "输入消息"
    public var expandedPlaceholder: String = "输入消息"
    public var safeAreaBottom: Float = 0f
    public var maxAttachmentCount: Int = 9
    public var attachmentSource: LlmAttachmentSource = LlmAttachmentSource.PHOTO_LIBRARY
    public var attachmentPicker: LlmAttachmentPicker? = null
    public var sending: () -> Boolean = { false }
    public var onSend: (text: String, attachments: List<MessagePart.Attachment>) -> Boolean = { _, _ -> false }
    public var onStop: () -> Unit = {}
    public var attachmentTitle: String = "添加附件"
    public var cameraLabel: String = "拍照"
    public var galleryLabel: String = "相册"
    public var onPickAttachments: ((remaining: Int, accept: (List<MessagePart.Attachment>) -> Unit) -> Unit)? = null
    public var onPreviewAttachment: (String) -> Unit = {}
    public var onKeyboardHeightChange: (height: Float) -> Unit = {}
}

public fun ViewContainer<*, *>.ChatDockComposer(
    configure: ChatDockComposerConfig.() -> Unit = {},
) {
    addChatDockComposer(viewRef = null, configure = configure)
}

internal fun ViewContainer<*, *>.ChatDockComposer(
    viewRef: (ViewRef<ChatDockComposerView>) -> Unit,
    configure: ChatDockComposerConfig.() -> Unit = {},
) {
    addChatDockComposer(viewRef = viewRef, configure = configure)
}

private fun ViewContainer<*, *>.addChatDockComposer(
    viewRef: ((ViewRef<ChatDockComposerView>) -> Unit)?,
    configure: ChatDockComposerConfig.() -> Unit,
) {
    val config = ChatDockComposerConfig().apply(configure)
    addChild(ChatDockComposerView()) {
        if (viewRef != null) {
            ref(viewRef)
        }
        attr {
            this.config = config
        }
    }
}

internal class ChatDockComposerView : ComposeView<ChatDockComposerAttr, ComposeEvent>() {
    internal fun dockAttr(): ChatDockComposerAttr = attr
    internal var textAreaRef: ViewRef<TextAreaView>? = null
    private val drafts = mutableListOf<MessagePart.Attachment>()
    private var pickScope: CoroutineScope? = null
    private var ignoreKeyboardLift = false

    override fun createAttr(): ChatDockComposerAttr = ChatDockComposerAttr()
    override fun createEvent(): ComposeEvent = ComposeEvent()

    override fun viewDidLoad() {
        super.viewDidLoad()
        val pageId = pagerId
        pickScope = CoroutineScope(SupervisorJob() + ComposerPagerDispatcher { block ->
            com.tencent.kuikly.core.timer.setTimeout(pageId, 0) { block.run() }
        })
        val sources = resolvePicker()?.supportedSources.orEmpty()
        attr.showCamera = LlmAttachmentSource.CAMERA in sources
        attr.showGallery = LlmAttachmentSource.PHOTO_LIBRARY in sources || attr.config.onPickAttachments != null
    }

    override fun viewWillUnload() {
        pickScope?.cancel()
        pickScope = null
        super.viewWillUnload()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                val lines = ctx.attr.lineCount.coerceIn(1, DOCK_MAX_INPUT_LINES)
                val expanded = ctx.attr.expanded || ctx.attr.previewCount > 0
                // Absolute children inside the panel do not reserve column space.
                // Without this height the list draws underneath the dock and text is clipped.
                height(composerPanelHeight(expanded, lines, ctx.attr.previewCount > 0, ctx.attr.textAboveIcons))
                marginLeft(16f)
                marginRight(16f)
                marginBottom(
                    composerDockBottom(ctx.attr.keyboardHeight, ctx.attr.config.safeAreaBottom),
                )
            }
            ctx.renderDock(this)
        }
    }

    internal fun syncDraftLayout(text: String): Boolean {
        val fontSize = attr.config.theme.fontSize.coerceAtLeast(1f)
        val pageWidth = pagerData.pageViewWidth
        val besideIcons = (pageWidth - 144f).coerceAtLeast(fontSize * 4f)
        val fullWidth = (pageWidth - 64f).coerceAtLeast(fontSize * 8f)
        val aboveIcons = estimateComposerLineCount(
            text,
            DOCK_MAX_INPUT_LINES,
            charsAcross(besideIcons, fontSize),
        ) > 1
        val nextLines = if (aboveIcons) {
            estimateComposerLineCount(text, DOCK_MAX_INPUT_LINES, charsAcross(fullWidth, fontSize))
        } else {
            1
        }
        val reflow = aboveIcons != attr.textAboveIcons || nextLines != attr.lineCount
        attr.draftText = text
        attr.textAboveIcons = aboveIcons
        attr.lineCount = nextLines
        return reflow
    }

    private fun charsAcross(width: Float, fontSize: Float): Int {
        return (width / (fontSize * 1.15f)).toInt().coerceAtLeast(4)
    }

    internal fun onFocusChanged(focused: Boolean) {
        attr.focused = focused
        if (focused) {
            ignoreKeyboardLift = false
            closeAttachmentSheet()
        } else {
            settleDockToBottom()
            if (!attr.attachmentTrayOpen && attr.draftText.isEmpty() && attr.previewCount == 0) {
                attr.expanded = false
            }
        }
    }

    internal fun onKeyboardChanged(height: Float, visible: Boolean) {
        if (!attr.focused) {
            settleDockToBottom()
            return
        }
        if (ignoreKeyboardLift && visible && height > 0.5f) return
        val next = if (visible && height > 0.5f) height.coerceAtLeast(0f) else 0f
        if (!visible || height <= 0.5f) ignoreKeyboardLift = false
        if (next != attr.keyboardHeight) {
            attr.keyboardHeight = next
            attr.config.onKeyboardHeightChange(next)
        }
    }

    private fun settleDockToBottom() {
        ignoreKeyboardLift = true
        if (attr.keyboardHeight != 0f) {
            attr.keyboardHeight = 0f
            attr.config.onKeyboardHeightChange(0f)
        }
    }

    internal fun dismissKeyboard() {
        closeAttachmentSheet()
        blurField()
        onFocusChanged(false)
    }

    internal fun toggleAttachmentTray() {
        if (attr.picking) return
        if (attr.attachmentTrayOpen || attr.sheetPresented) {
            closeAttachmentSheet()
            return
        }
        openAttachmentSheet()
    }

    internal fun openAttachmentSheet() {
        if (attr.picking) return
        blurField()
        attr.attachmentTrayOpen = true
        attr.sheetMounted = true
        val pageId = pagerId
        com.tencent.kuikly.core.timer.setTimeout(pageId, 16) {
            if (attr.attachmentTrayOpen) attr.sheetPresented = true
        }
    }

    internal fun closeAttachmentSheet() {
        attr.attachmentTrayOpen = false
        val closing = attr.sheetMounted
        attr.sheetPresented = false
        if (!closing) return
        val pageId = pagerId
        com.tencent.kuikly.core.timer.setTimeout(pageId, 340) {
            if (!attr.sheetPresented && !attr.attachmentTrayOpen) attr.sheetMounted = false
        }
    }

    internal fun previewAttachment(preview: String) {
        val index = attr.previewUris.indexOf(preview)
        val full = if (index in drafts.indices) drafts[index].uri else preview
        attr.config.onPreviewAttachment(full.ifEmpty { preview })
    }

    private fun blurField() {
        textAreaRef?.view?.blur()
        attr.focused = false
        settleDockToBottom()
    }

    internal fun performSend() {
        if (attr.config.sending()) {
            attr.config.onStop()
            return
        }
        val text = attr.draftText.trim()
        if (text.isEmpty() && drafts.isEmpty()) return
        val sent = attr.config.onSend(text, drafts.toList())
        if (!sent) return
        blurField()
        textAreaRef?.view?.setText("")
        attr.draftText = ""
        attr.lineCount = 1
        attr.textAboveIcons = false
        drafts.clear()
        attr.previewUris.clear()
        attr.previewCount = 0
        closeAttachmentSheet()
        attr.expanded = false
    }

    internal fun pickAttachment(source: LlmAttachmentSource) {
        if (attr.picking) return
        val remaining = attr.config.maxAttachmentCount - drafts.size
        if (remaining <= 0) return
        closeAttachmentSheet()
        blurField()
        if (source == LlmAttachmentSource.PHOTO_LIBRARY) {
            val custom = attr.config.onPickAttachments
            if (custom != null) {
                custom(remaining) { picked -> addDrafts(picked.take(remaining)) }
                return
            }
        }
        val picker = resolvePicker() ?: return
        attr.picking = true
        pickScope?.launch {
            val picked = runCatching { picker.pick(source, remaining) }.getOrDefault(emptyList())
            if (!isActive) return@launch
            val pageId = pagerId
            com.tencent.kuikly.core.timer.setTimeout(pageId, 0) {
                attr.picking = false
                if (pickScope?.isActive != true) return@setTimeout
                addDrafts(picked.map { it.toMessageAttachment() }.take(remaining))
            }
        } ?: run { attr.picking = false }
    }

    internal fun removePreview(preview: String) {
        val index = attr.previewUris.indexOf(preview)
        if (index < 0) return
        attr.previewUris.removeAt(index)
        attr.previewCount = attr.previewUris.size
        if (index < drafts.size) drafts.removeAt(index)
        if (attr.draftText.isEmpty() && attr.previewCount == 0 && !attr.focused && !attr.attachmentTrayOpen) {
            attr.expanded = false
        }
    }

    internal fun canAttach(): Boolean = resolvePicker() != null || attr.config.onPickAttachments != null

    private fun addDrafts(items: List<MessagePart.Attachment>) {
        items.forEach { item ->
            if (drafts.size >= attr.config.maxAttachmentCount) return@forEach
            if (item.id.isBlank() || item.uri.isBlank()) return@forEach
            if (drafts.any { it.id == item.id }) return@forEach
            drafts.add(item)
            attr.previewUris.add(item.previewUri.ifEmpty { item.uri })
        }
        attr.previewCount = attr.previewUris.size
        if (attr.previewCount > 0) attr.expanded = true
    }

    @Suppress("DEPRECATION")
    private fun resolvePicker(): LlmAttachmentPicker? {
        return attr.config.attachmentPicker
            ?: com.tencent.kuiklybase.chat.kuiklyai.platform.LlmChatPlatformRegistry.current?.attachmentPicker
    }
}

internal class ChatDockComposerAttr : ComposeAttr() {
    var config: ChatDockComposerConfig = ChatDockComposerConfig()
    var draftText by observable("")
    var lineCount by observable(1)
    var textAboveIcons by observable(false)
    var expanded by observable(false)
    var focused by observable(false)
    var keyboardHeight by observable(0f)
    var picking by observable(false)
    var attachmentTrayOpen by observable(false)
    var sheetMounted by observable(false)
    var sheetPresented by observable(false)
    var showCamera by observable(false)
    var showGallery by observable(false)
    var previewUris by observableList<String>()

    /** Plain count so panel height and the native field observe a value, not list size. */
    var previewCount by observable(0)

    fun previews(): ObservableList<String> = previewUris
}

private class ComposerPagerDispatcher(
    private val schedule: (Runnable) -> Unit,
) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        schedule(block)
    }
}
