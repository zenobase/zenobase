#!/usr/bin/env bash

set -euo pipefail

SBT_VERSION="$(sed -n 's/^sbt.version=//p' project/build.properties)"
SBT_DIR=".sbt-launch"
SBT_BIN="$SBT_DIR/sbt/bin/sbt"

if [ ! -f "$SBT_BIN" ]; then
    echo "Downloading sbt $SBT_VERSION..."
    mkdir -p "$SBT_DIR"
    curl -Ls "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" \
        | tar xz -C "$SBT_DIR"
fi

exec "$SBT_BIN" ${SBT_OPTS:-} "$@"