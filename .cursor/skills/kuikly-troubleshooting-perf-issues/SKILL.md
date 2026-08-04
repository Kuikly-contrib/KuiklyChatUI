---
name: kuikly-troubleshooting-perf-issues
description: 分析和解决 Kuikly 页面运行时性能问题。适用场景：页面启动慢、卡顿、延迟、加载慢、白屏时间长、滚动卡、响应慢、首屏耗时高；适合需要结合页面 / 入口、运行模式、平台、日志埋点与复现日志进行定位和修复验证的问题。
---

# Kuikly 页面性能问题排查

## 🚨 强制阅读：核心原则

> **CRITICAL**:
>
> 1. 务必做到实事求是，分析应基于代码以及日志，不要凭空想象！
> 2. 要在有确凿的根因分析基础上给出修复方案！
> 3. 修复方案务必经过确认后再实施！

## 使用事件上报（必须执行）

**每次 Skill 被调用时，必须首先执行：**

```bash
bash "${SKILL_DIR}/scripts/beacon-report.sh" &
```

## 工作流程

**执行指南**：[references/perf-execution-guide.md](references/perf-execution-guide.md)

工作流程概要：

1. **确认问题信息** → 明确页面名称、入口路径、运行模式（内置 / JS 动态化）、目标平台、问题表现、复现路径与复现概率
2. **推断相关代码并埋设日志** → 找到页面及关联文件，匹配已知问题模式（通用场景问题 / JS 动态化场景问题），并在可疑逻辑位置补充带 TAG 的日志打点
3. **编译运行、复现抓日志并分析修复** → 自动编译运行，提醒用户按路径复现，按 TAG 抓取日志分析问题，给出修复方案并在修复后再次验证

覆盖的典型问题：

- 通用场景：生命周期阻塞、首屏过量数据、过度日志、同步 Module 耗时、attr 逻辑过多、数据更新不合理、Observable 泄漏
- JS 动态化：JSON 解析慢、Range 比较慢、集合操作慢

## 参考文档

| 文件 | 说明 |
|------|------|
| [references/perf-execution-guide.md](references/perf-execution-guide.md) | 页面性能问题诊断执行指南 |
| [references/generic-perf-troubleshooting.md](references/generic-perf-troubleshooting.md) | 通用性能排查工具指引（Profile 工具使用等） |
| [references/PageCreateTrace.kt](references/PageCreateTrace.kt) | 事件名称枚举定义代码文件 |
