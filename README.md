# OpenTelemetry Backend

Spring Boot backend service for prime-number lookup with pluggable cache backends and end-to-end observability (traces, metrics, logs) via OpenTelemetry. The functionality of the service has been intentionally kept minimal in order to keep the focus on the observability aspect. This service is intended to be just one component of an end-to-end deployment illustrating how traces generated in a frontend are propagated all the way to Prometheus, from which the resulting data is then exported to
another service focused on analysing time-series and creating alerts.

## Architecture

### Core Components
- `PrimesController` (`/api/primes/getPrime`) exposes the HTTP API.
- `PrimesService` validates input, handles cache read/write, computes primes on cache miss, and records service/cache metrics.
- `PrimeCache` abstraction with three backends:
  - `InMemoryPrimeCache`
  - `RedisPrimeCache`
  - `PostgresPrimeCache`
- `CacheConfig` resolves the configured backend and falls back to `MEMORY` if connectivity checks fail.
- `PrimeCacheHealthIndicator` and `PrimeCacheInfoContributor` expose backend state/fallback details through Actuator.
- `OpenTelemetryConfig` wires OTLP trace/metric/log exporters.

### Runtime Flow
1. `GET /api/primes/getPrime?position=N`
2. Validate `N > 0` (`InvalidNumberException` otherwise)
3. Lookup cache using key `position`
4. Cache hit: return cached prime
5. Cache miss: compute nth prime using `PrimeUtils`, write cache, return result
6. Emit telemetry:
   - Spans (`@WithSpan`)
   - Micrometer observations (`@Observed`)
   - Custom counters/histograms (`ServiceMetrics`, `CacheMetrics`)

### Observability Topology (Docker Compose)
- `primes-service` exports OTLP data to `otel-collector`.
- `otel-collector` exports:
  - Traces to Jaeger (`jaeger-v2`)
  - Metrics in Prometheus format (`:8889`) scraped by Prometheus
  - Logs to debug output

## Setup

### Prerequisites
- Java 25
- Docker + Docker Compose (for containerized deployment/observability stack)

### Build and Test
```bash
./mvnw clean test
./mvnw clean package
```

### Run Locally
```bash
./mvnw spring-boot:run
```

Service defaults:
- App: `http://localhost:8080`
- Prime API: `http://localhost:8080/api/primes/getPrime?position=10`
- Actuator health: `http://localhost:8080/actuator/health`
- Actuator info: `http://localhost:8080/actuator/info`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html` (or `/swagger-ui/index.html`)

### Cache Configuration
Set via `application.properties` or environment variables.

Supported backends:
- `MEMORY` (default)
- `REDIS`
- `POSTGRES`

Key properties:
- `prime.cache.backend`
- `prime.cache.redis.host`
- `prime.cache.redis.port`
- `prime.cache.postgres.host`
- `prime.cache.postgres.port`
- `prime.cache.postgres.username`
- `prime.cache.postgres.password`
- `prime.cache.postgres.database`

Example (environment variables):
```bash
export PRIME_CACHE_BACKEND=REDIS
export PRIME_CACHE_REDIS_HOST=localhost
export PRIME_CACHE_REDIS_PORT=6379
./mvnw spring-boot:run
```

If configured backend is unreachable, the service automatically falls back to `MEMORY` and exposes fallback metadata via Actuator health/info.

## Deployment

### Option 1: Docker Compose (App + OTEL Collector + Jaeger + Prometheus)
```bash
./mvnw clean package -DskipTests
docker compose up -d --build
```

Endpoints:
- App API: `http://localhost:8080/api/primes/getPrime?position=100`
- Jaeger UI: `http://localhost:16686`
- Prometheus UI: `http://localhost:9090`
- OTEL Collector metrics endpoint: `http://localhost:8889/metrics`

Stop stack:
```bash
docker compose down
```

### Option 2: Build and Run App Image Only
```bash
./mvnw clean package -DskipTests
docker build -t opentelemetry-backend:latest .
docker run --rm -p 8080:8080 opentelemetry-backend:latest
```

## Project Structure
- `src/main/java/ereh/won/otbackend` - API, service, telemetry config, exception handling
- `src/main/java/ereh/won/otbackend/cache` - cache abstractions/backends/metrics
- `src/main/java/ereh/won/otbackend/config` - cache backend selection and bean wiring
- `src/main/java/ereh/won/otbackend/observability` - Actuator health/info contributors
- `src/main/resources/application.properties` - app and telemetry defaults
- `docker-compose.yaml` - local deployment topology
- `otel-collector-config.yaml` - collector pipelines/exporters
- `prometheus.yml` - scrape configuration

## Notes
- Logging is configured in `src/main/resources/logback-spring.xml` with console + rolling file + OpenTelemetry appender.
- Integration tests include backend parity/contract coverage for memory, Redis, and PostgreSQL cache paths.
