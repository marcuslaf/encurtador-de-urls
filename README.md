<h1 align="center">🔗 URL Shortener</h1>
<h3 align="center">Encurtador de URLs com métricas, cache Redis e autenticação por API key</h3>

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis"/>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" alt="License"/>
  <img src="https://img.shields.io/badge/Tests-31%20%E2%9C%93-44CC11?style=for-the-badge" alt="Tests"/>
</p>

<p align="center">
  <a href="#-overview">Overview</a> •
  <a href="#-visão-geral">Visão Geral</a> •
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-api">API</a> •
  <a href="#-architecture">Architecture</a> •
  <a href="#-running-tests">Tests</a>
</p>

---

## 🇺🇸 Overview

Production-grade URL shortener built with **Java 25** and **Spring Boot 3.5**. Features distributed rate limiting via Redis, API key authentication, QR code generation, paginated URL listing, and a complete CI/CD pipeline with GitHub Actions.

### Features

| Feature | Description |
|---------|-------------|
| **Short URL Creation** | Create short URLs with optional custom aliases and configurable expiration |
| **QR Code Generation** | Generate QR code images for any short URL |
| **Distributed Rate Limiting** | Bucket4j + Redis for consistent limits across multiple instances |
| **API Key Authentication** | Secure endpoints with `X-API-Key` header (stateless) |
| **Access Metrics** | Track total accesses and daily breakdown per URL |
| **Soft Delete** | Deactivate URLs without hard-deleting from database |
| **Pagination** | List all active URLs with pagination support |
| **Auto Expiration** | Hourly scheduler marks expired URLs as inactive |
| **Virtual Threads** | Project Loom (JDK 25) for high-throughput request handling |
| **Docker Multi-Stage** | Optimized production image with HEALTHCHECK |
| **CI/CD** | GitHub Actions pipeline with Testcontainers for integration tests |

### Tech Stack

```
Java 25 · Spring Boot 3.5 · Spring Security · PostgreSQL 15 · Redis 7
Flyway · Bucket4j · ZXing · Testcontainers · Docker · GitHub Actions
```

---

## 🇧🇷 Visão Geral

Encurtador de URLs de produção construído com **Java 25** e **Spring Boot 3.5**. Possui rate limiting distribuído via Redis, autenticação por API key, geração de QR codes, listagem paginada de URLs e um pipeline completo de CI/CD com GitHub Actions.

### Funcionalidades

| Funcionalidade | Descrição |
|----------------|-----------|
| **Criação de URL Curta** | Crie URLs curtas com aliases customizados e expiração configurável |
| **Geração de QR Code** | Gere imagens de QR code para qualquer URL curta |
| **Rate Limiting Distribuído** | Bucket4j + Redis para limites consistentes entre múltiplas instâncias |
| **Autenticação por API Key** | Endpoints seguros com header `X-API-Key` (stateless) |
| **Métricas de Acesso** | Acompanhe acessos totais e breakdown diário por URL |
| **Delete Soft** | Desative URLs sem remover do banco de dados |
| **Paginação** | Liste todas as URLs ativas com suporte a paginação |
| **Expiração Automática** | Scheduler horário marca URLs expiradas como inativas |
| **Virtual Threads** | Project Loom (JDK 25) para alta performance em requisições |
| **Docker Multi-Stage** | Imagem de produção otimizada com HEALTHCHECK |
| **CI/CD** | Pipeline GitHub Actions com Testcontainers para testes de integração |

### Stack Tecnológica

```
Java 25 · Spring Boot 3.5 · Spring Security · PostgreSQL 15 · Redis 7
Flyway · Bucket4j · ZXing · Testcontainers · Docker · GitHub Actions
```

---

## 🚀 Quick Start

### Docker Compose (Recommended / Recomendado)

```bash
git clone https://github.com/marcuslaf/encurtador-de-urls.git
cd encurtador-de-urls
docker-compose up --build
```

This starts PostgreSQL, Redis, and the application. Access Swagger UI at `http://localhost:8080/swagger-ui.html`.

