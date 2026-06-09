#!/bin/sh -e
cd "$(dirname "$0")/infra"
pnpm install --frozen-lockfile
pnpm --dir policy install --frozen-lockfile
exec pulumi up \
    --stack prod \
    --policy-pack ./policy \
    --policy-pack-config ./policy/config.json \
    "$@"
