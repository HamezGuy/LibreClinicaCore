FROM docker.io/library/maven:3-eclipse-temurin-8 AS builder

WORKDIR /app

COPY . .

RUN \
    # cache downloaded dependencies
    --mount=type=cache,target=/root/.m2 \
    # build cache
    --mount=type=cache,target=/app/core/target \
    --mount=type=cache,target=/app/docs/target \
    --mount=type=cache,target=/app/odm/target \
    --mount=type=cache,target=/app/web/target \
    --mount=type=cache,target=/app/ws/target \

    set -eux; \
    mvn clean package -DskipTests; \
    # Copy WARs to root for multi-stage build pickup
    # Use glob to handle version-suffixed filenames
    cp web/target/LibreClinica-web*.war /LibreClinica-web.war; \
    cp ws/target/LibreClinica-ws*.war /LibreClinica-ws.war;

############################################################
FROM tomcat:9-jdk11

RUN set -eux; \
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

# add LibreClinica web application WAR (context path: /libreclinica)
COPY --from=builder \
    /LibreClinica-web.war \
    /usr/local/tomcat/webapps/libreclinica.war

# add LibreClinica SOAP web services WAR (context path: /libreclinica-ws)
# This provides SOAP endpoints at /libreclinica-ws/ws/{service}/v1
COPY --from=builder \
    /LibreClinica-ws.war \
    /usr/local/tomcat/webapps/libreclinica-ws.war
