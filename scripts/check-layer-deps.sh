#!/usr/bin/env bash
# scripts/check-layer-deps.sh
# KuiklyChat 分层依赖门禁：锁死依赖方向，违规即阻断合入。
# 零依赖，可本地运行：  bash scripts/check-layer-deps.sh

set -u
ERRORS=0
LIB_MODULES=(
  kuiklychat-core
  kuiklychat-compose
  kuiklychat-compat
  kuiklychat-ai
  kuiklychat-testing
)
CHAT_DIRS=()
for module in "${LIB_MODULES[@]}"; do
  CHAT_DIRS+=("$module/src/commonMain/kotlin/com/tencent/kuiklybase/chat")
done
MAX_LINES=400
BASELINE="scripts/file-size-baseline.txt"

err() {
  echo "❌ $1"
  echo "✅ FIX: $2"
  echo "📖 See: $3"
  ERRORS=$((ERRORS + 1))
}

grep_chat() {
  grep -rn --include='*.kt' "$@" "${CHAT_DIRS[@]}" 2>/dev/null
}

# 规则1：commonMain 不得引入平台符号（androidx.compose 是多平台运行时，允许）
if grep_chat -E '^[[:space:]]*import[[:space:]]+(android\.|java\.|javax\.|platform\.|kotlinx\.cinterop)'; then
  err "commonMain 引入了平台符号（android./java./javax./platform./kotlinx.cinterop）。" \
      "平台能力用 expect/actual 下沉到 androidMain/iosMain/macosMain。" \
      "docs/architecture/boundaries.md"
fi

# 规则2：agui 包不得依赖任何 UI 包
if grep -rn --include='*.kt' -E "import[[:space:]]+com\.tencent\.kuiklybase\.chat\.(bubble|session|channel|legacy|navigation|indicator|ai|widget|agentcard|compose)" \
  kuiklychat-agui/src/commonMain/kotlin/com/tencent/kuiklybase/chat/agui 2>/dev/null; then
  err "agui 层直接依赖了 UI 包。" \
      "协议层只产出 ChatMessage / AgentCard 模型，UI 通过模型解耦。" \
      "docs/architecture/boundaries.md"
fi

# 规则3：agentcard(Kuikly DSL) 不得依赖 compose/agentcard(Compose DSL)
if grep -rn --include='*.kt' -E "import[[:space:]]+com\.tencent\.kuiklybase\.chat\.compose\.agentcard" \
  kuiklychat-agentcard/src/commonMain/kotlin/com/tencent/kuiklybase/chat/agentcard 2>/dev/null; then
  err "agentcard 直接依赖了 compose/agentcard。" \
      "两套渲染层互不依赖；共享类型抽到 model/ 或共享数据类。" \
      "docs/architecture/boundaries.md"
fi

# 规则4：commonMain 不得遗留裸 println（KDoc 注释中的示例除外）
if grep_chat 'println(' | grep -vE '\*|//'; then
  err "commonMain 存在裸 println( 调试打印)。" \
      "改用结构化日志；调试打印不得合入主干。" \
      "docs/conventions/logging.md"
fi

# 规则6：kuiklyai 的 provider/history 层不得依赖任何 UI 包。
if grep -rn --include='*.kt' -E "import[[:space:]]+(com\.tencent\.kuikly\.compose|com\.tencent\.kuiklybase\.chat\.(bubble|session|channel|legacy|navigation|indicator|widget|agentcard|compose|kuiklyai\.runtime))" \
  kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/provider \
  kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/history \
  kuiklychat-agui/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai/provider \
  2>/dev/null; then
  err "kuiklyai 的 provider/history 层依赖了 UI 包。" \
      "provider 层只产出 LlmChatMessage；渲染由 ChatControllerSession 或 ChatScreen 负责。" \
      "docs/design/feature-kuiklyai-toolkit.md"
fi

# 规则7：kuiklyai 不得依赖任何厂商 SDK。
if grep -rn --include='*.kt' -E "^[[:space:]]*import[[:space:]]+(com\.google\.(firebase|ai)|com\.firebase|dev\.langchain4j|com\.openai|com\.anthropic|com\.aallam\.openai|io\.ktor)" \
  kuiklychat-ai/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai \
  kuiklychat-agui/src/commonMain/kotlin/com/tencent/kuiklybase/chat/kuiklyai \
  2>/dev/null; then
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

