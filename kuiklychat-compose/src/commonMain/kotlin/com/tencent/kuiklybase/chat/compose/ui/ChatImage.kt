package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.coil3.AsyncImagePainter
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatMediaContext
import com.tencent.kuiklybase.chat.compose.core.ChatMediaLoadEvent
import com.tencent.kuiklybase.chat.compose.core.ChatMediaLoader
import com.tencent.kuiklybase.chat.compose.core.ChatMediaRequest
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach

@Composable
internal fun ChatImage(
    mediaContext: ChatMediaContext,
    contentDescription: String?,
    modifier: Modifier,
    theme: ChatTheme,
    strings: ChatStrings,
    imageContent: (@Composable (String, String?, Modifier) -> Unit)?,
    mediaRuntime: ChatMediaRuntime,
    errorContent: @Composable () -> Unit,
) {
    val source = mediaContext.source
    if (imageContent != null) {
        imageContent(source, contentDescription, modifier)
        return
    }

    val request = try {
        mediaRuntime.requestResolver.value.resolve(mediaContext)
    } catch (_: Exception) {
        null
    }
    if (request == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            errorContent()
        }
        return
    }

    val loadState by produceState<ChatMediaImageState>(
        initialValue = ChatMediaImageState.Loading(),
        mediaRuntime.loader,
        request,
    ) {
        value = ChatMediaImageState.Loading()
        try {
            val terminalEvent = awaitTerminalMediaEvent(
                loader = mediaRuntime.loader,
                request = request,
                onEvent = { event ->
                    if (event is ChatMediaLoadEvent.Progress) {
                        value = ChatMediaImageState.Loading(
                            receivedBytes = event.receivedBytes,
                            totalBytes = event.totalBytes,
                        )
                    }
                },
            )
            value = when (terminalEvent) {
                is ChatMediaLoadEvent.Ready -> ChatMediaImageState.Ready(terminalEvent.displaySource)
                is ChatMediaLoadEvent.Failed -> ChatMediaImageState.Failed
                is ChatMediaLoadEvent.Progress -> {
                    error("awaitTerminalMediaEvent returned a non-terminal event")
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            value = ChatMediaImageState.Failed
        }
    }

    Box(
        modifier = modifier.background(Color(theme.avatarBackgroundColor)),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = loadState) {
            is ChatMediaImageState.Loading -> {
                if (mediaContext is ChatMediaContext.AttachmentPreview) {
                    ChatImageLoadingPlaceholder(state, theme, strings)
                }
            }
            is ChatMediaImageState.Ready -> {
                if (isDisplayableImageSource(state.displaySource)) {
                    NativeChatImage(
                        source = state.displaySource,
                        contentDescription = contentDescription,
                        errorContent = errorContent,
                    )
                } else {
                    errorContent()
                }
            }
            ChatMediaImageState.Failed -> errorContent()
        }
    }
}

@Composable
private fun ChatImageLoadingPlaceholder(
    state: ChatMediaImageState.Loading,
    theme: ChatTheme,
    strings: ChatStrings,
) {
    val percent = mediaProgressPercent(state.receivedBytes, state.totalBytes)
    Text(
        text = percent?.let { "$it%" } ?: strings.loading,
        color = Color(theme.placeholderColor),
        fontSize = theme.smallFontSize.sp,
    )
}

internal fun mediaProgressPercent(receivedBytes: Long, totalBytes: Long?): Int? {
    if (totalBytes == null || totalBytes <= 0L) return null
    return ((receivedBytes.coerceIn(0L, totalBytes).toDouble() / totalBytes) * 100)
        .toInt()
        .coerceIn(0, 100)
}

internal suspend fun awaitTerminalMediaEvent(
    loader: ChatMediaLoader,
    request: ChatMediaRequest,
    onEvent: (ChatMediaLoadEvent) -> Unit = {},
): ChatMediaLoadEvent {
    return loader.load(request)
        .onEach(onEvent)
        .firstOrNull { event ->
            event is ChatMediaLoadEvent.Ready || event is ChatMediaLoadEvent.Failed
        }
        ?: ChatMediaLoadEvent.Failed("Media loader completed without a terminal event")
}

@Composable
private fun NativeChatImage(
    source: String,
    contentDescription: String?,
    errorContent: @Composable () -> Unit,
) {
    var failed by remember(source) { mutableStateOf(false) }
    val painter = rememberAsyncImagePainter(
        model = source,
        onState = { state ->
            failed = state is AsyncImagePainter.State.Error
        },
    )
    if (failed) {
        errorContent()
    } else {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal sealed interface ChatMediaImageState {
    data class Loading(
        val receivedBytes: Long = 0L,
        val totalBytes: Long? = null,
    ) : ChatMediaImageState

    data class Ready(
        val displaySource: String,
    ) : ChatMediaImageState

    data object Failed : ChatMediaImageState
}

@Composable
internal fun ChatImageErrorPlaceholder(
    theme: ChatTheme,
    strings: ChatStrings,
    fontSize: Float = theme.smallFontSize,
) {
    Text(
        text = strings.imageLoadFailed,
        color = Color(theme.placeholderColor),
        fontSize = fontSize.sp,
    )
}
