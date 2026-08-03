package com.tencent.kuiklybase.chat.agentcard_compose

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
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.base.attr.ImageUri

/**
 * 通过 SVG 资源名加载 Lucide 图标（macOS / iOS / Android 端资源加载）。
 *
 * 资源目录约定：
 *   - 组件内置图标放在 `KuiklyChat/src/commonMain/assets/common/`，使用 `kuikly_chat_` 前缀并随组件产物发布
 *   - 宿主扩展图标仍可放在宿主工程的 `src/commonMain/assets/common/`
 *   - macOS 端由 `KuiklyRenderComponentExpandHandler.m` 识别 `.svg` 后缀后走 NSImage 加载
 *   - 其他端当前未适配 SVG，需用对应 ImageAdapter 支持
 *
 * @param name 不含扩展名，如 `"ic_puzzle"`
 * @param size 显示尺寸
 * @param tint 着色（`Color.Unspecified` 表示保持原色；Lucide 图标默认 stroke=currentColor）
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
    Image(
        painter = painterResource(drawable),
        contentDescription = name,
        modifier = Modifier.size(size),
        colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint) else null,
    )
}
