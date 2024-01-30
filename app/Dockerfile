FROM eclipse-temurin:21-jre

RUN addgroup --system javauser && adduser --system --shell /bin/false --ingroup javauser javauser
COPY --chown=javauser:javauser target/app.jar app.jar
USER javauser

CMD ["java", "-XX:MaxRAMPercentage=75", "-XX:SoftRefLRUPolicyMSPerMB=2500", "-jar", "app.jar"]
