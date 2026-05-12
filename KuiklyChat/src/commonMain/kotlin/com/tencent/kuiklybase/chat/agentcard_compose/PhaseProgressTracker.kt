package com.tencent.kuiklybase.chat.agentcard_compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
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
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextDecoration
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.chat.agentcard.TodoProgressItem

/**
 * 整体进度 pill + 展开式二级阶段列表（参考产品对照设计）。
 *
 * - 折叠态：单个 pill `整体进度 N/5 ▼`
 * - 展开态：5 阶段竖排清单 + 虚线连接 + 状态图标
 *   - DEVELOPMENT 阶段进入后内嵌 TodoWrite 子任务列表
 *   - 完成 done 阶段名加删除线 + 灰色
 *   - 进行中 running 阶段紫色加粗
 *   - 未到 pending 阶段灰色
 *
 * openspec: add-workflow-phase-progress (visual revamp v2)
 */
@Composable
fun ComposePhaseProgressTracker(
    phaseStates: Map<String, String>,    // phaseId → status
    currentPhaseId: String?,
    todoCompleted: Int,
    todoTotal: Int,
    todoItems: List<TodoProgressItem> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val phases = listOf(
        "WORKSPACE_CREATE" to "创建工作区",
        "DESIGN_SYSTEM" to "设计系统确定",
        "DEVELOPMENT" to "开发任务",
        "LINT" to "lint 检查",
        "COMPILE" to "编译检查",
    )
    val totalPhases = phases.size
    val donePhases = phases.count { (id, _) ->
        val st = phaseStates[id]
        st == "done" || st == "failed"
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // ===== Pill 折叠态行头 =====
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (expanded) Color(0xFFEDE9FE) else Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "整体进度",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$donePhases/$totalPhases",
                fontSize = 13.sp,
                color = Color(0xFF6B21A8),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            LucideSvgIcon(
                name = if (expanded) "ic_chevrons_down_up" else "ic_chevrons_up_down",
                size = 12.dp,
                tint = Color(0xFF94A3B8)
            )
        }

        // ===== 展开态二级列表 =====
        if (expanded) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                phases.forEachIndexed { idx, (phaseId, phaseName) ->
                    val status = phaseStates[phaseId] ?: "pending"
                    val isCurrent = currentPhaseId == phaseId
                    val hasSubList = phaseId == "DEVELOPMENT" &&
                        (status == "running" || status == "done") &&
                        todoItems.isNotEmpty()
                    PhaseRow(
                        phaseName = phaseName,
                        status = status,
                        isCurrent = isCurrent,
                        // DEV 阶段下方有子列表时不画 connector — 子列表视觉
                        // 本身就是 connector 的延伸，避免上下间距叠加。
                        showConnector = idx < phases.size - 1 && !hasSubList,
                        // DEV 阶段额外 N/M 标识
                        suffix = if (phaseId == "DEVELOPMENT" && todoTotal > 0) "$todoCompleted/$todoTotal" else null,
                    )
                    if (hasSubList) {
                        TodoSubList(items = todoItems, isLastBlock = idx == phases.size - 1)
                    }
                }
            }
        }
    }
}

/**
 * 单个阶段行：圆点 + 阶段名 + 可选 suffix + 下方虚线连接器。
 */
@Composable
private fun PhaseRow(
    phaseName: String,
    status: String,
    isCurrent: Boolean,
    showConnector: Boolean,
    suffix: String?,
) {
    // 视觉优化：done 不再用饱和绿色 + 删除线，改为更克制的灰勾 + 浅灰文字
    // 配色更收敛：完成态像参考图 image 3 那样不抢戏
    val textColor = when (status) {
        "done" -> Color(0xFF64748B)        // 中灰（之前是 #94A3B8 太淡）
        "running" -> Color(0xFF6B21A8)     // 紫色 - 当前进行
        "failed" -> Color(0xFFB91C1C)      // 较深红
        else -> Color(0xFFCBD5E1)          // 浅灰 - 待办
    }
    val textWeight = if (isCurrent || status == "running") FontWeight.Bold else FontWeight.Normal

    Row(verticalAlignment = Alignment.CenterVertically) {
        // 状态指示 — 不再用大色块圆点，改为简洁的图标
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (status) {
                "running" -> {
                    Box(
                        modifier = Modifier.size(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center,
                    ) {
                        BouncingDots(color = Color.White, dotSize = 3f, dotGap = 2f)
                    }
                }
                "done" -> Text(
                    text = "✓",
                    fontSize = 14.sp,
                    color = Color(0xFF22C55E),
                    fontWeight = FontWeight.Bold,
                )
                "failed" -> Text(
                    text = "✗",
                    fontSize = 14.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold,
                )
                else -> Box(
                    modifier = Modifier.size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFE2E8F0))
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = phaseName,
            fontSize = 13.sp,
            color = textColor,
            fontWeight = textWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (suffix != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = suffix,
                fontSize = 11.sp,
                color = if (status == "running") Color(0xFF8B5CF6) else Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold,
            )
        }
    }

    if (showConnector) {
        // 竖直虚线连接：用一列小 Box 模拟
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Box(
                modifier = Modifier
                    .padding(start = 7.dp)        // 与 16dp 圆点中心对齐
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color(0xFFE2E8F0))
            )
        }
    }
}

/**
 * DEV 阶段的 todo 子任务列表，缩进展示。
 */
@Composable
private fun TodoSubList(
    items: List<TodoProgressItem>,
    isLastBlock: Boolean,
) {
    // top=2 紧贴 DEV 阶段标题（同时 PhaseRow 在有 sub-list 时已经省了下方
    // connector），避免标题和子任务之间间距过大。
    Column(
        modifier = Modifier.padding(start = 26.dp, top = 2.dp, bottom = 6.dp)
    ) {
        items.forEachIndexed { idx, item ->
            val (icon, color) = when (item.status) {
                "completed" -> "✓" to Color(0xFF22C55E)
                "in_progress" -> "" to Color(0xFF8B5CF6)
                else -> "○" to Color(0xFFCBD5E1)
            }
            val textColor = when (item.status) {
                "completed" -> Color(0xFF64748B)        // 中灰 - 已完成（不删除线）
                "in_progress" -> Color(0xFF6B21A8)      // 紫色加粗 - 当前
                else -> Color(0xFF94A3B8)               // 浅灰 - 待办
            }
            val display = item.activeForm?.takeIf {
                item.status == "in_progress" && it.isNotBlank()
            } ?: item.content

            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                    if (item.status == "in_progress") {
                        Box(
                            modifier = Modifier.size(11.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            BouncingDots(color = Color.White, dotSize = 2.5f, dotGap = 1.5f)
                        }
                    } else {
                        Text(text = icon, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = display,
                    fontSize = 12.sp,
                    color = textColor,
                    fontWeight = if (item.status == "in_progress") FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
