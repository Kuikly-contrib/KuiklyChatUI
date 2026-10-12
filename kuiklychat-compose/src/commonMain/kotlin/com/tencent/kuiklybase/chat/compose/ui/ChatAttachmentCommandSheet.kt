package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.ModalBottomSheet
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.agentcard.LucideSvgIcon
import com.tencent.kuiklybase.chat.compose.core.ChatStrings
import com.tencent.kuiklybase.chat.compose.core.ChatTheme

/** Attachment actions shown in the DSH-style command bottom sheet. */
public enum class ChatAttachmentCommandTile {
    CAMERA,
    GALLERY,
    FILE,
}

internal const val CHAT_COMPOSER_PLUS_ICON = "kuikly_chat_ic_plus"

private fun tileIcon(tile: ChatAttachmentCommandTile): String = when (tile) {
    ChatAttachmentCommandTile.CAMERA -> "kuikly_chat_ic_camera"
    ChatAttachmentCommandTile.GALLERY -> "kuikly_chat_ic_image"
    ChatAttachmentCommandTile.FILE -> "kuikly_chat_ic_file"
}

private fun tileLabel(tile: ChatAttachmentCommandTile, strings: ChatStrings): String = when (tile) {
    ChatAttachmentCommandTile.CAMERA -> strings.cameraLabel
    ChatAttachmentCommandTile.GALLERY -> strings.photoLibraryLabel
    ChatAttachmentCommandTile.FILE -> strings.fileLabel
}

/**
 * DSH-style half-screen panel: title row + three attachment tiles (camera / gallery / file).
 */
@Composable
internal fun ChatAttachmentCommandSheet(
    visible: Boolean,
    tiles: List<ChatAttachmentCommandTile>,
    theme: ChatTheme,
    strings: ChatStrings,
    onDismiss: () -> Unit,
    onPickTile: (ChatAttachmentCommandTile) -> Unit,
) {
    if (!visible || tiles.isEmpty()) return
    ModalBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        containerColor = Color(theme.composerColor),
        scrimColor = Color(0x66000000),
        dismissOnDrag = true,
        dismissThreshold = 0.25f,
        animationDurationMillis = 250,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.attachmentSheetTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(theme.incomingTextColor),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "\u00D7",
                        fontSize = 20.sp,
                        color = Color(theme.placeholderColor),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                tiles.forEach { tile ->
                    ChatAttachmentCommandTileCell(
                        tile = tile,
                        label = tileLabel(tile, strings),
                        iconName = tileIcon(tile),
                        theme = theme,
                        onClick = {
                            onDismiss()
                            onPickTile(tile)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatAttachmentCommandTileCell(
    tile: ChatAttachmentCommandTile,
    label: String,
    iconName: String,
    theme: ChatTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(theme.incomingBubbleColor))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LucideSvgIcon(
            name = iconName,
            size = 28.dp,
            tint = Color(theme.placeholderColor),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(theme.placeholderColor),
        )
    }
}
