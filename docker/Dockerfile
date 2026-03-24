FROM eclipse-temurin:25-jre

RUN groupadd -r app && useradd -r -g app app

COPY target/zenobase.jar /app/zenobase.jar
COPY target/libs/ /app/libs/
COPY docker/config/logback.xml /etc/app/logback.xml

RUN mkdir -p /etc/app && chown -R app:app /etc/app

COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

WORKDIR /app
USER app

ENTRYPOINT ["/app/entrypoint.sh"]

EXPOSE 9000

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -f http://localhost:9000/status || exit 1
