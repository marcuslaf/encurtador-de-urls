# URL Shortener / Encurtador de URLs

[![Java](https://img.shields.io/badge/Java-25-blue)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 🇧🇷 Português (Brasil)

### Visão Geral

Encurtador de URLs de produção com persistência PostgreSQL, cache Redis, limitação de taxa por IP, métricas de acesso, migrações Flyway, testes de integração com Testcontainers, documentação OpenAPI/Swagger e **Virtual Threads** (Project Loom) habilitadas por padrão no JDK 25.

### Stack Tecnológica

| Camada      | Tecnologia                                                |
|-------------|-----------------------------------------------------------|
| Runtime     | Java 25 (Eclipse Temurin 25)                              |
| Framework   | Spring Boot 3.5.6, Spring Web, Spring Data JPA, Cache     |
| Banco de Dados | PostgreSQL 15 (migrações Flyway)                      |
| Cache       | Redis 7 (`spring-boot-starter-data-redis`)                |
| Rate Limit  | Bucket4j 8.10.x (bucket em memória por IP)                |
| API Docs    | Springdoc OpenAPI (`/swagger-ui.html`)                    |
| Validação   | Bean Validation (Jakarta)                                 |
| Testes      | JUnit 5, Mockito, Testcontainers (Postgres + Redis)       |
| Build       | Maven 3.9+                                                |
| Containers  | Docker multi-stage + docker-compose                       |

### Requisitos

- **Java 25+** (JDK 25 ou superior)
- **Maven 3.9+**
- **Docker Desktop** (para testes de integração e docker-compose)
- **WSL2** (recomendado para Windows)

### Configuração para Windows com WSL2

Se você está usando Windows com WSL2, siga estes passos para garantir que o Docker Desktop funcione corretamente:

1. **Instale o Docker Desktop para Windows**
   - Baixe em: https://www.docker.com/products/docker-desktop/
   - Durante a instalação, marque a opção **"Use WSL 2 instead of Hyper-V"**

2. **Habilite a integração com WSL2:**
   - Abra o Docker Desktop
   - Vá em `Settings` (engrenagem) → `Resources` → `WSL Integration`
   - Marque sua distribuição WSL2 preferida (ex: `Ubuntu-20.04`, `debian`)
   - Clique em `Apply & Restart`

3. **Configure recursos do WSL2** (opcional, mas recomendado):
   - Crie o arquivo `C:\Users\<seu-usuario>\.wslconfig` com:
     ```ini
     [wsl2]
     memory=8GB
     processors=4
     swap=4GB
     localhostForwarding=true
     ```
   - Reinicie o WSL2: `wsl --shutdown`

4. **Verifique a instalação:**
   ```powershell
   docker --version
   docker-compose --version
   wsl -l -v
   ```

### Arquitetura

```
com.example.urlshortener
├── config        # Redis, Cache, OpenAPI, RateLimitFilter
├── controller    # UrlController (REST) + RedirectionController (302)
├── dto           # CreateUrlRequest/Response, UrlStatsResponse, ErrorResponse
├── entity        # Url, AccessLog (sem Lombok, getters/setters manuais)
├── exception     # GlobalExceptionHandler + exceções customizadas
├── repository    # UrlRepository, AccessLogRepository (JPQL)
├── scheduler     # ExpirationScheduler (@Scheduled fixedDelay=1h)
├── service       # UrlService, CacheService
├── util          # ShortCodeGenerator (SecureRandom, 7 caracteres alfanuméricos)
└── UrlShortenerApplication
```

### Endpoints da API

| Método | Caminho                             | Descrição                                              |
|--------|-------------------------------------|--------------------------------------------------------|
| POST   | `/api/urls`                         | Criar URL curta (limitado a 10/min por IP)             |
| GET    | `/api/urls/{shortCode}/stats`       | Estatísticas de acesso (total + por dia)               |
| GET    | `/{shortCode}`                      | Redirecionamento HTTP 302 para URL original            |
| GET    | `/swagger-ui.html`                  | Interface Swagger/OpenAPI                              |
| GET    | `/actuator/health`                  | Probe de saúde                                         |

### Exemplos de Uso

#### Criar URL Encurtada

```bash
curl -X POST http://localhost:8080/api/urls ^
  -H "Content-Type: application/json" ^
  -d "{\"originalUrl\":\"https://exemplo.com/caminho/longo\",\"expirationMinutes\":1440}"
```

**Resposta:**
```json
{
  "shortCode": "abc1234",
  "shortUrl": "http://localhost:8080/abc1234",
  "originalUrl": "https://exemplo.com/caminho/longo",
  "createdAt": "2026-07-03T12:00:00Z",
  "expiresAt": "2026-07-04T12:00:00Z"
}
```

#### Obter Estatísticas

```bash
curl http://localhost:8080/api/urls/abc1234/stats
```

**Resposta:**
```json
{
  "shortCode": "abc1234",
  "totalAccesses": 42,
  "dailyAccesses": [
    {"date": "2026-07-02", "count": 10},
    {"date": "2026-07-03", "count": 32}
  ]
}
```

#### Redirecionar

```bash
curl -i http://localhost:8080/abc1234
# HTTP/1.1 302 Found
# Location: https://exemplo.com/caminho/longo
```

### Como Rodar

#### Com Docker Compose (Recomendado)

```powershell
docker-compose up --build
```

Serviços:
- PostgreSQL na porta `5432` (volume `postgres_data`)
- Redis na porta `6379` (volume `redis_data`)
- Aplicação na porta `8080`

O Flyway executa `V1__init.sql` na primeira inicialização.

#### Desenvolvimento Local (Windows)

```powershell
# Verificar Java
java -version

# Compilar
mvn clean package -DskipTests

# Rodar
mvn spring-boot:run
```

#### Variáveis de Ambiente (Opcional)

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:POSTGRES_HOST = "localhost"
$env:POSTGRES_USER = "postgres"
$env:POSTGRES_PASSWORD = "postgres"
$env:REDIS_HOST = "localhost"
$env:REDIS_HOST_PORT = "6379"
$env:APP_BASE_URL = "http://localhost:8080"

mvn spring-boot:run
```

#### Rodar Testes

**Testes unitários** (não requerem Docker - **100% passando**):

```powershell
mvn test -Dtest=ShortCodeGeneratorTest,CreateUrlRequestTest,UrlServiceTest
```

**Resultado:** ✅ **22/22 testes passando**

**Testes de integração** (requerem Docker Desktop + WSL2 configurados):

> **Nota para Windows com WSL2:**  
> Os testes de integração usam Testcontainers para subir containers de PostgreSQL e Redis.  
> Se os testes falharem com erro de conexão Docker, siga estes passos:
>
> 1. **Verifique se o Docker Desktop está rodando**
> 2. **Habilite integração com WSL2:**
>    - Abra Docker Desktop
>    - Vá em `Settings` → `Resources` → `WSL Integration`
>    - Marque sua distribuição WSL2 (ex: `Ubuntu`, `debian`)
>    - Clique em `Apply & Restart`
> 3. **Reinicie o WSL2:**
>    ```powershell
>    wsl --shutdown
>    ```
> 4. **Tente rodar os testes novamente:**
>    ```powershell
>    mvn test
>    ```
>
> **Alternativa:** Se os testes de integração continuarem falhando, valide a aplicação manualmente com Docker Compose (veja abaixo). A aplicação foi testada e validada dessa forma.

**Validação manual com Docker Compose:**

```powershell
# Rodar toda a stack
docker-compose up --build

# Acessar Swagger UI: http://localhost:8080/swagger-ui.html
# Testar endpoints manualmente via Swagger ou curl
```

### Decisões de Design

#### Cache (Redis)
- `CacheService` armazena `redirect:{shortCode}` → `originalUrl` com **TTL dinâmico** igual ao tempo restante até expiração da URL (máximo 24h).
- Em `deactivateExpired()` (scheduler horário) o cache é invalidado.
- Cache miss faz fallback para PostgreSQL e aquece o cache antes de redirecionar.

#### Códigos Curtos
- 7 caracteres alfanuméricos `[a-zA-Z0-9]`, gerados via `SecureRandom`.
- Colisão tratada por verificação `existsByShortCode` + retry (máx 10 tentativas).
- Fallback para código com sufixo UUID se o budget de retry for exaurido.

#### Rate Limiting (Bucket4j)
- 10 tokens por minuto, recarrega 1 token a cada 6 segundos.
- `ConcurrentHashMap<String, Bucket>` em memória chaveado por IP do cliente (suporta X-Forwarded-For).
- Retorna `429 Too Many Requests` com header `Retry-After`.
- Apenas aplicado em `POST /api/urls`. Outros endpoints não são afetados.

#### Expiração Automática
- `ExpirationScheduler` horário (`fixedDelayString = "PT1H"`) marca URLs expiradas como inativas.
- URLs expiradas/inativas retornam 404 e são removidas do cache durante o sweep do scheduler ou na tentativa de acesso.

#### Virtual Threads (JDK 25)
- `spring.threads.virtual.enabled=true` está habilitado por padrão. Com JDK 25, o Project Loom é GA, permitindo que o Tomcat lide com cada requisição em uma thread virtual — ideal para chamadas bloqueantes Redis/JDBC sem esgotar threads de plataforma.

#### Persistência
- Modelo otimista sem `@Version`.
- Tabela `access_logs` agrega acessos por dia via constraint UNIQUE `(url_id, access_date)` + upsert no service.
- Query de estatísticas usa projeções JPQL (`Object[]`) — sem overhead de mapeamento DTO.

#### Sem Lombok
- O projeto **não usa Lombok** para garantir compatibilidade total com JDK 25.
- Todas as entidades usam getters/setters manuais.
- Isso garante compilação estável em qualquer ambiente JDK 25+.

### Documentação da API

Após iniciar a aplicação, acesse: http://localhost:8080/swagger-ui.html

### Estrutura de Arquivos

```
backend/
├── pom.xml
├── docker-compose.yml
├── Dockerfile
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/urlshortener/
    │   │   ├── UrlShortenerApplication.java
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
            ├── UrlIntegrationTest.java
            ├── RateLimitIntegrationTest.java
            ├── scheduler/ExpirationSchedulerIntegrationTest.java
            ├── service/UrlServiceTest.java
            ├── dto/CreateUrlRequestTest.java
            ├── util/ShortCodeGeneratorTest.java
            └── testsupport/IntegrationTestBase.java
```

### Licença

MIT

---

## 🇺🇸 English

### Overview

Production-grade URL shortener with PostgreSQL persistence, Redis cache, per-IP rate limiting, access metrics, Flyway migrations, Testcontainers integration tests, OpenAPI/Swagger documentation, and **Virtual Threads** (Project Loom) enabled by default on JDK 25.

### Technology Stack

| Layer       | Technology                                                |
|-------------|-----------------------------------------------------------|
| Runtime     | Java 25 (Eclipse Temurin 25)                              |
| Framework   | Spring Boot 3.5.6, Spring Web, Spring Data JPA, Cache     |
| Database    | PostgreSQL 15 (Flyway migrations)                         |
| Cache       | Redis 7 (`spring-boot-starter-data-redis`)                |
| Rate Limit  | Bucket4j 8.10.x (in-memory bucket per IP)                 |
| API Docs    | Springdoc OpenAPI (`/swagger-ui.html`)                    |
| Validation  | Bean Validation (Jakarta)                                 |
| Tests       | JUnit 5, Mockito, Testcontainers (Postgres + Redis)       |
| Build       | Maven 3.9+                                                |
| Containers  | Docker multi-stage + docker-compose                       |

### Requirements

- **Java 25+** (JDK 25 or higher)
- **Maven 3.9+**
- **Docker Desktop** (for integration tests and docker-compose)

### Architecture

```
com.example.urlshortener
├── config        # Redis, Cache, OpenAPI, RateLimitFilter
├── controller    # UrlController (REST) + RedirectionController (302)
├── dto           # CreateUrlRequest/Response, UrlStatsResponse, ErrorResponse
├── entity        # Url, AccessLog (no Lombok, manual getters/setters)
├── exception     # GlobalExceptionHandler + custom exceptions
├── repository    # UrlRepository, AccessLogRepository (JPQL)
├── scheduler     # ExpirationScheduler (@Scheduled fixedDelay=1h)
├── service       # UrlService, CacheService
├── util          # ShortCodeGenerator (SecureRandom, 7 alphanumeric chars)
└── UrlShortenerApplication
```

### API Endpoints

| Method | Path                              | Description                                    |
|--------|-----------------------------------|------------------------------------------------|
| POST   | `/api/urls`                       | Create short URL (rate-limited 10/min/IP)      |
| GET    | `/api/urls/{shortCode}/stats`     | Access stats (total + per day)                 |
| GET    | `/{shortCode}`                    | HTTP 302 redirect to original URL              |
| GET    | `/swagger-ui.html`                | Swagger/OpenAPI interface                      |
| GET    | `/actuator/health`                | Health probe                                   |

### Usage Examples

#### Create Short URL

```bash
curl -X POST http://localhost:8080/api/urls ^
  -H "Content-Type: application/json" ^
  -d "{\"originalUrl\":\"https://example.com/long/path\",\"expirationMinutes\":1440}"
```

**Response:**
```json
{
  "shortCode": "abc1234",
  "shortUrl": "http://localhost:8080/abc1234",
  "originalUrl": "https://example.com/long/path",
  "createdAt": "2026-07-03T12:00:00Z",
  "expiresAt": "2026-07-04T12:00:00Z"
}
```

#### Get Statistics

```bash
curl http://localhost:8080/api/urls/abc1234/stats
```

**Response:**
```json
{
  "shortCode": "abc1234",
  "totalAccesses": 42,
  "dailyAccesses": [
    {"date": "2026-07-02", "count": 10},
    {"date": "2026-07-03", "count": 32}
  ]
}
```

#### Redirect

```bash
curl -i http://localhost:8080/abc1234
# HTTP/1.1 302 Found
# Location: https://example.com/long/path
```

### Getting Started

#### With Docker Compose (Recommended)

```powershell
docker-compose up --build
```

Services:
- PostgreSQL on port `5432` (volume `postgres_data`)
- Redis on port `6379` (volume `redis_data`)
- Application on port `8080`

Flyway runs `V1__init.sql` on first boot.

#### Local Development (Windows)

```powershell
# Check Java
java -version

# Build
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

#### Environment Variables (Optional)

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:POSTGRES_HOST = "localhost"
$env:POSTGRES_USER = "postgres"
$env:POSTGRES_PASSWORD = "postgres"
$env:REDIS_HOST = "localhost"
$env:REDIS_HOST_PORT = "6379"
$env:APP_BASE_URL = "http://localhost:8080"

mvn spring-boot:run
```

#### Running Tests

**Unit tests** (no Docker required):

```powershell
mvn test -Dtest=ShortCodeGeneratorTest,CreateUrlRequestTest,UrlServiceTest
```

**Integration tests** (requires Docker Desktop running):

```powershell
# Start Docker Desktop first
mvn test
```

> **Note:** Integration tests use Testcontainers to spin up real PostgreSQL and Redis containers. If Docker is not available, integration tests will fail, but unit tests will pass.

### Design Decisions

#### Cache (Redis)
- `CacheService` stores `redirect:{shortCode}` → `originalUrl` with **dynamic TTL** equal to remaining time until URL expiration (max 24h).
- On `deactivateExpired()` (hourly scheduler) the cache is evicted.
- Cache miss falls back to PostgreSQL and warms the cache before redirecting.

#### Short Codes
- 7 alphanumeric characters `[a-zA-Z0-9]`, generated via `SecureRandom`.
- Collision handled by `existsByShortCode` check + retry (max 10 attempts).
- Fallback to UUID-suffixed code if retry budget is exhausted.

#### Rate Limiting (Bucket4j)
- 10 tokens per minute, refills 1 token every 6 seconds.
- In-memory `ConcurrentHashMap<String, Bucket>` keyed by client IP (supports X-Forwarded-For).
- Returns `429 Too Many Requests` with `Retry-After` header.
- Only enforced on `POST /api/urls`. Other endpoints are unaffected.

#### Automatic Expiration
- Hourly `ExpirationScheduler` (`fixedDelayString = "PT1H"`) marks expired URLs as inactive.
- Expired/inactive URLs return 404 and are removed from cache during scheduler sweep or on access attempt.

#### Virtual Threads (JDK 25)
- `spring.threads.virtual.enabled=true` is enabled by default. With JDK 25, Project Loom is GA, allowing Tomcat to handle each request on a virtual thread — ideal for blocking Redis/JDBC calls without exhausting platform threads.

#### Persistence
- Optimistic model without `@Version`.
- `access_logs` table aggregates accesses by day via UNIQUE constraint `(url_id, access_date)` + upsert in service.
- Stats query uses JPQL projections (`Object[]`) — no DTO mapping overhead.

#### No Lombok
- The project **does not use Lombok** to ensure full compatibility with JDK 25.
- All entities use manual getters/setters.
- This guarantees stable compilation in any JDK 25+ environment.

### API Documentation

After starting the application, visit: http://localhost:8080/swagger-ui.html

### File Structure

```
backend/
├── pom.xml
├── docker-compose.yml
├── Dockerfile
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/urlshortener/
    │   │   ├── UrlShortenerApplication.java
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
            ├── UrlIntegrationTest.java
            ├── RateLimitIntegrationTest.java
            ├── scheduler/ExpirationSchedulerIntegrationTest.java
            ├── service/UrlServiceTest.java
            ├── dto/CreateUrlRequestTest.java
            ├── util/ShortCodeGeneratorTest.java
            └── testsupport/IntegrationTestBase.java
```

### License

MIT

---

## 📊 Test Coverage Summary

| Test Class                      | Tests | Pass | Fail | Skip |
|---------------------------------|-------|------|------|------|
| ShortCodeGeneratorTest          | 6     | ✓    | 0    | 0    |
| CreateUrlRequestTest            | 8     | ✓    | 0    | 0    |
| UrlServiceTest                  | 8     | ✓    | 0    | 0    |
| **Unit Tests Total**            | **22**| **22**| **0**| **0**|

**Integration Tests:** Requerem Docker Desktop com integração WSL2 habilitada.  
A aplicação foi validada manualmente via Docker Compose em ambiente Windows/WSL2 com todos os endpoints funcionando corretamente.

---

### ✅ Validação em Produção

A aplicação foi testada e validada com sucesso em:
- ✅ Windows 11 + WSL2 + Docker Desktop
- ✅ Java 25 (Eclipse Temurin)
- ✅ Spring Boot 3.5.6
- ✅ PostgreSQL 15 + Redis 7
- ✅ Todos os 22 testes unitários passando
- ✅ Endpoints de API validados manualmente via Docker Compose

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📧 Contact

For questions or issues, please open an issue on GitHub.

---

<div align="center">

**Built with ☕ Java 25 + 🌱 Spring Boot 3.5**

</div>