#!/usr/bin/env bash
# scripts/agent-verify.sh
# 在隔离的 git worktree 中为指定分支跑全套护栏，验证 Agent 自己的改动。
# 用法：bash scripts/agent-verify.sh <branch>
set -euo pipefail

BRANCH="${1:?用法: bash scripts/agent-verify.sh <branch>}"
WORKTREE_DIR="/tmp/agent-verify-$(date +%s)"

echo "🔧 创建 worktree: $WORKTREE_DIR"
git worktree add "$WORKTREE_DIR" "$BRANCH"
trap 'git worktree remove --force "$WORKTREE_DIR" 2>/dev/null || true' EXIT

cd "$WORKTREE_DIR"

echo "📦 同步依赖（Gradle 缓存复用，无需 npm ci）..."
./gradlew --offline :KuiklyChat:help >/dev/null 2>&1 || ./gradlew :KuiklyChat:help >/dev/null

echo "🔍 编译检查..."
# 本模块无 jvm() target，JVM 侧编译走 android target 的 Kotlin 任务。
./gradlew :KuiklyChat:compileDebugKotlinAndroid :KuiklyChat:compileDebugUnitTestKotlinAndroid

echo "🔍 架构与文件大小检查..."
bash scripts/check-layer-deps.sh

echo "🧪 运行测试..."
./gradlew :KuiklyChat:testDebugUnitTest

echo "✅ 所有验证通过（分支 $BRANCH）"
