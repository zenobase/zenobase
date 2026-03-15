FROM eclipse-temurin:8-jre

RUN groupadd -r play && useradd -r -g play play

COPY target/universal/zenobase-SNAPSHOT.tgz /tmp/
RUN mkdir -p /var/play && \
    tar xzf /tmp/zenobase-SNAPSHOT.tgz -C /var/play --strip-components=1 && \
    rm /tmp/zenobase-SNAPSHOT.tgz && \
    chown -R play:play /var/play

RUN mkdir -p /etc/play && chown -R play:play /etc/play

COPY docker/play/config/logback-play.xml /etc/play/logback.xml
COPY docker/play/config/enableLegacyTLS.security /etc/play/enableLegacyTLS.security

COPY docker/play/entrypoint.sh /var/play/entrypoint.sh
RUN chmod +x /var/play/entrypoint.sh

WORKDIR /var/play
USER play

ENTRYPOINT ["/var/play/entrypoint.sh"]

EXPOSE 9000

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:9000/status || exit 1