Isso inicia PostgreSQL, Redis e a aplicação. Acesse o Swagger UI em `http://localhost:8080/swagger-ui.html`.

### Local Development / Desenvolvimento Local

```bash
# Prerequisites: Java 25+, Maven 3.9+, Docker Desktop
mvn clean package -DskipTests
mvn spring-boot:run
```

### Environment Variables / Variáveis de Ambiente

| Variable | Default | Description / Descrição |
|----------|---------|-------------------------|
| `APP_BASE_URL` | `http://localhost:8080` | Base URL for short links / URL base para links curtos |
| `APP_API_KEY` | _(empty = no auth)_ | API key for protected endpoints / Chave de API para endpoints protegidos |
| `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_USER` | `postgres` | PostgreSQL username |
| `POSTGRES_PASSWORD` | `postgres` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_HOST_PORT` | `6379` | Redis port |

---

## 📡 API

### Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `POST` | `/api/urls` | Create short URL | API Key |
| `GET` | `/api/urls` | List active URLs (paginated) | API Key |
| `GET` | `/api/urls/{code}/stats` | Access statistics | API Key |
| `GET` | `/api/urls/{code}/qr` | QR code image (PNG) | API Key |
| `DELETE` | `/api/urls/{code}` | Soft-delete URL | API Key |
| `GET` | `/{code}` | Redirect to original URL | Public |
| `GET` | `/swagger-ui.html` | Swagger UI | Public |
| `GET` | `/actuator/health` | Health check | Public |

### Examples / Exemplos

<details>
<summary><strong>🇬🇧 Create Short URL</strong></summary>

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "originalUrl": "https://example.com/very/long/path",
    "expirationMinutes": 1440
  }'
```

Response:
```json
{
  "shortCode": "aB3xK9m",
  "shortUrl": "http://localhost:8080/aB3xK9m",
  "originalUrl": "https://example.com/very/long/path",
  "createdAt": "2026-07-15T12:00:00Z",
  "expiresAt": "2026-07-16T12:00:00Z"
}
```

</details>

<details>
<summary><strong>🇧🇷 Criar URL Curta</strong></summary>

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sua-api-key" \
  -d '{
    "originalUrl": "https://exemplo.com/caminho/muito/longo",
    "expirationMinutes": 1440
  }'
```

Resposta:
```json
{
  "shortCode": "aB3xK9m",
  "shortUrl": "http://localhost:8080/aB3xK9m",
  "originalUrl": "https://exemplo.com/caminho/muito/longo",
  "createdAt": "2026-07-15T12:00:00Z",
  "expiresAt": "2026-07-16T12:00:00Z"
}
```

</details>

<details>
<summary><strong>🔗 Create with Custom Alias / Criar com Alias Customizado</strong></summary>

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{
    "originalUrl": "https://example.com",
    "customAlias": "my-brand"
  }'
```

</details>

<details>
<summary><strong>📊 Get Statistics / Obter Estatísticas</strong></summary>

```bash
curl http://localhost:8080/api/urls/aB3xK9m/stats \
  -H "X-API-Key: your-api-key"
```

Response:
```json
{
  "shortCode": "aB3xK9m",
  "totalAccesses": 42,
  "dailyAccesses": [
    { "date": "2026-07-14", "count": 10 },
    { "date": "2026-07-15", "count": 32 }
  ]
}
```

</details>

<details>
<summary><strong>📱 Get QR Code / Obter QR Code</strong></summary>

```bash
curl http://localhost:8080/api/urls/aB3xK9m/qr --output qr.png
```

</details>

<details>
<summary><strong>🗑️ Delete URL / Deletar URL</strong></summary>

```bash
curl -X DELETE http://localhost:8080/api/urls/aB3xK9m \
  -H "X-API-Key: your-api-key"
```

</details>

---

## 🏗️ Architecture

