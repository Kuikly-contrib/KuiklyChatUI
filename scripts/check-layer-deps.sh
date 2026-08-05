#!/usr/bin/env bash
# scripts/check-layer-deps.sh
# KuiklyChat 分层依赖门禁：锁死依赖方向，违规即阻断合入。
# 零依赖，可本地运行：  bash scripts/check-layer-deps.sh
# CI 中建议串联测试：  bash scripts/check-layer-deps.sh && ./gradlew :KuiklyChat:testDebugUnitTest

set -u
ERRORS=0
CHAT_ROOT="KuiklyChat/src/commonMain/kotlin/com/tencent/kuiklybase/chat"
MAX_LINES=400
BASELINE="scripts/file-size-baseline.txt"

err() {
  echo "❌ $1"
  echo "✅ FIX: $2"
  echo "📖 See: $3"
  ERRORS=$((ERRORS + 1))
}

# 规则1：commonMain 不得引入平台符号（androidx.compose 是多平台运行时，允许）
if grep -rn --include='*.kt' -E '^[[:space:]]*import[[:space:]]+(android\.|java\.|javax\.|platform\.|kotlinx\.cinterop)' "$CHAT_ROOT"; then
  err "commonMain 引入了平台符号（android./java./javax./platform./kotlinx.cinterop）。" \
      "平台能力用 expect/actual 下沉到 androidMain/iosMain/macosMain。" \
      "docs/architecture/boundaries.md"
fi

# 规则2：agui 不得依赖任何 UI 包
if grep -rn --include='*.kt' -E "import[[:space:]]+com\.tencent\.kuiklybase\.chat\.(bubble|session|channel|legacy|navigation|indicator|ai|widget|agentcard|compose)" "$CHAT_ROOT/agui"; then
  err "agui 层直接依赖了 UI 包。" \
      "协议层只产出 ChatMessage / AgentCard 模型，UI 通过模型解耦。" \
      "docs/architecture/boundaries.md"
fi

# 规则3：agentcard(Kuikly DSL) 不得依赖 compose/agentcard(Compose DSL)
if grep -rn --include='*.kt' -E "import[[:space:]]+com\.tencent\.kuiklybase\.chat\.compose\.agentcard" "$CHAT_ROOT/agentcard"; then
  err "agentcard 直接依赖了 compose/agentcard。" \
      "两套渲染层互不依赖；共享类型抽到 model/ 或共享数据类。" \
      "docs/architecture/boundaries.md"
fi

# 规则4：commonMain 不得遗留裸 println（KDoc 注释中的示例除外）
if grep -rn --include='*.kt' 'println(' "$CHAT_ROOT" | grep -vE '\*|//'; then
  err "commonMain 存在裸 println( 调试打印)。" \
      "改用结构化日志；调试打印不得合入主干。" \
      "docs/conventions/logging.md"
fi

# 规则6：kuiklyai 的 provider/history 层不得依赖任何 UI 包。
# LlmProvider 是对外扩展点，第三方要能在不碰 Compose 的前提下实现它。
if grep -rn --include='*.kt' -E "import[[:space:]]+(com\.tencent\.kuikly\.compose|com\.tencent\.kuiklybase\.chat\.(bubble|session|channel|legacy|navigation|indicator|widget|agentcard|compose|kuiklyai\.runtime))" \
  "$CHAT_ROOT/kuiklyai/provider" "$CHAT_ROOT/kuiklyai/history" 2>/dev/null; then
  err "kuiklyai 的 provider/history 层依赖了 UI 包。" \
      "provider 层只产出 LlmChatMessage；渲染由 legacy/ai 负责，两者通过模型解耦。" \
      "docs/design/feature-kuiklyai-toolkit.md"
fi

