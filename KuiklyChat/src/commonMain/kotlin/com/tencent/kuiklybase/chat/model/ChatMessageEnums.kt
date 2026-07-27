package com.tencent.kuiklybase.chat.model

/**
 * 聊天消息类型
 */
enum class MessageType {
    TEXT,       // 文本消息
    IMAGE,      // 图片消息
    VIDEO,      // 视频消息
    FILE,       // 文件消息
    SYSTEM,     // 系统消息（如时间提示、系统通知等）
    CUSTOM      // 自定义消息（由业务方通过 Slot 渲染）
}

/**
 * 消息发送状态
 */
enum class MessageStatus {
    SENDING,    // 发送中
    SENT,       // 已发送
    FAILED,     // 发送失败
    READ        // 已读
}

/**
 * 附件类型枚举
 */
enum class AttachmentType {
    IMAGE,          // 图片附件
    VIDEO,          // 视频附件
    FILE,           // 文件附件
    GIPHY,          // GIF 动图
    LINK_PREVIEW    // 链接预览
}

/**
 * 附件数据模型（对标 Stream Chat 的 Attachment）
 *
 * @param type 附件类型
 * @param url 附件资源 URL
 * @param title 附件标题（文件名等）
 * @param mimeType MIME 类型（如 "image/jpeg"、"video/mp4"）
 * @param fileSize 文件大小（字节）
 * @param width 宽度（图片/视频/GIF）
 * @param height 高度（图片/视频/GIF）
 * @param duration 时长（视频/音频，秒）
 * @param thumbnailUrl 缩略图 URL（视频/文件预览图）
 * @param extra 扩展数据
 */
