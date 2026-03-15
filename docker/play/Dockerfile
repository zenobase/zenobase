FROM eclipse-temurin:8-jre

RUN groupadd -r play && useradd -r -g play play

COPY target/universal/zenobase-SNAPSHOT.tgz /tmp/
RUN mkdir -p /var/play && \
    tar xzf /tmp/zenobase-SNAPSHOT.tgz -C /var/play --strip-components=1 && \
    rm /tmp/zenobase-SNAPSHOT.tgz && \
    chown -R play:play /var/play

RUN mkdir -p /etc/play && chown -R play:play /etc/play

WORKDIR /var/play
USER play

ENTRYPOINT ["/bin/sh", "-c", \
    "exec /var/play/bin/zenobase \
    -Duser.timezone=UTC \
    -Dconfig.file=/etc/play/prod.conf \
    -Dlogger.file=/etc/play/logback.xml \
    -Djava.security.properties=/etc/play/enableLegacyTLS.security \
    -Djdk.tls.client.protocols=TLSv1.2 \
    -J-XX:+UseContainerSupport \
    -J-XX:MaxRAMPercentage=75.0"]

EXPOSE 9000

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:9000/status || exit 1
