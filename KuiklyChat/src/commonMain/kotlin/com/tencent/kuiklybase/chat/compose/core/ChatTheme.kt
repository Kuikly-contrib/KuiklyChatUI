package com.tencent.kuiklybase.chat.compose.core

/** Semantic tokens shared by Compose and the future Kuikly DSL renderer. */
data class ChatTheme(
    val backgroundColor: Long = 0xFFF8FAFC,
    val incomingBubbleColor: Long = 0xFFFFFFFF,
    val outgoingBubbleColor: Long = 0xFF2563EB,
    val incomingTextColor: Long = 0xFF1E293B,
    val outgoingTextColor: Long = 0xFFFFFFFF,
    val placeholderColor: Long = 0xFF94A3B8,
    val composerColor: Long = 0xFFFFFFFF,
    val borderColor: Long = 0xFFE2E8F0,
    val errorColor: Long = 0xFFDC2626,
    val accentColor: Long = 0xFF2563EB,
    val fontSize: Float = 16f,
    val smallFontSize: Float = 12f,
    val bubbleRadius: Float = 18f,
    val contentPadding: Float = 12f
) {
    companion object {
        val Default = ChatTheme()
    }
}

data class ChatStrings(
    val inputPlaceholder: String = "输入消息",
    val send: String = "发送",
    val stop: String = "停止",
    val loading: String = "加载中…",
    val loadEarlier: String = "加载更早消息",
    val empty: String = "暂无消息",
    val retry: String = "重试",
    val failed: String = "发送失败",
    val scrollToBottom: String = "回到底部"
) {
    companion object {
        val Default = ChatStrings()
    }
}