# 规则10：SDK keep-set — 未冻结的实现细节不得 public。
if ! bash scripts/check-public-api.sh; then
  err "public API keep-set 校验失败。" \
      "将 AttachmentDraft / Composer 叶子组件 / 内置 Renderer 标为 internal。" \
      "docs/api/public-keep-set.md"
fi

# 规则9：compose/core 不得依赖 UI / 协议 / adapter
if grep -rn --include='*.kt' -E "import[[:space:]]+com\.tencent\.kuiklybase\.chat\.(compose\.(ui|adapter)|agui|session|bubble|legacy)" \
  kuiklychat-core/src/commonMain/kotlin/com/tencent/kuiklybase/chat/compose/core 2>/dev/null; then
  err "compose/core 依赖了 UI 或协议层。" \
      "核心层只放模型、Controller 与 ports。" \
      "docs/design/feature-kuikly-chat-ui-sdk-redesign.md"
fi

# 规则11：compose/ui 不得依赖 adapter / AG-UI / KuiklyAI
if grep -rn --include='*.kt' -E "import[[:space:]]+com\.tencent\.kuiklybase\.chat\.(compose\.adapter|agui|kuiklyai)" \
  kuiklychat-compose/src/commonMain/kotlin/com/tencent/kuiklybase/chat/compose/ui 2>/dev/null; then
  err "compose/ui 依赖了 adapter、AG-UI 或 KuiklyAI。" \
      "协议与 picker 实现放在 adapter；UI 只消费 ChatCallbacks / MessageRenderer。" \
      "docs/design/feature-kuikly-chat-ui-sdk-redesign.md"
fi

# 规则12：bubble/session 不得依赖 agui 协议类型
if grep -rn --include='*.kt' -E "import[[:space:]]+com\.tencent\.kuiklybase\.chat\.agui" \
  kuiklychat-compat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/bubble \
  kuiklychat-compat/src/commonMain/kotlin/com/tencent/kuiklybase/chat/session \
  2>/dev/null; then
  err "bubble/session 直接依赖了 agui。" \
      "把富消息 kind/payload 下沉到 model，或改为渲染 AgentCard / MessagePart。" \
      "docs/design/feature-kuikly-chat-ui-sdk-redesign.md"
fi

# 规则13：Gradle 模块不得形成环，且 Core 不得依赖 Compose / 协议模块
if grep -n 'project(":kuiklychat-' \
  kuiklychat-core/build.gradle.kts \
  kuiklychat-compose/build.gradle.kts \
  kuiklychat-compat/build.gradle.kts \
  kuiklychat-ai/build.gradle.kts \
  kuiklychat-testing/build.gradle.kts \
  | grep -E 'kuiklychat-core/build.gradle.kts:.*project\(":kuiklychat-(compose|compat|agui|agentcard|ai|testing)"\)'; then
  err "kuiklychat-core 依赖了下游模块。" \
      "Core 只能依赖 kotlinx；UI / 协议走 compose / compat / agui。" \
      "docs/api/public-keep-set.md"
fi
if grep -q 'project(":kuiklychat-agentcard")' kuiklychat-compose/build.gradle.kts; then
  err "kuiklychat-compose 依赖了 kuiklychat-agentcard。" \
      "AgentCard 是可选模块，应依赖 Compose UI，而不是反过来。" \
      "docs/api/public-keep-set.md"
fi
if grep -q 'project(":kuiklychat-agui")' kuiklychat-ai/build.gradle.kts; then
  err "kuiklychat-ai 依赖了 kuiklychat-agui。" \
      "AG-UI provider 放在 agui 模块，避免 ai ↔ agui 循环。" \
      "docs/api/public-keep-set.md"
fi

