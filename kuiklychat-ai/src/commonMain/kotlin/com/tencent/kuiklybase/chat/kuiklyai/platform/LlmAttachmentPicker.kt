package com.tencent.kuiklybase.chat.kuiklyai.platform

import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachment
import com.tencent.kuiklybase.chat.kuiklyai.provider.LlmAttachmentKind

/**
 * 可发起的附件采集来源。
 */
public enum class LlmAttachmentSource {
    /** 相册选图 */
    PHOTO_LIBRARY,

    /** 拍照 */
    CAMERA,

    /** 文件管理器选文件 */
    FILE
}

/**
 * 附件选择器：由宿主实现的平台能力钩子。
 *
 * `commonMain` 不能碰相册、相机、文件系统，更重要的是——**库不该替业务
 * 决定用哪个图片选择 SDK**。不同宿主的权限申请流程、图片压缩策略、
 * 隐私合规要求差别很大，硬塞一个默认实现只会让接入方绕着它走。
 *
 * 所以这里只定义契约；宿主可自行实现，也可使用平台集成包提供的默认实现
 * （Android `AndroidLlmPlatformRegistry`、iOS `KuiklyChatIOS` Module）。
 *
 * 未提供实现时（[LlmAttachmentPicker] 为 null），输入区不显示附件按钮，
 * 而不是显示一个点了没反应的按钮。
 */
public interface LlmAttachmentPicker {

    /** 当前宿主支持哪些来源。返回空集合等价于不支持附件。 */
    public val supportedSources: Set<LlmAttachmentSource>

    /**
     * 发起一次附件选择。
     *
     * 实现方负责权限申请、选择 UI、必要的压缩与转码。
     *
     * @return 用户选中的附件；用户取消时返回空列表
     */
    public suspend fun pick(source: LlmAttachmentSource): List<LlmAttachment>

    /**
     * 同 [pick]，并限制系统选择器最多接受 [maxCount] 个。
     * 默认实现调用 [pick] 后再截断。
     */
    public suspend fun pick(source: LlmAttachmentSource, maxCount: Int): List<LlmAttachment> =
        pick(source).take(maxCount.coerceAtLeast(0))

    /** 该来源能产出的附件类型，用于输入区展示合适的图标与文案。 */
    public fun kindOf(source: LlmAttachmentSource): LlmAttachmentKind = when (source) {
        LlmAttachmentSource.PHOTO_LIBRARY, LlmAttachmentSource.CAMERA ->
            LlmAttachmentKind.IMAGE
        LlmAttachmentSource.FILE -> LlmAttachmentKind.FILE
    }
}
