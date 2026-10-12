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
./gradlew --offline :kuiklychat-core:help >/dev/null 2>&1 || ./gradlew :kuiklychat-core:help >/dev/null

echo "🔍 编译检查..."
bash scripts/ci-gates.sh

echo "✅ 所有验证通过（分支 $BRANCH）"
