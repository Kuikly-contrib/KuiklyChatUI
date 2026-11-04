#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${ROOT_DIR}/kuiklychat-compose/src/commonMain/assets/common"
DEST="${ROOT_DIR}/KuiklyChatIOS/Resources/common"

mkdir -p "${DEST}"
rsync -a --delete "${SRC}/" "${DEST}/"
echo "Synced KuiklyChat SVG assets to ${DEST}"
