---
name: kuikly-third-party
description: 查询和获取 Kuikly 第三方 UI 组件的详细信息。自动同步组件仓库，支持按关键词或功能搜索组件，并使用 deepwiki-mcp 获取全面的组件文档（含使用示例和集成指南）。适用场景：查找第三方组件、Kuikly 有哪些组件、UI 组件库、第三方库、外部组件、开源组件、KuiklyUI 组件、图表组件、动画组件等。
---

# Kuikly 第三方组件查询

## 使用事件上报（必须执行）

**每次 Skill 被调用时，必须首先执行：**

```bash
bash "${SKILL_DIR}/scripts/beacon-report.sh" &
```

## 概述

本技能用于查询和获取 Kuikly 可用第三方 UI 组件的详细信息。它维护一个本地组件定义仓库的最新副本，并利用 deepwiki-mcp 工具获取每个组件的全面文档。

## 使用场景

在以下情况下使用此技能：
- 用户询问"Kuikly 有哪些第三方组件可用？"
- 用户请求特定 Kuikly 组件的信息（例如："告诉我 Kuikly 中的 ECharts 组件"）
- 用户需要 Kuikly 第三方组件的使用示例或文档
- 用户想通过关键词或功能搜索组件
- 用户询问可以与 KuiklyUI 集成的 UI 库

## 工作流程

### 1. 确保仓库是最新的

在查询组件之前，始终运行同步脚本以确保本地仓库是最新的：

```bash
python scripts/sync_repo.py
```

该脚本会：
- 如果仓库不存在则克隆（从 https://github.com/Tencent-TDS/KuiklyUI-third-party.git）
- 检查距上次更新是否超过 1 周
- 如果需要则自动执行 `git pull`
- 使用 `--force` 参数可强制立即更新：`python scripts/sync_repo.py --force`

仓库存储位置：`references/KuiklyUI-third-party/`

### 2. 查询可用组件

使用查询脚本查找和获取组件信息：

**列出所有组件：**
```bash
python scripts/query_components.py list
```

**按关键词搜索组件：**
```bash
python scripts/query_components.py search <关键词>
```

示例：
- `python scripts/query_components.py search chart` - 查找图表相关组件
- `python scripts/query_components.py search animation` - 查找动画库

**获取特定组件详情：**
```bash
python scripts/query_components.py get <组件名称>
```

示例：
- `python scripts/query_components.py get echarts` - 获取 ECharts 组件详情

脚本从 `references/KuiklyUI-third-party/KuiklyUI-Libraries.json` 读取数据，包含：
- 组件名称（componentName）
- 组件描述（componentDescription）
- GitHub URL（githubUrl 字段）
- 动态化模式是否支持直接引用该组件（sDynamic 字段）
- 其他元数据

### 3. 使用 deepwiki-mcp 获取详细文档

一旦识别出组件的 GitHub URL，使用 deepwiki-mcp 工具获取详细文档：

**步骤 1：从 GitHub URL 提取仓库名称**

JSON 中的 `githubUrl` 字段格式为：`https://github.com/owner/repo`

提取 `owner/repo` 部分（例如，从 `https://github.com/apache/echarts` 提取 `apache/echarts`）

**步骤 2：使用 deepwiki-mcp 获取文档**

首先，获取文档结构：
```
mcp__deepwiki-mcp__read_wiki_structure(repoName: "owner/repo")
```

然后，读取详细文档：
```
mcp__deepwiki-mcp__read_wiki_contents(repoName: "owner/repo")
```

或针对组件提出具体问题：
```
mcp__deepwiki-mcp__ask_question(
    repoName: "owner/repo",
    question: "如何将这个组件集成到 KuiklyUI 中？请给我展示使用示例。"
)
```

### 4. 总结并呈现给用户

获取文档后，向用户提供：
- 组件概览和用途
- 主要功能和特性
- KuiklyUI 集成说明
- 代码示例和使用模式，动态化模式是否支持直接引用该组件
- 官方文档链接
- 常见用例

## 使用示例

**用户问：** "Kuikly 有哪些图表组件可用？"

**工作流程：**
1. 运行 `python scripts/sync_repo.py` 确保仓库是最新的
2. 运行 `python scripts/query_components.py search chart` 查找图表相关组件
3. 对于找到的每个相关组件，提取 GitHub URL
4. 使用 `mcp_call_tool` 配合 deepwiki-mcp 获取详细文档
5. 总结组件的功能、使用示例和集成指南

**用户问：** "告诉我如何在 Kuikly 中使用 ECharts"

**工作流程：**
1. 运行 `python scripts/sync_repo.py`
2. 运行 `python scripts/query_components.py get echarts` 获取组件详情
3. 提取 GitHub URL（例如 `apache/echarts`）
4. 使用 deepwiki-mcp 的 `ask_question` 工具提出关于集成的具体问题
5. 向用户展示分步集成说明和代码示例

## 脚本参考

### `scripts/sync_repo.py`
管理 KuiklyUI-third-party 仓库同步。

**用法：**
- `python scripts/sync_repo.py` - 如果距上次更新超过 1 周则自动同步
- `python scripts/sync_repo.py --force` - 立即强制同步

**功能：**
- 如果仓库不存在则克隆
- 检查上次更新时间戳
- 需要时执行 git pull

### `scripts/query_components.py`
从 KuiklyUI-Libraries.json 查询组件信息。

**命令：**
- `list` - 显示所有可用组件
- `search <关键词>` - 按名称或描述搜索
- `get <名称>` - 获取特定组件详情（包括 GitHub URL）

**输出：**
显示组件名称、描述和 GitHub URL，以便进一步检索文档。

## 重要提示

- 始终先同步仓库，然后再查询，以确保获取最新信息
- GitHub URL 对于通过 deepwiki-mcp 获取详细文档至关重要
- 如果距上次更新超过 1 周，仓库会自动同步，防止数据过时
- 为了性能考虑，仓库只克隆一次，然后增量更新
- 如果 JSON 结构发生变化，查询脚本可能需要更新
