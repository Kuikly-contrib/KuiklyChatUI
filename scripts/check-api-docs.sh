#!/usr/bin/env bash
# 检查发布文档中的版本坐标、模块清单和外网仓库地址没有漂移。

set -u
ERRORS=0

fail() {
  echo "❌ $1"
  echo "✅ FIX: $2"
  echo "📖 See: docs/api/host-integration.md"
  ERRORS=$((ERRORS + 1))
}

BASE_VERSION=$(sed -n 's/^MAVEN_VERSION=//p' gradle.properties | head -1)
KOTLIN_VERSION=$(sed -n 's/^KOTLIN_VERSION_LIST=//p' gradle.properties | head -1)
OHOS_KOTLIN_VERSION=$(sed -n 's/^OHOS_KOTLIN_VERSION_LIST=//p' gradle.properties | head -1)
EXPECTED_STANDARD="${BASE_VERSION}-${KOTLIN_VERSION}"
EXPECTED_OHOS="${BASE_VERSION}-${OHOS_KOTLIN_VERSION}"
DOCS=(README.md docs/api/chat-ui.md docs/api/host-integration.md docs/api/public-keep-set.md docs/api/usage.md)

if [ -z "$BASE_VERSION" ] || [ -z "$KOTLIN_VERSION" ] || [ -z "$OHOS_KOTLIN_VERSION" ]; then
  fail "gradle.properties 缺少版本配置。" "补齐 MAVEN_VERSION / KOTLIN_VERSION_LIST / OHOS_KOTLIN_VERSION_LIST。"
fi

if [ ! -f docs/api/usage.md ]; then
  fail "缺少 API 使用指南 docs/api/usage.md。" "补齐依赖、Controller、两套 UI 与生命周期示例。"
fi

if grep -nE 'composerBottomInset(Dp)?[[:space:]]*=[[:space:]]*pagerData\.safeAreaInsets\.bottom|paddingBottom\(.*safeAreaInsets\.bottom' \
  README.md docs/api/usage.md docs/api/chat-ui.md; then
  fail "接入文档仍要求把 safeAreaInsets.bottom 传给底部输入区。" "Demo 与组件默认值均为 0f；移除该参数。"
fi

PUBLIC_BUILD_FILES=(
  settings.gradle.kts
  settings.ohos.gradle.kts
  gradle/publish-maven.gradle
  kuiklychat-compose/build.gradle.kts
  kuiklychat-compose/build.ohos.gradle.kts
  kuiklychat-compat/build.gradle.kts
  kuiklychat-compat/build.ohos.gradle.kts
)
if grep -nE 'internal-2\.|KuiklyMarkdownCompose:.*internal' "${PUBLIC_BUILD_FILES[@]}"; then
  fail "公开模块仍依赖内网 Markdown 版本。" "Compose 使用 markdown:0.4.1 / 0.4.1-ohos；DSL 使用 KuiklyMarkdown:1.0.6-2.1.21 或其 ohos 对应版本。"
fi

HOST_REPO_FILES=(
  settings.gradle.kts
  settings.ohos.gradle.kts
  README.md
  docs/api/host-integration.md
  docs/api/usage.md
  docs/api/chat-ui.md
)
if grep -nF 'https://mirrors.tencent.com/repository/maven/kuikly/' "${HOST_REPO_FILES[@]}"; then
  fail "公开构建或接入文档仍引用内网 Maven 仓库。" "使用 kuikly-open 与 maven-tencent，并移除 repository/maven/kuikly 地址。"
fi

if grep -nE 'uri\("https://mirrors\.tencent\.com/repository/maven/kuikly-open/"\)|maven\("https://mirrors\.tencent\.com/repository/maven/kuikly-open/"\)' "${HOST_REPO_FILES[@]}"; then
  fail "宿主依赖解析仍引用 kuikly-open 上传入口。" "上传入口只用于发布配置；宿主 repositories 使用 nexus/repository/maven-tencent。"
fi

if grep -nE '0\.[0-9]+\.[0-9]+-experimental|0\.x experimental|0\.1\.0|1\.0\.x-2\.' "${DOCS[@]}"; then
  fail "发布文档仍引用过期或占位的版本坐标。" "统一替换为当前基础版本 $BASE_VERSION。"
fi

for doc in "${DOCS[@]}"; do
  if grep -qE '[0-9]+\.[0-9]+\.[^[:space:]`"'"'"']+-2\.1\.21' "$doc"; then
    if ! grep -qF "$EXPECTED_STANDARD" "$doc"; then
      fail "$doc 的标准平台坐标与 gradle.properties 不一致。" "使用 $EXPECTED_STANDARD。"
    fi
  fi
done

for doc in README.md docs/api/chat-ui.md docs/api/host-integration.md docs/api/usage.md; do
  if ! grep -qF "$EXPECTED_OHOS" "$doc"; then
    fail "$doc 缺少鸿蒙坐标。" "使用 $EXPECTED_OHOS。"
  fi
done

if ! grep -qF 'https://mirrors.tencent.com/nexus/repository/maven-tencent/' docs/api/host-integration.md; then
  fail "接入文档缺少 Nexus 宿主解析仓库地址。" "加入 nexus/repository/maven-tencent 仓库地址。"
fi

if ! grep -qF 'https://mirrors.tencent.com/nexus/repository/maven-tencent/' README.md; then
  fail "README 缺少 Nexus 宿主解析仓库地址。" "加入 nexus/repository/maven-tencent 仓库地址。"
fi

if ! grep -qF 'exclude(group = "org.jetbrains", module = "markdown")' docs/api/usage.md; then
  fail "使用文档缺少 Android Markdown parser 去重配置。" "Compose 与 DSL 同时接入时排除 org.jetbrains:markdown。"
fi

if grep -nE 'implementation\("com\.tencent\.kuiklybase:(markdown|KuiklyMarkdown)' \
  README.md docs/api/usage.md docs/api/host-integration.md docs/api/chat-ui.md; then
  fail "宿主接入文档把 ChatUI 内部 Markdown 依赖写成了宿主依赖。" "只保留 kuiklychat-* 模块依赖；Markdown 组件由 POM / Gradle module 自动传递。"
fi

if grep -nE 'kuiklychat-agui|kuiklychat-agentcard|com\.tencent\.kuiklybase:KuiklyChat[`:]|:KuiklyChat`' README.md docs/api/chat-ui.md docs/api/host-integration.md docs/api/public-keep-set.md; then
  fail "发布文档仍列出当前未发布的模块。" "只保留 settings.gradle.kts / publish-maven.sh 中实际发布的模块。"
fi

if [ "$ERRORS" -gt 0 ]; then
  echo "发现 $ERRORS 个 API 文档一致性问题，请修复后重试。"
  exit 1
fi

echo "✅ API 文档版本与模块坐标检查通过"
