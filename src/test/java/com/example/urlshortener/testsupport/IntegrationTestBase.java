package com.example.urlshortener.testsupport;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Classe base para testes de integração usando:
 * - PostgreSQL local (localhost:5432)
 * - Redis via Docker (Testcontainers)
 * 
 * Requisitos:
 * 1. PostgreSQL rodando localmente em localhost:5432 com banco 'urlshortener_test'
 * 2. Docker Desktop rodando (para o container Redis)
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {}
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    // Container Redis usando Docker
    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

    @BeforeAll
    static void setup() {
        // Iniciar Redis via Docker
        if (!REDIS_CONTAINER.isRunning()) {
            REDIS_CONTAINER.start();
        }
        
        System.out.println("=== Iniciando testes de integração ===");
        System.out.println("DB: PostgreSQL local (localhost:5432/urlshortener_test)");
        System.out.println("Redis: Docker container on port " + REDIS_CONTAINER.getMappedPort(6379));
    }
}