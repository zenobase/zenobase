#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

OS_DIR="$SCRIPT_DIR/opensearch"
OS_CURRENT_HASH=$(find "$OS_DIR" -type f -exec md5 -q {} + 2>/dev/null | sort | md5 -q)
OS_EXISTING_HASH=$(docker inspect --format='{{index .Config.Labels "build.hash"}}' zenobase-opensearch 2>/dev/null || echo "")
if [ "$OS_CURRENT_HASH" = "$OS_EXISTING_HASH" ]; then
  echo "==> OpenSearch image is up to date"
else
  echo "==> Building OpenSearch image..."
  docker build --label "build.hash=$OS_CURRENT_HASH" -t zenobase-opensearch "$OS_DIR"
fi

echo "==> Building Play image..."
cd "$REPO_DIR"
./sbt universal:packageZipTarball
docker build -t zenobase-play -f "$SCRIPT_DIR/play/Dockerfile" "$REPO_DIR"

echo "==> Done. Run with: docker compose -f docker/docker-compose.local.yml up -d"