# 规则14：可发布库必须默认开启严格 explicitApi()，不得再用开关或 Warning 挡门禁
for module in "${LIB_MODULES[@]}"; do
  for gradle_file in "$module/build.gradle.kts" "$module/build.ohos.gradle.kts"; do
    [ -f "$gradle_file" ] || continue
    if grep -q 'findProperty("explicitApi")' "$gradle_file"; then
      err "$gradle_file 仍把 explicitApi 藏在属性开关后面。" \
          "在 kotlin { } 中无条件调用 explicitApi()。" \
          "docs/api/public-keep-set.md"
    fi
    if grep -q 'explicitApiWarning()' "$gradle_file"; then
      err "$gradle_file 仍使用 explicitApiWarning()。" \
          "发布库改为 kotlin { explicitApi() }。" \
          "docs/api/public-keep-set.md"
    fi
    if ! grep -q 'explicitApi()' "$gradle_file"; then
      err "$gradle_file 未启用严格 explicitApi()。" \
          "在 kotlin { } 中加入 explicitApi()。" \
          "docs/api/public-keep-set.md"
    fi
  done
done

# 规则15：Ohos 按模块编译，禁止伞形 fat-source
for module in "${LIB_MODULES[@]}"; do
  ohos_file="$module/build.ohos.gradle.kts"
  if [ ! -f "$ohos_file" ]; then
    err "缺少 $ohos_file。" \
        "为每个库模块提供 ohosArm64() 的 build.ohos.gradle.kts。" \
        "docs/api/public-keep-set.md"
    continue
  fi
  if ! grep -q 'ohosArm64' "$ohos_file"; then
    err "$ohos_file 未声明 ohosArm64()。" \
        "在该模块 Ohos 构建文件中加入 ohosArm64()。" \
        "docs/api/public-keep-set.md"
  fi
done
for module in "${LIB_MODULES[@]}"; do
  if ! grep -q "project(\":$module\").buildFileName" settings.ohos.gradle.kts \
    && ! grep -A20 'listOf(' settings.ohos.gradle.kts | grep -q ":$module"; then
    err "settings.ohos.gradle.kts 未给 :$module 指定 build.ohos.gradle.kts。" \
        "include 后设置 project(\":$module\").buildFileName。" \
        "docs/api/public-keep-set.md"
  fi
done

if grep -q 'include(":androidApp")' settings.ohos.gradle.kts; then
  err "settings.ohos.gradle.kts 不应 include :androidApp。" \
      "鸿蒙构建只保留 shared 与库模块；Android 宿主走 settings.gradle.kts。" \
      "docs/api/public-keep-set.md"
fi

# 规则16：消息列表必须按 message id 懒加载，滚动锚点必须基于 message key
LIST_FILE="kuiklychat-compose/src/commonMain/kotlin/com/tencent/kuiklybase/chat/compose/ui/ChatMessageList.kt"
SCROLL_FILE="kuiklychat-compose/src/commonMain/kotlin/com/tencent/kuiklybase/chat/compose/ui/ChatListScroll.kt"
if ! grep -q 'items(' "$LIST_FILE"; then
  err "ChatMessageList 没有 Lazy items()。" \
      "用 items(count, key = message.id) 渲染每条消息，不要把整表塞进单个 item。" \
      "docs/api/public-keep-set.md"
fi
if ! grep -q 'state.messages\[index\].id' "$LIST_FILE"; then
  err "ChatMessageList 未用 message id 作为 Lazy key。" \
      "key = { index -> state.messages[index].id }" \
      "docs/api/public-keep-set.md"
fi
if ! grep -q 'fun firstVisibleMessageKey' "$SCROLL_FILE" || ! grep -q 'fun lazyIndexOfMessageKey' "$SCROLL_FILE"; then
  err "缺少基于 message key 的滚动锚点函数。" \
      "保留 firstVisibleMessageKey / lazyIndexOfMessageKey。" \
      "docs/api/public-keep-set.md"
fi

# 规则5：单文件 ≤ 400 行。存量超标文件登记在 baseline 中，只告警不阻断；
# 新增超标文件直接阻断。baseline 中已达标的文件必须移除，否则报错，防止清单腐化。
STALE_BASELINE=""
NEW_OVERSIZED=""
while IFS= read -r f; do
  [ -z "$f" ] && continue
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
$(find "${LIB_MODULES[@]}" -path '*/src/*' -name '*.kt' 2>/dev/null | sort)
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

echo "✅ 架构依赖检查通过"
