package com.example.taskflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from app.jwt.* in application.properties.
 * ConfigurationPropertiesScan on the main class picks this up automatically.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expiryMs) {}
