FROM opensearchproject/opensearch:2.19.4

ENV compatibility.override_main_response_version=true

RUN opensearch-plugin install --batch repository-s3

EXPOSE 9200

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9200/_cluster/health || exit 1
