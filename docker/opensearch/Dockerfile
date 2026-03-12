FROM opensearchproject/opensearch:3.1.0

RUN opensearch-plugin remove --purge opensearch-neural-search && \
    opensearch-plugin remove --purge opensearch-security-analytics && \
    opensearch-plugin remove --purge opensearch-alerting && \
    opensearch-plugin remove --purge opensearch-anomaly-detection && \
    opensearch-plugin remove --purge opensearch-asynchronous-search && \
    opensearch-plugin remove --purge opensearch-cross-cluster-replication && \
    opensearch-plugin remove --purge opensearch-custom-codecs && \
    opensearch-plugin remove --purge opensearch-flow-framework && \
    opensearch-plugin remove --purge opensearch-geospatial && \
    opensearch-plugin remove --purge opensearch-index-management && \
    opensearch-plugin remove --purge opensearch-knn && \
    opensearch-plugin remove --purge opensearch-ltr && \
    opensearch-plugin remove --purge opensearch-skills && \
    opensearch-plugin remove --purge opensearch-ml && \
    opensearch-plugin remove --purge opensearch-notifications && \
    opensearch-plugin remove --purge opensearch-notifications-core && \
    opensearch-plugin remove --purge opensearch-observability && \
    opensearch-plugin remove --purge opensearch-performance-analyzer && \
    opensearch-plugin remove --purge opensearch-reports-scheduler && \
    opensearch-plugin remove --purge opensearch-search-relevance && \
    opensearch-plugin remove --purge opensearch-security && \
    opensearch-plugin remove --purge opensearch-sql && \
    opensearch-plugin remove --purge opensearch-system-templates && \
    opensearch-plugin remove --purge opensearch-ubi && \
    opensearch-plugin remove --purge query-insights

RUN opensearch-plugin install --batch repository-s3
RUN opensearch-plugin install --batch discovery-ec2

EXPOSE 9200

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9200/_cluster/health || exit 1