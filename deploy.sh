#!/bin/sh -e
cd "$(dirname "$0")/infra"
exec pulumi up \
    --stack prod \
    --policy-pack ./policy \
    --policy-pack-config ./policy/config.json \
    "$@"
