FROM adoptopenjdk/openjdk16:x86_64-alpine-jre-16.0.1_9

COPY target/app.jar /

RUN addgroup -S java && adduser -D -S -s /sbin/nologin -G java java
USER java

CMD java -DdevMode=true -Dendpoint.messaging=${MESSAGING_ENDPOINT} -jar /app.jar
