FROM eclipse-temurin:25.0.3_9-jre-alpine-3.23

RUN addgroup -S app && adduser -S app -G app

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
