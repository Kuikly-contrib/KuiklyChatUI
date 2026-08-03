package com.tencent.kuiklybase.chat.agentcard_compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * 统一的 Lucide 矢量图标集合。
 *
 * 实现方式：
 *   - 内置 SVG 资源放在组件模块 `KuiklyChat/src/commonMain/assets/common/ic_<name>.svg`
 *   - 通过 [LucideSvgIcon] + `ImageUri.commonAssets` 加载
 *   - 原生端需在 `KuiklyRenderComponentExpandHandler` 中识别 `.svg` 后缀
 *     （macApp 使用 NSImage，macOS 10.13+ 原生支持 SVG）
 *
 * 所有图标保持 Lucide 官方样式：24x24 viewBox、strokeWidth=2、round cap/join。
 */

/** Lucide `brain`：用于"深度思考 / 思考中"状态。 */
@Composable
internal fun BrainIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_brain", size = size, tint = color)
}

/** Lucide `circle-check`：用于"调用完成 / 成功"状态。 */
@Composable
internal fun CircleCheckIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_circle_check", size = size, tint = color)
}

/** Lucide `check`：已复制 / 选中 状态指示。 */
@Composable
internal fun CheckIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_check", size = size, tint = color)
}

/** Lucide `copy`：复制按钮。 */
@Composable
internal fun CopyIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_copy", size = size, tint = color)
}

/** Lucide `chevrons-up-down`：展开按钮。 */
@Composable
internal fun ChevronsUpDownIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_chevrons_up_down", size = size, tint = color)
}

/** Lucide `chevrons-down-up`：收起按钮。 */
@Composable
internal fun ChevronsDownUpIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_chevrons_down_up", size = size, tint = color)
}

/** Lucide `file-terminal`：Bash / 终端类工具调用。 */
@Composable
internal fun FileTerminalIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_file_terminal", size = size, tint = color)
}

/** Lucide `puzzle`：Skill 调用指示。 */
@Composable
internal fun PuzzleIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_puzzle", size = size, tint = color)
}

/** Lucide `file-plus-corner`：文件新增状态。 */
@Composable
internal fun FilePlusCornerIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_file_plus_corner", size = size, tint = color)
}

/** Lucide `file-pen`：文件编辑状态。 */
@Composable
internal fun FilePenIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_file_pen", size = size, tint = color)
}

/** Lucide `list-checks`：TodoWrite 任务清单。 */
@Composable
internal fun ListChecksIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_list_checks", size = size, tint = color)
}

/** Lucide `notebook-text`：Read 工具（读取文件）。 */
@Composable
internal fun NotebookTextIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_notebook_text", size = size, tint = color)
}

/** Lucide `notebook-pen`：Write 工具（写入文件）。 */
@Composable
internal fun NotebookPenIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_notebook_pen", size = size, tint = color)
}

/** Lucide `bot`：Agent / Task 工具（启动子 agent）。 */
@Composable
internal fun BotIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_bot", size = size, tint = color)
}

/** Lucide `search`：Glob / Grep / 文件查找类工具。 */
@Composable
internal fun SearchIcon(size: Dp = 14.dp, color: Color, strokeWidth: Float = 2f) {
    LucideSvgIcon(name = "ic_search", size = size, tint = color)
}
