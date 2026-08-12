FROM docker.io/library/maven:3-eclipse-temurin-8 AS builder

WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    set -eux; \
    find /root/.m2/repository -type f -size 0 -delete; \
    xml_api_jar=/root/.m2/repository/xml-apis/xml-apis-ext/1.3.04/xml-apis-ext-1.3.04.jar; \
    install -d "$(dirname "$xml_api_jar")"; \
    curl --fail --location --retry 3 \
      --output "$xml_api_jar.download" \
      https://repo.maven.apache.org/maven2/xml-apis/xml-apis-ext/1.3.04/xml-apis-ext-1.3.04.jar; \
    echo "d0b4887dc34d57de49074a58affad439a013d0baffa1a8034f8ef2a5ea191646  $xml_api_jar.download" \
      | sha256sum --check -; \
    mv "$xml_api_jar.download" "$xml_api_jar"; \
    mvn clean package -DskipTests; \
    # Copy WARs to root for multi-stage build pickup
    # Use glob to handle version-suffixed filenames
    cp web/target/LibreClinica-web*.war /LibreClinica-web.war; \
    cp ws/target/LibreClinica-ws*.war /LibreClinica-ws.war;

############################################################
FROM tomcat:9-jdk11

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends wget; \
    rm -rf /var/lib/apt/lists/*; \
    # set up redirection to application when accessing tomcat root
    mkdir /usr/local/tomcat/webapps/ROOT; \
    echo '<html><head><meta http-equiv="refresh" content="0; URL=libreclinica/" /></head></html>' \
        > /usr/local/tomcat/webapps/ROOT/index.html;

# set up volumes for data and logs
VOLUME \
    /usr/local/tomcat/libreclinica.data \
    /usr/local/tomcat/logs

# add config files
COPY \
    /docker/config/ \
    /usr/local/tomcat/libreclinica.config/

COPY docker/entrypoint.sh /usr/local/bin/libreclinica-entrypoint
RUN sed -i 's/\r$//' /usr/local/bin/libreclinica-entrypoint \
    && chmod +x /usr/local/bin/libreclinica-entrypoint

# add LibreClinica web application WAR (context path: /libreclinica)
COPY --from=builder \
    /LibreClinica-web.war \
    /usr/local/tomcat/webapps/libreclinica.war

# add LibreClinica SOAP web services WAR (context path: /libreclinica-ws)
# This provides SOAP endpoints at /libreclinica-ws/ws/{service}/v1
COPY --from=builder \
    /LibreClinica-ws.war \
    /usr/local/tomcat/webapps/libreclinica-ws.war

ENTRYPOINT ["/usr/local/bin/libreclinica-entrypoint"]
CMD ["catalina.sh", "run"]
