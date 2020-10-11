#!/bin/sh

set -e

envsubst </config/application.properties.tmpl >/config/application.properties

exec java \
      -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
      -XX:MaxRAMPercentage=75.0 \
      -Djava.security.egd=file:/dev/./urandom \
      -Dconfig.dir=/config/ \
      -jar /app.jar

