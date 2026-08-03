#!/usr/bin/env bash
# KuiklyAI API surface guardrail.
# Internal rendering and protocol helpers must not become public by omission.

set -u
ERRORS=0
ROOT="KuiklyChat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai"

check_internal() {
  local file="$1"
  local declaration="$2"
  local symbol="$3"
  if grep -Eq "^[[:space:]]*(public[[:space:]]+)?${declaration}" "$ROOT/$file"; then
    echo "❌ KuiklyAI 内部实现 $symbol 未声明为 internal。"
    echo "✅ FIX: 在声明前添加 internal，公共入口只保留 KuiklyAiChatView 和扩展契约。"
    echo "📖 See: docs/design/feature-kuiklyai-toolkit.md"
    ERRORS=$((ERRORS + 1))
  fi
}

check_internal "ui/LlmChatInput.kt" "fun[[:space:]]+LlmChatInput[[:space:]]*\\(" "LlmChatInput"
check_internal "ui/LlmMessageList.kt" "fun[[:space:]]+LlmMessageList[[:space:]]*\\(" "LlmMessageList"
check_internal "ui/LlmWelcomeSection.kt" "fun[[:space:]]+LlmWelcomeSection[[:space:]]*\\(" "LlmWelcomeSection"
check_internal "ui/LlmAttachmentRow.kt" "fun[[:space:]]+LlmAttachmentRow[[:space:]]*\\(" "LlmAttachmentRow"
check_internal "ui/LlmMessageBubbles.kt" "fun[[:space:]]+LlmUserBubble[[:space:]]*\\(" "LlmUserBubble"
check_internal "ui/LlmMessageBubbles.kt" "fun[[:space:]]+LlmResponseBubble[[:space:]]*\\(" "LlmResponseBubble"
check_internal "ui/LlmImagePreviewDialog.kt" "fun[[:space:]]+LlmImagePreviewDialog[[:space:]]*\\(" "LlmImagePreviewDialog"
check_internal "ui/LlmImagePreviewDialog.kt" "data[[:space:]]+class[[:space:]]+LlmImageTransformState[[:space:]]*\\(" "LlmImageTransformState"
check_internal "provider/LlmTransport.kt" "class[[:space:]]+SseDecoder[[:space:]]*\\(" "SseDecoder"
check_internal "provider/BaseLlmProvider.kt" "class[[:space:]]+LlmIdGenerator[[:space:]]*\\(" "LlmIdGenerator"

if [ "$ERRORS" -gt 0 ]; then
  echo "发现 $ERRORS 个 KuiklyAI API 暴露问题，请修复后重试。"
  exit 1
fi

echo "✅ KuiklyAI API surface 检查通过"
