#!/usr/bin/env bash
# scripts/ci-gates.sh
# 本地与工蜂共用的发布前门禁。默认跑 layer + android；ios / ohos 需要对应 runner。
#
# 用法：
#   bash scripts/ci-gates.sh              # layer + android
#   bash scripts/ci-gates.sh layer
#   bash scripts/ci-gates.sh android
#   bash scripts/ci-gates.sh ios
#   bash scripts/ci-gates.sh ohos
#   bash scripts/ci-gates.sh all          # 四项全跑（本机有 iOS / Ohos 工具链时）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

GATE="${1:-default}"

run_layer() {
  echo "── Gate: architecture / file size / public keep-set ──"
  bash scripts/check-layer-deps.sh
}

run_android() {
  echo "── Gate: Android compile + unit tests ──"
  ./gradlew \
    :kuiklychat-core:compileDebugKotlinAndroid \
    :kuiklychat-compose:compileDebugKotlinAndroid \
    :kuiklychat-compat:compileDebugKotlinAndroid \
    :kuiklychat-ai:compileDebugKotlinAndroid \
    :kuiklychat-testing:compileDebugKotlinAndroid \
    :kuiklychat-core:compileDebugUnitTestKotlinAndroid \
    :kuiklychat-compose:compileDebugUnitTestKotlinAndroid \
    :kuiklychat-compat:compileDebugUnitTestKotlinAndroid \
    :kuiklychat-ai:compileDebugUnitTestKotlinAndroid
  ./gradlew \
    :kuiklychat-core:testDebugUnitTest \
    :kuiklychat-compose:testDebugUnitTest \
    :kuiklychat-compat:testDebugUnitTest \
    :kuiklychat-ai:testDebugUnitTest \
    :kuiklychat-testing:testDebugUnitTest
}

run_ios() {
  echo "── Gate: iosSimulatorArm64 compile ──"
  ./gradlew \
    :kuiklychat-core:compileKotlinIosSimulatorArm64 \
    :kuiklychat-compose:compileKotlinIosSimulatorArm64 \
    :kuiklychat-compat:compileKotlinIosSimulatorArm64 \
    :kuiklychat-ai:compileKotlinIosSimulatorArm64
}

run_ohos() {
  echo "── Gate: Ohos library compile (kuiklychat-core) ──"
  ./gradlew -c settings.ohos.gradle.kts :kuiklychat-core:compileKotlinOhosArm64
}

case "$GATE" in
  layer) run_layer ;;
  android) run_android ;;
  ios) run_ios ;;
  ohos) run_ohos ;;
  all)
    run_layer
    run_android
    run_ios
    run_ohos
    ;;
  default)
    run_layer
    run_android
    ;;
  *)
    echo "未知门禁: $GATE" >&2
    echo "用法: bash scripts/ci-gates.sh [layer|android|ios|ohos|all]" >&2
    exit 1
    ;;
esac

echo "✅ ci-gates ($GATE) passed"
