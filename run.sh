#!/bin/bash
set -euo pipefail

./sbt universal:packageZipTarball
docker build -t zenobase-play -f docker/play/Dockerfile .
docker compose up -d
