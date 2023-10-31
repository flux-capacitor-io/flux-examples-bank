FROM eclipse-temurin:20-jre

RUN addgroup --system javauser && adduser --system --shell /bin/false --ingroup javauser javauser
COPY --chown=javauser:javauser target/auditlog.jar auditlog.jar
USER javauser

CMD ["java", "-XX:MaxRAMPercentage=75", "-XX:SoftRefLRUPolicyMSPerMB=2500", "-jar", "auditlog.jar"]
