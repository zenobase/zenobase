# Stage 1: Build
FROM eclipse-temurin:8-jdk AS builder

ENV SBT_VERSION=0.13.18

RUN curl -Ls https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz | tar xvz -C /usr/share/ && \
    ln -s /usr/share/sbt/bin/sbt /usr/local/bin/sbt

WORKDIR /build
COPY project/build.properties project/plugins.sbt project/
RUN sbt about

COPY build.sbt .
RUN sbt update

COPY . .
RUN sbt universal:package-zip-tarball

# Stage 2: Runtime
FROM eclipse-temurin:8-jre

RUN groupadd -r play && useradd -r -g play play

COPY --from=builder /build/target/universal/zenobase-SNAPSHOT.tgz /tmp/
RUN mkdir -p /var/play && \
    tar xzf /tmp/zenobase-SNAPSHOT.tgz -C /var/play --strip-components=1 && \
    rm /tmp/zenobase-SNAPSHOT.tgz && \
    chown -R play:play /var/play

RUN mkdir -p /etc/play && chown -R play:play /etc/play

WORKDIR /var/play
USER play

ENV JAVA_HEAP=2g

ENTRYPOINT ["/bin/sh", "-c", \
    "exec /var/play/bin/zenobase \
    -Duser.timezone=UTC \
    -Dconfig.file=/etc/play/prod.conf \
    -Dlogger.file=/etc/play/logback.xml \
    -Dhazelcast.config=/etc/play/hazelcast.xml \
    -Djava.security.properties=/etc/play/enableLegacyTLS.security \
    -Djdk.tls.client.protocols=TLSv1.2 \
    -J-Xms${JAVA_HEAP} \
    -J-Xmx${JAVA_HEAP}"]

EXPOSE 9000

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:9000/status || exit 1
