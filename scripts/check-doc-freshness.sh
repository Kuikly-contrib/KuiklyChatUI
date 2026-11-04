#!/usr/bin/env bash
# scripts/check-doc-freshness.sh
# 检查 docs/ 下文档 front-matter 里的 last_updated 是否过期。
# 口径与 docs/README.md 一致：以 front-matter 为准，不看 git 提交时间。
#
# 用法：
#   bash scripts/check-doc-freshness.sh              # 扫描全部 docs/design
#   bash scripts/check-doc-freshness.sh all          # 同上
#   bash scripts/check-doc-freshness.sh changed <base-ref>   # 只看相对 base 分支改动的文档
#
# 只告警不阻断，退出码恒为 0；缺失或格式非法的 last_updated 也只告警。
set -u

MODE="${1:-all}"
BASE_REF="${2:-}"
STALE_DAYS=60
DESIGN_DIR="docs/design"

# 日期转 epoch：GNU date 与 BSD/macOS date 参数不同，两种都试。
to_epoch() {
  date -j -f "%Y-%m-%d" "$1" "+%s" 2>/dev/null || date -d "$1" "+%s" 2>/dev/null
}

collect_files() {
  if [ "$MODE" = "changed" ]; then
    if [ -z "$BASE_REF" ]; then
      echo "用法: bash scripts/check-doc-freshness.sh changed <base-ref>" >&2
      exit 0
    fi
    git diff --name-only "${BASE_REF}...HEAD" -- "${DESIGN_DIR}/*.md"
  else
    find "$DESIGN_DIR" -name '*.md' | sort
  fi
}

NOW=$(date +%s)
STALE_COUNT=0
CHECKED=0

while IFS= read -r f; do
  [ -z "$f" ] && continue
  [ -f "$f" ] || continue
  CHECKED=$((CHECKED + 1))

  last_updated=$(sed -n '1,10s/^last_updated:[[:space:]]*//p' "$f" | head -1 | tr -d '\r')
  if [ -z "$last_updated" ]; then
    echo "⚠️  ${f} 缺少 front-matter 的 last_updated 字段"
    STALE_COUNT=$((STALE_COUNT + 1))
    continue
  fi

  epoch=$(to_epoch "$last_updated")
  if [ -z "$epoch" ]; then
    echo "⚠️  ${f} 的 last_updated 格式非法（应为 YYYY-MM-DD）：${last_updated}"
    STALE_COUNT=$((STALE_COUNT + 1))
    continue
  fi

  days_old=$(( (NOW - epoch) / 86400 ))
  if [ "$days_old" -gt "$STALE_DAYS" ]; then
    echo "⚠️  ${f} 的 last_updated 为 ${last_updated}，已 ${days_old} 天未更新（阈值 ${STALE_DAYS} 天）"
    STALE_COUNT=$((STALE_COUNT + 1))
  fi
done <<EOF
$(collect_files)
EOF

if [ "$CHECKED" -eq 0 ]; then
  echo "（本次没有需要检查的设计文档）"
elif [ "$STALE_COUNT" -eq 0 ]; then
  echo "✅ ${CHECKED} 份设计文档均在新鲜度窗口内"
else
  echo "共 ${STALE_COUNT} 份设计文档需要复核，请更新内容后同步 front-matter 的 last_updated。"
fi