# 规则7：kuiklyai 不得依赖任何厂商 SDK。
# 这是 KuiklyAI 与 Flutter AI Toolkit 的核心差异——后者的唯一官方实现
# 绑死 Firebase，导致「provider 可插拔」只在接口层成立。内置实现一律
# 只依赖宿主注入的 LlmTransport，不引入任何具体厂商依赖。
if grep -rn --include='*.kt' -E "^[[:space:]]*import[[:space:]]+(com\.google\.(firebase|ai)|com\.firebase|dev\.langchain4j|com\.openai|com\.anthropic|com\.aallam\.openai|io\.ktor)" \
  "$CHAT_ROOT/kuiklyai" 2>/dev/null; then
  err "kuiklyai 引入了厂商 SDK 或具体网络栈依赖。" \
      "内置 provider 只能依赖 LlmTransport 抽象，由宿主注入网络实现；厂商适配放到宿主侧或独立可选模块。" \
      "docs/design/feature-kuiklyai-toolkit.md"
fi

# 规则8：KuiklyAI 内部渲染/协议辅助声明必须显式 internal，避免默认 public 扩大 ABI。
if ! bash scripts/check-kuiklyai-api.sh; then
  err "KuiklyAI 内部实现意外暴露为 public API。" \
      "为实现细节添加 internal；仅公共 UI 入口、平台契约和 Provider 扩展点保持 public。" \
      "docs/design/feature-kuiklyai-toolkit.md"
fi

# 规则5：单文件 ≤ 400 行。存量超标文件登记在 baseline 中，只告警不阻断；
# 新增超标文件直接阻断。baseline 中已达标的文件必须移除，否则报错，防止清单腐化。
STALE_BASELINE=""
NEW_OVERSIZED=""
while IFS= read -r f; do
  lines=$(wc -l < "$f" | tr -d ' ')
  in_baseline=no
  if [ -f "$BASELINE" ] && grep -qxF "$f" "$BASELINE"; then
    in_baseline=yes
  fi

  if [ "$lines" -gt "$MAX_LINES" ]; then
    if [ "$in_baseline" = yes ]; then
      echo "⚠️  $f 有 $lines 行（存量豁免，待拆分）"
    else
      NEW_OVERSIZED="${NEW_OVERSIZED}    ${f}（${lines} 行）\n"
    fi
  elif [ "$in_baseline" = yes ]; then
    STALE_BASELINE="${STALE_BASELINE}    ${f}（现 ${lines} 行）\n"
  fi
done <<EOF
$(find KuiklyChat/src -name '*.kt' | sort)
EOF

if [ -n "$NEW_OVERSIZED" ]; then
  echo "以下文件超过 $MAX_LINES 行且不在存量豁免清单中："
  printf "%b" "$NEW_OVERSIZED"
  err "存在新增的超长文件。" \
      "拆分为职责单一的子文件（辅助函数/扩展/渲染器各自成文件），不要往 baseline 里加新条目。" \
      "docs/conventions/mechanical-rules.md"
fi

if [ -n "$STALE_BASELINE" ]; then
  echo "以下文件已降到 $MAX_LINES 行以内，但仍留在 $BASELINE 中："
  printf "%b" "$STALE_BASELINE"
  err "存量豁免清单已过期。" \
      "从 $BASELINE 删除上述文件所在行，锁定拆分成果。" \
      "docs/conventions/mechanical-rules.md"
fi

if [ "$ERRORS" -gt 0 ]; then
  echo ""
  echo "发现 $ERRORS 个架构违规，请修复后重试。"
  exit 1
fi

# 规则9：compose/core 不得依赖 compose/ui
if grep -rn --include='*.kt' -E "import[[:space:]]+com\.tencent\.kuiklybase\.chat\.compose\.ui" \
  "$CHAT_ROOT/compose/core" 2>/dev/null; then
  err "compose/core 依赖了 compose/ui。" \
      "核心层只放模型与缓存；Composable 注入放在 compose/ui。" \
      "docs/design/feature-compose-resolve-user-cache.md"
fi

if [ "$ERRORS" -gt 0 ]; then
  echo ""
  echo "发现 $ERRORS 个架构违规，请修复后重试。"
  exit 1
fi

echo "✅ 架构依赖检查通过"
