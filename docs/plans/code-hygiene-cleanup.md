---
last_updated: 2026-07-27
status: active
owner: @kuikly-chat-team
---

# 代码库卫生清理（后台清理 Agent Prompt）

> 将本文件作为周期性任务（如每周一，见 `.github/workflows/doc-gardening.yml`）的 Prompt，
> 让清理 Agent 在隔离 worktree 中执行，对每个发现生成**独立修复 PR**。

## 任务：代码库卫生清理

请执行以下检查，对每个发现的问题生成独立的修复 PR：

## 检查清单
1. **超长文件**：跑 `bash scripts/check-layer-deps.sh`，把「存量豁免，待拆分」的文件拆成更小模块；
   每拆完一个，从 `scripts/file-size-baseline.txt` 删掉对应行（门禁会校验清单不腐化）。清单见 `backlog.md`。
2. **缺失测试**：找出 `KuiklyChat/src/commonMain` 下没有对应 `commonTest` 单测的关键逻辑（如新增的事件/卡片/映射），补充基础测试。
3. **未使用的 import**：清理所有未使用的 import 语句（`git grep` + 编译器告警）。
4. **TODO/FIXME**：列出所有 TODO 和 FIXME，如果超过 30 天未处理则生成清理 PR。
5. **重复代码**：找出高度相似的代码段（>10 行），提取为共享工具函数/扩展。
6. **过时文档**：检查 `docs/design/` 中状态为 `Draft` 但已超过 30 天的文档，标记或补充。

## 约束
- 每个修复作为独立 PR，不要混在一起。
- 每个 PR 修改后必须确保 `bash scripts/check-layer-deps.sh` 通过且 `./gradlew :KuiklyChat:testDebugUnitTest` 全绿（81 个用例）。
- PR 标题格式：`chore(cleanup): [具体描述]`。
- 如果不确定某个修改是否安全，跳过并在 PR 中标注原因。
- 不修改 `commonMain` 的平台隔离规则（见 `docs/architecture/boundaries.md`）。