```
com.example.urlshortener
├── config
│   ├── SecurityConfig          # Spring Security + API key filter
│   ├── ApiKeyAuthFilter        # X-API-Key header authentication
│   ├── RedisConfig             # Redis + Lettuce configuration
│   ├── RateLimitFilter         # Bucket4j distributed rate limiting
│   └── OpenApiConfig           # Swagger/OpenAPI setup
├── controller
│   ├── UrlController           # REST CRUD + QR code generation
│   └── RedirectionController   # HTTP 302 redirect
├── dto                         # Request/Response records
├── entity                      # JPA entities (Url, AccessLog)
├── exception                   # Custom exceptions + global handler
├── repository                  # JPA repositories with JPQL + native queries
├── scheduler                   # ExpirationScheduler (hourly)
├── service
│   ├── UrlService              # Core business logic
│   └── CacheService            # Redis cache operations
└── util                        # ShortCodeGenerator (SecureRandom)
```

### Key Design Decisions / Decisões de Design

| Decision | Rationale |
|----------|-----------|
| **Atomic upsert** for access counting | Prevents lost increments under concurrent requests |
| **Redis-backed rate limiting** | Consistent limits across multiple app instances |
| **Soft delete** | Preserves data integrity and URL history |
| **Bulk JPQL update** for expiration | Avoids loading all expired URLs into memory |
| **No Lombok** | Guarantees JDK 25 compatibility |
| **Virtual Threads** | High throughput for blocking Redis/JDBC calls |
| **Testcontainers** | Fully containerized integration tests (no local DB needed) |

---

## 🧪 Running Tests

### Unit Tests / Testes Unitários

```bash
# No Docker required / Não requer Docker
mvn test -Dtest="ShortCodeGeneratorTest,CreateUrlRequestTest,UrlServiceTest,CacheServiceTest,GlobalExceptionHandlerTest"
```

**31/31 passing**

### Integration Tests / Testes de Integração

```bash
# Requires Docker Desktop / Requer Docker Desktop
mvn test
```

Both PostgreSQL and Redis are spun up automatically via Testcontainers.

PostgreSQL e Redis são iniciados automaticamente via Testcontainers.

### Test Coverage / Cobertura de Testes

| Test Class | Tests | Status |
|------------|-------|--------|
| `ShortCodeGeneratorTest` | 6 | ✅ Pass |
| `CreateUrlRequestTest` | 8 | ✅ Pass |
| `UrlServiceTest` | 8 | ✅ Pass |
| `CacheServiceTest` | 4 | ✅ Pass |
| `GlobalExceptionHandlerTest` | 5 | ✅ Pass |
| **Total** | **31** | **✅ All Pass** |

---

## 🐳 Docker

### Build Image / Construir Imagem

```bash
docker build . --tag url-shortener:latest
```

### Run Container / Rodar Container

```bash
docker run -p 8080:8080 \
  -e POSTGRES_HOST=your-db-host \
  -e REDIS_HOST=your-redis-host \
  -e APP_API_KEY=your-secret-key \
  url-shortener:latest
```

The Docker image uses a multi-stage build with:
- **Build stage**: Maven + JDK 25 for compilation
- **Runtime stage**: JRE 25 Alpine with non-root user, HEALTHCHECK, and optimized JVM flags

A imagem Docker usa build multi-stage com:
- **Estágio de build**: Maven + JDK 25 para compilação
- **Estágio de runtime**: JRE 25 Alpine com usuário non-root, HEALTHCHECK e flags JVM otimizadas

---

## 📁 Project Structure

```
├── .github/workflows/         # CI/CD pipeline
├── docker-compose.yml         # PostgreSQL + Redis + App
├── Dockerfile                 # Multi-stage production build
├── LICENSE                    # MIT License
├── pom.xml                    # Maven dependencies
├── README.md                  # This file
└── src/
    ├── main/
    │   ├── java/com/example/urlshortener/
    │   └── resources/
    │       ├── application.properties
    │       ├── application-prod.properties
    │   │   └── db/migration/V1__init.sql
    └── test/
        └── java/com/example/urlshortener/
```

---

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for more information.

Distribuído sob a licença **MIT**. Veja [LICENSE](LICENSE) para mais informações.

---

<p align="center">
  Built with ☕ Java 25 + 🌱 Spring Boot 3.5<br>
  <sub>Desenvolvido com ☕ Java 25 + 🌱 Spring Boot 3.5</sub>
</p>
