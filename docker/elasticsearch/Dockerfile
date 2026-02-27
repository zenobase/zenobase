FROM eclipse-temurin:8-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN groupadd -r elasticsearch && useradd -r -g elasticsearch elasticsearch

# Install ES 1.7.6 from deb (copied from zenobase-cookbooks)
COPY files/elasticsearch-1.7.6.deb /tmp/
RUN dpkg -i /tmp/elasticsearch-1.7.6.deb && rm /tmp/elasticsearch-1.7.6.deb

# Remove log4j (replaced by logback)
RUN rm -f /usr/share/elasticsearch/lib/log4j-1.2.17.jar \
          /usr/share/elasticsearch/lib/apache-log4j-extras-1.2.17.jar

# Add replacement libs: logback, jackson, slf4j (from zenobase-cookbooks)
COPY files/lib/ /usr/share/elasticsearch/lib/

# Add plugins: cloud-aws, geocluster-facet, decimal-histogram-facet (from zenobase-cookbooks)
COPY files/plugins/ /usr/share/elasticsearch/plugins/

RUN mkdir -p /var/lib/elasticsearch /var/log/elasticsearch && \
    chown -R elasticsearch:elasticsearch /var/lib/elasticsearch /var/log/elasticsearch /etc/elasticsearch

WORKDIR /usr/share/elasticsearch
USER elasticsearch

ENV ES_HEAP_SIZE=4g
ENV ES_CLUSTER_NAME=elasticsearch
ENV ES_DISCOVERY_TYPE=zen
ENV AWS_ACCESS_KEY=""
ENV AWS_SECRET_KEY=""
ENV AWS_REGION=us-east-1

CMD ["bin/elasticsearch", \
    "-Des.default.path.home=/usr/share/elasticsearch", \
    "-Des.default.path.data=/var/lib/elasticsearch", \
    "-Des.default.path.logs=/var/log/elasticsearch", \
    "-Des.default.path.conf=/etc/elasticsearch", \
    "-Dlogback.configurationFile=/etc/elasticsearch/logback.xml", \
    "-Duser.timezone=UTC", \
    "-Djdk.tls.client.protocols=TLSv1.2"]

EXPOSE 9200 9300

VOLUME /var/lib/elasticsearch

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9200/_cluster/health || exit 1
