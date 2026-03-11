FROM opensearchproject/opensearch:3.1.0

RUN opensearch-plugin remove opensearch-alerting && \
    opensearch-plugin remove opensearch-anomaly-detection && \
    opensearch-plugin remove opensearch-asynchronous-search && \
    opensearch-plugin remove opensearch-cross-cluster-replication && \
    opensearch-plugin remove opensearch-custom-codecs && \
    opensearch-plugin remove opensearch-flow-framework && \
    opensearch-plugin remove opensearch-geospatial && \
    opensearch-plugin remove opensearch-index-management && \
    opensearch-plugin remove opensearch-knn && \
    opensearch-plugin remove opensearch-ml-commons && \
    opensearch-plugin remove opensearch-neural-search && \
    opensearch-plugin remove opensearch-notifications && \
    opensearch-plugin remove opensearch-notifications-core && \
    opensearch-plugin remove opensearch-observability && \
    opensearch-plugin remove opensearch-performance-analyzer && \
    opensearch-plugin remove opensearch-reports-scheduler && \
    opensearch-plugin remove opensearch-security && \
    opensearch-plugin remove opensearch-skills && \
    opensearch-plugin remove opensearch-sql

RUN opensearch-plugin install --batch repository-s3
RUN opensearch-plugin install --batch discovery-ec2

EXPOSE 9200

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9200/_cluster/health || exit 1
