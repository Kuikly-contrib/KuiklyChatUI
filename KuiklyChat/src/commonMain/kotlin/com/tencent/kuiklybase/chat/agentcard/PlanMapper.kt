package com.tencent.kuiklybase.chat.agentcard

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuiklybase.chat.agui.AgUiEvent

/**
 * AG-UI Custom 事件 → [AgentCard.Plan] 的默认映射器。
 *
 * 业务方通过发送 `CUSTOM` 事件携带如下 payload 即可驱动 Plan 卡片：
 * ```
 * {
 *   "type": "CUSTOM",
 *   "name": "plan_created",   // 或 "plan_updated"
 *   "value": {
 *     "id": "plan_xxx",       // 可选；相同 id 会 upsert 合并
 *     "title": "实施计划",
 *     "summary": "共 4 步",
 *     "steps": [
 *       { "id": "s1", "title": "创建 SearchBar 组件", "status": "done" },
 *       { "id": "s2", "title": "创建 RatingSlider",    "status": "running" },
 *       { "id": "s3", "title": "集成搜索逻辑",          "status": "pending" }
 *     ]
 *   }
 * }
 * ```
 *
 * 支持的 `name`：
 * - `plan_created` / `plan_snapshot`：整体规划的全量快照（会替换）
 * - `plan_updated` / `plan_delta`：部分更新（会合并：steps 按 id 替换，未出现的保留）
 *
 * 步骤 `status` 字符串：`pending` / `running` / `done` / `failed` / `skipped`
 * （大小写不敏感，未识别回落到 `pending`）
 */
object PlanMapper : com.tencent.kuiklybase.chat.agentcard.AgUiCardMapper {

    /** 可识别的事件名 */
    private val PLAN_EVENT_NAMES = setOf(
        "plan_created", "plan_snapshot",
        "plan_updated", "plan_delta"
    )

    override fun map(event: AgUiEvent, session: AgUiCardSession): AgentCard? {
        if (event !is AgUiEvent.Custom) return null
        val name = event.name.lowercase()
        if (name !in PLAN_EVENT_NAMES) return null

        val payload = event.value.toJsonObject() ?: return null
        val planId = payload.optString("id").ifEmpty { "plan_${event.timestamp}" }
        val incoming = parsePlan(planId, payload, event.timestamp)

        // delta / updated：合并到已有 plan（如果有）
        val isDelta = name == "plan_updated" || name == "plan_delta"
        if (isDelta) {
            val existing = session.cards.firstOrNull { it is AgentCard.Plan && it.id == planId }
                as? AgentCard.Plan
            if (existing != null) {
                return mergePlan(existing, incoming)
            }
        }
        return incoming
    }

    /** 把 JSONObject 解析为 Plan 卡片 */
    private fun parsePlan(id: String, json: JSONObject, timestamp: Long): AgentCard.Plan {
        val title = json.optString("title").ifEmpty { "实施计划" }
        val summary = json.optString("summary")
        val steps = json.optJSONArray("steps")?.let { arr ->
            buildList {
                for (i in 0 until arr.length()) {
                    val s = arr.optJSONObject(i) ?: continue
                    add(
                        PlanStep(
                            id = s.optString("id").ifEmpty { "step_$i" },
                            title = s.optString("title"),
                            status = parseStatus(s.optString("status")),
                            detail = s.optString("detail")
                        )
                    )
                }
            }
        } ?: emptyList()

        return AgentCard.Plan(
            id = id,
            title = title,
            summary = summary,
            steps = steps,
            timestamp = timestamp
        )
    }

    /** 把 delta 合并到已有 plan（按 step.id 替换；新 step 追加到尾部） */
    private fun mergePlan(existing: AgentCard.Plan, delta: AgentCard.Plan): AgentCard.Plan {
        val mergedSteps = existing.steps.toMutableList()
        delta.steps.forEach { newStep ->
            val idx = mergedSteps.indexOfFirst { it.id == newStep.id }
            if (idx >= 0) mergedSteps[idx] = newStep
            else mergedSteps.add(newStep)
        }
        return existing.copy(
            title = delta.title.ifEmpty { existing.title },
            summary = delta.summary.ifEmpty { existing.summary },
            steps = mergedSteps,
            timestamp = delta.timestamp
        )
    }

    private fun parseStatus(raw: String): PlanStepStatus = when (raw.lowercase()) {
        "done", "completed", "success" -> PlanStepStatus.DONE
        "running", "in_progress", "active" -> PlanStepStatus.RUNNING
        "failed", "error" -> PlanStepStatus.FAILED
        "skipped", "cancel", "canceled" -> PlanStepStatus.SKIPPED
        else -> PlanStepStatus.PENDING
    }

    /**
     * 尽力把 CUSTOM 事件的 value 转换为 JSONObject：
     * - 如果已经是 JSONObject，直接返回
     * - 如果是 Map，包装成 JSONObject
     * - 如果是 JSON 字符串，尝试解析
     * - 否则返回 null
     */
    private fun Any?.toJsonObject(): JSONObject? {
        return when (this) {
            null -> null
            is JSONObject -> this
            is Map<*, *> -> {
                val obj = JSONObject()
                @Suppress("UNCHECKED_CAST")
                (this as Map<Any?, Any?>).forEach { (k, v) ->
                    if (k != null && v != null) obj.put(k.toString(), toJsonValue(v))
                }
                obj
            }
            is String -> runCatching { JSONObject(this) }.getOrNull()
            else -> null
        }
    }

    private fun toJsonValue(value: Any?): Any? = when (value) {
        null -> null
        is Map<*, *> -> {
            val obj = JSONObject()
            @Suppress("UNCHECKED_CAST")
            (value as Map<Any?, Any?>).forEach { (k, v) ->
                if (k != null && v != null) obj.put(k.toString(), toJsonValue(v))
            }
            obj
        }
        is List<*> -> {
            val arr = JSONArray()
            value.forEach { if (it != null) arr.put(toJsonValue(it)) }
            arr
        }
        else -> value
    }
}
