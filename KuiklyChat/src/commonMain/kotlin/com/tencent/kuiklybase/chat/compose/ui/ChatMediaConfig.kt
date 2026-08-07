package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuiklybase.chat.compose.core.ChatMediaContext
import com.tencent.kuiklybase.chat.compose.core.ChatMediaLoadEvent
import com.tencent.kuiklybase.chat.compose.core.ChatMediaLoader
import com.tencent.kuiklybase.chat.compose.core.ChatMediaRequest
import com.tencent.kuiklybase.chat.compose.core.ChatCrossCache
import com.tencent.kuiklybase.chat.compose.core.ChatByteCacheModule
import com.tencent.kuiklybase.chat.compose.core.DEFAULT_CHAT_MEDIA_CACHE_NAMESPACE
import com.tencent.kuiklybase.chat.compose.core.createChatByteCache
import kotlinx.coroutines.flow.flowOf

fun interface ChatMediaRequestResolver {
    fun resolve(context: ChatMediaContext): ChatMediaRequest

    companion object {
        val Default = ChatMediaRequestResolver { context ->
            ChatMediaRequest(
                source = context.source,
                mimeType = (context as? ChatMediaContext.AttachmentPreview)
                    ?.attachment
                    ?.mimeType
                    ?.takeIf(String::isNotBlank),
            )
        }
    }
}

/**
 * Media loading customization for [ChatScreen].
 *
 * When [loader] is null, the SDK owns a persistent [ChatCrossCache] backed by the current platform.
 * An injected loader remains caller-owned and is never closed by the SDK.
 */
data class ChatMediaConfig(
    val loader: ChatMediaLoader? = null,
    val requestResolver: ChatMediaRequestResolver = ChatMediaRequestResolver.Default,
    val cacheNamespace: String = DEFAULT_CHAT_MEDIA_CACHE_NAMESPACE,
) {
    companion object {
        val Default = ChatMediaConfig()
    }
}

internal data class ChatMediaRuntime(
    val loader: ChatMediaLoader,
    val requestResolver: State<ChatMediaRequestResolver>,
)

internal class ChatMediaLoaderHandle(
    val loader: ChatMediaLoader,
    private val ownsLoader: Boolean,
) {
    private var closed = false

    fun close() {
        if (closed) return
        closed = true
        if (ownsLoader) loader.close()
    }
}

internal fun createChatMediaLoaderHandle(
    injectedLoader: ChatMediaLoader?,
    defaultFactory: () -> ChatMediaLoader = ::PassthroughChatMediaLoader,
): ChatMediaLoaderHandle {
    return if (injectedLoader != null) {
        ChatMediaLoaderHandle(loader = injectedLoader, ownsLoader = false)
    } else {
        ChatMediaLoaderHandle(loader = defaultFactory(), ownsLoader = true)
    }
}

/**
 * Creates a caller-owned [ChatCrossCache] backed by the current platform and NetworkModule.
 *
 * The returned cache is closed automatically when this composition leaves. Pass it to
 * `ChatMediaConfig(loader = cache)` when direct get/set access or cross-component sharing is needed.
 */
@Composable
fun rememberChatCrossCache(
    namespace: String = DEFAULT_CHAT_MEDIA_CACHE_NAMESPACE,
): ChatCrossCache {
    val pager = LocalActivity.current.getPager()
    val cache = remember(namespace, pager) {
        createDefaultChatCrossCache(namespace, pager)
    }
    DisposableEffect(cache) {
        onDispose { cache.close() }
    }
    return cache
}

@Composable
internal fun rememberChatMediaRuntime(config: ChatMediaConfig): ChatMediaRuntime {
    val pager = LocalActivity.current.getPager()
    val handle = remember(config.loader, config.cacheNamespace, pager) {
        createChatMediaLoaderHandle(config.loader) {
            createDefaultChatCrossCache(config.cacheNamespace, pager)
        }
    }
    DisposableEffect(handle) {
        onDispose { handle.close() }
    }
    val latestResolver = rememberUpdatedState(config.requestResolver)
    return remember(handle, latestResolver) {
        ChatMediaRuntime(
            loader = handle.loader,
            requestResolver = latestResolver,
        )
    }
}

private fun createDefaultChatCrossCache(
    namespace: String,
    pager: com.tencent.kuikly.core.pager.IPager,
): ChatCrossCache = ChatCrossCache(
    store = createChatByteCache(namespace, pager),
    fetcher = KuiklyChatMediaFetcher(
        networkModule = {
            pager.getModule<NetworkModule>(NetworkModule.MODULE_NAME)
        },
        byteCacheModule = {
            pager.getModule<ChatByteCacheModule>(ChatByteCacheModule.MODULE_NAME)
        },
    ),
)

internal class PassthroughChatMediaLoader : ChatMediaLoader {
    private var closed = false

    override fun load(request: ChatMediaRequest) = if (closed) {
        flowOf(ChatMediaLoadEvent.Failed("Media loader is closed"))
    } else {
        flowOf(ChatMediaLoadEvent.Ready(request.source))
    }

    override fun close() {
        closed = true
    }
}
