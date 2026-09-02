package com.example.logistics;

import com.example.logistics.infrastructure.persistence.IdempotencyJpaRepository;
import com.example.logistics.infrastructure.persistence.OutboxJpaRepository;
import com.example.logistics.infrastructure.persistence.ShipmentJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base class for integration tests that boot the full Spring context backed by
 * Testcontainers (PostgreSQL, RabbitMQ, Keycloak) and authenticate against the real
 * Keycloak realm.
 *
 * <p>Containers are shared per JVM to keep the suite fast. Security is fully enabled;
 * requests here send real JWTs minted from the Keycloak token endpoint.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("shipments")
                    .withUsername("shipment")
                    .withPassword("shipment");

    protected static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine")
                    .withUser("guest", "guest");

    protected static final GenericContainer<?> KEYCLOAK;

    protected static String keycloakBaseUrl;
    protected static String keycloakIssuerUri;

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Map<String, String> TOKEN_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        KEYCLOAK = new GenericContainer<>("quay.io/keycloak/keycloak:24.0.4")
                .withExposedPorts(8080)
                .withCommand("start-dev", "--import-realm")
                .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                .withEnv("KC_DB", "dev-file")
                .withFileSystemBind(
                        "src/test/resources/keycloak-realm-export.json",
                        "/opt/keycloak/data/import/realm-export.json")
                .waitingFor(Wait.forHttp("/realms/master")
                        .forPort(8080)
                        .withStartupTimeout(Duration.ofSeconds(150)));
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ShipmentJpaRepository shipmentJpaRepository;

    @Autowired
    protected OutboxJpaRepository outboxJpaRepository;

    @Autowired
    protected IdempotencyJpaRepository idempotencyJpaRepository;

    @BeforeAll
    static void startContainers() {
        POSTGRES.start();
        RABBITMQ.start();
        if (!KEYCLOAK.isRunning()) {
            KEYCLOAK.start();
        }
        keycloakBaseUrl = "http://localhost:" + KEYCLOAK.getMappedPort(8080);
        keycloakIssuerUri = keycloakBaseUrl + "/realms/logistics";
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloakIssuerUri);
        registry.add("app.openapi.oauth.token-url", () ->
                keycloakBaseUrl + "/realms/logistics/protocol/openid-connect/token");
    }

    /**
     * Obtains a bearer token from the running Keycloak for a demo user.
     */
    protected String tokenFor(String username, String password) {
        return TOKEN_CACHE.computeIfAbsent(username + ":" + password,
                k -> fetchToken(username, password));
    }

    protected String adminToken() {
        return tokenFor("admin", "admin");
    }

    protected String operatorToken() {
        return tokenFor("operator", "operator123");
    }

    protected String userToken() {
        return tokenFor("user", "user123");
    }

    private String fetchToken(String username, String password) {
        final String tokenUrl = keycloakBaseUrl + "/realms/logistics/protocol/openid-connect/token";
        final String body = Map.of(
                "client_id", "shipment-service",
                "client_secret", "shipment-service-secret",
                "username", username,
                "password", password,
                "grant_type", "password").entrySet().stream()
                .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), java.nio.charset.StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        try {
            final HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            final HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new IllegalStateException("Token request failed: " + resp.statusCode() + " " + resp.body());
            }
            final JsonNode node = objectMapper.readTree(resp.body());
            return node.get("access_token").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to fetch Keycloak token", e);
        }
    }

    protected static HttpHeaders bearer(String token) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }
}
