#!/bin/sh
set -e

# Write prod.conf from ECS-injected secret env var
if [ -n "$PROD_CONF" ]; then
    echo "$PROD_CONF" > /etc/play/prod.conf
fi

exec /var/play/bin/zenobase \
    -Duser.timezone=UTC \
    -Dconfig.file=/etc/play/prod.conf \
    -Dlogger.file=/etc/play/logback.xml \
    -Djava.security.properties=/etc/play/enableLegacyTLS.security \
    -Djdk.tls.client.protocols=TLSv1.2 \
    -J-XX:+UseContainerSupport \
    -J-XX:MaxRAMPercentage=75.0
