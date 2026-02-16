FROM maven:3.9-eclipse-temurin-25 AS builder
LABEL authors="abiola"

WORKDIR /build

COPY .mvn . ./
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:25-jre
LABEL authors="abiola"

WORKDIR /app

RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=builder /build/target/opentelemetry-backend-0.1.1-SNAPSHOT.jar app.jar
COPY opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-javaagent:/app/opentelemetry-javaagent.jar", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
