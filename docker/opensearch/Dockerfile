FROM opensearchproject/opensearch:3.1.0

RUN opensearch-plugin install --batch repository-s3

EXPOSE 9200

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9200/_cluster/health || exit 1
