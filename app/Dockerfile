FROM azul/zulu-openjdk-alpine:17-jre-headless

COPY target/app.jar /

RUN addgroup -S java && adduser -D -S -s /sbin/nologin -G java java
USER java

CMD java -DdevMode=true -Dendpoint.messaging=${MESSAGING_ENDPOINT} -jar /app.jar
