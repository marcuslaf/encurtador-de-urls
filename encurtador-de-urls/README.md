# URL Shortener / Encurtador de URLs

[![CI](https://github.com/marcuslaf/encurtador-de-urls/actions/workflows/ci.yml/badge.svg)](https://github.com/marcuslaf/encurtador-de-urls/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-25-blue)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

Production-grade URL shortener with PostgreSQL persistence, Redis cache, distributed rate limiting, API key authentication, QR code generation, access metrics, Flyway migrations, and Testcontainers integration tests.

## Stack

| Layer       | Technology                                                    |
|-------------|---------------------------------------------------------------|
| Runtime     | Java 25 (Eclipse Temurin 25) with Virtual Threads             |
| Framework   | Spring Boot 3.5.6, Spring Web, Spring Data JPA, Spring Security |
| Database    | PostgreSQL 15 (Flyway migrations)                             |
| Cache       | Redis 7 (`spring-boot-starter-data-redis`)                    |
| Rate Limit  | Bucket4j 8.10.x (distributed via Redis)                      |
| QR Code     | ZXing 3.5.3                                                   |
| API Docs    | Springdoc OpenAPI (`/swagger-ui.html`)                        |
| Validation  | Bean Validation (Jakarta)                                     |
| Tests       | JUnit 5, Mockito, Testcontainers (Postgres + Redis)           |
| Build       | Maven 3.9+                                                    |
| Containers  | Docker multi-stage + docker-compose                           |
| CI/CD       | GitHub Actions                                                 |

## Requirements

- **Java 25+**
- **Maven 3.9+**
- **Docker Desktop** (for integration tests and docker-compose)

## API Endpoints

| Method | Path                              | Description                                         | Auth     |
|--------|-----------------------------------|-----------------------------------------------------|----------|
| POST   | `/api/urls`                       | Create short URL (rate-limited 10/min/IP)           | API Key  |
| GET    | `/api/urls`                       | List all active URLs (paginated)                    | API Key  |
| GET    | `/api/urls/{shortCode}/stats`     | Access statistics (total + per day)                 | API Key  |
| GET    | `/api/urls/{shortCode}/qr`        | QR code image (PNG)                                 | API Key  |
| DELETE | `/api/urls/{shortCode}`           | Soft-delete a short URL                             | API Key  |
| GET    | `/{shortCode}`                    | HTTP 302 redirect to original URL                   | Public   |
| GET    | `/swagger-ui.html`                | Swagger/OpenAPI UI                                  | Public   |
| GET    | `/actuator/health`                | Health probe                                        | Public   |

## Quick Start

### Docker Compose (Recommended)

```bash
docker-compose up --build
```

Services:
- PostgreSQL on port `5432`
- Redis on port `6379`
- Application on port `8080`

### Local Development

```bash
# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

### Environment Variables

| Variable               | Default                    | Description                     |
|------------------------|----------------------------|---------------------------------|
| `SPRING_PROFILES_ACTIVE` | `default`                | Spring profile                  |
| `POSTGRES_HOST`        | `localhost`                | PostgreSQL host                 |
| `POSTGRES_USER`        | `postgres`                 | PostgreSQL username             |
| `POSTGRES_PASSWORD`    | `postgres`                 | PostgreSQL password             |
| `REDIS_HOST`           | `localhost`                | Redis host                      |
| `REDIS_HOST_PORT`      | `6379`                     | Redis port                      |
| `APP_BASE_URL`         | `http://localhost:8080`    | Base URL for short links        |
| `APP_API_KEY`          | (empty = no auth)          | API key for protected endpoints |

## Usage Examples

### Create Short URL

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{"originalUrl":"https://example.com/long/path","expirationMinutes":1440}'
```

Response:
```json
{
  "shortCode": "abc1234",
  "shortUrl": "http://localhost:8080/abc1234",
  "originalUrl": "https://example.com/long/path",
  "createdAt": "2026-07-15T12:00:00Z",
  "expiresAt": "2026-07-16T12:00:00Z"
}
```

### Create with Custom Alias

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{"originalUrl":"https://example.com","customAlias":"my-link"}'
```

### Get QR Code

```bash
curl http://localhost:8080/api/urls/abc1234/qr --output qr.png
```

### List URLs (Paginated)

```bash
curl "http://localhost:8080/api/urls?page=0&size=20" \
  -H "X-API-Key: your-api-key"
```

### Get Statistics

```bash
curl http://localhost:8080/api/urls/abc1234/stats \
  -H "X-API-Key: your-api-key"
```

### Delete URL

```bash
curl -X DELETE http://localhost:8080/api/urls/abc1234 \
  -H "X-API-Key: your-api-key"
```

## Running Tests

### Unit Tests (no Docker required)

```bash
mvn test -Dtest=ShortCodeGeneratorTest,CreateUrlRequestTest,UrlServiceTest,CacheServiceTest,GlobalExceptionHandlerTest
```

### Integration Tests (requires Docker)

```bash
mvn test
```

Integration tests use Testcontainers for both PostgreSQL and Redis -- no local installation needed.

## Architecture

```
com.example.urlshortener
├── config        # SecurityConfig, RedisConfig, OpenApiConfig, RateLimitFilter
├── controller    # UrlController (REST CRUD + QR) + RedirectionController (302)
├── dto           # CreateUrlRequest/Response, UrlStatsResponse, ErrorResponse
├── entity        # Url, AccessLog (no Lombok)
├── exception     # GlobalExceptionHandler + custom exceptions
├── repository    # UrlRepository, AccessLogRepository (JPQL + native upsert)
├── scheduler     # ExpirationScheduler (@Scheduled fixedDelay=1h)
├── service       # UrlService, CacheService
├── util          # ShortCodeGenerator (SecureRandom, 7 alphanumeric chars)
└── UrlShortenerApplication
```

## Design Decisions

### Distributed Rate Limiting
- Bucket4j with Redis-backed `ProxyManager` for multi-instance consistency.
- 10 tokens per minute, refills 1 token every 6 seconds.
- Returns `429 Too Many Requests` with `Retry-After` header.
- Only enforced on `POST /api/urls`.

### Atomic Access Counting
- PostgreSQL `INSERT ... ON CONFLICT DO UPDATE` (upsert) prevents lost increments under concurrency.
- `registerAccess()` uses 2 queries (upsert + counter update) instead of 3.

### API Key Authentication
- `X-API-Key` header authentication via `ApiKeyAuthFilter`.
- Stateless sessions (no cookies).
- Swagger UI, actuator, and redirect endpoints remain public.
- Set `APP_API_KEY` env var to enable; if empty, all requests pass through.

### Cache Strategy
- `CacheService` stores `redirect:{shortCode}` -> `originalUrl` with dynamic TTL.
- Cache miss falls back to PostgreSQL and warms cache before redirecting.
- Expired URLs are evicted on access and by hourly scheduler sweep.

### QR Code Generation
- `GET /api/urls/{shortCode}/qr` returns a PNG QR code via ZXing.
- 24-hour cache header for client-side caching.

### Automatic Expiration
- Hourly `ExpirationScheduler` uses bulk JPQL update (no in-memory loading).
- Expired/inactive URLs return 404.

### Virtual Threads
- `spring.threads.virtual.enabled=true` -- Project Loom GA on JDK 25.
- Ideal for blocking Redis/JDBC calls without exhausting platform threads.

## File Structure

```
├── .github/workflows/     # CI/CD (GitHub Actions)
├── docker-compose.yml     # PostgreSQL + Redis + App
├── Dockerfile             # Multi-stage build
├── LICENSE                # MIT
├── pom.xml                # Maven dependencies
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/urlshortener/
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── repository/
    │   │   ├── scheduler/
    │   │   ├── service/
    │   │   └── util/
    │   └── resources/
    │       ├── application.properties
    │       ├── application-prod.properties
    │       └── db/migration/V1__init.sql
    └── test/
        └── java/com/example/urlshortener/
            ├── config/RateLimitFilterTest.java
            ├── dto/CreateUrlRequestTest.java
            ├── exception/GlobalExceptionHandlerTest.java
            ├── service/CacheServiceTest.java
            ├── service/UrlServiceTest.java
            ├── util/ShortCodeGeneratorTest.java
            ├── UrlIntegrationTest.java
            ├── RateLimitIntegrationTest.java
            ├── scheduler/ExpirationSchedulerIntegrationTest.java
            └── testsupport/IntegrationTestBase.java
```

## Test Coverage

| Test Class                      | Tests | Status |
|---------------------------------|-------|--------|
| ShortCodeGeneratorTest          | 6     | Pass   |
| CreateUrlRequestTest            | 8     | Pass   |
| UrlServiceTest                  | 8     | Pass   |
| CacheServiceTest                | 4     | Pass   |
| GlobalExceptionHandlerTest      | 5     | Pass   |
| **Unit Tests Total**            | **31**| **31 Pass** |

Integration tests require Docker Desktop (Testcontainers spins up PostgreSQL + Redis).

## License

MIT
