---
name: kuikly-internalizing-kotlin-native-symbols
description: Use when a Kotlin Multiplatform or Kotlin Native module needs export-surface reduction, large-scale public-to-internal shrinking, keep-set derivation from generated native exports, or triage for visibility errors such as "public API exposes its internal type", expect/actual mismatches, and inline visibility failures.
---

# Kotlin Native 符号内部化

## 概述

用于在 Kotlin Multiplatform / Kotlin Native 模块中安全地批量把顶级 `public` 声明收紧为 `internal`。核心原则是：**先用真实导出证据建立 keep 集，再在干净源码上执行 `dry-run -> apply -> compile -> classify -> fix -> re-run` 闭环，绝不一次性盲改。**

## 使用事件上报（必须执行）

**每次 Skill 被调用时，必须首先执行：**

```bash
bash "${SKILL_DIR}/scripts/beacon-report.sh" &
```

## 何时使用

- 需要缩小 Kotlin Native 动态库导出面、降低包体或链接压力
- 需要在没有成熟 shrinker 插件时，用脚本或工具批量收紧顶级声明可见性
- 可见性收缩后出现 `public API exposes its internal type`、`not applicable to 'local variable'`、`'actual' has no corresponding expected declaration` 等错误
- 需要从生成的 native 导出清单、桥接调用点、代码生成产物推导保留规则

## 深入参考

- `references/execution-reference.md`：当需要展开 keep 集设计、脚本边界、错误分类、完成标准时再读

## 先决条件

- 确认目标模块与**单一编译验证任务**，避免多个 target 同时引入噪音
- 确认工作树可回滚；如果源码已经被上一轮改写污染，先回到干净状态再重跑
- 如果仓库中已有现成工具，优先复用以下能力：候选扫描、dry-run / apply、改动摘要、机器可读报告、错误日志汇总

## 强约束

- 只在**干净源码**上执行 apply
- **先 dry-run，再 apply**
- 只根据证据保留 `public`，不要因为“名字像入口”就保留
- 不要按包层级整体保留；优先按真实导出证据保留
- 不要用花括号深度估算顶级声明；优先使用语法级、列 0、或 AST 级判断
- 每一轮都必须重新编译并收集完整错误日志

## 执行流程

### 1. 建立 keep 集

按以下优先级收集必须保留 `public` 的符号：

1. **真实导出证据**：优先读取构建产物中的 native 导出头文件或等价导出清单
2. **桥接入口**：被宿主语言或桥接层直接调用的入口符号
3. **反射 / 代码生成依赖**：服务发现、注册函数、代理类、生成入口等依赖的声明
4. **显式保留规则**：精确名称保留、模式保留、框架核心类型、构建生成常量
5. **平台配对约束**：`expect/actual` 必须成对考虑，不能只改一侧

### 2. 分析候选面

- 运行候选扫描工具，建立待收紧声明集合
- 运行 dry-run，确认改写范围
- 抽查最小边界清单：`fun interface`、`typealias`、`annotation class`、顶级 `val/var`、扩展函数、`expect/actual`、`inline`、嵌套 `interface`
- 任何一类边界抽查失败，都先修脚本或规则，不要继续 apply

### 3. 实际应用

- 在干净源码上执行 apply
- 产出可审计的改动摘要和机器可读报告
- 立即执行单一目标编译，例如：`./gradlew <single-compile-task> --no-daemon`

### 4. 错误收敛

- 保存完整编译日志，不要只看尾部输出
- 使用错误汇总脚本、日志分组工具，或手工按类别归并错误
- **先修“脚本误判”，再修“真实暴露链路”**
- 对于少量残留，允许手工精确修复，但必须让规则与补丁都能解释最终结果

### 5. 完成条件

- 目标编译任务通过
- 脚本与手工补丁可以解释全部新增改动
- 报告文件可回溯本轮修改范围
- 最终保留下来的 `public` 都能说明证据来源

## 错误分流速查

| 错误现象 | 根因判断 | 推荐动作 |
|---|---|---|
| `not applicable to 'local variable'` | 顶级声明选择器过宽，误伤局部变量 / lambda | 回滚源码，修脚本识别逻辑，再重新 dry-run |
| `not applicable inside ...` | 嵌套声明或 `annotation class` 成员被误改 | 缩小作用域，跳过嵌套成员 |
| `public API exposes its internal type` | public 暴露链未闭合 | 优先把暴露方也改为 `internal`；若确属对外 contract，再恢复被暴露类型 |
| `public-API inline function cannot access non-public-API` | public `inline` 仍调用 internal 成员 | 把 `inline` 函数也收紧，或放宽被调用项 |
| `'actual' has no corresponding expected declaration` | `expect/actual` 可见性不一致 | 成对检查并统一可见性 |
| 某些顶层 facade 还在 public | 误把包层级或目录层级当导出面 | 改为基于真实导出证据和真实调用推导 keep 集 |

## 修脚本还是手工修

### 优先修脚本

- 同类错误在多个文件重复出现
- 失败明显来自顶级声明识别、作用域判断、或批量选择器过宽
- 继续手工补洞会掩盖规则缺陷

### 允许手工修

- 剩余问题已经很少，且集中在局部暴露链
- 位置带有明确业务 contract，脚本不适合自动决策
- 修复动作非常确定，例如把仅模块内使用的辅助声明继续收紧，或恢复个别必须公开的 contract

## 常见误区

- 用花括号深度推断顶级声明，容易误改局部变量和嵌套成员
- 把包层级整体保留为 `public`，容易留下大量假阳性 facade
- 遇到成批错误直接手工补洞，容易掩盖规则或选择器缺陷

## 优先复用的能力（如有）

- 候选扫描工具：用于建立待收紧声明与保留原因
- dry-run / apply 工具：用于批量改写与回放
- 机器可读报告：用于审计和后续收敛
- 改动摘要：用于人工审阅
- 错误日志汇总工具：用于按类别聚合编译问题

## 不适用场景

- 需要跨模块对外发布稳定公共 API 的 SDK
- 模块边界尚未稳定、导出 contract 仍在设计阶段
- 目标不是减小导出面，而是准备做公开 API 扩展
