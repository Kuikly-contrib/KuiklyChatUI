package com.tencent.kuiklybase.chat.agentcard_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.hoverable
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.interaction.collectIsHoveredAsState
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.AgentCard
import com.tencent.kuiklybase.chat.agentcard.ToolState
import com.tencent.kuiklybase.elements.CodeHighlighter

/**
 * Read-only / look-up tools whose output is bulky and rarely interesting once
 * complete (file dump, regex hits, network results). Their cards default to
 * collapsed after SUCCESS so they don't drown the chat history.
 */
private val READ_LIKE_TOOLS = setOf(
    "Read", "Grep", "Glob", "WebFetch", "WebSearch", "ListDir", "Search",
)

/**
 * Tools whose result is shown by an independent [AgentCard.FileEdit] card
 * (rendered via [FileEditCard]) — duplicating the content inside ToolCallCard's
 * result section creates two expandable boxes for the same operation. We keep
 * only the row header for these, and let FileEditCard own the diff display.
 *
 * openspec: add-workflow-phase-progress (visual revamp)
 */
private val FILE_EDIT_TOOLS = setOf(
    "Write", "Edit", "MultiEdit", "StrReplace", "EditNotebook",
)

/**
 * Tools whose result is rendered by a dedicated card (e.g. TodoWrite → TodoProgressCard
 * + "执行任务 x/y" 进度条）。在 ToolCallCard 内重复展示 "输出" 代码块会与下方的
 * 专属卡片信息完全重复，挤占聊天区视觉空间。这些工具只保留行头（工具名 + 状态），
 * 不再显示 body。
 */
private val NO_RESULT_BODY_TOOLS = setOf<String>(
    // 之前 "TodoWrite" 在此；现已升级为 HIDDEN_TOOLS（完全不渲染卡片）
)

/**
 * Tools whose presence is fully represented by a sibling card already in the
 * stream — TodoWrite drives both the inline "执行任务 N/M" `TodoProgressCard`
 * and the input-bar "任务进度" pill (`TaskProgressPill`). Rendering an extra
 * `任务规划 ✓` row in ToolCallCard adds no information and visually doubles the
 * same event. We skip the entire card here.
 *
 * `Skill` is Claude's built-in skill-loading tool. The macApp pipeline routes
 * `.claude/skills/...` Read calls into a dedicated `AgentCard.Custom("skill_load")`
 * card via [com.tencent.tds.studioapp.studio.AgentEventHandler], so the raw
 * `Skill ✗` row (which often surfaces with empty args because the bridge can't
 * see Claude's skill schema) is pure noise — hide it.
 */
private val HIDDEN_TOOLS = setOf(
    "TodoWrite",
    "Skill",
)

/**
 * 工具英文名 → 中文动词标签映射，让普通用户更易理解（参考图 3 设计）。
 * Write 默认显示"新建"（最常见用例：写入新文件）。Edit/MultiEdit/StrReplace
 * 显示"编辑"。
 *
 * openspec: add-workflow-phase-progress (visual revamp v3)
 */
private fun toolDisplayLabel(toolName: String): String = when (toolName) {
    "Read" -> "读取"
    "Write" -> "新建"
    "Edit", "MultiEdit", "StrReplace" -> "编辑"
    "EditNotebook" -> "编辑笔记本"
    "Glob" -> "查找"
    "Grep" -> "搜索"
    "Bash", "execute_command" -> "执行脚本"
    "WebFetch" -> "抓取"
    "WebSearch" -> "网搜"
    "TodoWrite" -> "任务规划"
    "Agent", "Task" -> "子任务"
    "ListMcpResources" -> "列资源"
    "FetchMcpResource" -> "取资源"
    "SemanticSearch" -> "语义搜索"
    "ReadLints" -> "lint 检查"
    else -> toolName
}

