package com.tencent.kuiklybase.chat.compose.ui

import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuiklybase.chat.compose.core.MessageGroupStatus

data class BubbleCornerRadii(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp
)

/** Corner radii for grouped bubbles; the pointed side uses small radius when grouped. */
fun bubbleCornerRadii(
    groupStatus: MessageGroupStatus,
    isMine: Boolean,
    largeRadius: Dp,
    smallRadius: Dp = 4.dp
): BubbleCornerRadii {
    return when (groupStatus) {
        MessageGroupStatus.SINGLE -> BubbleCornerRadii(largeRadius, largeRadius, largeRadius, largeRadius)
        MessageGroupStatus.FIRST -> if (isMine) {
            BubbleCornerRadii(largeRadius, largeRadius, smallRadius, largeRadius)
        } else {
            BubbleCornerRadii(largeRadius, largeRadius, largeRadius, smallRadius)
        }
        MessageGroupStatus.MIDDLE -> if (isMine) {
            BubbleCornerRadii(largeRadius, smallRadius, smallRadius, largeRadius)
        } else {
            BubbleCornerRadii(smallRadius, largeRadius, largeRadius, smallRadius)
        }
        MessageGroupStatus.LAST -> if (isMine) {
            BubbleCornerRadii(largeRadius, smallRadius, largeRadius, largeRadius)
        } else {
            BubbleCornerRadii(smallRadius, largeRadius, largeRadius, largeRadius)
        }
    }
}

fun groupedBubbleShape(
    groupStatus: MessageGroupStatus,
    isMine: Boolean,
    largeRadius: Dp,
    smallRadius: Dp = 4.dp
): RoundedCornerShape {
    val radii = bubbleCornerRadii(groupStatus, isMine, largeRadius, smallRadius)
    return RoundedCornerShape(
        topStart = radii.topStart,
        topEnd = radii.topEnd,
        bottomEnd = radii.bottomEnd,
        bottomStart = radii.bottomStart
    )
}

fun messageRowTopPadding(groupStatus: MessageGroupStatus): Dp =
    if (groupStatus == MessageGroupStatus.MIDDLE || groupStatus == MessageGroupStatus.LAST) {
        2.dp
    } else {
        8.dp
    }
