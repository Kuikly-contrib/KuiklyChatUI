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
    val avatarBackgroundColor: Long = 0xFFCBD5E1,
    val avatarForegroundColor: Long = 0xFF334155,
    val timeTextColor: Long = 0xFF94A3B8,
    val systemBackgroundColor: Long = 0xFFE2E8F0,
    val systemTextColor: Long = 0xFF64748B,
    val dateSeparatorBgColor: Long = 0xFFCECECE,
    val dateSeparatorTextColor: Long = 0xFFFFFFFF,
    val fontSize: Float = 16f,
    val smallFontSize: Float = 12f,
    val bubbleRadius: Float = 18f,
    val bubbleRadiusSmall: Float = 4f,
    val systemRadius: Float = 12f,
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
    val sending: String = "发送中",
    val streaming: String = "生成中",
    val cancelled: String = "已取消",
    val scrollToBottom: String = "回到底部",
    val imageLoadFailed: String = "图片加载失败",
    val attachmentFallbackName: String = "附件",
    val photoLibraryLabel: String = "相册",
    val cameraLabel: String = "拍照",
    val fileLabel: String = "文件",
    val today: String = "今天",
    val yesterday: String = "昨天",
) {
    companion object {
        val Default = ChatStrings()
    }
}
