package com.tencent.kuiklybase.chat.compose.agentcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.ColorFilter
import com.tencent.kuikly.compose.ui.graphics.DefaultAlpha
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.base.attr.ImageUri

/**
 * 通过 SVG 资源名加载 Lucide 图标（macOS / iOS / Android 端资源加载）。
 *
 * 资源目录约定：
 *   - 组件内置图标放在 `KuiklyChat/src/commonMain/assets/common/`
 *   - 宿主扩展图标仍可放在宿主工程的 `src/commonMain/assets/common/`
 *   - iOS：`KuiklyChatIOS` 的 `KRChatImageExpandHandler` 负责 SVG 解码
 *   - Android：`KuiklyChatAndroid` 的 `KRChatImageAdapter` 负责 SVG 解码
 *
 * iOS 上 Compose `colorFilter` 会映射为原生 `tintColor`；透明度通过 [Image] 的 `alpha`
 * 单独传递，避免 tint 与 alpha 叠加后图标不可见。
 *
 * @param name 不含扩展名，如 `"ic_puzzle"` 或 `"kuikly_chat_ic_copy"`
 * @param size 显示尺寸
 * @param tint 着色（`Color.Unspecified` 表示保持 SVG 原色）
 */
@OptIn(InternalResourceApi::class)
@Composable
internal fun LucideSvgIcon(
    name: String,
    size: Dp = 14.dp,
    tint: Color = Color.Unspecified,
) {
    val drawable = remember(name) {
        DrawableResource(ImageUri.commonAssets("$name.svg").toUrl(""))
    }
    val iconAlpha = if (tint != Color.Unspecified) tint.alpha else DefaultAlpha
    val iconTint = if (tint != Color.Unspecified) {
        ColorFilter.tint(tint.copy(alpha = 1f))
    } else {
        null
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = name,
        modifier = Modifier.size(size),
        alpha = iconAlpha,
        colorFilter = iconTint,
    )
}
