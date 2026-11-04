#!/usr/bin/env bash
# scripts/check-public-api.sh
# 未冻结的实现细节不得以 public 形式合入。

set -u
ERRORS=0
COMPAT="kuiklychat-compat/src/commonMain/kotlin/com/tencent/kuiklybase/chat"

must_be_internal() {
  local path="$1"
  local pattern="$2"
  local symbol="$3"
  if [ ! -f "$path" ]; then
    echo "❌ keep-set 校验找不到 $path"
    echo "✅ FIX: 恢复文件或更新 scripts/check-public-api.sh"
    echo "📖 See: docs/api/public-keep-set.md"
    ERRORS=$((ERRORS + 1))
    return
  fi
  if grep -Eq "^(public[[:space:]]+)?${pattern}" "$path"; then
    echo "❌ $symbol 仍是 public（缺少 internal）。"
    echo "✅ FIX: 将 $symbol 标为 internal，宿主请走 keep-set 中的稳定入口。"
    echo "📖 See: docs/api/public-keep-set.md"
    ERRORS=$((ERRORS + 1))
  fi
}

must_be_internal "$COMPAT/model/AttachmentDraft.kt" "data[[:space:]]+class[[:space:]]+AttachmentDraft" "AttachmentDraft"
must_be_internal "$COMPAT/experience/ChatExperienceComponents.kt" "class[[:space:]]+ChatScrollToBottomButtonView" "ChatScrollToBottomButtonView"
must_be_internal "$COMPAT/channel/ChatChannelHeaderView.kt" "class[[:space:]]+ChatChannelHeaderView" "ChatChannelHeaderView"
must_be_internal "$COMPAT/channel/ChatChannelHeaderView.kt" "class[[:space:]]+ChatChannelHeaderAttr" "ChatChannelHeaderAttr"
must_be_internal "$COMPAT/channel/ChatChannelHeaderView.kt" "class[[:space:]]+ChatChannelHeaderEvent" "ChatChannelHeaderEvent"
must_be_internal "$COMPAT/channel/ChatChannelItemView.kt" "class[[:space:]]+ChatChannelItemView" "ChatChannelItemView"
must_be_internal "$COMPAT/channel/ChatChannelItemView.kt" "class[[:space:]]+ChatChannelItemAttr" "ChatChannelItemAttr"
must_be_internal "$COMPAT/channel/ChatChannelItemView.kt" "class[[:space:]]+ChatChannelItemEvent" "ChatChannelItemEvent"
must_be_internal "$COMPAT/channel/ChatChannelItemView.kt" "fun[[:space:]]+ViewContainer.*ChatChannelItem" "ChatChannelItem"
must_be_internal "$COMPAT/channel/ChatChannelMemberView.kt" "class[[:space:]]+ChatChannelMemberItemView" "ChatChannelMemberItemView"
must_be_internal "$COMPAT/channel/ChatChannelMemberView.kt" "class[[:space:]]+ChatChannelMemberItemAttr" "ChatChannelMemberItemAttr"
must_be_internal "$COMPAT/channel/ChatChannelMemberView.kt" "class[[:space:]]+ChatChannelMemberItemEvent" "ChatChannelMemberItemEvent"
must_be_internal "$COMPAT/channel/ChatChannelMemberView.kt" "fun[[:space:]]+ViewContainer.*ChatChannelMemberItem" "ChatChannelMemberItem"
if ! grep -q 'typealias ChatMessage' "$COMPAT/model/ChatMessage.kt"; then
  echo "❌ model ChatMessage 不是 compose.core.ChatMessage 的 typealias。"
  echo "✅ FIX: 会话与 Compose 共用同一 ChatMessage，构造走 sessionChatMessage()。"
  echo "📖 See: docs/api/public-keep-set.md"
  ERRORS=$((ERRORS + 1))
fi

