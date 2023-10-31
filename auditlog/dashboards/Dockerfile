FROM opensearchproject/opensearch-dashboards:latest

RUN opensearch-dashboards-plugin remove securityDashboards

COPY --chown=opensearch-dashboards:opensearch-dashboards opensearch_dashboards.yml /usr/share/opensearch-dashboards/config/