/** 工具名 → SVG 图标名（assets/common/ic_xxx.svg），null 表示无图标 */
private fun toolIconName(toolName: String): String? = when (toolName) {
    "Read" -> "ic_notebook_text"
    "Write" -> "ic_file_plus_corner"
    "Edit", "MultiEdit", "StrReplace" -> "ic_file_pen"
    "EditNotebook" -> "ic_notebook_pen"
    "Glob" -> "ic_search"
    "Grep" -> "ic_search"
    "Bash", "execute_command" -> "ic_file_terminal"
    "TodoWrite" -> "ic_list_checks"
    "Agent", "Task" -> "ic_bot"
    "WebFetch" -> "ic_download"
    "WebSearch" -> "ic_search"
    "SemanticSearch" -> "ic_brain"
    "ReadLints" -> "ic_bug"
    "ListMcpResources", "FetchMcpResource" -> "ic_puzzle"
    else -> null
}

/** 工具名 → 图标颜色（徽章背景和文字颜色统一，只有图标带颜色） */
private fun toolIconColor(toolName: String): Long = when (toolName) {
    "Write" -> 0xFF059669L                        // 绿色
    "Edit", "MultiEdit", "StrReplace", "EditNotebook" -> 0xFF2563EBL  // 蓝色
    "Bash", "execute_command" -> 0xFF6366F1L      // 靛紫色（中性，不引起焦虑）
    "Glob", "Grep", "WebSearch", "SemanticSearch" -> 0xFF0891B2L  // 青色
    "Read", "WebFetch" -> 0xFF0EA5E9L             // 天蓝色
    "TodoWrite" -> 0xFF7C3AEDL                    // 紫色
    "Agent", "Task" -> 0xFF4338CAL                // 靛蓝
    "ListMcpResources", "FetchMcpResource" -> 0xFFBE185DL  // 粉色
    "ReadLints" -> 0xFF854D0EL                    // 琥珀
    else -> 0xFF6B21A8L                           // 默认紫色
}

/**
 * 简化工具参数显示。
 *
 * 优先策略：尝试把 args 当 JSON 解析，提取最具语义的字段（file_path / path /
 * pattern / command / url / query / target_notebook），再做路径简化。这样
 * 类似 `Read(".")` / `Read("/abs/.../Projects/com_xxx/s")` 这种短路径不会
 * 被旧版 substring 搞成 `."` / `s"}` 的残片。
 *
 * Fallback：若 args 不是合法 JSON，对原始字符串做"含 Projects/com_xxx 截后缀"
 * 的尝试；找不到时直接返回 trim 后的原文。
 *
 * 最终硬截断 80 字符。
 */
