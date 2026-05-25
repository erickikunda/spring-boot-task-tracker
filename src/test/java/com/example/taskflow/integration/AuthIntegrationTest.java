package com.example.taskflow.integration;

import com.example.taskflow.dto.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Full-stack integration test: real PostgreSQL (Testcontainers), real Flyway migrations, real JWT.
// Requires Docker to be running. @ServiceConnection auto-configures the DataSource
// to point at the container — no manual properties needed.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired TestRestTemplate restTemplate;

    @Test
    void registerThenLogin_fullJwtFlow() {
        // 1. Register → 201 + JWT
        var register = restTemplate.postForEntity("/api/v1/auth/register",
                Map.of("email", "alice@example.com",
                        "displayName", "Alice",
                        "password", "password123"),
                AuthResponse.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(register.getBody().token()).isNotBlank();

        // 2. Login with same credentials → 200 + new JWT
        var login = restTemplate.postForEntity("/api/v1/auth/login",
                Map.of("email", "alice@example.com", "password", "password123"),
                AuthResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        var token = login.getBody().token();
        assertThat(token).isNotBlank();

        // 3. Protected endpoint without Authorization header → 401
        assertThat(restTemplate.getForEntity("/api/v1/users", String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        // 4. Same endpoint with Bearer token → 200
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var response = restTemplate.exchange("/api/v1/users", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