if grep -q 'val canonical:' "$COMPAT/model/ChatMessage.kt"; then
  echo "❌ model ChatMessage 仍是第二套信封（canonical 包装）。"
  echo "✅ FIX: 删除信封，改成 typealias compose.core.ChatMessage。"
  echo "📖 See: docs/api/public-keep-set.md"
  ERRORS=$((ERRORS + 1))
fi

if ! grep -q 'typealias MessageStatus' "$COMPAT/model/ChatMessageEnums.kt"; then
  echo "❌ model MessageStatus 不是 compose.core.MessageStatus 的 typealias。"
  echo "✅ FIX: 删除第二套枚举，已读态走 ChatMessage.readBy / isRead。"
  echo "📖 See: docs/api/public-keep-set.md"
  ERRORS=$((ERRORS + 1))
fi

if grep -q 'enum class MessageStatus' "$COMPAT/model/ChatMessageEnums.kt"; then
  echo "❌ model 仍有独立 MessageStatus 枚举。"
  echo "✅ FIX: 改成 typealias compose.core.MessageStatus。"
  echo "📖 See: docs/api/public-keep-set.md"
  ERRORS=$((ERRORS + 1))
fi

if [ "$ERRORS" -gt 0 ]; then
  echo "发现 $ERRORS 个 public API keep-set 违规，请修复后重试。"
  exit 1
fi

BASELINE="scripts/public-api-baseline.txt"
DUMP=$(mktemp)
python3 - <<'PY' > "$DUMP"
import os, re
roots = [
    "kuiklychat-core/src/commonMain/kotlin",
    "kuiklychat-compose/src/commonMain/kotlin",
    "kuiklychat-compat/src/commonMain/kotlin",
    "kuiklychat-ai/src/commonMain/kotlin",
]
pattern = re.compile(r'^(?:public\s+)?(class|data class|sealed class|sealed interface|object|interface|enum class|fun |typealias |const val |val |annotation class )(.+)$')
skip = re.compile(r'^(internal|private|protected)\s+')
rows = []
for root in roots:
    if not os.path.isdir(root):
        continue
    for dirpath, _, files in os.walk(root):
        for name in files:
            if not name.endswith('.kt'):
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding='utf-8') as handle:
                for line in handle:
                    text = line.rstrip()
                    if skip.match(text) or not pattern.match(text):
                        continue
                    rel = os.path.relpath(path).replace('\\', '/')
                    rows.append(f"{rel}::{text}")
print('\n'.join(sorted(rows)))
PY

if [ ! -f "$BASELINE" ]; then
  echo "❌ 缺少 $BASELINE"
  echo "✅ FIX: 运行 python dump 生成 public API 基线。"
  echo "📖 See: docs/api/public-keep-set.md"
  rm -f "$DUMP"
  exit 1
fi

ADDED=$(comm -13 "$BASELINE" "$DUMP" || true)
REMOVED=$(comm -23 "$BASELINE" "$DUMP" || true)
if [ -n "$ADDED" ]; then
  echo "❌ 新增了未登记的 public API："
  echo "$ADDED"
  echo "✅ FIX: 能 internal 就 internal；确实要暴露则更新 $BASELINE 并在 keep-set 说明理由。"
  echo "📖 See: docs/api/public-keep-set.md"
  ERRORS=$((ERRORS + 1))
fi
if [ -n "$REMOVED" ]; then
  echo "❌ $BASELINE 已过期（下列声明已不在 public dump 中）："
  echo "$REMOVED"
  echo "✅ FIX: 从 $BASELINE 删除上述行，锁定收口成果。"
  echo "📖 See: docs/api/public-keep-set.md"
  ERRORS=$((ERRORS + 1))
fi
rm -f "$DUMP"

if [ "$ERRORS" -gt 0 ]; then
  echo "发现 $ERRORS 个 public API keep-set 违规，请修复后重试。"
  exit 1
fi

echo "✅ public API keep-set 检查通过"
