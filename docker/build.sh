#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

ES_DIR="$SCRIPT_DIR/elasticsearch"
ES_CURRENT_HASH=$(find "$ES_DIR" -type f -exec md5 -q {} + 2>/dev/null | sort | md5 -q)
ES_EXISTING_HASH=$(docker inspect --format='{{index .Config.Labels "build.hash"}}' zenobase-elasticsearch 2>/dev/null || echo "")
if [ "$ES_CURRENT_HASH" = "$ES_EXISTING_HASH" ]; then
  echo "==> Elasticsearch image is up to date"
else
  echo "==> Building Elasticsearch image..."
  docker build --label "build.hash=$ES_CURRENT_HASH" -t zenobase-elasticsearch "$ES_DIR"
fi

echo "==> Building Play image..."
cd "$REPO_DIR"
./sbt universal:packageZipTarball
docker build -t zenobase-play -f "$SCRIPT_DIR/play/Dockerfile" "$REPO_DIR"

echo "==> Done. Run with: docker compose -f docker/docker-compose.local.yml up -d"
