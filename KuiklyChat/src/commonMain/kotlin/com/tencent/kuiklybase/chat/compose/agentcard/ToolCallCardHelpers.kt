package com.tencent.kuiklybase.chat.compose.agentcard

// ToolCallCard 的工具元数据表与纯函数辅助（参数精简、路径收敛、图标/配色映射）。
// 与 Composable 分开存放，便于单独阅读和测试。


/**
 * Read-only / look-up tools whose output is bulky and rarely interesting once
 * complete (file dump, regex hits, network results). Their cards default to
 * collapsed after SUCCESS so they don't drown the chat history.
 */
internal val READ_LIKE_TOOLS = setOf(
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
internal val FILE_EDIT_TOOLS = setOf(
    "Write", "Edit", "MultiEdit", "StrReplace", "EditNotebook",
)

/**
 * Tools whose result is rendered by a dedicated card (e.g. TodoWrite → TodoProgressCard
 * + "执行任务 x/y" 进度条）。在 ToolCallCard 内重复展示 "输出" 代码块会与下方的
 * 专属卡片信息完全重复，挤占聊天区视觉空间。这些工具只保留行头（工具名 + 状态），
 * 不再显示 body。
 */
internal val NO_RESULT_BODY_TOOLS = setOf<String>(
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
internal val HIDDEN_TOOLS = setOf(
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
internal fun toolDisplayLabel(toolName: String): String = when (toolName) {
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
internal fun toolIconName(toolName: String): String? = when (toolName) {
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
internal fun toolIconColor(toolName: String): Long = when (toolName) {
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
internal fun shortenArgs(args: String): String {
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
internal fun tryExtractFromJson(raw: String): String? {
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
internal fun simplifyPath(input: String): String {
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
