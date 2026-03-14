#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "==> Building Play image..."
cd "$REPO_DIR"
./sbt universal:packageZipTarball
docker build -t zenobase-play -f "$SCRIPT_DIR/play/Dockerfile" "$REPO_DIR"

echo "==> Done. Run with: docker compose -f docker/docker-compose.local.yml up -d"
