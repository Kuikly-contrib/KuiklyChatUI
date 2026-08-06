package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme

@Composable
internal fun ChatImage(
    source: String,
    contentDescription: String?,
    modifier: Modifier,
    theme: ChatTheme,
    strings: ChatStrings,
    imageContent: (@Composable (String, String?, Modifier) -> Unit)?,
    errorContent: @Composable () -> Unit,
) {
    if (imageContent != null) {
        imageContent(source, contentDescription, modifier)
        return
    }
    if (!isDisplayableImageSource(source)) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            errorContent()
        }
        return
    }
    var failed by remember(source) { mutableStateOf(false) }
    val painter = rememberAsyncImagePainter(
        model = source,
        onState = { state ->
            failed = state is AsyncImagePainter.State.Error
        },
    )
    Box(
        modifier = modifier.background(Color(theme.avatarBackgroundColor)),
        contentAlignment = Alignment.Center,
    ) {
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
