FROM eclipse-temurin:25-jdk
LABEL authors="abiola"

COPY target/opentelemetry-backend-0.0.1-SNAPSHOT.jar /app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
