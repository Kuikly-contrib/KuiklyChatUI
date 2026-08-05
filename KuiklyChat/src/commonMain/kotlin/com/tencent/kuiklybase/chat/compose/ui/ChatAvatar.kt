package com.tencent.kuiklybase.chat.compose.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.compose.core.ChatTheme
import com.tencent.kuiklybase.chat.compose.core.ChatUser
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus

fun shouldShowAvatar(
    groupStatus: MessageGroupStatus,
    showAvatar: Boolean,
    hideAvatarWhenGrouped: Boolean
): Boolean {
    if (!showAvatar) return false
    if (!hideAvatarWhenGrouped) return true
    return groupStatus == MessageGroupStatus.SINGLE || groupStatus == MessageGroupStatus.FIRST
}

fun avatarInitial(user: ChatUser): String {
    val trimmed = user.name.trim()
    if (trimmed.isNotEmpty()) return trimmed.first().uppercaseChar().toString()
    if (user.id.isNotEmpty()) return user.id.first().uppercaseChar().toString()
    return "?"
}

@Composable
internal fun ChatAvatar(
    user: ChatUser,
    theme: ChatTheme,
    size: Dp,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    avatarContent: (@Composable (ChatUser, Dp) -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }
  Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(theme.avatarBackgroundColor))
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        if (avatarContent != null) {
            avatarContent(user, size)
        } else {
            Text(
                text = avatarInitial(user),
                color = Color(theme.avatarForegroundColor),
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
