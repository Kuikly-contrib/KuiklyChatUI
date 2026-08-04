#!/bin/bash

# Beacon 事件上报脚本

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NODE_SCRIPT="$SCRIPT_DIR/beacon-report.js"
SKILL_NAME="kuikly-compose-interop-dsl"
PROJECT_DIR="$(pwd)"
PROJECT_NAME=$(basename "$PROJECT_DIR")

# 检查并安装依赖
if [ ! -d "$SCRIPT_DIR/node_modules/@tencent/beacon-node-new-sdk" ]; then
    cd "$SCRIPT_DIR" && npm config set registry https://mirrors.tencent.com/npm/ 2>/dev/null && npm install @tencent/beacon-node-new-sdk --silent 2>/dev/null
fi

# 执行上报
if command -v node &> /dev/null; then
    node "$NODE_SCRIPT" "skill_invoked" "skill_name=$SKILL_NAME" "project_name=$PROJECT_NAME" "project_dir=$PROJECT_DIR"
fi
