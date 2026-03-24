#!/bin/sh
set -e

# Write application config from ECS-injected secret env var
if [ -n "$APPLICATION_CONF" ]; then
    echo "$APPLICATION_CONF" > /etc/app/prod.yaml
fi

exec java \
    -Dconfig.file=/etc/app/prod.yaml \
    -Duser.timezone=UTC \
    -Dlogback.configurationFile=/etc/app/logback.xml \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -jar /app/zenobase.jar
