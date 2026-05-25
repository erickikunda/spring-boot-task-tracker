package com.example.taskflow.security;

import com.example.taskflow.config.JwtProperties;
import com.example.taskflow.domain.Role;
import com.example.taskflow.domain.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // Same Base64 dev secret as application.properties default — 32 bytes, valid for HS256
    private static final String DEV_SECRET = "dGhpcy1pcy1hLWRldi1vbmx5LXNlY3JldC0zMmItaGU=";

    private final JwtService jwtService = new JwtService(new JwtProperties(DEV_SECRET, 3_600_000L));

    private UserPrincipal principal(String email) {
        return new UserPrincipal(User.builder()
                .id(UUID.randomUUID()).email(email).displayName("Test")
                .passwordHash("h").role(Role.MEMBER).build());
    }

    @Test
    void generateToken_embeds_email_as_subject() {
        var token = jwtService.generateToken(principal("alice@example.com"));
        assertThat(jwtService.extractEmail(token)).isEqualTo("alice@example.com");
    }

    @Test
    void isTokenValid_true_for_matching_user() {
        var p = principal("alice@example.com");
        assertThat(jwtService.isTokenValid(jwtService.generateToken(p), p)).isTrue();
    }

    @Test
    void isTokenValid_false_for_different_user() {
        var alice = principal("alice@example.com");
        var bob   = principal("bob@example.com");
        assertThat(jwtService.isTokenValid(jwtService.generateToken(alice), bob)).isFalse();
    }

    @Test
    void isTokenValid_false_for_expired_token() throws InterruptedException {
        var shortLived = new JwtService(new JwtProperties(DEV_SECRET, 1L)); // 1 ms TTL
        var p = principal("alice@example.com");
        var token = shortLived.generateToken(p);
        Thread.sleep(10);
        assertThat(shortLived.isTokenValid(token, p)).isFalse();
    }

    @Test
    void isTokenValid_false_for_tampered_token() {
        var p = principal("alice@example.com");
        var token = jwtService.generateToken(p) + "tampered";
        assertThat(jwtService.isTokenValid(token, p)).isFalse();
    }
}