private fun shortenArgs(args: String): String {
    if (args.isBlank()) return args
    val raw = args.trim()

    // 步骤 1：尝试 JSON 提取
    val jsonExtracted = tryExtractFromJson(raw)
    var r = jsonExtracted ?: raw

    // 步骤 2：路径精简（适用于 jsonExtracted 是绝对路径，或 raw 本身就是路径串）
    r = simplifyPath(r)

    // 步骤 3：命令行字符串中嵌入的长引号路径同样收敛
    val pkgPattern = Regex("""Projects/com_[^/]+/""")
    r = r.replace(Regex(""""[^"]{40,}"""")) { match ->
        val path = match.value.trim('"')
        val pkg = pkgPattern.find(path)
        if (pkg != null) {
            "\"${path.substring(pkg.range.last + 1)}\""
        } else {
            val parts = path.split("/").filter { it.isNotEmpty() }
            if (parts.size > 2) "\"${parts.takeLast(2).joinToString("/")}\"" else match.value
        }
    }

    return r
}

/**
 * 从 JSON 字符串里挑一个最具语义的字段作为单行展示。返回 null 表示不是合法 JSON。
 *
 * 优先级：file_path > path > target_notebook > pattern > command > url > query
 *         > description > 整个 JSON 的紧凑形式
 */
private fun tryExtractFromJson(raw: String): String? {
    if (!(raw.startsWith("{") && raw.endsWith("}"))) return null
    return try {
        val obj = com.tencent.kuikly.core.nvi.serialization.json.JSONObject(raw)
        val priorityKeys = listOf(
            "file_path", "path", "target_notebook",
            "pattern", "command", "url", "query", "description",
        )
        for (k in priorityKeys) {
            val v = obj.optString(k, "")
            if (v.isNotBlank()) return v
        }
        // 没有优先字段，返回紧凑 JSON（去引号包裹）
        raw.removePrefix("{").removeSuffix("}").trim().takeIf { it.isNotEmpty() }
    } catch (e: Throwable) {
        null
    }
}

/**
 * 路径精简：
 * - 含 `Projects/com_xxx/` → 截 `Projects/com_xxx/` 之后部分
 * - 含 `Application Support` → 截 `Application Support/` 之后部分
 * - 纯长路径（无空格、含 `/`、超过 40 字符）→ 取最后两级
 * - 否则原样返回
 *
 * 关键改动：截取后若结果**短于 4 字符**（如 `s` / `.` / `..`），认为
 * 截过头了，给路径加 `项目名/` 前缀提供上下文，避免显示孤立的 `s` / `.`。
 */
private fun simplifyPath(input: String): String {
    if (input.isBlank()) return input
    val pkgPattern = Regex("""Projects/(com_[^/]+)/""")
    val pkgMatch = pkgPattern.find(input)
    if (pkgMatch != null) {
        val tail = input.substring(pkgMatch.range.last + 1).trim()
        // 截取后内容太短（如 "."、"s"），加项目名做上下文
        if (tail.length < 4) {
            val projName = pkgMatch.groupValues[1]
            // 进一步缩短项目名：com_zealotchen_applayer177730 → applayer177730（取最后一段）
            val shortName = projName.substringAfterLast("_").take(20)
            return if (tail.isEmpty()) shortName else "$shortName/$tail"
        }
        return tail
    }
    if (input.contains("Application Support")) {
        val idx = input.indexOf("Application Support")
        val after = input.substring(idx + "Application Support".length).trimStart('/').trim()
        if (after.length < 4) return input.substringAfterLast("/").ifBlank { input }
        return after
    }
    if (!input.contains(" ") && input.contains("/") && input.length > 40) {
        val parts = input.trim('"', '\'').split("/").filter { it.isNotEmpty() }
        if (parts.size > 2) return parts.takeLast(2).joinToString("/")
    }
    return input
}

/**
 * Compose 版工具调用卡片。
 *
 * 1:1 对齐 DSL [com.tencent.kuiklybase.chat.agentcard.ToolCallCard]。
 *
 * 视觉：紫色主题，左侧竖线 + 工具名徽章 + 右侧状态指示。
 * 交互：
 * - 点击头部切换结果/错误的展开/折叠
 * - openspec: add-tool-execution-loading
 *   - RUNNING / PENDING / FAILED → 默认展开（看流式 content / 失败原因）
 *   - SUCCESS + Read-like 工具（Read / Grep / Glob / WebFetch / ...）
 *     → 转 SUCCESS 后自动折叠（输出长又不重要）
 *   - SUCCESS + Write-like 工具（Write / Edit / Bash / ...）
 *     → 保持展开（用户通常想看完整结果）
 *   - 用户手动展开/折叠后，自动折叠不再触发（只在 RUNNING→SUCCESS 转换瞬间触发一次）
 */
@Composable
fun ComposeToolCallCard(
    card: AgentCard.ToolCall,
    theme: ComposeCardTheme = ComposeCardTheme.Default,
    modifier: Modifier = Modifier,
    onCopy: ((String) -> Unit)? = null
) {
    // ── HIDDEN_TOOLS：完全不渲染（事件已由专属卡片代理）──
    //   TodoWrite 同时驱动 TodoProgressCard（"执行任务 N/M"行）和输入框上方的
    //   "任务进度"胶囊；这里再画一行 "任务规划 ✓" 完全冗余，干脆 skip。
    if (card.toolName in HIDDEN_TOOLS) return

    // ── 诊断：确认卡片是否收到新数据 ──
    com.tencent.kuikly.core.log.KLog.i(
        "AgUiRecompose",
        "ToolCallCard RECOMPOSE | tool=${card.toolName} state=${card.state} resultLen=${card.result.length} id=${card.id.take(20)}"
    )
    // 默认展开策略：
    //   - Read 类工具（Read/Grep/Glob/WebFetch/...）：result 通常是几百~几万行
    //     文件内容/搜索结果，全展开会撑爆聊天区（一张卡占满整个视口，把后续
    //     卡挤到屏幕外，看起来像"消息后面是大段空白"）。默认折叠，用户点
    //     ▼ 才展开。
    //   - 其它工具（Bash/Write/TodoWrite/...）：默认展开。
    //   - RUNNING 状态强制展开，用户能看到流式 typewriter；用户手动切换后
    //     状态是 sticky 的（remember 只在 card.id 变化时重置）。
    val isReadLikeTool = card.toolName in READ_LIKE_TOOLS
    var expanded by remember(card.id) {
        mutableStateOf(card.state == ToolState.RUNNING || !isReadLikeTool)
    }
    // 执行完成后自动折叠输出区（RUNNING → SUCCESS/FAILED 转换瞬间触发一次）
    var prevState by remember(card.id) { mutableStateOf(card.state) }
    if (card.state != prevState) {
        if (prevState == ToolState.RUNNING && card.state != ToolState.RUNNING) {
            expanded = false
        }
        prevState = card.state
    }
    // Write/Edit 工具：RUNNING 中展示流式写入内容，完成后隐藏（已无独立 FileEditCard）
    val isFileEditTool = card.toolName in FILE_EDIT_TOOLS
    val isFileEditStreaming = isFileEditTool && card.state == ToolState.RUNNING
    val hasBody = when {
        // Write/Edit：有 result 就有 body（RUNNING 展示写入过程，完成后可展开查看）
        isFileEditTool && card.result.isNotEmpty() -> true
        isFileEditTool -> false
        // 其他工具：有 result 就有 body
        card.result.isNotEmpty() -> true
        else -> false
    } || card.errorMessage.isNotEmpty()

    // openspec: add-workflow-phase-progress (visual revamp)
    //   重构：去掉外层紫色边框 + 左侧装饰条 + "工具调用" 前缀。
    //   改为轻量单行：tool 小徽章 + 单行参数（路径/命令）+ 状态图标。
    //   只有展开时才出现代码块 body。整体视觉密度向参考图（image 3）对齐。
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = theme.cardMarginH.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (expanded && hasBody) 1.dp else 0.dp,
                color = if (expanded && hasBody) Color(0xFFE2E8F0) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .background(if (expanded && hasBody) Color(0xFFFFFFFF) else Color.Transparent)
    ) {
        // ===== 单行行头：tool 徽章 + 参数 + 状态 =====
        val hoverInteraction = remember { MutableInteractionSource() }
        val isHovered by hoverInteraction.collectIsHoveredAsState()
        val hoverBg = if (isHovered) Color(0x0A000000) else Color.Transparent
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .hoverable(hoverInteraction)
                .background(hoverBg)
                .let { if (hasBody) it.clickable { expanded = !expanded } else it }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 工具名：彩色图标 + 灰色文字（无背景色）
            val iconTint = toolIconColor(card.toolName)
            Row(
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconName = toolIconName(card.toolName)
                if (iconName != null) {
                    LucideSvgIcon(name = iconName, size = 14.dp, tint = Color(iconTint))
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    toolDisplayLabel(card.toolName),
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            // 参数（命令 / 路径）
            // RUNNING 中 args 是流式碎片（还没推完），等完成后再显示完整路径
            val showArgs = card.args.isNotEmpty() && card.state != ToolState.RUNNING
            if (showArgs) {
                Text(
                    shortenArgs(card.args), fontSize = 12.sp, color = Color(0xFF64748B),
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
            } else if (card.state == ToolState.RUNNING) {
                Spacer(Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }

            // ── 右侧操作区：复制 → 展开/收起 → 状态图标 ──
            // 复制按钮（仅已完成且有 result 时显示），点击后切换为 ✓ 反馈 1.5s
            if (card.state != ToolState.RUNNING && card.result.isNotEmpty()) {
                var copied by remember(card.id) { mutableStateOf(false) }
                if (copied) {
                    LaunchedEffect(Unit) {
                        delay(1500)
                        copied = false
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            onCopy?.invoke(card.result)
                            copied = true
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    if (copied) {
                        LucideSvgIcon(name = "ic_check", size = 12.dp, tint = Color(0xFF22C55E))
                    } else {
                        LucideSvgIcon(name = "ic_copy", size = 12.dp, tint = Color(0xFF94A3B8))
                    }
                }
            }
            // 展开/收起切换
            if (hasBody || card.result.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                LucideSvgIcon(
                    name = if (expanded) "ic_chevrons_down_up" else "ic_chevrons_up_down",
                    size = 12.dp,
                    tint = Color(0xFF94A3B8)
                )
            }
            // 状态图标（最右边）
            Spacer(Modifier.width(6.dp))
            when (card.state) {
                ToolState.PENDING -> Text(
                    "⏳", fontSize = 12.sp, color = Color(0xFF94A3B8)
                )
                ToolState.RUNNING -> BouncingDots(
                    color = Color(0xFF8B5CF6),
                    dotSize = 5f, dotGap = 3f
                )
                ToolState.SUCCESS -> LucideSvgIcon(
                    name = "ic_circle_check", size = 14.dp, tint = Color(theme.successColor)
                )
                ToolState.FAILED -> Text(
                    "✗", fontSize = 14.sp, color = Color(theme.errorColor),
                    fontWeight = FontWeight.Bold
                )
            }
            // 展开/收起切换图标已移除（点击整行即可 toggle）
        }

        // ===== 展开区：仅 result / error，不再重复显示 args =====
        if (expanded) {
            run {
                // 执行结果（SUCCESS / RUNNING 都显示）。
                // openspec: add-tool-execution-loading — GitHub-dark code block
                //   visual + typewriter animation + collapse/expand.
                //
                //   The 1500 chars/sec typewriter is needed because Anthropic
                //   delivers input_json_delta in 800ms bursts (80+ chunks at
                //   once). Rendering card.result directly causes a "pop" with
                //   no typewriter feel — Compose batches into 1-2 frames.
                //   We keep an internal `displayedLen` that catches up to
                //   `card.result.length` so user actually sees typing.
                //
                //   Default collapsed for SUCCESS (after the user has seen
                //   the typewriter) so the chat history stays compact.
                //   Default expanded for RUNNING so user can watch the
                //   typewriter live.
                val isStreaming = card.state == ToolState.RUNNING
                // Write/Edit RUNNING 中也显示流式写入内容
                val showResult = card.result.isNotEmpty()
                if (showResult) {
                    var displayedLen by remember(card.id) { mutableStateOf(0) }
                    var resultExpanded by remember(card.id) { mutableStateOf(true) }
                    val targetLen = card.result.length

                    // 默认展开；外层 TopRow 的 ▼/▲ 切换 expanded（这个 resultExpanded
                    // 仅在 body 内部用于代码块自身的额外开合，但目前外层 TopRow
                    // 已统一控制，这里保持永远展开即可）。
                    // (LaunchedEffect 移除：不再自动折叠)

                    // openspec: add-tool-execution-loading — burst-resistant LE.
                    //   Anthropic delivers input_json_delta in 60-200ms bursts
                    //   (30+ events in 3 seconds). If LE's key includes targetLen,
                    //   each burst cancels the in-flight `delay(30)` → displayedLen
                    //   never grows during the burst → user sees "0/N" header
                    //   for 3 seconds + zero typewriter motion.
                    //
                    //   Fix: keep LE alive for the lifetime of the placeholder
                    //   (key = card.id only). Pipe `targetLen` and `running` in
                    //   via mutableState — coroutine reads `.value` per tick to
                    //   pick up the latest value without restart.
                    val targetLenState = remember(card.id) { mutableStateOf(0) }
                    targetLenState.value = targetLen
                    val runningState = remember(card.id) { mutableStateOf(true) }
                    runningState.value = isStreaming

                    LaunchedEffect(card.id) {
                        while (true) {
                            val target = targetLenState.value
                            if (!runningState.value) {
                                // Tool finished — snap to end (no waiting for
                                // typewriter to catch up after spinner is gone).
                                if (displayedLen != target) displayedLen = target
                                delay(100)
                                continue
                            }
                            if (displayedLen >= target) {
                                // Caught up; wait for the next input_delta.
                                delay(30)
                                continue
                            }
                            val remaining = target - displayedLen
                            val maxStepCharsPerSec = 1500
                            val tickMs = 30
                            val stepFromRate = (maxStepCharsPerSec * tickMs / 1000).coerceAtLeast(20)
                            // If we've fallen way behind (model burst), allow
                            // larger jumps so the lag visible to the user is
                            // capped at ~3s regardless of content size.
                            val maxLagBudgetTicks = 100
                            val catchUpStep = (remaining / maxLagBudgetTicks).coerceAtLeast(1)
                            val step = maxOf(stepFromRate, catchUpStep)
                            displayedLen = (displayedLen + step).coerceAtMost(target)
                            delay(tickMs.toLong())
                        }
                    }

                    // Viewport-style trim: only render last 12 lines / 4000 chars
                    // so cards stay a fixed height regardless of file size.
                    val MAX_LINES = 12
                    val MAX_TAIL_CHARS = 4000
                    // ⚠️ 不能用 derivedStateOf { card.result.take(...) } ——
                    //    card 是 data class 的普通字段，不是 Compose State，
                    //    derivedStateOf 闭包捕获的是旧 card 引用，result 变了也不会重算。
                    //    直接在 Composable 函数体中计算，每次 recompose 都用最新 card.result。
                    val rawClamped = card.result.take(displayedLen)
                    val display = remember(rawClamped) {
                        var text = rawClamped
                        val charOmitted = (text.length - MAX_TAIL_CHARS).coerceAtLeast(0)
                        if (charOmitted > 0) text = text.takeLast(MAX_TAIL_CHARS)
                        val lines = text.split('\n')
                        val lineOmitted = (lines.size - MAX_LINES).coerceAtLeast(0)
                        val tailLines = if (lineOmitted > 0)
                            lines.takeLast(MAX_LINES).joinToString("\n")
                        else
                            text
                        when {
                            charOmitted > 0 && lineOmitted > 0 ->
                                "...（已省略 $charOmitted 字符 / $lineOmitted 行）\n$tailLines"
                            charOmitted > 0 ->
                                "...（已省略 $charOmitted 字符）\n$tailLines"
                            lineOmitted > 0 ->
                                "...（已省略 $lineOmitted 行）\n$tailLines"
                            else -> tailLines
                        }
                    }
                    val totalLines = remember(card.result) { card.result.count { it == '\n' } + 1 }
                    val canToggleExpand = !isStreaming  // RUNNING 时强制展开看打字机

                    // ===== 展开内容区（与行头同属一个卡片容器）=====
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // 分隔线
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))

                        // 工具输出：CodeHighlighter 语法高亮渲染
                        val codeText = display
                        // 从 args 中推测语言（文件扩展名）
                        val lang = card.args.substringAfterLast('.', "")
                            .takeIf { it.length in 1..10 && !it.contains('/') }
                        val segments = remember(codeText, lang) {
                            CodeHighlighter.highlight(
                                code = codeText,
                                language = lang,
                                darkTheme = false,
                            )
                        }
                        val hasHighlight = segments.any { it.color != null || it.bold }

                        if (hasHighlight) {
                            val annotated = remember(segments) {
                                buildAnnotatedString {
                                    segments.forEach { segment ->
                                        withStyle(SpanStyle(
                                            color = Color(segment.color ?: 0xFF64748BL),
                                            fontWeight = if (segment.bold) FontWeight.Bold else FontWeight.Normal,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                        )) {
                                            append(segment.text)
                                        }
                                    }
                                }
                            }
                            Text(
                                text = annotated,
                                lineHeight = 17.sp,
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        } else {
                            Text(
                                text = codeText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B),
                                lineHeight = 17.sp,
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
                // RUNNING + 空 result 时不显示额外占位 — 头部的 BouncingDots
                // spinner 已经表明工具正在准备，避免页面上 loading 视觉过多
                // ("正在思考" / 头部 spinner / 占位 stripe 三处堆叠)。
                // 等第一次 inputDelta 到达后，正常的代码块视图会出现。

                // 错误信息
                if (card.state == ToolState.FAILED && card.errorMessage.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color(0xFFFEF2F2))
                            .padding(10.dp)
                    ) {
                        Text(
                            "⚠️ ${card.errorMessage}", fontSize = 12.sp,
                            color = Color(theme.errorColor), lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
