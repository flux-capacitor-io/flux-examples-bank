FROM adoptopenjdk/openjdk14:alpine-jre

COPY target/app.jar /

RUN addgroup -S java && adduser -D -S -s /sbin/nologin -G java java
USER java

CMD java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -DdevMode=true -Dendpoint.messaging=${MESSAGING_ENDPOINT} -jar /app.jar
