# The CD workflow builds the jar first (./gradlew bootJar) and copies it to app.jar.
FROM eclipse-temurin:17-jre

ENV TZ=Asia/Seoul
WORKDIR /app

COPY app.jar /app/app.jar

USER 1000:1000
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
