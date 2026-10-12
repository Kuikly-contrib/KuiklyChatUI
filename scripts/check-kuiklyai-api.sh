#!/usr/bin/env bash
# KuiklyAI API surface guardrail.
# Internal rendering and protocol helpers must not become public by omission.

set -u
ERRORS=0
AI_ROOT="kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai"

check_internal() {
  local file="$1"
  local declaration="$2"
  local symbol="$3"
  if [ ! -f "$file" ]; then
    echo "❌ KuiklyAI API 校验找不到 $file"
    echo "✅ FIX: 恢复文件或更新 scripts/check-kuiklyai-api.sh"
    echo "📖 See: docs/design/feature-kuiklyai-toolkit.md"
    ERRORS=$((ERRORS + 1))
    return
  fi
  if grep -Eq "^[[:space:]]*(public[[:space:]]+)?${declaration}" "$file"; then
    echo "❌ KuiklyAI 内部实现 $symbol 未声明为 internal。"
    echo "✅ FIX: 在声明前添加 internal，公共入口只保留 Provider 和平台契约。"
    echo "📖 See: docs/design/feature-kuiklyai-toolkit.md"
    ERRORS=$((ERRORS + 1))
  fi
}

check_internal "$AI_ROOT/provider/BaseLlmProvider.kt" "class[[:space:]]+LlmIdGenerator[[:space:]]*\\(" "LlmIdGenerator"

if [ "$ERRORS" -gt 0 ]; then
  echo "发现 $ERRORS 个 KuiklyAI API 暴露问题，请修复后重试。"
  exit 1
fi

echo "✅ KuiklyAI API surface 检查通过"
