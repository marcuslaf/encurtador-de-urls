package com.example.urlshortener.testsupport;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests using:
 * - PostgreSQL via Docker (Testcontainers)
 * - Redis via Docker (Testcontainers)
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {}
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("urlshortener_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);

    @SuppressWarnings("resource")
    protected static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

    @BeforeAll
    static void setup() {
        if (!POSTGRES_CONTAINER.isRunning()) {
            POSTGRES_CONTAINER.start();
        }
        if (!REDIS_CONTAINER.isRunning()) {
            REDIS_CONTAINER.start();
        }

        System.setProperty("spring.datasource.url", POSTGRES_CONTAINER.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRES_CONTAINER.getUsername());
        System.setProperty("spring.datasource.password", POSTGRES_CONTAINER.getPassword());
        System.setProperty("spring.data.redis.host", REDIS_CONTAINER.getHost());
        System.setProperty("spring.data.redis.port", String.valueOf(REDIS_CONTAINER.getMappedPort(6379)));

        System.out.println("=== Integration tests starting ===");
        System.out.println("PostgreSQL: " + POSTGRES_CONTAINER.getJdbcUrl());
        System.out.println("Redis: " + REDIS_CONTAINER.getHost() + ":" + REDIS_CONTAINER.getMappedPort(6379));
    }
}