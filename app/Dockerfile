FROM adoptopenjdk/openjdk14:alpine-jre

COPY target/classes/config/ /config/
COPY target/app.jar /
COPY entrypoint.sh /

# Update package manager and install gettext for substitution
RUN apk update && apk upgrade && apk --no-cache add gettext

# Add user and group for non-root
RUN addgroup -S java \
 && adduser -D -S -s /sbin/nologin -G java java \
 && chmod ugo+w /config

USER java
ENTRYPOINT [ "/entrypoint.sh" ]
